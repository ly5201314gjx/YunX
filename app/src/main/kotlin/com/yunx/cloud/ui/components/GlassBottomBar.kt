package com.yunx.cloud.ui.components

import android.os.Build
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastCoerceIn
import androidx.compose.ui.util.fastRoundToInt
import androidx.compose.ui.util.lerp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberCombinedBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.InnerShadow
import com.kyant.backdrop.shadow.Shadow
import com.kyant.capsule.ContinuousCapsule
import com.yunx.cloud.ui.animation.DampedDragAnimation
import com.yunx.cloud.ui.animation.InteractiveHighlight
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.sign

/*
 * 复刻自 legado-with-MD3（io.legado.app.ui.widget.components.FloatingBottomBar）：1:1 液态玻璃动效。
 * 改动仅为：容器色改用析盘主题 surfaceContainer、高度/内边距贴合析盘底部栏、tab 数量由参数决定。
 * 真液态玻璃：blur + vibrancy + lens（折射）+ 动态高光/内阴影 + 拖拽吸附/缩放/速度惯性。
 */

val LocalGlassBottomBarTabScale = staticCompositionLocalOf { { 1f } }

@Composable
fun RowScope.GlassBottomBarItem(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val scale = LocalGlassBottomBarTabScale.current
    Column(
        modifier
            .clip(ContinuousCapsule)
            .clickable(
                interactionSource = null,
                indication = null,
                role = Role.Tab,
                onClick = onClick,
            )
            .fillMaxHeight()
            .weight(1f)
            .graphicsLayer {
                val currentScale = scale()
                scaleX = currentScale
                scaleY = currentScale
            },
        verticalArrangement = Arrangement.spacedBy(1.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
        content = content
    )
}

@Composable
fun GlassBottomBar(
    modifier: Modifier = Modifier,
    selectedIndex: () -> Int,
    onSelected: (index: Int) -> Unit,
    onReselected: (index: Int) -> Unit = {},
    backdrop: Backdrop,
    tabsCount: Int,
    isBlurEnabled: Boolean = true,
    hasCustomIcons: Boolean = false,
    content: @Composable RowScope.() -> Unit
) {
    val isInLightTheme = MaterialTheme.colorScheme.background.luminance() >= 0.5f
    val accentColor = MaterialTheme.colorScheme.primary
    val containerColor = if (isBlurEnabled) {
        MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.72f)
    } else {
        MaterialTheme.colorScheme.surfaceContainer
    }
    // 容器底色：竖向三段渐变（顶部霜感高光带 → 主体半透明玻璃色），
    // 相比两段平涂更贴近真实毛玻璃"顶缘受光、向下透出内容"的立体质感
    val drawContainer: DrawScope.() -> Unit = {
        drawRect(
            brush = Brush.verticalGradient(
                colorStops = arrayOf(
                    0.0f to Color.White.copy(if (isInLightTheme) 0.70f else 0.20f),
                    0.18f to Color.White.copy(if (isInLightTheme) 0.35f else 0.08f),
                    1.0f to containerColor
                ),
                startY = 0f,
                endY = size.height
            )
        )
    }

    val tabsBackdrop = rememberLayerBackdrop()
    val density = LocalDensity.current
    val isLtr = LocalLayoutDirection.current == LayoutDirection.Ltr
    val animationScope = rememberCoroutineScope()

    var tabWidthPx by remember { mutableFloatStateOf(0f) }
    var totalWidthPx by remember { mutableFloatStateOf(0f) }

    val offsetAnimation = remember { Animatable(0f) }
    val panelOffset by remember(density) {
        derivedStateOf {
            if (totalWidthPx == 0f) {
                0f
            } else {
                val fraction = (offsetAnimation.value / totalWidthPx).fastCoerceIn(-1f, 1f)
                with(density) {
                    4f.dp.toPx() * fraction.sign * EaseOut.transform(abs(fraction))
                }
            }
        }
    }

    var currentIndex by remember { mutableIntStateOf(selectedIndex()) }
    // 拖拽中标记：拖拽期间内容已实时跟随指示胶囊，暂停吸附动画，避免与手指拖拽互相打断
    var dragging by remember { mutableStateOf(false) }

    class DampedDragAnimationHolder {
        var instance: DampedDragAnimation? = null
    }

    val holder = remember { DampedDragAnimationHolder() }

    val dampedDragAnimation = remember(animationScope, tabsCount, density, isLtr) {
        DampedDragAnimation(
            animationScope = animationScope,
            initialValue = selectedIndex().toFloat(),
            valueRange = 0f..(tabsCount - 1).toFloat(),
            visibilityThreshold = 0.001f,
            initialScale = 1f,
            pressedScale = 78f / 56f,
            canDrag = { offset ->
                val anim = holder.instance ?: return@DampedDragAnimation true
                if (tabWidthPx == 0f) return@DampedDragAnimation false

                val currentValue = anim.value
                val indicatorX = currentValue * tabWidthPx
                val padding = with(density) { 4.dp.toPx() }
                val globalTouchX = if (isLtr) {
                    val touchX = indicatorX + offset.x
                    padding + touchX
                } else {
                    totalWidthPx - padding - tabWidthPx - indicatorX + offset.x
                }
                globalTouchX in 0f..totalWidthPx
            },
            onDragStarted = {
                dragging = true
            },
            onDragStopped = {
                dragging = false
                val targetIndex = targetValue.fastRoundToInt().fastCoerceIn(0, tabsCount - 1)
                currentIndex = targetIndex
                animateToValue(targetIndex.toFloat())
                if (targetIndex != selectedIndex()) {
                    onSelected(targetIndex)
                } else {
                    onReselected(targetIndex)
                }
                animationScope.launch {
                    offsetAnimation.animateTo(0f, spring(1f, 300f, 0.5f))
                }
            },
            onDrag = { _, dragAmount ->
                if (tabWidthPx > 0f) {
                    updateValue(
                        (targetValue + dragAmount.x / tabWidthPx * if (isLtr) 1f else -1f)
                            .fastCoerceIn(0f, (tabsCount - 1).toFloat())
                    )
                    // 内容实时跟随指示胶囊：越过相邻 Tab 中点即切换内容，
                    // 避免「液态玻璃已切换、上方内容仍停在旧 Tab」的错位感
                    val targetIdx = targetValue.fastRoundToInt().fastCoerceIn(0, tabsCount - 1)
                    if (targetIdx != selectedIndex()) {
                        onSelected(targetIdx)
                    }
                    animationScope.launch {
                        offsetAnimation.snapTo(offsetAnimation.value + dragAmount.x)
                    }
                }
            }
        ).also { holder.instance = it }
    }

    // ★ 以「外部选中索引的值」作为 key，兼顾点击与拖拽：
    //   - 直接点击某个 Tab：currentTab 变化 → selectedIndex() 返回值变化 → effect 重启 → 指示胶囊立即吸附过去；
    //   - 拖拽过程中 currentTab 不变 → key 不变 → 不会因下载进度等无关重组重启 effect、打断拖拽吸附/阻尼动画。
    //   （原 legado 以重组即新建的 selectedIndex lambda 为 key，点击正常但滑动会被无关重组打断；
    //     之前改为仅依赖稳定实例 + snapshotFlow 后又出现点击不吸附的问题，故改为直接以值驱动。）
    val externalIndex = selectedIndex()
    LaunchedEffect(dampedDragAnimation, externalIndex) {
        currentIndex = externalIndex
        dampedDragAnimation.animateToValue(externalIndex.toFloat())
    }

    val interactiveHighlight =
        if (isBlurEnabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            remember(animationScope, tabWidthPx) {
                InteractiveHighlight(
                    animationScope = animationScope,
                    position = { size, _ ->
                        Offset(
                            if (isLtr) {
                                (dampedDragAnimation.value + 0.5f) * tabWidthPx + panelOffset
                            } else {
                                size.width - (dampedDragAnimation.value + 0.5f) * tabWidthPx + panelOffset
                            },
                            size.height / 2f
                        )
                    }
                )
            }
        } else {
            null
        }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .onGloballyPositioned { coords ->
                    totalWidthPx = coords.size.width.toFloat()
                    val contentWidthPx = totalWidthPx - with(density) { 8.dp.toPx() }
                    tabWidthPx = contentWidthPx / tabsCount
                }
                .graphicsLayer { translationX = panelOffset }
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {}
                )
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { ContinuousCapsule },
                    effects = {
                        if (isBlurEnabled) {
                            vibrancy()
                            blur(12.dp.toPx())
                            lens(24.dp.toPx(), 24.dp.toPx())
                        }
                    },
                    highlight = {
                        Highlight.Default.copy(alpha = if (isBlurEnabled) 1f else 0f)
                    },
                    shadow = {
                        Shadow.Default.copy(
                            color = Color.Black.copy(if (isInLightTheme) 0.1f else 0.2f)
                        )
                    },
                    layerBlock = {
                        if (isBlurEnabled) {
                            val progress = dampedDragAnimation.pressProgress
                            val scale = lerp(1f, 1f + 16f.dp.toPx() / size.width, progress)
                            scaleX = scale
                            scaleY = scale
                        }
                    },
                    onDrawSurface = drawContainer
                )
                .then(
                    if (isBlurEnabled && interactiveHighlight != null) {
                        interactiveHighlight.modifier
                    } else {
                        Modifier
                    }
                )
                .height(64.dp)
                .padding(4.dp),
            verticalAlignment = Alignment.CenterVertically,
            content = content
        )

        CompositionLocalProvider(
            LocalGlassBottomBarTabScale provides {
                if (isBlurEnabled) {
                    lerp(1f, 1.2f, dampedDragAnimation.pressProgress)
                } else {
                    1f
                }
            }
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .clearAndSetSemantics {}
                    .alpha(0f)
                    .layerBackdrop(tabsBackdrop)
                    .graphicsLayer { translationX = panelOffset }
                    .drawBackdrop(
                        backdrop = backdrop,
                        shape = { ContinuousCapsule },
                        effects = {
                            if (isBlurEnabled) {
                                val progress = dampedDragAnimation.pressProgress
                                vibrancy()
                                blur(12.dp.toPx())
                                lens(24.dp.toPx() * progress, 24.dp.toPx() * progress)
                            }
                        },
                        highlight = {
                            Highlight.Default.copy(
                                alpha = if (isBlurEnabled) {
                                    dampedDragAnimation.pressProgress
                                } else {
                                    0f
                                }
                            )
                        },
                        onDrawSurface = drawContainer
                    )
                    .then(
                        if (isBlurEnabled && interactiveHighlight != null) {
                            interactiveHighlight.modifier
                        } else {
                            Modifier
                        }
                    )
                    .height(56.dp)
                    .padding(horizontal = 4.dp)
                    .then(
                        if (hasCustomIcons) Modifier
                        else Modifier.graphicsLayer(colorFilter = ColorFilter.tint(accentColor))
                    ),
                verticalAlignment = Alignment.CenterVertically,
                content = content
            )
        }

        if (tabWidthPx > 0f) {
            Box(
                Modifier
                    .padding(horizontal = 4.dp)
                    .graphicsLayer {
                        val contentWidth = totalWidthPx - with(density) { 8.dp.toPx() }
                        val singleTabWidth = contentWidth / tabsCount
                        val progressOffset = dampedDragAnimation.value * singleTabWidth

                        translationX = if (isLtr) {
                            progressOffset + panelOffset
                        } else {
                            -progressOffset + panelOffset
                        }
                    }
                    .then(
                        if (isBlurEnabled && interactiveHighlight != null) {
                            interactiveHighlight.gestureModifier
                        } else {
                            Modifier
                        }
                    )
                    .then(dampedDragAnimation.modifier)
                    .drawBackdrop(
                        backdrop = rememberCombinedBackdrop(backdrop, tabsBackdrop),
                        shape = { ContinuousCapsule },
                        effects = {
                            if (isBlurEnabled) {
                                val progress = dampedDragAnimation.pressProgress
                                lens(10f.dp.toPx() * progress, 14f.dp.toPx() * progress, true)
                            }
                        },
                        highlight = {
                            Highlight.Default.copy(
                                alpha = if (isBlurEnabled) {
                                    dampedDragAnimation.pressProgress
                                } else {
                                    0f
                                }
                            )
                        },
                        shadow = {
                            Shadow(alpha = if (isBlurEnabled) dampedDragAnimation.pressProgress else 0f)
                        },
                        innerShadow = {
                            InnerShadow(
                                radius = 8f.dp * dampedDragAnimation.pressProgress,
                                alpha = if (isBlurEnabled) dampedDragAnimation.pressProgress else 0f
                            )
                        },
                        layerBlock = {
                            if (isBlurEnabled) {
                                scaleX = dampedDragAnimation.scaleX
                                scaleY = dampedDragAnimation.scaleY
                                val velocity = dampedDragAnimation.velocity / 10f
                                scaleX /= 1f - (velocity * 0.75f).fastCoerceIn(-0.2f, 0.2f)
                                scaleY *= 1f - (velocity * 0.25f).fastCoerceIn(-0.2f, 0.2f)
                            }
                        },
                        onDrawSurface = {
                            val progress =
                                if (isBlurEnabled) dampedDragAnimation.pressProgress else 0f
                            drawRect(
                                color = if (isInLightTheme) {
                                    Color.Black.copy(0.1f)
                                } else {
                                    Color.White.copy(0.1f)
                                },
                                alpha = 1f - progress
                            )
                            drawRect(Color.Black.copy(alpha = 0.03f * progress))
                        }
                    )
                    .height(56.dp)
                    .width(with(density) { ((totalWidthPx - 8.dp.toPx()) / tabsCount).toDp() })
            )
        }
    }
}