// CheckFeedbackActivity.kt
package com.example.tastybites

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.os.Build
import android.os.Bundle
import android.telephony.SmsMessage
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CheckFeedbackActivity : AppCompatActivity() {

    private lateinit var smsListView: ListView
    private lateinit var smsAdapter: ArrayAdapter<String>
    private val smsList = mutableListOf<String>()
    private lateinit var delBtn : Button
    private lateinit var smsReceiver: BroadcastReceiver
    private lateinit var databaseHelper: DatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_check_feedback)

        delBtn = findViewById(R.id.delButton)
        delBtn.setOnClickListener(){
            deleteAllFeedback()
        }
        smsListView = findViewById(R.id.listViewFeedback)
        CoroutineScope(Dispatchers.Main).launch {
            updateListViewFromDatabase()
        }

        if (!hasSmsPermission()) {
            requestSmsPermission()
        } else {
            smsAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, smsList)
            smsListView.adapter = smsAdapter

            databaseHelper = DatabaseHelper(this)
            registerSmsReceiver()

            // Start the SmsService
            startService(Intent(this, SmsService::class.java))
        }


    }

    private fun deleteAllFeedback() {
        CoroutineScope(Dispatchers.IO).launch {
            val db = databaseHelper.writableDatabase

            // Delete all rows from the feedback table
            val deletedRows = db.delete(DatabaseHelper.TABLE_FEEDBACK, null, null)

            withContext(Dispatchers.Main) {
                if (deletedRows > 0) {
                    Toast.makeText(
                        this@CheckFeedbackActivity,
                        "All feedback deleted successfully",
                        Toast.LENGTH_SHORT
                    ).show()
                    // Update the UI or perform any other actions after deletion
                    updateListViewFromDatabase()
                } else {
                    Toast.makeText(
                        this@CheckFeedbackActivity,
                        "Failed to delete feedback",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            db.close()
        }
    }
    private fun registerSmsReceiver() {
        smsReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val bundle = intent.extras
                if (bundle != null) {
                    val pdus = bundle.get("pdus") as Array<*>
                    for (pdu in pdus) {
                        val message = SmsMessage.createFromPdu(pdu as ByteArray)
                        val sender = message.originatingAddress
                        val body = message.messageBody
                        val smsText = "From: $sender\nMessage: $body\n"
                        if (sender != null) {
                            println(sender.substring(3))
                        }

                        if (sender != null) {
                            if (isPhoneNumberRegistered(sender)) {
                                // Use Kotlin Coroutines for database operations
                                CoroutineScope(Dispatchers.Main).launch {
                                    insertSmsIntoDatabase(sender, body)
                                    updateListViewFromDatabase()
                                }
                            }
                        }
                    }
                }
            }
        }

        val intentFilter = IntentFilter("android.provider.Telephony.SMS_RECEIVED")
        registerReceiver(smsReceiver, intentFilter)

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
    private fun isPhoneNumberRegistered(phoneNumber: String): Boolean {
        val registeredNumbers = databaseHelper.getAllPhoneNumbers()
        return registeredNumbers.contains(phoneNumber.substring(3))
    }
    private suspend fun updateListViewFromDatabase() {
        withContext(Dispatchers.IO) {
            smsList.clear()

            val db = databaseHelper.readableDatabase
            val cursor = db.rawQuery("SELECT * FROM ${DatabaseHelper.TABLE_FEEDBACK}", null)

            while (cursor.moveToNext()) {
                val phoneNumber =
                    cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_PHONE_NUMBER_FEEDBACK))
                val feedbackMessage =
                    cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_FEEDBACK_TEXT))
                val smsText = "\nFrom: $phoneNumber\nMessage: $feedbackMessage\n"
                smsList.add(smsText)
            }

            cursor.close()
            db.close()
        }

        CoroutineScope(Dispatchers.Main).launch {
            smsAdapter.notifyDataSetChanged()
        }
    }

    private fun hasSmsPermission(): Boolean {
        val permissionGranted = ActivityCompat.checkSelfPermission(
            this,
            android.Manifest.permission.RECEIVE_SMS
        ) == PackageManager.PERMISSION_GRANTED

        Log.d("Permission", "SMS Permission: $permissionGranted")

        return permissionGranted
    }

    private fun requestSmsPermission() {
        Log.d("Permission", "SMS permission not granted. Requesting...")
        ActivityCompat.requestPermissions(
            this,
            arrayOf(android.Manifest.permission.RECEIVE_SMS),
            SMS_PERMISSION_REQUEST_CODE
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == SMS_PERMISSION_REQUEST_CODE && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Log.d("Permission", "SMS Permission granted")
            smsAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, smsList)
            smsListView.adapter = smsAdapter
            registerSmsReceiver()
        } else {
            Log.d("Permission", "SMS Permission denied")
            // Permission denied, handle accordingly (e.g., show a message to the user)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(smsReceiver)
        databaseHelper.close()
    }

    companion object {
        private const val SMS_PERMISSION_REQUEST_CODE = 123
    }
}
