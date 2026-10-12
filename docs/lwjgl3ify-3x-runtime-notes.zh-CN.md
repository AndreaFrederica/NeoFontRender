# lwjgl3ify 3.x 运行时适配笔记（1.7.10）

本文记录 1.7.10 分支在 lwjgl3ify 3.x 运行时下的两类平台适配：光标系统的 SDL 移植，以及 FML 事件订阅类的可见性要求。两者的症状都是 dev 启动崩溃，根因均在 lwjgl3ify/RFB 的类加载与字节码改写机制。

## 背景：lwjgl3ify 3.x 的运行时结构

- **真实 LWJGL3 保留原包名**：`org.lwjgl.*`（core/opengl/sdl 等）以普通 jar 形式在系统 classpath（Prism 实例库 / dev 的 java.class.path）上。
- **`org.lwjglx.*` 是 LWJGL2 兼容 shim**：lwjgl3ify 自带 `org.lwjglx.{input,openal,opengl,util}`，把 1.7.10 的 LWJGL2 API 调用转发到 LWJGL3。**注意 `org.lwjglx` 下没有 glfw、stb、sdl 等真实 LWJGL3 绑定。**
- **`org.lwjgl.*` 里另有少量 LWJGL2 转发类**（如 `org.lwjgl.input.Mouse` → `org.lwjglx.input.Mouse`），保证未经改写的直接引用也能工作。
- **3.x 的窗口栈是 SDL，不是 GLFW**：`org.lwjglx.opengl.Display` 内部全部调 `org.lwjgl.sdl.SDLVideo.*`（SDL3，LWJGL ≥3.4 绑定）。**运行时不存在 `org.lwjgl.glfw.*`**；lwjgl3ify 2.x 才是 GLFW。

## 机制一：LwjglRedirectTransformer 的包名改写

lwjgl3ify 的 RFB 转换器 `LwjglRedirectTransformer`（`sortAfter = {"*","mixin:mixin"}`，最后执行）会把每个类里 `org/lwjgl/` 的引用改写为 `org/lwjglx/`，豁免条件：

- 类上标注 `@me.eigenraven.lwjgl3ify.api.Lwjgl3Aware`（编译期依赖 `com.github.GTNewHorizons:lwjgl3ify:<ver>:api`，该 artifact 是源码 jar，javac 可直接用）；或
- jar manifest 声明 `Lwjgl3ify-Aware: true`（整包豁免，不要用在混用 LWJGL2 API 的 jar 上）。

**规则**：凡引用真实 LWJGL3 独有包（`org.lwjgl.sdl` 等）的类，必须加 `@Lwjgl3Aware`，否则引用被改写成不存在的 `org.lwjglx.*`，`ClassNotFoundException`。只引用 LWJGL2 API（`org.lwjgl.input`/`org.lwjgl.opengl`）的类不要加，让它们走 `org.lwjglx` shim 的正常改写路径。

## 光标系统：GLFW → SDL

main（1.12.2/Cleanroom lwjglxx）上的光标系统用 GLFW；1.7.10 侧改写为 SDL（`neofontrender.addons.cursor`）：

| main（GLFW） | 1.7.10（SDL） |
| --- | --- |
| `glfwGetCurrentContext()` | `SDLVideo.SDL_GL_GetCurrentContext()` |
| `glfwCreateStandardCursor(shape)` | `SDLMouse.SDL_CreateSystemCursor(SDL_SYSTEM_CURSOR_*)` |
| `glfwSetCursor(window, cursor)` | `SDLMouse.SDL_SetCursor(cursor)`（恢复默认用 `SDL_GetDefaultCursor()`，不接受 0） |
| `glfwDestroyCursor(handle)` | `SDLMouse.SDL_DestroyCursor(handle)` |
| `GLFWImage` + `glfwCreateCursor` | `SDLSurface.SDL_CreateSurfaceFrom(w, h, SDL_PIXELFORMAT_RGBA8888, pixels, w*4)` + `SDLMouse.SDL_CreateColorCursor`，创建后即可 `SDL_DestroySurface` |

形状映射：`ARROW→DEFAULT`、`IBEAM→TEXT`、`HAND→POINTER`、`HRESIZE→EW_RESIZE`、`VRESIZE→NS_RESIZE`、`NOT_ALLOWED→NOT_ALLOWED`、`WAIT→WAIT`（GLFW 无等待光标，main 上映射为箭头；SDL 有 `SDL_SYSTEM_CURSOR_WAIT`，1.7.10 按语义使用）。`RESIZE_DIAGONAL` 与 main 保持一致用默认箭头。

编译依赖：`compileOnly 'org.lwjgl:lwjgl-sdl:3.4.1'`（中央仓库稳定版；运行时是 lwjgl3ify 带入的 GTNH 3.4.2 快照，SDL3 ABI 稳定）。`CursorType` 的形状常量会被 javac 内联，枚举本身无运行时 SDL 依赖，单元测试无需 SDL jar。

**已知边界**：此实现要求 lwjgl3ify 3.x；lwjgl3ify 2.x（GLFW 时代）实例上光标系统不可用。

## 机制二：FML ASMEventHandler 的可见性要求

FML 为每个 `@SubscribeEvent` 方法动态生成 `ASMEventHandler_*` 调用类，位于 `cpw.mods.fml.common.eventhandler` 包、独立的 `ASMEventHandler$ASMClassLoader`。因此**持有事件方法的类必须是 `public`**（方法也需 `public`）：包私有类会导致现代 Java 抛出 `IllegalAccessError: failed to access class ...`，且只在运行时事件首次分发时暴露，编译期无感。

1.7.10 侧已改为 public 的类：`ItemZoomOverlay`、`ItemZoomKeyBindings`、`AdvancedTooltipHandler`、`HudBarsHandler`（UIE）、`BrilliantCaptureRenderer`（主 mod）。从 main 合并新的事件订阅类时保持 `public`。

## 验证记录

- 崩溃链：`NoClassDefFoundError: org/lwjglx/glfw/GLFW`（引用被改写）→ 加注解后 `org/lwjgl/glfw/GLFW`（运行时无 GLFW，确认 SDL 迁移必要）→ `IllegalAccessError`（事件类可见性）。
- `./gradlew build` 全绿（根工程 181 + UIE 495 测试）；javap 确认光标类无 `org/lwjgl/glfw` 引用、`@Lwjgl3Aware` 在 `RuntimeVisibleAnnotations`。
- 用户实测（2026-10-12）：dev 可进入游戏。
