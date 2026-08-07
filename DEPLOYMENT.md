# Deploying Maple Bank for free

This gets you a real public link — one for the backend API, one for the app itself —
using four free services: **Aiven** (MySQL database), **CloudAMQP** (RabbitMQ),
**Render** (backend), and **Vercel** (frontend). No credit card required anywhere.
The only real cost is that Render's free tier falls asleep after 15 minutes of no
traffic, so the first visit after a quiet period takes 30-60 seconds to wake up.
After that it's fast.

Total time: about 20-30 minutes, mostly clicking through dashboards.


## 0. Push your code to GitHub first

Render and Vercel both deploy by connecting to a GitHub repo, so your latest code
needs to be pushed. Run this from your own machine (not this chat) in the project
folder:

```bash
git add -A
git commit -m "Add Docker setup, rate limiting, and deployment config"
git push origin main
```

If you hit merge conflicts or auth issues pushing, sort those out first — everything
below assumes `origin/main` on GitHub matches what's on your machine.


## 1. Database — Aiven (free MySQL)

1. Go to [aiven.io](https://aiven.io) and sign up (no card needed).
2. Create a new service → **MySQL** → pick the free plan → choose any region → create.
3. Wait for it to go from "Rebuilding" to "Running" (a minute or two).
4. Open the service's **Overview** tab and note down: **Host**, **Port**, **User**
   (usually `avnadmin`), **Password**, and confirm the default database name
   (usually `defaultdb`).
5. Build your JDBC URL from those values — Aiven requires SSL:
   ```
   jdbc:mysql://<HOST>:<PORT>/defaultdb?sslMode=REQUIRED
   ```
   You'll paste this into Render as `DB_URL` in the next step.


## 2. Message queue — CloudAMQP (free RabbitMQ)

The backend needs a RabbitMQ broker to run at all (see the README's RabbitMQ section) -
it's what password-reset emails are sent through.

1. Go to [cloudamqp.com](https://www.cloudamqp.com) and sign up (no card needed).
2. **Create New Instance** → name it anything → plan **"Little Lemur" (Free)** → pick
   any region → create.
3. Open the instance and copy the **AMQP URL** from the details page. It looks like
   `amqps://someuser:somepassword@some-host.rmq.cloudamqp.com/someuser`.
4. Pull the pieces out of that URL — you'll need them separately in Render:
   - Host: the part after `@` and before the next `/` (e.g. `some-host.rmq.cloudamqp.com`)
   - Username and password: the parts before `@`
   - Port: CloudAMQP's free tier uses TLS on port `5671`, not the default `5672`


## 3. Backend — Render (free web service)

1. Go to [render.com](https://render.com) and sign up with GitHub (no card needed).
2. **New +** → **Web Service** → connect your `MapleBank` repo.
3. Render should detect the `Dockerfile` at the repo root automatically. If asked,
   set **Root Directory** to blank/`.` (not `frontend`) and **Runtime** to Docker.
4. Choose the **Free** instance type.
5. Under **Environment Variables**, add:

   | Key | Value |
   |---|---|
   | `DB_URL` | the JDBC URL you built in step 1 |
   | `DB_USERNAME` | your Aiven user (e.g. `avnadmin`) |
   | `DB_PASSWORD` | your Aiven password |
   | `RABBITMQ_HOST` | the host you pulled out of the CloudAMQP AMQP URL in step 2 |
   | `RABBITMQ_PORT` | `5671` |
   | `RABBITMQ_USERNAME` | the username from that same URL |
   | `RABBITMQ_PASSWORD` | the password from that same URL |
   | `RABBITMQ_SSL` | `true` |
   | `JWT_SECRET` | generate one: run `openssl rand -base64 32` locally and paste the output |
   | `MAIL_USERNAME` | optional — your Gmail address, only if you want password-reset emails to work (see README) |
   | `MAIL_PASSWORD` | optional — the Gmail app password that goes with it |
   | `FRONTEND_URL` | leave as `http://localhost:5173` for now — you'll update this in step 5 |
   | `ALLOWED_ORIGINS` | leave as `http://localhost:5173` for now — same, updated in step 5 |

   Don't set `PORT` — Render sets that automatically and the app now reads it.
6. Click **Create Web Service**. First build takes a few minutes (it's compiling
   the whole app). Watch the logs; once it says the app started, copy the public
   URL Render gives you (something like `https://unibank-xxxx.onrender.com`).
7. Sanity check: visit `https://<your-render-url>/swagger-ui.html` — you should see
   the API docs load.


## 4. Frontend — Vercel (free static hosting)

1. Go to [vercel.com](https://vercel.com) and sign up with GitHub (no card needed).
2. **Add New** → **Project** → import your `MapleBank` repo.
3. Set **Root Directory** to `frontend` (important — the repo root is the backend).
   Vercel should auto-detect the Vite framework preset.
4. Under **Environment Variables**, add:

   | Key | Value |
   |---|---|
   | `VITE_API_URL` | `https://<your-render-url>/api` (from step 3, with `/api` on the end) |

5. Click **Deploy**. A couple minutes later you'll get your live link, something like
   `https://unibank-xxxx.vercel.app` — that's the link you share.


## 5. Wire the two together

The backend needs to know the frontend's real URL, both to allow it through CORS and
to build the link inside password-reset emails.

1. Back in Render, open your web service → **Environment** → update:
   - `FRONTEND_URL` → your Vercel URL (e.g. `https://unibank-xxxx.vercel.app`)
   - `ALLOWED_ORIGINS` → the same Vercel URL
2. Save — Render will automatically redeploy with the new values.


## 6. Try it

Visit your Vercel URL. If the backend had been idle, the first register/login call
will hang for 30-60 seconds while Render wakes it up — that's expected, not a bug.
After that, register an account and click around normally.


## Notes

- **Cold starts**: if you're sharing this link for an interview or review, consider
  visiting it yourself a minute or two beforehand to wake the backend up.
- **Free tier limits**: Aiven's free MySQL plan, CloudAMQP's free RabbitMQ plan, and
  Render's free web service are all meant for small/low-traffic projects like this
  one — fine for a portfolio link, not for real users.
- **Redeploying**: pushing new commits to `main` on GitHub auto-redeploys both
  Render and Vercel.
- **Rotating secrets**: if you ever paste a real secret (JWT_SECRET, DB password,
  Gmail app password) into a public place by mistake, regenerate it — on Render/Aiven
  dashboards for those, and Google Account settings for the Gmail app password.
