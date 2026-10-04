package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.example.domain.model.CheckType
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.viewmodel.HesabyarViewModel

@Composable
fun CommitmentsScreen(
    viewModel: HesabyarViewModel,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabTitles = listOf("طلب و بدهی", "وام و اقساط", "چک‌ها")

    val debtsReceivables by viewModel.debtsReceivables.collectAsStateWithLifecycle()
    val loans by viewModel.loans.collectAsStateWithLifecycle()
    val checks by viewModel.checks.collectAsStateWithLifecycle()

    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("add_commitment_fab")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        when (selectedTab) {
                            0 -> "ثبت طلب / بدهی"
                            1 -> "ثبت وام جدید"
                            else -> "ثبت چک جدید"
                        },
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        },
        modifier = modifier.fillMaxSize()
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                tabTitles.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title, fontWeight = FontWeight.Bold) }
                    )
                }
            }

            when (selectedTab) {
                0 -> DebtsTabContent(debtsReceivables)
                1 -> LoansTabContent(loans)
                2 -> ChecksTabContent(checks)
            }
        }
    }

    if (showAddDialog) {
        when (selectedTab) {
            0 -> AddDebtDialog(
                onDismiss = { showAddDialog = false },
                onAdd = { person, amt, isRec, desc ->
                    viewModel.addDebtReceivable(person, amt, isRec, desc)
                    showAddDialog = false
                }
            )
            1 -> AddLoanDialog(
                onDismiss = { showAddDialog = false },
                onAdd = { title, total, inst, count ->
                    viewModel.addLoan(title, total, inst, count)
                    showAddDialog = false
                }
            )
            2 -> AddCheckDialog(
                onDismiss = { showAddDialog = false },
                onAdd = { num, amt, bank, person, type ->
                    viewModel.addCheck(num, amt, bank, person, type)
                    showAddDialog = false
                }
            )
        }
    }
}

@Composable
fun DebtsTabContent(items: List<com.example.data.local.entity.DebtReceivableEntity>) {
    if (items.isEmpty()) {
        EmptyCommitmentBox("هیچ طلب یا بدهی ثبت نشده است")
    } else {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(items) { item ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(if (item.isReceivable) IncomeGreen.copy(alpha = 0.15f) else ExpenseRed.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Handshake,
                                    contentDescription = null,
                                    tint = if (item.isReceivable) IncomeGreen else ExpenseRed,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(item.personName, fontWeight = FontWeight.Bold)
                                Text(
                                    if (item.isReceivable) "طلب از طرف (باید بپردازد)" else "بدهی شما (باید بپردازید)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Text(
                            text = FinancialFormatter.formatToman(item.amount),
                            fontWeight = FontWeight.Bold,
                            color = if (item.isReceivable) IncomeGreen else ExpenseRed
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LoansTabContent(loans: List<com.example.data.local.entity.LoanEntity>) {
    if (loans.isEmpty()) {
        EmptyCommitmentBox("هیچ وامی ثبت نشده است")
    } else {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(loans) { loan ->
                val progress = if (loan.totalInstallments > 0) {
                    (loan.paidInstallments.toFloat() / loan.totalInstallments).coerceIn(0f, 1f)
                } else 0f

                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(loan.title, fontWeight = FontWeight.Bold)
                            Text(FinancialFormatter.formatToman(loan.totalAmount), fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "${loan.paidInstallments} از ${loan.totalInstallments} قسط پرداخت شده",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                "مبلغ هر قسط: ${FinancialFormatter.formatToman(loan.installmentAmount)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ChecksTabContent(checks: List<com.example.data.local.entity.CheckEntity>) {
    if (checks.isEmpty()) {
        EmptyCommitmentBox("هیچ چکی ثبت نشده است")
    } else {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(checks) { check ->
                val isIssued = check.type == CheckType.ISSUED
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(check.issuerOrReceiver, fontWeight = FontWeight.Bold)
                            Text(
                                "${check.bankName} • چک ${check.type.titleFa}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                "سررسید: ${FinancialFormatter.formatShamsiDate(check.dueDate)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = FinancialFormatter.formatToman(check.amount),
                            fontWeight = FontWeight.Bold,
                            color = if (isIssued) ExpenseRed else IncomeGreen
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyCommitmentBox(message: String) {
    Box(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun AddDebtDialog(
    onDismiss: () -> Unit,
    onAdd: (person: String, amount: Long, isReceivable: Boolean, desc: String) -> Unit
) {
    var person by remember { mutableStateOf("") }
    var amountStr by remember { mutableStateOf("") }
    var isReceivable by remember { mutableStateOf(true) }
    var description by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("ثبت طلب یا بدهی جدید", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = isReceivable,
                        onClick = { isReceivable = true },
                        label = { Text("طلب (دریافتی)") }
                    )
                    FilterChip(
                        selected = !isReceivable,
                        onClick = { isReceivable = false },
                        label = { Text("بدهی (پرداختی)") }
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = person,
                    onValueChange = { person = it },
                    label = { Text("نام شخص یا شرکت") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it },
                    label = { Text("مبلغ به تومان") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(20.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("انصراف") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = {
                        val amt = amountStr.filter { it.isDigit() }.toLongOrNull() ?: 0L
                        if (person.isNotBlank() && amt > 0) {
                            onAdd(person, amt, isReceivable, description)
                        }
                    }) {
                        Text("ثبت")
                    }
                }
            }
        }
    }
}

@Composable
fun AddLoanDialog(
    onDismiss: () -> Unit,
    onAdd: (title: String, total: Long, installment: Long, count: Int) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var totalStr by remember { mutableStateOf("") }
    var instStr by remember { mutableStateOf("") }
    var countStr by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("ثبت وام جدید", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("عنوان وام") }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = totalStr, onValueChange = { totalStr = it }, label = { Text("مبلغ کل وام (تومان)") }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = instStr, onValueChange = { instStr = it }, label = { Text("مبلغ هر قسط (تومان)") }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = countStr, onValueChange = { countStr = it }, label = { Text("تعداد اقساط") }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(16.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("انصراف") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = {
                        val total = totalStr.filter { it.isDigit() }.toLongOrNull() ?: 0L
                        val inst = instStr.filter { it.isDigit() }.toLongOrNull() ?: 0L
                        val count = countStr.filter { it.isDigit() }.toIntOrNull() ?: 12
                        if (title.isNotBlank()) onAdd(title, total, inst, count)
                    }) {
                        Text("ثبت وام")
                    }
                }
            }
        }
    }
}

@Composable
fun AddCheckDialog(
    onDismiss: () -> Unit,
    onAdd: (num: String, amt: Long, bank: String, person: String, type: CheckType) -> Unit
) {
    var checkNum by remember { mutableStateOf("") }
    var amountStr by remember { mutableStateOf("") }
    var bank by remember { mutableStateOf("بانک ملت") }
    var person by remember { mutableStateOf("") }
    var checkType by remember { mutableStateOf(CheckType.ISSUED) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("ثبت چک جدید", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(12.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = checkType == CheckType.ISSUED,
                        onClick = { checkType = CheckType.ISSUED },
                        label = { Text("صادره (پرداختی)") }
                    )
                    FilterChip(
                        selected = checkType == CheckType.RECEIVED,
                        onClick = { checkType = CheckType.RECEIVED },
                        label = { Text("وارده (دریافتی)") }
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(value = person, onValueChange = { person = it }, label = { Text("در وجه / صادرکننده") }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = amountStr, onValueChange = { amountStr = it }, label = { Text("مبلغ چک (تومان)") }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = checkNum, onValueChange = { checkNum = it }, label = { Text("شماره چک یا شناسه صیادی") }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(16.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("انصراف") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = {
                        val amt = amountStr.filter { it.isDigit() }.toLongOrNull() ?: 0L
                        if (person.isNotBlank() && amt > 0) {
                            onAdd(checkNum, amt, bank, person, checkType)
                        }
                    }) {
                        Text("ثبت چک")
                    }
                }
            }
        }
    }
}
