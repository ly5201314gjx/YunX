package com.yunx.cloud.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.content.Intent
import android.net.Uri
import com.yunx.cloud.R
import com.yunx.cloud.ui.components.IosBlue
import com.yunx.cloud.ui.components.IosGray
import com.yunx.cloud.ui.components.IosGreen
import com.yunx.cloud.ui.components.IosGroupCard
import com.yunx.cloud.ui.components.IosIconTile
import com.yunx.cloud.ui.components.IosListRow
import com.yunx.cloud.ui.components.IosOrange
import com.yunx.cloud.ui.components.IosPrimaryButton
import com.yunx.cloud.ui.components.iosBackgroundColor
import com.yunx.cloud.ui.components.iosLabelColor
import com.yunx.cloud.ui.components.iosSecondaryLabelColor

/**
 * 首次启动引导页：介绍析盘（免费）+ 功能特性 + 免责声明。
 * iOS 风格：渐变图标 + 分组内嵌列表卡 + 底部主操作。
 */
@Composable
fun OnboardingScreen(
    onFinish: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(iosBackgroundColor())
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(start = 24.dp, end = 24.dp, top = 56.dp, bottom = 120.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ---------- 顶部：渐变图标 + 名称 + 标语 ----------
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .background(
                        brush = Brush.linearGradient(
                            listOf(IosBlue, com.yunx.cloud.ui.components.IosIndigo)
                        ),
                        shape = RoundedCornerShape(26.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(R.drawable.icon),
                    contentDescription = "析盘图标",
                    modifier = Modifier
                        .size(96.dp)
                        .clip(RoundedCornerShape(26.dp)),
                    contentScale = ContentScale.Crop
                )
            }
            Spacer(modifier = Modifier.height(18.dp))
            Text(
                text = "析盘",
                fontSize = 34.sp,
                fontWeight = FontWeight.SemiBold,
                color = iosLabelColor()
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "网盘分享链接解析与高速下载",
                fontSize = 16.sp,
                color = iosSecondaryLabelColor()
            )

            Spacer(modifier = Modifier.height(36.dp))

            // ---------- 功能特性 ----------
            OnboardingFeature(
                icon = Icons.Outlined.Link,
                title = "一键解析分享链接",
                description = "夸克 / UC / 迅雷 / 百度 / 139 / 123 分享链接自动识别，登录网盘账号后即可解析与下载"
            )
            OnboardingFeature(
                icon = Icons.Outlined.Speed,
                title = "高速分片下载",
                description = "多线程并发 + 断点续传，充分利用带宽"
            )
            OnboardingFeature(
                icon = Icons.Outlined.Storage,
                title = "多平台支持",
                description = "一个应用管理多个网盘账号，统一解析下载入口"
            )
            OnboardingFeature(
                icon = Icons.Outlined.Lock,
                title = "隐私安全",
                description = "登录凭证仅存本机，不上传任何服务器"
            )

            Spacer(modifier = Modifier.height(24.dp))

            // ---------- 免费卡 ----------
            IosGroupCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IosIconTile(
                        icon = Icons.Outlined.Favorite,
                        background = IosGreen.copy(alpha = 0.14f),
                        tint = IosGreen,
                        size = 40.dp,
                        iconSize = 20.dp
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            text = "完全免费",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = iosLabelColor()
                        )
                        Text(
                            text = "无广告、无内购，所有功能永久免费",
                            fontSize = 13.sp,
                            color = iosSecondaryLabelColor()
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            // ---------- 免责声明 ----------
            IosGroupCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Shield,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = IosOrange
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "免责声明：本应用仅供个人学习与技术交流，请勿用于商业用途。" +
                            "下载内容版权归原作者所有，请于下载后 24 小时内删除。" +
                            "使用本应用产生的任何后果由使用者自行承担。",
                        fontSize = 12.sp,
                        color = iosSecondaryLabelColor(),
                        lineHeight = 20.sp
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            // ---------- 开源仓库 ----------
            GitHubCard(context)

            Spacer(modifier = Modifier.height(8.dp))
        }

        // ---------- 底部操作 ----------
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(iosBackgroundColor())
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            IosPrimaryButton(
                text = "开始使用",
                onClick = onFinish,
                tint = IosBlue
            )
        }
    }
}

/** 功能特性条目：圆形图标底 + 标题 + 描述 */
@Composable
private fun OnboardingFeature(
    icon: ImageVector,
    title: String,
    description: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IosIconTile(
            icon = icon,
            background = IosGray.copy(alpha = 0.16f),
            tint = IosBlue,
            size = 44.dp,
            iconSize = 22.dp
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = iosLabelColor()
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                fontSize = 13.sp,
                color = iosSecondaryLabelColor(),
                textAlign = TextAlign.Start
            )
        }
    }
}

/** 开源仓库入口卡片 */
@Composable
private fun GitHubCard(context: android.content.Context) {
    IosGroupCard(modifier = Modifier.fillMaxWidth()) {
        IosListRow(
            title = "开源仓库",
            subtitle = "github.com/ly5201314gjx/YunX",
            icon = Icons.Outlined.Code,
            showDivider = false,
            onClick = {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/ly5201314gjx/YunX"))
                context.startActivity(intent)
            }
        )
    }
}
