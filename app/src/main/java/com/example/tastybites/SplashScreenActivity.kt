package com.example.tastybites

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.TextView

class SplashScreenActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash_screen)
        val tastebites=findViewById<TextView>(R.id.tastybites)

        Handler(Looper.getMainLooper()).postDelayed({
            startActivity(Intent(this,MainActivity::class.java))
            finish()
        },2000)

        tastebites.alpha=0f
        tastebites.animate().setDuration(1500).alpha(1f).withEndAction{
            overridePendingTransition(android.R.anim.fade_in,android.R.anim.fade_out)
        }
    }
}

