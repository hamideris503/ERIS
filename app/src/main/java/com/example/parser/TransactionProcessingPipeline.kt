package com.example.parser

import com.example.data.local.HesabyarDatabase
import com.example.data.local.entity.BankAccountEntity
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.SmsMessageEntity
import com.example.data.local.entity.TransactionEntity
import com.example.domain.model.CategoryType
import com.example.domain.model.SmsStatus
import com.example.domain.model.TransactionType

data class TransactionIngestionResult(
    val isDuplicate: Boolean,
    val transactionId: Long?,
    val matchedAccountId: Long?,
    val matchedCategoryId: Long?,
    val isInternalTransfer: Boolean = false,
    val status: String
)

object TransactionProcessingPipeline {

    /**
     * Checks if a candidate transaction is already present in the database.
     * Criteria:
     * 1. Direct SHA-256 hash match on source SMS.
     * 2. Direct match on trackingNumber (if present and non-empty).
     * 3. Candidate duplicate match: identical bank, amount, transaction type within 3 minutes tolerance.
     */
    suspend fun isDuplicate(
        db: HesabyarDatabase,
        candidate: ParsedTransactionCandidate,
        smsHash: String
    ): Boolean {
        // 1. By SMS Hash
        if (db.transactionDao().findBySmsHash(smsHash) != null) {
            return true
        }

        // 2. By Tracking Number
        if (!candidate.trackingNumber.isNullOrBlank()) {
            val byTracking = db.transactionDao().findByTrackingNumber(candidate.trackingNumber)
            if (byTracking != null) return true
        }

        // 3. By Amount, Type, and Time window (3 minutes)
        val duplicateCandidate = db.transactionDao().findDuplicateCandidate(
            amount = candidate.amount,
            type = candidate.type,
            timestamp = candidate.dateTime,
            timeToleranceMs = 180000L
        )
        return duplicateCandidate != null
    }

    /**
     * Matches transaction to a BankAccount.
     * 1. Matches card number suffix/prefix.
     * 2. Matches account number suffix/prefix.
     * 3. Falls back to first account registered under the same bankId.
     */
    suspend fun resolveAccount(
        db: HesabyarDatabase,
        candidate: ParsedTransactionCandidate
    ): BankAccountEntity? {
        val card = candidate.cardNumber
        val acc = candidate.accountNumber

        if (!card.isNullOrBlank() || !acc.isNullOrBlank()) {
            val account = db.bankAccountDao().findAccountByCardOrAccount(
                cardNumber = card ?: "",
                accountNumber = acc ?: ""
            )
            if (account != null) return account
        }

        // Fallback to first account for this bank
        return db.bankAccountDao().findFirstAccountByBank(candidate.bankId)
    }

    /**
     * Detects category using keyword rules on description, storeName, and transaction type.
     */
    suspend fun resolveCategory(
        db: HesabyarDatabase,
        candidate: ParsedTransactionCandidate
    ): Long? {
        val text = "${candidate.storeName ?: ""} ${candidate.description}".lowercase()

        // Rule-based classification
        val targetCatName = when {
            text.contains("دیجی کالا") || text.contains("digikala") || text.contains("هایپر") || text.contains("فروشگاه") || text.contains("مارکت") -> "خرید روزمره"
            text.contains("اسنپ") || text.contains("تپسی") || text.contains("مترو") || text.contains("اتوبوس") || text.contains("تاکسی") -> "حمل و نقل و اسنپ"
            text.contains("رستوران") || text.contains("کافه") || text.contains("فست فود") || text.contains("نانوا") || text.contains("سوپر") || text.contains("غذا") || text.contains("قنادی") -> "خوراک و رستوران"
            text.contains("اجاره") || text.contains("کرایه") || text.contains("رهن") || text.contains("صاحبخانه") || text.contains("ساختمان") -> "خانه و اجاره"
            text.contains("بنزین") || text.contains("سوخت") || text.contains("جایگاه") || text.contains("پمپ") -> "بنزین و سوخت"
            text.contains("دارو") || text.contains("درمان") || text.contains("بیمارستان") || text.contains("پزشک") || text.contains("مطب") -> "سلامت و دارو"
            text.contains("ایرانسل") || text.contains("همراه اول") || text.contains("رایتل") || text.contains("اینترنت") -> "اینترنت و ارتباطات"
            text.contains("قبض") || text.contains("آب") || text.contains("برق") || text.contains("گاز") -> "قبوض و شارژ"
            candidate.type == TransactionType.INTEREST -> "سود بانکی و سرمایه‌گذاری"
            candidate.type == TransactionType.FEE -> "سایر هزینه‌ها"
            (candidate.type == TransactionType.DEPOSIT || candidate.type == TransactionType.TRANSFER_IN) && (text.contains("حقوق") || text.contains("دستمزد") || text.contains("پاداش") || text.contains("عیدی")) -> "حقوق و دستمزد"
            candidate.type == TransactionType.DEPOSIT -> "سایر درآمدها"
            else -> null
        }

        if (targetCatName != null) {
            return when (targetCatName) {
                "خوراک و رستوران" -> 1L
                "خرید روزمره" -> 2L
                "حمل و نقل و اسنپ" -> 3L
                "بنزین و سوخت" -> 4L
                "خانه و اجاره" -> 5L
                "قبوض و شارژ" -> 6L
                "اینترنت و ارتباطات" -> 7L
                "سلامت و دارو" -> 8L
                "حقوق و دستمزد" -> 13L
                "سود بانکی و سرمایه‌گذاری" -> 14L
                "سایر درآمدها" -> 16L
                "سایر هزینه‌ها" -> 17L
                else -> null
            }
        }

        return null
    }

    /**
     * Internal Transfer Detection:
     * Detects if an incoming and outgoing transaction of identical amount occurred between
     * user accounts within 5 minutes, preventing double counting as income and expense.
     */
    suspend fun detectInternalTransfer(
        db: HesabyarDatabase,
        amount: Long,
        type: TransactionType,
        timestamp: Long,
        currentAccountId: Long?
    ): TransactionEntity? {
        val isIncoming = type in listOf(TransactionType.DEPOSIT, TransactionType.TRANSFER_IN)
        val isOutgoing = type in listOf(TransactionType.WITHDRAW, TransactionType.TRANSFER_OUT)
        if (!isIncoming && !isOutgoing) return null

        val oppositeTypes = if (isIncoming) {
            listOf(TransactionType.WITHDRAW, TransactionType.TRANSFER_OUT)
        } else {
            listOf(TransactionType.DEPOSIT, TransactionType.TRANSFER_IN)
        }

        for (oppType in oppositeTypes) {
            val counterpart = db.transactionDao().findDuplicateCandidate(
                amount = amount,
                type = oppType,
                timestamp = timestamp,
                timeToleranceMs = 300000L // 5 minutes
            )
            if (counterpart != null && counterpart.accountId != currentAccountId) {
                return counterpart
            }
        }
        return null
    }

    /**
     * Executes the full pipeline:
     * Parse -> Duplicate Check -> Account Resolution -> Category Resolution -> Internal Transfer Check -> Save Transaction & Update Balance -> Update SMS status.
     */
    suspend fun processAndSaveSms(
        db: HesabyarDatabase,
        sms: NormalizedSms
    ): TransactionIngestionResult {
        val parseResult = ParserRegistry.parse(sms)
        if (parseResult !is ParseResult.Success && parseResult !is ParseResult.NeedsReview) {
            return TransactionIngestionResult(
                isDuplicate = false,
                transactionId = null,
                matchedAccountId = null,
                matchedCategoryId = null,
                status = "PARSE_FAILED"
            )
        }

        val candidate = when (parseResult) {
            is ParseResult.Success -> parseResult.candidate
            is ParseResult.NeedsReview -> parseResult.candidate
            else -> return TransactionIngestionResult(false, null, null, null, false, "ERROR")
        }

        // Duplicate Check
        if (isDuplicate(db, candidate, sms.sha256Hash)) {
            return TransactionIngestionResult(
                isDuplicate = true,
                transactionId = null,
                matchedAccountId = null,
                matchedCategoryId = null,
                status = "DUPLICATE_IGNORED"
            )
        }

        // Account Resolution
        val matchedAccount = resolveAccount(db, candidate)

        // Category Resolution
        val matchedCategoryId = resolveCategory(db, candidate)

        // Internal Transfer Detection
        val internalTransferCounterpart = detectInternalTransfer(
            db = db,
            amount = candidate.amount,
            type = candidate.type,
            timestamp = candidate.dateTime,
            currentAccountId = matchedAccount?.id
        )

        val finalType = if (internalTransferCounterpart != null) {
            // Update counterpart to TRANSFER_INTERNAL
            db.transactionDao().updateTransaction(
                internalTransferCounterpart.copy(type = TransactionType.TRANSFER_INTERNAL)
            )
            TransactionType.TRANSFER_INTERNAL
        } else {
            candidate.type
        }

        val finalDesc = if (internalTransferCounterpart != null) {
            "انتقال داخلی بین حساب‌های خودی"
        } else {
            candidate.storeName ?: candidate.description
        }

        // Create and Save Transaction
        val transaction = TransactionEntity(
            accountId = matchedAccount?.id,
            bankId = candidate.bankId,
            type = finalType,
            amount = candidate.amount,
            currency = candidate.currency,
            dateTime = candidate.dateTime,
            balanceAfter = candidate.balanceAfter,
            categoryId = matchedCategoryId,
            description = finalDesc,
            cardNumber = candidate.cardNumber,
            accountNumber = candidate.accountNumber,
            trackingNumber = candidate.trackingNumber,
            terminalNumber = candidate.terminalNumber,
            sourceSmsHash = sms.sha256Hash,
            confidence = candidate.confidence,
            isVerified = parseResult is ParseResult.Success,
            isManual = false
        )

        val txId = db.transactionDao().insertTransaction(transaction)

        // Update Account Balance if balance was extracted or adjust by delta
        matchedAccount?.let { acc ->
            val updatedBalance = candidate.balanceAfter ?: run {
                when (finalType) {
                    TransactionType.DEPOSIT, TransactionType.TRANSFER_IN, TransactionType.REFUND, TransactionType.INTEREST ->
                        acc.currentBalance + candidate.amount
                    TransactionType.WITHDRAW, TransactionType.PURCHASE, TransactionType.TRANSFER_OUT, TransactionType.FEE ->
                        acc.currentBalance - candidate.amount
                    else -> acc.currentBalance
                }
            }
            db.bankAccountDao().updateBalance(acc.id, updatedBalance)
        }

        return TransactionIngestionResult(
            isDuplicate = false,
            transactionId = txId,
            matchedAccountId = matchedAccount?.id,
            matchedCategoryId = matchedCategoryId,
            isInternalTransfer = internalTransferCounterpart != null,
            status = "SUCCESS"
        )
    }
}
