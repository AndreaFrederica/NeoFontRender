# Arc3D / ModernUI 移植分析

本文档记录 NeoFontRender 当前文本渲染整理结果，以及对 Arc3D、ModernUI、ModernUI-MC 与高版本 Minecraft 文本系统的定向分析结论，作为后续移植工作的基线。

## 当前仓库的实际接缝

当前 1.12.2 仓库里，和后续后端迁移最相关的本地入口主要有三层：

- FontRenderer 接入层：src/main/java/neofontrender/mixin/MixinFontRenderer.java
- 后端选择与生命周期：src/main/java/neofontrender/core/font/FontManager.java
- 当前高级整段渲染实现：src/main/java/neofontrender/core/font/skia/SkijaTextRenderer.java

本次整理新增了两个最小抽象：

- TextRenderBackend：负责 measure / render / renderFormatted / 预热 / 字体族查询
- TextRenderResult：负责 advance 与 draw

随后又补了一层更接近未来移植目标的缓存骨架：

- TextLayoutCache：后端无关的测量 / 渲染结果缓存
- TextMeasureRequest：后端无关的测量请求键
- TextRenderRequest：后端无关的渲染请求键

并且新增了真正的布局结果对象骨架：

- TextLayoutResult：后端无关的布局结果接口
- CachedTextLayout：当前缓存层使用的最小布局结果实现

目的不是引入新的布局逻辑，而是先把 mixin、调试命令、测试界面对具体 Skija 类名的依赖去掉。这样未来新增 Arc3D 或其它整段文本后端时，可以优先复用现有接入面，而不是再改一轮 FontRenderer hook。

## 参考代码位置

本次复核的外部参考代码位于以下本机目录：

- D:/tmp/ModernUI
- D:/tmp/ModernUI-MC
- D:/tmp/Arc3D

重点确认过的类：

### ModernUI core

- icyllis.modernui.graphics.text.ShapedText
- icyllis.modernui.graphics.text.LayoutPiece
- icyllis.modernui.graphics.text.FontCollection
- icyllis.modernui.graphics.text.OutlineFont
- icyllis.modernui.graphics.text.EmojiFont

### ModernUI-MC

- icyllis.modernui.mc.text.TextLayoutEngine
- icyllis.modernui.mc.text.TextLayoutProcessor
- icyllis.modernui.mc.text.TextLayout
- icyllis.modernui.mc.text.GlyphManager
- icyllis.modernui.mc.text.ModernFontAtlas
- icyllis.modernui.mc.text.mixin.MixinFontRenderer

### Arc3D

- icyllis.arc3d.sketch.TextBlob
- icyllis.arc3d.sketch.GlyphRun
- icyllis.arc3d.sketch.GlyphRunList
- icyllis.arc3d.sketch.GlyphRunBuilder
- icyllis.arc3d.sketch.Canvas.drawTextBlob(...)

## 结论摘要

结论很明确：

- Arc3D 更接近绘制层和已布局 glyph run 的承载层，不是现成可替换 Paragraph 的完整文本布局引擎。
- ModernUI core 才包含 shaping、BiDi、fallback、font run 分段和 glyph positioning 的关键逻辑。
- ModernUI-MC 证明了“整段布局缓存 + glyph atlas + 专用文本渲染器”这条路线可行，但它依赖的是新版本 Minecraft 的 Font / drawInBatch 管线，不能直接移植到 1.12.2。
- 对当前仓库而言，Skija Paragraph 仍然是最现实的完整文本方案，因为它已经覆盖 shaping、fallback、emoji、复杂脚本与格式串整段布局。

## 为什么 Arc3D 不能直接替代当前后端

在 D:/tmp/Arc3D 中可以看到：

- TextBlob 是已布局文本的容器。
- GlyphRun / GlyphRunList 表示 positioned glyph 数据。
- Canvas.drawTextBlob(...) 负责消费这些 glyph run 并绘制。

这说明 Arc3D 文本能力主要集中在“如何绘制一批已经成型的 glyph”。它可以成为未来的渲染后端，但前提是我们先有自己的布局结果对象，至少需要包含：

- glyph ids
- glyph positions
- font run / font ids
- advance / bounds / baseline
- 可选的 cluster advance 与 line break 信息

如果没有这一层，Arc3D 只能替换最后的 draw，而不能替换当前 Skija Paragraph 在 shaping 与布局上的职责。

## 为什么 ModernUI 可以参考但不能整包搬入

### ModernUI core 的可借鉴点

ShapedText 和 LayoutPiece 已经把我们需要的中间结果结构定义得很清楚：

- ShapedText 直接保存 glyph、position、font-run、advance 信息，并显式依赖 ICU4J Bidi。
- LayoutPiece 负责按 font itemize 分段，再执行 complex layout，最终得到 glyph 和位置数组。
- OutlineFont 使用 Java2D Font / GlyphVector 作为底层 shaping 和测量来源。

这条线说明，如果以后不走 Skija Paragraph，而是做自有布局缓存层，那么最值得参考的是 ModernUI core 的“布局结果对象”和“按 font run 分段”的方式，而不是它的 UI 框架本身。

### ModernUI core 当前不适合直接依赖的原因

- 依赖链和 1.12.2 Forge 环境并不轻。
- 代码体量较大，包含大量和当前仓库无关的 UI / resource / runtime 结构。
- 许可证为 LGPL-3.0-or-later，分发合规需要单独评估。
- EmojiFont 源码里直接标了 TODO，说明 color emoji 相关实现并未完整落地。

本次检查到的一个关键事实是：EmojiFont 类头部就有如下注释含义，表明布局代码仍未完全正确，并且渲染部分待补：

- layout code is not correct
- add rendering

所以 color emoji 不能指望“引入 ModernUI core 后自动解决”。

## ModernUI-MC 对我们真正有价值的部分

ModernUI-MC 的价值主要是架构参考，而不是可直接迁移代码。

从 TextLayoutEngine 和它的 MixinFontRenderer 可以看到，它面向的是高版本 Minecraft：

- 它接管的是 net.minecraft.client.gui.Font 的 drawInBatch 系列入口。
- 它有完整的 TextLayoutEngine / TextLayoutProcessor / TextLayout 缓存体系。
- 它围绕 GlyphManager 与 ModernFontAtlas 组织 glyph atlas 与渲染资源。

这和我们现在的 1.12.2 FontRenderer 差异很大。我们当前的入口还是逐字符 / 格式码兼容的老接口，因此不能直接照搬 ModernUI-MC 的 mixin 和 renderer。

但它给出的长期方向是清楚的：

- 先做整段文本布局对象
- 再做布局缓存
- 再把渲染端替换为 atlas / SDF / shader
- emoji 与普通文字尽量分离处理

## 高版本 MC 参考的意义

高版本 Minecraft 的文本系统之所以更容易接入 ModernUI-MC，是因为它本身已经有更现代的 Font、FormattedCharSequence、batch draw 与 font manager 抽象。对我们这个 1.12.2 仓库来说，这些代码更适合作为“接口分层参考”，不适合作为直接移植目标。

换句话说，高版本 MC 对我们最有用的不是某个具体类，而是下面这条结构：

- 文本输入对象
- 样式遍历 / 格式化展开
- 布局缓存
- 绘制后端

这也是本次先整理本仓库抽象层的原因。

## 对当前仓库的建议路线

### 短期

- 继续保留 Skija Paragraph 作为完整文本高级后端。
- 把所有高级文本调用都收敛到 TextRenderBackend / TextRenderResult 接口。
- 继续让 SFR atlas 路径负责传统 AWT glyph atlas 模式。

### 中期

新增一个内部布局层，建议名字可以类似：

- TextLayoutRun
- TextLayoutEntry
- TextLayoutCache

最小输出建议包含：

- glyph ids
- x/y positions
- font run identity
- advance
- baseline / bounds
- formatted segment metadata

这样未来可以有两种接法：

- Skija 作为“布局 + 位图输出”实现
- Arc3D 作为“消费布局结果并绘制 glyph run / text blob”的实现

### 长期

如果以后确实要走 Arc3D / ModernUI 路线，更稳妥的顺序应当是：

1. 先在本仓库内部稳定 TextLayout 抽象与缓存对象。
2. 再评估是否借鉴 ModernUI core 的 run itemize / glyph result 结构。
3. 最后才考虑 Arc3D 绘制后端、glyph atlas、SDF、emoji sprite。

## 当前判断

当前阶段不建议：

- 直接把 ModernUI 整个作为依赖塞进 1.12.2 工程。
- 直接尝试让 Arc3D 替换 Skija Paragraph。
- 在没有内部布局抽象的前提下，开始移植 ModernUI-MC 的 atlas 与 mixin。

当前阶段建议：

- 先完成本仓库文本后端接口整理。
- 保持 Skija 为完整 shaping 方案。
- 把 Arc3D / ModernUI 视为下一阶段的布局与绘制分层参考，而不是立即集成目标。