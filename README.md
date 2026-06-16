# ShiftLog

Kotlin + Jetpack Compose + Room 本地打卡 App。

## 下载

[最新 Release APK](https://github.com/AvariceZhao/ShiftLog/releases/latest)（arm64 真机，Android 8.0+）

## 打开方式

1. Android Studio 选择 **Open**，打开本目录 `clock_in`
2. 等待 Gradle Sync 完成（首次会自动下载依赖并生成 `gradlew`）
3. 连接手机或启动模拟器，点击 Run

## 环境变量（命令行构建时）

```powershell
$env:ANDROID_HOME = "D:\AndroidStudioSDK"
$env:JAVA_HOME = "D:\Android Studio\studio\jbr"
```

Sync 完成后可在项目根目录执行：

```powershell
.\gradlew.bat assembleDebug
```

## 功能

- 上班 / 下班打卡（上班不可重复，下班可覆盖）
- 跨夜班次归属、迟到 / 早退判定
- 可配置工资周期、班次时间、目标天数 / 工时
- 周期历史、统计、目标进度（含日均）
- CSV 导出、补录 / 编辑
- **桌面小组件（2×2）**：今日状态、上下班打卡、剩余进度与所需日均

需求详见 `clock-in-app-requirements-lite.md`。
