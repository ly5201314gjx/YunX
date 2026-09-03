package com.yunx.cloud.ui.components

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.Shadow
import com.yunx.cloud.ui.animation.InteractiveHighlight
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tanh

/**
 * 析盘屏幕级 Backdrop（液态玻璃采样源）。
 * 由主框架（MainScreen）提供：背景层 + 内容层合并而成；
 * 玻璃组件（顶栏胶囊、底部导航、卡片）通过 [LocalScreenBackdrop] 获取采样源，
 * 自身绝不写回该层，避免自采样（与 legado-with-MD3 的 LocalTopBarBackdrop 同机制）。
 */
val LocalScreenBackdrop = compositionLocalOf<Backdrop?> { null }

/**
 * 页面内联玻璃卡片（如解析页输入卡片）的采样源：仅背景层。
 * 卡片位于内容层内部，若采样内容层会自采样；故采样纯背景层。
 */
val LocalGlassSurfaceBackdrop = compositionLocalOf<Backdrop?> { null }

/** 液态玻璃是否可用：需要 Android 13+（RenderEffect/RuntimeShader 支持）且存在取样源 */
val liquidGlassSupported: Boolean
    @Composable
    @ReadOnlyComposable
    get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
        LocalScreenBackdrop.current != null

/* ========== 顶栏玻璃胶囊（复刻自 legado TopBarLiquidGlass，1:1 效果） ========== */

/**
 * 真液态玻璃胶囊（顶栏按钮等）：真模糊(blur) + 色彩增强(vibrancy) + 折射(lens) + 边缘高光(Highlight) + 投影(Shadow)；
 * 支持按压/拖拽动态效果（InteractiveHighlight）：触摸处点亮径向高光，拖拽时玻璃按液态折射律动。
 * Android 13 以下或无线索源时自动回退为无效果。
 */
@Composable
fun Modifier.topBarLiquidGlass(shape: Shape): Modifier {
    val backdrop = LocalScreenBackdrop.current ?: return this
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return this
    val containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surface.copy(
        alpha = 0.5f
    )
    val shadowColor = Color.Black.copy(alpha = 0.04f)
    val animationScope = rememberCoroutineScope()
    val interactiveHighlight = remember(animationScope) { InteractiveHighlight(animationScope) }
    return drawBackdrop(
        backdrop = backdrop,
        shape = { shape },
        effects = {
            vibrancy()
            blur(12.dp.toPx())
            lens(24.dp.toPx(), 24.dp.toPx())
        },
        highlight = { Highlight.Default },
        shadow = {
            Shadow(
                radius = 12.dp,
                color = shadowColor
            )
        },
        layerBlock = {
            val width = size.width
            val height = size.height
            if (width > 0f && height > 0f) {
                val progress = interactiveHighlight.pressProgress
                val scale = 1f + 4.dp.toPx() / height * progress
                val maxOffset = size.minDimension
                val dragOffset = interactiveHighlight.dragOffset
                translationX = maxOffset * tanh(0.05f * dragOffset.x / maxOffset) * progress
                translationY = maxOffset * tanh(0.05f * dragOffset.y / maxOffset) * progress
                val maxDragScale = 4.dp.toPx() / height
                val offsetAngle = atan2(dragOffset.y, dragOffset.x)
                scaleX = scale + maxDragScale *
                    abs(cos(offsetAngle) * dragOffset.x / size.maxDimension) *
                    (width / height).coerceAtMost(1f) * progress
                scaleY = scale + maxDragScale *
                    abs(sin(offsetAngle) * dragOffset.y / size.maxDimension) *
                    (height / width).coerceAtMost(1f) * progress
            }
        },
        onDrawSurface = {
            drawRect(containerColor)
        },
    )
        .then(interactiveHighlight.modifier)
        .then(interactiveHighlight.gestureModifier)
}

/* ========== 玻璃表面容器（复刻自 legado ReaderMenuGlass，1:1 效果） ========== */

/**
 * 通用真液态玻璃表面（输入卡片 / 剪贴板提示卡等）：真模糊 + vibrancy + 可选折射 + 边缘高光；
 * [interactive] 开启后带 InteractiveHighlight 动态效果（按压高光 + 拖拽折射律动）。
 */
@Composable
fun Modifier.surfaceLiquidGlass(
    backdrop: Backdrop?,
    shape: Shape,
    surfaceBrush: Brush,
    blurRadius: Dp,
    lensRadius: Dp,
    useLens: Boolean,
    interactive: Boolean = false,
): Modifier {
    if (backdrop == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
        // 无液态玻璃能力（Android 12- / 未提供采样源）时回退为静态半透明表面，保持卡片视觉完整
        return this.background(surfaceBrush, shape)
    }

    val animationScope = rememberCoroutineScope()
    val interactiveHighlight = if (interactive) {
        remember(animationScope) { InteractiveHighlight(animationScope) }
    } else {
        null
    }

    return drawBackdrop(
        backdrop = backdrop,
        shape = { shape },
        effects = {
            vibrancy()
            blur(blurRadius.coerceAtLeast(0.dp).toPx())
            if (useLens) {
                lens(lensRadius.toPx(), lensRadius.toPx())
            }
        },
        highlight = { Highlight.Default },
        shadow = null,
        layerBlock = if (interactiveHighlight != null) {
            {
                val width = size.width
                val height = size.height
                if (width > 0f && height > 0f) {
                    val progress = interactiveHighlight.pressProgress
                    val scale = 1f + 4.dp.toPx() / height * progress
                    val maxOffset = size.minDimension
                    val dragOffset = interactiveHighlight.dragOffset
                    translationX = maxOffset *
                        tanh(0.05f * dragOffset.x / maxOffset) * progress
                    translationY = maxOffset *
                        tanh(0.05f * dragOffset.y / maxOffset) * progress

                    val maxDragScale = 4.dp.toPx() / height
                    val offsetAngle = atan2(dragOffset.y, dragOffset.x)
                    scaleX = scale + maxDragScale *
                        abs(cos(offsetAngle) * dragOffset.x / size.maxDimension) *
                        (width / height).coerceAtMost(1f) * progress
                    scaleY = scale + maxDragScale *
                        abs(sin(offsetAngle) * dragOffset.y / size.maxDimension) *
                        (height / width).coerceAtMost(1f) * progress
                }
            }
        } else {
            null
        },
        onDrawSurface = { drawRect(surfaceBrush) },
    )
        .then(interactiveHighlight?.modifier ?: Modifier)
        .then(interactiveHighlight?.gestureModifier ?: Modifier)
}