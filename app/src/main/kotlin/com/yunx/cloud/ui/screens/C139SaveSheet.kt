package com.yunx.cloud.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.SaveAlt
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yunx.cloud.ui.SnackbarController
import com.yunx.cloud.ui.components.IosBlue
import com.yunx.cloud.ui.components.IosIconTile
import com.yunx.cloud.ui.components.IosPrimaryButton
import com.yunx.cloud.ui.components.NativeSpringSoft
import com.yunx.cloud.ui.components.iosCardColor
import com.yunx.cloud.ui.components.iosLabelColor
import com.yunx.cloud.ui.components.iosSecondaryLabelColor
import com.yunx.cloud.ui.components.isIosDark
import com.yunx.cloud.ui.rememberGlobalSnackbarHostState
import com.yunx.cloud.ui.resolve.BackToParentItem
import com.yunx.cloud.ui.resolve.CrumbBar
import com.yunx.cloud.ui.resolve.ShareFileRow
import com.yunx.cloud.ui.viewmodel.C139CloudUiState
import com.yunx.cloud.ui.viewmodel.C139CloudViewModel
import com.yunx.cloud.ui.viewmodel.ResolveViewModel

/**
 * 转存到 139 网盘弹窗：浏览 139 个人网盘目录（只进文件夹），确认后转存到当前目录。
 * 复用 C139CloudViewModel 做目录浏览（与网盘页同一实例）。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun C139SaveSheet(
    resolveViewModel: ResolveViewModel,
    cloudViewModel: C139CloudViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val cloudState by cloudViewModel.uiState.collectAsState()
    val saving = resolveViewModel.isSaving
    val message = resolveViewModel.saveMessage

    LaunchedEffect(Unit) {
        cloudViewModel.loadRoot()
    }
// 转存结果提示
    LaunchedEffect(message) {
        if (message != null) {
            SnackbarController.show(message)
            resolveViewModel.consumeSaveMessage()
        }
    }

    // ModalBottomSheet 为独立窗口，需自带 Snackbar 宿主
    val snackbarHostState = rememberGlobalSnackbarHostState()

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = iosCardColor(),
        dragHandle = {
            // iOS 抓取条：细长灰条
            Box(
                modifier = Modifier
                    .padding(top = 10.dp, bottom = 4.dp)
                    .size(width = 36.dp, height = 5.dp)
                    .clip(CircleShape)
                    .background(if (isIosDark()) Color(0xFF3A3A3C) else Color(0xFFD1D1D6))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 24.dp, end = 24.dp, top = 4.dp, bottom = 32.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IosIconTile(
                    icon = Icons.Outlined.SaveAlt,
                    background = IosBlue.copy(alpha = 0.13f),
                    tint = IosBlue,
                    size = 40.dp,
                    iconSize = 20.dp
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "转存到139网盘",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = iosLabelColor()
                    )
                    Text(
                        text = resolveViewModel.saveTarget?.fname ?: "",
                        fontSize = 13.sp,
                        color = iosSecondaryLabelColor(),
                        maxLines = 1
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            CrumbBar(
                rootTitle = "根目录",
                pathNames = (cloudState as? C139CloudUiState.Loaded)?.pathNames ?: emptyList(),
                onNavigate = { cloudViewModel.navigateToLevel(it) }
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 返回上一级：固定在目录区上方（与网盘移动弹窗一致）
            if ((cloudState as? C139CloudUiState.Loaded)?.pathNames?.isNotEmpty() == true) {
                BackToParentItem(onClick = { cloudViewModel.back() })
                Spacer(modifier = Modifier.height(4.dp))
            }

            // 目录切换：淡入过渡（与网盘移动弹窗一致）
            AnimatedContent(
                targetState = cloudState,
                transitionSpec = { fadeIn(NativeSpringSoft) togetherWith fadeOut(NativeSpringSoft) },
                label = "c139SaveState"
            ) { s ->
                when (s) {
                    is C139CloudUiState.Loading -> Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.5.dp,
                        color = IosBlue
                    )
                }

                is C139CloudUiState.Error -> Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = s.message,
                            fontSize = 14.sp,
                            color = iosSecondaryLabelColor(),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        TextButton(onClick = { cloudViewModel.loadRoot() }) {
                            Text("重试", color = IosBlue, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                is C139CloudUiState.Loaded -> {
                    val dirs = s.files.filter { it.isdir }
                    if (dirs.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "当前目录没有子文件夹，可直接转存到此目录",
                                fontSize = 14.sp,
                                color = iosSecondaryLabelColor(),
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 280.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            items(dirs, key = { it.fid }) { dir ->
                                ShareFileRow(
                                    file = dir,
                                    onClick = { cloudViewModel.openFolder(dir) }
                                )
                            }
                        }
                    }
            }
            }
            }

            Spacer(modifier = Modifier.height(20.dp))

            val currentDirName =
                (cloudState as? C139CloudUiState.Loaded)?.pathNames?.lastOrNull() ?: "根目录"
            IosPrimaryButton(
                text = "转存到此目录（$currentDirName）",
                icon = Icons.Outlined.Folder,
                tint = IosBlue,
                enabled = !saving,
                loading = saving,
                onClick = {
                    val dirId = (cloudState as? C139CloudUiState.Loaded)?.dirId ?: "/"
                    resolveViewModel.saveToCloud(dirId)
                }
            )

            // 转存结果提示（ModalBottomSheet 为独立窗口，需自带 Snackbar 宿主）
            SnackbarHost(hostState = snackbarHostState)
        }
    }
}