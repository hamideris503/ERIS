package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.HesabyarDatabase
import com.example.data.local.entity.BankAccountEntity
import com.example.data.local.entity.BankEntity
import com.example.domain.model.BankSupportStatus
import com.example.parser.NormalizedSms
import com.example.parser.SmsNormalizer
import com.example.parser.TransactionProcessingPipeline
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PipelineAndDuplicateTest {

    private lateinit var db: HesabyarDatabase

    @Before
    fun setup() {
        runBlocking {
            val context = ApplicationProvider.getApplicationContext<Context>()
            db = Room.inMemoryDatabaseBuilder(context, HesabyarDatabase::class.java)
                .allowMainThreadQueries()
                .build()

            // Insert bank
            val bank = BankEntity(
                id = 1,
                nameFa = "بانک ملت",
                code = "BMLT",
                shortName = "ملت",
                colorHex = "#DC2626",
                smsSenderKeywords = "B.Mellat",
                cardPrefixes = "610433",
                supportStatus = BankSupportStatus.SUPPORTED
            )
            db.bankDao().insertBanks(listOf(bank))

            // Insert bank account
            val account = BankAccountEntity(
                id = 10,
                bankId = 1,
                accountTitle = "حساب اصلی",
                accountNumber = "12345678",
                cardNumber = "610433***1234",
                currentBalance = 10000000L
            )
            db.bankAccountDao().insertAccount(account)
        }
    }

    @After
    fun teardown() {
        db.close()
    }

    @Test
    fun duplicateDetection_preventsMultipleTransactions() = runBlocking {
        val raw = "بانک ملت\nواریز به حساب: 12345678\nمبلغ: ۱,۰۰۰,۰۰۰ ریال\nمانده: ۱۱,۰۰۰,۰۰۰ ریال\nپیگیری: ۱۲۳۴۵"
        val normalized = SmsNormalizer.normalizeText(raw)
        val timestamp = 1700000000000L
        val hash = SmsNormalizer.generateSha256("B.Mellat", raw, timestamp)

        val sms = NormalizedSms(
            sender = "B.Mellat",
            rawBody = raw,
            body = normalized,
            timestamp = timestamp,
            sha256Hash = hash
        )

        // 1st time
        val res1 = TransactionProcessingPipeline.processAndSaveSms(db, sms)
        assertFalse(res1.isDuplicate)
        assertEquals("SUCCESS", res1.status)
        assertEquals(10L, res1.matchedAccountId)

        // Check account balance updated: 10,000,000 + 100,000 = 10,100,000 (or set to balanceAfter 1,100,000)
        val acc = db.bankAccountDao().getAccountById(10)
        assertEquals(1100000L, acc?.currentBalance) // balanceAfter from SMS: 11,000,000 Rial = 1,100,000 Toman

        // 2nd time (same SMS)
        val res2 = TransactionProcessingPipeline.processAndSaveSms(db, sms)
        assertTrue(res2.isDuplicate)

        // 3rd to 10th time (same SMS)
        for (i in 3..10) {
            val res = TransactionProcessingPipeline.processAndSaveSms(db, sms)
            assertTrue(res.isDuplicate)
        }

        // Assert exactly 1 transaction in DB
        val totalTxCount = db.transactionDao().countTransactions()
        assertEquals(1, totalTxCount)
    }

    @Test
    fun accountResolutionAndCategoryMatching() = runBlocking {
        val raw = "بانک ملت\nخرید با کارت: 610433***1234\nمبلغ: ۲۰۰,۰۰۰ ریال\nمانده: ۱۰,۹۰۰,۰۰۰ ریال\nفروشگاه: اسنپ\nپیگیری: ۹۹۸۸"
        val normalized = SmsNormalizer.normalizeText(raw)
        val timestamp = 1700000050000L
        val hash = SmsNormalizer.generateSha256("B.Mellat", raw, timestamp)

        val sms = NormalizedSms(
            sender = "B.Mellat",
            rawBody = raw,
            body = normalized,
            timestamp = timestamp,
            sha256Hash = hash
        )

        val res = TransactionProcessingPipeline.processAndSaveSms(db, sms)
        assertFalse(res.isDuplicate)
        assertEquals("SUCCESS", res.status)
        assertEquals(10L, res.matchedAccountId)
        assertEquals(3L, res.matchedCategoryId) // Category 3 = حمل و نقل و اسنپ
    }
}
