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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
        AppScreenScaffold(
            currentRoute = currentRoute,
            onNavigate = onNavigate,
            title = stringResource(R.string.nav_home),
            subtitle = provider?.name,
            navigationChrome = AppNavigationChrome.TopBar,
            compactHeader = true,
            showScreenHeader = false
        ) {
            if (provider == null) {
                EmptyDashboard(
                    onAddProvider = onAddProvider,
                    onOpenSettings = { onNavigate(Routes.SETTINGS) }
                )
                return@AppScreenScaffold
            }
            NowPlusDashboard(
                uiState = uiState,
                onNavigate = onNavigate
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
    onNavigate: (String) -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        NowPlusBackground()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 22.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                NowBigCard(
                    channelCount = uiState.stats.liveChannelCount,
                    lastSyncedAt = uiState.providerHealth.lastSyncedAt,
                    onClick = { onNavigate(Routes.LIVE_TV) },
                    modifier = Modifier.weight(1f).fillMaxHeight()
                )
                Column(
                    modifier = Modifier.weight(1.55f).fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        NowMediumCard(
                            title = "PELÍCULAS",
                            icon = { NowMovieIcon() },
                            showRefresh = true,
                            lastSyncedAt = uiState.providerHealth.lastSyncedAt,
                            onClick = { onNavigate(Routes.MOVIES) },
                            modifier = Modifier.weight(1.05f).fillMaxHeight()
                        )
                        NowMediumCard(
                            title = "DESCARGAR",
                            icon = { NowDownloadIcon() },
                            showRefresh = false,
                            lastSyncedAt = 0L,
                            onClick = { onNavigate(Routes.SEARCH) },
                            modifier = Modifier.weight(0.95f).fillMaxHeight()
                        )
                    }
                    Row(
                        modifier = Modifier.weight(0.62f).fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        NowSmallCard(
                            icon = { NowEpgIcon() },
                            title = "EPG",
                            onClick = { onNavigate(Routes.EPG) },
                            modifier = Modifier.weight(1f).fillMaxHeight()
                        )
                        NowSmallCard(
                            icon = { NowMultiViewIcon() },
                            title = "PANTALLA\nMÚLTIPLE",
                            onClick = { onNavigate(Routes.MULTI_VIEW) },
                            modifier = Modifier.weight(1f).fillMaxHeight()
                        )
                        NowSmallCard(
                            icon = { NowCatchUpIcon() },
                            title = "CATCH UP",
                            onClick = { onNavigate(Routes.EPG) },
                            modifier = Modifier.weight(1f).fillMaxHeight()
                        )
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val expiryText = remember(uiState.providerHealth.expirationDate) {
                    if (uiState.providerHealth.expirationDate == null) "EXPIRACIÓN: Ilimitado"
                    else {
                        val fmt = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
                        "EXPIRACIÓN: ${fmt.format(Date(uiState.providerHealth.expirationDate))}"
                    }
                }
                Text(text = expiryText, style = MaterialTheme.typography.labelMedium, color = TextPrimary.copy(alpha = 0.80f))
                Text(
                    text = "Conectado : ${uiState.provider?.name ?: "TNET play"}",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextPrimary.copy(alpha = 0.80f)
                )
            }
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
    modifier: Modifier = Modifier
) {
    val lastUpdated = remember(lastSyncedAt) { nowFormatTimeAgo(lastSyncedAt) }
    TvClickableSurface(
        onClick = onClick,
        modifier = modifier,
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(20.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color(0xFF141414),
            focusedContainerColor = Color(0xFF1A1A1A)
        ),
        border = ClickableSurfaceDefaults.border(
            border = Border(border = BorderStroke(1.5.dp, Color.White.copy(alpha = 0.14f)), shape = RoundedCornerShape(20.dp)),
            focusedBorder = Border(border = BorderStroke(2.5.dp, FocusBorder), shape = RoundedCornerShape(20.dp))
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.015f)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(Color(0xFF1C1C1C), Color(0xFF0C0C0C))))
        ) {
            Column(
                modifier = Modifier.fillMaxSize().padding(18.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "TV EN VIVO",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary
                )
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        NowTvIcon()
                        if (channelCount > 0) {
                            Text(
                                text = "$channelCount canales",
                                style = MaterialTheme.typography.labelMedium,
                                color = TextPrimary.copy(alpha = 0.50f)
                            )
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Última actualización: $lastUpdated",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextPrimary.copy(alpha = 0.55f),
                        modifier = Modifier.weight(1f)
                    )
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .background(Color.White.copy(alpha = 0.10f), RoundedCornerShape(7.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        NowRefreshIcon()
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
    modifier: Modifier = Modifier
) {
    val lastUpdated = remember(lastSyncedAt) { nowFormatTimeAgo(lastSyncedAt) }
    TvClickableSurface(
        onClick = onClick,
        modifier = modifier,
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(20.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color(0xFF141414),
            focusedContainerColor = Color(0xFF1A1A1A)
        ),
        border = ClickableSurfaceDefaults.border(
            border = Border(border = BorderStroke(1.5.dp, Color.White.copy(alpha = 0.14f)), shape = RoundedCornerShape(20.dp)),
            focusedBorder = Border(border = BorderStroke(2.5.dp, FocusBorder), shape = RoundedCornerShape(20.dp))
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.015f)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(Color(0xFF1C1C1C), Color(0xFF0C0C0C))))
        ) {
            Column(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary
                )
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    icon()
                }
                if (showRefresh) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Última actualización: $lastUpdated",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextPrimary.copy(alpha = 0.50f),
                            modifier = Modifier.weight(1f)
                        )
                        Box(
                            modifier = Modifier
                                .size(26.dp)
                                .background(Color.White.copy(alpha = 0.10f), RoundedCornerShape(7.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            NowRefreshIcon()
                        }
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
    modifier: Modifier = Modifier
) {
    TvClickableSurface(
        onClick = onClick,
        modifier = modifier,
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(18.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color(0xFF141414),
            focusedContainerColor = Color(0xFF1A1A1A)
        ),
        border = ClickableSurfaceDefaults.border(
            border = Border(border = BorderStroke(1.5.dp, Color.White.copy(alpha = 0.14f)), shape = RoundedCornerShape(18.dp)),
            focusedBorder = Border(border = BorderStroke(2.5.dp, FocusBorder), shape = RoundedCornerShape(18.dp))
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.02f)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(Color(0xFF1C1C1C), Color(0xFF0C0C0C)))),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                icon()
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary,
                    textAlign = TextAlign.Center,
                    maxLines = 2
                )
            }
        }
    }
}

@Composable
private fun NowTvIcon() {
    val brandRed = Color(0xFFE8001C)
    Canvas(modifier = Modifier.size(72.dp)) {
        val w = size.width
        val h = size.height
        val stroke = androidx.compose.ui.graphics.drawscope.Stroke(width = 3.5f)
        val screen = androidx.compose.ui.geometry.Rect(w * 0.08f, h * 0.22f, w * 0.92f, h * 0.75f)
        drawRoundRect(color = Color.White.copy(alpha = 0.85f), topLeft = screen.topLeft, size = screen.size, cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f), style = stroke)
        drawLine(Color.White.copy(0.60f), Offset(w * 0.50f, h * 0.75f), Offset(w * 0.50f, h * 0.88f), 3f)
        drawLine(Color.White.copy(0.60f), Offset(w * 0.28f, h * 0.88f), Offset(w * 0.72f, h * 0.88f), 3f)
        drawLine(brandRed.copy(0.90f), Offset(w * 0.32f, h * 0.08f), Offset(w * 0.48f, h * 0.20f), 3.5f)
        drawLine(brandRed.copy(0.90f), Offset(w * 0.68f, h * 0.08f), Offset(w * 0.52f, h * 0.20f), 3.5f)
        val tri = Path().apply {
            moveTo(w * 0.38f, h * 0.38f); lineTo(w * 0.70f, h * 0.48f); lineTo(w * 0.38f, h * 0.60f); close()
        }
        drawPath(tri, color = Color.White.copy(alpha = 0.90f))
    }
}

@Composable
private fun NowMovieIcon() {
    val brand = Color(0xFFE8001C)
    Canvas(modifier = Modifier.size(72.dp)) {
        val w = size.width; val h = size.height
        val white = Color.White.copy(alpha = 0.92f)
        val dim = Color.White.copy(alpha = 0.28f)
        val sThick = androidx.compose.ui.graphics.drawscope.Stroke(width = 3.5f, cap = androidx.compose.ui.graphics.StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round)
        // Outer rounded rect (screen)
        val rect = androidx.compose.ui.geometry.Rect(w * 0.08f, h * 0.28f, w * 0.92f, h * 0.84f)
        drawRoundRect(color = dim, topLeft = rect.topLeft, size = rect.size, cornerRadius = androidx.compose.ui.geometry.CornerRadius(10f))
        drawRoundRect(color = white, topLeft = rect.topLeft, size = rect.size, cornerRadius = androidx.compose.ui.geometry.CornerRadius(10f), style = sThick)
        // Top clapper bar (filled)
        val clapBar = androidx.compose.ui.geometry.Rect(w * 0.08f, h * 0.18f, w * 0.92f, h * 0.30f)
        drawRoundRect(color = Color.White.copy(alpha = 0.18f), topLeft = clapBar.topLeft, size = clapBar.size, cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f))
        drawRoundRect(color = white, topLeft = clapBar.topLeft, size = clapBar.size, cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f), style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.5f))
        // Diagonal stripe on clapper
        drawLine(white.copy(0.50f), Offset(w * 0.28f, h * 0.18f), Offset(w * 0.22f, h * 0.30f), 2.5f)
        drawLine(white.copy(0.50f), Offset(w * 0.46f, h * 0.18f), Offset(w * 0.40f, h * 0.30f), 2.5f)
        drawLine(white.copy(0.50f), Offset(w * 0.64f, h * 0.18f), Offset(w * 0.58f, h * 0.30f), 2.5f)
        // Big play circle (brand red fill)
        val cx = w * 0.50f; val cy = h * 0.57f; val r = h * 0.17f
        drawCircle(brand.copy(alpha = 0.25f), radius = r, center = Offset(cx, cy))
        drawCircle(white, radius = r, center = Offset(cx, cy), style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.5f))
        val tri = Path().apply {
            moveTo(cx - r * 0.28f, cy - r * 0.48f)
            lineTo(cx + r * 0.62f, cy)
            lineTo(cx - r * 0.28f, cy + r * 0.48f)
            close()
        }
        drawPath(tri, color = white)
    }
}

@Composable
private fun NowDownloadIcon() {
    Canvas(modifier = Modifier.size(72.dp)) {
        val w = size.width; val h = size.height
        val white = Color.White.copy(alpha = 0.92f)
        val cap = androidx.compose.ui.graphics.StrokeCap.Round
        val join = androidx.compose.ui.graphics.StrokeJoin.Round
        // Cloud-style download: filled rounded rect top
        val cloud = androidx.compose.ui.geometry.Rect(w * 0.22f, h * 0.12f, w * 0.78f, h * 0.46f)
        drawRoundRect(Color.White.copy(alpha = 0.18f), topLeft = cloud.topLeft, size = cloud.size, cornerRadius = androidx.compose.ui.geometry.CornerRadius(cloud.height / 2))
        drawRoundRect(white, topLeft = cloud.topLeft, size = cloud.size, cornerRadius = androidx.compose.ui.geometry.CornerRadius(cloud.height / 2), style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3f))
        // Arrow shaft
        drawLine(white, Offset(w * 0.50f, h * 0.44f), Offset(w * 0.50f, h * 0.72f), 3.5f)
        // Arrow head (filled triangle)
        val tri = Path().apply {
            moveTo(w * 0.28f, h * 0.58f)
            lineTo(w * 0.50f, h * 0.78f)
            lineTo(w * 0.72f, h * 0.58f)
            close()
        }
        drawPath(tri, color = white)
        // Base line with rounded caps
        drawLine(white, Offset(w * 0.18f, h * 0.86f), Offset(w * 0.82f, h * 0.86f), 3.5f, cap = cap)
    }
}

@Composable
private fun NowEpgIcon() {
    Canvas(modifier = Modifier.size(58.dp)) {
        val w = size.width; val h = size.height
        val white = Color.White.copy(alpha = 0.92f)
        val accent = Color(0xFFE8001C).copy(alpha = 0.80f)
        val sMain = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.8f, cap = androidx.compose.ui.graphics.StrokeCap.Round)
        // TV screen body
        val screen = androidx.compose.ui.geometry.Rect(w * 0.06f, h * 0.20f, w * 0.94f, h * 0.82f)
        drawRoundRect(Color.White.copy(alpha = 0.12f), topLeft = screen.topLeft, size = screen.size, cornerRadius = androidx.compose.ui.geometry.CornerRadius(8f))
        drawRoundRect(white, topLeft = screen.topLeft, size = screen.size, cornerRadius = androidx.compose.ui.geometry.CornerRadius(8f), style = sMain)
        // 3 horizontal schedule bars inside screen
        val barH = h * 0.09f
        for (i in 0..2) {
            val top = h * (0.30f + i * 0.16f)
            // first segment (accent color = "now playing")
            drawRoundRect(if (i == 0) accent else Color.White.copy(0.20f), topLeft = Offset(w * 0.12f, top), size = androidx.compose.ui.geometry.Size(w * 0.32f, barH), cornerRadius = androidx.compose.ui.geometry.CornerRadius(3f))
            // second segment
            drawRoundRect(Color.White.copy(0.15f), topLeft = Offset(w * 0.46f, top), size = androidx.compose.ui.geometry.Size(w * 0.42f, barH), cornerRadius = androidx.compose.ui.geometry.CornerRadius(3f))
        }
        // "EPG" top label dot
        drawCircle(accent, radius = w * 0.04f, center = Offset(w * 0.50f, h * 0.13f))
    }
}

@Composable
private fun NowMultiViewIcon() {
    val brand = Color(0xFFE8001C)
    Canvas(modifier = Modifier.size(58.dp)) {
        val w = size.width; val h = size.height
        val white = Color.White.copy(alpha = 0.92f)
        val gap = w * 0.07f
        val cellW = (w - gap * 3f) / 2f
        val cellH = (h - gap * 3f) / 2f
        for (row in 0..1) {
            for (col in 0..1) {
                val left = gap + col * (cellW + gap)
                val top = gap + row * (cellH + gap)
                val fillColor = if (row == 0 && col == 0) brand.copy(alpha = 0.30f) else Color.White.copy(0.10f)
                drawRoundRect(fillColor, topLeft = Offset(left, top), size = androidx.compose.ui.geometry.Size(cellW, cellH), cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f))
                drawRoundRect(if (row == 0 && col == 0) white else white.copy(0.55f), topLeft = Offset(left, top), size = androidx.compose.ui.geometry.Size(cellW, cellH), cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f), style = androidx.compose.ui.graphics.drawscope.Stroke(width = if (row == 0 && col == 0) 2.8f else 1.8f))
                // Small play in top-left cell
                if (row == 0 && col == 0) {
                    val cx = left + cellW / 2f; val cy = top + cellH / 2f
                    val tri = Path().apply {
                        moveTo(cx - cellW * 0.18f, cy - cellH * 0.24f)
                        lineTo(cx + cellW * 0.26f, cy)
                        lineTo(cx - cellW * 0.18f, cy + cellH * 0.24f)
                        close()
                    }
                    drawPath(tri, color = white)
                }
            }
        }
    }
}

@Composable
private fun NowCatchUpIcon() {
    val brand = Color(0xFFE8001C)
    Canvas(modifier = Modifier.size(58.dp)) {
        val w = size.width; val h = size.height
        val white = Color.White.copy(alpha = 0.92f)
        val capR = androidx.compose.ui.graphics.StrokeCap.Round
        val cx = w * 0.52f; val cy = h * 0.54f; val r = h * 0.32f
        // Clock fill
        drawCircle(Color.White.copy(0.10f), radius = r, center = Offset(cx, cy))
        // Clock outline (almost full circle, gap at ~8 o'clock for back-arrow)
        drawArc(white, startAngle = 140f, sweepAngle = 280f, useCenter = false,
            topLeft = Offset(cx - r, cy - r), size = androidx.compose.ui.geometry.Size(r * 2, r * 2),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3f, cap = capR))
        // Back arrow at the gap (8 o'clock position)
        val ax = cx - r * 0.86f; val ay = cy + r * 0.50f
        drawLine(brand, Offset(ax - 7f, ay - 10f), Offset(ax + 2f, ay + 2f), 3f, cap = capR)
        drawLine(brand, Offset(ax - 7f, ay - 10f), Offset(ax + 9f, ay - 12f), 3f, cap = capR)
        // Clock hands
        drawLine(white, Offset(cx, cy), Offset(cx, cy - r * 0.52f), 3f, cap = capR)
        drawLine(white, Offset(cx, cy), Offset(cx + r * 0.40f, cy + r * 0.28f), 3f, cap = capR)
        drawCircle(brand, radius = 4f, center = Offset(cx, cy))
        // Tick marks (12, 3, 6, 9)
        for (i in 0..3) {
            val angle = Math.toRadians((i * 90 - 90).toDouble())
            val x1 = cx + (r * 0.78f) * Math.cos(angle).toFloat()
            val y1 = cy + (r * 0.78f) * Math.sin(angle).toFloat()
            val x2 = cx + r * Math.cos(angle).toFloat()
            val y2 = cy + r * Math.sin(angle).toFloat()
            drawLine(white.copy(0.50f), Offset(x1, y1), Offset(x2, y2), 2f, cap = capR)
        }
    }
}

@Composable
private fun NowRefreshIcon() {
    Canvas(modifier = Modifier.size(16.dp)) {
        val w = size.width; val h = size.height
        val white = Color.White.copy(alpha = 0.80f)
        val cap = androidx.compose.ui.graphics.StrokeCap.Round
        val cx = w / 2f; val cy = h / 2f; val r = w * 0.34f
        drawArc(white, startAngle = -180f, sweepAngle = 270f, useCenter = false,
            topLeft = Offset(cx - r, cy - r), size = androidx.compose.ui.geometry.Size(r * 2, r * 2),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f, cap = cap))
        // Arrow head pointing clockwise
        drawLine(white, Offset(cx - r, cy - 1f), Offset(cx - r + 5f, cy - 5f), 2f, cap = cap)
        drawLine(white, Offset(cx - r, cy - 1f), Offset(cx - r + 5f, cy + 4f), 2f, cap = cap)
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
