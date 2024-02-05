package com.example.tastybites

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.telephony.SmsMessage
import android.widget.Toast
import androidx.legacy.content.WakefulBroadcastReceiver.startWakefulService


class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val bundle: Bundle? = intent.extras
        bundle?.let {
            val pdus = it.get("pdus") as Array<*>

            for (pdu in pdus) {
                val message = SmsMessage.createFromPdu(pdu as ByteArray)
                val sender = message.originatingAddress
                val body = message.messageBody

                if (sender != null) {
                    val serviceIntent = Intent(context, SmsService::class.java)
                    serviceIntent.putExtras(intent.extras!!)

                    // Start the service to handle SMS
                    startWakefulService(context, serviceIntent)
                }
            }
        }
    }
}
