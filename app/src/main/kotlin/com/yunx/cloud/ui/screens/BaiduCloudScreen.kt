package com.yunx.cloud.ui.screens

import com.yunx.cloud.ui.SnackbarController
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.DriveFileMove
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yunx.cloud.data.network.model.ShareFile
import com.yunx.cloud.data.prefs.SettingsRepository
import com.yunx.cloud.ui.items.MultiSelectAction
import com.yunx.cloud.ui.items.MultiSelectBar
import com.yunx.cloud.ui.components.IosActionRow
import com.yunx.cloud.ui.components.IosAlertDialog
import com.yunx.cloud.ui.components.IosBackToParentItem
import com.yunx.cloud.ui.components.IosBlockButton
import com.yunx.cloud.ui.components.IosBlue
import com.yunx.cloud.ui.components.IosBottomSheet
import com.yunx.cloud.ui.components.IosButtonStyle
import com.yunx.cloud.ui.components.IosCheckbox
import com.yunx.cloud.ui.components.IosCrumbBar
import com.yunx.cloud.ui.components.IosEmptyHint
import com.yunx.cloud.ui.components.IosFileRow
import com.yunx.cloud.ui.components.IosIconButton
import com.yunx.cloud.ui.components.IosIconTile
import com.yunx.cloud.ui.components.IosPillButton
import com.yunx.cloud.ui.components.IosRed
import com.yunx.cloud.ui.components.IosScreenBackground
import com.yunx.cloud.ui.components.IosScrollToTopButton
import com.yunx.cloud.ui.components.IosSearchField
import com.yunx.cloud.ui.components.IosSegmentedControl
import com.yunx.cloud.ui.components.IosTextField
import com.yunx.cloud.ui.components.NativeSpringIntOffset
import com.yunx.cloud.ui.components.NativeSpringSoft
import com.yunx.cloud.ui.components.iosLabelColor
import com.yunx.cloud.ui.components.iosSecondaryLabelColor
import com.yunx.cloud.ui.resolve.DownloadLinkDialog
import com.yunx.cloud.ui.viewmodel.BaiduCloudUiState
import com.yunx.cloud.ui.viewmodel.BaiduCloudViewModel

/** 百度非会员限速阈值：>300MB 提示 */
private const val BAIDU_LIMIT_BYTES = 300L * 1024 * 1024

/**
 * 百度网盘云盘浏览页（参考夸克/UC/迅雷云盘）：
 * - 目录浏览 + 下拉刷新 + 面包屑回退
 * - 长按多选（批量下载/分享/移动/删除）
 * - 文件/文件夹操作菜单（下载/重命名/移动/分享/删除）
 * 认证走 Cookie（BDUSS），目录用绝对路径。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BaiduCloudScreen(
    viewModel: BaiduCloudViewModel,
    onExit: () -> Unit,
    onDownloadStarted: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsState()
    // 网盘内搜索：按文件名过滤当前目录（仅本地过滤，不影响浏览/操作逻辑）
    var searchQuery by remember { mutableStateOf("") }
    // 系统返回键 → 子目录返回上一级，根目录返回账号列表（对齐解析页返回行为）
    BackHandler {
        val s = state
        if (s is BaiduCloudUiState.Loaded && s.pathNames.isNotEmpty()) {
            searchQuery = "" // 返回上一级清空搜索
            viewModel.back()
        } else onExit()
    }
    // 文件列表滚动状态（返回顶部按钮用）
    val listState = rememberLazyListState()
    var showActionSheet by remember { mutableStateOf(false) }
    var showRename by remember { mutableStateOf(false) }
    var showMove by remember { mutableStateOf(false) }
    var showShare by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    // 百度非会员 >300MB 限速提示：记住「不再显示」，单文件/批量下载前拦截
    val settingsRepo = remember { SettingsRepository(context) }
    var limitHintDismissed by remember { mutableStateOf(settingsRepo.baiduLimitHintDismissed) }
    var showBaiduLimitDialog by remember { mutableStateOf(false) }
    var pendingBaiduDownload by remember { mutableStateOf<String?>(null) } // "single" / "batch"

    /** 判断是否需要弹限速提示；需要则记录待执行动作并弹窗，否则直接执行 */
    fun maybeShowBaiduLimit(files: List<ShareFile>, action: String, onProceed: () -> Unit) {
        if (!limitHintDismissed && files.any { it.fsize > BAIDU_LIMIT_BYTES }) {
            pendingBaiduDownload = action
            showBaiduLimitDialog = true
        } else {
            onProceed()
        }
    }

    LaunchedEffect(viewModel.cloudMessage) {
        viewModel.cloudMessage?.let {
            SnackbarController.show(it)
            viewModel.consumeMessage()
        }
    }

    LaunchedEffect(viewModel.downloadTriggered) {
        if (viewModel.downloadTriggered > 0) {
            viewModel.consumeDownloadTriggered()
            onDownloadStarted()
        }
    }

    // 单文件下载确认弹窗（对齐解析页：展示直链，长按可复制）
    viewModel.downloadLink?.let { link ->
        DownloadLinkDialog(
            link = link,
            onDownload = { viewModel.startDownload() },
            onDismiss = { viewModel.dismissDownloadDialog() }
        )
    }

    IosScreenBackground {
        AnimatedContent(
            targetState = state,
            transitionSpec = {
                fadeIn(NativeSpringSoft) togetherWith fadeOut(NativeSpringSoft)
            },
            label = "baiduCloudState"
        ) { s ->
            when (s) {
                is BaiduCloudUiState.Loading -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(32.dp),
                        strokeWidth = 2.5.dp,
                        color = IosBlue
                    )
                }

                is BaiduCloudUiState.Error -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = s.message,
                            fontSize = 15.sp,
                            color = iosSecondaryLabelColor(),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 32.dp)
                        )
                        Spacer(modifier = Modifier.height(18.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            IosPillButton(text = "返回", onClick = onExit, tint = IosBlue)
                            IosPillButton(text = "重试", onClick = { viewModel.loadRoot() }, tint = IosBlue)
                        }
                    }
                }

                is BaiduCloudUiState.Loaded -> Box(modifier = Modifier.fillMaxSize()) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // 网盘内搜索框（固定显示在顶部，不随列表滚动；多选模式下隐藏，避免与批量操作冲突）
                        if (!viewModel.multiSelectMode) {
                            IosSearchField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                placeholder = "搜索当前目录文件名",
                                onClear = { searchQuery = "" },
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                            )
                        }
                        // 搜索过滤后的文件列表（空关键词显示全部）
                        val visibleFiles = remember(s.files, searchQuery) {
                            val q = searchQuery.trim()
                            if (q.isEmpty()) s.files
                            else s.files.filter { it.fname.contains(q, ignoreCase = true) }
                        }
                        PullToRefreshBox(
                            isRefreshing = viewModel.refreshing,
                            onRefresh = { viewModel.refresh() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        ) {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier
                                .fillMaxSize(),
                            contentPadding = PaddingValues(
                                start = 16.dp, end = 16.dp, top = 16.dp,
                                bottom = if (viewModel.multiSelectMode) 176.dp else 96.dp
                            ),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            item {
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        if (viewModel.multiSelectMode) {
                                            IosIconButton(
                                                icon = Icons.AutoMirrored.Filled.ArrowBack,
                                                tint = IosBlue,
                                                onClick = { viewModel.exitMultiSelect() },
                                                contentDescription = "取消选择"
                                            )
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = "已选 ${viewModel.selected.size} 项",
                                                    fontSize = 17.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    color = iosLabelColor(),
                                                    maxLines = 1
                                                )
                                                Text(
                                                    text = if (viewModel.selected.size == s.files.size) "已全选" else "点击选择更多文件",
                                                    fontSize = 13.sp,
                                                    color = iosSecondaryLabelColor()
                                                )
                                            }
                                            IosPillButton(
                                                text = if (viewModel.selected.size == s.files.size) "取消全选" else "全选",
                                                onClick = { viewModel.toggleSelectAll(s.files) }
                                            )
                                        } else {
                                            IosIconButton(
                                                icon = Icons.AutoMirrored.Filled.ArrowBack,
                                                tint = IosBlue,
                                                onClick = onExit,
                                                contentDescription = "返回"
                                            )
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = "百度网盘",
                                                    fontSize = 17.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = iosLabelColor(),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = "共 ${s.files.size} 项",
                                                    fontSize = 13.sp,
                                                    color = iosSecondaryLabelColor()
                                                )
                                            }
                                        }
                                    }
                                    if (!viewModel.multiSelectMode) {
                                        IosCrumbBar(
                                            rootTitle = "百度网盘",
                                            pathNames = s.pathNames,
                                            onNavigate = { searchQuery = ""; viewModel.navigateToLevel(it) }
                                        )
                                    }
                                }
                            }

                            if (s.pathNames.isNotEmpty()) {
                                item {
                                    IosBackToParentItem(onClick = { viewModel.back() })
                                }
                            }

                            if (s.files.isEmpty()) {
                                item {
                                    IosEmptyHint(text = "此目录为空")
                                }
                            } else if (visibleFiles.isEmpty() && viewModel.multiSelectMode.not()) {
                                item {
                                    IosEmptyHint(text = "未找到与「${searchQuery.trim()}」匹配的文件")
                                }
                            }

                            items(visibleFiles, key = { it.fid }) { file ->
                                IosFileRow(
                                    file = file,
                                    modifier = Modifier.animateItem(),
                                    onClick = {
                                        if (viewModel.multiSelectMode) {
                                            viewModel.toggleSelect(file)
                                        } else if (file.isdir) {
                                            searchQuery = "" // 进入新目录清空搜索，避免误过滤
                                            viewModel.openFolder(file)
                                        } else {
                                            viewModel.openActions(file)
                                            showActionSheet = true
                                        }
                                    },
                                    onMore = if (!viewModel.multiSelectMode && file.isdir) {
                                        {
                                            viewModel.openActions(file)
                                            showActionSheet = true
                                        }
                                    } else {
                                        null
                                    },
                                    onLongClick = if (!viewModel.multiSelectMode) {
                                        { viewModel.enterMultiSelect(file) }
                                    } else {
                                        null
                                    },
                                    selected = viewModel.selected.contains(file),
                                    showCheckbox = viewModel.multiSelectMode
                                )
                            }
                        }
                    }
                    }

                    // 返回顶部按钮（上滑离开顶部后显示；多选模式下上移避开底部批量栏）
                    IosScrollToTopButton(
                        listState = listState,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(
                                end = 16.dp,
                                bottom = if (viewModel.multiSelectMode) 176.dp else 96.dp
                            )
                    )

                    AnimatedVisibility(
                        visible = viewModel.multiSelectMode,
                        enter = slideInVertically(NativeSpringIntOffset) { it } + fadeIn(NativeSpringSoft),
                        exit = slideOutVertically(NativeSpringIntOffset) { it } + fadeOut(NativeSpringSoft),
                        modifier = Modifier.align(Alignment.BottomCenter)
                    ) {
                        MultiSelectBar(
                            count = viewModel.selected.size,
                            actions = listOf(
                                MultiSelectAction("下载", Icons.Outlined.Download, IosBlue) {
                                    maybeShowBaiduLimit(viewModel.selected, "batch") { viewModel.downloadSelected() }
                                },
                                MultiSelectAction("分享", Icons.Outlined.Share, IosBlue) {
                                    showShare = true
                                },
                                MultiSelectAction("移动", Icons.Outlined.DriveFileMove, IosBlue) {
                                    viewModel.openMoveRoot()
                                    showMove = true
                                },
                                MultiSelectAction("删除", Icons.Outlined.Delete, IosRed) {
                                    showDeleteConfirm = true
                                }
                            )
                        )
                    }
                }
            }
        }
    }

    // 文件操作菜单
    if (showActionSheet && viewModel.actionFile != null) {
        BaiduActionSheet(
            file = viewModel.actionFile!!,
            viewModel = viewModel,
            onDownload = {
                showActionSheet = false
                val f = viewModel.actionFile
                if (f != null) {
                    maybeShowBaiduLimit(listOf(f), "single") { viewModel.downloadFile() }
                }
            },
            onDownloadFolder = {
                showActionSheet = false
                viewModel.downloadFolder()
            },
            onRename = {
                showActionSheet = false
                showRename = true
            },
            onMove = {
                showActionSheet = false
                viewModel.openMoveRoot()
                showMove = true
            },
            onShare = {
                showActionSheet = false
                showShare = true
            },
            onDelete = {
                showActionSheet = false
                showDeleteConfirm = true
            },
            onDismiss = {
                showActionSheet = false
                viewModel.dismissActions()
            }
        )
    }

    if (showRename && viewModel.actionFile != null) {
        BaiduRenameDialog(
            file = viewModel.actionFile!!,
            viewModel = viewModel,
            onDismiss = { showRename = false }
        )
    }

    if (showMove) {
        BaiduMoveSheet(
            viewModel = viewModel,
            onDismiss = { showMove = false }
        )
    }

    if (showShare) {
        BaiduShareSheet(
            viewModel = viewModel,
            onDismiss = { showShare = false }
        )
    }

    viewModel.shareResult?.let { info ->
        ShareResultDialog(
            info = info,
            onDismiss = { viewModel.dismissShareResult() }
        )
    }

    if (showDeleteConfirm) {
        val deleting = if (viewModel.multiSelectMode) "选中的 ${viewModel.selected.size} 项" else "「${viewModel.actionFile?.fname ?: ""}」"
        IosAlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = "删除文件",
            message = "确定要删除$deleting 吗？删除后进入回收站。",
            confirmText = "删除",
            confirmStyle = IosButtonStyle.Destructive,
            onConfirm = {
                showDeleteConfirm = false
                if (viewModel.multiSelectMode) viewModel.deleteSelected() else viewModel.deleteFile()
            },
            dismissText = "取消",
            onDismiss = { showDeleteConfirm = false }
        )
    }

    // 操作执行中加载弹窗（下载文件夹/批量下载显示进度）
    if (viewModel.isOperating) {
        IosAlertDialog(
            onDismissRequest = { },
            title = "处理中",
            confirmText = "",
            onConfirm = { },
            dismissText = "中断",
            onDismiss = { viewModel.cancelDownload() }
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp,
                    color = IosBlue
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = viewModel.folderProgress ?: "正在处理，请稍候…",
                    fontSize = 15.sp,
                    color = iosLabelColor()
                )
            }
        }
    }

    // 百度非会员 >300MB 限速提示弹窗（可勾选不再显示）
    if (showBaiduLimitDialog) {
        var neverShow by remember { mutableStateOf(limitHintDismissed) }
        IosAlertDialog(
            onDismissRequest = { showBaiduLimitDialog = false },
            title = "下载大文件提示",
            confirmText = "继续下载",
            onConfirm = {
                showBaiduLimitDialog = false
                settingsRepo.baiduLimitHintDismissed = neverShow
                limitHintDismissed = neverShow
                when (pendingBaiduDownload) {
                    "single" -> viewModel.downloadFile()
                    "batch" -> viewModel.downloadSelected()
                }
                pendingBaiduDownload = null
            },
            dismissText = "取消",
            onDismiss = { showBaiduLimitDialog = false }
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "百度网盘非会员超过300MB会被限速，下载速度可能较慢。是否继续下载？",
                    fontSize = 15.sp,
                    color = iosLabelColor()
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IosCheckbox(checked = neverShow, onCheckedChange = { neverShow = it })
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("不再显示此提示", fontSize = 15.sp, color = iosLabelColor())
                }
            }
        }
    }
}

/** 百度文件操作菜单：下载/分享/移动/重命名/删除 */
@Composable
private fun BaiduActionSheet(
    file: ShareFile,
    viewModel: BaiduCloudViewModel,
    onDownload: () -> Unit,
    onDownloadFolder: (() -> Unit)? = null,
    onRename: () -> Unit,
    onMove: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    IosBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // 标题
            Row(verticalAlignment = Alignment.CenterVertically) {
                IosIconTile(
                    icon = if (file.isdir) Icons.Outlined.DriveFileMove else Icons.Outlined.Download,
                    background = IosBlue.copy(alpha = 0.14f),
                    tint = IosBlue,
                    size = 42.dp,
                    iconSize = 22.dp
                )
                Spacer(modifier = Modifier.width(13.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = file.fname,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = iosLabelColor(),
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.height(1.dp))
                    Text(
                        text = if (file.isdir) "文件夹" else "文件",
                        fontSize = 13.sp,
                        color = iosSecondaryLabelColor()
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(thickness = 0.5.dp, color = androidx.compose.ui.graphics.Color(0x1A3C3C43))
            Spacer(modifier = Modifier.height(6.dp))

            // 操作项
            if (!file.isdir) {
                IosActionRow(
                    icon = Icons.Outlined.Download,
                    iconBackground = IosBlue.copy(alpha = 0.12f),
                    iconTint = IosBlue,
                    title = "下载",
                    subtitle = "使用内置下载功能保存到本机",
                    onClick = onDownload
                )
            } else if (onDownloadFolder != null) {
                IosActionRow(
                    icon = Icons.Outlined.Download,
                    iconBackground = IosBlue.copy(alpha = 0.12f),
                    iconTint = IosBlue,
                    title = "下载文件夹",
                    subtitle = "递归下载整个文件夹，保持目录结构",
                    onClick = onDownloadFolder
                )
            }
            IosActionRow(
                icon = Icons.Outlined.Share,
                iconBackground = IosBlue.copy(alpha = 0.12f),
                iconTint = IosBlue,
                title = "分享",
                subtitle = "生成分享链接（带提取码）",
                onClick = onShare
            )
            IosActionRow(
                icon = Icons.Outlined.DriveFileMove,
                iconBackground = IosBlue.copy(alpha = 0.12f),
                iconTint = IosBlue,
                title = "移动到",
                subtitle = "移动到网盘的其他目录",
                onClick = onMove
            )
            IosActionRow(
                icon = Icons.Outlined.Edit,
                iconBackground = IosBlue.copy(alpha = 0.12f),
                iconTint = IosBlue,
                title = "重命名",
                subtitle = "修改文件名",
                onClick = onRename
            )
            IosActionRow(
                icon = Icons.Outlined.Delete,
                iconBackground = IosRed.copy(alpha = 0.12f),
                iconTint = IosRed,
                title = "删除",
                subtitle = "删除到回收站",
                onClick = onDelete,
                showDivider = false
            )
        }
    }
}

/** 重命名弹窗 */
@Composable
private fun BaiduRenameDialog(
    file: ShareFile,
    viewModel: BaiduCloudViewModel,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(file.fname) }
    IosAlertDialog(
        onDismissRequest = onDismiss,
        title = "重命名",
        confirmText = "确定",
        onConfirm = {
            onDismiss()
            if (name.isNotBlank() && name != file.fname) viewModel.renameFile(name.trim())
        },
        dismissText = "取消",
        onDismiss = onDismiss
    ) {
        IosTextField(
            value = name,
            onValueChange = { name = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = "新文件名",
            singleLine = true
        )
    }
}

/** 移动目录选择弹窗（独立浏览，不影响主列表） */
@Composable
private fun BaiduMoveSheet(
    viewModel: BaiduCloudViewModel,
    onDismiss: () -> Unit
) {
    val moveState by viewModel.moveUiState.collectAsState()
    LaunchedEffect(Unit) { viewModel.openMoveRoot() }
    IosBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text("移动到", fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = iosLabelColor())
            Spacer(modifier = Modifier.height(10.dp))
            IosCrumbBar(
                rootTitle = "根目录",
                pathNames = (moveState as? BaiduCloudUiState.Loaded)?.pathNames ?: emptyList(),
                onNavigate = { viewModel.moveNavigateToLevel(it) }
            )
            Spacer(modifier = Modifier.height(8.dp))
            // 返回上一级：固定在目录区上方（不参与 AnimatedContent 过渡，避免与目录内容交叉叠加）
            if ((moveState as? BaiduCloudUiState.Loaded)?.pathNames?.isNotEmpty() == true) {
                IosBackToParentItem(onClick = { viewModel.moveBack() })
                Spacer(modifier = Modifier.height(4.dp))
            }
            AnimatedContent(
                targetState = moveState,
                transitionSpec = { fadeIn(NativeSpringSoft) togetherWith fadeOut(NativeSpringSoft) },
                label = "baiduMoveState"
            ) { s ->
                when (s) {
                    is BaiduCloudUiState.Loading -> Box(
                        modifier = Modifier.fillMaxWidth().height(180.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(32.dp),
                            strokeWidth = 2.5.dp,
                            color = IosBlue
                        )
                    }

                    is BaiduCloudUiState.Error -> Box(
                        modifier = Modifier.fillMaxWidth().height(140.dp),
                        contentAlignment = Alignment.Center
                    ) { Text(s.message, fontSize = 15.sp, color = iosSecondaryLabelColor()) }

                    is BaiduCloudUiState.Loaded -> {
                        val dirs = s.files.filter { it.isdir }
                        if (dirs.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxWidth().height(90.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "当前目录没有子文件夹，可直接移动到此处",
                                    fontSize = 15.sp,
                                    color = iosSecondaryLabelColor(),
                                    textAlign = TextAlign.Center
                                )
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxWidth().heightIn(max = 240.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                items(dirs, key = { it.fid }) { dir ->
                                    IosFileRow(file = dir, onClick = { viewModel.openMoveFolder(dir) })
                                }
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            val dirName = (moveState as? BaiduCloudUiState.Loaded)?.pathNames?.lastOrNull() ?: "根目录"
            IosBlockButton(
                text = "移动到此处（$dirName）",
                onClick = {
                    val to = (moveState as? BaiduCloudUiState.Loaded)?.dirPath ?: "/"
                    if (viewModel.multiSelectMode) viewModel.moveSelected(to) else viewModel.moveFile(to)
                    onDismiss()
                }
            )
        }
    }
}

/** 分享设置弹窗（百度必须带 4 位提取码 + 有效期） */
@Composable
private fun BaiduShareSheet(
    viewModel: BaiduCloudViewModel,
    onDismiss: () -> Unit
) {
    var passcode by remember { mutableStateOf("") }
    var period by remember { mutableStateOf(0) }
    val periodOptions = listOf(
        "永久有效" to 0,
        "1 天" to 1,
        "7 天" to 7,
        "30 天" to 30
    )
    IosBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text("分享文件", fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = iosLabelColor())
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                "百度分享必须带 4 位提取码",
                fontSize = 13.sp,
                color = iosSecondaryLabelColor()
            )
            Spacer(modifier = Modifier.height(14.dp))
            Text("提取码（4 位字母数字）", fontSize = 13.sp, color = iosSecondaryLabelColor())
            Spacer(modifier = Modifier.height(8.dp))
            IosTextField(
                value = passcode,
                onValueChange = { passcode = it.take(4).filter { c -> c.isLetterOrDigit() } },
                placeholder = "提取码（4 位字母数字）"
            )
            Spacer(modifier = Modifier.height(18.dp))
            Text("有效期", fontSize = 13.sp, color = iosSecondaryLabelColor())
            Spacer(modifier = Modifier.height(8.dp))
            IosSegmentedControl(
                options = periodOptions.map { it.first },
                selectedIndex = periodOptions.indexOfFirst { it.second == period }.coerceAtLeast(0),
                onSelected = { i -> period = periodOptions[i].second }
            )
            Spacer(modifier = Modifier.height(20.dp))
            IosBlockButton(
                text = "创建分享",
                onClick = {
                    if (viewModel.multiSelectMode) {
                        viewModel.shareSelected(period, passcode)
                    } else {
                        viewModel.shareFile(period, passcode)
                    }
                    onDismiss()
                },
                enabled = passcode.length == 4
            )
        }
    }
}