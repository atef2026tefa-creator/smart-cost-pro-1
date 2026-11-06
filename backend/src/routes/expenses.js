// src/routes/expenses.js
// CRUD for the unified expenses table — all scoped by req.userId (from JWT).

const express = require('express');
const router = express.Router();
const { query } = require('../config/database');
const auth = require('../middleware/auth');

// All routes here are protected — require a valid JWT.
router.use(auth);

// GET /api/expenses?from=YYYY-MM-DD&to=YYYY-MM-DD&type=house
router.get('/', async (req, res) => {
  try {
    const { from, to, type } = req.query;
    const params = [req.userId];
    let sql = `SELECT * FROM expenses WHERE user_id = $1`;
    if (from) { sql += ` AND expense_date >= $${params.length + 1}`; params.push(from); }
    if (to)   { sql += ` AND expense_date <= $${params.length + 1}`; params.push(to); }
    if (type) { sql += ` AND type = $${params.length + 1}`; params.push(type); }
    sql += ` ORDER BY expense_date DESC, created_at DESC`;

    const result = await query(sql, params);
    res.json(result.rows);
  } catch (e) {
    res.status(500).json({ error: 'LIST_FAILED', message: e.message });
  }
});

// POST /api/expenses
router.post('/', async (req, res) => {
  try {
    const { title, amount, type, category, expenseDate, paymentMethod, notes, detailsJson } = req.body;
    const result = await query(
      `INSERT INTO expenses
         (user_id, title, amount, type, category, expense_date, payment_method, notes, details_json)
       VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9)
       RETURNING *`,
      [req.userId, title, amount, type, category, expenseDate, paymentMethod || 'cash', notes, detailsJson]
    );
    res.status(201).json(result.rows[0]);
  } catch (e) {
    res.status(400).json({ error: 'CREATE_FAILED', message: e.message });
  }
});

// PUT /api/expenses/:id
router.put('/:id', async (req, res) => {
  try {
    const { title, amount, type, category, expenseDate, paymentMethod, notes, detailsJson } = req.body;
    const result = await query(
      `UPDATE expenses SET
         title = $1, amount = $2, type = $3, category = $4,
         expense_date = $5, payment_method = $6, notes = $7, details_json = $8
       WHERE id = $9 AND user_id = $10
       RETURNING *`,
      [title, amount, type, category, expenseDate, paymentMethod, notes, detailsJson, req.params.id, req.userId]
    );
    if (result.rowCount === 0) return res.status(404).json({ error: 'NOT_FOUND' });
    res.json(result.rows[0]);
  } catch (e) {
    res.status(400).json({ error: 'UPDATE_FAILED', message: e.message });
  }
});

// DELETE /api/expenses/:id
router.delete('/:id', async (req, res) => {
  try {
    const result = await query(
      `DELETE FROM expenses WHERE id = $1 AND user_id = $2`,
      [req.params.id, req.userId]
    );
    if (result.rowCount === 0) return res.status(404).json({ error: 'NOT_FOUND' });
    res.json({ success: true });
  } catch (e) {
    res.status(400).json({ error: 'DELETE_FAILED', message: e.message });
  }
});

module.exports = router;
