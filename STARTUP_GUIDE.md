# Server Startup Guide

This guide documents all methods to start the Advanced Task Manager application (backend + frontend).

---

## Current Method Used (Successfully Running)

### Method: Native Local Development with Docker PostgreSQL

**What was done:**
1. ✅ Stopped system PostgreSQL service to free port 5432
2. ✅ Started PostgreSQL in Docker container
3. ✅ Started Spring Boot backend natively
4. ✅ Started React frontend with Vite dev server

**Terminal Command Steps:**

```bash
# 1. Stop system PostgreSQL (if running)
sudo systemctl stop postgresql

# 2. Start PostgreSQL Docker container
cd /home/pavan/javaPS/advtaskmanager
docker run -d --name taskmanager-postgres-local \
  -e POSTGRES_DB=Employee \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=root \
  -p 5432:5432 \
  postgres:16-alpine

# 3. Start backend (in Terminal 1)
cd /home/pavan/javaPS/advtaskmanager
./mvnw spring-boot:run

# 4. Start frontend (in Terminal 2)
cd /home/pavan/javaPS/advtaskmanager/frontend
npm install  # First time only
npm run dev
```

**Access Points:**
- Frontend: http://localhost:5173
- Backend API: http://localhost:8080
- Actuator Health: http://localhost:8080/actuator/health

**Default Credentials:**
- Email: `admin@gmail.com`
- Password: `admin123`

**Logs Output:**
```
✅ Admin created → admin@gmail.com / admin123
```

---

## Alternative Methods

### Method 1: Full Docker Compose (Everything in Containers)

**Best for:** Production-like environment, isolation, clean setup

```bash
cd /home/pavan/javaPS/advtaskmanager
docker compose up -d
```

**What it starts:**
- PostgreSQL container (port 5432)
- Spring Boot backend container (port 8080)
- React frontend container (port 80)

**Access:**
- Frontend: http://localhost
- Backend API: http://localhost:8080

**Notes:**
- Requires `.env` file in root directory with `DB_PASSWORD`
- Cleans up with: `docker compose down -v`
- View logs: `docker compose logs -f`

---

### Method 2: Two Separate Terminal Windows (Simplest)

**Best for:** Local development, quick testing, debugging

```bash
# Terminal 1 - Start Backend
cd /home/pavan/javaPS/advtaskmanager
./mvnw spring-boot:run

# Terminal 2 - Start Frontend
cd /home/pavan/javaPS/advtaskmanager/frontend
npm run dev
```

**Prerequisites:**
- PostgreSQL running locally (or point to existing instance)
- Maven installed
- Node.js & npm installed

**Ports:**
- Backend: 8080
- Frontend: 5173

---

### Method 3: Background Processes

**Best for:** Running without taking up visible terminals

```bash
# Start backend in background
cd /home/pavan/javaPS/advtaskmanager
./mvnw spring-boot:run &

# Start frontend in background
cd /home/pavan/javaPS/advtaskmanager/frontend
npm run dev &

# View running processes
jobs

# Kill specific job
kill %1  # or %2
```

---

### Method 4: Docker Compose for DB + Native Services

**Best for:** Hybrid approach - containerized DB, native app servers

```bash
# Start only PostgreSQL via Docker
cd /home/pavan/javaPS/advtaskmanager
docker compose up postgres -d

# Wait a few seconds for DB to be ready
sleep 5

# Start backend natively (Terminal 1)
./mvnw spring-boot:run

# Start frontend natively (Terminal 2)
cd frontend && npm run dev
```

**Notes:**
- PostgreSQL in container, apps run natively
- Good for rapid development
- Easier debugging
- Database isolation

---

## Database Configuration

### Default Database Settings

```properties
# Database Configuration (src/main/resources/application.properties)
spring.datasource.url=jdbc:postgresql://localhost:5432/Employee
spring.datasource.username=postgres
spring.datasource.password=root
```

### Create Custom Database

```bash
# Access PostgreSQL
PGPASSWORD=root psql -U postgres -h localhost

# Inside psql:
CREATE DATABASE mydb;
\l  # List databases
```

---

## Troubleshooting

### Port Already in Use

**If port 5432 (PostgreSQL) is in use:**
```bash
# Kill existing PostgreSQL
sudo systemctl stop postgresql

# Or remove Docker container
docker stop taskmanager-postgres-local
docker rm taskmanager-postgres-local
```

**If port 8080 (Backend) is in use:**
```bash
# Find and kill process
lsof -i :8080
kill -9 <PID>
```

**If port 5173 (Frontend) is in use:**
```bash
lsof -i :5173
kill -9 <PID>
```

### Database Connection Failed

```
FATAL: password authentication failed for user "postgres"
```

**Solutions:**
- Verify PostgreSQL is running: `docker ps | grep postgres`
- Check credentials match: `POSTGRES_PASSWORD=root`
- Ensure database "Employee" exists
- Wait 10 seconds after starting for DB to initialize

### Backend Won't Start

```bash
# Clean cached files
./mvnw clean

# Rebuild everything
./mvnw clean install

# Then run
./mvnw spring-boot:run
```

### Frontend Build Issues

```bash
# Clear npm cache
npm cache clean --force

# Reinstall dependencies
rm -rf node_modules package-lock.json
npm install

# Then run
npm run dev
```

---

## Cleanup Commands

```bash
# Stop Docker containers
docker stop taskmanager-postgres-local
docker rm taskmanager-postgres-local

# Or via compose
docker compose down -v

# Kill background processes
killall java
killall node
```

---

## Performance Comparison

| Method | Setup Time | Performance | Best For |
|--------|-----------|-------------|----------|
| Docker Compose | 2-3 min | Medium | Production |
| Native + Docker DB | 1-2 min | High | Development |
| Native Only | 30 sec | High | Fast Testing |
| Background | 30 sec | High | Non-intrusive |

---

## Quick Start One-Liner

```bash
# Complete setup (choose one)

# Option A: Docker everything
docker compose up -d && sleep 2 && open http://localhost

# Option B: Native with Docker DB
docker run -d --name taskmanager-postgres-local -e POSTGRES_DB=Employee -e POSTGRES_USER=postgres -e POSTGRES_PASSWORD=root -p 5432:5432 postgres:16-alpine && sleep 3 && cd /home/pavan/javaPS/advtaskmanager && ./mvnw spring-boot:run &

# Option C: All native (assumes local DB exists)
cd /home/pavan/javaPS/advtaskmanager && ./mvnw spring-boot:run & && cd frontend && npm run dev
```

---

## Environment Variables

### Backend (.env file in root)
```env
DB_PASSWORD=root
ALLOWED_ORIGINS=http://localhost:5173,http://localhost:3000
JWT_SECRET=advTaskManagerJWTSecretKeyForAuthentication2024LongEnoughForHS256
```

### Frontend (.env.example → .env.local)
```env
VITE_API_BASE_URL=http://localhost:8080
```

---

## Useful Docker Commands

```bash
# View running containers
docker ps

# View all containers (including stopped)
docker ps -a

# View logs
docker logs taskmanager-postgres-local
docker logs -f taskmanager-postgres-local  # Follow logs

# Access container shell
docker exec -it taskmanager-postgres-local psql -U postgres -d Employee

# Remove orphaned containers
docker container prune
```

---

## Summary

**Recommended for Development:**
→ Use **Method 4** (Docker Compose for DB + Native Services)
- Fastest startup
- Easiest debugging
- Best error messages
- Database is isolated

**Recommended for Testing/CI:**
→ Use **Method 1** (Full Docker Compose)
- Completely isolated
- Production-like
- No system dependencies

**Recommended for Quick Testing:**
→ Use **Method 2** (Two Terminals)
- Simplest to understand
- Immediate feedback
- Full control
