package com.example.parkingtimerapp

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.ComponentActivity
import android.view.animation.AccelerateInterpolator
import android.widget.ImageView

class SplashActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash) // Set the custom layout

        // Find the logo view
        val splashLogo = findViewById<ImageView>(R.id.splash_logo)

        // Fade-in animation
        splashLogo.animate()
            .translationY(-100f) // Move the logo up by 100 pixels
            .setDuration(1000)
            .setInterpolator(AccelerateInterpolator())
            .withEndAction {
                // Move the logo back to its original position
                splashLogo.animate()
                    .translationY(0f)
                    .setDuration(500)
                    .start()
            }
            .start()

        // Delay for 3 seconds (3000 milliseconds)
        Handler(Looper.getMainLooper()).postDelayed({
            startActivity(Intent(this, MainActivity::class.java))

            // Use overrideActivityTransition for Android 12 and above
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                overrideActivityTransition(
                    OVERRIDE_TRANSITION_OPEN, // Type of transition (OPEN or CLOSE)
                    android.R.anim.fade_in,   // Enter animation
                    android.R.anim.fade_out   // Exit animation
                )
            } else {
                // Fallback to overridePendingTransition for older versions
                @Suppress("DEPRECATION")
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            }

            finish()
        }, 2000)
    }
}