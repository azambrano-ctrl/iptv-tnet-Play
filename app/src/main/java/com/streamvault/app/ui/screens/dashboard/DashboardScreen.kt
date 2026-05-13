package com.streamvault.app.ui.screens.dashboard

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.tv.material3.Border
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.SurfaceDefaults
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import com.streamvault.app.R
import com.streamvault.app.device.rememberIsTelevisionDevice
import com.streamvault.app.ui.components.ChannelLogoBadge
import com.streamvault.app.navigation.Routes
import com.streamvault.app.ui.components.CategoryRow
import com.streamvault.app.ui.components.ChannelCard
import com.streamvault.app.ui.components.ContinueWatchingRow
import com.streamvault.app.ui.components.MovieCard
import com.streamvault.app.ui.components.rememberCrossfadeImageModel
import com.streamvault.app.ui.components.SeriesCard
import com.streamvault.app.ui.components.shell.AppNavigationChrome
import com.streamvault.app.ui.components.shell.AppHeroHeader
import com.streamvault.app.ui.components.shell.AppScreenScaffold
import com.streamvault.app.ui.components.shell.StatusPill
import com.streamvault.app.ui.design.AppColors
import com.streamvault.app.ui.time.LocalAppTimeFormat
import com.streamvault.app.ui.time.createDateTimeFormat
import com.streamvault.app.ui.design.AppColors.Brand as Primary
import com.streamvault.app.ui.design.AppColors.FocusBorder as FocusBorder
import com.streamvault.app.ui.design.AppColors.SurfaceElevated as SurfaceElevated
import com.streamvault.app.ui.design.AppColors.SurfaceEmphasis as SurfaceHighlight
import com.streamvault.app.ui.design.AppColors.TextPrimary as OnBackground
import com.streamvault.app.ui.design.AppColors.TextPrimary as TextPrimary
import com.streamvault.app.ui.design.AppColors.TextTertiary as OnSurfaceDim
import com.streamvault.app.ui.design.AppColors.TextTertiary as TextTertiary
import com.streamvault.domain.model.Channel
import com.streamvault.domain.model.Movie
import com.streamvault.domain.model.PlaybackHistory
import com.streamvault.domain.model.Series
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.ui.res.painterResource
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.streamvault.app.ui.interaction.TvClickableSurface
import com.streamvault.app.ui.interaction.TvButton
import com.streamvault.app.ui.interaction.TvIconButton

@Composable
fun DashboardScreen(
    onNavigate: (String) -> Unit,
    onAddProvider: () -> Unit,
    onRecentChannelClick: (Channel, Long?) -> Unit,
    onFavoriteChannelClick: (Channel, Long?) -> Unit,
    onMovieClick: (Movie) -> Unit,
    onSeriesClick: (Series) -> Unit,
    onPlaybackHistoryClick: (PlaybackHistory) -> Unit,
    currentRoute: String,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val recordingChannelIds by viewModel.recordingChannelIds.collectAsStateWithLifecycle()
    val scheduledChannelIds by viewModel.scheduledChannelIds.collectAsStateWithLifecycle()
    val provider = uiState.provider
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.userMessage) {
        uiState.userMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.userMessageShown()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (provider == null) {
            AppScreenScaffold(
                currentRoute = currentRoute,
                onNavigate = onNavigate,
                title = stringResource(R.string.nav_home),
                subtitle = null,
                navigationChrome = AppNavigationChrome.TopBar,
                compactHeader = true,
                showScreenHeader = false
            ) {
                EmptyDashboard(
                    onAddProvider = onAddProvider,
                    onOpenSettings = { onNavigate(Routes.SETTINGS) }
                )
            }
        } else {
            NowPlusDashboard(
                uiState = uiState,
                onNavigate = onNavigate,
                onRefresh = viewModel::refreshProvider,
                isSyncing = uiState.isSyncing,
                onChannelClick = { channel -> onRecentChannelClick(channel, uiState.provider?.id) }
            )
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp)
        )
    }
}

@Composable
private fun DashboardHero(
    providerName: String,
    feature: DashboardFeature,
    stats: DashboardStats,
    onOpenLiveTv: () -> Unit,
    onOpenGuide: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenSavedLibrary: () -> Unit,
    onFeatureAction: () -> Unit
) {
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    val isTelevisionDevice = rememberIsTelevisionDevice()
    val heroHeight = when {
        screenWidth < 700.dp -> 176.dp
        !isTelevisionDevice && screenWidth < 1280.dp -> 196.dp
        else -> 220.dp
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp)
    ) {
        if (!feature.artworkUrl.isNullOrBlank()) {
            AsyncImage(
                model = rememberCrossfadeImageModel(feature.artworkUrl),
                contentDescription = feature.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(heroHeight)
                    .clip(RoundedCornerShape(28.dp))
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(heroHeight)
                    .clip(RoundedCornerShape(28.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.88f),
                                Color.Black.copy(alpha = 0.72f),
                                Color.Black.copy(alpha = 0.34f)
                            )
                        )
                    )
            )
        }

        AppHeroHeader(
            eyebrow = providerName,
            title = feature.title.ifBlank { stringResource(R.string.dashboard_title) },
            subtitle = feature.summary.ifBlank { stringResource(R.string.dashboard_subtitle, providerName) },
            modifier = Modifier
                .fillMaxWidth()
                .height(heroHeight),
            footer = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StatusPill(label = stringResource(R.string.nav_live_tv), containerColor = AppColors.BrandMuted)
                        StatusPill(label = stringResource(R.string.nav_epg), containerColor = AppColors.SurfaceEmphasis)
                        StatusPill(label = stringResource(R.string.favorites_title), containerColor = AppColors.Warning, contentColor = Color.Black)
                    }
                    DashboardStatRow(stats = stats)
                }
            },
            actions = {
                DashboardActionButton(label = stringResource(R.string.nav_live_tv), onClick = onOpenLiveTv)
                DashboardActionButton(label = stringResource(R.string.nav_epg), onClick = onOpenGuide)
                DashboardActionButton(label = stringResource(R.string.dashboard_search_library), onClick = onOpenSearch)
                DashboardActionButton(label = stringResource(R.string.favorites_title), onClick = onOpenSavedLibrary)
                if (feature.actionLabel.isNotBlank()) {
                    DashboardActionButton(
                        label = feature.actionLabel,
                        onClick = onFeatureAction
                    )
                }
            }
        )
    }
}

@Composable
private fun DashboardStatRow(
    stats: DashboardStats
) {
    val statItems = listOf(
        stringResource(R.string.dashboard_stat_live, stats.liveChannelCount),
        stringResource(R.string.dashboard_stat_favorites, stats.favoriteChannelCount),
        stringResource(R.string.dashboard_stat_recent, stats.recentChannelCount),
        stringResource(R.string.dashboard_stat_resume, stats.continueWatchingCount),
        stringResource(R.string.dashboard_stat_movies, stats.movieLibraryCount),
        stringResource(R.string.dashboard_stat_series, stats.seriesLibraryCount)
    )

    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        items(statItems, key = { it }) { statLabel ->
            Surface(
                shape = RoundedCornerShape(999.dp),
                colors = SurfaceDefaults.colors(
                    containerColor = AppColors.Surface.copy(alpha = 0.64f)
                )
            ) {
                Text(
                    text = statLabel,
                    style = MaterialTheme.typography.labelLarge,
                    color = TextPrimary,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                )
            }
        }
    }
}

@Composable
private fun DashboardShortcutRow(
    title: String,
    subtitle: String,
    shortcuts: List<DashboardLiveShortcut>,
    onShortcutClick: (DashboardLiveShortcut) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = TextPrimary
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = OnSurfaceDim
            )
        }

        LazyRow(
            contentPadding = PaddingValues(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(shortcuts, key = { "${it.type}:${it.categoryId}:${it.label}" }) { shortcut ->
                DashboardShortcutCard(
                    shortcut = shortcut,
                    onClick = { onShortcutClick(shortcut) }
                )
            }
        }
    }
}

@Composable
private fun DashboardShortcutCard(
    shortcut: DashboardLiveShortcut,
    onClick: () -> Unit
) {
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    val isTelevisionDevice = rememberIsTelevisionDevice()
    val cardWidth = when {
        screenWidth < 700.dp -> 148.dp
        !isTelevisionDevice && screenWidth < 1280.dp -> 160.dp
        else -> 170.dp
    }
    val accentColor = when (shortcut.type) {
        DashboardShortcutType.FAVORITES -> Color(0xFFFFC857)
        DashboardShortcutType.RECENT -> Color(0xFF4FD1C5)
        DashboardShortcutType.LAST_GROUP -> Color(0xFF60A5FA)
        DashboardShortcutType.CUSTOM_GROUP -> Primary
    }

    TvClickableSurface(
        onClick = onClick,
        modifier = Modifier
            .width(cardWidth)
            .height(76.dp),
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(16.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = SurfaceElevated,
            focusedContainerColor = SurfaceHighlight
        ),
        border = ClickableSurfaceDefaults.border(
            border = Border(
                border = BorderStroke(1.dp, accentColor.copy(alpha = 0.28f)),
                shape = RoundedCornerShape(16.dp)
            ),
            focusedBorder = Border(
                border = BorderStroke(2.dp, FocusBorder),
                shape = RoundedCornerShape(16.dp)
            )
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(accentColor)
                )
                Text(
                    text = shortcut.label,
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = shortcut.detail,
                style = MaterialTheme.typography.bodySmall,
                color = OnSurfaceDim,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun DashboardActionButton(
    label: String,
    onClick: () -> Unit
) {
    TvButton(
        onClick = onClick,
        colors = ButtonDefaults.colors(
            containerColor = Primary.copy(alpha = 0.18f),
            focusedContainerColor = Primary.copy(alpha = 0.32f),
            contentColor = TextPrimary
        )
    ) {
        Text(text = label)
    }
}

@Composable
private fun DashboardProviderHealthCard(
    providerName: String,
    health: DashboardProviderHealth,
    onOpenDiagnostics: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val appTimeFormat = LocalAppTimeFormat.current
    val dateTimeFormat = remember(appTimeFormat) { appTimeFormat.createDateTimeFormat() }
    val syncLabel = remember(health.lastSyncedAt, dateTimeFormat) {
        if (health.lastSyncedAt <= 0L) {
            context.getString(R.string.dashboard_provider_no_sync)
        } else {
            context.getString(R.string.dashboard_provider_synced_at, dateTimeFormat.format(Date(health.lastSyncedAt)))
        }
    }
    val expiryLabel = remember(health.expirationDate) {
        health.expirationDate?.let {
            val format = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
            context.getString(R.string.dashboard_provider_expires_at, format.format(Date(it)))
        } ?: context.getString(R.string.dashboard_provider_no_expiry)
    }
    val statusLabel = when (health.status) {
        com.streamvault.domain.model.ProviderStatus.ACTIVE -> stringResource(R.string.settings_status_active)
        com.streamvault.domain.model.ProviderStatus.PARTIAL -> stringResource(R.string.settings_status_partial)
        com.streamvault.domain.model.ProviderStatus.ERROR -> stringResource(R.string.settings_status_error)
        com.streamvault.domain.model.ProviderStatus.EXPIRED -> stringResource(R.string.settings_status_expired)
        com.streamvault.domain.model.ProviderStatus.DISABLED -> stringResource(R.string.settings_status_disabled)
        com.streamvault.domain.model.ProviderStatus.UNKNOWN -> stringResource(R.string.settings_status_unknown)
    }
    val sourceLabel = when (health.type) {
        com.streamvault.domain.model.ProviderType.XTREAM_CODES -> stringResource(R.string.dashboard_provider_xtream)
        com.streamvault.domain.model.ProviderType.M3U -> stringResource(R.string.dashboard_provider_m3u)
        com.streamvault.domain.model.ProviderType.STALKER_PORTAL -> "Stalker/MAG Portal"
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 48.dp, vertical = 4.dp),
        shape = RoundedCornerShape(22.dp),
        colors = SurfaceDefaults.colors(containerColor = SurfaceHighlight)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp, vertical = 18.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = stringResource(R.string.dashboard_provider_health_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary
                )
                Text(
                    text = providerName,
                    style = MaterialTheme.typography.bodyLarge,
                    color = OnSurfaceDim
                )
                Text(
                    text = "$syncLabel | $expiryLabel",
                    style = MaterialTheme.typography.bodySmall,
                    color = OnSurfaceDim
                )
            }

            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                item {
                    DashboardHealthPill(
                        label = statusLabel,
                        value = stringResource(R.string.dashboard_provider_status)
                    )
                }
                item {
                    DashboardHealthPill(
                        label = sourceLabel,
                        value = stringResource(R.string.dashboard_provider_source)
                    )
                }
                item {
                    DashboardHealthPill(
                        label = health.maxConnections.toString(),
                        value = stringResource(R.string.dashboard_provider_connections)
                    )
                }
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp, vertical = 0.dp),
            horizontalArrangement = Arrangement.End
        ) {
            DashboardActionButton(
                label = stringResource(R.string.dashboard_warning_review),
                onClick = onOpenDiagnostics
            )
        }
    }
}

@Composable
private fun DashboardHealthPill(
    label: String,
    value: String
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        colors = SurfaceDefaults.colors(containerColor = Color.White.copy(alpha = 0.08f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.labelSmall,
                color = OnSurfaceDim
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = TextPrimary
            )
        }
    }
}

@Composable
private fun DashboardProviderWarningCard(
    warnings: List<String>,
    onOpenSettings: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 48.dp, vertical = 6.dp),
        shape = RoundedCornerShape(20.dp),
        colors = SurfaceDefaults.colors(containerColor = SurfaceElevated)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 22.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.dashboard_warning_title),
                style = MaterialTheme.typography.titleMedium,
                color = Primary
            )
            Text(
                text = warnings.take(3).joinToString(" | "),
                style = MaterialTheme.typography.bodyMedium,
                color = OnSurfaceDim
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                DashboardActionButton(
                    label = stringResource(R.string.dashboard_warning_review),
                    onClick = onOpenSettings
                )
            }
        }
    }
}

@Composable
private fun DashboardUpdateCard(
    notice: DashboardUpdateNotice,
    onOpenSettings: () -> Unit,
    onInstallUpdate: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 48.dp, vertical = 6.dp),
        shape = RoundedCornerShape(20.dp),
        colors = SurfaceDefaults.colors(containerColor = Primary.copy(alpha = 0.16f)),
        border = Border(BorderStroke(1.dp, Primary.copy(alpha = 0.45f)))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 22.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.dashboard_update_title, notice.latestVersionName),
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary
            )
            Text(
                text = stringResource(
                    if (notice.installReady) {
                        R.string.dashboard_update_install_ready
                    } else {
                        R.string.dashboard_update_available
                    }
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = OnSurfaceDim
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                DashboardActionButton(
                    label = stringResource(
                        if (notice.installReady) {
                            R.string.dashboard_update_open_installer
                        } else {
                            R.string.dashboard_update_open_settings
                        }
                    ),
                    onClick = {
                        if (notice.installReady) {
                            onInstallUpdate()
                        } else {
                            onOpenSettings()
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun EmptyDashboard(
    onAddProvider: () -> Unit,
    onOpenSettings: () -> Unit
) {
    BoxWithConstraints(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        val isTelevisionDevice = rememberIsTelevisionDevice()
        val contentModifier = if (maxWidth < 900.dp) {
            Modifier.fillMaxWidth(0.9f)
        } else if (!isTelevisionDevice && maxWidth < 1280.dp) {
            Modifier.fillMaxWidth(0.76f)
        } else {
            Modifier.width(720.dp)
        }

        Surface(
            shape = RoundedCornerShape(28.dp),
            colors = SurfaceDefaults.colors(
                containerColor = SurfaceHighlight
            )
        ) {
            Column(
                modifier = contentModifier
                    .padding(horizontal = 32.dp, vertical = 28.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = stringResource(R.string.dashboard_empty_title),
                    style = MaterialTheme.typography.headlineMedium,
                    color = OnBackground
                )
                Text(
                    text = stringResource(R.string.dashboard_empty_body),
                    style = MaterialTheme.typography.titleMedium,
                    color = OnSurfaceDim
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    TvButton(onClick = onAddProvider) {
                        Text(stringResource(R.string.settings_add_provider))
                    }
                    TvButton(
                        onClick = onOpenSettings,
                        colors = ButtonDefaults.colors(
                            containerColor = SurfaceElevated,
                            focusedContainerColor = Primary.copy(alpha = 0.24f),
                            contentColor = TextPrimary
                        )
                    ) {
                        Text(stringResource(R.string.nav_settings))
                    }
                }
            }
        }
    }
}

@Composable
private fun rememberDashboardSections(
    uiState: DashboardUiState
): List<DashboardHomeSection> {
    return remember(
        uiState.feature.actionType,
        uiState.liveShortcuts,
        uiState.favoriteChannels,
        uiState.recentChannels,
        uiState.continueWatching,
        uiState.recentMovies,
        uiState.recentSeries
    ) {
        val preferred = listOf(
            DashboardHomeSection.FAVORITE_CHANNELS,
            DashboardHomeSection.RECENT_CHANNELS,
            DashboardHomeSection.LIVE_SHORTCUTS,
            DashboardHomeSection.CONTINUE_WATCHING,
            DashboardHomeSection.RECENT_MOVIES,
            DashboardHomeSection.RECENT_SERIES
        )

        preferred.filter { section ->
            when (section) {
                DashboardHomeSection.LIVE_SHORTCUTS -> uiState.liveShortcuts.isNotEmpty()
                DashboardHomeSection.FAVORITE_CHANNELS -> uiState.favoriteChannels.isNotEmpty()
                DashboardHomeSection.RECENT_CHANNELS -> uiState.recentChannels.isNotEmpty()
                DashboardHomeSection.CONTINUE_WATCHING -> uiState.continueWatching.isNotEmpty()
                DashboardHomeSection.RECENT_MOVIES -> uiState.recentMovies.isNotEmpty()
                DashboardHomeSection.RECENT_SERIES -> uiState.recentSeries.isNotEmpty()
            }
        }
    }
}

private enum class DashboardHomeSection {
    LIVE_SHORTCUTS,
    FAVORITE_CHANNELS,
    RECENT_CHANNELS,
    CONTINUE_WATCHING,
    RECENT_MOVIES,
    RECENT_SERIES
}

@Composable
private fun FavoriteChannelsRow(
    title: String,
    channels: List<Channel>,
    onSeeAll: () -> Unit,
    onChannelClick: (Channel) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary
            )
            TvClickableSurface(
                onClick = onSeeAll,
                shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(999.dp)),
                colors = ClickableSurfaceDefaults.colors(
                    containerColor = Primary.copy(alpha = 0.12f),
                    focusedContainerColor = Primary.copy(alpha = 0.22f),
                    contentColor = TextTertiary
                )
            ) {
                Text(
                    text = stringResource(R.string.category_see_all),
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }

        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(channels, key = { it.id }) { channel ->
                FavoriteChannelLogoCard(
                    channel = channel,
                    onClick = { onChannelClick(channel) }
                )
            }
        }
    }
}

@Composable
private fun FavoriteChannelLogoCard(
    channel: Channel,
    onClick: () -> Unit
) {
    TvClickableSurface(
        onClick = onClick,
        modifier = Modifier.width(86.dp),
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(18.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = SurfaceElevated,
            focusedContainerColor = SurfaceHighlight
        ),
        border = ClickableSurfaceDefaults.border(
            focusedBorder = Border(
                border = BorderStroke(2.dp, FocusBorder),
                shape = RoundedCornerShape(18.dp)
            )
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(RoundedCornerShape(999.dp))
            ) {
                ChannelLogoBadge(
                    channelName = channel.name,
                    logoUrl = channel.logoUrl,
                    shape = RoundedCornerShape(999.dp),
                    backgroundColor = AppColors.SurfaceEmphasis,
                    contentPadding = PaddingValues(8.dp),
                    textStyle = MaterialTheme.typography.labelLarge,
                    textColor = TextPrimary,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Text(
                text = channel.name,
                style = MaterialTheme.typography.bodySmall,
                color = TextPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun TnetWelcomeHero(
    liveChannelCount: Int,
    onOpenLiveTV: () -> Unit,
    onOpenMovies: () -> Unit
) {
    Box(
        modifier = androidx.compose.ui.Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 48.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Box(
                modifier = androidx.compose.ui.Modifier
                    .size(100.dp)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(Color(0x44FF4500), Color.Transparent)
                        ),
                        shape = androidx.compose.foundation.shape.CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "▶",
                    style = MaterialTheme.typography.displayMedium,
                    color = Color(0xFFFF4500)
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Bienvenido a TNET play",
                    style = MaterialTheme.typography.headlineSmall,
                    color = TextPrimary
                )
                if (liveChannelCount > 0) {
                    Text(
                        text = "$liveChannelCount canales en vivo disponibles",
                        style = MaterialTheme.typography.bodyLarge,
                        color = OnSurfaceDim
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TvButton(
                    onClick = onOpenLiveTV,
                    colors = androidx.tv.material3.ButtonDefaults.colors(
                        containerColor = Color(0xFFFF4500),
                        focusedContainerColor = Color(0xFFFF6200),
                        contentColor = Color.White
                    )
                ) { Text("▶  Ver TV en vivo") }
                TvButton(
                    onClick = onOpenMovies,
                    colors = androidx.tv.material3.ButtonDefaults.colors(
                        containerColor = AppColors.SurfaceElevated,
                        focusedContainerColor = AppColors.SurfaceEmphasis,
                        contentColor = TextPrimary
                    )
                ) { Text("★  Películas") }
            }
        }
    }
}

// ── NOW+ Style Dashboard ────────────────────────────────────────────

@Composable
private fun NowPlusDashboard(
    uiState: DashboardUiState,
    onNavigate: (String) -> Unit,
    onRefresh: () -> Unit = {},
    isSyncing: Boolean = false,
    onChannelClick: (Channel) -> Unit = {}
) {
    val expiryText = remember(uiState.providerHealth.expirationDate) {
        if (uiState.providerHealth.expirationDate == null) "Sin expiración"
        else {
            val fmt = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            "Expira: ${fmt.format(Date(uiState.providerHealth.expirationDate))}"
        }
    }
    val channels = remember(uiState.favoriteChannels, uiState.recentChannels) {
        (uiState.favoriteChannels + uiState.recentChannels).distinctBy { it.id }.take(20)
    }
    val heroMovie = uiState.recentMovies.firstOrNull()

    Column(modifier = Modifier.fillMaxSize()) {

        // ── Barra superior ──────────────────────────────────────────────────
        NowTopBar(
            onSearch   = { onNavigate(Routes.SEARCH) },
            onSettings = { onNavigate(Routes.SETTINGS) }
        )

        Box(modifier = Modifier.weight(1f)) {
        NowPlusBackground()
        Row(modifier = Modifier.fillMaxSize()) {

            // ── Sidebar Xuper-style ─────────────────────────────────────────
            Column(
                modifier = Modifier
                    .width(168.dp)
                    .fillMaxHeight()
                    .background(Color(0xF2080808))
                    .padding(vertical = 12.dp, horizontal = 10.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                DashboardSidebarTextItem(label = "TV EN VIVO",  accentColor = Color(0xFFE8001C), iconType = 0, onClick = { onNavigate(Routes.LIVE_TV)    })
                DashboardSidebarTextItem(label = "PELÍCULAS",   accentColor = Color(0xFFFFAA00), iconType = 1, onClick = { onNavigate(Routes.MOVIES)     })
                DashboardSidebarTextItem(label = "SERIES",      accentColor = Color(0xFF8B5CF6), iconType = 2, onClick = { onNavigate(Routes.SERIES)     })
                DashboardSidebarTextItem(label = "EPG",         accentColor = Color(0xFF3B82F6), iconType = 3, onClick = { onNavigate(Routes.EPG)        })
                DashboardSidebarTextItem(label = "MULTI VISTA", accentColor = Color(0xFF06B6D4), iconType = 4, onClick = { onNavigate(Routes.MULTI_VIEW) })
                DashboardSidebarTextItem(label = "CATCH UP",    accentColor = Color(0xFFFF6200), iconType = 5, onClick = { onNavigate(Routes.EPG)        })
                DashboardSidebarTextItem(label = "AJUSTES",     accentColor = Color(0xFF64748B), iconType = 6, onClick = { onNavigate(Routes.SETTINGS)   })

                Spacer(modifier = Modifier.weight(1f))

                // Info inferior
                Column(
                    modifier = Modifier.padding(horizontal = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (uiState.stats.liveChannelCount > 0) {
                        Text(
                            text = "${uiState.stats.liveChannelCount} canales · ${uiState.stats.movieLibraryCount} pel.",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.35f),
                            maxLines = 1
                        )
                    }
                    Text(
                        text = expiryText,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.45f),
                        maxLines = 1
                    )
                    Text(
                        text = uiState.provider?.name ?: "TNET play",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFE8001C),
                        maxLines = 1
                    )
                }
            }

            // ── Contenido principal ─────────────────────────────────────────
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                contentPadding = PaddingValues(start = 14.dp, end = 18.dp, top = 8.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Barra de estado
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(modifier = Modifier.size(7.dp).background(
                                if (isSyncing) Color(0xFFFFAA00) else Color(0xFF22C55E),
                                RoundedCornerShape(50)
                            ))
                            Text(
                                text = if (isSyncing) "Sincronizando..." else "En línea",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.55f)
                            )
                        }
                        TvIconButton(onClick = onRefresh) { NowRefreshIcon(spinning = isSyncing) }
                    }
                }

                // ── Hero Banner: película destacada ─────────────────────────
                if (heroMovie != null) {
                    item {
                        NowHeroBanner(
                            movie = heroMovie,
                            onPlay = { onNavigate(Routes.MOVIES) },
                            onMore = { onNavigate(Routes.MOVIES) }
                        )
                    }
                }

                // ── Canales ─────────────────────────────────────────────────
                if (channels.isNotEmpty()) {
                    item {
                        DashboardSectionHeader(
                            title = "TV EN VIVO  •  ${uiState.stats.liveChannelCount} canales",
                            onSeeAll = { onNavigate(Routes.LIVE_TV) }
                        )
                    }
                    item {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            contentPadding = PaddingValues(horizontal = 2.dp)
                        ) {
                            items(channels) { channel ->
                                ChannelCard(channel = channel, onClick = { onChannelClick(channel) })
                            }
                        }
                    }
                }

                // ── Películas ────────────────────────────────────────────────
                if (uiState.recentMovies.size > 1) {
                    item { DashboardSectionHeader(title = "PELÍCULAS RECIENTES", onSeeAll = { onNavigate(Routes.MOVIES) }) }
                    item {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            contentPadding = PaddingValues(horizontal = 2.dp)
                        ) {
                            items(uiState.recentMovies.drop(1).take(20)) { movie ->
                                MovieCard(movie = movie, onClick = { onNavigate(Routes.MOVIES) }, width = 136.dp, height = 200.dp)
                            }
                        }
                    }
                }

                // ── Series ──────────────────────────────────────────────────
                if (uiState.recentSeries.isNotEmpty()) {
                    item { DashboardSectionHeader(title = "SERIES", onSeeAll = { onNavigate(Routes.SERIES) }) }
                    item {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            contentPadding = PaddingValues(horizontal = 2.dp)
                        ) {
                            items(uiState.recentSeries.take(20)) { series ->
                                SeriesCard(series = series, onClick = { onNavigate(Routes.SERIES) })
                            }
                        }
                    }
                }

                // ── Estado vacío ────────────────────────────────────────────
                if (channels.isEmpty() && uiState.recentMovies.isEmpty() && uiState.recentSeries.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(top = 60.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                CircularProgressIndicator(color = Color(0xFFE8001C), modifier = Modifier.size(32.dp))
                                Text("Cargando contenido...", color = TextTertiary, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }

            // ── Panel de canales derecha ────────────────────────────────────
            if (channels.isNotEmpty()) {
                NowChannelPanel(
                    channels = channels,
                    onChannelClick = onChannelClick,
                    onSeeAll = { onNavigate(Routes.LIVE_TV) }
                )
            }
        }
        } // Box weight(1f)
    } // Column fillMaxSize
}

// ── Panel vertical de canales ─────────────────────────────────────────────────

@Composable
private fun NowChannelPanel(
    channels: List<Channel>,
    onChannelClick: (Channel) -> Unit,
    onSeeAll: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(196.dp)
            .fillMaxHeight()
            .background(Color(0xEE050505))
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(modifier = Modifier.size(7.dp).background(Color(0xFF22C55E), RoundedCornerShape(50)))
                Text(
                    text = "EN VIVO",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
            TvClickableSurface(
                onClick = onSeeAll,
                shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(6.dp)),
                colors = ClickableSurfaceDefaults.colors(
                    containerColor = Color.White.copy(.08f),
                    focusedContainerColor = Color(0xFFE8001C).copy(.70f)
                )
            ) {
                Text(
                    text = "VER TODO",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(.70f),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }

        // Divisor
        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(.08f)))

        // Lista de canales
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = 4.dp)
        ) {
            items(channels, key = { it.id }) { channel ->
                NowChannelRow(
                    channel = channel,
                    onClick = { onChannelClick(channel) }
                )
            }
        }
    }
}

@Composable
private fun NowChannelRow(
    channel: Channel,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .onFocusEvent { isFocused = it.hasFocus },
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(0.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = if (isFocused) Color(0xFFE8001C).copy(.20f) else Color.Transparent,
            focusedContainerColor = Color(0xFFE8001C).copy(.25f)
        ),
        border = ClickableSurfaceDefaults.border(
            border = Border(BorderStroke(0.dp, Color.Transparent)),
            focusedBorder = Border(BorderStroke(0.dp, Color.Transparent))
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Logo del canal
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.White.copy(.10f)),
                contentAlignment = Alignment.Center
            ) {
                ChannelLogoBadge(
                    channelName = channel.name,
                    logoUrl = channel.logoUrl,
                    shape = RoundedCornerShape(8.dp),
                    backgroundColor = Color.Transparent,
                    contentPadding = PaddingValues(4.dp),
                    textStyle = MaterialTheme.typography.labelSmall,
                    textColor = Color.White,
                    modifier = Modifier.fillMaxSize()
                )
            }
            // Nombre
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = channel.name,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = if (isFocused) FontWeight.Bold else FontWeight.Normal,
                    color = if (isFocused) Color.White else Color.White.copy(.78f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            // Punto de live
            if (isFocused) {
                Box(modifier = Modifier.size(6.dp).background(Color(0xFFE8001C), RoundedCornerShape(50)))
            }
        }
    }
}

// ── Top Bar ──────────────────────────────────────────────────────────────────

@Composable
private fun NowTopBar(
    onSearch: () -> Unit,
    onSettings: () -> Unit
) {
    // Reloj actualizado cada minuto
    var timeText by remember { mutableStateOf("") }
    LaunchedEffect(Unit) {
        while (true) {
            timeText = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())
            kotlinx.coroutines.delay(30_000L)
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp)
            .background(Color(0xF5060606))
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Logo TNET play
        Image(
            painter = painterResource(id = R.drawable.tnet_logo),
            contentDescription = "TNET play",
            modifier = Modifier.height(46.dp).wrapContentWidth(),
            contentScale = ContentScale.Fit
        )

        Spacer(modifier = Modifier.weight(1f))

        // Iconos de acción
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Buscar
            NowTopBarIconButton(onClick = onSearch) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Buscar",
                    tint = Color.White.copy(0.80f),
                    modifier = Modifier.size(20.dp)
                )
            }
            // Perfil
            NowTopBarIconButton(onClick = {}) {
                Canvas(modifier = Modifier.size(20.dp)) {
                    val cx = size.width/2f; val cy = size.height/2f
                    drawCircle(Color.White.copy(.80f), size.width*.32f, androidx.compose.ui.geometry.Offset(cx, cy*.72f), style = androidx.compose.ui.graphics.drawscope.Stroke(1.8f))
                    drawArc(Color.White.copy(.80f), 180f, 180f, false,
                        androidx.compose.ui.geometry.Offset(cx - size.width*.42f, cy*.10f),
                        androidx.compose.ui.geometry.Size(size.width*.84f, size.height*.84f),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(1.8f))
                }
            }
            // Notificaciones
            NowTopBarIconButton(onClick = {}) {
                Canvas(modifier = Modifier.size(20.dp)) {
                    val w = size.width; val h = size.height
                    val path = androidx.compose.ui.graphics.Path()
                    path.moveTo(w*.5f, h*.05f)
                    path.cubicTo(w*.15f, h*.10f, w*.10f, h*.35f, w*.12f, h*.62f)
                    path.lineTo(w*.88f, h*.62f)
                    path.cubicTo(w*.90f, h*.35f, w*.85f, h*.10f, w*.5f, h*.05f)
                    drawPath(path, Color.White.copy(.80f), style = androidx.compose.ui.graphics.drawscope.Stroke(1.8f, cap = androidx.compose.ui.graphics.StrokeCap.Round))
                    drawLine(Color.White.copy(.80f), androidx.compose.ui.geometry.Offset(w*.12f, h*.62f), androidx.compose.ui.geometry.Offset(w*.88f, h*.62f), 1.8f)
                    drawArc(Color.White.copy(.80f), 0f, 180f, false,
                        androidx.compose.ui.geometry.Offset(w*.36f, h*.62f),
                        androidx.compose.ui.geometry.Size(w*.28f, h*.22f),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(1.8f))
                }
            }
            // Ajustes
            NowTopBarIconButton(onClick = onSettings) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Ajustes",
                    tint = Color.White.copy(0.80f),
                    modifier = Modifier.size(20.dp)
                )
            }

            // Separador
            Box(modifier = Modifier.width(1.dp).height(22.dp).background(Color.White.copy(0.18f)))

            // Wifi
            Canvas(modifier = Modifier.size(20.dp).padding(2.dp)) {
                val cx = size.width/2f; val cy = size.height*.72f
                for (i in 0..2) {
                    val r = size.width*(0.22f + i*0.22f)
                    drawArc(Color.White.copy(if(i==2) .80f else .40f), 200f, 140f, false,
                        androidx.compose.ui.geometry.Offset(cx-r, cy-r),
                        androidx.compose.ui.geometry.Size(r*2, r*2),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(1.8f, cap = androidx.compose.ui.graphics.StrokeCap.Round))
                }
                drawCircle(Color.White.copy(.90f), 2.5f, androidx.compose.ui.geometry.Offset(cx, cy))
            }

            // Hora
            Text(
                text = timeText,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White.copy(0.88f),
                modifier = Modifier.padding(start = 4.dp, end = 2.dp)
            )
        }
    }
}

@Composable
private fun NowTopBarIconButton(
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    Surface(
        onClick = onClick,
        modifier = Modifier.size(36.dp).onFocusEvent { isFocused = it.hasFocus },
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(8.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = if (isFocused) Color.White.copy(.14f) else Color.Transparent,
            focusedContainerColor = Color.White.copy(.18f)
        ),
        border = ClickableSurfaceDefaults.border(
            focusedBorder = Border(BorderStroke(1.5.dp, Color.White.copy(.35f)))
        )
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            content()
        }
    }
}

// ── Hero Banner ─────────────────────────────────────────────────────────────

@Composable
private fun NowHeroBanner(
    movie: Movie,
    onPlay: () -> Unit,
    onMore: () -> Unit
) {
    var isFocusedPlay by remember { mutableStateOf(false) }
    var isFocusedMore by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(210.dp)
            .clip(RoundedCornerShape(16.dp))
    ) {
        // Fondo: poster de la película como background
        if (!movie.backdropUrl.isNullOrBlank() || !movie.posterUrl.isNullOrBlank()) {
            AsyncImage(
                model = rememberCrossfadeImageModel(movie.backdropUrl ?: movie.posterUrl),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(modifier = Modifier.fillMaxSize().background(
                Brush.linearGradient(listOf(Color(0xFF1A0A00), Color(0xFF0D0D1A)))
            ))
        }

        // Gradiente oscuro de izquierda hacia derecha (más oscuro a la izquierda para el texto)
        Box(modifier = Modifier.fillMaxSize().background(
            Brush.horizontalGradient(
                colors = listOf(
                    Color.Black.copy(alpha = 0.92f),
                    Color.Black.copy(alpha = 0.70f),
                    Color.Black.copy(alpha = 0.20f)
                ),
                startX = 0f,
                endX = Float.POSITIVE_INFINITY
            )
        ))
        // Gradiente bottom
        Box(modifier = Modifier.fillMaxSize().background(
            Brush.verticalGradient(
                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.55f)),
                startY = 80f,
                endY = Float.POSITIVE_INFINITY
            )
        ))

        // Contenido del banner
        Row(modifier = Modifier.fillMaxSize()) {
            // Info del lado izquierdo (70%)
            Column(
                modifier = Modifier
                    .weight(0.62f)
                    .fillMaxHeight()
                    .padding(horizontal = 22.dp, vertical = 18.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Badge
                Box(
                    modifier = Modifier
                        .background(Color(0xFFE8001C), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text("DESTACADO", style = MaterialTheme.typography.labelSmall, color = Color.White, fontWeight = FontWeight.Bold)
                }

                // Título y detalles
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = movie.name,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (!movie.year.isNullOrBlank()) {
                            Text(movie.year!!, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(0.65f))
                        }
                        if (movie.rating != null && movie.rating!! > 0f) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Canvas(modifier = Modifier.size(10.dp)) {
                                    val c = size.width / 2f
                                    val path = Path()
                                    for (i in 0..4) {
                                        val outer = Math.toRadians((i * 72 - 90).toDouble())
                                        val inner = Math.toRadians((i * 72 - 90 + 36).toDouble())
                                        if (i == 0) path.moveTo(c + (c * Math.cos(outer)).toFloat(), c + (c * Math.sin(outer)).toFloat())
                                        else path.lineTo(c + (c * Math.cos(outer)).toFloat(), c + (c * Math.sin(outer)).toFloat())
                                        path.lineTo(c + (c * 0.4f * Math.cos(inner)).toFloat(), c + (c * 0.4f * Math.sin(inner)).toFloat())
                                    }
                                    path.close()
                                    drawPath(path, Color(0xFFFFAA00))
                                }
                                Text(
                                    text = "%.1f".format(movie.rating!! / 10f * 10f),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFFFFAA00)
                                )
                            }
                        }
                        if (!movie.genre.isNullOrBlank()) {
                            Text(movie.genre!!, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(0.50f), maxLines = 1)
                        }
                    }

                    // Botones
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        // Botón VER
                        Surface(
                            onClick = onPlay,
                            modifier = Modifier
                                .height(38.dp)
                                .onFocusEvent { isFocusedPlay = it.hasFocus },
                            shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(10.dp)),
                            colors = ClickableSurfaceDefaults.colors(
                                containerColor = Color(0xFFE8001C),
                                focusedContainerColor = Color(0xFFFF2233)
                            ),
                            border = ClickableSurfaceDefaults.border(
                                focusedBorder = Border(BorderStroke(2.dp, Color.White))
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Canvas(modifier = Modifier.size(10.dp)) {
                                    val p = Path()
                                    p.moveTo(0f, 0f); p.lineTo(size.width, size.height / 2f); p.lineTo(0f, size.height); p.close()
                                    drawPath(p, Color.White)
                                }
                                Text("▶  VER", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                        // Botón MÁS INFO
                        Surface(
                            onClick = onMore,
                            modifier = Modifier
                                .height(38.dp)
                                .onFocusEvent { isFocusedMore = it.hasFocus },
                            shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(10.dp)),
                            colors = ClickableSurfaceDefaults.colors(
                                containerColor = Color.White.copy(alpha = 0.15f),
                                focusedContainerColor = Color.White.copy(alpha = 0.28f)
                            ),
                            border = ClickableSurfaceDefaults.border(
                                focusedBorder = Border(BorderStroke(2.dp, Color.White))
                            )
                        ) {
                            Text(
                                "MÁS INFO",
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }

            // Poster del lado derecho (38%) — solo en pantallas anchas
            Box(
                modifier = Modifier
                    .weight(0.38f)
                    .fillMaxHeight()
                    .padding(end = 18.dp, top = 16.dp, bottom = 16.dp)
            ) {
                if (!movie.posterUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = rememberCrossfadeImageModel(movie.posterUrl),
                        contentDescription = movie.name,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxHeight()
                            .align(Alignment.CenterEnd)
                            .clip(RoundedCornerShape(10.dp))
                    )
                    // Gradiente izquierdo sobre el poster para que se fusione
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(60.dp)
                            .align(Alignment.CenterStart)
                            .background(Brush.horizontalGradient(listOf(Color.Black.copy(0.80f), Color.Transparent)))
                    )
                }
            }
        }
    }
}

// ── Sidebar item ─────────────────────────────────────────────────────────────

@Composable
private fun DashboardSidebarTextItem(
    label: String,
    accentColor: Color,
    iconType: Int,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(46.dp)
            .onFocusEvent { isFocused = it.hasFocus },
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(10.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = if (isFocused) accentColor else Color.Transparent,
            focusedContainerColor = accentColor
        ),
        border = ClickableSurfaceDefaults.border(
            border = Border(BorderStroke(0.dp, Color.Transparent)),
            focusedBorder = Border(BorderStroke(0.dp, Color.Transparent))
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SidebarMiniIcon(
                type = iconType,
                color = if (isFocused) Color.White else accentColor
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (isFocused) FontWeight.Bold else FontWeight.Normal,
                color = if (isFocused) Color.White else Color.White.copy(alpha = 0.70f),
                maxLines = 1
            )
        }
    }
}

@Composable
private fun SidebarMiniIcon(type: Int, color: Color) {
    val capR = androidx.compose.ui.graphics.StrokeCap.Round
    val sw = 1.8f
    Canvas(modifier = Modifier.size(18.dp)) {
        val w = size.width; val h = size.height
        when (type) {
            // TV
            0 -> {
                val body = androidx.compose.ui.geometry.Rect(w*.06f, h*.22f, w*.94f, h*.78f)
                drawRoundRect(color.copy(.20f), body.topLeft, body.size, androidx.compose.ui.geometry.CornerRadius(5f))
                drawRoundRect(color, body.topLeft, body.size, androidx.compose.ui.geometry.CornerRadius(5f), style = androidx.compose.ui.graphics.drawscope.Stroke(sw))
                drawLine(color, androidx.compose.ui.geometry.Offset(w*.38f, h*.22f), androidx.compose.ui.geometry.Offset(w*.28f, h*.08f), sw, capR)
                drawLine(color, androidx.compose.ui.geometry.Offset(w*.62f, h*.22f), androidx.compose.ui.geometry.Offset(w*.72f, h*.08f), sw, capR)
                drawLine(color.copy(.6f), androidx.compose.ui.geometry.Offset(w*.5f, h*.78f), androidx.compose.ui.geometry.Offset(w*.5f, h*.90f), sw, capR)
                drawLine(color.copy(.6f), androidx.compose.ui.geometry.Offset(w*.30f, h*.90f), androidx.compose.ui.geometry.Offset(w*.70f, h*.90f), sw, capR)
                // play
                val pr = h*.10f; val cx = w*.5f; val cy = h*.50f
                val t = Path().apply { moveTo(cx-pr*.4f, cy-pr*.65f); lineTo(cx+pr*.75f, cy); lineTo(cx-pr*.4f, cy+pr*.65f); close() }
                drawPath(t, color.copy(.90f))
            }
            // Película
            1 -> {
                val cx = w/2f; val cy = h*.54f; val r = h*.32f
                drawCircle(color.copy(.18f), r+4f, androidx.compose.ui.geometry.Offset(cx,cy))
                drawCircle(color, r, androidx.compose.ui.geometry.Offset(cx,cy), style = androidx.compose.ui.graphics.drawscope.Stroke(sw))
                val t = Path().apply { moveTo(cx-r*.35f, cy-r*.55f); lineTo(cx+r*.65f, cy); lineTo(cx-r*.35f, cy+r*.55f); close() }
                drawPath(t, color)
                // líneas clapper arriba
                drawLine(color.copy(.5f), androidx.compose.ui.geometry.Offset(w*.1f, h*.18f), androidx.compose.ui.geometry.Offset(w*.9f, h*.18f), sw)
                drawLine(color.copy(.3f), androidx.compose.ui.geometry.Offset(w*.30f, h*.08f), androidx.compose.ui.geometry.Offset(w*.22f, h*.18f), sw, capR)
                drawLine(color.copy(.3f), androidx.compose.ui.geometry.Offset(w*.55f, h*.08f), androidx.compose.ui.geometry.Offset(w*.47f, h*.18f), sw, capR)
            }
            // Series
            2 -> {
                for (i in 0..2) {
                    val top = h*(0.14f + i*0.27f); val btm = h*(0.34f + i*0.27f)
                    val left = w*(0.06f + i*0.06f); val right = w*(0.80f + i*0.04f)
                    drawRoundRect(color.copy(if(i==2) .80f else .30f), androidx.compose.ui.geometry.Offset(left,top),
                        androidx.compose.ui.geometry.Size(right-left, btm-top), androidx.compose.ui.geometry.CornerRadius(4f),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(sw))
                }
                val cx = w*.38f; val cy = h*.64f; val pr = h*.10f
                val t = Path().apply { moveTo(cx-pr*.4f, cy-pr*.65f); lineTo(cx+pr*.75f, cy); lineTo(cx-pr*.4f, cy+pr*.65f); close() }
                drawPath(t, color)
            }
            // EPG
            3 -> {
                val body = androidx.compose.ui.geometry.Rect(w*.06f, h*.12f, w*.94f, h*.92f)
                drawRoundRect(color.copy(.15f), body.topLeft, body.size, androidx.compose.ui.geometry.CornerRadius(4f))
                drawRoundRect(color, body.topLeft, body.size, androidx.compose.ui.geometry.CornerRadius(4f), style = androidx.compose.ui.graphics.drawscope.Stroke(sw))
                // header
                drawRoundRect(color.copy(.70f), body.topLeft, androidx.compose.ui.geometry.Size(body.width, h*.22f), androidx.compose.ui.geometry.CornerRadius(4f))
                // row bars
                for (i in 0..2) {
                    val top = h*(0.40f + i*0.175f)
                    drawRoundRect(if(i==0) color.copy(.75f) else color.copy(.25f), androidx.compose.ui.geometry.Offset(w*.12f,top), androidx.compose.ui.geometry.Size(w*.35f, h*.10f), androidx.compose.ui.geometry.CornerRadius(2f))
                    drawRoundRect(color.copy(.18f), androidx.compose.ui.geometry.Offset(w*.52f,top), androidx.compose.ui.geometry.Size(w*.36f, h*.10f), androidx.compose.ui.geometry.CornerRadius(2f))
                }
            }
            // Multi Vista
            4 -> {
                val gap = w*.07f; val cw = (w-gap*3f)/2f; val ch = (h-gap*3f)/2f
                for (row in 0..1) for (col in 0..1) {
                    val left = gap + col*(cw+gap); val top = gap + row*(ch+gap)
                    drawRoundRect(
                        if(row==0&&col==0) color.copy(.30f) else color.copy(.12f),
                        androidx.compose.ui.geometry.Offset(left,top), androidx.compose.ui.geometry.Size(cw,ch), androidx.compose.ui.geometry.CornerRadius(3f))
                    drawRoundRect(
                        if(row==0&&col==0) color else color.copy(.45f),
                        androidx.compose.ui.geometry.Offset(left,top), androidx.compose.ui.geometry.Size(cw,ch), androidx.compose.ui.geometry.CornerRadius(3f),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(if(row==0&&col==0) sw+0.5f else sw-0.3f))
                    if(row==0&&col==0) {
                        val cx2=left+cw/2f; val cy2=top+ch/2f; val pr=cw*.25f
                        val t = Path().apply { moveTo(cx2-pr*.4f,cy2-pr*.6f); lineTo(cx2+pr*.7f,cy2); lineTo(cx2-pr*.4f,cy2+pr*.6f); close() }
                        drawPath(t, color)
                    }
                }
            }
            // Catch Up
            5 -> {
                val cx = w*.54f; val cy = h*.52f; val r = h*.32f
                drawCircle(color.copy(.15f), r, androidx.compose.ui.geometry.Offset(cx,cy))
                drawCircle(color, r, androidx.compose.ui.geometry.Offset(cx,cy), style = androidx.compose.ui.graphics.drawscope.Stroke(sw))
                // manecillas
                val ha = Math.toRadians(-60.0)
                drawLine(Color.White.copy(.8f), androidx.compose.ui.geometry.Offset(cx,cy), androidx.compose.ui.geometry.Offset(cx+(r*.42f*Math.cos(ha)).toFloat(), cy+(r*.42f*Math.sin(ha)).toFloat()), sw+.5f, capR)
                val ma = Math.toRadians(-90.0)
                drawLine(color, androidx.compose.ui.geometry.Offset(cx,cy), androidx.compose.ui.geometry.Offset(cx+(r*.62f*Math.cos(ma)).toFloat(), cy+(r*.62f*Math.sin(ma)).toFloat()), sw, capR)
                drawCircle(color, 2.5f, androidx.compose.ui.geometry.Offset(cx,cy))
                // flecha rewind
                drawLine(color.copy(.7f), androidx.compose.ui.geometry.Offset(cx-r*.85f,cy), androidx.compose.ui.geometry.Offset(cx-r*.55f,cy-r*.30f), sw, capR)
                drawLine(color.copy(.7f), androidx.compose.ui.geometry.Offset(cx-r*.85f,cy), androidx.compose.ui.geometry.Offset(cx-r*.55f,cy+r*.30f), sw, capR)
            }
            // Ajustes (settings gear)
            else -> {
                val cx = w/2f; val cy = h/2f; val ro = h*.34f; val ri = h*.20f
                drawCircle(color.copy(.15f), ro, androidx.compose.ui.geometry.Offset(cx,cy))
                drawCircle(color, ri, androidx.compose.ui.geometry.Offset(cx,cy), style = androidx.compose.ui.graphics.drawscope.Stroke(sw))
                for (i in 0..7) {
                    val a = Math.toRadians((i * 45).toDouble())
                    val x1 = cx + (ro*.72f * Math.cos(a)).toFloat(); val y1 = cy + (ro*.72f * Math.sin(a)).toFloat()
                    val x2 = cx + (ro * Math.cos(a)).toFloat(); val y2 = cy + (ro * Math.sin(a)).toFloat()
                    drawLine(color, androidx.compose.ui.geometry.Offset(x1,y1), androidx.compose.ui.geometry.Offset(x2,y2), sw+.4f, capR)
                }
            }
        }
    }
}

@Composable
private fun DashboardSectionHeader(
    title: String,
    onSeeAll: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = title, style = MaterialTheme.typography.titleSmall, color = TextPrimary, fontWeight = FontWeight.Bold)
        TvButton(
            onClick = onSeeAll,
            colors = ButtonDefaults.colors(containerColor = SurfaceElevated, focusedContainerColor = SurfaceHighlight)
        ) {
            Text("VER TODO →", style = MaterialTheme.typography.labelSmall, color = OnSurfaceDim)
        }
    }
}

@Composable
private fun NowPlusBackground() {
    val brandRed = Color(0xFFE8001C)
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val spacing = 68.dp.toPx()
        val lineAlpha = Color.White.copy(alpha = 0.032f)
        val stroke = 0.7f
        var x = -h
        while (x < w) { drawLine(lineAlpha, Offset(x, 0f), Offset(x + h, h), stroke); x += spacing }
        x = 0f
        while (x < w + h) { drawLine(lineAlpha, Offset(x, 0f), Offset(x - h, h), stroke); x += spacing }
        val beam1 = Path().apply {
            moveTo(w * 0.70f, 0f); lineTo(w * 0.82f, 0f)
            lineTo(w * 0.24f, h); lineTo(w * 0.14f, h); close()
        }
        drawPath(beam1, brush = androidx.compose.ui.graphics.Brush.linearGradient(
            colors = listOf(brandRed.copy(0f), brandRed.copy(0.17f), brandRed.copy(0f)),
            start = Offset(w * 0.70f, 0f), end = Offset(w * 0.24f, h)
        ))
        val beam2 = Path().apply {
            moveTo(w * 0.48f, 0f); lineTo(w * 0.55f, 0f)
            lineTo(w * 0.30f, h); lineTo(w * 0.24f, h); close()
        }
        drawPath(beam2, brush = androidx.compose.ui.graphics.Brush.linearGradient(
            colors = listOf(brandRed.copy(0f), brandRed.copy(0.10f), brandRed.copy(0f)),
            start = Offset(w * 0.48f, 0f), end = Offset(w * 0.30f, h)
        ))
    }
}

@Composable
private fun NowBigCard(
    channelCount: Int,
    lastSyncedAt: Long,
    onClick: () -> Unit,
    onRefresh: () -> Unit = {},
    isSyncing: Boolean = false,
    modifier: Modifier = Modifier
) {
    val accent = Color(0xFFE8001C)
    val lastUpdated = remember(lastSyncedAt) { nowFormatTimeAgo(lastSyncedAt) }
    val fillProgress = remember { Animatable(0f) }
    val wavePhase = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    fun triggerWave() {
        scope.launch {
            fillProgress.snapTo(0f)
            wavePhase.snapTo(0f)
            launch { wavePhase.animateTo(4f * Math.PI.toFloat(), tween(1200, easing = LinearEasing)) }
            fillProgress.animateTo(1f, tween(750, easing = FastOutSlowInEasing))
            fillProgress.animateTo(0f, tween(450, easing = FastOutSlowInEasing))
        }
        onRefresh()
    }
    TvClickableSurface(
        onClick = onClick,
        modifier = modifier,
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(20.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color(0xFF120608),
            focusedContainerColor = Color(0xFF1A080C)
        ),
        border = ClickableSurfaceDefaults.border(
            border = Border(border = BorderStroke(1.5.dp, accent.copy(alpha = 0.28f)), shape = RoundedCornerShape(20.dp)),
            focusedBorder = Border(border = BorderStroke(2.5.dp, FocusBorder), shape = RoundedCornerShape(20.dp))
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.015f)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.fillMaxSize().background(
                Brush.verticalGradient(listOf(accent.copy(0.22f), accent.copy(0.06f), Color.Transparent))
            ))
            // Water fill overlay
            val fp = fillProgress.value
            if (fp > 0f) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width; val h = size.height
                    val waterY = h * (1f - fp)
                    val amp = 18f
                    val phase = wavePhase.value
                    fun wavePath(yOffset: Float, phaseShift: Float): Path {
                        val p = Path()
                        val y0 = waterY + yOffset + (amp * kotlin.math.sin(phaseShift.toDouble())).toFloat()
                        p.moveTo(0f, y0)
                        var x = 3f
                        while (x <= w) {
                            val y = waterY + yOffset + (amp * kotlin.math.sin((x / w * 2.0 * Math.PI + phase + phaseShift).toDouble())).toFloat()
                            p.lineTo(x, y.toFloat())
                            x += 3f
                        }
                        p.lineTo(w, h); p.lineTo(0f, h); p.close()
                        return p
                    }
                    drawPath(wavePath(0f, 0f), color = accent.copy(alpha = 0.30f))
                    drawPath(wavePath(8f, Math.PI.toFloat() * 0.6f), color = accent.copy(alpha = 0.18f))
                }
            }
            Column(
                modifier = Modifier.fillMaxSize().padding(18.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(modifier = Modifier.size(8.dp).background(accent, RoundedCornerShape(999.dp)))
                    Text(
                        text = "TV EN VIVO",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary
                    )
                }
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        NowTvIcon()
                        if (channelCount > 0) {
                            Box(modifier = Modifier.background(accent.copy(0.18f), RoundedCornerShape(999.dp)).padding(horizontal = 12.dp, vertical = 4.dp)) {
                                Text(text = "$channelCount canales", style = MaterialTheme.typography.labelMedium, color = accent)
                            }
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Actualizado: $lastUpdated", style = MaterialTheme.typography.labelSmall, color = TextPrimary.copy(alpha = 0.50f), modifier = Modifier.weight(1f))
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .background(Color.White.copy(alpha = 0.10f), RoundedCornerShape(7.dp))
                            .clickable { triggerWave() },
                        contentAlignment = Alignment.Center
                    ) {
                        NowRefreshIcon(spinning = isSyncing)
                    }
                }
            }
        }
    }
}

@Composable
private fun NowMediumCard(
    title: String,
    icon: @Composable () -> Unit,
    showRefresh: Boolean,
    lastSyncedAt: Long,
    onClick: () -> Unit,
    onRefresh: () -> Unit = {},
    isSyncing: Boolean = false,
    modifier: Modifier = Modifier,
    accentColor: Color = Color(0xFFFFAA00)
) {
    val lastUpdated = remember(lastSyncedAt) { nowFormatTimeAgo(lastSyncedAt) }
    val fillProgress = remember { Animatable(0f) }
    val wavePhase = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    fun triggerWave() {
        scope.launch {
            fillProgress.snapTo(0f)
            wavePhase.snapTo(0f)
            launch { wavePhase.animateTo(4f * Math.PI.toFloat(), tween(1200, easing = LinearEasing)) }
            fillProgress.animateTo(1f, tween(750, easing = FastOutSlowInEasing))
            fillProgress.animateTo(0f, tween(450, easing = FastOutSlowInEasing))
        }
        onRefresh()
    }
    TvClickableSurface(
        onClick = onClick,
        modifier = modifier,
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(20.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color(0xFF0E0E0E),
            focusedContainerColor = Color(0xFF181818)
        ),
        border = ClickableSurfaceDefaults.border(
            border = Border(border = BorderStroke(1.5.dp, accentColor.copy(alpha = 0.24f)), shape = RoundedCornerShape(20.dp)),
            focusedBorder = Border(border = BorderStroke(2.5.dp, FocusBorder), shape = RoundedCornerShape(20.dp))
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.015f)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.fillMaxSize().background(
                Brush.verticalGradient(listOf(accentColor.copy(0.16f), accentColor.copy(0.04f), Color.Transparent))
            ))
            // Water fill overlay
            val fp = fillProgress.value
            if (fp > 0f) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width; val h = size.height
                    val waterY = h * (1f - fp)
                    val amp = 14f
                    val phase = wavePhase.value
                    fun wavePath(yOffset: Float, phaseShift: Float): Path {
                        val p = Path()
                        val y0 = waterY + yOffset + (amp * kotlin.math.sin(phaseShift.toDouble())).toFloat()
                        p.moveTo(0f, y0)
                        var x = 3f
                        while (x <= w) {
                            val y = waterY + yOffset + (amp * kotlin.math.sin((x / w * 2.0 * Math.PI + phase + phaseShift).toDouble())).toFloat()
                            p.lineTo(x, y.toFloat())
                            x += 3f
                        }
                        p.lineTo(w, h); p.lineTo(0f, h); p.close()
                        return p
                    }
                    drawPath(wavePath(0f, 0f), color = accentColor.copy(alpha = 0.32f))
                    drawPath(wavePath(6f, Math.PI.toFloat() * 0.6f), color = accentColor.copy(alpha = 0.18f))
                }
            }
            Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.SpaceBetween) {
                Text(text = title, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = TextPrimary)
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) { icon() }
                if (showRefresh) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "Actualizado: $lastUpdated", style = MaterialTheme.typography.labelSmall, color = TextPrimary.copy(alpha = 0.45f), modifier = Modifier.weight(1f))
                        Box(
                            modifier = Modifier
                                .size(26.dp)
                                .background(Color.White.copy(alpha = 0.10f), RoundedCornerShape(7.dp))
                                .clickable { triggerWave() },
                            contentAlignment = Alignment.Center
                        ) { NowRefreshIcon(spinning = isSyncing) }
                    }
                }
            }
        }
    }
}

@Composable
private fun NowSmallCard(
    icon: @Composable () -> Unit,
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    accentColor: Color = Color(0xFF3B82F6)
) {
    TvClickableSurface(
        onClick = onClick,
        modifier = modifier,
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(18.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color(0xFF0E0E0E),
            focusedContainerColor = Color(0xFF181818)
        ),
        border = ClickableSurfaceDefaults.border(
            border = Border(border = BorderStroke(1.5.dp, accentColor.copy(alpha = 0.26f)), shape = RoundedCornerShape(18.dp)),
            focusedBorder = Border(border = BorderStroke(2.5.dp, FocusBorder), shape = RoundedCornerShape(18.dp))
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.02f)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.fillMaxSize().background(
                Brush.verticalGradient(listOf(accentColor.copy(0.18f), accentColor.copy(0.04f), Color.Transparent))
            ))
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    icon()
                    Text(text = title, style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold), color = TextPrimary, textAlign = TextAlign.Center, maxLines = 2)
                }
            }
        }
    }
}

@Composable
private fun NowTvIcon() {
    val red = Color(0xFFE8001C)
    val cap = androidx.compose.ui.graphics.StrokeCap.Round
    Canvas(modifier = Modifier.size(80.dp)) {
        val w = size.width; val h = size.height
        val cx = w / 2f; val cy = h * 0.50f
        // Outer glow
        drawCircle(Brush.radialGradient(listOf(red.copy(0.28f), Color.Transparent), center = Offset(cx, cy), radius = w * 0.52f), radius = w * 0.52f, center = Offset(cx, cy))
        // TV body filled
        val body = androidx.compose.ui.geometry.Rect(w * 0.06f, h * 0.20f, w * 0.94f, h * 0.78f)
        drawRoundRect(Brush.verticalGradient(listOf(Color(0xFF2A2A2A), Color(0xFF111111)), startY = body.top, endY = body.bottom), topLeft = body.topLeft, size = body.size, cornerRadius = androidx.compose.ui.geometry.CornerRadius(16f))
        drawRoundRect(Color.White.copy(0.75f), topLeft = body.topLeft, size = body.size, cornerRadius = androidx.compose.ui.geometry.CornerRadius(16f), style = androidx.compose.ui.graphics.drawscope.Stroke(3f))
        // Screen fill (dark blue glow)
        val scr = androidx.compose.ui.geometry.Rect(w * 0.13f, h * 0.27f, w * 0.87f, h * 0.71f)
        drawRoundRect(Brush.linearGradient(listOf(Color(0xFF0A0F22), Color(0xFF060D1A)), start = Offset(scr.left, scr.top), end = Offset(scr.right, scr.bottom)), topLeft = scr.topLeft, size = scr.size, cornerRadius = androidx.compose.ui.geometry.CornerRadius(8f))
        // Red screen glow
        drawRoundRect(red.copy(0.12f), topLeft = scr.topLeft, size = scr.size, cornerRadius = androidx.compose.ui.geometry.CornerRadius(8f))
        // Play circle (red filled)
        val pr = h * 0.13f
        drawCircle(red.copy(0.95f), radius = pr, center = Offset(cx, cy))
        drawCircle(Color.White.copy(0.20f), radius = pr, center = Offset(cx, cy), style = androidx.compose.ui.graphics.drawscope.Stroke(2.5f))
        val tri = Path().apply {
            moveTo(cx - pr * 0.28f, cy - pr * 0.50f)
            lineTo(cx + pr * 0.62f, cy)
            lineTo(cx - pr * 0.28f, cy + pr * 0.50f); close()
        }
        drawPath(tri, Color.White)
        // LIVE dot
        drawCircle(red, radius = w * 0.035f, center = Offset(scr.right - w * 0.05f, scr.top + h * 0.055f))
        // Antenna
        drawLine(red, Offset(w * 0.35f, h * 0.20f), Offset(w * 0.46f, h * 0.07f), 3.5f, cap = cap)
        drawLine(red, Offset(w * 0.65f, h * 0.20f), Offset(w * 0.54f, h * 0.07f), 3.5f, cap = cap)
        drawCircle(red, radius = 4f, center = Offset(w * 0.46f, h * 0.07f))
        drawCircle(red, radius = 4f, center = Offset(w * 0.54f, h * 0.07f))
        // Stand
        drawLine(Color.White.copy(0.50f), Offset(cx, h * 0.78f), Offset(cx, h * 0.90f), 3f, cap = cap)
        drawLine(Color.White.copy(0.50f), Offset(w * 0.30f, h * 0.90f), Offset(w * 0.70f, h * 0.90f), 3f, cap = cap)
    }
}

@Composable
private fun NowMovieIcon() {
    val amber = Color(0xFFFFAA00)
    val cap = androidx.compose.ui.graphics.StrokeCap.Round
    Canvas(modifier = Modifier.size(76.dp)) {
        val w = size.width; val h = size.height
        val cx = w / 2f; val cy = h * 0.54f
        // Glow
        drawCircle(Brush.radialGradient(listOf(amber.copy(0.22f), Color.Transparent), center = Offset(cx, cy), radius = w * 0.50f), radius = w * 0.50f, center = Offset(cx, cy))
        // Clapper body (filled)
        val body = androidx.compose.ui.geometry.Rect(w * 0.09f, h * 0.28f, w * 0.91f, h * 0.86f)
        drawRoundRect(Brush.verticalGradient(listOf(Color(0xFF2C2010), Color(0xFF141008)), startY = body.top, endY = body.bottom), topLeft = body.topLeft, size = body.size, cornerRadius = androidx.compose.ui.geometry.CornerRadius(12f))
        drawRoundRect(Color.White.copy(0.75f), topLeft = body.topLeft, size = body.size, cornerRadius = androidx.compose.ui.geometry.CornerRadius(12f), style = androidx.compose.ui.graphics.drawscope.Stroke(2.8f))
        // Clapper top bar (filled amber)
        val bar = androidx.compose.ui.geometry.Rect(w * 0.09f, h * 0.17f, w * 0.91f, h * 0.30f)
        drawRoundRect(Brush.horizontalGradient(listOf(amber, Color(0xFFFF6200)), startX = bar.left, endX = bar.right), topLeft = bar.topLeft, size = bar.size, cornerRadius = androidx.compose.ui.geometry.CornerRadius(8f))
        // Diagonal stripes on clapper bar
        for (i in 0..3) {
            val x = w * (0.22f + i * 0.18f)
            drawLine(Color.Black.copy(0.40f), Offset(x, h * 0.17f), Offset(x - w * 0.08f, h * 0.30f), 3.5f, cap = cap)
        }
        // Hinge line
        drawLine(Color.White.copy(0.60f), Offset(w * 0.09f, h * 0.30f), Offset(w * 0.91f, h * 0.30f), 2f)
        // Big play circle (red filled)
        val pr = h * 0.18f
        drawCircle(Color(0xFFE8001C).copy(0.95f), radius = pr, center = Offset(cx, cy))
        drawCircle(amber.copy(0.40f), radius = pr + 6f, center = Offset(cx, cy), style = androidx.compose.ui.graphics.drawscope.Stroke(2f))
        val tri = Path().apply {
            moveTo(cx - pr * 0.28f, cy - pr * 0.50f)
            lineTo(cx + pr * 0.62f, cy)
            lineTo(cx - pr * 0.28f, cy + pr * 0.50f); close()
        }
        drawPath(tri, Color.White)
    }
}

@Composable
private fun NowSeriesIcon() {
    val purple = Color(0xFF8B5CF6)
    val purpleDark = Color(0xFF6D28D9)
    Canvas(modifier = Modifier.size(76.dp)) {
        val w = size.width; val h = size.height
        val cx = w / 2f; val cy = h / 2f
        // Glow
        drawCircle(Brush.radialGradient(listOf(purple.copy(0.28f), Color.Transparent), center = Offset(cx, cy), radius = w * 0.52f), radius = w * 0.52f, center = Offset(cx, cy))
        // Back card (offset top-right)
        val back = androidx.compose.ui.geometry.Rect(w * 0.24f, h * 0.08f, w * 0.92f, h * 0.66f)
        drawRoundRect(Brush.verticalGradient(listOf(purpleDark.copy(0.55f), purpleDark.copy(0.20f)), startY = back.top, endY = back.bottom), topLeft = back.topLeft, size = back.size, cornerRadius = androidx.compose.ui.geometry.CornerRadius(10f))
        drawRoundRect(purple.copy(0.40f), topLeft = back.topLeft, size = back.size, cornerRadius = androidx.compose.ui.geometry.CornerRadius(10f), style = androidx.compose.ui.graphics.drawscope.Stroke(2f))
        // Middle card
        val mid = androidx.compose.ui.geometry.Rect(w * 0.12f, h * 0.20f, w * 0.80f, h * 0.78f)
        drawRoundRect(Brush.verticalGradient(listOf(purpleDark.copy(0.70f), Color(0xFF1A0A30)), startY = mid.top, endY = mid.bottom), topLeft = mid.topLeft, size = mid.size, cornerRadius = androidx.compose.ui.geometry.CornerRadius(12f))
        drawRoundRect(purple.copy(0.60f), topLeft = mid.topLeft, size = mid.size, cornerRadius = androidx.compose.ui.geometry.CornerRadius(12f), style = androidx.compose.ui.graphics.drawscope.Stroke(2f))
        // Front card (main screen)
        val front = androidx.compose.ui.geometry.Rect(w * 0.04f, h * 0.32f, w * 0.72f, h * 0.90f)
        drawRoundRect(Brush.verticalGradient(listOf(Color(0xFF1E0A3C), Color(0xFF0D0520)), startY = front.top, endY = front.bottom), topLeft = front.topLeft, size = front.size, cornerRadius = androidx.compose.ui.geometry.CornerRadius(14f))
        drawRoundRect(Brush.verticalGradient(listOf(purple, purpleDark), startY = front.top, endY = front.bottom), topLeft = front.topLeft, size = front.size, cornerRadius = androidx.compose.ui.geometry.CornerRadius(14f), style = androidx.compose.ui.graphics.drawscope.Stroke(2.5f))
        // Play button on front card
        val pcx = front.left + front.width * 0.42f
        val pcy = front.top + front.height * 0.50f
        val pr = front.width * 0.22f
        drawCircle(purple.copy(0.25f), radius = pr, center = Offset(pcx, pcy))
        val play = Path().apply {
            moveTo(pcx - pr * 0.35f, pcy - pr * 0.55f)
            lineTo(pcx + pr * 0.65f, pcy)
            lineTo(pcx - pr * 0.35f, pcy + pr * 0.55f)
            close()
        }
        drawPath(play, Brush.linearGradient(listOf(Color.White, purple.copy(0.85f)), start = Offset(pcx - pr * 0.3f, pcy - pr * 0.3f), end = Offset(pcx + pr * 0.3f, pcy + pr * 0.3f)))
    }
}

@Composable
private fun NowEpgIcon() {
    val blue = Color(0xFF3B82F6)
    Canvas(modifier = Modifier.size(62.dp)) {
        val w = size.width; val h = size.height
        val cx = w / 2f; val cy = h / 2f
        // Glow
        drawCircle(Brush.radialGradient(listOf(blue.copy(0.24f), Color.Transparent), center = Offset(cx, cy), radius = w * 0.48f), radius = w * 0.48f, center = Offset(cx, cy))
        // Calendar body filled
        val body = androidx.compose.ui.geometry.Rect(w * 0.08f, h * 0.18f, w * 0.92f, h * 0.90f)
        drawRoundRect(Brush.verticalGradient(listOf(Color(0xFF0A1530), Color(0xFF060D20)), startY = body.top, endY = body.bottom), topLeft = body.topLeft, size = body.size, cornerRadius = androidx.compose.ui.geometry.CornerRadius(10f))
        drawRoundRect(blue.copy(0.80f), topLeft = body.topLeft, size = body.size, cornerRadius = androidx.compose.ui.geometry.CornerRadius(10f), style = androidx.compose.ui.graphics.drawscope.Stroke(2.5f))
        // Header bar (blue filled)
        val header = androidx.compose.ui.geometry.Rect(w * 0.08f, h * 0.18f, w * 0.92f, h * 0.36f)
        drawRoundRect(Brush.horizontalGradient(listOf(blue, Color(0xFF1D4ED8)), startX = header.left, endX = header.right), topLeft = header.topLeft, size = header.size, cornerRadius = androidx.compose.ui.geometry.CornerRadius(10f))
        // Header dots (ring binders)
        drawCircle(Color.White.copy(0.80f), radius = w * 0.035f, center = Offset(w * 0.32f, h * 0.18f))
        drawCircle(Color.White.copy(0.80f), radius = w * 0.035f, center = Offset(w * 0.68f, h * 0.18f))
        // 3 schedule rows
        val barH = h * 0.09f
        for (i in 0..2) {
            val top = h * (0.42f + i * 0.155f)
            drawRoundRect(if (i == 0) blue.copy(0.90f) else Color.White.copy(0.18f), topLeft = Offset(w * 0.14f, top), size = androidx.compose.ui.geometry.Size(w * 0.34f, barH), cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f))
            drawRoundRect(Color.White.copy(0.14f), topLeft = Offset(w * 0.52f, top), size = androidx.compose.ui.geometry.Size(w * 0.34f, barH), cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f))
        }
    }
}

@Composable
private fun NowMultiViewIcon() {
    val purple = Color(0xFF8B5CF6)
    val red = Color(0xFFE8001C)
    Canvas(modifier = Modifier.size(62.dp)) {
        val w = size.width; val h = size.height
        val cx = w / 2f; val cy = h / 2f
        // Glow
        drawCircle(Brush.radialGradient(listOf(purple.copy(0.24f), Color.Transparent), center = Offset(cx, cy), radius = w * 0.48f), radius = w * 0.48f, center = Offset(cx, cy))
        val gap = w * 0.08f
        val cellW = (w - gap * 3f) / 2f
        val cellH = (h - gap * 3f) / 2f
        val accents = listOf(red, purple.copy(0.70f), purple.copy(0.50f), purple.copy(0.35f))
        var idx = 0
        for (row in 0..1) {
            for (col in 0..1) {
                val left = gap + col * (cellW + gap)
                val top = gap + row * (cellH + gap)
                val ac = accents[idx++]
                drawRoundRect(Brush.verticalGradient(listOf(ac.copy(0.35f), ac.copy(0.15f)), startY = top, endY = top + cellH), topLeft = Offset(left, top), size = androidx.compose.ui.geometry.Size(cellW, cellH), cornerRadius = androidx.compose.ui.geometry.CornerRadius(7f))
                drawRoundRect(if (row == 0 && col == 0) red.copy(0.90f) else Color.White.copy(0.40f), topLeft = Offset(left, top), size = androidx.compose.ui.geometry.Size(cellW, cellH), cornerRadius = androidx.compose.ui.geometry.CornerRadius(7f), style = androidx.compose.ui.graphics.drawscope.Stroke(if (row == 0 && col == 0) 2.5f else 1.5f))
                if (row == 0 && col == 0) {
                    val pcx = left + cellW / 2f; val pcy = top + cellH / 2f
                    val pr = cellW * 0.28f
                    drawCircle(red.copy(0.90f), radius = pr, center = Offset(pcx, pcy))
                    val tri = Path().apply {
                        moveTo(pcx - pr * 0.28f, pcy - pr * 0.50f)
                        lineTo(pcx + pr * 0.62f, pcy)
                        lineTo(pcx - pr * 0.28f, pcy + pr * 0.50f); close()
                    }
                    drawPath(tri, Color.White)
                }
            }
        }
    }
}

@Composable
private fun NowCatchUpIcon() {
    val orange = Color(0xFFFF6200)
    val cap = androidx.compose.ui.graphics.StrokeCap.Round
    Canvas(modifier = Modifier.size(62.dp)) {
        val w = size.width; val h = size.height
        val cx = w * 0.52f; val cy = h * 0.52f; val r = h * 0.33f
        // Glow
        drawCircle(Brush.radialGradient(listOf(orange.copy(0.26f), Color.Transparent), center = Offset(cx, cy), radius = r * 1.60f), radius = r * 1.60f, center = Offset(cx, cy))
        // Clock fill (gradient)
        drawCircle(Brush.radialGradient(listOf(Color(0xFF1A0C00), Color(0xFF0A0600)), center = Offset(cx, cy), radius = r), radius = r, center = Offset(cx, cy))
        // Clock border
        drawCircle(orange.copy(0.85f), radius = r, center = Offset(cx, cy), style = androidx.compose.ui.graphics.drawscope.Stroke(3f))
        // Tick marks
        for (i in 0..11) {
            val angle = Math.toRadians((i * 30 - 90).toDouble())
            val r1 = if (i % 3 == 0) r * 0.72f else r * 0.80f
            drawLine(Color.White.copy(if (i % 3 == 0) 0.70f else 0.30f),
                Offset(cx + (r1).toFloat() * Math.cos(angle).toFloat(), cy + (r1).toFloat() * Math.sin(angle).toFloat()),
                Offset(cx + r * Math.cos(angle).toFloat(), cy + r * Math.sin(angle).toFloat()),
                if (i % 3 == 0) 2.5f else 1.5f, cap = cap)
        }
        // Hour hand (short, white)
        val ha = Math.toRadians(-60.0)
        drawLine(Color.White.copy(0.90f), Offset(cx, cy), Offset(cx + (r * 0.44f * Math.cos(ha)).toFloat(), cy + (r * 0.44f * Math.sin(ha)).toFloat()), 3.5f, cap = cap)
        // Minute hand (long, orange)
        val ma = Math.toRadians(-90.0)
        drawLine(orange, Offset(cx, cy), Offset(cx + (r * 0.65f * Math.cos(ma)).toFloat(), cy + (r * 0.65f * Math.sin(ma)).toFloat()), 3f, cap = cap)
        drawCircle(orange, radius = 5f, center = Offset(cx, cy))
        drawCircle(Color.White, radius = 2.5f, center = Offset(cx, cy))
        // Rewind arrows (two curved arrows)
        val ax = cx - r * 0.90f; val ay = cy - r * 0.14f
        drawArc(orange, startAngle = 160f, sweepAngle = 130f, useCenter = false,
            topLeft = Offset(ax - r * 0.22f, ay - r * 0.22f), size = androidx.compose.ui.geometry.Size(r * 0.44f, r * 0.44f),
            style = androidx.compose.ui.graphics.drawscope.Stroke(3f, cap = cap))
        drawLine(orange, Offset(ax - r * 0.14f, ay + r * 0.18f), Offset(ax - r * 0.22f, ay + r * 0.22f), 3f, cap = cap)
        drawLine(orange, Offset(ax + r * 0.04f, ay + r * 0.12f), Offset(ax - r * 0.22f, ay + r * 0.22f), 3f, cap = cap)
    }
}

@Composable
private fun NowRefreshIcon(spinning: Boolean = false) {
    val infiniteTransition = rememberInfiniteTransition(label = "refresh_spin")
    val angle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = LinearEasing)
        ),
        label = "refresh_angle"
    )
    Canvas(modifier = Modifier.size(16.dp).graphicsLayer { rotationZ = if (spinning) angle else 0f }) {
        val w = size.width; val h = size.height
        val white = Color.White
        val cap = androidx.compose.ui.graphics.StrokeCap.Round
        val cx = w / 2f; val cy = h / 2f; val r = w * 0.36f
        drawArc(white, startAngle = -180f, sweepAngle = 270f, useCenter = false,
            topLeft = Offset(cx - r, cy - r), size = androidx.compose.ui.geometry.Size(r * 2, r * 2),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3.2f, cap = cap))
        drawLine(white, Offset(cx - r, cy - 1f), Offset(cx - r + 5.5f, cy - 5.5f), 3.2f, cap = cap)
        drawLine(white, Offset(cx - r, cy - 1f), Offset(cx - r + 5.5f, cy + 4.5f), 3.2f, cap = cap)
    }
}

private fun nowFormatTimeAgo(timestampMs: Long): String {
    if (timestampMs <= 0L) return "nunca"
    val diff = System.currentTimeMillis() - timestampMs
    val mins = diff / 60_000L
    val hours = mins / 60L
    val days = hours / 24L
    return when {
        mins < 2L -> "ahora"
        mins < 60L -> "hace $mins min"
        hours < 24L -> "hace $hours h"
        else -> "hace $days días"
    }
}
