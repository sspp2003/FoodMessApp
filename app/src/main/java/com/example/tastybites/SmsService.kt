// SmsService.kt
package com.example.tastybites

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.IBinder
import android.telephony.SmsMessage
import android.util.Log
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// SmsService.kt
// ... (imports)

class SmsService : Service() {

    private lateinit var smsReceiver: BroadcastReceiver
    private lateinit var databaseHelper: DatabaseHelper

    companion object {
        const val NOTIFICATION_ID = 1
        const val CHANNEL_ID = "ForegroundServiceChannel"
    }

    override fun onCreate() {
        super.onCreate()

        databaseHelper = DatabaseHelper(this)

        // Create a notification channel for Foreground Service
        createNotificationChannel()

        // Register BroadcastReceiver to listen for SMS
        smsReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                CoroutineScope(Dispatchers.IO).launch {
                    handleSms(intent)
                }
            }
        }

        val intentFilter = IntentFilter("android.provider.Telephony.SMS_RECEIVED")
        registerReceiver(smsReceiver, intentFilter)
    }

    private suspend fun handleSms(intent: Intent) {
        val bundle = intent.extras
        if (bundle != null) {
            val pdus = bundle.get("pdus") as Array<*>
            for (pdu in pdus) {
                val message = SmsMessage.createFromPdu(pdu as ByteArray)
                val sender = message.originatingAddress
                val body = message.messageBody

                if (sender != null) {
                    if (isPhoneNumberRegistered(sender)) {
                        println("true")
                        insertSmsIntoDatabase(sender, body)
                    }
                }
            }
        }
    }

    private suspend fun insertSmsIntoDatabase(phoneNumber: String, message: String) {
        withContext(Dispatchers.IO) {
            if (!isPhoneNumberAlreadyExists(phoneNumber)) {
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
    private fun isPhoneNumberRegistered(phoneNumber: String): Boolean {
        val registeredNumbers = databaseHelper.getAllPhoneNumbers()
        return registeredNumbers.contains(phoneNumber.substring(3))
    }

    private fun isPhoneNumberAlreadyExists(phoneNumber: String): Boolean {
        val db = databaseHelper.readableDatabase
        val cursor = db.query(
            DatabaseHelper.TABLE_FEEDBACK,
            null,
            "${DatabaseHelper.COLUMN_PHONE_NUMBER_FEEDBACK} = ?",
            arrayOf(phoneNumber),
            null,
            null,
            null
        )

        val phoneNumberExists = cursor.count > 0
        cursor.close()
        db.close()

        return phoneNumberExists
    }
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(smsReceiver)
        databaseHelper.close()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Start the service as a Foreground Service
        val notification = createNotification()
        startForeground(NOTIFICATION_ID, notification)
        return START_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Foreground Service Channel",
                NotificationManager.IMPORTANCE_HIGH
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }

    private fun createNotification(): Notification {
        val pendingIntent: PendingIntent =
            Intent(this, MainActivity::class.java).let { notificationIntent ->
                PendingIntent.getActivity(this, 0, notificationIntent, 0)
            }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Foreground Service")
            .setContentText("Foreground Service is running")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .build()
    }
}
