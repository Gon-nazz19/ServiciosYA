import http from 'k6/http';
import { check } from 'k6';

// VUS and HOLD allow longer runs against Azure; defaults keep the local lab protocol.
const vus = Number(__ENV.VUS || 24);

export const options = {
  stages: [
    { duration: '5s', target: vus },
    { duration: __ENV.HOLD || '25s', target: vus },
    { duration: '5s', target: 0 },
  ],
  thresholds: {
    http_req_failed: ['rate<0.05'],
  },
};

export default function () {
  const response = http.post(__ENV.TARGET_URL, JSON.stringify({
    providerId: 'prestador-local',
    message: 'Solicitud de laboratorio',
  }), { headers: { 'Content-Type': 'application/json' } });
  check(response, { 'respuesta 200': (r) => r.status === 200 });
}

export function handleSummary(data) {
  return {
    '/results/k6-summary.json': JSON.stringify(data, null, 2),
    stdout: `Solicitudes: ${data.metrics.http_reqs?.values.count || 0}; ` +
      `errores: ${data.metrics.http_req_failed?.values.rate || 0}; ` +
      `p95: ${data.metrics.http_req_duration?.values['p(95)'] || 0} ms\n`,
  };
}
