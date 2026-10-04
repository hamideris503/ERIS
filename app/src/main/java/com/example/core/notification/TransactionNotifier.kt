package com.example.core.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.core.utils.FinancialFormatter
import com.example.data.local.entity.TransactionEntity
import com.example.domain.model.TransactionType

object TransactionNotifier {

    private const val CHANNEL_ID = "hesabyar_transactions"
    private const val CHANNEL_NAME = "تراکنش‌های بانکی"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "اطلاع‌رسانی ثبت خودکار تراکنش‌های بانکی حسابیار"
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            manager?.createNotificationChannel(channel)
        }
    }

    fun notifyTransactionParsed(
        context: Context,
        type: TransactionType,
        amount: Long,
        bankName: String,
        storeOrDesc: String?
    ) {
        try {
            createNotificationChannel(context)

            val title = "ثبت خودکار تراکنش ${type.titleFa}"
            val content = buildString {
                append("${type.titleFa} ")
                append(FinancialFormatter.formatToman(amount))
                if (!storeOrDesc.isNullOrBlank()) {
                    append(" • $storeOrDesc")
                }
                append(" ($bankName)")
            }

            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(content)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .build()

            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            manager?.notify(System.currentTimeMillis().toInt(), notification)
        } catch (e: Exception) {
            // Notifications are optional and best-effort
        }
    }
}
