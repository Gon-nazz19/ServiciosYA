#!/usr/bin/env node
/**
 * Sends the ServiciosYa push campaigns with Firebase Cloud Messaging (HTTP v1, through the Admin SDK).
 *
 * Campaigns are segmented by topic: the app subscribes each device to "novedades" and to
 * "categoria_<id>" for the categories the user explored, so no token is stored anywhere.
 * Sending to topics works on the free Spark plan; no Cloud Functions are needed.
 *
 * Usage:
 *   node send.js --list                     List the campaigns of campaigns.json.
 *   node send.js --dry-run --all            Validate every campaign without connecting to Firebase.
 *   node send.js --campaign <id>            Send one campaign (needs GOOGLE_APPLICATION_CREDENTIALS).
 *   node send.js --campaign <id> --validate Ask FCM to validate the message without delivering it.
 */
const path = require('path');
const { campaigns } = require(path.join(__dirname, 'campaigns.json'));

// Same rules the app uses (NotificationRoute and Topics).
const TOPIC = /^[a-zA-Z0-9_.~%-]+$/;
const ROUTE = /^(requests|(category|provider)\/[A-Za-z0-9_-]+)$/;

function validate(campaign) {
  const errors = [];
  if (!campaign.id || !/^[a-z0-9-]+$/.test(campaign.id)) errors.push('id inválido (usar minúsculas, números y guiones)');
  if (!campaign.topic || !TOPIC.test(campaign.topic)) errors.push(`topic inválido: ${campaign.topic}`);
  if (!campaign.title || campaign.title.length > 65) errors.push('título vacío o de más de 65 caracteres');
  if (!campaign.body || campaign.body.length > 240) errors.push('texto vacío o de más de 240 caracteres');
  if (campaign.route && !ROUTE.test(campaign.route)) errors.push(`route inválida: ${campaign.route}`);
  return errors;
}

function toMessage(campaign) {
  return {
    topic: campaign.topic,
    notification: { title: campaign.title, body: campaign.body },
    // The app reads these data fields to open the screen and to measure notification_open.
    data: { route: campaign.route || '', campaign_id: campaign.id, type: 'campaign' },
    android: {
      priority: 'high',
      notification: { channelId: 'novedades', icon: 'ic_stat_notification' },
    },
  };
}

function argument(name) {
  const index = process.argv.indexOf(name);
  return index === -1 ? undefined : process.argv[index + 1];
}

async function main() {
  const has = (flag) => process.argv.includes(flag);

  if (has('--list')) {
    for (const c of campaigns) console.log(`${c.id.padEnd(26)} → ${c.topic.padEnd(26)} ${c.title}`);
    return;
  }

  const id = argument('--campaign');
  const selected = has('--all') ? campaigns : campaigns.filter((c) => c.id === id);
  if (selected.length === 0) {
    console.error(id ? `No existe la campaña "${id}". Usá --list para ver las disponibles.` : 'Indicá --campaign <id> o --all.');
    process.exit(1);
  }

  let invalid = false;
  for (const campaign of selected) {
    const errors = validate(campaign);
    if (errors.length > 0) {
      invalid = true;
      console.error(`Campaña ${campaign.id || '(sin id)'}:\n- ${errors.join('\n- ')}`);
    }
  }
  if (invalid) process.exit(1);

  if (has('--dry-run')) {
    console.log(`${selected.length} campaña(s) válida(s). Modo --dry-run: no se envió nada.`);
    return;
  }
  if (selected.length > 1) {
    console.error('Por seguridad se envía una campaña por vez: usá --campaign <id>.');
    process.exit(1);
  }
  if (!process.env.GOOGLE_APPLICATION_CREDENTIALS) {
    console.error('Falta GOOGLE_APPLICATION_CREDENTIALS. Apuntala al JSON de la cuenta de servicio (ver README.md).');
    process.exit(1);
  }

  // Required lazily so --list and --dry-run work without installing dependencies.
  const { initializeApp, applicationDefault } = require('firebase-admin/app');
  const { getMessaging } = require('firebase-admin/messaging');
  const messaging = getMessaging(initializeApp({ credential: applicationDefault() }));

  const campaign = selected[0];
  const validateOnly = has('--validate');
  const messageId = await messaging.send(toMessage(campaign), validateOnly);
  console.log(`${validateOnly ? 'Validada' : 'Enviada'} "${campaign.id}" al topic ${campaign.topic}: ${messageId}`);
}

main().catch((error) => {
  console.error('No se pudo enviar la campaña:', error.message);
  process.exit(1);
});
