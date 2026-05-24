# 🚀 AWS Deployment — Step by Step (Beginner Friendly)
> Think of this like IKEA instructions — every single click is written down.

---

## 🗺️ Big Picture — What Goes Where

```
Your Laptop
  │
  ├── docker push ──────────────→ Docker Hub  (stores your Spring Boot image)
  │
  └── AWS Console
        ├── RDS PostgreSQL       (your database — like Neon but on AWS)
        ├── ECS Fargate          (runs your Spring Boot Docker container)
        └── (Vercel stays as-is for frontend, just update the API URL at the end)
```

**You don't need to touch Vercel until Step 4. Everything else is AWS.**

---

## 🛠️ BEFORE YOU START — Install 2 Things

### 1. AWS CLI
- Download: https://aws.amazon.com/cli/
- Install it (just Next → Next → Finish)
- Open PowerShell and verify:
```powershell
aws --version
# should print: aws-cli/2.x.x
```

### 2. Docker Desktop
- Download: https://www.docker.com/products/docker-desktop
- Install and **start it** (whale icon in taskbar = running)
- Verify:
```powershell
docker --version
# should print: Docker version 24.x.x
```

---

## 👤 AWS ACCOUNT SETUP — Create a Safe User (Don't use root!)

> Root account = your credit card. Never use it for daily work. Create a real user.

### Step A — Create an IAM User

1. Go to https://aws.amazon.com → sign in with your root account
2. In the top search bar → type **IAM** → click it
3. Left sidebar → click **Users**
4. Click the orange **Create user** button
5. Fill in:
   - **User name**: `taskmanager-admin`
   - Check ✅ **Provide user access to the AWS Management Console**
   - Select: **I want to create an IAM user**
   - Console password: **Custom password** → type: `Admin@AWS2024`
   - Uncheck "Users must create a new password"
6. Click **Next**
7. On permissions page → select **Attach policies directly**
8. Search for `AdministratorAccess` → check ✅ the box
9. Click **Next** → Click **Create user**
10. **IMPORTANT**: On the next page click **Download .csv** — save it, has your login URL

### Step B — Create Access Keys (for AWS CLI)

1. Still in IAM → click on the user `taskmanager-admin` you just made
2. Tab: **Security credentials**
3. Scroll to **Access keys** → click **Create access key**
4. Select: **Command Line Interface (CLI)**
5. Check the confirmation checkbox → click **Next** → **Create access key**
6. **COPY BOTH VALUES NOW** (you won't see secret again):
   - Access key ID: `AKIA...`
   - Secret access key: `xxxxx...`

### Step C — Configure AWS CLI

Open PowerShell on your laptop:
```powershell
aws configure
```
Enter when prompted:
```
AWS Access Key ID:     → paste your Access key ID
AWS Secret Access Key: → paste your Secret access key
Default region name:   → ap-south-2
Default output format: → json
```

Test it works:
```powershell
aws sts get-caller-identity
# Should print your account ID and user name — if yes, CLI is working ✅
```

---

## 🐳 STEP 1 — Push Your Docker Image to Docker Hub

> Docker Hub = free image storage. ECS will pull from here.

### 1.1 Create Docker Hub Account
- Go to https://hub.docker.com → Sign up (free)
- Your username will be used in image name — pick something like `kalyanreddy` or similar

### 1.2 Login from PowerShell
```powershell
docker login
# enter your Docker Hub username and password
```

### 1.3 Build your JAR + Docker Image
```powershell
# Go to project folder
cd C:\Users\areddy\IdeaProjectsAsam\advtaskmanager

# Build the JAR (this compiles your Spring Boot app)
./mvnw clean package -DskipTests

# Build Docker image — REPLACE "yourusername" with your actual Docker Hub username
docker build -t yourusername/advtaskmanager-backend:latest .

# This takes 2-5 minutes the first time
```

### 1.4 Push to Docker Hub
```powershell
docker push yourusername/advtaskmanager-backend:latest
```

### 1.5 Make it Public
1. Go to https://hub.docker.com → login
2. Click your repository `advtaskmanager-backend`
3. **Settings tab** → Visibility → click **Make Public**
4. Confirm

✅ **Done! Your image URL is: `yourusername/advtaskmanager-backend:latest`**

---

## 🗄️ STEP 2 — Create RDS PostgreSQL (Your Database)

> RDS = managed PostgreSQL. AWS handles backups, updates. Free tier = 750 hrs/month.

### 2.1 Open RDS Console
1. AWS Console top search bar → type **RDS** → click it
2. Click orange **Create database** button

### 2.2 Fill in Settings (follow exactly)

**Choose a database creation method:**
- ✅ Standard create

**Engine options:**
- Engine type: **PostgreSQL**
- Engine version: **PostgreSQL 15.x** (pick latest 15)

**Templates:**
- ✅ **Free tier** ← very important, click this!

**Settings:**
- DB instance identifier: `taskmanager-db`
- Master username: `postgres`
- Master password: `TaskManager2024` ← save this! (no special chars — avoid @ in passwords)
- Confirm password: `TaskManager2024`

**Instance configuration:**
- DB instance class: `db.t3.micro` (auto-selected by Free tier)

**Storage:**
- Allocated storage: `20` GB
- Leave everything else default

**Connectivity:**
- Compute resource: **Don't connect to an EC2**
- VPC: **Default VPC**
- Public access: **Yes** ← MUST be Yes
- VPC security group: **Create new**
  - New VPC security group name: `taskmanager-rds-sg`
- Availability Zone: **No preference**

**Additional configuration (expand this section):**
- Initial database name: `taskmanager` ← type this!
- (Leave everything else default)

Click **Create database** → wait 5-10 minutes until Status = **Available**

### 2.3 Open Port 5432 in the Security Group

1. Click on your `taskmanager-db` instance
2. In **Connectivity & security** tab → under **VPC security groups** → click the link `taskmanager-rds-sg`
3. You're now in EC2 Security Groups
4. Bottom panel → **Inbound rules** tab → click **Edit inbound rules**
5. Click **Add rule**:
   - Type: `PostgreSQL`
   - Protocol: `TCP` (auto)
   - Port range: `5432` (auto)
   - Source: `Anywhere-IPv4` (gives `0.0.0.0/0`)
6. Click **Save rules**

### 2.4 Copy Your DB Endpoint

1. Go back to RDS → click `taskmanager-db`
2. In **Connectivity & security** → copy the **Endpoint**
   - It looks like: `taskmanager-db.abc123xyz.us-east-1.rds.amazonaws.com`

Your full DB URL will be:
```
jdbc:postgresql://taskmanager-db.c9iii468arxx.ap-south-2.rds.amazonaws.com:5432/taskmanager
```
✅ **Your endpoint is already known — use exactly the above URL in Step 4.**

---

## 🔐 STEP 3 — Create IAM Role for ECS

> ECS needs permission to pull Docker images and write logs. This role gives it that.

1. AWS Console → search **IAM** → click it
2. Left sidebar → **Roles** → click **Create role**
3. **Trusted entity type**: AWS service
4. **Use case**: Search for `Elastic Container Service` → select **Elastic Container Service Task**
5. Click **Next**
6. In the search box type: `AmazonECSTaskExecutionRolePolicy`
7. Check ✅ the box next to it
8. Click **Next**
9. **Role name**: `ecsTaskExecutionRole`
10. Click **Create role**

✅ Done — ECS can now pull images and write logs.

---

## ⚙️ STEP 4 — ECS Fargate (Run Your Spring Boot App)

### 4.1 Create a Cluster

1. AWS Console → search **ECS** → click it
2. Left sidebar → **Clusters** → click **Create cluster**
3. **Cluster name**: `taskmanager-cluster`
4. **Infrastructure**: ✅ **AWS Fargate** (serverless — no servers to manage!)
5. Leave everything else default
6. Click **Create** → wait 30 seconds

### 4.2 Create Task Definition

> A Task Definition = recipe that tells ECS what container to run + what env vars to use

1. Left sidebar → **Task definitions** → **Create new task definition**

**Task definition configuration:**
- Family: `taskmanager-backend`
- Launch type: **AWS Fargate**
- Operating system: **Linux/X86_64**
- CPU: **0.5 vCPU**
- Memory: **1 GB**
- Task execution role: `ecsTaskExecutionRole` ← select from dropdown

**Container — click "Add container":**
- Name: `backend`
- Image URI: `yourusername/advtaskmanager-backend:latest` ← your Docker Hub image
- Essential container: ✅ Yes
- Port mappings: Container port = `8080`, Protocol = `TCP`

**Environment variables — click "Add environment variable" for EACH one:**

| Key | Value |
|-----|-------|
| `DB_URL` | `jdbc:postgresql://taskmanager-db.c9iii468arxx.ap-south-2.rds.amazonaws.com:5432/taskmanager` |
| `DB_USERNAME` | `postgres` |
| `DB_PASSWORD` | `TaskManager2024` |
| `JWT_SECRET` | `ecffb123135d247b35536556a6b494572733cb5311f7638e5ab7017ff31849402c76d2c00d1e0c0728f31abc76c8398fe272b9cbb64c636f7c851c464d52162c` |
| `OPENAI_API_KEY` | `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` |
| `AI_BASE_URL` | `https://api.groq.com/openai/v1/chat/completions` |
| `AI_MODEL` | `llama-3.3-70b-versatile` |
| `ALLOWED_ORIGINS` | `https://advtaskmanager.vercel.app,http://localhost:5173` |
| `JAVA_TOOL_OPTIONS` | `-Duser.timezone=UTC` |

> 📝 Replace `YOURCODE` in DB_URL with the actual endpoint you copied in Step 2.4

**Logging (scroll down):**
- Log collection: ✅ enabled (this sends logs to CloudWatch so you can debug)

Click **Create** at the bottom.

### 4.3 Create a Security Group for ECS

> This controls what traffic can reach your running container.

1. AWS Console → search **EC2** → click it
2. Left sidebar → scroll down → **Security Groups**
3. Click **Create security group**
4. **Security group name**: `taskmanager-ecs-sg`
5. **Description**: `Allow HTTP to Spring Boot`
6. **VPC**: Default VPC
7. **Inbound rules** → Add rule:
   - Type: `Custom TCP`
   - Port range: `8080`
   - Source: `Anywhere-IPv4`
8. Click **Create security group**

### 4.4 Create ECS Service

> A Service = keeps 1 (or more) copy of your task always running

1. Go back to ECS → **Clusters** → click `taskmanager-cluster`
2. **Services** tab → click **Create**

**Environment:**
- Compute options: **Launch type**
- Launch type: **FARGATE**
- Platform version: **LATEST**

**Deployment configuration:**
- Application type: **Service**
- Task definition family: `taskmanager-backend`
- Revision: **LATEST**
- Service name: `taskmanager-service`
- Desired tasks: `1`

**Networking:**
- VPC: **Default VPC**
- Subnets: select all available (check all checkboxes)
- Security group: click **Use an existing security group** → select `taskmanager-ecs-sg`
- Public IP: **TURNED ON** ← very important!

**Load balancing:**
- Load balancer type: **None** (skip for now, raw IP is fine to start)

Click **Create** → wait 2-3 minutes.

### 4.5 Get Your Backend URL

1. ECS → Clusters → `taskmanager-cluster` → **Tasks** tab
2. Click on the running task (status = RUNNING)
3. Scroll to **Network** section → copy the **Public IP**

Your backend URL is: `http://<that-public-ip>:8080`

**Test it in your browser or PowerShell:**
```powershell
curl http://<public-ip>:8080/actuator/health
# Should return: {"status":"UP"}
```

If you see `{"status":"UP"}` → 🎉 **Your Spring Boot app is running on AWS!**

**Test login:**
```powershell
curl -X POST http://<public-ip>:8080/api/auth/login `
  -H "Content-Type: application/json" `
  -d '{"email":"admin@gmail.com","password":"admin123"}'
# Should return a JWT token
```

---

## 🔗 STEP 5 — Connect Vercel Frontend to AWS Backend

Your Vercel frontend is already deployed. You just need to tell it the new backend URL.

1. Go to https://vercel.com → log in → click your `advtaskmanager` project
2. Top menu → **Settings**
3. Left sidebar → **Environment Variables**
4. Find `VITE_API_BASE_URL` (or add it if not there):
   - Key: `VITE_API_BASE_URL`
   - Value: `http://<your-ecs-public-ip>:8080`
5. Click **Save**
6. Go to **Deployments** tab → click the 3 dots on latest deployment → **Redeploy**

✅ **Your Vercel frontend now talks to your AWS backend!**

---

## 🔄 How to Redeploy After Code Changes

Every time you change code and want to update production:

```powershell
cd C:\Users\areddy\IdeaProjectsAsam\advtaskmanager

# 1. Build new JAR
./mvnw clean package -DskipTests

# 2. Build new Docker image
docker build -t yourusername/advtaskmanager-backend:latest .

# 3. Push to Docker Hub
docker push yourusername/advtaskmanager-backend:latest

# 4. Tell ECS to restart with new image
aws ecs update-service `
  --cluster taskmanager-cluster `
  --service taskmanager-service `
  --force-new-deployment `
  --region ap-south-2
```

ECS will pull the new image and restart — takes ~2 minutes. Zero downtime.

---

## 🆘 Troubleshooting

### Container keeps restarting / won't start
1. ECS → Clusters → taskmanager-cluster → **Tasks** tab
2. Click on the stopped task → **Logs** tab
3. Read the error — it will tell you exactly what's wrong

### Can't connect to database
- Check: RDS security group has port 5432 open to `0.0.0.0/0`
- Check: DB_URL in Task Definition has the **correct RDS endpoint**
- Check: DB_PASSWORD matches what you set in RDS

### CORS error in browser
- Add your Vercel URL to `ALLOWED_ORIGINS` in the Task Definition
- Update the service (create new revision → update service)

### `TimeZone Asia/Calcutta` error
- Already handled — `JAVA_TOOL_OPTIONS=-Duser.timezone=UTC` env var fixes it

### Docker image not found by ECS
- Make sure the image on Docker Hub is **Public** (Hub → repo → Settings → Visibility)

---

## 💰 What This Will Cost

| Service | Free? | Cost if not free |
|---------|-------|-----------------|
| RDS db.t3.micro | ✅ 750 hrs free/month | $0 for 1 year |
| ECS Fargate 0.5vCPU 1GB | ❌ | ~$15/month |
| Data transfer | Mostly free | Minimal |
| **With $200 credits** | | **~13 months free** |

> 💡 **Save money**: When not using, go to ECS → Services → `taskmanager-service` → Update → set **Desired tasks = 0**. Set back to 1 when you want it running.

---

## 🎯 Summary Checklist

```
□ Step 0: aws configure (CLI setup with IAM user keys)
□ Step 1: docker login → build JAR → docker build → docker push
□ Step 2: Create RDS PostgreSQL (free tier) → open port 5432 → copy endpoint
□ Step 3: Create IAM role ecsTaskExecutionRole
□ Step 4: Create ECS cluster → task definition (with all env vars) → service
□ Step 4.5: Get ECS public IP → test /actuator/health → see {"status":"UP"}
□ Step 5: Update VITE_API_BASE_URL in Vercel → redeploy
□ DONE 🎉
```

---

## ⏭️ Next Steps (Do Later)

Once basic deployment works, upgrade with:
1. **ALB (Load Balancer)** → gives you a stable URL instead of changing IP each restart
2. **Route 53** → custom domain like `api.yourdomain.com`
3. **ACM** → free HTTPS/SSL certificate
4. **GitHub Actions** → auto deploy on every `git push main`
5. **ECR** → move Docker image from Docker Hub to AWS (faster, private)
