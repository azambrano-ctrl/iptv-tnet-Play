package com.streamvault.app.ui.design

import androidx.compose.ui.graphics.Color

object AppColors {
    // Fondos — profundos para TV (mejor contraste con contenido iluminado)
    val Canvas = Color(0xFF050505)
    val CanvasElevated = Color(0xFF0D0D0D)
    val Surface = Color(0xFF141414)
    val SurfaceElevated = Color(0xFF1E1E1E)
    val SurfaceEmphasis = Color(0xFF252525)
    val SurfaceAccent = Color(0xFF2E2E2E)

    // Marca TNET play: rojo → naranja
    val Brand = Color(0xFFE8001C)           // rojo TNET primario
    val BrandGradientEnd = Color(0xFFFF6200) // naranja TNET
    val BrandMuted = Color(0x40E8001C)
    val BrandStrong = Color(0xFFFF2D1A)     // hover/focus
    val FocusGlow = Color(0x66FF4500)       // glow alrededor de elementos enfocados

    // Foco — blanco puro para máxima visibilidad en TV
    val Focus = Color(0xFFFFFFFF)
    val FocusBorder = Color(0xFFFF4500)     // borde naranja-rojo en foco

    val TextPrimary = Color(0xFFF8F8F8)
    val TextSecondary = Color(0xFFB8B8B8)
    val TextTertiary = Color(0xFF707070)
    val TextDisabled = Color(0xFF444444)

    val Live = Color(0xFFE8001C)
    val Success = Color(0xFF3DCB8A)
    val Warning = Color(0xFFFF6200)
    val Info = Color(0xFF45B8FF)

    val Divider = Color(0x22FFFFFF)
    val Outline = Color(0x33FF4500)

    val HeroTop = Color(0xBB050505)
    val HeroBottom = Color(0xF5050505)

    // Overlay semitransparente para cards en foco
    val CardFocusOverlay = Color(0x20FF4500)
}
