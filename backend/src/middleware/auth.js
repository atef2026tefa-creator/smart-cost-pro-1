// src/middleware/auth.js
// JWT authentication middleware — protects routes that need a logged-in user.

const { verifyToken } = require('../services/authService');

function authMiddleware(req, res, next) {
  const header = req.headers.authorization || '';
  const token = header.startsWith('Bearer ') ? header.substring(7) : null;

  if (!token) {
    return res.status(401).json({ error: 'NO_TOKEN', message: 'Authentication required' });
  }

  try {
    const decoded = verifyToken(token);
    req.userId = decoded.userId;
    req.userEmail = decoded.email;
    next();
  } catch (e) {
    return res.status(401).json({ error: 'INVALID_TOKEN', message: 'Token is invalid or expired' });
  }
}

module.exports = authMiddleware;
