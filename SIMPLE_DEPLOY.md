# 🚀 Simple Deploy Guide (Free, No DevOps needed)

## ⚠️ First — Understand What You're Deploying

Your app has **3 pieces**. Each goes to a different free service:

```
┌─────────────────────────────────────────────────────────┐
│  YOUR APP                                               │
│                                                         │
│  1. React Frontend  ──► VERCEL         (free)           │
│  2. Spring Boot API ──► RENDER         (free)           │
│  3. PostgreSQL DB   ──► NEON.TECH      (free)           │
└─────────────────────────────────────────────────────────┘
```

> **Why not all Vercel?** Vercel only runs JavaScript. Your backend is Java (Spring Boot) — it needs Render.

---

## 📋 Your Environment Variables (fill these in as you go)

```
DATABASE_URL     = <get from Neon — Step 1>
JWT_SECRET       = advTaskManagerJWTSecretKeyForAuthentication2024LongEnoughForHS256
OPENAI_API_KEY   = <your-groq-api-key-from-console.groq.com>
BACKEND_URL      = <get from Render — Step 2>   e.g. https://advtaskmanager.onrender.com
```

---

## STEP 1 — Set up the Database (Neon.tech — FREE)

1. Go to **https://neon.tech** → Sign up (use GitHub login)
2. Click **"New Project"** → name it `taskmanager` → **Create Project**
3. Copy the **Connection String**:
   ```
   postgresql://neondb_owner:SOMEPASSWORD@ep-xxx.us-east-2.aws.neon.tech/neondb?sslmode=require
   ```
4. 📝 **Save this as your `DATABASE_URL`**

---

## STEP 2 — Deploy the Backend (Render — FREE)

1. Go to **https://render.com** → Sign up with GitHub
2. Click **"New +" → "Web Service"** → connect **advtaskmanager** repo
3. Render detects `render.yaml` automatically → click **Apply**
4. Go to **Environment → Environment Variables** and add:

   | Variable | Value |
   |----------|-------|
   | `SPRING_PROFILES_ACTIVE` | `prod` |
   | `DB_URL` | your Neon connection string |
   | `DB_USERNAME` | your Neon username |
   | `DB_PASSWORD` | your Neon password |
   | `JWT_SECRET` | `advTaskManagerJWTSecretKeyForAuthentication2024LongEnoughForHS256` |
   | `OPENAI_API_KEY` | `gsk_...` (from console.groq.com) |
   | `AI_BASE_URL` | `https://api.groq.com/openai/v1/chat/completions` |
   | `AI_MODEL` | `llama-3.3-70b-versatile` |
   | `ALLOWED_ORIGINS` | *(add after Step 3 — your Vercel URL)* |

5. Render URL will be: `https://advtaskmanager.onrender.com`
6. **Test**: `https://advtaskmanager.onrender.com/actuator/health` → `{"status":"UP"}`

---

## STEP 3 — Update the Frontend (Vercel — FREE)

### ⚠️ Key Step — this is why Vercel was still using Railway

The `VITE_API_BASE_URL` env var in **Vercel dashboard** overrides the `.env.production` file.
You MUST update it there:

1. Go to **https://vercel.com** → open your **advtaskmanager** project
2. **Settings → Environment Variables**
3. Find `VITE_API_BASE_URL` → **Edit** (or Add if missing):

   | Name | Value |
   |------|-------|
   | `VITE_API_BASE_URL` | `https://advtaskmanager.onrender.com/api` |

4. **Deployments → "..." menu → Redeploy** the latest build
5. Wait ~1 minute — done ✅

> 💡 **First time on Vercel?**
> - "Add New → Project" → select repo → Root Directory = `frontend` → Framework = `Vite`
> - Add `VITE_API_BASE_URL` before clicking Deploy

---

## STEP 4 — Tell the Backend Your Vercel URL (CORS)

**Render → advtaskmanager → Environment → update:**

| Variable | Value |
|----------|-------|
| `ALLOWED_ORIGINS` | `https://advtaskmanager.vercel.app,http://localhost:5173` |

Render auto-redeploys. ✅

---

## STEP 5 — Test Everything

Open your Vercel URL:
- ✅ Login: `admin@gmail.com` / `admin123`
- ✅ Create a task, assign a user
- ✅ Try the AI assistant

---

## 🔁 Making Code Changes

```bash
git add .
git commit -m "your change"
git push origin main
# Render (backend) and Vercel (frontend) both redeploy automatically ✅
```

---

## ❓ Troubleshooting

| Problem | Fix |
|---------|-----|
| Frontend still calls Railway URL | Vercel → Settings → Env Vars → update `VITE_API_BASE_URL` → Redeploy |
| Backend health check fails | Render Logs → check DB_URL / DB_PASSWORD correct? |
| Login fails (CORS error) | Render → `ALLOWED_ORIGINS` must include your exact Vercel URL |
| DB connection error | Neon connection string must end with `?sslmode=require` |
| Render cold start (slow first load) | Free tier sleeps after 15 min — first request ~30s, normal |
| Render build fails | `Dockerfile` must be in root folder of repo |

---

## 💡 Quick Summary

```
Step 1: neon.tech   → Create DB       → copy connection string
Step 2: render.com  → Deploy backend  → set env vars → get Render URL
Step 3: vercel.com  → Update VITE_API_BASE_URL to Render URL → Redeploy
Step 4: render.com  → ALLOWED_ORIGINS = your Vercel URL → auto-redeploy
Step 5: Done! 🎉
```

**Total time: ~20 minutes**
