package com.yunx.cloud.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.yunx.cloud.data.db.XunleiAccountEntity
import com.yunx.cloud.ui.components.IosAccountSheet
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 已登录迅雷账号的底部弹窗：昵称、设备号、登录时间 + 退出登录（二次确认）。
 * iOS 风格。
 */
@Composable
fun XunleiAccountSheet(
    account: XunleiAccountEntity,
    onLogout: () -> Unit,
    onDismiss: () -> Unit
) {
    val loginTime = remember(account.updatedAt) {
        SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(account.updatedAt))
    }
    IosAccountSheet(
        avatarText = account.nickname.take(1),
        nickname = account.nickname,
        platformLabel = "迅雷网盘",
        loginTime = loginTime,
        logoutMessage = "确定要退出当前迅雷账号吗？",
        onLogout = onLogout,
        onDismiss = onDismiss,
        extraInfoRows = listOf("设备号" to (account.deviceId.ifBlank { "-" }))
    )
}
