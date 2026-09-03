package com.yunx.cloud.ui.screens

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.InsertDriveFile
import androidx.compose.material.icons.outlined.Leaderboard
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.SystemUpdate
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.yunx.cloud.data.db.DownloadTaskEntity
import com.yunx.cloud.data.download.DownloadPlatform
import com.yunx.cloud.data.download.DownloadStats
import com.yunx.cloud.data.network.model.QuotaInfo
import com.yunx.cloud.ui.SnackbarController
import com.yunx.cloud.ui.components.IosActionRow
import com.yunx.cloud.ui.components.IosAlertDialog
import com.yunx.cloud.ui.components.IosBlue
import com.yunx.cloud.ui.components.IosBottomSheet
import com.yunx.cloud.ui.components.IosButtonStyle
import com.yunx.cloud.ui.components.IosGray3
import com.yunx.cloud.ui.components.IosGreen
import com.yunx.cloud.ui.components.IosGroupCard
import com.yunx.cloud.ui.components.IosIconButton
import com.yunx.cloud.ui.components.IosOrange
import com.yunx.cloud.ui.components.IosProgressBar
import com.yunx.cloud.ui.components.IosRed
import com.yunx.cloud.ui.components.IosScreenBackground
import com.yunx.cloud.ui.components.IosSearchField
import com.yunx.cloud.ui.components.IosSectionHeader
import com.yunx.cloud.ui.components.IosSheetTitle
import com.yunx.cloud.ui.components.IosTextField
import com.yunx.cloud.ui.components.LocalGlassSurfaceBackdrop
import com.yunx.cloud.ui.components.NativeSpring
import com.yunx.cloud.ui.components.NativeSpringColorSoft
import com.yunx.cloud.ui.components.NativeSpringIntOffset
import com.yunx.cloud.ui.components.NativeSpringIntSize
import com.yunx.cloud.ui.components.NativeSpringSoft
import com.yunx.cloud.ui.components.iosBackgroundColor
import com.yunx.cloud.ui.components.iosCardColor
import com.yunx.cloud.ui.components.iosFormatBytes
import com.yunx.cloud.ui.components.iosFormatSpeed
import com.yunx.cloud.ui.components.iosLabelColor
import com.yunx.cloud.ui.components.iosPressColor
import com.yunx.cloud.ui.components.iosSecondaryLabelColor
import com.yunx.cloud.ui.components.iosSeparatorColor
import com.yunx.cloud.ui.components.isIosDark
import com.yunx.cloud.ui.components.surfaceLiquidGlass
import com.yunx.cloud.ui.viewmodel.DownloadViewModel
import com.yunx.cloud.ui.viewmodel.DriveQuotaViewModel
import java.io.File
import java.util.Calendar

/** iOS 分组图标配色（浅色底 + 系统色） */
private fun iosTint(color: Color): Pair<Color, Color> = color.copy(alpha = 0.14f) to color

/**
 * 下载页：任务列表（分片多线程下载 / 断点续传）、进度展示、暂停/继续/删除/打开。
 * iOS 非暗黑质感：浅灰分组背景 + 白色圆角任务卡 + 原生弹簧过渡。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadScreen(
    viewModel: DownloadViewModel,
    driveQuotaViewModel: DriveQuotaViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val tasks by viewModel.tasks.collectAsState()
    val stats by viewModel.stats.collectAsState()
    val speedHistory by viewModel.speedHistory.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<DownloadTaskEntity?>(null) }
    var showDeleteAllConfirm by remember { mutableStateOf(false) }
    // 分类与搜索（按来源/状态筛选 + 任务名搜索）
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var statusFilter by rememberSaveable { mutableStateOf(DownloadStatusFilter.ALL) }
    var sourceFilter by rememberSaveable { mutableStateOf<String?>(null) } // null=全部来源
    // 统计页入口
    var showStats by rememberSaveable { mutableStateOf(false) }

    // 过滤后的任务列表（搜索 + 状态 + 来源）
    val filteredTasks = remember(tasks, searchQuery, statusFilter, sourceFilter) {
        val query = searchQuery.trim()
        tasks.filter { task ->
            if (query.isNotBlank() && !task.fileName.contains(query, ignoreCase = true)) {
                return@filter false
            }
            when (statusFilter) {
                DownloadStatusFilter.ALL -> {}
                DownloadStatusFilter.ACTIVE -> if (
                    task.status != DownloadTaskEntity.STATUS_DOWNLOADING &&
                    task.status != DownloadTaskEntity.STATUS_PENDING
                ) return@filter false
                DownloadStatusFilter.COMPLETED -> if (task.status != DownloadTaskEntity.STATUS_COMPLETED) return@filter false
                DownloadStatusFilter.PAUSED -> if (task.status != DownloadTaskEntity.STATUS_PAUSED) return@filter false
                DownloadStatusFilter.FAILED -> if (task.status != DownloadTaskEntity.STATUS_FAILED) return@filter false
            }
            when (sourceFilter) {
                null -> {}
                DownloadPlatform.GENERIC -> if (task.platform.isNotBlank() && task.platform != DownloadPlatform.GENERIC) return@filter false
                else -> if (task.platform != sourceFilter) return@filter false
            }
            true
        }
    }
    // 下载统计摘要（今日/本周下载量 + 成功率）
    val summary = remember(tasks) { computeDownloadStats(tasks) }
    // 任务中出现的下载来源平台（分类筛选 Chips 用）
    val availableSources = remember(tasks) {
        tasks.map { it.platform }.filter { it.isNotBlank() }.distinct()
    }

    // Android 9- 写公共目录需要 WRITE_EXTERNAL_STORAGE
    val needLegacyPermission = Build.VERSION.SDK_INT < Build.VERSION_CODES.Q
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) showAddDialog = true
        else SnackbarController.show("需要存储权限才能保存到下载目录")
    }
    val hasPermission = remember {
        if (needLegacyPermission) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) ==
                PackageManager.PERMISSION_GRANTED
        } else true
    }

    IosScreenBackground {
        Box(modifier = modifier.fillMaxSize()) {
            if (tasks.isEmpty()) {
                IosEmptyDownloadState(modifier = Modifier.align(Alignment.Center))
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    // 实时速度 + 统计概览（合并为单一液态玻璃卡，消除多个框体间的割裂感；点击统计行进入统计页）
                    DownloadOverviewCard(
                        speedHistory = speedHistory,
                        summary = summary,
                        onStatsClick = { showStats = true }
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    // 任务名搜索
                    DownloadSearchField(
                        query = searchQuery,
                        onQueryChange = { searchQuery = it },
                        onClear = {
                            searchQuery = ""
                            statusFilter = DownloadStatusFilter.ALL
                            sourceFilter = null
                        }
                    )
                    Spacer(modifier = Modifier.height(9.dp))
                    // 状态 / 来源筛选（极简软色胶囊，不突出）
                    DownloadFilterChips(
                        statusFilter = statusFilter,
                        sourceFilter = sourceFilter,
                        sources = availableSources,
                        onStatusSelected = { statusFilter = it },
                        onSourceSelected = { sourceFilter = it }
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    // 批量操作：行内文本动作（无卡片框体）
                    IosDownloadBatchBar(
                        hasActive = tasks.any {
                            it.status == DownloadTaskEntity.STATUS_DOWNLOADING ||
                                it.status == DownloadTaskEntity.STATUS_PENDING
                        },
                        hasResumable = tasks.any {
                            it.status == DownloadTaskEntity.STATUS_PAUSED ||
                                it.status == DownloadTaskEntity.STATUS_FAILED
                        },
                        onPauseAll = { viewModel.pauseAll() },
                        onResumeAll = { viewModel.resumeAll() },
                        onDeleteAll = { showDeleteAllConfirm = true }
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    // 筛选 / 来源切换：列表整体淡入淡出 + 轻微纵向滑动（原生弹簧），避免内容突变导致跳动
                    AnimatedContent(
                        targetState = statusFilter to sourceFilter,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        transitionSpec = {
                            (fadeIn(NativeSpringSoft) + slideInVertically(NativeSpringIntOffset) { it / 12 })
                                .togetherWith(fadeOut(NativeSpringSoft) + slideOutVertically(NativeSpringIntOffset) { -it / 12 })
                        },
                        label = "downloadFilteredList"
                    ) {
                        DownloadTaskList(
                            filteredTasks = filteredTasks,
                            stats = stats,
                            onPause = { viewModel.pause(it) },
                            onResume = { viewModel.resume(it) },
                            onRemove = { pendingDelete = it },
                            onRedownload = { viewModel.redownload(it) }
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        IosAddDownloadDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { url, name ->
                showAddDialog = false
                viewModel.enqueue(url, name)
            }
        )
    }

    // 删除二次确认（可选同时删除本地文件）
    pendingDelete?.let { task ->
        IosDeleteConfirmDialog(
            task = task,
            onDismiss = { pendingDelete = null },
            onConfirm = { deleteLocal ->
                pendingDelete = null
                viewModel.remove(task.id, deleteLocal)
            }
        )
    }

    // 删除全部任务二次确认（可选同时删除本地文件）
    if (showDeleteAllConfirm) {
        var deleteAllLocal by remember { mutableStateOf(false) }
        val hasCompletedFile = tasks.any {
            it.status == DownloadTaskEntity.STATUS_COMPLETED && it.savePath.isNotBlank()
        }
        IosAlertDialog(
            onDismissRequest = { showDeleteAllConfirm = false },
            title = "删除全部任务",
            message = "确定删除所有下载任务吗？删除后任务记录将被清除，且不可恢复。",
            confirmText = "全部删除",
            confirmStyle = IosButtonStyle.Destructive,
            onConfirm = {
                showDeleteAllConfirm = false
                viewModel.removeAll(deleteAllLocal)
            },
            dismissText = "取消",
            onDismiss = { showDeleteAllConfirm = false }
        ) {
            if (hasCompletedFile) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IosCheckBoxRow(
                        checked = deleteAllLocal,
                        onCheckedChange = { deleteAllLocal = it },
                        text = "同时删除本地文件",
                        caption = "勾选后将一并删除所有已下载到 Download 目录的文件，且不可恢复。"
                    )
                }
            }
        }
    }

    // 统计页（功能 6）：今日/本周下载量、成功率、网盘配额
    if (showStats) {
        StatsScreen(
            onBack = { showStats = false },
            summary = summary,
            driveQuotaViewModel = driveQuotaViewModel
        )
    }
}

/** iOS 检查行（圆点/勾选 + 文本 + 可选说明），用于弹窗内选项 */
@Composable
private fun IosCheckBoxRow(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    text: String,
    caption: String? = null
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val bg by animateColorAsState(
        targetValue = if (pressed) iosPressColor() else Color.Transparent,
        animationSpec = NativeSpringColorSoft,
        label = "iosCheckRowBg"
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .clickable(
                interactionSource = interaction,
                indication = null
            ) { onCheckedChange(!checked) }
            .padding(horizontal = 4.dp, vertical = 6.dp),
        verticalAlignment = Alignment.Top
    ) {
        // iOS 勾选圆钮
        Box(
            modifier = Modifier
                .padding(top = 1.dp)
                .size(22.dp)
                .clip(CircleShape)
                .background(if (checked) IosBlue else Color(0xFFE9E9EB)),
            contentAlignment = Alignment.Center
        ) {
            if (checked) {
                Icon(
                    imageVector = Icons.Outlined.CheckCircle,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = text,
                fontSize = 15.sp,
                color = iosLabelColor()
            )
            caption?.let {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = it,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    color = iosSecondaryLabelColor()
                )
            }
        }
    }
}

/** iOS 批量操作条：极简行内文本动作（无卡片框体，与列表同列对齐），按压淡灰高亮 */
@Composable
private fun IosDownloadBatchBar(
    hasActive: Boolean,
    hasResumable: Boolean,
    onPauseAll: () -> Unit,
    onResumeAll: () -> Unit,
    onDeleteAll: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IosBatchAction(
            icon = Icons.Outlined.Pause,
            text = "全部暂停",
            enabled = hasActive,
            tint = IosBlue,
            onClick = onPauseAll
        )
        Spacer(modifier = Modifier.width(4.dp))
        IosBatchAction(
            icon = Icons.Outlined.PlayArrow,
            text = "全部开始",
            enabled = hasResumable,
            tint = IosBlue,
            onClick = onResumeAll
        )
        Spacer(modifier = Modifier.weight(1f))
        IosBatchAction(
            icon = Icons.Outlined.Delete,
            text = "删除全部",
            enabled = true,
            tint = IosRed,
            onClick = onDeleteAll
        )
    }
}

@Composable
private fun IosBatchAction(
    icon: ImageVector,
    text: String,
    enabled: Boolean,
    tint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val bg by animateColorAsState(
        targetValue = if (pressed && enabled) iosPressColor() else Color.Transparent,
        animationSpec = NativeSpringColorSoft,
        label = "iosBatchBg"
    )
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .clickable(
                enabled = enabled,
                interactionSource = interaction,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 8.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint.copy(alpha = if (enabled) 1f else 0.35f),
            modifier = Modifier.size(15.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = text,
            fontSize = 13.sp,
            color = tint.copy(alpha = if (enabled) 1f else 0.35f)
        )
    }
}

/** iOS 空状态：圆形图标 + 标题 + 副标题 */
@Composable
private fun IosEmptyDownloadState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(IosBlue.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.Download,
                contentDescription = null,
                modifier = Modifier.size(28.dp),
                tint = IosBlue
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "暂无下载任务",
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold,
            color = iosLabelColor()
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "解析分享后点击文件即可加入下载队列\n也可点击右下角按钮手动添加",
            fontSize = 14.sp,
            lineHeight = 20.sp,
            color = iosSecondaryLabelColor(),
            textAlign = TextAlign.Center
        )
    }
}

/** 下载任务列表：根目录任务 + 文件夹分组（供 AnimatedContent 筛选切换过渡） */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DownloadTaskList(
    filteredTasks: List<DownloadTaskEntity>,
    stats: Map<Long, DownloadStats>,
    onPause: (Long) -> Unit,
    onResume: (Long) -> Unit,
    onRemove: (DownloadTaskEntity) -> Unit,
    onRedownload: (DownloadTaskEntity) -> Unit
) {
    // 单遍分区：根目录任务（无路径分隔符）/ 文件夹任务（按顶级目录分组），
    // 仅在筛选结果变化时重算（进度每秒刷新，避免每次组合重复扫描全列表两次）
    val (rootTasks, folderGroups) = remember(filteredTasks) {
        val (root, folder) = filteredTasks.partition { !it.fileName.contains('/') }
        root to folder.groupBy { it.fileName.substringBefore('/') }
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (filteredTasks.isEmpty()) {
            // 筛选无结果提示
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 40.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Search,
                        contentDescription = null,
                        tint = IosGray3,
                        modifier = Modifier.size(40.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "没有匹配的任务",
                        fontSize = 15.sp,
                        color = iosSecondaryLabelColor()
                    )
                }
            }
        } else {
            items(rootTasks, key = { it.id }) { task ->
                IosDownloadTaskCard(
                    task = task,
                    stats = stats[task.id],
                    onPause = { onPause(task.id) },
                    onResume = { onResume(task.id) },
                    onRemove = { onRemove(task) },
                    onRedownload = { onRedownload(task) },
                    // 任务插入/移除/状态变化时原位动画（iOS 列表动效）
                    modifier = Modifier.animateItem()
                )
            }

            folderGroups.forEach { (folder, groupTasks) ->
                item(key = "folder_$folder") {
                    IosFolderDownloadGroup(
                        folder = folder,
                        tasks = groupTasks,
                        stats = stats,
                        onPause = onPause,
                        onResume = onResume,
                        onRemove = onRemove,
                        onRedownload = onRedownload,
                        modifier = Modifier.animateItem()
                    )
                }
            }
        }
    }
}

/** iOS 任务卡：白色圆角卡 + 图标块 + 状态 + 进度条 + 操作 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun IosDownloadTaskCard(
    task: DownloadTaskEntity,
    stats: DownloadStats?,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onRemove: () -> Unit,
    onRedownload: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isDownloading = task.status == DownloadTaskEntity.STATUS_DOWNLOADING ||
        task.status == DownloadTaskEntity.STATUS_PENDING
    val fraction = if (task.totalSize > 0) {
        (task.downloadedSize.toFloat() / task.totalSize).coerceIn(0f, 1f)
    } else 0f
    // 长按任务卡弹出操作菜单（复制直链 / 重新下载 / 删除）
    var showMenu by remember { mutableStateOf(false) }
    val (tintBlue) = iosTint(IosBlue)
    val (tintGreen) = iosTint(IosGreen)
    val (tintRed) = iosTint(IosRed)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(iosCardColor())
            .combinedClickable(
                onClick = {},
                onLongClick = { showMenu = true }
            )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IosIconTileBox(
                    icon = Icons.Outlined.InsertDriveFile,
                    background = tintBlue,
                    tint = IosBlue,
                    size = 40,
                    iconSize = 20
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = task.fileName,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Normal,
                        color = iosLabelColor(),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    val statusColor = when (task.status) {
                        DownloadTaskEntity.STATUS_FAILED -> IosRed
                        DownloadTaskEntity.STATUS_COMPLETED -> IosGreen
                        else -> iosSecondaryLabelColor()
                    }
                    Text(
                        text = taskStatusLine(task),
                        fontSize = 13.sp,
                        color = statusColor,
                        maxLines = 1
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                // 主操作：iOS 圆形图标按钮
                when (task.status) {
                    DownloadTaskEntity.STATUS_DOWNLOADING,
                    DownloadTaskEntity.STATUS_PENDING -> IosCircleButton(
                        icon = Icons.Outlined.Pause,
                        tint = IosBlue,
                        contentDescription = "暂停",
                        onClick = onPause
                    )
                    DownloadTaskEntity.STATUS_PAUSED -> IosCircleButton(
                        icon = Icons.Outlined.PlayArrow,
                        tint = IosBlue,
                        contentDescription = "继续",
                        onClick = onResume
                    )
                    DownloadTaskEntity.STATUS_FAILED -> IosCircleButton(
                        icon = Icons.Outlined.Refresh,
                        tint = IosRed,
                        contentDescription = "重试",
                        onClick = onResume
                    )
                    DownloadTaskEntity.STATUS_COMPLETED -> Row {
                        // APK 文件：额外显示「安装」按钮
                        if (task.fileName.endsWith(".apk", true)) {
                            IosCircleButton(
                                icon = Icons.Outlined.SystemUpdate,
                                tint = IosBlue,
                                contentDescription = "安装",
                                onClick = { installApk(context, task.savePath, task.fileName) }
                            )
                        }
                        IosCircleButton(
                            icon = Icons.Outlined.OpenInNew,
                            tint = IosBlue,
                            contentDescription = "打开",
                            onClick = { openSavedFile(context, task.savePath) }
                        )
                    }
                }
            }

            // 失败原因（红色小字展示具体错误）
            if (task.status == DownloadTaskEntity.STATUS_FAILED && task.errorMsg.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "失败原因：${task.errorMsg}",
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    color = IosRed,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // 实时统计 + 进度条：完成态整体折叠
            AnimatedVisibility(
                visible = task.status != DownloadTaskEntity.STATUS_COMPLETED,
                enter = expandVertically(NativeSpringIntSize, expandFrom = Alignment.Top) + fadeIn(NativeSpringSoft),
                exit = shrinkVertically(NativeSpringIntSize, shrinkTowards = Alignment.Top) + fadeOut(NativeSpringSoft)
            ) {
                Column {
                    Spacer(modifier = Modifier.height(10.dp))
                    if (isDownloading && stats != null && stats.speed > 0) {
                        Text(
                            text = "${formatSpeed(stats.speed)} · 剩余 ${formatRemain(stats.remainMillis)} · ${stats.chunkCount} 线程",
                            fontSize = 12.sp,
                            color = IosBlue
                        )
                        Spacer(modifier = Modifier.height(5.dp))
                    }
                    IosProgressBar(
                        progress = fraction,
                        color = when (task.status) {
                            DownloadTaskEntity.STATUS_FAILED -> IosRed
                            else -> IosBlue
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 底部行：进度信息 + 删除
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (task.status == DownloadTaskEntity.STATUS_COMPLETED) {
                        if (task.avgSpeed > 0) {
                            "平均 ${formatSpeed(task.avgSpeed)} · ${formatSize(task.totalSize)}"
                        } else {
                            formatSize(task.totalSize)
                        }
                    } else {
                        progressText(task)
                    },
                    fontSize = 12.sp,
                    color = iosSecondaryLabelColor(),
                    modifier = Modifier.weight(1f)
                )
                IosTextAction(
                    text = "删除",
                    tint = IosRed,
                    icon = Icons.Outlined.Delete,
                    onClick = onRemove
                )
            }
        }
    }

    // 长按任务卡弹出操作菜单（复制直链 / 重新下载 / 删除）
    if (showMenu) {
        IosDownloadActionSheet(
            title = task.fileName,
            onDismiss = { showMenu = false },
            onCopy = {
                showMenu = false
                copyToClipboard(context, task.url)
                SnackbarController.show("直链已复制")
            },
            onRedownload = {
                showMenu = false
                onRedownload()
            },
            onDelete = {
                showMenu = false
                onRemove()
            }
        )
    }
}

/** iOS 圆形图标按钮：淡蓝底 + 图标，按压缩放 */
@Composable
private fun IosCircleButton(
    icon: ImageVector,
    tint: Color,
    contentDescription: String,
    onClick: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.88f else 1f,
        animationSpec = NativeSpring,
        label = "iosCircleScale"
    )
    val bg by animateColorAsState(
        targetValue = if (pressed) tint.copy(alpha = 0.24f) else tint.copy(alpha = 0.12f),
        animationSpec = NativeSpringColorSoft,
        label = "iosCircleBg"
    )
    Box(
        modifier = Modifier
            .size(36.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(CircleShape)
            .background(bg)
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(19.dp)
        )
    }
}

/** iOS 文字动作按钮（如「删除」） */
@Composable
private fun IosTextAction(
    text: String,
    tint: Color,
    icon: ImageVector?,
    onClick: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val bg by animateColorAsState(
        targetValue = if (pressed) iosPressColor() else Color.Transparent,
        animationSpec = NativeSpringColorSoft,
        label = "iosTextActionBg"
    )
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bg)
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 6.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        icon?.let {
            Icon(
                imageVector = it,
                contentDescription = null,
                modifier = Modifier.size(13.dp),
                tint = tint
            )
            Spacer(modifier = Modifier.width(3.dp))
        }
        Text(
            text = text,
            fontSize = 14.sp,
            color = tint
        )
    }
}

/** iOS 任务图标块（圆角方块） */
@Composable
private fun IosIconTileBox(
    icon: ImageVector,
    background: Color,
    tint: Color,
    size: Int,
    iconSize: Int
) {
    Box(
        modifier = Modifier
            .size(size.dp)
            .clip(RoundedCornerShape((size / 4.1f).dp))
            .background(background),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(iconSize.dp)
        )
    }
}

/** iOS 文件夹下载组：同一「顶级目录」下的所有任务合并为一个可展开白色卡片 */
@Composable
private fun IosFolderDownloadGroup(
    folder: String,
    tasks: List<DownloadTaskEntity>,
    stats: Map<Long, DownloadStats>,
    onPause: (Long) -> Unit,
    onResume: (Long) -> Unit,
    onRemove: (DownloadTaskEntity) -> Unit,
    onRedownload: (DownloadTaskEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(true) }
    val completed = tasks.count { it.status == DownloadTaskEntity.STATUS_COMPLETED }
    val totalSize = tasks.sumOf { it.totalSize }
    // 聚合显示钳制：任何单项竞态残留都不会让"已下载 > 总大小"
    val downloaded = minOf(tasks.sumOf { it.downloadedSize }, totalSize)
    val fraction = if (totalSize > 0) {
        (downloaded.toFloat() / totalSize).coerceIn(0f, 1f)
    } else 0f
    val done = completed == tasks.size
    val (tintBlue) = iosTint(IosBlue)
    val (tintGreen) = iosTint(IosGreen)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(iosCardColor())
    ) {
        // 头部：点击展开/收起（按压高亮）
        val interaction = remember { MutableInteractionSource() }
        val pressed by interaction.collectIsPressedAsState()
        val headBg by animateColorAsState(
            targetValue = if (pressed) iosPressColor() else Color.Transparent,
            animationSpec = NativeSpringColorSoft,
            label = "iosFolderHeadBg"
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(headBg)
                .clickable(
                    interactionSource = interaction,
                    indication = null
                ) { expanded = !expanded }
                .padding(start = 14.dp, end = 12.dp, top = 12.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IosIconTileBox(
                icon = Icons.Outlined.Folder,
                background = tintGreen,
                tint = IosGreen,
                size = 40,
                iconSize = 21
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = folder,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Normal,
                    color = iosLabelColor(),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${completed}/${tasks.size} 个文件 · ${formatSize(downloaded)} / ${formatSize(totalSize)}",
                    fontSize = 13.sp,
                    color = iosSecondaryLabelColor(),
                    maxLines = 1
                )
            }
            // 总体进度徽标
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(if (done) tintGreen else Color(0xFFE9E9EB))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = if (done) "已完成" else "${(fraction * 100).toInt()}%",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (done) IosGreen else iosSecondaryLabelColor()
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                imageVector = if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                contentDescription = if (expanded) "收起" else "展开",
                tint = IosGray3,
                modifier = Modifier.size(18.dp)
            )
        }

        // 展开区：总体进度条 + 子任务紧凑列表
        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(NativeSpringIntSize, expandFrom = Alignment.Top) + fadeIn(NativeSpringSoft),
            exit = shrinkVertically(NativeSpringIntSize, shrinkTowards = Alignment.Top) + fadeOut(NativeSpringSoft)
        ) {
            Column {
                // 总体进度条（已完成时隐藏）
                if (!done) {
                    IosProgressBar(
                        progress = fraction,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                // 子任务列表（紧凑行，含子文件夹内文件）
                Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp)) {
                    tasks.forEachIndexed { index, task ->
                        IosDownloadSubTaskRow(
                            task = task,
                            stats = stats[task.id],
                            showDivider = index < tasks.size - 1,
                            onPause = { onPause(task.id) },
                            onResume = { onResume(task.id) },
                            onRemove = { onRemove(task) },
                            onRedownload = { onRedownload(task) }
                        )
                    }
                }
            }
        }
    }
}

/** iOS 文件夹组内子任务紧凑行：相对路径 + 状态 + 进度条 + 操作按钮 */
@Composable
private fun IosDownloadSubTaskRow(
    task: DownloadTaskEntity,
    stats: DownloadStats?,
    showDivider: Boolean,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onRemove: () -> Unit,
    onRedownload: () -> Unit
) {
    val context = LocalContext.current
    val isDownloading = task.status == DownloadTaskEntity.STATUS_DOWNLOADING ||
        task.status == DownloadTaskEntity.STATUS_PENDING
    val fraction = if (task.totalSize > 0) {
        (task.downloadedSize.toFloat() / task.totalSize).coerceIn(0f, 1f)
    } else 0f
    // 显示相对路径（去掉顶级目录前缀，如 "A/B/b.mp4" → "B/b.mp4"）
    val displayName = task.fileName.substringAfter('/')
    // 长按任务行弹出操作菜单
    var showMenu by remember { mutableStateOf(false) }
    val (tintBlue) = iosTint(IosBlue)
    val (tintRed) = iosTint(IosRed)

    Column(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .combinedClickable(
                    onClick = {},
                    onLongClick = { showMenu = true }
                )
                .padding(vertical = 8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IosIconTileBox(
                    icon = Icons.Outlined.InsertDriveFile,
                    background = tintBlue,
                    tint = IosBlue,
                    size = 32,
                    iconSize = 16
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = displayName,
                        fontSize = 15.sp,
                        color = iosLabelColor(),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = when {
                            isDownloading && stats != null && stats.speed > 0 ->
                                "${DownloadTaskEntity.statusText(task.status)} · ${formatSpeed(stats.speed)}"
                            task.status == DownloadTaskEntity.STATUS_COMPLETED && task.avgSpeed > 0 ->
                                "${DownloadTaskEntity.statusText(task.status)} · 平均 ${formatSpeed(task.avgSpeed)}"
                            else -> taskStatusLine(task)
                        },
                        fontSize = 12.sp,
                        color = if (task.status == DownloadTaskEntity.STATUS_FAILED) {
                            IosRed
                        } else {
                            iosSecondaryLabelColor()
                        },
                        maxLines = 1
                    )
                }
                // 主操作（暂停/继续/重试/打开）
                when (task.status) {
                    DownloadTaskEntity.STATUS_DOWNLOADING,
                    DownloadTaskEntity.STATUS_PENDING -> IosMiniCircleButton(
                        icon = Icons.Outlined.Pause,
                        tint = IosBlue,
                        contentDescription = "暂停",
                        onClick = onPause
                    )
                    DownloadTaskEntity.STATUS_PAUSED -> IosMiniCircleButton(
                        icon = Icons.Outlined.PlayArrow,
                        tint = IosBlue,
                        contentDescription = "继续",
                        onClick = onResume
                    )
                    DownloadTaskEntity.STATUS_FAILED -> IosMiniCircleButton(
                        icon = Icons.Outlined.Refresh,
                        tint = IosRed,
                        contentDescription = "重试",
                        onClick = onResume
                    )
                    DownloadTaskEntity.STATUS_COMPLETED -> IosMiniCircleButton(
                        icon = Icons.Outlined.OpenInNew,
                        tint = IosBlue,
                        contentDescription = "打开",
                        onClick = { openSavedFile(context, task.savePath) }
                    )
                }
                // 删除
                IosMiniCircleButton(
                    icon = Icons.Outlined.Delete,
                    tint = IosRed,
                    contentDescription = "删除",
                    onClick = onRemove
                )
            }
            // 细进度条（完成态折叠，带过渡动画）
            AnimatedVisibility(
                visible = task.status != DownloadTaskEntity.STATUS_COMPLETED,
                enter = expandVertically(NativeSpringIntSize) + fadeIn(NativeSpringSoft),
                exit = shrinkVertically(NativeSpringIntSize) + fadeOut(NativeSpringSoft)
            ) {
                IosProgressBar(
                    progress = fraction,
                    modifier = Modifier.padding(top = 6.dp),
                    color = if (task.status == DownloadTaskEntity.STATUS_FAILED) IosRed else IosBlue,
                    height = 2.5.dp
                )
            }
        }
        if (showDivider) {
            Box(
                modifier = Modifier
                    .padding(start = 42.dp)
                    .height(0.5.dp)
                    .fillMaxWidth()
                    .background(iosSeparatorColor())
            )
        }
    }

    // 长按任务行弹出操作菜单
    if (showMenu) {
        IosDownloadActionSheet(
            title = displayName,
            onDismiss = { showMenu = false },
            onCopy = {
                showMenu = false
                copyToClipboard(context, task.url)
                SnackbarController.show("直链已复制")
            },
            onRedownload = {
                showMenu = false
                onRedownload()
            },
            onDelete = {
                showMenu = false
                onRemove()
            }
        )
    }
}

/** iOS 底部操作菜单（长按任务弹出）：复制直链 / 重新下载 / 删除 */
@Composable
private fun IosDownloadActionSheet(
    title: String,
    onDismiss: () -> Unit,
    onCopy: () -> Unit,
    onRedownload: () -> Unit,
    onDelete: () -> Unit
) {
    val (tintBlue) = iosTint(IosBlue)
    val (tintOrange) = iosTint(IosOrange)
    val (tintRed) = iosTint(IosRed)
    IosBottomSheet(onDismissRequest = onDismiss) {
        IosSheetTitle(text = title, subtitle = "选择操作")
        Spacer(modifier = Modifier.height(14.dp))
        IosActionRow(
            icon = Icons.Outlined.ContentCopy,
            iconBackground = tintBlue,
            iconTint = IosBlue,
            title = "复制直链",
            onClick = onCopy,
            showDivider = true
        )
        IosActionRow(
            icon = Icons.Outlined.Refresh,
            iconBackground = tintOrange,
            iconTint = IosOrange,
            title = "重新下载",
            onClick = onRedownload,
            showDivider = true
        )
        IosActionRow(
            icon = Icons.Outlined.Delete,
            iconBackground = tintRed,
            iconTint = IosRed,
            title = "删除任务",
            onClick = onDelete
        )
        Spacer(modifier = Modifier.height(4.dp))
    }
}

@Composable
private fun IosMiniCircleButton(
    icon: ImageVector,
    tint: Color,
    contentDescription: String,
    onClick: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.86f else 1f,
        animationSpec = NativeSpring,
        label = "iosMiniScale"
    )
    Box(
        modifier = Modifier
            .size(32.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(CircleShape)
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(18.dp)
        )
    }
}

/** iOS 删除确认弹窗：可勾选「同时删除本地文件」 */
@Composable
private fun IosDeleteConfirmDialog(
    task: DownloadTaskEntity,
    onDismiss: () -> Unit,
    onConfirm: (deleteLocal: Boolean) -> Unit
) {
    var deleteLocal by remember { mutableStateOf(false) }
    val hasLocalFile = task.status == DownloadTaskEntity.STATUS_COMPLETED && task.savePath.isNotBlank()
    val hint = when {
        hasLocalFile -> "勾选后将一并删除已下载到 Download 目录的文件，且不可恢复。"
        task.status == DownloadTaskEntity.STATUS_COMPLETED -> "该任务没有已完成的本地文件。"
        else -> "该任务尚未完成，删除后将同时清除已下载的临时文件，且不可恢复。"
    }
    IosAlertDialog(
        onDismissRequest = onDismiss,
        title = "删除下载任务",
        message = "确定删除「${task.fileName}」吗？",
        confirmText = "删除",
        confirmStyle = IosButtonStyle.Destructive,
        onConfirm = { onConfirm(deleteLocal) },
        dismissText = "取消",
        onDismiss = onDismiss
    ) {
        if (hasLocalFile) {
            IosCheckBoxRow(
                checked = deleteLocal,
                onCheckedChange = { deleteLocal = it },
                text = "同时删除本地文件",
                caption = hint
            )
        } else {
            Text(
                text = hint,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                color = iosSecondaryLabelColor(),
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }
    }
}

/** iOS 添加下载任务弹窗 */
@Composable
private fun IosAddDownloadDialog(
    onDismiss: () -> Unit,
    onConfirm: (url: String, name: String) -> Unit
) {
    var url by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    IosAlertDialog(
        onDismissRequest = onDismiss,
        title = "添加下载任务",
        message = "下载文件将保存到 ${Environment.DIRECTORY_DOWNLOADS} 目录",
        confirmText = "开始下载",
        onConfirm = { onConfirm(url.trim(), name.trim()) },
        dismissText = "取消",
        onDismiss = onDismiss
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            IosTextField(
                value = url,
                onValueChange = {
                    url = it
                    if (name.isBlank()) name = it.substringAfterLast('/').take(80)
                },
                placeholder = "文件直链 URL"
            )
            IosTextField(
                value = name,
                onValueChange = { name = it },
                placeholder = "保存文件名"
            )
        }
    }
}

private fun copyToClipboard(context: Context, text: String) {
    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    cm.setPrimaryClip(ClipData.newPlainText("yunx_url", text))
}

private fun taskStatusLine(task: DownloadTaskEntity): String {
    val status = DownloadTaskEntity.statusText(task.status)
    return if (task.totalSize > 0) {
        // 显示值钳制到 total（防恢复竞态残留导致显示超总大小）
        val shown = minOf(task.downloadedSize, task.totalSize)
        "$status · ${formatSize(shown)} / ${formatSize(task.totalSize)}"
    } else {
        status
    }
}

private fun progressText(task: DownloadTaskEntity): String {
    if (task.totalSize <= 0) return ""
    // 显示值钳制到 total（防恢复竞态残留导致显示超总大小）
    val shown = minOf(task.downloadedSize, task.totalSize)
    val percent = (shown * 100 / task.totalSize).toInt().coerceIn(0, 100)
    return "已下载 ${formatSize(shown)} / ${formatSize(task.totalSize)} · $percent%"
}

private fun formatSize(bytes: Long): String = iosFormatBytes(bytes)

private fun formatSpeed(bytesPerSec: Long): String = iosFormatSpeed(bytesPerSec)

private fun formatRemain(millis: Long): String {
    if (millis < 0) return "计算中"
    val sec = millis / 1000
    return when {
        sec < 60 -> "${sec}秒"
        sec < 3600 -> "${sec / 60}分${sec % 60}秒"
        else -> "${sec / 3600}时${(sec % 3600) / 60}分"
    }
}

private fun openSavedFile(context: Context, savePath: String) {
    if (savePath.isBlank()) return
    val uri = if (savePath.startsWith("content://")) {
        Uri.parse(savePath)
    } else {
        // Android 7.0+ 禁止暴露 file:// URI，必须经 FileProvider 转 content://
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", File(savePath))
    }
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, "*/*")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    runCatching {
        context.startActivity(Intent.createChooser(intent, "打开文件"))
    }.onFailure {
        SnackbarController.show("无法打开该文件")
    }
}

/** 安装 APK：检查「安装未知来源应用」权限（Android 8+），ACTION_VIEW 调起系统安装器 */
private fun installApk(context: Context, savePath: String, fileName: String) {
    if (savePath.isBlank()) {
        SnackbarController.show("文件不存在")
        return
    }
    // Android 8+：需先授予「安装未知来源应用」
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
        !context.packageManager.canRequestPackageInstalls()
    ) {
        SnackbarController.show("请先允许安装未知来源应用")
        val intent = Intent(
            Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
            Uri.parse("package:${context.packageName}")
        )
        runCatching { context.startActivity(intent) }.onFailure {
            SnackbarController.show("无法打开设置")
        }
        return
    }
    val uri = if (savePath.startsWith("content://")) {
        Uri.parse(savePath)
    } else {
        // Android 7.0+ 禁止暴露 file:// URI，必须经 FileProvider 转 content://
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", File(savePath))
    }
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, "application/vnd.android.package-archive")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    runCatching {
        context.startActivity(Intent.createChooser(intent, "安装应用"))
    }.onFailure {
        SnackbarController.show("无法打开安装器")
    }
}

/* ================================================================
 * 以下为功能 4/7 配套组件：状态过滤、实时速度图表、统计概览入口
 * ================================================================ */

/** 下载状态筛选（按状态分组筛选） */
private enum class DownloadStatusFilter(val label: String) {
    ALL("全部"),
    ACTIVE("进行中"),
    COMPLETED("已完成"),
    PAUSED("已暂停"),
    FAILED("失败");
}

/** 下载统计摘要（今日/本周下载量 + 成功率）：统计页与下载页概览条共用 */
internal data class DownloadSummary(
    val todayCount: Int,
    val weekCount: Int,
    val successCount: Int,
    val totalCount: Int,
    val failCount: Int
)

/** 计算下载统计：今日/本周完成量、总体成功率 */
internal fun computeDownloadStats(tasks: List<DownloadTaskEntity>): DownloadSummary {
    val todayStart = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }.timeInMillis
    // 本周起点：周一 00:00（自然周，而非滚动 7 天）
    val weekStart = Calendar.getInstance().apply {
        timeInMillis = todayStart
        var dayOfWeek = get(Calendar.DAY_OF_WEEK)
        if (dayOfWeek < Calendar.MONDAY) dayOfWeek += 7 // SUNDAY=1 需回退 6 天
        add(Calendar.DAY_OF_MONTH, -(dayOfWeek - Calendar.MONDAY))
    }.timeInMillis
    var todayCount = 0
    var weekCount = 0
    var successCount = 0
    var failCount = 0
    for (t in tasks) {
        when (t.status) {
            DownloadTaskEntity.STATUS_COMPLETED -> successCount++
            DownloadTaskEntity.STATUS_FAILED -> {
                failCount++
                continue
            }
        }
        if (t.completedAt > 0) {
            if (t.completedAt >= todayStart) todayCount++
            if (t.completedAt >= weekStart) weekCount++
        }
    }
    return DownloadSummary(
        todayCount = todayCount,
        weekCount = weekCount,
        successCount = successCount,
        totalCount = successCount + failCount,
        failCount = failCount
    )
}

/** 实时速度曲线（液态玻璃卡片）：下载页顶部，迷你速度曲线 */
/** 实时速度曲线（液态玻璃卡片）：下载页顶部，迷你速度曲线 */
/** 实时速度曲线（液态玻璃卡片）：下载页顶部，迷你速度曲线 */
/** 实时速度曲线（液态玻璃卡片）：下载页顶部，迷你速度曲线 */
/**
 * 下载概览卡片（液态玻璃单卡）：实时速度曲线 + 今日/本周/成功率统计。
 * 将原本独立的速度卡与统计卡合并为一个整体，消除框体间的割裂感；
 * 底部统计行为可点击入口，点击进入统计页。
 */
@Composable
private fun DownloadOverviewCard(
    speedHistory: List<Long>,
    summary: DownloadSummary,
    onStatsClick: () -> Unit
) {
    val backdrop = LocalGlassSurfaceBackdrop.current
    val panelBg = if (isIosDark()) Color(0x332C2C2E) else Color(0x99FFFFFF)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 8.dp)
            .surfaceLiquidGlass(
                backdrop = backdrop,
                shape = RoundedCornerShape(14.dp),
                surfaceBrush = Brush.linearGradient(
                    listOf(panelBg, panelBg.copy(alpha = 0.35f)),
                    start = Offset(0f, 0f),
                    end = Offset(0f, 600f)
                ),
                blurRadius = 12.dp,
                lensRadius = 24.dp,
                useLens = true
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            val latest = speedHistory.lastOrNull() ?: 0L
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.Leaderboard,
                    contentDescription = null,
                    tint = IosBlue,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "实时速度",
                    fontSize = 11.sp,
                    color = iosSecondaryLabelColor(),
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = if (latest > 0) formatSpeed(latest) else "待机中",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (latest > 0) IosBlue else iosSecondaryLabelColor()
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            SpeedLineChart(
                data = speedHistory,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(32.dp)
            )
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 6.dp),
                thickness = 0.5.dp,
                color = iosSeparatorColor()
            )
            // 统计行（点击进入统计页）
            val interaction = remember { MutableInteractionSource() }
            val pressed by interaction.collectIsPressedAsState()
            val statsBg by animateColorAsState(
                targetValue = if (pressed) iosPressColor() else Color.Transparent,
                animationSpec = NativeSpringColorSoft,
                label = "overviewStatsBg"
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(statsBg)
                    .clickable(
                        interactionSource = interaction,
                        indication = null,
                        onClick = onStatsClick
                    )
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                DownloadStatItem(title = "今日完成", value = summary.todayCount.toString(), color = IosBlue, modifier = Modifier.weight(1f))
                DownloadStatItem(title = "本周完成", value = summary.weekCount.toString(), color = IosGreen, modifier = Modifier.weight(1f))
                val successRate = if (summary.totalCount > 0) {
                    (summary.successCount * 100 / summary.totalCount)
                } else 0
                DownloadStatItem(title = "成功率", value = "$successRate%", color = IosOrange, modifier = Modifier.weight(1f))
                Icon(
                    imageVector = Icons.Outlined.ChevronRight,
                    contentDescription = "查看统计",
                    tint = IosGray3,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

/** 迷你速度折线图：Canvas 绘制（面积渐变 + 折线） */
@Composable
private fun SpeedLineChart(data: List<Long>, modifier: Modifier = Modifier) {
    val lineColor = IosBlue
    val areaBrush = Brush.verticalGradient(
        colors = listOf(IosBlue.copy(alpha = 0.25f), IosBlue.copy(alpha = 0.02f))
    )
    Canvas(modifier = modifier) {
        if (data.isEmpty()) return@Canvas
        val maxV = (data.maxOrNull() ?: 1L).coerceAtLeast(1L)
        val stepX = size.width / 60f
        val padTop = 2.dp.toPx()
        val padBottom = 3.dp.toPx()
        val usableH = (size.height - padTop - padBottom).coerceAtLeast(1f)
        fun yOf(v: Long) = padTop + usableH * (1f - v.toFloat() / maxV)
        val visible = data.takeLast(60)
        val pts = visible.mapIndexed { i, v ->
            Offset(size.width - (visible.size - 1 - i) * stepX, yOf(v))
        }
        if (pts.size >= 2) {
            val areaPath = Path().apply {
                moveTo(pts.first().x, size.height)
                lineTo(pts.first().x, pts.first().y)
                for (p in pts.drop(1)) lineTo(p.x, p.y)
                lineTo(pts.last().x, size.height)
                close()
            }
            drawPath(areaPath, brush = areaBrush)
            val linePath = Path().apply {
                moveTo(pts.first().x, pts.first().y)
                for (p in pts.drop(1)) lineTo(p.x, p.y)
            }
            drawPath(
                path = linePath,
                color = lineColor,
                style = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
        }
    }
}

@Composable
private fun DownloadStatItem(
    title: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold,
            color = color
        )
        Spacer(modifier = Modifier.height(1.dp))
        Text(
            text = title,
            fontSize = 11.sp,
            color = iosSecondaryLabelColor()
        )
    }
}

/** 任务名搜索框（iOS 风格，搜索框自带一键清除，不额外增加会顶动布局的筛选清除按钮） */
@Composable
private fun DownloadSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit
) {
    IosSearchField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier.padding(horizontal = 16.dp),
        placeholder = "搜索任务名",
        onClear = onClear
    )
}

/** iOS 筛选 Chip（状态 / 来源通用）：极简软色胶囊，选中仅淡色底 + 系统蓝标注，不突兀 */
@Composable
private fun IosFilterChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val bg by animateColorAsState(
        targetValue = when {
            selected -> if (isIosDark()) IosBlue.copy(alpha = 0.28f) else IosBlue.copy(alpha = 0.13f)
            pressed -> iosPressColor()
            else -> Color.Transparent
        },
        animationSpec = NativeSpringColorSoft,
        label = "filterChipBg"
    )
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(bg)
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 11.dp, vertical = 5.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (selected) IosBlue else iosSecondaryLabelColor()
        )
    }
}

/** 状态 / 来源筛选 Chips 行（横向滚动） */
@Composable
private fun DownloadFilterChips(
    statusFilter: DownloadStatusFilter,
    sourceFilter: String?,
    sources: List<String>,
    onStatusSelected: (DownloadStatusFilter) -> Unit,
    onSourceSelected: (String?) -> Unit
) {
    val filteredSources = sources.take(4)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        DownloadStatusFilter.entries.forEach { s ->
            IosFilterChip(
                text = s.label,
                selected = statusFilter == s,
                // 点击其它状态直接切换；再次点击当前状态保持（不跳回「全部」，避免误触与逻辑跳变）
                onClick = { if (statusFilter != s) onStatusSelected(s) }
            )
        }
        if (filteredSources.isNotEmpty()) {
            IosFilterChip(
                text = "全部来源",
                selected = sourceFilter == null,
                onClick = { onSourceSelected(null) }
            )
            filteredSources.forEach { platform ->
                IosFilterChip(
                    text = platformName(platform),
                    selected = sourceFilter == platform,
                    onClick = { if (sourceFilter != platform) onSourceSelected(platform) }
                )
            }
        }
    }
}

private fun platformName(platform: String): String = when (platform) {
    DownloadPlatform.QUARK -> "夸克"
    DownloadPlatform.UC -> "UC"
    DownloadPlatform.XUNLEI -> "迅雷"
    DownloadPlatform.BAIDU -> "百度"
    DownloadPlatform.C139 -> "139"
    DownloadPlatform.PAN123 -> "123"
    else -> "其他"
}

/* ================================================================
 * 以下为功能 6 统计页：今日/本周下载量、成功率、网盘配额
 * ================================================================ */

/**
 * 统计页（功能 6）：今日/本周下载量、成功率、各网盘配额（复用 DriveQuotaViewModel）。
 * 独立的 iOS 页面：顶部返回、分组卡片统计。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun StatsScreen(
    onBack: () -> Unit,
    summary: DownloadSummary,
    driveQuotaViewModel: DriveQuotaViewModel
) {
    val quotaData = listOf(
        "夸克网盘" to driveQuotaViewModel.quarkQuota.collectAsState().value,
        "UC 网盘" to driveQuotaViewModel.ucQuota.collectAsState().value,
        "迅雷网盘" to driveQuotaViewModel.xunleiQuota.collectAsState().value,
        "百度网盘" to driveQuotaViewModel.baiduQuota.collectAsState().value,
        "139 网盘" to driveQuotaViewModel.c139Quota.collectAsState().value,
        "123 云盘" to driveQuotaViewModel.pan123Quota.collectAsState().value
    ).filter { it.second != null }.map { it.first to it.second!! }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = iosBackgroundColor(),
        topBar = {
            TopAppBar(
                title = { Text("下载统计", style = MaterialTheme.typography.titleLarge, color = iosLabelColor()) },
                navigationIcon = {
                    IosIconButton(
                        icon = Icons.AutoMirrored.Filled.ArrowBack,
                        tint = IosBlue,
                        onClick = onBack,
                        contentDescription = "返回"
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = iosBackgroundColor())
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // 下载概览
            IosSectionHeader("下载概览")
            IosGroupCard {
                Row(modifier = Modifier.padding(vertical = 14.dp)) {
                    DownloadStatItem(title = "今日完成", value = summary.todayCount.toString(), color = IosBlue, modifier = Modifier.weight(1f))
                        DownloadStatItem(title = "本周完成", value = summary.weekCount.toString(), color = IosGreen, modifier = Modifier.weight(1f))
                        val rate = if (summary.totalCount > 0) summary.successCount * 100 / summary.totalCount else 0
                        DownloadStatItem(title = "成功率", value = "$rate%", color = IosOrange, modifier = Modifier.weight(1f))
                }
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 14.dp),
                    thickness = 0.5.dp,
                    color = iosSeparatorColor()
                )
                Row(modifier = Modifier.padding(vertical = 12.dp)) {
                    DownloadStatItem(title = "成功", value = summary.successCount.toString(), color = IosGreen, modifier = Modifier.weight(1f))
                            DownloadStatItem(title = "失败", value = summary.failCount.toString(), color = IosRed, modifier = Modifier.weight(1f))
                            DownloadStatItem(title = "总任务", value = summary.totalCount.toString(), color = IosBlue, modifier = Modifier.weight(1f))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 网盘配额
            IosSectionHeader("网盘配额")
            val quotas = quotaData ?: emptyList()
            if (quotas.isEmpty()) {
                IosGroupCard {
                    Text(
                        text = "暂无已登录网盘，请在「网盘」页登录后查看空间配额",
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        color = iosSecondaryLabelColor(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp)
                    )
                }
            } else {
                IosGroupCard {
                    quotas.forEachIndexed { index, (name, q) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = name,
                                fontSize = 15.sp,
                                color = iosLabelColor(),
                                modifier = Modifier.width(84.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                val usedPct = if (q.total > 0) (q.used * 100 / q.total).toFloat() else 0f
                                IosProgressBar(
                                    progress = (usedPct / 100f).coerceIn(0f, 1f),
                                    color = if (usedPct >= 90f) IosRed else IosBlue,
                                    height = 6.dp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "${formatSize(q.used)} / ${formatSize(q.total)}",
                                    fontSize = 12.sp,
                                    color = iosSecondaryLabelColor()
                                )
                            }
                        }
                        if (index != quotas.size - 1) {
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 14.dp),
                                thickness = 0.5.dp,
                                color = iosSeparatorColor()
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // 首次进入自动刷新配额
    LaunchedEffect(Unit) { driveQuotaViewModel.loadAll() }
}
