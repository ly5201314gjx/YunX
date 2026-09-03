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
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
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
import com.yunx.cloud.ui.items.MultiSelectAction
import com.yunx.cloud.ui.items.MultiSelectBar
import com.yunx.cloud.ui.components.IosAlertDialog
import com.yunx.cloud.ui.components.IosBackToParentItem
import com.yunx.cloud.ui.components.IosBlue
import com.yunx.cloud.ui.components.IosButtonStyle
import com.yunx.cloud.ui.components.IosCrumbBar
import com.yunx.cloud.ui.components.IosEmptyHint
import com.yunx.cloud.ui.components.IosFileRow
import com.yunx.cloud.ui.components.IosIconButton
import com.yunx.cloud.ui.components.IosPillButton
import com.yunx.cloud.ui.components.IosRed
import com.yunx.cloud.ui.components.IosScreenBackground
import com.yunx.cloud.ui.components.IosSearchField
import com.yunx.cloud.ui.components.IosScrollToTopButton
import com.yunx.cloud.ui.components.NativeSpringIntOffset
import com.yunx.cloud.ui.components.NativeSpringSoft
import com.yunx.cloud.ui.components.iosLabelColor
import com.yunx.cloud.ui.components.iosSecondaryLabelColor
import com.yunx.cloud.ui.resolve.DownloadLinkDialog
import com.yunx.cloud.ui.viewmodel.QuarkCloudUiState
import com.yunx.cloud.ui.viewmodel.QuarkCloudViewModel

/**
 * 夸克云盘浏览页：展示个人网盘文件，支持进入文件夹 / 返回 / 面包屑回退。
 * iOS 非暗黑质感：浅灰分组背景 + 白色圆角文件卡 + 原生弹簧过渡。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CloudDriveScreen(
    viewModel: QuarkCloudViewModel,
    onExit: () -> Unit,
    /** 下载入队后通知上层切换到「下载」Tab（对齐解析页行为） */
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
        if (s is QuarkCloudUiState.Loaded && s.pathNames.isNotEmpty()) {
            searchQuery = "" // 返回上一级清空搜索
            viewModel.back()
        } else onExit()
    }
    // 文件列表滚动状态（返回顶部按钮用）
    val listState = rememberLazyListState()
    // 批量操作弹窗（多选模式底部栏触发：分享/移动需要设置或选目录，下载/删除直接执行）
    var showBatchActions by remember { mutableStateOf(false) }
    var batchInitial by remember { mutableStateOf(BatchStep.MENU) }
    // 批量删除二次确认
    var showDeleteConfirm by remember { mutableStateOf(false) }

    // 操作结果 Toast（放在本层：弹窗关闭后仍能正常弹出）
    LaunchedEffect(viewModel.cloudMessage) {
        viewModel.cloudMessage?.let {
            SnackbarController.show(it)
            viewModel.consumeMessage()
        }
    }

    // 下载入队后通知上层切换到「下载」Tab（消费事件，避免再次进入本页重复触发）
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

    // iOS 分组背景：避免 Tab 内切换时透出下层内容（账号列表）导致视觉重叠
    IosScreenBackground {
        // 目录切换（进入文件夹/返回）：列表淡入淡出过渡（原生弹簧）
        AnimatedContent(
            targetState = state,
            transitionSpec = {
                fadeIn(NativeSpringSoft) togetherWith fadeOut(NativeSpringSoft)
            },
            label = "cloudState"
        ) { s ->
            when (s) {
            is QuarkCloudUiState.Loading -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(32.dp),
                    strokeWidth = 2.5.dp,
                    color = IosBlue
                )
            }

            is QuarkCloudUiState.Error -> Box(
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

            is QuarkCloudUiState.Loaded -> Box(modifier = Modifier.fillMaxSize()) {
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
                            // 多选模式：取消选择
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
                                    text = "夸克网盘",
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
                    // 可点击面包屑（多选模式下隐藏）
                    if (!viewModel.multiSelectMode) {
                        IosCrumbBar(
                            rootTitle = "夸克网盘",
                            pathNames = s.pathNames,
                            onNavigate = { searchQuery = ""; viewModel.navigateToLevel(it) }
                        )
                    }
                }
            }

            // 返回上一级（根目录时不显示）
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
                        }
                    },
                    // 多选模式：隐藏行尾按钮；非多选时文件夹显示「更多」、全部可长按进入多选
                    onMore = if (!viewModel.multiSelectMode && file.isdir) {
                        { viewModel.openActions(file) }
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

                // 多选模式：底部批量操作栏（底部滑入淡入，退出反向，原生弹簧）
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
                                // 批量下载：保持网盘页显示处理中弹窗，不自动切页
                                viewModel.downloadSelected()
                            },
                            MultiSelectAction("分享", Icons.Outlined.Share, IosBlue) {
                                batchInitial = BatchStep.SHARE
                                showBatchActions = true
                            },
                            MultiSelectAction("移动", Icons.Outlined.DriveFileMove, IosBlue) {
                                batchInitial = BatchStep.MOVE
                                showBatchActions = true
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

    // 文件操作弹窗（更多按钮/点击文件 → 下载/分享/移动/重命名/删除）
    viewModel.actionFile?.let { file ->
        FileActionSheet(
            file = file,
            viewModel = viewModel,
            onDismiss = { viewModel.dismissActions() }
        )
    }

    // 批量操作弹窗（长按多选 → 底部栏分享/移动）
    if (showBatchActions) {
        BatchActionSheet(
            viewModel = viewModel,
            initialStep = batchInitial,
            onDismiss = { showBatchActions = false }
        )
    }

    // 批量删除二次确认（底部栏点删除直接弹确认）
    if (showDeleteConfirm) {
        IosAlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = "删除文件",
            message = "确定要删除选中的 ${viewModel.selected.size} 项吗？删除后将移入回收站。",
            confirmText = "删除",
            confirmStyle = IosButtonStyle.Destructive,
            onConfirm = {
                showDeleteConfirm = false
                viewModel.deleteSelected()
            },
            dismissText = "取消",
            onDismiss = { showDeleteConfirm = false }
        )
    }

    // 操作执行中：加载弹窗（下载取链/分享/移动/删除；下载文件夹显示进度）
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
}
}
