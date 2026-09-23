package com.example.serviciosya

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.serviciosya.di.AppContainer
import com.example.serviciosya.ui.theme.ServiciosYATheme

class MainActivity : ComponentActivity() {
    private val container by lazy { AppContainer(applicationContext) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ServiciosYATheme {
                ServiciosYaApp(container = container)
            }
        }
    }
}
