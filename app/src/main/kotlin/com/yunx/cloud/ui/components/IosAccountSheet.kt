package com.yunx.cloud.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yunx.cloud.ui.rememberGlobalSnackbarHostState

/**
 * iOS 风格账号信息底部弹窗：头像 + 昵称 + 状态、登录信息分组卡、凭证（可展开/复制）、退出登录。
 * 统一供夸克 / UC / 迅雷 / 百度 / 139 / 123 六个账号弹窗复用。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun IosAccountSheet(
    avatarText: String,
    nickname: String,
    platformLabel: String,
    loginTime: String,
    logoutMessage: String,
    onLogout: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    credentialLabel: String? = null,
    credentialValue: String? = null,
    onCopyCredential: (() -> Unit)? = null,
    extraInfoRows: List<Pair<String, String>> = emptyList()
) {
    var showFullCredential by rememberSaveable { mutableStateOf(false) }
    var showLogoutConfirm by remember { mutableStateOf(false) }

    val previewLimit = 200
    val credentialTruncated = credentialValue?.length?.let { it > previewLimit } ?: false
    val displayCredential = if (credentialValue != null) {
        if (showFullCredential || !credentialTruncated) credentialValue
        else credentialValue.take(previewLimit) + "…"
    } else null

    // ModalBottomSheet 为独立窗口，需自带 Snackbar 宿主
    val snackbarHostState = rememberGlobalSnackbarHostState()

    // 内容滚动到底后继续上滑的滚动量直接消费，避免传给 Sheet 造成上下抽动
    val scrollState = rememberScrollState()
    val sheetNestedScroll = remember(scrollState) {
        object : NestedScrollConnection {
            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                val dy = available.y
                if (dy > 0 && scrollState.value >= scrollState.maxValue) {
                    return Offset(0f, dy)
                }
                return Offset.Zero
            }
        }
    }

    IosBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .then(
                    if (showFullCredential) {
                        // 展开凭证：占满全屏并允许内部滚动
                        Modifier
                            .fillMaxHeight()
                            .verticalScroll(scrollState)
                            .nestedScroll(sheetNestedScroll)
                    } else {
                        Modifier
                    }
                )
        ) {
            // 用户信息
            Row(verticalAlignment = Alignment.CenterVertically) {
                IosAvatar(
                    text = avatarText,
                    background = IosBlue,
                    size = 52.dp
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = nickname,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = iosLabelColor(),
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(IosGreen, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "$platformLabel · 已登录",
                            fontSize = 13.sp,
                            color = iosSecondaryLabelColor()
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 登录信息
            IosSectionHeader("登录信息")
            IosGroupCard {
                IosListRow(
                    title = "登录时间",
                    value = loginTime,
                    showDivider = !extraInfoRows.isEmpty() || credentialLabel != null
                )
                extraInfoRows.forEachIndexed { index, (label, value) ->
                    IosListRow(
                        title = label,
                        value = value,
                        showDivider = index < extraInfoRows.size - 1 || credentialLabel != null
                    )
                }
                if (credentialLabel != null && displayCredential != null) {
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 11.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = credentialLabel,
                                fontSize = 17.sp,
                                color = iosLabelColor(),
                                modifier = Modifier.weight(1f)
                            )
                            // 展开/收起 + 复制
                            if (credentialTruncated) {
                                Text(
                                    text = if (showFullCredential) "收起" else "展开全部",
                                    fontSize = 15.sp,
                                    color = IosBlue,
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .clickable(interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }, indication = null) {
                                            showFullCredential = !showFullCredential
                                        }
                                        .padding(horizontal = 6.dp, vertical = 4.dp)
                                )
                            }
                            Icon(
                                imageVector = Icons.Outlined.ContentCopy,
                                contentDescription = null,
                                tint = IosBlue,
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .clickable(interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }, indication = null) {
                                        onCopyCredential?.invoke()
                                    }
                                    .padding(6.dp)
                                    .size(16.dp)
                            )
                        }
                        Text(
                            text = displayCredential,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            lineHeight = 15.sp,
                            color = iosSecondaryLabelColor(),
                            modifier = Modifier.padding(start = 14.dp, end = 14.dp, bottom = 12.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 退出登录
            IosBlockButton(
                text = "退出登录",
                tint = IosRed,
                onClick = { showLogoutConfirm = true }
            )

            // 复制提示（ModalBottomSheet 为独立窗口，需自带 Snackbar 宿主）
            Spacer(modifier = Modifier.height(8.dp))
            SnackbarHost(hostState = snackbarHostState)
        }
    }

    // 退出登录二次确认
    if (showLogoutConfirm) {
        IosAlertDialog(
            onDismissRequest = { showLogoutConfirm = false },
            title = "退出登录",
            message = logoutMessage,
            confirmText = "退出",
            confirmStyle = IosButtonStyle.Destructive,
            onConfirm = {
                showLogoutConfirm = false
                onLogout()
            },
            dismissText = "取消",
            onDismiss = { showLogoutConfirm = false }
        )
    }
}
