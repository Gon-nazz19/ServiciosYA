#!/usr/bin/env node
/**
 * Publishes ../../firestore.rules to the Firebase project of GOOGLE_APPLICATION_CREDENTIALS.
 * Uses the Admin SDK, so it does not need `firebase login`.
 *
 * Before releasing, the currently published rules are saved to
 * rules-backup-<timestamp>.rules (ignored by Git) so they can be restored.
 */
const fs = require('node:fs');
const path = require('node:path');

async function main() {
  const credentials = process.env.GOOGLE_APPLICATION_CREDENTIALS;
  if (!credentials) {
    console.error('Falta GOOGLE_APPLICATION_CREDENTIALS. Apuntala al JSON de la cuenta de servicio (ver README.md).');
    process.exit(1);
  }
  const { project_id: projectId } = require(path.resolve(credentials));
  const source = fs.readFileSync(path.join(__dirname, '..', '..', 'firestore.rules'), 'utf8');

  const { initializeApp, applicationDefault } = require('firebase-admin/app');
  const { getSecurityRules } = require('firebase-admin/security-rules');
  const securityRules = getSecurityRules(initializeApp({ credential: applicationDefault() }));

  try {
    const current = await securityRules.getFirestoreRuleset();
    const backupFile = path.join(__dirname, `rules-backup-${Date.now()}.rules`);
    fs.writeFileSync(backupFile, current.source.map((file) => file.content).join('\n'));
    console.log(`Reglas anteriores guardadas en ${path.basename(backupFile)}`);
  } catch (error) {
    console.log(`No había reglas publicadas para respaldar (${error.message}).`);
  }

  const ruleset = await securityRules.releaseFirestoreRulesetFromSource(source);
  console.log(`Reglas publicadas en ${projectId} (ruleset ${ruleset.name}).`);
}

main().catch((error) => {
  console.error('No se pudieron publicar las reglas:', error.message);
  process.exit(1);
});
