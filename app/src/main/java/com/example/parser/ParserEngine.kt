package com.example.parser

import com.example.domain.model.TransactionType

data class NormalizedSms(
    val sender: String,
    val rawBody: String,
    val body: String,
    val timestamp: Long,
    val sha256Hash: String
)

data class ParsedTransactionCandidate(
    val bankId: Long,
    val bankName: String,
    val type: TransactionType,
    val amount: Long, // In Tomans
    val currency: String = "TOMAN",
    val balanceAfter: Long? = null,
    val dateTime: Long = System.currentTimeMillis(),
    val cardNumber: String? = null,
    val accountNumber: String? = null,
    val trackingNumber: String? = null,
    val referenceNumber: String? = null,
    val terminalNumber: String? = null,
    val senderName: String? = null,
    val receiverName: String? = null,
    val storeName: String? = null,
    val description: String = "",
    val confidence: Int = 100
)

sealed class ParseResult {
    data class Success(val candidate: ParsedTransactionCandidate) : ParseResult()
    data class NeedsReview(val candidate: ParsedTransactionCandidate, val reason: String) : ParseResult()
    data class Unsupported(val reason: String) : ParseResult()
    data class Invalid(val reason: String) : ParseResult()
}

interface BankSmsParser {
    val bankId: Long
    val bankName: String
    fun canParse(message: NormalizedSms): Boolean
    fun parse(message: NormalizedSms): ParseResult
}

object ConfidenceEngine {
    const val THRESHOLD_AUTO_ACCEPT = 85
    const val THRESHOLD_REVIEW = 60

    fun calculate(
        hasAmount: Boolean,
        hasType: Boolean,
        hasBalance: Boolean,
        hasTrackingOrRef: Boolean,
        hasCardOrAccount: Boolean
    ): Int {
        var score = 0
        if (hasAmount) score += 35
        if (hasType) score += 25
        if (hasBalance) score += 20
        if (hasTrackingOrRef) score += 10
        if (hasCardOrAccount) score += 10
        return score.coerceIn(0, 100)
    }
}

object ValidationEngine {
    fun validate(candidate: ParsedTransactionCandidate): ParseResult {
        if (candidate.amount <= 0L) {
            return ParseResult.Invalid("مبلغ تراکنش باید بزرگتر از صفر باشد")
        }
        if (candidate.confidence < ConfidenceEngine.THRESHOLD_REVIEW) {
            return ParseResult.NeedsReview(candidate, "اطمینان تحلیل پیامک کمتر از حد نصاب است")
        }
        if (candidate.confidence < ConfidenceEngine.THRESHOLD_AUTO_ACCEPT) {
            return ParseResult.NeedsReview(candidate, "برخی اطلاعات نظیر شماره پیگیری یا مانده در دسترس نیست")
        }
        return ParseResult.Success(candidate)
    }
}
