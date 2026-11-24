package com.lightshare.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val LightShareTypography = Typography().let {
    it.copy(
        headlineLarge = TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Light, fontSize = 38.sp, letterSpacing = 0.5.sp),
        headlineMedium = TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Light, fontSize = 28.sp),
        titleLarge = TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Normal, fontSize = 22.sp)
    )
}
