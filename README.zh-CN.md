<p align="center">
  <img src="logo.svg" alt="Neo Font Render" width="200">
</p>

<h1 align="center">Neo Font Render</h1>

<p align="center">
  <img src="https://img.shields.io/badge/version-0.2.5-blue" alt="版本 0.2.5">
  <img src="https://img.shields.io/badge/modularui-3.1.6%2B-green" alt="ModularUI 3.1.6+">
</p>

<p align="center">
  面向 Minecraft 1.12.2 的现代字体渲染增强模组。<br>
  当前同时提供可配置的 AWT 图集渲染路径和 Skia 段落渲染路径，用来处理更现代的文本布局与 emoji。
</p>

<p align="center">
  <a href="README.md">English</a> |
  <a href="https://sirrus.cc">网站</a> |
  <a href="https://github.com/AndreaFrederica/NeoFontRender">GitHub</a>
</p>

## 简介

Neo Font Render 会把 Minecraft 1.12.2 传统的位图字体路径替换为一套更接近现代 UI 文本系统的渲染栈。

当前已经落地的能力包括：

- 支持系统字体和外部 TTF 文件，并带有 fallback 字体链
- 内置字体资源，包括 bundled 的 Sarasa UI SC 和 Noto Color Emoji
- 支持 Skia 段落渲染，覆盖 ligature、kerning、fallback、BiDi、复杂脚本和 emoji
- 当 Skia 不可用或初始化失败时，自动回退到 AWT atlas 渲染器
- 提供超采样、自适应倍率、mipmap、插值、shader 补偿、边缘处理等高级选项
- 修复 GUI 输入链路里的非 BMP / emoji 字符截断问题
- 为告示牌编辑器补上 Ctrl+V 粘贴和自动换行
- 提供游戏内配置界面、emoji 测试界面、快捷键和诊断命令

<p align="center">
  <img src="docs/screenshot.png" alt="配置界面" width="800">
</p>

<p align="center">
  <img src="docs/images/sign-paste-demo.png" alt="告示牌粘贴与emoji演示" width="800">
</p>

## Skia 支持

当前版本已经支持通过 `SkijaTextRenderer` 使用 Skia 渲染高级文本。

- Skia 模式要求 Java 9 及以上
- 已内置 Windows x64、Linux x64、Linux arm64、macOS x64、macOS arm64 的 native 包
- 如果当前运行时不兼容，模组会记录原因并自动回退到 AWT atlas 路径，而不是直接退回 vanilla

也就是说：

- Java 9+ 可以使用完整的 Skia 段落渲染路径
- Java 8 仍然可以使用 AWT fallback 路径运行模组

## 当前包含的补丁与功能

除了字体替换本身，当前代码里还已经包含这些补丁和增强：

- FontRenderer mixin 中的整串 Skia 渲染路径
- 传统 AWT glyph atlas fallback 渲染器
- GUI 输入链路的 Unicode IME 修复，解决 emoji 和其他非 BMP 字符被截断的问题
- 告示牌编辑器的 Ctrl+V 粘贴补丁，并按宽度自动换行到 4 行
- 可配置的内置字体，包括 bundled 的 Sarasa UI SC 和 emoji 字体
- 自定义字体纹理的过滤模式控制
- 游戏内 emoji 测试界面和字体诊断命令

## 快捷键与命令

默认快捷键：

- `O` 打开主配置界面
- `P` 打开 emoji 测试界面

可用命令：

- `/neofontrender info`
- `/neofontrender fonts`
- `/neofontrender reload`
- `/neofontrender test`
- `/neofontrender gui`

## 配置文件

主配置文件会生成在：

- `.minecraft/config/neofontrender.toml`

外部字体文件可以放在：

- `.minecraft/neofontrender/fonts/`

`font.name` 和 `font.fallbacks` 既可以写系统字体名，也可以写 TTF 文件路径。下面这份是按当前默认值和首启注释整理出来的完整配置样例：

```toml
# 启用或禁用整套字体替换管线。
enabled = true

# 字体选择和栅格化设置。
[font]
# 主字体名称或 TTF 文件路径，也支持逗号/分号分隔的字体列表。
name = "neofontrender:fonts/sarasa_ui_sc_regular.ttf"
# 主字体缺字时继续查询的 fallback 字体名或 TTF 路径。
fallbacks = ["Serif", "Monospaced"]
# 字体样式：0=普通，1=粗体，2=斜体，3=粗斜体。
style = 0
# 字号，单位是像素。8.0 接近原版 1.12 UI 文本高度。
size = 10.0
# 栅格化超采样倍率，最终栅格分辨率约等于 size * oversample。
oversample = 12.0
# 先自动把字体基线对齐到 Minecraft 参考基线，再做手动偏移。
autoBaseline = true
# 在自动对齐之后再施加的垂直偏移，正值向下。
baselineShift = 0.0
# Minecraft 空间下的参考基线，原版 8px UI 文本大约是 7.0。
referenceBaseline = 7.0
# 是否启用 AWT 抗锯齿。
antialias = true
# AWT 文本抗锯齿模式：off、on、gasp、lcd_hrgb、lcd_hbgr、lcd_vrgb、lcd_vbgr。
antialiasMode = "on"
# 是否启用 fractional metrics 以获得更精细的定位。
fractionalMetrics = true
# 是否总是把内置字体，例如 Noto Color Emoji，追加到 fallback 链里。
builtinFallbacks = true

# 文字阴影设置。
[shadow]
# 阴影偏移距离，单位像素。
length = 1.0
# 阴影透明度倍率，范围 0.0-1.0。
opacity = 0.25

# OpenGL 纹理渲染设置。
[rendering]
# 文本渲染引擎：vanilla、sfr、skia。
engine = "skia"
# Skia 模式下把整段格式化字符串作为一个 paragraph 渲染，让 shaping、ligature、kerning、emoji ZWJ 和 BiDi 能跨整串生效。
skiaAdvancedStringMode = true
# 使用 GL_LINEAR，而不是 GL_NEAREST。
interpolation = false
# 是否启用字体纹理 mipmap。
mipmap = true
# 把 SFR/Skia 栅格倍率限制到当前 GUI 像素倍率，减少过度下采样模糊。
adaptiveRasterScale = true
# 使用单独的 straight-alpha 文本绘制管线。彩色 emoji 建议保持关闭。
enhancedTextPipeline = false
# 使用一个兼容旧管线的小 shader 来补偿过细的抗锯齿边缘。
shaderTextPipeline = false
# enhanced shader 管线使用的边缘亮度补偿强度。
brightness = 0.0
# 给完全透明的 Skia 文本像素补邻近 RGB，避免线性采样时出现黑边。
textureEdgeBleed = false

# 性能相关设置。
[performance]
# 在后台线程初始化字体栅格化。
asyncInit = true
# 在启用替换渲染前预热常见 Basic Latin 和 Latin-1 字符。
prewarmBasicLatin = true

# 输入行为相关设置。
[input]
# 允许在原版告示牌编辑器中使用 Ctrl+V 粘贴。这个开关目前故意只保留在配置文件里。
allowSignPaste = true

# 调试日志设置。
[debug]
# 把 IME 输入修复的细节打印到日志里。
imeInput = false
```

几个实用说明：

- `rendering.engine` 支持 `vanilla`、`sfr`、`skia`
- `font.builtinFallbacks = true` 会把内置字体，例如 Noto Color Emoji，追加到 fallback 链尾部
- `input.allowSignPaste` 目前故意只提供配置文件开关
- `debug.imeInput = true` 适合排查 IME / emoji 输入问题

示例一：使用 Skia，并指定主字体和额外 fallback：

```toml
[font]
name = "Microsoft YaHei UI"
fallbacks = [
  "Noto Sans",
  "./neofontrender/fonts/MyExtraFallback.ttf"
]
builtinFallbacks = true

[rendering]
engine = "skia"
adaptiveRasterScale = true
mipmap = true
```

示例二：在 Java 8 或排障时强制走旧渲染路径：

```toml
[rendering]
engine = "sfr"

[input]
allowSignPaste = true
```

## 运行说明

- 目标游戏版本：Minecraft 1.12.2 / Forge 14.23.5.2847
- 当前默认渲染预设：Skia + 自适应倍率
- 编译目标：Java 8 字节码
- Skia 运行要求：Java 9+

## 当前架构

当前文本系统已经按“实际渲染职责”重排：

- `src/main/java/neofontrender/core/font/FontManager.java`：渲染器选择、生命周期和 fallback
- `src/main/java/neofontrender/core/font/backend`：后端抽象层
- `src/main/java/neofontrender/core/font/awt`：传统 AWT atlas 渲染路径
- `src/main/java/neofontrender/core/font/skia`：Skia 渲染器、运行时检查和调试工具
- `src/main/java/neofontrender/core/font/support`：共享渲染工具和 GL 辅助逻辑
- `src/main/java/neofontrender/core/font/layout`：未来布局层的预留接缝，目前不强行接入 Skia 热路径

进一步说明可以看：

- [docs/current-text-layering-report.md](docs/current-text-layering-report.md)
- [docs/arc3d-modernui-analysis.md](docs/arc3d-modernui-analysis.md)

## 开发环境

本项目基于 CleanroomMC TemplateDevEnv 和 RetroFuturaGradle。

- Gradle wrapper + RetroFuturaGradle 2.0.2
- Forge 14.23.5.2847 for Minecraft 1.12.2
- 使用 Azul Java 8 toolchain 编译
- 启用 CoreMod + MixinBooter

## 构建

```bash
./gradlew build
```

## 运行

```bash
./gradlew runClient
```

## 链接

- **网站**: https://sirrus.cc
- **作者**: AndreaFrederica
- **English README**: [README.md](README.md)

## 许可证

MIT License，详见 [LICENSE](LICENSE)。