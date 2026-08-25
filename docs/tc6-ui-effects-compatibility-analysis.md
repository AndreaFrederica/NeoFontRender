# Thaumcraft 6 与 UI Enhancements 兼容性分析

## 范围

本文记录 Minecraft 1.12.2、Thaumcraft 6.1.BETA26 与 NeoFontRender UI Enhancements
同时启用时的研究界面问题。当前方案采用仅在检测到 Thaumcraft 时加载的专用 Mixin；通用
framebuffer 状态修复实验保存在 `codex/tc6-generic-depth-pipeline-archive` 分支，暂不作为
当前实现的基础。

## 现象

UI 效果关闭时，TC6 研究界面的星空/研究背景、节点、外框和书本均正常。

开启高斯模糊或暗色遮罩中的任意一个后，中央研究背景纹理消失，露出经过 UIE 处理的世界
画面；TC6 的外框、节点、书本以及 HUD 仍可见。只开启高斯模糊时世界画面正常模糊，只
开启暗色遮罩时也会复现研究背景缺失。

## 渲染链路

UIE 在 `EntityRenderer.updateCameraAndRender` 的第一个 `setupOverlayRendering` 之前处理
世界 framebuffer。高斯模糊会在此处运行私有 ShaderGroup；暗色遮罩则在
`RenderGameOverlayEvent.Post(ALL)` 绘制。两种配置都会使 `GuiScreen.drawWorldBackground()`
被 `MixinGuiScreenBackground` 取消。

TC6 随后执行以下顺序：

1. 调用 vanilla 的默认背景入口；
2. `genResearchBackgroundFixedPre` 建立研究界面的固定管线状态；
3. `genResearchBackgroundZoomable` 绘制分类背景纹理；
4. 绘制节点、连接线和外框；
5. 通过 `UtilsFX.drawCustomTooltip` 绘制研究提示。

## 根因判断

“只开暗色遮罩也失败”排除了最终 blur framebuffer 合成是唯一根因。两种故障配置共同
经过的是 `drawWorldBackground` 的取消路径。TC6 的研究背景 pass 使用旧式固定管线，除
了可见的 vanilla 背景外，也可能依赖该方法留下的纹理、blend、alpha、颜色和深度状态。
直接在方法入口取消它，会让 TC6 的背景 quad 在不完整的状态下执行；之后的外框和节点
pass 会重新绑定纹理并修改部分状态，因此仍然能够显示。

此前尝试同步 depth test、depth function 和 depth mask 后现象没有改变，说明深度状态不是
充分解释。当前没有证据表明 TC6 资源损坏，也没有证据需要修改 TC6 的研究数据或纹理。

## 当前兼容策略

### 研究背景

`MixinThaumcraftResearchBrowserBackground` 仅在 Thaumcraft 类存在时加载，并在
`genResearchBackgroundFixedPre` 返回后关闭 TC6 的 legacy depth-equal 状态，使背景纹理
能够在 UIE 的 GUI 背景环境中绘制。该 Mixin 不会改变没有 Thaumcraft 的客户端行为。

### 研究 tooltip

TC6 的研究提示不经过 Forge 的标准 tooltip 事件，而是直接调用
`thaumcraft.client.lib.UtilsFX.drawCustomTooltip`。`MixinThaumcraftCustomTooltip` 在现代
tooltip 功能可用时拦截这两个重载，并转发到 `GuiUtils.drawHoveringText`，从而复用 UIE
现有的 `RenderTooltipEvent`、CJK 文本和现代面板渲染链路。现代 tooltip 被关闭或 Arc3D
不可用时，Mixin 不取消原调用，TC6 原始 tooltip 保持不变。

## 验证重点

- UIE 效果开启、TC6 存在：研究背景纹理恢复，节点和外框不改变。
- UIE 效果关闭、TC6 存在：专用 Mixin 不应改变 vanilla 研究界面。
- TC6 不存在：Thaumcraft 配置不排队，相关目标类不会被加载。
- 现代 tooltip 开启：研究提示进入 UIE 的现代 tooltip 事件路径。
- 现代 tooltip 关闭或 Arc3D 不可用：TC6 原始 `UtilsFX` tooltip 继续绘制。

## 未解决事项

当前实现仍需要在实际 Cleanroom + TC6 客户端中验证研究背景、缩放、搜索界面和 tooltip
边缘位置。若研究背景仍缺失，应优先记录 `genResearchBackgroundFixedPre` 返回后和
`genResearchBackgroundZoomable` 入口处的实际 GL 状态，而不是继续扩大 TC6 专用注入范围。
