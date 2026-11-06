// src/routes/auth.js
// Auth REST endpoints — wired to the authService.

const express = require('express');
const rateLimit = require('express-rate-limit');
const router = express.Router();
const authService = require('../services/authService');

// ──────────────────────────── Rate limiters ────────────────────────────

const authLimiter = rateLimit({
  windowMs: 15 * 60 * 1000,  // 15 min
  max: 10,
  message: { error: 'RATE_LIMITED', message: 'Too many attempts, please try later' }
});

const otpLimiter = rateLimit({
  windowMs: 60 * 60 * 1000,  // 1 hour
  max: 5,
  message: { error: 'RATE_LIMITED', message: 'Too many OTP requests' }
});

// ──────────────────────────── Routes ────────────────────────────

// POST /api/auth/register
router.post('/register', authLimiter, async (req, res) => {
  try {
    const { email, password, displayName } = req.body;
    const result = await authService.register(email, password, displayName);
    res.status(201).json(result);
  } catch (e) {
    const status = e.code === 'EMAIL_EXISTS' ? 409 : 400;
    res.status(status).json({ error: e.code || 'REGISTER_FAILED', message: e.message });
  }
});

// POST /api/auth/login
router.post('/login', authLimiter, async (req, res) => {
  try {
    const { email, password } = req.body;
    const result = await authService.login(email, password);
    res.json(result);
  } catch (e) {
    const status = (e.code === 'USER_NOT_FOUND' || e.code === 'INVALID_CREDENTIALS') ? 401 : 400;
    res.status(status).json({ error: e.code || 'LOGIN_FAILED', message: e.message });
  }
});

// POST /api/auth/google
router.post('/google', async (req, res) => {
  try {
    const { idToken } = req.body;
    const result = await authService.googleSignIn(idToken);
    res.json(result);
  } catch (e) {
    res.status(400).json({ error: e.code || 'GOOGLE_FAILED', message: e.message });
  }
});

// POST /api/auth/forgot-password
router.post('/forgot-password', otpLimiter, async (req, res) => {
  try {
    const { email } = req.body;
    const result = await authService.forgotPassword(email);
    // In production, the OTP is emailed and `result.otp` is null.
    // In dev, we return the OTP for testing.
    res.json({ success: true, otp: result.otp });
  } catch (e) {
    const status = e.code === 'RATE_LIMITED' ? 429 : 400;
    res.status(status).json({ error: e.code || 'OTP_FAILED', message: e.message });
  }
});

// POST /api/auth/verify-otp-and-reset
router.post('/verify-otp-and-reset', async (req, res) => {
  try {
    const { email, otp, newPassword } = req.body;
    await authService.verifyOtpAndResetPassword(email, otp, newPassword);
    res.json({ success: true });
  } catch (e) {
    const status = e.code === 'OTP_INVALID' ? 400 : 400;
    res.status(status).json({ error: e.code || 'RESET_FAILED', message: e.message });
  }
});

// GET /api/auth/me (protected — needs JWT)
router.get('/me', require('../middleware/auth'), async (req, res) => {
  res.json({ userId: req.userId, email: req.userEmail });
});

module.exports = router;
