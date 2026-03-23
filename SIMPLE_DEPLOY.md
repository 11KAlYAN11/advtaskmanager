# 🚀 Simple Deploy Guide (Free, No DevOps needed)

## ⚠️ First — Understand What You're Deploying

Your app has **3 pieces**. Each goes to a different free service:

```
┌─────────────────────────────────────────────────────────┐
│  YOUR APP                                               │
│                                                         │
│  1. React Frontend  ──► VERCEL         (free)           │
│  2. Spring Boot API ──► RAILWAY        (free)           │
│  3. PostgreSQL DB   ──► NEON.TECH      (free)           │
└─────────────────────────────────────────────────────────┘
```

> **Why not all Vercel?** Vercel only runs JavaScript. Your backend is Java (Spring Boot) — it needs Railway or Render.

---

## 📋 Your Environment Variables (fill these in as you go)

Keep this open and fill in the values as you complete each step:

```
DATABASE_URL  = <get from Neon — Step 1>
JWT_SECRET    = advTaskManagerJWTSecretKeyForAuthentication2024LongEnoughForHS256
OPENAI_API_KEY = <your-groq-api-key-from-console.groq.com>
BACKEND_URL   = <get from Railway — Step 2>
```

---

## STEP 1 — Set up the Database (Neon.tech — FREE)

1. Go to **https://neon.tech** → Sign up (use GitHub login)
2. Click **"New Project"**
3. Name it: `taskmanager`
4. Click **Create Project**
5. Copy the **Connection String** — it looks like:
   ```
   postgresql://neondb_owner:SOMEPASSWORD@ep-xxx.us-east-2.aws.neon.tech/neondb?sslmode=require
   ```
6. 📝 **Save this as your `DATABASE_URL`**

---

## STEP 2 — Deploy the Backend (Railway — FREE)

1. Go to **https://railway.app** → Sign up (use GitHub login)
2. Click **"New Project" → "Deploy from GitHub repo"**
3. Select your **advtaskmanager** repository
4. Railway will auto-detect Spring Boot ✅
5. Go to your project → **Variables** tab → Add these one by one:

   | Variable Name | Value |
   |---------------|-------|
   | `SPRING_PROFILES_ACTIVE` | `prod` |
   | `DB_URL` | your Neon connection string from Step 1 |
   | `DB_USERNAME` | your Neon username (in the connection string) |
   | `DB_PASSWORD` | your Neon password (in the connection string) |
   | `JWT_SECRET` | `advTaskManagerJWTSecretKeyForAuthentication2024LongEnoughForHS256` |
   | `OPENAI_API_KEY` | `<your-groq-api-key-from-console.groq.com>` |
   | `AI_BASE_URL` | `https://api.groq.com/openai/v1/chat/completions` |
   | `AI_MODEL` | `llama-3.3-70b-versatile` |
   | `PORT` | `8080` |
   | `ALLOWED_ORIGINS` | *(add after Step 3 — paste your Vercel URL here)* |

6. Go to **Settings → Networking → Generate Domain**
7. 📝 **Save the Railway URL** — it looks like `https://advtaskmanager-production-xxxx.railway.app`

   > ✅ Wait 2-3 minutes for it to build. Check logs in the **Logs** tab.

8. **Test it**: Open `https://YOUR-RAILWAY-URL.railway.app/actuator/health` in browser → should show `{"status":"UP"}`

---

## STEP 3 — Deploy the Frontend (Vercel — FREE)

1. Go to **https://vercel.com** → Sign up (use GitHub login)
2. Click **"Add New → Project"**
3. Select your **advtaskmanager** repository
4. **IMPORTANT** — Vercel will ask for settings. Fill in:

   | Setting | Value |
   |---------|-------|
   | **Root Directory** | `frontend` |
   | **Framework Preset** | `Vite` |
   | **Build Command** | `npm run build` |
   | **Output Directory** | `dist` |

5. **Add Environment Variable** (click "Environment Variables" before deploying):

   | Name | Value |
   |------|-------|
   | `VITE_API_BASE_URL` | `https://YOUR-RAILWAY-URL.railway.app/api` |

   > Replace `YOUR-RAILWAY-URL` with the actual URL from Step 2

6. Click **Deploy** → wait ~1 minute
7. Vercel gives you a URL like: `https://advtaskmanager-xyz.vercel.app`

---

## STEP 4 — Tell the Backend Your Vercel URL (CORS)

Go back to **Railway → Your Project → Variables** and add **one more variable**:

| Variable Name | Value |
|---------------|-------|
| `ALLOWED_ORIGINS` | `https://advtaskmanager-xyz.vercel.app,http://localhost:5173` |

> Replace `advtaskmanager-xyz.vercel.app` with your actual Vercel URL from Step 3.

Railway will auto-redeploy. **No code changes needed.** ✅

---

## STEP 5 — Test Everything

Open your Vercel URL and:
- ✅ Login: `admin@gmail.com` / `admin123`
- ✅ Create a task
- ✅ Create a user
- ✅ Try the AI assistant

---

## 🔁 Workflow After This (making code changes)

```
You write code → git push → Railway & Vercel auto-redeploy ✅
```

That's it. No manual steps needed after the first setup.

```bash
git add .
git commit -m "your change"
git push origin main
# Both Railway (backend) and Vercel (frontend) redeploy automatically
```

---

## ❓ Troubleshooting

| Problem | Fix |
|---------|-----|
| Backend health check fails | Check Railway logs → Variables tab (wrong DB URL?) |
| Frontend shows blank page | Check Vercel logs → Environment Variables (wrong VITE_API_BASE_URL?) |
| Login fails on live site | CORS not set — did you add Vercel URL to SecurityConfig.java? |
| DB connection error | Check Neon connection string — copy it exactly with `?sslmode=require` |
| Railway build fails | Make sure `pom.xml` is in the root folder (not inside a subfolder) |

---

## 💡 Quick Summary

```
Step 1: neon.tech     → Create DB    → Copy connection string
Step 2: railway.app   → Deploy API   → Set 9 env vars → Copy Railway URL
Step 3: vercel.com    → Deploy UI    → Set VITE_API_BASE_URL = Railway URL
Step 4: Add Vercel URL to SecurityConfig.java CORS → git push
Step 5: Done! 🎉
```

**Total time: ~20 minutes**



