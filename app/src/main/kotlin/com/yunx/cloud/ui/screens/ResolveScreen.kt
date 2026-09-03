package com.yunx.cloud.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.yunx.cloud.data.db.ResolveHistoryEntity
import com.yunx.cloud.data.network.ParsedShare
import com.yunx.cloud.data.network.ShareLinkParser
import com.yunx.cloud.data.network.SharePlatform
import com.yunx.cloud.ui.SnackbarController
import com.yunx.cloud.ui.components.BrandVioletDeep
import com.yunx.cloud.ui.components.BrandVioletInk
import com.yunx.cloud.ui.components.BrandVioletMid
import com.yunx.cloud.ui.components.IosAlertDialog
import com.yunx.cloud.ui.components.IosBlue
import com.yunx.cloud.ui.components.IosButtonStyle
import com.yunx.cloud.ui.components.IosGray3
import com.yunx.cloud.ui.components.IosGreen
import com.yunx.cloud.ui.components.IosGroupCard
import com.yunx.cloud.ui.components.IosIconButton
import com.yunx.cloud.ui.components.IosOrange
import com.yunx.cloud.ui.components.IosRed
import com.yunx.cloud.ui.components.IosTeal
import com.yunx.cloud.ui.components.IosTextField
import com.yunx.cloud.ui.components.LocalGlassSurfaceBackdrop
import com.yunx.cloud.ui.components.NativeSpringColorSoft
import com.yunx.cloud.ui.components.NativeSpringIntOffset
import com.yunx.cloud.ui.components.NativeSpringSoft
import com.yunx.cloud.ui.components.SoftBadgeLight
import com.yunx.cloud.ui.components.SoftCardShape
import com.yunx.cloud.ui.components.SoftGradientButton
import com.yunx.cloud.ui.components.SoftHeroIllustration
import com.yunx.cloud.ui.components.SoftInputField
import com.yunx.cloud.ui.components.SoftTextHint
import com.yunx.cloud.ui.components.iosCardColor
import com.yunx.cloud.ui.components.iosLabelColor
import com.yunx.cloud.ui.components.iosPressColor
import com.yunx.cloud.ui.components.iosSecondaryLabelColor
import com.yunx.cloud.ui.components.iosSeparatorColor
import com.yunx.cloud.ui.components.softShadow
import com.yunx.cloud.ui.components.surfaceLiquidGlass
import com.yunx.cloud.ui.resolve.DownloadLinkDialog
import com.yunx.cloud.ui.resolve.ShareDetailScreen
import com.yunx.cloud.ui.viewmodel.BaiduCloudViewModel
import com.yunx.cloud.ui.viewmodel.C139CloudViewModel
import com.yunx.cloud.ui.viewmodel.Pan123CloudViewModel
import com.yunx.cloud.ui.viewmodel.QuarkCloudViewModel
import com.yunx.cloud.ui.viewmodel.ResolveUiState
import com.yunx.cloud.ui.viewmodel.ResolveViewModel
import com.yunx.cloud.ui.viewmodel.UCCoudViewModel
import com.yunx.cloud.ui.viewmodel.WebExtractState
import com.yunx.cloud.ui.viewmodel.XunleiCloudViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 解析页：输入分享链接与提取码 → 解析 → 展示分享详情 → 获取下载直链。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResolveScreen(
    scrollBehavior: TopAppBarScrollBehavior,
    viewModel: ResolveViewModel,
    /** 夸克云盘浏览 ViewModel（分享文件转存目录选择用） */
    quarkCloudViewModel: QuarkCloudViewModel,
    /** 迅雷网盘云盘浏览 ViewModel（迅雷分享转存目录选择用） */
    xunleiCloudViewModel: XunleiCloudViewModel,
    /** 百度网盘云盘浏览 ViewModel（百度分享转存目录选择用） */
    baiduCloudViewModel: BaiduCloudViewModel,
    /** 139 网盘云盘浏览 ViewModel（139 分享转存目录选择用） */
    c139CloudViewModel: C139CloudViewModel,
    /** UC 网盘云盘浏览 ViewModel（UC 分享转存目录选择用） */
    ucCloudViewModel: UCCoudViewModel,
    /** 123 云盘浏览 ViewModel（123 分享转存目录选择用） */
    pan123CloudViewModel: Pan123CloudViewModel,
    modifier: Modifier = Modifier
) {
    val state = viewModel.uiState
    val downloadLink = viewModel.downloadLink
    val downloadError = viewModel.downloadError
    val context = LocalContext.current
    // 解析历史（Room Flow → 列表）
    val history by viewModel.resolveHistory.collectAsState()

    // 批量解析 / 网页提取 弹窗开关
    var showBatchDialog by rememberSaveable { mutableStateOf(false) }
    var showWebDialog by rememberSaveable { mutableStateOf(false) }

    // 输入框状态提升到页面层：进入详情/文件夹再返回时不清空
    var link by rememberSaveable { mutableStateOf("") }
    var pwd by rememberSaveable { mutableStateOf("") }
    var pwdEdited by rememberSaveable { mutableStateOf(false) }

    // 剪贴板分享链接提示状态：待提示的剪贴板文本 + 已忽略的文本
    // 用 rememberSaveable：切换 Tab 后返回仍保留（避免「忽略后切页回来又弹」）
    var clipboardSuggestion by rememberSaveable { mutableStateOf<String?>(null) }
    var ignoredClipboard by rememberSaveable { mutableStateOf<String?>(null) }

    // 检测函数：读取剪贴板，满足条件则设置提示（三重触发：组合时 / ON_RESUME / 剪贴板变化）
    val maybeSuggestClipboard: () -> Unit = {
        val text = readClipboardSafely(context)
        if (text != null &&
            state is ResolveUiState.Idle &&
            text.isNotBlank() &&
            text != link &&
            text != ignoredClipboard &&
            ShareLinkParser.parse(text) != null
        ) {
            clipboardSuggestion = text
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    DisposableEffect(lifecycleOwner, clipboard) {
        // 剪贴板变化立即检测（前台最灵敏，复制即提示）
        val clipListener = ClipboardManager.OnPrimaryClipChangedListener {
            maybeSuggestClipboard()
            // 部分 ROM 剪贴板内容写入有延迟，300ms 后重试一次
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                maybeSuggestClipboard()
            }, 300)
        }
        clipboard.addPrimaryClipChangedListener(clipListener)
        // 打开应用 / 从后台切回时检测
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) maybeSuggestClipboard()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        // 冷启动兜底：组合完成立即检测一次（避免 ON_RESUME 早于 observer 注册导致漏检）
        maybeSuggestClipboard()
        onDispose {
            clipboard.removePrimaryClipChangedListener(clipListener)
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Android 11 及以下：轻量轮询兜底（2s 一次）。
    // 部分 ROM（如 vivo）剪贴板监听不触发时仍能识别；Android 12+ 读剪贴板会弹系统提示，不轮询。
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
        LaunchedEffect(Unit) {
            while (true) {
                kotlinx.coroutines.delay(2000)
                maybeSuggestClipboard()
            }
        }
    }

    // 链接变化时自动匹配提取码（用户未手动输入时）
    LaunchedEffect(link) {
        if (!pwdEdited && pwd.isEmpty()) {
            ShareLinkParser.parse(link)?.pwd?.let { pwd = it }
        }
    }

    // 下载错误提示
    LaunchedEffect(downloadError) {
        downloadError?.let {
            SnackbarController.show(it)
            viewModel.consumeDownloadError()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        // 状态切换过渡：输入态/加载/详情/错误之间平滑淡入淡出 + 轻微位移
        AnimatedContent(
            targetState = state,
            transitionSpec = {
                (fadeIn(NativeSpringSoft) + slideInVertically(NativeSpringIntOffset) { it / 20 })
                    .togetherWith(fadeOut(NativeSpringSoft))
            },
            label = "resolveState"
        ) { s ->
            when (s) {
                is ResolveUiState.Detail -> ShareDetailScreen(
            session = s.session,
            files = s.files,
            viewModel = viewModel,
            quarkCloudViewModel = quarkCloudViewModel,
            xunleiCloudViewModel = xunleiCloudViewModel,
            baiduCloudViewModel = baiduCloudViewModel,
            c139CloudViewModel = c139CloudViewModel,
            ucCloudViewModel = ucCloudViewModel,
            pan123CloudViewModel = pan123CloudViewModel,
            scrollBehavior = scrollBehavior,
                    // 顶部左上角返回：退出文件页回到输入页（输入框内容保留）
                    onExit = { viewModel.backToInput() },
                    // 列表「返回上一级」：子目录回上级，根目录回输入页
                    onBack = { viewModel.navigateBack() }
                )
                is ResolveUiState.Loading -> LoadingContent()
                else -> ResolveInputContent(
                    viewModel = viewModel,
                    scrollBehavior = scrollBehavior,
                    state = s,
                    link = link,
                    onLinkChange = { link = it },
                    pwd = pwd,
                    onPwdChange = {
                        pwd = it
                        pwdEdited = true
                    },
                    onClearLink = {
                        link = ""
                        pwd = ""
                        pwdEdited = false
                    },
                    onClearPwd = { pwd = "" },
                    history = history,
                    onBatchClick = { showBatchDialog = true },
                    onWebClick = { showWebDialog = true },
                    onReparse = { h ->
                        link = h.link
                        pwd = h.pwd
                        pwdEdited = true
                        viewModel.startResolve(h.link, h.pwd.ifBlank { null })
                    },
                    onCopyLink = { h ->
                        copyToClipboard(context, h.link)
                        SnackbarController.show("分享链接已复制")
                    },
                    onCopyDirect = { h ->
                        copyToClipboard(context, h.directUrl)
                        SnackbarController.show("直链已复制")
                    },
                    onDeleteHistory = { viewModel.deleteHistory(it) },
                    onClearHistory = { viewModel.clearHistory() }
                )
            }
        }

        // 剪贴板分享链接提示卡片（仅输入页、有待提示内容时显示，带弹出动画）
        // animatedSuggestion 保留最后提示内容，保证退出动画期间卡片不消失
        var animatedSuggestion by remember { mutableStateOf<String?>(null) }
        LaunchedEffect(clipboardSuggestion) {
            clipboardSuggestion?.let { animatedSuggestion = it }
        }
        AnimatedVisibility(
            visible = state is ResolveUiState.Idle && clipboardSuggestion != null,
            enter = fadeIn(NativeSpringSoft) +
                slideInVertically(NativeSpringIntOffset) { -it / 2 } +
                scaleIn(NativeSpringSoft),
            exit = fadeOut(NativeSpringSoft) +
                slideOutVertically(NativeSpringIntOffset) { -it / 2 } +
                scaleOut(NativeSpringSoft),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            animatedSuggestion?.let { suggestion ->
                val parsed = ShareLinkParser.parse(suggestion)
                ClipboardSuggestCard(
                    platformName = parsed?.platform?.let { platformLabel(it) } ?: "网盘",
                    onPaste = {
                        link = suggestion
                        pwd = parsed?.pwd.orEmpty()
                        pwdEdited = true
                        clipboardSuggestion = null
                        viewModel.startResolve(suggestion, parsed?.pwd)
                    },
                    onDismiss = {
                        ignoredClipboard = suggestion
                        clipboardSuggestion = null
                    }
                )
            }
        }
    }

    // 获取下载直链加载弹窗（转存/取链需要时间，避免无反馈）
    if (viewModel.isFetchingDownloadLink) {
        IosAlertDialog(
            onDismissRequest = { },
            dismissText = null,
            confirmText = "",
            onConfirm = {},
            title = "获取下载链接",
            content = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp,
                        color = IosBlue
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "正在获取下载链接，请稍候…",
                        fontSize = 15.sp,
                        color = iosLabelColor()
                    )
                }
            }
        )
    }

    // 下载直链弹窗
    downloadLink?.let { link ->
        DownloadLinkDialog(
            link = link,
            onDownload = { viewModel.startDownload(link) },
            onDismiss = { viewModel.dismissDownloadDialog() }
        )
    }

    // 批量解析弹窗（一次粘贴多个链接，逐个取链加入下载）
    if (showBatchDialog) {
        BatchResolveDialog(
            onDismiss = { showBatchDialog = false },
            onResolve = { links ->
                showBatchDialog = false
                viewModel.startBatchResolve(links)
            }
        )
    }

    // 网页链接提取弹窗（粘贴网页 URL 自动抓取其中的网盘分享链接）
    if (showWebDialog) {
        WebExtractDialog(
            state = viewModel.webExtractState,
            onExtract = { viewModel.extractWebLinks(it) },
            onReset = { viewModel.resetWebExtract() },
            onResolveOne = { share ->
                showWebDialog = false
                viewModel.resetWebExtract()
                link = share.url
                pwd = share.pwd.orEmpty()
                pwdEdited = true
                viewModel.startResolve(share.url, share.pwd)
            },
            onResolveAll = { shares ->
                showWebDialog = false
                viewModel.resetWebExtract()
                viewModel.startBatchResolve(shares.map { it.url })
            },
            onDismiss = {
                showWebDialog = false
                viewModel.resetWebExtract()
            }
        )
    }

    // 批量解析进行中：加载弹窗（显示进度，可中断）
    if (viewModel.isBatchResolving) {
        IosAlertDialog(
            onDismissRequest = { },
            title = "批量解析",
            confirmText = "中断",
            confirmStyle = IosButtonStyle.Destructive,
            onConfirm = { viewModel.cancelBatchResolve() },
            dismissText = null,
            content = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp,
                        color = IosBlue
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = viewModel.batchResolveProgress?.let { "正在解析第 $it 个分享…" }
                            ?: "正在批量解析，请稍候…",
                        fontSize = 15.sp,
                        color = iosLabelColor()
                    )
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ResolveInputContent(
    viewModel: ResolveViewModel,
    scrollBehavior: TopAppBarScrollBehavior,
    state: ResolveUiState,
    link: String,
    onLinkChange: (String) -> Unit,
    pwd: String,
    onPwdChange: (String) -> Unit,
    onClearLink: () -> Unit,
    onClearPwd: () -> Unit,
    history: List<ResolveHistoryEntity>,
    onBatchClick: () -> Unit,
    onWebClick: () -> Unit,
    onReparse: (ResolveHistoryEntity) -> Unit,
    onCopyLink: (ResolveHistoryEntity) -> Unit,
    onCopyDirect: (ResolveHistoryEntity) -> Unit,
    onDeleteHistory: (Long) -> Unit,
    onClearHistory: () -> Unit
) {
    val isLoading = state is ResolveUiState.Loading
    val scheme = MaterialTheme.colorScheme

    Column(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // ===== 顶部 Hero 区：大标题 + 副标题 + 3D 立体插画 =====
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "解析",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Black,
                    color = scheme.onSurface
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "粘贴分享链接，一键解析分享内容",
                    fontSize = 13.sp,
                    color = scheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            SoftHeroIllustration()
        }

        Spacer(modifier = Modifier.height(2.dp))

        // ===== 卡片 1：分享链接 =====
        SoftInputCard(
            title = {
                Text(
                    text = "分享链接",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = scheme.onSurface
                )
            },
            icon = {
                Icon(
                    imageVector = Icons.Outlined.Link,
                    contentDescription = null,
                    tint = BrandVioletInk,
                    modifier = Modifier.size(18.dp)
                )
            }
        ) {
            SoftInputField(
                value = link,
                onValueChange = onLinkChange,
                placeholder = "例如：https://pan.quark.cn/s/xxxx",
                minLines = 2,
                maxLines = 4,
                onImeAction = { viewModel.startResolve(link, pwd) },
                trailingIcon = if (link.isNotEmpty()) {
                    {
                        IconButton(onClick = onClearLink) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "清空链接",
                                tint = SoftTextHint,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                } else null
            )
        }

        // ===== 卡片 2：提取码（可选）=====
        SoftInputCard(
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "提取码",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = scheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "(可选)",
                        fontSize = 11.sp,
                        color = scheme.onSurfaceVariant
                    )
                }
            },
            icon = {
                Text(
                    text = "123",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = BrandVioletInk
                )
            }
        ) {
            SoftInputField(
                value = pwd,
                onValueChange = onPwdChange,
                placeholder = "请输入提取码（可选）",
                singleLine = true,
                onImeAction = { viewModel.startResolve(link, pwd) },
                trailingIcon = if (pwd.isNotEmpty()) {
                    {
                        IconButton(onClick = onClearPwd) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "清空提取码",
                                tint = SoftTextHint,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                } else null
            )
        }

        // ===== 核心执行按钮：满宽渐变胶囊 =====
        SoftGradientButton(
            text = "开始解析",
            loading = isLoading,
            enabled = link.isNotBlank() && !isLoading,
            onClick = { viewModel.startResolve(link, pwd) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 2.dp)
        )

        // ===== 辅助入口：批量解析 / 网页提取 =====
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SecondaryActionButton(
                icon = Icons.Outlined.PlayArrow,
                text = "批量解析",
                onClick = onBatchClick,
                modifier = Modifier.weight(1f)
            )
            SecondaryActionButton(
                icon = Icons.Outlined.Public,
                text = "网页提取",
                onClick = onWebClick,
                modifier = Modifier.weight(1f)
            )
        }

        // ===== 错误提示 =====
        if (state is ResolveUiState.Error) {
            SoftErrorCard(message = state.message)
        }

        // ===== 解析历史 =====
        if (history.isNotEmpty()) {
            ResolveHistorySection(
                history = history,
                onReparse = onReparse,
                onCopyLink = onCopyLink,
                onCopyDirect = onCopyDirect,
                onDelete = onDeleteHistory,
                onClear = onClearHistory
            )
        }
    }
}

/** 次级操作按钮（批量解析 / 网页提取）：浅底 + 主题色图标，按压律动 */
@Composable
private fun SecondaryActionButton(
    icon: ImageVector,
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val bg by animateColorAsState(
        targetValue = if (pressed) iosPressColor() else MaterialTheme.colorScheme.surfaceContainer,
        animationSpec = NativeSpringColorSoft,
        label = "secondaryActionBg"
    )
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(bg)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = BrandVioletInk,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = text,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = BrandVioletInk
            )
        }
    }
}

/** 解析历史区块：列表 + 清空入口，支持重新解析 / 复制链接 / 复制直链 / 删除 */
@Composable
private fun ResolveHistorySection(
    history: List<ResolveHistoryEntity>,
    onReparse: (ResolveHistoryEntity) -> Unit,
    onCopyLink: (ResolveHistoryEntity) -> Unit,
    onCopyDirect: (ResolveHistoryEntity) -> Unit,
    onDelete: (Long) -> Unit,
    onClear: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.History,
                contentDescription = null,
                tint = BrandVioletInk,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "解析历史",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = scheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = onClear) {
                Text(
                    text = "清空",
                    fontSize = 13.sp,
                    color = scheme.onSurfaceVariant
                )
            }
        }
        IosGroupCard {
            history.forEachIndexed { index, h ->
                ResolveHistoryRow(
                    h = h,
                    onClick = { onReparse(h) },
                    onCopyLink = { onCopyLink(h) },
                    onCopyDirect = { onCopyDirect(h) },
                    onDelete = { onDelete(h.id) },
                    showDivider = index != history.size - 1
                )
            }
        }
    }
}

/** 单条解析历史行：平台徽标 + 结果标题 + 链接 + 时间；点击重新解析，尾部复制直链/删除 */
@Composable
private fun ResolveHistoryRow(
    h: ResolveHistoryEntity,
    onClick: () -> Unit,
    onCopyLink: () -> Unit,
    onCopyDirect: () -> Unit,
    onDelete: () -> Unit,
    showDivider: Boolean
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val bg by animateColorAsState(
        targetValue = if (pressed) iosPressColor() else Color.Transparent,
        animationSpec = NativeSpringColorSoft,
        label = "historyRowBg"
    )
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(bg)
                .clickable(interactionSource = interaction, indication = null, onClick = onClick)
                .padding(horizontal = 14.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(platformBg(h.platform)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = platformShort(h.platform),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = platformFg(h.platform)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (h.success) h.title.ifBlank { "分享内容" } else "解析失败",
                        fontSize = 16.sp,
                        color = iosLabelColor(),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (h.success) "成功" else "失败",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (h.success) IosGreen else IosRed
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = h.link,
                    fontSize = 12.sp,
                    color = iosSecondaryLabelColor(),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = formatHistoryTime(h.createTime) +
                        if (h.pwd.isNotBlank()) " · 提取码 ${h.pwd}" else "",
                    fontSize = 11.sp,
                    color = iosSecondaryLabelColor().copy(alpha = 0.8f)
                )
            }
            if (h.directUrl.isNotBlank()) {
                IosIconButton(
                    icon = Icons.Outlined.ContentCopy,
                    tint = IosBlue,
                    onClick = onCopyDirect,
                    size = 34.dp,
                    iconSize = 17.dp,
                    contentDescription = "复制直链"
                )
                Spacer(modifier = Modifier.width(2.dp))
            }
            IosIconButton(
                icon = Icons.Outlined.DeleteOutline,
                tint = iosSecondaryLabelColor(),
                onClick = onDelete,
                size = 34.dp,
                iconSize = 17.dp,
                contentDescription = "删除"
            )
        }
        if (showDivider) {
            HorizontalDivider(
                modifier = Modifier.padding(start = 66.dp),
                thickness = 0.5.dp,
                color = iosSeparatorColor()
            )
        }
    }
}

/** 批量解析弹窗：多行粘贴多个分享链接 */
@Composable
private fun BatchResolveDialog(
    onDismiss: () -> Unit,
    onResolve: (List<String>) -> Unit
) {
    var text by remember { mutableStateOf("") }
    val context = LocalContext.current
    // 剪贴板有内容时预填，减少输入
    LaunchedEffect(Unit) {
        readClipboardSafely(context)?.takeIf { it.isNotBlank() }?.let { text = it }
    }
    IosAlertDialog(
        onDismissRequest = onDismiss,
        title = "批量解析",
        message = "一次粘贴多个分享链接（每行一个），将逐个取直链加入下载",
        confirmText = "开始解析",
        confirmEnabled = text.isNotBlank(),
        onConfirm = {
            val links = text.lineSequence().map { it.trim() }.filter { it.isNotBlank() }.toList()
            onResolve(links)
        },
        dismissText = "取消",
        onDismiss = onDismiss,
        content = {
            IosTextField(
                value = text,
                onValueChange = { text = it },
                placeholder = "https://pan.quark.cn/s/xxx\nhttps://pan.baidu.com/s/xxx",
                singleLine = false,
                modifier = Modifier.height(110.dp)
            )
        }
    )
}

/** 网页链接提取弹窗：输入网页 URL → 抓取 → 展示提取到的分享链接（可单个解析或全部解析） */
@Composable
private fun WebExtractDialog(
    state: WebExtractState,
    onExtract: (String) -> Unit,
    onReset: () -> Unit,
    onResolveOne: (ParsedShare) -> Unit,
    onResolveAll: (List<ParsedShare>) -> Unit,
    onDismiss: () -> Unit
) {
    var url by remember { mutableStateOf("") }
    val context = LocalContext.current
    // 剪贴板有 URL 时预填
    LaunchedEffect(Unit) {
        readClipboardSafely(context)?.takeIf { it.contains("http") }?.trim()?.let { url = it }
    }
    val isResult = state is WebExtractState.Result
    IosAlertDialog(
        onDismissRequest = onDismiss,
        title = "网页链接提取",
        message = "粘贴网页地址，自动抓取其中的网盘分享链接",
        confirmText = when (state) {
            is WebExtractState.Loading -> ""
            is WebExtractState.Result -> "全部解析"
            else -> "提取链接"
        },
        confirmEnabled = when (state) {
            is WebExtractState.Loading -> false
            is WebExtractState.Result -> state.shares.isNotEmpty()
            else -> url.isNotBlank()
        },
        onConfirm = when (state) {
            is WebExtractState.Result -> { { onResolveAll(state.shares) } }
            else -> { { onExtract(url.trim()) } }
        },
        dismissText = if (isResult) "重新输入" else "取消",
        onDismiss = {
            if (isResult) onReset() else onDismiss()
        },
        content = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                when (state) {
                    is WebExtractState.Idle, is WebExtractState.Error -> {
                        IosTextField(
                            value = url,
                            onValueChange = { url = it },
                            placeholder = "https://example.com/page",
                            singleLine = true
                        )
                        if (state is WebExtractState.Error) {
                            Text(
                                text = state.message,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                    is WebExtractState.Loading -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "正在抓取网页…",
                                fontSize = 14.sp,
                                color = iosSecondaryLabelColor()
                            )
                        }
                    }
                    is WebExtractState.Result -> {
                        Text(
                            text = "找到 ${state.shares.size} 个分享链接，点击解析或全部解析",
                            fontSize = 13.sp,
                            color = iosSecondaryLabelColor()
                        )
                        LazyColumn(
                            modifier = Modifier.height(180.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(state.shares) { share ->
                                WebShareRow(share = share, onClick = { onResolveOne(share) })
                            }
                        }
                    }
                }
            }
        }
    )
}

/** 网页提取结果行：平台徽标 + 链接 + 提取码 */
@Composable
private fun WebShareRow(share: ParsedShare, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (pressed) iosPressColor() else iosCardColor())
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(platformBg(share.platform.name)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = platformShort(share.platform.name),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = platformFg(share.platform.name)
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = share.url,
                fontSize = 12.sp,
                color = iosLabelColor(),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "分享码 ${share.shareId}" +
                    if (share.pwd != null) " · 提取码 ${share.pwd}" else "",
                fontSize = 11.sp,
                color = iosSecondaryLabelColor()
            )
        }
        Icon(
            imageVector = Icons.Outlined.ChevronRight,
            contentDescription = null,
            tint = IosGray3,
            modifier = Modifier.size(16.dp)
        )
    }
}

/** 平台徽标背景色 */
private fun platformBg(name: String): Color = when (name) {
    "QUARK" -> IosBlue.copy(alpha = 0.14f)
    "UC" -> IosTeal.copy(alpha = 0.16f)
    "XUNLEI" -> IosOrange.copy(alpha = 0.16f)
    "BAIDU" -> IosRed.copy(alpha = 0.14f)
    "C139" -> Color(0xFF7C3AED).copy(alpha = 0.14f)
    "PAN123" -> IosGreen.copy(alpha = 0.16f)
    else -> IosBlue.copy(alpha = 0.14f)
}

/** 平台徽标前景色（可读性优先） */
private fun platformFg(name: String): Color = when (name) {
    "QUARK" -> IosBlue
    "UC" -> Color(0xFF0E7490)
    "XUNLEI" -> Color(0xFFC2410C)
    "BAIDU" -> IosRed
    "C139" -> Color(0xFF7C3AED)
    "PAN123" -> Color(0xFF15803D)
    else -> IosBlue
}

/** 平台徽标短名 */
private fun platformShort(name: String): String = when (name) {
    "QUARK" -> "夸克"
    "UC" -> "UC"
    "XUNLEI" -> "迅雷"
    "BAIDU" -> "百度"
    "C139" -> "139"
    "PAN123" -> "123"
    else -> "盘"
}

/** 解析历史时间：刚刚 / N 分钟前 / N 小时前 / MM-dd HH:mm */
private fun formatHistoryTime(ts: Long): String {
    val diff = System.currentTimeMillis() - ts
    return when {
        diff < 60_000 -> "刚刚"
        diff < 3_600_000 -> "${diff / 60_000} 分钟前"
        diff < 86_400_000 -> "${diff / 3_600_000} 小时前"
        else -> SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(Date(ts))
    }
}

/** 复制文本到剪贴板 */
private fun copyToClipboard(context: Context, text: String) {
    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    cm.setPrimaryClip(ClipData.newPlainText("yunx_share", text))
}

/** 液态玻璃输入卡片：真模糊 + vibrancy + 折射 + 边缘高光 + 按压律动（复刻 legado ReaderMenuGlass） */
@Composable
private fun SoftInputCard(
    title: @Composable () -> Unit,
    icon: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val backdrop = LocalGlassSurfaceBackdrop.current
    val surfaceBrush = Brush.linearGradient(
        listOf(
            MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.72f),
            MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.92f)
        )
    )
    val badge = if (isDark) Color(0x59A5B4FC) else SoftBadgeLight

    Column(
        modifier = modifier
            .fillMaxWidth()
            .surfaceLiquidGlass(
                backdrop = backdrop,
                shape = SoftCardShape,
                surfaceBrush = surfaceBrush,
                blurRadius = 12.dp,
                lensRadius = 24.dp,
                useLens = true,
                interactive = true,
            )
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(badge),
                contentAlignment = Alignment.Center
            ) { icon() }
            Spacer(modifier = Modifier.width(12.dp))
            title()
        }
        Spacer(modifier = Modifier.height(10.dp))
        content()
    }
}

/** 解析错误提示卡（软阴影 + 错误色容器） */
@Composable
private fun SoftErrorCard(
    message: String,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = SoftCardShape,
        color = MaterialTheme.colorScheme.errorContainer,
        modifier = modifier
            .fillMaxWidth()
            .softShadow(SoftCardShape, elevation = 6.dp)
    ) {
        Row(
            modifier = Modifier.padding(13.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.ErrorOutline,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
        }
    }
}

/** 全屏加载中（进入文件夹/解析中展示，避免闪回输入页） */
@Composable
private fun LoadingContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(modifier = Modifier.size(28.dp), strokeWidth = 3.dp)
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "加载中…",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** 安全读取剪贴板最新文本；失败返回 null（部分 ROM 可能限制剪贴板访问） */
private fun readClipboardSafely(context: Context): String? = runCatching {
    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    cm.primaryClip
        ?.takeIf { it.itemCount > 0 }
        ?.getItemAt(0)
        ?.coerceToText(context)
        ?.toString()
}.getOrNull()

/** 平台名称（提示卡片展示） */
private fun platformLabel(platform: SharePlatform): String = when (platform) {
    SharePlatform.QUARK -> "夸克网盘"
    SharePlatform.UC -> "UC 网盘"
    SharePlatform.XUNLEI -> "迅雷网盘"
    SharePlatform.BAIDU -> "百度网盘"
    SharePlatform.C139 -> "139 网盘"
    SharePlatform.PAN123 -> "123云盘"
}

/** 剪贴板分享链接提示卡片：检测到分享链接时，询问是否粘贴解析（液态玻璃 + 紫色渐变风格） */
@Composable
private fun ClipboardSuggestCard(
    platformName: String,
    onPaste: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val backdrop = LocalGlassSurfaceBackdrop.current
    val surfaceBrush = if (isDark) {
        Brush.linearGradient(listOf(Color(0x8CA5B4FC), Color(0x73A5B4FC), Color(0x8CA5B4FC)))
    } else {
        Brush.linearGradient(listOf(Color(0xFFEEF2FF), Color(0xFFE0E7FF), Color(0xFFF5F3FF)))
    }
    val fg = if (isDark) Color(0xFFD6E3FF) else Color(0xFF3730A3)
    val pill = SoftCardShape

    Box(
        modifier = modifier
            .fillMaxWidth()
            .surfaceLiquidGlass(
                backdrop = backdrop,
                shape = pill,
                surfaceBrush = surfaceBrush,
                blurRadius = 14.dp,
                lensRadius = 24.dp,
                useLens = true,
            )
            .padding(14.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.Link,
                    contentDescription = null,
                    tint = fg,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "检测到 $platformName 分享链接",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium,
                        color = fg
                    )
                    Text(
                        text = "是否粘贴到解析框并开始解析？",
                        style = MaterialTheme.typography.bodySmall,
                        color = fg.copy(alpha = 0.85f)
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onDismiss) {
                    Text("忽略", color = fg)
                }
                Spacer(modifier = Modifier.width(4.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(BrandVioletMid, BrandVioletDeep, BrandVioletInk)
                            )
                        )
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = LocalIndication.current,
                            onClick = onPaste
                        )
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "粘贴并解析",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Medium,
                        color = Color.White
                    )
                }
            }
        }
    }
}