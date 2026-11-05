package dev.horizon.android.model

enum class ZalithNavTab(val title: String) {
    HOME("Dashboard"),
    VERSIONS("Game & Assets"),
    STORE("Store"),
    FILES("Files & Mods"),
    ACCOUNTS("Account & Skins"),
    LOGS("System Logs"),
    SETTINGS("Settings")
}

enum class StoreCategory(
    val label: String,
    val headerTitle: String,
    val kindFilter: String,
    val targetFolder: String,
    val badgeLabel: String
) {
    ALL("All", "Horizon Mods — All", "all", "horizon-mods", "ALL"),
    MOD("Mods", "Horizon Mods — Mods", "mod", "horizon-mods", "MOD"),
    NATIVE_MOD("Native Mods", "Horizon Mods — Native Mods", "native-mod", "horizon-native-mods", "NATIVE MOD"),
    MODPACK("Modpacks", "Horizon Mods — Modpacks", "modpack", "horizon-packs", "MODPACK"),
    RESOURCE_PACK("Resource Packs", "Horizon Mods — Resource Packs", "resourcepack", "resourcepacks", "RESOURCE PACK"),
    SHADER_PACK("Shader Packs", "Horizon Mods — Shader Packs", "shader", "shaderpacks", "SHADER PACK")
}

enum class StoreSortOption(val label: String) {
    MOST_DOWNLOADED("Most downloaded"),
    BEST_RATED("Best rated"),
    RECENTLY_UPDATED("Recently updated"),
    NAME_AZ("Name (A–Z)")
}

enum class StoreStatusFilter(val label: String) {
    ALL("All Projects"),
    INSTALLED("Installed Only"),
    UPDATES("Updates Available"),
    OFFICIAL_SIGNED("Official & Signed")
}

enum class StoreInstallState {
    NOT_INSTALLED,
    INSTALLED,
    UPDATE_AVAILABLE
}

data class StoreVersionRelease(
    val id: String,
    val name: String,
    val versionNumber: String,
    val mcVersions: List<String>,
    val kindLabel: String = "MOD",
    val releaseType: String = "release",
    val downloadsCount: Long = 0L,
    val datePublished: String = "Recently",
    val fileName: String = "",
    val downloadUrl: String = "",
    val sizeBytes: Long = 0L,
    val sha256: String = "",
    val signed: Boolean = true
) {
    val primaryMcVersion: String
        get() = mcVersions.firstOrNull() ?: "26.3"

    val formattedDownloads: String
        get() = when {
            downloadsCount >= 1_000_000L -> String.format(java.util.Locale.US, "%.1fM", downloadsCount / 1_000_000.0)
            downloadsCount >= 1_000L -> String.format(java.util.Locale.US, "%.1fk", downloadsCount / 1_000.0)
            downloadsCount > 0L -> downloadsCount.toString()
            else -> "1.2k"
        }
}

data class StoreCatalogItem(
    val id: String,
    val slug: String = "",
    val title: String,
    val summary: String,
    val author: String,
    val version: String,
    val supportedMcVersions: List<String>,
    val downloadUrl: String,
    val fileName: String,
    val sizeBytes: Long,
    val sha256: String = "",
    val category: StoreCategory,
    val kindBadge: String = "MOD",
    val pageUrl: String = "",
    val sourceCodeUrl: String = "",
    val iconUrl: String = "",
    val downloadsCount: Long = 1200L,
    val ratingScore: Float = 4.8f,
    val ratingCount: Int = 12,
    val isSignedOrOfficial: Boolean = true,
    val side: String = "client",
    val license: String = "MIT",
    val displayTags: List<String> = emptyList(),
    val requiredModIds: List<String> = emptyList(),
    val allVersions: List<StoreVersionRelease> = emptyList()
) {
    val isCompatibleWith26_3: Boolean
        get() = supportedMcVersions.any {
            it.contains("26.3") || it.equals("all", ignoreCase = true)
        }

    val versionBadge: String
        get() = if (supportedMcVersions.isEmpty()) "26.3" else supportedMcVersions.first()

    val formattedDownloads: String
        get() = when {
            downloadsCount >= 1_000_000L -> String.format(java.util.Locale.US, "%.1fM downloads", downloadsCount / 1_000_000.0)
            downloadsCount >= 1_000L -> String.format(java.util.Locale.US, "%.1fk downloads", downloadsCount / 1_000.0)
            else -> "$downloadsCount downloads"
        }

    val targetInstallFolder: String
        get() = when (category) {
            StoreCategory.RESOURCE_PACK -> "resourcepacks"
            StoreCategory.SHADER_PACK -> "shaderpacks"
            StoreCategory.MODPACK -> "horizon-packs"
            else -> "horizon-mods"
        }
}

data class StoreSearchPageResult(
    val items: List<StoreCatalogItem>,
    val currentPage: Int,
    val totalPages: Int,
    val totalHits: Int
)

data class WorldSaveDetailInfo(
    val folderName: String,
    val displayName: String,
    val gameMode: String,
    val difficulty: String,
    val playTimeFormatted: String,
    val worldSeed: String,
    val lastPlayedFormatted: String
)

data class ResourcePackDetailInfo(
    val title: String,
    val description: String,
    val packType: String,
    val fileName: String,
    val fileSizeFormatted: String,
    val formatVersion: Int
)

data class GithubReleaseUpdateInfo(
    val latestVersion: String,
    val releaseTitle: String,
    val downloadUrl: String,
    val htmlUrl: String
)

enum class AccountType(val badge: String) {
    OFFLINE("Offline"),
    MICROSOFT("Microsoft"),
    ELY_BY("Ely.by")
}

/**
 * Matches the four touch schemes in `Vasysik/horizon-android` (`docs/android.md`):
 * Joystick/D-pad movement combined with Crosshair or Tap-to-interact aiming.
 */
enum class HorizonTouchScheme(val serdeKey: String, val label: String) {
    JOYSTICK_CROSSHAIR("joystick-crosshair", "Joystick + Crosshair Aiming"),
    JOYSTICK_TAP("joystick-tap", "Joystick + Tap to Interact"),
    DPAD_CROSSHAIR("dpad-crosshair", "D-Pad + Crosshair Aiming"),
    DPAD_TAP("dpad-tap", "D-Pad + Tap to Interact")
}

enum class HorizonRendererMode(val serdeKey: String, val label: String) {
    VULKAN_COMPAT("vulkan-compatibility", "Vulkan Compatibility (Default)"),
    VULKAN("vulkan", "Vulkan (High Performance)"),
    OPENGL("open-gl", "OpenGL ES (Fallback)")
}

enum class HorizonInputHost(val serdeKey: String, val label: String) {
    AUTO("auto", "Auto Detect"),
    TOUCH("touch", "Touch Screen"),
    KEYBOARD_MOUSE("keyboard-mouse", "Keyboard & Mouse"),
    CONTROLLER("controller", "Gamepad / Controller")
}

enum class HorizonOrientation(val serdeKey: String, val label: String) {
    LANDSCAPE("landscape", "Landscape"),
    REVERSE_LANDSCAPE("reverse-landscape", "Reverse Landscape (Flipped)"),
    PORTRAIT("portrait", "Portrait"),
    REVERSE_PORTRAIT("reverse-portrait", "Reverse Portrait"),
    AUTO("auto", "Auto Sensor")
}

enum class HorizonContentOrigin(val serdeKey: String, val label: String) {
    HORIZON("horizon", "Horizon (playhorizon.dev)"),
    MODRINTH("modrinth", "Modrinth")
}

enum class HorizonScreenAspect(val serdeKey: String, val label: String) {
    NATIVE("native", "Native Fullscreen (Device Ratio)"),
    WIDE("wide", "Wide 16:9 Presentation"),
    CLASSIC("classic", "Classic 4:3 Presentation"),
    SQUARE("square", "Square 1:1 Presentation")
}

enum class HorizonControllerNavMode(val serdeKey: String, val label: String) {
    FOCUS("focus", "Focus Highlight (Console D-Pad / Stick)"),
    POINTER("pointer", "Virtual Pointer Cursor (Stick Cursor)")
}

data class ModPackageDetailInfo(
    val modId: String,
    val displayName: String,
    val version: String,
    val description: String,
    val author: String,
    val apiLine: String,
    val side: String,
    val capabilities: List<String>,
    val hasWasmBinary: Boolean,
    val fileName: String,
    val fileSizeFormatted: String
)

enum class HorizonBackgroundMode(val serdeKey: String, val label: String) {
    WALLPAPER("wallpaper", "Static HD Minecraft Wallpaper (16:9)"),
    YOUTUBE_TRAILER("youtube-trailer", "Auto-Play Minecraft YouTube Trailer")
}

data class MinecraftTrailerVideo(
    val videoId: String,
    val title: String,
    val publishedDate: String,
    val watchUrl: String = "https://www.youtube.com/watch?v=$videoId",
    val directMp4Url: String = ""
)

data class SavedCustomSkinItem(
    val id: String,
    val fileName: String,
    val displayName: String,
    val filePath: String,
    val slimModel: Boolean,
    val sourceLabel: String = "Custom Skin",
    val addedAt: Long = System.currentTimeMillis()
)

/**
 * Launcher Theme Palettes:
 * - Default remains `MATERIAL_YOU` ("Monet / Material You (Default)").
 * - Includes `LIQUID_GLASS` ("Liquid Glass (Kyant0 Backdrop)") powered by `Kyant0/AndroidLiquidGlass`.
 */
enum class ZalithThemePalette(val label: String, val primaryHex: Long, val secondaryHex: Long, val surfaceHex: Long) {
    MATERIAL_YOU("Monet / Material You (Default)", 0xFF64B5F6, 0xFF81C784, 0xFF10131A),
    LIQUID_GLASS("Liquid Glass (Kyant0 Backdrop)", 0xFF90E4FF, 0xFFE0F7FA, 0xFF0B1017),
    EMBERMIRE("Embermire Dark", 0xFFFF7043, 0xFFFFB74D, 0xFF12151E),
    HORIZON_RUST("Horizon Rust", 0xFFFF5722, 0xFF26A69A, 0xFF0E1218),
    AMOLED_EMERALD("Emerald AMOLED", 0xFF66BB6A, 0xFFA5D6A7, 0xFF090C10)
}

data class ZalithAccount(
    val id: String,
    val username: String,
    val uuid: String,
    val accountType: AccountType,
    val isActive: Boolean = false,
    val skinPath: String = "",
    val slimSkinModel: Boolean = false,
    val skinSourceLabel: String = "Default (Classic Steve)",
    val capePath: String = "",
    val hideCape: Boolean = false
) {
    val providerQualifiedKey: String
        get() = "${accountType.name.lowercase(java.util.Locale.US)}:${
            if (accountType == AccountType.OFFLINE) username.lowercase(java.util.Locale.US)
            else uuid.lowercase(java.util.Locale.US)
        }"
}

data class SkinDetectionResult(
    val success: Boolean,
    val skinFile: java.io.File?,
    val slimModel: Boolean,
    val sourceLabel: String,
    val message: String,
    val savedFileName: String = ""
)

enum class HorizonAiAgentProvider(val serdeKey: String, val label: String) {
    GEMINI_API("gemini", "Google AI Studio (Gemini API)"),
    LOCAL_SOCKET("local", "Local AI / Control Socket (horizon-control.json)")
}

data class GeminiModelOption(
    val modelId: String,
    val displayName: String,
    val badge: String,
    val description: String
)

/**
 * Clean, canonical list of Google AI Studio / Gemini models with exact 1-to-1 alignment
 * between `displayName` and `modelId` (no duplicate bracket strings or mismatched IDs).
 */
val DEFAULT_GEMINI_MODELS = listOf(
    // === Gemini 4 Series ===
    GeminiModelOption(
        modelId = "gemini-4-argon",
        displayName = "Gemini 4 Argon",
        badge = "GEMINI 4 • ARGON",
        description = "Flagship autonomous Minecraft 26.3 agent with full world control, building, pathfinding & command execution"
    ),
    GeminiModelOption(
        modelId = "gemini-4.0-ultra",
        displayName = "Gemini 4.0 Ultra",
        badge = "GEMINI 4 • ULTRA",
        description = "Deepest reasoning for large builds, redstone automation, and complex multi-step survival tasks"
    ),
    GeminiModelOption(
        modelId = "gemini-4.0-pro",
        displayName = "Gemini 4.0 Pro",
        badge = "GEMINI 4 • PRO",
        description = "High-precision multi-step planning, inventory management, and world manipulation"
    ),
    GeminiModelOption(
        modelId = "gemini-4.0-flash",
        displayName = "Gemini 4.0 Flash",
        badge = "GEMINI 4 • FLASH",
        description = "Low-latency 4.0 agent for rapid in-game actions and real-time combat/navigation"
    ),
    GeminiModelOption(
        modelId = "gemini-4.0-flash-lite",
        displayName = "Gemini 4.0 Flash-Lite",
        badge = "GEMINI 4 • LITE",
        description = "Ultra-lightweight 4.0 model with instant response times and minimal token usage"
    ),

    // === Gemini 3.8 & 3.5 Series ===
    GeminiModelOption(
        modelId = "gemini-3.8-pro",
        displayName = "Gemini 3.8 Pro",
        badge = "GEMINI 3.8 • PRO",
        description = "Tactical survival, world navigation, and command sequence specialist"
    ),
    GeminiModelOption(
        modelId = "gemini-3.8-flash",
        displayName = "Gemini 3.8 Flash",
        badge = "GEMINI 3.8 • FLASH",
        description = "Fast multimodal 3.8 agent for instant command execution and gameplay assistance"
    ),
    GeminiModelOption(
        modelId = "gemini-3.8-flash-lite",
        displayName = "Gemini 3.8 Flash-Lite",
        badge = "GEMINI 3.8 • LITE",
        description = "Lightweight 3.8 model for instant in-game chat and quick actions"
    ),
    GeminiModelOption(
        modelId = "gemini-3.5-pro",
        displayName = "Gemini 3.5 Pro",
        badge = "GEMINI 3.5 • PRO",
        description = "Advanced command-block, structure, and world-editing specialist"
    ),
    GeminiModelOption(
        modelId = "gemini-3.5-flash",
        displayName = "Gemini 3.5 Flash",
        badge = "GEMINI 3.5 • FLASH",
        description = "Balanced speed and accuracy for continuous in-game assistance"
    ),
    GeminiModelOption(
        modelId = "gemini-3.5-flash-lite",
        displayName = "Gemini 3.5 Flash-Lite",
        badge = "GEMINI 3.5 • LITE",
        description = "Ultra-fast 3.5 Flash-Lite model for low-latency chat and control commands"
    ),

    // === Gemini 3.1 & 3.0 Series ===
    GeminiModelOption(
        modelId = "gemini-3.1-pro-preview",
        displayName = "Gemini 3.1 Pro Preview",
        badge = "GEMINI 3.1 • PRO",
        description = "Latest 3.1 Pro preview model for complex reasoning and agentic workflows"
    ),
    GeminiModelOption(
        modelId = "gemini-3.1-flash-lite-preview",
        displayName = "Gemini 3.1 Flash-Lite Preview",
        badge = "GEMINI 3.1 • LITE",
        description = "High-efficiency 3.1 Flash-Lite preview model"
    ),
    GeminiModelOption(
        modelId = "gemini-3.0-pro",
        displayName = "Gemini 3.0 Pro",
        badge = "GEMINI 3.0 • PRO",
        description = "Stable 3.0 Pro reasoning model for Minecraft world planning"
    ),
    GeminiModelOption(
        modelId = "gemini-3.0-flash",
        displayName = "Gemini 3.0 Flash",
        badge = "GEMINI 3.0 • FLASH",
        description = "Responsive 3.0 Flash model for in-game actions"
    ),

    // === Gemini 2.5 & 2.0 Series (Google AI Studio Direct) ===
    GeminiModelOption(
        modelId = "gemini-2.5-pro",
        displayName = "Gemini 2.5 Pro",
        badge = "GEMINI 2.5 • PRO",
        description = "Google AI Studio 2.5 Pro thinking model for deep analysis and multi-step execution"
    ),
    GeminiModelOption(
        modelId = "gemini-2.5-flash",
        displayName = "Gemini 2.5 Flash",
        badge = "GEMINI 2.5 • FLASH",
        description = "Google AI Studio 2.5 Flash high-speed multimodal agent"
    ),
    GeminiModelOption(
        modelId = "gemini-2.5-flash-lite",
        displayName = "Gemini 2.5 Flash-Lite",
        badge = "GEMINI 2.5 • LITE",
        description = "Google AI Studio 2.5 Flash-Lite high-throughput low-latency model"
    ),
    GeminiModelOption(
        modelId = "gemini-2.0-flash",
        displayName = "Gemini 2.0 Flash",
        badge = "GEMINI 2.0 • FLASH",
        description = "Google AI Studio 2.0 Flash general-purpose agent model"
    ),
    GeminiModelOption(
        modelId = "gemini-2.0-flash-lite",
        displayName = "Gemini 2.0 Flash-Lite",
        badge = "GEMINI 2.0 • LITE",
        description = "Google AI Studio 2.0 Flash-Lite fast response model"
    ),

    // === Gemma 3 Series ===
    GeminiModelOption(
        modelId = "gemma-3-27b-it",
        displayName = "Gemma 3 27B Instruct",
        badge = "GEMMA 3 • 27B",
        description = "Open-weights Gemma 3 27B Instruct served via Google AI Studio"
    )
)

data class HorizonConfigState(
    val profile: Int = 0,
    val renderer: HorizonRendererMode = HorizonRendererMode.VULKAN_COMPAT,
    val renderScalePercent: Int = 75,
    val orientation: HorizonOrientation = HorizonOrientation.LANDSCAPE,
    val screenAspect: HorizonScreenAspect = HorizonScreenAspect.NATIVE,
    val screenImmersive: Boolean = true,
    val screenAvoidCutout: Boolean = true,
    val displayRefreshHz: Int = 0,
    val performanceHintsAdpf: Boolean = true,
    val gpuStageProfiling: Boolean = false,
    val inputHost: HorizonInputHost = HorizonInputHost.AUTO,
    val touchScheme: HorizonTouchScheme = HorizonTouchScheme.JOYSTICK_CROSSHAIR,
    val touchSensitivityXPercent: Int = 100,
    val touchSensitivityYPercent: Int = 100,
    val touch360Joystick: Boolean = false,
    val touchSwapHandsButton: Boolean = false,
    val inventoryVirtualMouse: Boolean = true,
    val keypadControls: Boolean = false,
    val sprintToggle: Boolean = false,
    val inventoryTouchControls: Boolean = true,
    val gyroAimingEnabled: Boolean = false,
    val gyroSensitivityPercent: Int = 100,
    val controllerVibration: Boolean = true,
    val controllerNavMode: HorizonControllerNavMode = HorizonControllerNavMode.FOCUS,
    val controllerSensitivityPercent: Int = 100,
    val controllerDeadzonePercent: Int = 15,
    val extraFullbright: Boolean = false,
    val extraDynamicLights: Boolean = false,
    val extraAutoReconnect: Boolean = true,
    val extraMouseTweaks: Boolean = true,
    val extraInventorySort: Boolean = true,
    val recordPerformance: Boolean = false,
    val modSource: HorizonContentOrigin = HorizonContentOrigin.HORIZON,
    val shaderSource: HorizonContentOrigin = HorizonContentOrigin.HORIZON,
    val defaultServer: String = "",
    val proxy: String = "",
    val mcRenderDistance: Int = 8,
    val mcSimulationDistance: Int = 8,
    val mcMaxFps: Int = 120,
    val mcGuiScale: Int = 0,
    val mcFov: Int = 70,
    val mcVsync: Boolean = true,
    val mcFullscreen: Boolean = true,
    val themePalette: ZalithThemePalette = ZalithThemePalette.MATERIAL_YOU,
    val useDynamicColor: Boolean = true,
    val enableSystemLog: Boolean = true,
    val disableAudioSafeMode: Boolean = false,
    val discordRichPresence: Boolean = true,
    val publicOnDeviceAssetDownload: Boolean = true,
    val backgroundMode: HorizonBackgroundMode = HorizonBackgroundMode.WALLPAPER,
    val useYoutubeTrailerWallpaper: Boolean = false,
    val muteTrailerVideo: Boolean = false,
    val ramAllocationMb: Int = 0,
    val aiAgentEnabled: Boolean = false,
    val aiAgentProvider: HorizonAiAgentProvider = HorizonAiAgentProvider.GEMINI_API,
    val geminiModel: String = "gemini-4-argon",
    val aiAgentAutoAssistChat: Boolean = true,
    val aiAgentAutonomousActions: Boolean = true,
    val aiAgentTaskPrompt: String = "Bantu player melakukan apapun di dalam Minecraft 26.3: jalankan command, bangun struktur, tambang blok, navigasi koordinat, atur inventori, dan jawab pertanyaan."
)

val MINECRAFT_SPLASH_QUOTES = listOf(
    "Never dig straight down!",
    "Punching trees since day one.",
    "Creeper? Aww man...",
    "100% Native Rust — Zero JVM lag!",
    "Watch out for Wardens in the Deep Dark.",
    "Always carry a water bucket in the Overworld!",
    "Diamonds are best found deep in Deepslate.",
    "Redstone engineers build computers inside blocks.",
    "Don't look an Enderman in the eyes!",
    "A stack of 64 torches keeps the phantoms away.",
    "Villager trading halls: 1 Emerald for Mending!",
    "Sleeping in the Nether is an explosive idea.",
    "Enchanting Table speaks Standard Galactic Alphabet.",
    "Elytra + Firework Rockets = Infinite Skies.",
    "Beware of Trapped Chests in Desert Temples!",
    "Craft a Shield before your first skeleton duel.",
    "Silk Touch saves the bee nest with all its bees.",
    "Fortune III turns one diamond ore into four!",
    "Netherite floats in lava — your items are safe.",
    "Copper Golems and Trial Chambers await!",
    "Build a portal with 10 Obsidian blocks.",
    "Golden Carrots give the best saturation in the game.",
    "Crouch near Sculk Sensors to walk silently.",
    "Boats on Blue Ice travel faster than minecarts!",
    "Feed a dolphin raw cod to find sunken treasure.",
    "Name a sheep 'jeb_' for a rainbow wool coat!",
    "Name any mob 'Dinnerbone' to flip it upside down.",
    "Cats scare Creepers away from your front door.",
    "Snow Golems leave a trail of snow in cool biomes.",
    "Axolotls grant Regeneration when you help them fight.",
    "Totem of Undying saves you from fatal falls.",
    "Soul Speed III makes Soul Sand valleys a breeze.",
    "Bartering Gold Ingots with Piglins for Ender Pearls.",
    "Conduits give Water Breathing and Night Vision underwater.",
    "Every world seed holds 60 million blocks of adventure.",
    "Minecraft 26.3 running natively in Rust on Android!"
)
