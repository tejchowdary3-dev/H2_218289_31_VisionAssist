package com.visionassist.eyetest.ui.screens.result

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import com.visionassist.eyetest.ui.VisionAssistViewModelFactory
import com.visionassist.eyetest.ui.components.ClinicalCard
import com.visionassist.eyetest.ui.components.InfoBanner
import com.visionassist.eyetest.ui.components.PrimaryActionButton
import com.visionassist.eyetest.ui.components.ScaffoldScrollableContent
import com.visionassist.eyetest.ui.components.SecondaryActionButton
import com.visionassist.eyetest.ui.components.VisionAssistScaffold

@Composable
fun ResultScreen(
    factory: VisionAssistViewModelFactory,
    onNavigateUp: () -> Unit,
    onFindClinics: () -> Unit,
    onHistory: () -> Unit,
    onNewScreening: () -> Unit
) {
    val vm: ResultViewModel = viewModel(factory = factory)
    val state by vm.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { vm.refresh() }

    VisionAssistScaffold(
        title = stringResource(R.string.result_title),
        onNavigateUp = onNavigateUp
    ) { padding ->
        ScaffoldScrollableContent(paddingValues = padding) {
            InfoBanner(state.summary)
            state.session?.let { s ->
                ClinicalCard {
                    Text(
                        "${stringResource(R.string.reliability)}: ${s.reliability}",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            ClinicalCard {
                Text(
                    stringResource(R.string.key_findings),
                    style = MaterialTheme.typography.titleMedium
                )
                state.keyFindings.forEach { line ->
                    Text("• $line", style = MaterialTheme.typography.bodyMedium)
                }
            }
            state.refractionCard?.let { card ->
                ClinicalCard {
                    Text(
                        stringResource(R.string.result_refraction_title),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(card.severityLine, style = MaterialTheme.typography.bodyLarge)
                    card.rangeLine?.let { range ->
                        Spacer(Modifier.height(6.dp))
                        Text(range, style = MaterialTheme.typography.bodyMedium)
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        card.disclaimerLine,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            ClinicalCard {
                Text(stringResource(R.string.risk_level), style = MaterialTheme.typography.titleMedium)
                Text(state.riskLabel, style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.height(8.dp))
                Text(stringResource(R.string.recommendation), style = MaterialTheme.typography.titleMedium)
                Text(state.recommendation, style = MaterialTheme.typography.bodyLarge)
            }
            Text(
                stringResource(R.string.disclaimer_full),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            PrimaryActionButton(
                text = stringResource(R.string.find_clinics),
                onClick = onFindClinics,
                modifier = Modifier.fillMaxWidth()
            )
            SecondaryActionButton(
                text = stringResource(R.string.view_history),
                onClick = onHistory
            )
            SecondaryActionButton(
                text = stringResource(R.string.new_screening),
                onClick = onNewScreening
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}
