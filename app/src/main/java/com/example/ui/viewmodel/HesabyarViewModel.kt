package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.notification.NotificationHelper
import com.example.data.local.HesabyarDatabase
import com.example.data.local.entity.AssetEntity
import com.example.data.local.entity.BankAccountEntity
import com.example.data.local.entity.BudgetEntity
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.CheckEntity
import com.example.data.local.entity.DebtReceivableEntity
import com.example.data.local.entity.FinancialGoalEntity
import com.example.data.local.entity.LoanEntity
import com.example.data.local.entity.RecurringExpenseEntity
import com.example.data.local.entity.SmsMessageEntity
import com.example.data.local.entity.TransactionEntity
import com.example.data.repository.HesabyarRepository
import com.example.domain.model.AssetType
import com.example.domain.model.CategoryType
import com.example.domain.model.CheckStatus
import com.example.domain.model.CheckType
import com.example.domain.model.TransactionType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

enum class DateRangeFilter(val titleFa: String) {
    ALL("همه"),
    TODAY("امروز"),
    THIS_WEEK("این هفته"),
    THIS_MONTH("این ماه"),
    LAST_3_MONTHS("۳ ماه اخیر")
}

data class MonthlySummary(
    val monthName: String = "ماه جاری",
    val totalIncome: Long = 0L,
    val totalExpense: Long = 0L,
    val netBalance: Long = 0L,
    val transactionCount: Int = 0
)

data class DashboardSummary(
    val netWorth: Long = 0L,
    val totalIncome: Long = 0L,
    val totalExpense: Long = 0L,
    val netSavings: Long = 0L,
    val accountCount: Int = 0,
    val transactionCount: Int = 0
)

class HesabyarViewModel(application: Application) : AndroidViewModel(application) {

    private val db = HesabyarDatabase.getInstance(application)
    val repository = HesabyarRepository(db)

    init {
        // Prepopulate defaults if first run
        viewModelScope.launch {
            HesabyarDatabase.prepopulateDefaults(db)
        }
    }

    // Accounts & Banks
    val accounts: StateFlow<List<BankAccountEntity>> = repository.allAccounts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val banks = repository.allBanks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Transactions
    val allTransactions: StateFlow<List<TransactionEntity>> = repository.allTransactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentTransactions: StateFlow<List<TransactionEntity>> = repository.recentTransactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Filter & Search states
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _selectedTypeFilter = MutableStateFlow<TransactionType?>(null)
    val selectedTypeFilter = _selectedTypeFilter.asStateFlow()

    private val _selectedDateFilter = MutableStateFlow(DateRangeFilter.ALL)
    val selectedDateFilter = _selectedDateFilter.asStateFlow()

    private fun getStartTimestampFor(filter: DateRangeFilter): Long {
        val cal = Calendar.getInstance()
        return when (filter) {
            DateRangeFilter.ALL -> 0L
            DateRangeFilter.TODAY -> {
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                cal.timeInMillis
            }
            DateRangeFilter.THIS_WEEK -> {
                cal.add(Calendar.DAY_OF_YEAR, -7)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                cal.timeInMillis
            }
            DateRangeFilter.THIS_MONTH -> {
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                cal.timeInMillis
            }
            DateRangeFilter.LAST_3_MONTHS -> {
                cal.add(Calendar.MONTH, -3)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                cal.timeInMillis
            }
        }
    }

    val filteredTransactions: StateFlow<List<TransactionEntity>> = combine(
        allTransactions,
        _searchQuery,
        _selectedTypeFilter,
        _selectedDateFilter
    ) { txs, query, typeFilter, dateFilter ->
        val minTime = getStartTimestampFor(dateFilter)
        val cleanQuery = query.trim()
        val queryEng = com.example.parser.SmsNormalizer.convertDigitsToAscii(cleanQuery).replace(",", "").replace("،", "")

        txs.filter { tx ->
            val matchesDate = tx.dateTime >= minTime
            val matchesType = typeFilter == null || tx.type == typeFilter
            val matchesQuery = cleanQuery.isBlank() ||
                    tx.description.contains(cleanQuery, ignoreCase = true) ||
                    tx.type.titleFa.contains(cleanQuery, ignoreCase = true) ||
                    (tx.trackingNumber?.contains(cleanQuery) == true) ||
                    (tx.cardNumber?.contains(cleanQuery) == true) ||
                    (tx.terminalNumber?.contains(cleanQuery) == true) ||
                    tx.amount.toString().contains(queryEng) ||
                    (tx.balanceAfter?.toString()?.contains(queryEng) == true)
            matchesDate && matchesType && matchesQuery
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setDateFilter(filter: DateRangeFilter) {
        _selectedDateFilter.value = filter
    }

    // Categories
    val categories: StateFlow<List<CategoryEntity>> = repository.allCategories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Budgets
    val budgets: StateFlow<List<BudgetEntity>> = repository.allBudgets
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val categoryBudgetProgressList: StateFlow<List<CategoryBudgetProgress>> = combine(
        budgets,
        categories,
        allTransactions
    ) { budgetList, catList, txList ->
        val cal = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val monthStart = cal.timeInMillis

        val thisMonthExpenses = txList.filter {
            it.dateTime >= monthStart && it.type in listOf(
                TransactionType.WITHDRAW,
                TransactionType.PURCHASE,
                TransactionType.TRANSFER_OUT,
                TransactionType.FEE
            )
        }

        budgetList.map { budget ->
            val cat = catList.firstOrNull { it.id == budget.categoryId }
            val spent = thisMonthExpenses.filter { it.categoryId == budget.categoryId }.sumOf { it.amount }
            val pct = if (budget.amount > 0) ((spent.toDouble() / budget.amount.toDouble()) * 100).toInt() else 0
            CategoryBudgetProgress(
                budgetId = budget.id,
                categoryId = budget.categoryId,
                categoryName = cat?.nameFa ?: "دسته‌بندی",
                categoryIcon = cat?.iconName ?: "category",
                categoryColorHex = cat?.colorHex ?: "#3B82F6",
                budgetLimit = budget.amount,
                spentThisMonth = spent,
                progressPercent = pct,
                thresholdPercent = budget.thresholdPercent,
                alertEnabled = budget.alertEnabled,
                isExceeded = spent >= budget.amount
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Goals
    val goals: StateFlow<List<FinancialGoalEntity>> = repository.allGoals
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Debts & Receivables
    val debtsReceivables: StateFlow<List<DebtReceivableEntity>> = repository.allDebtsReceivables
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Loans
    val loans: StateFlow<List<LoanEntity>> = repository.allLoans
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Checks
    val checks: StateFlow<List<CheckEntity>> = repository.allChecks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Assets
    val assets: StateFlow<List<AssetEntity>> = repository.allAssets
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // SMS Diagnostic
    val diagnosticSms: StateFlow<List<SmsMessageEntity>> = repository.diagnosticSmsMessages
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Recurring Expenses
    val recurringExpenses: StateFlow<List<RecurringExpenseEntity>> = repository.allRecurringExpenses
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalActiveRecurringAmount: StateFlow<Long> = recurringExpenses.map { list ->
        list.filter { it.isActive }.sumOf { it.amount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    // Dashboard Summary
    val dashboardSummary: StateFlow<DashboardSummary> = combine(
        accounts,
        allTransactions,
        assets
    ) { accList, txList, assetList ->
        val bankTotal = accList.filter { !it.isExcludedFromNetWorth }.sumOf { it.currentBalance }
        val assetTotal = assetList.sumOf { it.estimatedValue }
        val netWorth = bankTotal + assetTotal

        val income = txList.filter {
            it.type in listOf(TransactionType.DEPOSIT, TransactionType.TRANSFER_IN, TransactionType.REFUND, TransactionType.INTEREST)
        }.sumOf { it.amount }

        val expense = txList.filter {
            it.type in listOf(TransactionType.WITHDRAW, TransactionType.PURCHASE, TransactionType.TRANSFER_OUT, TransactionType.FEE)
        }.sumOf { it.amount }

        DashboardSummary(
            netWorth = netWorth,
            totalIncome = income,
            totalExpense = expense,
            netSavings = income - expense,
            accountCount = accList.size,
            transactionCount = txList.size
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardSummary())

    val currentMonthSummary: StateFlow<MonthlySummary> = allTransactions.map { txList ->
        val cal = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val monthStart = cal.timeInMillis

        // Fallback to all if month has no transactions yet or filter for this month
        val monthTxs = txList.filter { it.dateTime >= monthStart }
        val activeTxs = if (monthTxs.isNotEmpty()) monthTxs else txList

        val income = activeTxs.filter {
            it.type in listOf(TransactionType.DEPOSIT, TransactionType.TRANSFER_IN, TransactionType.REFUND, TransactionType.INTEREST)
        }.sumOf { it.amount }

        val expense = activeTxs.filter {
            it.type in listOf(TransactionType.WITHDRAW, TransactionType.PURCHASE, TransactionType.TRANSFER_OUT, TransactionType.FEE)
        }.sumOf { it.amount }

        MonthlySummary(
            monthName = "ماه جاری",
            totalIncome = income,
            totalExpense = expense,
            netBalance = income - expense,
            transactionCount = activeTxs.size
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MonthlySummary())

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setTypeFilter(type: TransactionType?) {
        _selectedTypeFilter.value = type
    }

    // Add Manual Transaction
    fun addManualTransaction(
        accountId: Long?,
        type: TransactionType,
        amount: Long,
        description: String,
        categoryId: Long?
    ) {
        viewModelScope.launch {
            val tx = TransactionEntity(
                accountId = accountId,
                bankId = 1L,
                type = type,
                amount = amount,
                description = description,
                categoryId = categoryId,
                isManual = true,
                isVerified = true,
                confidence = 100,
                dateTime = System.currentTimeMillis()
            )
            repository.insertTransaction(tx)
        }
    }

    // Add Bank Account
    fun addBankAccount(title: String, initialBalance: Long, cardNumber: String, bankId: Long = 1L) {
        viewModelScope.launch {
            val acc = BankAccountEntity(
                bankId = bankId,
                accountTitle = title,
                cardNumber = cardNumber,
                currentBalance = initialBalance
            )
            repository.insertAccount(acc)
        }
    }

    // Add Budget
    fun addBudget(categoryId: Long, amount: Long) {
        viewModelScope.launch {
            val budget = BudgetEntity(
                categoryId = categoryId,
                amount = amount
            )
            repository.insertBudget(budget)
        }
    }

    // Add Goal
    fun addGoal(title: String, targetAmount: Long) {
        viewModelScope.launch {
            val goal = FinancialGoalEntity(
                title = title,
                targetAmount = targetAmount,
                currentSavedAmount = 0L
            )
            repository.insertGoal(goal)
        }
    }

    // Add Debt / Receivable
    fun addDebtReceivable(personName: String, amount: Long, isReceivable: Boolean, description: String) {
        viewModelScope.launch {
            val item = DebtReceivableEntity(
                personName = personName,
                amount = amount,
                isReceivable = isReceivable,
                description = description
            )
            repository.insertDebtReceivable(item)
        }
    }

    // Add Loan
    fun addLoan(title: String, totalAmount: Long, installmentAmount: Long, totalInstallments: Int) {
        viewModelScope.launch {
            val loan = LoanEntity(
                title = title,
                totalAmount = totalAmount,
                installmentAmount = installmentAmount,
                totalInstallments = totalInstallments
            )
            repository.insertLoan(loan)
        }
    }

    // Add Check
    fun addCheck(checkNumber: String, amount: Long, bankName: String, person: String, type: CheckType) {
        viewModelScope.launch {
            val check = CheckEntity(
                checkNumber = checkNumber,
                amount = amount,
                bankName = bankName,
                issuerOrReceiver = person,
                dueDate = System.currentTimeMillis() + 86400000L * 30, // 30 days default
                type = type
            )
            repository.insertCheck(check)
        }
    }

    // Add Asset
    fun addAsset(title: String, type: AssetType, value: Long) {
        viewModelScope.launch {
            val asset = AssetEntity(
                title = title,
                type = type,
                estimatedValue = value
            )
            repository.insertAsset(asset)
        }
    }

    fun deleteTransaction(id: Long) {
        viewModelScope.launch {
            repository.deleteTransaction(id)
        }
    }

    fun updateTransactionCategory(transactionId: Long, categoryId: Long?) {
        viewModelScope.launch {
            repository.updateTransactionCategory(transactionId, categoryId)
        }
    }

    fun createCategory(
        name: String,
        type: com.example.domain.model.CategoryType = com.example.domain.model.CategoryType.EXPENSE,
        iconName: String = "category",
        colorHex: String = "#3F51B5"
    ) {
        viewModelScope.launch {
            val cat = CategoryEntity(
                nameFa = name,
                type = type,
                iconName = iconName,
                colorHex = colorHex,
                isDefault = false
            )
            repository.insertCategory(cat)
        }
    }

    fun addRecurringExpense(
        title: String,
        amount: Long,
        categoryId: Long?,
        dueDayOfMonth: Int,
        note: String? = null
    ) {
        viewModelScope.launch {
            val item = RecurringExpenseEntity(
                title = title,
                amount = amount,
                categoryId = categoryId,
                dueDayOfMonth = dueDayOfMonth,
                note = note
            )
            repository.insertRecurringExpense(item)
        }
    }

    fun markRecurringExpenseAsPaid(expenseId: Long, autoCreateTransaction: Boolean = true, accountId: Long? = null) {
        viewModelScope.launch {
            repository.markRecurringExpenseAsPaid(expenseId)
            if (autoCreateTransaction) {
                val expense = recurringExpenses.value.firstOrNull { it.id == expenseId }
                if (expense != null) {
                    addManualTransaction(
                        accountId = accountId ?: accounts.value.firstOrNull()?.id,
                        type = TransactionType.PURCHASE,
                        amount = expense.amount,
                        description = "پرداخت هزینه دوره‌ای: ${expense.title}",
                        categoryId = expense.categoryId
                    )
                }
            }
        }
    }

    fun toggleRecurringExpense(id: Long, currentActive: Boolean) {
        viewModelScope.launch {
            repository.updateRecurringExpenseActive(id, !currentActive)
        }
    }

    fun deleteRecurringExpense(id: Long) {
        viewModelScope.launch {
            repository.deleteRecurringExpense(id)
        }
    }

    fun saveCategoryBudget(categoryId: Long, amount: Long, thresholdPercent: Int = 100, alertEnabled: Boolean = true) {
        viewModelScope.launch {
            val existing = repository.getBudgetByCategory(categoryId)
            if (existing != null) {
                repository.updateBudget(existing.copy(amount = amount, thresholdPercent = thresholdPercent, alertEnabled = alertEnabled))
            } else {
                repository.insertBudget(
                    BudgetEntity(
                        categoryId = categoryId,
                        amount = amount,
                        thresholdPercent = thresholdPercent,
                        alertEnabled = alertEnabled
                    )
                )
            }
        }
    }

    fun toggleBudgetAlert(budgetId: Long, enabled: Boolean) {
        viewModelScope.launch {
            val budget = budgets.value.firstOrNull { it.id == budgetId }
            if (budget != null) {
                repository.updateBudget(budget.copy(alertEnabled = enabled))
            }
        }
    }

    fun deleteBudgetById(budgetId: Long) {
        viewModelScope.launch {
            val budget = budgets.value.firstOrNull { it.id == budgetId }
            if (budget != null) {
                repository.deleteBudget(budget)
            }
        }
    }

    fun checkAndNotifyBudgetExceeded(context: Context, categoryId: Long) {
        viewModelScope.launch {
            val budget = repository.getBudgetByCategory(categoryId) ?: return@launch
            if (!budget.alertEnabled || budget.amount <= 0) return@launch

            val cal = java.util.Calendar.getInstance().apply {
                set(java.util.Calendar.DAY_OF_MONTH, 1)
                set(java.util.Calendar.HOUR_OF_DAY, 0)
                set(java.util.Calendar.MINUTE, 0)
                set(java.util.Calendar.SECOND, 0)
                set(java.util.Calendar.MILLISECOND, 0)
            }
            val monthStart = cal.timeInMillis
            val spent = repository.getCategorySpentSince(categoryId, monthStart)
            val thresholdAmount = (budget.amount * budget.thresholdPercent) / 100

            if (spent >= thresholdAmount) {
                val percentage = ((spent.toDouble() / budget.amount.toDouble()) * 100).toInt()
                val categoryName = categories.value.firstOrNull { it.id == categoryId }?.nameFa ?: "دسته‌بندی"
                NotificationHelper.sendBudgetExceededNotification(
                    context = context,
                    categoryId = categoryId,
                    categoryName = categoryName,
                    spentAmount = spent,
                    budgetLimit = budget.amount,
                    percentage = percentage
                )
            }
        }
    }

    fun testBudgetNotification(context: Context, categoryId: Long) {
        viewModelScope.launch {
            val category = categories.value.firstOrNull { it.id == categoryId }
            val catName = category?.nameFa ?: "خوراک و رستوران"
            val budget = repository.getBudgetByCategory(categoryId)
            val limit = budget?.amount ?: 2500000L
            val simulatedSpent = (limit * 1.08).toLong()
            NotificationHelper.sendBudgetExceededNotification(
                context = context,
                categoryId = categoryId,
                categoryName = catName,
                spentAmount = simulatedSpent,
                budgetLimit = limit,
                percentage = 108
            )
        }
    }
}

data class CategoryBudgetProgress(
    val budgetId: Long,
    val categoryId: Long,
    val categoryName: String,
    val categoryIcon: String,
    val categoryColorHex: String,
    val budgetLimit: Long,
    val spentThisMonth: Long,
    val progressPercent: Int,
    val thresholdPercent: Int,
    val alertEnabled: Boolean,
    val isExceeded: Boolean
)
