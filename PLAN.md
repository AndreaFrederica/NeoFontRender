# SmoothFont Replacement — 移植计划

## 0. 架构决策

- **不复制 SmoothFont 反编译代码**（clean-room 原则）。
- **纯 Mixin**，`is_coremod = false`，`use_mixins = true`，MixinBooter 10.2。
- **不替换 `FontRenderer` 实例**（避免破坏其他 mod 的字段注入）。改为在 `FontRenderer` 内部通过 Mixin 把关键方法重定向到我们的新管线。
- **参考 1.20.1 的分层架构**，但底层用 1.12.2 可用的技术（AWT 替代 STB，`Tessellator` 替代 `VertexConsumer`）。

## 1. 目标架构（1.12.2 适配版）

```
sfr.mod.core.font
├── FontManager          ← 加载配置、管理 FontSet、生命周期
├── FontSet              ← 缓存 GlyphInfo / BakedGlyph，按 codepoint 查询
├── FontTexture          ← 动态纹理图集（ShelfPacker 或 Node 二叉树）
├── GlyphInfo            ← 接口：advance、shadowOffset、bake()
├── GlyphProvider        ← 接口：getGlyph(int)、close()
├── BakedGlyph           ← 持有 UV + 偏移，负责 render()
└── providers
    ├── AwtTtfGlyphProvider      ← AWT Font + GlyphVector 光栅化
    ├── BitmapGlyphProvider      ← 回退到原版位图字体（可选）
    └── MissingGlyphProvider     ←  tofu / 空白回退
```

**与 1.20.1 的对应关系**

| 1.20.1（官方） | 1.12.2 适配版 |
|---------------|--------------|
| `FontManager` | `sfr.mod.core.font.FontManager` |
| `FontSet` | `sfr.mod.core.font.FontSet` |
| `FontTexture` (Node 二叉树) | `sfr.mod.core.font.FontTexture` (ShelfPacker 简化版) |
| `GlyphInfo` | `sfr.mod.core.font.GlyphInfo` |
| `GlyphProvider` | `sfr.mod.core.font.GlyphProvider` |
| `BakedGlyph` | `sfr.mod.core.font.BakedGlyph` |
| `TrueTypeGlyphProvider` (STB) | `AwtTtfGlyphProvider` (AWT) |
| `Font` (原 FontRenderer) | Mixin 注入 `FontRenderer`，保留类本身 |

## 2. 技术约束与适配

| 1.20.1 特性 | 1.12.2 可用替代方案 |
|------------|-------------------|
| `VertexConsumer` + `MultiBufferSource` | 直接 `Tessellator.getInstance().getBuffer()` |
| `Matrix4f` (JOML) | `GlStateManager.translate/scale` |
| `NativeImage` + `TextureUtil.prepareImage` | `DynamicTexture` (`int[]` buffer + `glTexImage2D`) |
| `STBTruetype` | `java.awt.Font` + `GlyphVector` + `BufferedImage` |
| `CodepointMap` | 直接使用 `Int2ObjectMap` (fastutil) 或 `HashMap<Integer, T>` |
| `StringSplitter` | 保留原版 `FontRenderer` 的 `trimStringToWidth` / `sizeStringToWidth`，但把宽度查询替换为 `GlyphInfo.getAdvance()` |
| `RenderType` | 自定义 `GlStateManager` 状态管理（blend、texture、alpha test） |

## 3. 实施阶段

### Phase 1 — 基础设施（~2h）

1. **完成 CleanroomMC `setupDecompWorkspace`**
   - 运行 `./gradlew.bat setupDecompWorkspace --stacktrace`
   - 确认 `FontRenderer.java` MCP 源码解压到 `.gradle/caches/...`
   - 验证 `./gradlew.bat build` 能通过（空 mod 可编译）

2. **建立包结构**
   ```
   src/main/java/sfr/mod/
   ├── SmoothFontReplacement.java          # @Mod 主类
   ├── config/
   │   └── SfrConfig.java                  # 配置管理（字体路径、大小、oversample）
   ├── core/
   │   └── font/
   │       ├── FontManager.java
   │       ├── FontSet.java
   │       ├── FontTexture.java
   │       ├── GlyphInfo.java
   │       ├── GlyphProvider.java
   │       ├── BakedGlyph.java
   │       └── providers/
   │           ├── AwtTtfGlyphProvider.java
   │           └── MissingGlyphProvider.java
   └── mixin/
       ├── MixinFontRenderer.java
       ├── MixinGlStateManager.java
       └── MixinMinecraft.java
   ```

3. **完善 AT 文件**
   - 补充 `FontRenderer` 私有字段/方法暴露（已部分完成）。

### Phase 2 — 核心字体管线（~4h）

4. **定义核心接口**
   - `GlyphProvider`：`GlyphInfo getGlyph(int codePoint)`，`IntSet getSupportedGlyphs()`，`void close()`
   - `GlyphInfo`：`float getAdvance()`，`float getAdvance(boolean bold)`，`float getBoldOffset()`，`float getShadowOffset()`，`BakedGlyph bake()`
   - `BakedGlyph`：持有 `DynamicTexture texture`, `u0/u1/v0/v1`, `left/right/up/down`；`render(float x, float y, float r, float g, float b, float a, boolean italic, boolean bold)` 用 `Tessellator` 画 quad

5. **实现 `FontTexture`（动态图集）**
   - 参考 1.20.1 `FontTexture` 的 **Node 二叉树打包** 或 neofont 的 **ShelfPacker**。
   - 每页 256×256 或 512×512 `DynamicTexture`。
   - `add(SheetGlyphInfo)` → 尝试插入现有页 → 满则新建页 → 注册到 `TextureManager`。

6. **实现 `FontSet`**
   - `providers` 链按优先级查询 `GlyphInfo`。
   - `glyphs` (`Int2ObjectMap<BakedGlyph>`) 懒加载：首次访问时 `glyphInfo.bake()` → `FontTexture.add()`。
   - `glyphsByWidth` (`Int2ObjectMap<IntList>`) 用于 obfuscated text（随机同宽度字符）。

7. **实现 `FontManager`**
   - 持有 `TextureManager`。
   - `reload(IResourceManager)`：加载 `smoothfontreplacement/fonts.json`（可选）→ 创建 `AwtTtfGlyphProvider` → 构建 `FontSet`。
   - 单例或 registry 模式，在 `Minecraft` 启动后初始化。

### Phase 3 — TTF 光栅化（~3h）

8. **实现 `AwtTtfGlyphProvider`**
   - 加载 TTF：从资源包路径读取 `InputStream` → `Font.createFont(Font.TRUETYPE_FONT, stream)`。
   - `getGlyph(int codePoint)`：
     - `Font.canDisplay(codePoint)` 检查支持性。
     - 使用 `GlyphVector` 测量 advance、bounds。
     - 返回 `AwtGlyphInfo`。
   - `AwtGlyphInfo.rasterize()`：
     - 创建 `BufferedImage.TYPE_INT_ARGB`。
     - `Graphics2D` 设置 `RenderingHints.KEY_TEXT_ANTIALIASING` + `KEY_FRACTIONALMETRICS`。
     - `drawString` 渲染 → 提取 alpha 通道 → `NativeImage`-like int[] → 上传到 `FontTexture`。
   - 支持配置：`size`, `oversample`, `shiftX`, `shiftY`。

### Phase 4 — Mixin 桥接（~4h）

9. **`MixinFontRenderer`**
   - **渲染接管**：
     ```java
     @Inject(method = "renderStringAtPos", at = @At("HEAD"), cancellable = true)
     private void sfr$renderStringAtPos(String text, boolean shadow, CallbackInfo ci) {
         if (FontManager.INSTANCE.isActive()) {
             FontManager.INSTANCE.renderString((FontRenderer)(Object)this, text, posX, posY, shadow);
             ci.cancel();
         }
     }
     ```
   - **字符宽度**：
     ```java
     @Inject(method = "getStringWidth", at = @At("HEAD"), cancellable = true)
     private void sfr$getStringWidth(String text, CallbackInfoReturnable<Integer> cir) { ... }
     ```
   - **单字符渲染**（`renderDefaultChar` / `renderUnicodeChar`）也重定向，确保所有字符都走新管线。
   - **格式码处理**：在 `renderStringAtPos` 中解析 `§` 颜色/样式码，转换为 `BakedGlyph` 的 color/bold/italic/underline/strikethrough 参数。

10. **`MixinMinecraft`**
    - `@Inject(method = "init", at = @At("RETURN"))`
    - 初始化 `FontManager`：传入 `this.renderEngine`，读取配置，加载默认 TTF。
    - 注册 `IResourceManagerReloadListener` 以便资源包切换时重载字体。

11. **`MixinGlStateManager`**（可选）
    - 如果需要精确的 blend 状态控制，可以 hook `enableBlend` / `blendFunc`。

### Phase 5 — 效果与兼容性（~3h）

12. **文本效果**
    - **Shadow**：`FontRenderer` 中已有 `dropShadow` 逻辑，新管线中 render 两次（偏移 1px，dim 25%）。
    - **Bold**：`GlyphInfo.getAdvance(bold)` 返回 `advance + boldOffset`；渲染时 `BakedGlyph.render()` 画两次（x + boldOffset）。
    - **Italic**：在 `BakedGlyph.render()` 中对 quad 顶点做斜切（参考 1.20.1 `BakedGlyph.render` 的 `italic ? 1.0F - 0.25F * f2 : 0.0F`）。
    - **Underline / Strikethrough**：使用 `BakedGlyph.Effect` 模式（画一条细 quad）。

13. **配置系统**
    - 使用 Forge `@Config` 或自定义 JSON：字体文件路径、大小、oversample、抗锯齿开关、阴影强度。

14. **回退机制**
    - 如果 TTF 加载失败（文件缺失、AWT 不支持），自动回退到原版 `FontRenderer` 的位图渲染。

### Phase 6 — 测试与调优（~2h）

15. **单元测试**
    - `AwtTtfGlyphProvider` 对常用字符（ASCII、中文、日文、韩文）的 `getGlyph` 测试。
    - `FontTexture` 打包边界测试。

16. **游戏内测试**
    - 主菜单、聊天框、物品名称、JEI/NEI 工具提示、书与笔、命令方块界面。
    - 测试 Unicode 字体开关 (`Force Unicode Font`)。
    - 与其他常见 mod（JEI、OptiFine、JourneyMap）的兼容性。

## 4. 风险与对策

| 风险 | 对策 |
|------|------|
| AWT 在 headless 环境（某些服务器/CI）不可用 | 字体光栅化仅在 client 侧初始化；CI 不跑客户端测试即可。 |
| `FontRenderer` 被其他 coremod 修改 | 提高 Mixin 优先级；或改用 `ModifyVariable` + 条件注入，减少冲突。 |
| 性能：逐字 `Tessellator` draw 可能慢 | 批量提交：在 `renderStringAtPos` 中收集所有 quad，一次性 `draw()`。 |
| 内存：动态图集无限增长 | 限制最大页数（如 8 页 512×512 = 16MB）；超出时回收最少使用页面。 |
| 1.12.2 没有 `ResourceLocation` 字体选择 | 暂时只支持单字体（默认 TTF），后续可扩展 `IFont` 接口。 |

## 5. 里程碑

- [ ] M1: `setupDecompWorkspace` 成功，空 mod 可编译运行
- [ ] M2: `FontTexture` + `ShelfPacker` 可用，能手动 bake 单个字符并显示
- [ ] M3: `AwtTtfGlyphProvider` 能渲染完整 ASCII 字符串
- [ ] M4: `MixinFontRenderer` 接管 `renderStringAtPos`，游戏内文字正常显示
- [ ] M5: 支持 shadow、bold、italic、format codes
- [ ] M6: 配置系统、回退机制、兼容性测试通过
