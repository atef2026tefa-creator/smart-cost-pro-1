package com.aistudio.smartcostpro.app.export

import com.aistudio.smartcostpro.app.domain.model.Expense
import com.aistudio.smartcostpro.app.domain.model.ExpenseType
import com.aistudio.smartcostpro.app.util.AppLocale
import com.aistudio.smartcostpro.app.util.DateFormatter
import java.io.StringWriter
import java.math.BigDecimal
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

/**
 * CSV exporter.
 *
 * **Stage 3 fixes:**
 *  - Prepends a UTF-8 BOM so Excel on Windows opens the Arabic text correctly
 *    (without the BOM, Excel assumes the CSV is in the system's ANSI codepage
 *    and Arabic characters render as `???`).
 *  - Prefixes any cell starting with `=`, `+`, `-`, `@`, tab or CR with a
 *    single quote `'` to prevent formula injection. Without this, a user
 *    could enter `=cmd|/c calc!A0` as an expense title and Excel would
 *    execute it when the CSV is opened.
 *  - Amounts are formatted with `BigDecimal.toPlainString()` instead of
 *    `Double.toString()` to avoid scientific notation (1.0E7) for amounts
 *    ≥ 10 million.
 */
object CsvExporter {

    /** UTF-8 BOM — signals Excel to decode the file as UTF-8. */
    private const val UTF8_BOM = "\uFEFF"

    /** Column headers — kept English so Excel/LibreOffice open the file directly. */
    private val EXPENSE_HEADERS = listOf(
        "id", "title", "amount", "type", "category", "date", "paymentMethod", "notes"
    )

    /**
     * Builds the CSV string for the given expenses, prefixed with a UTF-8 BOM.
     */
    fun expensesToCsv(expenses: List<Expense>, locale: AppLocale): String {
        val sw = StringWriter()
        sw.append(UTF8_BOM)
        sw.append(EXPENSE_HEADERS.joinToString(",") { escape(it) })
        sw.append("\r\n")
        for (e in expenses) {
            val row = listOf(
                e.id,
                e.title,
                formatAmount(e.amount),
                e.type.code,
                e.category,
                e.date,
                e.paymentMethod.code,
                e.notes.orEmpty()
            )
            sw.append(row.joinToString(",") { escape(it) })
            sw.append("\r\n")
        }
        return sw.toString()
    }

    /**
     * Generic CSV writer — used by the reports screen for arbitrary row sets.
     *
     * Also prepends the UTF-8 BOM and applies formula-injection protection.
     */
    fun writeRows(headers: List<String>, rows: List<List<String>>): String {
        val sw = StringWriter()
        sw.append(UTF8_BOM)
        sw.append(headers.joinToString(",") { escape(it) })
        sw.append("\r\n")
        for (row in rows) {
            sw.append(row.joinToString(",") { escape(it) })
            sw.append("\r\n")
        }
        return sw.toString()
    }

    /**
     * Escapes a single CSV cell per RFC 4180, plus protects against formula
     * injection by prefixing dangerous leading characters with a `'`.
     */
    private fun escape(value: String): String {
        // Formula injection: cells starting with =, +, -, @, tab or CR can
        // trigger Excel to evaluate them as formulas. Prefix with a single
        // quote so Excel shows the literal text instead.
        val safe = if (value.isNotEmpty() && value.first() in "=+-@\t\r") {
            "'$value"
        } else value

        // RFC 4180: wrap in quotes if the cell contains comma, quote, newline.
        return if (safe.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) {
            "\"" + safe.replace("\"", "\"\"") + "\""
        } else {
            safe
        }
    }

    /**
     * Formats an amount as a plain decimal string (no scientific notation).
     *
     * `Double.toString()` switches to `1.0E7` for amounts ≥ 10 million,
     * which Excel reads as text rather than a number. Using BigDecimal
     * keeps the full decimal representation.
     */
    private fun formatAmount(amount: Double): String {
        val symbols = DecimalFormatSymbols.getInstance(Locale.US)
        val formatter = DecimalFormat("#0.00", symbols).apply {
            isGroupingUsed = false
            minimumFractionDigits = 2
            maximumFractionDigits = 2
        }
        return formatter.format(BigDecimal.valueOf(amount).setScale(2, java.math.RoundingMode.HALF_UP))
    }
}

/**
 * HTML exporter — used by the Print button (spec §5.1 / §6 mention print for
 * House / Work / Personal sections).
 *
 * Stage 3 fix: amounts are formatted via `BigDecimal.toPlainString()` to
 * avoid scientific notation in the print output.
 */
object HtmlExporter {

    fun expensesToHtml(
        expenses: List<Expense>,
        title: String,
        locale: AppLocale,
        typeLabel: (ExpenseType) -> String,
        paymentLabel: (String) -> String,
        categoryLabel: (String) -> String
    ): String {
        val sb = StringBuilder()
        sb.append("<!DOCTYPE html><html lang=\"${locale.code}\"><head><meta charset=\"UTF-8\">")
        sb.append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">")
        sb.append("<title>").append(escapeHtml(title)).append("</title>")
        sb.append("<style>")
        sb.append("body{font-family:-apple-system,Segoe UI,Roboto,Arial,sans-serif;margin:24px;direction:${if (locale == AppLocale.Arabic) "rtl" else "ltr"};}")
        sb.append("h1{font-size:22px;margin-bottom:8px;}")
        sb.append("table{border-collapse:collapse;width:100%;font-size:14px;}")
        sb.append("th,td{border:1px solid #ccc;padding:8px;text-align:${if (locale == AppLocale.Arabic) "right" else "left"};}")
        sb.append("th{background:#0F7B5C;color:#fff;font-weight:600;}")
        sb.append("tr:nth-child(even){background:#f5f3ee;}")
        sb.append(".total{font-weight:bold;background:#FFE08A !important;}")
        sb.append("</style></head><body>")
        sb.append("<h1>").append(escapeHtml(title)).append("</h1>")
        sb.append("<table>")
        sb.append("<thead><tr>")
        listOf("title", "amount", "type", "category", "date", "payment", "notes").forEach {
            sb.append("<th>").append(it).append("</th>")
        }
        sb.append("</tr></thead><tbody>")
        for (e in expenses) {
            sb.append("<tr>")
            sb.append("<td>").append(escapeHtml(e.title)).append("</td>")
            sb.append("<td>").append(formatAmount(e.amount)).append("</td>")
            sb.append("<td>").append(escapeHtml(typeLabel(e.type))).append("</td>")
            sb.append("<td>").append(escapeHtml(categoryLabel(e.category))).append("</td>")
            sb.append("<td>").append(e.date).append("</td>")
            sb.append("<td>").append(escapeHtml(paymentLabel(e.paymentMethod.code))).append("</td>")
            sb.append("<td>").append(escapeHtml(e.notes.orEmpty())).append("</td>")
            sb.append("</tr>")
        }
        // Total row
        val total = expenses.sumOf { it.amount }
        sb.append("<tr class=\"total\">")
        sb.append("<td>Total</td>")
        sb.append("<td>").append(formatAmount(total)).append("</td>")
        sb.append("<td colspan=\"5\"></td>")
        sb.append("</tr>")
        sb.append("</tbody></table>")
        sb.append("<p style=\"margin-top:12px;color:#666;font-size:12px;\">Generated ${DateFormatter.todayIso()}</p>")
        sb.append("</body></html>")
        return sb.toString()
    }

    private fun escapeHtml(s: String): String = s
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&#39;")

    private fun formatAmount(amount: Double): String {
        val symbols = java.text.DecimalFormatSymbols.getInstance(java.util.Locale.US)
        val formatter = java.text.DecimalFormat("#0.00", symbols).apply {
            isGroupingUsed = false
            minimumFractionDigits = 2
            maximumFractionDigits = 2
        }
        return formatter.format(java.math.BigDecimal.valueOf(amount).setScale(2, java.math.RoundingMode.HALF_UP))
    }
}

/**
 * File-system helper used by the screen ViewModels to write export files
 * to the app's private `exports/` directory and expose them via the
 * FileProvider declared in `xml/file_paths.xml`.
 */
object ExportFiles {
    const val EXPORTS_DIR = "exports"

    fun writeTextFile(
        context: android.content.Context,
        fileName: String,
        content: String
    ): java.io.File {
        val dir = java.io.File(context.filesDir, EXPORTS_DIR).apply { mkdirs() }
        val file = java.io.File(dir, fileName)
        file.writeText(content, Charsets.UTF_8)
        return file
    }
}
