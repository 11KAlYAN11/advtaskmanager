# 🏗️ System Design — Advanced Task Manager

> **Stack:** Spring Boot 3 · React 19 · PostgreSQL · JWT · Docker · Kubernetes · Prometheus · Grafana

---

## 1. High-Level Architecture

```
                                ┌─────────────────────────────────────────────┐
                                │              Internet / Users                │
                                └──────────────────┬──────────────────────────┘
                                                   │ HTTPS
                                ┌──────────────────▼──────────────────────────┐
                                │        Ingress / Load Balancer               │
                                │   (Nginx Ingress / AWS ALB / Cloudflare)     │
                                │   • TLS termination (SSL/HTTPS)              │
                                │   • Rate limiting (100 req/s per IP)         │
                                │   • DDoS protection                          │
                                └───────────┬──────────────────┬───────────────┘
                                            │ /api/*           │ /*
                          ┌─────────────────▼───┐     ┌───────▼───────────────┐
                          │   Spring Boot Backend│     │  React Frontend       │
                          │   (2–10 pods / HPA) │     │  (Nginx / 2 pods)     │
                          │   Port 8080          │     │  Port 80              │
                          │   JWT Auth           │     │  SPA (index.html)     │
                          │   REST APIs          │     │  Vite build           │
                          └─────────┬───────────┘     └───────────────────────┘
                                    │
                 ┌──────────────────┼──────────────────┐
                 │                  │                   │
        ┌────────▼──────┐  ┌───────▼────────┐  ┌──────▼──────────┐
        │  PostgreSQL   │  │  Groq/OpenAI   │  │  Prometheus     │
        │  (StatefulSet)│  │  AI API        │  │  + Grafana      │
        │  Port 5432    │  │  (external)    │  │  Monitoring     │
        └───────────────┘  └────────────────┘  └─────────────────┘
```

---

## 2. Component Breakdown

### 2.1 Frontend (React 19 + TypeScript + Vite)

| Layer | Tech | Purpose |
|-------|------|---------|
| UI | React 19 | Component tree, virtual DOM |
| State | useState / Context | Auth context, task state |
| Routing | React Router | SPA navigation |
| HTTP | fetch / axios | API calls with JWT header |
| Drag & Drop | HTML5 DnD API | Kanban board |
| AI Chat | WebSocket/REST | Floating AI assistant |
| Build | Vite | Fast HMR dev, optimized prod build |
| Serve | Nginx | Static files + API proxy |

### 2.2 Backend (Spring Boot 3 + Java 17)

| Layer | Package | Purpose |
|-------|---------|---------|
| Controller | `controller/` | REST endpoints (`/api/tasks`, `/api/users`, `/api/auth`, `/api/ai`) |
| Service | `service/` | Business logic, orchestration |
| Repository | `repository/` | JPA/Hibernate data access |
| Entity | `entity/` | JPA entities (Task, User, Role) |
| Security | `security/` | JWT filter, UserDetails, BCrypt |
| Config | `config/` | CORS, SecurityConfig, DataInitializer |
| DTO | `dto/` | Request/Response transfer objects |

### 2.3 Database (PostgreSQL 16)

```sql
users table
  id BIGSERIAL PK
  name VARCHAR
  email VARCHAR UNIQUE
  password VARCHAR (BCrypt hashed)
  role ENUM (ADMIN, USER)

task table
  id BIGSERIAL PK
  title VARCHAR
  description TEXT
  status VARCHAR  -- TODO | IN_PROGRESS | REVIEW | DONE
  assigned_user_id FK → users.id
  created_at TIMESTAMP
  updated_at TIMESTAMP
```

---

## 3. Security Design

### 3.1 Authentication Flow

```
Client                    Backend                      DB
  │                           │                         │
  │── POST /api/auth/login ──►│                         │
  │   { email, password }     │── SELECT user by email ►│
  │                           │◄── User record ─────────│
  │                           │                         │
  │                           │ BCrypt.matches(pw, hash)│
  │                           │ → JWT(userId, role, exp)│
  │◄── { token: "eyJ..." } ───│                         │
  │                           │                         │
  │── GET /api/tasks ────────►│                         │
  │   Authorization: Bearer   │ JwtFilter validates     │
  │                           │ → SecurityContext set   │
  │◄── [ tasks array ] ───────│                         │
```

### 3.2 Authorization Matrix

| Endpoint | Public | USER | ADMIN |
|----------|--------|------|-------|
| `POST /api/auth/login` | ✅ | ✅ | ✅ |
| `GET /api/tasks` | ❌ | ✅ | ✅ |
| `POST /api/tasks` | ❌ | ✅ | ✅ |
| `PUT /api/tasks/{id}` | ❌ | ✅ | ✅ |
| `DELETE /api/tasks/{id}` | ❌ | ❌ | ✅ |
| `GET /api/users` | ❌ | ❌ | ✅ |
| `POST /api/users` | ❌ | ❌ | ✅ |
| `DELETE /api/users/{id}` | ❌ | ❌ | ✅ |
| `GET /actuator/health` | ✅ | ✅ | ✅ |
| `GET /actuator/prometheus` | ❌ | ❌ | ✅ |

### 3.3 Security Headers

| Header | Value | Protection |
|--------|-------|-----------|
| `X-Frame-Options` | `DENY` | Clickjacking |
| `X-Content-Type-Options` | `nosniff` | MIME sniffing |
| `Content-Security-Policy` | `default-src 'self'` | XSS |
| `Strict-Transport-Security` | `max-age=31536000` | SSL stripping (HTTPS only) |
| `Referrer-Policy` | `strict-origin-when-cross-origin` | Data leakage |
| `Permissions-Policy` | `camera=(), microphone=()` | Browser APIs |

### 3.4 CORS Policy

- **Allowed Origins:** `http://localhost:*` (dev), your domain (prod)
- **Allowed Methods:** GET, POST, PUT, DELETE, PATCH, OPTIONS
- **Credentials:** `true` (cookies/auth headers allowed)
- **Max Age:** 3600s (preflight cached)

---

## 4. Scaling Strategy

### 4.1 Horizontal Scaling (Scale Out)

```
                    Load Balancer
                         │
        ┌────────────────┼────────────────┐
        │                │                │
   Backend Pod 1    Backend Pod 2    Backend Pod 3
   (Spring Boot)    (Spring Boot)    (Spring Boot)
        │                │                │
        └────────────────┼────────────────┘
                         │
                   PostgreSQL (single)
```

**HPA (Horizontal Pod Autoscaler):**
- Min replicas: 2 (always HA)
- Max replicas: 10
- Scale out trigger: CPU > 70% OR Memory > 80%
- Scale in cooldown: 300s (avoid flapping)

**Why stateless backend?**
JWT tokens are self-contained — no sticky sessions needed. Any pod can serve any request.

### 4.2 Vertical Scaling (Scale Up)

Increase CPU/Memory limits in K8s manifests:
```yaml
resources:
  limits:
    memory: "2Gi"   # was 1Gi
    cpu: "2000m"    # was 1000m
```

### 4.3 Database Scaling

| Strategy | When to use |
|----------|------------|
| **Connection Pooling** (HikariCP) | Always — we already have this |
| **Read Replicas** | >80% read traffic (reports, dashboards) |
| **PgBouncer** (connection pooler) | >200 concurrent connections |
| **Partitioning** | >10M rows in task table |
| **Caching (Redis)** | Same query hit rate >80% |

---

## 5. DevOps Pipeline

```
Developer
    │ git push main
    ▼
GitHub Actions CI/CD
    │
    ├─ 1. Backend Tests (JUnit + H2)
    ├─ 2. Frontend Lint + Build
    │
    ├─ 3. Docker Build (multi-stage)
    │      └── Push to GitHub Container Registry (ghcr.io)
    │
    └─ 4. Deploy to Kubernetes (with manual approval)
           ├── kubectl apply -f k8s/
           └── kubectl rollout status deployment/backend
```

### GitOps Flow

```
Code Repo (GitHub)
       │ push
       ▼
GitHub Actions
       │ docker build + push
       ▼
Container Registry (ghcr.io / ECR / GCR)
       │ image tag: git sha
       ▼
K8s Deployment (RollingUpdate)
       │ maxSurge=1, maxUnavailable=0
       ▼
Zero-downtime deployment ✅
```

---

## 6. Monitoring Stack

```
Spring Boot App
    │ /actuator/prometheus (Micrometer)
    ▼
Prometheus (scrapes every 15s)
    │
    ▼
Grafana (dashboards + alerts)
    │
    ├── Dashboard: JVM metrics (heap, GC, threads)
    ├── Dashboard: HTTP metrics (req/s, latency, errors)
    ├── Dashboard: DB metrics (connections, query time)
    └── Alert: Error rate > 1% → Slack/PagerDuty
```

### Key Metrics to Watch

| Metric | Normal | Alert threshold |
|--------|--------|-----------------|
| `http_server_requests_seconds_count` | Growing | — |
| `http_server_requests_seconds_max` | < 500ms | > 2000ms |
| `jvm_memory_used_bytes` | < 700MB | > 900MB |
| `hikaricp_connections_active` | < 8 | > 18 |
| `process_cpu_usage` | < 0.5 | > 0.8 |
| HTTP error rate | < 0.5% | > 1% |

### Grafana Dashboard Import

1. Open Grafana → `http://localhost:3000` (admin/admin123)
2. Dashboards → Import
3. Use these community IDs:
   - **JVM Micrometer:** `4701`
   - **Spring Boot:** `12900`
   - **PostgreSQL:** `9628`
   - **Node Exporter:** `1860`

---

## 7. Load Testing Strategy

```
k6 load-tests/k6-load-test.js

Test Types:
┌─────────────┬──────────────┬────────────────┬──────────────────────────┐
│ Type        │ VUs          │ Duration       │ Goal                     │
├─────────────┼──────────────┼────────────────┼──────────────────────────┤
│ Smoke       │ 1            │ 30s            │ Basic sanity check       │
│ Load        │ 50           │ 5m             │ Normal traffic           │
│ Stress      │ 200          │ 10m            │ Find breaking point      │
│ Spike       │ 0 → 100 → 0  │ ~2m            │ Sudden burst behavior    │
└─────────────┴──────────────┴────────────────┴──────────────────────────┘

SLA Targets:
  p95 response time < 500ms
  p99 response time < 1000ms
  Error rate < 1%
```

---

## 8. Production Deployment Order

This is the exact order you should follow:

```
Phase 1: Containerize (Now)
  ✅ Dockerfile (backend) — multi-stage
  ✅ Dockerfile (frontend) — Nginx
  ✅ docker-compose.yml — full stack
  → Run: docker compose up -d

Phase 2: Monitoring (Before going live)
  ✅ Spring Actuator + Micrometer
  ✅ Prometheus config
  ✅ Grafana dashboards
  → Run: docker compose -f docker-compose.yml -f docker-compose.monitoring.yml up -d

Phase 3: CI/CD (Before team collaboration)
  ✅ GitHub Actions pipeline
  → Push to main → automatic test + build + push

Phase 4: Kubernetes (Cloud deployment)
  ✅ K8s manifests (namespace, configmap, secret, postgres, backend, frontend, ingress, HPA)
  → kubectl apply -f k8s/

Phase 5: Security Hardening (Before public launch)
  ✅ Security headers (XSS, HSTS, CSP, Clickjacking)
  ✅ CORS locked to your domain
  ✅ Actuator restricted to ADMIN
  → Add WAF (Cloudflare / AWS WAF)

Phase 6: Load Testing (Before launch)
  ✅ k6 smoke/load/stress/spike tests
  → k6 run load-tests/k6-load-test.js

Phase 7: Advanced (Post-launch)
  □ Redis caching (session, hot queries)
  □ CDN (CloudFront / Cloudflare) for frontend
  □ Flyway DB migrations
  □ Sealed Secrets (K8s)
  □ Service Mesh (Istio) for mTLS
  □ Log aggregation (ELK / Loki + Grafana)
```

---

## 9. CORS, XSS & CSRF Explained

### CORS (Cross-Origin Resource Sharing)
**Problem:** Browser blocks JS on `http://frontend.com` from calling `http://api.com`
**Solution:** Backend sends `Access-Control-Allow-Origin` header to allow trusted origins

```
Browser: "Hey backend, can I talk to you from localhost:5173?"
Backend: "Yes, I allow localhost:*" → Browser allows the request
```

### XSS (Cross-Site Scripting)
**Problem:** Attacker injects `<script>document.cookie</script>` into your page
**Solution:** Content-Security-Policy header restricts what scripts can run

```
CSP: "default-src 'self'" → Only YOUR scripts run, not injected ones
```

### CSRF (Cross-Site Request Forgery)
**Problem:** Malicious site tricks user's browser into making authenticated requests
**Solution:** JWT in Authorization header (not cookies) — CSRF is not possible because:
- Malicious site can't set the `Authorization` header (browser restricts it)
- `csrf().disable()` is safe with JWT + stateless sessions

### Clickjacking
**Problem:** Attacker puts your site in a hidden `<iframe>` and tricks users into clicking
**Solution:** `X-Frame-Options: DENY` → browser refuses to render your site in an iframe

---

## 10. Quick Commands Reference

```bash
# ── Local Development ────────────────────────────────────────────────────────
# Backend
./mvnw spring-boot:run

# Frontend
cd frontend && npm run dev

# ── Docker ───────────────────────────────────────────────────────────────────
# Start full stack
docker compose up -d

# Start with monitoring
docker compose -f docker-compose.yml -f docker-compose.monitoring.yml up -d

# View logs
docker compose logs -f backend
docker compose logs -f frontend

# Rebuild after code change
docker compose up -d --build backend

# Stop everything
docker compose down -v

# ── Kubernetes ───────────────────────────────────────────────────────────────
# Deploy all
kubectl apply -f k8s/ --namespace=taskmanager

# Check pod status
kubectl get pods -n taskmanager

# View backend logs
kubectl logs -f deployment/backend -n taskmanager

# Scale manually
kubectl scale deployment/backend --replicas=5 -n taskmanager

# Check HPA
kubectl get hpa -n taskmanager

# ── Load Testing ─────────────────────────────────────────────────────────────
# Smoke test
k6 run load-tests/k6-load-test.js

# Load test
k6 run --vus 50 --duration 5m load-tests/k6-load-test.js

# Stress test
k6 run --env SCENARIO=stress load-tests/k6-load-test.js

# ── Monitoring ───────────────────────────────────────────────────────────────
# Prometheus
open http://localhost:9090

# Grafana (admin/admin123)
open http://localhost:3000

# Spring Actuator health
curl http://localhost:8080/actuator/health

# Prometheus metrics
curl http://localhost:8080/actuator/prometheus
```

