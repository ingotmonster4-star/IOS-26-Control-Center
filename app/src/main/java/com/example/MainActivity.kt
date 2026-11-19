package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.system.PermissionHealthMonitor
import com.example.ui.LumaApp
import com.example.ui.theme.LumaCentreTheme

class MainActivity : ComponentActivity() {
    private var showReEnableFromIntent by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        showReEnableFromIntent = intent?.getBooleanExtra(
            PermissionHealthMonitor.EXTRA_SHOW_REENABLE_POPUP,
            false
        ) == true

        setContent {
            LumaCentreTheme {
                LumaApp(forceShowReEnableFromIntent = showReEnableFromIntent)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.getBooleanExtra(PermissionHealthMonitor.EXTRA_SHOW_REENABLE_POPUP, false)) {
            showReEnableFromIntent = true
        }
    }
}
