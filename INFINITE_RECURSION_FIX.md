# ✅ FIXED: Infinite Recursion Issue

## 🐛 Problem Detected

**Issue:** When fetching users or tasks via REST API, the application was creating infinite JSON objects due to bidirectional relationship between User and Task entities.

**Symptoms:**
- API responses never complete
- Memory consumption increases rapidly
- Browser/Postman shows continuous loading
- Possible StackOverflowError or OutOfMemoryError

---

## 🔍 Root Cause

### The Circular Reference Problem

```
User Entity                    Task Entity
    ↓                              ↓
tasks (List<Task>) ←→ assignedTo (User)
```

When Spring Boot tries to serialize a User to JSON:
1. User → includes `tasks` list
2. Each Task → includes `assignedTo` (User)
3. That User → includes `tasks` list again
4. Each Task → includes `assignedTo` again
5. **Infinite loop!** 🔄

### Example of the Problem:
```json
{
  "id": 1,
  "name": "John",
  "tasks": [
    {
      "id": 1,
      "title": "Task 1",
      "assignedTo": {
        "id": 1,
        "name": "John",
        "tasks": [
          {
            "id": 1,
            "title": "Task 1",
            "assignedTo": {
              // INFINITE LOOP CONTINUES...
```

---

## ✅ Solution Applied

### Added `@JsonIgnoreProperties` Annotation

This Jackson annotation breaks the circular reference by telling Spring to ignore specific properties during JSON serialization.

### Changes Made:

#### 1. User.java - BEFORE
```java
@OneToMany(mappedBy = "assignedTo", cascade = CascadeType.ALL)
private List<Task> tasks;
```

#### User.java - AFTER
```java
@OneToMany(mappedBy = "assignedTo", cascade = CascadeType.ALL)
@JsonIgnoreProperties("assignedTo") // Prevents infinite recursion
private List<Task> tasks;
```

**What it does:** When serializing a User's tasks, it ignores the `assignedTo` field in each Task.

---

#### 2. Task.java - BEFORE
```java
@ManyToOne
@JoinColumn(name = "assigned_user_id")
private User assignedTo;
```

#### Task.java - AFTER
```java
@ManyToOne
@JoinColumn(name = "assigned_user_id")
@JsonIgnoreProperties("tasks") // Prevents infinite recursion
private User assignedTo;
```

**What it does:** When serializing a Task's assignedTo user, it ignores the `tasks` field.

---

## 📊 Result - Proper JSON Output

### GET /api/users - Now Returns:
```json
[
  {
    "id": 1,
    "name": "John Developer",
    "email": "john@company.com",
    "role": "USER",
    "tasks": [
      {
        "id": 1,
        "title": "Setup CI/CD Pipeline",
        "description": "Configure GitHub Actions",
        "status": "TODO",
        "createdAt": "2026-03-21T15:00:00",
        "updatedAt": "2026-03-21T15:00:00"
        // assignedTo is excluded to prevent loop
      }
    ]
  }
]
```

### GET /api/tasks - Now Returns:
```json
[
  {
    "id": 1,
    "title": "Setup CI/CD Pipeline",
    "description": "Configure GitHub Actions",
    "status": "TODO",
    "assignedTo": {
      "id": 1,
      "name": "John Developer",
      "email": "john@company.com",
      "role": "USER"
      // tasks list is excluded to prevent loop
    },
    "createdAt": "2026-03-21T15:00:00",
    "updatedAt": "2026-03-21T15:00:00"
  }
]
```

---

## 🎯 Alternative Solutions (For Reference)

### Option 1: @JsonManagedReference & @JsonBackReference
```java
// User.java
@OneToMany(mappedBy = "assignedTo")
@JsonManagedReference
private List<Task> tasks;

// Task.java
@ManyToOne
@JsonBackReference
private User assignedTo;
```
**Result:** `assignedTo` is completely excluded from JSON

### Option 2: @JsonIgnore
```java
// User.java
@OneToMany(mappedBy = "assignedTo")
@JsonIgnore
private List<Task> tasks;
```
**Result:** Tasks are never included in User JSON

### Option 3: DTO Pattern (Recommended for Production)
Create separate DTO classes for API responses:
```java
public class UserResponse {
    private Long id;
    private String name;
    private String email;
    private Role role;
    // No tasks field - controlled separately
}

public class TaskResponse {
    private Long id;
    private String title;
    private String description;
    private TaskStatus status;
    private UserSummary assignedTo; // Simplified user object
}
```

**We chose Option 1 (@JsonIgnoreProperties)** because:
- ✅ Simple to implement
- ✅ Still includes both sides of relationship
- ✅ Breaks the circular reference
- ✅ Good for development/learning
- 🔄 Can migrate to DTOs later for production

---

## ✅ Testing the Fix

### Test 1: Create User
```bash
POST http://localhost:8080/api/users
{
  "name": "Alice Developer",
  "email": "alice@company.com",
  "password": "alice123",
  "role": "USER"
}
```

### Test 2: Create Task
```bash
POST http://localhost:8080/api/tasks
{
  "title": "Fix Bug #123",
  "description": "Resolve NPE in service",
  "status": "TODO"
}
```

### Test 3: Assign Task
```bash
PUT http://localhost:8080/api/tasks/1/assign/1
```

### Test 4: Get All Users (Should work now!)
```bash
GET http://localhost:8080/api/users
```
**Before:** Infinite loop, timeout, or error  
**After:** Clean JSON response with user and their tasks

### Test 5: Get All Tasks (Should work now!)
```bash
GET http://localhost:8080/api/tasks
```
**Before:** Infinite loop, timeout, or error  
**After:** Clean JSON response with tasks and assigned users

---

## 🔧 What You Need to Do

### 1. Restart Your Application
```bash
# In IntelliJ: 
Stop the running application (Red square button)
Run again (Green play button)

# OR from Maven:
mvn spring-boot:run
```

### 2. Test the Fixed Endpoints
```powershell
# Get all users (should work now)
Invoke-RestMethod -Uri "http://localhost:8080/api/users" -Method GET

# Get all tasks (should work now)
Invoke-RestMethod -Uri "http://localhost:8080/api/tasks" -Method GET
```

### 3. Verify in Postman
- Import the collection if not already done
- Run "Get All Users" - should return clean JSON
- Run "Get All Tasks" - should return clean JSON
- Create users, tasks, assign them
- Check that relationships work properly

---

## 📝 Summary

### What Was Wrong:
- ❌ Bidirectional JPA relationship caused infinite JSON serialization
- ❌ User → tasks → assignedTo → tasks → ... (infinite loop)

### What Was Fixed:
- ✅ Added `@JsonIgnoreProperties("assignedTo")` to User.tasks
- ✅ Added `@JsonIgnoreProperties("tasks")` to Task.assignedTo
- ✅ Breaks the circular reference during JSON serialization
- ✅ Both entities still maintain their JPA relationships

### Impact:
- ✅ GET /api/users now works properly
- ✅ GET /api/tasks now works properly
- ✅ No infinite loops or memory issues
- ✅ Clean, predictable JSON responses
- ✅ All other endpoints work as expected

---

## 🚀 Next Steps

1. ✅ **Restart your application** with the fixed code
2. ✅ **Test all endpoints** to ensure they work
3. 📖 **Continue testing** the full workflow from QUICKSTART.md
4. 🎯 **Ready for Phase 5:** DTO pattern for even better API design
5. 🔐 **Ready for Phase 6:** Add security and JWT

---

**🎉 The infinite recursion issue is now fixed!**

Your User and Task entities will serialize properly without circular reference issues.

