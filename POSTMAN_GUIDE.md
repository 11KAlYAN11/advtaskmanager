# 🚀 Postman Testing Guide — Advanced Task Manager API

Base URL: `http://localhost:8080`

---

## 📦 Step 1 — Set Up a Postman Environment

Create a Postman **Environment** called `Task Manager` with these variables:

| Variable   | Initial Value              | Description              |
|------------|---------------------------|--------------------------|
| `base_url` | `http://localhost:8080`   | Backend server           |
| `token`    | *(leave empty)*           | Auto-filled after login  |

> **How to create:** Top-right → *Environments* → ➕ New → add the variables above → Save

---

## 🔐 Step 2 — Login & Get Your JWT Token

### `POST /api/auth/login`

This is the **only endpoint that needs no token**.

**Request:**
```
POST {{base_url}}/api/auth/login
Content-Type: application/json
```

**Body (raw → JSON):**
```json
{
  "email": "admin@gmail.com",
  "password": "admin123"
}
```

**Response:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9.eyJyb2xlIjoiQURNSU4...",
  "name": "Admin",
  "email": "admin@gmail.com",
  "role": "ADMIN"
}
```

### ✅ Auto-save the token using a Postman Test Script

In the **Tests** tab of the login request, paste this:

```javascript
const res = pm.response.json();
if (res.token) {
    pm.environment.set("token", res.token);
    console.log("✅ Token saved:", res.token.substring(0, 30) + "...");
}
```

Now every time you login, the `{{token}}` variable is updated automatically.

---

## 🔑 Step 3 — Authorize All Other Requests

For every request that needs authentication:

1. Go to the **Authorization** tab
2. Select **Type → Bearer Token**
3. In the Token field type: `{{token}}`

> **Pro tip:** Set this on the **Collection level** so all requests inherit it automatically.
> Collection → Edit → Authorization → Bearer Token → `{{token}}`

---

## 👤 AUTH Endpoints

| Method | Endpoint | Auth | Who |
|--------|----------|------|-----|
| POST | `/api/auth/login` | ❌ None | Everyone |
| POST | `/api/auth/register` | ✅ Token | Any authenticated |

### POST `/api/auth/register`
Creates a USER-role account (for giving team members login access).

```
POST {{base_url}}/api/auth/register
Authorization: Bearer {{token}}
Content-Type: application/json
```
```json
{
  "name": "John Doe",
  "email": "john@gmail.com",
  "password": "john123"
}
```
**Response:**
```json
{
  "token": "eyJhbGci...",
  "name": "John Doe",
  "email": "john@gmail.com",
  "role": "USER"
}
```

---

## 👥 USER Endpoints

| Method | Endpoint | Auth | Role Required |
|--------|----------|------|---------------|
| GET    | `/api/users`      | ✅ | Any |
| GET    | `/api/users/{id}` | ✅ | Any |
| POST   | `/api/users`      | ✅ | **ADMIN** |
| DELETE | `/api/users/{id}` | ✅ | **ADMIN** |
| DELETE | `/api/users`      | ✅ | **ADMIN** |

### GET All Users
```
GET {{base_url}}/api/users
Authorization: Bearer {{token}}
```
**Response:**
```json
[
  {
    "id": 1,
    "name": "Admin",
    "email": "admin@gmail.com",
    "role": "ADMIN",
    "tasks": []
  }
]
```

### GET User by ID
```
GET {{base_url}}/api/users/1
Authorization: Bearer {{token}}
```

### POST Create User — 🔴 ADMIN only
```
POST {{base_url}}/api/users
Authorization: Bearer {{token}}
Content-Type: application/json
```
```json
{
  "name": "Jane Smith",
  "email": "jane@gmail.com",
  "password": "jane123",
  "role": "USER"
}
```
> ⚠️ If called with a USER-role token → **403 Forbidden**

### DELETE User by ID — 🔴 ADMIN only
```
DELETE {{base_url}}/api/users/2
Authorization: Bearer {{token}}
```

### DELETE All Users — 🔴 ADMIN only
```
DELETE {{base_url}}/api/users
Authorization: Bearer {{token}}
```

---

## 📋 TASK Endpoints

| Method | Endpoint | Auth | Role Required |
|--------|----------|------|---------------|
| GET    | `/api/tasks`                      | ✅ | Any |
| GET    | `/api/tasks/{id}`                 | ✅ | Any |
| GET    | `/api/tasks/user/{userId}`        | ✅ | Any |
| GET    | `/api/tasks/status?status=`       | ✅ | Any |
| POST   | `/api/tasks`                      | ✅ | Any |
| PUT    | `/api/tasks/{id}/assign/{userId}` | ✅ | Any |
| PUT    | `/api/tasks/{id}/status?status=`  | ✅ | Any |
| DELETE | `/api/tasks/{id}`                 | ✅ | **ADMIN** |
| DELETE | `/api/tasks`                      | ✅ | **ADMIN** |

### GET All Tasks
```
GET {{base_url}}/api/tasks
Authorization: Bearer {{token}}
```

### GET Tasks by Status
```
GET {{base_url}}/api/tasks/status?status=REVIEW
Authorization: Bearer {{token}}
```
Valid values: `TODO` | `IN_PROGRESS` | `REVIEW` | `DONE`

### GET Tasks by User
```
GET {{base_url}}/api/tasks/user/1
Authorization: Bearer {{token}}
```

### POST Create Task
```
POST {{base_url}}/api/tasks
Authorization: Bearer {{token}}
Content-Type: application/json
```
```json
{
  "title": "Fix login bug",
  "description": "Users can't login on mobile browsers",
  "status": "TODO"
}
```
**Response:**
```json
{
  "id": 5,
  "title": "Fix login bug",
  "description": "Users can't login on mobile browsers",
  "status": "TODO",
  "assignedTo": null,
  "createdAt": "2026-03-21T17:00:00",
  "updatedAt": "2026-03-21T17:00:00"
}
```

### PUT Assign Task to User
```
PUT {{base_url}}/api/tasks/5/assign/1
Authorization: Bearer {{token}}
```
> No body needed — user ID is in the URL path.

### PUT Update Task Status (Move Column)
```
PUT {{base_url}}/api/tasks/5/status?status=REVIEW
Authorization: Bearer {{token}}
```
Valid statuses: `TODO` → `IN_PROGRESS` → `REVIEW` → `DONE`

> No body needed — status is a query parameter.

### DELETE Task by ID — 🔴 ADMIN only
```
DELETE {{base_url}}/api/tasks/5
Authorization: Bearer {{token}}
```
> ⚠️ If called with USER-role token → **403 Forbidden**

### DELETE All Tasks — 🔴 ADMIN only
```
DELETE {{base_url}}/api/tasks
Authorization: Bearer {{token}}
```

---

## 🔐 Role-Based Access Matrix

| Operation | ADMIN Token | USER Token | No Token |
|-----------|------------|------------|----------|
| Login | ✅ | ✅ | ✅ (public) |
| Get users/tasks | ✅ | ✅ | ❌ 401 |
| Create task | ✅ | ✅ | ❌ 401 |
| Assign / move task | ✅ | ✅ | ❌ 401 |
| Create user | ✅ | ❌ 403 | ❌ 401 |
| Delete task | ✅ | ❌ 403 | ❌ 401 |
| Delete user | ✅ | ❌ 403 | ❌ 401 |

---

## ⚡ Quick Postman Collection Setup (Step-by-step)

```
1. Open Postman
2. Click "New Collection" → name it "Task Manager API"
3. Click Collection → Edit → Authorization
      Type: Bearer Token
      Token: {{token}}          ← all requests inherit this
4. Create Environment "Task Manager"
      base_url = http://localhost:8080
      token    = (empty for now)
5. Add the Login request → paste the Test script → Send
6. Token is now in {{token}} automatically
7. Add all other requests — they auto-use the token from step 3
```

---

## 🧪 Full Test Workflow

```
Step 1 → POST /api/auth/login              (get ADMIN token)
Step 2 → POST /api/users                   (create a normal user)
Step 3 → POST /api/auth/login              (login as new user, save USER token)
Step 4 → POST /api/tasks                   (create task as USER → ✅)
Step 5 → PUT  /api/tasks/1/status?status=IN_PROGRESS  (move task → ✅)
Step 6 → PUT  /api/tasks/1/assign/1        (assign to admin → ✅)
Step 7 → DELETE /api/tasks/1  (with USER token → ❌ 403 Forbidden)
Step 8 → Switch back to ADMIN token
Step 9 → DELETE /api/tasks/1  (with ADMIN token → ✅ deleted)
```

---

## 🐛 Common Errors

| Error | Cause | Fix |
|-------|-------|-----|
| `401 Unauthorized` | No token or expired token | Re-login to get fresh token |
| `403 Forbidden` | Logged in as USER, doing ADMIN op | Use ADMIN credentials |
| `400 Bad Request` | Email already exists on register | Use a different email |
| `500 Internal Server Error` | Check backend logs | Usually a DB or validation issue |

---

## 📌 JWT Token Details

- **Algorithm:** HS256
- **Expiry:** 24 hours (86400000 ms)
- **Header format:** `Authorization: Bearer <token>`
- **Payload contains:** `email`, `role`, `iat`, `exp`

You can inspect your token at [jwt.io](https://jwt.io) — paste the token and see the decoded payload.

---

*Generated: March 21, 2026 | Backend: Spring Boot 3.2.5 + Spring Security 6 + JJWT 0.12.3*

