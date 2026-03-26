package com.example.shannon

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import com.example.shannon.presentation.NetworkDiagnosticsRoute
import com.example.shannon.ui.theme.ShannonTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ShannonTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    NetworkDiagnosticsRoute(
                        applicationContext = applicationContext,
                        contentPadding = innerPadding,
                    )
                }
            }
        }
    }
}
