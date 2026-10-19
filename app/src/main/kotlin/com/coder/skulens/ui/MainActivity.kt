package com.coder.skulens.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.coder.skulens.App

import com.coder.skulens.ui.theme.SkuLensTheme

import androidx.compose.animation.Crossfade
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val factory = viewModelFactory {
            initializer {
                val app = application as App
                ProductViewModel(app.repository)
            }
        }
        val viewModel = ViewModelProvider(this, factory)[ProductViewModel::class.java]

        setContent {
            SkuLensTheme {
                var showSplash by remember { mutableStateOf(true) }
                Crossfade(targetState = showSplash, label = "splashTransition") { isSplash ->
                    if (isSplash) {
                        SplashScreen(onTimeout = { showSplash = false })
                    } else {
                        MainScreen(viewModel = viewModel)
                    }
                }
            }
        }
    }
}
