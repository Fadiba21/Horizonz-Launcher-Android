<!-- HORIZONZ_README_PRESERVE -->
<!-- HORIZONZ_README_MOBILE_V3 -->
<div align="center">

<img src="docs/screenshots/horizonz-banner.jpg" alt="HorizonZ Launcher Banner" width="100%" />

# 🎮 HorizonZ Launcher Android

**Native Rust Minecraft 26.3 Launcher & Engine for Android (`arm64-v8a` & `armeabi-v7a`)**

<!-- VERSION_BADGE_START -->
[![Release](https://img.shields.io/badge/HorizonZ_Launcher-v3.0-8BE0FF?style=for-the-badge&logo=android&logoColor=0E2430)](https://github.com/Fadiba21/Horizonz-Launcher/releases)
[![Minecraft](https://img.shields.io/badge/Minecraft-26.3_Rust-81C784?style=for-the-badge&logo=rust&logoColor=0E2430)](https://github.com/horizon-rust/horizon)
[![Upstream](https://img.shields.io/badge/Vasysik_Horizon-android_150a414-D0C6F2?style=for-the-badge)](https://github.com/Vasysik/horizon-android)
[![Gemini AI](https://img.shields.io/badge/Google_AI_Studio-Gemini_4_Argon_%7C_3.8_%7C_3.5-FFB74D?style=for-the-badge&logo=googlegemini&logoColor=261600)](https://aistudio.google.com/)
<!-- VERSION_BADGE_END -->

</div>

<!-- LATEST_VERSION_START -->
> 🚀 **Latest Update — HorizonZ v3.0 (`HZ-INGAME-BLACKSCREEN-SYNC-v3.0`, `3.0.0-HorizonZ`, `versionCode 71`)**: Updates the in-game startup and session handoff path (transparent splash overlay, launcher/game lifecycle handling, direct session-file account handoff, and stale empty asset-index cleanup) and adds bidirectional content synchronization across `home`, `profiles/slot-N`, and `.local/horizon-client/26.3` for the Manager, Store, and game lifecycle.
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

### 📦 HorizonZ v3.0 Summary (`versionCode 71` • `3.0.0-HorizonZ`)
- **Configured CI Target ABIs**: [HorizonZ-Launcher-3.0-arm64-v8a.apk](https://github.com/Fadiba21/Horizonz-Launcher/releases) (64-bit ARMv8) and [HorizonZ-Launcher-3.0-armeabi-v7a.apk](https://github.com/Fadiba21/Horizonz-Launcher/releases) (32-bit ARMv7) when built via `.github/workflows/build-apk.yml`.
- **What's New in v3.0 (`HZ-INGAME-BLACKSCREEN-SYNC-v3.0`)**:
  - **Game startup and session handoff (`HorizonActivity.java`, `ZalithMainActivity.kt`, `scripts/patch_horizon_direct_boot.py`, `HorizonNativeBridge.kt` + `BBS/` mirrors)**:
    - Replaces the opaque black splash window with a transparent, non-focusable, non-touchable window and restores decor focus when the splash is dismissed.
    - Removes launcher task finishing and process termination from the game-entry hibernation callback; adds a Rust exit guard set after the game ends, emits `100|Main Menu Ready` only while a game is active, and retains fallback pages while an error or worker is present.
    - Writes direct account-selection fields for the `HORIZON_SESSION_FILE` handoff path and removes stale empty `asset-index.json` markers.
  - **Two-way content synchronization and Manager/Store operations (`HorizonNativeBridge.kt`, `ZalithMainActivity.kt`, `scripts/patch_horizon_direct_boot.py` + `BBS/` mirrors)**:
    - Synchronizes mods, shaders, resource packs, saves, screenshots, modpacks, and related state files across `home`, `profiles/slot-N`, and `.local/horizon-client/26.3` at game launch/return, launcher resume, and Manager or Store refresh.
    - Extends toggle, delete, import, install, and uninstall handling across runtime directories, including bundled-mod lookup, resource-pack validation, original filenames on URI import, and a Modpacks category.
- **What's New in v2.9 (`HZ-REPAIR-v2.8-metadata-v2.9`)**:
  - **`allRuntimeDirs` Scope & Typed Receiver Repair in `HorizonNativeBridge.kt` (+ `BBS/` Mirror)**: Added class-scope `private fun getAllRuntimeDirs(home: File, profileHome: File): List<File>` (lines `1734..1737`) and declared `val allRuntimeDirs: List<File> = getAllRuntimeDirs(home, profileHome)` with explicit `allRuntimeDirs.forEach { dir: File ->` in `syncNativeHorizonAccountAndCatalog` (line `1745`), `resetTouchKeypadLayout` (lines `2217..2218`, previously `2213:9`), `setContentItemEnabled` (lines `2317..2318`, previously `2312:9`), and `recordInstalledStoreItem` (lines `3910..3911`, previously `3904:9`) in `packaging/android/java/dev/horizon/android/bridge/HorizonNativeBridge.kt` and `BBS/runtime-rust/patched_active/packaging/android/java/dev/horizon/android/bridge/HorizonNativeBridge.kt`.
  - **Aligned `v2.9` Metadata (`HZ-REPAIR-v2.8-metadata-v2.9`)**: Aligned `packaging/android/build.gradle` (`versionCode 70`, `versionName '2.9.0-HorizonZ'`), `INSTALLED_LAUNCHER_VERSION = "2.9"`, `HorizonActivity.java` + `BBS/` mirror (`expectedStamp = "2.9-0.7.7-150a414"`), `scripts/patch_horizon_direct_boot.py` + `BBS/` mirror (`User-Agent: HorizonZ-CI/2.9`), `.github/workflows/build-apk.yml` (`HorizonZ-Launcher-2.9-${{ matrix.abi }}.apk`, `horizonz-v2.9.0-build-${{ github.run_number }}`), and `BBS/upstream_state.json`, `BBS/custom_runtime_edits.json`, and `BBS/sync_and_merge_bbs.py` (`horizonz_version: "2.9"`, `launcher_version` / `version: "2.9.0-HorizonZ"`, `repair_request_id: "HZ-REPAIR-v2.8-metadata-v2.9"`). Note that candidate `v2.8` (`HZ-REPAIR-v2.7-8bbb561-allRuntimeDirs-v2.8`) failed static metadata QA and remains `REJECTED / NOT APPLIED / NOT BUILT`.
  - **Historical Compile & Static QA Record (Compilation/Runtime Limitation: `UNTESTED`)**:
    - **`v2.5` (commit `8fa0e81`, CI run `42132294958`)**: `packaging/android/java/dev/horizon/android/ui/ZalithMainActivity.kt:3604:57` failed with `Unresolved reference: scope` on both `arm64-v8a` and `armeabi-v7a`; Rust build passed, Release skipped.
    - **`v2.6` (commit `6cb1b39b67e4c8e3e14bc4c0ed23731465cb15ad`, CI run `42134511803`)**: `ZalithMainActivity.kt:3628:57` failed with `Unresolved reference: scope` alongside `HorizonNativeBridge.kt` `allRuntimeDirs` / `exists` / `add` and downstream type-inference and overload-resolution failures on both ABIs. `val scope = rememberCoroutineScope()` in `CleanAccountsAndSkinsViewport` and its asynchronous Ely.by authentication flow (`Dispatchers.IO`, `HorizonNativeBridge.authenticateElyByAccount`) remain retained in `ZalithMainActivity.kt`.
    - **`v2.7` (commit `8bbb5618b6cf844707f067bb960f7323eda63fd2`, CI run `42136917724`, `horizonz-v27-failed-log.txt`)**: `compileReleaseKotlin` failed on both `arm64-v8a` and `armeabi-v7a` with the exact same 47 diagnostics in `packaging/android/java/dev/horizon/android/bridge/HorizonNativeBridge.kt`, all addressed by the `getAllRuntimeDirs` / `allRuntimeDirs: List<File>` repair in `v2.9`:
      - `Unresolved reference 'allRuntimeDirs'` (3): `2213:9`, `2312:9`, `3904:9`.
      - `Unresolved reference 'exists'` (6): `2318:44`, `2335:46`, `2370:36`, `2374:45`, `2397:45`, `3906:35`.
      - `Unresolved reference 'add'` (3): `2345:38`, `2383:37`, `2415:29`.
      - `Cannot infer type for this parameter. Please specify it explicitly.` (13): `2213:34`, `2312:34`, `2318:54`, `2318:66`, `2335:98`, `2338:52`, `2374:96`, `2377:43`, `2397:96`, `2400:43`, `3904:34`, `3906:45`, `3906:57`.
      - `Overload resolution ambiguity between candidates` (22): `2318:88`, `2335:65`, `2335:77`, `2338:46`, `2343:23`, `2347:53`, `2347:72`, `2374:63`, `2374:75`, `2377:37`, `2378:34`, `2382:23`, `2384:43`, `2384:62`, `2397:63`, `2397:75`, `2400:37`, `2401:34`, `2413:23`, `2417:43`, `2417:62`, `3906:78`:
        - `readText` (2) at `2318:88`, `3906:78`: `fun File.readText(charset: Charset = ...): String`, `fun Reader.readText(): String`, `fun URL.readText(charset: Charset = ...): String`.
        - `readLines` (3) at `2335:65`, `2374:63`, `2397:63`: `fun File.readLines(charset: Charset = ...): List<String>`, `fun Reader.readLines(): List<String>`.
        - `toMutableList` (6) at `2335:77`, `2343:23`, `2374:75`, `2382:23`, `2397:75`, `2413:23`: `fun <T> Array<out T>.toMutableList(): MutableList<T>`, `fun BooleanArray.toMutableList(): MutableList<Boolean>`, `fun ByteArray.toMutableList(): MutableList<Byte>`, `fun CharArray.toMutableList(): MutableList<Char>`, `fun DoubleArray.toMutableList(): MutableList<Double>`, `fun FloatArray.toMutableList(): MutableList<Float>`, `fun IntArray.toMutableList(): MutableList<Int>`, `fun LongArray.toMutableList(): MutableList<Long>`, `fun ShortArray.toMutableList(): MutableList<Short>`, `fun <T> Collection<T>.toMutableList(): MutableList<T>`, `fun <T> Iterable<T>.toMutableList(): MutableList<T>`, `fun <T> Sequence<T>.toMutableList(): MutableList<T>`, `fun CharSequence.toMutableList(): MutableList<Char>`.
        - `map` (3) at `2338:46`, `2377:37`, `2400:37`: `fun <R, T> Result<T>.map(transform: (@ParameterName(...) T) -> R): Result<R>`, `fun <T, R> Array<out T>.map(transform: (T) -> R): List<R>`, `fun <R> BooleanArray.map(transform: (Boolean) -> R): List<R>`, `fun <R> ByteArray.map(transform: (Byte) -> R): List<R>`, `fun <R> CharArray.map(transform: (Char) -> R): List<R>`, `fun <R> DoubleArray.map(transform: (Double) -> R): List<R>`, `fun <R> FloatArray.map(transform: (Float) -> R): List<R>`, `fun <R> IntArray.map(transform: (Int) -> R): List<R>`, `fun <R> LongArray.map(transform: (Long) -> R): List<R>`, `fun <R> ShortArray.map(transform: (Short) -> R): List<R>`, `fun <T, R> Iterable<T>.map(transform: (T) -> R): List<R>`, `fun <K, V, R> Map<out K, V>.map(transform: (Map.Entry<K, V>) -> R): List<R>`, `fun <R> UByteArray.map(transform: (UByte) -> R): List<R>`, `fun <R> UIntArray.map(transform: (UInt) -> R): List<R>`, `fun <R> ULongArray.map(transform: (ULong) -> R): List<R>`, `fun <R> UShortArray.map(transform: (UShort) -> R): List<R>`, `fun <T, R> Sequence<T>.map(transform: (T) -> R): Sequence<R>`, `fun <R> CharSequence.map(transform: (Char) -> R): List<R>`.
        - `startsWith` (2) at `2378:34`, `2401:34`: `fun String.startsWith(prefix: String, ignoreCase: Boolean = ...): Boolean`, `fun CharSequence.startsWith(prefix: CharSequence, ignoreCase: Boolean = ...): Boolean`, `fun File.startsWith(other: String): Boolean`.
        - `joinToString` (3) at `2347:53`, `2384:43`, `2417:43`: `fun <T> Array<out T>.joinToString(separator: CharSequence = ..., prefix: CharSequence = ..., postfix: CharSequence = ..., limit: Int = ..., truncated: CharSequence = ..., transform: ((T) -> CharSequence)? = ...): String`, `fun BooleanArray.joinToString(...): String`, `fun ByteArray.joinToString(...): String`, `fun CharArray.joinToString(...): String`, `fun DoubleArray.joinToString(...): String`, `fun FloatArray.joinToString(...): String`, `fun IntArray.joinToString(...): String`, `fun LongArray.joinToString(...): String`, `fun ShortArray.joinToString(...): String`, `fun <T> Iterable<T>.joinToString(...): String`, `fun <T> Sequence<T>.joinToString(...): String`.
        - `plus` (3) at `2347:72`, `2384:62`, `2417:62`: `fun String?.plus(other: Any?): String`, `fun <T> Array<T>.plus(element: T): Array<T>`, `fun <T> Collection<T>.plus(element: T): List<T>`, `fun <T> Iterable<T>.plus(element: T): List<T>`, `fun <T> Set<T>.plus(element: T): Set<T>`, `fun <T> Sequence<T>.plus(element: T): Sequence<T>`, `fun Char.plus(other: String): String`.
    - **Verification Scope Limitation**: Only static reads, JSON parsing, Python AST parsing, source comparisons, and ZIP integrity checks were performed; Kotlin/Rust compilation and Android runtime execution remain **UNTESTED** (`NOT APPLIED / NOT BUILT`).
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
