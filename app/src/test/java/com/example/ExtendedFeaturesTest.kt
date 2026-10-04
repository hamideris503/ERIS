package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.core.security.BackupManager
import com.example.core.utils.CsvExporter
import com.example.data.local.HesabyarDatabase
import com.example.data.local.entity.BankAccountEntity
import com.example.data.local.entity.BankEntity
import com.example.data.local.entity.TransactionEntity
import com.example.domain.model.BankSupportStatus
import com.example.domain.model.TransactionType
import com.example.parser.NormalizedSms
import com.example.parser.ParseResult
import com.example.parser.ParserRegistry
import com.example.parser.SmsNormalizer
import com.example.parser.TransactionProcessingPipeline
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExtendedFeaturesTest {

    private lateinit var db: HesabyarDatabase

    @Before
    fun setup() {
        runBlocking {
            val context = ApplicationProvider.getApplicationContext<Context>()
            db = Room.inMemoryDatabaseBuilder(context, HesabyarDatabase::class.java)
                .allowMainThreadQueries()
                .build()

            val melli = BankEntity(
                id = 2,
                nameFa = "بانک ملی ایران",
                code = "BMI",
                shortName = "ملی",
                colorHex = "#2563EB",
                smsSenderKeywords = "B.Melli",
                cardPrefixes = "603799",
                supportStatus = BankSupportStatus.SUPPORTED
            )
            db.bankDao().insertBanks(listOf(melli))
        }
    }

    @After
    fun teardown() {
        db.close()
    }

    @Test
    fun parseMelliSmsDeposit() {
        val raw = "بانک ملی ایران\nواریز به: 603799***1234\nمبلغ: ۱,۵۰۰,۰۰۰ ریال\nمانده: ۱۲,۵۰۰,۰۰۰ ریال\nپیگیری: ۷۶۵۴۳۲"
        val normalized = SmsNormalizer.normalizeText(raw)
        val sms = NormalizedSms(
            sender = "B.Melli",
            rawBody = raw,
            body = normalized,
            timestamp = 1700000000000L,
            sha256Hash = "melli_hash_1"
        )

        val result = ParserRegistry.parse(sms)
        assertTrue(result is ParseResult.Success)
        val candidate = (result as ParseResult.Success).candidate

        assertEquals(2L, candidate.bankId)
        assertEquals("بانک ملی ایران", candidate.bankName)
        assertEquals(TransactionType.DEPOSIT, candidate.type)
        assertEquals(150000L, candidate.amount) // 1,500,000 Rial = 150,000 Toman
        assertEquals(1250000L, candidate.balanceAfter)
        assertEquals("765432", candidate.trackingNumber)
    }

    @Test
    fun parseMelliSmsPurchaseWithStore() {
        val raw = "بانک ملی ایران\nخرید با کارت: 603799***1234\nمبلغ: ۸۵,۰۰۰ ریال\nمانده: ۱۲,۰۱۵,۰۰۰ ریال\nفروشگاه: هایپراستار\nپیگیری: ۱۲۳۸۹۰"
        val normalized = SmsNormalizer.normalizeText(raw)
        val sms = NormalizedSms(
            sender = "B.Melli",
            rawBody = raw,
            body = normalized,
            timestamp = 1700000000000L,
            sha256Hash = "melli_hash_2"
        )

        val result = ParserRegistry.parse(sms)
        assertTrue(result is ParseResult.Success)
        val candidate = (result as ParseResult.Success).candidate

        assertEquals(TransactionType.PURCHASE, candidate.type)
        assertEquals(8500L, candidate.amount)
        assertEquals("هایپراستار", candidate.storeName)
    }

    @Test
    fun encryptedBackupAndRestoreRoundtrip() {
        val payload = """{"accounts":[{"title":"ملت جاری","balance":5000000}],"transactions":[{"amount":250000}]}"""
        val password = "StrongMasterPassword123!".toCharArray()

        val encrypted = BackupManager.encrypt(payload, password)
        assertTrue(encrypted.isNotBlank())

        val decrypted = BackupManager.decrypt(encrypted, password)
        assertEquals(payload, decrypted)
    }

    @Test
    fun csvExportContainsHeadersAndRows() {
        val txList = listOf(
            TransactionEntity(
                id = 1,
                type = TransactionType.PURCHASE,
                amount = 75000L,
                description = "خرید افق کوروش",
                dateTime = 1700000000000L,
                trackingNumber = "554433"
            )
        )
        val csv = CsvExporter.exportToCsv(txList)
        assertTrue(csv.contains("تاریخ شمسی,ساعت,نوع تراکنش,مبلغ (تومان)"))
        assertTrue(csv.contains("خرید افق کوروش"))
        assertTrue(csv.contains("75000"))
        assertTrue(csv.contains("554433"))
    }

    @Test
    fun internalTransferDetection_identifiesTransferBetweenAccounts() = runBlocking {
        // Account 1: Mellat
        val acc1 = BankAccountEntity(id = 1, bankId = 1, accountTitle = "ملت", currentBalance = 20000000L)
        // Account 2: Melli
        val acc2 = BankAccountEntity(id = 2, bankId = 2, accountTitle = "ملی", currentBalance = 5000000L)
        db.bankAccountDao().insertAccount(acc1)
        db.bankAccountDao().insertAccount(acc2)

        val timestamp = 1700000000000L

        // Outgoing transfer from Mellat (-1,000,000 Toman)
        val txOut = TransactionEntity(
            accountId = 1,
            bankId = 1,
            type = TransactionType.TRANSFER_OUT,
            amount = 1000000L,
            dateTime = timestamp,
            description = "انتقال پایا"
        )
        db.transactionDao().insertTransaction(txOut)

        // Incoming transfer to Melli (+1,000,000 Toman) within 1 minute
        val counterpart = TransactionProcessingPipeline.detectInternalTransfer(
            db = db,
            amount = 1000000L,
            type = TransactionType.TRANSFER_IN,
            timestamp = timestamp + 60000L,
            currentAccountId = 2
        )

        // Must successfully detect counterpart on Account 1!
        assertEquals(1L, counterpart?.accountId)
        assertEquals(1000000L, counterpart?.amount)
    }
}
