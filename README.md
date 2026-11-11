# BiliSave

莫奈风（Material You）B 站视频下载器 · Android 原生 App。

粘贴 BV 号 / av 号 / b23.tv 短链 → 解析 → 选清晰度 → 下载 DASH 音视频流 → 本地合封成一个可播放的 mp4。
不需要电脑，不需要 Gradle，纯用 Android 平台 API 在本机（甚至手机上）构建。

## 功能

- **地址解析**：BV / av / b23.tv 短链，自动跟随 302 还原真实地址，失败给出「未找到 BV 号」提示而不是崩溃
- **完整 WBI 签名**：`nav` 取 `wbi_img` 双密钥 → MIXIN_TAB 重排取 32 位 mixin_key → 参数排序过滤 → `w_md5` 得 `w_rid`，密钥缓存 1 小时
- **清晰的真实可用性**：解析时用 `x/player/wbi/playurl` 探一次，清晰度列表直接来自接口 `support_formats`，绝不写死 4K；服务端只给 480P 就只显示 480P
- **DASH 合封**：MediaExtractor + MediaMuxer 把视频 m4s 与音频 m4s 交错写入单个 mp4，合封后校验时长/音轨/视频轨，成功后删除中间文件；失败自动回退到 `fnval=1` 的整文件 mp4
- **扫码登录**：`qrcode/generate` → 本地离线生成二维码 → `qrcode/poll` 轮询，成功后从响应的 Set-Cookie 里提取 SESSDATA / bili_jct / DedeUserID，写入本机登录态，并拉取昵称头像
- **多账号**：账号列表存本地，一键切换；也可以手写粘贴 Cookie
- **后台下载**：前台服务（dataSync）+ 通知实时进度 + Range 断点续传；进程被杀后重开显示「已中断」，点重试续传
- **保存位置**：可 SAF 选择任意文件夹，默认写入 `Movies/BiliSave`（音频 `Music/BiliSave`）
- **外观**：Android 12+ 从壁纸取色（system_accent1/2/3），低版本以 B 站粉为种子生成调色板；深浅色跟随系统或手动指定；水彩雾面背景自绘

## 构建

无 Gradle、无 AndroidX、无第三方运行时依赖，只需要：

| 组件 | 说明 |
| --- | --- |
| JDK 17 | `javac` / `java` / `jar` / `keytool` |
| Android Platform | `android.jar`（本项目用 android-36 编译，minSdk 26 / targetSdk 35） |
| aapt2、zipalign | 资源编译与对齐 |
| r8 (d8) | dex |
| apksig | V1 + V2 签名 |

```sh
# 生成签名库（首次）
keytool -genkeypair -v -keystore keystore.jks -alias bilisave \
  -keyalg RSA -keysize 2048 -validity 10000 \
  -storepass "$KEYSTORE_PASS" -keypass "$KEYSTORE_PASS" \
  -dname "CN=BiliSave"

# 构建
sh build-ondevice.sh
# 产物：build/BiliSave-release.apk
```

`build-ondevice.sh` 顶部用环境变量指定工具链路径（`TC` / `KEYSTORE_PASS`），可以在 Termux、Linux 或任意装了 JDK 17 的机器上跑。

## 目录结构

```
AndroidManifest.xml
res/                      主题、图标、字符串
src/com/bilisave/app/
  MainActivity.java       三个页签的壳 + 主题切换 + 崩溃日志钩子
  AppState.java           全局状态与监听（任务、当前视频、提示）
  BiliApp.java            本地崩溃日志（不联网、不含 Cookie）
  data/Api.java           请求头、解析、WBI、playurl、流式下载、扫码登录
  data/Models.java        Video / Part / Format / Dash
  data/Task.java          任务模型与 JSON 持久化
  data/Prefs.java         Cookie、账号、主题、UA、目录、任务
  download/DownloadService.java  前台服务队列、进度、合封、导出
  download/Remuxer.java   DASH 两流合封（含可播放性校验）
  download/Export.java    SAF / MediaStore 落盘
  pages/                  HomePage、TasksPage、SettingsPage、QrLogin
  ui/                     Monet 调色板、雾面背景、chip、进度条、图标、弹窗
src/io/nayuki/qrcodegen/  二维码生成（MIT，见下）
tools/                    构建用的签名/验签小工具
release/BiliSave-1.1.apk  已签名安装包
```

## 隐私与合规

- 不收集账号密码；登录态来自你自己扫码或粘贴，**只存本机 SharedPreferences，不上传，不写日志，不做崩溃上报**
- 崩溃日志只写在本机 `/sdcard/Download/BiliSave-last-crash.txt`，内容仅堆栈
- 本工具仅供个人离线观看：请尊重 UP 主版权，不要二次分发，不要批量抓取，不要用于绕过付费墙

## 第三方

- [nayuki/QR-Code-generator](https://github.com/nayuki/QR-Code-generator)（MIT）用于离线生成登录二维码
- B 站接口均为官方 web 接口，无逆向私有协议

## 说明

仓库**不包含签名用 keystore**（私钥不入库）。要出正式包自己生成一个即可。

## License

MIT（见 LICENSE）。

