package com.jackasan1.deepseekmanager

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jackasan1.deepseekmanager.ui.DashboardScreen
import com.jackasan1.deepseekmanager.ui.DashboardViewModel
import com.jackasan1.deepseekmanager.ui.theme.DeepSeekManagerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DeepSeekManagerTheme {
                val viewModel: DashboardViewModel = viewModel()
                DashboardScreen(viewModel = viewModel)
            }
        }
    }
}
