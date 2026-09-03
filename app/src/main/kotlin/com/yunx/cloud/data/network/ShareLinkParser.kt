package com.yunx.cloud.data.network

/** 网盘平台 */
enum class SharePlatform { QUARK, UC, XUNLEI, BAIDU, C139, PAN123 }

/**
 * 解析结果：share_id + 提取码 + 平台。
 */
data class ParsedShare(
    val shareId: String,
    val pwd: String?,
    val platform: SharePlatform,
    /** 原始分享链接（批量解析/网页提取后重建会话用） */
    val url: String = ""
)

/**
 * 从分享链接或整段分享文案中提取 share_id 与提取码。
 * 支持：pan.quark.cn/s/xxx（夸克）、drive.uc.cn/s/xxx（UC）、pan.xunlei.com/s/xxx（迅雷）
 */
object ShareLinkParser {

    private val urlRegex = Regex("""https?://[^\s]+""")
    private val quarkShareIdRegex = Regex("""pan\.quark\.cn/s/([A-Za-z0-9]+)""", RegexOption.IGNORE_CASE)
    private val ucShareIdRegex = Regex("""drive\.uc\.cn/s/([A-Za-z0-9]+)""", RegexOption.IGNORE_CASE)
    private val xunleiShareIdRegex = Regex("""pan\.xunlei\.com/s/([A-Za-z0-9_-]+)""", RegexOption.IGNORE_CASE)
    private val baiduShareIdRegex = Regex("""pan\.baidu\.com/s/(1[A-Za-z0-9_-]+)""", RegexOption.IGNORE_CASE)
    private val c139ShareIdRegex = Regex("""yun\.139\.com/shareweb/.*?/w/i/([A-Za-z0-9_-]+)""", RegexOption.IGNORE_CASE)
    // 123 云盘分享链接（抓包 + alist 实践综合，文档 §4.1）：
    // - https://www.123pan.com/s/<ShareKey> / https://www.123865.com/s/<ShareKey>
    // - https://<UID>.share.123pan.cn/123pan/<ShareKey>
    // - https://www.123pan.cn/api/srr?sk=<ShareKey>&st=s
    // ShareKey 形态：含一个中划线、两端为字母数字，如 2785Vv-T4Ded
    private val pan123ShareIdRegex = Regex("""123(?:865|pan)\.(?:com|cn)/s/([A-Za-z0-9]+-[A-Za-z0-9]+)""", RegexOption.IGNORE_CASE)
    private val pan123ShareSubRegex = Regex("""share\.123pan\.cn/123pan/([A-Za-z0-9-]+)""", RegexOption.IGNORE_CASE)
    private val pan123SrrRegex = Regex("""api/srr\?sk=([A-Za-z0-9-]+)""", RegexOption.IGNORE_CASE)
    private val pwdInUrlRegex = Regex("""[?&]pwd=([A-Za-z0-9]+)""")
    private val pwdInTextRegex = Regex("""(?:提取码|访问码|密码)[：:]\s*([A-Za-z0-9]{4,8})""")

    fun parse(text: String): ParsedShare? {
        val url = urlRegex.find(text.trim())?.value
            ?.trimEnd('。', '，', ',', '；', ';', ')', ']', '}', '"', '\'')
            ?: return null
        return parseFromUrl(url, text)
    }

    /**
     * 从整段文本 / 网页 HTML 中提取全部网盘分享链接（去重）。
     * 供「网页链接提取器」使用：粘贴网页 URL 抓取页面后，调用本方法拿到所有分享。
     * 提取码优先取 URL 查询参数 pwd，其次取整段文本中的「提取码：xxxx」。
     */
    fun extractAll(text: String): List<ParsedShare> {
        val seen = LinkedHashMap<String, ParsedShare>()
        for (m in urlRegex.findAll(text)) {
            val url = m.value.trimEnd('。', '，', ',', '；', ';', ')', ']', '}', '"', '\'')
            parseFromUrl(url, text)?.let { p ->
                if (p.shareId.isNotBlank()) seen[p.shareId] = p
            }
        }
        return seen.values.toList()
    }

    private fun parseFromUrl(url: String, fullText: String): ParsedShare? {
        val cleanUrl = url.trimEnd('。', '，', ',', '；', ';', ')', ']', '}', '"', '\'')
        // 夸克链接
        quarkShareIdRegex.find(cleanUrl)?.groupValues?.getOrNull(1)?.let { sid ->
            val pwd = pwdInUrlRegex.find(cleanUrl)?.groupValues?.getOrNull(1)
                ?: pwdInTextRegex.find(fullText)?.groupValues?.getOrNull(1)
            return ParsedShare(shareId = sid, pwd = pwd, platform = SharePlatform.QUARK, url = cleanUrl)
        }
        // UC 链接
        ucShareIdRegex.find(cleanUrl)?.groupValues?.getOrNull(1)?.let { sid ->
            val pwd = pwdInUrlRegex.find(cleanUrl)?.groupValues?.getOrNull(1)
                ?: pwdInTextRegex.find(fullText)?.groupValues?.getOrNull(1)
            return ParsedShare(shareId = sid, pwd = pwd, platform = SharePlatform.UC, url = cleanUrl)
        }
        // 迅雷链接
        xunleiShareIdRegex.find(cleanUrl)?.groupValues?.getOrNull(1)?.let { sid ->
            val pwd = pwdInUrlRegex.find(cleanUrl)?.groupValues?.getOrNull(1)
                ?: pwdInTextRegex.find(fullText)?.groupValues?.getOrNull(1)
            return ParsedShare(shareId = sid, pwd = pwd, platform = SharePlatform.XUNLEI, url = cleanUrl)
        }
        // 百度链接：https://pan.baidu.com/s/1xxxxx?pwd=xxxx
        baiduShareIdRegex.find(cleanUrl)?.groupValues?.getOrNull(1)?.let { sid ->
            // 百度 surl 不包含开头的 "1"（verify/list 接口用 1 后面的部分）
            val surl = sid.removePrefix("1")
            val pwd = pwdInUrlRegex.find(cleanUrl)?.groupValues?.getOrNull(1)
                ?: pwdInTextRegex.find(fullText)?.groupValues?.getOrNull(1)
            return ParsedShare(shareId = surl, pwd = pwd, platform = SharePlatform.BAIDU, url = cleanUrl)
        }
        // 139（和彩云）链接：https://yun.139.com/shareweb/#/w/i/{linkID} 提取码 xxxx
        c139ShareIdRegex.find(cleanUrl)?.groupValues?.getOrNull(1)?.let { sid ->
            val pwd = pwdInUrlRegex.find(cleanUrl)?.groupValues?.getOrNull(1)
                ?: pwdInTextRegex.find(fullText)?.groupValues?.getOrNull(1)
            return ParsedShare(shareId = sid, pwd = pwd, platform = SharePlatform.C139, url = cleanUrl)
        }
        // 123 云盘链接（3 种形态，按优先级匹配）
        pan123ShareIdRegex.find(cleanUrl)?.groupValues?.getOrNull(1)?.let { sid ->
            val pwd = pwdInUrlRegex.find(cleanUrl)?.groupValues?.getOrNull(1)
                ?: pwdInTextRegex.find(fullText)?.groupValues?.getOrNull(1)
            return ParsedShare(shareId = sid, pwd = pwd, platform = SharePlatform.PAN123, url = cleanUrl)
        }
        pan123ShareSubRegex.find(cleanUrl)?.groupValues?.getOrNull(1)?.let { sid ->
            val pwd = pwdInUrlRegex.find(cleanUrl)?.groupValues?.getOrNull(1)
                ?: pwdInTextRegex.find(fullText)?.groupValues?.getOrNull(1)
            return ParsedShare(shareId = sid, pwd = pwd, platform = SharePlatform.PAN123, url = cleanUrl)
        }
        pan123SrrRegex.find(cleanUrl)?.groupValues?.getOrNull(1)?.let { sid ->
            val pwd = pwdInUrlRegex.find(cleanUrl)?.groupValues?.getOrNull(1)
                ?: pwdInTextRegex.find(fullText)?.groupValues?.getOrNull(1)
            return ParsedShare(shareId = sid, pwd = pwd, platform = SharePlatform.PAN123, url = cleanUrl)
        }
        return null
    }
}
