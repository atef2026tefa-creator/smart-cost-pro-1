// src/scripts/migrate.js
// Run: npm run migrate
// Creates all tables from sql/schema.sql.

const fs = require('fs');
const path = require('path');
const { pool } = require('../config/database');

(async () => {
  const schemaPath = path.join(__dirname, '..', '..', 'sql', 'schema.sql');
  const schemaSql = fs.readFileSync(schemaPath, 'utf8');
  console.log('Running schema.sql...');
  try {
    await pool.query(schemaSql);
    console.log('✓ Schema applied successfully');
  } catch (e) {
    console.error('✗ Migration failed:', e);
    process.exit(1);
  } finally {
    await pool.end();
  }
})();
