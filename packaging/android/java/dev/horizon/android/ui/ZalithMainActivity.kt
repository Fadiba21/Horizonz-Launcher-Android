@file:OptIn(ExperimentalMaterial3Api::class)

package dev.horizon.android.ui

import android.Manifest
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.compose.ui.viewinterop.AndroidView
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import dev.horizon.android.R
import dev.horizon.android.bridge.*
import dev.horizon.android.model.*
import dev.horizon.android.ui.theme.HorizonZalithTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

private val DASHBOARD_WALLPAPERS = listOf(
    R.drawable.mc_wall_01,
    R.drawable.mc_wall_02,
    R.drawable.mc_wall_03,
    R.drawable.mc_wall_04,
    R.drawable.mc_wall_05,
    R.drawable.mc_wall_06,
    R.drawable.mc_wall_07,
    R.drawable.mc_wall_08,
    R.drawable.mc_wall_09,
    R.drawable.mc_wall_10,
    R.drawable.mc_wall_11,
    R.drawable.mc_wall_12,
    R.drawable.mc_wall_13,
    R.drawable.mc_wall_14,
    R.drawable.mc_wall_15,
    R.drawable.mc_wall_16,
    R.drawable.mc_wall_17,
    R.drawable.mc_wall_18
)

data class HorizonDynamicPalette(
    val rootBg: Color = Color(0xFF14181B),
    val cardSlateBg: Color = Color(0xFF22282C),
    val cardRowBg: Color = Color(0xFF2A3136),
    val subRailCapsuleActive: Color = Color(0xFF394952),
    val skyBlueAccent: Color = Color(0xFF8BE0FF),
    val skyBlueDarkText: Color = Color(0xFF0E2430),
    val lavenderBadgeBg: Color = Color(0xFFD0C6F2),
    val lavenderBadgeText: Color = Color(0xFF231D38),
    val mutedSubtitleText: Color = Color(0xFF9EA8B0)
)

private var activeUiPalette by mutableStateOf(HorizonDynamicPalette())

private val LauncherRootBg: Color get() = activeUiPalette.rootBg
private val CardSlateBg: Color get() = activeUiPalette.cardSlateBg
private val CardRowBg: Color get() = activeUiPalette.cardRowBg
private val SubRailCapsuleActive: Color get() = activeUiPalette.subRailCapsuleActive
private val SkyBlueAccent: Color get() = activeUiPalette.skyBlueAccent
private val SkyBlueDarkText: Color get() = activeUiPalette.skyBlueDarkText
private val LavenderBadgeBg: Color get() = activeUiPalette.lavenderBadgeBg
private val LavenderBadgeText: Color get() = activeUiPalette.lavenderBadgeText
private val MutedSubtitleText: Color get() = activeUiPalette.mutedSubtitleText

@Composable
private fun SyncDynamicLauncherPalette(palette: ZalithThemePalette, useDynamicColor: Boolean) {
    val scheme = MaterialTheme.colorScheme
    activeUiPalette = remember(palette, useDynamicColor, scheme.primary, scheme.background) {
        if (useDynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            HorizonDynamicPalette(
                rootBg = Color(0xFF121619),
                cardSlateBg = Color(0xFF1E2429),
                cardRowBg = Color(0xFF262E34),
                subRailCapsuleActive = scheme.primary.copy(alpha = 0.28f),
                skyBlueAccent = scheme.primary,
                skyBlueDarkText = Color(0xFF0A1922),
                lavenderBadgeBg = scheme.secondaryContainer,
                lavenderBadgeText = scheme.onSecondaryContainer,
                mutedSubtitleText = Color(0xFF9EA8B0)
            )
        } else {
            when (palette) {
                ZalithThemePalette.MATERIAL_YOU -> HorizonDynamicPalette(
                    rootBg = Color(0xFF14181B),
                    cardSlateBg = Color(0xFF22282C),
                    cardRowBg = Color(0xFF2A3136),
                    subRailCapsuleActive = Color(0xFF394952),
                    skyBlueAccent = Color(0xFF8BE0FF),
                    skyBlueDarkText = Color(0xFF0E2430),
                    lavenderBadgeBg = Color(0xFFD0C6F2),
                    lavenderBadgeText = Color(0xFF231D38),
                    mutedSubtitleText = Color(0xFF9EA8B0)
                )
                ZalithThemePalette.EMBERMIRE -> HorizonDynamicPalette(
                    rootBg = Color(0xFF161215),
                    cardSlateBg = Color(0xFF261E22),
                    cardRowBg = Color(0xFF32272C),
                    subRailCapsuleActive = Color(0xFF4E342E),
                    skyBlueAccent = Color(0xFFFF8A65),
                    skyBlueDarkText = Color(0xFF260E07),
                    lavenderBadgeBg = Color(0xFFFFCCBC),
                    lavenderBadgeText = Color(0xFF3E170B),
                    mutedSubtitleText = Color(0xFFB5A19B)
                )
                ZalithThemePalette.HORIZON_RUST -> HorizonDynamicPalette(
                    rootBg = Color(0xFF11151C),
                    cardSlateBg = Color(0xFF1B222C),
                    cardRowBg = Color(0xFF242D3A),
                    subRailCapsuleActive = Color(0xFF4A2C22),
                    skyBlueAccent = Color(0xFFFF7043),
                    skyBlueDarkText = Color(0xFF210B04),
                    lavenderBadgeBg = Color(0xFFB2DFDB),
                    lavenderBadgeText = Color(0xFF00251A),
                    mutedSubtitleText = Color(0xFF98A6B8)
                )
                ZalithThemePalette.AMOLED_EMERALD -> HorizonDynamicPalette(
                    rootBg = Color(0xFF070A08),
                    cardSlateBg = Color(0xFF121A15),
                    cardRowBg = Color(0xFF1A261E),
                    subRailCapsuleActive = Color(0xFF24422E),
                    skyBlueAccent = Color(0xFF69F0AE),
                    skyBlueDarkText = Color(0xFF052412),
                    lavenderBadgeBg = Color(0xFFC8E6C9),
                    lavenderBadgeText = Color(0xFF0D2E14),
                    mutedSubtitleText = Color(0xFF8FA896)
                )
            }
        }
    }
}

enum class SettingsSubCategory(val label: String, val icon: ImageVector) {
    RENDERER("Renderer", Icons.Default.DisplaySettings),
    GAME("Game", Icons.Default.RocketLaunch),
    AI_AGENT("AI Agent", Icons.Default.AutoAwesome),
    CONTROLS("Controls", Icons.Default.Games),
    GAMEPAD("Gamepad", Icons.Default.SportsEsports),
    LAUNCHER("Launcher", Icons.Default.ViewInAr)
}

enum class AccountSubCategory(val label: String, val icon: ImageVector) {
    SKIN_STUDIO("Skin Studio", Icons.Default.Checkroom),
    PROFILES("Accounts", Icons.Default.People)
}

class ZalithMainActivity : ComponentActivity() {
    companion object {
        @Volatile
        private var hasCheckedUpdateThisSession = false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        HorizonNativeBridge.ensureHomeDirectories(this)
        HorizonNativeBridge.syncAndRepairModsStorage(this, 0)
        HorizonNativeBridge.syncAndRepairShadersStorage(this, 0)

        if (Build.VERSION.SDK_INT >= 33) {
            try {
                requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 101)
            } catch (ignored: Exception) {
            }
        }

        setContent {
            val context = LocalContext.current
            val lifecycleOwner = LocalLifecycleOwner.current
            val scope = rememberCoroutineScope()
            var config by remember { mutableStateOf<HorizonConfigState>(HorizonNativeBridge.loadSavedConfig(context)) }
            var accounts by remember { mutableStateOf<List<ZalithAccount>>(HorizonNativeBridge.loadSavedAccounts(context)) }
            val activeAccount = accounts.firstOrNull { it.isActive } ?: accounts.first()

            var isLauncherHibernated by remember { mutableStateOf(false) }
            var isLaunchingSplash by remember { mutableStateOf(false) }
            var isGameProcessSleeping by remember { mutableStateOf(false) }
            var releaseUpdateInfo by remember { mutableStateOf<GithubReleaseUpdateInfo?>(null) }
            var showUpdateBanner by remember { mutableStateOf(false) }

            LaunchedEffect(Unit) {
                if (!hasCheckedUpdateThisSession) {
                    hasCheckedUpdateThisSession = true
                    val found = withContext(Dispatchers.IO) {
                        HorizonNativeBridge.checkGithubLatestReleaseUpdate()
                    }
                    if (found != null) {
                        releaseUpdateInfo = found
                        delay(350L)
                        showUpdateBanner = true
                    }
                }
            }

            DisposableEffect(lifecycleOwner) {
                val observer = LifecycleEventObserver { _, event ->
                    when (event) {
                        Lifecycle.Event.ON_START, Lifecycle.Event.ON_RESUME -> {
                            isLauncherHibernated = false
                            isLaunchingSplash = false
                            isGameProcessSleeping = false
                            scope.launch(Dispatchers.IO) {
                                val loadedCfg = HorizonNativeBridge.loadSavedConfig(context)
                                HorizonNativeBridge.syncAllContentDirectoriesTwoWay(context, loadedCfg.profile)
                                val loadedAcc = HorizonNativeBridge.loadSavedAccounts(context)
                                withContext(Dispatchers.Main) {
                                    config = loadedCfg
                                    accounts = loadedAcc
                                }
                            }
                        }
                        Lifecycle.Event.ON_STOP -> {
                            val cfgSnap = config
                            val accSnap = activeAccount
                            scope.launch(Dispatchers.IO) {
                                HorizonNativeBridge.saveConfig(context, cfgSnap, accSnap)
                            }
                            if (isLauncherHibernated || isLaunchingSplash) {
                                isGameProcessSleeping = true
                                HorizonNativeBridge.releaseMemoryForGameLaunch(context)
                            }
                        }
                        else -> {}
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
            }

            // Debounced background config persistence so slider drags run at full frame rate without flooding disk I/O
            LaunchedEffect(config, activeAccount.id) {
                delay(180L)
                val cfgSnap = config
                val accSnap = activeAccount
                withContext(Dispatchers.IO) {
                    HorizonNativeBridge.saveConfig(context, cfgSnap, accSnap)
                }
            }

            HorizonZalithTheme(
                palette = config.themePalette,
                useDynamicColor = config.useDynamicColor
            ) {
                SyncDynamicLauncherPalette(
                    palette = config.themePalette,
                    useDynamicColor = config.useDynamicColor
                )
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = LauncherRootBg
                ) {
                    if (isGameProcessSleeping) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black)
                        )
                        return@Surface
                    }
                    Box(modifier = Modifier.fillMaxSize()) {
                        ZalithCleanLauncherScreen(
                            config = config,
                            accounts = accounts,
                            activeAccount = activeAccount,
                            isHibernated = isGameProcessSleeping || isLaunchingSplash,
                            onConfigChange = { updated ->
                                config = updated
                            },
                            onAccountsChange = { updatedList ->
                                accounts = updatedList
                                val cfgSnapshot = config
                                scope.launch(Dispatchers.IO) {
                                    HorizonNativeBridge.saveAccounts(context, updatedList, cfgSnapshot)
                                }
                            },
                            onCheckForUpdatesManual = {
                                scope.launch {
                                    Toast.makeText(context, "Checking github.com/Fadiba21/HorizonZ-Launcher-Android/releases...", Toast.LENGTH_SHORT).show()
                                    val found = withContext(Dispatchers.IO) {
                                        HorizonNativeBridge.checkGithubLatestReleaseUpdate()
                                    }
                                    if (found != null) {
                                        releaseUpdateInfo = found
                                        showUpdateBanner = true
                                    } else {
                                        Toast.makeText(context, "HorizonZ v${HorizonNativeBridge.INSTALLED_LAUNCHER_VERSION} sudah versi terbaru!", Toast.LENGTH_SHORT).show()
                                        try {
                                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(HorizonNativeBridge.GITHUB_FORK_RELEASES_URL)))
                                        } catch (ignored: Exception) {
                                        }
                                    }
                                }
                            },
                            onEnterGameHibernation = {
                                if (!isLaunchingSplash) {
                                    isLaunchingSplash = true
                                    val cfgSnapshot = config
                                    val accSnapshot = activeAccount
                                    scope.launch {
                                        withContext(Dispatchers.IO) {
                                            HorizonNativeBridge.syncAllContentDirectoriesTwoWay(context, cfgSnapshot.profile)
                                            HorizonNativeBridge.prepareNativeGameLaunch(context, cfgSnapshot, accSnapshot)
                                        }
                                        delay(120L)
                                        isLauncherHibernated = true
                                        isGameProcessSleeping = true
                                        HorizonNativeBridge.startHorizonGameActivity(context, cfgSnapshot, accSnapshot)
                                        // Wait until :game (HorizonActivity) has established its foreground window and surface,
                                        // then release launcher caches and finish ZalithMainActivity so no duplicate UI process runs!
                                        delay(1800L)
                                        withContext(Dispatchers.IO) {
                                            HorizonNativeBridge.releaseMemoryForGameLaunch(context)
                                        }
                                        runCatching { finish() }
                                    }
                                }
                            }
                        )

                        AnimatedVisibility(
                            visible = showUpdateBanner && releaseUpdateInfo != null && !isLauncherHibernated && !isLaunchingSplash,
                            enter = slideInVertically(
                                initialOffsetY = { fullHeight -> -fullHeight - 60 },
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioLowBouncy,
                                    stiffness = Spring.StiffnessMediumLow
                                )
                            ) + fadeIn(tween(260)),
                            exit = slideOutVertically(
                                targetOffsetY = { fullHeight -> -fullHeight - 60 },
                                animationSpec = tween(260, easing = FastOutLinearInEasing)
                            ) + fadeOut(tween(200)),
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .windowInsetsPadding(WindowInsets.safeDrawing)
                                .padding(top = 8.dp, start = 24.dp, end = 24.dp)
                        ) {
                            releaseUpdateInfo?.let { info ->
                                InAppUpdateBannerCard(
                                    info = info,
                                    onUpdateClick = {
                                        showUpdateBanner = false
                                        try {
                                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(info.downloadUrl))
                                            context.startActivity(intent)
                                        } catch (ignored: Exception) {
                                        }
                                    },
                                    onLaterClick = {
                                        showUpdateBanner = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

fun Modifier.animatedPressScale(
    enabled: Boolean = true,
    pressedScale: Float = 0.96f,
    onClick: () -> Unit
): Modifier = composed {
    val currentOnClick by rememberUpdatedState(onClick)
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) pressedScale else 1f,
        animationSpec = tween(durationMillis = 120, easing = FastOutSlowInEasing),
        label = "PressScale"
    )
    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .clickable(
            interactionSource = interactionSource,
            indication = null,
            enabled = enabled,
            onClick = { currentOnClick() }
        )
}

/**
 * Pure Black Minecraft Java Edition Style Loading Splash Screen ("HORIZONZ")
 * Displayed immediately when pressing PLAY and seamlessly continued by HorizonActivity's native overlay!
 */
@Composable
fun HorizonZBlackSplashOverlay() {
    var progress by remember { mutableFloatStateOf(0.16f) }
    LaunchedEffect(Unit) {
        while (progress < 0.92f) {
            delay(28L)
            progress = (progress + 0.022f).coerceAtMost(0.92f)
        }
    }

    val glyphH = arrayOf(
        intArrayOf(1,1,0,0,0,0,1,1), intArrayOf(1,1,0,0,0,0,1,1), intArrayOf(1,1,0,0,0,0,1,1),
        intArrayOf(1,1,1,1,1,1,1,1), intArrayOf(1,1,1,1,1,1,1,1), intArrayOf(1,1,0,0,0,0,1,1),
        intArrayOf(1,1,0,0,0,0,1,1), intArrayOf(1,1,0,0,0,0,1,1)
    )
    // Iconic 8x8 Minecraft Creeper Face as the letter 'O'!
    val glyphCreeperO = arrayOf(
        intArrayOf(1,1,3,1,1,3,1,1), intArrayOf(1,3,1,1,3,1,1,1), intArrayOf(1,2,2,1,1,2,2,1),
        intArrayOf(1,2,2,1,1,2,2,1), intArrayOf(1,1,1,2,2,1,1,1), intArrayOf(1,1,2,2,2,2,1,1),
        intArrayOf(1,1,2,2,2,2,1,1), intArrayOf(1,1,2,1,1,2,1,1)
    )
    val glyphR = arrayOf(
        intArrayOf(1,1,1,1,1,1,1,0), intArrayOf(1,1,0,0,0,0,1,1), intArrayOf(1,1,0,0,0,0,1,1),
        intArrayOf(1,1,1,1,1,1,1,0), intArrayOf(1,1,1,1,1,0,0,0), intArrayOf(1,1,0,0,1,1,0,0),
        intArrayOf(1,1,0,0,0,1,1,0), intArrayOf(1,1,0,0,0,0,1,1)
    )
    val glyphI = arrayOf(
        intArrayOf(1,1,1,1,1,1,1,1), intArrayOf(0,0,0,1,1,0,0,0), intArrayOf(0,0,0,1,1,0,0,0),
        intArrayOf(0,0,0,1,1,0,0,0), intArrayOf(0,0,0,1,1,0,0,0), intArrayOf(0,0,0,1,1,0,0,0),
        intArrayOf(0,0,0,1,1,0,0,0), intArrayOf(1,1,1,1,1,1,1,1)
    )
    val glyphZ = arrayOf(
        intArrayOf(1,1,1,1,1,1,1,1), intArrayOf(0,0,0,0,0,1,1,1), intArrayOf(0,0,0,0,1,1,1,0),
        intArrayOf(0,0,0,1,1,1,0,0), intArrayOf(0,0,1,1,1,0,0,0), intArrayOf(0,1,1,1,0,0,0,0),
        intArrayOf(1,1,1,0,0,0,0,0), intArrayOf(1,1,1,1,1,1,1,1)
    )
    val glyphO = arrayOf(
        intArrayOf(0,1,1,1,1,1,1,0), intArrayOf(1,1,1,0,0,1,1,1), intArrayOf(1,1,0,0,0,0,1,1),
        intArrayOf(1,1,0,0,0,0,1,1), intArrayOf(1,1,0,0,0,0,1,1), intArrayOf(1,1,0,0,0,0,1,1),
        intArrayOf(1,1,1,0,0,1,1,1), intArrayOf(0,1,1,1,1,1,1,0)
    )
    val glyphN = arrayOf(
        intArrayOf(1,1,0,0,0,0,1,1), intArrayOf(1,1,1,0,0,0,1,1), intArrayOf(1,1,1,1,0,0,1,1),
        intArrayOf(1,1,0,1,1,0,1,1), intArrayOf(1,1,0,0,1,1,1,1), intArrayOf(1,1,0,0,0,1,1,1),
        intArrayOf(1,1,0,0,0,0,1,1), intArrayOf(1,1,0,0,0,0,1,1)
    )
    val word = remember { arrayOf(glyphH, glyphCreeperO, glyphR, glyphI, glyphZ, glyphO, glyphN, glyphZ) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val letterCols = 8
            val letterRows = 8
            val gapCols = 1
            val totalCols = word.size * letterCols + (word.size - 1) * gapCols
            val pxSize = (w * 0.50f / totalCols).coerceIn(4f, 6.2.dp.toPx())
            val logoW = totalCols * pxSize
            val logoH = letterRows * pxSize
            val startX = (w - logoW) / 2f
            val startY = (h * 0.41f) - (logoH / 2f)
            val depthOff = (pxSize * 0.48f).coerceAtLeast(2f)

            // Pass 1: 3D Blocky Bottom Extrusion Shadow
            for (l in word.indices) {
                val glyph = word[l]
                val lx = startX + l * (letterCols + gapCols) * pxSize
                val shadowColor = when (l) {
                    1 -> Color(0xFF1B4D14)
                    word.lastIndex -> Color(0xFF1D4E63)
                    else -> Color(0xFF2A3238)
                }
                for (r in 0 until letterRows) {
                    for (c in 0 until letterCols) {
                        if (glyph[r][c] != 0) {
                            drawRect(
                                color = shadowColor,
                                topLeft = Offset(lx + c * pxSize + depthOff * 0.6f, startY + r * pxSize + depthOff),
                                size = Size(pxSize + 0.5f, pxSize + 0.5f)
                            )
                        }
                    }
                }
            }

            // Pass 2: Foreground Block Letters + Creeper Face 'O'
            for (l in word.indices) {
                val glyph = word[l]
                val lx = startX + l * (letterCols + gapCols) * pxSize
                for (r in 0 until letterRows) {
                    for (c in 0 until letterCols) {
                        val cell = glyph[r][c]
                        if (cell == 0) continue
                        val blockColor = when {
                            l == 1 -> when (cell) {
                                2 -> Color(0xFF111712)
                                3 -> Color(0xFF72E052)
                                else -> if (r == 0) Color(0xFF6CE04E) else Color(0xFF49AC30)
                            }
                            l == word.lastIndex -> if (r == 0) Color(0xFFC2F0FF) else SkyBlueAccent
                            else -> if (r == 0) Color.White else if (r >= 6) Color(0xFFD0D8DE) else Color(0xFFF2F6F8)
                        }
                        drawRect(
                            color = blockColor,
                            topLeft = Offset(lx + c * pxSize, startY + r * pxSize),
                            size = Size(pxSize + 0.5f, pxSize + 0.5f)
                        )
                    }
                }
            }

            // Classic Minecraft Java Progress Bar
            val barW = (w * 0.54f).coerceAtMost(400.dp.toPx())
            val barH = 14.dp.toPx()
            val barLeft = (w - barW) / 2f
            val barTop = h * 0.72f
            val strokeW = 1.8.dp.toPx()
            val pad = 2.8.dp.toPx()

            drawRect(
                color = Color.White,
                topLeft = Offset(barLeft, barTop),
                size = Size(barW, barH),
                style = Stroke(width = strokeW)
            )
            val fillW = ((barW - pad * 2f) * progress).coerceAtLeast(0f)
            if (fillW > 0f) {
                drawRect(
                    color = Color.White,
                    topLeft = Offset(barLeft + pad, barTop + pad),
                    size = Size(fillW, barH - pad * 2f)
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 34.dp),
            verticalArrangement = Arrangement.Bottom,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Initializing HorizonZ Rust Engine (${(progress * 100).toInt()}%)",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFFD5DEE5)
            )
        }
    }
}

/**
 * Custom Canvas Minecraft Grass Block Icon (for Dashboard & item badges).
 */
@Composable
fun MinecraftGrassCubeIcon(size: Dp = 36.dp) {
    Canvas(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(9.dp))
    ) {
        val w = this.size.width
        val h = this.size.height
        drawRect(color = Color(0xFF79553A), size = Size(w, h))
        drawRect(color = Color(0xFF569E3E), size = Size(w, h * 0.36f))
        drawRect(color = Color(0xFF74C454), size = Size(w, h * 0.12f))
        val step = w / 6f
        for (i in 0..5) {
            val fringeH = if (i % 2 == 0) h * 0.48f else h * 0.38f
            drawRect(
                color = Color(0xFF569E3E),
                topLeft = Offset(i * step, 0f),
                size = Size(step + 1f, fringeH)
            )
        }
        drawRect(color = Color(0xFF5E402A), topLeft = Offset(w * 0.2f, h * 0.62f), size = Size(step, step))
        drawRect(color = Color(0xFF91684A), topLeft = Offset(w * 0.6f, h * 0.55f), size = Size(step, step))
        drawRect(color = Color(0xFF5E402A), topLeft = Offset(w * 0.45f, h * 0.78f), size = Size(step, step))
    }
}

/**
 * Pixel-Art "RUST" Badge drawn in blocky 8-bit Minecraft Pixel Typography next to "Minecraft" on the Dashboard!
 */
@Composable
fun MinecraftPixelRustBadge() {
    val glyphR = arrayOf(
        intArrayOf(1,1,1,1,0),
        intArrayOf(1,0,0,0,1),
        intArrayOf(1,0,0,0,1),
        intArrayOf(1,1,1,1,0),
        intArrayOf(1,0,1,0,0),
        intArrayOf(1,0,0,1,0),
        intArrayOf(1,0,0,0,1)
    )
    val glyphU = arrayOf(
        intArrayOf(1,0,0,0,1),
        intArrayOf(1,0,0,0,1),
        intArrayOf(1,0,0,0,1),
        intArrayOf(1,0,0,0,1),
        intArrayOf(1,0,0,0,1),
        intArrayOf(1,0,0,0,1),
        intArrayOf(0,1,1,1,0)
    )
    val glyphS = arrayOf(
        intArrayOf(0,1,1,1,1),
        intArrayOf(1,0,0,0,0),
        intArrayOf(1,0,0,0,0),
        intArrayOf(0,1,1,1,0),
        intArrayOf(0,0,0,0,1),
        intArrayOf(0,0,0,0,1),
        intArrayOf(1,1,1,1,0)
    )
    val glyphT = arrayOf(
        intArrayOf(1,1,1,1,1),
        intArrayOf(0,0,1,0,0),
        intArrayOf(0,0,1,0,0),
        intArrayOf(0,0,1,0,0),
        intArrayOf(0,0,1,0,0),
        intArrayOf(0,0,1,0,0),
        intArrayOf(0,0,1,0,0)
    )
    val wordRust = remember { arrayOf(glyphR, glyphU, glyphS, glyphT) }
    val accentColor = SkyBlueAccent

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFF161D22).copy(alpha = 0.86f),
        modifier = Modifier.border(1.dp, accentColor.copy(alpha = 0.45f), RoundedCornerShape(8.dp))
    ) {
        Canvas(
            modifier = Modifier
                .padding(horizontal = 8.dp, vertical = 5.dp)
                .width(56.dp)
                .height(15.dp)
        ) {
            val cols = 5
            val rows = 7
            val gap = 1
            val totalCols = wordRust.size * cols + (wordRust.size - 1) * gap
            val px = minOf(size.width / totalCols, size.height / rows)
            val startX = (size.width - totalCols * px) / 2f
            val startY = (size.height - rows * px) / 2f
            val shadow = (px * 0.40f).coerceAtLeast(1f)

            for (l in wordRust.indices) {
                val g = wordRust[l]
                val lx = startX + l * (cols + gap) * px
                for (r in 0 until rows) {
                    for (c in 0 until cols) {
                        if (g[r][c] == 1) {
                            drawRect(
                                color = Color(0xFF0A1014),
                                topLeft = Offset(lx + c * px + shadow, startY + r * px + shadow),
                                size = Size(px + 0.4f, px + 0.4f)
                            )
                        }
                    }
                }
                for (r in 0 until rows) {
                    for (c in 0 until cols) {
                        if (g[r][c] == 1) {
                            drawRect(
                                color = if (r == 0) Color.White else accentColor,
                                topLeft = Offset(lx + c * px, startY + r * px),
                                size = Size(px + 0.4f, px + 0.4f)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Renders the 8x8 Face + Hat Overlay of a 64x64 Minecraft Skin with crisp nearest-neighbor pixel art!
 */
@Composable
fun MinecraftSkinHeadAvatar(
    skinBitmap: Bitmap,
    size: Dp = 34.dp,
    cornerRadius: Dp = 8.dp
) {
    val imgBmp = remember(skinBitmap) { skinBitmap.asImageBitmap() }
    Canvas(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(cornerRadius))
            .background(Color(0xFF1A2228))
    ) {
        val dstW = this.size.width.toInt().coerceAtLeast(1)
        val dstH = this.size.height.toInt().coerceAtLeast(1)
        // Base Head Front (8,8) size 8x8
        drawImage(
            image = imgBmp,
            srcOffset = IntOffset(8, 8),
            srcSize = IntSize(8, 8),
            dstOffset = IntOffset(0, 0),
            dstSize = IntSize(dstW, dstH),
            filterQuality = FilterQuality.None
        )
        // Hat / Outer Layer Front (40,8) size 8x8
        drawImage(
            image = imgBmp,
            srcOffset = IntOffset(40, 8),
            srcSize = IntSize(8, 8),
            dstOffset = IntOffset(0, 0),
            dstSize = IntSize(dstW, dstH),
            filterQuality = FilterQuality.None
        )
    }
}

/**
 * Renders the Full Standing 2D Minecraft Player Model (Front or Back View) from any 64x64 Skin Bitmap
 * with crisp nearest-neighbor pixel art, Hat/Jacket/Sleeve/Pants overlays, and Slim (3px) vs Wide (4px) arms!
 */
@Composable
fun MinecraftSkinFullBodyCanvas(
    skinBitmap: Bitmap,
    slimArms: Boolean,
    showBack: Boolean,
    modifier: Modifier = Modifier
) {
    val imgBmp = remember(skinBitmap) { skinBitmap.asImageBitmap() }
    Canvas(modifier = modifier) {
        val totalUnitsW = 16f
        val totalUnitsH = 32f
        val scale = minOf(size.width / (totalUnitsW + 4f), size.height / (totalUnitsH + 4f))
        val modelW = totalUnitsW * scale
        val modelH = totalUnitsH * scale
        val originX = (size.width - modelW) / 2f
        val originY = (size.height - modelH) / 2f

        // Subtle floor shadow oval under the character's feet
        drawOval(
            color = Color.Black.copy(alpha = 0.42f),
            topLeft = Offset(originX + 2f * scale, originY + 30.5f * scale),
            size = Size(12f * scale, 2.5f * scale)
        )

        fun part(sx: Int, sy: Int, sw: Int, sh: Int, dxUnits: Float, dyUnits: Float, dwUnits: Float, dhUnits: Float) {
            drawImage(
                image = imgBmp,
                srcOffset = IntOffset(sx, sy),
                srcSize = IntSize(sw, sh),
                dstOffset = IntOffset((originX + dxUnits * scale).toInt(), (originY + dyUnits * scale).toInt()),
                dstSize = IntSize((dwUnits * scale).toInt().coerceAtLeast(1), (dhUnits * scale).toInt().coerceAtLeast(1)),
                filterQuality = FilterQuality.None
            )
        }

        val armW = if (slimArms) 3 else 4
        val armWf = armW.toFloat()
        val leftArmDx = 12f
        val rightArmDx = if (slimArms) 1f else 0f

        if (!showBack) {
            // FRONT VIEW
            // Head (8,8,8,8) + Hat (40,8,8,8) at x=4, y=0
            part(8, 8, 8, 8, 4f, 0f, 8f, 8f)
            part(40, 8, 8, 8, 4f, 0f, 8f, 8f)

            // Torso (20,20,8,12) + Jacket (20,36,8,12) at x=4, y=8
            part(20, 20, 8, 12, 4f, 8f, 8f, 12f)
            part(20, 36, 8, 12, 4f, 8f, 8f, 12f)

            // Right Arm (44,20,armW,12) + Sleeve (44,36,armW,12)
            part(44, 20, armW, 12, rightArmDx, 8f, armWf, 12f)
            part(44, 36, armW, 12, rightArmDx, 8f, armWf, 12f)

            // Left Arm (36,52,armW,12) + Sleeve (52,52,armW,12)
            part(36, 52, armW, 12, leftArmDx, 8f, armWf, 12f)
            part(52, 52, armW, 12, leftArmDx, 8f, armWf, 12f)

            // Right Leg (4,20,4,12) + Pants (4,36,4,12) at x=4, y=20
            part(4, 20, 4, 12, 4f, 20f, 4f, 12f)
            part(4, 36, 4, 12, 4f, 20f, 4f, 12f)

            // Left Leg (20,52,4,12) + Pants (4,52,4,12) at x=8, y=20
            part(20, 52, 4, 12, 8f, 20f, 4f, 12f)
            part(4, 52, 4, 12, 8f, 20f, 4f, 12f)
        } else {
            // BACK VIEW
            // Head Back (24,8,8,8) + Hat Back (56,8,8,8)
            part(24, 8, 8, 8, 4f, 0f, 8f, 8f)
            part(56, 8, 8, 8, 4f, 0f, 8f, 8f)

            // Torso Back (32,20,8,12) + Jacket Back (32,36,8,12)
            part(32, 20, 8, 12, 4f, 8f, 8f, 12f)
            part(32, 36, 8, 12, 4f, 8f, 8f, 12f)

            // Back Left/Right Arms
            val rArmBackX = if (slimArms) 51 else 52
            val lArmBackX = if (slimArms) 43 else 44
            part(lArmBackX, 52, armW, 12, rightArmDx, 8f, armWf, 12f)
            part(rArmBackX, 20, armW, 12, leftArmDx, 8f, armWf, 12f)

            // Back Legs
            part(28, 52, 4, 12, 4f, 20f, 4f, 12f)
            part(12, 20, 4, 12, 8f, 20f, 4f, 12f)
        }
    }
}

private data class Skin3DQuad(
    val x0: Float, val y0: Float,
    val x1: Float, val y1: Float,
    val x2: Float, val y2: Float,
    val x3: Float, val y3: Float,
    val zDepth: Float,
    val colorArgb: Int
)

@Composable
fun MinecraftSkin3DPreviewCanvas(
    skinBitmap: Bitmap,
    slimArms: Boolean,
    autoRotate: Boolean,
    walkAnimation: Boolean,
    modifier: Modifier = Modifier
) {
    val pixels = remember(skinBitmap) {
        val scaled = if (skinBitmap.width == 64 && skinBitmap.height == 64) {
            skinBitmap
        } else {
            Bitmap.createScaledBitmap(skinBitmap, 64, 64, false)
        }
        val arr = IntArray(64 * 64)
        scaled.getPixels(arr, 0, 64, 0, 0, 64, 64)
        arr
    }

    var yawDeg by remember { mutableFloatStateOf(-28f) }
    var pitchDeg by remember { mutableFloatStateOf(12f) }
    var walkTick by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(autoRotate, walkAnimation) {
        while (autoRotate || walkAnimation) {
            delay(16L)
            if (autoRotate) {
                yawDeg = (yawDeg + 1.15f) % 360f
            }
            if (walkAnimation) {
                walkTick = (walkTick + 0.11f) % (Math.PI.toFloat() * 2f)
            }
        }
    }

    val reusablePath = remember { Path() }

    Canvas(
        modifier = modifier.pointerInput(Unit) {
            detectDragGestures { change, dragAmount ->
                change.consume()
                yawDeg = (yawDeg + dragAmount.x * 0.65f) % 360f
                pitchDeg = (pitchDeg + dragAmount.y * 0.38f).coerceIn(-40f, 40f)
            }
        }
    ) {
        val w = size.width
        val h = size.height
        val scale = minOf(w / 24f, h / 38f)
        val centerX = w * 0.5f
        val centerY = h * 0.5f

        drawOval(
            color = Color.Black.copy(alpha = 0.45f),
            topLeft = Offset(centerX - 6.5f * scale, centerY + 14.2f * scale),
            size = Size(13f * scale, 3.2f * scale)
        )

        val yawRad = Math.toRadians(yawDeg.toDouble()).toFloat()
        val pitchRad = Math.toRadians(pitchDeg.toDouble()).toFloat()
        val cosY = cos(yawRad)
        val sinY = sin(yawRad)
        val cosP = cos(pitchRad)
        val sinP = sin(pitchRad)

        val quads = ArrayList<Skin3DQuad>(1600)

        fun shadeColor(argb: Int, factor: Float): Int {
            val a = (argb ushr 24) and 0xFF
            val r = (((argb ushr 16) and 0xFF) * factor).roundToInt().coerceIn(0, 255)
            val g = (((argb ushr 8) and 0xFF) * factor).roundToInt().coerceIn(0, 255)
            val b = ((argb and 0xFF) * factor).roundToInt().coerceIn(0, 255)
            return (a shl 24) or (r shl 16) or (g shl 8) or b
        }

        fun transformPoint(
            lx: Float, ly: Float, lz: Float,
            pivX: Float, pivY: Float, pivZ: Float,
            rotXRad: Float
        ): FloatArray {
            val cosR = cos(rotXRad)
            val sinR = sin(rotXRad)
            val ry = ly * cosR - lz * sinR
            val rz = ly * sinR + lz * cosR
            val wx = pivX + lx
            val wy = pivY + ry
            val wz = pivZ + rz

            val x1 = wx * cosY + wz * sinY
            val z1 = -wx * sinY + wz * cosY
            val y2 = wy * cosP - z1 * sinP
            val z2 = wy * sinP + z1 * cosP

            val fov = 68f
            val persp = fov / (fov - z2).coerceAtLeast(20f)
            val sx = centerX + x1 * scale * persp
            val sy = centerY + y2 * scale * persp
            return floatArrayOf(sx, sy, z2)
        }

        fun emitFaceGrid(
            p00: FloatArray, p10: FloatArray, p11: FloatArray, p01: FloatArray,
            uStart: Int, vStart: Int, uWidth: Int, vHeight: Int,
            shade: Float, isOverlay: Boolean
        ) {
            val ux = p10[0] - p00[0]
            val uy = p10[1] - p00[1]
            val vx = p01[0] - p00[0]
            val vy = p01[1] - p00[1]
            if (ux * vy - uy * vx <= 0f && !isOverlay) return

            val invW = 1f / uWidth.toFloat()
            val invH = 1f / vHeight.toFloat()
            val zBias = if (isOverlay) 0.18f else 0f

            for (gy in 0 until vHeight) {
                val ty0 = gy * invH
                val ty1 = (gy + 1) * invH
                val vTex = (vStart + gy).coerceIn(0, 63)
                for (gx in 0 until uWidth) {
                    val uTex = (uStart + gx).coerceIn(0, 63)
                    val rawArgb = pixels[vTex * 64 + uTex]
                    val alpha = (rawArgb ushr 24) and 0xFF
                    if (alpha < 24) continue
                    val finalArgb = shadeColor(rawArgb or (0xFF shl 24), shade)

                    val tx0 = gx * invW
                    val tx1 = (gx + 1) * invW

                    fun bilerpX(tx: Float, ty: Float): Float {
                        val top = p00[0] + (p10[0] - p00[0]) * tx
                        val bot = p01[0] + (p11[0] - p01[0]) * tx
                        return top + (bot - top) * ty
                    }
                    fun bilerpY(tx: Float, ty: Float): Float {
                        val top = p00[1] + (p10[1] - p00[1]) * tx
                        val bot = p01[1] + (p11[1] - p01[1]) * tx
                        return top + (bot - top) * ty
                    }
                    fun bilerpZ(tx: Float, ty: Float): Float {
                        val top = p00[2] + (p10[2] - p00[2]) * tx
                        val bot = p01[2] + (p11[2] - p01[2]) * tx
                        return top + (bot - top) * ty
                    }

                    val x0 = bilerpX(tx0, ty0)
                    val y0 = bilerpY(tx0, ty0)
                    val x1 = bilerpX(tx1, ty0)
                    val y1 = bilerpY(tx1, ty0)
                    val x2 = bilerpX(tx1, ty1)
                    val y2 = bilerpY(tx1, ty1)
                    val x3 = bilerpX(tx0, ty1)
                    val y3 = bilerpY(tx0, ty1)
                    val zAvg = bilerpZ((tx0 + tx1) * 0.5f, (ty0 + ty1) * 0.5f) + zBias

                    quads.add(Skin3DQuad(x0, y0, x1, y1, x2, y2, x3, y3, zAvg, finalArgb))
                }
            }
        }

        fun emitBox(
            pivX: Float, pivY: Float, pivZ: Float,
            xMin: Float, yMin: Float, zMin: Float,
            xMax: Float, yMax: Float, zMax: Float,
            rotXRad: Float,
            uOrigin: Int, vOrigin: Int,
            texW: Int, texH: Int, texD: Int,
            isOverlay: Boolean
        ) {
            val ftl = transformPoint(xMin, yMin, zMax, pivX, pivY, pivZ, rotXRad)
            val ftr = transformPoint(xMax, yMin, zMax, pivX, pivY, pivZ, rotXRad)
            val fbr = transformPoint(xMax, yMax, zMax, pivX, pivY, pivZ, rotXRad)
            val fbl = transformPoint(xMin, yMax, zMax, pivX, pivY, pivZ, rotXRad)

            val btl = transformPoint(xMin, yMin, zMin, pivX, pivY, pivZ, rotXRad)
            val btr = transformPoint(xMax, yMin, zMin, pivX, pivY, pivZ, rotXRad)
            val bbr = transformPoint(xMax, yMax, zMin, pivX, pivY, pivZ, rotXRad)
            val bbl = transformPoint(xMin, yMax, zMin, pivX, pivY, pivZ, rotXRad)

            emitFaceGrid(ftl, ftr, fbr, fbl, uOrigin + texD, vOrigin + texD, texW, texH, 1.0f, isOverlay)
            emitFaceGrid(btr, btl, bbl, bbr, uOrigin + texD * 2 + texW, vOrigin + texD, texW, texH, 0.74f, isOverlay)
            emitFaceGrid(btl, ftl, fbl, bbl, uOrigin, vOrigin + texD, texD, texH, 0.84f, isOverlay)
            emitFaceGrid(ftr, btr, bbr, fbr, uOrigin + texD + texW, vOrigin + texD, texD, texH, 0.82f, isOverlay)
            emitFaceGrid(btl, btr, ftr, ftl, uOrigin + texD, vOrigin, texW, texD, 1.06f, isOverlay)
            emitFaceGrid(fbl, fbr, bbr, bbl, uOrigin + texD + texW, vOrigin, texW, texD, 0.62f, isOverlay)
        }

        val swingAngle = if (walkAnimation) sin(walkTick) * 0.42f else 0.14f
        val armW = if (slimArms) 3 else 4
        val armWf = armW.toFloat()

        emitBox(0f, -8f, 0f, -4f, -8f, -4f, 4f, 0f, 4f, 0f, 0, 0, 8, 8, 8, false)
        emitBox(0f, -8f, 0f, -4.35f, -8.35f, -4.35f, 4.35f, 0.35f, 4.35f, 0f, 32, 0, 8, 8, 8, true)

        emitBox(0f, -8f, 0f, -4f, 0f, -2f, 4f, 12f, 2f, 0f, 16, 16, 8, 12, 4, false)
        emitBox(0f, -8f, 0f, -4.25f, -0.25f, -2.25f, 4.25f, 12.25f, 2.25f, 0f, 16, 32, 8, 12, 4, true)

        val rArmPivX = -4f - armWf * 0.5f
        emitBox(rArmPivX, -6f, 0f, -armWf * 0.5f, -2f, -2f, armWf * 0.5f, 10f, 2f, swingAngle, 40, 16, armW, 12, 4, false)
        emitBox(rArmPivX, -6f, 0f, -armWf * 0.5f - 0.25f, -2.25f, -2.25f, armWf * 0.5f + 0.25f, 10.25f, 2.25f, swingAngle, 40, 32, armW, 12, 4, true)

        val lArmPivX = 4f + armWf * 0.5f
        emitBox(lArmPivX, -6f, 0f, -armWf * 0.5f, -2f, -2f, armWf * 0.5f, 10f, 2f, -swingAngle, 32, 48, armW, 12, 4, false)
        emitBox(lArmPivX, -6f, 0f, -armWf * 0.5f - 0.25f, -2.25f, -2.25f, armWf * 0.5f + 0.25f, 10.25f, 2.25f, -swingAngle, 48, 48, armW, 12, 4, true)

        emitBox(-2f, 4f, 0f, -2f, 0f, -2f, 2f, 12f, 2f, -swingAngle * 0.85f, 0, 16, 4, 12, 4, false)
        emitBox(-2f, 4f, 0f, -2.25f, -0.25f, -2.25f, 2.25f, 12.25f, 2.25f, -swingAngle * 0.85f, 0, 32, 4, 12, 4, true)

        emitBox(2f, 4f, 0f, -2f, 0f, -2f, 2f, 12f, 2f, swingAngle * 0.85f, 16, 48, 4, 12, 4, false)
        emitBox(2f, 4f, 0f, -2.25f, -0.25f, -2.25f, 2.25f, 12.25f, 2.25f, swingAngle * 0.85f, 0, 48, 4, 12, 4, true)

        quads.sortBy { it.zDepth }

        for (i in 0 until quads.size) {
            val q = quads[i]
            reusablePath.reset()
            reusablePath.moveTo(q.x0, q.y0)
            reusablePath.lineTo(q.x1, q.y1)
            reusablePath.lineTo(q.x2, q.y2)
            reusablePath.lineTo(q.x3, q.y3)
            reusablePath.close()
            drawPath(path = reusablePath, color = Color(q.colorArgb))
        }
    }
}

/**
 * Zalith-style Settings Dropdown Card (`429668.jpg` style):
 * Full-width dark slate rounded card with Title, Description subtitle, `Selected: <value>`, and right dropdown arrow.
 */
@Composable
fun <T> ZalithSettingDropdownCard(
    title: String,
    description: String,
    selectedItem: T,
    options: List<T>,
    itemLabel: (T) -> String,
    onSelect: (T) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val arrowRot by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = tween(220),
        label = "SettingDropdownArrow"
    )

    Box(modifier = Modifier.fillMaxWidth()) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = CardSlateBg,
            modifier = Modifier
                .fillMaxWidth()
                .animatedPressScale { expanded = !expanded }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text(
                        text = title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                    Text(
                        text = description,
                        fontSize = 12.sp,
                        color = MutedSubtitleText
                    )
                    Text(
                        text = "Selected: ${itemLabel(selectedItem)}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFFC5D1D8)
                    )
                }
                Spacer(Modifier.width(12.dp))
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier
                        .size(24.dp)
                        .rotate(arrowRot)
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(Color(0xFF262E34))
        ) {
            options.forEach { option ->
                val isSelected = option == selectedItem
                DropdownMenuItem(
                    text = {
                        Text(
                            text = itemLabel(option),
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) SkyBlueAccent else Color.White
                        )
                    },
                    trailingIcon = if (isSelected) {
                        {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                tint = SkyBlueAccent,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    } else null,
                    onClick = {
                        expanded = false
                        onSelect(option)
                    }
                )
            }
        }
    }
}

/**
 * Zalith-style Slider Card (`429668.jpg` bottom card style):
 * Title + Subtitle + thick track slider on left + Sky-Blue `<  50%  >` stepper pill on right!
 */
@Composable
fun ZalithSettingSliderCard(
    title: String,
    description: String,
    value: Int,
    valueRange: IntRange,
    step: Int = 1,
    valueSuffix: String = "%",
    onValueChange: (Int) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = CardSlateBg,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
            Text(
                text = description,
                fontSize = 12.sp,
                color = MutedSubtitleText
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Slider(
                    value = value.toFloat(),
                    onValueChange = { onValueChange(it.toInt().coerceIn(valueRange.first, valueRange.last)) },
                    valueRange = valueRange.first.toFloat()..valueRange.last.toFloat(),
                    colors = SliderDefaults.colors(
                        thumbColor = SkyBlueAccent,
                        activeTrackColor = SkyBlueAccent,
                        inactiveTrackColor = Color(0xFF33424B)
                    ),
                    modifier = Modifier.weight(1f)
                )

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = SkyBlueAccent
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "$value$valueSuffix",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = SkyBlueDarkText
                        )
                        Icon(
                            imageVector = Icons.Default.ArrowLeft,
                            contentDescription = "Decrease",
                            tint = SkyBlueDarkText,
                            modifier = Modifier
                                .size(20.dp)
                                .animatedPressScale {
                                    onValueChange((value - step).coerceIn(valueRange.first, valueRange.last))
                                }
                        )
                        Icon(
                            imageVector = Icons.Default.ArrowRight,
                            contentDescription = "Increase",
                            tint = SkyBlueDarkText,
                            modifier = Modifier
                                .size(20.dp)
                                .animatedPressScale {
                                    onValueChange((value + step).coerceIn(valueRange.first, valueRange.last))
                                }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ZalithSettingToggleCard(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = CardSlateBg,
        modifier = Modifier
            .fillMaxWidth()
            .animatedPressScale { onCheckedChange(!checked) }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
                Text(
                    text = description,
                    fontSize = 12.sp,
                    color = MutedSubtitleText
                )
            }
            Spacer(Modifier.width(12.dp))
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = SkyBlueDarkText,
                    checkedTrackColor = SkyBlueAccent
                )
            )
        }
    }
}

/**
 * Vertical Sub-Navigation Rail with Capsule Active Indicator (`429668.jpg` & `429665.jpg` left rail style).
 */
@Composable
fun <T> ZalithSubCategoryRail(
    items: List<T>,
    selectedItem: T,
    itemLabel: (T) -> String,
    itemIcon: (T) -> ImageVector,
    onSelect: (T) -> Unit
) {
    Column(
        modifier = Modifier
            .width(84.dp)
            .fillMaxHeight()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items.forEach { item ->
            val selected = item == selectedItem
            val pillBg by animateColorAsState(
                targetValue = if (selected) SubRailCapsuleActive else Color.Transparent,
                animationSpec = tween(200),
                label = "SubRailPillBg"
            )
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .animatedPressScale { onSelect(item) }
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .width(54.dp)
                        .height(32.dp)
                        .clip(RoundedCornerShape(50))
                        .background(pillBg)
                ) {
                    Icon(
                        imageVector = itemIcon(item),
                        contentDescription = itemLabel(item),
                        tint = if (selected) SkyBlueAccent else MutedSubtitleText,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(Modifier.height(3.dp))
                Text(
                    text = itemLabel(item),
                    fontSize = 11.sp,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (selected) Color.White else MutedSubtitleText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun LavenderTagBadge(text: String) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = LavenderBadgeBg
    ) {
        Text(
            text = text,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            color = LavenderBadgeText,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
        )
    }
}

@Composable
private fun InAppUpdateBannerCard(
    info: GithubReleaseUpdateInfo,
    onUpdateClick: () -> Unit,
    onLaterClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = CardSlateBg,
        tonalElevation = 8.dp,
        shadowElevation = 12.dp,
        modifier = Modifier.widthIn(max = 520.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            SubRailCapsuleActive,
                            CardSlateBg
                        )
                    )
                )
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(SkyBlueAccent)
                ) {
                    Icon(
                        Icons.Default.SystemUpdate,
                        contentDescription = null,
                        tint = SkyBlueDarkText,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Column {
                    Text(
                        text = "New Version!! (v${info.latestVersion})",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 13.sp,
                        color = Color.White
                    )
                    Text(
                        text = "Installed: v${HorizonNativeBridge.INSTALLED_LAUNCHER_VERSION} • ${info.releaseTitle}",
                        fontSize = 11.sp,
                        color = MutedSubtitleText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(Modifier.width(10.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                TextButton(
                    onClick = onLaterClick,
                    modifier = Modifier.height(34.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                ) {
                    Text("Later", fontSize = 12.sp, color = MutedSubtitleText)
                }
                Button(
                    onClick = onUpdateClick,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SkyBlueAccent),
                    modifier = Modifier.height(34.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp)
                ) {
                    Text("Update", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SkyBlueDarkText)
                }
            }
        }
    }
}

@Composable
fun ZalithCleanLauncherScreen(
    config: HorizonConfigState,
    accounts: List<ZalithAccount>,
    activeAccount: ZalithAccount,
    isHibernated: Boolean,
    onConfigChange: (HorizonConfigState) -> Unit,
    onAccountsChange: (List<ZalithAccount>) -> Unit,
    onCheckForUpdatesManual: () -> Unit = {},
    onEnterGameHibernation: () -> Unit
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(ZalithNavTab.HOME) }
    var storeInitialCategory by remember { mutableStateOf(StoreCategory.ALL) }
    var installStatus by remember { mutableStateOf<AssetInstallStatus>(HorizonNativeBridge.pollInstallProgress(context, config.enableSystemLog)) }
    var skinDetectionBanner by remember { mutableStateOf<String?>(null) }
    var skinReloadCounter by remember { mutableIntStateOf(0) }
    var pendingCustomSkinName by remember { mutableStateOf("") }

    // Cache the active account's 64x64 skin bitmap for instant rendering on Dashboard & Skin Studio
    val activeSkinBitmap = remember(activeAccount.id, activeAccount.skinPath, activeAccount.slimSkinModel, skinReloadCounter) {
        HorizonNativeBridge.loadOrGenerateAccountSkinBitmap(context, activeAccount)
    }

    // Hoisted skinPicker at root screen level so it is NEVER disposed when Google Photos / File Picker opens!
    val skinPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            val res = HorizonNativeBridge.detectAndImportSkinFromUri(context, uri, activeAccount, config, pendingCustomSkinName)
            pendingCustomSkinName = ""
            if (res.success && res.skinFile != null) {
                val updatedList = accounts.map { acc ->
                    if (acc.id == activeAccount.id) {
                        acc.copy(
                            skinPath = res.skinFile.absolutePath,
                            slimSkinModel = res.slimModel,
                            skinSourceLabel = res.sourceLabel
                        )
                    } else acc
                }
                onAccountsChange(updatedList)
                skinReloadCounter += 1
                skinDetectionBanner = res.message
                Toast.makeText(context, res.message, Toast.LENGTH_SHORT).show()
            } else {
                skinDetectionBanner = res.message
                Toast.makeText(context, res.message, Toast.LENGTH_LONG).show()
            }
        }
    }

    LaunchedEffect(config.enableSystemLog, isHibernated) {
        while (!isHibernated) {
            installStatus = HorizonNativeBridge.pollInstallProgress(context, config.enableSystemLog)
            delay(500L)
        }
    }

    val triggerDownload = {
        selectedTab = ZalithNavTab.VERSIONS
        val started = HorizonNativeBridge.startMinecraft26Download(context, config.enableSystemLog)
        if (!started && installStatus.ready) {
            onEnterGameHibernation()
        }
    }

    val handleLaunchOrDownload = {
        if (!installStatus.ready && !installStatus.running) {
            triggerDownload()
        } else if (!installStatus.running) {
            onEnterGameHibernation()
        } else {
            selectedTab = ZalithNavTab.VERSIONS
        }
    }

    // Smooth Morph / Connected Width Animation for the Right Launch Sidebar:
    // Includes the 10.dp gap INSIDE the animated 192.dp width (10.dp start padding + 182.dp card)
    // so when it morphs to 0.dp on Store/Files/Skins/Settings, there is ZERO Row gap jump ("jedug")!
    val showRightSidebar = selectedTab == ZalithNavTab.HOME || selectedTab == ZalithNavTab.VERSIONS
    val sidebarMorphWidth by animateDpAsState(
        targetValue = if (showRightSidebar) 192.dp else 0.dp,
        animationSpec = tween(durationMillis = 270, easing = FastOutSlowInEasing),
        label = "SidebarMorphWidth"
    )
    val sidebarMorphAlpha by animateFloatAsState(
        targetValue = if (showRightSidebar) 1f else 0f,
        animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
        label = "SidebarMorphAlpha"
    )

    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(LauncherRootBg)
                .windowInsetsPadding(WindowInsets.safeDrawing.union(WindowInsets.displayCutout))
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Clean, Zero-Duplicate Top Bar:
                // Left: Back button (only when not on HOME) + Title
                // Right: Folder + Log (no duplicate Profile, Settings, or Download buttons!)
                ZalithReferenceTopBar(
                    selectedTab = selectedTab,
                    installStatus = installStatus,
                    onNavigateHome = { selectedTab = ZalithNavTab.HOME },
                    onOpenDataFolder = { HorizonNativeBridge.openSystemFilesApp(context) },
                    onOpenLogs = { selectedTab = ZalithNavTab.LOGS }
                )

                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(top = 4.dp, bottom = 4.dp)
                ) {
                    ZalithIconRail(
                        selectedTab = selectedTab,
                        onSelectTab = { selectedTab = it }
                    )

                    Spacer(Modifier.width(10.dp))

                    // Main Morphing Content Area:
                    // Smoothly expands/contracts its width in sync with `sidebarMorphWidth` and morphs
                    // between pages with scale + slide + fade!
                    AnimatedContent(
                        targetState = selectedTab,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        transitionSpec = {
                            val forward = targetState.ordinal >= initialState.ordinal
                            (fadeIn(animationSpec = tween(250, easing = FastOutSlowInEasing)) +
                                scaleIn(
                                    initialScale = 0.96f,
                                    animationSpec = tween(250, easing = FastOutSlowInEasing)
                                ) +
                                slideInVertically(
                                    animationSpec = tween(250, easing = FastOutSlowInEasing),
                                    initialOffsetY = { fullH -> if (forward) fullH / 14 else -fullH / 14 }
                                ))
                                .togetherWith(
                                    fadeOut(animationSpec = tween(180, easing = FastOutLinearInEasing)) +
                                        scaleOut(
                                            targetScale = 0.97f,
                                            animationSpec = tween(180, easing = FastOutLinearInEasing)
                                        )
                                )
                                .using(sizeTransform = null)
                        },
                        label = "MainTabMorphTransition"
                    ) { tab ->
                        Box(modifier = Modifier.fillMaxSize()) {
                            when (tab) {
                                ZalithNavTab.HOME -> CleanHomeViewport(
                                    config = config,
                                    onConfigChange = onConfigChange,
                                    logEnabled = config.enableSystemLog,
                                    isHibernated = isHibernated
                                )

                                ZalithNavTab.VERSIONS -> CleanVersionsViewport(
                                    context = context,
                                    installStatus = installStatus,
                                    onStartDownload = triggerDownload,
                                    onPauseDownload = {
                                        HorizonNativeBridge.pauseMinecraft26Download(context, config.enableSystemLog)
                                    },
                                    onResumeDownload = {
                                        HorizonNativeBridge.resumeMinecraft26Download(context, config.enableSystemLog)
                                    },
                                    onCancelDownload = {
                                        HorizonNativeBridge.cancelMinecraft26Download(context, config.enableSystemLog)
                                    },
                                    onSmartRepairAssets = {
                                        HorizonNativeBridge.smartRepairAssets(context, config.enableSystemLog)
                                    }
                                )

                                ZalithNavTab.STORE -> CleanStoreViewport(
                                    profileSlot = config.profile,
                                    logEnabled = config.enableSystemLog,
                                    initialCategory = storeInitialCategory
                                )

                                ZalithNavTab.FILES -> CleanFilesViewport(
                                    profileSlot = config.profile,
                                    logEnabled = config.enableSystemLog,
                                    onNavigateToStoreCategory = { targetCat ->
                                        storeInitialCategory = targetCat
                                        selectedTab = ZalithNavTab.STORE
                                    },
                                    onQuickPlayWorld = {
                                        handleLaunchOrDownload()
                                    }
                                )

                                ZalithNavTab.ACCOUNTS -> CleanAccountsAndSkinsViewport(
                                    accounts = accounts,
                                    activeAccount = activeAccount,
                                    activeSkinBitmap = activeSkinBitmap,
                                    config = config,
                                    detectionBanner = skinDetectionBanner,
                                    customSkinNameInput = pendingCustomSkinName,
                                    onCustomSkinNameChange = { pendingCustomSkinName = it },
                                    skinReloadCounter = skinReloadCounter,
                                    onLaunchSkinPicker = { skinPicker.launch("image/*") },
                                    onUpdateBanner = { skinDetectionBanner = it },
                                    onSkinUpdated = { skinReloadCounter += 1 },
                                    onAccountsChange = onAccountsChange
                                )

                                ZalithNavTab.LOGS -> CleanLogsViewport(
                                    logEnabled = config.enableSystemLog
                                )

                                ZalithNavTab.SETTINGS -> CleanSettingsViewport(
                                    config = config,
                                    onConfigChange = onConfigChange,
                                    onCheckForUpdatesManual = onCheckForUpdatesManual,
                                    onOpenLogs = { selectedTab = ZalithNavTab.LOGS }
                                )
                            }
                        }
                    }

                    // Smoothly Morphing Right Launch Sidebar Container (0.dp <-> 192.dp with zero layout jolt!)
                    if (sidebarMorphWidth > 0.5.dp) {
                        Box(
                            modifier = Modifier
                                .width(sidebarMorphWidth)
                                .fillMaxHeight()
                                .padding(start = 10.dp)
                                .graphicsLayer {
                                    alpha = sidebarMorphAlpha
                                    translationX = (1f - sidebarMorphAlpha) * 36f
                                }
                        ) {
                            ZalithLaunchSidebar(
                                activeAccount = activeAccount,
                                activeSkinBitmap = activeSkinBitmap,
                                installStatus = installStatus,
                                onOpenAccountScreen = { selectedTab = ZalithNavTab.ACCOUNTS },
                                onOpenVersions = { selectedTab = ZalithNavTab.VERSIONS },
                                onLaunchOrDownload = handleLaunchOrDownload
                            )
                        }
                    }
                }
            }
        }

        // Render the Black "HORIZONZ" Splash Screen as a top-level overlay when launching the game
        // (without unmounting the Compose tree underneath!)
        AnimatedVisibility(
            visible = isHibernated,
            enter = fadeIn(tween(180)),
            exit = fadeOut(tween(220))
        ) {
            HorizonZBlackSplashOverlay()
        }
    }
}

/**
 * Clean Top Bar without duplicate buttons:
 * - Left: `<- Back` pill (only when not on Dashboard) + Title (`HorizonZ` or `HorizonZ — <Page>`)
 * - Right: `Folder` + `Log` (Profile is in Right Sidebar / Left Rail; Settings & Downloads are in Left Rail)
 */
@Composable
private fun ZalithReferenceTopBar(
    selectedTab: ZalithNavTab,
    installStatus: AssetInstallStatus,
    onNavigateHome: () -> Unit,
    onOpenDataFolder: () -> Unit,
    onOpenLogs: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(38.dp)
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AnimatedVisibility(
                visible = selectedTab != ZalithNavTab.HOME,
                enter = fadeIn(tween(180)) + expandHorizontally(tween(200)),
                exit = fadeOut(tween(150)) + shrinkHorizontally(tween(180))
            ) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = CardSlateBg,
                    modifier = Modifier.animatedPressScale(onClick = onNavigateHome)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = SkyBlueAccent,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = "Back",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    }
                }
            }

            AnimatedContent(
                targetState = selectedTab,
                transitionSpec = {
                    fadeIn(tween(180)).togetherWith(fadeOut(tween(140))).using(sizeTransform = null)
                },
                label = "TopBarTitleAnim"
            ) { tab ->
                Text(
                    text = if (tab == ZalithNavTab.HOME) "HorizonZ" else "HorizonZ — ${tab.title}",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }


        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(50),
                color = CardSlateBg,
                modifier = Modifier.animatedPressScale(onClick = onOpenDataFolder)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Folder,
                        contentDescription = "Data Folder",
                        tint = Color(0xFFD5DEE5),
                        modifier = Modifier.size(15.dp)
                    )
                    Text("Folder", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = Color.White)
                }
            }

            Surface(
                shape = RoundedCornerShape(50),
                color = if (selectedTab == ZalithNavTab.LOGS) SubRailCapsuleActive else CardSlateBg,
                modifier = Modifier.animatedPressScale(onClick = onOpenLogs)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Terminal,
                        contentDescription = "Log",
                        tint = SkyBlueAccent,
                        modifier = Modifier.size(15.dp)
                    )
                    Text("Log", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = Color.White)
                }
            }
        }
    }
}

@Composable
private fun ZalithIconRail(
    selectedTab: ZalithNavTab,
    onSelectTab: (ZalithNavTab) -> Unit
) {
    val railTabs = remember {
        listOf(
            ZalithNavTab.HOME,
            ZalithNavTab.VERSIONS,
            ZalithNavTab.STORE,
            ZalithNavTab.FILES,
            ZalithNavTab.ACCOUNTS,
            ZalithNavTab.SETTINGS
        )
    }
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = CardSlateBg,
        modifier = Modifier
            .fillMaxHeight()
            .width(52.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically)
        ) {
            railTabs.forEach { tab ->
                val isSelected = selectedTab == tab
                val iconScale by animateFloatAsState(
                    targetValue = if (isSelected) 1.08f else 1.0f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessMedium
                    ),
                    label = "railIconScale"
                )
                val iconVec = when (tab) {
                    ZalithNavTab.HOME -> Icons.Default.Home
                    ZalithNavTab.VERSIONS -> Icons.Default.Download
                    ZalithNavTab.STORE -> Icons.Default.Storefront
                    ZalithNavTab.FILES -> Icons.Default.Extension
                    ZalithNavTab.ACCOUNTS -> Icons.Default.Checkroom
                    ZalithNavTab.LOGS -> Icons.Default.Terminal
                    ZalithNavTab.SETTINGS -> Icons.Default.Settings
                }
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (isSelected) SubRailCapsuleActive else Color.Transparent,
                    modifier = Modifier
                        .size(38.dp)
                        .animatedPressScale { onSelectTab(tab) }
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Icon(
                            imageVector = iconVec,
                            contentDescription = tab.title,
                            tint = if (isSelected) SkyBlueAccent else MutedSubtitleText,
                            modifier = Modifier
                                .size(19.dp)
                                .graphicsLayer {
                                    scaleX = iconScale
                                    scaleY = iconScale
                                }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Main Dashboard Viewport:
 * - 18 Authentic HD 16:9 Minecraft Wallpapers cycling every 5 seconds with smooth crossfade.
 * - Zero duplicate navigation buttons on top-right (only a single clean `Wallpaper` button).
 */
private class HorizonZVideoJsBridge(
    private val mainHandler: android.os.Handler,
    private val onVideoEndedCallback: () -> Unit
) {
    private val triggered = java.util.concurrent.atomic.AtomicBoolean(false)

    fun resetTrigger() {
        triggered.set(false)
    }

    @JavascriptInterface
    fun onEnded() {
        if (triggered.compareAndSet(false, true)) {
            mainHandler.post { onVideoEndedCallback() }
        }
    }

    @JavascriptInterface
    fun onError() {
        if (triggered.compareAndSet(false, true)) {
            mainHandler.post { onVideoEndedCallback() }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun MinecraftTrailerVideoWallpaperPlayer(
    videoId: String,
    directStreamUrl: String,
    trailerTitle: String,
    isMuted: Boolean,
    isPaused: Boolean,
    onVideoEndedOrError: () -> Unit,
    modifier: Modifier = Modifier
) {
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    val currentOnEnded by rememberUpdatedState(onVideoEndedOrError)
    val mainHandler = remember { android.os.Handler(android.os.Looper.getMainLooper()) }
    val jsBridge = remember {
        HorizonZVideoJsBridge(mainHandler) { currentOnEnded() }
    }

    val buildUnifiedPlayerHtml = remember {
        { vid: String, initialStreamUrl: String, muted: Boolean ->
            val mBool = if (muted) "true" else "false"
            val safeStream = initialStreamUrl.replace("'", "\'")
            """
            <!DOCTYPE html>
            <html>
            <head>
            <meta charset="utf-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
            <meta name="referrer" content="origin">
            <style>
              * { box-sizing: border-box; margin: 0; padding: 0; user-select: none; -webkit-user-select: none; }
              html, body { width: 100vw; height: 100vh; background: #000; overflow: hidden; }
              #stage { position: fixed; inset: 0; width: 100vw; height: 100vh; background: #000; overflow: hidden; }
              #ytframe {
                position: absolute;
                top: -12%;
                left: -12%;
                width: 124vw;
                height: 124vh;
                border: 0;
                background: #000;
                pointer-events: none;
                z-index: 1;
              }
              #directVid {
                position: absolute;
                top: -4%;
                left: -4%;
                width: 108vw;
                height: 108vh;
                object-fit: cover;
                background: #000;
                pointer-events: none;
                z-index: 2;
                display: none;
              }
            </style>
            </head>
            <body>
              <div id="stage">
                <iframe
                  id="ytframe"
                  src="https://www.youtube-nocookie.com/embed/$vid?enablejsapi=1&autoplay=1&mute=1&controls=0&modestbranding=1&rel=0&showinfo=0&iv_load_policy=3&cc_load_policy=0&cc_lang_pref=none&fs=0&playsinline=1&vq=hd720&origin=https://www.minecraft.net"
                  allow="autoplay; encrypted-media; picture-in-picture"
                  referrerpolicy="origin">
                </iframe>
                <video id="directVid" playsinline webkit-playsinline autoplay muted preload="auto"></video>
              </div>
              <script>
                window.__hzMuted = $mBool;
                window.__hzPaused = false;
                window.__hzDirectActive = false;
                window.__hzEndedSent = false;

                function notifyEnded() {
                  if (window.__hzEndedSent) return;
                  window.__hzEndedSent = true;
                  try {
                    if (window.HorizonZBridge && window.HorizonZBridge.onEnded) {
                      window.HorizonZBridge.onEnded();
                      return;
                    }
                  } catch (e) {}
                  window.location.href = 'https://www.minecraft.net/__hz_ended';
                }

                function ytCmd(func, args) {
                  try {
                    var f = document.getElementById('ytframe');
                    if (f && f.contentWindow) {
                      f.contentWindow.postMessage(JSON.stringify({
                        event: 'command',
                        func: func,
                        args: args || []
                      }), '*');
                    }
                  } catch (e) {}
                }

                function syncAudioState() {
                  var dv = document.getElementById('directVid');
                  if (window.__hzDirectActive && dv) {
                    dv.muted = !!window.__hzMuted;
                    if (!window.__hzMuted) dv.volume = 1.0;
                    ytCmd('mute', []);
                    ytCmd('pauseVideo', []);
                  } else {
                    if (window.__hzMuted) {
                      ytCmd('mute', []);
                    } else {
                      ytCmd('unMute', []);
                      ytCmd('setVolume', [100]);
                    }
                  }
                }

                window.__hzSetMuted = function(m) {
                  window.__hzMuted = !!m;
                  syncAudioState();
                };

                window.__hzSetPaused = function(p) {
                  window.__hzPaused = !!p;
                  var dv = document.getElementById('directVid');
                  if (window.__hzPaused) {
                    if (dv && !dv.paused) dv.pause();
                    ytCmd('pauseVideo', []);
                  } else {
                    if (window.__hzDirectActive && dv) {
                      dv.play().catch(function(){});
                    } else {
                      ytCmd('playVideo', []);
                      syncAudioState();
                    }
                  }
                };

                window.__hzAttachDirectStream = function(streamUrl) {
                  if (!streamUrl || streamUrl.indexOf('http') !== 0 || window.__hzDirectActive) return;
                  var dv = document.getElementById('directVid');
                  if (!dv) return;
                  dv.src = streamUrl;
                  dv.muted = true;
                  dv.onplaying = function() {
                    window.__hzDirectActive = true;
                    dv.style.display = 'block';
                    dv.muted = !!window.__hzMuted;
                    if (!window.__hzMuted) dv.volume = 1.0;
                    ytCmd('pauseVideo', []);
                  };
                  dv.ontimeupdate = function() {
                    if (dv.currentTime >= 298) {
                      notifyEnded();
                    }
                  };
                  dv.onended = function() {
                    notifyEnded();
                  };
                  dv.onerror = function() {
                    window.__hzDirectActive = false;
                    dv.style.display = 'none';
                    ytCmd('playVideo', []);
                    syncAudioState();
                  };
                  var p = dv.play();
                  if (p && p.catch) {
                    p.catch(function() {
                      dv.muted = true;
                      dv.play().catch(function() {
                        window.__hzDirectActive = false;
                        dv.style.display = 'none';
                      });
                    });
                  }
                };

                window.addEventListener('message', function(ev) {
                  try {
                    var data = typeof ev.data === 'string' ? JSON.parse(ev.data) : ev.data;
                    if (!data) return;
                    if (data.event === 'onReady') {
                      ytCmd('unloadModule', ['captions']);
                      ytCmd('unloadModule', ['cc']);
                      ytCmd('setPlaybackQualityRange', ['hd720', 'hd720']);
                      if (!window.__hzPaused && !window.__hzDirectActive) {
                        ytCmd('playVideo', []);
                        syncAudioState();
                      }
                    } else if (data.event === 'infoDelivery' && data.info) {
                      if (data.info.playerState === 1) {
                        ytCmd('unloadModule', ['captions']);
                        ytCmd('unloadModule', ['cc']);
                        ytCmd('setPlaybackQualityRange', ['hd720', 'hd720']);
                        syncAudioState();
                      } else if (data.info.playerState === 0) {
                        notifyEnded();
                      }
                      if (typeof data.info.currentTime === 'number' && data.info.currentTime >= 298) {
                        notifyEnded();
                      }
                    } else if (data.event === 'onError') {
                      if (!window.__hzDirectActive) {
                        notifyEnded();
                      }
                    }
                  } catch (e) {}
                });

                var f = document.getElementById('ytframe');
                f.addEventListener('load', function() {
                  try {
                    f.contentWindow.postMessage(JSON.stringify({ event: 'listening', id: 1, channel: 'widget' }), '*');
                  } catch (e) {}
                  setTimeout(function() {
                    ytCmd('unloadModule', ['captions']);
                    ytCmd('unloadModule', ['cc']);
                    ytCmd('setPlaybackQualityRange', ['hd720', 'hd720']);
                    if (!window.__hzDirectActive && !window.__hzPaused) {
                      ytCmd('playVideo', []);
                      syncAudioState();
                    }
                  }, 350);
                });

                setInterval(function() {
                  try {
                    if (f && f.contentWindow) {
                      f.contentWindow.postMessage(JSON.stringify({ event: 'listening', id: 1, channel: 'widget' }), '*');
                    }
                    if (!window.__hzDirectActive && !window.__hzPaused) {
                      ytCmd('unloadModule', ['captions']);
                      ytCmd('unloadModule', ['cc']);
                      syncAudioState();
                    }
                  } catch (e) {}
                }, 1200);

                if ('$safeStream'.indexOf('http') === 0) {
                  window.__hzAttachDirectStream('$safeStream');
                }
              </script>
            </body>
            </html>
            """.trimIndent()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            webViewRef?.let { wv ->
                runCatching {
                    wv.stopLoading()
                    wv.loadUrl("about:blank")
                    wv.onPause()
                    wv.pauseTimers()
                    wv.removeAllViews()
                    wv.destroy()
                }
            }
            webViewRef = null
        }
    }

    LaunchedEffect(directStreamUrl, webViewRef) {
        if (directStreamUrl.startsWith("http")) {
            val safe = directStreamUrl.replace("'", "\'")
            webViewRef?.evaluateJavascript(
                "if (window.__hzAttachDirectStream) window.__hzAttachDirectStream('$safe');",
                null
            )
        }
    }

    LaunchedEffect(isMuted, webViewRef) {
        val mStr = if (isMuted) "true" else "false"
        webViewRef?.evaluateJavascript(
            "if (window.__hzSetMuted) window.__hzSetMuted($mStr);",
            null
        )
    }

    LaunchedEffect(isPaused, webViewRef) {
        webViewRef?.let { wv ->
            if (isPaused) {
                wv.evaluateJavascript("if (window.__hzSetPaused) window.__hzSetPaused(true);", null)
                wv.onPause()
                wv.pauseTimers()
            } else {
                wv.onResume()
                wv.resumeTimers()
                wv.evaluateJavascript("if (window.__hzSetPaused) window.__hzSetPaused(false);", null)
            }
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            WebView(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                setBackgroundColor(android.graphics.Color.BLACK)
                isClickable = false
                isFocusable = false
                runCatching {
                    CookieManager.getInstance().setAcceptCookie(true)
                    CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                }
                settings.apply {
                    javaScriptEnabled = true
                    mediaPlaybackRequiresUserGesture = false
                    domStorageEnabled = true
                    databaseEnabled = true
                    allowFileAccess = true
                    loadsImagesAutomatically = true
                    useWideViewPort = true
                    loadWithOverviewMode = true
                    cacheMode = WebSettings.LOAD_DEFAULT
                    mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                    userAgentString = "Mozilla/5.0 (Linux; Android 14; Pixel 8 Pro) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.6478.122 Mobile Safari/537.36"
                }
                addJavascriptInterface(jsBridge, "HorizonZBridge")
                webChromeClient = WebChromeClient()
                webViewClient = object : WebViewClient() {
                    private fun handleInterceptUrl(url: String?): Boolean {
                        if (url == null) return false
                        if (url.contains("__hz_ended") || url.startsWith("horizonz://")) {
                            jsBridge.onEnded()
                            return true
                        }
                        return false
                    }

                    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                        val reqUrl = request?.url?.toString()
                        if (handleInterceptUrl(reqUrl)) return true
                        if (request != null && request.isForMainFrame && reqUrl != null && !reqUrl.startsWith("https://www.minecraft.net")) {
                            return true
                        }
                        return false
                    }

                    @Deprecated("Deprecated in Java")
                    override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                        return handleInterceptUrl(url)
                    }
                }
                jsBridge.resetTrigger()
                tag = videoId
                loadDataWithBaseURL(
                    "https://www.minecraft.net/",
                    buildUnifiedPlayerHtml(videoId, directStreamUrl, isMuted),
                    "text/html",
                    "utf-8",
                    null
                )
                webViewRef = this
            }
        },
        update = { wv ->
            if (wv.tag != videoId) {
                wv.tag = videoId
                jsBridge.resetTrigger()
                wv.loadDataWithBaseURL(
                    "https://www.minecraft.net/",
                    buildUnifiedPlayerHtml(videoId, directStreamUrl, isMuted),
                    "text/html",
                    "utf-8",
                    null
                )
            }
        }
    )
}

@Composable
private fun CleanHomeViewport(
    config: HorizonConfigState,
    onConfigChange: (HorizonConfigState) -> Unit,
    logEnabled: Boolean,
    isHibernated: Boolean
) {
    val context = LocalContext.current
    var customWallpapers by remember { mutableStateOf<List<File>>(emptyList()) }
    LaunchedEffect(Unit) {
        customWallpapers = withContext(Dispatchers.IO) {
            HorizonNativeBridge.getCustomWallpapers(context)
        }
    }
    val totalCount = DASHBOARD_WALLPAPERS.size + customWallpapers.size
    var quoteIndex by remember { mutableIntStateOf((MINECRAFT_SPLASH_QUOTES.indices).random()) }
    var wallpaperIndex by remember { mutableIntStateOf((0 until totalCount.coerceAtLeast(1)).random()) }

    val isVideoMode = config.useYoutubeTrailerWallpaper || config.backgroundMode == HorizonBackgroundMode.YOUTUBE_TRAILER
    var trailerList by remember {
        mutableStateOf(HorizonNativeBridge.OFFICIAL_MINECRAFT_TRAILERS_FALLBACK.shuffled())
    }
    var currentTrailerIndex by remember {
        mutableIntStateOf((HorizonNativeBridge.OFFICIAL_MINECRAFT_TRAILERS_FALLBACK.indices).random())
    }
    var activeDirectStreamUrl by remember { mutableStateOf("") }

    val pickRandomNextTrailer = remember {
        { curIdx: Int, size: Int ->
            if (size <= 1) 0
            else {
                var candidate = (0 until size).random()
                if (candidate == curIdx) {
                    candidate = (curIdx + 1) % size
                }
                candidate
            }
        }
    }

    LaunchedEffect(isVideoMode) {
        if (isVideoMode) {
            val fetched = withContext(Dispatchers.IO) {
                HorizonNativeBridge.fetchLatestMinecraftYoutubeTrailers()
            }
            if (fetched.isNotEmpty()) {
                trailerList = fetched.shuffled()
                currentTrailerIndex = (fetched.indices).random()
            }
        }
    }

    val activeTrailer = trailerList.getOrElse(currentTrailerIndex % trailerList.size.coerceAtLeast(1)) {
        HorizonNativeBridge.OFFICIAL_MINECRAFT_TRAILERS_FALLBACK.first()
    }

    LaunchedEffect(isVideoMode, activeTrailer.videoId) {
        if (isVideoMode) {
            activeDirectStreamUrl = ""
            val resolved = withContext(Dispatchers.IO) {
                HorizonNativeBridge.resolveDirectTrailerStreamUrl(activeTrailer.videoId)
            }
            activeDirectStreamUrl = resolved
        }
    }

    val wallpaperPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            val importedWallpaper: File? = HorizonNativeBridge.importUriToDestination(
                context,
                uri,
                "custom_wallpapers",
                "mc_custom_${System.currentTimeMillis()}.jpg",
                logEnabled
            )
            if (importedWallpaper != null) {
                customWallpapers = HorizonNativeBridge.getCustomWallpapers(context)
                wallpaperIndex = DASHBOARD_WALLPAPERS.size
                if (isVideoMode) {
                    onConfigChange(config.copy(backgroundMode = HorizonBackgroundMode.WALLPAPER, useYoutubeTrailerWallpaper = false))
                }
            }
        }
    }

    LaunchedEffect(totalCount, isHibernated, isVideoMode) {
        while (!isHibernated && !isVideoMode) {
            delay(5000L)
            wallpaperIndex = (wallpaperIndex + 1) % totalCount.coerceAtLeast(1)
        }
    }

    LaunchedEffect(isHibernated, isVideoMode) {
        while (!isHibernated && !isVideoMode) {
            delay(5000L)
            var next = (MINECRAFT_SPLASH_QUOTES.indices).random()
            if (next == quoteIndex) {
                next = (quoteIndex + 1) % MINECRAFT_SPLASH_QUOTES.size
            }
            quoteIndex = next
        }
    }

    val titleScale by animateFloatAsState(
        targetValue = if (isVideoMode) 0.84f else 1.0f,
        animationSpec = tween(durationMillis = 480, easing = FastOutSlowInEasing),
        label = "MinecraftTitleScaleAnim"
    )
    val titleOffsetY by animateDpAsState(
        targetValue = if (isVideoMode) 12.dp else 0.dp,
        animationSpec = tween(durationMillis = 480, easing = FastOutSlowInEasing),
        label = "MinecraftTitleOffsetAnim"
    )

    Surface(
        shape = RoundedCornerShape(22.dp),
        color = CardSlateBg,
        modifier = Modifier.fillMaxSize()
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AnimatedContent(
                targetState = isVideoMode,
                transitionSpec = {
                    fadeIn(animationSpec = tween(520, easing = FastOutSlowInEasing))
                        .togetherWith(fadeOut(animationSpec = tween(420)))
                        .using(sizeTransform = null)
                },
                modifier = Modifier.fillMaxSize(),
                label = "WallpaperVsVideoModeTransition"
            ) { videoActive ->
                if (videoActive && !isHibernated) {
                    MinecraftTrailerVideoWallpaperPlayer(
                        videoId = activeTrailer.videoId,
                        directStreamUrl = activeDirectStreamUrl,
                        trailerTitle = activeTrailer.title,
                        isMuted = config.muteTrailerVideo,
                        isPaused = isHibernated,
                        onVideoEndedOrError = {
                            currentTrailerIndex = pickRandomNextTrailer(currentTrailerIndex, trailerList.size.coerceAtLeast(1))
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                } else if (!isHibernated) {
                    AnimatedContent(
                        targetState = wallpaperIndex % totalCount.coerceAtLeast(1),
                        transitionSpec = {
                            fadeIn(animationSpec = tween(900)).togetherWith(fadeOut(animationSpec = tween(900))).using(sizeTransform = null)
                        },
                        modifier = Modifier.fillMaxSize(),
                        label = "MinecraftWallpaperCarousel"
                    ) { idx ->
                        if (idx >= DASHBOARD_WALLPAPERS.size && (idx - DASHBOARD_WALLPAPERS.size) < customWallpapers.size) {
                            val customFile = customWallpapers[idx - DASHBOARD_WALLPAPERS.size]
                            val bmp = remember(customFile.absolutePath) {
                                runCatching { BitmapFactory.decodeFile(customFile.absolutePath)?.asImageBitmap() }.getOrNull()
                            }
                            if (bmp != null) {
                                Image(
                                    bitmap = bmp,
                                    contentDescription = "Custom Minecraft Wallpaper",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Image(
                                    painter = painterResource(id = DASHBOARD_WALLPAPERS[0]),
                                    contentDescription = "Minecraft Wallpaper",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        } else {
                            val resId = DASHBOARD_WALLPAPERS[idx % DASHBOARD_WALLPAPERS.size]
                            Image(
                                painter = painterResource(id = resId),
                                contentDescription = "Minecraft Wallpaper",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = if (isVideoMode) 0.04f else 0.08f),
                                Color.Black.copy(alpha = if (isVideoMode) 0.56f else 0.68f)
                            )
                        )
                    )
            )

            var isDashboardMenuExpanded by remember { mutableStateOf(false) }

            // Animated 3-Dots Corner Controller (minimizes buttons into a sleek 3-dots pill!)
            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AnimatedVisibility(
                    visible = isDashboardMenuExpanded,
                    enter = fadeIn(tween(250)) + expandHorizontally(tween(250)),
                    exit = fadeOut(tween(180)) + shrinkHorizontally(tween(200))
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isVideoMode) {
                            DashboardQuickIconPill(
                                icon = if (config.muteTrailerVideo) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                                label = if (config.muteTrailerVideo) "Silent" else "Sound On",
                                onClick = {
                                    onConfigChange(config.copy(muteTrailerVideo = !config.muteTrailerVideo))
                                }
                            )
                            DashboardQuickIconPill(
                                icon = Icons.Default.Shuffle,
                                label = "Random Trailer",
                                onClick = {
                                    currentTrailerIndex = pickRandomNextTrailer(currentTrailerIndex, trailerList.size.coerceAtLeast(1))
                                }
                            )
                        } else {
                            DashboardQuickIconPill(
                                icon = Icons.Default.Wallpaper,
                                label = "Ganti Wallpaper",
                                onClick = { wallpaperPicker.launch("image/*") }
                            )
                        }

                        DashboardQuickIconPill(
                            icon = if (isVideoMode) Icons.Default.Image else Icons.Default.PlayCircleFilled,
                            label = if (isVideoMode) "Mode Wallpaper" else "Play Trailer",
                            onClick = {
                                val nextVideo = !isVideoMode
                                onConfigChange(
                                    config.copy(
                                        backgroundMode = if (nextVideo) HorizonBackgroundMode.YOUTUBE_TRAILER else HorizonBackgroundMode.WALLPAPER,
                                        useYoutubeTrailerWallpaper = nextVideo
                                    )
                                )
                            }
                        )
                    }
                }

                // Sleek Minimalist 3-Dots Menu Pill (always accessible, zero video obstruction!)
                Surface(
                    shape = CircleShape,
                    color = if (isDashboardMenuExpanded) SubRailCapsuleActive else Color(0xFF14181B).copy(alpha = 0.78f),
                    modifier = Modifier
                        .size(36.dp)
                        .animatedPressScale { isDashboardMenuExpanded = !isDashboardMenuExpanded }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isDashboardMenuExpanded) Icons.Default.Close else Icons.Default.MoreVert,
                            contentDescription = "Menu Pengaturan Video & Wallpaper",
                            tint = if (isDashboardMenuExpanded) SkyBlueAccent else Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier
                        .offset(y = titleOffsetY)
                        .graphicsLayer {
                            scaleX = titleScale
                            scaleY = titleScale
                            transformOrigin = TransformOrigin(0f, 1f)
                        },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MinecraftGrassCubeIcon(size = 40.dp)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(9.dp)
                    ) {
                        Text(
                            text = "Minecraft",
                            fontSize = 30.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                        MinecraftPixelRustBadge()
                        LavenderTagBadge(text = "26.3")
                    }
                }

                AnimatedVisibility(
                    visible = !isVideoMode,
                    enter = fadeIn(tween(420, easing = FastOutSlowInEasing)) +
                        expandVertically(tween(420, easing = FastOutSlowInEasing)) +
                        slideInVertically(tween(420, easing = FastOutSlowInEasing)) { h -> h / 2 },
                    exit = fadeOut(tween(280)) +
                        shrinkVertically(tween(340, easing = FastOutSlowInEasing)) +
                        slideOutVertically(tween(280)) { h -> h / 2 }
                ) {
                    AnimatedContent(
                        targetState = MINECRAFT_SPLASH_QUOTES[quoteIndex],
                        transitionSpec = {
                            (slideInVertically(tween(340)) { h -> h / 2 } + fadeIn(tween(340)))
                                .togetherWith(
                                    slideOutVertically(tween(260)) { h -> -h / 2 } + fadeOut(tween(240))
                                )
                                .using(sizeTransform = null)
                        },
                        modifier = Modifier
                            .fillMaxWidth(0.92f)
                            .animatedPressScale {
                                if (!isVideoMode) {
                                    quoteIndex = (quoteIndex + 1) % MINECRAFT_SPLASH_QUOTES.size
                                    wallpaperIndex = (wallpaperIndex + 1) % totalCount.coerceAtLeast(1)
                                }
                            },
                        label = "MinecraftSplashQuote"
                    ) { quote ->
                        Text(
                            text = "“$quote”",
                            fontSize = 13.sp,
                            fontStyle = FontStyle.Italic,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFFFE082)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DashboardQuickIconPill(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(50),
        color = Color(0xFF181E22).copy(alpha = 0.76f),
        modifier = Modifier.animatedPressScale(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = SkyBlueAccent,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White
            )
        }
    }
}

/**
 * Full-Viewport Account Profile & Skin Studio (`ZalithNavTab.ACCOUNTS`):
 * - Clean, well-proportioned cards with zero vertical badge wrapping (`In-Game Ready` stays on 1 line!).
 * - Uses hoisted `onLaunchSkinPicker` so Google Photos selection always succeeds and updates immediately!
 */
@Composable
private fun CleanAccountsAndSkinsViewport(
    accounts: List<ZalithAccount>,
    activeAccount: ZalithAccount,
    activeSkinBitmap: Bitmap,
    config: HorizonConfigState,
    detectionBanner: String?,
    customSkinNameInput: String,
    onCustomSkinNameChange: (String) -> Unit,
    skinReloadCounter: Int,
    onLaunchSkinPicker: () -> Unit,
    onUpdateBanner: (String?) -> Unit,
    onSkinUpdated: () -> Unit,
    onAccountsChange: (List<ZalithAccount>) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var selectedSubTab by remember { mutableStateOf(AccountSubCategory.SKIN_STUDIO) }
    var is3DPreview by remember { mutableStateOf(true) }
    var autoRotate3D by remember { mutableStateOf(true) }
    var walkAnim3D by remember { mutableStateOf(true) }
    var showBackView2D by remember { mutableStateOf(false) }
    var newAccountName by remember { mutableStateOf("") }
    var selectedAccountType by remember { mutableStateOf(AccountType.OFFLINE) }
    var elyByPassword by remember { mutableStateOf("") }
    var elyByTotpToken by remember { mutableStateOf("") }
    var elyByStatusMessage by remember { mutableStateOf<String?>(null) }

    var savedSkinLibrary by remember { mutableStateOf<List<SavedCustomSkinItem>>(emptyList()) }
    var renameSkinTarget by remember { mutableStateOf<SavedCustomSkinItem?>(null) }
    var renameSkinInput by remember { mutableStateOf("") }

    LaunchedEffect(skinReloadCounter, activeAccount.skinPath) {
        savedSkinLibrary = withContext(Dispatchers.IO) {
            HorizonNativeBridge.loadSavedSkinLibrary(context)
        }
    }

    renameSkinTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { renameSkinTarget = null },
            containerColor = Color(0xFF1F262B),
            title = {
                Text("Atur Nama Skin", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Ubah nama file skin (kosongkan untuk otomatis default.png, default1.png, dst.):",
                        fontSize = 12.sp,
                        color = MutedSubtitleText
                    )
                    OutlinedTextField(
                        value = renameSkinInput,
                        onValueChange = { renameSkinInput = it },
                        singleLine = true,
                        label = { Text("Nama Skin (.png)", fontSize = 11.sp) }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val updated = HorizonNativeBridge.renameSavedCustomSkin(context, target, renameSkinInput)
                        if (updated != null) {
                            savedSkinLibrary = HorizonNativeBridge.loadSavedSkinLibrary(context)
                            if (activeAccount.skinPath == target.filePath) {
                                val updatedAccounts = accounts.map { acc ->
                                    if (acc.id == activeAccount.id) {
                                        acc.copy(skinPath = updated.filePath, skinSourceLabel = updated.fileName)
                                    } else acc
                                }
                                onAccountsChange(updatedAccounts)
                            }
                            onSkinUpdated()
                            onUpdateBanner("Nama skin berhasil diubah menjadi '${updated.fileName}'!")
                        }
                        renameSkinTarget = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SkyBlueAccent)
                ) {
                    Text("Simpan", color = SkyBlueDarkText, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { renameSkinTarget = null }) {
                    Text("Batal", color = Color.White)
                }
            }
        )
    }

    Row(
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ZalithSubCategoryRail(
            items = AccountSubCategory.entries,
            selectedItem = selectedSubTab,
            itemLabel = { it.label },
            itemIcon = { it.icon },
            onSelect = { selectedSubTab = it }
        )

        AnimatedContent(
            targetState = selectedSubTab,
            transitionSpec = {
                (fadeIn(tween(200, easing = FastOutSlowInEasing)) +
                    slideInVertically(tween(200, easing = FastOutSlowInEasing)) { h -> h / 14 })
                    .togetherWith(fadeOut(tween(150)))
                    .using(sizeTransform = null)
            },
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            label = "AccountSubTabTransition"
        ) { subTab ->
            when (subTab) {
                AccountSubCategory.SKIN_STUDIO -> {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(22.dp),
                            color = CardSlateBg,
                            modifier = Modifier
                                .width(206.dp)
                                .fillMaxHeight()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(7.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        MinecraftSkinHeadAvatar(
                                            skinBitmap = activeSkinBitmap,
                                            size = 26.dp,
                                            cornerRadius = 6.dp
                                        )
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = activeAccount.username,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = if (activeAccount.slimSkinModel) "Slim (3px)" else "Wide (4px)",
                                                fontSize = 10.sp,
                                                color = SkyBlueAccent,
                                                maxLines = 1
                                            )
                                        }
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(50),
                                        color = SkyBlueAccent,
                                        modifier = Modifier.animatedPressScale { is3DPreview = !is3DPreview }
                                    ) {
                                        Text(
                                            text = if (is3DPreview) "3D Preview" else "2D View",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = SkyBlueDarkText,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth()
                                        .padding(vertical = 6.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(
                                            Brush.verticalGradient(
                                                listOf(Color(0xFF171E24), Color(0xFF101519))
                                            )
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    AnimatedContent(
                                        targetState = is3DPreview,
                                        transitionSpec = {
                                            fadeIn(tween(260)).togetherWith(fadeOut(tween(200))).using(sizeTransform = null)
                                        },
                                        modifier = Modifier.fillMaxSize(),
                                        label = "Skin3Dvs2DTransition"
                                    ) { mode3d ->
                                        if (mode3d) {
                                            MinecraftSkin3DPreviewCanvas(
                                                skinBitmap = activeSkinBitmap,
                                                slimArms = activeAccount.slimSkinModel,
                                                autoRotate = autoRotate3D,
                                                walkAnimation = walkAnim3D,
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .padding(6.dp)
                                            )
                                        } else {
                                            MinecraftSkinFullBodyCanvas(
                                                skinBitmap = activeSkinBitmap,
                                                slimArms = activeAccount.slimSkinModel,
                                                showBack = showBackView2D,
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .padding(8.dp)
                                                    .clickable { showBackView2D = !showBackView2D }
                                            )
                                        }
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (is3DPreview) {
                                        Surface(
                                            shape = RoundedCornerShape(50),
                                            color = if (autoRotate3D) SubRailCapsuleActive else CardRowBg,
                                            modifier = Modifier.animatedPressScale { autoRotate3D = !autoRotate3D }
                                        ) {
                                            Text(
                                                text = if (autoRotate3D) "Spin: ON" else "Spin: OFF",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = if (autoRotate3D) SkyBlueAccent else MutedSubtitleText,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                            )
                                        }
                                        Surface(
                                            shape = RoundedCornerShape(50),
                                            color = if (walkAnim3D) SubRailCapsuleActive else CardRowBg,
                                            modifier = Modifier.animatedPressScale { walkAnim3D = !walkAnim3D }
                                        ) {
                                            Text(
                                                text = if (walkAnim3D) "Walk: ON" else "Walk: OFF",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = if (walkAnim3D) SkyBlueAccent else MutedSubtitleText,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                            )
                                        }
                                    } else {
                                        Surface(
                                            shape = RoundedCornerShape(50),
                                            color = SubRailCapsuleActive,
                                            modifier = Modifier.animatedPressScale { showBackView2D = !showBackView2D }
                                        ) {
                                            Text(
                                                text = if (showBackView2D) "Tampak Belakang" else "Tampak Depan",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = SkyBlueAccent,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        text = activeAccount.skinSourceLabel,
                                        fontSize = 9.sp,
                                        color = MutedSubtitleText,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(18.dp),
                                color = CardSlateBg,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "Import Skin & Simpan ke Pilih Skin",
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "Atur nama skin sendiri atau kosongkan agar otomatis diberi nama default.png, default1.png, default2.png, dst.",
                                                fontSize = 11.sp,
                                                color = MutedSubtitleText,
                                                maxLines = 2
                                            )
                                        }

                                        Spacer(Modifier.width(10.dp))

                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = SkyBlueAccent,
                                            modifier = Modifier.animatedPressScale(onClick = onLaunchSkinPicker)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.AddPhotoAlternate,
                                                    contentDescription = "Pilih Foto / Skin",
                                                    tint = SkyBlueDarkText,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Text(
                                                    text = "Import Skin",
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = SkyBlueDarkText,
                                                    maxLines = 1
                                                )
                                            }
                                        }
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = Color(0xFF1A2024),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(38.dp)
                                            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(horizontal = 12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Edit,
                                                contentDescription = null,
                                                tint = SkyBlueAccent,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Box(
                                                modifier = Modifier.weight(1f),
                                                contentAlignment = Alignment.CenterStart
                                            ) {
                                                if (customSkinNameInput.isEmpty()) {
                                                    Text(
                                                        text = "Nama skin (opsional — kosongkan = default.png, default1.png, default2.png...)",
                                                        fontSize = 11.sp,
                                                        color = MutedSubtitleText,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                                BasicTextField(
                                                    value = customSkinNameInput,
                                                    onValueChange = onCustomSkinNameChange,
                                                    singleLine = true,
                                                    textStyle = TextStyle(color = Color.White, fontSize = 12.sp),
                                                    cursorBrush = SolidColor(SkyBlueAccent),
                                                    modifier = Modifier.fillMaxWidth()
                                                )
                                            }
                                            if (customSkinNameInput.isNotEmpty()) {
                                                Icon(
                                                    imageVector = Icons.Default.Close,
                                                    contentDescription = "Clear",
                                                    tint = MutedSubtitleText,
                                                    modifier = Modifier
                                                        .size(14.dp)
                                                        .clickable { onCustomSkinNameChange("") }
                                                )
                                            }
                                        }
                                    }

                                    detectionBanner?.let { bannerMsg ->
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = SubRailCapsuleActive,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.Verified,
                                                    contentDescription = null,
                                                    tint = SkyBlueAccent,
                                                    modifier = Modifier.size(15.dp)
                                                )
                                                Text(
                                                    text = bannerMsg,
                                                    fontSize = 11.sp,
                                                    color = Color.White,
                                                    modifier = Modifier.weight(1f)
                                                )
                                            }
                                        }
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Model Lengan:",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = Color.White
                                        )
                                        listOf(false to "Wide (4px Steve)", true to "Slim (3px Alex)").forEach { (isSlim, label) ->
                                            val selected = activeAccount.slimSkinModel == isSlim
                                            Surface(
                                                shape = RoundedCornerShape(50),
                                                color = if (selected) SkyBlueAccent else CardRowBg,
                                                modifier = Modifier.animatedPressScale {
                                                    val updatedList = accounts.map { acc ->
                                                        if (acc.id == activeAccount.id) acc.copy(slimSkinModel = isSlim) else acc
                                                    }
                                                    onAccountsChange(updatedList)
                                                    onSkinUpdated()
                                                }
                                            ) {
                                                Text(
                                                    text = label,
                                                    fontSize = 11.sp,
                                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                                    color = if (selected) SkyBlueDarkText else Color.White,
                                                    maxLines = 1,
                                                    modifier = Modifier.padding(horizontal = 11.dp, vertical = 5.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(18.dp),
                                color = CardSlateBg,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(7.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Pilih Skin (Tersimpan & Preset Siap Pakai)",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                        LavenderTagBadge(text = "${savedSkinLibrary.size} Imported")
                                    }

                                    if (savedSkinLibrary.isNotEmpty()) {
                                        savedSkinLibrary.forEach { savedItem ->
                                            val itemBmp = remember(savedItem.filePath, savedItem.addedAt) {
                                                BitmapFactory.decodeFile(savedItem.filePath)
                                                    ?: HorizonNativeBridge.generatePresetSkinBitmap(BuiltInSkinPreset.CLASSIC_STEVE, context)
                                            }
                                            val isCurrent = activeAccount.skinPath == savedItem.filePath ||
                                                activeAccount.skinSourceLabel == savedItem.fileName
                                            Surface(
                                                shape = RoundedCornerShape(14.dp),
                                                color = if (isCurrent) SubRailCapsuleActive else CardRowBg,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .animatedPressScale {
                                                        val updatedAcc = HorizonNativeBridge.applySavedCustomSkin(
                                                            context,
                                                            savedItem,
                                                            activeAccount,
                                                            config
                                                        )
                                                        val updatedList = accounts.map { if (it.id == activeAccount.id) updatedAcc else it }
                                                        onAccountsChange(updatedList)
                                                        onSkinUpdated()
                                                        onUpdateBanner("Skin '${savedItem.fileName}' aktif & siap digunakan di in-game!")
                                                    }
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                                        modifier = Modifier.weight(1f)
                                                    ) {
                                                        MinecraftSkinHeadAvatar(
                                                            skinBitmap = itemBmp,
                                                            size = 32.dp,
                                                            cornerRadius = 7.dp
                                                        )
                                                        Column(modifier = Modifier.weight(1f)) {
                                                            Text(
                                                                text = savedItem.fileName,
                                                                fontSize = 12.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color = Color.White,
                                                                maxLines = 1,
                                                                overflow = TextOverflow.Ellipsis
                                                            )
                                                            Text(
                                                                text = "${savedItem.sourceLabel} • ${if (savedItem.slimModel) "Slim (3px)" else "Wide (4px)"}",
                                                                fontSize = 10.sp,
                                                                color = SkyBlueAccent,
                                                                maxLines = 1,
                                                                overflow = TextOverflow.Ellipsis
                                                            )
                                                        }
                                                    }

                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Edit,
                                                            contentDescription = "Ubah Nama Skin",
                                                            tint = Color(0xFFD5DEE5),
                                                            modifier = Modifier
                                                                .size(17.dp)
                                                                .clickable {
                                                                    renameSkinInput = savedItem.fileName.removeSuffix(".png")
                                                                    renameSkinTarget = savedItem
                                                                }
                                                        )
                                                        Icon(
                                                            imageVector = Icons.Outlined.Delete,
                                                            contentDescription = "Hapus Skin",
                                                            tint = Color(0xFFFF8A80),
                                                            modifier = Modifier
                                                                .size(17.dp)
                                                                .clickable {
                                                                    HorizonNativeBridge.deleteSavedCustomSkin(context, savedItem)
                                                                    savedSkinLibrary = HorizonNativeBridge.loadSavedSkinLibrary(context)
                                                                }
                                                        )
                                                        Surface(
                                                            shape = RoundedCornerShape(9.dp),
                                                            color = if (isCurrent) SkyBlueAccent else Color(0xFF344149)
                                                        ) {
                                                            Text(
                                                                text = if (isCurrent) "Active" else "Pakai",
                                                                fontSize = 11.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color = if (isCurrent) SkyBlueDarkText else Color.White,
                                                                maxLines = 1,
                                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    BuiltInSkinPreset.entries.forEach { preset ->
                                        val presetBmp = remember(preset) { HorizonNativeBridge.generatePresetSkinBitmap(preset, context) }
                                        val isCurrent = activeAccount.skinSourceLabel == "Preset: ${preset.title}"
                                        Surface(
                                            shape = RoundedCornerShape(14.dp),
                                            color = if (isCurrent) SubRailCapsuleActive else CardRowBg,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .animatedPressScale {
                                                    val updatedAcc = HorizonNativeBridge.applyBuiltInPresetSkin(
                                                        context,
                                                        preset,
                                                        activeAccount,
                                                        config
                                                    )
                                                    val updatedList = accounts.map { if (it.id == activeAccount.id) updatedAcc else it }
                                                    onAccountsChange(updatedList)
                                                    onSkinUpdated()
                                                    onUpdateBanner("Skin '${preset.title}' aktif & siap digunakan di in-game!")
                                                    Toast.makeText(context, "Skin '${preset.title}' terpasang!", Toast.LENGTH_SHORT).show()
                                                }
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    MinecraftSkinHeadAvatar(
                                                        skinBitmap = presetBmp,
                                                        size = 32.dp,
                                                        cornerRadius = 7.dp
                                                    )
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(
                                                            text = preset.title,
                                                            fontSize = 12.sp,
                                                            fontWeight = FontWeight.SemiBold,
                                                            color = Color.White,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                        Text(
                                                            text = preset.subtitle,
                                                            fontSize = 10.sp,
                                                            color = MutedSubtitleText,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                    }
                                                }

                                                Spacer(Modifier.width(8.dp))

                                                Surface(
                                                    shape = RoundedCornerShape(9.dp),
                                                    color = if (isCurrent) SkyBlueAccent else Color(0xFF344149)
                                                ) {
                                                    Text(
                                                        text = if (isCurrent) "Active" else "Pakai",
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (isCurrent) SkyBlueDarkText else Color.White,
                                                        maxLines = 1,
                                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                AccountSubCategory.PROFILES -> {
                    Surface(
                        shape = RoundedCornerShape(22.dp),
                        color = CardSlateBg,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(14.dp)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "Player Accounts & Microsoft Sign-In",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )

                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = CardRowBg,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(50),
                                        color = Color(0xFF1E2428),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(36.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(horizontal = 14.dp),
                                            contentAlignment = Alignment.CenterStart
                                        ) {
                                            if (newAccountName.isEmpty()) {
                                                Text("Enter Player Name / Gamertag...", fontSize = 12.sp, color = MutedSubtitleText)
                                            }
                                            BasicTextField(
                                                value = newAccountName,
                                                onValueChange = { newAccountName = it },
                                                singleLine = true,
                                                textStyle = TextStyle(color = Color.White, fontSize = 12.sp),
                                                cursorBrush = SolidColor(SkyBlueAccent),
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        }
                                    }

                                    AccountType.entries.forEach { t ->
                                        val isSelected = selectedAccountType == t
                                        Surface(
                                            shape = RoundedCornerShape(50),
                                            color = if (isSelected) SubRailCapsuleActive else Color(0xFF1E2428),
                                            modifier = Modifier.animatedPressScale { selectedAccountType = t }
                                        ) {
                                            Text(
                                                text = t.badge,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = if (isSelected) SkyBlueAccent else MutedSubtitleText,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                            )
                                        }
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = SkyBlueAccent,
                                        modifier = Modifier.animatedPressScale {
                                            if (selectedAccountType == AccountType.ELY_BY) {
                                                elyByStatusMessage = "Use Authenticate Ely.by below with your password to sign in."
                                            } else if (newAccountName.isNotBlank()) {
                                                val added = ZalithAccount(
                                                    id = UUID.randomUUID().toString(),
                                                    username = newAccountName.trim(),
                                                    uuid = UUID.randomUUID().toString(),
                                                    accountType = selectedAccountType,
                                                    isActive = true
                                                )
                                                val updated = accounts.map { it.copy(isActive = false) } + added
                                                onAccountsChange(updated)
                                                newAccountName = ""
                                            }
                                        }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(Icons.Default.PersonAdd, contentDescription = null, tint = SkyBlueDarkText, modifier = Modifier.size(15.dp))
                                            Text("Save Profile", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SkyBlueDarkText)
                                        }
                                    }
                                }
                            }

                            if (selectedAccountType == AccountType.ELY_BY) {
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = CardRowBg,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = "Ely.by Account Sign-In (Password & 2FA are never saved to disk)",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = SkyBlueAccent
                                        )
                                        OutlinedTextField(
                                            value = elyByPassword,
                                            onValueChange = { elyByPassword = it },
                                            singleLine = true,
                                            label = { Text("Ely.by Password", fontSize = 11.sp) },
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                        OutlinedTextField(
                                            value = elyByTotpToken,
                                            onValueChange = { elyByTotpToken = it.filter { ch -> ch.isDigit() }.take(6) },
                                            singleLine = true,
                                            label = { Text("Optional 6-Digit 2FA Code", fontSize = 11.sp) },
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Button(
                                                onClick = {
                                                    if (newAccountName.isNotBlank() && elyByPassword.isNotBlank()) {
                                                        val loginCopy = newAccountName.trim()
                                                        val passCopy = elyByPassword
                                                        val totpCopy = elyByTotpToken.trim()
                                                        elyByPassword = ""
                                                        elyByTotpToken = ""
                                                        elyByStatusMessage = "Authenticating with Ely.by..."
                                                        scope.launch {
                                                            val (authOk, authedAcc) = withContext(Dispatchers.IO) {
                                                                HorizonNativeBridge.authenticateElyByAccount(
                                                                    context = context,
                                                                    usernameOrEmail = loginCopy,
                                                                    passwordRaw = passCopy,
                                                                    totpCodeRaw = totpCopy
                                                                )
                                                            }
                                                            if (authOk && authedAcc != null) {
                                                                val updated = accounts.map { it.copy(isActive = false) } + authedAcc
                                                                onAccountsChange(updated)
                                                                newAccountName = ""
                                                                elyByStatusMessage = "Ely.by account ${authedAcc.username} ready."
                                                            } else {
                                                                elyByStatusMessage = "Ely.by sign-in failed. Verify credentials or 2FA code."
                                                            }
                                                        }
                                                    }
                                                }
                                            ) {
                                                Text("Authenticate Ely.by", fontSize = 11.sp)
                                            }
                                            elyByStatusMessage?.let { msg ->
                                                Text(text = msg, fontSize = 11.sp, color = MutedSubtitleText)
                                            }
                                        }
                                    }
                                }
                            }

                            if (selectedAccountType == AccountType.MICROSOFT) {
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = SubRailCapsuleActive,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .animatedPressScale {
                                            runCatching {
                                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.microsoft.com/link")))
                                            }
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(Icons.Default.OpenInBrowser, contentDescription = null, tint = SkyBlueAccent, modifier = Modifier.size(17.dp))
                                        Text(
                                            text = "Open Microsoft Device Link (microsoft.com/link) to authorize Xbox Live Gamertag",
                                            fontSize = 12.sp,
                                            color = Color.White
                                        )
                                    }
                                }
                            }

                            accounts.forEach { acc ->
                                val accSkinBmp = remember(acc.id, acc.skinPath, acc.slimSkinModel) {
                                    HorizonNativeBridge.loadOrGenerateAccountSkinBitmap(context, acc)
                                }
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = if (acc.isActive) SubRailCapsuleActive else CardRowBg,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .animatedPressScale {
                                            val updated = accounts.map { it.copy(isActive = it.id == acc.id) }
                                            onAccountsChange(updated)
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            MinecraftSkinHeadAvatar(
                                                skinBitmap = accSkinBmp,
                                                size = 38.dp,
                                                cornerRadius = 9.dp
                                            )
                                            Column(modifier = Modifier.weight(1f)) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    Text(
                                                        text = acc.username,
                                                        fontSize = 14.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color.White,
                                                        maxLines = 1
                                                    )
                                                    LavenderTagBadge(text = acc.accountType.badge)
                                                    if (acc.isActive) {
                                                        LavenderTagBadge(text = "Active")
                                                    }
                                                }
                                                Text(
                                                    text = "Skin: ${acc.skinSourceLabel}",
                                                    fontSize = 11.sp,
                                                    color = MutedSubtitleText,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }

                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Surface(
                                                shape = RoundedCornerShape(10.dp),
                                                color = CardSlateBg,
                                                modifier = Modifier.animatedPressScale {
                                                    val updated = accounts.map { it.copy(isActive = it.id == acc.id) }
                                                    onAccountsChange(updated)
                                                    selectedSubTab = AccountSubCategory.SKIN_STUDIO
                                                }
                                            ) {
                                                Text(
                                                    text = "Edit Skin",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = SkyBlueAccent,
                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                                )
                                            }

                                            if (accounts.size > 1) {
                                                Icon(
                                                    imageVector = Icons.Outlined.Delete,
                                                    contentDescription = "Delete Account",
                                                    tint = Color(0xFFFF8A80),
                                                    modifier = Modifier
                                                        .size(20.dp)
                                                        .animatedPressScale {
                                                            val remaining = accounts.filterNot { it.id == acc.id }
                                                            val fixed = if (remaining.none { it.isActive }) {
                                                                remaining.mapIndexed { idx, item -> item.copy(isActive = idx == 0) }
                                                            } else remaining
                                                            onAccountsChange(fixed)
                                                        }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CleanLogsViewport(
    logEnabled: Boolean
) {
    val context = LocalContext.current
    var selectedCategory by remember { mutableStateOf(LogCategoryFilter.ALL) }
    var selectedSeverity by remember { mutableStateOf(LogSeverityFilter.ALL) }
    var searchQuery by remember { mutableStateOf("") }
    var refreshTick by remember { mutableIntStateOf(0) }
    var logEntries by remember {
        mutableStateOf(HorizonNativeBridge.readStructuredLogs(context, logEnabled, LogCategoryFilter.ALL))
    }

    LaunchedEffect(logEnabled, selectedCategory, refreshTick) {
        while (true) {
            logEntries = HorizonNativeBridge.readStructuredLogs(context, logEnabled, selectedCategory)
            delay(1000L)
        }
    }

    val filteredEntries = remember(logEntries, selectedSeverity, searchQuery) {
        logEntries.filter { entry ->
            val matchSev = selectedSeverity == LogSeverityFilter.ALL || entry.severity == selectedSeverity
            val matchQuery = searchQuery.isBlank() ||
                entry.message.contains(searchQuery, ignoreCase = true) ||
                entry.sourceTag.contains(searchQuery, ignoreCase = true)
            matchSev && matchQuery
        }
    }

    Row(
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ZalithSubCategoryRail(
            items = LogCategoryFilter.entries,
            selectedItem = selectedCategory,
            itemLabel = { it.label },
            itemIcon = {
                when (it) {
                    LogCategoryFilter.ALL -> Icons.Default.Terminal
                    LogCategoryFilter.LAUNCHER -> Icons.Default.Dashboard
                    LogCategoryFilter.DOWNLOADER -> Icons.Default.CloudDownload
                    LogCategoryFilter.ENGINE -> Icons.Default.SportsEsports
                }
            },
            onSelect = { selectedCategory = it }
        )

        Surface(
            shape = RoundedCornerShape(22.dp),
            color = CardSlateBg,
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Top Search & Action Toolbar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = CardRowBg,
                        modifier = Modifier
                            .weight(1f)
                            .height(34.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 14.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            if (searchQuery.isEmpty()) {
                                Text("Search logs (URL, speed, wgpu, skin)...", fontSize = 12.sp, color = MutedSubtitleText)
                            }
                            BasicTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                singleLine = true,
                                textStyle = TextStyle(color = Color.White, fontSize = 12.sp),
                                cursorBrush = SolidColor(SkyBlueAccent),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    LogSeverityFilter.entries.forEach { sev ->
                        val selected = selectedSeverity == sev
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = if (selected) SubRailCapsuleActive else CardRowBg,
                            modifier = Modifier.animatedPressScale { selectedSeverity = sev }
                        ) {
                            Text(
                                text = sev.label,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (selected) SkyBlueAccent else MutedSubtitleText,
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = SkyBlueAccent,
                        modifier = Modifier.animatedPressScale {
                            val raw = filteredEntries.joinToString("\n") { "[${it.timestamp}] [${it.sourceTag}/${it.severity.label}] ${it.message}" }
                            HorizonNativeBridge.copyToClipboard(context, "HorizonZ Logs", raw)
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = SkyBlueDarkText, modifier = Modifier.size(14.dp))
                            Text("Copy", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SkyBlueDarkText)
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF3A2326),
                        modifier = Modifier.animatedPressScale {
                            HorizonNativeBridge.clearLogs(context)
                            refreshTick += 1
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Default.DeleteSweep, contentDescription = "Clear", tint = Color(0xFFFF8A80), modifier = Modifier.size(14.dp))
                            Text("Clear", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFF8A80))
                        }
                    }
                }

                // Log Console List
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF12171B))
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(filteredEntries, key = { it.id }) { entry ->
                        val badgeColor = when (entry.severity) {
                            LogSeverityFilter.ERR -> Color(0xFFFF5252)
                            LogSeverityFilter.WARN -> Color(0xFFFFB74D)
                            LogSeverityFilter.DL -> Color(0xFF66BB6A)
                            else -> SkyBlueAccent
                        }
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF1A2126),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = entry.timestamp,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp,
                                    color = MutedSubtitleText
                                )
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = badgeColor.copy(alpha = 0.18f)
                                ) {
                                    Text(
                                        text = "${entry.sourceTag} • ${entry.severity.label}",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = badgeColor,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                Text(
                                    text = entry.message,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    color = Color(0xFFE1E8ED),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CleanVersionsViewport(
    context: android.content.Context,
    installStatus: AssetInstallStatus,
    onStartDownload: () -> Unit,
    onPauseDownload: () -> Unit,
    onResumeDownload: () -> Unit,
    onCancelDownload: () -> Unit,
    onSmartRepairAssets: () -> Unit
) {
    var logExpanded by remember { mutableStateOf(false) }
    var showCancelConfirmDialog by remember { mutableStateOf(false) }
    var showRepairConfirmDialog by remember { mutableStateOf(false) }
    var showDeleteAllConfirmDialog by remember { mutableStateOf(false) }
    var isRepairRunning by remember { mutableStateOf(false) }
    var repairResultPopupMessage by remember { mutableStateOf<String?>(null) }
    var deleteResultPopupMessage by remember { mutableStateOf<String?>(null) }
    var sizeRefreshTick by remember { mutableIntStateOf(0) }
    var installedSizeText by remember { mutableStateOf("Checking...") }

    LaunchedEffect(installStatus.ready, installStatus.running, sizeRefreshTick) {
        installedSizeText = withContext(Dispatchers.IO) {
            HorizonNativeBridge.getInstalledAssetsDiskSizeFormatted(context)
        }
    }

    val chevronRotation by animateFloatAsState(
        targetValue = if (logExpanded) 180f else 0f,
        animationSpec = tween(250),
        label = "ChevronRotation"
    )
    val animatedProgress by animateFloatAsState(
        targetValue = installStatus.progressFraction,
        animationSpec = tween(300),
        label = "DownloadProgress"
    )

    if (showCancelConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showCancelConfirmDialog = false },
            containerColor = Color(0xFF1F262B),
            icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFFFB74D)) },
            title = { Text("Batalkan Download?", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White) },
            text = {
                Text(
                    "Proses unduhan aset Minecraft 26.3 yang sedang berjalan akan dihentikan dan file sementara (.partial) akan dibersihkan.",
                    fontSize = 13.sp,
                    color = Color(0xFFD5DEE5)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showCancelConfirmDialog = false
                        onCancelDownload()
                        sizeRefreshTick += 1
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935))
                ) {
                    Text("Ya, Batalkan", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCancelConfirmDialog = false }) {
                    Text("Lanjutkan", fontSize = 12.sp, color = SkyBlueAccent)
                }
            }
        )
    }

    if (showRepairConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showRepairConfirmDialog = false },
            containerColor = Color(0xFF1F262B),
            icon = { Icon(Icons.Default.Build, contentDescription = null, tint = SkyBlueAccent) },
            title = { Text("Smart Repair Aset Minecraft 26.3", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White) },
            text = {
                Text(
                    "Sistem akan memeriksa kelengkapan file engine, tabel worldgen 26.3, audio, dan 60 ikon kontrol sentuh.\n\nFile yang rusak atau hilang akan dipulihkan otomatis tanpa menghapus data dunia (Saves) atau Mod Anda.",
                    fontSize = 13.sp,
                    color = Color(0xFFD5DEE5),
                    lineHeight = 18.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showRepairConfirmDialog = false
                        isRepairRunning = true
                        HorizonNativeBridge.smartRepairAssets(context, true) { _, resultMsg ->
                            isRepairRunning = false
                            sizeRefreshTick += 1
                            repairResultPopupMessage = resultMsg
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SkyBlueAccent)
                ) {
                    Text("Mulai Repair Sekarang", color = SkyBlueDarkText, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRepairConfirmDialog = false }) {
                    Text("Batal", fontSize = 12.sp, color = MutedSubtitleText)
                }
            }
        )
    }

    repairResultPopupMessage?.let { msg ->
        AlertDialog(
            onDismissRequest = { repairResultPopupMessage = null },
            containerColor = Color(0xFF1F262B),
            icon = { Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF81C784)) },
            title = { Text("Hasil Smart Repair Aset", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White) },
            text = {
                Text(
                    text = msg,
                    fontSize = 13.sp,
                    color = Color(0xFFD5DEE5),
                    lineHeight = 18.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = { repairResultPopupMessage = null },
                    colors = ButtonDefaults.buttonColors(containerColor = SkyBlueAccent)
                ) {
                    Text("OK, Mengerti", color = SkyBlueDarkText, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        )
    }

    if (showDeleteAllConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteAllConfirmDialog = false },
            containerColor = Color(0xFF1F262B),
            icon = { Icon(Icons.Default.DeleteForever, contentDescription = null, tint = Color(0xFFFF8A80)) },
            title = { Text("Hapus Total Semua Aset Minecraft?", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White) },
            text = {
                Text(
                    "Seluruh file aset Minecraft 26.3 (.local/horizon-client/26.3) akan dihapus total dari penyimpanan perangkat ($installedSizeText).\n\nAnda perlu mengunduh ulang aset jika ingin masuk ke dalam game lagi. Data Saves & Mods tetap aman.",
                    fontSize = 13.sp,
                    color = Color(0xFFD5DEE5),
                    lineHeight = 18.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteAllConfirmDialog = false
                        HorizonNativeBridge.deleteAllMinecraftAssets(context, true)
                        sizeRefreshTick += 1
                        deleteResultPopupMessage = "Seluruh aset Minecraft 26.3 berhasil dihapus total dari perangkat. Status aset kini direset ke 0 MB (Belum Terpasang)."
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935))
                ) {
                    Icon(Icons.Outlined.Delete, contentDescription = null, tint = Color.White, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(5.dp))
                    Text("Ya, Hapus Total", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAllConfirmDialog = false }) {
                    Text("Batal", fontSize = 12.sp, color = MutedSubtitleText)
                }
            }
        )
    }

    deleteResultPopupMessage?.let { msg ->
        AlertDialog(
            onDismissRequest = { deleteResultPopupMessage = null },
            containerColor = Color(0xFF1F262B),
            icon = { Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SkyBlueAccent) },
            title = { Text("Aset Berhasil Dihapus Total", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White) },
            text = {
                Text(
                    text = msg,
                    fontSize = 13.sp,
                    color = Color(0xFFD5DEE5),
                    lineHeight = 18.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = { deleteResultPopupMessage = null },
                    colors = ButtonDefaults.buttonColors(containerColor = SkyBlueAccent)
                ) {
                    Text("Selesai", color = SkyBlueDarkText, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        )
    }

    Surface(
        shape = RoundedCornerShape(22.dp),
        color = CardSlateBg,
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header Row: Simple Title & Subtitle (Status pill removed per user request!)
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = "Game & Assets Minecraft",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "Kelola instalasi aset resmi, perbaikan otomatis (Repair), atau hapus total aset",
                    fontSize = 11.sp,
                    color = MutedSubtitleText
                )
            }

            // Main Clean Version & Assets Card
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = CardRowBg,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Row 1: Icon + Version Name + 3 Simple Summary Pills (Zero Overlap)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        MinecraftGrassCubeIcon(size = 44.dp)
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "Minecraft 26.3",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 15.sp,
                                    color = Color.White
                                )
                                LavenderTagBadge(text = "Rust Engine")
                                LavenderTagBadge(text = "v0.7.6 Base")
                            }
                            Text(
                                text = when {
                                    installStatus.ready -> "Aset lengkap terpasang • Siap langsung masuk ke dalam game"
                                    installStatus.running -> "${installStatus.stage} ${if (installStatus.speedText.isNotBlank()) "• ${installStatus.speedText}" else ""}"
                                    else -> "Unduh paket aset resmi Mojang untuk mulai bermain Minecraft 26.3"
                                },
                                fontSize = 11.sp,
                                color = if (installStatus.ready) Color(0xFF81C784) else MutedSubtitleText,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Row 2: 3 Equal Simple Info Cards (Engine, Controls, Storage Size)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF1B2126),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text("Status Aset", fontSize = 10.sp, color = MutedSubtitleText)
                                Text(
                                    text = if (installStatus.ready) "Lengkap (100%)" else "${installStatus.percent}% Terunduh",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (installStatus.ready) Color(0xFF81C784) else Color.White,
                                    maxLines = 1
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF1B2126),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text("Komponen", fontSize = 10.sp, color = MutedSubtitleText)
                                Text(
                                    text = "Worldgen + 60 Touch",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SkyBlueAccent,
                                    maxLines = 1
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF1B2126),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text("Ukuran Aset", fontSize = 10.sp, color = MutedSubtitleText)
                                Text(
                                    text = installedSizeText,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    maxLines = 1
                                )
                            }
                        }
                    }

                    // Row 3: Progress Bar (shown cleanly when downloading or when progress > 0)
                    if (installStatus.running || installStatus.done > 0L || isRepairRunning) {
                        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (isRepairRunning) "Memverifikasi & Memperbaiki File..." else installStatus.stage,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (installStatus.paused) Color(0xFFFFB74D) else SkyBlueAccent,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = buildString {
                                        if (installStatus.speedText.isNotBlank()) {
                                            append(installStatus.speedText)
                                            append(" • ")
                                        }
                                        append("${installStatus.percent}%")
                                    },
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                            LinearProgressIndicator(
                                progress = { if (isRepairRunning) 1f else animatedProgress },
                                color = if (installStatus.ready) Color(0xFF81C784) else SkyBlueAccent,
                                trackColor = Color(0xFF33424B),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(7.dp)
                                    .clip(RoundedCornerShape(50))
                            )
                        }
                    }

                    if (installStatus.error.isNotEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFFF5252).copy(alpha = 0.14f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = installStatus.error,
                                fontSize = 11.sp,
                                color = Color(0xFFFF8A80),
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }

                    // Row 4: Clean Action Buttons Bar (Download/Pause, Repair Pop-Up, Hapus Total Pop-Up)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (installStatus.running) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = SkyBlueAccent,
                                modifier = Modifier
                                    .weight(1f)
                                    .animatedPressScale {
                                        if (installStatus.paused) onResumeDownload() else onPauseDownload()
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(vertical = 10.dp, horizontal = 12.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (installStatus.paused) Icons.Default.PlayArrow else Icons.Default.Pause,
                                        contentDescription = null,
                                        tint = SkyBlueDarkText,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = if (installStatus.paused) "Lanjutkan Download" else "Jeda (Pause)",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = SkyBlueDarkText,
                                        maxLines = 1
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFF3A2326),
                                modifier = Modifier
                                    .weight(1f)
                                    .animatedPressScale { showCancelConfirmDialog = true }
                            ) {
                                Row(
                                    modifier = Modifier.padding(vertical = 10.dp, horizontal = 12.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Cancel, contentDescription = null, tint = Color(0xFFFF8A80), modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = "Batalkan Download",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFFF8A80),
                                        maxLines = 1
                                    )
                                }
                            }
                        } else {
                            // 1. Download / Re-Download Button
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = SkyBlueAccent,
                                modifier = Modifier
                                    .weight(1f)
                                    .animatedPressScale {
                                        if (installStatus.ready) {
                                            showRepairConfirmDialog = true
                                        } else {
                                            onStartDownload()
                                        }
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(vertical = 10.dp, horizontal = 10.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (installStatus.ready) Icons.Default.Verified else Icons.Default.Download,
                                        contentDescription = null,
                                        tint = SkyBlueDarkText,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = if (installStatus.ready) "Verifikasi Aset" else "Download Aset",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = SkyBlueDarkText,
                                        maxLines = 1
                                    )
                                }
                            }

                            // 2. Smart Repair Button (Opens Repair Confirmation & Result Pop-Up!)
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = SubRailCapsuleActive,
                                modifier = Modifier
                                    .weight(1f)
                                    .animatedPressScale { showRepairConfirmDialog = true }
                            ) {
                                Row(
                                    modifier = Modifier.padding(vertical = 10.dp, horizontal = 10.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Build,
                                        contentDescription = null,
                                        tint = SkyBlueAccent,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = "Repair Aset",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        maxLines = 1
                                    )
                                }
                            }

                            // 3. Hapus Total Semua Aset Button (Opens Delete All Confirmation Pop-Up!)
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFF3A2326),
                                modifier = Modifier
                                    .weight(1f)
                                    .animatedPressScale { showDeleteAllConfirmDialog = true }
                            ) {
                                Row(
                                    modifier = Modifier.padding(vertical = 10.dp, horizontal = 10.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Delete,
                                        contentDescription = null,
                                        tint = Color(0xFFFF8A80),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = "Hapus Total Aset",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFFF8A80),
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }

                    // Row 5: Compact Collapsible Download & Repair Log
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF181E22),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { logExpanded = !logExpanded }
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Terminal,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp),
                                        tint = SkyBlueAccent
                                    )
                                    Text(
                                        text = "Detail Log Aktivitas Aset",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFFD5DEE5)
                                    )
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "Salin Log",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = SkyBlueAccent,
                                        modifier = Modifier.clickable {
                                            val textToCopy = if (installStatus.logs.isNotEmpty()) {
                                                installStatus.logs.joinToString("\n")
                                            } else {
                                                "Stage: ${installStatus.stage} (${installStatus.percent}%)"
                                            }
                                            HorizonNativeBridge.copyToClipboard(context, "Download Log", textToCopy)
                                        }
                                    )
                                    Icon(
                                        Icons.Default.ExpandMore,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier
                                            .size(18.dp)
                                            .rotate(chevronRotation)
                                    )
                                }
                            }

                            AnimatedVisibility(
                                visible = logExpanded,
                                enter = expandVertically(tween(200)) + fadeIn(tween(180)),
                                exit = shrinkVertically(tween(180)) + fadeOut(tween(150))
                            ) {
                                val logContent = if (installStatus.logs.isNotEmpty()) {
                                    installStatus.logs.takeLast(25).joinToString("\n")
                                } else if (installStatus.ready) {
                                    "[OK] Minecraft 26.3 assets, worldgen tables, and 60/60 touch controls verified."
                                } else {
                                    "Tekan 'Download Aset' untuk mengunduh atau 'Repair Aset' untuk memeriksa file."
                                }
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 60.dp, max = 130.dp)
                                        .padding(start = 10.dp, end = 10.dp, bottom = 10.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFF0E1215))
                                        .padding(8.dp)
                                        .verticalScroll(rememberScrollState())
                                ) {
                                    Text(
                                        text = logContent,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 10.sp,
                                        color = Color(0xFFA5D6A7)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Store Viewport styled after `429665.jpg` & `429667.jpg`.
 */
@Composable
private fun CleanStoreViewport(
    profileSlot: Int,
    logEnabled: Boolean,
    initialCategory: StoreCategory = StoreCategory.ALL
) {
    val context = LocalContext.current
    var selectedCategory by remember(initialCategory) { mutableStateOf(initialCategory) }
    var searchQuery by remember { mutableStateOf("") }
    var gameVersionQuery by remember { mutableStateOf("") }
    var sortOption by remember { mutableStateOf(StoreSortOption.MOST_DOWNLOADED) }
    var statusFilter by remember { mutableStateOf(StoreStatusFilter.ALL) }
    var currentPage by remember { mutableIntStateOf(1) }
    var totalPages by remember { mutableIntStateOf(1) }
    var totalHits by remember { mutableIntStateOf(19) }
    var refreshTrigger by remember { mutableIntStateOf(0) }

    // Expandable filter accordions in Right Sidebar
    var expandSortBy by remember { mutableStateOf(false) }
    var expandStatus by remember { mutableStateOf(false) }

    // Drill-down selected project for Version Accordion & Right Detail Card
    var selectedProject by remember { mutableStateOf<StoreCatalogItem?>(null) }
    var projectVersions by remember { mutableStateOf<List<StoreVersionRelease>>(emptyList()) }
    var isLoadingVersions by remember { mutableStateOf(false) }
    var showOnlyMcReleases by remember { mutableStateOf(true) }
    var searchMcVersionsQuery by remember { mutableStateOf("") }
    var expandedMcVerGroups by remember { mutableStateOf<Set<String>>(emptySet()) }

    var isLoading by remember { mutableStateOf(false) }
    var catalogItems by remember { mutableStateOf<List<StoreCatalogItem>>(emptyList()) }
    var installedMap by remember { mutableStateOf(HorizonNativeBridge.getInstalledStoreVersionsMap(context, profileSlot)) }
    var favoriteIds by remember { mutableStateOf(HorizonNativeBridge.getFavoriteProjectIds(context)) }
    var storeProgress by remember { mutableStateOf(StoreDownloadProgress()) }
    var pendingIncompatibleItem by remember { mutableStateOf<StoreCatalogItem?>(null) }
    var showCancelStoreDialog by remember { mutableStateOf(false) }

    val modpackImportPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            val importedPack: File? = HorizonNativeBridge.importUriToDestination(
                context,
                uri,
                "horizon-packs",
                "imported_${System.currentTimeMillis()}.hpack",
                logEnabled,
                profileSlot
            )
            val packOk: Boolean = importedPack != null
            refreshTrigger += 1
            Toast.makeText(
                context,
                if (packOk) "Horizon Modpack (.hpack) berhasil diimpor!" else "Gagal mengimpor Horizon Modpack (.hpack).",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    LaunchedEffect(
        selectedCategory,
        searchQuery,
        gameVersionQuery,
        sortOption,
        statusFilter,
        currentPage,
        refreshTrigger,
        profileSlot
    ) {
        isLoading = true
        val pageRes = withContext(Dispatchers.IO) {
            HorizonNativeBridge.searchPlayHorizonStore(
                context = context,
                profileSlot = profileSlot,
                category = selectedCategory,
                searchQuery = searchQuery,
                gameVersionFilter = gameVersionQuery,
                sortOption = sortOption,
                statusFilter = statusFilter,
                page = currentPage
            )
        }
        installedMap = HorizonNativeBridge.getInstalledStoreVersionsMap(context, profileSlot)
        catalogItems = pageRes.items
        totalPages = pageRes.totalPages
        totalHits = pageRes.totalHits
        isLoading = false
    }

    LaunchedEffect(selectedProject) {
        val proj = selectedProject ?: return@LaunchedEffect
        isLoadingVersions = true
        val vers = withContext(Dispatchers.IO) {
            HorizonNativeBridge.fetchProjectVersionReleases(proj)
        }
        projectVersions = vers
        val firstVerGroup = vers.firstOrNull()?.primaryMcVersion ?: "26.3"
        expandedMcVerGroups = setOf(firstVerGroup)
        isLoadingVersions = false
    }

    val startItemDownload = { item: StoreCatalogItem ->
        HorizonNativeBridge.downloadAndInstallStoreItem(
            context = context,
            item = item,
            profileSlot = profileSlot,
            logEnabled = logEnabled,
            onProgress = { prog -> storeProgress = prog },
            onComplete = { _, msg ->
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    installedMap = HorizonNativeBridge.getInstalledStoreVersionsMap(context, profileSlot)
                    refreshTrigger += 1
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    pendingIncompatibleItem?.let { item ->
        AlertDialog(
            onDismissRequest = { pendingIncompatibleItem = null },
            icon = { Icon(Icons.Default.WarningAmber, contentDescription = null, tint = Color(0xFFFFB74D)) },
            title = { Text("Peringatan Versi Minecraft", style = MaterialTheme.typography.titleMedium) },
            text = {
                Text(
                    "Item '${item.title}' tercatat untuk Minecraft versi ${item.versionBadge} (sedangkan engine saat ini adalah Minecraft 26.3).\n\nApakah Anda ingin tetap mengunduh dan memasangnya?",
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val target = item
                        pendingIncompatibleItem = null
                        startItemDownload(target)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SkyBlueAccent)
                ) {
                    Text("Tetap Download", fontSize = 12.sp, color = SkyBlueDarkText, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingIncompatibleItem = null }) {
                    Text("Cancel", fontSize = 12.sp)
                }
            }
        )
    }

    if (showCancelStoreDialog) {
        AlertDialog(
            onDismissRequest = { showCancelStoreDialog = false },
            icon = { Icon(Icons.Default.Cancel, contentDescription = null, tint = Color(0xFFFF6E6E)) },
            title = { Text("Batalkan Download Store?", style = MaterialTheme.typography.titleMedium) },
            text = {
                Text(
                    "File '${storeProgress.activeTitle}' yang sedang diunduh akan dihentikan dan file sementara (.part) akan langsung dihapus.",
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showCancelStoreDialog = false
                        HorizonNativeBridge.storeDownloadCancelled.set(true)
                        HorizonNativeBridge.storeDownloadPaused.set(false)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935))
                ) {
                    Text("Ya, Batalkan & Hapus", color = Color.White, fontSize = 12.sp)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCancelStoreDialog = false }) {
                    Text("Lanjutkan", fontSize = 12.sp)
                }
            }
        )
    }

    Row(
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // 1. Left Sub-Navigation Rail matching playhorizon.dev/mods (`All`, `Mods`, `Native Mods`, `Modpacks`, `Resource Packs`, `Shader Packs`)
        ZalithSubCategoryRail(
            items = StoreCategory.entries,
            selectedItem = selectedCategory,
            itemLabel = { it.label },
            itemIcon = {
                when (it) {
                    StoreCategory.ALL -> Icons.Default.Apps
                    StoreCategory.MOD -> Icons.Default.Extension
                    StoreCategory.NATIVE_MOD -> Icons.Default.Memory
                    StoreCategory.MODPACK -> Icons.Default.Inventory2
                    StoreCategory.RESOURCE_PACK -> Icons.Default.Brush
                    StoreCategory.SHADER_PACK -> Icons.Default.Lightbulb
                }
            },
            onSelect = {
                selectedCategory = it
                selectedProject = null
                currentPage = 1
            }
        )

        // 2. Center + Right Columns (3-Column Browser vs Project Version Accordion View)
        AnimatedContent(
            targetState = selectedProject,
            transitionSpec = {
                (fadeIn(tween(220, easing = FastOutSlowInEasing)) +
                    slideInHorizontally(tween(220, easing = FastOutSlowInEasing)) { w -> if (targetState != null) w / 12 else -w / 12 })
                    .togetherWith(fadeOut(tween(160)))
                    .using(sizeTransform = null)
            },
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            label = "StoreMasterDetailTransition"
        ) { detailProj ->
            if (detailProj == null) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Center Column: Non-Overlapping Top Header Bar + Project List
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Dedicated Top Header Bar (NEVER overlaps cards!) showing `19 of 19 • playhorizon.dev/mods` + Pagination + Refresh
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = CardSlateBg,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 7.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = SubRailCapsuleActive
                                    ) {
                                        Text(
                                            text = "HORIZON REGISTRY",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = SkyBlueAccent,
                                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                        )
                                    }
                                    Text(
                                        text = "${catalogItems.size} of $totalHits • playhorizon.dev/mods",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MutedSubtitleText,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(50),
                                        color = CardRowBg
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.ChevronLeft,
                                                contentDescription = "Previous",
                                                tint = if (currentPage > 1) Color.White else MutedSubtitleText.copy(alpha = 0.4f),
                                                modifier = Modifier
                                                    .size(16.dp)
                                                    .animatedPressScale(enabled = currentPage > 1) {
                                                        if (currentPage > 1) currentPage -= 1
                                                    }
                                            )
                                            Text(
                                                text = "$currentPage / $totalPages",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = Color.White
                                            )
                                            Icon(
                                                imageVector = Icons.Default.ChevronRight,
                                                contentDescription = "Next",
                                                tint = if (currentPage < totalPages) Color.White else MutedSubtitleText.copy(alpha = 0.4f),
                                                modifier = Modifier
                                                    .size(16.dp)
                                                    .animatedPressScale(enabled = currentPage < totalPages) {
                                                        if (currentPage < totalPages) currentPage += 1
                                                    }
                                            )
                                        }
                                    }

                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = "Refresh Registry",
                                        tint = Color.White,
                                        modifier = Modifier
                                            .size(18.dp)
                                            .animatedPressScale { refreshTrigger += 1 }
                                    )
                                }
                            }
                        }

                        // Active Download Progress Bar with Pause / Resume / Cancel
                        AnimatedVisibility(visible = storeProgress.running) {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = SubRailCapsuleActive,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "Downloading ${storeProgress.activeTitle} (${storeProgress.percent}%)",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White,
                                                maxLines = 1
                                            )
                                            Text(
                                                text = "${storeProgress.speedText} • ${storeProgress.statusText}",
                                                fontSize = 10.sp,
                                                color = MutedSubtitleText,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = SkyBlueAccent,
                                                modifier = Modifier.animatedPressScale {
                                                    val nextPaused = !HorizonNativeBridge.storeDownloadPaused.get()
                                                    HorizonNativeBridge.storeDownloadPaused.set(nextPaused)
                                                }
                                            ) {
                                                Text(
                                                    text = if (storeProgress.paused) "Resume" else "Pause",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = SkyBlueDarkText,
                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                                )
                                            }
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = Color(0xFF3A2326),
                                                modifier = Modifier.animatedPressScale { showCancelStoreDialog = true }
                                            ) {
                                                Text(
                                                    text = "Cancel",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = Color(0xFFFF8A80),
                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                                )
                                            }
                                        }
                                    }
                                    LinearProgressIndicator(
                                        progress = { (storeProgress.percent / 100f).coerceIn(0f, 1f) },
                                        color = SkyBlueAccent,
                                        trackColor = Color(0xFF263238),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(5.dp)
                                            .clip(RoundedCornerShape(50))
                                    )
                                }
                            }
                        }

                        if (isLoading) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                LinearProgressIndicator(
                                    color = SkyBlueAccent,
                                    trackColor = CardSlateBg,
                                    modifier = Modifier
                                        .width(180.dp)
                                        .height(4.dp)
                                        .clip(RoundedCornerShape(50))
                                )
                            }
                        } else if (catalogItems.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = CardSlateBg
                                ) {
                                    Text(
                                        text = "No projects found on playhorizon.dev/mods matching your filter.",
                                        fontSize = 12.sp,
                                        color = MutedSubtitleText,
                                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp)
                                    )
                                }
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(bottom = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(catalogItems, key = { it.id }) { item ->
                                    val installState = HorizonNativeBridge.getStoreItemInstallState(item, installedMap)
                                    val installedVer = installedMap[item.id] ?: installedMap[item.slug]
                                    val isCurrentlyDownloading = storeProgress.running && storeProgress.activeItemId == item.id

                                    Surface(
                                        shape = RoundedCornerShape(18.dp),
                                        color = CardSlateBg,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .animatedPressScale { selectedProject = item }
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 12.dp, vertical = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            // Left 48dp Pixel-Style Square Icon Badge (matching `429816.jpg`)
                                            StoreAsyncProjectIcon(
                                                iconUrl = item.iconUrl,
                                                title = item.title,
                                                isMod = item.category == StoreCategory.MOD || item.category == StoreCategory.NATIVE_MOD,
                                                isWorld = item.category == StoreCategory.MODPACK,
                                                size = 48.dp
                                            )

                                            Spacer(Modifier.width(12.dp))

                                            // Middle Column: Title + Summary + Rating Stars & Downloads + Version & Author (`429816.jpg` exact layout!)
                                            Column(
                                                modifier = Modifier.weight(1f),
                                                verticalArrangement = Arrangement.spacedBy(3.dp)
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    Text(
                                                        text = item.title,
                                                        fontSize = 14.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color.White,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis,
                                                        modifier = Modifier.weight(1f, fill = false)
                                                    )
                                                    // Kind Badge (`MOD`, `NATIVE MOD`, `MODPACK`, `RESOURCE PACK`, `SHADER PACK`)
                                                    Surface(
                                                        shape = RoundedCornerShape(6.dp),
                                                        color = Color(0xFF14181E)
                                                    ) {
                                                        Text(
                                                            text = item.kindBadge,
                                                            fontSize = 9.sp,
                                                            fontWeight = FontWeight.ExtraBold,
                                                            color = Color(0xFFD5DEE5),
                                                            maxLines = 1,
                                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                        )
                                                    }
                                                    LavenderTagBadge(text = item.versionBadge)
                                                }

                                                Text(
                                                    text = item.summary,
                                                    fontSize = 11.sp,
                                                    color = Color(0xFFD0D8DE),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )

                                                // Star rating + downloads + version + author (`★★★★★ 4.5 (2) · 5.6k downloads   0.1.1 · levzzz5154`)
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    Text(
                                                        text = "★★★★★ ${String.format(Locale.US, "%.1f", item.ratingScore)} (${item.ratingCount}) · ${item.formattedDownloads}",
                                                        fontSize = 10.sp,
                                                        color = Color(0xFFFFCA28),
                                                        maxLines = 1
                                                    )
                                                    Text(
                                                        text = "${item.version} · ${item.author}",
                                                        fontSize = 10.sp,
                                                        fontFamily = FontFamily.Monospace,
                                                        color = MutedSubtitleText,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                            }

                                            Spacer(Modifier.width(10.dp))

                                            // Right Action Column: Installed / Update / Cancel / Install Button
                                            Column(
                                                horizontalAlignment = Alignment.End,
                                                verticalArrangement = Arrangement.spacedBy(5.dp)
                                            ) {
                                                when {
                                                    isCurrentlyDownloading -> {
                                                        Surface(
                                                            shape = RoundedCornerShape(10.dp),
                                                            color = Color(0xFF3A2326),
                                                            modifier = Modifier.animatedPressScale { showCancelStoreDialog = true }
                                                        ) {
                                                            Row(
                                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                                                verticalAlignment = Alignment.CenterVertically,
                                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                            ) {
                                                                Icon(Icons.Default.Cancel, contentDescription = "Cancel", tint = Color(0xFFFF8A80), modifier = Modifier.size(14.dp))
                                                                Text("${storeProgress.percent}% Cancel", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFF8A80))
                                                            }
                                                        }
                                                    }

                                                    installState == StoreInstallState.UPDATE_AVAILABLE -> {
                                                        Surface(
                                                            shape = RoundedCornerShape(10.dp),
                                                            color = Color(0xFFFFB74D),
                                                            modifier = Modifier.animatedPressScale(enabled = !storeProgress.running) {
                                                                if (item.isCompatibleWith26_3) startItemDownload(item)
                                                                else pendingIncompatibleItem = item
                                                            }
                                                        ) {
                                                            Row(
                                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                                                verticalAlignment = Alignment.CenterVertically,
                                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                            ) {
                                                                Icon(Icons.Default.SystemUpdateAlt, contentDescription = "Update", tint = Color(0xFF261600), modifier = Modifier.size(14.dp))
                                                                Text("Update", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF261600))
                                                            }
                                                        }
                                                        Text(
                                                            text = "v${installedVer ?: "0.1"} → v${item.version}",
                                                            fontSize = 9.sp,
                                                            color = Color(0xFFFFB74D),
                                                            maxLines = 1
                                                        )
                                                    }

                                                    installState == StoreInstallState.INSTALLED -> {
                                                        Row(
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                        ) {
                                                            Surface(
                                                                shape = RoundedCornerShape(10.dp),
                                                                color = Color(0xFF1E3E29)
                                                            ) {
                                                                Row(
                                                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
                                                                    verticalAlignment = Alignment.CenterVertically,
                                                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                                ) {
                                                                    Icon(Icons.Default.CheckCircle, contentDescription = "Installed", tint = Color(0xFF81C784), modifier = Modifier.size(14.dp))
                                                                    Text("Installed", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF81C784))
                                                                }
                                                            }
                                                            Icon(
                                                                imageVector = Icons.Outlined.Delete,
                                                                contentDescription = "Uninstall",
                                                                tint = Color(0xFFFF8A80),
                                                                modifier = Modifier
                                                                    .size(18.dp)
                                                                    .animatedPressScale {
                                                                        HorizonNativeBridge.uninstallStoreItem(context, item, profileSlot, logEnabled)
                                                                        installedMap = HorizonNativeBridge.getInstalledStoreVersionsMap(context, profileSlot)
                                                                    }
                                                            )
                                                        }
                                                    }

                                                    else -> {
                                                        Surface(
                                                            shape = RoundedCornerShape(10.dp),
                                                            color = SkyBlueAccent,
                                                            modifier = Modifier.animatedPressScale(enabled = !storeProgress.running) {
                                                                if (item.isCompatibleWith26_3) startItemDownload(item)
                                                                else pendingIncompatibleItem = item
                                                            }
                                                        ) {
                                                            Row(
                                                                modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp),
                                                                verticalAlignment = Alignment.CenterVertically,
                                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                            ) {
                                                                Icon(Icons.Default.Download, contentDescription = "Install", tint = SkyBlueDarkText, modifier = Modifier.size(14.dp))
                                                                Text("Install", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SkyBlueDarkText)
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 3. Right Filter & Registry Actions Sidebar (`208.dp`, tailored to playhorizon.dev/mods!)
                    Column(
                        modifier = Modifier
                            .width(208.dp)
                            .fillMaxHeight()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StoreRightSearchPill(
                            value = searchQuery,
                            placeholder = "Search Horizon mods...",
                            onValueChange = {
                                searchQuery = it
                                currentPage = 1
                            }
                        )

                        StoreRightSearchPill(
                            value = gameVersionQuery,
                            placeholder = "Game Version (26.3)",
                            onValueChange = {
                                gameVersionQuery = it
                                currentPage = 1
                            }
                        )

                        if (selectedCategory == StoreCategory.MODPACK) {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = CardSlateBg,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .animatedPressScale { modpackImportPicker.launch("*/*") }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 10.dp, horizontal = 12.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, tint = SkyBlueAccent, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(5.dp))
                                    Text("Import .hpack Modpack", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                                }
                            }
                        }

                        // Sort By Accordion (`Most downloaded`, `Best rated`, `Recently updated`, `Name (A-Z)` — matching 429816.jpg!)
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = CardSlateBg,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { expandSortBy = !expandSortBy }
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text("Sort By", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                                        LavenderTagBadge(text = sortOption.label)
                                    }
                                    Icon(
                                        imageVector = if (expandSortBy) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                AnimatedVisibility(visible = expandSortBy) {
                                    Column(
                                        modifier = Modifier.padding(start = 8.dp, end = 8.dp, bottom = 8.dp),
                                        verticalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        StoreSortOption.entries.forEach { opt ->
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .clickable {
                                                        sortOption = opt
                                                        currentPage = 1
                                                        expandSortBy = false
                                                    }
                                                    .padding(horizontal = 6.dp, vertical = 4.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                RadioButton(
                                                    selected = sortOption == opt,
                                                    onClick = {
                                                        sortOption = opt
                                                        currentPage = 1
                                                        expandSortBy = false
                                                    },
                                                    colors = RadioButtonDefaults.colors(selectedColor = SkyBlueAccent),
                                                    modifier = Modifier.size(20.dp)
                                                )
                                                Spacer(Modifier.width(6.dp))
                                                Text(opt.label, fontSize = 11.sp, color = Color.White)
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Status Filter Accordion (`All Projects`, `Installed Only`, `Updates Available`, `Official & Signed`)
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = CardSlateBg,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { expandStatus = !expandStatus }
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text("Filter Status", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                                        LavenderTagBadge(text = statusFilter.label)
                                    }
                                    Icon(
                                        imageVector = if (expandStatus) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                AnimatedVisibility(visible = expandStatus) {
                                    Column(
                                        modifier = Modifier.padding(start = 8.dp, end = 8.dp, bottom = 8.dp),
                                        verticalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        StoreStatusFilter.entries.forEach { st ->
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .clickable {
                                                        statusFilter = st
                                                        currentPage = 1
                                                        expandStatus = false
                                                    }
                                                    .padding(horizontal = 6.dp, vertical = 4.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                RadioButton(
                                                    selected = statusFilter == st,
                                                    onClick = {
                                                        statusFilter = st
                                                        currentPage = 1
                                                        expandStatus = false
                                                    },
                                                    colors = RadioButtonDefaults.colors(selectedColor = SkyBlueAccent),
                                                    modifier = Modifier.size(20.dp)
                                                )
                                                Spacer(Modifier.width(6.dp))
                                                Text(st.label, fontSize = 11.sp, color = Color.White)
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Official playhorizon.dev/mods Portal Actions (`Submit yours` & `Make a mod` from 429816.jpg!)
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = CardSlateBg,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(7.dp)
                            ) {
                                Text(
                                    text = "Horizon Mods Portal",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "Reviewed mods, native mods, modpacks, resource packs & WGSL shaders for Horizon 26.3.",
                                    fontSize = 10.sp,
                                    color = MutedSubtitleText,
                                    lineHeight = 13.sp
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color(0xFF5E5CE6),
                                        modifier = Modifier
                                            .weight(1f)
                                            .animatedPressScale {
                                                runCatching {
                                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://playhorizon.dev/mods/submit")))
                                                }
                                            }
                                    ) {
                                        Text(
                                            text = "Submit yours",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            textAlign = TextAlign.Center,
                                            maxLines = 1,
                                            modifier = Modifier.padding(vertical = 7.dp, horizontal = 4.dp)
                                        )
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = CardRowBg,
                                        modifier = Modifier
                                            .weight(1f)
                                            .animatedPressScale {
                                                runCatching {
                                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://playhorizon.dev/docs/make-a-mod")))
                                                }
                                            }
                                    ) {
                                        Text(
                                            text = "Make a mod",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            textAlign = TextAlign.Center,
                                            maxLines = 1,
                                            modifier = Modifier.padding(vertical = 7.dp, horizontal = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // ============================================================
                // MODE 2: PROJECT VERSION ACCORDION & RIGHT SUMMARY CARD
                // ============================================================
                val detailInstallState = HorizonNativeBridge.getStoreItemInstallState(detailProj, installedMap)
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = CardSlateBg,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = CardRowBg,
                                    modifier = Modifier.animatedPressScale { selectedProject = null }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = SkyBlueAccent, modifier = Modifier.size(15.dp))
                                        Text("Back to List", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (showOnlyMcReleases) SubRailCapsuleActive else CardRowBg,
                                    modifier = Modifier.animatedPressScale { showOnlyMcReleases = !showOnlyMcReleases }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (showOnlyMcReleases) Icons.Default.Check else Icons.Default.FilterList,
                                            contentDescription = null,
                                            tint = SkyBlueAccent,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Text(
                                            text = "Show Only MC Releases",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = Color.White
                                        )
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = CardRowBg,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(32.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(horizontal = 12.dp),
                                        contentAlignment = Alignment.CenterStart
                                    ) {
                                        if (searchMcVersionsQuery.isEmpty()) {
                                            Text("Search MC Versions", fontSize = 11.sp, color = MutedSubtitleText)
                                        }
                                        BasicTextField(
                                            value = searchMcVersionsQuery,
                                            onValueChange = { searchMcVersionsQuery = it },
                                            singleLine = true,
                                            textStyle = TextStyle(color = Color.White, fontSize = 11.sp),
                                            cursorBrush = SolidColor(SkyBlueAccent),
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }
                            }

                            if (isLoadingVersions) {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    LinearProgressIndicator(
                                        color = SkyBlueAccent,
                                        trackColor = CardRowBg,
                                        modifier = Modifier
                                            .width(180.dp)
                                            .height(4.dp)
                                            .clip(RoundedCornerShape(50))
                                    )
                                }
                            } else {
                                val groupedByMcVer = remember(projectVersions, showOnlyMcReleases, searchMcVersionsQuery) {
                                    val map = linkedMapOf<String, MutableList<StoreVersionRelease>>()
                                    projectVersions.forEach { rel ->
                                        rel.mcVersions.forEach { mcV ->
                                            val matchesSearch = searchMcVersionsQuery.isBlank() || mcV.contains(searchMcVersionsQuery, ignoreCase = true)
                                            if (matchesSearch) {
                                                map.getOrPut(mcV) { mutableListOf() }.add(rel)
                                            }
                                        }
                                    }
                                    map
                                }

                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    groupedByMcVer.entries.forEach { (mcVer, releases) ->
                                        item(key = "mcgroup_$mcVer") {
                                            val isExpanded = mcVer in expandedMcVerGroups
                                            val isRecommended = mcVer.contains("26.3")
                                            Surface(
                                                shape = RoundedCornerShape(14.dp),
                                                color = CardRowBg,
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Column(modifier = Modifier.fillMaxWidth()) {
                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .clickable {
                                                                expandedMcVerGroups = if (isExpanded) expandedMcVerGroups - mcVer
                                                                else expandedMcVerGroups + mcVer
                                                            }
                                                            .padding(horizontal = 14.dp, vertical = 11.dp),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Row(
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                        ) {
                                                            Text(
                                                                text = mcVer,
                                                                fontSize = 13.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color = Color.White
                                                            )
                                                            LavenderTagBadge(text = detailProj.kindBadge)
                                                        }

                                                        Row(
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                        ) {
                                                            if (isRecommended) {
                                                                Icon(Icons.Default.Star, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = LavenderBadgeBg, modifier = Modifier.size(16.dp))
                                                            }
                                                            Icon(
                                                                imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                                                contentDescription = null,
                                                                tint = Color.White,
                                                                modifier = Modifier.size(20.dp)
                                                            )
                                                        }
                                                    }

                                                    AnimatedVisibility(visible = isExpanded) {
                                                        Column(
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .padding(start = 10.dp, end = 10.dp, bottom = 10.dp),
                                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                                        ) {
                                                            releases.forEach { rel ->
                                                                val isDownloadingThis = storeProgress.running && storeProgress.activeItemId == detailProj.id
                                                                val installedVerForProj = installedMap[detailProj.id]
                                                                    ?: installedMap[detailProj.slug]
                                                                    ?: installedMap[HorizonNativeBridge.sanitizeModId(detailProj.id)]
                                                                val isThisExactVerInstalled = installedVerForProj != null && installedVerForProj == rel.versionNumber
                                                                Surface(
                                                                    shape = RoundedCornerShape(12.dp),
                                                                    color = Color(0xFF1B2125),
                                                                    modifier = Modifier
                                                                        .fillMaxWidth()
                                                                        .animatedPressScale(enabled = !isDownloadingThis) {
                                                                            if (isThisExactVerInstalled) {
                                                                                Toast.makeText(
                                                                                    context,
                                                                                    "${detailProj.title} v${rel.versionNumber} sudah terpasang (tidak perlu instal ulang).",
                                                                                    Toast.LENGTH_SHORT
                                                                                ).show()
                                                                            } else {
                                                                                val dlItem = detailProj.copy(
                                                                                    version = rel.versionNumber,
                                                                                    supportedMcVersions = listOf(mcVer),
                                                                                    downloadUrl = rel.downloadUrl.ifBlank { detailProj.downloadUrl },
                                                                                    fileName = rel.fileName.ifBlank { detailProj.fileName },
                                                                                    sizeBytes = if (rel.sizeBytes > 0) rel.sizeBytes else detailProj.sizeBytes
                                                                                )
                                                                                if (mcVer.contains("26.3")) {
                                                                                    startItemDownload(dlItem)
                                                                                } else {
                                                                                    pendingIncompatibleItem = dlItem
                                                                                }
                                                                            }
                                                                        }
                                                                ) {
                                                                    Row(
                                                                        modifier = Modifier
                                                                            .fillMaxWidth()
                                                                            .padding(horizontal = 12.dp, vertical = 9.dp),
                                                                        verticalAlignment = Alignment.CenterVertically,
                                                                        horizontalArrangement = Arrangement.SpaceBetween
                                                                    ) {
                                                                        Row(
                                                                            verticalAlignment = Alignment.CenterVertically,
                                                                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                                                                            modifier = Modifier.weight(1f)
                                                                        ) {
                                                                            Box(
                                                                                contentAlignment = Alignment.Center,
                                                                                modifier = Modifier
                                                                                    .size(28.dp)
                                                                                    .clip(CircleShape)
                                                                                    .background(Color(0xFF1E4628))
                                                                            ) {
                                                                                Text(
                                                                                    text = "R",
                                                                                    fontSize = 12.sp,
                                                                                    fontWeight = FontWeight.ExtraBold,
                                                                                    color = Color(0xFF4CAF50)
                                                                                )
                                                                            }

                                                                            Column(modifier = Modifier.weight(1f)) {
                                                                                Text(
                                                                                    text = rel.name,
                                                                                    fontSize = 12.sp,
                                                                                    fontWeight = FontWeight.Bold,
                                                                                    color = Color.White,
                                                                                    maxLines = 1,
                                                                                    overflow = TextOverflow.Ellipsis
                                                                                )
                                                                                Text(
                                                                                    text = "↗ ${rel.formattedDownloads}   •   ${rel.datePublished}   •   ${if (rel.signed) "Signed & Reviewed" else "Reviewed"}",
                                                                                    fontSize = 10.sp,
                                                                                    color = MutedSubtitleText,
                                                                                    maxLines = 1
                                                                                )
                                                                            }
                                                                        }

                                                                        Surface(
                                                                            shape = RoundedCornerShape(8.dp),
                                                                            color = when {
                                                                                isThisExactVerInstalled -> Color(0xFF1E3E29)
                                                                                installedVerForProj != null -> Color(0xFF3E3117)
                                                                                else -> SkyBlueAccent
                                                                            }
                                                                        ) {
                                                                            Text(
                                                                                text = when {
                                                                                    isThisExactVerInstalled -> "✓ Installed"
                                                                                    installedVerForProj != null -> "↑ Update"
                                                                                    else -> "Install"
                                                                                },
                                                                                fontSize = 10.sp,
                                                                                fontWeight = FontWeight.Bold,
                                                                                color = when {
                                                                                    isThisExactVerInstalled -> Color(0xFF81C784)
                                                                                    installedVerForProj != null -> Color(0xFFFFD54F)
                                                                                    else -> SkyBlueDarkText
                                                                                },
                                                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                                                            )
                                                                        }
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Right Project Summary Card (fixed layout so Favorite pill never wraps vertically!)
                    val isFav = detailProj.id in favoriteIds
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = CardSlateBg,
                        modifier = Modifier
                            .width(236.dp)
                            .fillMaxHeight()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(14.dp)
                                .verticalScroll(rememberScrollState()),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(9.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(modifier = Modifier.weight(1f, fill = false)) {
                                    LavenderTagBadge(text = detailProj.category.badgeLabel)
                                }

                                Spacer(Modifier.width(6.dp))

                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = if (isFav) Color(0xFF3A2329) else CardRowBg,
                                    modifier = Modifier.animatedPressScale {
                                        favoriteIds = HorizonNativeBridge.toggleProjectFavorite(context, detailProj.id)
                                    }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (isFav) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                            contentDescription = "Favorite",
                                            tint = if (isFav) Color(0xFFFF6E6E) else Color.White,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Text(
                                            text = if (isFav) "Favorited" else "Favorite",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (isFav) Color(0xFFFFB4B4) else Color.White,
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    }
                                }
                            }

                            StoreAsyncProjectIcon(
                                iconUrl = detailProj.iconUrl,
                                title = detailProj.title,
                                isMod = detailProj.category == StoreCategory.MOD || detailProj.category == StoreCategory.NATIVE_MOD,
                                isWorld = detailProj.category == StoreCategory.MODPACK,
                                size = 62.dp
                            )

                            Text(
                                text = detailProj.title,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                textAlign = TextAlign.Center
                            )

                            Text(
                                text = "v${detailProj.version} · By ${detailProj.author} · ${detailProj.license}",
                                fontSize = 10.sp,
                                color = SkyBlueAccent,
                                textAlign = TextAlign.Center
                            )

                            Text(
                                text = detailProj.summary,
                                fontSize = 11.sp,
                                color = Color(0xFFD0D8DE),
                                lineHeight = 15.sp
                            )

                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = when (detailInstallState) {
                                    StoreInstallState.INSTALLED -> Color(0xFF1E3E29)
                                    StoreInstallState.UPDATE_AVAILABLE -> Color(0xFFFFB74D)
                                    StoreInstallState.NOT_INSTALLED -> SkyBlueAccent
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .animatedPressScale(enabled = !storeProgress.running) {
                                        when (detailInstallState) {
                                            StoreInstallState.INSTALLED -> {
                                                Toast.makeText(
                                                    context,
                                                    "${detailProj.title} (v${installedMap[detailProj.id] ?: detailProj.version}) sudah terpasang.",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                            StoreInstallState.UPDATE_AVAILABLE, StoreInstallState.NOT_INSTALLED -> {
                                                if (detailProj.isCompatibleWith26_3) startItemDownload(detailProj)
                                                else pendingIncompatibleItem = detailProj
                                            }
                                        }
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(vertical = 8.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = when (detailInstallState) {
                                            StoreInstallState.INSTALLED -> Icons.Default.CheckCircle
                                            StoreInstallState.UPDATE_AVAILABLE -> Icons.Default.SystemUpdateAlt
                                            StoreInstallState.NOT_INSTALLED -> Icons.Default.Download
                                        },
                                        contentDescription = null,
                                        tint = when (detailInstallState) {
                                            StoreInstallState.INSTALLED -> Color(0xFF81C784)
                                            StoreInstallState.UPDATE_AVAILABLE -> Color(0xFF261600)
                                            StoreInstallState.NOT_INSTALLED -> SkyBlueDarkText
                                        },
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(Modifier.width(5.dp))
                                    Text(
                                        text = when (detailInstallState) {
                                            StoreInstallState.INSTALLED -> "✓ Installed (v${installedMap[detailProj.id] ?: detailProj.version})"
                                            StoreInstallState.UPDATE_AVAILABLE -> "↑ Update (${installedMap[detailProj.id] ?: "old"} → v${detailProj.version})"
                                            StoreInstallState.NOT_INSTALLED -> "Install v${detailProj.version}"
                                        },
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = when (detailInstallState) {
                                            StoreInstallState.INSTALLED -> Color(0xFF81C784)
                                            StoreInstallState.UPDATE_AVAILABLE -> Color(0xFF261600)
                                            StoreInstallState.NOT_INSTALLED -> SkyBlueDarkText
                                        }
                                    )
                                }
                            }

                            if (detailInstallState == StoreInstallState.INSTALLED || detailInstallState == StoreInstallState.UPDATE_AVAILABLE) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0xFF3A2326),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .animatedPressScale {
                                            HorizonNativeBridge.uninstallStoreItem(context, detailProj, profileSlot, logEnabled)
                                            installedMap = HorizonNativeBridge.getInstalledStoreVersionsMap(context, profileSlot)
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(vertical = 8.dp),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Outlined.Delete, contentDescription = null, tint = Color(0xFFFF8A80), modifier = Modifier.size(15.dp))
                                        Spacer(Modifier.width(5.dp))
                                        Text("Uninstall from Horizon", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFF8A80))
                                    }
                                }
                            }

                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(7.dp)
                            ) {
                                Text(
                                    text = "Related Links",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )

                                if (detailProj.pageUrl.isNotBlank()) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .animatedPressScale {
                                                runCatching {
                                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(detailProj.pageUrl)))
                                                }
                                            }
                                            .padding(vertical = 2.dp)
                                    ) {
                                        Icon(Icons.Default.Language, contentDescription = null, tint = Color(0xFFD5DEE5), modifier = Modifier.size(15.dp))
                                        Text("playhorizon.dev Page", fontSize = 11.sp, color = Color(0xFFD5DEE5))
                                    }
                                }

                                if (detailProj.sourceCodeUrl.isNotBlank()) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .animatedPressScale {
                                                runCatching {
                                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(detailProj.sourceCodeUrl)))
                                                }
                                            }
                                            .padding(vertical = 2.dp)
                                    ) {
                                        Icon(Icons.Default.Code, contentDescription = null, tint = Color(0xFFD5DEE5), modifier = Modifier.size(15.dp))
                                        Text("View Source Code", fontSize = 11.sp, color = Color(0xFFD5DEE5))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StoreRightSearchPill(
    value: String,
    placeholder: String,
    onValueChange: (String) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(50),
        color = CardRowBg,
        modifier = Modifier
            .fillMaxWidth()
            .height(36.dp)
            .border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(50))
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.CenterStart
            ) {
                if (value.isEmpty()) {
                    Text(placeholder, fontSize = 11.sp, color = MutedSubtitleText, maxLines = 1)
                }
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    singleLine = true,
                    textStyle = TextStyle(color = Color.White, fontSize = 11.sp),
                    cursorBrush = SolidColor(SkyBlueAccent),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = placeholder,
                tint = Color.White,
                modifier = Modifier.size(15.dp)
            )
        }
    }
}

@Composable
private fun StoreAsyncProjectIcon(
    iconUrl: String,
    title: String,
    isMod: Boolean,
    isWorld: Boolean,
    size: Dp = 44.dp
) {
    var loadedBmp by remember(iconUrl) { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(iconUrl) {
        if (iconUrl.isNotBlank()) {
            loadedBmp = withContext(Dispatchers.IO) {
                HorizonNativeBridge.loadProjectIconBitmap(iconUrl)
            }
        }
    }
    if (loadedBmp != null) {
        Image(
            bitmap = loadedBmp!!.asImageBitmap(),
            contentDescription = title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(size)
                .clip(RoundedCornerShape(10.dp))
                .border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(10.dp))
        )
    } else {
        StoreOrFileItemIconBadge(
            title = title,
            isMod = isMod,
            isWorld = isWorld,
            size = size
        )
    }
}

@Composable
private fun StoreOrFileItemIconBadge(
    title: String,
    isMod: Boolean,
    isWorld: Boolean,
    size: Dp = 44.dp
) {
    // Dark square pixel-styled initial badge matching playhorizon.dev/mods (`429816.jpg`)
    val bgBrush = when {
        isWorld -> Brush.linearGradient(listOf(Color(0xFF43A047), Color(0xFF5D4037)))
        isMod -> Brush.linearGradient(listOf(Color(0xFF141820), Color(0xFF222A36)))
        else -> Brush.linearGradient(listOf(Color(0xFF281E3D), Color(0xFF1B3538)))
    }
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(10.dp))
            .background(bgBrush)
            .border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(10.dp))
    ) {
        if (isWorld) {
            MinecraftGrassCubeIcon(size = size * 0.76f)
        } else {
            val initials = title.trim().take(2).uppercase()
            Text(
                text = initials,
                fontSize = 15.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
            )
        }
    }
}

private data class ManagerRowUiItem(
    val key: String,
    val file: File,
    val folder: String,
    val displayTitle: String,
    val subtitleText: String,
    val detailSummary: String,
    val isEnabled: Boolean,
    val lastModified: Long,
    val modDetail: ModPackageDetailInfo?,
    val saveDetail: WorldSaveDetailInfo?,
    val packDetail: ResourcePackDetailInfo?,
    val iconImage: ImageBitmap?,
    val storeCatalogMatch: StoreCatalogItem? = null,
    val hasStoreUpdate: Boolean = false
)

enum class FilesSortMode(val label: String) {
    BY_NAME("Sort by Name"),
    BY_FILE_NAME("Sort by File Name"),
    BY_LAST_MODIFIED("Sort by Last Updated")
}

private data class PendingDeleteTarget(
    val file: File,
    val displayTitle: String,
    val folder: String,
    val folderLabel: String
)

/**
 * Synchronized Files & Mods Manager (`CleanFilesViewport`):
 * - 100% two-way synced with the `playhorizon.dev/mods` Store (`horizon-mods/<id>.hmod`, `zalith-store-installed.json`,
 *   `horizon-mods.txt`, `hmod_list.json`, `options.txt`).
 * - Automatically deduplicates and self-heals `.hmod` ZIP archives on open & refresh (`syncAndRepairModsStorage`).
 * - Includes an animated Delete Confirmation Pop-Up (`AlertDialog`) before deleting any Mod, Save, Resource Pack,
 *   Shader Pack, or Screenshot.
 * - Includes smooth transition animations (`AnimatedContent`, `AnimatedVisibility`, `animateColorAsState`,
 *   `animateFloatAsState`) across folders, rows, switches, and expandable details.
 */
@Composable
private fun CleanFilesViewport(
    profileSlot: Int,
    logEnabled: Boolean,
    onNavigateToStoreCategory: (StoreCategory) -> Unit,
    onQuickPlayWorld: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val home = remember(profileSlot) {
        HorizonNativeBridge.getProfileHome(HorizonNativeBridge.ensureHomeDirectories(context), profileSlot)
    }
    var currentFolder by remember { mutableStateOf("mods") }
    var searchQuery by remember { mutableStateOf("") }
    var filterEnabledOnly by remember { mutableStateOf(false) }
    var sortMode by remember { mutableStateOf(FilesSortMode.BY_NAME) }
    var showSortDropdown by remember { mutableStateOf(false) }
    var refreshCount by remember { mutableIntStateOf(0) }

    // For Rename Dialog
    var renameTargetFile by remember { mutableStateOf<File?>(null) }
    var renameInputText by remember { mutableStateOf("") }

    // For Delete Confirmation Pop-Up Dialog (`AlertDialog`)
    var pendingDeleteTarget by remember { mutableStateOf<PendingDeleteTarget?>(null) }

    val categories = listOf(
        Triple("mods", "Mods", Icons.Default.Extension),
        Triple("modpacks", "Modpacks", Icons.Default.Inventory2),
        Triple("saves", "Saves", Icons.Default.Public),
        Triple("resourcepacks", "Resource Packs", Icons.Default.Brush),
        Triple("shaderpacks", "Shader Packs", Icons.Default.Lightbulb),
        Triple("screenshots", "Screenshots", Icons.Default.Image)
    )

    var filteredList by remember { mutableStateOf<List<ManagerRowUiItem>>(emptyList()) }

    LaunchedEffect(currentFolder, searchQuery, filterEnabledOnly, sortMode, refreshCount, profileSlot) {
        val folderSnap = currentFolder
        // Clear any stale items from another folder immediately so Mods never bleed into Shaders/ResourcePacks/Saves!
        filteredList = filteredList.filter { it.folder == folderSnap }
        val computed = withContext(Dispatchers.IO) {
            HorizonNativeBridge.syncAllContentDirectoriesTwoWay(context, profileSlot)
            val loadedFiles: List<File> = when (folderSnap) {
                "mods" -> {
                    HorizonNativeBridge.syncAndRepairModsStorage(context, profileSlot)
                    val hmodDir = File(home, "horizon-mods").apply { mkdirs() }
                    hmodDir.listFiles()
                        ?.filter { f ->
                            f.isFile &&
                                !f.name.endsWith(".part") &&
                                !f.name.endsWith(".tmp") &&
                                (f.name.endsWith(".hmod", true) || f.name.endsWith(".hnmod", true))
                        }
                        ?.distinctBy { HorizonNativeBridge.sanitizeModId(it.name) }
                        .orEmpty()
                }
                "shaderpacks" -> {
                    HorizonNativeBridge.syncAndRepairShadersStorage(context, profileSlot)
                    val shaderDir = File(home, "shaderpacks").apply { mkdirs() }
                    shaderDir.listFiles()
                        ?.filter { f ->
                            !f.name.endsWith(".part") &&
                                !f.name.endsWith(".tmp") &&
                                !f.name.endsWith(".hmod", true) &&
                                !f.name.endsWith(".hnmod", true) &&
                                !f.name.endsWith(".hpack", true) &&
                                (f.isDirectory || f.name.endsWith(".hshader", true) || f.name.endsWith(".zip", true))
                        }
                        .orEmpty()
                }
                "resourcepacks" -> {
                    val rpDir = File(home, "resourcepacks").apply { mkdirs() }
                    rpDir.listFiles()
                        ?.filter { f ->
                            !f.name.endsWith(".part") &&
                                !f.name.endsWith(".tmp") &&
                                !f.name.endsWith(".hmod", true) &&
                                !f.name.endsWith(".hnmod", true) &&
                                !f.name.endsWith(".hshader", true) &&
                                !f.name.endsWith(".hpack", true) &&
                                !f.name.equals("HorizonZ-CustomSkin.zip", ignoreCase = true) &&
                                (f.isDirectory || f.name.endsWith(".zip", true))
                        }
                        .orEmpty()
                }
                "modpacks" -> {
                    val packDir = File(home, "horizon-packs").apply { mkdirs() }
                    packDir.listFiles()
                        ?.filter { f ->
                            !f.name.endsWith(".part") &&
                                !f.name.endsWith(".tmp") &&
                                !f.name.endsWith(".hmod", true) &&
                                (f.isFile && f.name.endsWith(".hpack", true) || f.isDirectory)
                        }
                        .orEmpty()
                }
                "saves" -> {
                    val savesDir = File(home, "saves").apply { mkdirs() }
                    savesDir.listFiles()
                        ?.filter { f ->
                            !f.name.endsWith(".part") &&
                                !f.name.endsWith(".tmp") &&
                                !f.name.endsWith(".hmod", true) &&
                                (f.isDirectory || f.name.endsWith(".zip", true))
                        }
                        .orEmpty()
                }
                "screenshots" -> {
                    val shotsDir = File(home, "screenshots").apply { mkdirs() }
                    shotsDir.listFiles()
                        ?.filter { f ->
                            f.isFile &&
                                !f.name.endsWith(".part") &&
                                !f.name.endsWith(".tmp") &&
                                (f.name.endsWith(".png", true) || f.name.endsWith(".jpg", true) || f.name.endsWith(".jpeg", true) || f.name.endsWith(".webp", true))
                        }
                        .orEmpty()
                }
                else -> {
                    val dirFile = File(home, folderSnap).apply { mkdirs() }
                    dirFile.listFiles()?.filter { !it.name.endsWith(".part") && !it.name.endsWith(".tmp") }.orEmpty()
                }
            }

            val installedVersionsMap = HorizonNativeBridge.getInstalledStoreVersionsMap(context, profileSlot)
            val isToggleableFolder = folderSnap in listOf("mods", "shaderpacks", "resourcepacks")
            val prebuilt = loadedFiles.mapNotNull { file ->
                val modDetail = if (folderSnap == "mods") HorizonNativeBridge.inspectModPackage(file) else null
                val saveDetail = if (folderSnap == "saves") HorizonNativeBridge.inspectWorldSave(file) else null
                val packDetail = if (folderSnap in listOf("resourcepacks", "shaderpacks")) HorizonNativeBridge.inspectResourcePack(file) else null
                val enabled = if (isToggleableFolder) {
                    HorizonNativeBridge.isContentItemEnabled(context, folderSnap, file.name, profileSlot)
                } else true

                val displayTitle = when {
                    modDetail != null -> modDetail.displayName
                    saveDetail != null -> saveDetail.displayName
                    packDetail != null -> packDetail.title
                    else -> file.nameWithoutExtension.replace("-", " ").replace("_", " ")
                }
                val subtitleText = when {
                    modDetail != null -> "${modDetail.description} • by ${modDetail.author} (${modDetail.fileName})"
                    saveDetail != null -> "Last played: ${saveDetail.lastPlayedFormatted}"
                    packDetail != null -> packDetail.description
                    else -> "File Name: ${file.name}"
                }

                val matchesQuery = searchQuery.isBlank() ||
                    file.name.contains(searchQuery, ignoreCase = true) ||
                    displayTitle.contains(searchQuery, ignoreCase = true) ||
                    subtitleText.contains(searchQuery, ignoreCase = true)
                val matchesEnabled = !filterEnabledOnly || !isToggleableFolder || enabled

                if (!matchesQuery || !matchesEnabled) {
                    null
                } else {
                    val iconBmp = HorizonNativeBridge.loadLocalContentIconBitmap(file)?.asImageBitmap()
                    val detailSummary = when {
                        modDetail != null -> "ID: ${modDetail.modId} • API ${modDetail.apiLine} • Side: ${modDetail.side} • Size: ${modDetail.fileSizeFormatted}"
                        packDetail != null -> "${packDetail.packType} • Format v${packDetail.formatVersion} • Size: ${packDetail.fileSizeFormatted}"
                        saveDetail != null -> "${saveDetail.difficulty} • ${saveDetail.playTimeFormatted} • Seed: ${saveDetail.worldSeed}"
                        else -> "File: ${file.name} (${file.length() / 1024} KB)"
                    }
                    val catMatch = HorizonNativeBridge.findCatalogItemForLocalFile(file, folderSnap)
                    val cleanFileId = HorizonNativeBridge.sanitizeModId(file.name)
                    val recordedVer = installedVersionsMap[cleanFileId]
                        ?: modDetail?.let { installedVersionsMap[it.modId] }
                        ?: catMatch?.let { installedVersionsMap[it.id] ?: installedVersionsMap[it.slug] }
                    val rawManifestVer = modDetail?.version
                    val curVer = when {
                        !recordedVer.isNullOrBlank() && !rawManifestVer.isNullOrBlank() -> {
                            val rT = HorizonNativeBridge.parseVersionNumbers(recordedVer)
                            val mT = HorizonNativeBridge.parseVersionNumbers(rawManifestVer)
                            if (rT != null && mT != null && HorizonNativeBridge.compareVersionLists(rT, mT) >= 0) recordedVer else rawManifestVer
                        }
                        !recordedVer.isNullOrBlank() -> recordedVer
                        !rawManifestVer.isNullOrBlank() -> rawManifestVer
                        else -> "1.0.0"
                    }
                    val curTuple = HorizonNativeBridge.parseVersionNumbers(curVer)
                    val latestTuple = catMatch?.let { HorizonNativeBridge.parseVersionNumbers(it.version) }
                    val updateAvailable = catMatch != null && curTuple != null && latestTuple != null &&
                        HorizonNativeBridge.compareVersionLists(latestTuple, curTuple) > 0
                    ManagerRowUiItem(
                        key = "$folderSnap:${file.absolutePath}:${file.lastModified()}:$curVer:$enabled",
                        file = file,
                        folder = folderSnap,
                        displayTitle = displayTitle,
                        subtitleText = subtitleText,
                        detailSummary = detailSummary,
                        isEnabled = enabled,
                        lastModified = file.lastModified(),
                        modDetail = modDetail?.copy(version = curVer),
                        saveDetail = saveDetail,
                        packDetail = packDetail,
                        iconImage = iconBmp,
                        storeCatalogMatch = catMatch,
                        hasStoreUpdate = updateAvailable
                    )
                }
            }
            when (sortMode) {
                FilesSortMode.BY_NAME -> prebuilt.sortedBy { it.displayTitle.lowercase(Locale.US) }
                FilesSortMode.BY_FILE_NAME -> prebuilt.sortedBy { it.file.name.lowercase(Locale.US) }
                FilesSortMode.BY_LAST_MODIFIED -> prebuilt.sortedByDescending { it.lastModified }
            }
        }
        if (currentFolder == folderSnap) {
            filteredList = computed
        }
    }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            val folderSnap = currentFolder
            val ext = when (folderSnap) {
                "mods" -> ".hmod"
                "modpacks" -> ".hpack"
                "shaderpacks" -> ".hshader"
                "screenshots" -> ".png"
                else -> ".zip"
            }
            scope.launch {
                val importedFile: File? = withContext(Dispatchers.IO) {
                    HorizonNativeBridge.importUriToDestination(
                        context,
                        uri,
                        folderSnap,
                        "imported_${System.currentTimeMillis()}$ext",
                        logEnabled,
                        profileSlot
                    )
                }
                val ok: Boolean = importedFile != null
                refreshCount += 1
                Toast.makeText(
                    context,
                    if (ok) "Berhasil mengimpor ke $folderSnap & menyinkronkan ke engine!" else "Gagal mengimpor file.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    // Rename Modal Dialog
    renameTargetFile?.let { target ->
        AlertDialog(
            onDismissRequest = { renameTargetFile = null },
            icon = { Icon(Icons.Default.Edit, contentDescription = null, tint = SkyBlueAccent) },
            title = { Text("Ubah Nama ${target.name}", style = MaterialTheme.typography.titleMedium) },
            text = {
                OutlinedTextField(
                    value = renameInputText,
                    onValueChange = { renameInputText = it },
                    label = { Text("Nama Baru", fontSize = 12.sp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (HorizonNativeBridge.renameContentFile(context, target, renameInputText, logEnabled)) {
                            refreshCount += 1
                        }
                        renameTargetFile = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SkyBlueAccent)
                ) {
                    Text("Simpan", color = SkyBlueDarkText, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            },
            dismissButton = {
                TextButton(onClick = { renameTargetFile = null }) {
                    Text("Batal", fontSize = 12.sp)
                }
            }
        )
    }

    // Delete Confirmation Pop-Up Dialog (`AlertDialog` requested by user!)
    pendingDeleteTarget?.let { delTarget ->
        AlertDialog(
            onDismissRequest = { pendingDeleteTarget = null },
            icon = {
                Icon(
                    imageVector = Icons.Default.WarningAmber,
                    contentDescription = null,
                    tint = Color(0xFFFF8A80)
                )
            },
            title = {
                Text(
                    text = "Hapus ${delTarget.displayTitle}?",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Apakah Anda yakin ingin menghapus '${delTarget.file.name}' dari kategori ${delTarget.folderLabel}?\n\nFile ini akan dihapus secara permanen dari perangkat dan statusnya di Store akan otomatis direset menjadi belum terpasang.",
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val target = delTarget
                        pendingDeleteTarget = null
                        HorizonNativeBridge.deleteContentItemWithSync(
                            context = context,
                            targetFile = target.file,
                            folder = target.folder,
                            profileSlot = profileSlot,
                            logEnabled = logEnabled
                        )
                        refreshCount += 1
                        Toast.makeText(
                            context,
                            "${target.displayTitle} berhasil dihapus.",
                            Toast.LENGTH_SHORT
                        ).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935))
                ) {
                    Icon(Icons.Outlined.Delete, contentDescription = null, tint = Color.White, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(5.dp))
                    Text("Ya, Hapus Permanen", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteTarget = null }) {
                    Text("Batal", fontSize = 12.sp)
                }
            }
        )
    }

    Row(
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Left Sub-Navigation Rail (`Mods`, `Saves`, `Resource Packs`, `Shader Packs`, `Screenshots`)
        ZalithSubCategoryRail(
            items = categories,
            selectedItem = categories.first { it.first == currentFolder },
            itemLabel = { it.second },
            itemIcon = { it.third },
            onSelect = { currentFolder = it.first }
        )

        // Right Main Rounded Slate Container
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = CardSlateBg,
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Top Action Toolbar (`Filter  Sort  [ Search... ]  [ ] Enabled Only  + Import  ↓ Store  ↻`)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (currentFolder == "mods") {
                        Icon(
                            imageVector = Icons.Default.FilterAlt,
                            contentDescription = "Filter Enabled Mods",
                            tint = if (filterEnabledOnly) SkyBlueAccent else Color.White,
                            modifier = Modifier
                                .size(19.dp)
                                .animatedPressScale { filterEnabledOnly = !filterEnabledOnly }
                        )
                    }

                    // Sort Icon with Floating Dropdown
                    Box {
                        Icon(
                            imageVector = Icons.Default.Sort,
                            contentDescription = "Sort",
                            tint = Color.White,
                            modifier = Modifier
                                .size(19.dp)
                                .animatedPressScale { showSortDropdown = true }
                        )
                        DropdownMenu(
                            expanded = showSortDropdown,
                            onDismissRequest = { showSortDropdown = false },
                            modifier = Modifier.background(Color(0xFF283036))
                        ) {
                            FilesSortMode.entries.forEach { mode ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = mode.label,
                                            fontSize = 12.sp,
                                            fontWeight = if (sortMode == mode) FontWeight.Bold else FontWeight.Normal,
                                            color = if (sortMode == mode) SkyBlueAccent else Color.White
                                        )
                                    },
                                    onClick = {
                                        sortMode = mode
                                        showSortDropdown = false
                                    }
                                )
                            }
                        }
                    }

                    // Pill Search Input
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = CardRowBg,
                        modifier = Modifier
                            .weight(1f)
                            .height(34.dp)
                            .border(1.dp, Color(0xFF3E4A52), RoundedCornerShape(50))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 14.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            if (searchQuery.isEmpty()) {
                                Text(
                                    text = "Search ${categories.first { it.first == currentFolder }.second.lowercase(Locale.US)}...",
                                    fontSize = 12.sp,
                                    color = MutedSubtitleText
                                )
                            }
                            BasicTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                singleLine = true,
                                textStyle = TextStyle(color = Color.White, fontSize = 12.sp),
                                cursorBrush = SolidColor(SkyBlueAccent),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    // Item counter badge
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = CardRowBg
                    ) {
                        Text(
                            text = "${filteredList.size} items",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = SkyBlueAccent,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }

                    if (currentFolder in listOf("saves", "resourcepacks", "shaderpacks")) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.animatedPressScale { filterEnabledOnly = !filterEnabledOnly }
                        ) {
                            Checkbox(
                                checked = filterEnabledOnly,
                                onCheckedChange = { filterEnabledOnly = it },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = SkyBlueAccent,
                                    checkmarkColor = SkyBlueDarkText,
                                    uncheckedColor = MutedSubtitleText
                                ),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text("Active", fontSize = 11.sp, color = Color.White)
                        }
                    }

                    // `+ Import` action button
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = CardRowBg,
                        modifier = Modifier.animatedPressScale { picker.launch("*/*") }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Import", tint = SkyBlueAccent, modifier = Modifier.size(15.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Import", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                        }
                    }

                    // `↓ Store` action button (switches directly to Store tab with matching category!)
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = SubRailCapsuleActive,
                        modifier = Modifier.animatedPressScale {
                            val targetCat = when (currentFolder) {
                                "mods" -> StoreCategory.MOD
                                "modpacks", "saves" -> StoreCategory.MODPACK
                                "resourcepacks" -> StoreCategory.RESOURCE_PACK
                                "shaderpacks" -> StoreCategory.SHADER_PACK
                                else -> StoreCategory.MOD
                            }
                            onNavigateToStoreCategory(targetCat)
                        }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.Download, contentDescription = "Store", tint = SkyBlueAccent, modifier = Modifier.size(15.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Store", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = SkyBlueAccent)
                        }
                    }

                    // `↻` Refresh & Auto-Repair icon button
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh & Sync",
                        tint = Color.White,
                        modifier = Modifier
                            .size(20.dp)
                            .animatedPressScale {
                                refreshCount += 1
                                Toast.makeText(context, "Folder ${currentFolder} disinkronkan & diverifikasi!", Toast.LENGTH_SHORT).show()
                            }
                    )
                }

                // Smooth Animated List Content across folders and filter states
                AnimatedContent(
                    targetState = currentFolder,
                    transitionSpec = {
                        (fadeIn(tween(240, easing = FastOutSlowInEasing)) +
                            slideInVertically(tween(240, easing = FastOutSlowInEasing)) { h -> h / 12 } +
                            scaleIn(initialScale = 0.98f, animationSpec = tween(240, easing = FastOutSlowInEasing)))
                            .togetherWith(
                                fadeOut(tween(150)) +
                                    scaleOut(targetScale = 0.98f, animationSpec = tween(150))
                            )
                            .using(sizeTransform = null)
                    },
                    modifier = Modifier.fillMaxSize(),
                    label = "FilesFolderAnimatedTransition"
                ) { folder ->
                    val folderLabel = categories.firstOrNull { it.first == folder }?.second ?: folder
                    val folderItems = remember(filteredList, folder) { filteredList.filter { it.folder == folder } }
                    if (folderItems.isEmpty()) {
                        val emptyLabel = when (folder) {
                            "mods" -> "Belum ada mod terpasang untuk profil ini (Klik 'Store' untuk mengunduh dari playhorizon.dev/mods)"
                            "modpacks" -> "Belum ada Modpack (.hpack) terpasang untuk profil ini (Klik 'Store' untuk mengunduh Modpack)"
                            "saves" -> "Belum ada dunia (World Save) untuk versi 26.3"
                            "resourcepacks" -> "Belum ada Resource Pack untuk versi 26.3"
                            "shaderpacks" -> "Belum ada Shader Pack (.hshader) untuk versi 26.3"
                            "screenshots" -> "Belum ada tangkapan layar (Screenshots)"
                            else -> "Tidak ada item di folder ini"
                        }
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = CardRowBg
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(Icons.Default.FolderOpen, contentDescription = null, tint = SkyBlueAccent, modifier = Modifier.size(18.dp))
                                    Text(
                                        text = emptyLabel,
                                        fontSize = 12.sp,
                                        color = Color(0xFFD5DEE5)
                                    )
                                }
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(
                                items = folderItems,
                                key = { it.key },
                                contentType = { it.folder }
                            ) { rowItem ->
                                FileManagerRowCard(
                                    rowItem = rowItem,
                                    folderLabel = folderLabel,
                                    profileSlot = profileSlot,
                                    logEnabled = logEnabled,
                                    onTriggerRefresh = { refreshCount += 1 },
                                    onRequestRename = { target ->
                                        renameInputText = target.nameWithoutExtension
                                        renameTargetFile = target
                                    },
                                    onRequestDelete = { targetFile, title ->
                                        pendingDeleteTarget = PendingDeleteTarget(
                                            file = targetFile,
                                            displayTitle = title,
                                            folder = folder,
                                            folderLabel = folderLabel
                                        )
                                    },
                                    onQuickPlayWorld = onQuickPlayWorld
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FileManagerRowCard(
    rowItem: ManagerRowUiItem,
    folderLabel: String,
    profileSlot: Int,
    logEnabled: Boolean,
    onTriggerRefresh: () -> Unit,
    onRequestRename: (File) -> Unit,
    onRequestDelete: (File, String) -> Unit,
    onQuickPlayWorld: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val file = rowItem.file
    val folder = rowItem.folder
    val displayTitle = rowItem.displayTitle
    val subtitleText = rowItem.subtitleText
    val isEnabled = rowItem.isEnabled
    val modDetail = rowItem.modDetail
    val saveDetail = rowItem.saveDetail
    val packDetail = rowItem.packDetail

    var showInfoPopover by remember { mutableStateOf(false) }
    var showMoreMenu by remember { mutableStateOf(false) }
    var expandedDetails by remember { mutableStateOf(false) }

    val isToggleable = folder in listOf("mods", "shaderpacks", "resourcepacks")
    val borderColor = if (isToggleable && isEnabled) SkyBlueAccent.copy(alpha = 0.38f) else Color.Transparent
    val cardColor = if (expandedDetails) SubRailCapsuleActive.copy(alpha = 0.55f) else CardRowBg

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = cardColor,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, borderColor, RoundedCornerShape(16.dp))
            .clickable { expandedDetails = !expandedDetails }
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (rowItem.iconImage != null) {
                    Image(
                        bitmap = rowItem.iconImage,
                        contentDescription = displayTitle,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(10.dp))
                    )
                } else {
                    StoreOrFileItemIconBadge(
                        title = displayTitle,
                        isMod = folder == "mods",
                        isWorld = folder == "saves",
                        size = 44.dp
                    )
                }

                Spacer(Modifier.width(12.dp))

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = displayTitle,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        when (folder) {
                            "mods" -> {
                                LavenderTagBadge(text = "v${modDetail?.version ?: "1.0.0"}")
                                LavenderTagBadge(text = "26.3")
                                if (rowItem.hasStoreUpdate && rowItem.storeCatalogMatch != null) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = Color(0xFFFFB74D),
                                        modifier = Modifier.clickable {
                                            val targetItem = rowItem.storeCatalogMatch
                                            Toast.makeText(context, "Mengupdate ${targetItem.title} ke v${targetItem.version}...", Toast.LENGTH_SHORT).show()
                                            HorizonNativeBridge.downloadAndInstallStoreItem(
                                                context = context,
                                                item = targetItem,
                                                profileSlot = profileSlot,
                                                logEnabled = logEnabled,
                                                onProgress = {},
                                                onComplete = { _, msg ->
                                                    android.os.Handler(android.os.Looper.getMainLooper()).post {
                                                        onTriggerRefresh()
                                                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                            )
                                        }
                                    ) {
                                        Text(
                                            text = "↑ Update v${rowItem.storeCatalogMatch.version}",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color(0xFF261600),
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (isEnabled) Color(0xFF1E3E29) else Color(0xFF3A2326)
                                ) {
                                    Text(
                                        text = if (isEnabled) "ON" else "OFF",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (isEnabled) Color(0xFF81C784) else Color(0xFFFF8A80),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            "saves" -> {
                                LavenderTagBadge(text = "26.3")
                                LavenderTagBadge(text = saveDetail?.gameMode ?: "Survival Mode")
                            }
                            "resourcepacks", "shaderpacks" -> {
                                LavenderTagBadge(text = "26.3")
                                if (isEnabled) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = Color(0xFF1E3E29)
                                    ) {
                                        Text(
                                            text = "ACTIVE",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color(0xFF81C784),
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                    Text(
                        text = subtitleText,
                        fontSize = 11.sp,
                        color = MutedSubtitleText,
                        maxLines = 2,
                        lineHeight = 14.sp,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(Modifier.width(10.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (isToggleable) {
                        Checkbox(
                            checked = isEnabled,
                            onCheckedChange = {
                                scope.launch(Dispatchers.IO) {
                                    HorizonNativeBridge.toggleContentItem(context, folder, file.name, profileSlot, logEnabled)
                                    withContext(Dispatchers.Main) {
                                        onTriggerRefresh()
                                    }
                                }
                            },
                            colors = CheckboxDefaults.colors(
                                checkedColor = SkyBlueAccent,
                                checkmarkColor = SkyBlueDarkText,
                                uncheckedColor = MutedSubtitleText
                            ),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Box {
                        Icon(
                            imageVector = Icons.Outlined.Info,
                            contentDescription = "Info",
                            tint = Color(0xFFD5DEE5),
                            modifier = Modifier
                                .size(20.dp)
                                .clickable { showInfoPopover = true }
                        )

                        if (showInfoPopover) {
                            DropdownMenu(
                                expanded = showInfoPopover,
                                onDismissRequest = { showInfoPopover = false },
                                modifier = Modifier
                                    .widthIn(min = 260.dp, max = 330.dp)
                                    .background(Color(0xFF283036))
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    if (saveDetail != null) {
                                        Text("Save Info", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                        Spacer(Modifier.height(2.dp))
                                        Text("File Name: ${saveDetail.folderName}", fontSize = 12.sp, color = Color(0xFFE0E6EB))
                                        Text("Game Mode: ${saveDetail.gameMode}", fontSize = 12.sp, color = Color(0xFFE0E6EB))
                                        Text("Difficulty: ${saveDetail.difficulty}", fontSize = 12.sp, color = Color(0xFFE0E6EB))
                                        Text("Play Time: ${saveDetail.playTimeFormatted}", fontSize = 12.sp, color = Color(0xFFE0E6EB))
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = "World Seed: ${saveDetail.worldSeed}",
                                                fontSize = 12.sp,
                                                color = Color(0xFFE0E6EB),
                                                modifier = Modifier.weight(1f, fill = false)
                                            )
                                            Icon(
                                                imageVector = Icons.Default.ContentCopy,
                                                contentDescription = "Copy Seed",
                                                tint = SkyBlueAccent,
                                                modifier = Modifier
                                                    .size(15.dp)
                                                    .clickable {
                                                        HorizonNativeBridge.copyToClipboard(context, "World Seed", saveDetail.worldSeed)
                                                    }
                                            )
                                        }
                                    } else if (packDetail != null) {
                                        Text(
                                            text = if (folder == "shaderpacks") "Shader Pack Information" else "Resource Pack Information",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                        Spacer(Modifier.height(2.dp))
                                        Text("Pack Type: ${packDetail.packType}", fontSize = 12.sp, color = Color(0xFFE0E6EB))
                                        Text("File Name: ${packDetail.fileName}", fontSize = 12.sp, color = Color(0xFFE0E6EB))
                                        Text("File Size: ${packDetail.fileSizeFormatted}", fontSize = 12.sp, color = Color(0xFFE0E6EB))
                                        Text("Format Version: ${packDetail.formatVersion}", fontSize = 12.sp, color = Color(0xFFE0E6EB))
                                    } else if (modDetail != null) {
                                        Text("Horizon Mod Manifest (horizon-mod.toml)", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                        Spacer(Modifier.height(2.dp))
                                        Text("Mod ID: ${modDetail.modId}", fontSize = 12.sp, color = SkyBlueAccent)
                                        Text("Version: ${modDetail.version} (API ${modDetail.apiLine})", fontSize = 12.sp, color = Color(0xFFE0E6EB))
                                        Text("Author: ${modDetail.author}", fontSize = 12.sp, color = Color(0xFFE0E6EB))
                                        Text("File: ${modDetail.fileName} (${modDetail.fileSizeFormatted})", fontSize = 12.sp, color = Color(0xFFE0E6EB))
                                        Text(
                                            text = "Capabilities: ${modDetail.capabilities.joinToString(", ").ifEmpty { "registries" }}",
                                            fontSize = 12.sp,
                                            color = Color(0xFF81C784)
                                        )
                                        Text(
                                            text = "Status: ${if (isEnabled) "ON (Synced with horizon-mods.txt)" else "OFF (Disabled)"}",
                                            fontSize = 12.sp,
                                            color = if (isEnabled) Color(0xFF81C784) else Color(0xFFFF8A80)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (folder == "mods") {
                        Icon(
                            imageVector = Icons.Outlined.Delete,
                            contentDescription = "Delete Mod",
                            tint = Color(0xFFFF8A80),
                            modifier = Modifier
                                .size(20.dp)
                                .clickable {
                                    onRequestDelete(file, displayTitle)
                                }
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Outlined.Delete,
                            contentDescription = "Delete",
                            tint = Color(0xFFFF8A80),
                            modifier = Modifier
                                .size(19.dp)
                                .clickable {
                                    onRequestDelete(file, displayTitle)
                                }
                        )
                        Box {
                            Icon(
                                imageVector = Icons.Default.MoreHoriz,
                                contentDescription = "More Options",
                                tint = Color(0xFFD5DEE5),
                                modifier = Modifier
                                    .size(20.dp)
                                    .clickable { showMoreMenu = true }
                            )

                            if (showMoreMenu) {
                                DropdownMenu(
                                    expanded = showMoreMenu,
                                    onDismissRequest = { showMoreMenu = false },
                                    modifier = Modifier.background(Color(0xFF283036))
                                ) {
                                    if (folder == "saves") {
                                        DropdownMenuItem(
                                            leadingIcon = { Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(17.dp)) },
                                            text = { Text("Quick Play", fontSize = 12.sp, color = Color.White) },
                                            onClick = {
                                                showMoreMenu = false
                                                onQuickPlayWorld()
                                            }
                                        )
                                        DropdownMenuItem(
                                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, tint = Color.White, modifier = Modifier.size(17.dp)) },
                                            text = { Text("Rename", fontSize = 12.sp, color = Color.White) },
                                            onClick = {
                                                showMoreMenu = false
                                                onRequestRename(file)
                                            }
                                        )
                                        DropdownMenuItem(
                                            leadingIcon = { Icon(Icons.Default.Save, contentDescription = null, tint = Color.White, modifier = Modifier.size(17.dp)) },
                                            text = { Text("Backup Save", fontSize = 12.sp, color = Color.White) },
                                            onClick = {
                                                showMoreMenu = false
                                                HorizonNativeBridge.backupWorldSave(context, file, logEnabled)
                                            }
                                        )
                                        DropdownMenuItem(
                                            leadingIcon = { Icon(Icons.Outlined.Delete, contentDescription = null, tint = Color(0xFFFF8A80), modifier = Modifier.size(17.dp)) },
                                            text = { Text("Delete", fontSize = 12.sp, color = Color(0xFFFF8A80)) },
                                            onClick = {
                                                showMoreMenu = false
                                                onRequestDelete(file, displayTitle)
                                            }
                                        )
                                    } else {
                                        if (isToggleable) {
                                            DropdownMenuItem(
                                                leadingIcon = {
                                                    Icon(
                                                        if (isEnabled) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
                                                        contentDescription = null,
                                                        tint = SkyBlueAccent,
                                                        modifier = Modifier.size(17.dp)
                                                    )
                                                },
                                                text = { Text(if (isEnabled) "Disable Pack" else "Enable Pack", fontSize = 12.sp, color = Color.White) },
                                                onClick = {
                                                    showMoreMenu = false
                                                    scope.launch(Dispatchers.IO) {
                                                        HorizonNativeBridge.toggleContentItem(context, folder, file.name, profileSlot, logEnabled)
                                                        withContext(Dispatchers.Main) {
                                                            onTriggerRefresh()
                                                        }
                                                    }
                                                }
                                            )
                                        }
                                        DropdownMenuItem(
                                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, tint = Color.White, modifier = Modifier.size(17.dp)) },
                                            text = { Text("Rename", fontSize = 12.sp, color = Color.White) },
                                            onClick = {
                                                showMoreMenu = false
                                                onRequestRename(file)
                                            }
                                        )
                                        DropdownMenuItem(
                                            leadingIcon = { Icon(Icons.Outlined.Delete, contentDescription = null, tint = Color(0xFFFF8A80), modifier = Modifier.size(17.dp)) },
                                            text = { Text("Delete", fontSize = 12.sp, color = Color(0xFFFF8A80)) },
                                            onClick = {
                                                showMoreMenu = false
                                                onRequestDelete(file, displayTitle)
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            AnimatedVisibility(
                visible = expandedDetails,
                enter = expandVertically(tween(220, easing = FastOutSlowInEasing)) + fadeIn(tween(200)),
                exit = shrinkVertically(tween(180)) + fadeOut(tween(150))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 68.dp, end = 14.dp, bottom = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = rowItem.detailSummary,
                        fontSize = 10.sp,
                        color = SkyBlueAccent
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (isToggleable) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isEnabled) Color(0xFF1E3E29) else SkyBlueAccent,
                                modifier = Modifier.clickable {
                                    scope.launch(Dispatchers.IO) {
                                        HorizonNativeBridge.toggleContentItem(context, folder, file.name, profileSlot, logEnabled)
                                        withContext(Dispatchers.Main) {
                                            onTriggerRefresh()
                                        }
                                    }
                                }
                            ) {
                                Text(
                                    text = if (isEnabled) "Matikan (Disable)" else "Aktifkan (Enable)",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isEnabled) Color(0xFF81C784) else SkyBlueDarkText,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF3A2326),
                            modifier = Modifier.clickable { onRequestDelete(file, displayTitle) }
                        ) {
                            Text(
                                text = "Hapus",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFF8A80),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CleanSettingsViewport(
    config: HorizonConfigState,
    onConfigChange: (HorizonConfigState) -> Unit,
    onCheckForUpdatesManual: () -> Unit = {},
    onOpenLogs: () -> Unit
) {
    val context = LocalContext.current
    var selectedSection by remember { mutableStateOf(SettingsSubCategory.RENDERER) }

    Row(
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ZalithSubCategoryRail(
            items = SettingsSubCategory.entries,
            selectedItem = selectedSection,
            itemLabel = { it.label },
            itemIcon = { it.icon },
            onSelect = { selectedSection = it }
        )

        AnimatedContent(
            targetState = selectedSection,
            transitionSpec = {
                (fadeIn(tween(220, easing = FastOutSlowInEasing)) +
                    slideInVertically(tween(220, easing = FastOutSlowInEasing)) { h -> h / 14 })
                    .togetherWith(fadeOut(tween(150)))
                    .using(sizeTransform = null)
            },
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            label = "SettingsSubSectionTransition"
        ) { section ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                when (section) {
                    SettingsSubCategory.RENDERER -> {
                        ZalithRamAllocationCard(
                            context = context,
                            ramAllocationMb = config.ramAllocationMb,
                            onRamChange = { newRamMb ->
                                onConfigChange(config.copy(ramAllocationMb = newRamMb))
                            }
                        )

                        ZalithSettingDropdownCard(
                            title = "Graphics API (Horizon Android 0.7.6)",
                            description = "Set the native Vulkan / OpenGL ES renderer used by Minecraft 26.3",
                            selectedItem = config.renderer,
                            options = HorizonRendererMode.entries,
                            itemLabel = { it.label },
                            onSelect = { onConfigChange(config.copy(renderer = it)) }
                        )

                        ZalithSettingDropdownCard(
                            title = "Screen Presentation Aspect Ratio (v0.7.5)",
                            description = "Configure display fitting ratio for both logical rendering and touch input bounds",
                            selectedItem = config.screenAspect,
                            options = HorizonScreenAspect.entries,
                            itemLabel = { it.label },
                            onSelect = { onConfigChange(config.copy(screenAspect = it)) }
                        )

                        ZalithSettingDropdownCard(
                            title = "Orientation Rule",
                            description = "Choose how screen orientation is locked or sensor-rotated",
                            selectedItem = config.orientation,
                            options = HorizonOrientation.entries,
                            itemLabel = { it.label },
                            onSelect = { onConfigChange(config.copy(orientation = it)) }
                        )

                        ZalithSettingSliderCard(
                            title = "Resolution Scale",
                            description = "Lower values boost FPS on mobile GPUs; higher values sharpen 3D terrain & HUD",
                            value = config.renderScalePercent,
                            valueRange = 25..100,
                            step = 5,
                            valueSuffix = "%",
                            onValueChange = { onConfigChange(config.copy(renderScalePercent = it)) }
                        )

                        ZalithSettingDropdownCard(
                            title = "Preferred Display Refresh Rate (Android 11+)",
                            description = "Request high-refresh display mode from SurfaceFlinger (display_hz in v0.7.5)",
                            selectedItem = config.displayRefreshHz,
                            options = listOf(0, 60, 90, 120, 144),
                            itemLabel = { if (it == 0) "Auto (System Default)" else "$it Hz" },
                            onSelect = { onConfigChange(config.copy(displayRefreshHz = it)) }
                        )

                        ZalithSettingToggleCard(
                            title = "Avoid Camera Cutout / Notch (v0.7.5)",
                            description = "Inset viewport safely away from punch-hole cameras and display cutouts",
                            checked = config.screenAvoidCutout,
                            onCheckedChange = { onConfigChange(config.copy(screenAvoidCutout = it)) }
                        )

                        ZalithSettingToggleCard(
                            title = "Immersive Fullscreen Navigation (v0.7.5)",
                            description = "Hide Android system status and navigation bars while playing",
                            checked = config.screenImmersive,
                            onCheckedChange = { onConfigChange(config.copy(screenImmersive = it)) }
                        )

                        ZalithSettingToggleCard(
                            title = "VSync (Vertical Synchronization)",
                            description = "Synchronize frame rate with display refresh rate in options.txt",
                            checked = config.mcVsync,
                            onCheckedChange = { onConfigChange(config.copy(mcVsync = it)) }
                        )

                        ZalithSettingToggleCard(
                            title = "Android ADPF Performance Hints (v0.7.5)",
                            description = "Send Android Dynamic Performance Framework CPU/GPU thermal scheduling hints",
                            checked = config.performanceHintsAdpf,
                            onCheckedChange = { onConfigChange(config.copy(performanceHintsAdpf = it)) }
                        )
                    }

                    SettingsSubCategory.GAME -> {
                        ZalithRamAllocationCard(
                            context = context,
                            ramAllocationMb = config.ramAllocationMb,
                            onRamChange = { newRamMb ->
                                onConfigChange(config.copy(ramAllocationMb = newRamMb))
                            }
                        )

                        ZalithSettingDropdownCard(
                            title = "Game Profile Slot",
                            description = "Select which isolated profile directory (saves, mods, options.txt) is active",
                            selectedItem = config.profile,
                            options = listOf(0, 1, 2, 3),
                            itemLabel = { if (it == 0) "Default (Profile 0)" else "Profile $it" },
                            onSelect = { onConfigChange(config.copy(profile = it)) }
                        )

                        ZalithSettingSliderCard(
                            title = "Render Distance",
                            description = "Number of visible world chunks rendered around the player (synced with options.txt)",
                            value = config.mcRenderDistance,
                            valueRange = 2..32,
                            step = 1,
                            valueSuffix = " Chunks",
                            onValueChange = { onConfigChange(config.copy(mcRenderDistance = it)) }
                        )

                        ZalithSettingSliderCard(
                            title = "Simulation Distance",
                            description = "Number of active entity and block-tick chunks around the player (synced with options.txt)",
                            value = config.mcSimulationDistance,
                            valueRange = 5..32,
                            step = 1,
                            valueSuffix = " Chunks",
                            onValueChange = { onConfigChange(config.copy(mcSimulationDistance = it)) }
                        )

                        ZalithSettingSliderCard(
                            title = "Max Framerate Limit",
                            description = "Maximum FPS cap inside Minecraft (260 = Unlimited, synced with options.txt)",
                            value = config.mcMaxFps,
                            valueRange = 30..260,
                            step = 10,
                            valueSuffix = " FPS",
                            onValueChange = { onConfigChange(config.copy(mcMaxFps = it)) }
                        )

                        ZalithSettingDropdownCard(
                            title = "GUI Scale",
                            description = "In-game HUD and inventory interface scale factor (synced with options.txt)",
                            selectedItem = config.mcGuiScale,
                            options = listOf(0, 1, 2, 3, 4),
                            itemLabel = { if (it == 0) "Auto" else "${it}x" },
                            onSelect = { onConfigChange(config.copy(mcGuiScale = it)) }
                        )

                        // Built-in Horizon 26.3 Client Extras (`horizon-extras.txt`)
                        ZalithSettingToggleCard(
                            title = "Fullbright Cave Lighting (Horizon Extra)",
                            description = "Display blocks and entities with a fully bright lightmap in dark caves (horizon-extras.txt)",
                            checked = config.extraFullbright,
                            onCheckedChange = { onConfigChange(config.copy(extraFullbright = it)) }
                        )

                        ZalithSettingToggleCard(
                            title = "Dynamic Handheld Lights (Horizon Extra)",
                            description = "Held torches, lanterns, and glowing items illuminate nearby blocks in real time",
                            checked = config.extraDynamicLights,
                            onCheckedChange = { onConfigChange(config.copy(extraDynamicLights = it)) }
                        )

                        ZalithSettingToggleCard(
                            title = "Auto-Reconnect on Server Drop (Horizon Extra)",
                            description = "Automatically retry server connection with 5s–60s backoff if disconnected",
                            checked = config.extraAutoReconnect,
                            onCheckedChange = { onConfigChange(config.copy(extraAutoReconnect = it)) }
                        )

                        ZalithSettingToggleCard(
                            title = "Mouse Tweaks & Inventory Sort Button (Horizon Extra)",
                            description = "Enable fast slot dragging and one-click chest/inventory sorting in horizon-extras.txt",
                            checked = config.extraMouseTweaks,
                            onCheckedChange = {
                                onConfigChange(config.copy(extraMouseTweaks = it, extraInventorySort = it))
                            }
                        )
                    }

                    SettingsSubCategory.AI_AGENT -> {
                        val scope = rememberCoroutineScope()
                        var localApiKey by remember {
                            mutableStateOf(HorizonNativeBridge.loadGeminiApiKeyLocal(context))
                        }
                        var showApiKeyText by remember { mutableStateOf(false) }
                        var availableModels by remember {
                            mutableStateOf(DEFAULT_GEMINI_MODELS)
                        }
                        var isRefreshingModels by remember { mutableStateOf(false) }
                        var isTestingApi by remember { mutableStateOf(false) }
                        var testResultDialogText by remember { mutableStateOf<String?>(null) }
                        var testResultSuccess by remember { mutableStateOf(false) }

                        LaunchedEffect(Unit) {
                            val savedKey = HorizonNativeBridge.loadGeminiApiKeyLocal(context)
                            if (savedKey.isNotBlank()) {
                                val live = withContext(Dispatchers.IO) {
                                    HorizonNativeBridge.fetchAvailableGeminiModels(context, savedKey)
                                }
                                if (live.isNotEmpty()) {
                                    availableModels = live
                                }
                            }
                        }

                        if (testResultDialogText != null) {
                            AlertDialog(
                                onDismissRequest = { testResultDialogText = null },
                                containerColor = Color(0xFF1E2429),
                                title = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (testResultSuccess) Icons.Default.CheckCircle else Icons.Default.ErrorOutline,
                                            contentDescription = null,
                                            tint = if (testResultSuccess) Color(0xFF81C784) else Color(0xFFFF8A80)
                                        )
                                        Text(
                                            text = if (testResultSuccess) "Google AI Studio Terhubung!" else "Koneksi Gemini API Gagal",
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                },
                                text = {
                                    Text(
                                        text = testResultDialogText.orEmpty(),
                                        fontSize = 13.sp,
                                        color = Color(0xFFD5DEE5),
                                        lineHeight = 18.sp
                                    )
                                },
                                confirmButton = {
                                    Button(
                                        onClick = { testResultDialogText = null },
                                        colors = ButtonDefaults.buttonColors(containerColor = SkyBlueAccent)
                                    ) {
                                        Text("OK", color = SkyBlueDarkText, fontWeight = FontWeight.Bold)
                                    }
                                }
                            )
                        }

                        ZalithSettingToggleCard(
                            title = "Enable Minecraft In-Game AI Agent (Gemini AI Studio)",
                            description = "Aktifkan AI Agent di dalam Minecraft 26.3 yang terhubung ke Google AI Studio (Gemini API) & socket kontrol game (horizon-control.json)",
                            checked = config.aiAgentEnabled,
                            onCheckedChange = { onConfigChange(config.copy(aiAgentEnabled = it)) }
                        )

                        ZalithSettingDropdownCard(
                            title = "AI Agent Engine Provider",
                            description = "Pilih integrasi Google AI Studio (Gemini API dengan Local API Key) atau Local Socket saja",
                            selectedItem = config.aiAgentProvider,
                            options = HorizonAiAgentProvider.entries,
                            itemLabel = { it.label },
                            onSelect = { onConfigChange(config.copy(aiAgentProvider = it)) }
                        )

                        // Local-Only Google AI Studio API Key Card
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = CardSlateBg,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(18.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Google AI Studio API Key (Disimpan Lokal)",
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color.White
                                        )
                                        Text(
                                            text = "API Key disimpan khusus di penyimpanan privat perangkat (0600) dan tidak pernah dikirim ke log atau server lain.",
                                            fontSize = 12.sp,
                                            color = MutedSubtitleText
                                        )
                                    }
                                    LavenderTagBadge(
                                        text = if (localApiKey.isNotBlank()) "Key Tersimpan Lokal" else "Belum Diisi"
                                    )
                                }

                                OutlinedTextField(
                                    value = localApiKey,
                                    onValueChange = { newKey ->
                                        localApiKey = newKey
                                        HorizonNativeBridge.saveGeminiApiKeyLocal(context, newKey)
                                    },
                                    label = { Text("Paste Google AI Studio API Key (AIzaSy...)", fontSize = 11.sp) },
                                    singleLine = true,
                                    visualTransformation = if (showApiKeyText) {
                                        androidx.compose.ui.text.input.VisualTransformation.None
                                    } else {
                                        androidx.compose.ui.text.input.PasswordVisualTransformation()
                                    },
                                    trailingIcon = {
                                        IconButton(onClick = { showApiKeyText = !showApiKeyText }) {
                                            Icon(
                                                imageVector = if (showApiKeyText) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                                contentDescription = "Show/Hide API Key",
                                                tint = SkyBlueAccent
                                            )
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            HorizonNativeBridge.saveGeminiApiKeyLocal(context, localApiKey)
                                            Toast.makeText(
                                                context,
                                                "API Key Gemini berhasil disimpan secara lokal di perangkat!",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                            scope.launch {
                                                isRefreshingModels = true
                                                val models = withContext(Dispatchers.IO) {
                                                    HorizonNativeBridge.fetchAvailableGeminiModels(context, localApiKey)
                                                }
                                                availableModels = models
                                                isRefreshingModels = false
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = SkyBlueAccent),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Default.Save, contentDescription = null, tint = SkyBlueDarkText, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("Simpan Key & Muat Model", fontSize = 12.sp, color = SkyBlueDarkText, fontWeight = FontWeight.Bold)
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            if (!isTestingApi) {
                                                isTestingApi = true
                                                scope.launch {
                                                    val (ok, msg) = withContext(Dispatchers.IO) {
                                                        HorizonNativeBridge.testGeminiApiConnection(
                                                            context = context,
                                                            apiKeyOverride = localApiKey,
                                                            modelId = config.geminiModel
                                                        )
                                                    }
                                                    isTestingApi = false
                                                    testResultSuccess = ok
                                                    testResultDialogText = msg
                                                }
                                            }
                                        },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Default.Bolt, contentDescription = null, tint = SkyBlueAccent, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            text = if (isTestingApi) "Menguji Gemini..." else "Test Koneksi Gemini",
                                            fontSize = 12.sp,
                                            color = SkyBlueAccent,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }

                                    if (localApiKey.isNotBlank()) {
                                        OutlinedButton(
                                            onClick = {
                                                localApiKey = ""
                                                HorizonNativeBridge.clearGeminiApiKeyLocal(context)
                                                Toast.makeText(context, "API Key lokal dihapus.", Toast.LENGTH_SHORT).show()
                                            }
                                        ) {
                                            Icon(Icons.Default.DeleteOutline, contentDescription = "Clear Key", tint = Color(0xFFFF8A80), modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                            }
                        }

                        // Clean, Non-Buggy Modal Gemini Model Selector (1-to-1 DisplayName <-> ModelId)
                        var showModelPickerDialog by remember { mutableStateOf(false) }
                        var modelSearchQuery by remember { mutableStateOf("") }
                        val normalizedActiveModelId = HorizonNativeBridge.normalizeGeminiModelId(config.geminiModel)
                        val selectedModelObj = HorizonNativeBridge.findGeminiModelOption(normalizedActiveModelId, availableModels)

                        if (showModelPickerDialog) {
                            val filteredModels = remember(availableModels, modelSearchQuery) {
                                val q = modelSearchQuery.trim().lowercase(Locale.US)
                                if (q.isEmpty()) availableModels
                                else availableModels.filter {
                                    it.displayName.lowercase(Locale.US).contains(q) ||
                                        it.modelId.lowercase(Locale.US).contains(q) ||
                                        it.badge.lowercase(Locale.US).contains(q)
                                }
                            }
                            AlertDialog(
                                onDismissRequest = { showModelPickerDialog = false },
                                containerColor = Color(0xFF161B20),
                                title = {
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "Pilih Model Google AI Studio (${availableModels.size})",
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                            LavenderTagBadge(text = selectedModelObj.badge)
                                        }
                                        OutlinedTextField(
                                            value = modelSearchQuery,
                                            onValueChange = { modelSearchQuery = it },
                                            placeholder = { Text("Cari model (misal: Argon, 4.0, 3.8, 3.5, 2.5, Lite)...", fontSize = 11.sp) },
                                            singleLine = true,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                },
                                text = {
                                    LazyColumn(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(260.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        items(filteredModels, key = { it.modelId }) { option ->
                                            val isSelected = HorizonNativeBridge.normalizeGeminiModelId(option.modelId) == normalizedActiveModelId
                                            Surface(
                                                shape = RoundedCornerShape(12.dp),
                                                color = if (isSelected) SkyBlueAccent.copy(alpha = 0.18f) else Color(0xFF20272E),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .animatedPressScale {
                                                        onConfigChange(config.copy(geminiModel = option.modelId))
                                                        showModelPickerDialog = false
                                                    }
                                            ) {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Column(
                                                        modifier = Modifier.weight(1f),
                                                        verticalArrangement = Arrangement.spacedBy(2.dp)
                                                    ) {
                                                        Row(
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                        ) {
                                                            Text(
                                                                text = option.displayName,
                                                                fontSize = 13.5.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color = if (isSelected) SkyBlueAccent else Color.White
                                                            )
                                                            Text(
                                                                text = option.badge,
                                                                fontSize = 9.5.sp,
                                                                fontWeight = FontWeight.SemiBold,
                                                                color = Color(0xFFD0C6F2)
                                                            )
                                                        }
                                                        Text(
                                                            text = "ID: ${option.modelId}",
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.Medium,
                                                            color = SkyBlueAccent.copy(alpha = 0.88f)
                                                        )
                                                        Text(
                                                            text = option.description,
                                                            fontSize = 11.sp,
                                                            color = MutedSubtitleText,
                                                            maxLines = 2,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                    }
                                                    if (isSelected) {
                                                        Spacer(Modifier.width(8.dp))
                                                        Icon(
                                                            imageVector = Icons.Default.CheckCircle,
                                                            contentDescription = null,
                                                            tint = SkyBlueAccent,
                                                            modifier = Modifier.size(18.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                },
                                confirmButton = {
                                    TextButton(onClick = { showModelPickerDialog = false }) {
                                        Text("Tutup", color = SkyBlueAccent, fontWeight = FontWeight.Bold)
                                    }
                                }
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = CardSlateBg,
                            modifier = Modifier
                                .fillMaxWidth()
                                .animatedPressScale { showModelPickerDialog = true }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 18.dp, vertical = 14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = if (isRefreshingModels) "Model Google AI Studio (Memuat...)" else "Pilih Model Google AI Studio",
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color.White
                                        )
                                        LavenderTagBadge(text = selectedModelObj.badge)
                                    }
                                    Text(
                                        text = "Aktif: ${selectedModelObj.displayName}  •  ID: ${selectedModelObj.modelId}",
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = SkyBlueAccent
                                    )
                                    Text(
                                        text = selectedModelObj.description,
                                        fontSize = 11.5.sp,
                                        color = MutedSubtitleText
                                    )
                                }
                                Spacer(Modifier.width(10.dp))
                                Icon(
                                    imageVector = Icons.Default.UnfoldMore,
                                    contentDescription = "Select Model",
                                    tint = SkyBlueAccent,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = CardSlateBg,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(18.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "Custom / Manual Gemini Model ID",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )
                                Text(
                                    text = "Pilih dari 22+ model di atas (Gemini 4 Argon, Gemini 3.8, Gemini 3.5 Flash-Lite, Gemini 2.5 Pro/Flash, dll.) atau ketik ID model secara manual:",
                                    fontSize = 11.sp,
                                    color = MutedSubtitleText
                                )
                                OutlinedTextField(
                                    value = config.geminiModel,
                                    onValueChange = { onConfigChange(config.copy(geminiModel = it.trim())) },
                                    label = { Text("Active Gemini Model ID (contoh: gemini-4-argon, gemini-3.8-pro, gemini-3.5-flash-lite)", fontSize = 11.sp) },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }

                        ZalithSettingToggleCard(
                            title = "In-Game Chat Assistant (@ai / !ai Trigger)",
                            description = "Izinkan mengetik @ai atau !ai di chat Minecraft untuk bertanya atau memberi perintah langsung ke Gemini AI Agent",
                            checked = config.aiAgentAutoAssistChat,
                            onCheckedChange = { onConfigChange(config.copy(aiAgentAutoAssistChat = it)) }
                        )

                        ZalithSettingToggleCard(
                            title = "Autonomous Game Control Actions (Move, Look, Mine, Place, Craft)",
                            description = "Izinkan Gemini AI Agent mengendalikan aksi karakter melalui protokol lokal horizon-control.json saat diminta",
                            checked = config.aiAgentAutonomousActions,
                            onCheckedChange = { onConfigChange(config.copy(aiAgentAutonomousActions = it)) }
                        )

                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = CardSlateBg,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(18.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "Instruksi & Peran Default AI Agent di Minecraft",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )
                                Text(
                                    text = "Instruksi sistem yang dikirim ke model ${config.geminiModel} saat AI Agent aktif di dalam dunia Minecraft 26.3:",
                                    fontSize = 12.sp,
                                    color = MutedSubtitleText
                                )
                                OutlinedTextField(
                                    value = config.aiAgentTaskPrompt,
                                    onValueChange = { onConfigChange(config.copy(aiAgentTaskPrompt = it)) },
                                    label = { Text("System Prompt / Tugas AI Agent", fontSize = 11.sp) },
                                    maxLines = 3,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }

                    SettingsSubCategory.CONTROLS -> {
                        ZalithSettingDropdownCard(
                            title = "Input Mode (v0.7.5)",
                            description = "Select primary input host (Auto follows active touch, mouse, or controller input)",
                            selectedItem = config.inputHost,
                            options = HorizonInputHost.entries,
                            itemLabel = { it.label },
                            onSelect = { onConfigChange(config.copy(inputHost = it)) }
                        )

                        ZalithSettingDropdownCard(
                            title = "Touch Control Scheme (v2.3)",
                            description = "Combine Joystick or D-Pad movement with Crosshair or Tap-to-Interact aiming",
                            selectedItem = config.touchScheme,
                            options = HorizonTouchScheme.entries,
                            itemLabel = { it.label },
                            onSelect = { onConfigChange(config.copy(touchScheme = it)) }
                        )

                        ZalithSettingSliderCard(
                            title = "Horizontal Touch Sensitivity (X)",
                            description = "Independent horizontal camera look sensitivity for touch input",
                            value = config.touchSensitivityXPercent,
                            valueRange = 20..250,
                            step = 5,
                            valueSuffix = "%",
                            onValueChange = { onConfigChange(config.copy(touchSensitivityXPercent = it)) }
                        )

                        ZalithSettingSliderCard(
                            title = "Vertical Touch Sensitivity (Y)",
                            description = "Independent vertical camera look sensitivity for touch input",
                            value = config.touchSensitivityYPercent,
                            valueRange = 20..250,
                            step = 5,
                            valueSuffix = "%",
                            onValueChange = { onConfigChange(config.copy(touchSensitivityYPercent = it)) }
                        )

                        ZalithSettingToggleCard(
                            title = "360-Degree Joystick Movement (v2.3)",
                            description = "Allow continuous 360-degree analog movement vector when using Touch Joystick schemes",
                            checked = config.touch360Joystick,
                            onCheckedChange = { onConfigChange(config.copy(touch360Joystick = it)) }
                        )

                        ZalithSettingToggleCard(
                            title = "Swap Hands Touch Button (v2.3)",
                            description = "Show dedicated off-hand swap action button on the touch HUD",
                            checked = config.touchSwapHandsButton,
                            onCheckedChange = { onConfigChange(config.copy(touchSwapHandsButton = it)) }
                        )

                        ZalithSettingToggleCard(
                            title = "Virtual Menu Mouse in Inventories (v2.3)",
                            description = "Enable precision virtual cursor mode inside inventory, chest, and crafting screens",
                            checked = config.inventoryVirtualMouse,
                            onCheckedChange = { onConfigChange(config.copy(inventoryVirtualMouse = it)) }
                        )

                        ZalithSettingToggleCard(
                            title = "On-Screen Touch Keypad",
                            description = "Enable customizable virtual touch buttons inside Minecraft (horizon-keypad.json)",
                            checked = config.keypadControls,
                            onCheckedChange = { onConfigChange(config.copy(keypadControls = it)) }
                        )

                        ZalithSettingToggleCard(
                            title = "Sprint Toggle / Tap-to-Sprint (v0.7.4+)",
                            description = "Use tap-to-toggle mode for the touch Sprint button instead of holding",
                            checked = config.sprintToggle,
                            onCheckedChange = { onConfigChange(config.copy(sprintToggle = it)) }
                        )

                        ZalithSettingToggleCard(
                            title = "Inventory Touch Action Buttons (v0.7.4+)",
                            description = "Keep Attack (left-click), Interact (right-click), and Sneak (Shift) touch buttons visible inside inventories",
                            checked = config.inventoryTouchControls,
                            onCheckedChange = { onConfigChange(config.copy(inventoryTouchControls = it)) }
                        )

                        ZalithSettingToggleCard(
                            title = "Gyroscope Camera Aiming (v0.7.4+)",
                            description = "Enable motion-sensor gyroscope camera aiming with adaptive axes",
                            checked = config.gyroAimingEnabled,
                            onCheckedChange = { onConfigChange(config.copy(gyroAimingEnabled = it)) }
                        )

                        if (config.gyroAimingEnabled) {
                            ZalithSettingSliderCard(
                                title = "Gyroscope Aim Sensitivity",
                                description = "Adjust motion sensor aiming speed multiplier",
                                value = config.gyroSensitivityPercent,
                                valueRange = 10..200,
                                step = 10,
                                valueSuffix = "%",
                                onValueChange = { onConfigChange(config.copy(gyroSensitivityPercent = it)) }
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = CardSlateBg,
                            modifier = Modifier
                                .fillMaxWidth()
                                .animatedPressScale { HorizonNativeBridge.resetTouchKeypadLayout(context, config) }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 18.dp, vertical = 14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                    Text("Reset Touch Keypad Layout", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                                    Text("Restore on-screen touch button positions in horizon-keypad.json back to default", fontSize = 12.sp, color = MutedSubtitleText)
                                }
                                Icon(Icons.Default.Refresh, contentDescription = null, tint = SkyBlueAccent)
                            }
                        }
                    }

                    SettingsSubCategory.GAMEPAD -> {
                        ZalithSettingDropdownCard(
                            title = "Controller & Gamepad Routing (v0.7.5)",
                            description = "Configure USB & Bluetooth gamepad input host (Xbox, DualSense, Switch Pro)",
                            selectedItem = config.inputHost,
                            options = listOf(HorizonInputHost.AUTO, HorizonInputHost.CONTROLLER, HorizonInputHost.KEYBOARD_MOUSE, HorizonInputHost.TOUCH),
                            itemLabel = { it.label },
                            onSelect = { onConfigChange(config.copy(inputHost = it)) }
                        )

                        ZalithSettingDropdownCard(
                            title = "Controller Menu & Inventory Navigation (v0.7.5)",
                            description = "Choose Focus highlight (D-pad/Stick step) or Virtual Pointer cursor in menus (horizon-controller.txt)",
                            selectedItem = config.controllerNavMode,
                            options = HorizonControllerNavMode.entries,
                            itemLabel = { it.label },
                            onSelect = { onConfigChange(config.copy(controllerNavMode = it)) }
                        )

                        ZalithSettingToggleCard(
                            title = "Controller Vibration & Rumble (v0.7.4+)",
                            description = "Enable controller haptics and rumble feedback when supported by Android driver",
                            checked = config.controllerVibration,
                            onCheckedChange = { onConfigChange(config.copy(controllerVibration = it)) }
                        )

                        ZalithSettingSliderCard(
                            title = "Controller Look Sensitivity",
                            description = "Right-stick camera yaw/pitch rotation speed (synced with horizon-controller.txt)",
                            value = config.controllerSensitivityPercent,
                            valueRange = 20..200,
                            step = 10,
                            valueSuffix = "%",
                            onValueChange = { onConfigChange(config.copy(controllerSensitivityPercent = it)) }
                        )

                        ZalithSettingSliderCard(
                            title = "Analog Stick Radial Deadzone",
                            description = "Prevent stick drift on left and right thumbsticks (synced with horizon-controller.txt)",
                            value = config.controllerDeadzonePercent,
                            valueRange = 5..40,
                            step = 5,
                            valueSuffix = "%",
                            onValueChange = { onConfigChange(config.copy(controllerDeadzonePercent = it)) }
                        )

                        ZalithSettingToggleCard(
                            title = "Audio Safe Mode",
                            description = "Disable OpenAL event callbacks if Bluetooth controllers or headsets cause audio stutter",
                            checked = config.disableAudioSafeMode,
                            onCheckedChange = { onConfigChange(config.copy(disableAudioSafeMode = it)) }
                        )
                    }

                    SettingsSubCategory.LAUNCHER -> {
                        ZalithSettingDropdownCard(
                            title = "Dashboard Background Mode (Wallpaper vs. Video Trailer)",
                            description = "Choose between static 16:9 HD Minecraft wallpapers or auto-playing official Minecraft YouTube Trailers (720p)",
                            selectedItem = config.backgroundMode,
                            options = HorizonBackgroundMode.entries,
                            itemLabel = { it.label },
                            onSelect = { mode ->
                                onConfigChange(
                                    config.copy(
                                        backgroundMode = mode,
                                        useYoutubeTrailerWallpaper = (mode == HorizonBackgroundMode.YOUTUBE_TRAILER)
                                    )
                                )
                            }
                        )

                        ZalithSettingToggleCard(
                            title = "Auto-Play Minecraft YouTube 'Trailer' Video",
                            description = "Automatically play the latest official @Minecraft YouTube videos containing 'Trailer' on the dashboard and turn off static wallpaper while playing",
                            checked = config.useYoutubeTrailerWallpaper || config.backgroundMode == HorizonBackgroundMode.YOUTUBE_TRAILER,
                            onCheckedChange = { enabled ->
                                onConfigChange(
                                    config.copy(
                                        useYoutubeTrailerWallpaper = enabled,
                                        backgroundMode = if (enabled) HorizonBackgroundMode.YOUTUBE_TRAILER else HorizonBackgroundMode.WALLPAPER
                                    )
                                )
                            }
                        )

                        ZalithSettingToggleCard(
                            title = "Silent / Mute Trailer Video Audio",
                            description = "Mute audio while playing the 720p Minecraft Trailer video on the dashboard (disable to hear full trailer audio)",
                            checked = config.muteTrailerVideo,
                            onCheckedChange = { muted ->
                                onConfigChange(config.copy(muteTrailerVideo = muted))
                            }
                        )

                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = CardSlateBg,
                            modifier = Modifier
                                .fillMaxWidth()
                                .animatedPressScale { onCheckForUpdatesManual() }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 18.dp, vertical = 14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                    Text(
                                        text = "Check for Launcher Updates (HorizonZ v${HorizonNativeBridge.INSTALLED_LAUNCHER_VERSION})",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "Connected to official fork: github.com/${HorizonNativeBridge.GITHUB_FORK_REPO_SLUG}/releases",
                                        fontSize = 12.sp,
                                        color = SkyBlueAccent
                                    )
                                }
                                Icon(Icons.Default.SystemUpdate, contentDescription = null, tint = SkyBlueAccent)
                            }
                        }

                        ZalithSettingDropdownCard(
                            title = "Theme Palette (Default: Monet / Material You)",
                            description = "Pilih palet warna tampilan Launcher (Default: Monet / Material You)",
                            selectedItem = config.themePalette,
                            options = ZalithThemePalette.entries,
                            itemLabel = { it.label },
                            onSelect = { chosenPalette ->
                                onConfigChange(
                                    config.copy(
                                        themePalette = chosenPalette,
                                        useDynamicColor = (chosenPalette == ZalithThemePalette.MATERIAL_YOU)
                                    )
                                )
                            }
                        )

                        ZalithSettingToggleCard(
                            title = "Material You Dynamic Color (Android 12+)",
                            description = "Adapt launcher accent colors to your system wallpaper automatically",
                            checked = config.useDynamicColor,
                            onCheckedChange = { onConfigChange(config.copy(useDynamicColor = it)) }
                        )

                        ZalithSettingToggleCard(
                            title = "System & Game Runtime Logs",
                            description = "Record launcher, installer, and Minecraft runtime logs for troubleshooting",
                            checked = config.enableSystemLog,
                            onCheckedChange = { onConfigChange(config.copy(enableSystemLog = it)) }
                        )

                        ZalithSettingToggleCard(
                            title = "Record Performance Telemetry",
                            description = "Write frame-time and GPU benchmark metrics to performance.json during gameplay",
                            checked = config.recordPerformance,
                            onCheckedChange = { onConfigChange(config.copy(recordPerformance = it)) }
                        )

                        ZalithSettingToggleCard(
                            title = "Detailed GPU Stage Profiling (p50/p95/p99)",
                            description = "Include rolling GPU preparation, upload, rendering, and post-processing timings",
                            checked = config.gpuStageProfiling,
                            onCheckedChange = { onConfigChange(config.copy(gpuStageProfiling = it)) }
                        )

                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = CardSlateBg,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(18.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text("Quick Server (DNS/SRV v0.7.5), Proxy & Diagnostics", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                    OutlinedTextField(
                                        value = config.defaultServer,
                                        onValueChange = { onConfigChange(config.copy(defaultServer = it)) },
                                        label = { Text("Quick-Join Server (domain / ip:port)", fontSize = 11.sp) },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f)
                                    )
                                    OutlinedTextField(
                                        value = config.proxy,
                                        onValueChange = { onConfigChange(config.copy(proxy = it)) },
                                        label = { Text("HTTP/SOCKS5 Proxy", fontSize = 11.sp) },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(onClick = { HorizonNativeBridge.exportPerformanceReport(context, config.profile) }) {
                                        Icon(Icons.Default.Analytics, contentDescription = null, modifier = Modifier.size(15.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Export Perf", fontSize = 11.sp)
                                    }
                                    OutlinedButton(onClick = { HorizonNativeBridge.openSystemFilesApp(context) }) {
                                        Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(15.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Open Folder", fontSize = 11.sp)
                                    }
                                    OutlinedButton(onClick = onOpenLogs) {
                                        Icon(Icons.Default.Terminal, contentDescription = null, modifier = Modifier.size(15.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("System Log", fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ZalithRamAllocationCard(
    context: android.content.Context,
    ramAllocationMb: Int,
    onRamChange: (Int) -> Unit
) {
    val totalRamMb = remember { HorizonNativeBridge.getDeviceTotalRamMb(context) }
    val availRamMb = remember(ramAllocationMb) { HorizonNativeBridge.getDeviceAvailableRamMb(context) }
    val isUnlimited = ramAllocationMb <= 0
    val minRamMb = 1024
    val maxRamMb = totalRamMb.coerceAtLeast(2048)
    val sliderDisplayMb = if (isUnlimited) {
        maxRamMb
    } else {
        ramAllocationMb.coerceIn(minRamMb, maxRamMb)
    }

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = CardSlateBg,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "RAM Allocation (Minecraft 26.3 Engine)",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        LavenderTagBadge(
                            text = if (isUnlimited) "Unlimited (Default OS)" else "$sliderDisplayMb MB"
                        )
                    }
                    Text(
                        text = "RAM HP Terdeteksi: $totalRamMb MB (~${String.format(Locale.US, "%.1f", totalRamMb / 1024f)} GB) • Tersedia: $availRamMb MB",
                        fontSize = 11.sp,
                        color = SkyBlueAccent
                    )
                    Text(
                        text = if (isUnlimited) {
                            "Mode Default Aktif: Bebas mengikuti batas maksimal OS Android secara dinamis (Direkomendasikan)"
                        } else {
                            "Batas alokasi memori manual untuk runtime Rust, WGPU & tekstur Minecraft: $sliderDisplayMb MB"
                        },
                        fontSize = 11.sp,
                        color = MutedSubtitleText
                    )
                }
            }

            // Mode Selector Pills: [ Bebas / Limit OS Android (Default) ]  [ Atur Manual (Slider) ]
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isUnlimited) SkyBlueAccent else CardRowBg,
                    modifier = Modifier
                        .weight(1f)
                        .animatedPressScale { onRamChange(0) }
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 10.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isUnlimited) Icons.Default.CheckCircle else Icons.Default.AllInclusive,
                            contentDescription = null,
                            tint = if (isUnlimited) SkyBlueDarkText else SkyBlueAccent,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "Bebas / Limit OS Android (Default)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isUnlimited) SkyBlueDarkText else Color.White,
                            maxLines = 1
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (!isUnlimited) SkyBlueAccent else CardRowBg,
                    modifier = Modifier
                        .weight(1f)
                        .animatedPressScale {
                            if (isUnlimited) {
                                val recommended = ((totalRamMb * 3) / 4 / 256 * 256).coerceIn(minRamMb, maxRamMb)
                                onRamChange(recommended)
                            }
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 10.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = null,
                            tint = if (!isUnlimited) SkyBlueDarkText else SkyBlueAccent,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = if (!isUnlimited) "Manual: $sliderDisplayMb MB" else "Atur Manual (Slider)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (!isUnlimited) SkyBlueDarkText else Color.White,
                            maxLines = 1
                        )
                    }
                }
            }

            // Slider Row (1024 MB .. totalRamMb, step 256 MB)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Slider(
                    value = sliderDisplayMb.toFloat(),
                    onValueChange = { rawVal ->
                        val snapped = ((rawVal.toInt() + 128) / 256) * 256
                        val clamped = snapped.coerceIn(minRamMb, maxRamMb)
                        onRamChange(clamped)
                    },
                    valueRange = minRamMb.toFloat()..maxRamMb.toFloat(),
                    colors = SliderDefaults.colors(
                        thumbColor = SkyBlueAccent,
                        activeTrackColor = SkyBlueAccent,
                        inactiveTrackColor = Color(0xFF33424B)
                    ),
                    modifier = Modifier.weight(1f)
                )

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isUnlimited) SubRailCapsuleActive else SkyBlueAccent
                ) {
                    Text(
                        text = if (isUnlimited) "OS Limit" else "$sliderDisplayMb MB",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isUnlimited) SkyBlueAccent else SkyBlueDarkText,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ZalithLaunchSidebar(
    activeAccount: ZalithAccount,
    activeSkinBitmap: Bitmap,
    installStatus: AssetInstallStatus,
    onOpenAccountScreen: () -> Unit,
    onOpenVersions: () -> Unit,
    onLaunchOrDownload: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = CardSlateBg,
        modifier = Modifier
            .fillMaxHeight()
            .width(182.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(11.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = CardRowBg,
                modifier = Modifier
                    .fillMaxWidth()
                    .animatedPressScale(onClick = onOpenAccountScreen)
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MinecraftSkinHeadAvatar(
                        skinBitmap = activeSkinBitmap,
                        size = 34.dp,
                        cornerRadius = 9.dp
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = activeAccount.username,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (activeAccount.slimSkinModel) "${activeAccount.accountType.badge} • Slim"
                            else "${activeAccount.accountType.badge} • Wide",
                            fontSize = 10.sp,
                            color = SkyBlueAccent,
                            maxLines = 1
                        )
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = CardRowBg,
                    modifier = Modifier
                        .fillMaxWidth()
                        .animatedPressScale(onClick = onOpenVersions)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Minecraft 26.3", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = Color.White)
                            Text(
                                text = if (installStatus.ready) "Ready"
                                else if (installStatus.paused) "Paused (${installStatus.percent}%)"
                                else if (installStatus.running) "${installStatus.percent}% (${installStatus.speedText.ifBlank { "Downloading" }})"
                                else "Needs Download",
                                fontSize = 11.sp,
                                color = if (installStatus.ready) Color(0xFF81C784)
                                else if (installStatus.paused) Color(0xFFFFB74D)
                                else SkyBlueAccent,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Icon(
                            Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = MutedSubtitleText,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (installStatus.running) SubRailCapsuleActive else SkyBlueAccent,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .animatedPressScale(onClick = onLaunchOrDownload)
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (installStatus.ready) Icons.Default.PlayArrow else Icons.Default.Download,
                            contentDescription = null,
                            tint = if (installStatus.running) SkyBlueAccent else SkyBlueDarkText
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = if (installStatus.paused) "PAUSED"
                            else if (installStatus.running) "${installStatus.percent}%"
                            else if (installStatus.ready) "PLAY"
                            else "INSTALL",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.sp,
                            color = if (installStatus.running) SkyBlueAccent else SkyBlueDarkText
                        )
                    }
                }
            }
        }
    }
}
