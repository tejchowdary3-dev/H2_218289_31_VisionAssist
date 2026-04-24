package com.visionassist.eyetest.ui.screens.splash

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.visionassist.eyetest.R
import com.visionassist.eyetest.ui.theme.AppBlack
import com.visionassist.eyetest.ui.theme.AppBrandRed
import com.visionassist.eyetest.ui.theme.AppBrandRedDark
import com.visionassist.eyetest.ui.theme.AppBrandRedSoft
import com.visionassist.eyetest.ui.theme.AppBrandRedSoftStrong
import com.visionassist.eyetest.ui.theme.AppOnSurface
import com.visionassist.eyetest.ui.theme.AppOnSurfaceMuted
import com.visionassist.eyetest.ui.theme.AppWhite
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onFinished: () -> Unit) {
    LaunchedEffect(Unit) {
        delay(1400)
        onFinished()
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(AppBlack, AppBrandRedSoft, AppBrandRedSoftStrong)
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .padding(horizontal = 28.dp)
                .navigationBarsPadding(),
            shape = RoundedCornerShape(24.dp),
            color = AppWhite.copy(alpha = 0.96f),
            tonalElevation = 3.dp
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 28.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(90.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(AppBrandRedDark, AppBrandRed)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "VA",
                        style = MaterialTheme.typography.headlineMedium,
                        color = AppWhite,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.splash_title),
                    style = MaterialTheme.typography.headlineMedium,
                    color = AppOnSurface,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = stringResource(R.string.splash_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppOnSurfaceMuted,
                    textAlign = TextAlign.Center
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(100.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    AppBrandRed.copy(alpha = 0.1f),
                                    AppBrandRed,
                                    AppWhite,
                                    AppBrandRed
                                )
                            )
                        )
                )
                CircularProgressIndicator(
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .size(36.dp),
                    color = AppBrandRedDark,
                    strokeWidth = 3.dp,
                    trackColor = AppBrandRedSoftStrong
                )
            }
        }
    }
}
