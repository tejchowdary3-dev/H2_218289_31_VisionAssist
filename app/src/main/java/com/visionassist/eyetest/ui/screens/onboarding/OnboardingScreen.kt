package com.visionassist.eyetest.ui.screens.onboarding

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
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
import com.visionassist.eyetest.ui.theme.StreamingCardRadius

@Composable
fun OnboardingScreen(
    factory: VisionAssistViewModelFactory,
    onContinue: () -> Unit
) {
    val vm: OnboardingViewModel = viewModel(factory = factory)
    val state by vm.uiState.collectAsStateWithLifecycle()
    val examSteps = LocalContext.current.resources.getStringArray(R.array.exam_protocol_steps)

    VisionAssistScaffold(title = stringResource(R.string.app_name)) { padding ->
        ScaffoldScrollableContent(paddingValues = padding) {
            Text(
                stringResource(R.string.onboarding_title),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(R.string.splash_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(16.dp))
            InfoBanner(stringResource(R.string.onboarding_body))
            Spacer(Modifier.height(16.dp))
            ClinicalCard {
                Text(
                    stringResource(R.string.exam_protocol_title),
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(Modifier.height(10.dp))
                examSteps.forEachIndexed { index, line ->
                    Text(
                        text = "${index + 1}. $line",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
            }
            Spacer(Modifier.height(20.dp))
            ClinicalCard {
                Text(
                    stringResource(R.string.profile_section_title),
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = state.ageText,
                    onValueChange = vm::onAgeChange,
                    label = { Text(stringResource(R.string.age_label)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(StreamingCardRadius),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = state.screenTimeText,
                    onValueChange = vm::onScreenTimeChange,
                    label = { Text(stringResource(R.string.screen_time_label)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(StreamingCardRadius),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )
                state.error?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                }
            }
            Spacer(Modifier.height(24.dp))
            PrimaryActionButton(
                text = stringResource(R.string.action_continue),
                onClick = { if (vm.saveAndContinue()) onContinue() },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}
