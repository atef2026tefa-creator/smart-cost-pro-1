// src/config/database.js
// PostgreSQL connection pool — uses the pg library.
// Connection params come from .env (DB_HOST, DB_PORT, etc.)

const { Pool } = require('pg');

const pool = new Pool({
  host: process.env.DB_HOST || 'localhost',
  port: parseInt(process.env.DB_PORT || '5432', 10),
  database: process.env.DB_NAME || 'smartcost',
  user: process.env.DB_USER || 'smartcost_user',
  password: process.env.DB_PASSWORD || '',
  max: 20,                      // max connections in pool
  idleTimeoutMillis: 30000,
  connectionTimeoutMillis: 2000,
});

pool.on('error', (err) => {
  console.error('Unexpected PostgreSQL pool error:', err);
  process.exit(-1);
});

/**
 * Run a query with optional params.
 * @returns {Promise<import('pg').QueryResult>}
 */
async function query(text, params) {
  const start = Date.now();
  const result = await pool.query(text, params);
  const duration = Date.now() - start;
  if (process.env.NODE_ENV !== 'production') {
    console.log('executed query', { text: text.substring(0, 80), duration, rows: result.rowCount });
  }
  return result;
}

/**
 * Get a client from the pool (for transactions).
 */
async function getClient() {
  return pool.connect();
}

module.exports = { pool, query, getClient };
