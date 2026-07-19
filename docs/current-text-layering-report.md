# 当前文本系统分层报告

本文档描述 NeoFontRender 当前这一版的文本渲染分层，重点不是抽象上能分几层，而是代码目录现在实际上怎么分、每层为什么存在、调用关系怎么走。

## 当前目录结构

当前字体相关代码已经按“实际渲染职责”拆成下面几组：

- src/main/java/neofontrender/core/font/FontManager.java
- src/main/java/neofontrender/core/font/backend
- src/main/java/neofontrender/core/font/awt
- src/main/java/neofontrender/core/font/skia
- src/main/java/neofontrender/core/font/support
- src/main/java/neofontrender/core/font/layout

这次整理的核心原则是：

- 根包只保留总入口，不再堆实现细节。
- 真正的渲染器按实现路线分组，而不是把所有字体类平铺在一个目录里。
- 只有“跨渲染器共用”的东西才放共享层。
- 未来预留层保留，但不强行插进当前 Skia 热路径。

## 现在的分成逻辑

### 1. 管理入口层

职责：选择当前用哪条渲染路径，并管理 reload、close、fallback。

主要文件：

- src/main/java/neofontrender/core/font/FontManager.java

为什么单独放在根包：

- 它不是某个具体渲染器的实现，而是整套字体系统的总调度入口。
- Mixin、命令、GUI 调试入口都应该先碰它，而不是直接碰具体后端。

当前行为：

- 选 vanilla 时直接关闭自定义渲染。
- 选 Skia 时先检查运行时兼容性，再尝试初始化 Skia。
- Skia 不兼容或初始化失败时，自动回退到 AWT atlas 路径。

### 2. 后端抽象层

职责：定义“高级文本后端”对外要提供的最小能力。

主要文件：

- src/main/java/neofontrender/core/font/backend/TextRenderBackend.java
- src/main/java/neofontrender/core/font/backend/TextRenderResult.java

为什么单独拆成 backend：

- 这一层不属于 AWT，也不属于 Skia。
- 它表达的是调用面：测量、渲染、返回 draw-ready 结果。
- 接入层只应该依赖这层，避免再把 mixin 绑死到某个具体实现类上。

当前定位：

- 这是“高级整段文本后端”的抽象面。
- 现在主要由 Skia 实现。
- AWT atlas 路径还没有完全适配成同一种 backend 模型，所以暂时不硬塞进来。

### 3. AWT atlas 渲染层

职责：承载传统逐字 glyph atlas 路径，也就是当前的 fallback 文本实现。

主要文件：

- src/main/java/neofontrender/core/font/awt/FontSet.java
- src/main/java/neofontrender/core/font/awt/FontTexture.java
- src/main/java/neofontrender/core/font/awt/BakedGlyph.java
- src/main/java/neofontrender/core/font/awt/GlyphInfo.java
- src/main/java/neofontrender/core/font/awt/GlyphProvider.java
- src/main/java/neofontrender/core/font/awt/providers/AwtTtfGlyphProvider.java
- src/main/java/neofontrender/core/font/awt/providers/MissingGlyphProvider.java

为什么这些类归到 awt：

- 它们都服务同一条渲染路线：AWT 光栅化字形，再写入 Minecraft atlas，再逐 glyph 绘制。
- 它们的对象模型也是一套闭环：provider -> glyph info -> atlas texture -> baked glyph。
- 这套模型不是通用的“后端接口”，而是 AWT atlas 渲染器自己的内部结构。

当前定位：

- 这是默认 fallback 路线。
- 这条路仍然偏 legacy，但职责已经集中，不再散落在根包里。

### 4. Skia 渲染层

职责：承载高级整段文本路径，包括 paragraph layout、复杂脚本、emoji fallback 和位图输出。

主要文件：

- src/main/java/neofontrender/core/font/skia/SkijaTextRenderer.java
- src/main/java/neofontrender/core/font/skia/SkijaRuntimeSupport.java
- src/main/java/neofontrender/core/font/skia/SkijaDebugTextRenderer.java
- src/main/java/neofontrender/core/font/skia/SkijaEmojiTest.java

为什么这些类归到 skia：

- 它们都直接依赖 Skija / Paragraph 能力。
- 它们表达的是同一条实现路线，不应该再和 AWT atlas 类混放。
- SkijaRuntimeSupport 也放在这里，因为它判断的是“Skia 这一组能不能启用”，不是全局字体系统通用逻辑。

当前定位：

- 这是当前唯一实现了 TextRenderBackend 的高级文本渲染器。
- 它内部自己维护 measure/render cache。
- 当前仍然走“Skia 直接接 backend”的开洞法，不通过 layout 预留层中转。

额外说明：

- 现在 SkijaRuntimeSupport 的最低运行时门槛是 Java 9+，并按 windows/linux/macos 和 x64/arm64 做 embedded native 选择。

### 5. 共享支持层

职责：放两条渲染路线都可能复用的底层支持逻辑。

主要文件：

- src/main/java/neofontrender/core/font/support/FontPixelUtils.java
- src/main/java/neofontrender/core/font/support/FontRenderPipeline.java
- src/main/java/neofontrender/core/font/support/FontRenderTuning.java

为什么单独拆成 support：

- 这些类不是某一个渲染器自己的业务对象。
- 它们解决的是共性问题：像素清理、OpenGL 绘制状态、纹理过滤和 raster scale 调整。
- 如果继续留在根包，会和具体渲染器实现类混在一起，边界还是不清楚。

当前定位：

- AWT atlas 路径会用到这层。
- Skia 输出位图贴图时也会复用这里的一部分逻辑。

### 6. 预留布局层

职责：保留未来“先布局、后绘制”的后端中间模型，但当前不强行接入现有 Skia 实现。

主要文件：

- src/main/java/neofontrender/core/font/layout/TextLayoutMode.java
- src/main/java/neofontrender/core/font/layout/TextMeasureRequest.java
- src/main/java/neofontrender/core/font/layout/TextRenderRequest.java
- src/main/java/neofontrender/core/font/layout/TextLayoutCache.java
- src/main/java/neofontrender/core/font/layout/TextLayoutResult.java
- src/main/java/neofontrender/core/font/layout/CachedTextLayout.java

为什么它还单独保留：

- 这层表达的是未来可能需要的通用布局对象，而不是当前某条渲染路线的必要步骤。
- 现在如果强行把它接到 Skia 前面，只会增加一层没有独立收益的中转。
- 所以它现在是“预留层”，不是“必经层”。

当前定位：

- 保留可扩展点。
- 不进入当前主热路径。
- 等以后真有独立布局对象需求时再扩展，而不是现在为了抽象而抽象。

## 当前调用关系

### Skia 路径

当前高级文本主路径是：

FontRenderer mixin
-> FontManager
-> TextRenderBackend
-> SkijaTextRenderer
-> paragraph layout / rasterize
-> TextRenderResult.draw()

这条路径的设计重点是：

- 接入层只认 backend 抽象。
- Skia 直接实现 backend。
- 不强塞 layout 中间层。

### AWT atlas 路径

当前 fallback 路径是：

FontRenderer mixin
-> FontManager
-> FontSet
-> GlyphProvider / FontTexture
-> BakedGlyph.render()

这条路径的设计重点是：

- 让 legacy atlas 模型完整收拢到 awt 包内。
- 不再把 atlas 相关类散放在字体根包。

## 这次整理后的实际收益

- FontManager 现在是明确的系统入口，而不是实现堆放点。
- backend 只放抽象面，调用关系更清楚。
- AWT fallback 路径已经能从目录结构上一眼看出是独立渲染器。
- Skia 相关实现和运行时探测现在是一组完整的 skia 包。
- 共用辅助逻辑被提到 support，不再和具体渲染器混在一起。
- layout 预留层被保留，但不再误导为“当前已经接入主路径”。

## 现在的判断标准

后续再加类时，建议按下面的标准放置：

- 负责全局选择、reload、fallback 的，放 root 的 FontManager 周边。
- 只是定义后端调用协议的，放 backend。
- 只服务 AWT atlas 渲染器的，放 awt。
- 只服务 Skia 渲染器的，放 skia。
- 被多个渲染器复用的底层工具，放 support。
- 只是未来预留的布局对象和缓存键，放 layout。

这样分的目的不是追求抽象层数，而是让“哪个类服务哪条渲染路线”在目录上直接可见。