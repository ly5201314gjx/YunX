package com.yunx.cloud.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yunx.cloud.data.db.BaiduAccountEntity
import com.yunx.cloud.data.db.C139AccountEntity
import com.yunx.cloud.data.db.Pan123AccountEntity
import com.yunx.cloud.data.db.QuarkAccountEntity
import com.yunx.cloud.data.db.UCAccountEntity
import com.yunx.cloud.data.db.XunleiAccountEntity
import com.yunx.cloud.data.network.model.QuotaInfo
import com.yunx.cloud.ui.viewmodel.BaiduCloudViewModel
import com.yunx.cloud.ui.viewmodel.C139CloudViewModel
import com.yunx.cloud.ui.viewmodel.DriveQuotaViewModel
import com.yunx.cloud.ui.viewmodel.Pan123CloudViewModel
import com.yunx.cloud.ui.viewmodel.QuarkCloudViewModel
import com.yunx.cloud.ui.viewmodel.UCCoudViewModel
import com.yunx.cloud.ui.viewmodel.XunleiCloudViewModel
import com.yunx.cloud.ui.components.IosAvatar
import com.yunx.cloud.ui.components.IosBlue
import com.yunx.cloud.ui.components.IosGreen
import com.yunx.cloud.ui.components.IosIconButton
import com.yunx.cloud.ui.components.IosProgressBar
import com.yunx.cloud.ui.components.IosScreenBackground
import com.yunx.cloud.ui.components.NativeSpring
import com.yunx.cloud.ui.components.NativeSpringColorSoft
import com.yunx.cloud.ui.components.NativeSpringIntSize
import com.yunx.cloud.ui.components.NativeSpringSoft
import com.yunx.cloud.ui.components.NetworkAvatar
import com.yunx.cloud.ui.components.iosFillColor
import com.yunx.cloud.ui.components.iosLabelColor
import com.yunx.cloud.ui.components.iosPressColor
import com.yunx.cloud.ui.components.iosSecondaryLabelColor
import com.yunx.cloud.ui.components.iosSeparatorColor

/**
 * 网盘账号展示模型。
 * TODO: 迅雷 / UC 后续接入 cookie 登录后，isLoggedIn 由真实登录态驱动。
 */
private data class DriveAccount(
    val id: String,
    val name: String,
    val description: String,
    val avatarText: String,
    /** 真实品牌图标（favicon 服务返回 PNG；123 云盘为 ICO，由 NetworkAvatar 内置解析） */
    val iconUrl: String,
    val isLoggedIn: Boolean = false
)

/**
 * 网盘页：
 * - 夸克未登录：点击进入登录页；
 * - 夸克已登录：副标题显示昵称，点击弹出账号信息底部弹窗（可查看 Cookie / 退出登录）。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DriveScreen(
    scrollBehavior: TopAppBarScrollBehavior,
    quarkAccount: QuarkAccountEntity?,
    ucAccount: UCAccountEntity?,
    xunleiAccount: XunleiAccountEntity?,
    baiduAccount: BaiduAccountEntity?,
    c139Account: C139AccountEntity?,
    pan123Account: Pan123AccountEntity?,
    /** 夸克云盘浏览 ViewModel（网盘 Tab 内切换展示，非全屏） */
    quarkCloudViewModel: QuarkCloudViewModel,
    /** UC 网盘云盘浏览 ViewModel */
    ucCloudViewModel: UCCoudViewModel,
    /** 迅雷网盘云盘浏览 ViewModel */
    xunleiCloudViewModel: XunleiCloudViewModel,
    /** 百度网盘云盘浏览 ViewModel */
    baiduCloudViewModel: BaiduCloudViewModel,
    /** 139 网盘云盘浏览 ViewModel */
    c139CloudViewModel: C139CloudViewModel,
    /** 123 云盘浏览 ViewModel */
    pan123CloudViewModel: Pan123CloudViewModel,
    /** 网盘空间详情 ViewModel（顶部空间总览） */
    driveQuotaViewModel: DriveQuotaViewModel,
    onQuarkLogin: () -> Unit,
    onQuarkLogout: () -> Unit,
    /** 夸克云盘下载入队后切换到「下载」Tab */
    onDownloadStarted: () -> Unit = {},
    onUCLogin: () -> Unit,
    onUCLogout: () -> Unit,
    onXunleiLogin: () -> Unit,
    onXunleiLogout: () -> Unit,
    onBaiduLogin: () -> Unit,
    onBaiduLogout: () -> Unit,
    onC139Login: () -> Unit,
    onC139Logout: () -> Unit,
    onPan123Login: () -> Unit,
    onPan123Logout: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showQuarkSheet by remember { mutableStateOf(false) }
    var showUCSheet by remember { mutableStateOf(false) }
    var showXunleiSheet by remember { mutableStateOf(false) }
    var showBaiduSheet by remember { mutableStateOf(false) }
    var showC139Sheet by remember { mutableStateOf(false) }
    var showPan123Sheet by remember { mutableStateOf(false) }
    // 夸克云盘浏览：网盘 Tab 内切换（非全屏），切 Tab 再回来仍保留
    var showCloud by rememberSaveable { mutableStateOf(false) }
    // UC 网盘云盘浏览：网盘 Tab 内切换（非全屏）
    var showUCCloud by rememberSaveable { mutableStateOf(false) }
    // 迅雷网盘云盘浏览：网盘 Tab 内切换（非全屏）
    var showXunleiCloud by rememberSaveable { mutableStateOf(false) }
    // 百度网盘云盘浏览：网盘 Tab 内切换（非全屏）
    var showBaiduCloud by rememberSaveable { mutableStateOf(false) }
    // 139 网盘云盘浏览：网盘 Tab 内切换（非全屏）
    var showC139Cloud by rememberSaveable { mutableStateOf(false) }
    // 123 云盘浏览：网盘 Tab 内切换（非全屏）
    var showPan123Cloud by rememberSaveable { mutableStateOf(false) }

    // 夸克：登录态由数据库驱动；已登录则副标题显示昵称
    val quark = DriveAccount(
        id = "quark",
        name = "夸克网盘",
        description = quarkAccount?.nickname ?: "点击登录，支持解析下载",
        avatarText = "夸",
        iconUrl = "https://favicon.im/pan.quark.cn?format=png",
        isLoggedIn = quarkAccount != null
    )
    val uc = DriveAccount(
        id = "uc",
        name = "UC网盘",
        description = ucAccount?.nickname ?: "点击登录，支持解析下载",
        avatarText = "UC",
        iconUrl = "https://icon.horse/icon/drive.uc.cn",
        isLoggedIn = ucAccount != null
    )
    val xunlei = DriveAccount(
        id = "xunlei",
        name = "迅雷网盘",
        description = xunleiAccount?.nickname ?: "点击登录，支持解析下载",
        avatarText = "迅",
        iconUrl = "https://favicon.im/pan.xunlei.com?format=png",
        isLoggedIn = xunleiAccount != null
    )
    val baidu = DriveAccount(
        id = "baidu",
        name = "百度网盘",
        description = baiduAccount?.nickname ?: "点击登录，支持解析下载",
        avatarText = "度",
        iconUrl = "https://favicon.im/pan.baidu.com?format=png",
        isLoggedIn = baiduAccount != null
    )
    val c139 = DriveAccount(
        id = "c139",
        name = "139网盘",
        description = c139Account?.nickname ?: "点击登录，支持解析下载",
        avatarText = "139",
        iconUrl = "https://favicon.im/cloud.189.cn?format=png",
        isLoggedIn = c139Account != null
    )
    val pan123 = DriveAccount(
        id = "pan123",
        name = "123云盘",
        description = pan123Account?.nickname ?: "点击登录，支持解析下载",
        avatarText = "123",
        iconUrl = "https://favicon.im/123pan.com?format=png",
        isLoggedIn = pan123Account != null
    )
    val others = remember {
        emptyList<DriveAccount>()
    }

    // 进入网盘页加载空间详情（仅已登录平台）
    LaunchedEffect(Unit) {
        driveQuotaViewModel.loadAll()
    }
    // 下拉刷新状态：绑定空间配额加载中状态
    val isRefreshing by driveQuotaViewModel.loading.collectAsState()

    // 账号列表 ↔ 夸克云盘 ↔ UC 云盘 ↔ 迅雷云盘 ↔ 百度云盘 ↔ 139 云盘 ↔ 123 云盘：平滑过渡（淡入 + 轻微缩放，原生弹簧）
    IosScreenBackground {
        AnimatedContent(
        targetState = when {
            showCloud -> 1
            showUCCloud -> 2
            showXunleiCloud -> 3
            showBaiduCloud -> 4
            showC139Cloud -> 5
            showPan123Cloud -> 6
            else -> 0
        },
        transitionSpec = {
            (fadeIn(NativeSpringSoft) + scaleIn(NativeSpring, initialScale = 0.98f))
                .togetherWith(fadeOut(NativeSpringSoft) + scaleOut(NativeSpring, targetScale = 0.98f))
        },
        label = "driveContent"
    ) { target ->
        when (target) {
            1 -> CloudDriveScreen(
                viewModel = quarkCloudViewModel,
                onExit = { showCloud = false },
                onDownloadStarted = onDownloadStarted
            )
            2 -> UCCoudScreen(
            viewModel = ucCloudViewModel,
            onExit = { showUCCloud = false },
            onDownloadStarted = onDownloadStarted
        )
        3 -> XunleiCloudScreen(
            viewModel = xunleiCloudViewModel,
            onExit = { showXunleiCloud = false },
            onDownloadStarted = onDownloadStarted
        )
        4 -> BaiduCloudScreen(
            viewModel = baiduCloudViewModel,
            onExit = { showBaiduCloud = false },
            onDownloadStarted = onDownloadStarted
        )
        5 -> C139CloudScreen(
            viewModel = c139CloudViewModel,
            onExit = { showC139Cloud = false },
            onDownloadStarted = onDownloadStarted
        )
        6 -> Pan123CloudScreen(
            viewModel = pan123CloudViewModel,
            onExit = { showPan123Cloud = false },
            onDownloadStarted = onDownloadStarted
        )
            else -> PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = { driveQuotaViewModel.loadAll() },
                modifier = Modifier.fillMaxSize()
            ) {
                LazyColumn(
                modifier = modifier
                    .fillMaxSize()
                    .nestedScroll(scrollBehavior.nestedScrollConnection),
                contentPadding = PaddingValues(top = 8.dp, bottom = 96.dp)
            ) {
                item {
                    Text(
                        text = "登录网盘后即可浏览文件与下载",
                        fontSize = 13.sp,
                        color = iosSecondaryLabelColor(),
                        modifier = Modifier.padding(start = 20.dp, top = 4.dp, bottom = 2.dp)
                    )
                }
                item(key = quark.id) {
                    DriveAccountRow(
                        account = quark,
                        quota = driveQuotaViewModel.quarkQuota.collectAsState().value,
                        onClick = if (quark.isLoggedIn) {
                            { showCloud = true }
                        } else {
                            onQuarkLogin
                        },
                        onMoreClick = if (quark.isLoggedIn) {
                            { showQuarkSheet = true }
                        } else {
                            null
                        }
                    )
                }
                item(key = uc.id) {
                    DriveAccountRow(
                        account = uc,
                        quota = driveQuotaViewModel.ucQuota.collectAsState().value,
                        onClick = if (uc.isLoggedIn) {
                            { showUCCloud = true }
                        } else {
                            onUCLogin
                        },
                        onMoreClick = if (uc.isLoggedIn) {
                            { showUCSheet = true }
                        } else {
                            null
                        }
                    )
                }
                item(key = xunlei.id) {
                    DriveAccountRow(
                        account = xunlei,
                        quota = driveQuotaViewModel.xunleiQuota.collectAsState().value,
                        onClick = if (xunlei.isLoggedIn) {
                            { showXunleiCloud = true }
                        } else {
                            onXunleiLogin
                        },
                        onMoreClick = if (xunlei.isLoggedIn) {
                            { showXunleiSheet = true }
                        } else {
                            null
                        }
                    )
                }
                item(key = baidu.id) {
                    DriveAccountRow(
                        account = baidu,
                        quota = driveQuotaViewModel.baiduQuota.collectAsState().value,
                        onClick = if (baidu.isLoggedIn) {
                            { showBaiduCloud = true }
                        } else {
                            onBaiduLogin
                        },
                        onMoreClick = if (baidu.isLoggedIn) {
                            { showBaiduSheet = true }
                        } else {
                            null
                        }
                    )
                }
                item(key = c139.id) {
                    DriveAccountRow(
                        account = c139,
                        quota = driveQuotaViewModel.c139Quota.collectAsState().value,
                        onClick = if (c139.isLoggedIn) {
                            { showC139Cloud = true }
                        } else {
                            onC139Login
                        },
                        onMoreClick = if (c139.isLoggedIn) {
                            { showC139Sheet = true }
                        } else {
                            null
                        }
                    )
                }
                item(key = pan123.id) {
                    DriveAccountRow(
                        account = pan123,
                        quota = driveQuotaViewModel.pan123Quota.collectAsState().value,
                        onClick = if (pan123.isLoggedIn) {
                            { showPan123Cloud = true }
                        } else {
                            onPan123Login
                        },
                        onMoreClick = if (pan123.isLoggedIn) {
                            { showPan123Sheet = true }
                        } else {
                            null
                        },
                        showDivider = false
                    )
                }
                items(others, key = { it.id }) { account ->
                    DriveAccountRow(account = account, showDivider = false)
                }
            }
            }
        }
    }
    }

    // 已登录夸克：点击卡片弹出账号信息底部弹窗
    if (showQuarkSheet && quarkAccount != null) {
        QuarkAccountSheet(
            account = quarkAccount,
            onLogout = {
                onQuarkLogout()
                showQuarkSheet = false
            },
            onDismiss = { showQuarkSheet = false }
        )
    }

    // 已登录 UC：点击卡片弹出账号信息底部弹窗
    if (showUCSheet && ucAccount != null) {
        UCAccountSheet(
            account = ucAccount,
            onLogout = {
                onUCLogout()
                showUCSheet = false
            },
            onDismiss = { showUCSheet = false }
        )
    }

    // 已登录迅雷：点击卡片弹出账号信息底部弹窗
    if (showXunleiSheet && xunleiAccount != null) {
        XunleiAccountSheet(
            account = xunleiAccount,
            onLogout = {
                onXunleiLogout()
                showXunleiSheet = false
            },
            onDismiss = { showXunleiSheet = false }
        )
    }

    // 已登录百度：点击卡片弹出账号信息底部弹窗
    if (showBaiduSheet && baiduAccount != null) {
        BaiduAccountSheet(
            account = baiduAccount,
            onLogout = {
                onBaiduLogout()
                showBaiduSheet = false
            },
            onDismiss = { showBaiduSheet = false }
        )
    }

    // 已登录 139：点击卡片弹出账号信息底部弹窗
    if (showC139Sheet && c139Account != null) {
        C139AccountSheet(
            account = c139Account,
            onLogout = {
                onC139Logout()
                showC139Sheet = false
            },
            onDismiss = { showC139Sheet = false }
        )
    }

    // 已登录 123：点击卡片弹出账号信息底部弹窗
    if (showPan123Sheet && pan123Account != null) {
        Pan123AccountSheet(
            account = pan123Account,
            onLogout = {
                onPan123Logout()
                showPan123Sheet = false
            },
            onDismiss = { showPan123Sheet = false }
        )
    }
}

@Composable
private fun DriveAccountRow(
    account: DriveAccount,
    /** 网盘空间详情（已登录且有数据时在行内显示进度条）；null 不显示 */
    quota: QuotaInfo? = null,
    onClick: (() -> Unit)? = null,
    /** 已登录时右侧「三个点」更多按钮（打开账号弹窗）；null 则不显示 */
    onMoreClick: (() -> Unit)? = null,
    /** 是否在行底绘制细分隔线（末行关闭） */
    showDivider: Boolean = true
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val bg by animateColorAsState(
        targetValue = if (pressed) iosPressColor() else Color.Transparent,
        animationSpec = NativeSpringColorSoft,
        label = "driveRowBg"
    )
    // iOS 极简透明行：不采用框体约束，直接平铺在分组背景上，仅保留点击高亮与细分隔线
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(bg)
                .then(
                    if (onClick != null) {
                        Modifier.clickable(
                            interactionSource = interaction,
                            indication = null,
                            onClick = onClick
                        )
                    } else {
                        Modifier
                    }
                )
                .padding(horizontal = 18.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 品牌头像：真实联网图标（加载中/失败时回退文字头像）
            NetworkAvatar(
                imageUrl = account.iconUrl,
                size = 38.dp,
                contentDescription = account.name,
                fallback = {
                    IosAvatar(
                        text = account.avatarText,
                        background = if (account.isLoggedIn) IosBlue else iosFillColor(),
                        contentColor = if (account.isLoggedIn) Color.White else iosSecondaryLabelColor(),
                        size = 38.dp
                    )
                }
            )

            Spacer(modifier = Modifier.width(13.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = account.name,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = iosLabelColor()
                    )
                    if (account.isLoggedIn) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(color = IosGreen, shape = CircleShape)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = account.description,
                    fontSize = 12.sp,
                    color = iosSecondaryLabelColor(),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                // 已登录且有空间数据：行内展示剩余空间进度条（出现时淡入 + 纵向展开，原生弹簧）
                AnimatedVisibility(
                    visible = account.isLoggedIn && quota != null,
                    enter = fadeIn(NativeSpringSoft) + expandVertically(
                        expandFrom = Alignment.Top,
                        animationSpec = NativeSpringIntSize
                    ),
                    exit = fadeOut(NativeSpringSoft) + shrinkVertically(animationSpec = NativeSpringIntSize)
                ) {
                    Column {
                        Spacer(modifier = Modifier.height(6.dp))
                        quota?.let { QuotaInlineBar(it) }
                    }
                }
            }

            when {
                account.isLoggedIn && onMoreClick != null -> IosIconButton(
                    icon = Icons.Outlined.MoreVert,
                    tint = iosSecondaryLabelColor(),
                    onClick = onMoreClick,
                    contentDescription = "更多"
                )
                onClick != null -> IosLoginPill(text = "登录", onClick = onClick)
            }
        }
        if (showDivider) {
            HorizontalDivider(
                modifier = Modifier.padding(start = 18.dp + 38.dp + 13.dp),
                thickness = 0.5.dp,
                color = iosSeparatorColor()
            )
        }
    }
}

/** 网盘卡片内空间进度条：已用 / 总容量 + 细进度条（iOS 蓝） */
@Composable
private fun QuotaInlineBar(quota: QuotaInfo) {
    val ratio = if (quota.total > 0) {
        (quota.used.toFloat() / quota.total.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }
    Column {
        Text(
            text = "已用 ${formatBytes(quota.used)} / ${formatBytes(quota.total)}",
            fontSize = 12.sp,
            color = iosSecondaryLabelColor()
        )
        Spacer(modifier = Modifier.height(5.dp))
        IosProgressBar(progress = ratio, color = IosBlue, height = 4.dp)
    }
}

/** iOS 轻量登录胶囊：淡蓝底 + 蓝色文字（比实心蓝更轻盈，契合 iOS 质感），按压弹性微缩 */
@Composable
private fun IosLoginPill(
    text: String,
    onClick: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.92f else 1f,
        animationSpec = NativeSpring,
        label = "loginPillScale"
    )
    Box(
        modifier = Modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(CircleShape)
            .background(IosBlue.copy(alpha = 0.12f))
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 16.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = IosBlue,
            maxLines = 1
        )
    }
}

/** 字节数格式化：B / KB / MB / GB / TB */
private fun formatBytes(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    var value = bytes.toDouble()
    var unit = 0
    while (value >= 1024 && unit < units.size - 1) {
        value /= 1024
        unit++
    }
    return if (unit == 0) "${bytes} B"
    else String.format("%.1f %s", value, units[unit])
}