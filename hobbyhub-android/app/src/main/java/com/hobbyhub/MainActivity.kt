package com.hobbyhub

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.hobbyhub.ui.navigation.AppNavigation
import com.hobbyhub.ui.theme.HobbyHubTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val deepLinkCommunityId = intent?.data?.lastPathSegment
        setContent {
            HobbyHubTheme {
                AppNavigation(initialCommunityId = deepLinkCommunityId)
            }
        }
    }
}
