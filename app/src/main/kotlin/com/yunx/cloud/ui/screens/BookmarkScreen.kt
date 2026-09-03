package com.yunx.cloud.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yunx.cloud.data.db.BookmarkEntity
import com.yunx.cloud.data.network.ShareLinkParser
import com.yunx.cloud.ui.components.IosAlertDialog
import com.yunx.cloud.ui.components.IosBlue
import com.yunx.cloud.ui.components.IosGray
import com.yunx.cloud.ui.components.IosGroupCard
import com.yunx.cloud.ui.components.IosIconButton
import com.yunx.cloud.ui.components.IosIconTile
import com.yunx.cloud.ui.components.IosMultilineTextField
import com.yunx.cloud.ui.components.IosPrimaryButton
import com.yunx.cloud.ui.components.IosRed
import com.yunx.cloud.ui.components.IosTextField
import com.yunx.cloud.ui.components.NativeSpringColorSoft
import com.yunx.cloud.ui.components.NativeSpringIntSizeSoft
import com.yunx.cloud.ui.components.NativeSpringSoft
import com.yunx.cloud.ui.components.iosBackgroundColor
import com.yunx.cloud.ui.components.iosCardColor
import com.yunx.cloud.ui.components.iosLabelColor
import com.yunx.cloud.ui.components.iosPressColor
import com.yunx.cloud.ui.components.iosSecondaryLabelColor
import com.yunx.cloud.ui.components.isIosDark
import com.yunx.cloud.ui.rememberGlobalSnackbarHostState
import com.yunx.cloud.ui.viewmodel.BookmarkViewModel

/** 「自定义分类」虚拟选项标识（不参与持久化，仅用于弹窗交互） */
private const val CUSTOM_CATEGORY = "__custom__"

/**
 * 收藏网盘链接页：分类筛选 + 收藏列表，支持新增 / 解析 / 复制 / 修改分类 / 删除。
 * iOS 风格：分组背景 + 白色分组卡片 + 系统蓝强调色。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookmarkScreen(
    viewModel: BookmarkViewModel,
    onBack: () -> Unit,
    /** 点击收藏 → 关闭本页并切到解析页自动解析该链接 */
    onResolve: (link: String, pwd: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val bookmarks by viewModel.bookmarks.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val context = LocalContext.current

    // 独立全屏覆盖页：自带 Snackbar 宿主（覆盖层会遮挡主页 Scaffold 的 SnackbarHost）
    val snackbarHostState = rememberGlobalSnackbarHostState()

    // null 表示「全部」
    var selectedCategory by rememberSaveable { mutableStateOf<String?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }
    var editingBookmark by remember { mutableStateOf<BookmarkEntity?>(null) }
    var menuBookmark by remember { mutableStateOf<BookmarkEntity?>(null) }

    val filtered = remember(bookmarks, selectedCategory) {
        val cat = selectedCategory
        if (cat == null) bookmarks else bookmarks.filter { it.category == cat }
    }

    BackHandler { onBack() }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = iosBackgroundColor(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("收藏网盘链接", color = iosLabelColor()) },
                navigationIcon = {
                    IosIconButton(
                        icon = Icons.AutoMirrored.Filled.ArrowBack,
                        tint = IosBlue,
                        onClick = onBack,
                        contentDescription = "返回"
                    )
                },
                actions = {
                    IosIconButton(
                        icon = Icons.Outlined.Add,
                        tint = IosBlue,
                        onClick = { showAddDialog = true },
                        contentDescription = "添加收藏"
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = iosBackgroundColor()
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // 分类筛选：作为列表头部，随列表一起上下滚动
            item(key = "categories") {
                CategoryFilterBar(
                    categories = categories,
                    selected = selectedCategory,
                    onSelect = { selectedCategory = it }
                )
            }

            if (filtered.isEmpty()) {
                item(key = "empty") {
                    EmptyBookmark(onAdd = { showAddDialog = true })
                }
            } else {
                items(filtered, key = { it.id }) { bookmark ->
                    BookmarkRow(
                        bookmark = bookmark,
                        onClick = { onResolve(bookmark.link, bookmark.pwd) },
                        onLongClick = { menuBookmark = bookmark }
                    )
                }
            }
        }
    }

    // 添加收藏弹窗
    if (showAddDialog) {
        AddBookmarkDialog(
            categories = categories,
            onConfirm = { link, title, category, pwd ->
                showAddDialog = false
                val parsed = ShareLinkParser.parse(link)
                viewModel.addBookmark(
                    link = link,
                    title = title,
                    platform = parsed?.platform?.name.orEmpty(),
                    pwd = pwd.ifBlank { parsed?.pwd.orEmpty() },
                    category = category
                )
            },
            onDismiss = { showAddDialog = false }
        )
    }

    // 修改分类弹窗
    editingBookmark?.let { bookmark ->
        EditCategoryDialog(
            currentCategory = bookmark.category,
            categories = categories,
            onConfirm = { category ->
                viewModel.updateCategory(bookmark.id, category)
                editingBookmark = null
            },
            onDismiss = { editingBookmark = null }
        )
    }

    // 长按操作菜单
    menuBookmark?.let { bookmark ->
        BookmarkMenuDialog(
            bookmark = bookmark,
            onResolve = {
                menuBookmark = null
                onResolve(bookmark.link, bookmark.pwd)
            },
            onCopy = {
                menuBookmark = null
                copyToClipboard(context, bookmark.link)
                com.yunx.cloud.ui.SnackbarController.show("链接已复制")
            },
            onEditCategory = {
                menuBookmark = null
                editingBookmark = bookmark
            },
            onDelete = {
                menuBookmark = null
                viewModel.delete(bookmark.id)
            },
            onDismiss = { menuBookmark = null }
        )
    }
}

/** 分类筛选胶囊：全部 + 各分类（iOS 风格 chip） */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CategoryFilterBar(
    categories: List<String>,
    selected: String?,
    onSelect: (String?) -> Unit
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        IosChip(text = "全部", selected = selected == null, onClick = { onSelect(null) })
        categories.forEach { cat ->
            IosChip(text = cat, selected = selected == cat, onClick = { onSelect(cat) })
        }
    }
}

/** iOS 分类胶囊：选中系统蓝填充，未选中浅灰，颜色弹簧过渡 */
@Composable
private fun IosChip(text: String, selected: Boolean, onClick: () -> Unit) {
    val bg by animateColorAsState(
        targetValue = if (selected) IosBlue else if (isIosDark()) Color(0xFF2C2C2E) else Color(0xFFE9E9EB),
        animationSpec = NativeSpringColorSoft,
        label = "iosChipBg"
    )
    val fg by animateColorAsState(
        targetValue = if (selected) Color.White else iosSecondaryLabelColor(),
        animationSpec = NativeSpringColorSoft,
        label = "iosChipFg"
    )
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(9.dp))
            .background(bg)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 13.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = fg,
            maxLines = 1
        )
    }
}

/** 收藏列表项：平台 / 分类标签 + 标题 + 链接，点击解析、长按打开菜单（iOS 分组卡片） */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BookmarkRow(
    bookmark: BookmarkEntity,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val bg by androidx.compose.animation.animateColorAsState(
        targetValue = if (pressed) iosPressColor() else Color.Transparent,
        animationSpec = NativeSpringColorSoft,
        label = "iosBookmarkBg"
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(iosCardColor())
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(bg)
                .combinedClickable(
                    interactionSource = interaction,
                    indication = null,
                    onClick = onClick,
                    onLongClick = onLongClick
                )
                .padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            IosIconTile(
                icon = Icons.Outlined.Link,
                background = IosBlue.copy(alpha = 0.13f),
                tint = IosBlue,
                size = 34.dp,
                iconSize = 17.dp
            )
            Spacer(modifier = Modifier.width(13.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (bookmark.platform.isNotBlank()) {
                        TagPill(
                            text = bookmarkPlatformLabel(bookmark.platform),
                            bg = IosBlue.copy(alpha = 0.12f),
                            fg = IosBlue
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    TagPill(
                        text = bookmark.category,
                        bg = if (isIosDark()) Color(0xFF2C2C2E) else Color(0xFFE9E9EB),
                        fg = iosSecondaryLabelColor()
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = bookmark.title.ifBlank { bookmark.link },
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Medium,
                    color = iosLabelColor(),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (bookmark.title.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = bookmark.link,
                        fontSize = 13.sp,
                        color = iosSecondaryLabelColor(),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

/** 平台 / 分类小标签 */
@Composable
private fun TagPill(text: String, bg: Color, fg: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bg)
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = text,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = fg,
            maxLines = 1
        )
    }
}

/** 空状态 */
@Composable
private fun EmptyBookmark(onAdd: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Outlined.BookmarkBorder,
            contentDescription = null,
            modifier = Modifier.size(46.dp),
            tint = iosSecondaryLabelColor()
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "还没有收藏任何网盘链接",
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            color = iosLabelColor()
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "可在解析页点击「添加至收藏」，或点击右上角 + 手动添加",
            fontSize = 13.sp,
            color = iosSecondaryLabelColor(),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        IosPrimaryButton(
            text = "添加收藏",
            icon = Icons.Outlined.Add,
            tint = IosBlue,
            onClick = onAdd,
            modifier = Modifier.padding(horizontal = 40.dp)
        )
    }
}

/** 新增收藏弹窗：链接 + 标题 + 提取码 + 分类（iOS 风格） */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AddBookmarkDialog(
    categories: List<String>,
    onConfirm: (link: String, title: String, category: String, pwd: String) -> Unit,
    onDismiss: () -> Unit
) {
    var link by remember { mutableStateOf("") }
    var title by remember { mutableStateOf("") }
    var pwd by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(BookmarkEntity.DEFAULT_CATEGORY) }
    var customCategory by remember { mutableStateOf("") }

    IosAlertDialog(
        onDismissRequest = onDismiss,
        title = "添加收藏",
        confirmText = "收藏",
        confirmEnabled = link.isNotBlank(),
        onConfirm = {
            onConfirm(
                link.trim(),
                title.trim(),
                customCategory.ifBlank { selectedCategory },
                pwd.trim()
            )
        },
        dismissText = "取消",
        onDismiss = onDismiss,
        content = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                IosMultilineTextField(
                    value = link,
                    onValueChange = { link = it },
                    placeholder = "粘贴分享链接"
                )
                IosTextField(
                    value = title,
                    onValueChange = { title = it },
                    placeholder = "标题（可选）"
                )
                IosTextField(
                    value = pwd,
                    onValueChange = { pwd = it },
                    placeholder = "提取码（可选）"
                )
                Text(
                    text = "分类",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = iosSecondaryLabelColor()
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    categories.forEach { cat ->
                        IosChip(
                            text = cat,
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = cat }
                        )
                    }
                }
                IosTextField(
                    value = customCategory,
                    onValueChange = { customCategory = it },
                    placeholder = "自定义分类（可选）"
                )
            }
        }
    )
}

/** 解析详情页「添加至收藏」弹窗：可自定义标题；分类选择含「自定义」选项，点击后展开输入框（iOS 风格） */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun AddToBookmarkDialog(
    title: String,
    initialCategory: String,
    categories: List<String>,
    onConfirm: (title: String, category: String) -> Unit,
    onDismiss: () -> Unit
) {
    var titleInput by remember { mutableStateOf(title) }
    var selectedCategory by remember { mutableStateOf(initialCategory) }
    var customCategory by remember { mutableStateOf("") }
    val isCustom = selectedCategory == CUSTOM_CATEGORY

    IosAlertDialog(
        onDismissRequest = onDismiss,
        title = "添加至收藏",
        confirmText = "收藏",
        confirmEnabled = !(isCustom && customCategory.isBlank()),
        onConfirm = {
            onConfirm(
                titleInput.trim(),
                if (isCustom) customCategory.trim() else selectedCategory
            )
        },
        dismissText = "取消",
        onDismiss = onDismiss,
        content = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                IosTextField(
                    value = titleInput,
                    onValueChange = { titleInput = it },
                    placeholder = "标题（可选）"
                )
                Text(
                    text = "分类",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = iosSecondaryLabelColor()
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    categories.forEach { cat ->
                        IosChip(
                            text = cat,
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = cat }
                        )
                    }
                    IosChip(
                        text = "自定义",
                        selected = isCustom,
                        onClick = { selectedCategory = CUSTOM_CATEGORY }
                    )
                }
                AnimatedVisibility(
                    visible = isCustom,
                    enter = expandVertically(NativeSpringIntSizeSoft) + fadeIn(NativeSpringSoft),
                    exit = shrinkVertically(NativeSpringIntSizeSoft) + fadeOut(NativeSpringSoft)
                ) {
                    IosTextField(
                        value = customCategory,
                        onValueChange = { customCategory = it },
                        placeholder = "自定义分类"
                    )
                }
            }
        }
    )
}

/** 修改分类弹窗（iOS 风格） */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EditCategoryDialog(
    currentCategory: String,
    categories: List<String>,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedCategory by remember { mutableStateOf(currentCategory) }
    var customCategory by remember { mutableStateOf("") }

    IosAlertDialog(
        onDismissRequest = onDismiss,
        title = "修改分类",
        confirmText = "确定",
        onConfirm = { onConfirm(customCategory.ifBlank { selectedCategory }) },
        dismissText = "取消",
        onDismiss = onDismiss,
        content = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    categories.forEach { cat ->
                        IosChip(
                            text = cat,
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = cat }
                        )
                    }
                }
                IosTextField(
                    value = customCategory,
                    onValueChange = { customCategory = it },
                    placeholder = "自定义分类（可选）"
                )
            }
        }
    )
}

/** 长按操作菜单（iOS 风格） */
@Composable
private fun BookmarkMenuDialog(
    bookmark: BookmarkEntity,
    onResolve: () -> Unit,
    onCopy: () -> Unit,
    onEditCategory: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    IosAlertDialog(
        onDismissRequest = onDismiss,
        confirmText = "",
        onConfirm = {},
        title = bookmark.title.ifBlank { bookmark.link },
        dismissText = "取消",
        onDismiss = onDismiss,
        content = {
            Column(modifier = Modifier.fillMaxWidth()) {
                MenuActionRow(icon = Icons.Outlined.Link, tint = IosBlue, text = "解析", onClick = onResolve)
                MenuActionRow(icon = Icons.Outlined.ContentCopy, tint = IosBlue, text = "复制链接", onClick = onCopy)
                MenuActionRow(icon = Icons.Outlined.Edit, tint = IosBlue, text = "修改分类", onClick = onEditCategory)
                MenuActionRow(icon = Icons.Outlined.Delete, tint = IosRed, text = "删除", onClick = onDelete)
            }
        }
    )
}

/** 菜单操作行：图标 + 文字，按压浅灰高亮 */
@Composable
private fun MenuActionRow(
    icon: ImageVector,
    tint: Color,
    text: String,
    onClick: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val bg by androidx.compose.animation.animateColorAsState(
        targetValue = if (pressed) iosPressColor() else Color.Transparent,
        animationSpec = NativeSpringColorSoft,
        label = "iosMenuRowBg"
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 4.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(19.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = text,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = if (tint == IosRed) IosRed else iosLabelColor()
        )
    }
}

/** 平台枚举名 → 展示名 */
internal fun bookmarkPlatformLabel(platform: String): String = when (platform) {
    "QUARK" -> "夸克网盘"
    "UC" -> "UC网盘"
    "XUNLEI" -> "迅雷网盘"
    "BAIDU" -> "百度网盘"
    "C139" -> "139网盘"
    "PAN123" -> "123云盘"
    else -> "网盘"
}

private fun copyToClipboard(context: Context, text: String) {
    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    cm.setPrimaryClip(ClipData.newPlainText("yunx_bookmark", text))
}
