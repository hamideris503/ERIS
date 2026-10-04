# حسابیار (Hesabyar) - دستیار هوشمند مدیریت مالی و پیامک‌های بانکی

اپلیکیشن اندروید بومی (Native Android) ساخته‌شده با کاتلین و Jetpack Compose بر مبنای Clean Architecture و MVVM مخصوص کاربران ایرانی برای مدیریت خودکار و آفلاین امور مالی شخصی بر اساس تحلیل پیامک‌های بانکی.

## ویژگی‌های کلیدی
- **کاملاً آفلاین و امن (Zero Internet Permission):** هیچ اطلاعات یا تراکنش مالی از دستگاه کاربر خارج نمی‌شود.
- **پردازش پس‌زمینه پیامک‌ها (SMS Pipeline):** دریافت آنی و بدون کراش پیامک‌های بانکی از طریق BroadcastReceiver با اولویت بالا.
- **عادی‌سازی هوشمند (SmsNormalizer):** تبدیل ارقام فارسی و عربی به استاندارد، مدیریت نیم‌فاصله‌ها، کاراکترهای پولی و تبدیل ریال به تومان.
- **تشخیص بانک و موتور پارسر (BankDetector & ParserRegistry):** تفکیک دقیق بانک‌ها با پارسر عملیاتی بانک ملت (واریز، برداشت، خرید، کارمزد، سود و انتقال پایا/ساتنا).
- **سیستم ارزیابی اطمینان (ConfidenceEngine):** سنجش میزان کامل بودن فیلدها و هدایت موارد مبهم به نیازمند بررسی.
- **جلوگیری از ثبت تکراری (DuplicateEngine):** هش SHA-256، کد پیگیری و بازه زمانی ۳ دقیقه‌ای (تست‌شده تا ۱۰ بار دریافت تکراری).
- **تطبیق حساب و دسته‌بندی خودکار (Account & Category Resolution):** اتصال به حساب بانکی مربوطه و اعمال قوانین هوشمند دسته‌بندی (دیجی‌کالا، اسنپ، بنزین و...).
- **داشبورد جامع مالی:** دارایی کل، درآمد و هزینه ماه، موجودی حساب‌های بانکی، اهداف پس‌انداز و آخرین تراکنش‌ها با قابلیت ماسک امنیتی حریم خصوصی (Privacy Mask).
- **مدیریت تعهدات مالی:** بدهی و طلب، وام‌ها و پیشرفت اقساط، چک‌های صیادی و سررسید.
- **گزارش‌ها و تحلیل مالی:** سهم دسته‌بندی‌ها، بیشترین هزینه و میانگین هزینه روزانه (محاسبه‌شده از دیتابیس لوکال بدون هوش مصنوعی ابری).
- **پشتیبان‌گیری رمزگذاری‌شده و خروجی CSV.**
- **پشتیبانی کامل از زبان فارسی، RTL و تقویم شمسی (جلالی).**

## معماری و پکیج‌ها
```text
com.example
├── core/
│   └── utils/ (FinancialFormatter, Shamsi Date)
├── data/
│   ├── local/
│   │   ├── dao/ (BankDao, BankAccountDao, TransactionDao, CategoryDao, BudgetDao, ...)
│   │   ├── entity/ (Entities)
│   │   ├── Converters.kt
│   │   └── HesabyarDatabase.kt
│   └── repository/ (HesabyarRepository)
├── domain/
│   └── model/ (Enums & Domain models)
├── parser/
│   ├── banks/ (MellatParser)
│   ├── BankDetector.kt
│   ├── ParserEngine.kt
│   ├── ParserRegistry.kt
│   ├── SmsNormalizer.kt
│   └── TransactionProcessingPipeline.kt
├── sms/
│   └── HesabyarSmsReceiver.kt
└── ui/
    ├── components/ (HesabyarBottomBar)
    ├── navigation/ (Screen)
    ├── screens/ (DashboardScreen, TransactionsScreen, ReportsScreen, CommitmentsScreen, MoreScreen)
    ├── theme/ (Color, Theme, Type)
    └── viewmodel/ (HesabyarViewModel)
```

## دستور تست و ساخت
```bash
# اجرای تست‌های محلی روی JVM
gradle testDebugUnitTest

# بیلد نسخه Debug
gradle assembleDebug
```
