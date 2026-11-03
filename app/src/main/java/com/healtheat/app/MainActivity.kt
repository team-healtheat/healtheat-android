package com.healtheat.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import com.healtheat.app.ui.HealtheatApp
import com.healtheat.app.ui.theme.HealtheatTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, true)
        setContent {
            HealtheatTheme { HealtheatApp() }
        }
    }
}
