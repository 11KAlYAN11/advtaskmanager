# 🚀 Deployment Guide — Advanced Task Manager
> **Your single source of truth** for every configuration change needed across Local → Docker → Kubernetes → Cloud

---

## 📋 Quick Navigation

| Goal | Jump to |
|------|---------|
| Run locally on my machine | [§1 Local Development](#1-local-development) |
| Run with Docker (no K8s) | [§2 Docker](#2-docker) |
| Add Prometheus + Grafana monitoring | [§3 Docker + Monitoring](#3-docker--monitoring) |
| Deploy to Kubernetes | [§4 Kubernetes](#4-kubernetes) |
| Set up CI/CD with GitHub Actions | [§5 CICD](#5-cicd--github-actions) |
| Deploy to AWS EKS | [§6 Cloud](#6-cloud-deployment) |
| What are all the secrets/tokens I need? | [§0 Secrets Checklist](#0-secrets--tokens-checklist) |

---

## 0. Secrets & Tokens Checklist

> **Gather these BEFORE you start any deployment.** Everything else is just copying them into the right place.

| Secret | Where to get it | Used in |
|--------|----------------|---------|
| `DB_PASSWORD` | You choose (e.g. `taskmanager123`) | DB, Docker, K8s |
| `JWT_SECRET` | Generate (see below) | Backend, Docker, K8s |
| `OPENAI_API_KEY` | https://console.groq.com (free) or https://platform.openai.com | Backend, Docker, K8s |
| `DOCKER_USERNAME` | Your Docker Hub username | GitHub Actions |
| `DOCKER_PASSWORD` | Your Docker Hub password/token | GitHub Actions |
| Your domain | e.g. `taskmanager.yourdomain.com` | K8s Ingress, TLS |

### Generate a strong JWT secret
```bash
# Linux / Mac / WSL:
node -e "console.log(require('crypto').randomBytes(32).toString('hex'))"

# Or just use this (change it in prod):
# a8f3c2d1e7b4a9f0c3d6e2b8a1f4c7d0e3b6a9f2c5d8e1b4a7f0c3d6e9b2a5f8
```

---

## 1. Local Development

### Files to change

#### `src/main/resources/application.properties`

```properties
# ── YOUR CHANGES ──────────────────────────────────────────
spring.datasource.url=jdbc:postgresql://localhost:5432/YOUR_DB_NAME
#                                                     ↑ e.g. Employee (current) or taskmanager
spring.datasource.username=YOUR_POSTGRES_USERNAME
#                          ↑ e.g. postgres
spring.datasource.password=YOUR_POSTGRES_PASSWORD
#                          ↑ e.g. root (whatever you set when installing PostgreSQL)

openai.api.key=gsk_YOUR_GROQ_KEY_HERE
#              ↑ Get free key from https://console.groq.com
# ─────────────────────────────────────────────────────────
```

> ✅ Everything else in `application.properties` is already correct for local dev.

### Run it

```bash
# Terminal 1 – Backend
./mvnw spring-boot:run       # Windows: mvnw.cmd spring-boot:run

# Terminal 2 – Frontend
cd frontend
npm install                  # first time only
npm run dev
```

| Service | URL |
|---------|-----|
| Frontend | http://localhost:5173 |
| Backend API | http://localhost:8080 |
| Health check | http://localhost:8080/actuator/health |

### Default login
```
Email:    admin@gmail.com
Password: admin123
```

---

## 2. Docker

### Step 1 — Create your `.env` file

```bash
# In the project root:
cp .env.example .env
```

Now open `.env` and fill in **every value**:

```dotenv
# .env  ← NEVER commit this file to git!

# ── Database ─────────────────────────────────────────────────
DB_PASSWORD=taskmanager123          # ← choose any password

# ── JWT Secret ───────────────────────────────────────────────
JWT_SECRET=REPLACE_WITH_STRONG_256BIT_RANDOM_HEX
#          ↑ generate with: node -e "console.log(require('crypto').randomBytes(32).toString('hex'))"

# ── AI (Groq — free) ──────────────────────────────────────────
OPENAI_API_KEY=gsk_your_groq_key_here
#              ↑ from https://console.groq.com
AI_BASE_URL=https://api.groq.com/openai/v1/chat/completions
AI_MODEL=llama-3.3-70b-versatile
```

> That's it. Docker Compose reads `.env` automatically — **no other file changes needed**.

### Step 2 — Build & Run

```bash
docker compose up -d --build
```

### Step 3 — Verify

```bash
docker compose ps           # all containers should be "healthy"
docker compose logs -f backend    # watch backend startup
```

| Service | URL |
|---------|-----|
| Frontend | http://localhost:80 |
| Backend API | http://localhost:8080 |
| Health check | http://localhost:8080/actuator/health |

### Useful Docker commands

```bash
# Stop everything (keeps data)
docker compose down

# Stop and DELETE all data (fresh start)
docker compose down -v

# Rebuild only backend after code change
docker compose up -d --build backend

# See logs
docker compose logs -f          # all
docker compose logs -f backend  # backend only
docker compose logs -f frontend # frontend only
```

---

## 3. Docker + Monitoring

> Runs Prometheus + Grafana on top of the normal Docker stack.

```bash
docker compose -f docker-compose.yml -f docker-compose.monitoring.yml up -d
```

| Service | URL | Login |
|---------|-----|-------|
| Prometheus | http://localhost:9090 | — |
| Grafana | http://localhost:3000 | admin / admin123 |

### Import Grafana Dashboards

1. Open Grafana → http://localhost:3000 (admin / admin123)
2. Click **Dashboards → Import**
3. Enter these community dashboard IDs one by one:

| Dashboard | ID |
|-----------|-----|
| JVM (Micrometer) | `4701` |
| Spring Boot | `12900` |
| PostgreSQL | `9628` |
| Node Exporter (Host) | `1860` |

---

## 4. Kubernetes

### Prerequisites

```bash
# Install kubectl
# Windows: winget install Kubernetes.kubectl
# Mac:     brew install kubectl

# Install a local K8s cluster (pick one):
# Option A — Docker Desktop (enable in Settings → Kubernetes)
# Option B — Minikube: winget install minikube
# Option C — kind:     winget install kubernetes-sigs.kind

# Verify
kubectl version --client
kubectl get nodes
```

---

### Step 1 — Push Docker images to a registry

> K8s pulls images from a registry (Docker Hub, GitHub Container Registry, AWS ECR, etc.)
> **You cannot use local images directly** (except with minikube tricks).

```bash
# ── Docker Hub example ────────────────────────────────────
docker login

# Build and tag
docker build -t YOUR_DOCKERHUB_USERNAME/taskmanager-backend:latest .
docker build -t YOUR_DOCKERHUB_USERNAME/taskmanager-frontend:latest ./frontend

# Push
docker push YOUR_DOCKERHUB_USERNAME/taskmanager-backend:latest
docker push YOUR_DOCKERHUB_USERNAME/taskmanager-frontend:latest
```

---

### Step 2 — Edit K8s manifests

#### `k8s/04-backend.yaml` — line 30

```yaml
# CHANGE THIS LINE:
image: YOUR_REGISTRY/taskmanager-backend:latest

# TO:
image: YOUR_DOCKERHUB_USERNAME/taskmanager-backend:latest
# example:
image: kalyan123/taskmanager-backend:latest
```

#### `k8s/05-frontend.yaml` — line 24

```yaml
# CHANGE THIS LINE:
image: YOUR_REGISTRY/taskmanager-frontend:latest

# TO:
image: YOUR_DOCKERHUB_USERNAME/taskmanager-frontend:latest
# example:
image: kalyan123/taskmanager-frontend:latest
```

#### `k8s/06-ingress.yaml` — line 29

```yaml
# CHANGE THIS LINE:
- host: taskmanager.yourdomain.com

# TO your actual domain:
- host: taskmanager.kalyan.com
```

---

### Step 3 — Fill in Kubernetes Secrets

> K8s secrets must be **base64 encoded**. Use these commands:

```bash
# Windows PowerShell:
[Convert]::ToBase64String([System.Text.Encoding]::UTF8.GetBytes("your-value-here"))

# Linux/Mac/WSL:
echo -n "your-value-here" | base64
```

#### `k8s/02-secrets.yaml` — fill ALL 3 values

```yaml
data:
  # DB_PASSWORD — encode your chosen password
  # e.g. echo -n "taskmanager123" | base64  →  dGFza21hbmFnZXIxMjM=
  DB_PASSWORD: dGFza21hbmFnZXIxMjM=          # ← replace with YOUR base64 encoded password

  # JWT_SECRET — encode your 256-bit hex secret
  # e.g. echo -n "a8f3c2d1..." | base64
  JWT_SECRET: YOUR_BASE64_ENCODED_JWT_SECRET   # ← replace

  # OPENAI_API_KEY — encode your Groq key
  # e.g. echo -n "gsk_abc123..." | base64
  OPENAI_API_KEY: YOUR_BASE64_ENCODED_GROQ_KEY # ← replace
```

**Quick base64 for Windows PowerShell:**
```powershell
# DB password
[Convert]::ToBase64String([System.Text.Encoding]::UTF8.GetBytes("taskmanager123"))

# JWT secret
[Convert]::ToBase64String([System.Text.Encoding]::UTF8.GetBytes("your-jwt-secret-here"))

# Groq API key
[Convert]::ToBase64String([System.Text.Encoding]::UTF8.GetBytes("gsk_your_groq_key"))
```

---

### Step 4 — Deploy to Kubernetes

```bash
# Apply all manifests in order
kubectl apply -f k8s/

# Watch pods come up
kubectl get pods -n taskmanager -w

# Check logs
kubectl logs -f deployment/backend -n taskmanager
kubectl logs -f deployment/frontend -n taskmanager

# Check services
kubectl get services -n taskmanager

# Get ingress
kubectl get ingress -n taskmanager
```

### Step 5 — Expose locally (if using Minikube / Docker Desktop)

```bash
# Option A — Port forward (quick test, no ingress needed)
kubectl port-forward service/backend-service 8080:8080 -n taskmanager
kubectl port-forward service/frontend-service 3000:80 -n taskmanager

# Option B — Minikube tunnel (enables LoadBalancer + Ingress)
minikube tunnel
```

---

### Step 6 — Set up Nginx Ingress Controller (for real traffic)

```bash
# Install Nginx Ingress Controller
kubectl apply -f https://raw.githubusercontent.com/kubernetes/ingress-nginx/controller-v1.10.1/deploy/static/provider/cloud/deploy.yaml

# Wait for it
kubectl wait --namespace ingress-nginx \
  --for=condition=ready pod \
  --selector=app.kubernetes.io/component=controller \
  --timeout=120s
```

---

### Kubernetes Cheat Sheet

```bash
# Scale manually
kubectl scale deployment/backend --replicas=5 -n taskmanager

# Check HPA (auto-scaler)
kubectl get hpa -n taskmanager

# Describe a failing pod
kubectl describe pod <pod-name> -n taskmanager

# Delete and redeploy
kubectl rollout restart deployment/backend -n taskmanager

# Check rollout status
kubectl rollout status deployment/backend -n taskmanager

# Delete everything (clean slate)
kubectl delete namespace taskmanager
kubectl apply -f k8s/
```

---

## 5. CI/CD — GitHub Actions

> Automates: test → build → push Docker image → deploy to K8s on every `git push main`

### Step 1 — Add GitHub Secrets

Go to your GitHub repo → **Settings → Secrets and variables → Actions → New repository secret**

Add these secrets:

| Secret Name | Value |
|-------------|-------|
| `DOCKER_USERNAME` | Your Docker Hub username |
| `DOCKER_PASSWORD` | Your Docker Hub password or access token |
| `JWT_SECRET` | Your JWT secret |
| `OPENAI_API_KEY` | Your Groq/OpenAI API key |
| `KUBECONFIG` | *(optional)* Your K8s cluster config for auto-deploy |

### Step 2 — The workflow is at `.github/workflows/ci-cd.yml`

It runs automatically on every push to `main`. The pipeline:
1. ✅ Runs backend tests (JUnit)
2. ✅ Builds frontend (npm build)
3. ✅ Builds Docker images (multi-stage)
4. ✅ Pushes to Docker Hub
5. ✅ (Optional) Deploys to K8s

### Manual trigger

```bash
# Push to main to trigger the pipeline
git add .
git commit -m "feat: my changes"
git push origin main
```

---

## 6. Cloud Deployment

### AWS EKS (Elastic Kubernetes Service)

```bash
# Install AWS CLI + eksctl
# https://docs.aws.amazon.com/cli/latest/userguide/install-cliv2.html
# https://eksctl.io/

# Configure AWS credentials
aws configure

# Create EKS cluster (takes ~15 min)
eksctl create cluster \
  --name taskmanager \
  --region ap-south-1 \        # ← change to your region
  --nodegroup-name workers \
  --node-type t3.medium \
  --nodes 2 \
  --nodes-min 2 \
  --nodes-max 5

# Connect kubectl to EKS
aws eks update-kubeconfig --name taskmanager --region ap-south-1

# Use AWS ECR instead of Docker Hub
aws ecr create-repository --repository-name taskmanager-backend --region ap-south-1
aws ecr create-repository --repository-name taskmanager-frontend --region ap-south-1

# Login to ECR
aws ecr get-login-password --region ap-south-1 | \
  docker login --username AWS --password-stdin \
  YOUR_ACCOUNT_ID.dkr.ecr.ap-south-1.amazonaws.com

# Build + push to ECR
docker build -t YOUR_ACCOUNT_ID.dkr.ecr.ap-south-1.amazonaws.com/taskmanager-backend:latest .
docker push YOUR_ACCOUNT_ID.dkr.ecr.ap-south-1.amazonaws.com/taskmanager-backend:latest

# Deploy
kubectl apply -f k8s/
```

### GCP GKE (Google Kubernetes Engine)

```bash
# Install gcloud CLI: https://cloud.google.com/sdk/docs/install

gcloud auth login
gcloud config set project YOUR_PROJECT_ID

# Create cluster
gcloud container clusters create taskmanager \
  --zone asia-south1-a \        # ← change to your zone
  --num-nodes 2 \
  --machine-type e2-medium

# Get credentials
gcloud container clusters get-credentials taskmanager --zone asia-south1-a

# Use Google Container Registry (GCR)
docker tag taskmanager-backend gcr.io/YOUR_PROJECT_ID/taskmanager-backend:latest
docker push gcr.io/YOUR_PROJECT_ID/taskmanager-backend:latest
```

---

## 7. Production Hardening Checklist

> Before going live, verify all of these:

```
Security
  [ ] Change admin password from admin123 to something strong
  [ ] Use a real 256-bit random JWT_SECRET (not the example one)
  [ ] Set CORS origins to your actual domain only (in SecurityConfig.java)
  [ ] Enable HTTPS / TLS (cert-manager + Let's Encrypt)
  [ ] Restrict /actuator/prometheus to internal network only

Database
  [ ] Set a strong DB_PASSWORD
  [ ] Enable PostgreSQL SSL in production
  [ ] Take regular backups (pg_dump or managed DB like AWS RDS)

K8s Secrets
  [ ] Use Sealed Secrets or AWS Secrets Manager (NOT plain K8s secrets)
  [ ] Rotate secrets periodically

AI
  [ ] Set rate limits on /api/ai endpoint to prevent abuse
  [ ] Monitor Groq/OpenAI usage and costs

Monitoring
  [ ] Grafana dashboards imported (IDs: 4701, 12900, 9628, 1860)
  [ ] Set up alert rules in Grafana (error rate, latency, memory)
  [ ] Set up notification channel (Slack, email, PagerDuty)
```

---

## 8. File-by-File Change Summary

> **"What exactly do I need to edit and where?"** — answered here.

| File | What to change | When |
|------|---------------|------|
| `src/main/resources/application.properties` | `spring.datasource.url`, `spring.datasource.username`, `spring.datasource.password`, `openai.api.key` | Local dev |
| `.env` (copy from `.env.example`) | `DB_PASSWORD`, `JWT_SECRET`, `OPENAI_API_KEY` | Docker |
| `k8s/02-secrets.yaml` | Base64 values for `DB_PASSWORD`, `JWT_SECRET`, `OPENAI_API_KEY` | K8s |
| `k8s/04-backend.yaml` line 30 | `image: YOUR_REGISTRY/...` → your Docker Hub/ECR image | K8s |
| `k8s/05-frontend.yaml` line 24 | `image: YOUR_REGISTRY/...` → your Docker Hub/ECR image | K8s |
| `k8s/06-ingress.yaml` line 29 | `taskmanager.yourdomain.com` → your real domain | K8s |
| `k8s/01-configmap.yaml` | `AI_BASE_URL`, `AI_MODEL` if using OpenAI instead of Groq | K8s |
| GitHub Secrets | `DOCKER_USERNAME`, `DOCKER_PASSWORD`, `JWT_SECRET`, `OPENAI_API_KEY` | CI/CD |
| `src/main/resources/application-prod.properties` | Already configured for env vars — **no change needed** ✅ | Docker/K8s |
| `config/SecurityConfig.java` CORS section | Update allowed origins for your production domain | Prod |

---

## 9. CORS — Locking to Your Domain in Production

Open `src/main/java/com/kalyan/advtaskmanager/config/SecurityConfig.java` and find the `corsConfigurationSource()` method:

```java
// CHANGE FROM (dev — allows all localhost):
configuration.setAllowedOriginPatterns(List.of("http://localhost:*"));

// CHANGE TO (production):
configuration.setAllowedOriginPatterns(List.of(
    "https://taskmanager.yourdomain.com",   // ← your domain
    "http://localhost:5173"                  // ← keep for local dev (or remove for pure prod)
));
```

---

## 10. What Happens Automatically (No Config Needed)

| Feature | How |
|---------|-----|
| Admin user created on first startup | `DataInitializer.java` — checks if admin exists, creates if not |
| Database tables created | Hibernate `ddl-auto=update` (dev) / `validate` (prod) |
| `task_status_check` constraint fixed | `schema.sql` runs on startup in dev mode |
| JWT token expiry | 24 hours (86400000ms) — set in `application.properties` |
| Password hashing | BCrypt auto-applied in `DataInitializer.java` and `UserService.java` |
| Health check endpoint | `/actuator/health` — used by Docker and K8s probes |
| Prometheus metrics | `/actuator/prometheus` — scraped every 15s |

---

## Summary — Deployment Order

```
Step 1: Local Dev
  → Edit application.properties (DB creds + Groq key)
  → ./mvnw spring-boot:run + npm run dev

Step 2: Docker (simplest "production-like" setup)
  → cp .env.example .env
  → Fill .env (DB_PASSWORD, JWT_SECRET, OPENAI_API_KEY)
  → docker compose up -d --build

Step 3: Docker + Monitoring
  → docker compose -f docker-compose.yml -f docker-compose.monitoring.yml up -d
  → Import Grafana dashboards (IDs: 4701, 12900, 9628, 1860)

Step 4: CI/CD
  → Add GitHub Secrets (DOCKER_USERNAME, DOCKER_PASSWORD, JWT_SECRET, OPENAI_API_KEY)
  → Push to main → pipeline runs automatically

Step 5: Kubernetes (cloud)
  → Build + push images to registry (Docker Hub / ECR / GCR)
  → Update image names in k8s/04-backend.yaml and k8s/05-frontend.yaml
  → Fill k8s/02-secrets.yaml with base64 encoded secrets
  → Update domain in k8s/06-ingress.yaml
  → kubectl apply -f k8s/
```

