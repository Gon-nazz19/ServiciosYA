#!/usr/bin/env node
/**
 * Usage metrics for the TP2 analysis (Measure step), read from Firebase Auth and Firestore.
 * Read-only: it never writes to Firebase.
 *
 * Usage:
 *   node report.js [--since 2026-10-01T18:00] [--exclude a@x.com,b@y.com]
 *
 *   --since    Only users created and requests made from this local date/time on
 *              (use the start time of the test session).
 *   --exclude  Comma-separated emails to leave out (e.g. developer accounts).
 */
const path = require('node:path');

function parseArgs(argv) {
  const args = { since: null, exclude: [] };
  for (let i = 0; i < argv.length; i++) {
    const value = argv[i + 1];
    if (argv[i] === '--since') {
      // A date without time would be parsed as UTC; treat it as local midnight instead.
      const date = new Date(/^\d{4}-\d{2}-\d{2}$/.test(value || '') ? `${value}T00:00` : value);
      if (!value || Number.isNaN(date.getTime())) {
        throw new Error(`Fecha inválida para --since: "${value}". Usá por ejemplo 2026-10-01T18:00`);
      }
      args.since = date;
      i++;
    } else if (argv[i] === '--exclude') {
      args.exclude = (value || '').split(',').map((e) => e.trim().toLowerCase()).filter(Boolean);
      i++;
    } else {
      throw new Error(`Opción desconocida: ${argv[i]}`);
    }
  }
  return args;
}

const pct = (part, total) => (total === 0 ? '—' : `${((part / total) * 100).toFixed(1)} %`);
const fmtDate = (date) => date.toLocaleString('es-AR', { dateStyle: 'short', timeStyle: 'short' });

function table(headers, rows) {
  if (rows.length === 0) return '_Sin datos._\n';
  const line = (cells) => `| ${cells.join(' | ')} |`;
  return [line(headers), line(headers.map(() => '---')), ...rows.map(line)].join('\n') + '\n';
}

function countBy(items, key) {
  const counts = new Map();
  for (const item of items) counts.set(key(item), (counts.get(key(item)) || 0) + 1);
  return [...counts.entries()].sort((a, b) => b[1] - a[1]);
}

async function listAllUsers(auth) {
  const users = [];
  let pageToken;
  do {
    const page = await auth.listUsers(1000, pageToken);
    users.push(...page.users);
    pageToken = page.pageToken;
  } while (pageToken);
  return users;
}

async function main() {
  const args = parseArgs(process.argv.slice(2));
  const credentials = process.env.GOOGLE_APPLICATION_CREDENTIALS;
  if (!credentials) {
    throw new Error('Falta GOOGLE_APPLICATION_CREDENTIALS. Apuntala al JSON de la cuenta de servicio (ver README.md).');
  }
  const { project_id: projectId } = require(path.resolve(credentials));

  const { initializeApp, applicationDefault } = require('firebase-admin/app');
  const { getAuth } = require('firebase-admin/auth');
  const { getFirestore } = require('firebase-admin/firestore');
  const app = initializeApp({ credential: applicationDefault() });
  const db = getFirestore(app);

  const allUsers = await listAllUsers(getAuth(app));
  const excludedUids = new Set(
    allUsers.filter((u) => args.exclude.includes((u.email || '').toLowerCase())).map((u) => u.uid),
  );
  const users = allUsers.filter((u) => {
    if (excludedUids.has(u.uid)) return false;
    return !args.since || new Date(u.metadata.creationTime) >= args.since;
  });

  const [requestsSnap, providersSnap, categoriesSnap] = await Promise.all([
    db.collection('serviceRequests').get(),
    db.collection('providers').get(),
    db.collection('categories').get(),
  ]);
  const categoryNames = new Map(categoriesSnap.docs.map((d) => [d.id, d.get('name')]));
  const providerNames = new Map(providersSnap.docs.map((d) => [d.id, d.get('name')]));

  const requests = requestsSnap.docs
    .map((d) => ({ id: d.id, ...d.data(), createdAt: d.get('createdAt')?.toDate() ?? null }))
    .filter((r) => !excludedUids.has(r.clientId))
    .filter((r) => !args.since || (r.createdAt && r.createdAt >= args.since));

  const userIds = new Set(users.map((u) => u.uid));
  const requesters = new Set(requests.map((r) => r.clientId));
  const convertedUsers = [...userIds].filter((uid) => requesters.has(uid)).length;
  const withMessage = requests.filter((r) => (r.message || '').trim().length > 0).length;
  const dates = requests.map((r) => r.createdAt).filter(Boolean).sort((a, b) => a - b);

  const out = [];
  out.push(`# Métricas de uso — ${projectId}\n`);
  out.push(`- Generado: ${fmtDate(new Date())}`);
  out.push(`- Desde: ${args.since ? fmtDate(args.since) : 'el inicio (sin filtro)'}`);
  out.push(`- Cuentas excluidas: ${args.exclude.length ? args.exclude.join(', ') : 'ninguna'}`);
  if (dates.length) out.push(`- Solicitudes entre ${fmtDate(dates[0])} y ${fmtDate(dates[dates.length - 1])}`);
  out.push('');

  out.push('## Resumen\n');
  out.push(table(['Métrica', 'Valor'], [
    ['Usuarios registrados', users.length],
    ['Usuarios que enviaron ≥ 1 solicitud', convertedUsers],
    ['**Conversión a solicitud**', `**${pct(convertedUsers, users.length)}**`],
    ['Solicitudes totales', requests.length],
    ['Solicitudes por usuario que convirtió', requesters.size ? (requests.length / requesters.size).toFixed(2) : '—'],
    ['Solicitudes con mensaje', `${withMessage} (${pct(withMessage, requests.length)})`],
    ['Prestadores con ≥ 1 solicitud', `${new Set(requests.map((r) => r.providerId)).size} de ${providersSnap.size}`],
  ]));

  out.push('## Solicitudes por categoría\n');
  out.push(table(['Categoría', 'Solicitudes', '%'], countBy(requests, (r) => r.categoryId).map(([id, n]) => [
    categoryNames.get(id) || id, n, pct(n, requests.length),
  ])));

  out.push('## Solicitudes por prestador\n');
  out.push(table(['Prestador', 'Categoría', 'Solicitudes'], countBy(requests, (r) => r.providerId).map(([id, n]) => {
    const request = requests.find((r) => r.providerId === id);
    return [providerNames.get(id) || request.providerName || id, categoryNames.get(request.categoryId) || '', n];
  })));

  out.push('## Estado de las solicitudes\n');
  out.push(table(['Estado', 'Solicitudes'], countBy(requests, (r) => r.status || 'PENDING')));

  console.log(out.join('\n'));
}

main().catch((error) => {
  console.error('No se pudo generar el reporte:', error.message);
  process.exit(1);
});
