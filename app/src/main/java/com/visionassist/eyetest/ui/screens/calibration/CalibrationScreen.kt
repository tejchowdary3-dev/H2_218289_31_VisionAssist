package com.visionassist.eyetest.ui.screens.calibration

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.visionassist.eyetest.R
import com.visionassist.eyetest.ui.VisionAssistViewModelFactory
import com.visionassist.eyetest.ui.components.ClinicalCard
import com.visionassist.eyetest.ui.components.InfoBanner
import com.visionassist.eyetest.ui.components.PrimaryActionButton
import com.visionassist.eyetest.ui.components.ScaffoldScrollableContent
import com.visionassist.eyetest.ui.components.VisionAssistScaffold

@Composable
fun CalibrationScreen(
    factory: VisionAssistViewModelFactory,
    onNavigateUp: () -> Unit,
    onContinue: () -> Unit
) {
    val vm: CalibrationViewModel = viewModel(factory = factory)
    val state by vm.uiState.collectAsStateWithLifecycle()
    val screenWidthDp = LocalConfiguration.current.screenWidthDp.dp
    val barWidth = screenWidthDp * (0.35f + 0.55f * state.slider01)

    VisionAssistScaffold(
        title = stringResource(R.string.calibration_title),
        onNavigateUp = onNavigateUp
    ) { padding ->
        ScaffoldScrollableContent(paddingValues = padding) {
            InfoBanner(stringResource(R.string.calibration_body))
            Spacer(Modifier.height(16.dp))
            ClinicalCard {
                Text(
                    stringResource(R.string.calibration_reference_fmt, state.referenceWidthMm),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Box(
                        modifier = Modifier
                            .width(barWidth.coerceIn(48.dp, screenWidthDp * 0.95f))
                            .height(44.dp)
                            .padding(start = 10.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.primary)
                    )
                }
                Spacer(Modifier.height(8.dp))
                Slider(
                    value = state.slider01,
                    onValueChange = vm::onSliderChange,
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.secondary,
                        activeTrackColor = MaterialTheme.colorScheme.secondary
                    )
                )
                TextButton(onClick = vm::resetToDefault) {
                    Text(
                        stringResource(R.string.calibration_reset),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Spacer(Modifier.height(24.dp))
            PrimaryActionButton(text = stringResource(R.string.action_continue), onClick = onContinue)
            Spacer(Modifier.height(24.dp))
        }
    }
}
