package com.visionassist.eyetest.ui.screens.vision

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.visionassist.eyetest.R
import com.visionassist.eyetest.VisionAssistApp
import com.visionassist.eyetest.ui.ar.BackCameraPreview
import com.visionassist.eyetest.domain.model.TestedEye
import com.visionassist.eyetest.ui.VisionAssistViewModelFactory
import com.visionassist.eyetest.ui.components.ClinicalCard
import com.visionassist.eyetest.ui.components.ClinicalStepProgress
import com.visionassist.eyetest.ui.components.EnvironmentToggleRow
import com.visionassist.eyetest.ui.components.InfoBanner
import com.visionassist.eyetest.ui.components.VisionAssistScaffold
import com.visionassist.eyetest.ui.theme.SnellenOptotype
import com.visionassist.eyetest.ui.theme.SnellenPanelBg
import com.visionassist.eyetest.ui.theme.SnellenPanelBorder
import com.visionassist.eyetest.ui.theme.VisionAssistHorizontalPadding
import com.visionassist.eyetest.ui.theme.VisionAssistMaxContentWidth
import com.visionassist.eyetest.ui.theme.VisionAssistVerticalPadding
import com.visionassist.eyetest.ui.speech.rememberVisionAssistVoice
import kotlinx.coroutines.launch
import kotlin.math.min

@Composable
fun VisionTestScreen(
    factory: VisionAssistViewModelFactory,
    onNavigateUp: () -> Unit,
    onFinished: () -> Unit
) {
    val vm: VisionTestViewModel = viewModel(factory = factory)
    val state by vm.uiState.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()

    LaunchedEffect(state.navigateToResults) {
        if (!state.navigateToResults) return@LaunchedEffect
        vm.consumeNavigateToResults()
        onFinished()
    }

    val (step, total, stepLabel) = visionStepProgress(state)

    VisionAssistScaffold(
        title = stringResource(R.string.vision_test_title),
        onNavigateUp = onNavigateUp,
        bottomBar = {
            if (state.internalPhase == VisionTestPhase.ACUITY) {
                Surface(
                    shadowElevation = 8.dp,
                    tonalElevation = 2.dp,
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.navigationBarsPadding()
                ) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = VisionAssistHorizontalPadding, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = vm::clearEntry,
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(stringResource(R.string.clear))
                        }
                        Button(
                            onClick = vm::submitLine,
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                stringResource(R.string.submit_line_reading),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = VisionAssistMaxContentWidth)
                    .align(Alignment.TopCenter)
                    .verticalScroll(scrollState)
                    .padding(
                        horizontal = VisionAssistHorizontalPadding,
                        vertical = VisionAssistVerticalPadding
                    ),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ClinicalStepProgress(
                    currentStep = step,
                    totalSteps = total,
                    stepLabel = stepLabel
                )

                if (state.internalPhase == VisionTestPhase.ACUITY) {
                    ClinicalCard {
                        Text(
                            stringResource(R.string.pre_exam_checklist_title),
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            stringResource(R.string.pre_exam_checklist_body),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                InfoBanner(stringResource(R.string.snellen_hospital_tip))

                ClinicalCard {
                    Text(
                        stringResource(R.string.test_environment_title),
                        style = MaterialTheme.typography.titleMedium
                    )
                    EnvironmentToggleRow(
                        label = stringResource(R.string.good_lighting),
                        checked = state.lightingGood,
                        onCheckedChange = vm::setLightingGood
                    )
                    EnvironmentToggleRow(
                        label = stringResource(R.string.arms_length_distance),
                        checked = state.distanceGood,
                        onCheckedChange = vm::setDistanceGood
                    )
                }

                when (state.internalPhase) {
                    VisionTestPhase.ACUITY -> AcuityColumn(state, vm)
                    VisionTestPhase.NEAR -> NearClinicalContent(vm)
                    VisionTestPhase.ASTIGMATISM -> AstigClinicalContent(state, vm)
                    VisionTestPhase.CONTRAST -> ContrastClinicalContent(state, vm)
                    VisionTestPhase.DONE -> Unit
                }
            }
        }
    }
}

@Composable
private fun visionStepProgress(state: VisionTestUiState): Triple<Int, Int, String> {
    return when (state.internalPhase) {
        VisionTestPhase.ACUITY -> when (state.testedEye) {
            TestedEye.RIGHT -> Triple(1, 5, stringResource(R.string.phase_acuity_right))
            TestedEye.LEFT -> Triple(2, 5, stringResource(R.string.phase_acuity_left))
        }
        VisionTestPhase.NEAR -> Triple(3, 5, stringResource(R.string.phase_near))
        VisionTestPhase.ASTIGMATISM -> Triple(4, 5, stringResource(R.string.phase_astigmatism))
        VisionTestPhase.CONTRAST -> Triple(5, 5, stringResource(R.string.phase_contrast))
        VisionTestPhase.DONE -> Triple(5, 5, stringResource(R.string.phase_complete))
    }
}

@Composable
private fun AcuityColumn(state: VisionTestUiState, vm: VisionTestViewModel) {
    val context = LocalContext.current
    val voice = rememberVisionAssistVoice()
    val scope = rememberCoroutineScope()
    val coachClient = remember(context) {
        (context.applicationContext as? VisionAssistApp)?.repository?.currentCoachClient()
    }
    var aiHint by remember { mutableStateOf<String?>(null) }
    var aiLoading by remember { mutableStateOf(false) }
    var arPassthroughEnabled by remember { mutableStateOf(false) }

    LaunchedEffect(state.line, state.testedEye) {
        aiHint = null
    }
    var cameraGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        cameraGranted = granted
        if (granted) {
            arPassthroughEnabled = true
        } else {
            arPassthroughEnabled = false
        }
    }

    ClinicalCard {
        Text(
            text = if (state.testedEye == TestedEye.RIGHT) {
                stringResource(R.string.doctor_examiner_script_od)
            } else {
                stringResource(R.string.doctor_examiner_script_os)
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = if (state.testedEye == TestedEye.RIGHT) {
                stringResource(R.string.eye_under_test_right)
            } else {
                stringResource(R.string.eye_under_test_left)
            },
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.secondary,
            fontWeight = FontWeight.Bold
        )
        Text(state.instruction, style = MaterialTheme.typography.bodyLarge)
        Text(
            stringResource(R.string.acuity_row_label, state.levelLabel),
            style = MaterialTheme.typography.titleMedium
        )
    }

    ClinicalCard {
        val eyeShort = if (state.testedEye == TestedEye.RIGHT) {
            stringResource(R.string.voice_eye_right_short)
        } else {
            stringResource(R.string.voice_eye_left_short)
        }
        val instructionSpeech = stringResource(
            R.string.voice_instructions_tts,
            eyeShort,
            state.instruction,
            state.levelLabel
        )
        val hospitalTip = stringResource(R.string.snellen_hospital_tip)
        Text(stringResource(R.string.voice_assistant_title), style = MaterialTheme.typography.titleMedium)
        Text(
            stringResource(R.string.voice_assistant_body),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = { voice.speak("$instructionSpeech $hospitalTip") },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(stringResource(R.string.voice_hear_instructions))
            }
            OutlinedButton(
                onClick = { voice.speakLetterLine(state.line) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(stringResource(R.string.voice_read_letters))
            }
        }
        if (coachClient != null) {
            Spacer(Modifier.height(6.dp))
            Text(
                stringResource(R.string.ai_coach_privacy_note),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            FilledTonalButton(
                onClick = {
                    aiLoading = true
                    val eyeLabel = if (state.testedEye == TestedEye.RIGHT) {
                        context.getString(R.string.voice_eye_right_short)
                    } else {
                        context.getString(R.string.voice_eye_left_short)
                    }
                    scope.launch {
                        val result = coachClient.fetchCoachingTip(
                            testedEyeLabel = eyeLabel,
                            levelLabel = state.levelLabel,
                            onScreenInstruction = state.instruction
                        )
                        aiHint = result.getOrElse { e ->
                            context.getString(R.string.ai_coach_error_fmt, e.message ?: "")
                        }
                        aiLoading = false
                        aiHint?.let { voice.speak(it) }
                    }
                },
                enabled = !aiLoading,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    if (aiLoading) {
                        stringResource(R.string.ai_coach_loading)
                    } else {
                        stringResource(R.string.ai_coach_get_tip)
                    }
                )
            }
            aiHint?.let { hint ->
                Spacer(Modifier.height(8.dp))
                Text(hint, style = MaterialTheme.typography.bodyMedium)
                OutlinedButton(
                    onClick = { voice.speak(hint) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(stringResource(R.string.ai_coach_speak_tip))
                }
            }
        } else {
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(R.string.ai_coach_key_hint),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    ClinicalCard {
        Text(stringResource(R.string.ar_mode_title), style = MaterialTheme.typography.titleMedium)
        Text(
            stringResource(R.string.ar_mode_body),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                stringResource(R.string.ar_toggle_label),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f)
            )
            Switch(
                checked = arPassthroughEnabled,
                onCheckedChange = { on ->
                    if (on) {
                        if (cameraGranted) {
                            arPassthroughEnabled = true
                        } else {
                            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                        }
                    } else {
                        arPassthroughEnabled = false
                    }
                }
            )
        }
        if (arPassthroughEnabled && !cameraGranted) {
            Text(
                stringResource(R.string.ar_camera_denied),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }
    }

    val letterSpacing = min(12f, state.baseFontSp * 0.12f).sp
    val showCamera = arPassthroughEnabled && cameraGranted
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 180.dp, max = 300.dp)
            .clip(RoundedCornerShape(16.dp))
            .border(2.dp, SnellenPanelBorder, RoundedCornerShape(16.dp))
    ) {
        if (showCamera) {
            BackCameraPreview(
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(SnellenPanelBg)
            )
        }
        if (showCamera) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.52f))
            )
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = state.line,
                color = SnellenOptotype,
                fontSize = state.baseFontSp.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = letterSpacing,
                maxLines = 1
            )
        }
    }

    ClinicalCard {
        Text(
            stringResource(R.string.your_response_label),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = if (state.userEntry.isEmpty()) "—" else state.userEntry,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Medium,
            letterSpacing = 4.sp
        )
        state.helper?.let {
            Text(
                it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    Text(
        stringResource(R.string.tap_letters_instruction),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    val letters = state.allowedLetters.toSet().toList().sorted()
    val rows = letters.chunked(4)
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        rows.forEach { rowLetters ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rowLetters.forEach { ch ->
                    Button(
                        onClick = { vm.appendLetter(ch) },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    ) {
                        Text(ch.toString(), fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    }
                }
                repeat(4 - rowLetters.size) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun NearClinicalContent(vm: VisionTestViewModel) {
    ClinicalCard {
        Text(stringResource(R.string.near_vision_title), style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        Text(
            stringResource(R.string.doctor_near_intro),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(8.dp))
        Text(
            stringResource(R.string.near_vision_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
    NearChoiceCard(
        title = stringResource(R.string.near_option_large_title),
        subtitle = stringResource(R.string.near_option_large_sub),
        onClick = { vm.selectNearVisionChoice(0) }
    )
    NearChoiceCard(
        title = stringResource(R.string.near_option_medium_title),
        subtitle = stringResource(R.string.near_option_medium_sub),
        onClick = { vm.selectNearVisionChoice(1) }
    )
    NearChoiceCard(
        title = stringResource(R.string.near_option_small_title),
        subtitle = stringResource(R.string.near_option_small_sub),
        onClick = { vm.selectNearVisionChoice(2) }
    )
}

@Composable
private fun NearChoiceCard(title: String, subtitle: String, onClick: () -> Unit) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(20.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun AstigClinicalContent(state: VisionTestUiState, vm: VisionTestViewModel) {
    ClinicalCard {
        Text(stringResource(R.string.astig_title), style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        Text(
            stringResource(R.string.doctor_astig_intro),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(8.dp))
        Text(
            stringResource(R.string.astig_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(8.dp))
        RadialSpokesPreview()
    }
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
        FilterChip(
            selected = state.astigmatismUneven == false,
            onClick = { vm.setAstigmatismUneven(false) },
            label = { Text(stringResource(R.string.astig_equal)) },
            modifier = Modifier.weight(1f)
        )
        FilterChip(
            selected = state.astigmatismUneven == true,
            onClick = { vm.setAstigmatismUneven(true) },
            label = { Text(stringResource(R.string.astig_uneven)) },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun RadialSpokesPreview() {
    val spokeColor = MaterialTheme.colorScheme.outline
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
    ) {
        val c = Offset(size.width / 2f, size.height / 2f)
        val r = size.minDimension / 2f * 0.85f
        for (i in 0 until 12) {
            rotate(i * 30f, pivot = c) {
                drawLine(
                    color = spokeColor,
                    start = c,
                    end = Offset(c.x, c.y - r),
                    strokeWidth = 3f
                )
            }
        }
    }
}

@Composable
private fun ContrastClinicalContent(state: VisionTestUiState, vm: VisionTestViewModel) {
    ClinicalCard {
        Text(
            stringResource(R.string.doctor_contrast_intro),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
    Spacer(Modifier.height(8.dp))
    state.helper?.let {
        Text(
            it,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 4.dp)
        )
    }
    Text(
        stringResource(R.string.contrast_instruction),
        style = MaterialTheme.typography.bodyLarge,
        modifier = Modifier.padding(bottom = 8.dp)
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val leftStronger = state.contrastCorrectTargetOnLeft
        ContrastChoiceCard(
            label = stringResource(R.string.contrast_side_left),
            strong = leftStronger,
            modifier = Modifier
                .weight(1f)
                .clickable { vm.answerContrast(leftSelected = true) }
        )
        ContrastChoiceCard(
            label = stringResource(R.string.contrast_side_right),
            strong = !leftStronger,
            modifier = Modifier
                .weight(1f)
                .clickable { vm.answerContrast(leftSelected = false) }
        )
    }
}

@Composable
private fun ContrastChoiceCard(
    label: String,
    strong: Boolean,
    modifier: Modifier = Modifier
) {
    val letterStrong = MaterialTheme.colorScheme.onSurface
    val letterWeak = MaterialTheme.colorScheme.onSurfaceVariant
    ElevatedCard(
        modifier = modifier.height(160.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(label, style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(12.dp))
            Text(
                text = "E",
                fontSize = 56.sp,
                color = if (strong) letterStrong else letterWeak,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
