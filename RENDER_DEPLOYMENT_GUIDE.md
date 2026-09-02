# 🚀 How to Deploy Your Mental Health App on Render (Free Tier)

Your application is now fully configured and ready to be hosted on **Render (render.com)**.

---

## 📋 Step 1: Push Your Code to GitHub

Open your terminal in the project directory and run:

```bash
git add .
git commit -m "Configure app for Render deployment"
git push origin main
```

*(If you haven't initialized a repository yet, create a new repository on [github.com](https://github.com), then run `git remote add origin YOUR_REPO_URL && git push -u origin main`)*.

---

## 🌐 Step 2: Deploy on Render

1. Go to **[https://dashboard.render.com](https://dashboard.render.com)** and sign in with GitHub.
2. Click **New +** → Select **Web Service**.
3. Choose your repository (`mental-health` or your repo name) and click **Connect**.
4. Configure the settings:
   - **Name**: `breathe-heal-grow` *(or any name you like)*
   - **Region**: Choose closest to you (e.g., *Singapore / Oregon / Frankfurt*)
   - **Branch**: `main`
   - **Runtime**: **`Docker`** *(Recommended — Render will automatically detect the `Dockerfile` we created!)*
   - **Instance Type**: **`Free`**
5. Click **Create Web Service** at the bottom!

---

## ⚙️ Environment Variables (Optional)

Render will automatically set `PORT=10000` (which our `application.properties` now reads automatically via `server.port=${PORT:8080}`).

If you want to customize your deployment on Render, you can set these in the **Environment** tab on Render:

| Key | Value | Notes |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | `dev` *(or `prod`)* | `dev` uses built-in in-memory DB (simplest). `prod` uses PostgreSQL. |
| `AI_PROVIDER` | `groq` | Multi-key failover is pre-configured |

---

## 🎉 That's It!
Render will build the Docker container and provide a live URL like:
**`https://breathe-heal-grow.onrender.com`**

You can share this live link directly in your B.Tech project presentation or resume!
