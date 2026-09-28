package com.studiotaraneh.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.studiotaraneh.app.ui.StudioTaranehApp
import com.studiotaraneh.app.ui.theme.StudioTaranehTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { StudioTaranehTheme { StudioTaranehApp() } }
    }
}
