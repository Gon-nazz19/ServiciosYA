// Firestore Security Rules tests (US-130). Run with `npm test`: it starts the local
// Firestore emulator with a demo project, so the real database is never touched.
const { readFileSync } = require('node:fs');
const path = require('node:path');
const { before, after, beforeEach, describe, test } = require('node:test');
const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require('@firebase/rules-unit-testing');
const {
  addDoc,
  collection,
  deleteDoc,
  doc,
  getDoc,
  getDocs,
  query,
  serverTimestamp,
  setDoc,
  updateDoc,
  where,
} = require('firebase/firestore');

const ALICE = 'alice-uid';
const BOB = 'bob-uid';

let testEnv;

// Same payload FirestoreServiceRequestRepository sends.
function newRequest(clientId, overrides = {}) {
  return {
    clientId,
    providerId: 'carlos-electricidad',
    categoryId: 'electricistas',
    providerName: 'Carlos Electricidad',
    categoryName: 'Electricista',
    message: 'Necesito cambiar un enchufe',
    status: 'PENDING',
    createdAt: serverTimestamp(),
    ...overrides,
  };
}

// Same payload FirebaseAuthRepository sends on registration.
function newUser(overrides = {}) {
  return {
    name: 'Alice',
    email: 'alice@example.com',
    role: 'CLIENT',
    createdAt: serverTimestamp(),
    ...overrides,
  };
}

const signedIn = (uid) => testEnv.authenticatedContext(uid).firestore();
const anonymous = () => testEnv.unauthenticatedContext().firestore();

before(async () => {
  testEnv = await initializeTestEnvironment({
    projectId: 'demo-serviciosya',
    firestore: {
      rules: readFileSync(path.join(__dirname, '..', '..', 'firestore.rules'), 'utf8'),
      host: '127.0.0.1',
      port: 8080,
    },
  });
});

after(async () => {
  await testEnv.cleanup();
});

beforeEach(async () => {
  await testEnv.clearFirestore();
  await testEnv.withSecurityRulesDisabled(async (context) => {
    const db = context.firestore();
    await setDoc(doc(db, 'categories/electricistas'), { name: 'Electricista', icon: 'electrical_services', active: true });
    await setDoc(doc(db, 'providers/carlos-electricidad'), {
      name: 'Carlos Electricidad', categoryId: 'electricistas', city: 'San Francisco', active: true,
    });
    await setDoc(doc(db, 'serviceRequests/alice-request'), { ...newRequest(ALICE), createdAt: new Date() });
    await setDoc(doc(db, 'serviceRequests/bob-request'), { ...newRequest(BOB), createdAt: new Date() });
    await setDoc(doc(db, 'users/bob-uid'), { ...newUser({ name: 'Bob', email: 'bob@example.com' }), createdAt: new Date() });
  });
});

describe('categories', () => {
  test('signed-in users can read active categories like the Home query', async () => {
    const db = signedIn(ALICE);
    await assertSucceeds(getDocs(query(collection(db, 'categories'), where('active', '==', true))));
  });

  test('anonymous users cannot read categories', async () => {
    await assertFails(getDocs(collection(anonymous(), 'categories')));
  });

  test('nobody can write categories from the app', async () => {
    await assertFails(setDoc(doc(signedIn(ALICE), 'categories/nueva'), { name: 'Nueva', active: true }));
    await assertFails(deleteDoc(doc(signedIn(ALICE), 'categories/electricistas')));
  });
});

describe('providers', () => {
  test('signed-in users can list providers by category and open one', async () => {
    const db = signedIn(ALICE);
    await assertSucceeds(getDocs(query(
      collection(db, 'providers'),
      where('categoryId', '==', 'electricistas'),
      where('active', '==', true),
    )));
    await assertSucceeds(getDocs(query(collection(db, 'providers'), where('active', '==', true))));
    await assertSucceeds(getDoc(doc(db, 'providers/carlos-electricidad')));
  });

  test('anonymous users cannot read providers', async () => {
    await assertFails(getDoc(doc(anonymous(), 'providers/carlos-electricidad')));
  });

  test('nobody can create or edit providers from the app', async () => {
    const db = signedIn(ALICE);
    await assertFails(setDoc(doc(db, 'providers/fake'), { name: 'Fake', categoryId: 'electricistas', active: true }));
    await assertFails(updateDoc(doc(db, 'providers/carlos-electricidad'), { rating: 5 }));
  });
});

describe('serviceRequests', () => {
  test('a signed-in user can create their own pending request', async () => {
    await assertSucceeds(addDoc(collection(signedIn(ALICE), 'serviceRequests'), newRequest(ALICE)));
  });

  test('a request with an empty message is allowed', async () => {
    await assertSucceeds(addDoc(collection(signedIn(ALICE), 'serviceRequests'), newRequest(ALICE, { message: '' })));
  });

  test('anonymous users cannot create requests', async () => {
    await assertFails(addDoc(collection(anonymous(), 'serviceRequests'), newRequest(ALICE)));
  });

  test('a user cannot create a request on behalf of someone else', async () => {
    await assertFails(addDoc(collection(signedIn(ALICE), 'serviceRequests'), newRequest(BOB)));
  });

  test('new requests must be PENDING with server timestamp', async () => {
    const db = signedIn(ALICE);
    await assertFails(addDoc(collection(db, 'serviceRequests'), newRequest(ALICE, { status: 'ACCEPTED' })));
    await assertFails(addDoc(collection(db, 'serviceRequests'), newRequest(ALICE, { createdAt: new Date(2020, 0, 1) })));
  });

  test('requests must point to an existing provider', async () => {
    await assertFails(addDoc(
      collection(signedIn(ALICE), 'serviceRequests'),
      newRequest(ALICE, { providerId: 'no-existe' }),
    ));
  });

  test('messages over 500 characters and unknown fields are rejected', async () => {
    const db = signedIn(ALICE);
    await assertFails(addDoc(collection(db, 'serviceRequests'), newRequest(ALICE, { message: 'a'.repeat(501) })));
    await assertFails(addDoc(collection(db, 'serviceRequests'), newRequest(ALICE, { price: 1000 })));
  });

  test('"Mis solicitudes" query returns only the own requests', async () => {
    const db = signedIn(ALICE);
    const snapshot = await assertSucceeds(getDocs(query(
      collection(db, 'serviceRequests'),
      where('clientId', '==', ALICE),
    )));
    const ids = snapshot.docs.map((d) => d.id);
    if (ids.length !== 1 || ids[0] !== 'alice-request') {
      throw new Error(`Se esperaba solo alice-request y llegó: ${ids.join(', ')}`);
    }
  });

  test('a user cannot read requests of other users', async () => {
    const db = signedIn(ALICE);
    await assertFails(getDoc(doc(db, 'serviceRequests/bob-request')));
    await assertFails(getDocs(query(collection(db, 'serviceRequests'), where('clientId', '==', BOB))));
    await assertFails(getDocs(collection(db, 'serviceRequests')));
  });

  test('anonymous users cannot read any request', async () => {
    await assertFails(getDoc(doc(anonymous(), 'serviceRequests/alice-request')));
  });

  test('clients cannot change or delete requests', async () => {
    const db = signedIn(ALICE);
    await assertFails(updateDoc(doc(db, 'serviceRequests/alice-request'), { status: 'COMPLETED' }));
    await assertFails(deleteDoc(doc(db, 'serviceRequests/alice-request')));
  });
});

describe('users', () => {
  test('registration creates the own CLIENT profile', async () => {
    await assertSucceeds(setDoc(doc(signedIn(ALICE), `users/${ALICE}`), newUser()));
  });

  test('a user cannot create a profile for another uid', async () => {
    await assertFails(setDoc(doc(signedIn(ALICE), `users/${BOB}-2`), newUser()));
  });

  test('a user cannot self-assign another role', async () => {
    await assertFails(setDoc(doc(signedIn(ALICE), `users/${ALICE}`), newUser({ role: 'PROVIDER' })));
    await assertFails(setDoc(doc(signedIn(ALICE), `users/${ALICE}`), newUser({ role: 'ADMIN' })));
  });

  test('users can read only their own profile', async () => {
    await setDoc(doc(signedIn(ALICE), `users/${ALICE}`), newUser());
    await assertSucceeds(getDoc(doc(signedIn(ALICE), `users/${ALICE}`)));
    await assertFails(getDoc(doc(signedIn(ALICE), `users/${BOB}`)));
    await assertFails(getDoc(doc(anonymous(), `users/${BOB}`)));
  });

  test('profiles cannot be modified afterwards', async () => {
    await assertFails(updateDoc(doc(signedIn(BOB), `users/${BOB}`), { role: 'ADMIN' }));
  });
});

describe('everything else', () => {
  test('unknown collections are denied', async () => {
    await assertFails(setDoc(doc(signedIn(ALICE), 'secrets/x'), { a: 1 }));
    await assertFails(getDoc(doc(signedIn(ALICE), 'secrets/x')));
  });
});
