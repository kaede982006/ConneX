package com.connex.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.connex.app.ui.theme.ConneXTheme
import com.connex.app.ui.navigation.ConneXRoot
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ConneXTheme {
                ConneXRoot()
            }
        }
    }
}
