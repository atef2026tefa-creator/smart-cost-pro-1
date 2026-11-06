# Smart Cost Pro — Step-by-Step Backend Setup (Oracle Cloud VM)

This is the additive, side-by-side deployment guide. We never touch `smart-time-ai`,
`livekit`, or `caddy`'s existing config.

## Prerequisites

The project owner must first:

1. **Create a DuckDNS subdomain**: go to https://www.duckdns.org, log in, add
   `smart-cost-pro` as a new subdomain, and point its A record to `130.162.241.22`.
2. **Create a Gmail App Password** dedicated to Smart Cost Pro at
   https://myaccount.google.com/apppasswords (requires 2FA). Do NOT reuse the
   smart-time-ai password.
3. **Upload `backend.tar.gz`** to the server (from the project owner's PC, not by the AI).

---

## Step 1 — Extract the backend on the server

> Run on the server, as user `ubuntu`.

```bash
cd /home/ubuntu
mkdir -p smart-cost-pro
tar -xzf backend.tar.gz -C smart-cost-pro --strip-components=1
cd smart-cost-pro
```

Verify:
```bash
ls -la
# Should show: package.json, .env.example, sql/, src/, ...
```

---

## Step 2 — Create the `.env` file with real secrets

> Run on the server, in `/home/ubuntu/smart-cost-pro/`.

```bash
# Generate a strong JWT secret (64 chars hex) — copy the output
openssl rand -hex 64

# Generate a strong DB password — copy the output
openssl rand -hex 24

# Create the .env file with strict permissions
cp .env.example .env
chmod 600 .env
nano .env
```

In `nano`, replace every `REPLACE_ME_*`:
- `DB_PASSWORD` → paste the output of `openssl rand -hex 24`
- `JWT_SECRET` → paste the output of `openssl rand -hex 64`
- `SMTP_USER` → your real Gmail (e.g. `smartcostpro.2026@gmail.com`)
- `SMTP_PASS` → the 16-char App Password from Google
- `SMTP_FROM` → `"Smart Cost Pro <smartcostpro.2026@gmail.com>"`
- `GOOGLE_CLIENT_ID` → the Web Client ID from google-services.json (optional — leave REPLACE_ME to disable Google Sign-In)
- `CORS_ORIGINS` → `https://smart-cost-pro.duckdns.org`

Save (Ctrl+O, Enter, Ctrl+X).

> **Never** paste secrets in chat, never reuse smart-time-ai's, never commit `.env` to git.

---

## Step 3 — Start PostgreSQL in a Docker container

> Run on the server. This is **additive** — we use a new container with its own volume.

```bash
# Create a Docker volume for the Postgres data (persists across container restarts)
docker volume create smartcost-pgdata

# Start Postgres 16 (arm64 image) bound to 127.0.0.1 only
# Memory limit 512MB (well within the 6GB VM)
docker run -d \
  --name smartcost-postgres \
  --restart unless-stopped \
  --memory=512m \
  -p 127.0.0.1:5432:5432 \
  -e POSTGRES_DB=smartcost \
  -e POSTGRES_USER=smartcost_user \
  -e POSTGRES_PASSWORD=$(grep DB_PASSWORD .env | cut -d= -f2) \
  -v smartcost-pgdata:/var/lib/postgresql/data \
  postgres:16

# Verify it's running
docker ps | grep smartcost-postgres
```

---

## Step 4 — Install backend dependencies + run migration

> Run on the server, in `/home/ubuntu/smart-cost-pro/`.

```bash
npm install --omit=dev

# Run the SQL migration (creates all 15 tables)
# This connects to the Docker Postgres via the .env credentials.
npm run migrate
```

Verify:
```bash
# Connect to Postgres in the container and list tables
docker exec -it smartcost-postgres psql -U smartcost_user -d smartcost -c '\dt'
# Should show ~15 tables: users, password_reset_otps, expenses, monthly_income, ...
```

---

## Step 5 — Start the backend with PM2

> Run on the server, in `/home/ubuntu/smart-cost-pro/`.

```bash
# PM2 reads PORT from .env automatically (dotenv is loaded in server.js).
# We use --name to give it a unique PM2 ID (separate from smart-time-ai).
pm2 start src/server.js --name smart-cost-pro

# Save the PM2 process list (so it survives a future reboot — see Step 8)
pm2 save

# Verify both apps are running side by side
pm2 list
# Should show: smart-time-ai (id 0) + smart-cost-pro (id 1)
```

Verify the backend is healthy:
```bash
curl http://127.0.0.1:3001/health
# Should return: {"ok":true,"version":"1.0.0"}
```

---

## Step 6 — Add the Caddy site block

> Run on the server. **Back up first, validate, then reload (NOT restart).**

```bash
# Back up the current Caddyfile
sudo cp /etc/caddy/Caddyfile /etc/caddy/Caddyfile.bak

# Append the new site block
cat >> /tmp/caddy-new-block <<'EOF'

smart-cost-pro.duckdns.org {
    reverse_proxy 127.0.0.1:3001
}
EOF

sudo sh -c 'cat /tmp/caddy-new-block >> /etc/caddy/Caddyfile'
rm /tmp/caddy-new-block

# Validate the new config (Caddy will print any errors)
sudo caddy validate --config /etc/caddy/Caddyfile

# If validation passes, reload (NOT restart — keeps smart-time-ai up)
sudo systemctl reload caddy
```

Verify:
```bash
# Caddy will auto-provision a Let's Encrypt cert on first request
curl -I https://smart-cost-pro.duckdns.org/health
# Should return HTTP/2 200
```

---

## Step 7 — Test the full flow

> Run from anywhere (your PC, the server, an Android device on the same network).

```bash
# Health check
curl https://smart-cost-pro.duckdns.org/health

# Register a test user
curl -X POST https://smart-cost-pro.duckdns.org/api/auth/register \
  -H 'Content-Type: application/json' \
  -d '{"email":"test@example.com","password":"test123456"}'

# Login
curl -X POST https://smart-cost-pro.duckdns.org/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"test@example.com","password":"test123456"}'

# Forgot password (check the test inbox if SMTP is real)
curl -X POST https://smart-cost-pro.duckdns.org/api/auth/forgot-password \
  -H 'Content-Type: application/json' \
  -d '{"email":"test@example.com"}'
```

---

## Step 8 — Enable PM2 startup on boot (ask the project owner first)

> ⚠️ This step requires the project owner's confirmation. Ask before running.

`smart-time-ai` does NOT auto-start after a reboot (no `pm2 startup` was ever run).
If we run `pm2 startup` now, it will create a `pm2-ubuntu` systemd unit that
starts BOTH `smart-time-ai` and `smart-cost-pro` after a reboot. This is the
correct long-term fix, but it's a system change — **ask the project owner first.**

If the project owner approves:

```bash
pm2 startup
# PM2 will print a command like: sudo env PATH=$PATH:... pm2 startup ... -u ubuntu --hp /home/ubuntu
# Run that exact command.

pm2 save
# This saves the current process list (smart-time-ai + smart-cost-pro) so both
# auto-start after the next reboot.
```

---

## Step 9 — Set up a daily pg_dump backup

> Run on the server. This protects the user data if the Docker volume is lost.

```bash
# Create a backup directory
sudo mkdir -p /var/backups/smartcost
sudo chown ubuntu:ubuntu /var/backups/smartcost

# Create a daily backup script
cat > /home/ubuntu/smartcost-backup.sh <<'BASH'
#!/bin/bash
set -e
DATE=$(date +%Y%m%d_%H%M%S)
BACKUP_DIR=/var/backups/smartcost
docker exec smartcost-postgres pg_dump -U smartcost_user -d smartcost \
  | gzip > "$BACKUP_DIR/smartcost_$DATE.sql.gz"
# Keep the last 7 days
find "$BACKUP_DIR" -name "smartcost_*.sql.gz" -mtime +7 -delete
BASH

chmod +x /home/ubuntu/smartcost-backup.sh

# Add it to the ubuntu user's crontab (no need for root cron)
( crontab -l 2>/dev/null; echo "0 3 * * * /home/ubuntu/smartcost-backup.sh" ) | crontab -

# Verify the crontab entry
crontab -l | grep smartcost
```

---

## Rollback (if anything goes wrong)

```bash
# Stop the new app
pm2 delete smart-cost-pro
pm2 save

# Remove the Caddy site block
sudo nano /etc/caddy/Caddyfile   # delete the smart-cost-pro.duckdns.org block
sudo caddy validate --config /etc/caddy/Caddyfile
sudo systemctl reload caddy

# Stop and remove the Postgres container (data stays on the volume)
docker stop smartcost-postgres
docker rm smartcost-postgres

# (Optional) delete the volume — WARNING: this deletes all user data
# docker volume rm smartcost-pgdata

# (Optional) remove the project folder
rm -rf /home/ubuntu/smart-cost-pro
```

---

## What NOT to do

- ❌ Do NOT touch `smart-time-ai`'s PM2 process, `.env`, or JSON files.
- ❌ Do NOT reuse smart-time-ai's Gmail App Password.
- ❌ Do NOT start a second LiveKit container.
- ❌ Do NOT run `apt upgrade` or reboot without the project owner's OK.
- ❌ Do NOT commit `.env` to git.
- ❌ Do NOT paste secrets in chat.
- ❌ Do NOT change Caddy's existing `smart-time-ai.duckdns.org` block.
