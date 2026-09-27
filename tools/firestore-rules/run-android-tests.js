#!/usr/bin/env node
// Runs the app's instrumented repository tests (US-151). Meant to be launched by
// `npm run test:android`, which starts the Firestore and Auth emulators first.
const { spawnSync } = require('node:child_process');
const path = require('node:path');

const root = path.join(__dirname, '..', '..');
const gradlew = path.join(root, process.platform === 'win32' ? 'gradlew.bat' : 'gradlew');

const result = spawnSync(gradlew, ['connectedDebugAndroidTest', '--console=plain'], {
  cwd: root,
  stdio: 'inherit',
  shell: process.platform === 'win32',
});
process.exit(result.status ?? 1);
