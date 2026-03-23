/**
 * ════════════════════════════════════════════════════════════════════════════
 *  k6 Load Test — Advanced Task Manager
 *
 *  Install k6:  https://k6.io/docs/getting-started/installation/
 *  Run:
 *    # Smoke test (1 user, 30s)
 *    k6 run load-tests/k6-load-test.js
 *
 *    # Load test (50 users, 5 min)
 *    k6 run --vus 50 --duration 5m load-tests/k6-load-test.js
 *
 *    # Stress test (ramp up to 200 users)
 *    k6 run --env SCENARIO=stress load-tests/k6-load-test.js
 *
 *    # With Prometheus output (needs k6 Prometheus remote write)
 *    k6 run --out experimental-prometheus-rw load-tests/k6-load-test.js
 * ════════════════════════════════════════════════════════════════════════════
 */

import http from 'k6/http';
import { check, sleep, group } from 'k6';
import { Counter, Rate, Trend } from 'k6/metrics';

// ── Custom metrics ────────────────────────────────────────────────────────────
const loginErrors  = new Counter('login_errors');
const taskErrors   = new Counter('task_errors');
const errorRate    = new Rate('error_rate');
const loginTrend   = new Trend('login_duration_ms');
const taskGetTrend = new Trend('task_list_duration_ms');

// ── Config ────────────────────────────────────────────────────────────────────
const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const ADMIN_EMAIL = __ENV.ADMIN_EMAIL || 'admin@gmail.com';
const ADMIN_PASS  = __ENV.ADMIN_PASS  || 'admin123';

// ── Scenarios ─────────────────────────────────────────────────────────────────
export const options = {
  scenarios: {

    // ── SMOKE TEST — verify system works under minimal load
    smoke: {
      executor: 'constant-vus',
      vus: 1,
      duration: '30s',
      tags: { scenario: 'smoke' },
    },

    // ── LOAD TEST — expected production traffic
    load: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '1m',  target: 20  },   // ramp up
        { duration: '3m',  target: 50  },   // stay at 50 users
        { duration: '1m',  target: 0   },   // ramp down
      ],
      tags: { scenario: 'load' },
    },

    // ── STRESS TEST — find breaking point
    stress: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '2m',  target: 50  },
        { duration: '5m',  target: 100 },
        { duration: '2m',  target: 200 },
        { duration: '5m',  target: 200 },   // hold peak load
        { duration: '2m',  target: 0   },   // ramp down
      ],
      tags: { scenario: 'stress' },
    },

    // ── SPIKE TEST — sudden burst
    spike: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '10s', target: 100 },   // instant spike
        { duration: '1m',  target: 100 },
        { duration: '10s', target: 0   },
      ],
      tags: { scenario: 'spike' },
    },
  },

  // Only run 'smoke' by default — override with --env SCENARIO=load
  ...((__ENV.SCENARIO || 'smoke') !== 'smoke'
    ? { scenarios: { [__ENV.SCENARIO || 'smoke']: options?.scenarios?.[__ENV.SCENARIO || 'smoke'] } }
    : {}),

  // ── Thresholds (SLA) ─────────────────────────────────────────────────────
  thresholds: {
    http_req_duration:      ['p(95)<500', 'p(99)<1000'],  // 95% under 500ms
    http_req_failed:        ['rate<0.01'],                 // <1% errors
    error_rate:             ['rate<0.01'],
    login_duration_ms:      ['p(95)<1000'],
    task_list_duration_ms:  ['p(95)<300'],
  },
};

// ── Helpers ───────────────────────────────────────────────────────────────────
function jsonHeaders(token) {
  const headers = { 'Content-Type': 'application/json' };
  if (token) headers['Authorization'] = `Bearer ${token}`;
  return { headers };
}

// ── Main virtual user flow ────────────────────────────────────────────────────
export default function () {
  let token = null;

  // ── 1. Login ──────────────────────────────────────────────────────────────
  group('Auth — Login', () => {
    const start = Date.now();
    const res = http.post(
      `${BASE_URL}/api/auth/login`,
      JSON.stringify({ email: ADMIN_EMAIL, password: ADMIN_PASS }),
      jsonHeaders()
    );
    loginTrend.add(Date.now() - start);

    const ok = check(res, {
      'login status 200':    (r) => r.status === 200,
      'login has token':     (r) => r.json('token') !== undefined,
    });

    if (!ok) {
      loginErrors.add(1);
      errorRate.add(1);
      return;  // no point continuing without token
    }

    errorRate.add(0);
    token = res.json('token');
  });

  if (!token) return;
  sleep(0.5);

  // ── 2. List Tasks ─────────────────────────────────────────────────────────
  group('Tasks — Get All', () => {
    const start = Date.now();
    const res = http.get(`${BASE_URL}/api/tasks`, jsonHeaders(token));
    taskGetTrend.add(Date.now() - start);

    const ok = check(res, {
      'tasks status 200':    (r) => r.status === 200,
      'tasks is array':      (r) => Array.isArray(r.json()),
    });
    if (!ok) taskErrors.add(1);
    errorRate.add(!ok ? 1 : 0);
  });

  sleep(1);

  // ── 3. Create a Task ──────────────────────────────────────────────────────
  group('Tasks — Create', () => {
    const payload = {
      title: `Load Test Task ${Date.now()}`,
      description: 'Created by k6 load test',
      status: 'TODO',
    };
    const res = http.post(
      `${BASE_URL}/api/tasks`,
      JSON.stringify(payload),
      jsonHeaders(token)
    );

    const ok = check(res, {
      'create task 200/201': (r) => r.status === 200 || r.status === 201,
    });
    if (!ok) taskErrors.add(1);
    errorRate.add(!ok ? 1 : 0);
  });

  sleep(1);

  // ── 4. Actuator Health ────────────────────────────────────────────────────
  group('Actuator — Health', () => {
    const res = http.get(`${BASE_URL}/actuator/health`);
    check(res, {
      'health is UP': (r) => r.json('status') === 'UP',
    });
  });

  sleep(Math.random() * 2 + 0.5);  // think time: 0.5–2.5s
}

// ── Summary report ────────────────────────────────────────────────────────────
export function handleSummary(data) {
  return {
    'load-tests/results/summary.json': JSON.stringify(data, null, 2),
    stdout: textSummary(data, { indent: ' ', enableColors: true }),
  };
}

function textSummary(data, _opts) {
  const m = data.metrics;
  return `
╔══════════════════════════════════════════════════════╗
║           k6 Load Test — Task Manager                ║
╠══════════════════════════════════════════════════════╣
║  Requests       : ${String(m.http_reqs?.values?.count || 0).padEnd(32)}║
║  Failed         : ${String(m.http_req_failed?.values?.rate?.toFixed(4) || 0).padEnd(32)}║
║  Avg Duration   : ${String((m.http_req_duration?.values?.avg || 0).toFixed(2) + 'ms').padEnd(32)}║
║  p95 Duration   : ${String((m.http_req_duration?.values?.['p(95)'] || 0).toFixed(2) + 'ms').padEnd(32)}║
║  p99 Duration   : ${String((m.http_req_duration?.values?.['p(99)'] || 0).toFixed(2) + 'ms').padEnd(32)}║
╚══════════════════════════════════════════════════════╝
`;
}

