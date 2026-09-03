package com.yunx.cloud.ui.login

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yunx.cloud.ui.components.IosBlue
import com.yunx.cloud.ui.components.IosIconButton
import com.yunx.cloud.ui.components.IosPasswordField
import com.yunx.cloud.ui.components.IosPrimaryButton
import com.yunx.cloud.ui.components.IosTextField
import com.yunx.cloud.ui.components.iosBackgroundColor
import com.yunx.cloud.ui.components.iosLabelColor
import com.yunx.cloud.ui.components.iosSecondaryLabelColor
import com.yunx.cloud.ui.viewmodel.Pan123AccountViewModel

/**
 * 123 云盘登录页：账号（手机号）+ 密码表单登录（文档 §5.1：POST user.123pan.cn/api/user/sign_in 换 JWT）。
 * 123 无短信/验证码登录，纯账号密码；成功即落库并自动关闭登录页。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Pan123LoginScreen(
    viewModel: Pan123AccountViewModel,
    onBack: () -> Unit,
    onSaved: () -> Unit
) {
    val account by viewModel.pan123Account.collectAsState()
    val error = viewModel.loginError
    var username by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }

    // 登录错误提示
    LaunchedEffect(error) {
        error?.let {
            SnackbarController.show(it)
            viewModel.consumeLoginError()
        }
    }
    // 登录成功后自动关闭登录页
    LaunchedEffect(account) {
        if (account != null) onSaved()
    }

    BackHandler { onBack() }

    val snackbarHostState = rememberGlobalSnackbarHostState()

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("123云盘登录", color = iosLabelColor()) },
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
                text = "登录123云盘",
                fontSize = 28.sp,
                fontWeight = FontWeight.SemiBold,
                color = iosLabelColor()
            )
            Text(
                text = "使用 123 云盘账号登录，支持解析与下载分享文件",
                fontSize = 15.sp,
                color = iosSecondaryLabelColor()
            )

            Spacer(modifier = Modifier.height(4.dp))
            IosTextField(
                value = username,
                onValueChange = { username = it },
                placeholder = "手机号 / 账号"
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
                enabled = username.isNotBlank() && password.isNotBlank() && !viewModel.isLoggingIn,
                loading = viewModel.isLoggingIn
            )

            Text(
                text = "凭证为登录后签发的 JWT（约 90 天有效），仅用于请求 123 云盘接口",
                fontSize = 12.sp,
                color = iosSecondaryLabelColor()
            )
        }
    }
}
