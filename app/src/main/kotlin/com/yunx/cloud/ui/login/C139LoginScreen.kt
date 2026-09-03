package com.yunx.cloud.ui.login

import android.graphics.Bitmap
import android.os.Build
import android.util.Log
import com.yunx.cloud.util.LogRedactor
import android.webkit.CookieManager
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.material3.SnackbarHost
import com.yunx.cloud.ui.SnackbarController
import com.yunx.cloud.ui.rememberGlobalSnackbarHostState
import androidx.annotation.RequiresApi
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
import com.yunx.cloud.data.network.C139Constants
import com.yunx.cloud.ui.components.IosAlertDialog
import com.yunx.cloud.ui.components.IosBlue
import com.yunx.cloud.ui.components.IosIconButton
import com.yunx.cloud.ui.components.IosMultilineTextField
import com.yunx.cloud.ui.components.iosBackgroundColor
import com.yunx.cloud.ui.components.iosLabelColor
import com.yunx.cloud.ui.components.iosSecondaryLabelColor
import com.yunx.cloud.ui.viewmodel.C139AccountViewModel
import kotlinx.coroutines.launch

private const val TAG = "C139Login"

/**
 * 139 网盘（和彩云）登录页：
 * - WebView 加载 yun.139.com，由用户手动登录（短信/验证码/风控由官网处理）；
 * - 右上角「保存」从 CookieManager 提取 mail.10086.cn / yun.139.com 的 Cookie（需含 Os_SSo_Sid + RMKEY）；
 * - 支持手动粘贴 Cookie（需含 Os_SSo_Sid= 与 RMKEY=）。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun C139LoginScreen(
    viewModel: C139AccountViewModel,
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
            // 139 网盘用手机 UA（移动版页面在 WebView 渲染稳定；PC 版 SPA 会因环境检测白屏）
            settings.userAgentString = WebSettings.getDefaultUserAgent(context)
            webViewClient = object : WebViewClient() {
                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                    Log.d(TAG, "onPageStarted: ${LogRedactor.url(url)}")
                    isLoading = true
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    Log.d(TAG, "onPageFinished: ${LogRedactor.url(url)}")
                    isLoading = false
                    // 强制覆盖页面 viewport：适配屏幕宽度 + 允许双指缩放（139 移动版页面 viewport 缺失或限制缩放时生效）
                    view?.evaluateJavascript(
                        "(function(){var m=document.querySelector('meta[name=\"viewport\"]');" +
                            "var c='width=device-width,initial-scale=1.0,maximum-scale=5.0,user-scalable=yes';" +
                            "if(m){m.setAttribute('content',c);}else{var n=document.createElement('meta');n.name='viewport';n.content=c;document.head.appendChild(n);}" +
                            "window.dispatchEvent(new Event('resize'));})()",
                        null
                    )
                }

                override fun onReceivedError(
                    view: WebView?,
                    request: WebResourceRequest?,
                    error: android.webkit.WebResourceError?
                ) {
                    Log.e(TAG, "onReceivedError: code=${error?.errorCode} desc=${error?.description} origin=${LogRedactor.url(request?.url)}")
                    isLoading = false
                }

                override fun onReceivedHttpError(
                    view: WebView?,
                    request: WebResourceRequest?,
                    errorResponse: WebResourceResponse?
                ) {
                    Log.e(TAG, "onReceivedHttpError: status=${errorResponse?.statusCode} reason=${errorResponse?.reasonPhrase} origin=${LogRedactor.url(request?.url)}")
                }

                @RequiresApi(Build.VERSION_CODES.O)
                override fun onRenderProcessGone(view: WebView?, detail: RenderProcessGoneDetail?): Boolean {
                    Log.e(TAG, "onRenderProcessGone: didCrash=${detail?.didCrash()} priority=${detail?.rendererPriorityAtExit()}")
                    // 渲染进程崩溃（139 PC 版页面较重/低端机内存不足）：提示并自动重载一次
                    isLoading = false
                    SnackbarController.show("页面加载异常，正在重试…")
                    view?.post { view.reload() }
                    return true
                }
            }
            webChromeClient = WebChromeClient()
            loadUrl(C139Constants.LOGIN_URL)
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
                title = { Text("139网盘登录", color = iosLabelColor()) },
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
                                val cookie = C139Constants.extractCookies {
                                    CookieManager.getInstance().getCookie(it)
                                }
                                val saved = viewModel.saveC139Account(cookie)
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
                        text = "1. 在下方网页中登录 139 网盘账号（手机号）",
                        fontSize = 14.sp,
                        color = iosLabelColor()
                    )
                    Text(
                        text = "2. 登录完成后点右上角「保存」，自动提取 Cookie",
                        fontSize = 14.sp,
                        color = iosLabelColor()
                    )
                    Text(
                        text = "3. 或点击「粘贴」图标，手动输入 Cookie（需含 Os_SSo_Sid= 与 RMKEY=）",
                        fontSize = 14.sp,
                        color = iosLabelColor()
                    )
                    Text(
                        text = "4. 若网页空白：用系统浏览器打开 yun.139.com 登录后复制 Cookie 粘贴，效果相同",
                        fontSize = 13.sp,
                        color = iosSecondaryLabelColor()
                    )
                    Text(
                        text = "5. Cookie 有效期有限，失效后需重新登录",
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
                    val saved = viewModel.saveC139Account(cookieInput.trim())
                    isSavingManual = false
                    if (saved) {
                        SnackbarController.show("登录成功")
                        showCookieDialog = false
                        onSaved()
                    } else {
                        SnackbarController.show("Cookie 无效，请检查是否包含 Os_SSo_Sid= 与 RMKEY=")
                    }
                }
            },
            dismissText = "取消",
            onDismiss = { if (!isSavingManual) showCookieDialog = false },
            content = {
                Column {
                    Text(
                        text = "从网页登录态复制完整的 Cookie（需包含 Os_SSo_Sid= 与 RMKEY=）",
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
