# GL_BLEND 管理架构说明

## 背景

Minecraft 1.12.2 的 vanilla bitmap font 使用 1-bit alpha（完全透明或完全不透明），因此很多代码路径在绘制文字前后会随意开关 `GL_BLEND`（例如 `Gui.drawRect` 最后会 `disableBlend()`）。

我们的替换字体（Skia / SFR）使用 anti-aliased 多 bit alpha，边缘像素必须依赖 blend 才能正确与背景合成。如果 blend 被关闭，半透明边缘会直接把 raw RGB 写入 framebuffer，导致深色边缘和锯齿。

## 设计原则

**Per-quad 强制 blend**：每个文字 quad（`RenderedText.draw()` / `BakedGlyph.render()`）都通过 `FontRenderPipeline.begin()` 强制开启 `GL_BLEND`，绘制完成后在 `close()` 中恢复 caller 的状态。

这样做的原因是 Minecraft 的 GL 状态管理非常复杂，caller 可能在任何时候关闭 blend，字符串级统一开关无法覆盖所有边界情况（MC 内部会偷偷污染状态）。

## 架构

```
MixinFontRenderer.sfr$onDrawString
  └─ backend.renderFormatted()
        └─ RenderedText.draw()
              └─ FontRenderPipeline.begin()   ← quad 级：capture + 强制 enableBlend + 设置 func
                    └─ 绘制 quad
              └─ FontRenderPipeline.close()   ← 恢复之前的 blend 状态和 func

MixinFontRenderer.sfr$onRenderStringAtPos
  └─ sfr$renderSkiaFormatted() / sfr$renderRun()
        └─ RenderedText.draw() / BakedGlyph.render()
              └─ FontRenderPipeline.begin()
                    └─ 绘制 quad
              └─ FontRenderPipeline.close()
```

## 已清理的冗余代码

| 文件 | 清理原因 |
|---|---|
| `FontBlendGuard.java` | 上层的字符串级 blend guard，和 quad 级职责重叠 |
| `MixinGui.java` | 唯一作用是阻止 `Gui.drawRect` 的 `disableBlend()`，没有必要 |
| `MixinRenderItem.java` | `FontBlendGuard` 包裹 + 两个 `disableBlend()` redirect，quad 级已能处理 |
| `MixinGuiOverlayDebug.java` 中的 blend guard | Debug 文字走 `drawString` / `renderStringAtPos`，quad 级已覆盖 |

## `FontRenderPipeline.begin()` 行为

```java
public static State begin(float rasterScale) {
    if (!NeofontrenderConfig.isLoaded()) {
        return State.NOOP;
    }
    boolean enhanced = NeofontrenderConfig.enhancedTextPipeline();
    if (!enhanced && !NeofontrenderConfig.forceBlendForText()) {
        return State.NOOP;
    }

    State state = new State();
    state.capture();  // 查 4 次 GL 状态（blend + blend func + shader program）

    GlStateManager.enableTexture2D();
    GlStateManager.enableAlpha();
    GlStateManager.enableBlend();
    // 设置正确的 blend func（根据 premultiplied 配置）

    if (enhanced && shaderTextPipeline) {
        // 启用可选的 brightness-correction shader
    }

    return state;
}
```

`State.close()` 恢复 capture 时的 blend 状态和 shader program。

## 注意事项

- 每字符/每 run 会触发 4 次 GL 查询（`glIsEnabled` + 3 个 `glGetInteger`），这是性能开销的主要来源。如果未来能找到更可靠的方式在字符串级管理 blend，可以重新优化。
- `FontRenderPipeline` 对 **所有** 通过 `RenderedText.draw()` 或 `BakedGlyph.render()` 的绘制都生效，包括绕过 `MixinFontRenderer` 的直接调用。
