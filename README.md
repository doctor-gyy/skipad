# 跳过开屏广告（自用版）

一句话：装好并开启一次权限后，打开任意 App，屏幕上出现“跳过 / 跳过广告 / Skip”时，它替你点一下。

---

## 一、先把三件事说清楚

1. **这不是“破解”广告。** 广告仍然会加载，只是有人（这个程序）替你按了“跳过”。所以该消耗的流量、该等的加载时间还是会有，只是你不用动手、也不用等那 5 秒。
2. **不 root、不刷机、不装 Xposed、不改系统分区。** 只用了安卓官方公开的“无障碍服务（AccessibilityService）”接口，这是系统给读屏软件用的正规能力，随时能在设置里关掉，卸载后不留任何东西。
3. **唯一的“门槛”是手动开一次无障碍权限。** 这是安卓的安全设计，任何同类工具（包括李跳跳、轻启动）都绕不开这一步。除此之外没有别的门槛。

**它做不到的情况（正常现象，不是你装错了）：**

- 广告的“跳过”是纯图片、网页（WebView）或特殊绘制控件，系统读不到文字 → 点不到；
- App 把“跳过”做成整屏点击、摇一摇、上滑；
- 广告根本没有“跳过”按钮。

**安全说明：** 这个 App 的清单文件里**没有申请联网权限**，从系统层面就没有联网能力；代码一共三百行左右，全部开源在文件夹里，可以自己看。

---

## 二、把它变成能装的 APK（两种方式，选一种）

### 方案 A：GitHub 云端编译（推荐，电脑上不用装任何软件）

1. 打开 https://github.com 注册并登录（免费）。
2. 右上角 **+** → **New repository**，名字填 `SkipAd`，选 Private（私有）也可以，点 **Create repository**。
3. 进入仓库，点 **Add file** → **Upload files**。
4. 把 `SkipAd` 文件夹**里面的全部内容**拖进网页（包括 `.github` 这个文件夹）。
   - 注意：拖的是“里面的内容”，要让 `build.gradle`、`settings.gradle`、`app` 直接出现在仓库根目录。
   - 如果网页上传不认 `.github` 文件夹，就在仓库里点 **Add file → Create new file**，文件名输入 `.github/workflows/build-apk.yml`，把该文件内容粘进去。
5. 点 **Commit changes**。
6. 点仓库顶部的 **Actions** 标签。如果是第一次，会出现一个绿色按钮 **I understand my workflows, go ahead and enable them**，点它。
7. 等 2～4 分钟，列表里出现绿色对勾 ✔。点进那次运行，页面最下面 **Artifacts** → 下载 **SkipAd-apk.zip**。
8. 解压得到 `app-debug.apk`，用微信“文件传输助手”、QQ 或数据线发到手机。

> 私有仓库每月有 2000 分钟免费额度，一次构建约 2 分钟，够用很久。
> 如果 GitHub 打不开，只能走方案 B，或者找一台装了 Android Studio 的电脑。

### 方案 B：Android Studio 本地编译（有电脑、网络较好时）

1. 到 https://developer.android.com/studio 下载安装 Android Studio（安装包约 1GB）。
2. 第一次启动按向导走，让它下载 Android SDK（约 2～3GB，需要时间）。
3. 欢迎页选 **Open**，选中本文件夹 `SkipAd`，等待右下角 Gradle Sync 完成（第一次要下载依赖）。
   - 如果提示缺少 `gradle-wrapper.jar`：点提示里的修复链接 / OK，让 Studio 自动补全（需要联网）。
4. 菜单 **Build → Build App Bundle(s) or APK → Build APK(s)**。
5. 右下角弹窗点 **locate**，得到 `app/build/outputs/apk/debug/app-debug.apk`，传到手机。

---

## 三、手机上的设置（3 步，只做一次）

1. **安装 APK。** 点开下载的 `app-debug.apk`，系统会提示“未知来源/禁止安装”，按提示允许本次安装；Play 保护提示风险时选“仍然安装”（这是自己签名的调试包，属正常）。
2. **安卓 13 及以上先解除限制。** 打开刚装的 App → 点“第 2 步：应用信息” → 右上角 **⋮** → **允许受限设置**（部分手机叫“允许受限制的设置”）。不做这步，无障碍开关会是灰的、点不动。
3. **开无障碍。** 回到 App → 点“第 1 步” → 在列表里找 **已下载的服务 / 已安装的服务** → **跳过开屏广告** → 打开 → 弹窗点“允许”。
4. 回到 App，看到 **✅ 服务已开启** 就成功了。之后不用一直开着这个界面，退回桌面即可。

可选但建议：设置 → 应用 → 跳过开屏广告 → **电池** → 选“无限制”，并把“自启动”打开（国产系统清理后台时容易把无障碍服务一起关掉）。

---

## 四、验证与排错

- 打开一个带开屏广告的 App，出现“跳过 3”时应该自己点掉，直接进首页。
- **没反应**：先回本 App 看是不是变回 ⚠️（被系统杀了）；再看那个 App 的“跳过”是不是图片（图片读不到文字，无解）。
- **误点**：把 `SkipService.java` 里 `KEYWORDS` 中太宽泛的词删掉；或把长度限制 `16` 改小。
- **耗电**：只有屏幕内容变化时才扫描，且每次最多看 500 个控件，正常可以忽略。
- **不想要了**：系统设置 → 无障碍 → 关闭；再卸载 App，系统里不会残留任何东西。

---

## 五、想自己改

| 想改什么 | 改哪里 |
| --- | --- |
| 识别哪些文字（比如“跳过 3”“关闭广告”） | `app/src/main/java/com/example/skipad/SkipService.java` 顶部的 `KEYWORDS` |
| 扫描时机、重试次数 | 同一个文件 `onAccessibilityEvent` 里的 `postDelayed` |
| 界面文字、按钮 | `app/src/main/java/com/example/skipad/MainActivity.java` |

---

## 六、合规提醒

仅供自己（和家人）的手机使用，**不要上架应用商店或公开分发**：Google Play 和国内应用商店都禁止用无障碍服务去广告，公开传播容易被下架，也可能涉及不正当竞争。

另外，无障碍权限本身能读屏幕内容，所以这类工具**只装自己编译的版本**最稳。

---

## 文件结构

```
SkipAd/
├── settings.gradle
├── build.gradle
├── gradle.properties
├── gradle/wrapper/gradle-wrapper.properties
├── .github/workflows/build-apk.yml     ← 云端编译脚本
└── app/
    ├── build.gradle
    └── src/main/
        ├── AndroidManifest.xml         ← 没有 INTERNET 权限
        ├── java/com/example/skipad/
        │   ├── MainActivity.java       ← 界面：看状态、跳设置
        │   └── SkipService.java        ← 核心：找“跳过”并点击
        └── res/
            ├── values/strings.xml
            └── xml/accessibility_service_config.xml
```
