package com.example.parser.banks

import com.example.domain.model.TransactionType
import com.example.parser.BankSmsParser
import com.example.parser.ConfidenceEngine
import com.example.parser.NormalizedSms
import com.example.parser.ParseResult
import com.example.parser.ParsedTransactionCandidate
import com.example.parser.SmsNormalizer
import com.example.parser.ValidationEngine

class MelliParser : BankSmsParser {
    override val bankId: Long = 2L
    override val bankName: String = "بانک ملی ایران"

    private val amountRegex = Regex("""(?:مبلغ|مبلغ:)\s*[:]?\s*([0-9,]+)\s*(ریال|تومان)?""")
    private val balanceRegex = Regex("""(?:مانده|موجودی|مانده:)\s*[:]?\s*([0-9,]+)\s*(ریال|تومان)?""")
    private val cardRegex = Regex("""(?:کارت|به کارت|از کارت|با کارت|به:|از:)\s*([0-9*]{4,19})""")
    private val trackingRegex = Regex("""(?:پیگیری|کد پیگیری|شماره پیگیری)\s*[:]?\s*([0-9]+)""")
    private val storeRegex = Regex("""(?:فروشگاه|پذیرنده)\s*[:]?\s*([^\n\r]+)""")

    override fun canParse(message: NormalizedSms): Boolean {
        val s = message.sender.lowercase()
        val b = message.body
        return s.contains("melli") || b.contains("بانک ملی") || b.contains("بانک‌ملی") ||
                (b.contains("ملی") && (b.contains("واریز") || b.contains("برداشت") || b.contains("خرید") || b.contains("انتقال")))
    }

    override fun parse(message: NormalizedSms): ParseResult {
        val body = message.body

        val type = when {
            body.contains("سود") -> TransactionType.INTEREST
            body.contains("کارمزد") -> TransactionType.FEE
            body.contains("خرید") -> TransactionType.PURCHASE
            body.contains("واریز") -> TransactionType.DEPOSIT
            body.contains("انتقال به") || body.contains("انتقال پایا") -> TransactionType.TRANSFER_OUT
            body.contains("انتقال از") -> TransactionType.TRANSFER_IN
            body.contains("برداشت") -> TransactionType.WITHDRAW
            body.contains("اصلاحیه") || body.contains("برگشت") -> TransactionType.REFUND
            else -> TransactionType.OTHER
        }

        val amountMatch = amountRegex.find(body)
            ?: return ParseResult.Invalid("مبلغ تراکنش بانک ملی در پیامک یافت نشد")

        val amountStr = amountMatch.groupValues[1]
        val unit = amountMatch.groupValues.getOrNull(2) ?: "ریال"
        val isRial = unit.contains("ریال") || !body.contains("تومان")
        val amountInToman = SmsNormalizer.parseAmountToToman(amountStr, isRial)

        if (amountInToman <= 0L) {
            return ParseResult.Invalid("مبلغ نامعتبر است: $amountStr")
        }

        val balanceMatch = balanceRegex.find(body)
        val balanceInToman = balanceMatch?.let {
            val bStr = it.groupValues[1]
            val bUnit = it.groupValues.getOrNull(2) ?: "ریال"
            val bIsRial = bUnit.contains("ریال") || !body.contains("تومان")
            SmsNormalizer.parseAmountToToman(bStr, bIsRial)
        }

        val card = cardRegex.find(body)?.groupValues?.get(1)
        val tracking = trackingRegex.find(body)?.groupValues?.get(1)
        val store = storeRegex.find(body)?.groupValues?.get(1)?.trim()

        val confidence = ConfidenceEngine.calculate(
            hasAmount = true,
            hasType = type != TransactionType.OTHER,
            hasBalance = balanceInToman != null,
            hasTrackingOrRef = tracking != null,
            hasCardOrAccount = card != null
        )

        val candidate = ParsedTransactionCandidate(
            bankId = bankId,
            bankName = bankName,
            type = type,
            amount = amountInToman,
            currency = "TOMAN",
            balanceAfter = balanceInToman,
            dateTime = message.timestamp,
            cardNumber = card,
            trackingNumber = tracking,
            storeName = store,
            description = store ?: type.titleFa,
            confidence = confidence
        )

        return ValidationEngine.validate(candidate)
    }
}
