package com.yunx.cloud.ui.login

import android.graphics.Bitmap
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.material3.SnackbarHost
import com.yunx.cloud.ui.SnackbarController
import com.yunx.cloud.ui.rememberGlobalSnackbarHostState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.yunx.cloud.data.network.QuarkConstants
import com.yunx.cloud.ui.components.IosAlertDialog
import com.yunx.cloud.ui.components.IosBlue
import com.yunx.cloud.ui.components.IosIconButton
import com.yunx.cloud.ui.components.IosMultilineTextField
import com.yunx.cloud.ui.components.iosBackgroundColor
import com.yunx.cloud.ui.components.iosLabelColor
import com.yunx.cloud.ui.components.iosSecondaryLabelColor
import com.yunx.cloud.ui.viewmodel.QuarkAccountViewModel
import kotlinx.coroutines.launch

/**
 * 夸克网盘登录页：
 * - 顶部标题栏：返回按钮 + 手动输入 Cookie 图标 + 保存按钮（登录完成后点击，提取 Cookie 并校验保存）
 * - 主体：WebView 加载夸克网盘官网，由用户手动登录
 * - 进入页面时弹登录教程
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuarkLoginScreen(
    viewModel: QuarkAccountViewModel,
    onBack: () -> Unit,
    onSaved: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isSaving by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }

    // 手动输入 Cookie 弹窗状态
    var showCookieDialog by remember { mutableStateOf(false) }
    var cookieInput by remember { mutableStateOf("") }
    var isSavingManual by remember { mutableStateOf(false) }

    // 登录教程弹窗：进入页面即展示一次
    var showTutorial by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { showTutorial = true }

    val webView = remember {
        WebView(context).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true

            settings.setSupportZoom(true)          // 支持缩放
            settings.builtInZoomControls = true    // 启用内置缩放机制（双指缩放）
            settings.displayZoomControls = false   // 隐藏屏幕上的缩放按钮（只保留手势）

            settings.useWideViewPort = true        // 支持 viewport 标签
            settings.loadWithOverviewMode = true

            settings.userAgentString = QuarkConstants.USER_AGENT
            webViewClient = object : WebViewClient() {
                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                    isLoading = true
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    isLoading = false
                }
            }
            webChromeClient = WebChromeClient()
            loadUrl(QuarkConstants.LOGIN_URL)
        }
    }

    // 页面销毁时释放 WebView
    DisposableEffect(Unit) {
        onDispose { webView.destroy() }
    }

    // 系统返回键 → 返回主页（保存中禁用）
    BackHandler(enabled = !isSaving && !isSavingManual) { onBack() }

    // 全局 Snackbar 宿主
    val snackbarHostState = rememberGlobalSnackbarHostState()

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("夸克网盘登录", color = iosLabelColor()) },
                navigationIcon = {
                    IosIconButton(
                        icon = Icons.AutoMirrored.Filled.ArrowBack,
                        tint = IosBlue,
                        onClick = { if (!isSaving && !isSavingManual) onBack() },
                        contentDescription = "返回"
                    )
                },
                actions = {
                    // 手动输入 Cookie
                    IosIconButton(
                        icon = Icons.Outlined.ContentPaste,
                        tint = IosBlue,
                        onClick = { if (!isSaving && !isSavingManual) showCookieDialog = true },
                        contentDescription = "手动输入 Cookie",
                        enabled = !isSaving && !isSavingManual
                    )
                    TextButton(
                        onClick = {
                            scope.launch {
                                isSaving = true
                                val cookie = CookieManager.getInstance()
                                    .getCookie(QuarkConstants.COOKIE_DOMAIN)
                                    .orEmpty()
                                val saved = viewModel.saveQuarkAccount(cookie)
                                isSaving = false
                                if (saved) {
                                    SnackbarController.show("登录成功")
                                    onSaved()
                                } else {
                                    SnackbarController.show("未检测到登录态，请先完成登录")
                                }
                            }
                        },
                        enabled = !isSaving && !isSavingManual
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = IosBlue
                            )
                        } else {
                            Text("保存", color = IosBlue, fontWeight = FontWeight.SemiBold)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = iosBackgroundColor()
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            AndroidView(
                factory = { webView },
                modifier = Modifier.fillMaxSize()
            )
            if (isLoading) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = IosBlue
                )
            }
        }
    }

    // 登录教程弹窗
    if (showTutorial) {
        IosAlertDialog(
            onDismissRequest = { showTutorial = false },
            title = "登录教程",
            confirmText = "知道了",
            onConfirm = { showTutorial = false },
            dismissText = null,
            content = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "1. 在下方网页中登录夸克账号",
                        fontSize = 14.sp,
                        color = iosLabelColor()
                    )
                    Text(
                        text = "2. 登录完成后点右上角「保存」，自动提取 Cookie",
                        fontSize = 14.sp,
                        color = iosLabelColor()
                    )
                    Text(
                        text = "3. 或点击「粘贴」图标，手动输入 Cookie（需含 __pus= 与 __puus=）",
                        fontSize = 14.sp,
                        color = iosLabelColor()
                    )
                    Text(
                        text = "4. Cookie 约 30 天有效，失效后需重新登录",
                        fontSize = 13.sp,
                        color = iosSecondaryLabelColor()
                    )
                }
            }
        )
    }

    // 手动输入 Cookie 弹窗
    if (showCookieDialog) {
        IosAlertDialog(
            onDismissRequest = { if (!isSavingManual) showCookieDialog = false },
            title = "手动输入 Cookie",
            confirmText = "保存",
            confirmEnabled = cookieInput.isNotBlank() && !isSavingManual,
            onConfirm = {
                scope.launch {
                    isSavingManual = true
                    val saved = viewModel.saveQuarkAccount(cookieInput.trim())
                    isSavingManual = false
                    if (saved) {
                        SnackbarController.show("登录成功")
                        showCookieDialog = false
                        onSaved()
                    } else {
                        SnackbarController.show("Cookie 无效，请检查是否包含 __pus= 与 __puus=")
                    }
                }
            },
            dismissText = "取消",
            onDismiss = { if (!isSavingManual) showCookieDialog = false },
            content = {
                Column {
                    Text(
                        text = "从网页登录态复制完整的 Cookie（需包含 __pus= 与 __puus=）",
                        fontSize = 13.sp,
                        color = iosSecondaryLabelColor()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    IosMultilineTextField(
                        value = cookieInput,
                        onValueChange = { cookieInput = it },
                        placeholder = "粘贴 Cookie…",
                        minLines = 4,
                        maxLines = 8
                    )
                    if (isSavingManual) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = IosBlue
                            )
                        }
                    }
                }
            }
        )
    }
}