package com.visionassist.eyetest

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.visionassist.eyetest.ui.VisionAssistViewModelFactory
import com.visionassist.eyetest.ui.navigation.AppNavHost
import com.visionassist.eyetest.ui.theme.VisionAssistTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
        )
        val factory = VisionAssistViewModelFactory(application)
        setContent {
            VisionAssistTheme {
                AppNavHost(factory = factory)
            }
        }
    }
}
