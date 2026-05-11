package com.streamvault.app.ui.design

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

data class AppSpacing(
    val xs: Dp = 10.dp,
    val sm: Dp = 16.dp,
    val md: Dp = 24.dp,
    val lg: Dp = 32.dp,
    val xl: Dp = 48.dp,
    val xxl: Dp = 64.dp,
    val screenGutter: Dp = 64.dp,
    val railWidth: Dp = 148.dp,
    val sectionGap: Dp = 40.dp,
    val cardGap: Dp = 20.dp,
    val chipGap: Dp = 12.dp,
    val safeTop: Dp = 40.dp,
    val safeBottom: Dp = 40.dp,
    val safeHoriz: Dp = 64.dp
)

val LocalAppSpacing = staticCompositionLocalOf { AppSpacing() }
