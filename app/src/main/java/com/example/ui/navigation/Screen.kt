package com.example.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    data object Dashboard : Screen("dashboard", "خانه", Icons.Filled.Home)
    data object Transactions : Screen("transactions", "تراکنش‌ها", Icons.Filled.AccountBalanceWallet)
    data object Reports : Screen("reports", "گزارش‌ها", Icons.Filled.Assessment)
    data object Commitments : Screen("commitments", "تعهدات", Icons.Filled.Assignment)
    data object More : Screen("more", "بیشتر", Icons.Filled.MoreHoriz)

    companion object {
        val bottomNavItems: List<Screen>
            get() = listOf(Dashboard, Transactions, Reports, Commitments, More)
    }
}
