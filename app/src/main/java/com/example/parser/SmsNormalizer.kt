package com.example.parser

import java.security.MessageDigest

object SmsNormalizer {

    private val PERSIAN_DIGITS = charArrayOf('۰', '۱', '۲', '۳', '۴', '۵', '۶', '۷', '۸', '۹')
    private val ARABIC_DIGITS = charArrayOf('٠', '١', '٢', '٣', '٤', '٥', '٦', '٧', '٨', '٩')

    /**
     * Converts Persian and Arabic digits to standard ASCII digits (0-9).
     */
    fun convertDigitsToAscii(input: String): String {
        val chars = input.toCharArray()
        for (i in chars.indices) {
            val c = chars[i]
            when {
                c in '۰'..'۹' -> chars[i] = (c - '۰' + '0'.code).toChar()
                c in '٠'..'٩' -> chars[i] = (c - '٠' + '0'.code).toChar()
            }
        }
        return String(chars)
    }

    /**
     * Normalizes whitespace, removes zero-width spaces, and cleans punctuation.
     */
    fun normalizeText(input: String): String {
        return convertDigitsToAscii(input)
            .replace('\u200C', ' ') // Zero-width non-joiner (نیم‌فاصله)
            .replace('\u200B', ' ') // Zero-width space
            .replace('\u0640', ' ') // Tatweel
            .replace("ي", "ی")       // Arabic Yeh to Persian
            .replace("ك", "ک")       // Arabic Kaf to Persian
            .replace("٫", ".")       // Persian decimal point
            .replace("٬", ",")       // Persian thousands separator
            .replace("،", ",")       // Persian comma
            .replace("\r\n", "\n")
            .replace("\r", "\n")
            .trim()
    }

    /**
     * Extracts pure numeric value from a normalized amount string (handles commas, Tomans/Rials).
     * By default, Hesabyar stores currency in TOMAN.
     * If the text explicitly mentions "ریال" or "IRR", it divides by 10 (converts Rial to Toman).
     */
    fun parseAmountToToman(amountText: String, isRial: Boolean): Long {
        val cleanNumberStr = convertDigitsToAscii(amountText)
            .replace(",", "")
            .replace(" ", "")
            .trim()
        val raw = cleanNumberStr.toLongOrNull() ?: 0L
        return if (isRial) raw / 10L else raw
    }

    /**
     * Generates a stable SHA-256 hash for deduplication.
     */
    fun generateSha256(sender: String, body: String, timestamp: Long): String {
        val normalizedBody = normalizeText(body).replace(Regex("\\s+"), " ")
        val input = "${sender.trim().lowercase()}|$normalizedBody|${timestamp / 60000}" // minute-level grouping
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(input.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }
}
