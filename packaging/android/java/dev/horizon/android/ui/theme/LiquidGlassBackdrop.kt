package dev.horizon.android.ui.theme

import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.os.Build
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Kyant0/AndroidLiquidGlass (`io.github.kyant0:backdrop`) optical parameters & spring constants:
 * - SDF rounded-rect lens refraction (`refractionHeight`, `refractionAmount`, `depthEffect`)
 * - Chromatic aberration / dispersion at curved edges (`chromaticAberration`)
 * - Directional SDF rim highlight (`Highlight.Default`) + inner shadow (`InnerShadow`)
 * - Pointer-following radial Plus glow (`InteractiveHighlight`: 0.08 flat + 0.15 radial)
 * - Underdamped spring constants (`k = 300f, zeta = 0.55f` & `k = 250f, zeta = 0.6f`)
 */
data class KyantLiquidGlassSpec(
    val cornerRadius: Dp = 20.dp,
    val refractionHeightPx: Float = 28f,
    val refractionAmountPx: Float = -38f,
    val depthEffect: Float = 0.35f,
    val chromaticAberration: Float = 1.35f,
    val surfaceTint: Color = Color(0x2890E4FF),
    val surfaceCover: Color = Color(0x40101822),
    val rimHighlightAlpha: Float = 0.62f,
    val innerShadowAlpha: Float = 0.32f,
    val enableAgslShader: Boolean = true
)

private const val KYANT_LIQUID_GLASS_AGSL = """
uniform shader content;
uniform float2 size;
uniform float cornerRadius;
uniform float refractionHeight;
uniform float refractionAmount;
uniform float chromatic;
uniform float2 pointerPos;
uniform float pressAmount;

float sdRoundedBox(float2 p, float2 b, float r) {
    float2 q = abs(p) - b + r;
    return min(max(q.x, q.y), 0.0) + length(max(q, 0.0)) - r;
}

half4 main(float2 coord) {
    float2 halfSize = size * 0.5;
    float2 p = coord - halfSize;
    float r = min(cornerRadius, min(halfSize.x, halfSize.y));
    float d = sdRoundedBox(p, halfSize, r);

    float edgeBand = clamp(1.0 + d / max(refractionHeight, 1.0), 0.0, 1.0);
    float lensCurve = edgeBand * edgeBand * (3.0 - 2.0 * edgeBand);

    float2 q = abs(p) - (halfSize - r);
    float2 grad = sign(p) * normalize(max(q, float2(0.001)));
    float2 offset = grad * lensCurve * refractionAmount * (1.0 + pressAmount * 0.25);

    float2 disp = offset * (0.04 * chromatic);
    half4 colR = content.eval( clamp(coord + offset - disp, float2(0.0), size - 1.0) );
    half4 colG = content.eval( clamp(coord + offset,        float2(0.0), size - 1.0) );
    half4 colB = content.eval( clamp(coord + offset + disp, float2(0.0), size - 1.0) );

    half4 color = half4(colR.r, colG.g, colB.b, colG.a);

    float rim = smoothstep(0.0, 1.0, lensCurve) * (1.0 - smoothstep(-3.5, 0.0, d));
    color.rgb += half3(rim * 0.09);

    return color;
}
"""

/**
 * Applies Kyant0's Liquid Glass backdrop surface effect (`Modifier.liquidGlassSurface`):
 * - Uses AGSL `RuntimeShader` on Android 13+ (API 33+) when `enableAgslShader` is true
 * - Renders SDF-gradient directional specular rim lighting (`Highlight.Default`), inner depth
 *   refraction band, hue-preserving glass tint, and interactive pointer glow (`InteractiveHighlight`).
 */
fun Modifier.liquidGlassSurface(
    enabled: Boolean,
    spec: KyantLiquidGlassSpec = KyantLiquidGlassSpec(),
    pointerOffset: Offset = Offset.Unspecified,
    pressProgress: Float = 0f
): Modifier = composed {
    if (!enabled) return@composed this

    val density = LocalDensity.current
    val cornerPx = with(density) { spec.cornerRadius.toPx() }

    val agslModifier = if (spec.enableAgslShader && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        var boxSize by remember { mutableStateOf(Size.Zero) }
        val runtimeShader = remember {
            runCatching { RuntimeShader(KYANT_LIQUID_GLASS_AGSL) }.getOrNull()
        }
        Modifier
            .onSizeChanged { boxSize = Size(it.width.toFloat(), it.height.toFloat()) }
            .graphicsLayer {
                if (runtimeShader != null && boxSize.width > 4f && boxSize.height > 4f) {
                    runCatching {
                        runtimeShader.setFloatUniform("size", boxSize.width, boxSize.height)
                        runtimeShader.setFloatUniform("cornerRadius", cornerPx)
                        runtimeShader.setFloatUniform("refractionHeight", spec.refractionHeightPx)
                        runtimeShader.setFloatUniform("refractionAmount", spec.refractionAmountPx * 0.18f)
                        runtimeShader.setFloatUniform("chromatic", spec.chromaticAberration)
                        val px = if (pointerOffset != Offset.Unspecified) pointerOffset.x else boxSize.width * 0.5f
                        val py = if (pointerOffset != Offset.Unspecified) pointerOffset.y else boxSize.height * 0.5f
                        runtimeShader.setFloatUniform("pointerPos", px, py)
                        runtimeShader.setFloatUniform("pressAmount", pressProgress)
                        renderEffect = RenderEffect
                            .createRuntimeShaderEffect(runtimeShader, "content")
                            .asComposeRenderEffect()
                    }
                }
            }
    } else {
        Modifier
    }

    this
        .clip(RoundedCornerShape(spec.cornerRadius))
        .then(agslModifier)
        .drawWithCache {
            val w = size.width
            val h = size.height
            val cr = CornerRadius(cornerPx, cornerPx)
            val minDim = min(w, h).coerceAtLeast(1f)

            val baseBrush = Brush.linearGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.14f + pressProgress * 0.06f),
                    spec.surfaceTint,
                    spec.surfaceCover
                ),
                start = Offset(0f, 0f),
                end = Offset(w, h)
            )

            val refractionBandBrush = Brush.linearGradient(
                colors = listOf(
                    Color(0x88B8F2FF),
                    Color(0x3890E4FF),
                    Color.Transparent,
                    Color(0x28D4C4FB),
                    Color(0x667CE8FF)
                ),
                start = Offset(0f, 0f),
                end = Offset(w, h)
            )

            val rimBrush = Brush.linearGradient(
                colors = listOf(
                    Color.White.copy(alpha = (spec.rimHighlightAlpha + pressProgress * 0.18f).coerceAtMost(0.95f)),
                    Color(0x99A6EEFF),
                    Color.White.copy(alpha = 0.14f),
                    Color(0x88B8E8FF),
                    Color.White.copy(alpha = (spec.rimHighlightAlpha * 0.78f).coerceAtMost(0.85f))
                ),
                start = Offset(0f, 0f),
                end = Offset(w, h)
            )

            onDrawWithContent {
                drawRoundRect(
                    color = spec.surfaceCover,
                    cornerRadius = cr
                )
                drawRoundRect(
                    brush = baseBrush,
                    cornerRadius = cr
                )
                val inset = 1.5.dp.toPx()
                if (w > inset * 4 && h > inset * 4) {
                    drawRoundRect(
                        brush = refractionBandBrush,
                        topLeft = Offset(inset, inset),
                        size = Size(w - inset * 2f, h - inset * 2f),
                        cornerRadius = CornerRadius(max(0f, cornerPx - inset), max(0f, cornerPx - inset)),
                        style = Stroke(width = 2.2.dp.toPx())
                    )
                }

                if (pressProgress > 0.01f || pointerOffset != Offset.Unspecified) {
                    val center = if (pointerOffset != Offset.Unspecified) pointerOffset else Offset(w * 0.5f, h * 0.5f)
                    val glowRadius = (minDim * 1.5f).coerceAtLeast(36f)
                    val glowAlpha = (0.10f + pressProgress * 0.16f).coerceIn(0f, 0.32f)
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color.White.copy(alpha = glowAlpha),
                                Color(0xFF90E4FF).copy(alpha = glowAlpha * 0.55f),
                                Color.Transparent
                            ),
                            center = center,
                            radius = glowRadius
                        ),
                        radius = glowRadius,
                        center = center,
                        blendMode = BlendMode.Plus
                    )
                }

                drawContent()

                drawRoundRect(
                    brush = rimBrush,
                    cornerRadius = cr,
                    style = Stroke(width = 1.25.dp.toPx())
                )
            }
        }
}

/**
 * Interactive Liquid Glass button modifier (`LiquidButton` from Kyant0/AndroidLiquidGlass):
 * - Underdamped spring press inflation (`k = 300f, dampingRatio = 0.52f`)
 * - Pointer-following radial Plus glow & refractive rim highlight when `isLiquidGlass` is enabled.
 */
fun Modifier.liquidGlassInteractiveButton(
    isLiquidGlass: Boolean,
    cornerRadius: Dp = 16.dp,
    onClick: () -> Unit
): Modifier = composed {
    var pressed by remember { mutableStateOf(false) }
    var pointerPos by remember { mutableStateOf(Offset.Unspecified) }

    val scale by animateFloatAsState(
        targetValue = if (pressed) (if (isLiquidGlass) 1.045f else 0.96f) else 1f,
        animationSpec = spring(
            dampingRatio = if (isLiquidGlass) 0.52f else Spring.DampingRatioMediumBouncy,
            stiffness = if (isLiquidGlass) 300f else Spring.StiffnessMedium
        ),
        label = "LiquidBtnScale"
    )
    val pressProgress by animateFloatAsState(
        targetValue = if (pressed) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 250f),
        label = "LiquidBtnPress"
    )

    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .then(
            if (isLiquidGlass) {
                Modifier.liquidGlassSurface(
                    enabled = true,
                    spec = KyantLiquidGlassSpec(
                        cornerRadius = cornerRadius,
                        enableAgslShader = false
                    ),
                    pointerOffset = pointerPos,
                    pressProgress = pressProgress
                )
            } else {
                Modifier
            }
        )
        .pointerInput(onClick) {
            detectTapGestures(
                onPress = { offset ->
                    pointerPos = offset
                    pressed = true
                    tryAwaitRelease()
                    pressed = false
                    pointerPos = Offset.Unspecified
                },
                onTap = { onClick() }
            )
        }
}

/**
 * Faithful vertical adaptation of Kyant0/AndroidLiquidGlass's `LiquidBottomTabs` navbar for the Launcher Sidebar:
 * - Features a translucent Liquid Glass track container (`liquidGlassSurface`).
 * - A continuous, spring-animated sliding Refractive Lens Pill Indicator (`pillIndexAnim`) that:
 *   1. Glides smoothly between tabs using Kyant0's underdamped spring (`stiffness = 300f, dampingRatio = 0.58f`).
 *   2. Inflates into a protruding refractive glass lens (`pillScaleX`, `pillScaleY`) with chromatic dispersion
 *      rim lighting and pointer-following radial glow when pressed or dragged vertically along the sidebar!
 *   3. Supports both single-tap tab switching AND continuous vertical finger dragging across the rail!
 */
@Composable
fun <T> KyantLiquidGlassSidebarRail(
    tabs: List<T>,
    selectedTab: T,
    isLiquidGlass: Boolean,
    trackColor: Color,
    activePillColor: Color,
    activeIconColor: Color,
    inactiveIconColor: Color,
    tabIcon: (T) -> ImageVector,
    tabTitle: (T) -> String,
    onSelectTab: (T) -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val selectedIndex = remember(tabs, selectedTab) {
        tabs.indexOf(selectedTab).coerceAtLeast(0)
    }

    val pillIndexAnim = remember { Animatable(selectedIndex.toFloat()) }
    var isDraggingOrPressed by remember { mutableStateOf(false) }
    var pointerOffset by remember { mutableStateOf(Offset.Unspecified) }
    var dragVelocityY by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(selectedIndex, isDraggingOrPressed) {
        if (!isDraggingOrPressed) {
            pillIndexAnim.animateTo(
                targetValue = selectedIndex.toFloat(),
                animationSpec = spring(
                    dampingRatio = if (isLiquidGlass) 0.58f else 0.72f,
                    stiffness = if (isLiquidGlass) 300f else 380f
                )
            )
        }
    }

    val lensInflation by animateFloatAsState(
        targetValue = if (isDraggingOrPressed) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.52f, stiffness = 260f),
        label = "KyantLensInflation"
    )

    val motionStretch = (abs(pillIndexAnim.velocity) * 0.035f + abs(dragVelocityY) * 0.0008f).coerceIn(0f, 0.22f)
    val pillScaleX = 1f + (lensInflation * 0.14f) - (motionStretch * 0.25f)
    val pillScaleY = 1f + (lensInflation * 0.12f) + (motionStretch * 0.55f)

    val itemSizeDp = 38.dp
    val itemSpacingDp = 8.dp
    val density = LocalDensity.current
    val itemStepPx = with(density) { (itemSizeDp + itemSpacingDp).toPx() }
    var railHeightPx by remember { mutableFloatStateOf(0f) }

    val totalItemsHeightPx = remember(tabs.size, itemStepPx) {
        if (tabs.isEmpty()) 0f else tabs.size * with(density) { itemSizeDp.toPx() } + (tabs.size - 1) * with(density) { itemSpacingDp.toPx() }
    }
    val topStartY = max(0f, (railHeightPx - totalItemsHeightPx) * 0.5f)

    Box(
        modifier = modifier
            .fillMaxHeight()
            .width(54.dp)
            .onSizeChanged { railHeightPx = it.height.toFloat() }
            .then(
                if (isLiquidGlass) {
                    Modifier.liquidGlassSurface(
                        enabled = true,
                        spec = KyantLiquidGlassSpec(
                            cornerRadius = 22.dp,
                            refractionHeightPx = 30f,
                            refractionAmountPx = -42f,
                            chromaticAberration = 1.45f,
                            surfaceTint = Color(0x2E8BE0FF),
                            surfaceCover = Color(0x480D151E),
                            rimHighlightAlpha = 0.68f,
                            enableAgslShader = true
                        ),
                        pointerOffset = pointerOffset,
                        pressProgress = lensInflation
                    )
                } else {
                    Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(trackColor)
                }
            )
            .pointerInput(tabs, topStartY, itemStepPx) {
                detectDragGestures(
                    onDragStart = { startOffset ->
                        isDraggingOrPressed = true
                        pointerOffset = startOffset
                        val rawIdx = ((startOffset.y - topStartY) / itemStepPx) - 0.35f
                        val clamped = rawIdx.coerceIn(0f, (tabs.size - 1).toFloat())
                        scope.launch { pillIndexAnim.snapTo(clamped) }
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        pointerOffset = change.position
                        dragVelocityY = dragAmount.y
                        val rawIdx = ((change.position.y - topStartY) / itemStepPx) - 0.35f
                        val clamped = rawIdx.coerceIn(0f, (tabs.size - 1).toFloat())
                        scope.launch { pillIndexAnim.snapTo(clamped) }
                        val nearest = clamped.roundToInt().coerceIn(0, tabs.lastIndex)
                        if (tabs[nearest] != selectedTab) {
                            onSelectTab(tabs[nearest])
                        }
                    },
                    onDragEnd = {
                        val finalIdx = pillIndexAnim.value.roundToInt().coerceIn(0, tabs.lastIndex)
                        onSelectTab(tabs[finalIdx])
                        dragVelocityY = 0f
                        isDraggingOrPressed = false
                        pointerOffset = Offset.Unspecified
                    },
                    onDragCancel = {
                        dragVelocityY = 0f
                        isDraggingOrPressed = false
                        pointerOffset = Offset.Unspecified
                    }
                )
            }
    ) {
        if (railHeightPx > 0f && tabs.isNotEmpty()) {
            val pillTopPx = topStartY + pillIndexAnim.value * itemStepPx
            Box(
                modifier = Modifier
                    .offset { IntOffset(x = 0, y = pillTopPx.roundToInt()) }
                    .align(Alignment.TopCenter)
                    .size(itemSizeDp)
                    .graphicsLayer {
                        scaleX = pillScaleX
                        scaleY = pillScaleY
                    }
                    .then(
                        if (isLiquidGlass) {
                            Modifier.liquidGlassSurface(
                                enabled = true,
                                spec = KyantLiquidGlassSpec(
                                    cornerRadius = 15.dp,
                                    refractionHeightPx = 22f,
                                    refractionAmountPx = -46f,
                                    chromaticAberration = 1.65f,
                                    surfaceTint = Color(0x5290E4FF),
                                    surfaceCover = Color(0x55182838),
                                    rimHighlightAlpha = 0.88f,
                                    enableAgslShader = false
                                ),
                                pressProgress = 0.45f + lensInflation * 0.55f
                            )
                        } else {
                            Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .background(activePillColor)
                        }
                    )
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(itemSpacingDp, Alignment.CenterVertically)
        ) {
            tabs.forEachIndexed { idx, tab ->
                val dist = abs(pillIndexAnim.value - idx.toFloat()).coerceIn(0f, 1f)
                val selectedFraction = 1f - dist
                val iconScale = 1f + selectedFraction * (if (isLiquidGlass) 0.14f else 0.06f)

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(itemSizeDp)
                        .clip(RoundedCornerShape(14.dp))
                        .pointerInput(tab) {
                            detectTapGestures(
                                onPress = { off ->
                                    isDraggingOrPressed = true
                                    pointerOffset = Offset(off.x, topStartY + idx * itemStepPx + off.y)
                                    tryAwaitRelease()
                                    isDraggingOrPressed = false
                                    pointerOffset = Offset.Unspecified
                                },
                                onTap = {
                                    onSelectTab(tab)
                                }
                            )
                        }
                ) {
                    Icon(
                        imageVector = tabIcon(tab),
                        contentDescription = tabTitle(tab),
                        tint = if (selectedFraction > 0.5f) {
                            if (isLiquidGlass) Color.White else activeIconColor
                        } else {
                            if (isLiquidGlass) Color(0xFFB8C7D4) else inactiveIconColor
                        },
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
