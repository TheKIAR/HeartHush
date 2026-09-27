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
        setContent { App() }
    }
}
