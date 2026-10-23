# ChaoxingSignFaker · rD227 修改版

基于 [aquamarine5/ChaoxingSignFaker](https://github.com/aquamarine5/ChaoxingSignFaker) 修改，原作者为 aquamarine5 / Brainspark。保留原作者版权说明，继续使用 [GNU AGPL v3](LICENSE)。本修改版源码位于 [rD227/ChaoxingSignFaker](https://github.com/rD227/ChaoxingSignFaker)。

## 修改内容

- 独立包名 `io.github.rd227.chaoxingsignfaker`，可与原版共存；Provider、权限、外部调用动作和分享深链接均使用独立标识。
- 移除打赏界面、签到后的打赏弹窗和打赏资源。
- 移除友盟 SDK、埋点、作者 Supabase 统计与排行榜、统计图表任务。
- 移除 Sentry 插件、SDK 初始化、上传配置与远程崩溃上报；保留本机错误信息和用户提示。
- 移除 Stackbricks、七牛配置、作者更新域名及原版包名校验。
- 移除作者域名的远程黑名单请求。账号分享使用原版 `http://cdn.aquamarine5.fun/` 格式，兼容原版扫码、粘贴和 `cxsignfaker://import` 导入；同时兼容此前本版的 `rd227cxsignfaker://import` 链接。扫码和粘贴只在本地解析，浏览器打开则会将分享参数发送给原作者网站，网页可用性取决于该网站。
- 无需搭建本项目的后端；账号与签到请求直接访问学习通，地图与定位继续使用百度 SDK。
- 通过 [Android_CN_OAID 4.2.17](https://github.com/gzu-liyujiang/Android_CN_OAID/tree/4.2.17) 的厂商接口读取 OAID，沿用原版 AES 设备码算法；不引入友盟或统计上报，不用 UUID、Android ID、Google AAID 冒充 OAID。该依赖使用 MulanPSL-2.0。只迁移能够确认来自此前 UUID 算法的本人设备码，其他已保存设备码保留。读取失败时不标记为统一设备码；厂商支持、隐私设置及官方客户端实现可能不同，不能保证所有手机与官方学习通一致。此前分享出去的旧设备码，需要机主更新后重新分享、导入。

## GitHub 更新

应用每次进程冷启动检查一次：

```text
https://api.github.com/repos/rD227/ChaoxingSignFaker/releases/latest
```

只使用正式 Release，草稿和预发布不作为正式更新。没有 Release（HTTP 404）时显示“暂无正式 Release”，不会回退到作者服务。登录页和设置页均可手动检查。

- “稍后再说”：本次不提示，下次冷启动仍检查。
- “忽略此次更新”：保存此 Release 标签，更高版本仍会提示；手动检查可重新显示已忽略版本。
- 点击“浏览器下载更新”，直接通过默认浏览器打开此 Release 的 APK 下载链接，由浏览器下载并交给系统安装器安装；应用不下载 APK，也不请求安装权限。
- 使用 Gradle 默认 APK 名：优先 `app-release.apk`，兼容 `app-release-unsigned.apk`。未签名包需要正式签名后才能安装，建议发布签名的 `app-release.apk`。
- 标签使用与发布 APK 对应的 `v1.19.4` 等数字版本。构建时自动增加 `versionCode` 和 `versionName`；保持自己的签名密钥一致，安装包的签名和版本由系统安装器校验。
- 检查更新使用独立 HTTP 客户端，不携带学习通会话。忽略更新的选择保存在本地 DataStore。

## 本地构建

保留原有构建版本：JDK 21、Android SDK 37、Gradle Wrapper。用 Android Studio 打开本目录，或配置 Android SDK 路径后执行：

```powershell
.\gradlew.bat :app:assembleDebug :app:assembleRelease :app:lintRelease
```

按本项目 `AGENTS.md` 的要求，不运行测试任务。

### 自动递增版本

版本保存于 `app/version.properties`，记录最近一次构建使用的版本。每次实际构建 Debug 或 Release APK/AAB 时，`VERSION_CODE` 加 1，`VERSION_NAME` 的最后一段加 1，例如 `1.19.3 → 1.19.4`。同一次 Gradle 调用同时构建 Debug 和 Release 只递增一次，两个产物及其 `BuildConfig` 使用相同版本。

Android Studio 同步、`help`、`clean` 及 `--dry-run` 不改写版本文件。版本在构建任务开始前写入，因此构建中途失败也会消耗该版本号。单独执行源码编译或 Lint 不递增，除非此次任务同时包含 APK/AAB 的构建任务。

构建完成后，从 APK 的 `output-metadata.json` 读取实际版本来创建 GitHub Release 标签。版本文件应与代码一起提交，其他工作副本和 CI 以仓库中记录的值为起点，不共享本机尚未提交的递增值。

原作者签名文件已移除。Debug 使用本机调试签名。正式 Release 可以在 Android Studio 中签名，或配置以下环境变量：

```text
RELEASE_KEYSTORE_PATH
RELEASE_KEYSTORE_PASSWORD
RELEASE_KEY_ALIAS
RELEASE_KEY_PASSWORD
```

未配置时，Release 输出默认的 `app/build/outputs/apk/release/app-release-unsigned.apk`；配置正式签名后输出 `app-release.apk`。私钥和密码不提交到仓库。

独立包名不能沿用原作者百度地图 Key 的包名/签名绑定。继续使用百度官方地图与定位 SDK，需要在[百度地图开放平台](https://lbsyun.baidu.com/docs/guide?title=android-guide%2Fprepare)为本修改版申请 Android SDK AK，并勾选需要的地图、定位和检索服务，无需自建后端。官方说明非商业用途提供免费服务与配额，商用及超额服务按平台规则处理。

申请时填写包名 `io.github.rd227.chaoxingsignfaker`。当前 `new.jks / key0` 正式签名的 SHA1 是 `57:49:D4:40:FE:63:48:D4:C5:B3:6D:22:37:C3:E3:84:F5:0A:50:94`，Debug 签名需另外登记。签名变更后应重新核对，不可混用原作者的包名或签名信息。

通过 `BAIDU_MAPS_API_KEY` 环境变量，或 Gradle 属性 `BAIDU_MAPS_API_KEY` 注入 AK；属性优先。Android Studio 本机构建可以把 `BAIDU_MAPS_API_KEY=你的AK` 写入用户目录下的 `.gradle/gradle.properties`，不提交到仓库。GitHub Actions 已预留同名 Secret。没有配置 AK 的安装包会明确提示地图服务未配置，不再初始化空 Key 的地图和定位；配置了 AK 仍需验证平台服务权限、包名及签名是否匹配。

支持 Android 8.0 及以上。独立包名不会读取原版的本机账号和照片。官方接口的实际可用性需要在自己的账号上验证。

## 署名与许可证

原作者：aquamarine5（@海蓝色的咕咕鸽）及 Brainspark。修改者：rD227。原作者版权说明和 AGPL-3.0 许可证继续保留。

[开发说明](CONTRIBUTING.md) · [许可证](LICENSE)
