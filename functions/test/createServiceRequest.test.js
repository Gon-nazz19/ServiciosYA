const assert = require('node:assert/strict');
const { before, after, beforeEach, test } = require('node:test');
const { initializeApp, deleteApp } = require('firebase-admin/app');
const { getFirestore } = require('firebase-admin/firestore');

const projectId = 'demo-serviciosya';
const endpoint = `http://127.0.0.1:5001/${projectId}/southamerica-east1/createServiceRequest`;
const authEndpoint = 'http://127.0.0.1:9099/identitytoolkit.googleapis.com/v1/accounts:signUp?key=fake-key';
let app;
let db;
let token;
let uid;
let sequence = 0;

const key = () => (++sequence).toString(16).padStart(32, '0');
const payload = (overrides = {}) => ({ providerId: 'carlos', message: '  Hola  ', idempotencyKey: key(), ...overrides });

async function invoke(data, bearer = token) {
  const response = await fetch(endpoint, {
    method: 'POST',
    headers: { 'content-type': 'application/json', ...(bearer ? { authorization: `Bearer ${bearer}` } : {}) },
    body: JSON.stringify({ data }),
  });
  return { status: response.status, body: await response.json() };
}

before(() => {
  app = initializeApp({ projectId });
  db = getFirestore(app);
});

after(async () => { await deleteApp(app); });

beforeEach(async () => {
  const response = await fetch(authEndpoint, {
    method: 'POST',
    headers: { 'content-type': 'application/json' },
    body: JSON.stringify({ email: `user-${Date.now()}-${key()}@example.com`, password: 'secret123', returnSecureToken: true }),
  });
  assert.equal(response.ok, true);
  const account = await response.json();
  token = account.idToken;
  uid = account.localId;
  await Promise.all([
    db.doc(`users/${uid}`).set({ role: 'CLIENT' }),
    db.doc('categories/electricistas').set({ name: 'Electricista', active: true }),
    db.doc('providers/carlos').set({ name: 'Carlos Electricidad', categoryId: 'electricistas', active: true }),
    db.doc('providers/inactivo').set({ name: 'Inactivo', categoryId: 'electricistas', active: false }),
  ]);
});

test('requires authentication and a CLIENT profile', async () => {
  assert.equal((await invoke(payload(), null)).body.error.status, 'UNAUTHENTICATED');
  await db.doc(`users/${uid}`).set({ role: 'PROVIDER' });
  assert.equal((await invoke(payload())).body.error.status, 'PERMISSION_DENIED');
});

test('rejects invalid inputs and unavailable providers', async () => {
  for (const data of [
    payload({ providerId: '' }), payload({ providerId: 'a/b' }),
    payload({ message: 123 }), payload({ message: 'a'.repeat(501) }),
    payload({ idempotencyKey: 'bad' }),
  ]) {
    assert.equal((await invoke(data)).body.error.status, 'INVALID_ARGUMENT');
  }
  assert.equal((await invoke(payload({ providerId: 'missing' }))).body.error.status, 'NOT_FOUND');
  assert.equal((await invoke(payload({ providerId: 'inactivo' }))).body.error.status, 'NOT_FOUND');
});

test('creates a pending request using only server-side catalog data', async () => {
  const result = await invoke({ ...payload(), providerName: 'Spoof', categoryName: 'Spoof', categoryId: 'fake', clientId: 'other', status: 'COMPLETED' });
  assert.equal(result.status, 200);
  const id = result.body.result.requestId;
  assert.match(id, /^[0-9a-f]{64}$/);
  const saved = (await db.doc(`serviceRequests/${id}`).get()).data();
  assert.equal(saved.clientId, uid);
  assert.equal(saved.providerName, 'Carlos Electricidad');
  assert.equal(saved.categoryName, 'Electricista');
  assert.equal(saved.categoryId, 'electricistas');
  assert.equal(saved.message, 'Hola');
  assert.equal(saved.status, 'PENDING');
  assert.ok(saved.createdAt.toMillis() > 0);
});

test('retries with the same key return the same request, including concurrent calls', async () => {
  const data = payload();
  const [first, second] = await Promise.all([invoke(data), invoke(data)]);
  assert.equal(first.status, 200);
  assert.equal(second.status, 200);
  assert.equal(first.body.result.requestId, second.body.result.requestId);
  assert.equal((await invoke(data)).body.result.requestId, first.body.result.requestId);
  const snapshot = await db.collection('serviceRequests').where('clientId', '==', uid).get();
  assert.equal(snapshot.size, 1);
  assert.equal((await invoke({ ...data, message: 'Different' })).body.error.status, 'ALREADY_EXISTS');
});
