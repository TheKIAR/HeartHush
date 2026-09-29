package com.secount.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import com.secount.app.logic.AppCtx
import com.secount.app.logic.PhotoPick
import com.secount.app.ui.App

class MainActivity : ComponentActivity() {
    private val photoPicker = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        PhotoPick.deliver(uri)
        try {
            SecountWidget.requestRefresh(this)
        } catch (ignored: Exception) {
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Crash log: capture uncaught exceptions to a file; App.kt offers to share it.
        try {
            val prev = Thread.getDefaultUncaughtExceptionHandler()
            Thread.setDefaultUncaughtExceptionHandler { t, e ->
                try {
                    val f = java.io.File(filesDir, "crash_log.txt")
                    val sw = java.io.StringWriter()
                    e.printStackTrace(java.io.PrintWriter(sw))
                    f.writeText("Secount crash @ ${java.util.Date()}\n${sw}\n")
                } catch (ignored: Exception) {
                }
                prev?.uncaughtException(t, e)
            }
        } catch (ignored: Exception) {
        }
        // Swap the branded launch screen for the app theme before drawing.
        setTheme(R.style.Theme_Secount)
        super.onCreate(savedInstanceState)
        AppCtx.app = applicationContext
        AppCtx.photoLauncher = { mime -> photoPicker.launch(mime) }
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

    override fun onResume() {
        super.onResume()
        try {
            SecountWidget.requestRefresh(this)
        } catch (ignored: Exception) {
        }
    }

    override fun onPause() {
        super.onPause()
        // User rule: lock only when going to Home/background, never on a timer.
        // App.kt polls this flag once per second and shows the PIN gate on return.
        // While the gallery picker is open we also background — that must NOT lock.
        try {
            if (com.secount.app.logic.PhotoLockGuard.picking) return
            getSharedPreferences("secount", MODE_PRIVATE)
                .edit().putString("secount_need_lock", "1").apply()
        } catch (ignored: Exception) {
        }
    }
}
