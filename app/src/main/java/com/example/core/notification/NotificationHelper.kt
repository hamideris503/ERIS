package com.example.core.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.core.utils.FinancialFormatter

object NotificationHelper {

    const val CHANNEL_BUDGET_ALERTS = "hesabyar_budget_alerts"
    private const val CHANNEL_NAME = "هشدارهای بودجه و سقف مخارج"
    private const val CHANNEL_DESC = "اعلان خودکار عبور مخارج از سقف تعیین‌شده دسته‌بندی‌ها"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_BUDGET_ALERTS, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESC
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 300, 200, 300)
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Checks if the app has permission to post notifications.
     */
    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            NotificationManagerCompat.from(context).areNotificationsEnabled()
        }
    }

    /**
     * Dispatches a local budget alert notification when spending crosses the limit.
     */
    fun sendBudgetExceededNotification(
        context: Context,
        categoryId: Long,
        categoryName: String,
        spentAmount: Long,
        budgetLimit: Long,
        percentage: Int
    ) {
        createNotificationChannel(context)

        if (!hasNotificationPermission(context)) {
            return
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            categoryId.toInt(),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val title = "⚠️ عبور از سقف بودجه: $categoryName"
        val message = "مخارج شما در «$categoryName» به ${FinancialFormatter.toPersianDigits(percentage.toString())}٪ سقف تعیین‌شده رسید!\nمبلغ هزینه شده: ${FinancialFormatter.formatToman(spentAmount)}\nسقف مجاز: ${FinancialFormatter.formatToman(budgetLimit)}"

        val notification = NotificationCompat.Builder(context, CHANNEL_BUDGET_ALERTS)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText("مخارج شما در «$categoryName» به ${FinancialFormatter.toPersianDigits(percentage.toString())}٪ سقف تعیین‌شده رسید.")
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setColor(0xFFDC2626.toInt()) // Alert Red
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setVibrate(longArrayOf(0, 300, 200, 300))
            .build()

        try {
            val notificationManager = NotificationManagerCompat.from(context)
            notificationManager.notify(1000 + categoryId.toInt(), notification)
        } catch (e: SecurityException) {
            // Handled safely if permission revoked
        }
    }
}
