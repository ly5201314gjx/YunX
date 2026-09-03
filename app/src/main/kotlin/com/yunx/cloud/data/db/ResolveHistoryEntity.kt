package com.yunx.cloud.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 解析历史（Room 持久化）：记录解析过的分享链接、提取码与结果标题。
 * 支持重新解析、复制链接、清空。
 */
@Entity(tableName = "resolve_history")
data class ResolveHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    /** 原始分享链接 */
    val link: String,
    /** 提取码（可能为空） */
    val pwd: String = "",
    /** 解析成功后的分享标题（结果）；失败时为空 */
    val title: String = "",
    /** 平台标识（SharePlatform.name） */
    val platform: String = "",
    /** 是否解析成功 */
    val success: Boolean = true,
    /** 最近一次获取到的下载直链（可过期；非空时历史项可「复制直链」） */
    val directUrl: String = "",
    val createTime: Long = System.currentTimeMillis()
)
