package com.example

import com.example.ui.navigation.Screen
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun skeleton_navigationItems_areCorrect() {
        val items = Screen.bottomNavItems
        assertEquals(5, items.size)
        assertEquals("dashboard", Screen.Dashboard.route)
        assertEquals("transactions", Screen.Transactions.route)
        assertEquals("reports", Screen.Reports.route)
        assertEquals("commitments", Screen.Commitments.route)
        assertEquals("more", Screen.More.route)
        assertTrue(items.all { it.title.isNotEmpty() })
    }
}
