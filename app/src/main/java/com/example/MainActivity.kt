package com.example

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.fragment.app.FragmentActivity
import com.example.ui.BossAppViewModel
import com.example.ui.BossCalculatorApp

class MainActivity : FragmentActivity() {

    private val viewModel: BossAppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BossCalculatorApp(viewModel = viewModel)
        }
    }
}
