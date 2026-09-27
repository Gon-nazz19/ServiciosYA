#!/usr/bin/env node
/**
 * Loads the ServiciosYa test dataset into Cloud Firestore.
 *
 * Idempotent: every document has a fixed id and is written with set(), so running
 * the script again overwrites the same documents instead of duplicating them.
 *
 * Usage:
 *   node seed.js --dry-run   Validate seed-data.json without connecting to Firebase.
 *   node seed.js             Write to the project of GOOGLE_APPLICATION_CREDENTIALS.
 */
const path = require('path');
const data = require(path.join(__dirname, 'seed-data.json'));

const PROVIDERS_PER_CATEGORY = 3;
const EXPECTED_CATEGORIES = 5;

function validate({ categories, providers }) {
  const errors = [];
  const categoryIds = new Set();

  if (categories.length !== EXPECTED_CATEGORIES) {
    errors.push(`Se esperaban ${EXPECTED_CATEGORIES} categorías y hay ${categories.length}.`);
  }
  for (const category of categories) {
    if (!category.id || !category.name || !category.icon) {
      errors.push(`Categoría incompleta: ${JSON.stringify(category)}`);
    }
    if (categoryIds.has(category.id)) errors.push(`Categoría duplicada: ${category.id}`);
    categoryIds.add(category.id);
  }

  const providerIds = new Set();
  const perCategory = {};
  for (const provider of providers) {
    if (!provider.id || !provider.name || !provider.categoryId || !provider.city) {
      errors.push(`Prestador incompleto: ${JSON.stringify(provider)}`);
    }
    if (providerIds.has(provider.id)) errors.push(`Prestador duplicado: ${provider.id}`);
    providerIds.add(provider.id);
    if (!categoryIds.has(provider.categoryId)) {
      errors.push(`El prestador ${provider.id} referencia una categoría inexistente: ${provider.categoryId}`);
    }
    if (provider.rating !== null && (typeof provider.rating !== 'number' || provider.rating < 0 || provider.rating > 5)) {
      errors.push(`Rating inválido en ${provider.id}: ${provider.rating}`);
    }
    perCategory[provider.categoryId] = (perCategory[provider.categoryId] || 0) + 1;
  }
  for (const id of categoryIds) {
    if (perCategory[id] !== PROVIDERS_PER_CATEGORY) {
      errors.push(`La categoría ${id} tiene ${perCategory[id] || 0} prestadores (se esperaban ${PROVIDERS_PER_CATEGORY}).`);
    }
  }
  return errors;
}

function toProviderDocument(provider) {
  const document = {
    userId: '',
    name: provider.name,
    description: provider.description,
    categoryId: provider.categoryId,
    city: provider.city,
    profileImageUrl: '',
    phone: provider.phone,
    reviewCount: provider.reviewCount,
    verified: provider.verified,
    active: true,
  };
  // The app treats a missing rating as "no rating yet".
  if (provider.rating !== null) document.rating = provider.rating;
  return document;
}

async function main() {
  const dryRun = process.argv.includes('--dry-run');
  const errors = validate(data);
  if (errors.length > 0) {
    console.error('seed-data.json tiene errores:\n- ' + errors.join('\n- '));
    process.exit(1);
  }
  console.log(`Datos válidos: ${data.categories.length} categorías, ${data.providers.length} prestadores.`);
  if (dryRun) {
    console.log('Modo --dry-run: no se escribió nada en Firestore.');
    return;
  }

  if (!process.env.GOOGLE_APPLICATION_CREDENTIALS) {
    console.error(
      'Falta GOOGLE_APPLICATION_CREDENTIALS. Apuntala al JSON de la cuenta de servicio (ver README.md).',
    );
    process.exit(1);
  }

  // Required lazily so --dry-run works without installing dependencies.
  const { initializeApp, applicationDefault } = require('firebase-admin/app');
  const { getFirestore } = require('firebase-admin/firestore');
  const firestore = getFirestore(initializeApp({ credential: applicationDefault() }));

  const batch = firestore.batch();
  for (const { id, ...category } of data.categories) {
    batch.set(firestore.collection('categories').doc(id), category);
  }
  for (const provider of data.providers) {
    batch.set(firestore.collection('providers').doc(provider.id), toProviderDocument(provider));
  }
  await batch.commit();

  const { project_id: projectId } = require(path.resolve(process.env.GOOGLE_APPLICATION_CREDENTIALS));
  console.log(`Seed cargado en ${projectId}.`);
}

main().catch((error) => {
  console.error('No se pudo cargar el seed:', error.message);
  process.exit(1);
});
