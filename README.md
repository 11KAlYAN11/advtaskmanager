# Advanced Task Manager

> Spring Boot 3 · React 19 · PostgreSQL · JWT · AI Agent · Docker · Kubernetes · Prometheus · Grafana

---

## 🗂️ Documentation Index

| Doc | Purpose |
|-----|---------|
| 📋 **[DEPLOYMENT_GUIDE.md](DEPLOYMENT_GUIDE.md)** | **← START HERE for all config changes** — Local / Docker / K8s / Cloud |
| 📐 [SYSTEM_DESIGN.md](SYSTEM_DESIGN.md) | Architecture, scaling, security design |
| 📖 [API_DOCUMENTATION.md](API_DOCUMENTATION.md) | REST API endpoints reference |
| 📮 [POSTMAN_GUIDE.md](POSTMAN_GUIDE.md) | Testing APIs via Postman |
| 🚀 [QUICKSTART.md](QUICKSTART.md) | 5-minute local setup |

---

## ✅ RBAC Summary
| Feature | ADMIN | USER |
|---|---|---|
| 📋 Task Board (Kanban drag & drop) | ✅ | ✅ |
| ➕ Create tasks | ✅ | ✅ |
| 🔄 Assign tasks | ✅ | ✅ |
| 🔀 Move status (drag & drop) | ✅ | ✅ |
| 🗑️ Delete tasks | ✅ | ❌ |
| 👥 Users tab | ✅ | ❌ |
| ➕ Create users | ✅ | ❌ |
| 🗑️ Delete users | ✅ | ❌ |

---

## ✅ AI Agent Integration
- 🤖 Groq `llama-3.3-70b-versatile` (OpenAI-compatible, free tier)
- CRUD via natural language ("Create a task called Fix Login Bug")
- Context-aware (AI sees current tasks + users + your role)
- Role-enforced (USER can't delete via AI either)
- Floating chat widget — bottom-right corner

### To activate AI:
1. Get free key from https://console.groq.com
2. Open `src/main/resources/application.properties`
3. Set: `openai.api.key=gsk_your-key`
4. Restart backend

---

## ✅ Security
- JWT authentication (stateless, no sessions)
- BCrypt password hashing
- Role-based authorization (ADMIN / USER)
- Security headers: XSS (CSP), Clickjacking (X-Frame-Options), HSTS, MIME sniffing
- CORS locked to localhost (dev) / your domain (prod)
- Actuator endpoints restricted to ADMIN

---

## ✅ DevOps & Production Readiness
| Component | Status |
|---|---|
| 🐳 Docker (backend multi-stage) | ✅ `Dockerfile` |
| 🐳 Docker (frontend Nginx) | ✅ `frontend/Dockerfile` |
| 🐳 docker-compose (full stack) | ✅ `docker-compose.yml` |
| 📊 Prometheus + Grafana | ✅ `docker-compose.monitoring.yml` |
| ☸️ Kubernetes manifests | ✅ `k8s/` (namespace, configmap, secret, postgres, backend, frontend, ingress, HPA) |
| 🔄 GitHub Actions CI/CD | ✅ `.github/workflows/ci-cd.yml` |
| 🔥 k6 Load Tests (smoke/load/stress/spike) | ✅ `load-tests/k6-load-test.js` |
| 📐 System Design doc | ✅ `SYSTEM_DESIGN.md` |
| 🏥 Spring Actuator health + Prometheus metrics | ✅ `/actuator/health`, `/actuator/prometheus` |

---

## Quick Start

### Local dev
```bash
# Backend
./mvnw spring-boot:run

# Frontend
cd frontend && npm run dev
```

### Docker (full stack)
```bash
cp .env.example .env        # fill in your secrets
docker compose up -d
# App:  http://localhost:80
# API:  http://localhost:8080
```

### Docker + Monitoring
```bash
docker compose -f docker-compose.yml -f docker-compose.monitoring.yml up -d
# Prometheus: http://localhost:9090
# Grafana:    http://localhost:3000  (admin / admin123)
```

### Load Testing
```bash
# Install k6: https://k6.io/docs/getting-started/installation/
k6 run load-tests/k6-load-test.js                          # smoke (1 user, 30s)
k6 run --vus 50 --duration 5m load-tests/k6-load-test.js   # load
k6 run --env SCENARIO=stress load-tests/k6-load-test.js    # stress
```

### Kubernetes
```bash
kubectl apply -f k8s/
kubectl get pods -n taskmanager
```

---

## Default Credentials
| Role | Email | Password |
|------|-------|----------|
| Admin | admin@gmail.com | admin123 |

---

## Docs
- 🚀 **[Deployment Guide](DEPLOYMENT_GUIDE.md)** — **Every config/secret you need to change for Docker, K8s, Cloud**
- 📐 [System Design](SYSTEM_DESIGN.md) — Architecture, scaling, security, DevOps pipeline
- 📖 [API Documentation](API_DOCUMENTATION.md)
- 📮 [Postman Guide](POSTMAN_GUIDE.md)
- 🚀 [Quick Start](QUICKSTART.md)

---

## 🗺️ Roadmap
- [ ] Jira-style task detail modal
- [ ] "Reported by" / "Created by" on tasks
- [ ] Redis caching layer
- [ ] Flyway DB migrations
- [ ] CDN for frontend static assets
- [ ] Sealed Secrets for K8s
- [ ] ELK / Loki log aggregation
- [ ] MCP server for AI agents
