package com.yunx.cloud.ui.items

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yunx.cloud.ui.components.IosBlue
import com.yunx.cloud.ui.components.NativeSpring
import com.yunx.cloud.ui.components.NativeSpringColorSoft
import com.yunx.cloud.ui.components.iosCardColor
import com.yunx.cloud.ui.components.iosLabelColor
import com.yunx.cloud.ui.components.iosPressColor
import com.yunx.cloud.ui.components.iosSecondaryLabelColor

/** 多选底部批量操作项 */
internal data class MultiSelectAction(
    val label: String,
    val icon: ImageVector,
    val tint: Color,
    val onClick: () -> Unit
)

/**
 * 多选模式底部批量操作栏（云盘页共用）：iOS 风格悬浮白色圆角卡片，
 * 每项图标 + 文字纵向排布，按压弹性微缩 + 淡灰高亮。
 */
@Composable
internal fun MultiSelectBar(
    count: Int,
    actions: List<MultiSelectAction>
) {
    // 面板整体上抬（bottom=96dp），避开悬浮液态玻璃导航栏（高约 76dp），
    // 保证多选操作（下载/分享/移动/删除）完整可见、可点
    Box(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp)
                .shadow(elevation = 18.dp, shape = RoundedCornerShape(22.dp), clip = false)
                .clip(RoundedCornerShape(22.dp))
                .background(iosCardColor()),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            actions.forEach { action ->
                MultiSelectBarItem(action)
            }
        }
    }
}

@Composable
private fun MultiSelectBarItem(action: MultiSelectAction) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val bg by animateColorAsState(
        targetValue = if (pressed) iosPressColor() else Color.Transparent,
        animationSpec = NativeSpringColorSoft,
        label = "multiItemBg"
    )
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.9f else 1f,
        animationSpec = NativeSpring,
        label = "multiItemScale"
    )
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(16.dp))
            .background(bg)
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = action.onClick
            )
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Icon(
            imageVector = action.icon,
            contentDescription = action.label,
            tint = action.tint,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = action.label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = iosLabelColor()
        )
    }
}
