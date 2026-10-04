package com.example

import com.example.domain.model.TransactionType
import com.example.parser.BankDetector
import com.example.parser.NormalizedSms
import com.example.parser.ParseResult
import com.example.parser.ParserRegistry
import com.example.parser.SmsNormalizer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MellatParserTest {

    @Test
    fun detectMellatBank() {
        val sms = NormalizedSms(
            sender = "B.Mellat",
            rawBody = "",
            body = "بانک ملت\nواریز مبلغ: 100,000 ریال",
            timestamp = 1700000000000L,
            sha256Hash = "hash1"
        )
        val result = BankDetector.detect(sms)
        assertEquals(1L, result?.bankId)
        assertEquals("بانک ملت", result?.bankNameFa)
    }

    @Test
    fun parseMellatDeposit() {
        val raw = "بانک ملت\nواریز به حساب: 12345678\nمبلغ: ۲,۵۰۰,۰۰۰ ریال\nمانده: ۱۴,۰۰۰,۰۰۰ ریال\nپیگیری: ۹۸۷۶۵۴"
        val normalized = SmsNormalizer.normalizeText(raw)
        val sms = NormalizedSms(
            sender = "B.Mellat",
            rawBody = raw,
            body = normalized,
            timestamp = 1700000000000L,
            sha256Hash = "hash2"
        )

        val result = ParserRegistry.parse(sms)
        assertTrue(result is ParseResult.Success)
        val candidate = (result as ParseResult.Success).candidate

        assertEquals(TransactionType.DEPOSIT, candidate.type)
        assertEquals(250000L, candidate.amount) // 2,500,000 Rial = 250,000 Toman
        assertEquals(1400000L, candidate.balanceAfter) // 14,000,000 Rial = 1,400,000 Toman
        assertEquals("12345678", candidate.accountNumber)
        assertEquals("987654", candidate.trackingNumber)
        assertTrue(candidate.confidence >= 85)
    }

    @Test
    fun parseMellatPurchaseWithStore() {
        val raw = "بانک ملت\nخرید با کارت: 610433***1234\nمبلغ: ۱۲۰,۰۰۰ ریال\nمانده: ۱۳,۳۸۰,۰۰۰ ریال\nفروشگاه: دیجی کالا\nپیگیری: ۸۸۷۷۶۶"
        val normalized = SmsNormalizer.normalizeText(raw)
        val sms = NormalizedSms(
            sender = "+982000400",
            rawBody = raw,
            body = normalized,
            timestamp = 1700000000000L,
            sha256Hash = "hash3"
        )

        val result = ParserRegistry.parse(sms)
        assertTrue(result is ParseResult.Success)
        val candidate = (result as ParseResult.Success).candidate

        assertEquals(TransactionType.PURCHASE, candidate.type)
        assertEquals(12000L, candidate.amount) // 120,000 Rial = 12,000 Toman
        assertEquals(1338000L, candidate.balanceAfter)
        assertEquals("دیجی کالا", candidate.storeName)
        assertEquals("887766", candidate.trackingNumber)
    }

    @Test
    fun parseMellatFee() {
        val raw = "بانک ملت\nکسر کارمزد\nمبلغ: ۱۲,۰۰۰ ریال\nاز حساب: 12345678\nمانده: ۱۳,۳۶۸,۰۰۰ ریال\nپیگیری: ۱۱۲۲۳۳"
        val normalized = SmsNormalizer.normalizeText(raw)
        val sms = NormalizedSms(
            sender = "B.Mellat",
            rawBody = raw,
            body = normalized,
            timestamp = 1700000000000L,
            sha256Hash = "hash4"
        )

        val result = ParserRegistry.parse(sms)
        assertTrue(result is ParseResult.Success)
        val candidate = (result as ParseResult.Success).candidate
        assertEquals(TransactionType.FEE, candidate.type)
        assertEquals(1200L, candidate.amount)
    }
}
