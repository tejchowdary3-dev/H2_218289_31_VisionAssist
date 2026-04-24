package com.visionassist.eyetest.ui.screens.result

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.visionassist.eyetest.R
import com.visionassist.eyetest.data.repository.VisionAssistRepository
import com.visionassist.eyetest.domain.model.RefractionSeverity
import com.visionassist.eyetest.domain.model.ReliabilityLevel
import com.visionassist.eyetest.domain.model.VisionSessionResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class RefractionResultCardUi(
    val severityLine: String,
    val rangeLine: String?,
    val disclaimerLine: String
)

data class ResultUiState(
    val session: VisionSessionResult?,
    val summary: String,
    val keyFindings: List<String>,
    val refractionCard: RefractionResultCardUi?,
    val riskLabel: String,
    val recommendation: String
)

class ResultViewModel(
    application: Application,
    private val repository: VisionAssistRepository
) : AndroidViewModel(application) {

    private val _ui = MutableStateFlow(buildState(repository))
    val uiState: StateFlow<ResultUiState> = _ui.asStateFlow()

    fun refresh() {
        viewModelScope.launch {
            repository.hydrateLastSessionFromHistory()
            _ui.value = buildState(repository)
        }
    }

    private fun buildState(repo: VisionAssistRepository): ResultUiState {
        val app = getApplication<Application>()
        val s = repo.lastSessionResult
        if (s == null) {
            return ResultUiState(
                session = null,
                summary = app.getString(R.string.result_empty_summary),
                keyFindings = emptyList(),
                refractionCard = null,
                riskLabel = app.getString(R.string.result_empty_risk),
                recommendation = app.getString(R.string.result_empty_recommendation)
            )
        }
        val blurTierCount = repo.currentBlurLadder().tiers.size
        val findings = buildList {
            s.acuityRight?.let {
                add(app.getString(R.string.result_finding_od_acuity, it.snellenApproxLabel))
            }
            s.acuityLeft?.let {
                add(app.getString(R.string.result_finding_os_acuity, it.snellenApproxLabel))
            }
            add(app.getString(R.string.result_finding_near_score, (s.nearVisionScore01 * 100).toInt()))
            add(app.getString(R.string.result_finding_contrast_score, (s.contrastSensitivity01 * 100).toInt()))
            if (s.astigmatismFlag) add(app.getString(R.string.result_finding_astig_reported))
            s.blurIllustrationTierIndex?.let { idx ->
                add(
                    app.getString(
                        R.string.result_finding_blur_ladder,
                        idx + 1,
                        blurTierCount
                    )
                )
            }
        }
        val estimate = repo.estimateRefraction(s)
        val refractionCard = RefractionResultCardUi(
            severityLine = when (estimate.severity) {
                RefractionSeverity.NONE -> app.getString(R.string.result_refraction_sev_none)
                RefractionSeverity.MILD -> app.getString(R.string.result_refraction_sev_mild)
                RefractionSeverity.MODERATE -> app.getString(R.string.result_refraction_sev_moderate)
                RefractionSeverity.STRONG -> app.getString(R.string.result_refraction_sev_strong)
                RefractionSeverity.INDETERMINATE -> app.getString(R.string.result_refraction_sev_indeterminate)
            },
            rangeLine = estimate.diopterFeelLow?.let { lo ->
                estimate.diopterFeelHigh?.let { hi ->
                    if (hi > 0.01f) {
                        app.getString(R.string.result_refraction_range_fmt, lo, hi)
                    } else null
                }
            },
            disclaimerLine = app.getString(R.string.result_refraction_disclaimer)
        )
        val kind = riskKind(s)
        val riskLabel = when (kind) {
            RiskKind.Caution -> app.getString(R.string.risk_caution)
            RiskKind.Exam -> app.getString(R.string.risk_consider_exam)
            RiskKind.Routine -> app.getString(R.string.risk_routine)
        }
        val recommendation = when (kind) {
            RiskKind.Exam -> app.getString(R.string.rec_book_exam)
            RiskKind.Caution -> app.getString(R.string.rec_repeat_screening)
            RiskKind.Routine -> app.getString(R.string.rec_maintain_exams)
        }
        val summary = app.getString(
            R.string.result_summary_fmt,
            s.reliability.name,
            s.reliabilityNotes
        )
        return ResultUiState(
            session = s,
            summary = summary,
            keyFindings = findings,
            refractionCard = refractionCard,
            riskLabel = riskLabel,
            recommendation = recommendation
        )
    }

    private enum class RiskKind { Caution, Exam, Routine }

    private fun riskKind(s: VisionSessionResult): RiskKind = when {
        s.reliability == ReliabilityLevel.LOW -> RiskKind.Caution
        s.acuityRight != null && s.acuityLeft != null &&
            (s.acuityRight.levelIndexAchieved < 3 || s.acuityLeft.levelIndexAchieved < 3) -> RiskKind.Exam
        else -> RiskKind.Routine
    }
}
