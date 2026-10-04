package com.example

import com.example.parser.SmsNormalizer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class SmsNormalizerTest {

    @Test
    fun convertPersianDigitsToAscii() {
        val persian = "۱۲۳۴۵۶۷۸۹۰"
        val ascii = SmsNormalizer.convertDigitsToAscii(persian)
        assertEquals("1234567890", ascii)
    }

    @Test
    fun convertArabicDigitsToAscii() {
        val arabic = "١٢٣٤٥٦٧٨٩٠"
        val ascii = SmsNormalizer.convertDigitsToAscii(arabic)
        assertEquals("1234567890", ascii)
    }

    @Test
    fun normalizeTextCleansSpecialCharacters() {
        val raw = "واریز: ۱۲٬۵۰۰٬۰۰۰ ریال\nموجودی: ۴۵٬۰۰۰٬۰۰۰ ریال"
        val normalized = SmsNormalizer.normalizeText(raw)
        assertEquals("واریز: 12,500,000 ریال\nموجودی: 45,000,000 ریال", normalized)
    }

    @Test
    fun parseAmountToTomanHandlesRialAndToman() {
        val rialAmount = SmsNormalizer.parseAmountToToman("500,000", isRial = true)
        assertEquals(50000L, rialAmount)

        val tomanAmount = SmsNormalizer.parseAmountToToman("50,000", isRial = false)
        assertEquals(50000L, tomanAmount)
    }

    @Test
    fun sha256HashIsDeterministic() {
        val sender = "B.Mellat"
        val body = "برداشت ۱۰۰,۰۰۰ ریال"
        val timestamp = 1700000000000L

        val hash1 = SmsNormalizer.generateSha256(sender, body, timestamp)
        val hash2 = SmsNormalizer.generateSha256(sender, body, timestamp)
        assertEquals(hash1, hash2)

        val differentSenderHash = SmsNormalizer.generateSha256("B.Melli", body, timestamp)
        assertNotEquals(hash1, differentSenderHash)
    }
}
