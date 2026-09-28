package com.secount.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.secount.app.logic.AppCtx
import com.secount.app.ui.App

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Swap the branded launch screen for the app theme before drawing.
        setTheme(R.style.Theme_Secount)
        super.onCreate(savedInstanceState)
        AppCtx.app = applicationContext
        // Ask for notification permission (Android 13+) so D-day secret
        // alerts can appear outside the app. In-app card works regardless.
        try {
            if (android.os.Build.VERSION.SDK_INT >= 33) {
                if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) !=
                    android.content.pm.PackageManager.PERMISSION_GRANTED
                ) {
                    requestPermissions(
                        arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 1001
                    )
                }
            }
        } catch (ignored: Exception) {
        }
        setContent { App() }
    }

    override fun onPause() {
        super.onPause()
        // User rule: lock only when going to Home/background, never on a timer.
        // App.kt polls this flag once per second and shows the PIN gate on return.
        try {
            getSharedPreferences("secount", MODE_PRIVATE)
                .edit().putString("secount_need_lock", "1").apply()
        } catch (ignored: Exception) {
        }
    }
}
