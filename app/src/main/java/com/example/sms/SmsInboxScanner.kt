package com.example.sms

import android.content.Context
import android.content.pm.PackageManager
import android.provider.Telephony
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.data.local.HesabyarDatabase
import com.example.data.local.entity.SmsMessageEntity
import com.example.domain.model.SmsStatus
import com.example.parser.NormalizedSms
import com.example.parser.SmsNormalizer
import com.example.parser.TransactionProcessingPipeline
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object SmsInboxScanner {

    data class ScanSummary(
        val totalRead: Int,
        val parsedCount: Int,
        val duplicatesSkipped: Int,
        val unrecognizedCount: Int
    )

    suspend fun scanExistingBankSms(context: Context, limit: Int = 100): ScanSummary = withContext(Dispatchers.IO) {
        if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED) {
            return@withContext ScanSummary(0, 0, 0, 0)
        }

        var totalRead = 0
        var parsedCount = 0
        var duplicatesSkipped = 0
        var unrecognizedCount = 0

        val db = HesabyarDatabase.getInstance(context)

        val projection = arrayOf(
            Telephony.Sms.Inbox.ADDRESS,
            Telephony.Sms.Inbox.BODY,
            Telephony.Sms.Inbox.DATE
        )

        try {
            val cursor = context.contentResolver.query(
                Telephony.Sms.Inbox.CONTENT_URI,
                projection,
                null,
                null,
                "${Telephony.Sms.Inbox.DATE} DESC LIMIT $limit"
            )

            cursor?.use {
                val addressIndex = it.getColumnIndex(Telephony.Sms.Inbox.ADDRESS)
                val bodyIndex = it.getColumnIndex(Telephony.Sms.Inbox.BODY)
                val dateIndex = it.getColumnIndex(Telephony.Sms.Inbox.DATE)

                while (it.moveToNext()) {
                    val sender = if (addressIndex >= 0) it.getString(addressIndex) ?: "" else ""
                    val body = if (bodyIndex >= 0) it.getString(bodyIndex) ?: "" else ""
                    val timestamp = if (dateIndex >= 0) it.getLong(dateIndex) else System.currentTimeMillis()

                    if (body.isBlank()) continue
                    totalRead++

                    val normalizedBody = SmsNormalizer.normalizeText(body)
                    val hash = SmsNormalizer.generateSha256(sender, body, timestamp)

                    val existing = db.smsMessageDao().findByHash(hash)
                    if (existing != null) {
                        duplicatesSkipped++
                        continue
                    }

                    val normalizedSms = NormalizedSms(
                        sender = sender,
                        rawBody = body,
                        body = normalizedBody,
                        timestamp = timestamp,
                        sha256Hash = hash
                    )

                    val res = TransactionProcessingPipeline.processAndSaveSms(db, normalizedSms)
                    val status = when (res.status) {
                        "SUCCESS" -> {
                            parsedCount++
                            SmsStatus.PARSED
                        }
                        "DUPLICATE_IGNORED" -> {
                            duplicatesSkipped++
                            SmsStatus.IGNORED
                        }
                        "PARSE_FAILED" -> {
                            unrecognizedCount++
                            SmsStatus.UNRECOGNIZED
                        }
                        else -> {
                            unrecognizedCount++
                            SmsStatus.NEEDS_REVIEW
                        }
                    }

                    val smsEntity = SmsMessageEntity(
                        sender = sender,
                        body = normalizedBody,
                        timestamp = timestamp,
                        status = status,
                        sha256Hash = hash,
                        parsedBankId = 1L,
                        parsedTransactionId = res.transactionId
                    )
                    db.smsMessageDao().insertSms(smsEntity)
                }
            }
        } catch (e: Exception) {
            Log.e("SmsInboxScanner", "Error reading SMS inbox", e)
        }

        ScanSummary(
            totalRead = totalRead,
            parsedCount = parsedCount,
            duplicatesSkipped = duplicatesSkipped,
            unrecognizedCount = unrecognizedCount
        )
    }
}
