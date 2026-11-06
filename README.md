# Smart Cost Pro — Android App

Native Android financial app built with Kotlin + Jetpack Compose + Material 3.

## Stage 3.3.2 — security hardening + Oracle backend

### What's in this repo

```
cost-app-pro/
├── app/                 ← Android app (Kotlin + Compose + Material 3)
├── backend/             ← REST backend (Node.js + Express + PostgreSQL)
└── oracle/              ← Deployment guide for Oracle Cloud VM
```

### Architecture

```
[Android App]
    │
    └── HTTPS → Oracle Cloud VM (Caddy reverse proxy)
            ├── smart-cost-pro backend  (PM2, port 3001, Express.js)
            │   ├── JWT auth (bcrypt-hashed passwords)
            │   ├── OTP reset (6-digit, 10-min, single-use, emailed via Gmail)
            │   ├── Google Sign-In (verified server-side via google-auth-library)
            │   └── Expenses/income/certificates/students/... CRUD (PostgreSQL)
            └── PostgreSQL 16 (Docker, 127.0.0.1:5432 only)
```

### Authentication (Stage 3.3.2 — security-hardened)

- **Email + password** sign-in / sign-up (bcrypt, 12 rounds)
- **Google Sign-In** — ID token verified server-side via `google-auth-library`
- **Biometric login** (AndroidX BiometricPrompt, Class 3 strong)
- **Guest mode** (local only, no server account)
- **Forgot-password flow** with OTP:
  - 6-digit code via `crypto.randomInt` (CSPRNG)
  - 10-minute validity, single-use, max 5 wrong attempts
  - Emailed via Gmail SMTP (nodemailer)
  - Returned in the response **only** when `NODE_ENV === 'development'`
- **5-stage login UI**: Login → Register → ForgotPassword → OTP → ResetPassword

### Security hardening (Stage 3.3.2)

| Fix | What changed |
|---|---|
| **Google Sign-In** | Server now verifies the ID token via `google-auth-library` (was: accepted any JSON payload) |
| **JWT_SECRET** | Server refuses to start if missing or weak (was: had a known default) |
| **OTP debug leak** | Returned only in `development` mode, not just non-production (was: leaked in staging) |
| **trust proxy** | Express uses Caddy's X-Forwarded-For, so per-IP rate limiting works (was: all clients shared one IP) |
| **OTP entropy** | `crypto.randomInt` CSPRNG (was: `Math.random`) |
| **Email normalization** | All emails lowercased + trimmed (was: `A@x.com` ≠ `a@x.com`) |
| **Error messages** | Generic auth errors; same message for "user not found" and "wrong password" (no enumeration) |
| **Cleartext HTTP** | Only allowed in debug builds via `network_security_config.xml` (was: would break on Android 9+) |
| **Bearer token** | `Authorization: Bearer <jwt>` sent on every request (was: not sent → protected routes 401) |

### Backend stack

- **Node.js 20** + **Express 4**
- **PostgreSQL 16** (in Docker on Oracle VM, bound to 127.0.0.1)
- **bcryptjs** for password hashing (12 rounds)
- **jsonwebtoken** for JWT (30-day expiry)
- **google-auth-library** for Google ID token verification
- **nodemailer** for OTP emails (Gmail SMTP)
- **helmet** + **cors** + **express-rate-limit** for hardening
- **PM2** as the daemon (separate process from `smart-time-ai`)

### Android stack

- **Kotlin** 2.0.21 + **Jetpack Compose** (BOM 2024.12) + **Material 3**
- **Hilt** 2.52 (dependency injection)
- **Navigation Compose** 2.8.5
- **Room** 2.6.1 (13 tables — offline-first cache)
- **AndroidX Biometric** 1.2.0-alpha05 (Class 3 strong)
- **DataStore Preferences** (session + settings)
- **AppCompat** 1.7.0 (per-app language switching)
- **Reorderable** 2.4 (drag-and-drop sections)

### How to build the app

1. Open in Android Studio (Ladybug or newer)
2. Sync Gradle
3. Run ▶ on emulator/device (minSdk 24+)

```bash
./gradlew assembleDebug
./gradlew installDebug
```

### How to deploy the backend

See **[oracle/SETUP_ORACLE.md](oracle/SETUP_ORACLE.md)** for the step-by-step guide.

The TL;DR: extract `backend.tar.gz` on the Oracle VM → fill in `.env` with real
secrets → start Postgres in Docker → `npm install && npm run migrate` →
`pm2 start src/server.js --name smart-cost-pro` → add a Caddy site block.

### Tests

```bash
./gradlew testDebugUnitTest
```

Runs `CalculatorsTest` (15 tests covering the spec's acceptance scenarios).

### License

MIT — see `LICENSE`.
