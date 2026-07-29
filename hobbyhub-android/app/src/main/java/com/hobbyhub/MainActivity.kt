package com.hobbyhub

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.hobbyhub.ui.navigation.AppNavigation
import com.hobbyhub.ui.theme.HobbyHubTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            HobbyHubTheme {
                AppNavigation()
            }
        }
    }
}
