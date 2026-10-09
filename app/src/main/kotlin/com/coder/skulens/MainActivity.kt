package com.coder.skulens

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val factory = viewModelFactory {
            initializer { ProductViewModel((application as App).repository) }
        }
        val viewModel = ViewModelProvider(this, factory)[ProductViewModel::class.java]

        setContent {
            MaterialTheme {
                MainScreen(viewModel = viewModel)
            }
        }
    }
}
