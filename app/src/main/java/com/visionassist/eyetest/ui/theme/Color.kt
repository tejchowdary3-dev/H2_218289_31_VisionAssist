package com.visionassist.eyetest.ui.theme

import androidx.compose.ui.graphics.Color

// —— Black + blue + white app chrome (clean + premium) ——
val AppBrandRed = Color(0xFF4DA3FF)
val AppBrandRedDark = Color(0xFF2563EB)
val AppBrandRedSoft = Color(0xFFF3F8FF)
val AppBrandRedSoftStrong = Color(0xFFE3EEFF)

val AppWhite = Color(0xFFFFFFFF)
val AppSurface = Color(0xFFFFFFFF)
val AppOnSurface = Color(0xFF0B1220)
val AppOnSurfaceMuted = Color(0xFF4B5E7A)
val AppOutline = Color(0xFFCAD8EE)
val AppBlack = Color(0xFF020617)

/** Overlays / scrims (still neutral for readability). */
val AppScrim = Color(0x99000000)

/** Legacy alias: primary brand red used across buttons and accents. */
val StreamingBrand = AppBrandRed

/** Legacy alias: darker red for pressed states and containers. */
val StreamingBrandMuted = AppBrandRedDark

/** Snellen: white panel, dark letters, red frame — matches app theme while staying readable. */
val SnellenPanelBg = Color(0xFFF8FBFF)
val SnellenOptotype = Color(0xFF1A1A1A)
val SnellenPanelBorder = AppBrandRed
