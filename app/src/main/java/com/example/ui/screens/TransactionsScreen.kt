package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.utils.FinancialFormatter
import com.example.data.local.entity.BankAccountEntity
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.TransactionEntity
import com.example.domain.model.TransactionType
import com.example.ui.components.CategoryAssignDialog
import com.example.ui.components.MonthlyTransactionSummaryCard
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.TransferBlue
import com.example.ui.viewmodel.DateRangeFilter
import com.example.ui.viewmodel.HesabyarViewModel

@Composable
fun TransactionsScreen(
    viewModel: HesabyarViewModel,
    modifier: Modifier = Modifier
) {
    val transactions by viewModel.filteredTransactions.collectAsStateWithLifecycle()
    val accounts by viewModel.accounts.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedFilter by viewModel.selectedTypeFilter.collectAsStateWithLifecycle()
    val selectedDateFilter by viewModel.selectedDateFilter.collectAsStateWithLifecycle()
    val monthlySummary by viewModel.currentMonthSummary.collectAsStateWithLifecycle()

    var showAddDialog by remember { mutableStateOf(false) }
    var selectedTxForDetails by remember { mutableStateOf<TransactionEntity?>(null) }
    var txForCategoryAssign by remember { mutableStateOf<TransactionEntity?>(null) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("add_transaction_fab")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Add, contentDescription = "ثبت تراکنش")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("ثبت تراکنش", fontWeight = FontWeight.Bold)
                }
            }
        },
        modifier = modifier.fillMaxSize()
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Monthly Summary Dashboard Component
            MonthlyTransactionSummaryCard(summary = monthlySummary)

            Spacer(modifier = Modifier.height(10.dp))

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                placeholder = {
                    Text(
                        "جستجو در عنوان، فروشگاه، مبلغ، کد پیگیری...",
                        style = MaterialTheme.typography.bodySmall
                    )
                },
                leadingIcon = {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = "جستجو",
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.setSearchQuery("") }) {
                            Icon(
                                Icons.Default.Clear,
                                contentDescription = "پاک کردن متن جستجو",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .testTag("transaction_search_input")
            )

            // Active Search feedback badge
            if (searchQuery.isNotBlank()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "نتایج برای «$searchQuery» (${transactions.size} مورد یافت شد)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                    TextButton(
                        onClick = { viewModel.setSearchQuery("") },
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text(
                            text = "حذف فیلتر جستجو",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            // Date Range Filter Chips
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CalendarToday,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "زمان:",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(6.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    contentPadding = PaddingValues(vertical = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(DateRangeFilter.values()) { filter ->
                        val isSelected = selectedDateFilter == filter
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.setDateFilter(filter) },
                            label = { Text(filter.titleFa, style = MaterialTheme.typography.labelSmall) },
                            modifier = Modifier.testTag("date_filter_${filter.name}")
                        )
                    }
                }
            }

            // Transaction Type Filter Chips
            val filterOptions = listOf(
                null to "همه انواع",
                TransactionType.PURCHASE to "خرید",
                TransactionType.DEPOSIT to "واریز",
                TransactionType.WITHDRAW to "برداشت",
                TransactionType.TRANSFER_OUT to "انتقال",
                TransactionType.TRANSFER_INTERNAL to "انتقال خودی",
                TransactionType.FEE to "کارمزد"
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.FilterList,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "نوع:",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(6.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    contentPadding = PaddingValues(vertical = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(filterOptions) { (type, label) ->
                        val isSelected = selectedFilter == type
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.setTypeFilter(if (isSelected) null else type) },
                            label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                            modifier = Modifier.testTag("filter_chip_$label")
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Transaction List
            if (transactions.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Receipt,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = if (searchQuery.isNotBlank()) "تراکنشی با این مشخصات یافت نشد" else "هنوز تراکنشی ثبت نشده است",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "پیامک‌های بانکی جدید به محض دریافت خودکار اینجا لیست می‌شوند.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 80.dp),
                    modifier = Modifier.testTag("transaction_list")
                ) {
                    items(transactions, key = { it.id }) { tx ->
                        TransactionItemCard(
                            tx = tx,
                            categories = categories,
                            onClick = { selectedTxForDetails = tx },
                            onAssignCategory = { txForCategoryAssign = tx },
                            onDelete = { viewModel.deleteTransaction(tx.id) }
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddManualTransactionDialog(
            accounts = accounts,
            categories = categories,
            onDismiss = { showAddDialog = false },
            onSave = { accId, type, amount, desc, catId ->
                viewModel.addManualTransaction(accId, type, amount, desc, catId)
                showAddDialog = false
            }
        )
    }

    selectedTxForDetails?.let { tx ->
        TransactionDetailDialog(
            tx = tx,
            categories = categories,
            onDismiss = { selectedTxForDetails = null },
            onAssignCategory = {
                txForCategoryAssign = tx
            },
            onDelete = {
                viewModel.deleteTransaction(tx.id)
                selectedTxForDetails = null
            }
        )
    }

    txForCategoryAssign?.let { tx ->
        CategoryAssignDialog(
            currentCategoryId = tx.categoryId,
            categories = categories,
            onDismiss = { txForCategoryAssign = null },
            onSelectCategory = { newCatId ->
                viewModel.updateTransactionCategory(tx.id, newCatId)
                txForCategoryAssign = null
                // If details dialog is open for this tx, update it
                if (selectedTxForDetails?.id == tx.id) {
                    selectedTxForDetails = tx.copy(categoryId = newCatId)
                }
            },
            onCreateCustomCategory = { name ->
                viewModel.createCategory(name)
            }
        )
    }
}

@Composable
fun TransactionItemCard(
    tx: TransactionEntity,
    categories: List<CategoryEntity>,
    onClick: () -> Unit,
    onAssignCategory: () -> Unit,
    onDelete: () -> Unit
) {
    val isPositive = tx.type in listOf(TransactionType.DEPOSIT, TransactionType.TRANSFER_IN, TransactionType.REFUND, TransactionType.INTEREST)
    val color = when {
        tx.type == TransactionType.TRANSFER_INTERNAL -> TransferBlue
        isPositive -> IncomeGreen
        tx.type == TransactionType.TRANSFER_OUT -> TransferBlue
        else -> ExpenseRed
    }

    val sign = when {
        tx.type == TransactionType.TRANSFER_INTERNAL -> "⇄"
        isPositive -> "+"
        else -> "-"
    }

    val assignedCategory = categories.firstOrNull { it.id == tx.categoryId }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("transaction_item_${tx.id}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(color.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when {
                                tx.type == TransactionType.TRANSFER_INTERNAL -> Icons.Default.SwapHoriz
                                isPositive -> Icons.Default.ArrowDownward
                                tx.type == TransactionType.TRANSFER_OUT -> Icons.Default.SwapHoriz
                                else -> Icons.Default.ArrowUpward
                            },
                            contentDescription = null,
                            tint = color,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = tx.description.ifBlank { tx.type.titleFa },
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = FinancialFormatter.formatShamsiDate(tx.dateTime),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (tx.isManual) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "• دستی",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "$sign ${FinancialFormatter.formatToman(tx.amount)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = color
                    )
                    if (tx.balanceAfter != null) {
                        Text(
                            text = "مانده: ${FinancialFormatter.formatToman(tx.balanceAfter)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Category Tag & Action Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Category Chip / Tag
                if (assignedCategory != null) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f))
                            .clickable { onAssignCategory() }
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                            .testTag("tx_category_badge_${tx.id}")
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = when {
                                    assignedCategory.nameFa.contains("خوراک") || assignedCategory.nameFa.contains("غذا") -> "🍽️"
                                    assignedCategory.nameFa.contains("خانه") || assignedCategory.nameFa.contains("اجاره") -> "🏠"
                                    assignedCategory.nameFa.contains("حقوق") || assignedCategory.nameFa.contains("دستمزد") -> "💼"
                                    assignedCategory.nameFa.contains("حمل") || assignedCategory.nameFa.contains("اسنپ") -> "🚗"
                                    assignedCategory.nameFa.contains("خرید") -> "🛒"
                                    assignedCategory.nameFa.contains("بنزین") -> "⛽"
                                    assignedCategory.nameFa.contains("سلامت") || assignedCategory.nameFa.contains("دارو") -> "💊"
                                    assignedCategory.nameFa.contains("قبض") -> "🧾"
                                    assignedCategory.nameFa.contains("اینترنت") -> "📱"
                                    assignedCategory.nameFa.contains("سود") || assignedCategory.nameFa.contains("سرمایه") -> "📈"
                                    else -> "🏷️"
                                },
                                style = MaterialTheme.typography.labelSmall
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = assignedCategory.nameFa,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .clickable { onAssignCategory() }
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                            .testTag("tx_add_category_btn_${tx.id}")
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "+ تعیین دسته‌بندی",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (!tx.trackingNumber.isNullOrBlank() || !tx.cardNumber.isNullOrBlank()) {
                        Text(
                            text = listOfNotNull(
                                tx.trackingNumber?.let { "پیگیری: $it" },
                                tx.cardNumber?.let { "کارت: ${it.takeLast(4)}" }
                            ).joinToString(" • "),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                    }

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(24.dp).testTag("delete_tx_${tx.id}")
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "حذف تراکنش",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TransactionDetailDialog(
    tx: TransactionEntity,
    categories: List<CategoryEntity>,
    onDismiss: () -> Unit,
    onAssignCategory: () -> Unit,
    onDelete: () -> Unit
) {
    val assignedCategory = categories.firstOrNull { it.id == tx.categoryId }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth().testTag("transaction_detail_dialog")
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "جزئیات تراکنش بانکی",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "بستن")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                DetailRow("نوع تراکنش", tx.type.titleFa)
                DetailRow("مبلغ", FinancialFormatter.formatToman(tx.amount))
                if (tx.balanceAfter != null) {
                    DetailRow("مانده پس از تراکنش", FinancialFormatter.formatToman(tx.balanceAfter))
                }
                DetailRow("تاریخ و ساعت", FinancialFormatter.formatShamsiDate(tx.dateTime))

                // Interactive Category Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "دسته‌بندی (تگ)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                            .clickable { onAssignCategory() }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = assignedCategory?.nameFa ?: "تعیین نشده (لمس کنید)",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                if (!tx.description.isBlank()) {
                    DetailRow("شرح / فروشگاه", tx.description)
                }
                if (!tx.cardNumber.isNullOrBlank()) {
                    DetailRow("شماره کارت", tx.cardNumber)
                }
                if (!tx.trackingNumber.isNullOrBlank()) {
                    DetailRow("شماره پیگیری", tx.trackingNumber)
                }
                if (!tx.terminalNumber.isNullOrBlank()) {
                    DetailRow("شماره پایانه", tx.terminalNumber)
                }
                DetailRow("روش ثبت", if (tx.isManual) "ثبت دستی توسط کاربر" else "تحلیل خودکار از پیامک بانکی")
                DetailRow("نمره اطمینان هوش پارسر", "${tx.confidence}٪")

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(
                        onClick = onDelete,
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("حذف تراکنش")
                    }

                    Button(onClick = onDismiss) {
                        Text("متوجه شدم")
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailRow(title: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = title, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun AddManualTransactionDialog(
    accounts: List<BankAccountEntity>,
    categories: List<CategoryEntity>,
    onDismiss: () -> Unit,
    onSave: (accountId: Long?, type: TransactionType, amount: Long, desc: String, categoryId: Long?) -> Unit
) {
    var amountStr by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(TransactionType.PURCHASE) }
    var selectedAccountId by remember { mutableStateOf(accounts.firstOrNull()?.id) }
    var selectedCategoryId by remember { mutableStateOf(categories.firstOrNull()?.id) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "ثبت تراکنش دستی جدید",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(16.dp))

                // Type selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(TransactionType.PURCHASE, TransactionType.DEPOSIT, TransactionType.WITHDRAW).forEach { type ->
                        FilterChip(
                            selected = selectedType == type,
                            onClick = { selectedType = type },
                            label = { Text(type.titleFa, style = MaterialTheme.typography.labelSmall) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it.filter { c -> c.isDigit() } },
                    label = { Text("مبلغ به تومان") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().testTag("manual_tx_amount")
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("شرح / نام فروشگاه") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().testTag("manual_tx_desc")
                )

                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("انصراف")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val amount = amountStr.toLongOrNull() ?: 0L
                            if (amount > 0L) {
                                onSave(selectedAccountId, selectedType, amount, description, selectedCategoryId)
                            }
                        },
                        enabled = amountStr.isNotBlank() && (amountStr.toLongOrNull() ?: 0L) > 0L,
                        modifier = Modifier.testTag("save_manual_tx_button")
                    ) {
                        Text("ثبت تراکنش")
                    }
                }
            }
        }
    }
}
