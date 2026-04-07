# Advanced Task Manager - Complete Setup & Operations Guide

Complete guide for starting the application and managing git changes.

---

## 📋 Quick Start (Next Time)

### One Command To Start Everything:

```bash
cd /home/pavan/javaPS/advtaskmanager

# Step 1: Start PostgreSQL
docker run -d --name taskmanager-postgres-local \
  -e POSTGRES_DB=Employee \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=root \
  -p 5432:5432 \
  postgres:16-alpine && sleep 5

# Step 2: Start Backend (Keep Terminal Open)
./mvnw spring-boot:run

# Step 3: Start Frontend (Open New Terminal)
cd frontend && npm run dev
```

---

## 🚀 Detailed Startup Instructions

### Prerequisites
- Docker installed
- Java 21+ installed
- Maven installed
- Node.js & npm installed
- Port 5432, 8080, 5173 available

### Step-by-Step Startup

#### Step 1: Start PostgreSQL Database
```bash
cd /home/pavan/javaPS/advtaskmanager

# Remove old containers if they exist
docker rm -f taskmanager-postgres-local 2>/dev/null

# Start PostgreSQL container
docker run -d --name taskmanager-postgres-local \
  -e POSTGRES_DB=Employee \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=root \
  -p 5432:5432 \
  postgres:16-alpine

# Wait for database to be ready
sleep 5
```

**Verify:**
```bash
docker ps | grep postgres
# Should show: taskmanager-postgres-local running on 5432
```

#### Step 2: Start Spring Boot Backend
```bash
# Open Terminal 1
cd /home/pavan/javaPS/advtaskmanager
./mvnw spring-boot:run
```

**Expected Output:**
```
Started AdvtaskmanagerApplication in 3.117 seconds
✅ Admin created → admin@gmail.com / admin123
```

**Verify Backend is Running:**
```bash
curl http://localhost:8080/actuator/health
# Should return: {"status":"UP"}
```

#### Step 3: Start React Frontend
```bash
# Open Terminal 2
cd /home/pavan/javaPS/advtaskmanager/frontend
npm run dev
```

**Expected Output:**
```
VITE v8.0.1 ready in 343 ms
➜ Local: http://localhost:5173/
```

### Access the Application
- **Frontend:** http://localhost:5173
- **Backend API:** http://localhost:8080
- **Admin Email:** admin@gmail.com
- **Admin Password:** admin123

---

## 🛑 Stopping Services

### Stop All Services
```bash
# Terminal 1: Press Ctrl+C (stops backend)
# Terminal 2: Press Ctrl+C (stops frontend)
# Then run:
docker stop taskmanager-postgres-local
```

### Individual Stop Commands
```bash
# Stop Backend
killall java

# Stop Frontend
killall npm

# Stop Database
docker stop taskmanager-postgres-local
docker rm taskmanager-postgres-local  # Optional: remove container
```

### Clean Up Everything
```bash
killall java npm 2>/dev/null
docker stop taskmanager-postgres-local
docker rm taskmanager-postgres-local
echo "✅ All services stopped and cleaned"
```

---

## 🔍 Checking for Code Changes

### View Git Status
```bash
cd /home/pavan/javaPS/advtaskmanager

# See all changes (modified, added, deleted files)
git status

# Output example:
# On branch main
# Changes not staged for commit:
#   modified: src/main/java/com/kalyan/advtaskmanager/service/AIService.java
#   modified: frontend/src/components/Dashboard.jsx
```

### View Detailed Changes

#### Option 1: See Changes in All Files
```bash
git diff
```

#### Option 2: See Changes in Specific File
```bash
# Backend file changes
git diff src/main/java/com/kalyan/advtaskmanager/service/AIService.java

# Frontend file changes
git diff frontend/src/components/Dashboard.jsx
```

#### Option 3: See Only File Names (Summary)
```bash
git diff --name-only
```

#### Option 4: See Changes with Statistics
```bash
git diff --stat
```

**Example Output:**
```
src/main/java/com/kalyan/advtaskmanager/service/AIService.java | 45 +++++++++-----
frontend/src/components/Dashboard.jsx                            | 12 ++--
2 files changed, 57 insertions(+), 20 deletions(-)
```

### View Staged Changes
```bash
# Changes ready to be committed
git diff --cached
```

### View All Changes Since Last Commit
```bash
# Shows both staged and unstaged changes
git diff HEAD
```

---

## 📦 Committing Changes

### Stage Changes
```bash
# Stage all changes
git add .

# Stage specific file
git add src/main/java/com/kalyan/advtaskmanager/service/AIService.java

# Stage specific directory
git add frontend/src/
```

### Commit Changes
```bash
git commit -m "Add feature: describe your change here"
```

### View Commit History
```bash
# Last 5 commits
git log --oneline -5

# Detailed commit history
git log -p

# Commits for specific file
git log src/main/java/com/kalyan/advtaskmanager/service/AIService.java
```

### Undo Changes
```bash
# Discard all uncommitted changes
git checkout -- .

# Discard changes in specific file
git checkout -- src/main/java/com/kalyan/advtaskmanager/service/AIService.java

# Unstage files (keep changes in working directory)
git reset HEAD src/main/java/com/kalyan/advtaskmanager/service/AIService.java
```

---

## 🔄 Git Workflow Summary

```
1. Make code changes
   ↓
2. Check changes: git status / git diff
   ↓
3. Stage changes: git add .
   ↓
4. Commit: git commit -m "message"
   ↓
5. Push (if remote): git push origin main
   ↓
6. Create PR (if team repo)
```

---

## 📊 Useful Git Commands for Quick Reference

```bash
# What files changed?
git status

# What lines changed?
git diff

# What was changed recently?
git log --oneline -10

# Compare with main branch
git diff main

# See changes before committing
git diff --color-words

# Stash changes (save for later)
git stash

# Restore stashed changes
git stash pop

# Create new branch
git checkout -b feature/my-feature

# Switch branch
git checkout main

# Merge branch
git merge feature/my-feature
```

---

## 🗂️ Project Structure

```
advtaskmanager/
├── src/
│   ├── main/
│   │   ├── java/com/kalyan/advtaskmanager/
│   │   │   ├── service/        # Business logic
│   │   │   ├── controller/     # API endpoints
│   │   │   ├── entity/         # Database models
│   │   │   └── repository/     # Database queries
│   │   └── resources/
│   │       └── application.properties  # Database config
│   └── test/
├── frontend/
│   ├── src/
│   │   ├── components/         # React components
│   │   ├── pages/              # Page components
│   │   └── App.jsx             # Main app
│   ├── package.json            # Dependencies
│   └── vite.config.ts          # Vite config
├── pom.xml                     # Maven config
├── docker-compose.yml          # Docker config
└── STARTUP_GUIDE.md            # Startup instructions
```

---

## 🐛 Troubleshooting

### Port Already in Use

```bash
# Port 5432 (PostgreSQL)
sudo systemctl stop postgresql
lsof -i :5432 | grep LISTEN | awk '{print $2}' | xargs kill -9

# Port 8080 (Backend)
lsof -i :8080 | grep LISTEN | awk '{print $2}' | xargs kill -9

# Port 5173 (Frontend)
lsof -i :5173 | grep LISTEN | awk '{print $2}' | xargs kill -9
```

### Database Connection Failed

```bash
# Check if docker container is running
docker ps | grep postgres

# If not running, start it
docker start taskmanager-postgres-local

# If still not working, check logs
docker logs taskmanager-postgres-local
```

### Backend Won't Start

```bash
# Clean build
cd /home/pavan/javaPS/advtaskmanager
./mvnw clean install

# Then run
./mvnw spring-boot:run
```

### Frontend Build Issues

```bash
cd /home/pavan/javaPS/advtaskmanager/frontend

# Clear cache
npm cache clean --force

# Reinstall dependencies
rm -rf node_modules package-lock.json
npm install

# Run
npm run dev
```

---

## 📝 Configuration Files

### Backend Configuration
**Location:** `src/main/resources/application.properties`

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/Employee
spring.datasource.username=postgres
spring.datasource.password=root
spring.datasource.driver-class-name=org.postgresql.Driver
spring.jpa.hibernate.ddl-auto=update
```

### Environment Variables
Create/update `.env` in root:
```env
DB_PASSWORD=root
ALLOWED_ORIGINS=http://localhost:5173,http://localhost:3000
JWT_SECRET=advTaskManagerJWTSecretKeyForAuthentication2024LongEnoughForHS256
```

---

## 📱 API Endpoints (Backend)

```
GET    /api/tasks              - Get all tasks
POST   /api/tasks              - Create new task
GET    /api/tasks/{id}         - Get task by ID
PUT    /api/tasks/{id}         - Update task
DELETE /api/tasks/{id}         - Delete task

GET    /actuator/health        - Health check
GET    /actuator/info          - App info
```

---

## 🎯 Development Workflow

### 1. Start Services
```bash
# Terminal 1: Database
docker run -d --name taskmanager-postgres-local \
  -e POSTGRES_DB=Employee \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=root \
  -p 5432:5432 postgres:16-alpine && sleep 5

# Terminal 2: Backend
cd /home/pavan/javaPS/advtaskmanager && ./mvnw spring-boot:run

# Terminal 3: Frontend
cd /home/pavan/javaPS/advtaskmanager/frontend && npm run dev
```

### 2. Make Changes
Edit code in your IDE

### 3. Check Changes
```bash
git status
git diff
```

### 4. Test Changes
- Test frontend at http://localhost:5173
- Test API at http://localhost:8080/api/tasks

### 5. Commit Changes
```bash
git add .
git commit -m "Describe what you changed"
```

### 6. Push (if applicable)
```bash
git push origin main
```

---

## 📚 Backend Technologies

- **Framework:** Spring Boot 3.4.3
- **Language:** Java 21
- **Database:** PostgreSQL 16
- **ORM:** Hibernate/JPA
- **Build Tool:** Maven
- **Authentication:** JWT

---

## 🎨 Frontend Technologies

- **Framework:** React 19.2.4
- **Build Tool:** Vite 8.0.1
- **Language:** TypeScript
- **Server:** Nginx (production)

---

## 🔐 Security Notes

- Default admin credentials should be changed in production
- JWT secret in `.env` should be strong and unique
- Database password should be strong in production
- Use environment variables for sensitive data
- Enable HTTPS in production

---

## 📞 Quick Help Commands

```bash
# Current working directory
pwd

# Change to project directory
cd /home/pavan/javaPS/advtaskmanager

# List files
ls -la

# View file contents
cat src/main/resources/application.properties

# Search for text in files
grep -r "password" .

# View real-time logs
docker logs -f taskmanager-postgres-local

# List all docker containers
docker ps -a

# Check processes
ps aux | grep -E "java|npm"
```

---

## 🎓 Example: Making a Change

### Scenario: Add new feature to backend

```bash
# 1. Check current status
git status
# No changes

# 2. Make your changes
# Edit: src/main/java/com/kalyan/advtaskmanager/service/AIService.java

# 3. Check what changed
git diff
# Shows: +15 lines added, -3 lines removed

# 4. Stage changes
git add .

# 5. Commit
git commit -m "feat: Add AI suggestion feature to task service"

# 6. View commit
git log --oneline -1
# commit abc1234 feat: Add AI suggestion feature to task service

# 7. Push (if using GitHub)
git push origin main
```

---

## 📞 Need Help?

- Backend logs: Terminal where backend is running
- Frontend logs: Browser console (F12) + Terminal where frontend runs
- Database logs: `docker logs taskmanager-postgres-local`
- Git help: `git help <command>`

---

## ✅ Verification Checklist

Before committing code, verify:

- [ ] All services started without errors
- [ ] `git status` shows expected files
- [ ] `git diff` shows expected changes
- [ ] Frontend loads at http://localhost:5173
- [ ] Backend API responds at http://localhost:8080
- [ ] No console errors in browser
- [ ] No warnings in IDE
- [ ] Code follows project conventions

---

**Last Updated:** April 7, 2026

**Author:** Development Team

**Next Maintainer:** Use this guide to onboard new developers!
