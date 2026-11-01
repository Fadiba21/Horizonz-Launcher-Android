<!-- HORIZONZ_README_PRESERVE -->
<!-- HORIZONZ_README_MOBILE_V2 -->
<div align="center">
<img src="docs/screenshots/horizonz-banner.jpg" alt="HorizonZ Launcher Banner" width="100%" />

# HorizonZ Launcher for Android

**Native Rust Minecraft 26.3 Launcher & Engine for Android (`arm64-v8a` & `armeabi-v7a`)**

<!-- VERSION_BADGE_START -->
[![Release](https://img.shields.io/badge/HorizonZ_Launcher-v1.7-8BE0FF?style=for-the-badge&logo=android&logoColor=0E2430)](https://github.com/Fadiba21/Horizonz-Launcher/releases)
[![Minecraft](https://img.shields.io/badge/Minecraft-26.3_Rust-81C784?style=for-the-badge)](https://github.com/horizon-rust/horizon)
[![Upstream](https://img.shields.io/badge/Vasysik_Horizon-v0.7.6-D0C6F2?style=for-the-badge)](https://github.com/Vasysik/horizon-android)
<!-- VERSION_BADGE_END -->
</div>

<!-- LATEST_VERSION_START -->
> **Latest Release — HorizonZ v1.7**
<!-- LATEST_VERSION_END -->

## Key Features

- **Native Rust Engine (`Minecraft 26.3`)**: Runs directly on Vulkan / OpenGL ES (`wgpu`) without JVM overhead (`arm64-v8a` & `armeabi-v7a`).
- **Trailer Dashboard & Wallpapers**: Auto-plays official YouTube trailers from `@Minecraft` that are under five minutes, without captions and with automatic next-video playback, plus an animated three-dot menu.
- **Flexible RAM Allocation**: A dynamic RAM allocation slider based on the phone's available memory, with built-in **Unrestricted / Android OS Limit** modes.
- **Simple Game & Assets Manager**: Downloads official assets without hanging, includes automatic Smart Repair, and provides a delete-all-assets button.
- **Synchronized Store & Manager**: Install and update Mods (`.hmod`), Resource Packs, Shader Packs, and Modpacks directly from [playhorizon.dev/mods](https://playhorizon.dev/mods).
- **3D Skin Studio**: Preview characters in 3D with 360-degree rotation and walking animations, detect Wide/Slim models, and manage custom skins.
- **Zero-Overhead Gaming**: Background processes and WebView close automatically when you enter the game.

## Launcher Screenshots

### Dashboard
<img src="docs/screenshots/launcher-dashboard.jpg" alt="HorizonZ Dashboard" width="100%" />

### Game & Assets
<img src="docs/screenshots/launcher-versions.jpg" alt="Game & Assets" width="100%" />

### Store
<img src="docs/screenshots/launcher-store.jpg" alt="HorizonZ Store" width="100%" />

### Files & Mods
<img src="docs/screenshots/launcher-mods-manager.jpg" alt="Files & Mods Manager" width="100%" />

### Skin Studio
<img src="docs/screenshots/launcher-skins.jpg" alt="Skin Studio" width="100%" />

### Settings & RAM
<img src="docs/screenshots/launcher-settings.jpg" alt="Settings & RAM Allocation" width="100%" />

## Runtime Components (`BBS/`)

- `BBS/sync_and_merge_bbs.py`: Automatically syncs the runtime from upstream [`Vasysik/horizon-android`](https://github.com/Vasysik/horizon-android) (`v0.7.6`) with active HorizonZ patches.
- `scripts/patch_horizon_direct_boot.py`: Direct-boots Minecraft 26.3, handles RAM allocation and automatic skins, and includes `.hmod` fixes.

## Download & Releases

**What's new in v1.7:** Google AI Studio AI Agent integration (Gemini API with local API-key storage using file permissions `0600` and model selection); shader-pack fixes for `horizon-shaders.toml` schema 1 (WGSL); Native Mods & Extras synchronization; a Store Favorite button layout fix; and an updated YouTube trailer player for trailers under five minutes without WebView blocking.

1. Download the APK matching your device architecture (`arm64-v8a` / `armeabi-v7a`) from [**Releases**](https://github.com/Fadiba21/Horizonz-Launcher/releases).
2. Open the launcher, go to **Game & Assets**, and download the official assets until the process reaches **100%**.
3. Press **PLAY** in the right-hand panel to launch Minecraft 26.3.

## License & Credits

- **Fork Repository**: [Fadiba21/Horizonz-Launcher](https://github.com/Fadiba21/Horizonz-Launcher)
- **Upstream Android Runtime**: [Vasysik/horizon-android](https://github.com/Vasysik/horizon-android) (`v0.7.6`)
- **Core Rust Engine**: [horizon-rust/horizon](https://github.com/horizon-rust/horizon) (`Apache-2.0`)
