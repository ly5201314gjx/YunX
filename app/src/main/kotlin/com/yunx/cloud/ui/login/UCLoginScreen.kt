package com.yunx.cloud.ui.login

import android.graphics.Bitmap
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebSettings
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
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
import com.yunx.cloud.data.network.UCConstants
import com.yunx.cloud.ui.components.IosAlertDialog
import com.yunx.cloud.ui.components.IosBlue
import com.yunx.cloud.ui.components.IosIconButton
import com.yunx.cloud.ui.components.IosMultilineTextField
import com.yunx.cloud.ui.components.iosBackgroundColor
import com.yunx.cloud.ui.components.iosLabelColor
import com.yunx.cloud.ui.components.iosSecondaryLabelColor
import com.yunx.cloud.ui.viewmodel.UCAccountViewModel
import kotlinx.coroutines.launch

/**
 * UC 网盘登录页：WebView 登录 + 手动输入 Cookie + 教程弹窗。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UCLoginScreen(
    viewModel: UCAccountViewModel,
    onBack: () -> Unit,
    onSaved: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isSaving by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }
    var showCookieDialog by remember { mutableStateOf(false) }
    var cookieInput by remember { mutableStateOf("") }
    var isSavingManual by remember { mutableStateOf(false) }
    var showTutorial by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { showTutorial = true }

    val webView = remember {
        WebView(context).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.setSupportZoom(true)
            settings.builtInZoomControls = true
            settings.displayZoomControls = false
            settings.useWideViewPort = true
            settings.loadWithOverviewMode = true
            settings.layoutAlgorithm = WebSettings.LayoutAlgorithm.NARROW_COLUMNS
            setInitialScale(0)
            settings.userAgentString = UCConstants.USER_AGENT
            webViewClient = object : WebViewClient() {
                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                    isLoading = true
                }
                override fun onPageFinished(view: WebView?, url: String?) {
                    isLoading = false
                    // 强制覆盖页面 viewport：允许缩放 + 适配屏幕宽度（桌面版页面无 viewport 或限制了缩放时生效）
                    view?.evaluateJavascript(
                        "(function(){var m=document.querySelector('meta[name=\"viewport\"]');" +
                            "var c='width=device-width,initial-scale=1.0,maximum-scale=5.0,user-scalable=yes';" +
                            "if(m){m.setAttribute('content',c);}else{var n=document.createElement('meta');n.name='viewport';n.content=c;document.head.appendChild(n);}" +
                            "window.dispatchEvent(new Event('resize'));})()",
                        null
                    )
                }
            }
            webChromeClient = WebChromeClient()
            loadUrl(UCConstants.LOGIN_URL)
        }
    }

    DisposableEffect(Unit) { onDispose { webView.destroy() } }
    BackHandler(enabled = !isSaving && !isSavingManual) { onBack() }

    // 全局 Snackbar 宿主
    val snackbarHostState = rememberGlobalSnackbarHostState()

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("UC网盘登录", color = iosLabelColor()) },
                navigationIcon = {
                    IosIconButton(
                        icon = Icons.AutoMirrored.Filled.ArrowBack,
                        tint = IosBlue,
                        onClick = { if (!isSaving && !isSavingManual) onBack() },
                        contentDescription = "返回"
                    )
                },
                actions = {
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
                                    .getCookie(UCConstants.COOKIE_DOMAIN).orEmpty()
                                val saved = viewModel.saveUCAccount(cookie)
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
            AndroidView(factory = { webView }, modifier = Modifier.fillMaxSize())
            if (isLoading) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = IosBlue
                )
            }
        }
    }

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
                        text = "1. 在下方网页中登录 UC 账号",
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
                        text = "4. Cookie 有效期有限，失效后需重新登录",
                        fontSize = 13.sp,
                        color = iosSecondaryLabelColor()
                    )
                }
            }
        )
    }

    if (showCookieDialog) {
        IosAlertDialog(
            onDismissRequest = { if (!isSavingManual) showCookieDialog = false },
            title = "手动输入 Cookie",
            confirmText = "保存",
            confirmEnabled = cookieInput.isNotBlank() && !isSavingManual,
            onConfirm = {
                scope.launch {
                    isSavingManual = true
                    val saved = viewModel.saveUCAccount(cookieInput.trim())
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
