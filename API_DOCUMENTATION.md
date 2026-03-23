# 🚀 Advanced Task Manager - API Documentation

## 📋 Overview
A production-ready Spring Boot backend for task management with user assignment, status tracking, and extensible architecture for JWT authentication and AI integration.

---

## 🛠️ Tech Stack
- **Java 17**
- **Spring Boot 3.2.5**
- **Spring Data JPA**
- **PostgreSQL**
- **Lombok**
- **Maven**

---

## 🔧 Setup Instructions

### 1. Database Setup
Make sure PostgreSQL is running and create a database:
```sql
CREATE DATABASE Employee;
```

### 2. Configuration
Update `application.properties` with your database credentials:
```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/Employee
spring.datasource.username=postgres
spring.datasource.password=root
```

### 3. Run Application
```bash
mvn spring-boot:run
```

Or from IntelliJ IDEA:
- Right-click on `AdvtaskmanagerApplication.java`
- Click "Run"

### 4. Access Application
- **Base URL**: http://localhost:8080
- **API Base**: http://localhost:8080/api

---

## 📡 API Endpoints

### 🏠 Home & Health
```
GET /              → Application info & available APIs
GET /health        → Health check
```

---

### 👤 User Management APIs

#### 1️⃣ Create User
```http
POST /api/users
Content-Type: application/json

{
  "name": "John Doe",
  "email": "john@example.com",
  "password": "password123",
  "role": "USER"
}
```

**Response:**
```json
{
  "id": 1,
  "name": "John Doe",
  "email": "john@example.com",
  "password": "password123",
  "role": "USER",
  "tasks": []
}
```

#### 2️⃣ Get All Users
```http
GET /api/users
```

**Response:**
```json
[
  {
    "id": 1,
    "name": "John Doe",
    "email": "john@example.com",
    "role": "USER",
    "tasks": []
  }
]
```

#### 3️⃣ Get User by ID
```http
GET /api/users/1
```

---

### 📌 Task Management APIs

#### 1️⃣ Create Task
```http
POST /api/tasks
Content-Type: application/json

{
  "title": "Build Authentication Module",
  "description": "Implement JWT authentication with Spring Security",
  "status": "TODO"
}
```

**Response:**
```json
{
  "id": 1,
  "title": "Build Authentication Module",
  "description": "Implement JWT authentication with Spring Security",
  "status": "TODO",
  "assignedTo": null,
  "createdAt": "2026-03-21T14:00:00",
  "updatedAt": "2026-03-21T14:00:00"
}
```

#### 2️⃣ Get All Tasks
```http
GET /api/tasks
```

#### 3️⃣ Assign Task to User
```http
PUT /api/tasks/1/assign/1
```

**Description:** Assigns task with ID 1 to user with ID 1

**Response:**
```json
{
  "id": 1,
  "title": "Build Authentication Module",
  "description": "Implement JWT authentication with Spring Security",
  "status": "TODO",
  "assignedTo": {
    "id": 1,
    "name": "John Doe",
    "email": "john@example.com",
    "role": "USER"
  },
  "createdAt": "2026-03-21T14:00:00",
  "updatedAt": "2026-03-21T14:05:00"
}
```

#### 4️⃣ Update Task Status
```http
PUT /api/tasks/1/status?status=IN_PROGRESS
```

**Valid Status Values:**
- `TODO`
- `IN_PROGRESS`
- `DONE`

#### 5️⃣ Get Tasks by User
```http
GET /api/tasks/user/1
```

**Description:** Get all tasks assigned to user with ID 1

#### 6️⃣ Get Tasks by Status
```http
GET /api/tasks/status?status=IN_PROGRESS
```

---

## 🗂️ Data Models

### User Entity
```java
{
  "id": Long,
  "name": String,
  "email": String (unique),
  "password": String,
  "role": "ADMIN" | "USER",
  "tasks": [Task]
}
```

### Task Entity
```java
{
  "id": Long,
  "title": String,
  "description": String,
  "status": "TODO" | "IN_PROGRESS" | "DONE",
  "assignedTo": User,
  "createdAt": LocalDateTime,
  "updatedAt": LocalDateTime
}
```

---

## 🧪 Testing with cURL

### Create a User
```bash
curl -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Alice Smith",
    "email": "alice@example.com",
    "password": "secure123",
    "role": "ADMIN"
  }'
```

### Create a Task
```bash
curl -X POST http://localhost:8080/api/tasks \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Fix Bug #123",
    "description": "Resolve NPE in user service",
    "status": "TODO"
  }'
```

### Assign Task
```bash
curl -X PUT http://localhost:8080/api/tasks/1/assign/1
```

### Update Task Status
```bash
curl -X PUT "http://localhost:8080/api/tasks/1/status?status=IN_PROGRESS"
```

### Get All Tasks
```bash
curl http://localhost:8080/api/tasks
```

---

## 🧪 Testing with Postman

1. **Import Collection**
   - Create a new Collection: "Task Manager"
   - Add requests for each endpoint above

2. **Environment Variables**
   ```
   base_url = http://localhost:8080
   api_base = {{base_url}}/api
   ```

3. **Sample Test Flow**
   - Create 2-3 users
   - Create 5-10 tasks
   - Assign tasks to users
   - Update task statuses
   - Query tasks by user/status

---

## 🏗️ Architecture

### Layered Design
```
Controller Layer (REST APIs)
    ↓
Service Layer (Business Logic)
    ↓
Repository Layer (Data Access)
    ↓
Database (PostgreSQL)
```

### Package Structure
```
com.kalyan.advtaskmanager
├── controller/      → REST endpoints
├── service/         → Business logic
├── repository/      → JPA repositories
├── entity/          → Database models
├── dto/             → Data transfer objects (future)
├── exception/       → Custom exceptions (future)
├── config/          → Configuration classes
├── security/        → JWT & auth (future)
└── util/            → Helper utilities
```

---

## 🔐 Future Enhancements

### Phase 5: Production Improvements
- ✅ **DTO Layer** - Clean API contracts
- ✅ **Validation** - Input validation (@Valid, @NotNull)
- ✅ **Exception Handling** - Global error handling

### Phase 6: Security
- 🔐 **Spring Security** - Authentication & Authorization
- 🔑 **JWT Tokens** - Stateless authentication
- 👥 **Role-based Access** - ADMIN vs USER permissions

### Phase 7: Advanced Features
- 📄 **Pagination** - Handle large datasets
- 🔍 **Search & Filter** - Advanced queries
- 📊 **Auditing** - Track who changed what

### Phase 8: AI Integration
- 🤖 **Natural Language Processing** - "Create a task to fix the login bug"
- 🧠 **MCP Integration** - AI agent → API → Service
- 💡 **Smart Assignment** - Auto-assign based on workload

---

## 📊 Database Schema

### Users Table
```sql
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    password VARCHAR(255),
    role VARCHAR(50) CHECK (role IN ('ADMIN', 'USER'))
);
```

### Tasks Table
```sql
CREATE TABLE task (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    status VARCHAR(50) CHECK (status IN ('TODO', 'IN_PROGRESS', 'DONE')),
    assigned_user_id BIGINT REFERENCES users(id),
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);
```

---

## 🐛 Troubleshooting

### Port 8080 Already in Use
```bash
# Windows
netstat -ano | findstr :8080
taskkill /PID <PID> /F

# Linux/Mac
lsof -i :8080
kill -9 <PID>
```

### Database Connection Issues
1. Verify PostgreSQL is running
2. Check credentials in `application.properties`
3. Ensure database `Employee` exists
4. Check firewall settings

### Maven Build Failures
```bash
mvn clean install -U
```

### IntelliJ Not Recognizing Changes
- File → Invalidate Caches → Invalidate and Restart
- Right-click pom.xml → Maven → Reload Project

---

## 📝 Sample Workflow

```bash
# 1. Create Admin User
POST /api/users
{
  "name": "Admin User",
  "email": "admin@company.com",
  "role": "ADMIN"
}

# 2. Create Regular User
POST /api/users
{
  "name": "Developer",
  "email": "dev@company.com",
  "role": "USER"
}

# 3. Create Tasks
POST /api/tasks
{
  "title": "Setup CI/CD Pipeline",
  "description": "Configure GitHub Actions",
  "status": "TODO"
}

# 4. Assign Task
PUT /api/tasks/1/assign/2

# 5. Update Status
PUT /api/tasks/1/status?status=IN_PROGRESS

# 6. Check Progress
GET /api/tasks/user/2
GET /api/tasks/status?status=IN_PROGRESS
```

---

## 🎯 Interview Talking Points

> "I built a Spring Boot-based task management system with:
> - Layered architecture (Controller → Service → Repository)
> - JPA relationships (One-to-Many between User and Task)
> - RESTful APIs for CRUD operations
> - PostgreSQL integration
> - Designed for extensibility with JWT auth and AI integration
> - Production-ready patterns including proper dependency injection and exception handling"

---

## 📚 Resources

- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- [Spring Data JPA](https://spring.io/projects/spring-data-jpa)
- [PostgreSQL Documentation](https://www.postgresql.org/docs/)
- [REST API Best Practices](https://restfulapi.net/)

---

## 📄 License
This project is for educational purposes.

---

## 👨‍💻 Author
Built by Kalyan as a production-style learning project

---

**🚀 Happy Coding!**

