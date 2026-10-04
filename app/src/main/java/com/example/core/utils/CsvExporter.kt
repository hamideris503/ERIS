package com.example.core.utils

import com.example.data.local.entity.TransactionEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CsvExporter {

    private val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.US)

    /**
     * Converts a list of transactions into a standard UTF-8 CSV string with BOM for Excel compatibility.
     */
    fun exportToCsv(transactions: List<TransactionEntity>): String {
        val sb = StringBuilder()
        // UTF-8 BOM for Persian characters in Excel
        sb.append('\uFEFF')
        sb.append("تاریخ شمسی,ساعت,نوع تراکنش,مبلغ (تومان),دسته‌بندی,شرح / فروشگاه,شماره کارت,کد پیگیری,مانده پس از تراکنش,ثبت دستی\n")

        transactions.forEach { tx ->
            val dateStr = FinancialFormatter.formatShamsiDate(tx.dateTime)
            val timeStr = timeFormat.format(Date(tx.dateTime))
            val typeStr = tx.type.titleFa
            val amountStr = tx.amount.toString()
            val catStr = tx.categoryId?.toString() ?: "سایر"
            val descStr = sanitizeCsv(tx.description)
            val cardStr = tx.cardNumber ?: ""
            val trackStr = tx.trackingNumber ?: ""
            val balanceStr = tx.balanceAfter?.toString() ?: ""
            val isManualStr = if (tx.isManual) "بله" else "خیر"

            sb.append("$dateStr,$timeStr,$typeStr,$amountStr,$catStr,$descStr,$cardStr,$trackStr,$balanceStr,$isManualStr\n")
        }

        return sb.toString()
    }

    private fun sanitizeCsv(value: String): String {
        return if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            "\"${value.replace("\"", "\"\"")}\""
        } else {
            value
        }
    }
}
