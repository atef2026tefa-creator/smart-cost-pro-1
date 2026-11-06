// src/services/authService.js
// Stage 3.3.2 — security fixes applied (per code review):
//   1. Google Sign-In now verifies the ID token via google-auth-library.
//   2. JWT_SECRET must be set; the server refuses to start if it's missing or the default.
//   3. OTP is returned only when NODE_ENV === 'development' (NOT just != production).
//   4. trust proxy is set on the Express app (handled in server.js).
//   5. OTP uses crypto.randomInt (CSPRNG), not Math.random.
//   6. All emails are normalized to lowercase + trimmed.
//   7. Errors return generic messages (no PostgreSQL internals leaked).

const bcrypt = require('bcryptjs');
const jwt = require('jsonwebtoken');
const crypto = require('crypto');
const nodemailer = require('nodemailer');
const { OAuth2Client } = require('google-auth-library');
const { query } = require('../config/database');

// ──────────────────────────── JWT ────────────────────────────

const JWT_SECRET = process.env.JWT_SECRET;
if (!JWT_SECRET || JWT_SECRET.includes('change_me') || JWT_SECRET.length < 32) {
  console.error('FATAL: JWT_SECRET must be set to a strong random value (>= 32 chars).');
  console.error('Generate one with: openssl rand -hex 64');
  process.exit(1);
}
const JWT_EXPIRES_IN = process.env.JWT_EXPIRES_IN || '30d';

function signToken(userId, email) {
  return jwt.sign({ userId, email }, JWT_SECRET, { expiresIn: JWT_EXPIRES_IN });
}

function verifyToken(token) {
  return jwt.verify(token, JWT_SECRET);
}

// ──────────────────────────── Google ────────────────────────────

const googleClient = new OAuth2Client(process.env.GOOGLE_CLIENT_ID);

async function verifyGoogleIdToken(idToken) {
  if (!process.env.GOOGLE_CLIENT_ID) {
    throw Object.assign(new Error('Google Sign-In not configured'), { code: 'GOOGLE_NOT_CONFIGURED' });
  }
  const ticket = await googleClient.verifyIdToken({
    idToken,
    audience: process.env.GOOGLE_CLIENT_ID,
  });
  const payload = ticket.getPayload();
  if (!payload) {
    throw Object.assign(new Error('Invalid Google token'), { code: 'GOOGLE_FAILED' });
  }
  return {
    email: payload.email.toLowerCase().trim(),
    googleId: payload.sub,
    displayName: payload.name || payload.email.substring(0, payload.email.indexOf('@')),
  };
}

// ──────────────────────────── SMTP ────────────────────────────

const transporter = nodemailer.createTransport({
  host: process.env.SMTP_HOST || 'smtp.gmail.com',
  port: parseInt(process.env.SMTP_PORT || '587', 10),
  secure: false,
  auth: {
    user: process.env.SMTP_USER,
    pass: process.env.SMTP_PASS,
  },
});

// ──────────────────────────── Helpers ────────────────────────────

function normalizeEmail(email) {
  return (email || '').toLowerCase().trim();
}

function genericAuthError(code) {
  const messages = {
    INVALID_CREDENTIALS: 'البريد الإلكتروني أو كلمة المرور غير صحيحة',
    USER_NOT_FOUND: 'البريد الإلكتروني أو كلمة المرور غير صحيحة',
    EMAIL_EXISTS: 'هذا البريد مُسجّل مسبقاً',
    WEAK_PASSWORD: 'كلمة المرور يجب أن تكون 6 أحرف على الأقل',
    MISSING_FIELDS: 'كل الحقول مطلوبة',
  };
  return messages[code] || 'حدث خطأ غير متوقع';
}

// ──────────────────────────── Register ────────────────────────────

async function register(email, password, displayName) {
  email = normalizeEmail(email);
  if (!email || !password) {
    throw Object.assign(new Error(genericAuthError('MISSING_FIELDS')), { code: 'MISSING_FIELDS' });
  }
  if (password.length < 6) {
    throw Object.assign(new Error(genericAuthError('WEAK_PASSWORD')), { code: 'WEAK_PASSWORD' });
  }

  const existing = await query('SELECT id FROM users WHERE email = $1', [email]);
  if (existing.rowCount > 0) {
    throw Object.assign(new Error(genericAuthError('EMAIL_EXISTS')), { code: 'EMAIL_EXISTS' });
  }

  const passwordHash = await bcrypt.hash(password, 12);
  const result = await query(
    `INSERT INTO users (email, password_hash, display_name)
     VALUES ($1, $2, $3)
     RETURNING id, email, display_name`,
    [email, passwordHash, displayName || email.split('@')[0]]
  );
  const user = result.rows[0];
  return { user: { id: user.id, email: user.email, displayName: user.display_name }, token: signToken(user.id, user.email) };
}

// ──────────────────────────── Login ────────────────────────────

async function login(email, password) {
  email = normalizeEmail(email);
  if (!email || !password) {
    throw Object.assign(new Error(genericAuthError('MISSING_FIELDS')), { code: 'MISSING_FIELDS' });
  }

  // Always use the same error code for "user not found" and "wrong password" to prevent user enumeration.
  const result = await query(
    'SELECT id, email, password_hash, display_name FROM users WHERE email = $1',
    [email]
  );
  if (result.rowCount === 0) {
    throw Object.assign(new Error(genericAuthError('INVALID_CREDENTIALS')), { code: 'INVALID_CREDENTIALS' });
  }

  const user = result.rows[0];
  const valid = await bcrypt.compare(password, user.password_hash);
  if (!valid) {
    throw Object.assign(new Error(genericAuthError('INVALID_CREDENTIALS')), { code: 'INVALID_CREDENTIALS' });
  }

  return { user: { id: user.id, email: user.email, displayName: user.display_name }, token: signToken(user.id, user.email) };
}

// ──────────────────────────── Google Sign-In ────────────────────────────

async function googleSignIn(idToken) {
  // Stage 3.3.2: verify the ID token via Google — never trust a JSON payload from the client.
  const profile = await verifyGoogleIdToken(idToken);
  const { email, googleId, displayName } = profile;

  // Look up by google_id first, then by email (link accounts).
  let result = await query(
    'SELECT id, email, display_name FROM users WHERE google_id = $1 OR email = $2',
    [googleId, email]
  );
  if (result.rowCount === 0) {
    // Create a new user with no password (Google-only).
    result = await query(
      `INSERT INTO users (email, google_id, display_name, email_verified)
       VALUES ($1, $2, $3, TRUE)
       RETURNING id, email, display_name`,
      [email, googleId, displayName]
    );
  } else {
    // Optionally link: if user exists by email but no google_id, set it.
    const user = result.rows[0];
    if (!user.google_id) {
      await query('UPDATE users SET google_id = $1 WHERE id = $2', [googleId, user.id]);
    }
  }
  const user = result.rows[0];
  return { user: { id: user.id, email: user.email, displayName: user.display_name }, token: signToken(user.id, user.email) };
}

// ──────────────────────────── Forgot password (OTP) ────────────────────────────

const OTP_VALIDITY_MINUTES = parseInt(process.env.OTP_VALIDITY_MINUTES || '10', 10);
const OTP_RATE_LIMIT_PER_HOUR = parseInt(process.env.OTP_RATE_LIMIT_PER_HOUR || '5', 10);

async function forgotPassword(rawEmail) {
  const email = normalizeEmail(rawEmail);
  if (!email) {
    throw Object.assign(new Error(genericAuthError('MISSING_FIELDS')), { code: 'EMPTY_EMAIL' });
  }

  // Rate-limit: max N OTPs per email per hour.
  const recentCount = await query(
    `SELECT COUNT(*) FROM password_reset_otps
     WHERE email = $1 AND created_at > NOW() - INTERVAL '1 hour'`,
    [email]
  );
  if (parseInt(recentCount.rows[0].count, 10) >= OTP_RATE_LIMIT_PER_HOUR) {
    throw Object.assign(new Error('Too many requests'), { code: 'RATE_LIMITED' });
  }

  // Generate a 6-digit OTP using crypto.randomInt (CSPRNG, not Math.random).
  const otp = String(crypto.randomInt(100000, 1000000));
  const otpHash = await bcrypt.hash(otp, 10);
  const expiresAt = new Date(Date.now() + OTP_VALIDITY_MINUTES * 60 * 1000);

  // Invalidate previous unconsumed OTPs for this email.
  await query(
    `UPDATE password_reset_otps SET consumed = TRUE
     WHERE email = $1 AND consumed = FALSE`,
    [email]
  );

  await query(
    `INSERT INTO password_reset_otps (email, otp_hash, expires_at)
     VALUES ($1, $2, $3)`,
    [email, otpHash, expiresAt]
  );

  // Send the OTP email.
  try {
    await transporter.sendMail({
      from: process.env.SMTP_FROM || `Smart Cost Pro <${process.env.SMTP_USER}>`,
      to: email,
      subject: 'رمز التحقق - Smart Cost Pro',
      text: `رمز التحقق الخاص بك: ${otp}\n\nالرمز صالح لمدة ${OTP_VALIDITY_MINUTES} دقائق فقط.\n\nSmart Cost Pro Team`,
      html: `
        <div dir="rtl" style="font-family: Segoe UI, Tahoma, sans-serif; max-width: 500px; margin: 0 auto; padding: 24px;">
          <h2 style="color: #1a5a8a; margin-bottom: 24px;">Smart Cost Pro</h2>
          <p>مرحباً،</p>
          <p>رمز التحقق الخاص بك هو:</p>
          <div style="text-align: center; margin: 24px 0;">
            <span style="font-size: 36px; font-weight: bold; color: #1a5a8a; letter-spacing: 8px; padding: 16px 32px; background: #f5f8fb; border-radius: 12px; display: inline-block;">
              ${otp}
            </span>
          </div>
          <p>الرمز صالح لمدة <strong>${OTP_VALIDITY_MINUTES} دقائق</strong> فقط ولا يمكن استخدامه مرة أخرى.</p>
          <p style="color: #666; font-size: 12px;">إذا لم تطلب هذا الرمز، يمكنك تجاهل هذه الرسالة.</p>
          <hr style="margin: 24px 0; border: none; border-top: 1px solid #eee;">
          <p style="color: #999; font-size: 11px;">Smart Cost Pro Team</p>
        </div>
      `,
    });
  } catch (e) {
    console.error('Failed to send OTP email:', e);
    // Don't fail the request — the OTP is in the DB and the dev-mode
    // app can read it via the debug endpoint if needed.
  }

  // Stage 3.3.2 fix: only return the OTP in development mode (strict check).
  // The previous code `!== 'production'` leaked the OTP in any non-prod environment.
  if (process.env.NODE_ENV === 'development') {
    return { otp };
  }
  return { otp: null };
}

// ──────────────────────────── Verify OTP + Reset password ────────────────────────────

const MAX_OTP_ATTEMPTS = 5;

async function verifyOtpAndResetPassword(rawEmail, otp, newPassword) {
  const email = normalizeEmail(rawEmail);
  if (!email || !otp || !newPassword) {
    throw Object.assign(new Error(genericAuthError('MISSING_FIELDS')), { code: 'MISSING_FIELDS' });
  }
  if (newPassword.length < 6) {
    throw Object.assign(new Error(genericAuthError('WEAK_PASSWORD')), { code: 'WEAK_PASSWORD' });
  }

  const result = await query(
    `SELECT id, otp_hash, expires_at, consumed, attempts
     FROM password_reset_otps
     WHERE email = $1 AND consumed = FALSE
     ORDER BY created_at DESC
     LIMIT 1`,
    [email]
  );
  if (result.rowCount === 0) {
    throw Object.assign(new Error('لم يُطلب رمز لهذا البريد'), { code: 'OTP_NOT_FOUND' });
  }

  const otpRow = result.rows[0];

  if (new Date() > otpRow.expires_at) {
    await query('UPDATE password_reset_otps SET consumed = TRUE WHERE id = $1', [otpRow.id]);
    throw Object.assign(new Error('انتهت صلاحية الرمز'), { code: 'OTP_EXPIRED' });
  }

  if (otpRow.attempts >= MAX_OTP_ATTEMPTS) {
    await query('UPDATE password_reset_otps SET consumed = TRUE WHERE id = $1', [otpRow.id]);
    throw Object.assign(new Error('محاولات كثيرة خاطئة'), { code: 'OTP_USED' });
  }

  const valid = await bcrypt.compare(otp, otpRow.otp_hash);
  if (!valid) {
    await query('UPDATE password_reset_otps SET attempts = attempts + 1 WHERE id = $1', [otpRow.id]);
    throw Object.assign(new Error('الرمز غير صحيح'), { code: 'OTP_INVALID' });
  }

  await query('UPDATE password_reset_otps SET consumed = TRUE WHERE id = $1', [otpRow.id]);

  const passwordHash = await bcrypt.hash(newPassword, 12);
  const userResult = await query(
    'UPDATE users SET password_hash = $1 WHERE email = $2 RETURNING id',
    [passwordHash, email]
  );
  if (userResult.rowCount === 0) {
    throw Object.assign(new Error('المستخدم غير موجود'), { code: 'USER_NOT_FOUND' });
  }

  return { success: true };
}

// ──────────────────────────── Cleanup ────────────────────────────

async function cleanupExpiredOtps() {
  const result = await query(
    `DELETE FROM password_reset_otps WHERE expires_at < NOW() - INTERVAL '1 hour'`
  );
  return result.rowCount;
}

module.exports = {
  signToken,
  verifyToken,
  register,
  login,
  googleSignIn,
  forgotPassword,
  verifyOtpAndResetPassword,
  cleanupExpiredOtps,
};
