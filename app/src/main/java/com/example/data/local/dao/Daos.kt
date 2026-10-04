package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.AppSettingEntity
import com.example.data.local.entity.AssetEntity
import com.example.data.local.entity.BankAccountEntity
import com.example.data.local.entity.BankEntity
import com.example.data.local.entity.BudgetEntity
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.CheckEntity
import com.example.data.local.entity.DebtReceivableEntity
import com.example.data.local.entity.FinancialGoalEntity
import com.example.data.local.entity.LoanEntity
import com.example.data.local.entity.ParserRuleEntity
import com.example.data.local.entity.RecurringExpenseEntity
import com.example.data.local.entity.SmsMessageEntity
import com.example.data.local.entity.TransactionEntity
import com.example.domain.model.TransactionType
import kotlinx.coroutines.flow.Flow

@Dao
interface BankDao {
    @Query("SELECT * FROM banks ORDER BY nameFa ASC")
    fun getAllBanks(): Flow<List<BankEntity>>

    @Query("SELECT * FROM banks WHERE id = :id LIMIT 1")
    suspend fun getBankById(id: Long): BankEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBanks(banks: List<BankEntity>)
}

@Dao
interface BankAccountDao {
    @Query("SELECT * FROM bank_accounts WHERE isHidden = 0 ORDER BY id ASC")
    fun getActiveAccounts(): Flow<List<BankAccountEntity>>

    @Query("SELECT * FROM bank_accounts ORDER BY id ASC")
    fun getAllAccounts(): Flow<List<BankAccountEntity>>

    @Query("SELECT * FROM bank_accounts WHERE id = :id LIMIT 1")
    suspend fun getAccountById(id: Long): BankAccountEntity?

    @Query("SELECT * FROM bank_accounts WHERE cardNumber = :cardNumber OR accountNumber = :accountNumber LIMIT 1")
    suspend fun findAccountByCardOrAccount(cardNumber: String, accountNumber: String): BankAccountEntity?

    @Query("SELECT * FROM bank_accounts WHERE bankId = :bankId LIMIT 1")
    suspend fun findFirstAccountByBank(bankId: Long): BankAccountEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccount(account: BankAccountEntity): Long

    @Update
    suspend fun updateAccount(account: BankAccountEntity)

    @Delete
    suspend fun deleteAccount(account: BankAccountEntity)

    @Query("UPDATE bank_accounts SET currentBalance = :newBalance WHERE id = :id")
    suspend fun updateBalance(id: Long, newBalance: Long)
}

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY dateTime DESC, id DESC")
    fun getAllTransactions(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE dateTime >= :fromTimestamp AND dateTime <= :toTimestamp ORDER BY dateTime DESC")
    fun getTransactionsInRange(fromTimestamp: Long, toTimestamp: Long): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE accountId = :accountId ORDER BY dateTime DESC")
    fun getTransactionsByAccount(accountId: Long): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE type = :type ORDER BY dateTime DESC")
    fun getTransactionsByType(type: TransactionType): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE id = :id LIMIT 1")
    suspend fun getTransactionById(id: Long): TransactionEntity?

    @Query("SELECT * FROM transactions WHERE sourceSmsHash = :smsHash LIMIT 1")
    suspend fun findBySmsHash(smsHash: String): TransactionEntity?

    @Query("SELECT * FROM transactions WHERE trackingNumber IS NOT NULL AND trackingNumber = :trackingNumber LIMIT 1")
    suspend fun findByTrackingNumber(trackingNumber: String): TransactionEntity?

    @Query("""
        SELECT * FROM transactions 
        WHERE amount = :amount 
          AND type = :type 
          AND abs(dateTime - :timestamp) <= :timeToleranceMs 
        LIMIT 1
    """)
    suspend fun findDuplicateCandidate(
        amount: Long,
        type: TransactionType,
        timestamp: Long,
        timeToleranceMs: Long = 180000L // 3 minutes
    ): TransactionEntity?

    @Query("SELECT * FROM transactions ORDER BY dateTime DESC LIMIT :limit")
    fun getRecentTransactions(limit: Int = 10): Flow<List<TransactionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity): Long

    @Update
    suspend fun updateTransaction(transaction: TransactionEntity)

    @Query("UPDATE transactions SET categoryId = :categoryId WHERE id = :id")
    suspend fun updateTransactionCategory(id: Long, categoryId: Long?)

    @Delete
    suspend fun deleteTransaction(transaction: TransactionEntity)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteTransactionById(id: Long)

    @Query("SELECT COUNT(*) FROM transactions")
    suspend fun countTransactions(): Int

    @Query("SELECT SUM(amount) FROM transactions WHERE type IN ('DEPOSIT', 'TRANSFER_IN', 'REFUND') AND dateTime >= :fromTimestamp AND dateTime <= :toTimestamp")
    fun getTotalIncome(fromTimestamp: Long, toTimestamp: Long): Flow<Long?>

    @Query("SELECT SUM(amount) FROM transactions WHERE type IN ('WITHDRAW', 'PURCHASE', 'TRANSFER_OUT', 'FEE') AND dateTime >= :fromTimestamp AND dateTime <= :toTimestamp")
    fun getTotalExpense(fromTimestamp: Long, toTimestamp: Long): Flow<Long?>

    @Query("SELECT SUM(amount) FROM transactions WHERE categoryId = :categoryId AND type IN ('WITHDRAW', 'PURCHASE', 'TRANSFER_OUT', 'FEE') AND dateTime >= :fromTimestamp")
    suspend fun getCategorySpentSince(categoryId: Long, fromTimestamp: Long): Long?
}

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories ORDER BY sortOrder ASC, id ASC")
    fun getAllCategories(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE id = :id LIMIT 1")
    suspend fun getCategoryById(id: Long): CategoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: CategoryEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategories(categories: List<CategoryEntity>)

    @Update
    suspend fun updateCategory(category: CategoryEntity)

    @Delete
    suspend fun deleteCategory(category: CategoryEntity)
}

@Dao
interface BudgetDao {
    @Query("SELECT * FROM budgets ORDER BY id ASC")
    fun getAllBudgets(): Flow<List<BudgetEntity>>

    @Query("SELECT * FROM budgets WHERE categoryId = :categoryId LIMIT 1")
    suspend fun getBudgetByCategory(categoryId: Long): BudgetEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBudget(budget: BudgetEntity): Long

    @Update
    suspend fun updateBudget(budget: BudgetEntity)

    @Delete
    suspend fun deleteBudget(budget: BudgetEntity)
}

@Dao
interface FinancialGoalDao {
    @Query("SELECT * FROM financial_goals ORDER BY isCompleted ASC, createdAt DESC")
    fun getAllGoals(): Flow<List<FinancialGoalEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoal(goal: FinancialGoalEntity): Long

    @Update
    suspend fun updateGoal(goal: FinancialGoalEntity)

    @Delete
    suspend fun deleteGoal(goal: FinancialGoalEntity)
}

@Dao
interface DebtReceivableDao {
    @Query("SELECT * FROM debts_receivables ORDER BY isSettled ASC, createdAt DESC")
    fun getAllDebtsReceivables(): Flow<List<DebtReceivableEntity>>

    @Query("SELECT * FROM debts_receivables WHERE isReceivable = :isReceivable ORDER BY isSettled ASC, createdAt DESC")
    fun getByType(isReceivable: Boolean): Flow<List<DebtReceivableEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(debtReceivable: DebtReceivableEntity): Long

    @Update
    suspend fun update(debtReceivable: DebtReceivableEntity)

    @Delete
    suspend fun delete(debtReceivable: DebtReceivableEntity)
}

@Dao
interface LoanDao {
    @Query("SELECT * FROM loans ORDER BY isFinished ASC, createdAt DESC")
    fun getAllLoans(): Flow<List<LoanEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLoan(loan: LoanEntity): Long

    @Update
    suspend fun updateLoan(loan: LoanEntity)

    @Delete
    suspend fun deleteLoan(loan: LoanEntity)
}

@Dao
interface CheckDao {
    @Query("SELECT * FROM checks ORDER BY dueDate ASC")
    fun getAllChecks(): Flow<List<CheckEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCheck(check: CheckEntity): Long

    @Update
    suspend fun updateCheck(check: CheckEntity)

    @Delete
    suspend fun deleteCheck(check: CheckEntity)
}

@Dao
interface AssetDao {
    @Query("SELECT * FROM assets ORDER BY estimatedValue DESC")
    fun getAllAssets(): Flow<List<AssetEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAsset(asset: AssetEntity): Long

    @Update
    suspend fun updateAsset(asset: AssetEntity)

    @Delete
    suspend fun deleteAsset(asset: AssetEntity)
}

@Dao
interface SmsMessageDao {
    @Query("SELECT * FROM sms_messages ORDER BY timestamp DESC")
    fun getAllMessages(): Flow<List<SmsMessageEntity>>

    @Query("SELECT * FROM sms_messages WHERE sha256Hash = :hash LIMIT 1")
    suspend fun findByHash(hash: String): SmsMessageEntity?

    @Query("SELECT * FROM sms_messages WHERE status = 'UNRECOGNIZED' OR status = 'NEEDS_REVIEW' ORDER BY timestamp DESC")
    fun getDiagnosticMessages(): Flow<List<SmsMessageEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertSms(sms: SmsMessageEntity): Long

    @Update
    suspend fun updateSms(sms: SmsMessageEntity)
}

@Dao
interface ParserRuleDao {
    @Query("SELECT * FROM parser_rules WHERE isEnabled = 1 ORDER BY priority DESC")
    fun getActiveRules(): Flow<List<ParserRuleEntity>>

    @Query("SELECT * FROM parser_rules ORDER BY priority DESC")
    fun getAllRules(): Flow<List<ParserRuleEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRule(rule: ParserRuleEntity): Long

    @Delete
    suspend fun deleteRule(rule: ParserRuleEntity)
}

@Dao
interface AppSettingDao {
    @Query("SELECT value FROM app_settings WHERE `key` = :key LIMIT 1")
    suspend fun getValue(key: String): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setValue(setting: AppSettingEntity)
}

@Dao
interface RecurringExpenseDao {
    @Query("SELECT * FROM recurring_expenses ORDER BY dueDayOfMonth ASC")
    fun getAllRecurringExpenses(): Flow<List<RecurringExpenseEntity>>

    @Query("SELECT * FROM recurring_expenses WHERE isActive = 1 ORDER BY dueDayOfMonth ASC")
    fun getActiveRecurringExpenses(): Flow<List<RecurringExpenseEntity>>

    @Query("SELECT * FROM recurring_expenses WHERE id = :id")
    suspend fun getRecurringExpenseById(id: Long): RecurringExpenseEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecurringExpense(item: RecurringExpenseEntity): Long

    @Update
    suspend fun updateRecurringExpense(item: RecurringExpenseEntity)

    @Query("UPDATE recurring_expenses SET lastPaidDate = :paidDate WHERE id = :id")
    suspend fun markAsPaid(id: Long, paidDate: Long)

    @Query("UPDATE recurring_expenses SET isActive = :isActive WHERE id = :id")
    suspend fun updateActiveStatus(id: Long, isActive: Boolean)

    @Delete
    suspend fun deleteRecurringExpense(item: RecurringExpenseEntity)

    @Query("DELETE FROM recurring_expenses WHERE id = :id")
    suspend fun deleteRecurringExpenseById(id: Long)

    @Query("SELECT SUM(amount) FROM recurring_expenses WHERE isActive = 1")
    fun getTotalActiveMonthlyExpenses(): Flow<Long?>
}
