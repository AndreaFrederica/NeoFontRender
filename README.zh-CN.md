<p align="center">
  <img src="logo.svg" alt="Neo Font Render" width="200">
</p>

<h1 align="center">Neo Font Render</h1>

<p align="center">
  面向 lwjgl3ify 运行环境的 Minecraft 1.7.10 现代文本整形与字体渲染模组。<br>
  <a href="README.md">English</a> · <a href="https://github.com/AndreaFrederica/NeoFontRender">GitHub</a>
</p>

## 功能

Neo Font Render 用可配置的现代渲染器替代 Minecraft 1.7.10 的传统位图字体路径。

- 默认使用 Cosmic Text，提供原生的文本整形与栅格化。
- 保留 SFR/AWT 兼容渲染器，便于兼容与排障。
- 可恢复原版渲染器，同时保留本 Mod 的输入与兼容性修复。
- 支持系统字体、本地 TTF/OTF、内置 Noto Sans SC、Noto Color Emoji 与 fallback 字体链。
- 包含 Unicode/IME 输入修复、告示牌粘贴与换行、可配置的告示牌优化、游戏内设置界面和诊断命令。

<p align="center">
  <img src="docs/screenshot.png" alt="Neo Font Render 设置界面" width="800">
</p>

## 环境与安装

- Minecraft 1.7.10 与 lwjgl3ify 运行环境。
- Java 25。
- [ModularUI2 2.3.81+](https://github.com/GTNewHorizons/ModularUI2)。

下载适合安装方式的发行包并放入 `mods` 文件夹。一般建议直接使用 `full` 包。

| 文件 | 使用场景 |
| --- | --- |
| `neofontrender-<version>-full.jar` | 完整的一体化安装。 |
| `neofontrender-<version>-core.jar` | 只使用核心渲染器与系统字体。 |
| `neofontrender-resources-<version>.jar` | 与 `core` 搭配，使用内置字体资源。 |

不要将 `full` 与拆分的 `core` 或 `resources` 包同时安装。

### 可选 UI Enhancements 附属模组

[NeoFontRender UI Enhancements](addons/ui-enhancements/README.md) 是配套的可选附属模组，提供
现代工具提示、可配置 HUD 状态条、增强聊天、平滑滚动、文本框光标与界面过渡效果。附属使用
独立配置文件，但设置页面会直接嵌入 NeoFontRender 设置界面。

- [附属说明与构建方法](addons/ui-enhancements/README.md)
- [下载 UI Enhancements Release](https://github.com/AndreaFrederica/NeoFontRender/releases?q=ui-enhancements)

## 快速开始

按 `O` 打开设置界面；按 `P` 打开 emoji 测试界面。主配置文件位于：

```text
.minecraft/config/neofontrender.toml
```

自定义字体可放入：

```text
.minecraft/neofontrender/fonts/
```

新配置的默认渲染设置：

```toml
[font]
name = "Noto Sans SC"
path = "neofontrender:fonts/noto_sans_sc-regular.otf"
fallbacks = ["Serif", "Monospaced"]
size = 8.5

[rendering]
engine = "cosmic"
interpolation = true
advancedStringMode = true
```

`rendering.engine` 可设为 `cosmic`、`sfr` 或 `vanilla`。Cosmic 缺字时会查询已配置的系统字体和
内置资源；选择 `sfr` 可使用 AWT 兼容路径，选择 `vanilla` 可恢复 Minecraft 原版渲染器。

### 兼容性

- ModularUI2 提供设置界面；Neo Font Render 同时处理其文本输入框的 Unicode 编辑。
- 可选的 Tinkers' Construct 与 Mantle 集成会使用选定字体渲染 1.7.10 匠魂手册，同时保持 Mantle 的文本测量与换行行为；可通过 `compat.tinkersconstruct.enabled` 控制。
- UI Enhancements 附属模组会在检测到 NEI 时启用工具提示集成，但不会将 NEI 设为必需依赖。

常用命令：

```text
/neofontrender info
/neofontrender fonts
/neofontrender reload
/neofontrender gui
```

## 外部集成 API

其他客户端 Mod 可以通过稳定 API 修改当前字体，无需直接操作 Neo Font Render 的内部配置或
renderer 类。`apply()` 可以从任意线程调用：更新会被调度到客户端线程，默认保存配置，并只重载
一次字体后端。

```java
import neofontrender.api.FontStyle;
import neofontrender.api.NeoFontRenderApi;
import neofontrender.api.RenderingEngine;

NeoFontRenderApi.updateFont()
        .font("Noto Sans SC")
        .fallbackFonts("Noto Color Emoji", "SansSerif")
        .size(8.5F)
        .style(FontStyle.PLAIN)
        .engine(RenderingEngine.COSMIC)
        .apply();
```

使用 `.persist(false)` 可进行仅当前会话生效的修改。`NeoFontRenderApi.getFontState()` 会返回包含
字体配置和当前后端的不可变快照。将本 Mod 作为可选依赖时，应先通过
`Loader.isModLoaded("neofontrender")` 判断是否已安装，再引用 API。提供给其他 Mod 复用的 GUI
基础组件位于 `neofontrender.client.gui.component.base`。

`font(...)` 会清除 Cosmic 的分字形覆盖，确保选中的字体族在各后端一致生效。需要分别指定
regular、bold、italic 与 bold-italic 字体文件时，可组合使用 `primaryFont(...)` 和
`cosmicFaceOverrides(...)`。

## 开发

项目使用 RetroFuturaGradle 与 GTNH Gradle 构建约定，基于 Gradle 9.3.1 和 Java 25 工具链。

```bash
./gradlew runClient25
./gradlew build
./gradlew packageVariants
```

`packageVariants` 会在 `build/libs` 生成 full、core 与 resources 三种发行包。本地构建会通过 Cargo 编译 Cosmic JNI；CI 会组装供 full 和 core 共同使用的 Windows、Linux 与 macOS native 集合。

## 项目信息

- 许可证：[MIT](LICENSE)
- 贡献者：[AndreaFrederica](https://github.com/AndreaFrederica)、[baka-gourd](https://github.com/baka-gourd)、[DHJComical](https://github.com/DHJComical)
- 设计文档：[docs](docs/)
