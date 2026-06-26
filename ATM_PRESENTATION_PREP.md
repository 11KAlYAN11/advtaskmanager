# Advanced Task Manager — Presentation Prep Guide
> Read this once before your presentation. Covers the full system + 25 Q&A you're likely to face.

---

## PART 1 — WHAT IS THIS PROJECT?

**Advanced Task Manager (ATM)** is a production-ready, cloud-native Jira-style task management REST API built with **Spring Boot 3.4.3 + Java 17**. It supports JWT authentication, role-based access control, an AI assistant, import/export, and full observability via Prometheus + Grafana.

**Deployed on:** Railway (backend) + Vercel (React frontend)  
**Database:** PostgreSQL 16  
**Live API Docs:** Swagger UI at `/swagger-ui.html`

---

## PART 2 — TECH STACK AT A GLANCE

| Layer | Technology | Why |
|---|---|---|
| Framework | Spring Boot 3.4.3 | Production-grade, auto-config, massive ecosystem |
| Language | Java 17 | LTS, records, sealed classes, performance |
| Database | PostgreSQL 16 | Reliable RDBMS, Railway add-on |
| ORM | JPA / Hibernate | Standard Java persistence, DDL management |
| Auth | JWT (JJWT 0.12.3) | Stateless, scalable, no server-side sessions |
| Password | BCrypt | Slow hash by design — brute-force resistant |
| AI | Groq API (llama-3.3-70b) | OpenAI-compatible, fast inference, free tier |
| Monitoring | Micrometer + Prometheus + Grafana | Industry standard observability stack |
| API Docs | Springdoc OpenAPI 2.8.3 | Auto-generates Swagger UI from annotations |
| Build | Maven | Dependency management, multi-phase builds |
| Container | Docker (multi-stage) | Minimal JRE image, non-root user |
| Hosting | Railway / Render | Git-push deploy, managed Postgres add-on |
| Code gen | Lombok | Eliminates boilerplate getters/setters/builders |

---

## PART 3 — ARCHITECTURE OVERVIEW

```
React (Vercel)
      │  HTTPS  JWT in Authorization header
      ▼
Railway Reverse Proxy (HTTPS termination)
      │  X-Forwarded-Proto: https
      ▼
Spring Boot (Railway container, port 8080)
  ├── JwtAuthenticationFilter  ← runs on every request
  ├── SecurityFilterChain      ← CORS, auth rules, RBAC
  ├── Controllers              ← REST endpoints
  ├── Services                 ← business logic
  ├── Repositories             ← JPA/Hibernate
  └── AIService                ← calls Groq API (OpenAI format)
      │
      ▼
PostgreSQL 16 (Railway add-on)
      │
      ▼
Prometheus scrapes /actuator/prometheus every 15s
      │
      ▼
Grafana dashboards (JVM, DB, HTTP metrics)
```

---

## PART 4 — DATA MODEL

### User
```
id          BIGSERIAL PK
name        VARCHAR NOT NULL
email       VARCHAR UNIQUE NOT NULL
password    VARCHAR (BCrypt hash)
role        VARCHAR  ── ADMIN | USER
tasks       OneToMany → Task (lazy, @JsonIgnore)
```

### Task
```
id                BIGSERIAL PK
title             VARCHAR NOT NULL
description       TEXT
status            VARCHAR  ── TODO | IN_PROGRESS | REVIEW | DONE
priority          VARCHAR  ── LOW | MEDIUM | HIGH | CRITICAL  (default MEDIUM)
due_date          DATE
assigned_user_id  FK → users.id
created_at        TIMESTAMP (auto @PrePersist)
updated_at        TIMESTAMP (auto @PreUpdate)
```

**Relationship:** One User → Many Tasks. Delete user = cascade delete their tasks.

---

## PART 5 — ALL REST ENDPOINTS

### Auth (Public — no token needed)
```
POST  /api/auth/login       { email, password } → { token, role, userId }
POST  /api/auth/register    { name, email, password } → { token, role, userId }
```

### Users
```
POST   /api/users            Create user         ADMIN only
GET    /api/users            List all users      Authenticated
GET    /api/users/{id}       Get user by ID      Authenticated
DELETE /api/users/{id}       Delete user         ADMIN only
DELETE /api/users            Delete all users    ADMIN only
```

### Tasks
```
POST  /api/tasks                          Create task             Authenticated
GET   /api/tasks                          List tasks (filterable) Authenticated
GET   /api/tasks/user/{userId}            Tasks by user           Authenticated
GET   /api/tasks/status?status=TODO       Tasks by status         Authenticated
PUT   /api/tasks/{id}                     Update task             Authenticated
PUT   /api/tasks/{id}/status?status=DONE  Update status only      Authenticated
PUT   /api/tasks/{id}/assign/{userId}     Assign task             Authenticated
DELETE /api/tasks/{id}                    Delete task             ADMIN only
DELETE /api/tasks                         Delete all tasks        ADMIN only
```
**Filter params on GET /api/tasks:** `?status=TODO&priority=HIGH&assignedTo=3&q=search_text`

### AI Assistant
```
POST  /api/ai/chat   { message } → { reply, refreshData, error }   Authenticated
```

### Import / Export (ADMIN only)
```
GET   /api/data/export        Export full JSON snapshot
POST  /api/data/import        Import from JSON snapshot
GET   /api/data/export/csv    Export as ZIP (users.csv + tasks.csv)
POST  /api/data/import/csv    Import from ZIP (multipart/form-data)
```

### Monitoring (Public)
```
GET  /actuator/health       Liveness + readiness probes
GET  /actuator/info         App name, version, description
GET  /actuator/prometheus   Prometheus metrics scrape endpoint
```

---

## PART 6 — JWT AUTHENTICATION FLOW

```
1. POST /api/auth/login  { email, password }
        │
        ▼
2. AuthenticationManager validates via BCrypt
        │
        ▼
3. JwtUtil.generateToken()
   ├── Subject:    email
   ├── Claim:      role (ADMIN or USER)
   ├── Expiry:     24 hours (86,400,000 ms)
   └── Signature:  HMAC-SHA256 (JWT_SECRET env var)
        │
        ▼
4. AuthResponse { token, role, userId } returned to client

5. Client stores token, sends every request:
   Authorization: Bearer <token>
        │
        ▼
6. JwtAuthenticationFilter intercepts:
   ├── Extracts token from header
   ├── Verifies signature + expiry
   ├── Loads user from DB
   └── Sets SecurityContextHolder (Spring knows who you are)
        │
        ▼
7. SecurityFilterChain checks role for the route
   └── 401 if missing/invalid token
   └── 403 if insufficient role
```

---

## PART 7 — ROLE-BASED ACCESS CONTROL (RBAC)

| Action | USER | ADMIN |
|---|:---:|:---:|
| Login / Register | ✅ | ✅ |
| View tasks | ✅ | ✅ |
| Create / Update / Assign task | ✅ | ✅ |
| Delete task | ❌ | ✅ |
| View users | ✅ | ✅ |
| Create / Delete user | ❌ | ✅ |
| Import / Export data | ❌ | ✅ |
| AI — create/update/assign | ✅ | ✅ |
| AI — delete task / create user | ❌ | ✅ |
| Actuator (non-public) | ❌ | ✅ |

**Default admin:** `admin@gmail.com` / `admin123` (created by `DataInitializer` on first run)

---

## PART 8 — AI ASSISTANT

**Provider:** Groq API (llama-3.3-70b-versatile model)  
**Format:** OpenAI Chat Completions API (drop-in compatible)

### How it works
1. User sends a plain-English message: *"Create a task to fix the login bug and assign it to John"*
2. `AIService` builds a **system prompt** containing:
   - Current user's role
   - All existing tasks (title, status, assignee)
   - All existing users
3. Message + context sent to Groq with **tool definitions** (function calling)
4. If AI decides to use a tool, ATM executes it, then sends result back to AI
5. AI generates a natural-language reply

### Available AI Tools
| Tool | Who can use |
|---|---|
| `create_task` | USER + ADMIN |
| `update_task_status` | USER + ADMIN |
| `assign_task` | USER + ADMIN |
| `delete_task` | ADMIN only |
| `create_user` | ADMIN only |

### Response fields
```json
{ "reply": "Done! Task created.", "refreshData": true, "error": false }
```
`refreshData: true` tells the frontend to re-fetch tasks after a mutation.

---

## PART 9 — SECURITY HEADERS

| Header | Value | Purpose |
|---|---|---|
| HSTS | max-age=31536000; includeSubDomains | Force HTTPS for 1 year |
| CSP | default-src 'self'; script-src 'unsafe-inline'... | XSS mitigation |
| X-Frame-Options | SAMEORIGIN | Prevent clickjacking (allow Swagger iframe) |
| X-Content-Type-Options | nosniff | Prevent MIME sniffing |
| CSRF | Disabled | Not needed — stateless JWT, no cookies |

---

## PART 10 — MONITORING STACK

```
Spring Boot
  └── /actuator/prometheus  ← Micrometer auto-exports:
        ├── JVM heap, GC pauses, thread count
        ├── HTTP request count + latency per endpoint
        ├── DB connection pool (HikariCP) stats
        └── Tomcat thread pool

Prometheus (docker-compose.monitoring.yml)
  └── Scrapes backend every 15 seconds, 15-day retention
  └── Also scrapes: node-exporter (CPU/RAM), postgres-exporter

Grafana
  └── Visualizes all Prometheus data
  └── Dashboards: JVM overview, HTTP traffic, DB health
```

---

## PART 11 — DEPLOYMENT ARCHITECTURE

### Docker Image (multi-stage)
```dockerfile
Stage 1: maven:3-eclipse-temurin-17  →  mvn package (fat JAR)
Stage 2: eclipse-temurin:17-jre-alpine  →  copy JAR, run as non-root appuser
```

**JVM tuning for 512 MB Railway containers:**
- `-XX:+UseSerialGC` — lower memory than G1GC
- `-Xmx220m / -Xms64m` — bounded heap
- `-Xss256k` — small thread stacks
- `-XX:TieredStopAtLevel=1` — skip heavy JIT on startup

### Railway (Primary)
- Auto-deploys on `git push main`
- PostgreSQL as Railway add-on (connection via `DB_URL` env var)
- Health check: `/actuator/health` every 30s

### Vercel (Frontend)
- React app built separately, deployed to Vercel
- Calls Railway backend via `REACT_APP_API_URL` env var
- CORS on backend allows all origins (JWT provides auth)

### Required Environment Variables
```
JWT_SECRET          256-bit secret for HMAC-SHA256 signing
DB_URL              jdbc:postgresql://host:port/db
DB_USERNAME         Postgres user
DB_PASSWORD         Postgres password
OPENAI_API_KEY      Groq API key (gsk_...)
APP_URL             https://your-railway-url (used in Swagger server list)
ALLOWED_ORIGINS     https://your-vercel-app.vercel.app (CORS)
```

---

## PART 12 — IMPORT / EXPORT

| Format | Export | Import | Notes |
|---|---|---|---|
| JSON | GET /api/data/export | POST /api/data/import | Full snapshot, ADMIN only |
| CSV/ZIP | GET /api/data/export/csv | POST /api/data/import/csv | ZIP of users.csv + tasks.csv |

**Import strategy:** Delete all → re-insert with ID remapping → preserves task-to-user assignments.  
**Transactional:** `@Transactional` — all-or-nothing. If import fails midway, DB rolls back.

---

---

# PART 13 — Q&A (25 Expected Interview / Presentation Questions)

---

### SECURITY

**Q1. Why JWT instead of sessions?**
JWT is stateless — the server doesn't store anything. Every token is self-contained (user identity + role + expiry). This means any instance of the app can validate a token without sharing state, enabling horizontal scaling. Sessions require a shared session store (Redis, DB) or sticky routing, which adds infrastructure complexity.

---

**Q2. How does the JWT flow work end-to-end?**
Login → `AuthenticationManager` validates email/password via BCrypt → `JwtUtil.generateToken()` signs a token with HMAC-SHA256 using `JWT_SECRET` → token returned to client → client sends `Authorization: Bearer <token>` on every request → `JwtAuthenticationFilter` verifies signature and expiry → Spring `SecurityContextHolder` holds the authenticated user for that request.

---

**Q3. Where is the JWT secret stored and why?**
In the `JWT_SECRET` environment variable, never hardcoded. If hardcoded, anyone with repo access could forge tokens. Railway and Render inject env vars at runtime and keep them encrypted in their vaults. Locally we use a `.env` file (gitignored), loaded by our custom `DotEnvPostProcessor`.

---

**Q4. What happens if someone tampers with the JWT payload?**
The HMAC-SHA256 signature covers the header + payload. Changing a single bit in the payload invalidates the signature. `JwtUtil.isTokenValid()` will throw a `SignatureException` and return false — the request gets a 401.

---

**Q5. Why is CSRF disabled?**
CSRF attacks work by exploiting browser cookie-based sessions — the browser automatically sends cookies to any site. We use JWTs in the `Authorization` header, which browsers don't send automatically. So CSRF is irrelevant here. Disabling it removes unnecessary overhead.

---

**Q6. Why are passwords BCrypt-hashed and not SHA-256?**
SHA-256 is fast — an attacker with a GPU can compute billions of hashes per second. BCrypt is deliberately slow (configurable cost factor). Even if the database leaks, cracking BCrypt hashes takes years at scale. Spring's `BCryptPasswordEncoder` is the standard choice for production.

---

**Q7. How does RBAC work?**
Each JWT contains a `role` claim (ADMIN or USER). Spring Security's `SecurityFilterChain` checks this before allowing access to protected routes. For example, `DELETE /api/tasks/**` requires `hasRole("ADMIN")` — if a USER token hits that endpoint, Spring returns 403 before the controller is even invoked.

---

### ARCHITECTURE & DESIGN

**Q8. Why Spring Boot and not something lighter like Quarkus or Micronaut?**
Spring Boot has the largest ecosystem, most mature security library (Spring Security), and best JPA/Hibernate support. For a team project or portfolio piece, it's the most recognizable stack. Quarkus/Micronaut offer faster startup but Spring Boot 3 with GraalVM native compilation largely closes that gap.

---

**Q9. What does `SessionCreationPolicy.STATELESS` mean?**
It tells Spring Security: never create or use an HTTP session. No `JSESSIONID` cookie is ever set. Every request must carry its own JWT. This is required for true stateless REST APIs and enables horizontal scaling without session affinity.

---

**Q10. Explain the layered architecture.**
- **Controller** — receives HTTP request, validates input, delegates to service
- **Service** — business logic, transactions, orchestration (e.g., `TaskService`, `AIService`)
- **Repository** — JPA interface, Spring auto-generates SQL; no raw SQL written
- **Entity** — JPA-mapped POJOs that represent DB tables
- **DTO** — data shapes for request/response (decouples API contract from DB schema)
- **Config** — security, CORS, OpenAPI, data initialization
- **Security** — JWT filter, custom UserDetailsService

---

**Q11. Why use `@PrePersist` / `@PreUpdate` for timestamps?**
It guarantees `createdAt` and `updatedAt` are always set by the application, regardless of how the entity is saved. If you relied on the caller to set them, they could be null or wrong. It's a single source of truth.

---

**Q12. What is `HikariCP` and why does it matter?**
HikariCP is a JDBC connection pool. Opening a new DB connection for every request is expensive (~100ms). HikariCP keeps a pool of pre-opened connections and hands them to threads on demand. For 512MB Railway containers we cap at 10 max connections to avoid overwhelming the DB.

---

### AI INTEGRATION

**Q13. How does the AI assistant actually modify tasks?**
Groq's API supports OpenAI-style **function calling** (tool use). We send tool definitions (create_task, update_task_status, assign_task, delete_task, create_user) alongside the user's message. The LLM responds with a structured JSON tool call instead of plain text. Our `AIService` executes the tool call (calls the real service method), then sends the result back to the LLM, which generates a human-readable reply.

---

**Q14. What stops an AI from doing things a USER isn't allowed to do?**
`AIService` checks the current user's role before executing any tool. If a USER's session triggers `delete_task`, the service throws an exception (same as if they called the REST endpoint directly). The AI's tool call is just an instruction — the real authorization happens in the Java service layer.

---

**Q15. Why Groq instead of OpenAI directly?**
Groq offers a generous free tier, extremely fast inference (typically < 1 second), and its API is 100% OpenAI Chat Completions compatible — we could swap to OpenAI by changing one env var (`AI_BASE_URL`). For a demo/portfolio project, free + fast is the right tradeoff.

---

### DATABASE

**Q16. Why `ddl-auto=update` instead of `create` or `validate`?**
- `create` — drops and recreates schema on every restart. Fine for dev, catastrophic for prod.
- `validate` — checks schema matches entities but makes no changes. Safe but requires manual migration.
- `update` — adds missing tables/columns, never drops. Safe enough for this scale. For serious prod with migrations, Flyway or Liquibase would be better.

---

**Q17. What is the relationship between User and Task?**
One-to-Many: one user can have many tasks. In JPA, `Task` has `@ManyToOne` with a `assigned_user_id` foreign key column. `User` has `@OneToMany(mappedBy = "assignedTo")` with `@JsonIgnore` to prevent infinite recursion during serialization and eager loading issues.

---

**Q18. Why is `@JsonIgnore` on the `tasks` field in `User`?**
Without it, serializing a `User` would load all their `Task` objects, and each Task's user field would load the User again — infinite recursion → `StackOverflowError`. `@JsonIgnore` breaks the cycle. Tasks are accessed via dedicated endpoints (`GET /api/tasks/user/{userId}`).

---

### MONITORING

**Q19. How does Prometheus work with this app?**
Spring Actuator + Micrometer auto-expose metrics at `/actuator/prometheus` in Prometheus text format. Prometheus scrapes this endpoint every 15 seconds and stores time-series data. Grafana queries Prometheus and renders dashboards. No code changes needed — just a dependency and config.

---

**Q20. What metrics are exported?**
JVM heap/non-heap memory, GC pause count and duration, HTTP request count and latency per endpoint, Tomcat thread pool utilization, HikariCP connection pool (active/pending/idle), and system CPU. Node Exporter adds host CPU/RAM/disk. Postgres Exporter adds DB-level metrics.

---

### DEPLOYMENT

**Q21. Why multi-stage Docker build?**
Stage 1 uses a full Maven + JDK image to compile and package the JAR (~700MB image). Stage 2 copies only the JAR into a minimal JRE Alpine image (~180MB). This drastically reduces the final image size, attack surface, and pull time.

---

**Q22. Why run as a non-root user in Docker?**
If someone exploits the app and gets a shell, they'd be `appuser` with no privileges — they can't modify system files, install software, or escape the container as easily. Running as root inside a container is a security anti-pattern; Railway and Kubernetes security policies often block it.

---

**Q23. How does HTTPS work if Spring Boot serves HTTP?**
Railway terminates SSL at its reverse proxy layer and forwards requests internally as HTTP with `X-Forwarded-Proto: https`. We set `server.forward-headers-strategy=framework` so Spring trusts these forwarded headers — it then generates `https://` URLs in OpenAPI specs, redirects, and `Location` headers correctly.

---

**Q24. What does `spring.profiles.active=prod` change?**
It activates `application-prod.properties` which overrides HikariCP pool settings (keepalive interval, max lifetime tuned for managed PostgreSQL that kills idle connections after 5 minutes), sets `spring.sql.init.mode=never` (skip schema.sql in prod), and tightens logging levels.

---

### DESIGN CHOICES

**Q25. If you were to scale this to 10,000 users, what would you change?**
1. **Add Flyway/Liquibase** for safe, versioned schema migrations.
2. **Add Redis** for JWT blacklisting (logout/token revocation) and caching frequent queries.
3. **Switch to G1GC or ZGC** with more heap once the container has > 1GB RAM.
4. **Add pagination** to all list endpoints (Spring Data `Pageable`).
5. **Separate AI service** into its own microservice to avoid blocking the main request thread pool.
6. **Add an API gateway** (Kong, Spring Cloud Gateway) for rate limiting.
7. **Async task processing** via Spring `@Async` or a message queue (RabbitMQ/Kafka) for heavy imports.
8. **Read replicas** for PostgreSQL to offload analytics/reporting queries.

---

## QUICK REFERENCE CHEATSHEET

```
Default admin:     admin@gmail.com / admin123
JWT expiry:        24 hours
AI model:          llama-3.3-70b-versatile (Groq)
DB pool max:       10 connections
Heap (prod):       64m initial → 220m max
Health check:      GET /actuator/health
Swagger UI:        GET /swagger-ui.html
Metrics:           GET /actuator/prometheus
Task statuses:     TODO → IN_PROGRESS → REVIEW → DONE
Task priorities:   LOW | MEDIUM (default) | HIGH | CRITICAL
Roles:             USER (default on register) | ADMIN
```

---

*Good luck — you built this, you know it better than anyone in the room.*
