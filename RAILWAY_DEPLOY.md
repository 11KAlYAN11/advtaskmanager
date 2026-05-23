# 🚂 Railway Deployment Guide — Advanced Task Manager

> **Railway** gives you persistent servers (no cold starts), built-in PostgreSQL, and a generous free tier.  
> Much better than Render for Spring Boot.

---

## Why Railway over Render?

| | Railway | Render (free) |
|---|---|---|
| Cold starts | ❌ None | ✅ ~30s cold start |
| Persistent server | ✅ Yes | ❌ Spins down after 15 min |
| Built-in PostgreSQL | ✅ Yes (add plugin) | ✅ Yes (separate service) |
| Free tier | ✅ $5 credit/month | ✅ Limited |
| Deploy from GitHub | ✅ Yes | ✅ Yes |
| Custom domains | ✅ Yes | ✅ Yes |

---

## 🚀 Step-by-Step Deployment

### Step 1 — Create Railway Account

1. Go to https://railway.app
2. Sign up with GitHub
3. Authorize Railway to access your repos

---

### Step 2 — Create New Project

1. Click **New Project**
2. Select **Deploy from GitHub repo**
3. Choose your `advtaskmanager` repo
4. Railway will auto-detect the `Dockerfile` ✅

---

### Step 3 — Add PostgreSQL Database

1. In your Railway project, click **+ New**
2. Select **Database → Add PostgreSQL**
3. Railway creates a Postgres instance and auto-sets `DATABASE_URL`

---

### Step 4 — Set Environment Variables

In Railway → your backend service → **Variables** tab, add these:

| Variable | Value |
|----------|-------|
| `SPRING_PROFILES_ACTIVE` | `prod` |
| `DB_URL` | Copy from Railway Postgres → `JDBC_DATABASE_URL` variable (starts with `jdbc:postgresql://`) |
| `DB_USERNAME` | Copy from Railway Postgres → `PGUSER` |
| `DB_PASSWORD` | Copy from Railway Postgres → `PGPASSWORD` |
| `JWT_SECRET` | Generate: `node -e "console.log(require('crypto').randomBytes(32).toString('hex'))"` |
| `OPENAI_API_KEY` | Your Groq key from https://console.groq.com (free) |
| `AI_BASE_URL` | `https://api.groq.com/openai/v1/chat/completions` |
| `AI_MODEL` | `llama-3.3-70b-versatile` |
| `ALLOWED_ORIGINS` | Your Vercel frontend URL e.g. `https://advtaskmanager.vercel.app,http://localhost:5173` |

> 💡 **DB tip**: Railway Postgres auto-injects `DATABASE_URL` (postgres:// format).  
> You need the **JDBC** format. Either:
> - Use the `JDBC_DATABASE_URL` variable Railway provides (it starts with `jdbc:postgresql://`)
> - Or manually set `DB_URL` from the Postgres **Connect** tab → copy the JDBC URL

---

### Step 5 — Deploy

Railway automatically deploys on every push to your `main` branch.

To trigger a manual deploy:
1. Go to your service → **Deployments** tab
2. Click **Deploy** or just push code to GitHub

---

### Step 6 — Get Your Public URL

1. Go to your service → **Settings** tab
2. Under **Networking** → click **Generate Domain**
3. You'll get a URL like: `https://advtaskmanager-production.up.railway.app`

This is your backend API URL.

---

### Step 7 — Update Frontend API URL

In your **Vercel** project (frontend), set the environment variable:

```
VITE_API_URL=https://advtaskmanager-production.up.railway.app
```

Or update your frontend `.env` / `vite.config.ts` to point to the Railway URL.

---

## 📊 Monitor Your App

Railway provides built-in metrics (CPU, Memory, Network) in the **Metrics** tab — no extra setup needed.

For detailed application metrics, your `/actuator/health` and `/actuator/prometheus` endpoints are available.

---

## 🔧 Useful Railway CLI Commands

```bash
# Install Railway CLI
npm install -g @railway/cli

# Login
railway login

# Link to your project (run from project root)
railway link

# View logs
railway logs

# Deploy manually
railway up

# Open your app
railway open

# Set an environment variable
railway variables set JWT_SECRET=your-secret-here

# Run a command in Railway environment (e.g. check DB)
railway run java -jar app.jar
```

---

## 🗄️ Database Notes

Railway PostgreSQL connection details are in the **Postgres service → Variables** tab:

| Railway Variable | Use as |
|-----------------|--------|
| `JDBC_DATABASE_URL` | → set as `DB_URL` in backend service |
| `PGUSER` | → set as `DB_USERNAME` |
| `PGPASSWORD` | → set as `DB_PASSWORD` |
| `PGDATABASE` | Database name (for reference) |

> Railway Postgres does **NOT** drop idle connections like Render/Neon.  
> But HikariCP settings in `application-prod.properties` are already tuned — leave them as is.

---

## 🌐 Full Stack Setup (Railway Backend + Vercel Frontend)

```
GitHub Push → Railway auto-deploys Spring Boot backend
                       ↓
           https://your-app.up.railway.app  (API)
                       ↑
Vercel deploys React frontend → calls Railway backend
           https://your-app.vercel.app  (UI)
```

### CORS — Set allowed origins in Railway

Set this Railway variable:
```
ALLOWED_ORIGINS=https://your-app.vercel.app,http://localhost:5173
```

Your `SecurityConfig.java` already reads `${ALLOWED_ORIGINS:http://localhost:5173}` ✅

---

## 🔑 Default Login After Deploy

```
Email:    admin@gmail.com
Password: admin123
```

> **Change this password** after first login in production!

---

## ✅ Deployment Checklist

```
[ ] Railway account created + GitHub connected
[ ] New project created from GitHub repo
[ ] PostgreSQL plugin added
[ ] All environment variables set (see Step 4 table)
[ ] Domain generated in Settings → Networking
[ ] Frontend VITE_API_URL updated to Railway URL
[ ] ALLOWED_ORIGINS set to Vercel frontend URL
[ ] App is live at https://your-app.up.railway.app/actuator/health
[ ] Login works: admin@gmail.com / admin123
```

---

## 🐛 Troubleshooting

| Problem | Fix |
|---------|-----|
| Build fails | Check Railway build logs — usually a missing env var |
| `DB_URL` not working | Use `JDBC_DATABASE_URL` from Postgres service directly |
| 500 on startup | Check `railway logs` — often missing `JWT_SECRET` |
| CORS errors | Set `ALLOWED_ORIGINS` to your exact Vercel URL |
| App not reachable | Generate domain in Settings → Networking |
| `task_status_check` error | Already fixed in `schema.sql` — runs on startup ✅ |

