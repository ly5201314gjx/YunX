# 析盘

> 网盘分享链接解析 & 高速下载 · 完全免费开源
>
> 「粘贴链接 → 浏览分享 → 高速下载」，这才是网盘链接该有的打开方式。

## 它是什么

一个干净利落的 Android 工具：拿到网盘分享链接，直接解析出分享内容，然后**满速**把文件拖回本地。

没有会员墙，没有限速条，没有花里胡哨的广告。你只负责粘贴链接，剩下的交给析盘。

## 为什么叫「析盘」

拆解（析）+ 网盘（盘）。**把链接拆开，把网盘搬空。** ATM 机式下载，童叟无欺。

## 支持的网盘

- 夸克网盘
- UC 网盘
- 迅雷网盘
- 百度网盘（**不建议用，容易触发账号风控，慎入**）
- 123 云盘
- 139 网盘（和彩云）
- ~~其他网盘？懒了，有需要请开 issue~~

## 功能特性

- **一键解析**：识别夸克 / UC / 迅雷 / 百度 / 139 / 123 分享链接，自动带出提取码
- **Range 分片 + 断点续传**：分片并发下载，前中后段速度曲线平坦，不搞「开头冲刺、后段限速」那一套
- **暂停 / 继续 / 删除 / 打开**：下载管理一条龙，暂停再开不重下
- **临时转存自动清理**：百度 / 迅雷取链后即清，夸克保留到下载完成，不污染你的网盘
- **账号安全**：夸克 / UC / 百度 / 139 走 WebView Cookie 登录，123 走账号密码 JWT；可导出加密备份
- **剪贴板识别**：复制链接回到应用，自动弹提示一键粘贴
- **液态玻璃 UI**：iOS 质感的毛玻璃导航 + 页面过渡，SwiftUI 党狂喜

## 截图

| 解析直链 | 分享解析 | 下载管理 |
|:---:|:---:|:---:|
| ![解析输入](images/Link.jpg) | ![文件列表](images/Parsing.jpg) | ![下载管理](images/Download.jpg) |

| 网盘登录 | 设置 | 关于 |
|:---:|:---:|:---:|
| ![网盘登录](images/Login.jpg) | ![设置](images/Setting.jpg) | ![关于](images/about.jpg) |

## 怎么用

1. 「网盘」页登录你需要的网盘账号
2. 「解析」页粘贴分享链接（可带提取码）
3. 浏览分享内容，点文件拿直链
4. 「下载」页看进度，暂停 / 继续 / 删除 / 打开随便来

## 技术栈

- Kotlin
- Jetpack Compose + Material 3
- Room（凭证与下载任务持久化）
- OkHttp（网络请求 + 分片下载）
- KSP

## 构建

要求：minSdk 21，targetSdk 34。

```bash
git clone https://github.com/ly5201314gjx/YunX.git
```

用 Android Studio 打开直接跑。项目在 AndroidIDE 上开发调试，理论上兼容其它 Android 构建环境。

## 免责声明

项目仅供个人学习与技术交流，**请勿用于商业用途**。下载内容版权归原作者所有，请在下载后 24 小时内删除。使用本项目产生的一切后果由使用者自行承担。

## 开源协议

基于 [GNU AGPL-3.0](https://www.gnu.org/licenses/agpl-3.0.html) 协议开源，详见 [LICENSE](./LICENSE)。

## 协议逆向说明

部分网盘平台的解析基于抓包分析与开源项目（如 alist）的协议研究整理。接口随官方调整可能失效，以实际运行为准。

## 耻辱榜

**倒卖的你是活不起了是吗😂**

- 析盘完全免费开源。如果你下载到一个要钱的版本，说明你被骗了，请立刻去退款。
- 倒卖狗 🐶：qq1360735243

## Star History

[![Star History Chart](https://api.star-history.com/chart?repos=ly5201314gjx/YunX&type=date&legend=top-left&sealed_token=hccCg_4ek01_Sz38X79eMbjM11mNpOZti6_hLoztWW4Zdtx-8FScydd7YTdiCBUWvgpsuGDO70RrUKP-bOfbI3Gw8BnME1zIl5EHA9JWsv--_DDwWPjvKbZiAGNDslG3ZTDZ-Ssiapu7j08W4fPT6emGWaIIuawHoIw3Nic_xQu7hUSVO6_YeJRGRoEy)](https://www.star-history.com/?repos=ly5201314gjx%2FYunX&type=date&legend=top-left)