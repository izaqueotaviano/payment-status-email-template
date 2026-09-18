package com.iptvtv.player

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.iptvtv.player.navigation.AppNavHost
import com.iptvtv.player.ui.theme.IptvTvPlayerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val container = (application as IptvApplication).container
        setContent {
            IptvTvPlayerTheme {
                AppNavHost(container = container)
            }
        }
    }
}
