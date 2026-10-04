package com.example.domain.model

enum class TransactionType(val titleFa: String) {
    DEPOSIT("واریز"),
    WITHDRAW("برداشت"),
    PURCHASE("خرید"),
    TRANSFER_IN("انتقال به حساب"),
    TRANSFER_OUT("انتقال از حساب"),
    TRANSFER_INTERNAL("انتقال بین حساب‌های خودی"),
    FEE("کارمزد"),
    INTEREST("سود بانکی"),
    REFUND("بازگشت وجه"),
    CASH("پول نقد"),
    OTHER("سایر")
}

enum class CategoryType(val titleFa: String) {
    EXPENSE("هزینه"),
    INCOME("درآمد")
}

enum class CheckType(val titleFa: String) {
    ISSUED("صادره (پرداختی)"),
    RECEIVED("وارده (دریافتی)")
}

enum class CheckStatus(val titleFa: String) {
    PENDING("در انتظار وصول"),
    PASSED("پاس شده"),
    BOUNCED("برگشت خورده"),
    CANCELLED("باطل شده")
}

enum class AssetType(val titleFa: String) {
    BANK("حساب بانکی"),
    CASH("وجه نقد"),
    GOLD("طلا و سکه"),
    CURRENCY("ارز"),
    INVESTMENT("بورس و صندوق"),
    VEHICLE("خودرو"),
    REAL_ESTATE("ملک و زمین"),
    OTHER("سایر دارایی‌ها")
}

enum class SmsStatus(val titleFa: String) {
    PENDING("در انتظار پردازش"),
    PARSED("تحلیل شده"),
    NEEDS_REVIEW("نیازمند بررسی"),
    UNRECOGNIZED("ناشناخته"),
    IGNORED("نادیده گرفته شده")
}

enum class BankSupportStatus {
    SUPPORTED,
    PARTIAL,
    UNTESTED,
    UNKNOWN
}
