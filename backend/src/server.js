// src/server.js
// Smart Cost Pro — Express.js backend entry point (Stage 3.3.2 — security hardening).

const express = require('express');
const cors = require('cors');
const helmet = require('helmet');
const morgan = require('morgan');
const dotenv = require('dotenv');

dotenv.config();

const app = express();

// ──────────────────────────── Trust proxy ────────────────────────────
// Stage 3.3.2 fix: behind Caddy, every request has the same remote IP (127.0.0.1),
// which would defeat per-IP rate limiting. trust proxy = 1 makes Express use
// Caddy's X-Forwarded-For header for req.ip and req.connection.remoteAddress.
app.set('trust proxy', 1);

// ──────────────────────────── Security ────────────────────────────
app.use(helmet());

// ──────────────────────────── CORS ────────────────────────────
const allowedOrigins = (process.env.CORS_ORIGINS || '').split(',').map(s => s.trim()).filter(Boolean);
app.use(cors({
  origin(origin, callback) {
    if (!origin || allowedOrigins.length === 0 || allowedOrigins.includes(origin)) {
      callback(null, true);
    } else {
      callback(new Error('Not allowed by CORS'));
    }
  }
}));

// ──────────────────────────── Body parsers ────────────────────────────
app.use(express.json({ limit: '5mb' }));
app.use(express.urlencoded({ extended: true }));

// ──────────────────────────── Logging ────────────────────────────
if (process.env.NODE_ENV !== 'production') {
  app.use(morgan('dev'));
}

// ──────────────────────────── Routes ────────────────────────────
app.get('/health', (req, res) => res.json({ ok: true, version: '1.0.0' }));
app.use('/api/auth', require('./routes/auth'));
app.use('/api/expenses', require('./routes/expenses'));
// Add more routes here: /api/income, /api/certificates, etc.

// ──────────────────────────── 404 + error handlers ────────────────────────────
app.use((req, res) => {
  res.status(404).json({ error: 'NOT_FOUND', message: `Route ${req.path} not found` });
});

// eslint-disable-next-line no-unused-vars
app.use((err, req, res, next) => {
  // Don't leak internal error details in production.
  console.error('Server error:', err);
  const message = process.env.NODE_ENV === 'production'
    ? 'حدث خطأ في الخادم'
    : err.message;
  res.status(500).json({ error: 'SERVER_ERROR', message });
});

// ──────────────────────────── Start ────────────────────────────
const PORT = parseInt(process.env.PORT || '3001', 10);
app.listen(PORT, () => {
  console.log(`Smart Cost Pro backend running on port ${PORT}`);
  console.log(`Environment: ${process.env.NODE_ENV || 'development'}`);
});
