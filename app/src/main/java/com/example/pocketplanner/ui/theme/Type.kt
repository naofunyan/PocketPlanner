package com.example.pocketplanner.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.pocketplanner.R // Don't forget this import!

// Map the single variable font file to the weights we need
val GoogleSansFlex = FontFamily(
    Font(R.font.google_sans_flex, FontWeight.Normal),
    Font(R.font.google_sans_flex, FontWeight.Medium),
    Font(R.font.google_sans_flex, FontWeight.SemiBold),
    Font(R.font.google_sans_flex, FontWeight.Bold),
    Font(R.font.google_sans_flex, FontWeight.ExtraBold),
    Font(R.font.google_sans_flex, FontWeight.Black)
)

// We can use the code font for numbers or specific labels later
val GoogleSansCode = FontFamily(
    Font(R.font.google_sans_code, FontWeight.Normal),
    Font(R.font.google_sans_code, FontWeight.Bold)
)

// Apply the font to the Material Typography
val Typography = Typography(
    displayLarge = TextStyle(
        fontFamily = GoogleSansFlex,
        fontWeight = FontWeight.Bold,
        fontSize = 57.sp,
        lineHeight = 64.sp,
        letterSpacing = (-0.25).sp
    ),
    titleLarge = TextStyle(
        fontFamily = GoogleSansFlex,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = GoogleSansFlex,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    ),
    labelMedium = TextStyle(
        fontFamily = GoogleSansFlex,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
)