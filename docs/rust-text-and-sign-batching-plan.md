# Rust 文字模式与跨告示牌合批计划

## 目标

在保留现有 AWT/Skia 后端的前提下增加一个可独立关闭的 Rust 文字后端，并评估跨
TileEntity 告示牌合批。两个方向必须分开测量：Rust 后端主要优化 shaping、fallback、
栅格化和 atlas 管理；跨告示牌合批主要优化 Minecraft 1.12.2 TESR 与 OpenGL 提交次数。

Rust 并不天然比 Skia 快。当前统计已经表明 Paragraph/测量缓存命中率较高，而大量告示牌
场景仍受逐告示牌矩阵、纹理绑定和 draw call 限制。因此不得用单纯替换语言或字体库代替
端到端基准测试。

## Rust 后端候选架构

第一版不让 Rust 持有 Minecraft 的 OpenGL 上下文。Rust 只负责文字布局、字形缓存和 atlas
更新，Java/LWJGL2 继续负责纹理上传和绘制，以降低与 Cleanroom、Celeritas、OptiFine、
shader 和不同 OpenGL 上下文实现冲突的风险。

建议依赖：

- `cosmic-text`：文本 shaping、BiDi、字体 fallback 与布局。
- `swash`：缺失 glyph 的灰度/彩色字形栅格化；优先复用 `cosmic-text` 对应接口。
- `etagere`：atlas 矩形分配。
- `bytemuck`：稳定的实例/更新缓冲区二进制布局。
- JNI：Minecraft 1.12.2/Java 8 兼容边界，不依赖 Panama。

不在第一版引入 Rust OpenGL。只有 Java 提交路径被证明仍是决定性瓶颈，并且共享上下文
生命周期能被可靠验证后，才考虑 Rust 直接提交。

### 2026-07-19 cosmic-text 主分支实测

研究基于 `pop-os/cosmic-text` 提交
`899d74d39b1b2b0f8b9eac544f415388a61328e4`：

- crate 版本 `0.19.0`，`MIT OR Apache-2.0`。
- 最低 Rust 版本 `1.89`；本机 `rustc 1.91.1` 可构建。
- 当前 shaping 是 `HarfRust`，不是早期方案中提到的 `rustybuzz`。
- 栅格化仍由可选 `swash` 提供，支持 mask、彩色 glyph 和 outline。
- Windows/Java 显式传入字体数据时，建议关闭默认 feature，使用：

```toml
cosmic-text = { version = "0.19", default-features = false, features = ["std", "swash"] }
```

这样不引入 `fontconfig`、`vi`、`syntect` 和编辑器功能。`FontSystem` 的 fontdb 应由 JNI
接口加载 Java 选定的字体字节，不能依赖不同系统的字体扫描结果，否则三种后端的 fallback
顺序无法保持一致。

当前最小依赖树仍包含两套 `skrifa`：cosmic-text 使用 `0.40`，swash 使用 `0.44`。这不是
架构阻塞，但会增加一部分 native 体积；升级依赖收敛后可重新测量。

实际创建了 Windows `cdylib`，强制链接 `FontSystem + Buffer + Advanced shaping +
SwashCache + Buffer.draw` 完整路径，并使用：

```toml
[profile.release]
opt-level = "z"
lto = "fat"
codegen-units = 1
panic = "abort"
strip = "symbols"
```

结果为 `1,562,624` 字节。该数字尚未包含 JNI crate、atlas allocator 和项目胶水，合理目标
应按每平台约 2-3 MiB 估算，而不是把 1.49 MiB 当成最终承诺。

当前 mod 的两份内置字体分别约为：

```text
sarasa_ui_sc_regular.ttf      24,049,996 bytes
noto_color_emoji_regular.ttf 25,111,640 bytes
```

因此“减小体积”不能只替换 Skija。建议产物拆分为：

```text
neofontrender-core.jar        Java + 当前平台 cosmic-text native
neofontrender-fontpack.jar    可选 Sarasa + Noto Color Emoji
```

或首次运行优先选择系统字体，仅在用户安装可选字体包时提供完全一致的跨平台 fallback。
若仍把约 49 MiB 字体嵌入主 JAR，Rust 后端只能削减 Skija/ICU/多平台 native 部分，无法实现
小型主包。

API 结论：

- `Buffer::set_text` 与 `set_rich_text` 可以表达 Minecraft styled spans。
- `shape_until_scroll` 负责 shaping/layout，`layout_runs()` 暴露最终 glyph 位置。
- `SwashCache::get_image` 可取得 mask/color glyph 位图；生产实现不应使用逐像素 `draw`
  callback 穿过 JNI，而应在 Rust 内直接写 atlas update buffer。
- `Buffer::draw` 是方便的 CPU callback API，不适合作为 Java 边界。
- 第一版应返回 glyph instances 和合并后的 atlas dirty rectangles，Java 一次读取 DirectBuffer。

## JNI 边界

JNI 必须按一整段文本或一整个告示牌传输，禁止逐字符、逐 glyph 调用。

建议接口：

```java
native long createEngine(byte[][] fontData, EngineOptions options);
native long shapeText(long engine, String text, StyleRun[] styles,
                      float fontSize, float maxWidth, int flags);
native ByteBuffer glyphInstances(long layout);
native AtlasUpdate[] takeAtlasUpdates(long engine);
native float layoutAdvance(long layout);
native void releaseLayout(long layout);
native void destroyEngine(long engine);
```

`GlyphInstance` 至少包含 atlas 页、glyph rectangle、UV、x/y、advance、颜色和装饰标志。
`AtlasUpdate` 按页合并脏矩形，Java 使用少量 `glTexSubImage2D` 上传。所有 native handle 必须
有显式释放和 Cleaner/终止回退，错误通过状态码与受控异常返回，不能让 Rust panic 穿过 JNI。

## Minecraft 语义边界

第一阶段继续在 Java 解析 Minecraft `§` 格式、`ITextComponent` 和点击区域，向 Rust 传递
styled spans。测量、截断、换行和最终绘制必须由同一次 Rust layout 产生，避免 Java 宽度与
Rust glyph advance 不一致。

必须覆盖：

- UTF-16 到 Unicode scalar 的正确转换，包括代理对。
- BiDi、组合字符、emoji、CJK 与字体 fallback。
- 粗体、斜体、下划线、删除线、阴影和 Minecraft 颜色重置。
- `trimStringToWidth`、告示牌 90px 限宽、编辑行标记和 GUI 点击区域。
- 字体资源包/本地字体加载失败时回退现有后端。

fallback 顺序必须可配置，不能把固定 Noto Sans CJK 行为写死在 native 层。

## 分阶段实施

### 阶段 0：基准与判定门槛

- 固定三个场景：GUI 文本、普通世界文本、625 块告示牌墙。
- 记录 FPS/frame time、CPU sampling、文字布局耗时、栅格耗时、JNI 耗时、纹理上传次数、
  draw call、缓存命中率与显存占用。
- 增加禁用文字但保留告示牌模型、禁用模型但保留文字的对照项，分离真正瓶颈。

若 Skia shaping/栅格在热缓存下不足总帧时间的 10%，Rust 原型不得宣称解决告示牌性能。

### 阶段 1：Rust 最小原型

- 新增独立 Cargo crate 和 Windows x86_64 DLL 构建。
- 只支持单字体、单色、单行文本，完成 create/shape/raster/release。
- Java 上传单页灰度 atlas，并用一次 BufferBuilder 提交整段 glyph。
- 先做离线基准，不接管默认 FontRenderer。

### 阶段 2：可选运行模式

- 增加 `rendering.engine = "rust"`，默认仍保持当前稳定后端。
- native 缺失、ABI 不匹配或初始化失败时记录原因并回退 Skia/AWT。
- 增加启动期能力检查、版本号和调试 overlay。
- 配置关闭时不加载 Rust renderer Mixin；native DLL 可以不加载。

### 阶段 3：格式与 fallback 完整性

- 支持 styled spans、fallback、BiDi、emoji 和文字装饰。
- 用同一 layout 实现测量与绘制，建立与原版/Skia 的截图和宽度差异测试。
- atlas 分页、LRU、脏矩形合并和资源释放必须有统计。

### 阶段 4：告示牌专用路径

- 一次 JNI 调用布局四行告示牌文字。
- glyph instance 直接写入共享 atlas，不再为每块告示牌创建独立文字纹理。
- 相同文本复用 layout/instance 模板，仅为不同世界矩阵生成实例数据。
- 保留现有 LOD、视锥剔除、近距离高清桶和兼容性回退。

## 跨告示牌合批计划

该功能使用独立启动期开关：

```toml
[performance]
signCrossTileBatching = false
```

初期默认关闭。关闭并重启后不得应用对应的 TileEntityRendererDispatcher flush Mixin。

### 1. 原版调用链与实际注入边界

Minecraft/Forge 1.12.2 当前调用链为：

```text
RenderGlobal.renderEntities(...)
  -> TileEntityRendererDispatcher.preDrawBatch()
  -> 遍历 renderChunk.getCompiledChunk().getTileEntities()
     -> shouldRenderInPass(pass)
     -> camera.isBoundingBoxInFrustum(te.getRenderBoundingBox())
     -> TileEntityRendererDispatcher.render(te, partialTicks, -1)
  -> 遍历 setTileEntities，执行相同 pass/视锥判断
  -> TileEntityRendererDispatcher.drawBatch(pass)
  -> 单独绘制 damagedBlocks 中的 TileEntity
```

这意味着：

- 原版已经在 TESR 调用前做 TileEntity AABB 视锥剔除。当前告示牌 Mixin 中再次创建
  `Frustum` 的逻辑是冗余保护，不应成为新合批器的依赖。
- `preDrawBatch()` 到 `drawBatch(pass)` 是 Forge 明确提供的单帧、单 pass 生命周期边界。
- 破坏阶段告示牌在 `drawBatch(pass)` 之后单独绘制，天然可以继续走原版路径。
- 不需要 Mixin `RenderGlobal`，可减少与 Celeritas/OptiFine 重写世界渲染循环的冲突。

建议新增 `MixinTileEntityRendererDispatcher`：

```java
@Inject(method = "preDrawBatch", at = @At("TAIL"))
private void nfr$beginSignBatch(CallbackInfo ci) {
    SignBatchRenderer.begin(MinecraftForgeClient.getRenderPass());
}

@Inject(method = "drawBatch", at = @At("HEAD"))
private void nfr$flushSignBatch(int pass, CallbackInfo ci) {
    SignBatchRenderer.flush(pass);
}
```

在 `HEAD` flush 是为了避免 Forge `drawBatch` 随后绑定 block atlas、修改 blend/cull/shade
状态污染告示牌材质。`flush` 必须完整恢复它修改的状态，使原版 FastTESR batch 能继续绘制。

### 2. 为什么必须收集完整告示牌而不是只延迟模型

不能让文字立即绘制、模型延迟到 pass 末尾。原版文字使用 `depthMask(false)`，如果先画文字
再画板面，后画的板面仍会写颜色并覆盖文字。正确顺序必须是：

```text
所有告示牌模型批次
  -> 所有告示牌文字批次
  -> Forge FastTESR drawBatch
```

因此 `MixinTileEntitySignRenderer` 在 batch active 且符合条件时应在 `render` 的 `HEAD` 收集
完整实例并 `ci.cancel()`。已有的逐行 `@Redirect`、四行 Paragraph 合并和模型 LOD 只用于
非 batch/fallback 路径；被收集的实例不能再进入这些 immediate hooks。

以下情况禁止收集，直接调用原版：

- `destroyStage >= 0`。
- 当前不是 Forge render pass 0。
- batch 尚未 begin、已经 flush 或上下文 token 与本帧不一致。
- TileEntity/renderer 不是原版 `TileEntitySign`/`TileEntitySignRenderer` 的预期实现。
- 资源/字体后端初始化中、上下文丢失或上一次 flush 异常。
- 配置关闭，或兼容性探测明确标记当前渲染器不支持。

### 3. 建议新增的代码结构

```text
neofontrender/client/render/sign/
  SignBatchRenderer.java       生命周期、分组、flush、状态恢复、统计
  SignBatchEntry.java          一块告示牌的不可变快照
  SignModelBatch.java          CPU 顶点生成和模型 VBO/Tessellator 提交
  SignTextBatch.java           Paragraph atlas 与文字四边形提交
  SignBatchStats.java          收集/回退/批次/耗时/atlas 统计
  SignTransform.java           metadata 到板面/文字矩阵的纯函数

neofontrender/mixin/
  MixinTileEntityRendererDispatcher.java
  MixinTileEntitySignRenderer.java
  NeoFontRenderMixinPlugin.java
```

`SignBatchEntry` 不保存可变的 `TileEntitySign` 引用作为最终数据源。收集时立即快照：

```text
BlockPos
camera-relative x/y/z
standing 或 wall
metadata/facing
combined light
四行 formatted string
lineBeingEdited
字体/配置 generation
资源 generation
```

世界卸载或 TileEntity 在 flush 前变化时，快照仍然自洽；下一帧自然读取新状态。

### 4. BatchContext 生命周期

使用渲染线程拥有的 `BatchContext`，不使用全局跨帧列表：

```text
IDLE -> COLLECTING -> FLUSHING -> IDLE
```

`begin(pass)`：

- 若上次状态不是 `IDLE`，丢弃残留实例并记录一次 recovery。
- 记录当前线程、world identity、frame counter、pass、字体 generation 和资源 generation。
- 仅 pass 0 进入 `COLLECTING`；其他 pass 保持 bypass。

`collect(entry)`：

- 校验线程、world、pass 和 generation。
- 超过可配置最大实例数时停止收集，后续告示牌走原版；不能无限扩容。
- 不执行 GL 调用，不改变 Minecraft 状态。

`flush(pass)`：

- 原子切换到 `FLUSHING`，先取走当前列表，防止重入追加。
- world/pass/generation 不一致时全部放弃并记录原因。
- `try/finally` 中依次提交模型和文字；finally 清空并回到 `IDLE`。
- flush 异常只禁用后续 batch，不尝试在 pass 末尾 replay immediate renderer，因为原始遍历位置
  已经过去，replay 容易再次污染矩阵。调试构建可 fail-fast，发布构建下一帧回退。

不要用 `System.currentTimeMillis` 判断帧。优先使用 `Minecraft.getSystemTime` 加内部递增 token，
并以 `preDrawBatch`/`drawBatch` 配对作为权威边界。

### 5. 模型批次的精确实现

初版只接管原版 `textures/entity/sign.png`。资源包替换同一路径时仍能工作，因为 flush 绑定的是
当前 `TextureManager` 中该 `ResourceLocation`，不缓存像素副本。

顶点不能通过每块告示牌 `pushMatrix/rotate/callList` 生成，那仍保留大量 GL 调用。应在 CPU
把 LOD 模型的局部顶点转换为 camera-relative 世界顶点，并连续写入一个 BufferBuilder：

```text
local ModelSign vertex
  -> 站立/墙面 metadata rotation
  -> 原版 standing/wall translation
  -> TileEntity camera-relative position
  -> batch vertex
```

建议格式包含：

```text
POSITION + TEX + COLOR + LIGHTMAP + NORMAL
```

每块告示牌从 world 读取一次 `getCombinedLight(pos, 0)`，将 sky/block light 写入各顶点，避免
每实例调用 `OpenGlHelper.setLightmapTextureCoords`。板面至少保留前后面；近距离不进入 batch，
所以远距离侧面可以省略。站立告示牌增加杆的前后面，墙面告示牌不生成杆。

分组键第一版为：

```text
texture ResourceLocation + render pass + destroy stage + vertex format + lighting mode
```

朝向已经烘进 CPU 顶点，不必拆成四个 draw call。若以后改为 GPU instancing，朝向才进入
instance 数据或 pipeline key。原版只有一个 sign texture，正常情况下模型为一次绑定和一次
draw。资源包 reload 时只重新解析纹理，不需要重建静态 UV；若检测到非 64x32 纹理，UV
仍是归一化值，保持原版区域语义。

### 6. 模型 batch 的状态协议

提交前显式设置：

```text
modelview = 当前世界 camera 矩阵（顶点已经 camera-relative，不叠加每实例矩阵）
texture2D = enabled
sign texture = bound
lightmap = enabled
depth test = enabled
depth mask = true
blend = 与原版 sign model 一致
color = 1,1,1,1
rescale normal = enabled 或使用正确顶点 normal
```

必须通过 `GlStateManager` 恢复缓存可见状态，不能只用裸 `GL11` 恢复 blend/depth/texture，
否则会再次出现 Minecraft 状态缓存与驱动不一致。矩阵使用严格配对的 push/pop；纹理绑定
结束后可不猜测旧 texture id，而是在进入 flush 时读取并在 finally 通过 TextureManager/GL
恢复。需要单独验证 active texture unit 与 lightmap unit。

### 7. 第一版文字处理：先保证顺序，不宣称完全合批

模型 batch 落地后，文字阶段可先逐条执行现有四行 `renderSign`：

- 对每个 entry 应用与原版完全一致的文字 transform。
- 调用 `FontRenderTuning.updateFromCurrentGlState(false)`。
- 复用现有四行 Paragraph cache、LOD 和近距离高清策略。
- 模型已经先写入深度；文字使用 `depthMask(false)`，最后统一恢复 true。

这一阶段仍有每块告示牌的矩阵、纹理绑定和 draw call，但已经验证“收集完整 sign、模型先画、
文字后画”的生命周期正确性。必须单独报告 `model batches=1` 和 `text draws=N`，避免把模型
合批描述成完整告示牌合批。

### 8. 第二版文字处理：告示牌 Paragraph atlas

在 Skia 模式中，最快的渐进方案不是立即重写 glyph atlas，而是把每块告示牌的四行 Paragraph
结果放进共享的 sign atlas：

```text
SignParagraphKey = 四行 formatted text
                 + font/style/fallback generation
                 + raster scale bucket
                 + color/shadow flags
                 + texture path bucket
```

每个 atlas entry 保存：

```text
page id
pixel rectangle
normalized UV
logical width/height
horizontal/vertical offset
last used frame
generation
```

实现约束：

- 按 raster scale bucket 分页，避免近距离 32x 与远距离低分辨率条目互相浪费空间。
- 远距离页建议 2048x2048；近距离条目可能超过单页尺寸，需要独占页或回退独立纹理。
- 使用固定 90px 内容宽度加透明 border，Paragraph layout 宽度仍必须只用 90px，不能再次把
  border 算进居中区域。
- GPU shared 模式下为每页保留 Skia `Surface`/backend texture；缺失条目使用 scissor 清空目标
  rect 后直接 paint，Java 只绑定共享 GL texture，不做 GPU->CPU->GPU 拷贝。
- CPU fallback 使用合并后的脏矩形 `glTexSubImage2D`，禁止每 glyph 上传。
- atlas 分配失败、entry 超大或上下文不共享时回退当前独立 `RenderedText`。

文字顶点按 atlas page 分组。每块告示牌将一个四边形的四个局部角通过 `SignTransform` 转成
camera-relative 世界坐标，写入页面对应 BufferBuilder。最终每个可见 atlas page 一次纹理
绑定和一次 draw。625 块唯一文本若位于 2 个 atlas 页，应从约 625 次文字 draw 降到约 2 次。

### 9. 第三版文字处理：Rust glyph atlas（可选）

Rust 后端不需要 Paragraph bitmap atlas。`cosmic-text/swash` 返回共享 glyph atlas UV 和 glyph
instances，Java 在收集时将 glyph 局部坐标乘告示牌 transform，按 glyph atlas page 直接合并。
同一 glyph 在数百块告示牌间天然复用，显存通常优于缓存 625 张 Paragraph bitmap。

两种 atlas 后端应实现相同的 Java 接口：

```java
interface SignTextBatchSource {
    PreparedSignText prepare(SignTextRequest request);
    void append(PreparedSignText text, SignTransform transform, SignTextBuffers out);
    void flushUploads();
}
```

Skia 实现使用一个 Paragraph quad，Rust 实现使用多个 glyph quad。`SignBatchRenderer` 不依赖
具体字体引擎。

### 10. 配置与 Mixin 加载策略

新增配置：

```toml
[performance]
signCrossTileBatching = false
signCrossTileBatchModels = true
signCrossTileBatchText = true
signBatchMaxEntries = 4096
signBatchAtlasPageSize = 2048
signBatchAtlasMaxPages = 8
```

`signCrossTileBatching=false` 时，启动插件不应用
`MixinTileEntityRendererDispatcher` 中对应 flush hook。`MixinTileEntitySignRenderer` 可能仍因
单 sign LOD/四行合并而加载，但其 cross-tile 分支必须保持关闭。UI 明确标记总开关需要重启；
子开关可以运行时生效。

启动插件必须按精确 key 解析，不能使用会让 `signModelLodDistance` 误匹配
`signModelLod` 的宽泛 `startsWith`。建议把早期 TOML 读取抽成只接受 `key = bool` 的小型解析器，
并给缺失/损坏配置保守默认值。

### 11. 统计与验证场景

调试 overlay/日志新增：

```text
sign visible / collected / fallback / culled
model vertices / model batches / model flush ms
text entries / text cache hits / atlas pages / atlas uploads
text draws / text flush ms
recovery count + last fallback reason
```

必须分别测试：

1. 625 块墙面告示牌，唯一文本。
2. 625 块墙面告示牌，大部分文本相同。
3. 站立告示牌，16 个 metadata 朝向。
4. 墙面告示牌四朝向、正反面和视锥边缘。
5. 正在编辑的告示牌与 `> text <` 标记。
6. 破坏动画和资源包 reload。
7. Celeritas 开/关、shader/OptiFine 可用组合。
8. 世界切换、维度切换、F3+T、窗口缩放和 GL context 重建。
9. batching 开关关闭并重启后的原版调用图。

每个阶段都记录模型关闭/文字关闭的 A/B 数据。模型 batch 若只提升 1-2 FPS，但 text atlas
把 draw call 从数百降到个位数，报告必须如实区分收益来源。

### 12. 实施提交顺序

建议拆成可独立回滚的提交：

1. `refactor: extract deterministic sign transforms`：纯函数和原版位置截图测试。
2. `feat: add optional sign batch lifecycle`：dispatcher begin/flush、只统计不取消渲染。
3. `feat: batch distant sign models`：完整收集、模型批次、文字仍逐条延迟。
4. `feat: add sign batch diagnostics`：overlay、回退原因和基准脚本。
5. `feat: add skia sign paragraph atlas`：共享页、上传、按页文字批次。
6. `feat: expose sign batching settings`：UI、启动期 Mixin gate 和兼容性说明。
7. 后续独立提交 Rust glyph atlas，不与 Skia atlas 首次落地混在一起。

阶段 2 只统计、不改变画面的版本很重要：它先验证 Celeritas 环境下
`preDrawBatch/drawBatch` 是否每帧配对，再允许阶段 3 取消原版告示牌渲染。

## 验收标准

Rust 模式：

- 普通文本与 Skia 的宽度误差不超过 0.25 逻辑像素，换行点一致。
- GUI、告示牌、格式颜色、CJK、emoji 和 BiDi 无明显视觉回归。
- native 初始化失败、DLL 缺失和设备重建均能回退，不导致 JVM 崩溃。
- 热缓存场景必须报告布局、栅格与 JNI 的独立耗时，确认收益来源。

跨告示牌合批：

- 625 块告示牌墙的模型批次数从数百次降至材质/朝向组数量级。
- 文字 atlas 完成后，文字 draw call 降至可见 atlas 页数量级。
- 不出现深度错误、破坏纹理错误、跨帧残影或资源包材质错误。
- 开关关闭并重启后恢复原始目标类调用图。

## 明确不做

- 不在原型阶段重写整个 Minecraft GUI/TextComponent 系统。
- 不在没有 profiler 数据时同时引入 Rust shaping、Rust OpenGL 和跨 TESR 合批。
- 不以平均 FPS 作为唯一指标；必须查看 1% low、frame time 和提交/上传计数。
- 不把 native 崩溃风险隐藏在自动回退描述中；越过 JNI 的内存错误无法可靠回退。

## 推荐下一步

先完成阶段 0 的可重复基准和阶段 1 的离线 Rust 原型。若 `cosmic-text + swash` 在布局与
栅格部分有明确优势，再接入可选 `rust` engine；同时独立实现告示牌模型批次收集实验，
避免把字体库收益与 draw-call 收益混在一起。
