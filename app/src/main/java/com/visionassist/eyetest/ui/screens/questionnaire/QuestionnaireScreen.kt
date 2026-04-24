package com.visionassist.eyetest.ui.screens.questionnaire

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.visionassist.eyetest.R
import com.visionassist.eyetest.domain.model.FocalDifficultyPattern
import com.visionassist.eyetest.domain.model.SymptomLevel
import com.visionassist.eyetest.ui.VisionAssistViewModelFactory
import com.visionassist.eyetest.ui.components.ClinicalCard
import com.visionassist.eyetest.ui.components.InfoBanner
import com.visionassist.eyetest.ui.components.PrimaryActionButton
import com.visionassist.eyetest.ui.components.ScaffoldScrollableContent
import com.visionassist.eyetest.ui.components.VisionAssistScaffold

@Composable
fun QuestionnaireScreen(
    factory: VisionAssistViewModelFactory,
    onNavigateUp: () -> Unit,
    onContinue: () -> Unit
) {
    val vm: QuestionnaireViewModel = viewModel(factory = factory)
    val state by vm.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(state.askNightVision, state.nightVision, state.focalPattern, state.distanceBlur, state.nearDifficulty, state.eyeStrain) {
        vm.skipNightIfNotAsked()
    }

    VisionAssistScaffold(
        title = stringResource(R.string.questionnaire_title),
        onNavigateUp = onNavigateUp
    ) { padding ->
        ScaffoldScrollableContent(paddingValues = padding) {
            InfoBanner(stringResource(R.string.questionnaire_intro))
            ClinicalCard {
                when {
                    state.focalPattern == null -> {
                        Text(
                            stringResource(R.string.question_focal_dominance),
                            style = MaterialTheme.typography.titleMedium
                        )
                        FocalPatternRow { vm.setFocalPattern(it) }
                    }
                    state.distanceBlur == null -> {
                        Text(
                            stringResource(R.string.question_distance_blur),
                            style = MaterialTheme.typography.titleMedium
                        )
                        SymptomRow { vm.setDistanceBlur(it) }
                    }
                    state.nearDifficulty == null -> {
                        Text(
                            stringResource(R.string.question_near_difficulty),
                            style = MaterialTheme.typography.titleMedium
                        )
                        SymptomRow { vm.setNearDifficulty(it) }
                    }
                    state.eyeStrain == null -> {
                        Text(
                            stringResource(R.string.question_eye_strain),
                            style = MaterialTheme.typography.titleMedium
                        )
                        SymptomRow { vm.setEyeStrain(it) }
                    }
                    state.askNightVision && state.nightVision == null -> {
                        Text(
                            stringResource(R.string.question_night_glare),
                            style = MaterialTheme.typography.titleMedium
                        )
                        SymptomRow { vm.setNight(it) }
                    }
                    else -> {
                        Text(
                            stringResource(R.string.questionnaire_ready),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            PrimaryActionButton(
                text = stringResource(R.string.action_continue),
                onClick = {
                    vm.commit()
                    onContinue()
                },
                enabled = vm.isComplete(),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun FocalPatternRow(onPick: (FocalDifficultyPattern) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        FocalChip(stringResource(R.string.focal_distant_harder)) { onPick(FocalDifficultyPattern.DISTANT_HARDER) }
        FocalChip(stringResource(R.string.focal_near_harder)) { onPick(FocalDifficultyPattern.NEAR_HARDER) }
        FocalChip(stringResource(R.string.focal_both)) { onPick(FocalDifficultyPattern.BOTH) }
        FocalChip(stringResource(R.string.focal_neither)) { onPick(FocalDifficultyPattern.NEITHER) }
    }
}

@Composable
private fun FocalChip(label: String, onClick: () -> Unit) {
    FilterChip(
        selected = false,
        onClick = onClick,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    )
}

@Composable
private fun SymptomRow(onPick: (SymptomLevel) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SymptomLevel.entries.forEach { level ->
            val label = level.name.lowercase().replaceFirstChar { c -> c.uppercase() }
            FilterChip(
                selected = false,
                onClick = { onPick(level) },
                label = { Text(label) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )
        }
    }
}
