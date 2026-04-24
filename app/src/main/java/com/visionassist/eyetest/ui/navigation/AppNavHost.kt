package com.visionassist.eyetest.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.visionassist.eyetest.ui.VisionAssistViewModelFactory
import com.visionassist.eyetest.ui.screens.calibration.CalibrationScreen
import com.visionassist.eyetest.ui.screens.clinics.ClinicListScreen
import com.visionassist.eyetest.ui.screens.history.HistoryScreen
import com.visionassist.eyetest.ui.screens.onboarding.OnboardingScreen
import com.visionassist.eyetest.ui.screens.blurref.BlurReferenceScreen
import com.visionassist.eyetest.ui.screens.questionnaire.QuestionnaireScreen
import com.visionassist.eyetest.ui.screens.result.ResultScreen
import com.visionassist.eyetest.ui.screens.splash.SplashScreen
import com.visionassist.eyetest.ui.screens.vision.VisionTestScreen

@Composable
fun AppNavHost(
    factory: VisionAssistViewModelFactory,
    navController: NavHostController = rememberNavController()
) {
    NavHost(navController = navController, startDestination = Routes.SPLASH) {
        composable(Routes.SPLASH) {
            SplashScreen(
                onFinished = {
                    navController.navigate(Routes.ONBOARDING) {
                        popUpTo(Routes.SPLASH) { inclusive = true }
                    }
                }
            )
        }
        composable(Routes.ONBOARDING) {
            OnboardingScreen(
                factory = factory,
                onContinue = { navController.navigate(Routes.CALIBRATION) }
            )
        }
        composable(Routes.CALIBRATION) {
            CalibrationScreen(
                factory = factory,
                onNavigateUp = { navController.popBackStack() },
                onContinue = { navController.navigate(Routes.QUESTIONNAIRE) }
            )
        }
        composable(Routes.QUESTIONNAIRE) {
            QuestionnaireScreen(
                factory = factory,
                onNavigateUp = { navController.popBackStack() },
                onContinue = { navController.navigate(Routes.BLUR_REFERENCE) }
            )
        }
        composable(Routes.BLUR_REFERENCE) {
            BlurReferenceScreen(
                factory = factory,
                onNavigateUp = { navController.popBackStack() },
                onContinue = { navController.navigate(Routes.VISION_TEST) }
            )
        }
        composable(Routes.VISION_TEST) {
            VisionTestScreen(
                factory = factory,
                onNavigateUp = { navController.popBackStack() },
                onFinished = { navController.navigate(Routes.RESULT) }
            )
        }
        composable(Routes.RESULT) {
            ResultScreen(
                factory = factory,
                onNavigateUp = { navController.popBackStack() },
                onFindClinics = { navController.navigate(Routes.CLINICS) },
                onHistory = { navController.navigate(Routes.HISTORY) },
                onNewScreening = {
                    navController.navigate(Routes.ONBOARDING) {
                        popUpTo(Routes.SPLASH) { inclusive = false }
                        launchSingleTop = true
                    }
                }
            )
        }
        composable(Routes.CLINICS) {
            ClinicListScreen(
                factory = factory,
                onNavigateUp = { navController.popBackStack() }
            )
        }
        composable(Routes.HISTORY) {
            HistoryScreen(
                factory = factory,
                onNavigateUp = { navController.popBackStack() }
            )
        }
    }
}
