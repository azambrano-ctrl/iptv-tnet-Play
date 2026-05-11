---
name: Project StreamVault-IPTV / TNET play
description: Android TV IPTV app — architecture, brand colors, key files changed, Git remote
type: project
originSessionId: 948572b8-667b-4e20-934c-6bfd1076aad1
---
## Repo
- Local: `C:\Users\User\Downloads\StreamVault-IPTV`
- Git remote (user's): https://github.com/azambrano-ctrl/iptv-tnet-Play.git
- Original upstream: https://github.com/Davidona/StreamVault-IPTV.git

## Brand Colors (AppColors.kt)
- `Brand = Color(0xFFE8001C)` — TNET red
- `BrandGradientEnd = Color(0xFFFF6200)` — TNET orange
- `FocusBorder = Color(0xFFFF4500)` — orange-red focus border (used on ALL interactive elements)
- `Focus = Color(0xFFFFFFFF)` — white content color when focused

## Architecture
- Multi-module: `:app`, `:domain`, `:data`, `:player`
- Jetpack Compose TV (`androidx.tv.material3`)
- Navigation: `Routes` object with `LIVE_TV`, `MOVIES`, `SERIES`, `EPG`, `SETTINGS`, `SEARCH`, `MULTI_VIEW`
- ViewModels via Hilt (`hiltViewModel()` from `androidx.hilt.lifecycle.viewmodel.compose`)
- Java toolchain: JBR 21 (Android Studio JBR at `C:\Program Files\Android\Android Studio\jbr`)
  - `domain/build.gradle.kts` uses `jvmToolchain(21)` (changed from 17 to match installed JDK)
  - `gradle.properties` has `org.gradle.java.home` pointing to JBR

## Key Files Changed
- `app/.../ui/design/AppColors.kt` — TNET brand palette
- `app/.../ui/components/shell/AppShell.kt` — TopNavigationBar: logo 42dp, icons RIGHT-aligned, icon-only nav (label appears on focus/select)
- `app/.../ui/screens/dashboard/DashboardScreen.kt` — Full NOW+ style grid dashboard with Canvas background, Canvas icons
- `app/.../ui/screens/home/HomeScreen.kt` — FocusBorder on quick filters
- `app/.../ui/screens/player/overlay/PlayerControlsChrome.kt` — FocusBorder on transport buttons
- `app/.../ui/screens/player/overlay/PlayerSystemOverlays.kt` — FocusBorder on all overlays

## Dashboard Layout (NowPlusDashboard)
- Full-screen grid, no scrolling — designed for TV D-pad
- Left: big "TV EN VIVO" card (Canvas TV icon + channel count)
- Top-right: "PELÍCULAS" (clapperboard icon) + "DESCARGAR" (cloud download icon)
- Bottom-right: "EPG" (TV guide icon) + "PANTALLA MÚLTIPLE" (2x2 grid icon) + "CATCH UP" (clock icon)
- Background: Canvas geometric diamond grid + 2 red diagonal laser beams
- Footer: EXPIRACIÓN date + "Conectado: provider name"

## Focus Border Pattern
All `TvClickableSurface` / `Button` components use:
```kotlin
border = ClickableSurfaceDefaults.border(
    focusedBorder = Border(
        border = BorderStroke(2.dp, FocusBorder),
        shape = RoundedCornerShape(Xdp)
    )
)
```

**Why:** `Border(BorderStroke, Shape)` — second arg must use named param `shape =` (not positional), because positional second arg is `inset: Dp`.
