package com.yunx.cloud.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yunx.cloud.data.update.UpdateChecker
import com.yunx.cloud.ui.components.IosAlertDialog
import com.yunx.cloud.ui.components.IosBlue
import com.yunx.cloud.ui.components.IosGray6
import com.yunx.cloud.ui.components.iosLabelColor
import com.yunx.cloud.ui.components.iosSecondaryLabelColor

/**
 * 发现新版本弹窗（iOS 风格）：
 * 标题 + 当前/最新版本 + 更新说明（可滚动）+ 下载更新 / 稍后 / 忽略本次 / 镜像站。
 */
@Composable
fun UpdateDialog(
    currentVersion: String,
    release: UpdateChecker.Release,
    onDownload: () -> Unit,
    onLater: () -> Unit,
    onIgnore: () -> Unit,
    downloading: Boolean = false,
    /** 使用镜像站下载（可选）；为 null 时不显示镜像站入口 */
    onDownloadMirror: (() -> Unit)? = null
) {
    IosAlertDialog(
        onDismissRequest = onLater,
        title = "发现新版本",
        confirmText = if (downloading) "下载中…" else "下载更新",
        onConfirm = onDownload,
        dismissText = "稍后",
        onDismiss = onLater,
        content = {
            // 版本号：最新 + 当前
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = release.tagName,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = IosBlue
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "当前 $currentVersion",
                    fontSize = 13.sp,
                    color = iosSecondaryLabelColor()
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "更新内容",
                fontSize = 13.sp,
                color = iosSecondaryLabelColor()
            )
            Spacer(modifier = Modifier.height(6.dp))
            // 更新说明（可滚动，防止长文本撑爆弹窗）
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isDark()) IosGray6.copy(alpha = 0.6f) else IosGray6)
                    .padding(12.dp)
            ) {
                Text(
                    text = release.body.ifBlank { "暂无更新说明" },
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    fontSize = 13.sp,
                    lineHeight = 20.sp,
                    color = iosLabelColor()
                )
            }
            if (downloading) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(14.dp),
                        strokeWidth = 2.dp,
                        color = IosBlue
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "正在下载更新…",
                        fontSize = 13.sp,
                        color = iosSecondaryLabelColor()
                    )
                }
            }
            if (onDownloadMirror != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "使用镜像站下载",
                    fontSize = 15.sp,
                    color = IosBlue,
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onDownloadMirror
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "忽略本次",
                fontSize = 13.sp,
                color = iosSecondaryLabelColor(),
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onIgnore
                    )
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    )
}

/** 当前是否暗色（用于更新说明底板） */
@Composable
private fun isDark(): Boolean =
    androidx.compose.material3.MaterialTheme.colorScheme.background.luminance() < 0.5f
