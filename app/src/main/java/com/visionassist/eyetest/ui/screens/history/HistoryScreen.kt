package com.visionassist.eyetest.ui.screens.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.visionassist.eyetest.R
import com.visionassist.eyetest.domain.model.TestResultRecord
import com.visionassist.eyetest.domain.model.VisionSessionResult
import com.visionassist.eyetest.ui.VisionAssistViewModelFactory
import com.visionassist.eyetest.ui.components.ClinicalCard
import com.visionassist.eyetest.ui.components.InfoBanner
import com.visionassist.eyetest.ui.components.ScaffoldListContent
import com.visionassist.eyetest.ui.components.VisionAssistScaffold
import java.text.DateFormat
import java.util.Date

@Composable
fun HistoryScreen(
    factory: VisionAssistViewModelFactory,
    onNavigateUp: () -> Unit
) {
    val vm: HistoryViewModel = viewModel(factory = factory)
    val records by vm.records.collectAsStateWithLifecycle()

    VisionAssistScaffold(
        title = stringResource(R.string.history_title),
        onNavigateUp = onNavigateUp
    ) { padding ->
        ScaffoldListContent(paddingValues = padding) {
            ClinicalCard {
                Text(
                    trendSummaryText(records),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(8.dp))
            if (records.isEmpty()) {
                InfoBanner(
                    text = stringResource(R.string.result_empty_summary),
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    items(records, key = { it.id }) { row ->
                        HistoryRow(record = row)
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryRow(record: TestResultRecord) {
    val df = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
    val s = record.session
    ClinicalCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            df.format(Date(record.timestampMillis)),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            stringResource(
                R.string.history_row_acuity_fmt,
                s.acuityRight?.snellenApproxLabel ?: "—",
                s.acuityLeft?.snellenApproxLabel ?: "—",
                s.reliability.name
            ),
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun trendSummaryText(records: List<TestResultRecord>): String {
    if (records.size < 2) return stringResource(R.string.history_trend_need_more)
    val newer = records[0].session
    val older = records[1].session
    val aNew = acuityAvg(newer)
    val aOld = acuityAvg(older)
    return when {
        aNew > aOld + 0.5f -> stringResource(R.string.history_trend_improved)
        aNew < aOld - 0.5f -> stringResource(R.string.history_trend_declined)
        else -> stringResource(R.string.history_trend_stable)
    }
}

private fun acuityAvg(s: VisionSessionResult): Float {
    val r = s.acuityRight?.levelIndexAchieved ?: 0
    val l = s.acuityLeft?.levelIndexAchieved ?: 0
    return (r + l) / 2f
}
