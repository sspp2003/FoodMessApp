package com.example.tastybites

import android.Manifest
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.telephony.SmsManager
import android.widget.Button
import android.widget.EditText
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat


class FeedbackActivity : AppCompatActivity() {
    private lateinit var dbHelper: DatabaseHelper
    private val SEND_SMS_REQUEST_CODE = 123
    private lateinit var feedBackText:EditText
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_feedback)

        feedBackText = findViewById(R.id.feedBackText)
        val sendButton = findViewById<Button>(R.id.button)
        dbHelper = DatabaseHelper(this)
        sendButton.setOnClickListener {
            // Check for SMS permission before sending feedback request
            if (checkPermission()) {
                // Trigger the process to send feedback request to all registered numbers
                sendSMSToAll()
            } else {
                requestPermission()
            }
        }

    }
    private fun checkPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.SEND_SMS
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.SEND_SMS),
            SEND_SMS_REQUEST_CODE
        )
    }
    fun sendSMSToAll() {
        val phoneNumbers = dbHelper.getAllPhoneNumbers()

        for (phoneNumber in phoneNumbers) {
            // Replace "Your feedback request message" with the actual message
            val feedbackRequestMessage = feedBackText.text.toString()

            // Assume you have a method in DatabaseHelper to send SMS
            // (you would replace this with actual SMS sending logic)
            sendSMS(phoneNumber, feedbackRequestMessage)

        }
    }
    fun sendSMS(phoneNumber: String, message: String) {
        try {
            val smsManager = SmsManager.getDefault()
            smsManager.sendTextMessage(phoneNumber, null, message, null, null)
        } catch (e: Exception) {
            // Handle exceptions related to SMS sending (e.g., permissions, service not available)
            e.printStackTrace()
        }
    }
}