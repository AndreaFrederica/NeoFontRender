# Minecraft 1.12.2 字体渲染调用点分析

> 分析目标：找出所有使用 `GlStateManager.scale()` 产生字体大小效果的调用点，
> 评估用 `ModernTextApi` 原生大小渲染 API 替代的可行性。

---

## 1. 背景

原版 MC 1.12.2 的 `FontRenderer` API **没有字体大小参数**：

```java
// 唯一的绘制入口
public int drawString(String text, float x, float y, int color, boolean dropShadow)
public int drawStringWithShadow(String text, float x, float y, int color)

// 唯一的测量入口
public int getStringWidth(String text)
```

因此，MC 内部所有需要不同字号的地方，都通过 `GlStateManager.scale()` 实现缩放。

项目已有的 `ModernTextApi` 提供了原生大小渲染：

```java
// 指定 fontSize 绘制，无需 GL 缩放
ModernTextApi.drawFormatted(text, x, y, fontSize, argb, shadow)
ModernTextApi.measure(text, fontSize)
```

后端支持（`supportsNativeFontSize()` 返回 true）：
- `SkijaTextRenderer`
- `CosmicTextRenderer`
- `AwtModernTextRenderer`

---

## 2. 同时使用 scale + drawString 的 Vanilla 类（9 个）

### 2.1 可优化（3 处核心目标）

#### GuiIngame — 标题/副标题动画 ⭐⭐⭐

**位置**：`renderTitleAndSubtitle()` 方法

```java
// 标题渲染：4x 缩放
GlStateManager.pushMatrix();
GlStateManager.scale(4.0f, 4.0f, 4.0f);          // 8px * 4 = 32px
int titleWidth = fontRenderer.getStringWidth(displayedTitle);
fontRenderer.drawString(displayedTitle, -titleWidth / 2, -10, 0xFFFFFF);
GlStateManager.popMatrix();

// 副标题渲染：2x 缩放
GlStateManager.pushMatrix();
GlStateManager.scale(2.0f, 2.0f, 2.0f);          // 8px * 2 = 16px
int subtitleWidth = fontRenderer.getStringWidth(displayedSubTitle);
fontRenderer.drawString(displayedSubTitle, -subtitleWidth / 2, 5, 0xFFFFFF);
GlStateManager.popMatrix();
```

**优化方案**：
```java
// 替代标题渲染
float titleFontSize = 32.0f;  // 8 * 4
float titleW = ModernTextApi.measure(title, titleFontSize);
ModernTextApi.drawFormatted(title, -titleW / 2, -10, titleFontSize, 0xFFFFFFFF, true);

// 替代副标题渲染
float subtitleFontSize = 16.0f;  // 8 * 2
float subtitleW = ModernTextApi.measure(subtitle, subtitleFontSize);
ModernTextApi.drawFormatted(subtitle, -subtitleW / 2, 5, subtitleFontSize, 0xFFFFFFFF, true);
```

**收益**：
- 消除 2 次 pushMatrix/popMatrix + 2 次 scale 调用
- 标题/副标题是高频显示内容（每次进游戏、每次死亡都触发）
- 原生 size 渲染质量更高（无 GL 缩放的锯齿/模糊）

---

#### GuiMainMenu — Splash 文字 ⭐⭐

**位置**：`drawScreen()` 方法

```java
GlStateManager.pushMatrix();
// splash 文字动态缩放
float splashScale = ...; // 基于时间/宽度计算
GlStateManager.scale(splashScale, splashScale, splashScale);
drawCenteredString(fontRenderer, splashText, width / 2, 15, 0xFFFF00);
GlStateManager.popMatrix();
```

**优化方案**：
```java
float splashFontSize = 8.0f * splashScale;
float splashW = ModernTextApi.measure(splashText, splashFontSize);
ModernTextApi.drawFormatted(splashText, (width - splashW) / 2, 15, splashFontSize, 0xFFFFFF00, false);
```

**收益**：
- Splash 文字有旋转+缩放动画，每帧都调用
- 消除每帧的矩阵操作开销

---

#### GuiGameOver — 死亡标题 ⭐⭐

**位置**：`drawScreen()` 方法

```java
GlStateManager.pushMatrix();
GlStateManager.scale(2.0f, 2.0f, 2.0f);          // 8px * 2 = 16px
drawCenteredString(fontRenderer, deathTitle, width / 2, 30, 0xFFFFFF);
GlStateManager.popMatrix();
```

**优化方案**：
```java
float deathFontSize = 16.0f;  // 8 * 2
float deathW = ModernTextApi.measure(deathTitle, deathFontSize);
ModernTextApi.drawFormatted(deathTitle, (width - deathW) / 2, 30, deathFontSize, 0xFFFFFFFF, true);
```

**收益**：
- 每次死亡都触发
- 标题文字通常较短，measure 开销极低

---

### 2.2 不可优化（6 处）

| 类名 | scale 用途 | 为什么不能替换 |
|---|---|---|
| **GuiNewChat** | `scale(chatScale, chatScale, 1)` | 用户配置的聊天缩放，消息量大，measure 成本高 |
| **GuiSubtitleOverlay** | `scale(1,1,1)` + translate | 仅用于定位，无实际字体缩放 |
| **GuiEnchantment** | `scale(5,5,5)` | 附魔书 3D 模型缩放，不是字体缩放 |
| **GuiEditSign** | `scale(-93.75,-93.75,-93.75)` | 告示牌 3D 模型缩放 |
| **GuiInventory** | `scale(x,y,z)` | 玩家模型预览缩放 |
| **EntityRenderer** | 无直接 scale+font 组合 | - |

### 2.3 项目自身代码中的 scale + drawString

| 文件 | 行号 | scale 值 | 说明 |
|---|---|---|---|
| `SignBatchRenderer.java` | 166 | `scale(0.0104, -0.0104, 0.0104)` | 告示牌模型坐标转换 |
| `NeofontrenderCommand.java` | 439 | `scale(textScale, textScale, 1)` | 调试命令测试缩放 |

---

## 3. 仅使用 drawString 系列方法的 Vanilla 类（~20+ 个）

这些调用点**已被 MixinFontRenderer 拦截**，无需额外处理。

| 类名 | 调用方法 | 场景 |
|---|---|---|
| **GuiScreen** | drawString, getStringWidth | GUI 基类 |
| **GuiChat** | drawStringWithShadow, getStringWidth | 聊天输入框 |
| **GuiTextField** | drawStringWithShadow, getStringWidth | 所有文本输入框 |
| **GuiSlot** | drawStringWithShadow | 列表选择 |
| **GuiButton** | drawString | 所有按钮 |
| **GuiContainer** | drawString, getStringWidth | 容器 GUI |
| **GuiRecipeBook** | drawStringWithShadow | 合成配方书 |
| **GuiOverlayDebug** | drawStringWithShadow, getStringWidth | F3 调试屏 |
| **GuiPlayerTabOverlay** | drawStringWithShadow, getStringWidth | Tab 列表 |
| **GuiWinGame** | drawStringWithShadow | 结局 credits |
| **GuiScreenBook** | drawStringWithShadow | 书本界面 |
| **GuiOptions** | drawStringWithShadow | 设置界面 |
| **RenderItem** | drawStringWithShadow, getStringWidth | 物品悬浮提示 |
| **TileEntitySignRenderer** | drawString, getStringWidth | 告示牌渲染 |
| **GuiMerchant** | drawStringWithShadow | 村民交易 |
| **GuiBeacon** | drawStringWithShadow | 信标界面 |
| **GuiShulkerBox** | drawString | 潜影盒 |
| **GuiSpectator** | drawStringWithShadow | 旁观者菜单 |
| **GuiToast** | drawStringWithShadow | 成就/进度提示 |
| **GuiEnchantment** | drawStringWithShadow, drawSplitString | 附魔界面文字 |
| **GuiGameOver** | drawCenteredString | 死亡界面（除标题外） |
| **GuiMainMenu** | drawString, drawCenteredString | 主菜单（除 splash 外） |

**Mixin 拦截链**：
```
FontRenderer.drawString(text, x, y, color, dropShadow)
  → MixinFontRenderer.sfr$onDrawString()        [HEAD, cancellable]
    → backend.renderFormatted(text, color, false)  [text backend]
    → sfr$renderRun()                              [SFR/AWT backend]
```

---

## 4. 建议的优化优先级

| 优先级 | 目标 | 调用频率 | 实现复杂度 | 收益 |
|---|---|---|---|---|
| **P0** | GuiIngame 标题/副标题 | 每次进游戏/死亡 | 低 | 高 — 消除矩阵操作，提升渲染质量 |
| **P1** | GuiMainMenu splash | 每次启动/每帧 | 中 | 中 — 每帧调用，有动画效果 |
| **P2** | GuiGameOver 死亡标题 | 每次死亡 | 低 | 中 — 触发频率低但实现简单 |
| - | 其余 drawString 调用 | - | - | 已通过 Mixin 优化，无需额外处理 |

---

## 5. 实现注意事项

1. **坐标转换**：`scale` 缩放后，drawString 的坐标是在缩放空间中的。替换为原生 API 时需要将坐标转换回屏幕空间。

2. **measure 开销**：`ModernTextApi.measure()` 需要 text shaping，对于短文本（标题类）开销可忽略，对于长文本（聊天类）需谨慎。

3. **fallback**：当 `supportsNativeFontSize()` 返回 false 时，应保留原有 scale 路径作为 fallback。

4. **shadow 渲染**：原版 `drawStringWithShadow` 的阴影是通过偏移 1px 绘制两次实现的。`ModernTextApi` 的 shadow 参数是原生支持的，质量更好。

5. **§ 格式代码**：原版 drawString 支持 `§c` 等格式代码。`ModernTextApi.drawFormatted()` 同样支持，无需额外处理。

---

## 6. 结论

Vanilla MC 1.12.2 中**真正用 `GlStateManager.scale()` 产生字体大小效果的地方只有 3-4 处**，全部集中在 HUD/overlay 层。其余 scale 调用要么是 3D 模型缩放，要么是用户配置的聊天缩放。

项目已有的 `ModernTextApi` 原生大小渲染 API 可以直接替换这 3-4 处调用，实现：
- 消除不必要的矩阵 push/pop/scale 操作
- 获得原生字号的渲染质量（无 GL 缩放锯齿）
- 简化代码逻辑（不需要手动计算缩放后坐标）
