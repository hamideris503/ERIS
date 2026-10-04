package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.local.dao.AppSettingDao
import com.example.data.local.dao.AssetDao
import com.example.data.local.dao.BankAccountDao
import com.example.data.local.dao.BankDao
import com.example.data.local.dao.BudgetDao
import com.example.data.local.dao.CategoryDao
import com.example.data.local.dao.CheckDao
import com.example.data.local.dao.DebtReceivableDao
import com.example.data.local.dao.FinancialGoalDao
import com.example.data.local.dao.LoanDao
import com.example.data.local.dao.ParserRuleDao
import com.example.data.local.dao.RecurringExpenseDao
import com.example.data.local.dao.SmsMessageDao
import com.example.data.local.dao.TransactionDao
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
import com.example.domain.model.BankSupportStatus
import com.example.domain.model.CategoryType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        BankEntity::class,
        BankAccountEntity::class,
        TransactionEntity::class,
        CategoryEntity::class,
        BudgetEntity::class,
        FinancialGoalEntity::class,
        DebtReceivableEntity::class,
        LoanEntity::class,
        CheckEntity::class,
        AssetEntity::class,
        SmsMessageEntity::class,
        ParserRuleEntity::class,
        AppSettingEntity::class,
        RecurringExpenseEntity::class
    ],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class HesabyarDatabase : RoomDatabase() {

    abstract fun bankDao(): BankDao
    abstract fun bankAccountDao(): BankAccountDao
    abstract fun transactionDao(): TransactionDao
    abstract fun categoryDao(): CategoryDao
    abstract fun budgetDao(): BudgetDao
    abstract fun financialGoalDao(): FinancialGoalDao
    abstract fun debtReceivableDao(): DebtReceivableDao
    abstract fun loanDao(): LoanDao
    abstract fun checkDao(): CheckDao
    abstract fun assetDao(): AssetDao
    abstract fun smsMessageDao(): SmsMessageDao
    abstract fun parserRuleDao(): ParserRuleDao
    abstract fun appSettingDao(): AppSettingDao
    abstract fun recurringExpenseDao(): RecurringExpenseDao

    companion object {
        @Volatile
        private var INSTANCE: HesabyarDatabase? = null

        fun getInstance(context: Context): HesabyarDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    HesabyarDatabase::class.java,
                    "hesabyar_database.db"
                )
                    .fallbackToDestructiveMigration()
                    .addCallback(DatabaseCallback())
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback : Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    CoroutineScope(Dispatchers.IO).launch {
                        prepopulateDefaults(database)
                    }
                }
            }
        }

        suspend fun prepopulateDefaults(db: HesabyarDatabase) {
            // Prepopulate Iranian Banks
            val banks = listOf(
                BankEntity(1, "بانک ملت", "BMLT", "ملت", "#DC2626", "B.Mellat,Mellat,بانک ملت,982000400", "610433", BankSupportStatus.SUPPORTED),
                BankEntity(2, "بانک ملی ایران", "BMI", "ملی", "#2563EB", "B.Melli,Melli,بانک ملی,بانک‌ملی", "603799", BankSupportStatus.PARTIAL),
                BankEntity(3, "بانک صادرات ایران", "BSI", "صادرات", "#1E3A8A", "B.Saderat,Saderat,صادرات,021000", "603769", BankSupportStatus.PARTIAL),
                BankEntity(4, "بانک تجارت", "BTEJ", "تجارت", "#0D9488", "B.Tejarat,Tejarat,بانک تجارت", "585983", BankSupportStatus.PARTIAL),
                BankEntity(5, "بانک سامان", "BSMN", "سامان", "#0284C7", "B.Saman,Saman,بانک سامان", "621986", BankSupportStatus.PARTIAL),
                BankEntity(6, "بانک پاسارگاد", "BPAS", "پاسارگاد", "#D97706", "Pasargad,B.Pasargad,بانک پاسارگاد", "502229", BankSupportStatus.PARTIAL),
                BankEntity(7, "بانک پارسیان", "BPAR", "پارسیان", "#7C3AED", "Parsian,B.Parsian,بانک پارسیان", "622106", BankSupportStatus.PARTIAL),
                BankEntity(8, "بانک رفاه کارگران", "BREF", "رفاه", "#059669", "Refah,B.Refah,بانک رفاه", "589463", BankSupportStatus.PARTIAL),
                BankEntity(9, "بانک سپه", "BSEP", "سپه", "#B91C1C", "Sepah,B.Sepah,بانک سپه", "589210", BankSupportStatus.PARTIAL),
                BankEntity(10, "بانک مسکن", "BMSK", "مسکن", "#EA580C", "Maskan,B.Maskan,بانک مسکن", "628023", BankSupportStatus.PARTIAL),
                BankEntity(11, "بانک کشاورزی", "BKES", "کشاورزی", "#15803D", "Keshavarzi,B.Keshavarzi,بانک کشاورزی", "603770", BankSupportStatus.PARTIAL),
                BankEntity(12, "بانک شهر", "BSHR", "شهر", "#BE123C", "Shahr,B.Shahr,بانک شهر", "504706", BankSupportStatus.PARTIAL),
                BankEntity(13, "بانک رسالت", "BRES", "رسالت", "#047857", "Resalat,B.Resalat,بانک رسالت", "504172", BankSupportStatus.PARTIAL)
            )
            db.bankDao().insertBanks(banks)

            // Prepopulate Categories
            val categories = listOf(
                CategoryEntity(nameFa = "خوراک و رستوران", iconName = "restaurant", colorHex = "#F97316", type = CategoryType.EXPENSE, isDefault = true, sortOrder = 1),
                CategoryEntity(nameFa = "خرید روزمره", iconName = "shopping_cart", colorHex = "#3B82F6", type = CategoryType.EXPENSE, isDefault = true, sortOrder = 2),
                CategoryEntity(nameFa = "حمل و نقل و اسنپ", iconName = "directions_car", colorHex = "#EAB308", type = CategoryType.EXPENSE, isDefault = true, sortOrder = 3),
                CategoryEntity(nameFa = "بنزین و سوخت", iconName = "local_gas_station", colorHex = "#EF4444", type = CategoryType.EXPENSE, isDefault = true, sortOrder = 4),
                CategoryEntity(nameFa = "خانه و اجاره", iconName = "home", colorHex = "#8B5CF6", type = CategoryType.EXPENSE, isDefault = true, sortOrder = 5),
                CategoryEntity(nameFa = "قبوض و شارژ", iconName = "receipt_long", colorHex = "#64748B", type = CategoryType.EXPENSE, isDefault = true, sortOrder = 6),
                CategoryEntity(nameFa = "اینترنت و ارتباطات", iconName = "wifi", colorHex = "#06B6D4", type = CategoryType.EXPENSE, isDefault = true, sortOrder = 7),
                CategoryEntity(nameFa = "سلامت و دارو", iconName = "medical_services", colorHex = "#10B981", type = CategoryType.EXPENSE, isDefault = true, sortOrder = 8),
                CategoryEntity(nameFa = "تفریح و سرگرمی", iconName = "sports_esports", colorHex = "#EC4899", type = CategoryType.EXPENSE, isDefault = true, sortOrder = 9),
                CategoryEntity(nameFa = "پوشاک و زیبایی", iconName = "checkroom", colorHex = "#F43F5E", type = CategoryType.EXPENSE, isDefault = true, sortOrder = 10),
                CategoryEntity(nameFa = "آموزش و کتاب", iconName = "school", colorHex = "#6366F1", type = CategoryType.EXPENSE, isDefault = true, sortOrder = 11),
                CategoryEntity(nameFa = "سفر و اقامت", iconName = "flight", colorHex = "#14B8A6", type = CategoryType.EXPENSE, isDefault = true, sortOrder = 12),
                CategoryEntity(nameFa = "حقوق و دستمزد", iconName = "payments", colorHex = "#10B981", type = CategoryType.INCOME, isDefault = true, sortOrder = 13),
                CategoryEntity(nameFa = "سود بانکی و سرمایه‌گذاری", iconName = "trending_up", colorHex = "#059669", type = CategoryType.INCOME, isDefault = true, sortOrder = 14),
                CategoryEntity(nameFa = "هدیه و پاداش", iconName = "card_giftcard", colorHex = "#84CC16", type = CategoryType.INCOME, isDefault = true, sortOrder = 15),
                CategoryEntity(nameFa = "سایر درآمدها", iconName = "add_circle", colorHex = "#0D9488", type = CategoryType.INCOME, isDefault = true, sortOrder = 16),
                CategoryEntity(nameFa = "سایر هزینه‌ها", iconName = "remove_circle", colorHex = "#94A3B8", type = CategoryType.EXPENSE, isDefault = true, sortOrder = 17)
            )
            db.categoryDao().insertCategories(categories)

            // Prepopulate Recurring Expenses (Subscriptions, Rent, Internet)
            val defaultRecurring = listOf(
                RecurringExpenseEntity(
                    title = "اجاره ماهانه مسکن",
                    amount = 12000000L,
                    categoryId = 5L,
                    dueDayOfMonth = 1,
                    period = "ماهانه",
                    note = "واریز به حساب صاحبخانه"
                ),
                RecurringExpenseEntity(
                    title = "اینترنت ثابت و مودم",
                    amount = 450000L,
                    categoryId = 7L,
                    dueDayOfMonth = 10,
                    period = "ماهانه",
                    note = "بسته اینترنت ۳۰ روزه"
                ),
                RecurringExpenseEntity(
                    title = "شارژ و خدمات ساختمان",
                    amount = 350000L,
                    categoryId = 6L,
                    dueDayOfMonth = 25,
                    period = "ماهانه",
                    note = "واریز به حساب مدیریت ساختمان"
                )
            )
            defaultRecurring.forEach { db.recurringExpenseDao().insertRecurringExpense(it) }
        }
    }
}
