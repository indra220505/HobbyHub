package com.hobbyhub

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.hobbyhub.ui.navigation.AppNavigation
import com.hobbyhub.ui.theme.HobbyHubTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val data = intent?.data
        val deepLinkCommunityId = when {
            data?.scheme == "hobbyhub" -> data.host ?: data.lastPathSegment
            data?.path?.startsWith("/community/") == true -> data.lastPathSegment
            else -> data?.lastPathSegment
        }
        setContent {
            HobbyHubTheme {
                AppNavigation(initialCommunityId = deepLinkCommunityId)
            }
        }
    }
}
