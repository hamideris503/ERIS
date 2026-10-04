package com.example.data.repository

import com.example.data.local.HesabyarDatabase
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
import com.example.data.local.entity.SmsMessageEntity
import com.example.data.local.entity.TransactionEntity
import com.example.domain.model.TransactionType
import kotlinx.coroutines.flow.Flow

class HesabyarRepository(private val db: HesabyarDatabase) {

    // Bank & Accounts
    val allBanks: Flow<List<BankEntity>> = db.bankDao().getAllBanks()
    val activeAccounts: Flow<List<BankAccountEntity>> = db.bankAccountDao().getActiveAccounts()
    val allAccounts: Flow<List<BankAccountEntity>> = db.bankAccountDao().getAllAccounts()

    suspend fun getBankById(id: Long): BankEntity? = db.bankDao().getBankById(id)
    suspend fun getAccountById(id: Long): BankAccountEntity? = db.bankAccountDao().getAccountById(id)
    suspend fun insertAccount(account: BankAccountEntity): Long = db.bankAccountDao().insertAccount(account)
    suspend fun updateAccount(account: BankAccountEntity) = db.bankAccountDao().updateAccount(account)
    suspend fun deleteAccount(account: BankAccountEntity) = db.bankAccountDao().deleteAccount(account)

    // Transactions
    val allTransactions: Flow<List<TransactionEntity>> = db.transactionDao().getAllTransactions()
    val recentTransactions: Flow<List<TransactionEntity>> = db.transactionDao().getRecentTransactions(10)

    fun getTransactionsInRange(from: Long, to: Long): Flow<List<TransactionEntity>> =
        db.transactionDao().getTransactionsInRange(from, to)

    fun getTransactionsByType(type: TransactionType): Flow<List<TransactionEntity>> =
        db.transactionDao().getTransactionsByType(type)

    fun getIncomeSum(from: Long, to: Long): Flow<Long?> = db.transactionDao().getTotalIncome(from, to)
    fun getExpenseSum(from: Long, to: Long): Flow<Long?> = db.transactionDao().getTotalExpense(from, to)

    suspend fun getTransactionById(id: Long): TransactionEntity? = db.transactionDao().getTransactionById(id)
    suspend fun insertTransaction(transaction: TransactionEntity): Long {
        val id = db.transactionDao().insertTransaction(transaction)
        // If accountId is present, adjust account balance
        transaction.accountId?.let { accId ->
            db.bankAccountDao().getAccountById(accId)?.let { acc ->
                val newBal = when (transaction.type) {
                    TransactionType.DEPOSIT, TransactionType.TRANSFER_IN, TransactionType.REFUND, TransactionType.INTEREST ->
                        acc.currentBalance + transaction.amount
                    TransactionType.WITHDRAW, TransactionType.PURCHASE, TransactionType.TRANSFER_OUT, TransactionType.FEE ->
                        acc.currentBalance - transaction.amount
                    else -> acc.currentBalance
                }
                db.bankAccountDao().updateBalance(accId, newBal)
            }
        }
        return id
    }

    suspend fun deleteTransaction(id: Long) = db.transactionDao().deleteTransactionById(id)
    suspend fun updateTransactionCategory(id: Long, categoryId: Long?) =
        db.transactionDao().updateTransactionCategory(id, categoryId)

    // Categories
    val allCategories: Flow<List<CategoryEntity>> = db.categoryDao().getAllCategories()
    suspend fun insertCategory(category: CategoryEntity): Long = db.categoryDao().insertCategory(category)

    // Budgets
    val allBudgets: Flow<List<BudgetEntity>> = db.budgetDao().getAllBudgets()
    suspend fun insertBudget(budget: BudgetEntity): Long = db.budgetDao().insertBudget(budget)
    suspend fun updateBudget(budget: BudgetEntity) = db.budgetDao().updateBudget(budget)
    suspend fun getBudgetByCategory(categoryId: Long): BudgetEntity? = db.budgetDao().getBudgetByCategory(categoryId)
    suspend fun getCategorySpentSince(categoryId: Long, fromTimestamp: Long): Long =
        db.transactionDao().getCategorySpentSince(categoryId, fromTimestamp) ?: 0L
    suspend fun deleteBudget(budget: BudgetEntity) = db.budgetDao().deleteBudget(budget)

    // Goals
    val allGoals: Flow<List<FinancialGoalEntity>> = db.financialGoalDao().getAllGoals()
    suspend fun insertGoal(goal: FinancialGoalEntity): Long = db.financialGoalDao().insertGoal(goal)
    suspend fun updateGoal(goal: FinancialGoalEntity) = db.financialGoalDao().updateGoal(goal)
    suspend fun deleteGoal(goal: FinancialGoalEntity) = db.financialGoalDao().deleteGoal(goal)

    // Debts & Receivables
    val allDebtsReceivables: Flow<List<DebtReceivableEntity>> = db.debtReceivableDao().getAllDebtsReceivables()
    fun getDebtsOrReceivables(isReceivable: Boolean): Flow<List<DebtReceivableEntity>> =
        db.debtReceivableDao().getByType(isReceivable)

    suspend fun insertDebtReceivable(item: DebtReceivableEntity): Long = db.debtReceivableDao().insert(item)
    suspend fun updateDebtReceivable(item: DebtReceivableEntity) = db.debtReceivableDao().update(item)
    suspend fun deleteDebtReceivable(item: DebtReceivableEntity) = db.debtReceivableDao().delete(item)

    // Loans
    val allLoans: Flow<List<LoanEntity>> = db.loanDao().getAllLoans()
    suspend fun insertLoan(loan: LoanEntity): Long = db.loanDao().insertLoan(loan)
    suspend fun updateLoan(loan: LoanEntity) = db.loanDao().updateLoan(loan)
    suspend fun deleteLoan(loan: LoanEntity) = db.loanDao().deleteLoan(loan)

    // Checks
    val allChecks: Flow<List<CheckEntity>> = db.checkDao().getAllChecks()
    suspend fun insertCheck(check: CheckEntity): Long = db.checkDao().insertCheck(check)
    suspend fun updateCheck(check: CheckEntity) = db.checkDao().updateCheck(check)
    suspend fun deleteCheck(check: CheckEntity) = db.checkDao().deleteCheck(check)

    // Assets
    val allAssets: Flow<List<AssetEntity>> = db.assetDao().getAllAssets()
    suspend fun insertAsset(asset: AssetEntity): Long = db.assetDao().insertAsset(asset)
    suspend fun deleteAsset(asset: AssetEntity) = db.assetDao().deleteAsset(asset)

    // SMS Messages
    val allSmsMessages: Flow<List<SmsMessageEntity>> = db.smsMessageDao().getAllMessages()
    val diagnosticSmsMessages: Flow<List<SmsMessageEntity>> = db.smsMessageDao().getDiagnosticMessages()
    suspend fun insertSms(sms: SmsMessageEntity): Long = db.smsMessageDao().insertSms(sms)
    suspend fun updateSms(sms: SmsMessageEntity) = db.smsMessageDao().updateSms(sms)

    // Parser Rules
    val activeParserRules: Flow<List<ParserRuleEntity>> = db.parserRuleDao().getActiveRules()
    suspend fun insertParserRule(rule: ParserRuleEntity): Long = db.parserRuleDao().insertRule(rule)

    // Recurring Expenses
    val allRecurringExpenses: Flow<List<com.example.data.local.entity.RecurringExpenseEntity>> = db.recurringExpenseDao().getAllRecurringExpenses()
    val activeRecurringExpenses: Flow<List<com.example.data.local.entity.RecurringExpenseEntity>> = db.recurringExpenseDao().getActiveRecurringExpenses()
    suspend fun insertRecurringExpense(item: com.example.data.local.entity.RecurringExpenseEntity): Long = db.recurringExpenseDao().insertRecurringExpense(item)
    suspend fun updateRecurringExpense(item: com.example.data.local.entity.RecurringExpenseEntity) = db.recurringExpenseDao().updateRecurringExpense(item)
    suspend fun markRecurringExpenseAsPaid(id: Long, date: Long = System.currentTimeMillis()) = db.recurringExpenseDao().markAsPaid(id, date)
    suspend fun updateRecurringExpenseActive(id: Long, isActive: Boolean) = db.recurringExpenseDao().updateActiveStatus(id, isActive)
    suspend fun deleteRecurringExpense(id: Long) = db.recurringExpenseDao().deleteRecurringExpenseById(id)

    // Settings
    suspend fun getSetting(key: String): String? = db.appSettingDao().getValue(key)
    suspend fun setSetting(key: String, value: String) =
        db.appSettingDao().setValue(AppSettingEntity(key, value))
}
