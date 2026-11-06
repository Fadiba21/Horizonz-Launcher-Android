<!-- HORIZONZ_README_PRESERVE -->
<!-- HORIZONZ_README_MOBILE_V3 -->
<div align="center">

<img src="docs/screenshots/horizonz-banner.jpg" alt="HorizonZ Launcher Banner" width="100%" />

# 🎮 HorizonZ Launcher Android

**Native Rust Minecraft 26.3 Launcher & Engine for Android (`arm64-v8a` & `armeabi-v7a`)**

<!-- VERSION_BADGE_START -->
[![Release](https://img.shields.io/badge/HorizonZ_Launcher-v2.9-8BE0FF?style=for-the-badge&logo=android&logoColor=0E2430)](https://github.com/Fadiba21/Horizonz-Launcher/releases)
[![Minecraft](https://img.shields.io/badge/Minecraft-26.3_Rust-81C784?style=for-the-badge&logo=rust&logoColor=0E2430)](https://github.com/horizon-rust/horizon)
[![Upstream](https://img.shields.io/badge/Vasysik_Horizon-android_150a414-D0C6F2?style=for-the-badge)](https://github.com/Vasysik/horizon-android)
[![Gemini AI](https://img.shields.io/badge/Google_AI_Studio-Gemini_4_Argon_%7C_3.8_%7C_3.5-FFB74D?style=for-the-badge&logo=googlegemini&logoColor=261600)](https://aistudio.google.com/)
<!-- VERSION_BADGE_END -->

</div>

<!-- LATEST_VERSION_START -->
> 🚀 **Latest Source Update — HorizonZ v2.9 (`HZ-REPAIR-v2.8-metadata-v2.9`)**: Adds a typed `getAllRuntimeDirs` helper and repairs four runtime-directory call sites associated with the previous Kotlin compiler failures; aligns `2.9.0-HorizonZ` (`versionCode 70`) metadata and retains the v2.7 Ely.by coroutine-scope fix. CI targets both Android ABIs, and release publication is gated on both builds passing.
<!-- LATEST_VERSION_END -->

## ✨ Key Features

- 🦀 **Native Rust Engine (`Minecraft 26.3`)**: Runs on **Vulkan** and **OpenGL ES (`wgpu`)** via `libhorizon_android.so` (`arm64-v8a` & `armeabi-v7a`).
- 🤖 **In-Game AI Agent (`Google AI Studio / Gemini API`)**: Connects to the game's local control endpoint (`horizon-control.json` TCP `port` + `auth <TOKEN>` protocol) and an in-game monochrome `AI` HUD overlay with **21 selectable Gemini & Gemma models** (`Gemini 4 Argon`, `Gemini 3.8`, `Gemini 3.5 Flash-Lite`, `Gemini 2.5`, etc.) and **device-local API key storage (`0600`)**.
- 🎬 **Random Cinematic & Trailer Player**: Plays a curated playlist of **Minecraft Cinematic & Official Trailers** under 5 minutes in **random shuffle** order at 720p, with captions disabled by default and a collapsible 3-dot control menu.
- 🧩 **Two-Way Store & Manager Synchronization**: Browse, download, install, and update **Mods (`.hmod` containing `mod.wasm`)**, **Native Mods (`.hnmod`)**, **Shader Packs (`.hshader`)**, **Resource Packs**, and **Modpacks (`.hpack`)** from the `playhorizon.dev/mods` catalog.
- 🧠 **Configurable RAM Budget**: Configures the engine memory budget (`horizon-memory.txt` & `WGPU_MEMORY_BUDGET_MB`) via a device-RAM-aware slider, defaulting to **Unlimited (OS Limit)**.
- 🎨 **Wardrobe & 3D Skin Studio**: Supports the `schema: 1` `horizon-appearance.json` and `wardrobe/wardrobe.json` contract (`wardrobe/images/<sha256>.png` with 1 MiB PNG validation), live 3D character preview (360° touch rotation & walking animation), **Wide (4px)** / **Slim (3px)** arm model detection, and **Offline**, **Microsoft**, and **Ely.by** (with optional 6-digit 2FA) account profiles.
- 🎨 **Monet / Material You & Custom Dark Themes**: Includes `MATERIAL_YOU` (Default), `EMBERMIRE`, `HORIZON_RUST`, and `AMOLED_EMERALD` themes with a hardware-accelerated navigation rail, and pauses launcher UI timers and `WebView` instances before finishing the launcher activity on game handoff.

## ⚙️ Architecture & Technical Overview

| Technical Component | Specification & How It Works in HorizonZ |
| :--- | :--- |
| **Process & Rendering (`:game`)** | `HorizonActivity` runs in an isolated process (`android:process=":game"`) using `NativeActivity` + `wgpu` (**Vulkan Compatibility**, **Vulkan High-Perf**, or **OpenGL ES**) with Android ADPF thermal hints. |
| **Assets & Worldgen (`26.3`)** | Downloads and verifies SHA-1/SHA-256 checksums for `client.jar`, Mojang CDN asset objects, `26.3` worldgen registries, and 60 touch control textures into `.local/horizon-client/26.3`. |
| **Appearance, Wardrobe & Session Handoff** | Writes `horizon-appearance.json` (`{"schema":1,"skin":...,"cape":...,"slim":...,"hide_cape":...}`) and provider-qualified selections in `wardrobe/wardrobe.json` (`wardrobe/images/<sha256>.png`), and stages read-once `0600` `sessions/session-<id>.json` (`HORIZON_SESSION_FILE`) files with `"user_type": "ely"` for authenticated `Ely.by` sessions without storing passwords or 2FA codes. |
| **Mod & WGSL Shader Pipeline** | Supports executable `.hmod` WebAssembly mods (`mod.wasm`) and `.hnmod` manifests (`horizon-mods.txt` & `horizon-extras.txt`), plus automatic `.hshader` verification (`horizon-shaders.toml` `schema = 1` with `input.color`-compliant `sky.wgsl` and `post.wgsl` stages). |
| **Gemini AI Agent Bridge** | `HorizonZ-GeminiAgentBridge` connects the local `$HORIZON_CONTROL` TCP JSON-file socket (`state`, `inventory`, `looking-at`, `messages`, `chat`, `command`) and the in-game monochrome `AI` command panel to `generativelanguage.googleapis.com` using the local key file `horizon-gemini-secret.json` (`0600`). |
| **Runtime Auto-Sync (`BBS/`)** | `BBS/sync_and_merge_bbs.py` tracks runtime updates from `Vasysik/horizon-android` (`branch: android`, pinned commit `150a414b09a18c900489d9462a7fc035dae744b7`) and reapplies HorizonZ Rust and Android patches during the CI build workflow. |

## 📱 Launcher User Interface

### 🏠 1. Main Dashboard (HD Wallpapers & Random Cinematic Trailer)
<img src="docs/screenshots/launcher-dashboard.jpg" alt="HorizonZ Dashboard" width="100%" />

### ⬇️ 2. Game & Assets Manager (Download, Smart Repair & Asset Storage)
<img src="docs/screenshots/launcher-versions.jpg" alt="HorizonZ Game & Assets" width="100%" />

### 🛒 3. HorizonZ Store (`playhorizon.dev/mods` Registry)
<img src="docs/screenshots/launcher-store.jpg" alt="HorizonZ Store" width="100%" />

### 🧩 4. Files & Mods Manager (Mods, Saves, Resource Packs & Shaders)
<img src="docs/screenshots/launcher-mods-manager.jpg" alt="HorizonZ Files & Mods Manager" width="100%" />

### 👕 5. Account & 3D Skin Studio
<img src="docs/screenshots/launcher-skins.jpg" alt="HorizonZ Account & Skin Studio" width="100%" />

### ✨ 6. Settings & AI Agent Integration (Google AI Studio / Gemini API)
<img src="docs/screenshots/launcher-settings.jpg" alt="HorizonZ Settings & Gemini AI Agent" width="100%" />

## 📥 Source & Release Summary

### 📦 HorizonZ v2.9 Summary (`versionCode 70` • `2.9.0-HorizonZ`)

- **Configured CI target ABIs**: `arm64-v8a` (64-bit ARMv8) and `armeabi-v7a` (32-bit ARMv7). The workflow publishes a release only when both ABI builds pass.

#### What's New in the v2.9 APK (`HZ-REPAIR-v2.8-metadata-v2.9`)

- **Kotlin runtime-directory repair (`HZ-REPAIR-v2.8-metadata-v2.9`):** Add a class-level `getAllRuntimeDirs(home, profileHome)` helper and explicitly type the returned directories as `List<File>` and each `forEach` receiver as `File` in the runtime-sync, keypad-reset, content-toggle, and Store-recording code. This targets the previous 47 Kotlin compiler diagnostics per ABI, including the unresolved `allRuntimeDirs` references and resulting `exists`, `add`, type-inference, and overload errors; the same changes are mirrored in `BBS/`.
- **Version and retained fixes:** Align launcher, runtime-stamp, and CI/BBS metadata with `2.9.0-HorizonZ` (`versionCode 70`) while retaining the v2.7 `CleanAccountsAndSkinsViewport` coroutine-scope fix.

### Previous Releases

- **What's New in v2.7 (`HZ-REPAIR-v2.5-8fa0e81-scope-v2.7`, `versionCode 68` • `2.7.0-HorizonZ`, commit `8bbb5618b6cf844707f067bb960f7323eda63fd2`)**:
  - **Lexical Coroutine Scope Fix in `CleanAccountsAndSkinsViewport` (`ZalithMainActivity.kt`)**: Added `val scope = rememberCoroutineScope()` in `CleanAccountsAndSkinsViewport` so `scope.launch` in the Ely.by authentication button resolves to the composable's lifecycle-bound `CoroutineScope`, and aligned `v2.7` (`2.7.0-HorizonZ`, `versionCode 68`) metadata.
- **What's New in v2.6**:
  - **Removed Liquid Glass Theme & Optimized Launcher UI**: Removed `LiquidGlassBackdrop.kt` and `LIQUID_GLASS` shader overhead, replaced the sidebar rail with a fast hardware-accelerated Compose rail, debounced slider config writes (180ms) with immediate flush on activity stop/launch, and cached SHA-256 skin digests.
  - **Two-Way Launcher ↔ In-Game Settings & Content Synchronization**: Mirrors `options.txt` (including `fov` and `gamma` for Fullbright), `horizon-extras.txt`, `horizon-mods.txt`, `hmod_list.json`, `horizon-shaders.txt`, `horizon-keypad.json`, `horizon-touch.txt`, `horizon-controller.txt`, `horizon-memory.txt`, and `horizon-appearance.json` across `home`, `profileHome`, and `.local/horizon-client/26.3`, and reads back in-game changes on launcher resume.
  - **Animated In-Game AI Agent Popup & Lower In-Game Overhead**: Upgraded the floating `✦ AI` pill and command panel in `HorizonActivity.java` with spring entrance/exit animations (`OvershootInterpolator`), an animated pulsing status orb and active pulse bar, 1-tap Quick Action chips (`⚡ Status & Pos`, `🏠 Build Shelter`, `⛏️ Mine Wood`, `☀️ Day & Clear`), cached `$HORIZON_CONTROL` socket metadata, and tail-only `RandomAccessFile` log polling.
- **What's New in v2.5 (`QA-HZ-UPSTREAM-c2665c8-150a414-v25-1`)**:
  - **Ely.by Session Discriminator Fix (`"user_type": "ely"`)**: Updated `HorizonNativeBridge.prepareReadOnceSessionFile` in `packaging/android/java/dev/horizon/android/bridge/HorizonNativeBridge.kt` (and its identical active mirror in `BBS/runtime-rust/patched_active/.../HorizonNativeBridge.kt`) to write `put("user_type", "ely")` instead of `"ely_by"`, matching `AuthSession::parse` in `crates/horizon-client-render/src/auth.rs` at pinned commit `150a414b09a18c900489d9462a7fc035dae744b7` (`t != "msa" && t != "ely"` / `ely: field("user_type") == Some("ely")`).
  - **Aligned v2.5 Version & BBS Metadata**: Bumped `packaging/android/build.gradle` (`versionCode 66`, `versionName '2.5.0-HorizonZ'`), `INSTALLED_LAUNCHER_VERSION = "2.5"`, `HorizonActivity.java` (`expectedStamp = "2.5-0.7.7-150a414"`), `scripts/patch_horizon_direct_boot.py` (and `BBS/` mirror), `.github/workflows/build-apk.yml`, `BBS/upstream_state.json`, `BBS/custom_runtime_edits.json`, and `BBS/sync_and_merge_bbs.py` to `2.5` / `2.5.0-HorizonZ` (`qa_request_id: "QA-HZ-UPSTREAM-c2665c8-150a414-v25-1"` over `request_id: "HZ-UPSTREAM-c2665c8-150a414"`), keeping historical cached `BBS/runtime-rust/upstream_clean/` hashes explicitly marked as historical `125c672` snapshots without inventing a published upstream release tag.
- **Retained Changes from v2.4 & v2.3 (`QA-HZ-UPSTREAM-c2665c8-150a414-v24-1` / `HZ-UPSTREAM-c2665c8-150a414`)**:
  - **Kotlin Call & Property Fixes (`v2.4`)**: Retains `config.touchScheme.serdeKey` in `startHorizonGameActivity`, aligned `authenticateElyByAccount` named arguments (`usernameOrEmail`, `passwordRaw`, `totpCodeRaw`) and `Pair<Boolean, ZalithAccount?>` destructuring in `ZalithMainActivity.kt` with fail-closed validation, and `Int`/`IntRange` parameters on the Horizontal (`X`) and Vertical (`Y`) Touch Sensitivity `ZalithSettingSliderCard` calls. Note that in upstream `Vasysik/horizon-android` (`150a414`), touch control scheme and sensitivity are configured in-game under `Options > Controls > Touch Controls`; launcher settings persist these preferences locally in `zalith-config.json`, `android-launcher.json`, and `horizon-touch.txt`.
  - **Upstream `Vasysik/horizon-android` Integration (`c2665c8` → `150a414`)**: Retains pinned commit `150a414b09a18c900489d9462a7fc035dae744b7` (`branch: android`), `horizon-appearance.json` (`schema: 1`) and `wardrobe/wardrobe.json` (`wardrobe/images/<sha256>.png`) staging, and `AccountType.ELY_BY` sign-in UI with optional 6-digit 2FA without storing passwords or 2FA codes on disk.

### 🚀 Quick Start Steps
1. **Build or Install the APK**: Build or download the APK matching your device ABI (`arm64-v8a` or `armeabi-v7a`) from [**Releases**](https://github.com/Fadiba21/Horizonz-Launcher/releases).
2. **Download 26.3 Assets**: Open the **Game & Assets** tab and download the official Minecraft 26.3 assets until progress reaches **100%**.
3. **Configure AI Agent (Optional)**: Open **Settings > AI Agent**, paste your API key from [Google AI Studio](https://aistudio.google.com/apikey) (stored locally on your device), select a model (`Gemini 4 Argon`, `Gemini 3.8`, `Gemini 3.5 Flash-Lite`, etc.), and tap **Test Gemini Connection**.
4. **Launch & Play**: Tap **PLAY** on the Dashboard to start Minecraft 26.3; tap the **`AI`** button at the top-left of the in-game screen or type `@ai <message>` in chat at any time to use the AI assistant.

## 📜 License & Credits

- 🔗 **Fork Repository**: [Fadiba21/Horizonz-Launcher](https://github.com/Fadiba21/Horizonz-Launcher)
- 📱 **Upstream Android Runtime**: [Vasysik/horizon-android](https://github.com/Vasysik/horizon-android) (`branch: android`, pinned commit `150a414b09a18c900489d9462a7fc035dae744b7`)
- 🦀 **Core Rust Engine**: [horizon-rust/horizon](https://github.com/horizon-rust/horizon) (`Apache-2.0`)
