package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.domain.model.AssetType
import com.example.domain.model.BankSupportStatus
import com.example.domain.model.CategoryType
import com.example.domain.model.CheckStatus
import com.example.domain.model.CheckType
import com.example.domain.model.SmsStatus
import com.example.domain.model.TransactionType

@Entity(tableName = "banks")
data class BankEntity(
    @PrimaryKey val id: Long,
    val nameFa: String,
    val code: String,
    val shortName: String,
    val colorHex: String,
    val smsSenderKeywords: String, // Comma separated: e.g. "B.Mellat,Mellat,982000400"
    val cardPrefixes: String,     // e.g. "610433"
    val supportStatus: BankSupportStatus = BankSupportStatus.SUPPORTED
)

@Entity(tableName = "bank_accounts")
data class BankAccountEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val bankId: Long,
    val accountTitle: String,
    val accountNumber: String = "",
    val cardNumber: String = "",
    val iban: String = "",
    val currentBalance: Long = 0L,
    val currency: String = "TOMAN",
    val isExcludedFromNetWorth: Boolean = false,
    val isHidden: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "transactions",
    indices = [
        Index("accountId"),
        Index("bankId"),
        Index("categoryId"),
        Index("dateTime"),
        Index("sourceSmsHash", unique = false),
        Index("trackingNumber")
    ]
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val accountId: Long? = null,
    val bankId: Long? = null,
    val type: TransactionType,
    val amount: Long, // In Tomans (positive integer)
    val currency: String = "TOMAN",
    val dateTime: Long = System.currentTimeMillis(),
    val balanceAfter: Long? = null,
    val categoryId: Long? = null,
    val description: String = "",
    val note: String? = null,
    val sender: String? = null,
    val receiver: String? = null,
    val cardNumber: String? = null,
    val accountNumber: String? = null,
    val referenceNumber: String? = null,
    val trackingNumber: String? = null,
    val terminalNumber: String? = null,
    val sourceSmsId: Long? = null,
    val sourceSmsHash: String? = null,
    val isManual: Boolean = false,
    val isVerified: Boolean = true,
    val confidence: Int = 100,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nameFa: String,
    val iconName: String,
    val colorHex: String,
    val type: CategoryType,
    val isDefault: Boolean = false,
    val sortOrder: Int = 0
)

@Entity(tableName = "budgets")
data class BudgetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val categoryId: Long,
    val amount: Long,
    val period: String = "MONTHLY",
    val thresholdPercent: Int = 80,
    val alertEnabled: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "financial_goals")
data class FinancialGoalEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val targetAmount: Long,
    val currentSavedAmount: Long = 0L,
    val targetDate: Long? = null,
    val colorHex: String = "#059669",
    val isCompleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "debts_receivables")
data class DebtReceivableEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val personName: String,
    val amount: Long,
    val isReceivable: Boolean, // true = طلب (طلبکاریم), false = بدهی (بدهکاریم)
    val dueDate: Long? = null,
    val description: String = "",
    val isSettled: Boolean = false,
    val paidAmount: Long = 0L,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "loans")
data class LoanEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val totalAmount: Long,
    val installmentAmount: Long,
    val totalInstallments: Int,
    val paidInstallments: Int = 0,
    val firstDueDate: Long = System.currentTimeMillis(),
    val dueDayOfMonth: Int = 1,
    val bankName: String? = null,
    val isFinished: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "checks")
data class CheckEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val checkNumber: String,
    val sayadId: String? = null,
    val amount: Long,
    val bankName: String,
    val issuerOrReceiver: String,
    val dueDate: Long,
    val type: CheckType,
    val status: CheckStatus = CheckStatus.PENDING,
    val description: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "assets")
data class AssetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val type: AssetType,
    val estimatedValue: Long,
    val quantity: Double = 1.0,
    val unit: String = "واحد",
    val purchasePrice: Long? = null,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "sms_messages",
    indices = [Index(value = ["sha256Hash"], unique = true)]
)
data class SmsMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sender: String,
    val body: String,
    val timestamp: Long,
    val status: SmsStatus = SmsStatus.PENDING,
    val sha256Hash: String,
    val parsedBankId: Long? = null,
    val parsedTransactionId: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "parser_rules")
data class ParserRuleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val senderPattern: String,
    val bodyKeyword: String,
    val amountRegex: String,
    val balanceRegex: String? = null,
    val transactionType: TransactionType,
    val bankName: String = "",
    val priority: Int = 1,
    val isEnabled: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "app_settings")
data class AppSettingEntity(
    @PrimaryKey val key: String,
    val value: String
)

@Entity(tableName = "recurring_expenses")
data class RecurringExpenseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val amount: Long, // in Tomans
    val categoryId: Long? = null,
    val dueDayOfMonth: Int = 1, // 1 to 31
    val period: String = "ماهانه",
    val note: String? = null,
    val isActive: Boolean = true,
    val lastPaidDate: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)
