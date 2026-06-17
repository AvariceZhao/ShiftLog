# ShiftLog

> 打工人的本地打卡记录 — 迟到早退一目了然，周期工时自己掌控。

[![Release](https://img.shields.io/github/v/release/AvariceZhao/ShiftLog?label=Release)](https://github.com/AvariceZhao/ShiftLog/releases/latest)
![Android 8+](https://img.shields.io/badge/Android-8.0%2B-3DDC84?logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-2.0-7F52FF?logo=kotlin&logoColor=white)
![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-UI-4285F4?logo=jetpackcompose&logoColor=white)

**[下载最新 APK →](https://github.com/AvariceZhao/ShiftLog/releases/latest)**（arm64 真机）

---

## 预览

<table align="center">
  <tr>
    <td align="center"><b>打卡首页</b><br><img src="docs/screenshots/home.jpg" alt="打卡首页" width="190"><br><sub>一键上下班打卡</sub></td>
    <td align="center"><b>桌面小组件</b><br><img src="docs/screenshots/widget.png" alt="桌面小组件" width="190"><br><sub>2×2 快捷打卡</sub></td>
  </tr>
  <tr>
    <td align="center"><b>历史 · 日历</b><br><img src="docs/screenshots/history-calendar.jpg" alt="历史日历" width="190"><br><sub>出勤色块 + 周期统计</sub></td>
    <td align="center"><b>历史 · 列表</b><br><img src="docs/screenshots/history-list.jpg" alt="历史列表" width="190"><br><sub>记录列表</sub></td>
  </tr>
</table>

---

## 功能

| | |
|---|---|
| **打卡** | 上班 / 下班一键记录；跨夜班次自动归属 |
| **判定** | 迟到、早退、旷工、缺卡 |
| **周期** | 可配置工资周期、标准上下班、目标天数 / 工时 |
| **进度** | 剩余天数 / 工时、所需日均 |
| **历史** | 列表与日历切换；补录、编辑、CSV 导出 |
| **备份** | JSON 全量备份与恢复 |
| **小组件** | 2×2 桌面组件，到点自动刷新状态 |
| **其它** | 桌面快捷打卡、GitHub 检查更新 |

详细需求见 [`docs/clock-in-app-requirements-lite.md`](docs/clock-in-app-requirements-lite.md)。

---

## 从源码运行

1. Android Studio **Open** 本目录
2. Gradle Sync 完成后连接设备，点击 **Run**

命令行构建（可选）：

```powershell
$env:ANDROID_HOME = "D:\AndroidStudioSDK"
$env:JAVA_HOME = "D:\Android Studio\studio\jbr"
.\gradlew.bat assembleDebug
```

Release 签名打包见 [`keystore.properties.example`](keystore.properties.example)，执行：

```powershell
.\gradlew.bat clean packageReleaseApk
```

产物：`app/release/ShiftLog-v{version}-arm64-release.apk`

---

## 技术栈

Kotlin · Jetpack Compose · Room · Glance App Widget · WorkManager

---

## License

[MIT](LICENSE)
