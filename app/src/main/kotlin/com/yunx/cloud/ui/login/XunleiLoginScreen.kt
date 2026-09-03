package com.yunx.cloud.ui.login

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import com.yunx.cloud.ui.SnackbarController
import com.yunx.cloud.ui.rememberGlobalSnackbarHostState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yunx.cloud.ui.components.IosBlockButton
import com.yunx.cloud.ui.components.IosBlue
import com.yunx.cloud.ui.components.IosIconButton
import com.yunx.cloud.ui.components.IosPasswordField
import com.yunx.cloud.ui.components.IosPrimaryButton
import com.yunx.cloud.ui.components.IosTextField
import com.yunx.cloud.ui.components.iosBackgroundColor
import com.yunx.cloud.ui.components.iosLabelColor
import com.yunx.cloud.ui.components.iosSecondaryLabelColor
import com.yunx.cloud.ui.viewmodel.XunleiAccountViewModel

/**
 * 迅雷网盘登录页：账号+密码登录，触发风控时切换短信验证码流程。
 * 步骤：账号密码 →（需要时）发送短信 → 输入验证码 → 完成。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun XunleiLoginScreen(
    viewModel: XunleiAccountViewModel,
    onBack: () -> Unit,
    onSaved: () -> Unit,
    onVerify: (url: String, deviceId: String) -> Unit = { _, _ -> }
) {
    val context = LocalContext.current
    val step = viewModel.loginStep
    val error = viewModel.loginError
    val smsSent = viewModel.smsSent
    // collectAsState 订阅账号：登录成功后 account 变非空，必触发重组 → 自动关闭登录页
    val account by viewModel.xunleiAccount.collectAsState()

    var username by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var smsCode by rememberSaveable { mutableStateOf("") }
    var isSending by remember { mutableStateOf(false) }

    // 登录错误提示
    LaunchedEffect(error) {
        error?.let {
            SnackbarController.show(it)
            viewModel.consumeLoginError()
        }
    }
    // 登录成功后自动关闭登录页（短信/密码任一方式成功，账号非空即关闭）
    LaunchedEffect(account) {
        if (account != null) onSaved()
    }

    BackHandler { onBack() }

    // 全局 Snackbar 宿主
    val snackbarHostState = rememberGlobalSnackbarHostState()

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("迅雷网盘登录", color = iosLabelColor()) },
                navigationIcon = {
                    IosIconButton(
                        icon = Icons.AutoMirrored.Filled.ArrowBack,
                        tint = IosBlue,
                        onClick = onBack,
                        contentDescription = "返回"
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = iosBackgroundColor()
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = if (step?.needSms == true) "短信验证" else "登录迅雷网盘",
                fontSize = 28.sp,
                fontWeight = FontWeight.SemiBold,
                color = iosLabelColor()
            )
            Text(
                text = if (step?.needSms == true) {
                    if (smsSent) "账号密码登录触发安全验证，验证码已发送至 ${username}"
                    else "账号密码登录触发安全验证，请点击下方「发送验证码」"
                } else {
                    "使用迅雷账号登录，支持解析与下载分享文件"
                },
                fontSize = 15.sp,
                color = iosSecondaryLabelColor()
            )

            if (step == null || !step.needSms) {
                Spacer(modifier = Modifier.height(4.dp))
                IosTextField(
                    value = username,
                    onValueChange = { username = it },
                    placeholder = "手机号 / 邮箱"
                )
                IosPasswordField(
                    value = password,
                    onValueChange = { password = it },
                    placeholder = "密码"
                )
                IosPrimaryButton(
                    text = "登录",
                    onClick = { viewModel.login(username, password) },
                    tint = IosBlue,
                    enabled = username.isNotBlank() && password.isNotBlank() && !isSending
                )
            } else {
                Spacer(modifier = Modifier.height(4.dp))
                IosTextField(
                    value = smsCode,
                    onValueChange = { smsCode = it },
                    placeholder = "短信验证码",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                IosPrimaryButton(
                    text = "验证并登录",
                    onClick = {
                        isSending = true
                        viewModel.loginWithSms(
                            username, smsCode,
                            step.smsCreditKey, step.smsToken
                        )
                        isSending = false
                    },
                    tint = IosBlue,
                    enabled = smsCode.isNotBlank()
                )
                if (!smsSent) {
                    // 进入界面不会自动发送验证码：主按钮「发送验证码」提示用户主动获取
                    IosBlockButton(
                        text = "发送验证码",
                        onClick = {
                            viewModel.sendSms(username)
                            SnackbarController.show("验证码已发送")
                        },
                        tint = IosBlue
                    )
                } else {
                    TextButton(
                        onClick = {
                            viewModel.sendSms(username)
                            SnackbarController.show("验证码已发送")
                        },
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Text(
                            text = "重新发送验证码",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = IosBlue
                        )
                    }
                }
                Text(
                    text = "若始终收不到短信，请确认手机号正确，或稍后重试 / 切换网络",
                    fontSize = 12.sp,
                    color = iosSecondaryLabelColor(),
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )

                // 短信发不出时的应用内验证兜底（应用内 WebView 承载验证页；核心验证仍走自有短信流）
                if (step.reviewUrl.isNotBlank()) {
                    TextButton(
                        onClick = {
                            // 用与登录请求一致的设备签名（deviceSign = div101.xxx）：
                            // 验证页会把 URL 里的 deviceid 原样当 devicesign 用，
                            // 必须与 v3/login 的 devicesign 字段一致，否则报"登录信息已过期"
                            onVerify(step.reviewUrl, com.yunx.cloud.data.network.XunleiDeviceFingerprint.deviceSign())
                        },
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Text(
                            text = "短信收不到？应用内验证",
                            fontSize = 15.sp,
                            color = IosBlue
                        )
                    }
                    Text(
                        text = "应用内完成验证后，将自动重新登录",
                        fontSize = 12.sp,
                        color = iosSecondaryLabelColor(),
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }
            }

            // 未设置密码：跳转迅雷官网设置（浏览器打开）
            Spacer(modifier = Modifier.height(8.dp))
            TextButton(
                onClick = {
                    runCatching {
                        context.startActivity(
                            Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse("https://i.xunlei.com/xluser/validate/findpwd_acc.html")
                            )
                        )
                    }
                },
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text(
                    text = "未设置密码，点我前往设置",
                    fontSize = 15.sp,
                    color = IosBlue
                )
            }
        }
    }
}
