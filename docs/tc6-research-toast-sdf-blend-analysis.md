# TC6 ResearchToast 与 Cosmic SDF 混合状态故障分析

## 结论摘要

Minecraft 1.12.2 的 `GlStateManager` 会缓存 OpenGL 状态。Thaumcraft 6.1.BETA26
的研究图标渲染直接调用 `GL11.glEnable/Disable(GL_BLEND)`，没有同步
`GlStateManager` 的缓存。其结果是：驱动中的真实 `GL_BLEND` 已关闭，但
`GlStateManager` 仍可能认为它已开启。

NeoFontRender 的 Cosmic SDF 管线原本只调用：

```java
GlStateManager.enableBlend();
```

当缓存与驱动失步时，这个调用会被状态缓存判定为无须执行，真实的
`GL_BLEND` 因而继续保持关闭。SDF fragment shader 对字形外部像素输出固定的
文字 RGB 和零 alpha；在混合关闭时，零 alpha 不再保护目标 framebuffer，整个
文字 quad 会被写成纯文字颜色。因此 TC6 研究提示中的“研究完成！”偶发显示为
一块紫色矩形。

最终修复不依赖 TC6 专用 Mixin，而是在 Cosmic 普通 RGBA 与 SDF 两个绘制状态
作用域中同时维护 `GlStateManager` 缓存和真实 OpenGL 状态：

1. 先通过 `GlStateManager` 同步 Minecraft 的缓存；
2. 再通过原始 `GL11/GL14` 调用强制设置驱动状态；
3. 退出作用域时，根据进入前从驱动读取的真实状态，同时恢复缓存和驱动。

## 影响范围

- Minecraft：1.12.2
- 运行环境：Cleanroom 0.6.x、Java 25
- Thaumcraft：6.1.BETA26
- 字体后端：Cosmic
- SDF：启用时稳定复现；普通 Cosmic 路径存在同类隐患
- 受影响界面：TC6 `ResearchToast`
- 典型现象：第一行“研究完成！”变成紫色实心矩形，第二行研究名称仍正常

这不是 TC6 研究数据、翻译文本、字体测量、CJK 排版、纹理损坏或 UIE 背景合成
导致的问题。TC6 研究界面背景和自定义 tooltip 的兼容问题属于另外两条链路，见
`docs/tc6-ui-effects-compatibility-analysis.md`。

## 可见现象

一次性授予多项研究后，屏幕上会同时出现多个研究完成提示。部分提示正常显示：

```text
研究完成！
核心媒介：奥术地雷
```

另一些提示的第一行会变为与标题颜色相同的紫色矩形，但第二行研究名称仍然可以
正常显示。错误矩形的位置、宽度和高度与第一行 Cosmic 文字纹理的 quad 一致。

该现象具有以下特征：

- 不是所有研究都失败；
- 失败与研究图标类型有关；
- 只有第一行标题容易失败；
- 开关 NFR 字体替换可以改变结果；
- 开关 SDF 不会改变 TC6 留下状态的事实，但启用 SDF 时矩形症状最直接；
- 字体纹理一旦生成，后续缓存命中稳定，错误仍可发生。

## TC6 原始调用链

通过反编译 `Thaumcraft-1.12.2-6.1.BETA26.jar`，研究提示的入口为：

```text
ResearchToast.func_193653_a(GuiToast, long)
  -> 绑定 thaumcraft:textures/gui/hud.png
  -> 绘制 toast 背景
  -> GuiResearchBrowser.drawResearchIcon(entry, 6, 8, 0, false)
  -> FontRenderer.drawString(I18n.translate("research.complete"), 30, 7, 0xA239F1)
  -> FontRenderer.getStringWidth(entry.getLocalizedName())
  -> FontRenderer.drawString(entry.getLocalizedName(), ... , 0xFFAA09)
```

标题的颜色常量 `10631665` 即 `0xA239F1`。运行时采样也记录到：

```text
text='研究完成！' color=0xa239f1 shadow=false
```

因此截图中的紫色矩形不是任意污染色，它就是标题的前景色。

## TC6 图标渲染留下的状态

`GuiResearchBrowser.drawResearchIcon` 在入口和出口使用原始 LWJGL 调用：

```java
GL11.glPushMatrix();
GL11.glEnable(GL11.GL_BLEND);
GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

// ResourceLocation、ItemStack 或 focus 图标分支

GL11.glDisable(GL11.GL_BLEND);
GL11.glPopMatrix();
```

这里有两个关键问题：

1. 方法退出时无条件关闭 blend，而不是恢复进入前状态；
2. 它直接调用 `GL11`，所以不会更新 Minecraft 的 `GlStateManager` 缓存。

### ItemStack 分支

当研究图标是 `ItemStack` 时，TC6 还会经过：

```text
RenderHelper.enableGUIStandardItemLighting
GL_LIGHTING / GL_RESCALE_NORMAL / GL_COLOR_MATERIAL
RenderItem.renderItemAndEffectIntoGUI
depthMask(true)
enable(GL_DEPTH_TEST)
```

`RenderItem` 内部会通过 `GlStateManager` 修改一部分状态，而 TC6 最后再用原始
`GL11.glDisable(GL_BLEND)` 关闭 blend。这个组合最容易形成：

```text
GlStateManager cache: blend = true
OpenGL driver:        blend = false
```

诊断日志中，失败状态与 `material:true` 高度相关。`GL_COLOR_MATERIAL` 不是紫色矩形
的直接原因；它是 ItemStack 图标分支留下的识别标志。

## Minecraft 1.12.2 状态缓存为何会失效

`GlStateManager.enableBlend()` 并不是无条件执行 `glEnable`。它维护内部布尔缓存，
其逻辑可概括为：

```java
if (!blendState.currentState) {
    blendState.currentState = true;
    GL11.glEnable(GL11.GL_BLEND);
}
```

如果其他代码绕过 `GlStateManager`，直接执行：

```java
GL11.glDisable(GL11.GL_BLEND);
```

内部缓存不会改变。下一次 `GlStateManager.enableBlend()` 看到缓存仍为 `true`，便不会
调用驱动。此时 Java 层认为 blend 已开启，实际 OpenGL 状态却仍是关闭的。

同样的风险也适用于 blend factors：原始 `glBlendFunc` 可以改变驱动中的 RGB/alpha
因子，但不会同步 `GlStateManager` 保存的因子缓存。

## NFR 字体调用链

标题进入 vanilla `FontRenderer.drawString` 后，被 NFR 的 Mixin 接管：

```text
ResearchToast
  -> FontRenderer.drawString(..., shadow=false)
  -> MixinFontRenderer.sfr$onDrawString
     -> FontRenderTuning.updateFromCurrentGlState(false)
     -> TextRenderBackend.renderFormatted(...)
     -> CosmicRenderedText.draw(...)
```

采样确认这一调用使用：

```text
engine=cosmic
advanced=true
shadow=false
fontSize=8.5
advance=42.5
```

`CosmicRenderedText.draw` 再根据纹理类型分为两条路径：

```text
CosmicRenderedText.draw
  +-- 普通 RGBA/premultiplied texture
  |     -> PremultipliedBlendState
  |     -> drawRgba
  |
  +-- SDF texture
        -> CosmicSdfPipeline.begin
        -> bind SDF texture
        -> CosmicSdfPipeline.draw
        -> CosmicSdfPipeline.State.close
```

最初的修复只补强了 `PremultipliedBlendState`。实际复现日志中的所有目标绘制均显示：

```text
program=18
```

这证明标题使用的是独立的 SDF shader 路径，所以普通 RGBA 状态保护器的修复无法
影响故障。这正是第一次强制 blend 后视觉问题仍然存在的原因。

## SDF shader 为何产生整块纯色

Cosmic SDF fragment shader 的核心逻辑为：

```glsl
float distance = texture2D(sdfTexture, gl_TexCoord[0].st).r;
float coverage = smoothstep(..., distance);
gl_FragColor = vec4(textColor.rgb, textColor.a * coverage);
```

字形内部：

```text
RGB   = 文字颜色
alpha = 接近 1
```

字形外部：

```text
RGB   = 仍然是文字颜色
alpha = 0
```

正确开启 straight-alpha blend 时，字形外部的零 alpha 不会改变 framebuffer：

```text
out.rgb = src.rgb * src.alpha + dst.rgb * (1 - src.alpha)
```

当 blend 关闭时，OpenGL 不再使用 alpha 合成，shader 输出会直接覆盖目标：

```text
out.rgb = src.rgb
```

因此 SDF quad 的每个像素，包括 `coverage=0` 的字形外区域，都会写入
`textColor.rgb`，最终形成尺寸与文字 quad 完全一致的紫色矩形。

## 为什么只有第一行失败

这个现象可以由同一状态失步完整解释，不需要假设标题和研究名称使用了不同字体实现。

一个典型的 ItemStack toast 帧如下：

```text
1. RenderItem/TC6 图标路径让 GlStateManager 缓存认为 blend=true。
2. TC6 原始 GL11.glDisable(GL_BLEND) 让驱动实际变为 blend=false。
3. 第一行“研究完成！”进入 SDF begin。
4. GlStateManager.enableBlend() 因缓存为 true 而跳过，第一行绘制失败。
5. 第一行 State.close() 根据进入时 glIsEnabled=false 恢复状态；这一过程也把
   GlStateManager 缓存重新推回 false。
6. 第二行研究名称进入 SDF begin。
7. 此时缓存为 false，GlStateManager.enableBlend() 真正调用 glEnable，第二行正常。
```

所以“只有研究完成几个字出问题”反而是状态缓存失步的强证据：第一行既是受害者，
又在退出状态作用域时部分修正了缓存，第二行因此得以正常绘制。

## 运行时采样与证据

### 第一阶段：字体几何与缓存

临时诊断器仅匹配：

```text
Research Completed!
研究完成!
研究完成！
```

并由 `debug.renderStats` 控制。第一轮有效采样显示：

```text
xy=(30.0, 7.0)
quad=37.5x9.0
offset=(-0.25, -0.78974915)
scale=8.0
advance=42.5
```

光栅化只发生一次，后续均命中同一缓存。不同帧之间没有 width、height、advance、
offset 或坐标跳变，因此排除了：

- CJK 宽度测量错误；
- Cosmic cache key 不稳定；
- 每帧重复光栅化生成不同纹理；
- TC 标题坐标或矩阵抖动；
- tooltip 换行和布局代码影响 ResearchToast。

### 第二阶段：GL 状态

早期采样器的当前颜色格式化存在错误，导致 GL 摘要退化为：

```text
gl=unavailable(IllegalFormatConversionException)
```

修正采样器后，进入 Cosmic 前稳定观察到：

```text
blend:false
alpha:true/false
depth:true(mask=true func=515)
fog:false
texture:true
program=0
```

这证明 TC6 在标题前确实关闭了真实 blend。

### 第三阶段：before/prepared 对照

增强采样在状态保护器前后分别记录：

```text
cosmic.draw.before
cosmic.draw.prepared
```

同时记录 active texture、client active texture、unit 0/1、texture env 和 shader program。

最终故障日志共包含：

```text
cosmic.draw.before   = 2664
cosmic.draw.prepared = 2662
cosmic.render.cache  = 2663
cosmic.render.raster = 1
font.drawString      = 2670
```

`prepared` 阶段的关键统计为：

```text
blend=false = 1462
blend=true  = 1200
program=18  = 2662
```

状态组合为：

```text
1462 x blend=false, material=true
1157 x blend=true,  material=false
  43 x blend=true,  material=true
```

这直接证明：

1. SDF shader 已成功绑定；
2. SDF 状态准备结束后，真实 blend 仍可能关闭；
3. 失败高度集中在 ItemStack 图标状态分支；
4. 问题发生在 `CosmicSdfPipeline.begin()`，不是调用前状态本身不可接受。

### 被排除的活动纹理单元假设

所有 5326 条 draw 阶段记录均为：

```text
active=33984
client=33984
unit0 texture=true
unit1 texture=false
```

`33984` 即 `GL_TEXTURE0`。因此没有证据表明字体纹理被绑定到 lightmap unit；活动纹理
单元不是此次紫色矩形的根因。

## 修复实现

### 普通 Cosmic RGBA 路径

`PremultipliedBlendState` 在进入时执行：

```java
GlStateManager.enableBlend();
GL11.glEnable(GL11.GL_BLEND);

GlStateManager.tryBlendFuncSeparate(
        GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA,
        GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA);
GL14.glBlendFuncSeparate(
        GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA,
        GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA);
```

这里使用 premultiplied-alpha 因子，因为普通 Cosmic RGBA 纹理已经预乘 alpha。

### Cosmic SDF 路径

`CosmicSdfPipeline.begin()` 在进入时执行：

```java
GlStateManager.enableBlend();
GL11.glEnable(GL11.GL_BLEND);

GlStateManager.tryBlendFuncSeparate(
        GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA,
        GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA);
GL14.glBlendFuncSeparate(
        GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA,
        GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA);
```

SDF shader 输出非预乘 RGB，因此 RGB 使用 straight-alpha 因子；alpha 通道继续使用
`ONE / ONE_MINUS_SRC_ALPHA`。

### 状态恢复

两个作用域都先从驱动读取进入前的真实状态。退出时不再只依赖缓存：

```java
GlStateManager.tryBlendFuncSeparate(oldSrcRgb, oldDstRgb, oldSrcAlpha, oldDstAlpha);
GL14.glBlendFuncSeparate(oldSrcRgb, oldDstRgb, oldSrcAlpha, oldDstAlpha);

if (oldBlendEnabled) {
    GlStateManager.enableBlend();
    GL11.glEnable(GL11.GL_BLEND);
} else {
    GlStateManager.disableBlend();
    GL11.glDisable(GL11.GL_BLEND);
}
```

这样无论进入作用域时缓存与驱动是否一致，退出后两边都会收敛到进入前的真实状态。

## 为什么不使用 TC6 专用 Mixin

TC6 是本次问题的触发者，但 NFR 的字体 quad 必须能够在任意 legacy caller 留下的状态中
正确合成。把修复写进 TC6 Mixin 会有以下缺点：

- 只修复一个已知调用方；
- 其他直接调用 `GL11` 的旧模组仍会触发相同问题；
- Cosmic 的“局部捕获、强制正确状态、完整恢复”契约仍不成立；
- 增加核心字体行为对 TC6 类和版本的耦合。

因此最终策略是：

- TC6 专用 Mixin 继续只处理研究背景和 `UtilsFX` tooltip 等 TC 私有语义；
- blend/cache 一致性由 Cosmic 渲染器自身保证；
- 核心修复不引用任何 Thaumcraft 类，可被移植到 1.7.10 或其他版本。

## 验证状态

### 已完成

- 实机复现并记录错误截图；
- 确认 TC6 `ResearchToast` 原始字节码调用链；
- 确认标题进入 NFR Cosmic 后端；
- 确认字体 geometry 和 cache 稳定；
- 确认 SDF prepared 阶段存在 `blend=false`；
- 排除 active texture/lightmap unit 假设；
- 普通 Cosmic 与 SDF 路径均补充真实 GL 强制设置和双层恢复；
- `build`、测试和 `packageVariants` 构建通过；
- 用户实机反馈修改后现象已消失。

### 建议的最终回归

在合并前建议再覆盖以下组合：

1. SDF 开启，连续授予多项包含 ResourceLocation/ItemStack/focus 图标的研究；
2. SDF 关闭，重复同一测试；
3. HDR 开启和关闭；
4. 小窗口与大窗口；
5. 普通 tooltip、聊天、HUD 和研究名称，确认状态恢复没有改变后续绘制；
6. 保留一次修复后的诊断日志，确认所有 `cosmic.draw.prepared` 均为 `blend:true`。

## 诊断代码的生命周期

`FontRenderDiagnostics` 是临时、默认关闭的运行时采样器。启用方式为：

```toml
[debug]
renderStats = true
```

采样会在客户端渲染线程执行大量 `glGet*` 并输出高频 INFO 日志，不适合正常游戏。
完成最后一次回归后应设置：

```toml
renderStats = false
```

合并前可以选择：

- 保留诊断器，但继续由 `debug.renderStats` 严格门控；或
- 删除 TC6 专用匹配和 `before/prepared` 高频日志，仅保留这份分析报告。

## 后续架构建议

仓库中仍有其他字体状态作用域主要依赖 `GlStateManager`，例如 AWT 和通用
`FontRenderPipeline`。本次日志只证明 Cosmic SDF/RGBA 路径存在并触发了问题，不能直接
断言其他路径已经出错；但它们具有相同的潜在风险。

后续可以抽取一个小型的 GL 状态作用域工具，明确维护两层状态：

```text
Minecraft cached state
OpenGL driver state
```

该工具至少应统一处理：

- blend enable；
- separate blend factors；
- alpha test；
- texture enable/binding；
- fog；
- shader program；
- 必要时的 active texture unit。

在 1.12.2 legacy 模组生态中，不能假设所有调用方都只使用 `GlStateManager`。对抗锯齿
字体、SDF 和 HDR 纹理而言，混合状态是每个文字 quad 的必要前置条件，而不是调用方可以
选择提供的环境条件。

