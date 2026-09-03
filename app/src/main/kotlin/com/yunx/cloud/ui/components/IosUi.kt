package com.yunx.cloud.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

/* =========================================================
 * iOS 设计系统（iOS Design Language）
 * 灵感来自 iOS 16+ 系统设置 / 文件 App 的「分组内嵌列表 + 玻璃质感」：
 *  - 浅灰分组背景 + 纯白圆角卡片
 *  - 克制的 0.5dp 分割线、17pt 正文、13pt 次级文字
 *  - iOS 系统蓝/绿/红作为交互强调色
 *  - 全部动效采用最流畅的原生安卓弹簧（Spring）与轻量渐变动画
 * ========================================================= */

/* ========== iOS 系统色（自适应亮 / 暗） ========== */

internal val IosBlue = Color(0xFF007AFF)
internal val IosGreen = Color(0xFF34C759)
internal val IosRed = Color(0xFFFF3B30)
internal val IosOrange = Color(0xFFFF9500)
internal val IosYellow = Color(0xFFFFCC00)
internal val IosTeal = Color(0xFF5AC8FA)
internal val IosPurple = Color(0xFFAF52DE)
internal val IosPink = Color(0xFFFF2D55)
internal val IosIndigo = Color(0xFF5856D6)
internal val IosGray = Color(0xFF8E8E93)
internal val IosGray2 = Color(0xFFAEAEB2)
internal val IosGray3 = Color(0xFFC7C7CC)
internal val IosGray4 = Color(0xFFD1D1D6)
internal val IosGray5 = Color(0xFFE5E5EA)
internal val IosGray6 = Color(0xFFF2F2F7)

private val IosGroupedDark = Color(0xFF000000)
private val IosCardDark = Color(0xFF1C1C1E)
private val IosSecondaryDark = Color(0x99EBEBF5)
private val IosSeparatorDark = Color(0x33545458)
private val IosPressDark = Color(0x14FFFFFF)
private val IosPressLight = Color(0x14000000)

/** 当前是否暗色（基于主题背景亮度推断，兼容动态取色） */
@Composable
internal fun isIosDark(): Boolean = MaterialTheme.colorScheme.background.luminance() < 0.5f

/** iOS 分组背景：亮色 F2F2F7 / 暗色纯黑 */
@Composable
internal fun iosBackgroundColor(): Color = if (isIosDark()) IosGroupedDark else IosGray6

/** iOS 卡片背景：纯白 / 暗色 1C1C1E */
@Composable
internal fun iosCardColor(): Color = if (isIosDark()) IosCardDark else Color.White

/** iOS 主文字色 */
@Composable
internal fun iosLabelColor(): Color = if (isIosDark()) Color.White else Color.Black

/** iOS 次级文字色 */
@Composable
internal fun iosSecondaryLabelColor(): Color = if (isIosDark()) IosSecondaryDark else Color(0x993C3C43)

/** iOS 分割线颜色 */
@Composable
internal fun iosSeparatorColor(): Color = if (isIosDark()) IosSeparatorDark else Color(0x1A3C3C43)

/** iOS 点击高亮色 */
@Composable
internal fun iosPressColor(): Color = if (isIosDark()) IosPressDark else IosPressLight

/** 最流畅的原生安卓弹簧：低阻尼 + 适中刚度，模拟原生 Spring 的手感 */

/** 弹簧版尺寸/位置动效 */
internal val NativeSpring = spring<Float>(
    dampingRatio = Spring.DampingRatioMediumBouncy,
    stiffness = Spring.StiffnessMedium
)

/** 弹簧版颜色/透明度动效（更柔和，不弹跳） */
internal val NativeSpringSoft = spring<Float>(
    dampingRatio = Spring.DampingRatioNoBouncy,
    stiffness = Spring.StiffnessMediumLow
)

/** Int 版弹簧（用于 expandVertically/shrinkVertically 高度过渡） */
internal val NativeSpringInt = spring<Int>(
    dampingRatio = Spring.DampingRatioMediumBouncy,
    stiffness = Spring.StiffnessMedium
)

/** Int 版柔缓弹簧（高度过渡的柔和版） */
internal val NativeSpringIntSoft = spring<Int>(
    dampingRatio = Spring.DampingRatioNoBouncy,
    stiffness = Spring.StiffnessMediumLow
)

/** 位移版弹簧（slideInHorizontally / slideInVertically 等 IntOffset 过渡） */
internal val NativeSpringIntOffset = spring<IntOffset>(
    dampingRatio = Spring.DampingRatioMediumBouncy,
    stiffness = Spring.StiffnessMedium
)

/** 位移版柔缓弹簧（IntOffset 过渡柔和版） */
internal val NativeSpringIntOffsetSoft = spring<IntOffset>(
    dampingRatio = Spring.DampingRatioNoBouncy,
    stiffness = Spring.StiffnessMediumLow
)

/** 尺寸版弹簧（expandVertically / shrinkVertically 等 IntSize 过渡） */
internal val NativeSpringIntSize = spring<IntSize>(
    dampingRatio = Spring.DampingRatioMediumBouncy,
    stiffness = Spring.StiffnessMedium
)

/** 尺寸版柔缓弹簧（IntSize 过渡柔和版） */
internal val NativeSpringIntSizeSoft = spring<IntSize>(
    dampingRatio = Spring.DampingRatioNoBouncy,
    stiffness = Spring.StiffnessMediumLow
)

/** 颜色版弹簧（animateColorAsState 等 Color 过渡） */
internal val NativeSpringColor = spring<Color>(
    dampingRatio = Spring.DampingRatioMediumBouncy,
    stiffness = Spring.StiffnessMedium
)

/** 颜色版柔缓弹簧（Color 过渡柔和版） */
internal val NativeSpringColorSoft = spring<Color>(
    dampingRatio = Spring.DampingRatioNoBouncy,
    stiffness = Spring.StiffnessMediumLow
)

/** 尺寸(Dp)版弹簧（animateDpAsState 等 Dp 过渡） */
internal val NativeSpringDp = spring<Dp>(
    dampingRatio = Spring.DampingRatioMediumBouncy,
    stiffness = Spring.StiffnessMedium
)

/** 尺寸(Dp)版柔缓弹簧（Dp 过渡柔和版） */
internal val NativeSpringDpSoft = spring<Dp>(
    dampingRatio = Spring.DampingRatioNoBouncy,
    stiffness = Spring.StiffnessMediumLow
)

/* ========== 分组页背景 ========== */

/** 整页 iOS 分组背景容器（覆盖到顶栏下沿，天然衔接顶栏底色） */
@Composable
internal fun IosScreenBackground(content: @Composable BoxScope.() -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(iosBackgroundColor()),
        content = content
    )
}

/* ========== 分组标题 ========== */

/** iOS 分组 Section 头：小号灰色文字，可带右上辅助说明 */
@Composable
internal fun IosSectionHeader(
    text: String,
    modifier: Modifier = Modifier,
    trailing: (@Composable RowScope.() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 18.dp, end = 18.dp, top = 14.dp, bottom = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text.uppercase(),
            fontSize = 13.sp,
            letterSpacing = 0.4.sp,
            fontWeight = FontWeight.Medium,
            color = iosSecondaryLabelColor(),
            modifier = Modifier.weight(1f)
        )
        trailing?.invoke(this)
    }
}

/* ========== 分组卡片 ========== */

/** iOS 白色分组卡片：12dp 圆角，无投影（平贴背景，靠颜色分层） */
@Composable
internal fun IosGroupCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(iosCardColor())
    ) {
        content()
    }
}

/* ========== 列表行 ========== */

/**
 * iOS 列表行：前置圆角图标块 + 标题/副标题 + 尾部（值/开关/箭头）。
 * 点击时整行淡灰高亮；行间 0.5dp 分割线（末行通过 showDivider=false 关闭）。
 * 行内所有过渡用原生弹簧，达到「跟手、顺滑」的质感。
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun IosListRow(
    title: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    iconBackground: Color = IosBlue.copy(alpha = 0.14f),
    iconTint: Color = IosBlue,
    subtitle: String? = null,
    value: String? = null,
    trailing: (@Composable RowScope.() -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    showDivider: Boolean = true,
    contentPadding: PaddingValues = PaddingValues(horizontal = 14.dp, vertical = 11.dp),
    titleFontSize: Int = 17,
    subtitleFontSize: Int = 13
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val bg by animateColorAsState(
        targetValue = if (pressed) iosPressColor() else Color.Transparent,
        animationSpec = NativeSpringColorSoft,
        label = "iosRowBg"
    )

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(bg)
                .then(
                    when {
                        onClick != null && onLongClick != null -> Modifier.combinedClickable(
                            interactionSource = interaction,
                            indication = null,
                            onClick = onClick,
                            onLongClick = onLongClick
                        )
                        onClick != null -> Modifier.clickable(
                            interactionSource = interaction,
                            indication = null,
                            onClick = onClick
                        )
                        onLongClick != null -> Modifier.combinedClickable(
                            interactionSource = interaction,
                            indication = null,
                            onClick = {},
                            onLongClick = onLongClick
                        )
                        else -> Modifier
                    }
                )
                .padding(contentPadding),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                IosIconTile(
                    icon = icon,
                    background = iconBackground,
                    tint = iconTint,
                    size = 30.dp
                )
                Spacer(modifier = Modifier.width(13.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = titleFontSize.sp,
                    fontWeight = FontWeight.Normal,
                    color = iosLabelColor(),
                    maxLines = 1
                )
                if (subtitle != null) {
                    Spacer(modifier = Modifier.height(1.5.dp))
                    Text(
                        text = subtitle,
                        fontSize = subtitleFontSize.sp,
                        color = iosSecondaryLabelColor(),
                        maxLines = 3
                    )
                }
            }
            value?.let {
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = it,
                    fontSize = 17.sp,
                    color = iosSecondaryLabelColor(),
                    maxLines = 1
                )
            }
            if (trailing != null) {
                Spacer(modifier = Modifier.width(4.dp))
                trailing()
            } else if (onClick != null) {
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Outlined.ChevronRight,
                    contentDescription = null,
                    tint = IosGray3,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
        if (showDivider) {
            HorizontalDivider(
                modifier = Modifier.padding(start = if (icon != null) 57.dp else 14.dp),
                thickness = 0.5.dp,
                color = iosSeparatorColor()
            )
        }
    }
}

/** iOS 圆角图标块（列表行前置图标底座） */
@Composable
internal fun IosIconTile(
    icon: ImageVector,
    background: Color,
    tint: Color,
    size: Dp = 30.dp,
    iconSize: Dp = 17.dp
) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(size / 4.1f))
            .background(background),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(iconSize)
        )
    }
}

/** iOS 圆形头像（账号 / 昵称首字） */
@Composable
internal fun IosAvatar(
    text: String,
    background: Color,
    contentColor: Color = Color.White,
    size: Dp = 52.dp
) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(background),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = (size.value * 0.36f).sp,
            fontWeight = FontWeight.SemiBold,
            color = contentColor,
            maxLines = 1
        )
    }
}

/* ========== iOS 开关 ========== */

/**
 * iOS 风格开关：51x31dp，绿色开启轨道 + 白色圆形滑块，
 * 滑块位移/尺寸用弹簧动画，按压时滑块轻微放大（iOS 原生手感）。
 */
@Composable
internal fun IosSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)? = null,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val trackColor by animateColorAsState(
        targetValue = when {
            !enabled -> if (isIosDark()) Color(0xFF39393D) else Color(0xFFE9E9EB)
            checked -> IosGreen
            else -> if (isIosDark()) Color(0xFF39393D) else Color(0xFFE9E9EB)
        },
        animationSpec = NativeSpringColorSoft,
        label = "iosSwitchTrack"
    )
    val thumbTravel by animateDpAsState(
        targetValue = if (checked) 19.dp else 0.dp,
        animationSpec = NativeSpringDp,
        label = "iosSwitchTravel"
    )
    val thumbSize by animateDpAsState(
        targetValue = if (pressed) 31.dp else 27.dp,
        animationSpec = NativeSpringDp,
        label = "iosSwitchThumb"
    )
    Box(
        modifier = modifier
            .size(width = 51.dp, height = 31.dp)
            .clip(CircleShape)
            .background(trackColor)
            .clickable(
                enabled = enabled && onCheckedChange != null,
                interactionSource = interaction,
                indication = null
            ) { onCheckedChange?.invoke(!checked) },
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .padding(horizontal = 2.dp)
                .offset(x = thumbTravel)
                .size(thumbSize)
                .shadow(elevation = 2.dp, shape = CircleShape, clip = false)
                .clip(CircleShape)
                .background(Color.White)
        )
    }
}

/* ========== iOS 复选框（多选） ========== */

/**
 * iOS 风格复选框：22dp 圆角方块，选中时系统蓝填充 + 白色对勾，
 * 按压微缩、勾选以弹簧过渡，跟手顺滑。
 */
@Composable
internal fun IosCheckbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.86f else 1f,
        animationSpec = NativeSpring,
        label = "iosCheckScale"
    )
    val bg by animateColorAsState(
        targetValue = if (checked) IosBlue else Color.Transparent,
        animationSpec = NativeSpringColorSoft,
        label = "iosCheckBg"
    )
    val borderColor by animateColorAsState(
        targetValue = if (checked) IosBlue else if (isIosDark()) Color(0xFF48484A) else Color(0xFFC7C7CC),
        animationSpec = NativeSpringColorSoft,
        label = "iosCheckBorder"
    )
    val checkAlpha by animateFloatAsState(
        targetValue = if (checked) 1f else 0f,
        animationSpec = NativeSpring,
        label = "iosCheckAlpha"
    )
    Box(
        modifier = modifier
            .size(22.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(6.dp))
            .background(bg)
            .border(if (checked) 0.dp else 1.5.dp, borderColor, RoundedCornerShape(6.dp))
            .clickable(
                interactionSource = interaction,
                indication = null
            ) { onCheckedChange(!checked) },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Filled.Check,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier
                .size(15.dp)
                .graphicsLayer { alpha = checkAlpha }
        )
    }
}

/** iOS 小胶囊按钮：系统蓝填充圆角胶囊，按压弹性微缩，用于「全选 / 去登录」等轻操作 */
@Composable
internal fun IosPillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = IosBlue,
    enabled: Boolean = true
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && enabled) 0.93f else 1f,
        animationSpec = NativeSpring,
        label = "iosPillScale"
    )
    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(CircleShape)
            .background(tint.copy(alpha = if (enabled) 1f else 0.4f))
            .clickable(
                enabled = enabled,
                interactionSource = interaction,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 14.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = Color.White,
            maxLines = 1
        )
    }
}

/* ========== iOS 图标按钮 ========== */

/** iOS 圆形图标按钮：无底色、按压弹性微缩，用于头部返回 / 更多 / 转存等轻量操作 */
@Composable
internal fun IosIconButton(
    icon: ImageVector,
    tint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 34.dp,
    iconSize: Dp = 20.dp,
    contentDescription: String? = null,
    enabled: Boolean = true
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && enabled) 0.85f else 1f,
        animationSpec = NativeSpring,
        label = "iosIconBtnScale"
    )
    Box(
        modifier = modifier
            .size(size)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(CircleShape)
            .clickable(
                enabled = enabled,
                interactionSource = interaction,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint.copy(alpha = if (enabled) 1f else 0.4f),
            modifier = Modifier.size(iconSize)
        )
    }
}

/* ========== iOS 主按钮（全宽填充） ========== */

/** iOS 主按钮：全宽 50dp 系统蓝/绿填充圆角，按压弹性微缩，可带图标与加载态，用于「保存」「继续」等主操作 */
@Composable
internal fun IosPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    tint: Color = IosBlue,
    enabled: Boolean = true,
    loading: Boolean = false
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && enabled) 0.97f else 1f,
        animationSpec = NativeSpring,
        label = "iosPrimaryScale"
    )
    val bg by animateColorAsState(
        targetValue = tint.copy(alpha = if (enabled) 1f else 0.4f),
        animationSpec = NativeSpringColorSoft,
        label = "iosPrimaryBg"
    )
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .clickable(
                enabled = enabled,
                interactionSource = interaction,
                indication = null,
                onClick = onClick
            ),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (loading) {
            androidx.compose.material3.CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                strokeWidth = 2.dp,
                color = Color.White
            )
        } else {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = text,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                maxLines = 1
            )
        }
    }
}

/* ========== iOS 文本框 ========== */

/**
 * iOS 风格文本框：浅灰圆角输入框 + 系统蓝光标 + 占位文字，
 * 用于提取码 / 重命名等轻量输入。
 */
@Composable
internal fun IosTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    singleLine: Boolean = true,
    keyboardOptions: androidx.compose.foundation.text.KeyboardOptions = androidx.compose.foundation.text.KeyboardOptions.Default
) {
    val bg = if (isIosDark()) Color(0xFF1C1C1E) else Color(0xFFF2F2F7)
    val borderColor = if (isIosDark()) Color(0xFF3A3A3C) else Color(0xFFE5E5EA)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(bg)
            .border(1.dp, borderColor, RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 9.dp)
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = singleLine,
            keyboardOptions = keyboardOptions,
            textStyle = TextStyle(fontSize = 16.sp, color = iosLabelColor()),
            cursorBrush = SolidColor(IosBlue),
            decorationBox = { innerTextField ->
                Box {
                    if (value.isEmpty() && placeholder != null) {
                        Text(
                            text = placeholder,
                            fontSize = 16.sp,
                            color = iosSecondaryLabelColor()
                        )
                    }
                    innerTextField()
                }
            },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/**
 * iOS 风格搜索框：浅灰圆角输入框 + 放大镜图标 + 一键清除（有内容时显示）。
 * 供下载页任务搜索、云盘页文件搜索复用。
 */
@Composable
internal fun IosSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "搜索",
    onClear: (() -> Unit)? = null
) {
    val bg = if (isIosDark()) Color(0xFF1C1C1E) else Color(0xFFF2F2F7)
    val borderColor = if (isIosDark()) Color(0xFF3A3A3C) else Color(0xFFE5E5EA)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(bg)
            .border(1.dp, borderColor, RoundedCornerShape(10.dp))
            .padding(start = 10.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Outlined.Search,
            contentDescription = null,
            tint = iosSecondaryLabelColor(),
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = TextStyle(fontSize = 15.sp, color = iosLabelColor()),
            cursorBrush = SolidColor(IosBlue),
            decorationBox = { innerTextField ->
                Box(modifier = Modifier.weight(1f)) {
                    if (value.isEmpty() && placeholder.isNotEmpty()) {
                        Text(
                            text = placeholder,
                            fontSize = 15.sp,
                            color = iosSecondaryLabelColor()
                        )
                    }
                    innerTextField()
                }
            },
            modifier = Modifier.weight(1f)
        )
        if (value.isNotEmpty()) {
            IosIconButton(
                icon = Icons.Outlined.Close,
                tint = iosSecondaryLabelColor(),
                onClick = { onClear?.invoke() ?: onValueChange("") },
                size = 30.dp,
                iconSize = 18.dp,
                contentDescription = "清除"
            )
        }
    }
}

/**
 * iOS 风格密码输入框：浅灰圆角输入框 + 系统蓝光标 + 占位文字 + 可见性切换，
 * 用于账号密码类登录页。
 */
@Composable
internal fun IosPasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default
) {
    var visible by remember { mutableStateOf(false) }
    val bg = if (isIosDark()) Color(0xFF1C1C1E) else Color(0xFFF2F2F7)
    val borderColor = if (isIosDark()) Color(0xFF3A3A3C) else Color(0xFFE5E5EA)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(bg)
            .border(1.dp, borderColor, RoundedCornerShape(10.dp))
            .padding(start = 12.dp, end = 4.dp, top = 9.dp, bottom = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            keyboardOptions = keyboardOptions.copy(
                keyboardType = if (visible) KeyboardType.Text else KeyboardType.Password
            ),
            visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
            textStyle = TextStyle(fontSize = 16.sp, color = iosLabelColor()),
            cursorBrush = SolidColor(IosBlue),
            decorationBox = { innerTextField ->
                Box(modifier = Modifier.weight(1f)) {
                    if (value.isEmpty() && placeholder != null) {
                        Text(
                            text = placeholder,
                            fontSize = 16.sp,
                            color = iosSecondaryLabelColor()
                        )
                    }
                    innerTextField()
                }
            },
            modifier = Modifier.weight(1f)
        )
        IosIconButton(
            icon = if (visible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
            tint = iosSecondaryLabelColor(),
            onClick = { visible = !visible },
            size = 32.dp,
            iconSize = 20.dp,
            contentDescription = if (visible) "隐藏密码" else "显示密码"
        )
    }
}

/**
 * iOS 风格多行文本框：浅灰圆角输入框 + 系统蓝光标 + 占位文字，
 * 用于分享链接 / 备注等多行输入。
 */
@Composable
internal fun IosMultilineTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    minLines: Int = 2,
    maxLines: Int = 4
) {
    val bg = if (isIosDark()) Color(0xFF1C1C1E) else Color(0xFFF2F2F7)
    val borderColor = if (isIosDark()) Color(0xFF3A3A3C) else Color(0xFFE5E5EA)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(bg)
            .border(1.dp, borderColor, RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 9.dp)
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = TextStyle(fontSize = 16.sp, lineHeight = 22.sp, color = iosLabelColor()),
            cursorBrush = SolidColor(IosBlue),
            minLines = minLines,
            maxLines = maxLines,
            decorationBox = { innerTextField ->
                Box {
                    if (value.isEmpty() && placeholder != null) {
                        Text(
                            text = placeholder,
                            fontSize = 16.sp,
                            color = iosSecondaryLabelColor()
                        )
                    }
                    innerTextField()
                }
            },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/**
 * iOS 进度条：3dp 胶囊轨道 + 圆头进度，进度变化用弹簧过渡，
 * 支持成功（绿）与失败（红）着色。
 */
@Composable
internal fun IosProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    color: Color = IosBlue,
    trackColor: Color? = null,
    height: Dp = 3.dp
) {
    val animated by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = NativeSpringSoft,
        label = "iosProgress"
    )
    val track = trackColor ?: if (isIosDark()) Color(0xFF39393D) else Color(0xFFE9E9EB)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(CircleShape)
            .background(track)
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(if (animated <= 0f) 0f else animated.coerceIn(0.03f, 1f))
                .clip(CircleShape)
                .background(color)
        )
    }
}

/* ========== iOS 分段控件 ========== */

/**
 * iOS 分段控件：胶囊底槽 + 白色滑片 + 弹跳吸附动画，
 * 选中项高亮主题色（默认系统蓝）。
 */
@Composable
internal fun IosSegmentedControl(
    options: List<String>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = IosBlue
) {
    val bg = if (isIosDark()) Color(0xFF2C2C2E) else Color(0xFFE9E9EB)
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(9.dp))
            .background(bg)
            .padding(2.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        options.forEachIndexed { index, label ->
            val selected = index == selectedIndex
            val selectedBg by animateColorAsState(
                targetValue = if (selected) Color.White else Color.Transparent,
                animationSpec = NativeSpringColorSoft,
                label = "iosSegBg"
            )
            val textColor by animateColorAsState(
                targetValue = if (selected) iosLabelColor() else iosSecondaryLabelColor(),
                animationSpec = NativeSpringColorSoft,
                label = "iosSegText"
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(7.dp))
                    .background(selectedBg)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onSelected(index) }
                    .padding(vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    fontSize = 13.sp,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                    color = textColor,
                    maxLines = 1
                )
            }
        }
    }
}

/* ========== iOS 弹窗（Alert） ========== */

internal enum class IosButtonStyle { Default, Destructive }

/**
 * iOS 风格弹窗：居中圆角卡片，标题居中加粗，正文居中灰色，
 * 底部按钮横排（左取消 / 右确认），确认可为破坏性红色。
 */
@Composable
internal fun IosAlertDialog(
    onDismissRequest: () -> Unit,
    confirmText: String,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null,
    message: String? = null,
    confirmStyle: IosButtonStyle = IosButtonStyle.Default,
    confirmEnabled: Boolean = true,
    dismissText: String? = "取消",
    onDismiss: (() -> Unit)? = null,
    content: (@Composable ColumnScope.() -> Unit)? = null
) {
    Dialog(onDismissRequest = onDismissRequest) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 40.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(iosCardColor())
                .padding(horizontal = 16.dp, vertical = 20.dp)
        ) {
            title?.let {
                Text(
                    text = it,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = iosLabelColor(),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                if (message != null || content != null) Spacer(modifier = Modifier.height(8.dp))
            }
            message?.let {
                Text(
                    text = it,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    color = iosSecondaryLabelColor(),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                if (content != null) Spacer(modifier = Modifier.height(12.dp))
            }
            content?.invoke(this)
            val showButtonRow = dismissText != null || confirmText.isNotBlank()
            if (showButtonRow && (title != null || message != null || content != null)) {
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(thickness = 0.5.dp, color = iosSeparatorColor())
                Spacer(modifier = Modifier.height(4.dp))
            }
            if (showButtonRow) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    if (dismissText != null && onDismiss != null) {
                        IosDialogButton(
                            text = dismissText,
                            style = IosButtonStyle.Default,
                            bold = false,
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f)
                        )
                        Box(
                            modifier = Modifier
                                .width(0.5.dp)
                                .height(40.dp)
                                .background(iosSeparatorColor())
                        )
                    }
                    if (confirmText.isNotBlank()) {
                        IosDialogButton(
                            text = confirmText,
                            style = confirmStyle,
                            bold = true,
                            enabled = confirmEnabled,
                            onClick = onConfirm,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun IosDialogButton(
    text: String,
    style: IosButtonStyle,
    bold: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val bg by animateColorAsState(
        targetValue = if (pressed && enabled) iosPressColor() else Color.Transparent,
        animationSpec = NativeSpringColorSoft,
        label = "iosDialogBtnBg"
    )
    Box(
        modifier = modifier
            .height(40.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .clickable(
                enabled = enabled,
                interactionSource = interaction,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 16.sp,
            fontWeight = if (bold) FontWeight.SemiBold else FontWeight.Normal,
            color = when (style) {
                IosButtonStyle.Default -> IosBlue
                IosButtonStyle.Destructive -> IosRed
            }.copy(alpha = if (enabled) 1f else 0.4f)
        )
    }
}

/* ========== iOS 底部弹窗（Sheet） ========== */

/**
 * iOS 风格底部弹窗容器：白色圆角面板 + 顶部抓取条，
 * 内容区可放「标题 + 操作列表」或自定义内容。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun IosBottomSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = iosCardColor(),
        dragHandle = {
            // iOS 抓取条：细长灰条
            Box(
                modifier = Modifier
                    .padding(top = 10.dp, bottom = 4.dp)
                    .size(width = 36.dp, height = 5.dp)
                    .clip(CircleShape)
                    .background(if (isIosDark()) Color(0xFF3A3A3C) else Color(0xFFD1D1D6))
            )
        }
    ) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 32.dp)
        ) {
            content()
        }
    }
}

/** iOS 弹窗内大标题（居中 / 左对齐均可） */
@Composable
internal fun IosSheetTitle(
    text: String,
    modifier: Modifier = Modifier,
    center: Boolean = true,
    subtitle: String? = null
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = text,
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold,
            color = iosLabelColor(),
            textAlign = if (center) TextAlign.Center else TextAlign.Start,
            modifier = Modifier.fillMaxWidth()
        )
        subtitle?.let {
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = it,
                fontSize = 13.sp,
                color = iosSecondaryLabelColor(),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/** iOS 底部弹窗操作项：圆角图标块 + 标题 + 副标题，按压高亮 */
@Composable
internal fun IosActionRow(
    icon: ImageVector,
    iconBackground: Color,
    iconTint: Color,
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    showDivider: Boolean = false
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val bg by animateColorAsState(
        targetValue = if (pressed) iosPressColor() else Color.Transparent,
        animationSpec = NativeSpringColorSoft,
        label = "iosActionRowBg"
    )
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(bg)
                .clickable(
                    interactionSource = interaction,
                    indication = null,
                    onClick = onClick
                )
                .padding(vertical = 8.dp, horizontal = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IosIconTile(icon = icon, background = iconBackground, tint = iconTint, size = 36.dp, iconSize = 19.dp)
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    color = iosLabelColor(),
                    maxLines = 1
                )
                subtitle?.let {
                    Spacer(modifier = Modifier.height(1.dp))
                    Text(
                        text = it,
                        fontSize = 12.sp,
                        color = iosSecondaryLabelColor(),
                        maxLines = 2
                    )
                }
            }
        }
        if (showDivider) {
            HorizontalDivider(
                modifier = Modifier.padding(start = 52.dp),
                thickness = 0.5.dp,
                color = iosSeparatorColor()
            )
        }
    }
}

/** iOS 底部弹窗整块按钮（如「退出登录」「删除」），居中红色/蓝色文字 */
@Composable
internal fun IosBlockButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = IosBlue,
    enabled: Boolean = true,
    loading: Boolean = false
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val bg by animateColorAsState(
        targetValue = if (pressed && enabled) iosPressColor() else Color.Transparent,
        animationSpec = NativeSpringColorSoft,
        label = "iosBlockBg"
    )
    val scale by animateFloatAsState(
        targetValue = if (pressed && enabled) 0.975f else 1f,
        animationSpec = NativeSpring,
        label = "iosBlockScale"
    )
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(10.dp))
            .background(bg)
            .clickable(
                enabled = enabled,
                interactionSource = interaction,
                indication = null,
                onClick = onClick
            ),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (loading) {
            androidx.compose.material3.CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                strokeWidth = 2.dp,
                color = tint
            )
        } else {
            Text(
                text = text,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = tint.copy(alpha = if (enabled) 1f else 0.4f)
            )
        }
    }
}

/* ========== 辅助：进度百分比文本 ========== */

/** 文件大小格式化（B/KB/MB/GB/TB） */
internal fun iosFormatBytes(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    var value = bytes.toDouble()
    var i = 0
    while (value >= 1024 && i < units.size - 1) {
        value /= 1024
        i++
    }
    return String.format("%.1f %s", value, units[i])
}

/** 速度格式化（B/s/KB/s/MB/s/GB/s） */
internal fun iosFormatSpeed(bytesPerSec: Long): String {
    if (bytesPerSec <= 0) return "0 B/s"
    val units = arrayOf("B/s", "KB/s", "MB/s", "GB/s")
    var value = bytesPerSec.toDouble()
    var i = 0
    while (value >= 1024 && i < units.size - 1) {
        value /= 1024
        i++
    }
    return String.format("%.1f %s", value, units[i])
}
