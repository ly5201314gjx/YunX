package com.yunx.cloud.ui.screens

import android.content.ComponentName
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.yunx.cloud.R
import com.yunx.cloud.data.prefs.SettingsRepository
import com.yunx.cloud.ui.components.IosBlue
import com.yunx.cloud.ui.components.IosGroupCard
import com.yunx.cloud.ui.components.IosIconButton
import com.yunx.cloud.ui.components.IosSectionHeader
import com.yunx.cloud.ui.components.IosSegmentedControl
import com.yunx.cloud.ui.components.IosSwitch
import com.yunx.cloud.ui.components.NativeSpring
import com.yunx.cloud.ui.components.NativeSpringColor
import com.yunx.cloud.ui.components.NativeSpringIntSizeSoft
import com.yunx.cloud.ui.components.NativeSpringSoft
import com.yunx.cloud.ui.components.iosBackgroundColor
import com.yunx.cloud.ui.components.iosCardColor
import com.yunx.cloud.ui.components.iosLabelColor
import com.yunx.cloud.ui.components.iosPressColor
import com.yunx.cloud.ui.components.iosSecondaryLabelColor
import com.yunx.cloud.ui.components.isIosDark
import com.yunx.cloud.ui.theme.ThemeController

/** 预置主题色（Material 风格种子色） */
private val presetColors = listOf(
    "蓝色" to 0xFF415F91L,
    "靛蓝" to 0xFF3F51B5L,
    "紫色" to 0xFF6750A4L,
    "玫红" to 0xFFC2185BL,
    "红色" to 0xFFB3261EL,
    "橙色" to 0xFFF4631CL,
    "金黄" to 0xFFF9A825L,
    "绿色" to 0xFF38761DL,
    "青色" to 0xFF00897BL,
    "天蓝" to 0xFF0288D1L,
)

/**
 * 主题与外观设置页（iOS 风格）：
 * - 外观模式：分段控件单选（跟随系统 / 浅色 / 深色）
 * - 主题色：可折叠分组卡片，动态色彩开关（Android12+）+ LazyRow 色圆选择 + 自定义调色盘
 * - 桌面图标：可折叠分组卡片，经典 / 新图标切换
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    BackHandler { onBack() }
    var showColorPicker by remember { mutableStateOf(false) }
    // 主题色卡片默认展开
    var expanded by rememberSaveable { mutableStateOf(true) }

    // 单一动画源驱动折叠（Animatable 支持打断：快速连续点击时自动平滑过渡到新目标）：
    // 高度 = contentHeightPx * progress，透明度 = progress，二者同步
    val density = LocalDensity.current
    var contentHeightPx by remember { mutableIntStateOf(0) }
    val expandProgress = remember { Animatable(if (expanded) 1f else 0f) }
    androidx.compose.runtime.LaunchedEffect(expanded) {
        expandProgress.animateTo(
            targetValue = if (expanded) 1f else 0f,
            animationSpec = NativeSpringSoft
        )
    }

    // Android12- 动态色不可用，视为默认蓝色
    val effectiveColorMode = if (ThemeController.colorMode == 0 && Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
        1
    } else {
        ThemeController.colorMode
    }

    // ---------- 桌面图标动态切换 ----------
    val settingsRepo = remember { SettingsRepository(context) }
    var appIconVariant by remember { mutableStateOf(settingsRepo.appIconVariant) }
    var iconExpanded by rememberSaveable { mutableStateOf(true) }
    val switchAppIcon: (Int) -> Unit = { variant ->
        val pm = context.packageManager
        val main = ComponentName(context, "com.yunx.cloud.MainActivity")
        val alias = ComponentName(context, "com.yunx.cloud.MainActivityIcon2")
        if (variant == 1) {
            pm.setComponentEnabledSetting(alias, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP)
            pm.setComponentEnabledSetting(main, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP)
        } else {
            pm.setComponentEnabledSetting(main, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP)
            pm.setComponentEnabledSetting(alias, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP)
        }
        settingsRepo.appIconVariant = variant
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = iosBackgroundColor(),
        topBar = {
            TopAppBar(
                title = { Text("主题与外观", style = androidx.compose.material3.MaterialTheme.typography.titleLarge, color = iosLabelColor()) },
                navigationIcon = {
                    IosIconButton(
                        icon = Icons.AutoMirrored.Filled.ArrowBack,
                        tint = IosBlue,
                        onClick = onBack,
                        contentDescription = "返回"
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = iosBackgroundColor()
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
        ) {
            // ---------- 外观模式 ----------
            IosSectionHeader("外观模式")
            IosGroupCard(modifier = Modifier.padding(horizontal = 16.dp)) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "选择应用的明暗外观",
                        fontSize = 13.sp,
                        color = iosSecondaryLabelColor()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    IosSegmentedControl(
                        options = listOf("跟随系统", "浅色", "深色"),
                        selectedIndex = ThemeController.darkMode,
                        onSelected = { ThemeController.setDarkMode(context, it) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ---------- 主题色（可折叠卡片） ----------
            IosGroupCard(modifier = Modifier.padding(horizontal = 16.dp)) {
                Column {
                    // Header：点击展开/收起
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { expanded = !expanded }
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Palette,
                            contentDescription = null,
                            tint = IosBlue
                        )
                        Spacer(modifier = Modifier.width(13.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "主题色",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = iosLabelColor()
                            )
                            AnimatedVisibility(
                                visible = !expanded,
                                enter = fadeIn(NativeSpringSoft) + expandVertically(NativeSpringIntSizeSoft, expandFrom = Alignment.Top),
                                exit = fadeOut(NativeSpringSoft) + shrinkVertically(NativeSpringIntSizeSoft, shrinkTowards = Alignment.Top)
                            ) {
                                Text(
                                    text = when {
                                        effectiveColorMode == 0 -> "动态色彩（跟随壁纸）"
                                        effectiveColorMode == 2 -> "自定义颜色"
                                        else -> "默认蓝色"
                                    },
                                    fontSize = 13.sp,
                                    color = iosSecondaryLabelColor(),
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                        }
                        val rotation by animateFloatAsState(
                            targetValue = if (expanded) 180f else 0f,
                            label = "arrow",
                            animationSpec = NativeSpringSoft
                        )
                        Icon(
                            imageVector = Icons.Filled.ExpandMore,
                            contentDescription = null,
                            tint = iosSecondaryLabelColor(),
                            modifier = Modifier.rotate(rotation)
                        )
                    }

                    // 展开内容：高度 + 透明度由 Animatable 同步驱动（可打断、不裁剪、无跳变）
                    val animatedHeightDp = with(density) { (contentHeightPx * expandProgress.value).toDp() }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(if (contentHeightPx > 0) Modifier.height(animatedHeightDp) else Modifier)
                            .clipToBounds()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .wrapContentHeight(unbounded = true)
                                .onSizeChanged { contentHeightPx = it.height }
                                .graphicsLayer { alpha = expandProgress.value }
                        ) {
                            Column(modifier = Modifier.padding(start = 14.dp, end = 14.dp, bottom = 14.dp)) {
                                androidx.compose.material3.HorizontalDivider(
                                    thickness = 0.5.dp,
                                    color = androidx.compose.ui.graphics.Color.Transparent
                                )
                                Spacer(modifier = Modifier.height(12.dp))

                                // 动态色彩开关（仅 Android 12+）
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("动态色彩", fontSize = 17.sp, color = iosLabelColor())
                                            Text(
                                                "从系统壁纸自动取色",
                                                fontSize = 13.sp,
                                                color = iosSecondaryLabelColor()
                                            )
                                        }
                                        IosSwitch(
                                            checked = ThemeController.colorMode == 0,
                                            onCheckedChange = { on ->
                                                ThemeController.setColorMode(context, if (on) 0 else 1)
                                            }
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(12.dp))
                                }

                                // 主题色选择（动态开启时隐藏）
                                AnimatedVisibility(visible = effectiveColorMode != 0) {
                                    Column {
                                        Text(
                                            text = "主题颜色",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = iosSecondaryLabelColor()
                                        )
                                        LazyRow(
                                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                                            contentPadding = PaddingValues(top = 12.dp, bottom = 8.dp)
                                        ) {
                                            itemsIndexed(presetColors) { _, (name, color) ->
                                                // 默认蓝色模式只高亮蓝色；自定义模式高亮匹配种子色的那个
                                                val isSelected = (effectiveColorMode == 1 && color == 0xFF415F91L) ||
                                                    (effectiveColorMode == 2 && ThemeController.seedColor == color)
                                                ColorSelectionItem(
                                                    color = color,
                                                    name = name,
                                                    isSelected = isSelected,
                                                    onClick = { ThemeController.setSeedColor(context, color) }
                                                )
                                            }
                                            item {
                                                val isCustomSelected = effectiveColorMode == 2 &&
                                                    ThemeController.seedColor !in presetColors.map { it.second }
                                                CustomColorButton(
                                                    isSelected = isCustomSelected,
                                                    customColor = ThemeController.seedColor,
                                                    onClick = {
                                                        ThemeController.setColorMode(context, 2)
                                                        showColorPicker = true
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

            Spacer(modifier = Modifier.height(12.dp))

            // ---------- 桌面图标（可折叠卡片） ----------
            IosSectionHeader("桌面图标")
            IosGroupCard(modifier = Modifier.padding(horizontal = 16.dp)) {
                Column {
                    // Header：点击展开/收起
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { iconExpanded = !iconExpanded }
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.StarOutline,
                            contentDescription = null,
                            tint = IosBlue
                        )
                        Spacer(modifier = Modifier.width(13.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "桌面图标",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = iosLabelColor()
                            )
                            // 与主题色卡片一致的副标题动画：展开时隐藏、收起时显示
                            AnimatedVisibility(
                                visible = !iconExpanded,
                                enter = fadeIn(NativeSpringSoft) + expandVertically(NativeSpringIntSizeSoft, expandFrom = Alignment.Top),
                                exit = fadeOut(NativeSpringSoft) + shrinkVertically(NativeSpringIntSizeSoft, shrinkTowards = Alignment.Top)
                            ) {
                                Text(
                                    text = if (appIconVariant == 1) "新图标" else "经典图标",
                                    fontSize = 13.sp,
                                    color = iosSecondaryLabelColor(),
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                        }
                        val iconRotation by animateFloatAsState(
                            targetValue = if (iconExpanded) 180f else 0f,
                            label = "iconArrow",
                            animationSpec = NativeSpringSoft
                        )
                        Icon(
                            imageVector = Icons.Filled.ExpandMore,
                            contentDescription = null,
                            tint = iosSecondaryLabelColor(),
                            modifier = Modifier.rotate(iconRotation)
                        )
                    }
                    AnimatedVisibility(
                        visible = iconExpanded,
                        enter = fadeIn(NativeSpringSoft) + expandVertically(NativeSpringIntSizeSoft, expandFrom = Alignment.Top),
                        exit = fadeOut(NativeSpringSoft) + shrinkVertically(NativeSpringIntSizeSoft, shrinkTowards = Alignment.Top)
                    ) {
                        Column(modifier = Modifier.padding(start = 14.dp, end = 14.dp, bottom = 14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(28.dp)
                            ) {
                                AppIconOption(
                                    iconRes = R.drawable.icon,
                                    name = "经典图标",
                                    isSelected = appIconVariant == 0,
                                    onClick = {
                                        appIconVariant = 0
                                        switchAppIcon(0)
                                    }
                                )
                                AppIconOption(
                                    iconRes = R.drawable.icon2,
                                    name = "新图标",
                                    isSelected = appIconVariant == 1,
                                    onClick = {
                                        appIconVariant = 1
                                        switchAppIcon(1)
                                    }
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Android 12+ 立即生效；部分设备需回到桌面或重启启动器后查看。",
                                fontSize = 13.sp,
                                lineHeight = 18.sp,
                                color = iosSecondaryLabelColor()
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }

    // 自定义调色盘
    if (showColorPicker) {
        ColorPickerDialog(
            initialColor = ThemeController.seedColor,
            onDismiss = { showColorPicker = false },
            onColorSelected = { color ->
                ThemeController.setSeedColor(context, color)
                showColorPicker = false
            }
        )
    }
}

/** 预置色圆点（色圆 + 名称，选中显示对勾） */
@Composable
private fun ColorSelectionItem(
    color: Long,
    name: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(Color(color.toInt()))
                .border(
                    width = if (isSelected) 3.dp else 1.dp,
                    color = if (isSelected) IosBlue else iosSecondaryLabelColor().copy(alpha = 0.4f),
                    shape = CircleShape
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClick
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isSelected) {
                val scale by animateFloatAsState(
                    targetValue = if (isSelected) 1f else 0f,
                    label = "checkScale",
                    animationSpec = NativeSpring
                )
                Icon(
                    imageVector = Icons.Outlined.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier
                        .size(22.dp)
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                        }
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = name,
            fontSize = 12.sp,
            color = if (isSelected) IosBlue else iosSecondaryLabelColor(),
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

/** 自定义颜色按钮（色圆 + 名称，圆内显示 Palette 图标） */
@Composable
private fun CustomColorButton(
    isSelected: Boolean,
    customColor: Long,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(
                    if (isSelected) Color(customColor.toInt())
                    else if (isIosDark()) Color(0xFF2C2C2E) else Color(0xFFE9E9EB)
                )
                .border(
                    width = if (isSelected) 3.dp else 1.dp,
                    color = if (isSelected) IosBlue else iosSecondaryLabelColor().copy(alpha = 0.4f),
                    shape = CircleShape
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClick
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.Palette,
                contentDescription = null,
                tint = if (isSelected) Color.White else iosSecondaryLabelColor(),
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "自定义",
            fontSize = 12.sp,
            color = if (isSelected) IosBlue else iosSecondaryLabelColor(),
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

// ================= 调色盘 Dialog（iOS 风格） =================

@Composable
private fun ColorPickerDialog(
    initialColor: Long,
    onDismiss: () -> Unit,
    onColorSelected: (Long) -> Unit
) {
    val initialHsv = colorToHsv(Color(initialColor.toInt()))
    var hue by remember { mutableFloatStateOf(initialHsv[0]) }
    var saturation by remember { mutableFloatStateOf(initialHsv[1]) }
    var value by remember { mutableFloatStateOf(initialHsv[2]) }
    val currentColor = Color.hsv(hue, saturation, value)
    var hexInput by remember(currentColor) {
        mutableStateOf(colorToHex(currentColor).removePrefix("#"))
    }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 40.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(iosCardColor())
                .padding(horizontal = 18.dp, vertical = 20.dp)
        ) {
            Text(
                text = "自定义颜色",
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                color = iosLabelColor(),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))

            // 顶部：HEX 输入 + 预览
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "#",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = iosSecondaryLabelColor()
                    )
                    BasicTextField(
                        value = hexInput,
                        onValueChange = { input ->
                            val filtered = input.filter { it.isLetterOrDigit() }.take(6).uppercase()
                            hexInput = filtered
                            if (filtered.length == 6) {
                                runCatching {
                                    val color = hexToColor(filtered)
                                    val hsv = colorToHsv(color)
                                    hue = hsv[0]
                                    saturation = hsv[1]
                                    value = hsv[2]
                                }
                            }
                        },
                        textStyle = androidx.compose.ui.text.TextStyle(
                            color = iosLabelColor(),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        ),
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Characters,
                            keyboardType = KeyboardType.Ascii
                        ),
                        cursorBrush = SolidColor(IosBlue),
                        modifier = Modifier.width(140.dp)
                    )
                }
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isIosDark()) Color(0xFF2C2C2E) else Color(0xFFF2F2F7))
                        .border(1.dp, if (isIosDark()) Color(0xFF3A3A3C) else Color(0xFFE5E5EA), RoundedCornerShape(10.dp))
                ) {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(currentColor)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 颜色选择核心区：左侧 SatVal 方块 + 右侧色相条
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
            ) {
                SatValPanel(
                    hue = hue,
                    saturation = saturation,
                    value = value,
                    onValChange = { s, v ->
                        saturation = s
                        value = v
                    },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .clip(RoundedCornerShape(8.dp))
                        .border(1.dp, if (isIosDark()) Color(0xFF3A3A3C) else Color(0xFFE5E5EA), RoundedCornerShape(8.dp))
                )
                Spacer(modifier = Modifier.width(12.dp))
                VerticalHueSlider(
                    hue = hue,
                    onHueChange = { hue = it },
                    modifier = Modifier
                        .width(22.dp)
                        .fillMaxSize()
                        .clip(RoundedCornerShape(8.dp))
                        .border(1.dp, if (isIosDark()) Color(0xFF3A3A3C) else Color(0xFFE5E5EA), RoundedCornerShape(8.dp))
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 底部按钮：iOS 弹窗按钮横排
            androidx.compose.material3.HorizontalDivider(
                thickness = 0.5.dp,
                color = if (isIosDark()) Color(0x33545458) else Color(0x1A3C3C43)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                IosPickerButton(
                    text = "取消",
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                )
                Box(
                    modifier = Modifier
                        .width(0.5.dp)
                        .height(40.dp)
                        .background(if (isIosDark()) Color(0x33545458) else Color(0x1A3C3C43))
                )
                IosPickerButton(
                    text = "应用",
                    onClick = { onColorSelected(currentColor.toArgb().toLong() and 0xFFFFFFFFL) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

/** 调色盘底部按钮（iOS 弹窗样式：蓝色文字，按压高亮） */
@Composable
private fun IosPickerButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val bg by animateColorAsState(
        targetValue = if (pressed) iosPressColor() else Color.Transparent,
        animationSpec = NativeSpringColor,
        label = "pickerBtnBg"
    )
    Box(
        modifier = modifier
            .height(40.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = IosBlue
        )
    }
}

/** 饱和度/明度大方块（可拖拽/点击） */
@Composable
private fun SatValPanel(
    hue: Float,
    saturation: Float,
    value: Float,
    onValChange: (Float, Float) -> Unit,
    modifier: Modifier
) {
    Box(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .matchParentSize()
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        onValChange(
                            (offset.x / size.width).coerceIn(0f, 1f),
                            1f - (offset.y / size.height).coerceIn(0f, 1f)
                        )
                    }
                }
                .pointerInput(Unit) {
                    detectDragGestures { change, _ ->
                        change.consume()
                        onValChange(
                            (change.position.x / size.width).coerceIn(0f, 1f),
                            1f - (change.position.y / size.height).coerceIn(0f, 1f)
                        )
                    }
                }
        ) {
            drawRect(color = Color.hsv(hue, 1f, 1f))
            drawRect(brush = Brush.horizontalGradient(listOf(Color.White, Color.Transparent)))
            drawRect(brush = Brush.verticalGradient(listOf(Color.Transparent, Color.Black)))

            // 指示器（方形描边）
            val x = saturation * size.width
            val y = (1f - value) * size.height
            val cursorSize = 14f
            drawRect(
                color = Color.Black.copy(alpha = 0.5f),
                topLeft = Offset(x - cursorSize / 2, y - cursorSize / 2),
                size = Size(cursorSize, cursorSize),
                style = Stroke(3f)
            )
            drawRect(
                color = Color.White,
                topLeft = Offset(x - cursorSize / 2, y - cursorSize / 2),
                size = Size(cursorSize, cursorSize),
                style = Stroke(1.5f)
            )
        }
    }
}

/** 竖向色相条（可拖拽/点击） */
@Composable
private fun VerticalHueSlider(
    hue: Float,
    onHueChange: (Float) -> Unit,
    modifier: Modifier
) {
    Box(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .matchParentSize()
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        onHueChange((offset.y / size.height * 360f).coerceIn(0f, 360f))
                    }
                }
                .pointerInput(Unit) {
                    detectDragGestures { change, _ ->
                        change.consume()
                        onHueChange((change.position.y / size.height * 360f).coerceIn(0f, 360f))
                    }
                }
        ) {
            val colors = (0..360 step 10).map { Color.hsv(it.toFloat(), 1f, 1f) }
            drawRect(brush = Brush.verticalGradient(colors = colors))

            // 指示器（横条）
            val y = (hue / 360f) * size.height
            val barHeight = 6f
            drawRect(
                color = Color.Black.copy(alpha = 0.5f),
                topLeft = Offset(0f, y - barHeight / 2),
                size = Size(size.width, barHeight),
                style = Stroke(2f)
            )
            drawRect(
                color = Color.White,
                topLeft = Offset(0f, y - barHeight / 2),
                size = Size(size.width, barHeight),
                style = Stroke(1f)
            )
        }
    }
}

// ================= 辅助 =================

/** 桌面图标选项：图标预览 + 名称，选中高亮边框 + 角标对勾 */
@Composable
private fun AppIconOption(
    iconRes: Int,
    name: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(RoundedCornerShape(18.dp))
                .border(
                    width = if (isSelected) 3.dp else 1.dp,
                    color = if (isSelected) IosBlue else iosSecondaryLabelColor().copy(alpha = 0.35f),
                    shape = RoundedCornerShape(18.dp)
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClick
                ),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(iconRes),
                contentDescription = name,
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(14.dp))
            )
            if (isSelected) {
                // 右上角选中角标（对勾 + 主题色圆底）
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(20.dp)
                        .background(IosBlue, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Check,
                        contentDescription = "已选择",
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = name,
            fontSize = 12.sp,
            color = if (isSelected) IosBlue else iosSecondaryLabelColor(),
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

private fun colorToHsv(color: Color): FloatArray {
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(color.toArgb(), hsv)
    return hsv
}

private fun colorToHex(color: Color): String {
    val red = (color.red * 255).toInt()
    val green = (color.green * 255).toInt()
    val blue = (color.blue * 255).toInt()
    return "#%02X%02X%02X".format(red, green, blue)
}

private fun hexToColor(hex: String): Color {
    val clean = hex.removePrefix("#")
    return Color(
        red = clean.substring(0, 2).toInt(16) / 255f,
        green = clean.substring(2, 4).toInt(16) / 255f,
        blue = clean.substring(4, 6).toInt(16) / 255f
    )
}
