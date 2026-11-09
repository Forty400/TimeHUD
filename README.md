# TimeHUD（时间浮窗）

Android 8.0+ 悬浮时钟，便于整屏录屏后定位 Bug 时间点。  
包名：`com.internal.timeoverlay` · 版本：`0.1.4`

## 功能范围（v0.1.4）

- 跨普通 App 显示本地时间（到秒 / 到毫秒，可选日期）
- 设置页：预览与总开关、格式、尺寸、外观、锁定穿透
- 前台服务 + 无桌面角标的低重要性通知
- 无第三方运行时依赖（原生 Java 17 + Android View）
- 不做：iOS、登录、网络校时、录屏、开机自启

## 构建说明

### 环境

- JDK 17+
- Android SDK（compileSdk 35），命令行或 Android Studio
- 将 SDK 路径写入本目录 `local.properties`：

```properties
sdk.dir=C\:\\Users\\YOU\\AppData\\Local\\Android\\Sdk
```

### 命令

在仓库根目录（本目录）执行：

```bash
# Windows
gradlew.bat test
gradlew.bat lint
gradlew.bat assembleRelease
```

产物：`app/build/outputs/apk/release/app-release.apk`（已签名，可直接安装）。

开源仓库内可直接下载预构建包：[`release/TimeHUD-0.1.4.apk`](release/TimeHUD-0.1.4.apk)。

若尚未配置 `keystore/timehud-release.jks`，Release 会回退使用 Debug 签名，仍可装内部包。

### Release 签名（可选）

```bash
mkdir keystore
keytool -genkeypair -v -keystore keystore/timehud-release.jks -alias timehud -keyalg RSA -keysize 2048 -validity 3650
```

在 `gradle.properties`（勿提交密钥）或环境变量中设置：

- `TIMEHUD_STORE_PASSWORD`
- `TIMEHUD_KEY_PASSWORD`
- `TIMEHUD_KEY_ALIAS`（默认 `timehud`）

## 验证结果

见 [VERIFICATION.md](VERIFICATION.md)。
