package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.HesabyarDatabase
import com.example.data.local.entity.BankAccountEntity
import com.example.data.local.entity.BankEntity
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.TransactionEntity
import com.example.domain.model.BankSupportStatus
import com.example.domain.model.CategoryType
import com.example.domain.model.TransactionType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DatabaseUnitTest {

    private lateinit var db: HesabyarDatabase

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, HesabyarDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun insertAndReadBankAndAccount() = runBlocking {
        val bank = BankEntity(
            id = 1,
            nameFa = "بانک ملت",
            code = "BMLT",
            shortName = "ملت",
            colorHex = "#DC2626",
            smsSenderKeywords = "B.Mellat,Mellat",
            cardPrefixes = "610433",
            supportStatus = BankSupportStatus.SUPPORTED
        )
        db.bankDao().insertBanks(listOf(bank))

        val loadedBank = db.bankDao().getBankById(1)
        assertNotNull(loadedBank)
        assertEquals("بانک ملت", loadedBank?.nameFa)

        val account = BankAccountEntity(
            bankId = 1,
            accountTitle = "حساب جاری ملت",
            accountNumber = "12345678",
            cardNumber = "6104337890123456",
            currentBalance = 15000000L
        )
        val accId = db.bankAccountDao().insertAccount(account)
        val loadedAcc = db.bankAccountDao().getAccountById(accId)
        assertNotNull(loadedAcc)
        assertEquals(15000000L, loadedAcc?.currentBalance)
    }

    @Test
    fun insertAndQueryTransactionsAndDuplicate() = runBlocking {
        val transaction = TransactionEntity(
            bankId = 1,
            type = TransactionType.PURCHASE,
            amount = 250000L,
            description = "خرید سوپرمارکت",
            trackingNumber = "998877",
            sourceSmsHash = "abc123hash",
            dateTime = 1000000L
        )
        val txId = db.transactionDao().insertTransaction(transaction)
        val loaded = db.transactionDao().getTransactionById(txId)
        assertNotNull(loaded)
        assertEquals(250000L, loaded?.amount)

        // Test Duplicate Candidate Query
        val duplicate = db.transactionDao().findDuplicateCandidate(
            amount = 250000L,
            type = TransactionType.PURCHASE,
            timestamp = 1000500L,
            timeToleranceMs = 180000L
        )
        assertNotNull(duplicate)
        assertEquals(txId, duplicate?.id)

        // Test Find by Hash
        val byHash = db.transactionDao().findBySmsHash("abc123hash")
        assertNotNull(byHash)
        assertEquals(txId, byHash?.id)
    }

    @Test
    fun insertCategoryAndBudget() = runBlocking {
        val cat = CategoryEntity(
            nameFa = "خوراک",
            iconName = "restaurant",
            colorHex = "#F97316",
            type = CategoryType.EXPENSE
        )
        val catId = db.categoryDao().insertCategory(cat)
        val allCats = db.categoryDao().getAllCategories().first()
        assertEquals(1, allCats.size)
        assertEquals("خوراک", allCats.first().nameFa)
    }
}
