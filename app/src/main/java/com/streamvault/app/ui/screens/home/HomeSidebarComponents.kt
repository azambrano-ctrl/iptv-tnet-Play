package com.streamvault.app.ui.screens.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.SurfaceDefaults
import androidx.tv.material3.Text
import com.streamvault.app.R
import com.streamvault.app.device.rememberIsTelevisionDevice
import com.streamvault.app.ui.components.PlayerRenderView
import com.streamvault.app.ui.interaction.TvButton
import com.streamvault.app.ui.interaction.TvClickableSurface
import com.streamvault.app.ui.theme.FocusBorder
import com.streamvault.app.ui.theme.OnBackground
import com.streamvault.app.ui.theme.OnSurface
import com.streamvault.app.ui.theme.OnSurfaceDim
import com.streamvault.app.ui.theme.Primary
import com.streamvault.app.ui.theme.PrimaryLight
import com.streamvault.app.ui.theme.SurfaceElevated
import com.streamvault.app.ui.theme.SurfaceHighlight
import com.streamvault.app.ui.time.LocalAppTimeFormat
import com.streamvault.app.ui.time.createTimeFormat
import com.streamvault.domain.model.Category
import com.streamvault.domain.model.Channel
import com.streamvault.player.PlayerEngine
import com.streamvault.player.PlayerRenderSurfaceType
import com.streamvault.player.PlayerSurfaceResizeMode
import java.util.Date

@Composable
internal fun CompactSplitLauncherButton(
    slotCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    TvClickableSurface(
        onClick = onClick,
        modifier = modifier
            .widthIn(min = 112.dp)
            .height(34.dp),
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(999.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Primary.copy(alpha = 0.18f),
            focusedContainerColor = Primary.copy(alpha = 0.3f),
            contentColor = OnBackground
        ),
        border = ClickableSurfaceDefaults.border(
            border = Border(
                border = BorderStroke(1.dp, Primary.copy(alpha = 0.55f))
            )
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.action_split),
                style = MaterialTheme.typography.labelSmall,
                color = OnBackground,
                maxLines = 1
            )
            Text(
                text = stringResource(R.string.label_slots_count, slotCount),
                style = MaterialTheme.typography.labelSmall,
                color = PrimaryLight,
                maxLines = 1
            )
        }
    }
}

@Composable
internal fun LivePreviewPane(
    channel: Channel?,
    playerEngine: PlayerEngine?,
    isLoading: Boolean,
    errorMessage: String?,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(Color.Black, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
    ) {
        // Video fullscreen
        if (channel != null && playerEngine != null && errorMessage == null) {
            PlayerRenderView(
                playerEngine = playerEngine,
                resizeMode = PlayerSurfaceResizeMode.FILL,
                surfaceType = PlayerRenderSurfaceType.SURFACE_VIEW,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            // Placeholder cuando no hay preview
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        androidx.compose.ui.graphics.Brush.verticalGradient(
                            listOf(Color(0xFF1A0000), Color(0xFF0D0D0D))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Canvas(modifier = Modifier.size(48.dp)) {
                        val w = size.width; val h = size.height
                        drawRoundRect(
                            color = Color(0xFF333333),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(8f)
                        )
                        val path = androidx.compose.ui.graphics.Path().apply {
                            moveTo(w * 0.38f, h * 0.28f)
                            lineTo(w * 0.72f, h * 0.50f)
                            lineTo(w * 0.38f, h * 0.72f)
                            close()
                        }
                        drawPath(path, color = Color(0xFFE8001C))
                    }
                    Text(
                        text = "Selecciona un canal",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // Loading spinner
        if (isLoading && channel != null) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = Color(0xFFE8001C),
                    strokeWidth = 3.dp,
                    modifier = Modifier.size(40.dp)
                )
            }
        }

        // Bottom gradient overlay con info del canal
        if (channel != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomStart)
                    .background(
                        androidx.compose.ui.graphics.Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
                        )
                    )
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = channel.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    channel.currentProgram?.let { program ->
                        Text(
                            text = program.title,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.80f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        LinearProgressIndicator(
                            progress = { program.progressPercent() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(3.dp)
                                .padding(top = 2.dp),
                            color = Color(0xFFE8001C),
                            trackColor = Color.White.copy(alpha = 0.25f)
                        )
                    }
                }
            }
        }

        // Borde rojo TNET sutil
        Box(
            modifier = Modifier
                .fillMaxSize()
                .border(1.dp, Color(0xFFE8001C).copy(alpha = 0.3f), RoundedCornerShape(16.dp))
        )
    }
}

@Composable
internal fun CategoryItem(
    category: Category,
    isSelected: Boolean,
    isLocked: Boolean = false,
    isPinned: Boolean = false,
    focusRequester: FocusRequester,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    onJumpToSearch: () -> Boolean,
    onJumpToContent: () -> Boolean,
    onFocused: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }

    TvClickableSurface(
        onClick = onClick,
        onLongClick = onLongClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .focusRequester(focusRequester)
            .onFocusChanged {
                isFocused = it.isFocused
                if (it.isFocused) onFocused()
            }
            .onPreviewKeyEvent { event ->
                if (event.nativeKeyEvent.action == android.view.KeyEvent.ACTION_DOWN) {
                    when (event.nativeKeyEvent.keyCode) {
                        android.view.KeyEvent.KEYCODE_DPAD_LEFT -> onJumpToSearch()
                        android.view.KeyEvent.KEYCODE_DPAD_RIGHT -> onJumpToContent()
                        else -> false
                    }
                } else false
            },
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1f),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1f),
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(10.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = if (isSelected) Color(0xFFE8001C) else Color.Transparent,
            focusedContainerColor = Color(0xFFE8001C).copy(alpha = 0.75f),
            contentColor = Color.White
        ),
        border = ClickableSurfaceDefaults.border(
            focusedBorder = Border(
                border = BorderStroke(2.dp, Color(0xFFFF4500)),
                shape = RoundedCornerShape(10.dp)
            )
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            if (isPinned) {
                PinnedCategoryGlyph(
                    tint = Color.White,
                    modifier = Modifier.padding(end = 8.dp)
                )
            }
            Text(
                text = category.name,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = Color.White,
                modifier = Modifier.weight(1f)
            )
            if (isLocked) {
                Text(
                    text = "🔒",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun PinnedCategoryGlyph(
    tint: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.size(12.dp)) {
        val headRadius = size.minDimension * 0.18f
        val centerX = size.width * 0.45f
        val headCenterY = size.height * 0.26f
        drawCircle(
            color = tint,
            radius = headRadius,
            center = androidx.compose.ui.geometry.Offset(centerX, headCenterY)
        )
        drawLine(
            color = tint,
            start = androidx.compose.ui.geometry.Offset(centerX, headCenterY + headRadius * 0.6f),
            end = androidx.compose.ui.geometry.Offset(centerX, size.height * 0.8f),
            strokeWidth = size.minDimension * 0.12f
        )
        drawLine(
            color = tint,
            start = androidx.compose.ui.geometry.Offset(size.width * 0.18f, size.height * 0.38f),
            end = androidx.compose.ui.geometry.Offset(size.width * 0.72f, size.height * 0.38f),
            strokeWidth = size.minDimension * 0.14f
        )
    }
}

@Composable
internal fun ReorderSidePanel(
    channels: List<Channel>,
    onMoveUp: (Channel) -> Unit,
    onMoveDown: (Channel) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit
) {
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    val isTelevisionDevice = rememberIsTelevisionDevice()
    val panelWidth = if (screenWidth < 900.dp) {
        (screenWidth * 0.36f).coerceIn(188.dp, 220.dp)
    } else if (!isTelevisionDevice && screenWidth < 1280.dp) {
        (screenWidth * 0.28f).coerceIn(220.dp, 252.dp)
    } else {
        272.dp
    }
    var draggingChannel by remember { mutableStateOf<Channel?>(null) }
    val panelFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        panelFocusRequester.requestFocus()
    }

    Box(
        modifier = Modifier
            .fillMaxHeight()
            .width(panelWidth)
            .background(SurfaceElevated)
            .padding(16.dp)
            .focusRequester(panelFocusRequester)
            .focusGroup()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Text(
                stringResource(R.string.home_reorder_channels),
                style = MaterialTheme.typography.titleMedium,
                color = Primary,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                TvButton(
                    onClick = onSave,
                    colors = ButtonDefaults.colors(
                        containerColor = Primary,
                        contentColor = Color.White
                    ),
                    modifier = Modifier.weight(1f)
                ) { Text(stringResource(R.string.action_save), maxLines = 1) }

                TvButton(
                    onClick = onCancel,
                    colors = ButtonDefaults.colors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = OnSurface
                    ),
                    modifier = Modifier.weight(1f)
                ) { Text(stringResource(R.string.action_cancel), maxLines = 1) }
            }

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(channels, key = { _, channel -> channel.id }) { index, channel ->
                    var isFocused by remember { mutableStateOf(false) }
                    val isDraggingThis = draggingChannel == channel

                    TvClickableSurface(
                        onClick = {
                            draggingChannel = if (isDraggingThis) null else channel
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .onFocusChanged { isFocused = it.isFocused }
                            .onKeyEvent { event ->
                                if (event.nativeKeyEvent.action == android.view.KeyEvent.ACTION_DOWN) {
                                    if (isDraggingThis) {
                                        when (event.nativeKeyEvent.keyCode) {
                                            android.view.KeyEvent.KEYCODE_DPAD_UP -> {
                                                onMoveUp(channel)
                                                true
                                            }
                                            android.view.KeyEvent.KEYCODE_DPAD_DOWN -> {
                                                onMoveDown(channel)
                                                true
                                            }
                                            else -> false
                                        }
                                    } else if (
                                        event.nativeKeyEvent.keyCode == android.view.KeyEvent.KEYCODE_DPAD_LEFT ||
                                        event.nativeKeyEvent.keyCode == android.view.KeyEvent.KEYCODE_DPAD_RIGHT
                                    ) {
                                        true
                                    } else {
                                        false
                                    }
                                } else false
                            },
                        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(8.dp)),
                        colors = ClickableSurfaceDefaults.colors(
                            focusedContainerColor = if (isDraggingThis) Primary else Primary.copy(alpha = 0.2f),
                            containerColor = if (isDraggingThis) Primary.copy(alpha = 0.5f) else Color.Transparent
                        ),
                        border = ClickableSurfaceDefaults.border(
                            focusedBorder = Border(
                                border = BorderStroke(2.dp, FocusBorder),
                                shape = RoundedCornerShape(8.dp)
                            )
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(12.dp)
                                .fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (isDraggingThis) {
                                Text(
                                    stringResource(R.string.action_move),
                                    color = Color.White,
                                    modifier = Modifier.padding(end = 8.dp)
                                )
                            }
                            Text(
                                stringResource(R.string.channel_number_name_format, index + 1, channel.name),
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (isDraggingThis) Color.White else if (isFocused) OnBackground else OnSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            Text(
                if (draggingChannel != null) "UP/DOWN to move.\nOK to drop." else "OK to grab channel.",
                style = MaterialTheme.typography.bodySmall,
                color = OnSurfaceDim,
                modifier = Modifier
                    .padding(top = 16.dp)
                    .fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        }
    }
}
