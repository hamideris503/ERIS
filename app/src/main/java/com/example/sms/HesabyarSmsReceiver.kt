package com.example.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import com.example.core.notification.TransactionNotifier
import com.example.data.local.HesabyarDatabase
import com.example.data.local.entity.SmsMessageEntity
import com.example.domain.model.SmsStatus
import com.example.parser.NormalizedSms
import com.example.parser.SmsNormalizer
import com.example.parser.TransactionProcessingPipeline
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class HesabyarSmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        if (messages.isNullOrEmpty()) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = HesabyarDatabase.getInstance(context)

                val sender = messages[0].displayOriginatingAddress ?: ""
                val fullBody = messages.joinToString("") { it.displayMessageBody ?: "" }
                val timestamp = messages[0].timestampMillis

                val normalizedBody = SmsNormalizer.normalizeText(fullBody)
                val hash = SmsNormalizer.generateSha256(sender, fullBody, timestamp)

                // Check if duplicate SMS message
                val existing = db.smsMessageDao().findByHash(hash)
                if (existing != null) {
                    Log.d("HesabyarSms", "Duplicate SMS ignored: $hash")
                    return@launch
                }

                val normalizedSms = NormalizedSms(
                    sender = sender,
                    rawBody = fullBody,
                    body = normalizedBody,
                    timestamp = timestamp,
                    sha256Hash = hash
                )

                // Process through pipeline: Parse -> Duplicate check -> Resolve Account & Category -> Save
                val ingestionResult = TransactionProcessingPipeline.processAndSaveSms(db, normalizedSms)

                val status = when (ingestionResult.status) {
                    "SUCCESS" -> SmsStatus.PARSED
                    "DUPLICATE_IGNORED" -> SmsStatus.IGNORED
                    "PARSE_FAILED" -> SmsStatus.UNRECOGNIZED
                    else -> SmsStatus.NEEDS_REVIEW
                }

                val smsEntity = SmsMessageEntity(
                    sender = sender,
                    body = normalizedBody,
                    timestamp = timestamp,
                    status = status,
                    sha256Hash = hash,
                    parsedBankId = 1L,
                    parsedTransactionId = ingestionResult.transactionId
                )
                db.smsMessageDao().insertSms(smsEntity)
                Log.d("HesabyarSms", "Processed SMS: result=${ingestionResult.status}, txId=${ingestionResult.transactionId}")

                // Post user notification if a new transaction was recorded
                if (ingestionResult.transactionId != null) {
                    val savedTx = db.transactionDao().getTransactionById(ingestionResult.transactionId)
                    if (savedTx != null) {
                        val bank = savedTx.bankId?.let { db.bankDao().getBankById(it) }
                        TransactionNotifier.notifyTransactionParsed(
                            context = context,
                            type = savedTx.type,
                            amount = savedTx.amount,
                            bankName = bank?.nameFa ?: "بانک",
                            storeOrDesc = savedTx.description
                        )
                    }
                }

            } catch (e: Exception) {
                Log.e("HesabyarSms", "Error processing incoming SMS", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
