package com.yunx.cloud.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import com.yunx.cloud.ui.navigation.MainTab

/* ========== Soft UI（微新拟物）品牌色 ========== */

/** 梦幻紫罗兰渐变：起始 -> 主体 -> 深沉 */
internal val BrandVioletStart = Color(0xFFA5B4FC)
internal val BrandVioletMid = Color(0xFF818CF8)
internal val BrandVioletDeep = Color(0xFF6366F1)
internal val BrandVioletInk = Color(0xFF4F46E5)
/** 浅紫微透底座（图标底座 / 选中高光底） */
internal val SoftBadgeLight = Color(0xFFEEF2FF)
internal val SoftBadgeDeep = Color(0xFFF5F3FF)
/** 内嵌输入底盒底色 */
internal val SoftInputBgLight = Color(0xFFF8FAFC)
/** 输入占位/辅助文字 */
internal val SoftTextHint = Color(0xFF94A3B8)
/** 弥散投影（Blur 18dp / Offset Y 6dp 的等效合成色） */
internal val SoftShadowAmbient = Color(0x1F0C1E29)
internal val SoftShadowSpot = Color(0x3B0C1E29)

internal val SoftCardShape = RoundedCornerShape(18.dp)
internal val SoftFieldShape = RoundedCornerShape(10.dp)

/**
 * 柔和弥散阴影：与常规硬边阴影不同，拆成 ambient（环境弱影）+ spot（更聚焦的投影），
 * 得到"卡片浮起"而非"轮廓发光"的层次感。
 */
internal fun Modifier.softShadow(
    shape: Shape,
    elevation: Dp = 8.dp,
    ambient: Color = SoftShadowAmbient,
    spot: Color = SoftShadowSpot
): Modifier = shadow(
    elevation = elevation,
    shape = shape,
    ambientColor = ambient,
    spotColor = spot
)

/* ========== 3D Hero 插画：斜置链扣 + 双层托盘 + 悬浮粒子 ========== */

/**
 * 解析页顶部插画：一枚微倾斜的链条圆环悬于双层微拟物阶梯托盘之上，
 * 两侧漂浮两颗淡紫水滴粒子（轻微上下往复呼吸），营造立体空间感。
 */
@Composable
fun SoftHeroIllustration(modifier: Modifier = Modifier) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    // 粒子呼吸动画
    val transition = rememberInfiniteTransition(label = "heroParticles")
    val p1 by transition.animateFloat(
        initialValue = 0f, targetValue = 4f,
        animationSpec = infiniteRepeatable(tween(1500, easing = LinearEasing), RepeatMode.Reverse),
        label = "p1"
    )
    val p2 by transition.animateFloat(
        initialValue = 0f, targetValue = 3f,
        animationSpec = infiniteRepeatable(tween(1900, easing = LinearEasing), RepeatMode.Reverse),
        label = "p2"
    )
    val trayLight = if (isDark) Color(0x4DA5B4FC) else Color(0xFFEEF2FF)
    val trayDeep = if (isDark) Color(0x59A5B4FC) else Color(0xFFE0E7FF)
    val ringBrush = Brush.linearGradient(
        listOf(BrandVioletStart, BrandVioletMid, BrandVioletDeep, BrandVioletInk)
    )

    Box(modifier = modifier.size(width = 128.dp, height = 100.dp)) {
        // 上层托盘（承托插画主体）
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .size(width = 54.dp, height = 10.dp)
                .clip(RoundedCornerShape(5.dp))
                .background(trayLight)
        )
        // 下层托盘（更宽的阶梯底座，略带投影）
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .offset(y = 6.dp)
                .size(width = 78.dp, height = 12.dp)
                .clip(RoundedCornerShape(6.dp))
                .softShadow(RoundedCornerShape(6.dp), elevation = 6.dp, spot = SoftShadowAmbient)
                .background(trayDeep)
        )
        // 斜置微晶链扣（45° 倾角），以 gradient 描边呈现发光感
        Canvas(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = 4.dp)
                .size(width = 74.dp, height = 56.dp)
        ) {
            val stroke = 6.dp.toPx()
            val inset = stroke / 2f
            val w = size.width - stroke
            val h = size.height - stroke
            val sweep = 300f // 留出缺口，形成"锁扣"而非完整圆环
            drawArc(
                brush = ringBrush,
                startAngle = -75f,
                sweepAngle = sweep,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = Size(w, h),
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
            // 内侧高光（左上受光），提升晶体体积感
            drawArc(
                color = Color.White.copy(alpha = 0.55f),
                startAngle = -60f,
                sweepAngle = 100f,
                useCenter = false,
                topLeft = Offset(inset + stroke * 1.4f, inset + stroke * 1.4f),
                size = Size(w - stroke * 5.2f, h - stroke * 5.2f),
                style = Stroke(width = 1.4.dp.toPx(), cap = StrokeCap.Round)
            )
        }
        // 右上粒子
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 2.dp, y = (4.dp + p1.dp))
                .size(9.dp)
                .clip(CircleShape)
                .background(Brush.verticalGradient(listOf(Color(0xFFC7D2FE), BrandVioletMid)))
        )
        // 左下粒子
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .offset(x = 12.dp, y = (-6.dp + p2.dp))
                .size(6.dp)
                .clip(CircleShape)
                .background(Brush.verticalGradient(listOf(Color(0xFFC7D2FE), BrandVioletDeep)))
        )
    }
}

/* ========== 输入槽：内嵌底盒 + 焦点描边动画 ========== */

/**
 * 无边框内嵌式输入框：浅灰底盒 + 圆角；聚焦时描边平滑过渡为紫罗兰并轻微加粗。
 * 高度参数遵循当前 APP 输入习惯（紧凑），多行随内容伸展。
 */
@Composable
fun SoftInputField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    singleLine: Boolean = false,
    minLines: Int = 1,
    maxLines: Int = 3,
    imeAction: ImeAction = ImeAction.Done,
    onImeAction: () -> Unit = {},
    trailingIcon: (@Composable () -> Unit)? = null,
    textColor: Color = MaterialTheme.colorScheme.onSurface
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val bg = if (isDark) Color.White.copy(alpha = 0.06f) else SoftInputBgLight
    val borderColor by animateColorAsState(
        targetValue = if (focused) BrandVioletMid else bg,
        animationSpec = NativeSpringColorSoft,
        label = "fieldBorder"
    )
    val borderWidth by animateFloatAsState(
        targetValue = if (focused) 1.5f else 1f,
        animationSpec = NativeSpringSoft,
        label = "fieldBorderWidth"
    )
    val hintColor = if (isDark) MaterialTheme.colorScheme.outline else SoftTextHint

    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        singleLine = singleLine,
        minLines = minLines,
        maxLines = maxLines,
        textStyle = LocalTextStyle.current.copy(
            color = textColor,
            fontSize = 13.sp,
            lineHeight = 18.sp
        ),
        cursorBrush = SolidColor(BrandVioletInk),
        interactionSource = interaction,
        keyboardOptions = KeyboardOptions(imeAction = imeAction),
        keyboardActions = KeyboardActions(onDone = { onImeAction() }),
        decorationBox = { innerTextField ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(SoftFieldShape)
                    .background(bg)
                    .border(borderWidth.dp, borderColor, SoftFieldShape)
                    .padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 40.dp)
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (value.isEmpty()) {
                        Text(
                            text = placeholder,
                            fontSize = 11.sp,
                            color = hintColor,
                            maxLines = 1
                        )
                    }
                    innerTextField()
                }
                trailingIcon?.invoke()
            }
        }
    )
}

/* ========== 主执行按钮：满宽胶囊渐变 + 波纹缩放 ========== */

/**
 * 主执行按钮：紫蓝->正紫渐变填充，外围紫光扩散投影；
 * 按压时整体缩放 0.97 并带涟漪反馈；加载态展示进度圈与"解析中…"。
 */
@Composable
fun SoftGradientButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    minHeight: Dp = 50.dp
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.97f else 1f,
        animationSpec = NativeSpringSoft,
        label = "buttonScale"
    )
    val gradient = Brush.horizontalGradient(
        listOf(BrandVioletMid, BrandVioletDeep, BrandVioletInk)
    )
    val shape = RoundedCornerShape(minHeight / 2)

    Box(
        modifier = modifier
            .height(minHeight)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .softShadow(shape, elevation = 12.dp, spot = Color(0x3D6366F1))
            .clip(shape)
            .background(gradient)
            .clickable(enabled = enabled, interactionSource = interaction, indication = LocalIndication.current) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = Color.White
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = if (loading) "解析中…" else text,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White.copy(alpha = if (enabled) 1f else 0.55f)
            )
            Spacer(modifier = Modifier.weight(1f))
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = Color.White.copy(alpha = if (enabled) 1f else 0.55f),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

/* ========== 底部悬浮胶囊导航栏 ========== */

private val SoftBarShape = RoundedCornerShape(30.dp)

/**
 * 底部悬浮胶囊导航：白底 92% 半透明 + 柔和投影，4 项横向均分；
 * 选中项套浅紫胶囊底座并点亮紫罗兰，未选中保持线条深灰。
 * 尺寸相对系统 NavigationBar 更紧凑（60dp），贴合"精致紧凑"的视觉目标。
 */
@Composable
fun SoftBottomBar(
    currentTab: MainTab,
    onTabSelected: (MainTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val barColor = if (isDark) Color(0xF2111318) else Color(0xEBFFFFFF)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom))
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Surface(
            shape = SoftBarShape,
            color = barColor,
            shadowElevation = 4.dp,
            modifier = Modifier
                .fillMaxWidth()
                .softShadow(SoftBarShape, elevation = 16.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth().height(60.dp)) {
                MainTab.values().forEach { tab ->
                    SoftTabItem(
                        selected = currentTab == tab,
                        tab = tab,
                        onClick = { onTabSelected(tab) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun SoftTabItem(
    selected: Boolean,
    tab: MainTab,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val badgeColor = if (isDark) Color(0x59A5B4FC) else SoftBadgeLight
    val idleTint = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else Color(0xFF4B5563)
    val activeTint = if (isDark) MaterialTheme.colorScheme.primary else BrandVioletInk
    val highlight by animateColorAsState(
        targetValue = if (selected) badgeColor else Color.Transparent,
        animationSpec = NativeSpringColorSoft,
        label = "tabBadge"
    )
    val iconTint by animateColorAsState(
        targetValue = if (selected) activeTint else idleTint,
        animationSpec = NativeSpringColorSoft,
        label = "tabIcon"
    )
    val labelColor by animateColorAsState(
        targetValue = if (selected) activeTint else idleTint,
        animationSpec = NativeSpringColorSoft,
        label = "tabLabel"
    )

    Column(
        modifier = modifier
            .height(60.dp)
            .clip(SoftBarShape)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = LocalIndication.current, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(width = 46.dp, height = 26.dp)
                .clip(RoundedCornerShape(13.dp))
                .background(highlight),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                contentDescription = tab.title,
                tint = iconTint,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = tab.title,
            fontSize = 10.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = labelColor,
            maxLines = 1
        )
    }
}