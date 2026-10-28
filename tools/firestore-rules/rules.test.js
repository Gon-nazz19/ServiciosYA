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
      port: 8081,
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
  test('a signed-in user cannot create a request directly, even with a valid payload', async () => {
    await assertFails(addDoc(collection(signedIn(ALICE), 'serviceRequests'), newRequest(ALICE)));
  });

  test('anonymous users cannot create requests', async () => {
    await assertFails(addDoc(collection(anonymous(), 'serviceRequests'), newRequest(ALICE)));
  });

  test('a user cannot create a request on behalf of someone else', async () => {
    await assertFails(addDoc(collection(signedIn(ALICE), 'serviceRequests'), newRequest(BOB)));
  });

  test('changing status or timestamp does not allow direct creation', async () => {
    const db = signedIn(ALICE);
    await assertFails(addDoc(collection(db, 'serviceRequests'), newRequest(ALICE, { status: 'ACCEPTED' })));
    await assertFails(addDoc(collection(db, 'serviceRequests'), newRequest(ALICE, { createdAt: new Date(2020, 0, 1) })));
  });

  test('a direct request cannot point to an unknown provider', async () => {
    await assertFails(addDoc(
      collection(signedIn(ALICE), 'serviceRequests'),
      newRequest(ALICE, { providerId: 'no-existe' }),
    ));
  });

  test('other payloads cannot bypass the callable', async () => {
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

// Same payload FirestoreSubscriptionStore sends.
const inDays = (days) => new Date(Date.now() + days * 24 * 60 * 60 * 1000);
function newSubscription(overrides = {}) {
  return {
    adFree: { plan: 'adfree_yearly', expiresAt: inDays(365) },
    updatedAt: serverTimestamp(),
    ...overrides,
  };
}

describe('subscriptions (simulated payment gateway)', () => {
  const ref = (db, uid = ALICE) => doc(db, `subscriptions/${uid}`);

  test('a user can save the own yearly and lifetime client plans', async () => {
    const db = signedIn(ALICE);
    await assertSucceeds(setDoc(ref(db), newSubscription()));
    await assertSucceeds(setDoc(ref(db), newSubscription({ adFree: { plan: 'adfree_lifetime', expiresAt: null } })));
  });

  test('a user can save a provider plan with a free trial', async () => {
    const featured = { plan: 'featured_monthly', trialEndsAt: inDays(7), expiresAt: inDays(37) };
    await assertSucceeds(setDoc(ref(signedIn(ALICE)), { featured, updatedAt: serverTimestamp() }));
    await assertSucceeds(setDoc(ref(signedIn(ALICE)), newSubscription({
      featured: { plan: 'featured_weekly', expiresAt: inDays(7) },
    })));
  });

  test('users read only their own subscription', async () => {
    await setDoc(ref(signedIn(ALICE)), newSubscription());
    await assertSucceeds(getDoc(ref(signedIn(ALICE))));
    await assertFails(getDoc(ref(signedIn(BOB), ALICE)));
    await assertFails(getDoc(ref(anonymous(), ALICE)));
  });

  test('a user cannot write the subscription of someone else or write anonymously', async () => {
    await assertFails(setDoc(ref(signedIn(ALICE), BOB), newSubscription()));
    await assertFails(setDoc(ref(anonymous(), ALICE), newSubscription()));
  });

  test('other fields and a fake update time are rejected', async () => {
    const db = signedIn(ALICE);
    await assertFails(setDoc(ref(db), newSubscription({ admin: true })));
    await assertFails(setDoc(ref(db), newSubscription({ updatedAt: new Date(2020, 0, 1) })));
  });

  test('unknown plans, extra keys and plans in the wrong slot are rejected', async () => {
    const db = signedIn(ALICE);
    await assertFails(setDoc(ref(db), newSubscription({ adFree: { plan: 'gold', expiresAt: null } })));
    await assertFails(setDoc(ref(db), newSubscription({ adFree: { plan: 'adfree_yearly', expiresAt: null, free: true } })));
    await assertFails(setDoc(ref(db), newSubscription({ adFree: { plan: 'featured_weekly', expiresAt: inDays(7) } })));
    await assertFails(setDoc(ref(db), {
      featured: { plan: 'adfree_lifetime', expiresAt: null },
      updatedAt: serverTimestamp(),
    }));
  });

  test('a subscription cannot be deleted', async () => {
    await setDoc(ref(signedIn(ALICE)), newSubscription());
    await assertFails(deleteDoc(ref(signedIn(ALICE))));
  });
});

describe('providers: the owner can only set featuredUntil', () => {
  beforeEach(async () => {
    await testEnv.withSecurityRulesDisabled(async (context) => {
      await setDoc(doc(context.firestore(), 'providers/alice-plumbing'), {
        userId: ALICE, name: 'Alice Plomería', categoryId: 'plomeros', city: 'San Francisco', active: true, rating: 4.5,
      });
    });
  });

  test('the owner can set featuredUntil on the own profile', async () => {
    await assertSucceeds(updateDoc(doc(signedIn(ALICE), 'providers/alice-plumbing'), { featuredUntil: inDays(30) }));
  });

  test('another user cannot feature a profile that is not theirs', async () => {
    await assertFails(updateDoc(doc(signedIn(BOB), 'providers/alice-plumbing'), { featuredUntil: inDays(30) }));
    await assertFails(updateDoc(doc(anonymous(), 'providers/alice-plumbing'), { featuredUntil: inDays(30) }));
  });

  test('the owner cannot change anything else of the profile', async () => {
    const ref = doc(signedIn(ALICE), 'providers/alice-plumbing');
    await assertFails(updateDoc(ref, { rating: 5 }));
    await assertFails(updateDoc(ref, { active: false }));
    await assertFails(updateDoc(ref, { userId: BOB }));
    await assertFails(updateDoc(ref, { featuredUntil: inDays(30), rating: 5 }));
  });

  test('featuredUntil has to be a timestamp', async () => {
    const ref = doc(signedIn(ALICE), 'providers/alice-plumbing');
    await assertFails(updateDoc(ref, { featuredUntil: 'tomorrow' }));
    await assertFails(updateDoc(ref, { featuredUntil: 123456789 }));
  });

  test('a profile without userId cannot be updated by anyone', async () => {
    await assertFails(updateDoc(doc(signedIn(ALICE), 'providers/carlos-electricidad'), { featuredUntil: inDays(30) }));
  });

  test('profiles still cannot be created or deleted from the app', async () => {
    await assertFails(setDoc(doc(signedIn(ALICE), 'providers/new'), { userId: ALICE, name: 'New', active: true }));
    await assertFails(deleteDoc(doc(signedIn(ALICE), 'providers/alice-plumbing')));
  });

  test('"Mi plan" can find the own provider profile by userId', async () => {
    const snapshot = await assertSucceeds(getDocs(query(
      collection(signedIn(ALICE), 'providers'),
      where('userId', '==', ALICE),
    )));
    if (snapshot.docs.length !== 1 || snapshot.docs[0].id !== 'alice-plumbing') {
      throw new Error(`Se esperaba solo alice-plumbing y llegó: ${snapshot.docs.map((d) => d.id).join(', ')}`);
    }
  });
});

describe('everything else', () => {
  test('unknown collections are denied', async () => {
    await assertFails(setDoc(doc(signedIn(ALICE), 'secrets/x'), { a: 1 }));
    await assertFails(getDoc(doc(signedIn(ALICE), 'secrets/x')));
  });
});
