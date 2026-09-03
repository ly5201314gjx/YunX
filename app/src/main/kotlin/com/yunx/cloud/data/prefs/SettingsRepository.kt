package com.yunx.cloud.data.prefs

import android.content.Context
import com.yunx.cloud.data.backup.AuthCrypto
import com.yunx.cloud.data.download.DownloadPlatform
import org.json.JSONArray
import org.json.JSONObject

/**
 * 应用设置（SharedPreferences 持久化）。
 */
class SettingsRepository(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("yunx_settings", Context.MODE_PRIVATE)

    /** 下载线程数（通用/手动添加，分片并发数），默认 32，上限 512 */
    var downloadThreads: Int
        get() = downloadThreadsFor(DownloadPlatform.GENERIC)
        set(value) = setDownloadThreads(DownloadPlatform.GENERIC, value)

    /** 获取指定平台的下载线程数；迅雷固定 8，其余默认 32、上限 512 */
    fun downloadThreadsFor(platform: String): Int {
        if (platform == DownloadPlatform.XUNLEI) return XUNLEI_DOWNLOAD_THREADS
        return prefs.getInt(prefsKey(platform), DEFAULT_DOWNLOAD_THREADS)
            .coerceIn(1, MAX_DOWNLOAD_THREADS)
    }

    /** 设置指定平台的下载线程数；迅雷不可修改 */
    fun setDownloadThreads(platform: String, value: Int) {
        if (platform == DownloadPlatform.XUNLEI) return
        prefs.edit().putInt(prefsKey(platform), value.coerceIn(1, MAX_DOWNLOAD_THREADS)).apply()
    }

    private fun prefsKey(platform: String): String =
        if (platform.isBlank() || platform == DownloadPlatform.GENERIC) "download_threads"
        else "download_threads_$platform"

    /** 自定义下载保存目录（SAF tree Uri，content://...）；null/空 = 系统默认 Download 目录 */
    var downloadDirUri: String?
        get() = prefs.getString("download_dir_uri", null)
        set(value) {
            prefs.edit().putString("download_dir_uri", value).apply()
        }

    /** 最大同时下载任务数（默认 1：前台任务吃满带宽，其余排队；参考 IDM 默认单任务满速） */
    var maxConcurrentDownloads: Int
        get() = prefs.getInt("max_concurrent_downloads", DEFAULT_MAX_CONCURRENT_DOWNLOADS)
        set(value) {
            prefs.edit().putInt("max_concurrent_downloads", value.coerceIn(1, 10)).apply()
        }

    /** 下载速度限制（字节/秒；0 = 不限速） */
    var downloadSpeedLimit: Long
        get() = prefs.getLong("download_speed_limit", 0L)
        set(value) {
            prefs.edit().putLong("download_speed_limit", value.coerceAtLeast(0L)).apply()
        }

    /** 下载失败后自动重试次数（默认 3，范围 0-10） */
    var downloadRetryCount: Int
        get() = prefs.getInt("download_retry_count", DEFAULT_DOWNLOAD_RETRY_COUNT)
        set(value) {
            prefs.edit().putInt("download_retry_count", value.coerceIn(0, 10)).apply()
        }

    /** 锁屏后保持下载：开启后下载时获取 WakeLock，并可引导加入「忽略电池优化」白名单（默认开启） */
    var keepDownloadWhenLocked: Boolean
        get() = prefs.getBoolean("keep_download_when_locked", true)
        set(value) {
            prefs.edit().putBoolean("keep_download_when_locked", value).apply()
        }

    /** 通知栏进度样式：true=完整通知（进度条+下载速度）；false=仅显示通知（隐藏速度） */
    var notificationShowSpeed: Boolean
        get() = prefs.getBoolean("notification_show_speed", true)
        set(value) {
            prefs.edit().putBoolean("notification_show_speed", value).apply()
        }

    /** 桌面图标样式：0=经典图标(icon)，1=新图标(icon2)；切换经 activity-alias 动态生效 */
    var appIconVariant: Int
        get() = prefs.getInt("app_icon_variant", 0)
        set(value) {
            prefs.edit().putInt("app_icon_variant", value.coerceIn(0, 1)).apply()
        }

    /** 忽略 SSL 证书校验（抓包调试用，隐藏菜单开启；默认关闭） */
    var ignoreSslCert: Boolean
        get() = prefs.getBoolean("ignore_ssl_cert", false)
        set(value) {
            prefs.edit().putBoolean("ignore_ssl_cert", value).apply()
        }

    /** 百度网盘大文件限速提示：是否已选择「不再显示」 */
    var baiduLimitHintDismissed: Boolean
        get() = prefs.getBoolean("baidu_limit_hint_dismissed", false)
        set(value) {
            prefs.edit().putBoolean("baidu_limit_hint_dismissed", value).apply()
        }

    /** 深色模式：0=跟随系统，1=浅色，2=深色 */
    var darkMode: Int
        get() = prefs.getInt("dark_mode", 0)
        set(value) {
            prefs.edit().putInt("dark_mode", value.coerceIn(0, 2)).apply()
        }

    /** 主题色模式：0=动态色彩（Android12+ 壁纸取色，低版本回退默认蓝），1=默认蓝色，2=自定义种子色 */
    var themeColorMode: Int
        get() = prefs.getInt("theme_color_mode", 0)
        set(value) {
            prefs.edit().putInt("theme_color_mode", value.coerceIn(0, 2)).apply()
        }

    /** 自定义主题种子色（ARGB 值） */
    var themeSeedColor: Long
        get() = prefs.getLong("theme_seed_color", DEFAULT_SEED_COLOR)
        set(value) {
            prefs.edit().putLong("theme_seed_color", value).apply()
        }

    // ---------- 下载完成提醒（设置页独立开关，可选铃声） ----------

    /** 下载完成提醒总开关 */
    var notifyEnabled: Boolean
        get() = prefs.getBoolean("notify_enabled", true)
        set(value) {
            prefs.edit().putBoolean("notify_enabled", value).apply()
        }

    /** 下载成功完成时提醒 */
    var notifyOnComplete: Boolean
        get() = prefs.getBoolean("notify_on_complete", true)
        set(value) {
            prefs.edit().putBoolean("notify_on_complete", value).apply()
        }

    /** 下载失败时提醒 */
    var notifyOnFailed: Boolean
        get() = prefs.getBoolean("notify_on_failed", true)
        set(value) {
            prefs.edit().putBoolean("notify_on_failed", value).apply()
        }

    /** 完成/失败提醒铃声："" = 跟随系统默认铃声；"none" = 静音（仅横幅不响铃） */
    var notifySoundUri: String
        get() = prefs.getString("notify_sound_uri", "") ?: ""
        set(value) {
            prefs.edit().putString("notify_sound_uri", value).apply()
        }

    // ---------- 设置导出/导入（备份整机设置到本地文件） ----------

    /** 导出全部设置（SharedPreferences 全量）为 JSON；带类型标记保证导入类型一致性 */
    fun exportSettingsJson(): String {
        val root = JSONObject()
        root.put("app", BACKUP_TAG)
        root.put("version", BACKUP_VERSION)
        root.put("exportedAt", System.currentTimeMillis())
        val data = JSONObject()
        for ((key, v) in prefs.all) {
            val item = JSONObject()
            when (v) {
                is Boolean -> { item.put("t", "bool"); item.put("v", v) }
                is Int -> { item.put("t", "int"); item.put("v", v) }
                is Long -> { item.put("t", "long"); item.put("v", v) }
                is Float -> { item.put("t", "float"); item.put("v", v.toDouble()) }
                is String -> { item.put("t", "string"); item.put("v", v) }
                is Set<*> -> {
                    item.put("t", "stringset")
                    item.put("v", JSONArray(v.map { it.toString() }))
                }
                else -> continue
            }
            data.put(key, item)
        }
        root.put("settings", data)
        return root.toString(2)
    }

    /** 导出设置并加密（至少 8 位口令），返回密文内容 */
    fun exportSettingsEncrypted(password: String): String {
        require(password.length >= 8) { "备份口令至少 8 位" }
        return AuthCrypto.encrypt(exportSettingsJson(), password)
    }

    /** 从 JSON 恢复全部设置；文件不合法抛异常 */
    fun importSettingsJson(json: String) {
        val root = JSONObject(json)
        if (root.optString("app") != BACKUP_TAG) {
            throw IllegalArgumentException("不是有效的析盘设置备份文件")
        }
        val data = root.optJSONObject("settings") ?: return
        val editor = prefs.edit()
        val keys = data.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            val item = data.optJSONObject(key) ?: continue
            when (item.optString("t")) {
                "bool" -> editor.putBoolean(key, item.optBoolean("v"))
                "int" -> editor.putInt(key, item.optInt("v"))
                "long" -> editor.putLong(key, item.optLong("v"))
                "float" -> editor.putFloat(key, item.optDouble("v").toFloat())
                "string" -> editor.putString(key, item.optString("v"))
                "stringset" -> {
                    val arr = item.optJSONArray("v") ?: continue
                    val set = HashSet<String>()
                    for (i in 0 until arr.length()) set.add(arr.optString(i))
                    editor.putStringSet(key, set)
                }
            }
        }
        editor.apply()
    }

    /** 从加密内容恢复设置（密码错误抛异常） */
    fun importSettingsEncrypted(content: String, password: String) {
        importSettingsJson(AuthCrypto.decrypt(content, password))
    }

    companion object {
        private const val BACKUP_TAG = "yunx_settings_backup"
        private const val BACKUP_VERSION = 1

        const val DEFAULT_DOWNLOAD_THREADS = 32
        const val MAX_DOWNLOAD_THREADS = 512
        const val XUNLEI_DOWNLOAD_THREADS = 8
        const val DEFAULT_MAX_CONCURRENT_DOWNLOADS = 1
        const val DEFAULT_DOWNLOAD_RETRY_COUNT = 3

        /** 默认主题种子色：Material Blue（与内置默认方案一致） */
        const val DEFAULT_SEED_COLOR = 0xFF415F91L
    }
}
