package com.visionassist.eyetest.ui

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.visionassist.eyetest.VisionAssistApp
import com.visionassist.eyetest.data.repository.VisionAssistRepository
import com.visionassist.eyetest.ui.screens.blurref.BlurReferenceViewModel
import com.visionassist.eyetest.ui.screens.calibration.CalibrationViewModel
import com.visionassist.eyetest.ui.screens.clinics.ClinicListViewModel
import com.visionassist.eyetest.ui.screens.history.HistoryViewModel
import com.visionassist.eyetest.ui.screens.onboarding.OnboardingViewModel
import com.visionassist.eyetest.ui.screens.questionnaire.QuestionnaireViewModel
import com.visionassist.eyetest.ui.screens.result.ResultViewModel
import com.visionassist.eyetest.ui.screens.vision.VisionTestViewModel

class VisionAssistViewModelFactory(
    private val application: Application
) : ViewModelProvider.Factory {

    private val repo: VisionAssistRepository
        get() = (application as VisionAssistApp).repository

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(OnboardingViewModel::class.java) ->
                OnboardingViewModel(repo) as T
            modelClass.isAssignableFrom(CalibrationViewModel::class.java) ->
                CalibrationViewModel(repo) as T
            modelClass.isAssignableFrom(QuestionnaireViewModel::class.java) ->
                QuestionnaireViewModel(repo) as T
            modelClass.isAssignableFrom(BlurReferenceViewModel::class.java) ->
                BlurReferenceViewModel(repo) as T
            modelClass.isAssignableFrom(VisionTestViewModel::class.java) ->
                VisionTestViewModel(repo) as T
            modelClass.isAssignableFrom(ResultViewModel::class.java) ->
                ResultViewModel(application, repo) as T
            modelClass.isAssignableFrom(ClinicListViewModel::class.java) ->
                ClinicListViewModel(repo) as T
            modelClass.isAssignableFrom(HistoryViewModel::class.java) ->
                HistoryViewModel(repo) as T
            else -> throw IllegalArgumentException("Unknown VM ${modelClass.name}")
        }
    }
}
