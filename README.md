# 🐋 MCLauncher - Minecraft Java 版 Android 启动器

> PojavLauncher 风格的 Minecraft Java 版启动器，带 PCL 深色 UI、模组商店、触屏按键映射

## ✨ 功能特性

- ✅ **版本管理** — 从 Mojang manifest 拉取所有 Java 版（Release/Snapshot/旧版）
- ✅ **模组商店** — Modrinth API 驱动，6 大分类，一键下载
- ✅ **模组加载器** — Fabric / Forge / NeoForge / Quilt 支持
- ✅ **皮肤管理** — 多皮肤切换
- ✅ **触屏按键映射** — 虚拟摇杆 + 自定义按键布局编辑器
- ✅ **Java 设置** — 内存分配 / JVM 参数 / 系统 JDK
- ✅ **游戏启动核心** — Boardwalk (类 Wine 转译层) 让 Android 跑 LWJGL
- ✅ **PCL 风格 UI** — 深色侧边栏 + Material3 Compose

## 📁 项目结构

```
MinecraftLauncher/
├── build.gradle.kts                  # 根 Gradle（插件声明）
├── settings.gradle.kts               # 项目设置 + Maven 仓库
├── gradle.properties                 # Gradle JVM 参数
├── gradle/wrapper/                   # Gradle Wrapper
├── app/
│   ├── build.gradle.kts              # App 模块依赖
│   ├── proguard-rules.pro            # 混淆规则
│   └── src/main/
│       ├── AndroidManifest.xml       # 权限 + 组件声明
│       ├── res/                      # 资源
│       │   ├── values/               # 字符串/颜色/主题
│       │   └── drawable/             # 图标
│       └── java/com/mclauncher/
│           ├── MCApplication.kt       # Application 单例
│           ├── data/                 # 数据层
│           │   ├── ApiModels.kt      # API 数据类
│           │   ├── MojangApi.kt      # Mojang 版本 API
│           │   ├── ModrinthApi.kt    # Modrinth 模组 API
│           │   ├── DownloadManager.kt # 多任务下载器
│           │   └── LibraryDownloader.kt # 依赖库下载
│           ├── game/                 # 游戏核心
│           │   ├── GameSession.kt    # 启动参数构建
│           │   ├── BoardwalkRunner.kt # Boardwalk 进程启动
│           │   ├── GameActivity.kt   # 游戏界面
│           │   └── BoardwalkExtensions.kt # 按键注入扩展
│           ├── keyboard/             # 按键映射
│           │   ├── TouchOverlay.kt   # 触屏悬浮按键
│           │   ├── KeyboardMapperActivity.kt # 布局编辑器
│           │   └── FloatingKeyboardService.kt # 无障碍服务手势注入
│           ├── mods/                 # 模组
│           │   └── ModStoreScreen.kt # 模组商店 UI
│           └── ui/                   # Compose UI
│               ├── theme/Theme.kt    # MC 主题色
│               ├── MainActivity.kt   # 主入口 + 侧边栏导航
│               ├── HomeScreen.kt    # 首页（启动大按钮）
│               ├── VersionManagerScreen.kt # 版本管理页
│               ├── SkinManagerScreen.kt    # 皮肤管理页
│               ├── KeyboardScreen.kt       # 按键布局页
│               ├── SettingsScreen.kt       # 设置页
└── .github/workflows/
    └── build.yml                     # GitHub Actions 云打包
```

## ☁️ 云打包（GitHub Actions）

### 方式一：Push 到 GitHub 自动构建 ⭐ 推荐

1. **创建 GitHub 仓库**（免费），然后：

```bash
# 用手机 Termux 或 PC Git 客户端
cd MinecraftLauncher
git init
git add .
git commit -m "🎉 Initial commit: MCLauncher"
git branch -M main
git remote add origin https://github.com/你的用户名/MCLauncher.git
git push -u origin main
```

2. **GitHub Actions 自动运行** —— 构建完成后：
   - 👉 进入仓库 → **Actions** 标签页
   - 👉 点进最新的绿色 Run
   - 👉 底部 **Artifacts** 区下载 `MCLauncher-APK-N` 压缩包
   - 👉 解压得到 `.apk`

3. **Release 版本** — Push 到 main 分支后，自动生成 Release 草稿

### 方式二：手动触发

- GitHub 仓库 → Actions → Build MCLauncher APK → Run workflow
- 可选 debug / release 构建类型

### Release 签名（可选）

要签名 release APK，在 GitHub 仓库 **Settings → Secrets and variables → Actions** 添加：

| Secret | 说明 |
|--------|------|
| `KEYSTORE_BASE64` | `base64 -i release.keystore` 的输出 |
| `KEYSTORE_PASSWORD` | keystore 密码 |
| `KEY_ALIAS` | key 别名 |
| `KEY_PASSWORD` | key 密码 |

## 🖥️ 本地构建（Android Studio）

1. 安装 [Android Studio](https://developer.android.com/studio) Hedgehog+
2. 打开项目目录 `MinecraftLauncher`
3. 等 Gradle Sync 完成
4. 连接 Android 设备（USB 调试）
5. Run → 运行到设备

## 📜 权限说明

首次启动会请求：
- 🌐 **INTERNET** — 下载 MC 版本和模组
- 💾 **MANAGE_EXTERNAL_STORAGE** — 存放 `.minecraft` 目录
- 🪟 **SYSTEM_ALERT_WINDOW** — 游戏内悬浮按键
- ♿ **Accessibility Service** — 手势注入（模拟键鼠）

## ⚠️ 免责声明

本项目仅用于学习研究目的。Minecraft 是 Mojang AB 的商标。Boardwalk 转译层来自 [PojavLauncher](https://github.com/PojavLauncherTeam) 团队。

## 🎨 技术栈

- **Kotlin 1.9** + **Jetpack Compose Material3**
- **Gradle KTS** + **AGP 8.5**
- **OkHttp** + **Kotlinx-Serialization**
- **Boardwalk (libboardwalk.so)** — LWJGL/OpenGL ES 转译
- **Modrinth API v2** + **Mojang Piston API**
- **GitHub Actions** — CI 云打包
