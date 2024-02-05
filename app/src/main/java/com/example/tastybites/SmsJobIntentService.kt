package com.example.tastybites

import android.content.ContentValues

// SmsJobIntentService.kt
import android.app.IntentService
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.telephony.SmsMessage
import android.util.Log
import androidx.core.app.JobIntentService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SmsJobIntentService : JobIntentService() {

    companion object {
        private const val JOB_ID = 1000

        fun enqueueWork(context: Context, work: Intent) {
            enqueueWork(context, SmsJobIntentService::class.java, JOB_ID, work)
        }
    }

    private lateinit var databaseHelper: DatabaseHelper

    override fun onCreate() {
        super.onCreate()
        databaseHelper = DatabaseHelper(this)
    }

    override fun onHandleWork(intent: Intent) {
        handleSms(intent)
    }

    private fun handleSms(intent: Intent) {
        Log.d("SmsJobIntentService", "Handling SMS in background")
        val bundle: Bundle? = intent.extras
        bundle?.let {
            val pdus = it.get("pdus") as Array<*>

            for (pdu in pdus) {
                val message = SmsMessage.createFromPdu(pdu as ByteArray)
                val sender = message.originatingAddress
                val body = message.messageBody

                if (sender != null) {
                    // Use Kotlin Coroutines for database operations
                    CoroutineScope(Dispatchers.IO).launch {
                        insertSmsIntoDatabase(sender, body)
                    }
                }
            }
        }
    }

    private suspend fun insertSmsIntoDatabase(phoneNumber: String, message: String) {
        Log.d("SmsJobIntentService", "Inserting SMS into the database")
        withContext(Dispatchers.IO) {
            val db = databaseHelper.writableDatabase
            val values = ContentValues().apply {
                put(DatabaseHelper.COLUMN_PHONE_NUMBER_FEEDBACK, phoneNumber)
                put(DatabaseHelper.COLUMN_FEEDBACK_TEXT, message)
            }

            // Check for successful insertion
            val feedbackId = db.insert(DatabaseHelper.TABLE_FEEDBACK, null, values)
            if (feedbackId == -1L) {
                Log.e("Insertion Error", "Failed to insert SMS into the database.")
            }

            db.close()
        }
    }
}
