package com.yunx.cloud.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.yunx.cloud.data.network.HttpClients
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import okhttp3.Request
import java.io.IOException
import java.nio.ByteBuffer
import java.util.concurrent.ConcurrentHashMap

/**
 * 圆形网盘品牌头像：通过 OkHttp 拉取真实品牌图标（PNG/JPEG/WebP/ICO）。
 * - ICO（如 123 云盘官方 favicon）由 [decodeIco] 解析其中的位图；
 * - 内存缓存 + 并发去重，避免列表重组时重复请求；
 * - 加载中 / 失败时回退到 [fallback]（文字头像）。
 */
@Composable
fun NetworkAvatar(
    imageUrl: String?,
    size: Dp,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    fallback: @Composable () -> Unit = {}
) {
    var imageBitmap by remember(imageUrl) { mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null) }
    LaunchedEffect(imageUrl) {
        if (imageUrl == null) return@LaunchedEffect
        imageBitmap = runCatching {
            val bytes = withContext(Dispatchers.IO) {
                NetworkIconCache.fetch(imageUrl)
            }
            withContext(Dispatchers.Default) {
                decodeImage(bytes)?.asImageBitmap()
            }
        }.getOrNull()
    }
    val bitmap = imageBitmap
    if (bitmap != null) {
        Image(
            bitmap = bitmap,
            contentDescription = contentDescription,
            modifier = modifier.size(size).clip(CircleShape),
            contentScale = ContentScale.Crop
        )
    } else {
        fallback()
    }
}

/** 网络图标内存缓存 + 并发去重 */
object NetworkIconCache {
    private val cache = LruCache<String, ByteArray>(48)
    private val inFlight = ConcurrentHashMap<String, Deferred<ByteArray>>()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    suspend fun fetch(url: String): ByteArray {
        cache.get(url)?.let { return it }
        val running = inFlight[url]
        if (running != null) return running.await()
        val deferred = scope.async {
            val request = Request.Builder().url(url).build()
            HttpClients.apiClient().newCall(request).execute().use { resp ->
                if (!resp.isSuccessful) throw IOException("HTTP ${resp.code}")
                resp.body?.bytes() ?: throw IOException("empty body")
            }
        }
        inFlight[url] = deferred
        return try {
            val bytes = deferred.await()
            if (bytes.isNotEmpty() && bytes.size <= 5_000_000) cache.put(url, bytes)
            bytes
        } finally {
            inFlight.remove(url)
        }
    }
}

/** 优先按 ICO 解析，否则交给系统解码器（PNG/JPEG/WebP） */
private fun decodeImage(bytes: ByteArray): Bitmap? {
    if (bytes.size < 8) return null
    val isIco = bytes[0] == 0.toByte() && bytes[1] == 0.toByte() &&
        bytes[2] == 1.toByte() && bytes[3] == 0.toByte()
    if (isIco) return decodeIco(bytes)
    return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
}

/**
 * 解析 ICO 容器：从目录项中挑出最大的图，解出其中的 BMP 位图（常见 32bpp / 24bpp，
 * 兼容 0=BI_RGB 与 3=BI_BITFIELDS），并应用 AND 掩码得到透明通道。
 */
private fun decodeIco(bytes: ByteArray): Bitmap? {
    if (bytes.size < 6) return null
    val count = readShortLE(bytes, 4)
    if (count <= 0) return null
    var bestOff = -1
    var bestScore = -1
    for (i in 0 until count) {
        val off = 6 + i * 16
        if (off + 16 > bytes.size) break
        val w = if (bytes[off].toInt() == 0) 256 else bytes[off].toInt() and 0xFF
        val h = if (bytes[off + 1].toInt() == 0) 256 else bytes[off + 1].toInt() and 0xFF
        val score = w * h
        if (score > bestScore) {
            bestScore = score
            bestOff = off
        }
    }
    if (bestOff < 0) return null
    val imgOff = readIntLE(bytes, bestOff + 12)
    if (imgOff + 40 > bytes.size) return null
    return decodeIcoBmp(bytes, imgOff)
}

private fun decodeIcoBmp(bytes: ByteArray, start: Int): Bitmap? {
    val biSize = readIntLE(bytes, start)
    if (biSize < 40) return null
    val width = readIntLE(bytes, start + 4)
    val height = readIntLE(bytes, start + 8) // 2 倍：XOR 位图 + AND 掩码
    val bpp = readShortLE(bytes, start + 14)
    val compression = readIntLE(bytes, start + 16)
    if (width <= 0 || height <= 0) return null
    if (compression != 0 && compression != 3) return null
    val realH = height / 2
    if (realH <= 0) return null
    val bppBytes = bpp / 8
    if (bppBytes < 3) return null
    // BI_BITFIELDS 时位图头后紧跟 12 字节通道掩码
    var pxStart = start + biSize
    if (compression == 3) pxStart += 12
    val rowStride = (width * bppBytes + 3) / 4 * 4
    val maskRowBytes = (width + 31) / 32 * 4
    val andMaskStart = pxStart + rowStride * realH
    val bitmap = Bitmap.createBitmap(width, realH, Bitmap.Config.ARGB_8888)
    val buf = ByteBuffer.allocate(width * realH * 4)
    for (y in 0 until realH) {
        val srcRow = pxStart + (realH - 1 - y) * rowStride // BMP 自底向上
        val maskRow = andMaskStart + (realH - 1 - y) * maskRowBytes
        for (x in 0 until width) {
            val idx = srcRow + x * bppBytes
            if (idx + bppBytes > bytes.size) return null
            val b = bytes[idx].toInt() and 0xFF
            val g = bytes[idx + 1].toInt() and 0xFF
            val r = bytes[idx + 2].toInt() and 0xFF
            var a = 255
            if (bppBytes >= 4) a = bytes[idx + 3].toInt() and 0xFF
            // AND 掩码：1 表示透明（MSB 先行）
            val byteIdx = maskRow + x / 8
            if (byteIdx < bytes.size && ((bytes[byteIdx].toInt() shr (7 - x % 8)) and 1) == 1) a = 0
            val out = (y * width + x) * 4
            buf.put(out, r.toByte())
            buf.put(out + 1, g.toByte())
            buf.put(out + 2, b.toByte())
            buf.put(out + 3, a.toByte())
        }
    }
    buf.rewind()
    bitmap.copyPixelsFromBuffer(buf)
    return bitmap
}

private fun readShortLE(bytes: ByteArray, off: Int): Int {
    if (off + 2 > bytes.size) return 0
    return (bytes[off].toInt() and 0xFF) or ((bytes[off + 1].toInt() and 0xFF) shl 8)
}

private fun readIntLE(bytes: ByteArray, off: Int): Int {
    if (off + 4 > bytes.size) return 0
    return (bytes[off].toInt() and 0xFF) or
        ((bytes[off + 1].toInt() and 0xFF) shl 8) or
        ((bytes[off + 2].toInt() and 0xFF) shl 16) or
        ((bytes[off + 3].toInt() and 0xFF) shl 24)
}
