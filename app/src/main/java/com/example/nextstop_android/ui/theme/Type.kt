package com.example.nextstop_android.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
// 🔑 IMPORTANT: Use your project's R, not the library's R
import com.example.nextstop_android.R

// 1. Define the Google Font Provider
val provider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs
)

// 2. Define the Rubik Font Family
val RubikFontFamily = FontFamily(
    Font(googleFont = GoogleFont("Rubik"), fontProvider = provider),
    Font(googleFont = GoogleFont("Rubik"), fontProvider = provider, weight = FontWeight.Bold),
    Font(googleFont = GoogleFont("Rubik"), fontProvider = provider, weight = FontWeight.Medium)
)


private val defaultTypography = Typography()

// 3. Map styles while keeping default weights and heights
val Typography = Typography(
    displayLarge = defaultTypography.displayLarge.copy(fontFamily = RubikFontFamily),
    displayMedium = defaultTypography.displayMedium.copy(fontFamily = RubikFontFamily),
    displaySmall = defaultTypography.displaySmall.copy(fontFamily = RubikFontFamily),
    headlineLarge = defaultTypography.headlineLarge.copy(fontFamily = RubikFontFamily),
    headlineMedium = defaultTypography.headlineMedium.copy(fontFamily = RubikFontFamily),
    headlineSmall = defaultTypography.headlineSmall.copy(fontFamily = RubikFontFamily),
    titleLarge = defaultTypography.titleLarge.copy(fontFamily = RubikFontFamily),
    titleMedium = defaultTypography.titleMedium.copy(fontFamily = RubikFontFamily),
    titleSmall = defaultTypography.titleSmall.copy(fontFamily = RubikFontFamily),
    bodyLarge = defaultTypography.bodyLarge.copy(fontFamily = RubikFontFamily),
    bodyMedium = defaultTypography.bodyMedium.copy(fontFamily = RubikFontFamily),
    bodySmall = defaultTypography.bodySmall.copy(fontFamily = RubikFontFamily),
    labelLarge = defaultTypography.labelLarge.copy(fontFamily = RubikFontFamily),
    labelMedium = defaultTypography.labelMedium.copy(fontFamily = RubikFontFamily),
    labelSmall = defaultTypography.labelSmall.copy(fontFamily = RubikFontFamily)
)