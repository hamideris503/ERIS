package com.example.parser.banks

import com.example.domain.model.TransactionType
import com.example.parser.BankSmsParser
import com.example.parser.ConfidenceEngine
import com.example.parser.NormalizedSms
import com.example.parser.ParseResult
import com.example.parser.ParsedTransactionCandidate
import com.example.parser.SmsNormalizer
import com.example.parser.ValidationEngine

class MellatParser : BankSmsParser {
    override val bankId: Long = 1L
    override val bankName: String = "بانک ملت"

    private val amountRegex = Regex("""(?:مبلغ|مبلغ:)\s*[:]?\s*([0-9,]+)\s*(ریال|تومان)?""")
    private val balanceRegex = Regex("""(?:مانده|موجودی|مانده:)\s*[:]?\s*([0-9,]+)\s*(ریال|تومان)?""")
    private val cardRegex = Regex("""(?:کارت|با کارت)\s*[:]?\s*([0-9*]{4,19})""")
    private val accountRegex = Regex("""(?:به حساب|از حساب|حساب)\s*[:]?\s*([0-9]{5,15})""")
    private val trackingRegex = Regex("""(?:پیگیری|کد پیگیری|شماره پیگیری)\s*[:]?\s*([0-9]+)""")
    private val storeRegex = Regex("""(?:فروشگاه|پذیرنده)\s*[:]?\s*([^\n\r]+)""")

    override fun canParse(message: NormalizedSms): Boolean {
        val s = message.sender.lowercase()
        val b = message.body
        return s.contains("mellat") || s.contains("2000400") || b.contains("بانک ملت") ||
                (b.contains("ملت") && (b.contains("واریز") || b.contains("برداشت") || b.contains("خرید")))
    }

    override fun parse(message: NormalizedSms): ParseResult {
        val body = message.body

        // Determine transaction type
        val type = when {
            body.contains("سود سپرده") || body.contains("واریز سود") -> TransactionType.INTEREST
            body.contains("کسر کارمزد") || body.contains("کارمزد") -> TransactionType.FEE
            body.contains("خرید با کارت") || body.contains("خرید") -> TransactionType.PURCHASE
            body.contains("واریز به حساب") || body.contains("واریز") -> TransactionType.DEPOSIT
            body.contains("انتقال وجه پایا به") || body.contains("انتقال به") || body.contains("انتقال وجه ساتنا") -> TransactionType.TRANSFER_OUT
            body.contains("انتقال از") || body.contains("پایا از") -> TransactionType.TRANSFER_IN
            body.contains("برداشت از") || body.contains("برداشت") -> TransactionType.WITHDRAW
            body.contains("برگشت وجه") || body.contains("اصلاحیه") -> TransactionType.REFUND
            else -> TransactionType.OTHER
        }

        // Extract amount
        val amountMatch = amountRegex.find(body)
        if (amountMatch == null) {
            return ParseResult.Invalid("مبلغ تراکنش در پیامک یافت نشد")
        }

        val amountStr = amountMatch.groupValues[1]
        val unit = amountMatch.groupValues.getOrNull(2) ?: "ریال"
        val isRial = unit.contains("ریال") || !body.contains("تومان")
        val amountInToman = SmsNormalizer.parseAmountToToman(amountStr, isRial)

        if (amountInToman <= 0L) {
            return ParseResult.Invalid("مبلغ نامعتبر است: $amountStr")
        }

        // Extract balance
        val balanceMatch = balanceRegex.find(body)
        val balanceInToman = balanceMatch?.let {
            val bStr = it.groupValues[1]
            val bUnit = it.groupValues.getOrNull(2) ?: "ریال"
            val bIsRial = bUnit.contains("ریال") || !body.contains("تومان")
            SmsNormalizer.parseAmountToToman(bStr, bIsRial)
        }

        // Card & Account
        val card = cardRegex.find(body)?.groupValues?.get(1)
        val account = accountRegex.find(body)?.groupValues?.get(1)
        val tracking = trackingRegex.find(body)?.groupValues?.get(1)
        val store = storeRegex.find(body)?.groupValues?.get(1)?.trim()

        val confidence = ConfidenceEngine.calculate(
            hasAmount = true,
            hasType = type != TransactionType.OTHER,
            hasBalance = balanceInToman != null,
            hasTrackingOrRef = tracking != null,
            hasCardOrAccount = card != null || account != null
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
            accountNumber = account,
            trackingNumber = tracking,
            storeName = store,
            description = store ?: type.titleFa,
            confidence = confidence
        )

        return ValidationEngine.validate(candidate)
    }
}
