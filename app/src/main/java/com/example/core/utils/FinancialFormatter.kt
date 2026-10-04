package com.example.core.utils

import java.text.NumberFormat
import java.util.Locale

object FinancialFormatter {

    private val numberFormat = NumberFormat.getNumberInstance(Locale.US)

    /**
     * Formats amount to Persian/English standard with thousands separators.
     * e.g., 2500000 -> "2,500,000 تومان"
     */
    fun formatToman(amount: Long): String {
        return "${numberFormat.format(amount)} تومان"
    }

    /**
     * Converts a timestamp to an approximate Shamsi (Jalali) date string.
     * Standard Jalali calculation algorithm from epoch millis.
     */
    fun formatShamsiDate(timestamp: Long): String {
        val daysSinceEpoch = (timestamp / (1000 * 60 * 60 * 24)).toInt() + 719468 // days since 0000-03-01 Gregorian
        
        // Convert to Gregorian year, month, day
        var era = (if (daysSinceEpoch >= 0) daysSinceEpoch else daysSinceEpoch - 146096) / 146097
        var doe = daysSinceEpoch - era * 146097
        var yoe = (doe - doe / 1020 + doe / 1460 - doe / 36524) / 365
        var y = yoe + era * 400
        var doy = doe - (365 * yoe + yoe / 4 - yoe / 100)
        var mp = (5 * doy + 2) / 153
        var d = doy - (153 * mp + 2) / 5 + 1
        var m = mp + (if (mp < 10) 3 else -9)
        if (m <= 2) y += 1

        // Gregorian to Jalali (Persian)
        val gDaysInMonth = intArrayOf(31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)
        val jDaysInMonth = intArrayOf(31, 31, 31, 31, 31, 31, 30, 30, 30, 30, 30, 29)

        var gy = y - 1600
        var gm = m - 1
        var gd = d - 1

        var gDayNo = 365 * gy + (gy + 3) / 4 - (gy + 99) / 100 + (gy + 399) / 400
        for (i in 0 until gm) gDayNo += gDaysInMonth[i]
        if (gm > 1 && ((gy % 4 == 0 && gy % 100 != 0) || (gy % 400 == 0))) gDayNo++
        gDayNo += gd

        var jDayNo = gDayNo - 79
        val jNp = jDayNo / 12053
        jDayNo %= 12053

        var jy = 979 + 33 * jNp + 4 * (jDayNo / 1461)
        jDayNo %= 1461

        if (jDayNo >= 366) {
            jy += (jDayNo - 1) / 365
            jDayNo = (jDayNo - 1) % 365
        }

        var jm = 0
        while (jm < 11 && jDayNo >= jDaysInMonth[jm]) {
            jDayNo -= jDaysInMonth[jm]
            jm++
        }
        val jd = jDayNo + 1

        val monthNames = arrayOf(
            "فروردین", "اردیبهشت", "خرداد",
            "تیر", "مرداد", "شهریور",
            "مهر", "آبان", "آذر",
            "دی", "بهمن", "اسفند"
        )

        return "$jd ${monthNames[jm]} $jy"
    }

    fun formatShortDate(timestamp: Long): String {
        return formatShamsiDate(timestamp)
    }
}
