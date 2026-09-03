package com.yunx.cloud.data.download

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build
import androidx.core.content.ContextCompat
import com.yunx.cloud.MainActivity
import com.yunx.cloud.R
import com.yunx.cloud.data.prefs.SettingsRepository
import kotlin.math.absoluteValue

/**
 * 下载完成/失败提醒：独立通知（非前台服务通知），完成/失败开关独立，可选铃声。
 * - 总开关 / 完成开关 / 失败开关 / 铃声均来自设置页；
 * - 铃声："" = 跟随系统默认铃声；"none" = 静音（仅横幅不响铃）；其它为自定义铃声 Uri；
 * - Android 13+ 未授权通知权限时静默跳过（不崩溃）。
 */
object DownloadNotifier {

    private const val CHANNEL_ID = "yunx_download_done"
    private const val NOTIFICATION_ID_BASE = 2001

    fun notify(context: Context, success: Boolean, fileName: String, error: String) {
        val settings = SettingsRepository(context)
        // 总开关 + 完成/失败独立开关
        if (!settings.notifyEnabled) return
        if (success && !settings.notifyOnComplete) return
        if (!success && !settings.notifyOnFailed) return
        // Android 13+ 通知权限未授予：静默跳过
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) return

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        ensureChannel(nm, settings)

        val contentIntent = PendingIntent.getActivity(
            context, 1, Intent(context, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE
        )
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(context, CHANNEL_ID)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(context)
        }
        builder
            .setSmallIcon(R.drawable.icon)
            .setContentTitle(if (success) "下载完成" else "下载失败")
            .setContentText(if (success) fileName else "$fileName：$error")
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
        // 每个任务独立通知 id（避免互相覆盖）
        val id = NOTIFICATION_ID_BASE + fileName.hashCode().absoluteValue % 5000
        nm.notify(id, builder.build())
    }

    /** 按当前铃声设置重建通知渠道（铃声变化即时生效；IMPORTANCE_HIGH 保证横幅+响铃/静音按设置） */
    private fun ensureChannel(nm: NotificationManager, settings: SettingsRepository) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        // 删除旧渠道重建，确保铃声修改立即生效
        nm.deleteNotificationChannel(CHANNEL_ID)
        val channel = NotificationChannel(CHANNEL_ID, "下载完成提醒", NotificationManager.IMPORTANCE_HIGH)
        when (val uri = settings.notifySoundUri) {
            "none" -> channel.setSound(null, null)
            "" -> { /* 跟随系统默认铃声 */ }
            else -> runCatching {
                Uri.parse(uri)?.let { sound ->
                    channel.setSound(
                        sound,
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                            .build()
                    )
                }
            }
        }
        nm.createNotificationChannel(channel)
    }
}
