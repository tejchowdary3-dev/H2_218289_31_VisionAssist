package com.visionassist.eyetest.ui.screens.vision

import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.visionassist.eyetest.data.repository.VisionAssistRepository
import com.visionassist.eyetest.domain.engine.ReliabilityEngine
import com.visionassist.eyetest.domain.engine.VisionTestEngine
import com.visionassist.eyetest.domain.model.AcuityResult
import com.visionassist.eyetest.domain.model.TestedEye
import com.visionassist.eyetest.domain.model.VisionTestConfig
import com.visionassist.eyetest.domain.model.VisionTestConfigDefaults
import com.visionassist.eyetest.domain.model.VisionSessionResult
import kotlin.math.sqrt
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class VisionTestPhase {
    ACUITY,
    NEAR,
    ASTIGMATISM,
    CONTRAST,
    DONE
}

data class VisionTestUiState(
    val internalPhase: VisionTestPhase,
    val testedEye: TestedEye,
    val instruction: String,
    val allowedLetters: String,
    val line: String,
    val levelLabel: String,
    val baseFontSp: Float,
    val userEntry: String,
    val helper: String?,
    val lightingGood: Boolean = true,
    val distanceGood: Boolean = true,
    val nearChoiceIndex: Int? = null,
    val astigmatismUneven: Boolean? = null,
    val contrastRound: Int = 0,
    val contrastCorrectTargetOnLeft: Boolean = false,
    val navigateToResults: Boolean = false
)

class VisionTestViewModel(
    private val repository: VisionAssistRepository
) : ViewModel() {

    private var config: VisionTestConfig = repository.currentVisionTestConfig()
    private var engine = VisionTestEngine(config)
    private val reliabilityEngine = ReliabilityEngine()

    private val timingsMs = ArrayList<Long>()
    private val lineCorrectCounts = ArrayList<Int>()
    private var totalRetries = 0

    private var acuityEyeQueue = listOf(TestedEye.RIGHT, TestedEye.LEFT)
    private var acuityEyeIndex = 0
    private var levelIndex = 0
    private var bestPassedLevel = -1
    private var retryOnLevel = 0
    private var retriesThisEye = 0
    private var lineStartedAt = 0L

    private var acuityRight: AcuityResult? = null
    private var acuityLeft: AcuityResult? = null

    private var nearScore = 0.5f
    private var astigmatismFlag = false
    private var contrastScore = 0.5f
    private var contrastRoundIndex = 0
    private var contrastCorrect = 0
    private var astigTransitionJob: Job? = null

    private val _ui = MutableStateFlow(buildInitialAcuityState())
    val uiState: StateFlow<VisionTestUiState> = _ui.asStateFlow()

    init {
        viewModelScope.launch {
            val fetched = repository.fetchVisionTestConfig()
            val shouldApply = fetched != config &&
                _ui.value.internalPhase == VisionTestPhase.ACUITY &&
                timingsMs.isEmpty() &&
                _ui.value.userEntry.isEmpty()
            config = fetched
            engine = VisionTestEngine(config)
            if (shouldApply) {
                _ui.value = buildInitialAcuityState()
            }
        }
    }

    private fun buildInitialAcuityState(): VisionTestUiState {
        val safeConfig = configOrDefault()
        val eye = acuityEyeQueue[acuityEyeIndex]
        val line = engine.randomLine(safeConfig.lineLength)
        lineStartedAt = SystemClock.elapsedRealtime()
        val env = repository.environmentFlags
        return VisionTestUiState(
            internalPhase = VisionTestPhase.ACUITY,
            testedEye = eye,
            instruction = engine.instructionForEye(eye),
            allowedLetters = safeConfig.letters,
            line = line,
            levelLabel = engine.labelForLevel(levelIndex),
            baseFontSp = engine.fontSizeSpForLevel(levelIndex) * repository.calibrationScale,
            userEntry = "",
            helper = safeConfig.initialHelper,
            lightingGood = env.lightingGood,
            distanceGood = env.distanceGood
        )
    }

    fun appendLetter(char: Char) {
        val upper = char.uppercaseChar()
        if (upper !in configOrDefault().letters) return
        _ui.update { s ->
            if (s.userEntry.length >= s.line.length) s
            else s.copy(userEntry = s.userEntry + upper)
        }
    }

    fun clearEntry() {
        _ui.update { it.copy(userEntry = "") }
    }

    fun submitLine() {
        val snap = _ui.value
        if (snap.internalPhase != VisionTestPhase.ACUITY) return
        val elapsed = (SystemClock.elapsedRealtime() - lineStartedAt).coerceAtLeast(1L)
        timingsMs.add(elapsed)
        val correct = snap.userEntry.zip(snap.line).count { it.first == it.second }
        lineCorrectCounts.add(correct)

        if (engine.linePassed(correct)) {
            bestPassedLevel = maxOf(bestPassedLevel, levelIndex)
            if (levelIndex >= engine.snellenLevels.lastIndex) {
                finishCurrentEye()
            } else {
                levelIndex++
                retryOnLevel = 0
                showNewLine()
            }
        } else {
            if (retryOnLevel < configOrDefault().maxRetriesPerLevel) {
                retryOnLevel++
                totalRetries++
                retriesThisEye++
                showNewLine()
            } else {
                finishCurrentEye()
            }
        }
    }

    private fun showNewLine() {
        val line = engine.randomLine(configOrDefault().lineLength)
        lineStartedAt = SystemClock.elapsedRealtime()
        _ui.update { s ->
            s.copy(
                line = line,
                levelLabel = engine.labelForLevel(levelIndex),
                baseFontSp = engine.fontSizeSpForLevel(levelIndex) * repository.calibrationScale,
                userEntry = "",
                helper = if (retryOnLevel > 0) configOrDefault().retryHelper else null
            )
        }
    }

    private fun finishCurrentEye() {
        val eye = acuityEyeQueue[acuityEyeIndex]
        val labelIdx = bestPassedLevel.coerceAtLeast(0)
        val label = if (bestPassedLevel < 0) {
            "${engine.labelForLevel(0)} or below (screening)"
        } else {
            engine.labelForLevel(labelIdx)
        }
        val result = AcuityResult(
            eye = eye,
            snellenApproxLabel = label,
            levelIndexAchieved = bestPassedLevel.coerceAtLeast(0),
            retriesUsed = retriesThisEye
        )
        when (eye) {
            TestedEye.RIGHT -> acuityRight = result
            TestedEye.LEFT -> acuityLeft = result
        }
        if (acuityEyeIndex >= acuityEyeQueue.lastIndex) {
            startNearPhase()
        } else {
            acuityEyeIndex++
            levelIndex = 0
            bestPassedLevel = -1
            retryOnLevel = 0
            retriesThisEye = 0
            val nextEye = acuityEyeQueue[acuityEyeIndex]
            val safeConfig = configOrDefault()
            val line = engine.randomLine(safeConfig.lineLength)
            lineStartedAt = SystemClock.elapsedRealtime()
            val env = repository.environmentFlags
            _ui.update {
                VisionTestUiState(
                    internalPhase = VisionTestPhase.ACUITY,
                    testedEye = nextEye,
                    instruction = engine.instructionForEye(nextEye),
                    allowedLetters = safeConfig.letters,
                    line = line,
                    levelLabel = engine.labelForLevel(0),
                    baseFontSp = engine.fontSizeSpForLevel(0) * repository.calibrationScale,
                    userEntry = "",
                    helper = safeConfig.switchEyeHelper,
                    lightingGood = env.lightingGood,
                    distanceGood = env.distanceGood
                )
            }
        }
    }

    private fun startNearPhase() {
        val safeConfig = configOrDefault()
        val prev = _ui.value
        _ui.update {
            VisionTestUiState(
                internalPhase = VisionTestPhase.NEAR,
                testedEye = TestedEye.RIGHT,
                instruction = safeConfig.nearPhaseInstruction,
                allowedLetters = safeConfig.letters,
                line = "",
                levelLabel = "",
                baseFontSp = 18f * repository.calibrationScale,
                userEntry = "",
                helper = null,
                lightingGood = prev.lightingGood,
                distanceGood = prev.distanceGood,
                nearChoiceIndex = null
            )
        }
    }

    fun selectNearVisionChoice(index: Int) {
        astigTransitionJob?.cancel()
        nearScore = configOrDefault().nearOptionScores.getOrElse(index) { 0.88f }
        val safeConfig = configOrDefault()
        _ui.update {
            it.copy(
                nearChoiceIndex = index,
                internalPhase = VisionTestPhase.ASTIGMATISM,
                instruction = safeConfig.astigPhaseInstruction,
                helper = safeConfig.astigHelper,
                astigmatismUneven = null,
                lightingGood = it.lightingGood,
                distanceGood = it.distanceGood
            )
        }
    }

    fun setAstigmatismUneven(uneven: Boolean) {
        astigTransitionJob?.cancel()
        astigmatismFlag = uneven
        _ui.update { it.copy(astigmatismUneven = uneven) }
        astigTransitionJob = viewModelScope.launch {
            delay(280)
            if (_ui.value.internalPhase != VisionTestPhase.ASTIGMATISM) return@launch
            startContrastPhase()
        }
    }

    private fun startContrastPhase() {
        contrastRoundIndex = 0
        contrastCorrect = 0
        newContrastRound()
    }

    private fun newContrastRound() {
        val correctOnLeft = kotlin.random.Random.nextBoolean()
        val safeConfig = configOrDefault()
        _ui.update {
            it.copy(
                internalPhase = VisionTestPhase.CONTRAST,
                instruction = safeConfig.contrastInstruction,
                helper = "Round ${contrastRoundIndex + 1} of ${safeConfig.contrastRounds}",
                contrastRound = contrastRoundIndex,
                contrastCorrectTargetOnLeft = correctOnLeft,
                astigmatismUneven = null,
                lightingGood = it.lightingGood,
                distanceGood = it.distanceGood
            )
        }
    }

    fun answerContrast(leftSelected: Boolean) {
        val correctSideIsLeft = _ui.value.contrastCorrectTargetOnLeft
        val correct = leftSelected == correctSideIsLeft
        if (correct) contrastCorrect++
        contrastRoundIndex++
        val rounds = configOrDefault().contrastRounds.toFloat()
        if (contrastRoundIndex >= configOrDefault().contrastRounds) {
            contrastScore = contrastCorrect / rounds
            finalizeSession()
        } else {
            newContrastRound()
        }
    }

    private fun finalizeSession() {
        val avgMs = if (timingsMs.isEmpty()) 3000L else timingsMs.average().toLong()
        val consistency = lineConsistency01(lineCorrectCounts)
        val (rel, notes) = reliabilityEngine.evaluate(
            avgResponseTimeMs = avgMs,
            lineConsistency01 = consistency,
            totalRetries = totalRetries,
            environment = repository.environmentFlags
        )
        val session = VisionSessionResult(
            acuityRight = acuityRight,
            acuityLeft = acuityLeft,
            nearVisionScore01 = nearScore,
            astigmatismFlag = astigmatismFlag,
            contrastSensitivity01 = contrastScore,
            reliability = rel,
            reliabilityNotes = notes,
            responseTimeAvgMs = avgMs,
            lineConsistencyScore01 = consistency,
            blurIllustrationTierIndex = repository.blurIllustrationLevelIndex
        )
        viewModelScope.launch {
            repository.commitSessionResult(session)
            _ui.update { it.copy(internalPhase = VisionTestPhase.DONE, navigateToResults = true) }
        }
    }

    fun consumeNavigateToResults() {
        _ui.update { it.copy(navigateToResults = false) }
    }

    fun setLightingGood(good: Boolean) {
        val e = repository.environmentFlags
        repository.setEnvironment(e.copy(lightingGood = good))
        _ui.update { it.copy(lightingGood = good) }
    }

    fun setDistanceGood(good: Boolean) {
        val e = repository.environmentFlags
        repository.setEnvironment(e.copy(distanceGood = good))
        _ui.update { it.copy(distanceGood = good) }
    }

    private fun lineConsistency01(counts: List<Int>): Float {
        if (counts.size < 2) return 0.75f
        val mean = counts.average()
        val varSum = counts.sumOf { (it - mean) * (it - mean) }
        val stdev = sqrt(varSum / counts.size)
        val normalized = (stdev / 2.5).coerceIn(0.0, 1.0)
        return (1.0 - normalized).toFloat()
    }

    private fun configOrDefault(): VisionTestConfig {
        if (config.snellenLevels.isEmpty()) {
            config = VisionTestConfigDefaults.create()
            engine = VisionTestEngine(config)
        }
        return config
    }
}
