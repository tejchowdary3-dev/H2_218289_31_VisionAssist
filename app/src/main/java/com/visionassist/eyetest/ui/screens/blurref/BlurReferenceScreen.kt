package com.visionassist.eyetest.ui.screens.blurref

import android.os.Build
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.visionassist.eyetest.R
import com.visionassist.eyetest.domain.model.BlurLadderTier
import com.visionassist.eyetest.domain.model.FocalDifficultyPattern
import com.visionassist.eyetest.ui.VisionAssistViewModelFactory
import com.visionassist.eyetest.ui.components.ClinicalCard
import com.visionassist.eyetest.ui.components.InfoBanner
import com.visionassist.eyetest.ui.components.PrimaryActionButton
import com.visionassist.eyetest.ui.components.ScaffoldScrollableContent
import com.visionassist.eyetest.ui.components.VisionAssistScaffold

@Composable
fun BlurReferenceScreen(
    factory: VisionAssistViewModelFactory,
    onNavigateUp: () -> Unit,
    onContinue: () -> Unit
) {
    val vm: BlurReferenceViewModel = viewModel(factory = factory)
    val ui by vm.state.collectAsStateWithLifecycle()
    val pattern = vm.focalPattern

    VisionAssistScaffold(
        title = stringResource(R.string.blur_reference_title),
        onNavigateUp = onNavigateUp
    ) { padding ->
        ScaffoldScrollableContent(paddingValues = padding) {
            InfoBanner(stringResource(R.string.blur_reference_disclaimer))
            Spacer(Modifier.height(12.dp))
            ClinicalCard {
                Text(
                    text = promptForPattern(pattern),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                Spacer(Modifier.height(8.dp))
                Text(
                    stringResource(R.string.blur_reference_api_note),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
            Spacer(Modifier.height(16.dp))
            ui.ladder.tiers.forEachIndexed { index, tier ->
                BlurTierRow(
                    tier = tier,
                    sampleLine = ui.ladder.sampleLine,
                    selected = ui.selectedTierIndex == index,
                    onClick = { vm.selectTier(index) }
                )
                Spacer(Modifier.height(10.dp))
            }
            Spacer(Modifier.height(8.dp))
            PrimaryActionButton(
                text = stringResource(R.string.action_continue),
                onClick = {
                    vm.persistSelection()
                    onContinue()
                },
                enabled = ui.selectedTierIndex != null,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun promptForPattern(pattern: FocalDifficultyPattern?): String {
    return when (pattern) {
        FocalDifficultyPattern.DISTANT_HARDER ->
            stringResource(R.string.blur_prompt_distant)
        FocalDifficultyPattern.NEAR_HARDER ->
            stringResource(R.string.blur_prompt_near)
        FocalDifficultyPattern.BOTH ->
            stringResource(R.string.blur_prompt_both)
        FocalDifficultyPattern.NEITHER, null ->
            stringResource(R.string.blur_prompt_neutral)
    }
}

@Composable
private fun BlurTierRow(
    tier: BlurLadderTier,
    sampleLine: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val density = LocalDensity.current
    val blurPx = with(density) { tier.blurDp.dp.toPx() }
    ClinicalCard {
        Text(
            tier.title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 88.dp)
                .clip(RoundedCornerShape(12.dp))
                .border(
                    width = if (selected) 3.dp else 1.dp,
                    color = if (selected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.outlineVariant,
                    shape = RoundedCornerShape(12.dp)
                )
                .clickable(onClick = onClick)
                .padding(12.dp),
            contentAlignment = Alignment.Center
        ) {
            val textModifier = Modifier
                .then(
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && blurPx > 0.5f) {
                        Modifier.graphicsLayer {
                            renderEffect = android.graphics.RenderEffect
                                .createBlurEffect(
                                    blurPx,
                                    blurPx,
                                    android.graphics.Shader.TileMode.CLAMP
                                )
                                .asComposeRenderEffect()
                        }
                    } else if (blurPx > 0.5f) {
                        Modifier.graphicsLayer {
                            alpha = (1f - (blurPx / 80f).coerceIn(0.15f, 0.55f))
                        }
                    } else {
                        Modifier
                    }
                )
            Text(
                text = sampleLine,
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                ),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = textModifier
            )
        }
    }
}
