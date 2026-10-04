package com.example.parser

data class BankDetectionResult(
    val bankId: Long,
    val bankNameFa: String,
    val matchedReason: String
)

object BankDetector {

    fun detect(message: NormalizedSms): BankDetectionResult? {
        val s = message.sender.lowercase()
        val b = message.body

        // Mellat
        if (s.contains("mellat") || s.contains("2000400") || b.contains("بانک ملت") || b.contains("ملت") && (b.contains("واریز") || b.contains("برداشت") || b.contains("خرید"))) {
            return BankDetectionResult(1, "بانک ملت", "تطبیق فرستنده یا عنوان بانک ملت")
        }

        // Melli
        if (s.contains("melli") || b.contains("بانک ملی") || b.contains("بانک‌ملی")) {
            return BankDetectionResult(2, "بانک ملی ایران", "تطبیق بانک ملی")
        }

        // Saderat
        if (s.contains("saderat") || b.contains("بانک صادرات") || b.contains("صادرات")) {
            return BankDetectionResult(3, "بانک صادرات ایران", "تطبیق بانک صادرات")
        }

        // Tejarat
        if (s.contains("tejarat") || b.contains("بانک تجارت")) {
            return BankDetectionResult(4, "بانک تجارت", "تطبیق بانک تجارت")
        }

        // Saman
        if (s.contains("saman") || b.contains("بانک سامان")) {
            return BankDetectionResult(5, "بانک سامان", "تطبیق بانک سامان")
        }

        // Pasargad
        if (s.contains("pasargad") || b.contains("بانک پاسارگاد")) {
            return BankDetectionResult(6, "بانک پاسارگاد", "تطبیق بانک پاسارگاد")
        }

        return null
    }
}
