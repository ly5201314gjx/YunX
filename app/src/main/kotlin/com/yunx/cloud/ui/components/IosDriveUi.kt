package com.yunx.cloud.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.InsertDriveFile
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.SaveAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yunx.cloud.data.network.model.ShareFile
import kotlinx.coroutines.launch

/**
 * tab2 云盘浏览专用 iOS 组件：
 * 与 tab1 解析页共用的 Material 版 ShareFileRow / CrumbBar / BackToParentItem
 * 保持互不影响，这里提供 iOS 非暗黑质感的高品质替代实现。
 */

/** 灰色图标底座（文件类图标底色，iOS 灰） */
@Composable
internal fun iosFillColor(): Color = if (isIosDark()) Color(0xFF3A3A3C) else Color(0xFFE9E9EB)

/**
 * iOS 文件行：白色圆角卡片 + 前置图标底座 + 文件名滚动 + 副标题，
 * 按压弹性微缩并淡灰高亮，多选时行首 iOS 复选框。用于网盘云盘浏览列表。
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun IosFileRow(
    file: ShareFile,
    onClick: () -> Unit,
    /** 非空时行尾显示「转存」按钮 */
    onSave: (() -> Unit)? = null,
    /** 非空时行尾显示「更多」按钮（打开文件操作菜单） */
    onMore: (() -> Unit)? = null,
    /** 长按进入多选（多选模式下为 null） */
    onLongClick: (() -> Unit)? = null,
    /** 多选模式：是否选中 */
    selected: Boolean = false,
    /** 是否显示行首复选框（仅多选模式列表传 true；移动/转存等选择器不显示） */
    showCheckbox: Boolean = false,
    /** 列表项动画等（调用方传入 Modifier.animateItem()） */
    modifier: Modifier = Modifier
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val bg by animateColorAsState(
        targetValue = when {
            pressed -> iosPressColor()
            selected -> IosBlue.copy(alpha = 0.10f)
            else -> Color.Transparent
        },
        animationSpec = NativeSpringColorSoft,
        label = "iosFileRowBg"
    )
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.982f else 1f,
        animationSpec = NativeSpring,
        label = "iosFileRowScale"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(12.dp))
            .background(iosCardColor())
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(bg)
                .then(
                    if (onLongClick != null) {
                        Modifier.combinedClickable(
                            interactionSource = interaction,
                            indication = null,
                            onClick = onClick,
                            onLongClick = onLongClick
                        )
                    } else {
                        Modifier.clickable(
                            interactionSource = interaction,
                            indication = null,
                            onClick = onClick
                        )
                    }
                )
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 多选模式：行首 iOS 复选框
            if (showCheckbox) {
                IosCheckbox(checked = selected, onCheckedChange = { onClick() })
                Spacer(modifier = Modifier.width(12.dp))
            }
            // 图标（文件夹蓝 / 文件灰）
            IosIconTile(
                icon = if (file.isdir) Icons.Outlined.Folder else Icons.Outlined.InsertDriveFile,
                background = if (file.isdir) IosBlue.copy(alpha = 0.14f) else iosFillColor(),
                tint = if (file.isdir) IosBlue else iosSecondaryLabelColor(),
                size = 38.dp,
                iconSize = 20.dp
            )
            Spacer(modifier = Modifier.width(13.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = file.fname,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Medium,
                    color = iosLabelColor(),
                    maxLines = 1,
                    modifier = Modifier.basicMarquee(iterations = Int.MAX_VALUE)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (file.isdir) "文件夹" else iosFormatBytes(file.fsize),
                    fontSize = 13.sp,
                    color = iosSecondaryLabelColor()
                )
            }
            if (onSave != null) {
                Spacer(modifier = Modifier.width(4.dp))
                IosIconButton(icon = Icons.Outlined.SaveAlt, tint = IosBlue, onClick = onSave)
            }
            if (onMore != null) {
                Spacer(modifier = Modifier.width(2.dp))
                IosIconButton(
                    icon = Icons.Outlined.MoreVert,
                    tint = iosSecondaryLabelColor(),
                    onClick = onMore
                )
            }
            Spacer(modifier = Modifier.width(2.dp))
            Icon(
                imageVector = Icons.Outlined.ChevronRight,
                contentDescription = null,
                tint = IosGray3,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

/** iOS「返回上一级」：白色圆角卡片 + 蓝色上箭头 + 蓝色文字 */
@Composable
internal fun IosBackToParentItem(onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val bg by animateColorAsState(
        targetValue = if (pressed) iosPressColor() else Color.Transparent,
        animationSpec = NativeSpringColorSoft,
        label = "iosBackParentBg"
    )
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.985f else 1f,
        animationSpec = NativeSpring,
        label = "iosBackParentScale"
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(12.dp))
            .background(iosCardColor())
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(bg)
                .clickable(
                    interactionSource = interaction,
                    indication = null,
                    onClick = onClick
                )
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.ArrowUpward,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = IosBlue
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "返回上一级",
                fontSize = 16.sp,
                color = IosBlue,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

/**
 * iOS 可点击面包屑：根标题 > 目录1 > 目录2。
 * 非当前层灰色可点击回退；当前层系统蓝高亮（文件夹图标）。横向滚动并自动定位。
 */
@Composable
internal fun IosCrumbBar(
    rootTitle: String,
    pathNames: List<String>,
    onNavigate: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val crumbs = buildList {
        add(rootTitle.ifBlank { "根目录" })
        pathNames.forEach { add(it) }
    }
    val scroll = rememberScrollState()
    LaunchedEffect(crumbs.size, crumbs.lastOrNull()) {
        scroll.scrollTo(scroll.maxValue)
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(scroll)
            .padding(start = 4.dp, top = 4.dp)
    ) {
        crumbs.forEachIndexed { i, name ->
            val isLast = i == crumbs.size - 1
            if (!isLast) {
                Text(
                    text = name,
                    fontSize = 13.sp,
                    color = iosSecondaryLabelColor(),
                    maxLines = 1,
                    modifier = Modifier
                        .clickable { onNavigate(i) }
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                )
                Icon(
                    imageVector = Icons.Outlined.ChevronRight,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = IosGray3
                )
            } else {
                Icon(
                    imageVector = Icons.Outlined.FolderOpen,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = IosBlue
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = name,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = IosBlue,
                    maxLines = 1,
                    modifier = Modifier.padding(end = 4.dp)
                )
            }
        }
    }
}

/** iOS 空目录提示 */
@Composable
internal fun IosEmptyHint(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        fontSize = 15.sp,
        color = iosSecondaryLabelColor(),
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 40.dp),
        textAlign = androidx.compose.ui.text.style.TextAlign.Center
    )
}

/**
 * iOS 返回顶部按钮：白色圆角卡片 + 系统蓝上箭头 + 柔和阴影，
 * 列表上滑离开顶部后右下角淡入缩放出现，点击平滑回顶，按压弹性微缩。
 */
@Composable
internal fun IosScrollToTopButton(
    listState: LazyListState,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    // 上滑离开顶部（首项不是 0，或首项有偏移）即显示
    val showButton by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 0
        }
    }
    AnimatedVisibility(
        visible = showButton,
        enter = fadeIn(NativeSpringSoft) + scaleIn(NativeSpring, initialScale = 0.8f),
        exit = fadeOut(NativeSpringSoft) + scaleOut(NativeSpring, targetScale = 0.8f),
        modifier = modifier
    ) {
        val interaction = remember { MutableInteractionSource() }
        val pressed by interaction.collectIsPressedAsState()
        val scale by animateFloatAsState(
            targetValue = if (pressed) 0.9f else 1f,
            animationSpec = NativeSpring,
            label = "iosToTopScale"
        )
        Box(
            modifier = Modifier
                .size(42.dp)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
                .shadow(elevation = 6.dp, shape = RoundedCornerShape(14.dp), clip = false)
                .clip(RoundedCornerShape(14.dp))
                .background(iosCardColor())
                .clickable(
                    interactionSource = interaction,
                    indication = null
                ) { scope.launch { listState.animateScrollToItem(0) } },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.KeyboardArrowUp,
                contentDescription = "返回顶部",
                tint = IosBlue,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}
