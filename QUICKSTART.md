# 🚀 Quick Start Guide - Advanced Task Manager

## ⚡ Start Application (3 Ways)

### Option 1: IntelliJ IDEA
1. Open `AdvtaskmanagerApplication.java`
2. Right-click → **Run 'AdvtaskmanagerApplication'**
3. Wait for "Started AdvtaskmanagerApplication" message

### Option 2: Maven Command
```bash
cd C:\Users\areddy\IdeaProjectsAsam\advtaskmanager
mvn spring-boot:run
```

### Option 3: Run JAR
```bash
cd C:\Users\areddy\IdeaProjectsAsam\advtaskmanager
mvn clean package -DskipTests
java -jar target/advtaskmanager-0.0.1-SNAPSHOT.jar
```

---

## ✅ Verify Application is Running

### 1. Open Browser
Navigate to: **http://localhost:8080**

You should see:
```json
{
  "application": "Advanced Task Manager",
  "version": "1.0.0",
  "status": "Running",
  "endpoints": { ... },
  "availableAPIs": { ... }
}
```

### 2. Health Check
Navigate to: **http://localhost:8080/health**

You should see:
```json
{
  "status": "UP",
  "message": "Application is running successfully"
}
```

---

## 🧪 Test APIs with Postman

### Step 1: Import Collection
1. Open Postman
2. Click **Import** button
3. Select `TaskManager_Postman_Collection.json`
4. Collection will be imported with all endpoints

### Step 2: Run Test Workflow

#### 1. Create Admin User
```
POST http://localhost:8080/api/users
Body:
{
  "name": "Admin User",
  "email": "admin@company.com",
  "password": "admin123",
  "role": "ADMIN"
}
```

#### 2. Create Regular User
```
POST http://localhost:8080/api/users
Body:
{
  "name": "John Developer",
  "email": "john@company.com",
  "password": "john123",
  "role": "USER"
}
```

#### 3. Verify Users Created
```
GET http://localhost:8080/api/users
```

#### 4. Create a Task
```
POST http://localhost:8080/api/tasks
Body:
{
  "title": "Setup CI/CD Pipeline",
  "description": "Configure GitHub Actions",
  "status": "TODO"
}
```

#### 5. Assign Task to User
```
PUT http://localhost:8080/api/tasks/1/assign/2
```
(Assigns task ID 1 to user ID 2)

#### 6. Update Task Status
```
PUT http://localhost:8080/api/tasks/1/status?status=IN_PROGRESS
```

#### 7. Get Tasks by User
```
GET http://localhost:8080/api/tasks/user/2
```

#### 8. Get Tasks by Status
```
GET http://localhost:8080/api/tasks/status?status=IN_PROGRESS
```

---

## 🧪 Test with cURL (Windows PowerShell)

### Create User
```powershell
Invoke-RestMethod -Uri "http://localhost:8080/api/users" -Method POST -ContentType "application/json" -Body '{
  "name": "Test User",
  "email": "test@example.com",
  "password": "test123",
  "role": "USER"
}'
```

### Get All Users
```powershell
Invoke-RestMethod -Uri "http://localhost:8080/api/users" -Method GET
```

### Create Task
```powershell
Invoke-RestMethod -Uri "http://localhost:8080/api/tasks" -Method POST -ContentType "application/json" -Body '{
  "title": "New Task",
  "description": "Task description",
  "status": "TODO"
}'
```

### Get All Tasks
```powershell
Invoke-RestMethod -Uri "http://localhost:8080/api/tasks" -Method GET
```

---

## 📊 Check Database

### PostgreSQL
```sql
-- Connect to database
psql -U postgres -d Employee

-- Check users
SELECT * FROM users;

-- Check tasks
SELECT * FROM task;

-- Check assigned tasks
SELECT 
  t.id,
  t.title,
  t.status,
  u.name as assigned_to
FROM task t
LEFT JOIN users u ON t.assigned_user_id = u.id;
```

---

## 🐛 Common Issues & Solutions

### Issue 1: Port 8080 Already in Use
**Solution:**
```powershell
# Find process using port 8080
netstat -ano | findstr :8080

# Kill the process (replace <PID> with actual process ID)
taskkill /PID <PID> /F
```

### Issue 2: Database Connection Failed
**Solution:**
1. Verify PostgreSQL is running
2. Check `application.properties`:
   - URL: `jdbc:postgresql://localhost:5432/Employee`
   - Username: `postgres`
   - Password: `root`
3. Create database if missing:
   ```sql
   CREATE DATABASE Employee;
   ```

### Issue 3: ClassNotFoundException: org.postgresql.Driver
**Solution:**
1. In IntelliJ: Right-click `pom.xml` → Maven → Reload Project
2. Or run: `mvn clean install`

### Issue 4: 404 Error on All Endpoints
**Solution:**
- Make sure UserController has `@RestController` and `@RequestMapping("/api/users")`
- Rebuild project: `mvn clean install`

---

## 📝 Sample Test Scenario

### Scenario: Project Management Workflow

```
1. Create Team Members
   POST /api/users → Admin (ID: 1)
   POST /api/users → Developer1 (ID: 2)
   POST /api/users → Developer2 (ID: 3)

2. Create Tasks
   POST /api/tasks → "Setup Database" (ID: 1)
   POST /api/tasks → "Implement APIs" (ID: 2)
   POST /api/tasks → "Write Tests" (ID: 3)

3. Assign Tasks
   PUT /api/tasks/1/assign/2 → Assign to Developer1
   PUT /api/tasks/2/assign/2 → Assign to Developer1
   PUT /api/tasks/3/assign/3 → Assign to Developer2

4. Start Work
   PUT /api/tasks/1/status?status=IN_PROGRESS

5. Check Developer1's Tasks
   GET /api/tasks/user/2

6. Check In-Progress Tasks
   GET /api/tasks/status?status=IN_PROGRESS

7. Complete Task
   PUT /api/tasks/1/status?status=DONE

8. View All Completed Tasks
   GET /api/tasks/status?status=DONE
```

---

## 🎯 Quick Reference

### All Available Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/` | Application info |
| GET | `/health` | Health check |
| POST | `/api/users` | Create user |
| GET | `/api/users` | Get all users |
| GET | `/api/users/{id}` | Get user by ID |
| POST | `/api/tasks` | Create task |
| GET | `/api/tasks` | Get all tasks |
| PUT | `/api/tasks/{taskId}/assign/{userId}` | Assign task |
| PUT | `/api/tasks/{taskId}/status?status=...` | Update status |
| GET | `/api/tasks/user/{userId}` | Get tasks by user |
| GET | `/api/tasks/status?status=...` | Get tasks by status |

### Valid Enum Values

**Role:**
- `ADMIN`
- `USER`

**TaskStatus:**
- `TODO`
- `IN_PROGRESS`
- `DONE`

---

## 📚 Next Steps

1. ✅ Test all APIs with Postman
2. ✅ Create sample data (users & tasks)
3. ✅ Test task assignment workflow
4. 📖 Read `API_DOCUMENTATION.md` for detailed info
5. 🚀 Ready for Phase 5: DTOs & Validation
6. 🔐 Ready for Phase 6: Security & JWT

---

**🎉 Your application is now running successfully!**

Access it at: **http://localhost:8080**

