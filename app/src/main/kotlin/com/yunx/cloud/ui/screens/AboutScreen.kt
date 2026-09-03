package com.yunx.cloud.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.runtime.remember
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.compose.BackHandler
import android.content.Intent
import android.net.Uri
import com.yunx.cloud.R
import com.yunx.cloud.ui.components.IosBlue
import com.yunx.cloud.ui.components.IosGray
import com.yunx.cloud.ui.components.IosGray6
import com.yunx.cloud.ui.components.IosGreen
import com.yunx.cloud.ui.components.IosGroupCard
import com.yunx.cloud.ui.components.IosIconButton
import com.yunx.cloud.ui.components.IosIconTile
import com.yunx.cloud.ui.components.IosListRow
import com.yunx.cloud.ui.components.IosOrange
import com.yunx.cloud.ui.components.IosPurple
import com.yunx.cloud.ui.components.IosSectionHeader
import com.yunx.cloud.ui.components.iosBackgroundColor
import com.yunx.cloud.ui.components.iosLabelColor
import com.yunx.cloud.ui.components.iosSecondaryLabelColor

/**
 * 关于析盘页：应用介绍、支持平台、功能特性、技术栈与免责声明。
 * iOS 风格：分组背景 + 白色分组卡片 + 系统蓝强调色。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    onBack: () -> Unit,
    onPreviewOnboarding: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    // 系统返回键 → 返回主页（而不是退出应用）
    BackHandler { onBack() }
    val pkgInfo = remember {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0) }.getOrNull()
    }
    val versionName = pkgInfo?.versionName ?: "1.0"
    val versionCode = pkgInfo?.versionCode ?: 1

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = iosBackgroundColor(),
        topBar = {
            TopAppBar(
                title = { Text("关于析盘", color = iosLabelColor()) },
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
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ---------- App 头部 ----------
            AppHeader(versionName = versionName, versionCode = versionCode)

            // ---------- 应用简介 ----------
            IosSectionHeader("应用简介")
            IosGroupCard {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    IosIconTile(
                        icon = Icons.Outlined.Cloud,
                        background = IosBlue.copy(alpha = 0.14f),
                        tint = IosBlue,
                        size = 34.dp,
                        iconSize = 18.dp
                    )
                    Spacer(modifier = Modifier.width(13.dp))
                    Column {
                        Text(
                            text = "析盘（YunX）",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = iosLabelColor()
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "一款网盘分享链接解析与高速下载工具。粘贴分享链接，登录网盘账号后即可浏览分享内容并直接高速下载文件。",
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                            color = iosSecondaryLabelColor()
                        )
                    }
                }
            }

            // ---------- 支持平台 ----------
            IosSectionHeader("支持平台")
            IosGroupCard {
                val platforms = listOf(
                    "夸克网盘" to Icons.Outlined.Cloud,
                    "UC 网盘" to Icons.Outlined.Storage,
                    "迅雷网盘" to Icons.Outlined.Speed,
                    "百度网盘" to Icons.Outlined.Link,
                    "139 网盘" to Icons.Outlined.Storage,
                    "123云盘" to Icons.Outlined.Cloud
                )
                platforms.forEachIndexed { index, (name, icon) ->
                    IosListRow(
                        title = name,
                        icon = icon,
                        iconBackground = IosBlue.copy(alpha = 0.12f),
                        iconTint = IosBlue,
                        showDivider = index < platforms.size - 1
                    )
                }
            }

            // ---------- 功能特性 ----------
            IosSectionHeader("功能特性")
            IosGroupCard {
                val features = listOf(
                    "一键解析分享链接" to "夸克 / UC / 迅雷 / 百度 / 139 / 123 分享直链识别",
                    "高速分片下载" to "多线程并发 + 断点续传，充分利用带宽",
                    "取链即删" to "转存后立即清理，不留残留",
                    "凭证本地化" to "Cookie 加密落库，仅存本机"
                )
                features.forEachIndexed { index, (title, desc) ->
                    IosListRow(
                        title = title,
                        subtitle = desc,
                        icon = Icons.Outlined.CheckCircle,
                        iconBackground = IosGreen.copy(alpha = 0.14f),
                        iconTint = IosGreen,
                        showDivider = index < features.size - 1
                    )
                }
            }

            // ---------- 技术栈 ----------
            IosSectionHeader("技术栈")
            IosGroupCard {
                val techs = listOf("Kotlin", "Jetpack Compose", "Material 3", "Room", "OkHttp", "KSP")
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IosIconTile(
                        icon = Icons.Outlined.Code,
                        background = IosPurple.copy(alpha = 0.14f),
                        tint = IosPurple,
                        size = 34.dp,
                        iconSize = 18.dp
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        // 两两一行排布技术标签
                        techs.chunked(2).forEach { rowTechs ->
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                rowTechs.forEach { name ->
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(
                                                if (iosIsDark()) IosGray6.copy(alpha = 0.12f) else IosGray6
                                            )
                                            .padding(horizontal = 10.dp, vertical = 5.dp)
                                    ) {
                                        Text(
                                            text = name,
                                            fontSize = 12.sp,
                                            color = iosSecondaryLabelColor(),
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ---------- 免责声明 ----------
            IosSectionHeader("免责声明")
            IosGroupCard {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    IosIconTile(
                        icon = Icons.Outlined.Shield,
                        background = IosOrange.copy(alpha = 0.14f),
                        tint = IosOrange,
                        size = 34.dp,
                        iconSize = 18.dp
                    )
                    Spacer(modifier = Modifier.width(13.dp))
                    Text(
                        text = "本应用仅供个人学习与技术交流使用，请勿用于任何商业用途。下载内容版权归原作者所有，请于下载后 24 小时内删除。使用本应用产生的任何后果由使用者自行承担。",
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        color = iosSecondaryLabelColor()
                    )
                }
            }

            // ---------- 重新预览欢迎界面 + 开源仓库 ----------
            IosGroupCard {
                IosListRow(
                    title = "重新预览欢迎界面",
                    subtitle = "重新展示首次启动引导页",
                    icon = Icons.Outlined.Info,
                    iconBackground = IosBlue.copy(alpha = 0.14f),
                    iconTint = IosBlue,
                    onClick = onPreviewOnboarding,
                    showDivider = true
                )
                IosListRow(
                    title = "开源仓库",
                    subtitle = "github.com/ly5201314gjx/YunX",
                    icon = Icons.Outlined.Code,
                    iconBackground = IosGray.copy(alpha = 0.16f),
                    iconTint = IosGray,
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/ly5201314gjx/YunX"))
                        context.startActivity(intent)
                    },
                    showDivider = false,
                    trailing = {
                        Icon(
                            imageVector = Icons.Outlined.OpenInNew,
                            contentDescription = null,
                            tint = IosGray3(),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                )
            }

            Text(
                text = "本项目基于 GNU AGPL-3.0 协议开源",
                fontSize = 12.sp,
                color = iosSecondaryLabelColor(),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "析盘 v$versionName · Made with ❤",
                fontSize = 12.sp,
                color = iosSecondaryLabelColor(),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                textAlign = TextAlign.Center
            )
        }
    }
}

/** App 头部：渐变图标 + 应用名 + 版本 + 标语 */
@Composable
private fun AppHeader(versionName: String, versionCode: Int) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 14.dp, bottom = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(84.dp)
                .clip(RoundedCornerShape(22.dp)),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(R.drawable.icon),
                contentDescription = "析盘图标",
                modifier = Modifier
                    .size(84.dp)
                    .clip(RoundedCornerShape(22.dp)),
                contentScale = ContentScale.Crop
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "析盘",
            fontSize = 24.sp,
            fontWeight = FontWeight.SemiBold,
            color = iosLabelColor()
        )
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = "YunX · v$versionName ($versionCode)",
            fontSize = 13.sp,
            color = iosSecondaryLabelColor()
        )
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = "网盘链接解析与高速下载",
            fontSize = 12.sp,
            color = iosSecondaryLabelColor()
        )
    }
}

/** 当前是否暗色（用于技术栈标签底色） */
@Composable
private fun iosIsDark(): Boolean =
    MaterialTheme.colorScheme.background.luminance() < 0.5f

/** iOS 分割线灰（用于 OpenInNew 尾部图标着色） */
@Composable
private fun IosGray3(): androidx.compose.ui.graphics.Color =
    androidx.compose.ui.graphics.Color(0xFFC7C7CC)
