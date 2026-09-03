package com.yunx.cloud.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.yunx.cloud.data.db.UCAccountEntity
import com.yunx.cloud.ui.SnackbarController
import com.yunx.cloud.ui.components.IosAccountSheet
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 已登录 UC 账号的底部弹窗：展示用户信息、登录时间、Cookie（可展开/复制），并提供退出登录。
 * iOS 风格。
 */
@Composable
fun UCAccountSheet(
    account: UCAccountEntity,
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
        platformLabel = "UC网盘",
        loginTime = loginTime,
        logoutMessage = "确定要退出当前 UC 账号吗？退出后将清除本地 Cookie。",
        onLogout = onLogout,
        onDismiss = onDismiss,
        credentialLabel = "Cookie",
        credentialValue = account.cookie,
        onCopyCredential = {
            copyToClipboard(context, account.cookie)
            SnackbarController.show("Cookie 已复制")
        }
    )
}

private fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("uc_cookie", text))
}
