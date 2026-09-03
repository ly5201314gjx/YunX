package com.yunx.cloud.ui.resolve

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import com.yunx.cloud.data.network.model.DownloadLink
import com.yunx.cloud.ui.SnackbarController
import com.yunx.cloud.ui.components.IosAlertDialog
import com.yunx.cloud.ui.components.IosBlue
import com.yunx.cloud.ui.components.NativeSpring
import com.yunx.cloud.ui.components.NativeSpringColorSoft
import com.yunx.cloud.ui.components.iosLabelColor
import com.yunx.cloud.ui.components.iosPressColor
import com.yunx.cloud.ui.components.iosSecondaryLabelColor
import com.yunx.cloud.ui.rememberGlobalSnackbarHostState

/**
 * 下载直链弹窗（iOS 风格）：展示文件名与直链（点击/长按直链复制），支持「开始下载」（分片多线程下载）。
 * 点「关闭」或弹窗外（管壁）关闭 = 放弃下载，由上层清理临时转存。
 */
@Composable
fun DownloadLinkDialog(
    link: DownloadLink,
    onDownload: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    // Dialog 内提示宿主（AlertDialog 为独立窗口）
    val snackbarHostState = rememberGlobalSnackbarHostState()

    IosAlertDialog(
        onDismissRequest = onDismiss,
        title = link.filename,
        message = "下载直链已生成（有效期约 15-30 分钟）",
        confirmText = "开始下载",
        onConfirm = onDownload,
        dismissText = "关闭",
        onDismiss = onDismiss,
        modifier = modifier
    ) {
        Spacer(modifier = Modifier.height(4.dp))
        // 直链卡片：点击/长按复制
        val interaction = remember { MutableInteractionSource() }
        val pressed by interaction.collectIsPressedAsState()
        val bg by animateColorAsState(
            targetValue = if (pressed) iosPressColor() else if (isDark(context)) Color.Transparent else IosBlue.copy(alpha = 0.06f),
            animationSpec = NativeSpringColorSoft,
            label = "iosLinkCardBg"
        )
        val scale by animateFloatAsState(
            targetValue = if (pressed) 0.985f else 1f,
            animationSpec = NativeSpring,
            label = "iosLinkCardScale"
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
                .clip(RoundedCornerShape(10.dp))
                .background(bg)
                .combinedClickable(
                    onClick = {
                        copyToClipboard(context, link.downloadUrl)
                        SnackbarController.show("下载链接已复制")
                    },
                    onLongClick = {
                        copyToClipboard(context, link.downloadUrl)
                        SnackbarController.show("下载链接已复制")
                    }
                )
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(IosBlue.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Link,
                    contentDescription = null,
                    tint = IosBlue,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = link.downloadUrl,
                modifier = Modifier.weight(1f),
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                lineHeight = 15.sp,
                color = iosLabelColor(),
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.width(6.dp))
            Icon(
                imageVector = Icons.Outlined.ContentCopy,
                contentDescription = "复制",
                tint = IosBlue,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = "点击「开始下载」将分片多线程下载并保存到 Download 目录",
            fontSize = 12.sp,
            lineHeight = 16.sp,
            color = iosSecondaryLabelColor(),
            textAlign = TextAlign.Start,
            modifier = Modifier.fillMaxWidth()
        )
        SnackbarHost(hostState = snackbarHostState)
    }
}

private fun isDark(context: Context): Boolean {
    val mode = context.resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK
    return mode == android.content.res.Configuration.UI_MODE_NIGHT_YES
}

private fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("download_url", text))
}
