# Smart Cost Pro — Backend Setup Guide

This guide explains how to deploy the Smart Cost Pro backend on the same Oracle Cloud
VM that runs `smart-time-ai`. Both apps run side by side.

## Architecture

```
[Android App]
    │
    └── HTTPS → Oracle Cloud VM (130.162.241.22)
            ├── Caddy (reverse proxy + auto-HTTPS via DuckDNS)
            │   ├── smart-time-ai.duckdns.org   → 127.0.0.1:3000  (existing)
            │   └── smart-cost-pro.duckdns.org   → 127.0.0.1:3001  (new)
            ├── smart-time-ai       (PM2, port 3000 — leave alone)
            ├── smart-cost-pro      (PM2, port 3001 — this app)
            ├── PostgreSQL 16       (Docker, 127.0.0.1:5432 — new)
            └── LiveKit             (Docker — leave alone)
```

## What's already on the server

| Component | Status |
|---|---|
| Node.js 20.20.2 + npm 10.8.2 | ✅ installed |
| PM2 7.0.4 | ✅ running smart-time-ai (id 0) |
| Docker 29.1.3 | ✅ running livekit |
| Caddy 2.11.7 | ✅ systemd, listening on 80/443 |
| smart-time-ai | ✅ port 3000, do NOT touch |
| livekit | ✅ Docker container, do NOT touch |

## What we'll add

| Component | Notes |
|---|---|
| `/home/ubuntu/smart-cost-pro/` | New project folder |
| PostgreSQL 16 in Docker | Bound to 127.0.0.1:5432 only, with named volume + memory limit |
| smart-cost-pro PM2 process | Port 3001, separate `.env`, never reuse smart-time-ai secrets |
| Caddy site block | New block for smart-cost-pro.duckdns.org |

## Critical constraints

- **Never** reuse smart-time-ai's `.env`, `users.json`, `reset-codes.json`, or Gmail App Password.
- **Never** restart or modify smart-time-ai's PM2 process.
- **Never** run `apt upgrade` or reboot without asking the project owner.
- **Never** start a second LiveKit container — reuse the existing one.
- **All secrets** (DB password, JWT secret, Gmail App Password, Google Client ID) are typed
  into `.env` directly on the server — never pasted in chat or committed to git.

## Step-by-step setup

See **[SETUP_ORACLE.md](SETUP_ORACLE.md)** for the full step-by-step guide.

## Quick rollback

```bash
# Stop the new app
pm2 delete smart-cost-pro

# Remove the Caddy site block (it's a single block — see SETUP_ORACLE.md)
sudo nano /etc/caddy/Caddyfile   # remove the smart-cost-pro block
sudo systemctl reload caddy

# Stop the Postgres container
docker stop smartcost-postgres
docker rm smartcost-postgres

# Remove the project folder (optional — keeps your data on the volume)
rm -rf /home/ubuntu/smart-cost-pro
```

## Security

- The backend verifies Google ID tokens via `google-auth-library` (no spoofed JSON).
- `JWT_SECRET` must be set; the server refuses to start if it's missing or weak.
- OTPs are 6-digit via `crypto.randomInt` (CSPRNG), 10-minute validity, single-use.
- All emails normalized to lowercase (prevents `A@x.com` vs `a@x.com` account duplication).
- Login returns the same error for "user not found" and "wrong password" (no enumeration).
- `trust proxy = 1` so per-IP rate limiting works behind Caddy.
- `helmet` for security headers + `cors` with explicit allowed origins.
