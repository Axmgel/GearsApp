package com.axmgel.gearsapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.axmgel.gearsapp.ui.GearsNavHost
import com.axmgel.gearsapp.ui.theme.GearsTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GearsTheme {
                GearsNavHost()
            }
        }
    }
}
