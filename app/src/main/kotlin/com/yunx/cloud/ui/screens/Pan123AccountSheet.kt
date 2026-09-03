package com.yunx.cloud.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.yunx.cloud.data.db.Pan123AccountEntity
import com.yunx.cloud.ui.SnackbarController
import com.yunx.cloud.ui.components.IosAccountSheet
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 已登录 123 账号的底部弹窗：展示用户信息、登录时间、Token（可展开/复制），并提供退出登录。
 * iOS 风格。
 */
@Composable
fun Pan123AccountSheet(
    account: Pan123AccountEntity,
    onLogout: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val loginTime = remember(account.updatedAt) {
        SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(account.updatedAt))
    }
    IosAccountSheet(
        avatarText = account.nickname.take(1),
        nickname = account.nickname,
        platformLabel = "123云盘",
        loginTime = loginTime,
        logoutMessage = "确定要退出当前 123 账号吗？退出后将清除本地凭证。",
        onLogout = onLogout,
        onDismiss = onDismiss,
        credentialLabel = "Token（JWT）",
        credentialValue = account.accessToken,
        onCopyCredential = {
            copyToClipboard(context, account.accessToken)
            SnackbarController.show("Token 已复制")
        },
        extraInfoRows = listOf("登录账号" to account.account.ifBlank { account.nickname })
    )
}

private fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("pan123_token", text))
}
