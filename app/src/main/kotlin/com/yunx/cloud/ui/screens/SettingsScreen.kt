package com.yunx.cloud.ui.screens
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Article
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Power
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.SystemUpdate
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yunx.cloud.data.backup.AuthBackupManager
import com.yunx.cloud.data.backup.AuthCrypto
import com.yunx.cloud.data.download.DownloadPlatform
import com.yunx.cloud.data.download.DownloadSaver
import com.yunx.cloud.data.prefs.SettingsRepository
import com.yunx.cloud.data.update.UpdateChecker
import com.yunx.cloud.ui.SnackbarController
import com.yunx.cloud.ui.components.IosAlertDialog
import com.yunx.cloud.ui.components.IosBlue
import com.yunx.cloud.ui.components.IosButtonStyle
import com.yunx.cloud.ui.components.IosGray
import com.yunx.cloud.ui.components.IosGreen
import com.yunx.cloud.ui.components.IosGroupCard
import com.yunx.cloud.ui.components.IosIndigo
import com.yunx.cloud.ui.components.IosListRow
import com.yunx.cloud.ui.components.IosOrange
import com.yunx.cloud.ui.components.IosPink
import com.yunx.cloud.ui.components.IosPurple
import com.yunx.cloud.ui.components.IosRed
import com.yunx.cloud.ui.components.IosScreenBackground
import com.yunx.cloud.ui.components.IosSectionHeader
import com.yunx.cloud.ui.components.IosSwitch
import com.yunx.cloud.ui.components.IosTeal
import com.yunx.cloud.ui.components.IosPasswordField
import com.yunx.cloud.ui.components.IosTextField
import com.yunx.cloud.ui.components.iosLabelColor
import com.yunx.cloud.ui.components.iosSecondaryLabelColor
import com.yunx.cloud.util.LogExporter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** 可选的下载线程数档位（最高 512） */
private val threadOptions = listOf(1, 2, 4, 8, 16, 32, 64, 128, 256, 512)

/** 按平台下载线程数设置项 */
private data class ThreadPlatform(val platform: String, val label: String)

private val threadPlatforms = listOf(
    ThreadPlatform(DownloadPlatform.QUARK, "夸克网盘"),
    ThreadPlatform(DownloadPlatform.UC, "UC 网盘"),
    ThreadPlatform(DownloadPlatform.XUNLEI, "迅雷网盘"),
    ThreadPlatform(DownloadPlatform.BAIDU, "百度网盘"),
    ThreadPlatform(DownloadPlatform.C139, "139 网盘"),
    ThreadPlatform(DownloadPlatform.PAN123, "123 云盘"),
)

/** iOS 分组图标配色（浅色底 + 系统色） */
private fun iosTint(color: androidx.compose.ui.graphics.Color): Pair<androidx.compose.ui.graphics.Color, androidx.compose.ui.graphics.Color> =
    color.copy(alpha = 0.14f) to color

/**
 * 设置页：下载线程数设置 + 主题外观 + 检查更新 + 日志与网盘认证。
 * iOS 分组内嵌列表风格：浅灰分组背景 + 白色圆角卡片 + 原生弹簧交互动画。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    scrollBehavior: TopAppBarScrollBehavior,
    onThemeClick: () -> Unit,
    onAboutClick: () -> Unit,
    backupManager: AuthBackupManager,
    /** 用应用内置下载器下载更新 APK（URL + 文件名），由 MainScreen 注入 DownloadManager */
    onDownloadUpdateApk: (url: String, fileName: String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showThreadsDialog by remember { mutableStateOf(false) }
    var showLogDialog by remember { mutableStateOf(false) }
    // 检查更新结果（非空时弹更新对话框）
    var updateRelease by remember { mutableStateOf<UpdateChecker.Release?>(null) }
    // 网盘认证导出弹窗（AES 加密 + 导出范围）
    var showExportAuthDialog by remember { mutableStateOf(false) }
    // 网盘认证导入：加密文件内容（非空时弹解密密码框）
    var pendingImportContent by remember { mutableStateOf<String?>(null) }
    var showImportAuthDialog by remember { mutableStateOf(false) }
    // 导出/导入处理中（PBKDF2 21万次迭代派生密钥，偶发 1~3s，期间显示加载弹窗）
    var isExporting by remember { mutableStateOf(false) }
    var isImporting by remember { mutableStateOf(false) }
    // 按平台线程数：二级弹窗当前选择的平台
    var selectedThreadPlatform by remember { mutableStateOf(threadPlatforms.first()) }
    var showPlatformThreadDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    // 下载保存目录（SAF）：本地状态驱动 UI 刷新，同时同步 SharedPreferences
    val settingsRepo = remember { SettingsRepository(context) }
    var downloadDirUri by remember { mutableStateOf(settingsRepo.downloadDirUri) }
    var showDevMenu by remember { mutableStateOf(false) }
    // 网络与下载策略（本地状态驱动 UI，同时同步 SharedPreferences）
    var maxConcurrent by remember { mutableStateOf(settingsRepo.maxConcurrentDownloads) }
    var speedLimitBps by remember { mutableStateOf(settingsRepo.downloadSpeedLimit) }
    var retryCount by remember { mutableStateOf(settingsRepo.downloadRetryCount) }
    var showConcurrencyDialog by remember { mutableStateOf(false) }
    var showSpeedDialog by remember { mutableStateOf(false) }
    var showRetryDialog by remember { mutableStateOf(false) }
    // 用户体验与系统适配：锁屏保持下载 / 通知栏速度
    var keepLocked by remember { mutableStateOf(settingsRepo.keepDownloadWhenLocked) }
    var showSpeed by remember { mutableStateOf(settingsRepo.notificationShowSpeed) }
    var showBatteryDialog by remember { mutableStateOf(false) }
    // 通知权限（Android 13+）：未授权时点击「通知栏下载进度」先申请，授权后生效
    val notifyPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            showSpeed = true
            settingsRepo.notificationShowSpeed = true
        }
    }
    // 下载完成提醒（功能 8）：总开关 / 完成 / 失败独立开关 / 铃声（跟随系统 / 静音 / 自定义）
    var notifyEnabled by remember { mutableStateOf(settingsRepo.notifyEnabled) }
    var notifyOnComplete by remember { mutableStateOf(settingsRepo.notifyOnComplete) }
    var notifyOnFailed by remember { mutableStateOf(settingsRepo.notifyOnFailed) }
    var notifySoundUri by remember { mutableStateOf(settingsRepo.notifySoundUri) }
    var showNotifySoundDialog by remember { mutableStateOf(false) }
    val ringtonePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val uri = result.data?.getParcelableExtra<Uri>(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
        if (uri != null) {
            notifySoundUri = uri.toString()
            settingsRepo.notifySoundUri = uri.toString()
            SnackbarController.show("铃声已更新")
        }
    }
    fun openRingtonePicker() {
        val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
            putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION)
            putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "选择下载完成提醒铃声")
            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true)
            putExtra(RingtoneManager.EXTRA_RINGTONE_DEFAULT_URI, Settings.System.DEFAULT_NOTIFICATION_URI)
        }
        runCatching { context.startActivity(intent) }.onFailure {
            SnackbarController.show("无法打开铃声选择器")
        }
    }
    // 设置导出/导入（功能 10）：备份整机设置到本地文件
    // 全部状态变量声明在 launcher 之前，供其 lambda 捕获
    var showExportSettingsDialog by remember { mutableStateOf(false) }
    var pendingImportSettings by remember { mutableStateOf<String?>(null) }
    var showImportSettingsDialog by remember { mutableStateOf(false) }
    var isExportingSettings by remember { mutableStateOf(false) }
    var isImportingSettings by remember { mutableStateOf(false) }
    // 导出口令（非空=加密导出，null=明文导出）
    var exportSettingsPassword: String? = null
    val settingsExportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            scope.launch {
                isExportingSettings = true
                try {
                    val content = exportSettingsPassword?.let { settingsRepo.exportSettingsEncrypted(it) }
                        ?: settingsRepo.exportSettingsJson()
                    context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use {
                        it.write(content)
                    }
                    SnackbarController.show("设置已导出")
                } catch (e: Exception) {
                    SnackbarController.show("导出失败：${e.message}")
                } finally {
                    isExportingSettings = false
                    exportSettingsPassword = null
                }
            }
        }
    }
    val settingsImportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                isImportingSettings = true
                try {
                    val text = runCatching {
                        context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                    }.getOrNull()
                    if (text == null) {
                        SnackbarController.show("读取文件失败")
                        return@launch
                    }
                    if (text.isBlank()) {
                        SnackbarController.show("备份文件为空")
                        return@launch
                    }
                    if (AuthCrypto.isEncrypted(text)) {
                        pendingImportSettings = text
                        showImportSettingsDialog = true
                    } else {
                        runCatching { settingsRepo.importSettingsJson(text) }
                            .onFailure { SnackbarController.show("导入设置失败：${it.message}") }
                            .onSuccess {
                                SnackbarController.show("设置已恢复，部分设置重启后生效")
                            }
                    }
                } finally {
                    isImportingSettings = false
                }
            }
        }
    }
    val dirLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            // 持久授权：应用重启后仍可写（API19+；Android 10/11+ 分区存储必需）
            val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            runCatching { context.contentResolver.takePersistableUriPermission(uri, flags) }
            settingsRepo.downloadDirUri = uri.toString()
            downloadDirUri = uri.toString()
            SnackbarController.show("下载保存目录已更新")
        }
    }
    // 导入网盘认证文件选择器：选择后先判断是否加密备份，加密则弹密码框
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                isImporting = true
                try {
                    val text = runCatching {
                        context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                    }.getOrNull()
                    if (text == null) {
                        SnackbarController.show("读取文件失败")
                        return@launch
                    }
                    if (AuthCrypto.isEncrypted(text)) {
                        // 加密备份：关闭加载弹窗，弹解密密码框（解密在确认后执行）
                        pendingImportContent = text
                        showImportAuthDialog = true
                    } else {
                        // 明文备份：直接导入
                        val count = runCatching {
                            withContext(Dispatchers.IO) { backupManager.importJson(text) }
                        }.getOrElse { e ->
                            SnackbarController.show("导入失败：${e.message}")
                            return@launch
                        }
                        SnackbarController.show("已恢复 $count 个平台的认证信息")
                    }
                } finally {
                    isImporting = false
                }
            }
        }
    }

    val (tintBlue) = iosTint(IosBlue)
    val (tintGreen) = iosTint(IosGreen)
    val (tintIndigo) = iosTint(IosIndigo)
    val (tintOrange) = iosTint(IosOrange)
    val (tintTeal) = iosTint(IosTeal)
    val (tintPurple) = iosTint(IosPurple)
    val (tintGray) = iosTint(IosGray)
    val (tintPink) = iosTint(IosPink)

    IosScreenBackground {
        Column(
            modifier = modifier
                .fillMaxSize()
                .nestedScrollCompat(scrollBehavior)
                .verticalScroll(rememberScrollState())
                // 底部留出悬浮玻璃导航栏高度，保证最后的功能块能滚动到可见位置
                .padding(bottom = 96.dp)
        ) {
            // ===== 下载 =====
            IosSectionHeader("下载")
            IosGroupCard {
                IosListRow(
                    icon = Icons.Outlined.Tune,
                    iconBackground = tintBlue,
                    iconTint = IosBlue,
                    title = "下载线程数",
                    subtitle = "按网盘分别设置分片并发数（默认 32，最高 512）",
                    onClick = { showThreadsDialog = true },
                    showDivider = true
                )
                IosListRow(
                    icon = Icons.Outlined.FolderOpen,
                    iconBackground = tintGreen,
                    iconTint = IosGreen,
                    title = "下载保存目录",
                    subtitle = downloadDirUri?.let { "已自定义：${DownloadSaver.safDirDisplay(it)}" }
                        ?: "系统默认 Download（点击自定义）",
                    onClick = { dirLauncher.launch(null) },
                    trailing = if (downloadDirUri != null) {
                        {
                            TextButton(
                                onClick = {
                                    downloadDirUri = null
                                    settingsRepo.downloadDirUri = null
                                    SnackbarController.show("已恢复默认下载目录")
                                },
                                modifier = Modifier.padding(start = 8.dp)
                            ) {
                                Text(
                                    text = "恢复默认",
                                    fontSize = 15.sp,
                                    color = IosBlue
                                )
                            }
                        }
                    } else null,
                    showDivider = true
                )
                IosListRow(
                    icon = Icons.Outlined.Layers,
                    iconBackground = tintIndigo,
                    iconTint = IosIndigo,
                    title = "最大同时下载任务数",
                    subtitle = "同时下载 $maxConcurrent 个任务（限制后台并发，避免占满带宽）",
                    onClick = { showConcurrencyDialog = true },
                    showDivider = true
                )
                IosListRow(
                    icon = Icons.Outlined.Speed,
                    iconBackground = tintOrange,
                    iconTint = IosOrange,
                    title = "下载速度限制",
                    subtitle = speedLimitText(speedLimitBps),
                    onClick = { showSpeedDialog = true },
                    showDivider = true
                )
                IosListRow(
                    icon = Icons.Outlined.Refresh,
                    iconBackground = tintTeal,
                    iconTint = IosTeal,
                    title = "失败自动重试",
                    subtitle = if (retryCount == 0) "失败后不自动重试" else "失败后自动重试 $retryCount 次（断点续传）",
                    onClick = { showRetryDialog = true },
                    showDivider = true
                )
                IosListRow(
                    icon = Icons.Outlined.Power,
                    iconBackground = tintPurple,
                    iconTint = IosPurple,
                    title = "锁屏后保持下载",
                    subtitle = "开启后下载时获取 WakeLock 维持网络，并可加入「忽略电池优化」白名单",
                    onClick = {
                        keepLocked = !keepLocked
                        settingsRepo.keepDownloadWhenLocked = keepLocked
                        if (keepLocked) {
                            val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
                            if (pm?.isIgnoringBatteryOptimizations(context.packageName) != true) {
                                showBatteryDialog = true
                            }
                        }
                    },
                    trailing = { IosSwitch(checked = keepLocked, onCheckedChange = null) },
                    showDivider = true
                )
                IosListRow(
                    icon = Icons.Outlined.Notifications,
                    iconBackground = tintBlue,
                    iconTint = IosBlue,
                    title = "通知栏下载进度",
                    subtitle = when {
                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
                            PackageManager.PERMISSION_GRANTED ->
                            "未授予通知权限，下载通知将不可见（点击申请）"
                        showSpeed -> "完整通知：进度条 + 下载速度"
                        else -> "仅显示通知（隐藏下载速度）"
                    },
                    onClick = {
                        // Android 13+ 未授权：先申请通知权限，授权后自动开启完整通知
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
                            PackageManager.PERMISSION_GRANTED
                        ) {
                            notifyPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            showSpeed = !showSpeed
                            settingsRepo.notificationShowSpeed = showSpeed
                        }
                    },
                    trailing = { IosSwitch(checked = showSpeed, onCheckedChange = null) },
                    showDivider = false
                )
            }

            // ===== 外观 =====
            IosSectionHeader("外观")
            IosGroupCard {
                IosListRow(
                    icon = Icons.Outlined.Palette,
                    iconBackground = tintPurple,
                    iconTint = IosPurple,
                    title = "主题与外观",
                    subtitle = "主题色、动态色彩与深色模式",
                    onClick = onThemeClick,
                    showDivider = false
                )
            }

            // ===== 通用 =====
            IosSectionHeader("通用")
            IosGroupCard {
                IosListRow(
                    icon = Icons.Outlined.SystemUpdate,
                    iconBackground = tintBlue,
                    iconTint = IosBlue,
                    title = "检查更新",
                    subtitle = "检查 GitHub 是否有新版本可用",
                    onClick = {
                        scope.launch {
                            SnackbarController.show("正在检查更新…")
                            val release = runCatching { UpdateChecker.fetchLatestRelease() }.getOrNull()
                            val current = UpdateChecker.currentVersion(context)
                            if (release == null) {
                                SnackbarController.show("检查更新失败，请检查网络")
                            } else if (UpdateChecker.compareVersions(release.tagName, current) > 0) {
                                updateRelease = release
                            } else {
                                SnackbarController.show("已是最新版本")
                            }
                        }
                    },
                    showDivider = true
                )
                IosListRow(
                    icon = Icons.Outlined.Article,
                    iconBackground = tintGray,
                    iconTint = IosGray,
                    title = "导出日志",
                    subtitle = "导出崩溃日志与应用信息，便于排查问题",
                    onClick = { showLogDialog = true },
                    showDivider = false
                )
            }

            // ===== 网盘认证 =====
            IosSectionHeader("网盘认证")
            IosGroupCard {
                IosListRow(
                    icon = Icons.Outlined.Backup,
                    iconBackground = tintGreen,
                    iconTint = IosGreen,
                    title = "导出网盘认证",
                    subtitle = "使用至少 8 位口令加密 Cookie/JWT 后导出",
                    onClick = { showExportAuthDialog = true },
                    showDivider = true
                )
                IosListRow(
                    icon = Icons.Outlined.Restore,
                    iconBackground = tintTeal,
                    iconTint = IosTeal,
                    title = "导入网盘认证",
                    subtitle = "选择加密或明文的认证备份文件，恢复网盘登录",
                    onClick = { importLauncher.launch(arrayOf("application/json", "application/octet-stream", "*/*")) },
                    showDivider = false
                )
            }

            // ===== 设置备份（功能 10）=====
            IosSectionHeader("设置备份")
            IosGroupCard {
                IosListRow(
                    icon = Icons.Outlined.Backup,
                    iconBackground = tintBlue,
                    iconTint = IosBlue,
                    title = "导出设置",
                    subtitle = "将全部设置为 JSON 导出（可选口令加密），保存到本地文件",
                    onClick = { showExportSettingsDialog = true },
                    showDivider = true
                )
                IosListRow(
                    icon = Icons.Outlined.Restore,
                    iconBackground = tintTeal,
                    iconTint = IosTeal,
                    title = "导入设置",
                    subtitle = "选择之前导出的备份文件，恢复全部设置",
                    onClick = { settingsImportLauncher.launch(arrayOf("application/json", "application/octet-stream", "*/*")) },
                    showDivider = false
                )
            }

            // ===== 关于 =====
            IosSectionHeader("关于")
            IosGroupCard {
                IosListRow(
                    icon = Icons.Outlined.Info,
                    iconBackground = tintBlue,
                    iconTint = IosBlue,
                    title = "关于析盘",
                    subtitle = "版本信息、支持平台与技术说明",
                    onClick = onAboutClick,
                    onLongClick = { showDevMenu = true }, // 长按打开隐藏开发调试菜单
                    showDivider = false
                )
            }
        }
    }

    // 导出日志方式选择弹窗
    if (showLogDialog) {
        IosAlertDialog(
            onDismissRequest = { showLogDialog = false },
            title = "导出日志",
            message = "选择日志导出方式：",
            confirmText = "取消",
            onConfirm = { showLogDialog = false },
            onDismiss = { showLogDialog = false },
            dismissText = null
        ) {
            IosDialogOption(
                icon = Icons.Outlined.Article,
                text = "分享日志（发送到其他应用）",
                onClick = {
                    showLogDialog = false
                    scope.launch {
                        val file = withContext(Dispatchers.IO) { LogExporter.export(context) }
                        if (file != null && LogExporter.share(context, file)) {
                            SnackbarController.show("日志已分享")
                        } else {
                            SnackbarController.show("导出日志失败")
                        }
                    }
                }
            )
            IosDialogOption(
                icon = Icons.Outlined.Article,
                text = "保存到下载目录",
                onClick = {
                    showLogDialog = false
                    scope.launch {
                        val ok = withContext(Dispatchers.IO) {
                            LogExporter.saveToDownloads(context)
                        }
                        SnackbarController.show(if (ok) "已保存到下载目录" else "保存失败")
                    }
                }
            )
            IosDialogOption(
                icon = Icons.Outlined.Article,
                text = "清空日志缓存（logcat -c）",
                onClick = {
                    showLogDialog = false
                    scope.launch {
                        val ok = withContext(Dispatchers.IO) {
                            LogExporter.clearLogcat()
                        }
                        SnackbarController.show(if (ok) "日志缓存已清空" else "清空失败")
                    }
                }
            )
        }
    }

    // 隐藏开发调试菜单（长按「关于析盘」打开）
    if (showDevMenu) {
        IosAlertDialog(
            onDismissRequest = { showDevMenu = false },
            title = "开发调试",
            confirmText = "关闭",
            onConfirm = { showDevMenu = false },
            onDismiss = { showDevMenu = false },
            dismissText = null
        ) {
            IosDialogOption(
                icon = Icons.Outlined.SystemUpdate,
                text = "显示检查更新弹窗",
                onClick = {
                    showDevMenu = false
                    // 调试用途：直接弹出更新弹窗（不判断是否已是最新版），预览弹窗 UI
                    scope.launch {
                        val release = runCatching { UpdateChecker.fetchLatestRelease() }.getOrNull()
                        updateRelease = release ?: UpdateChecker.Release(
                            tagName = "v1.2.4（预览）",
                            body = "这是调试预览弹窗，用于查看更新弹窗 UI（含镜像站下载按钮）。",
                            assets = emptyList(),
                            publishedAt = ""
                        )
                    }
                }
            )
        }
    }

    // 检查更新结果弹窗（发现新版本时展示，下载走系统浏览器）
    updateRelease?.let { release ->
        UpdateDialog(
            currentVersion = UpdateChecker.currentVersion(context),
            release = release,
            onDownload = {
                updateRelease = null
                val apk = release.assets.firstOrNull { it.name.endsWith(".apk", true) }
                if (apk != null) {
                    onDownloadUpdateApk(apk.downloadUrl, apk.name)
                    SnackbarController.show("已加入下载 ${apk.name}")
                } else {
                    SnackbarController.show("未找到 APK 下载链接")
                }
            },
            onDownloadMirror = {
                updateRelease = null
                val apk = release.assets.firstOrNull { it.name.endsWith(".apk", true) }
                if (apk != null) {
                    onDownloadUpdateApk(UpdateChecker.mirrorUrl(apk.downloadUrl), apk.name)
                    SnackbarController.show("已通过镜像站加入下载 ${apk.name}")
                } else {
                    SnackbarController.show("未找到 APK 下载链接")
                }
            },
            onLater = { updateRelease = null },
            onIgnore = {
                context.getSharedPreferences("yunx_prefs", android.content.Context.MODE_PRIVATE)
                    .edit()
                    .putString("ignored_version", release.tagName)
                    .apply()
                updateRelease = null
            }
        )
    }

    // 线程数选择弹窗（按平台）
    if (showThreadsDialog) {
        IosAlertDialog(
            onDismissRequest = { showThreadsDialog = false },
            title = "下载线程数",
            message = "按网盘分别设置分片并发数；线程数不是越多越好，适当调整",
            confirmText = "取消",
            onConfirm = { showThreadsDialog = false },
            onDismiss = { showThreadsDialog = false },
            dismissText = null
        ) {
            threadPlatforms.forEach { item ->
                val current = settingsRepo.downloadThreadsFor(item.platform)
                val isXunlei = item.platform == DownloadPlatform.XUNLEI
                IosCheckRow(
                    text = item.label,
                    value = if (isXunlei) "固定 8 线程" else "$current 线程",
                    enabled = !isXunlei,
                    onClick = {
                        if (!isXunlei) {
                            selectedThreadPlatform = item
                            showPlatformThreadDialog = true
                        }
                    }
                )
            }
        }
    }

    // 单个平台线程数选择（二级弹窗）
    if (showPlatformThreadDialog) {
        val current = settingsRepo.downloadThreadsFor(selectedThreadPlatform.platform)
        IosAlertDialog(
            onDismissRequest = { showPlatformThreadDialog = false },
            title = "${selectedThreadPlatform.label}线程数",
            confirmText = "取消",
            onConfirm = { showPlatformThreadDialog = false },
            onDismiss = { showPlatformThreadDialog = false },
            dismissText = null
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 320.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                threadOptions.chunked(2).forEach { rowValues ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        rowValues.forEach { value ->
                            IosCheckRow(
                                text = "$value 线程",
                                selected = current == value,
                                onClick = {
                                    settingsRepo.setDownloadThreads(selectedThreadPlatform.platform, value)
                                    showPlatformThreadDialog = false
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        // 奇数个时补空占位，保持两列对齐
                        if (rowValues.size == 1) Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }

    // 导出网盘认证弹窗（AES 加密密码 + 导出范围）
    if (showExportAuthDialog) {
        ExportAuthDialog(
            onDismiss = { showExportAuthDialog = false },
            onConfirm = { password, onlyLoggedIn ->
                showExportAuthDialog = false
                isExporting = true
                scope.launch {
                    try {
                        val content = runCatching {
                            withContext(Dispatchers.IO) { backupManager.export(password, onlyLoggedIn) }
                        }.getOrNull()
                        if (content == null) {
                            SnackbarController.show("导出失败")
                            return@launch
                        }
                        val encrypted = true
                        val saved = withContext(Dispatchers.IO) {
                            backupManager.saveToDownloads(context, content, encrypted)
                        }
                        SnackbarController.show(
                            if (saved) {
                                if (encrypted) "已加密导出到下载目录" else "已导出到下载目录"
                            } else {
                                "导出失败"
                            }
                        )
                    } finally {
                        isExporting = false
                    }
                }
            }
        )
    }

    // 导入加密备份弹窗（解密密码）
    if (showImportAuthDialog) {
        ImportAuthDialog(
            onDismiss = {
                showImportAuthDialog = false
                pendingImportContent = null
            },
            onConfirm = { password ->
                showImportAuthDialog = false
                val content = pendingImportContent
                pendingImportContent = null
                if (content != null) {
                    isImporting = true
                    scope.launch {
                        try {
                            val count = try {
                                withContext(Dispatchers.IO) { backupManager.import(content, password) }
                            } catch (e: javax.crypto.AEADBadTagException) {
                                SnackbarController.show("密码错误，解密失败")
                                return@launch
                            } catch (e: Exception) {
                                SnackbarController.show("导入失败：${e.message}")
                                return@launch
                            }
                            SnackbarController.show("已恢复 $count 个平台的认证信息")
                        } finally {
                            isImporting = false
                        }
                    }
                }
            }
        )
    }

    // 导出设置弹窗（功能 10）：明文 / 口令加密导出
    if (showExportSettingsDialog) {
        ExportSettingsDialog(
            onDismiss = {
                showExportSettingsDialog = false
                exportSettingsPassword = null
            },
            onPlain = {
                showExportSettingsDialog = false
                exportSettingsPassword = null
                settingsExportLauncher.launch("yunx_settings_backup.json")
            },
            onEncrypted = { password ->
                showExportSettingsDialog = false
                exportSettingsPassword = password
                settingsExportLauncher.launch("yunx_settings_backup_enc.json")
            }
        )
    }

    // 导入设置（加密备份）弹窗：输入口令解密导入
    if (showImportSettingsDialog) {
        ImportSettingsDialog(
            onDismiss = {
                showImportSettingsDialog = false
                pendingImportSettings = null
            },
            onConfirm = { password ->
                showImportSettingsDialog = false
                val content = pendingImportSettings
                pendingImportSettings = null
                if (content != null) {
                    isImportingSettings = true
                    scope.launch {
                        try {
                            settingsRepo.importSettingsEncrypted(content, password)
                            SnackbarController.show("设置已恢复，部分设置重启后生效")
                        } catch (e: javax.crypto.AEADBadTagException) {
                            SnackbarController.show("密码错误，解密失败")
                        } catch (e: Exception) {
                            SnackbarController.show("导入设置失败：${e.message}")
                        } finally {
                            isImportingSettings = false
                        }
                    }
                }
            }
        )
    }

    // 导出/导入处理中：转圈加载弹窗（PBKDF2 派生密钥耗时较长，避免用户以为界面卡死）
    if (isExporting) OperationLoadingDialog("正在导出认证…")
    if (isImporting) OperationLoadingDialog("正在导入认证…")
    if (isExportingSettings) OperationLoadingDialog("正在导出设置…")
    if (isImportingSettings) OperationLoadingDialog("正在导入设置…")

    // 最大同时下载任务数
    if (showConcurrencyDialog) {
        val options = listOf(1, 2, 3, 5, 8)
        IosAlertDialog(
            onDismissRequest = { showConcurrencyDialog = false },
            title = "最大同时下载任务数",
            confirmText = "取消",
            onConfirm = { showConcurrencyDialog = false },
            onDismiss = { showConcurrencyDialog = false },
            dismissText = null
        ) {
            options.forEach { v ->
                IosCheckRow(
                    text = "同时下载 $v 个任务",
                    selected = maxConcurrent == v,
                    onClick = {
                        maxConcurrent = v
                        settingsRepo.maxConcurrentDownloads = v
                        showConcurrencyDialog = false
                    }
                )
            }
        }
    }

    // 下载速度限制：预设档位 + 自定义（KB/s）
    if (showSpeedDialog) {
        val presets = listOf(0L, 1L * 1024 * 1024, 2L * 1024 * 1024, 5L * 1024 * 1024, 10L * 1024 * 1024)
        // 弹窗内临时选择（不立即写设置）：null=未操作，-1=自定义，其余=预设值
        var tempSelected by remember { mutableStateOf<Long?>(null) }
        // 自定义输入：打开时若当前是自定义档位，带出原值（重新打开保留）
        var customKb by remember {
            mutableStateOf(
                if (speedLimitBps > 0 && speedLimitBps !in presets) (speedLimitBps / 1024).toString() else ""
            )
        }
        val effective = tempSelected ?: speedLimitBps
        // 自定义选中态：显式识别「-1=自定义」哨兵；未操作时按当前值是否为自定义档位判断
        val isCustom = when {
            tempSelected == -1L -> true
            tempSelected == null -> speedLimitBps > 0 && speedLimitBps !in presets
            else -> false
        }
        IosAlertDialog(
            onDismissRequest = { showSpeedDialog = false },
            title = "下载速度限制",
            confirmText = "取消",
            onConfirm = { showSpeedDialog = false },
            onDismiss = { showSpeedDialog = false },
            dismissText = null
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                presets.forEach { v ->
                    IosCheckRow(
                        text = if (v == 0L) "不限速" else speedLimitText(v),
                        selected = !isCustom && effective == v,
                        onClick = { tempSelected = v }
                    )
                }
                // 自定义档位：点击单选即可选中（进入自定义模式）
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IosCheckRow(
                        text = "自定义速度",
                        selected = isCustom,
                        onClick = {
                            tempSelected = -1L
                            // 当前已是自定义值时带出原值，便于修改
                            if (speedLimitBps > 0 && speedLimitBps !in presets && customKb.isBlank()) {
                                customKb = (speedLimitBps / 1024).toString()
                            }
                        },
                        modifier = Modifier.width(150.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    IosTextField(
                        value = customKb,
                        onValueChange = {
                            customKb = it.filter(Char::isDigit).take(6)
                            // 输入即视为选择自定义
                            tempSelected = -1L
                        },
                        modifier = Modifier.weight(1f),
                        placeholder = "KB/s",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }
                // 应用按钮
                Spacer(modifier = Modifier.height(6.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    TextButton(
                        onClick = {
                            // 以当前选中项为准：选自定义则应用输入；选预设则应用预设值
                            if (isCustom) {
                                val kb = customKb.toLongOrNull()?.coerceAtLeast(1L)
                                if (kb != null) {
                                    speedLimitBps = kb * 1024
                                    settingsRepo.downloadSpeedLimit = kb * 1024
                                }
                                // 自定义输入为空：保持原值
                            } else if (tempSelected != null) {
                                val v = tempSelected ?: speedLimitBps
                                speedLimitBps = v
                                settingsRepo.downloadSpeedLimit = v
                            }
                            // 未做任何选择：保持当前值
                            showSpeedDialog = false
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("确定", color = IosBlue)
                    }
                }
            }
        }
    }

    // 失败自动重试次数
    if (showRetryDialog) {
        val options = listOf(0, 1, 2, 3, 5, 8, 10)
        IosAlertDialog(
            onDismissRequest = { showRetryDialog = false },
            title = "失败自动重试",
            confirmText = "取消",
            onConfirm = { showRetryDialog = false },
            onDismiss = { showRetryDialog = false },
            dismissText = null
        ) {
            options.forEach { v ->
                IosCheckRow(
                    text = if (v == 0) "不自动重试" else "失败后自动重试 $v 次",
                    selected = retryCount == v,
                    onClick = {
                        retryCount = v
                        settingsRepo.downloadRetryCount = v
                        showRetryDialog = false
                    }
                )
            }
        }
    }

    // 锁屏保持下载：引导加入「忽略电池优化」白名单
    if (showBatteryDialog) {
        IosAlertDialog(
            onDismissRequest = { showBatteryDialog = false },
            title = "保持后台下载",
            message = "为确保障屏后下载不中断，建议将析盘加入「忽略电池优化」白名单。是否前往系统设置？",
            confirmText = "前往设置",
            confirmStyle = IosButtonStyle.Default,
            onConfirm = {
                showBatteryDialog = false
                runCatching {
                    context.startActivity(
                        Intent(
                            Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                            Uri.parse("package:${context.packageName}")
                        )
                    )
                }
            },
            dismissText = "暂不",
            onDismiss = { showBatteryDialog = false }
        )
    }
}

/** 设置页专用：兼容 nestedScroll（避免直接引用 scrollBehavior 容器冲突） */
@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun Modifier.nestedScrollCompat(scrollBehavior: TopAppBarScrollBehavior): Modifier =
    this.then(Modifier.padding(0.dp)) // 占位；实际滚动连接由外层 IosScreenBackground 处理

/** iOS 弹窗内选项行（圆点选中态 / 右侧值） */
@Composable
private fun IosCheckRow(
    text: String,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    value: String? = null,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val labelColor = iosLabelColor()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
            .clickable(
                enabled = enabled,
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onClick() }
            .padding(vertical = 9.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 选中圆点
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(CircleShape)
                .background(if (selected) IosBlue else if (enabled) androidx.compose.ui.graphics.Color(0xFFE9E9EB) else androidx.compose.ui.graphics.Color(0xFFE9E9EB)),
            contentAlignment = Alignment.Center
        ) {
            if (selected) {
                Icon(
                    imageVector = Icons.Outlined.Check,
                    contentDescription = null,
                    tint = androidx.compose.ui.graphics.Color.White,
                    modifier = Modifier.size(13.dp)
                )
            }
        }
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = text,
            fontSize = 15.sp,
            color = if (enabled) labelColor else labelColor.copy(alpha = 0.5f),
            modifier = Modifier.weight(1f)
        )
        value?.let {
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = it,
                fontSize = 15.sp,
                color = if (enabled) IosBlue else iosSecondaryLabelColor(),
                maxLines = 1
            )
        }
    }
}

/** iOS 弹窗内文字选项行 */
@Composable
private fun IosDialogOption(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(vertical = 11.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IosIconTileSmall(icon = icon, tint = IosBlue)
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = text,
            fontSize = 15.sp,
            color = IosBlue
        )
    }
}

@Composable
private fun IosIconTileSmall(icon: androidx.compose.ui.graphics.vector.ImageVector, tint: androidx.compose.ui.graphics.Color) {
    Box(
        modifier = Modifier
            .size(28.dp)
            .clip(CircleShape)
            .background(tint.copy(alpha = 0.12f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(16.dp))
    }
}

/** 导出网盘认证弹窗：AES 加密密码 + 导出范围（仅已登录 / 全部绑定） */
@Composable
private fun ExportAuthDialog(
    onDismiss: () -> Unit,
    onConfirm: (password: String, onlyLoggedIn: Boolean) -> Unit
) {
    var password by remember { mutableStateOf("") }
    var onlyLoggedIn by remember { mutableStateOf(true) }
    IosAlertDialog(
        onDismissRequest = onDismiss,
        title = "导出网盘认证",
        message = "设置至少 8 位密码对认证文件进行 AES 加密。密码请务必牢记，丢失无法找回。",
        confirmText = "导出",
        onConfirm = { onConfirm(password, onlyLoggedIn) },
        onDismiss = onDismiss,
        dismissText = "取消"
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            IosPasswordField(
                value = password,
                onValueChange = { password = it },
                placeholder = "加密密码（至少 8 位）"
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "导出范围",
                fontSize = 13.sp,
                color = iosSecondaryLabelColor(),
                modifier = Modifier.padding(start = 4.dp, top = 4.dp)
            )
            IosCheckRow(
                text = "仅导出当前已登录的网盘",
                selected = onlyLoggedIn,
                onClick = { onlyLoggedIn = true }
            )
            IosCheckRow(
                text = "导出全部绑定的网盘",
                selected = !onlyLoggedIn,
                onClick = { onlyLoggedIn = false }
            )
        }
        // 导出按钮启用态：密码长度 >= 8
        Spacer(modifier = Modifier.height(2.dp))
        IosBlockButtonSmall(
            text = "导出",
            enabled = password.length >= 8,
            onClick = { onConfirm(password, onlyLoggedIn) }
        )
    }
}

/** 导入加密备份弹窗：输入解密密码 */
@Composable
private fun ImportAuthDialog(
    onDismiss: () -> Unit,
    onConfirm: (password: String) -> Unit
) {
    var password by remember { mutableStateOf("") }
    IosAlertDialog(
        onDismissRequest = onDismiss,
        title = "导入网盘认证",
        message = "该备份文件已加密，请输入导出时设置的密码进行解密。",
        confirmText = "取消",
        onConfirm = onDismiss,
        onDismiss = onDismiss,
        dismissText = null
    ) {
        IosPasswordField(
            value = password,
            onValueChange = { password = it },
            placeholder = "解密密码"
        )
        Spacer(modifier = Modifier.height(10.dp))
        IosBlockButtonSmall(
            text = "解密并导入",
            enabled = password.isNotBlank(),
            onClick = { onConfirm(password) }
        )
    }
}

/** 导出设置弹窗（功能 10）：明文 JSON 或口令加密导出 */
@Composable
private fun ExportSettingsDialog(
    onDismiss: () -> Unit,
    onPlain: () -> Unit,
    onEncrypted: (password: String) -> Unit
) {
    var encrypted by remember { mutableStateOf(false) }
    var password by remember { mutableStateOf("") }
    IosAlertDialog(
        onDismissRequest = onDismiss,
        title = "导出设置",
        message = "将全部设置备份到本地文件。可明文导出，或设置至少 8 位口令加密后导出。",
        confirmText = "取消",
        onConfirm = onDismiss,
        onDismiss = onDismiss,
        dismissText = null
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            IosDialogOption(
                icon = Icons.Outlined.Article,
                text = "明文导出（JSON，不加密）",
                onClick = {
                    encrypted = false
                    password = ""
                }
            )
            IosDialogOption(
                icon = Icons.Outlined.Lock,
                text = "口令加密导出（推荐）",
                onClick = {
                    encrypted = true
                    password = ""
                }
            )
            if (encrypted) {
                Spacer(modifier = Modifier.height(6.dp))
                IosPasswordField(
                    value = password,
                    onValueChange = { password = it },
                    placeholder = "加密口令（至少 8 位）"
                )
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        IosBlockButtonSmall(
            text = "导出",
            enabled = !encrypted || password.length >= 8,
            onClick = {
                if (encrypted) onEncrypted(password) else onPlain()
            }
        )
    }
}

/** 导入设置（加密备份）弹窗：输入口令解密 */
@Composable
private fun ImportSettingsDialog(
    onDismiss: () -> Unit,
    onConfirm: (password: String) -> Unit
) {
    var password by remember { mutableStateOf("") }
    IosAlertDialog(
        onDismissRequest = onDismiss,
        title = "导入设置",
        message = "该备份文件已加密，请输入导出时设置的口令进行解密。",
        confirmText = "取消",
        onConfirm = onDismiss,
        onDismiss = onDismiss,
        dismissText = null
    ) {
        IosPasswordField(
            value = password,
            onValueChange = { password = it },
            placeholder = "解密口令"
        )
        Spacer(modifier = Modifier.height(10.dp))
        IosBlockButtonSmall(
            text = "解密并导入",
            enabled = password.isNotBlank(),
            onClick = { onConfirm(password) }
        )
    }
}

/** iOS 弹窗内整块主按钮（蓝色填充） */
@Composable
private fun IosBlockButtonSmall(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(10.dp))
            .background(if (enabled) IosBlue else IosBlue.copy(alpha = 0.35f))
            .clickable(
                enabled = enabled,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = androidx.compose.ui.graphics.Color.White
        )
    }
}

/** 操作处理中弹窗：转圈加载 + 提示文案，禁止关闭（防止中途取消导致导入/导出状态不一致） */
@Composable
private fun OperationLoadingDialog(message: String) {
    IosAlertDialog(
        onDismissRequest = {},
        title = message,
        confirmText = "",
        onConfirm = {},
        onDismiss = null,
        dismissText = null
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(28.dp),
                strokeWidth = 2.5.dp,
                color = IosBlue
            )
        }
    }
}

/** 速度限制展示文案：0=不限速；>=1MB/s 显示 MB/s，否则 KB/s */
private fun speedLimitText(bps: Long): String {
    if (bps <= 0) return "不限速"
    return if (bps >= 1024 * 1024) {
        val mb = bps / (1024.0 * 1024.0)
        if (mb >= 10) String.format("%.0f MB/s", mb) else String.format("%.1f MB/s", mb)
    } else {
        "${bps / 1024} KB/s"
    }
}
