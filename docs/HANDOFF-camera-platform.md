# UIE 统一相机平台 + 控制器移植 — 交接文档

> 交接时间：2026-08-11
> 相机平台：全部 7 个实施阶段代码完成，待客户端实机回归
> 控制器移植：已迁移为独立 NFR + UIE 依赖 addon；设备、绑定、配置/UI 和多平台 native 分发完成，客户端启动链路已验证，待交互实机回归

## 1. 项目概况

UIE (UI Enhancements) 是 Minecraft 1.12.2 Forge 的增强模组 addon。统一相机平台将以下三个独立相机系统合并到同一套四元数底层：

- **Shoulder Surfing** (1.12.2-2.9.6, MIT) — 肩部第三人称视角
- **Omnilook** (0.3, Unlicense) — 自由观察/视角锁定
- **UIE Flight API** (v9) — 三轴飞行、四元数跟踪、HUD

新增能力：六自由度无人机、旋转/位置惯性、相机 drag/sway。

控制器移植目标：通过独立的 SDL3 Java bindings addon 为 UIE Input API 提供原生手柄支持。
控制器 addon 直接依赖 NFR 本体和 UIE，但 UIE 主包不携带 SDL 或控制器运行时。

## 2. 关键文件清单

### 架构文档
| 文件 | 说明 |
|---|---|
| `docs/uie-camera-platform-architecture-plan.md` | 主架构计划（1438 行），包含全部设计决定、数据流、API 契约 |
| `docs/HANDOFF-camera-platform.md` | 本交接文档 |
| `addons/ui-enhancements/NOTICE.md` | 第三方许可证和归属清单 |

### API 层 (`addons/ui-enhancements/src/main/java/neofontrender/addons/api/`)
| 包 | 核心类 | 说明 |
|---|---|---|
| `api.camera` | `CameraApi`, `CameraFrame`, `CameraAttitude`, `CameraVector`, `CameraBasis` | 公开相机 API v2，不可变帧 + 四元数 |
| `api.camera` | `CameraProvider`, `CameraModifier`, `CameraSession`, `CameraRegistration` | Provider 注册和 session 管理 |
| `api.camera` | `CameraMeasurement`, `CameraProjection`, `CameraRay`, `CameraHit` | 测量和空间查询 |
| `api.input` | `InputApi`, `InputFrame`, `InputAction`, `InputContext` | 设备无关输入仲裁（**手柄将接入此层**） |
| `api.input` | `InputDeviceSource`, `InputBinding`, `InputDisposition` | 设备源和绑定（**手柄适配器注册入口**） |
| `api.flight` | `FlightCameraTracking`, `FlightManeuverInput`, `FlightControlInput` | Flight API v9 兼容层 |

### 内核层 (`addons/ui-enhancements/src/main/java/neofontrender/addons/`)
| 包 | 核心类 | 说明 |
|---|---|---|
| `camera` | `CameraRuntime`, `CameraModule`, `CameraRenderBridge` | 相机运行时和渲染桥接 |
| `camera` | `ShoulderCameraRig`, `ShoulderCameraConfig` | 肩部视角偏移和碰撞 |
| `camera` | `FreeLookCameraRig` | 自由观察四元数控制 |
| `camera` | `DroneMotionController`, `DroneCameraConfig` | 无人机六自由度 |
| `camera` | `CameraPerspectiveController`, `CameraSessionOwner` | 视角循环和 session 生命周期 |
| `camera` | `CameraPickingService`, `CameraPresentationPolicy` | 拾取路由和准星策略 |
| `camera` | `ValkyrienCameraCompat`, `ShaderCameraCompat` | 第三方兼容 |
| `input` | `VanillaInputBridge`, `DroneInputGuard`, `FreeLookInputGuard` | 输入桥接和 guard |
| `flight` | `FlightInputAdapter` | Flight 旧 API 适配 |
| `compat` | `CameraExternalCompat` | 外置 Shoulder/Omnilook 检测 |

### 控制器 addon (`addons/ui-enhancements-controller/`)
| 文件 | 说明 |
|---|---|
| `build.gradle` | 独立 Cleanroom addon，编译期依赖 NFR/UIE/ModularUI，携带 SDL3 FFM Java bindings |
| `src/main/java/.../ControllerAddonMod.java` | addon 生命周期和 `InputApi.registerDeviceSource()` 注册 |
| `src/main/java/.../ControllerConfig.java` | 独立 TOML 配置与运行时参数 |
| `src/main/java/.../ControllerSettingsPage.java` | NFR 设置页集成 |
| `src/main/java/.../sdl/` | SDL 初始化、枚举、热插拔、轴/按钮/帽子采样 |
| `src/test/java/.../sdl/` | dead-zone 和按钮边沿单测 |
| `LICENSE-SDL3` / `THIRD-PARTY-NOTICE.md` | SDL zlib 许可证、MPL 源码与版本出处 |

### 参考资料 (`addons/ui-enhancements-controller/vendor/controlify-upstream/`)
| 路径 | 说明 |
|---|---|
| `src-main/resources/assets/controlify/controllers/` | 手柄识别数据库、默认绑定、字体映射 |
| `src-main/resources/assets/controlify/controllers/gamecontrollerdb-sdl3.txt` | SDL3 游戏控制器数据库 |
| `src-main/resources/assets/controlify/controllers/controller_identification.json5` | 手柄厂商/产品 ID 识别数据 |
| `LICENSE` | Controlify 许可证 |

### Mixin 配置 (9 份)
| 文件 | 注入目标 |
|---|---|
| `mixins.neofontrender_ui_enhancements.json` | 主 Mixin（相机、输入、准星、透明度） |
| `mixins.neofontrender_ui_enhancements_shouldersurfing.json` | Shoulder Surfing 旧修补链 |
| `mixins.neofontrender_ui_enhancements_shouldersurfing_tconstruct.json` | TiC 兼容 |
| `mixins.neofontrender_ui_enhancements_shouldersurfing_matteroverdrive.json` | Matter Overdrive 兼容 |
| `mixins.neofontrender_ui_enhancements_bettercombat.json` | Better Combat 兼容 |
| 其余 4 份 | Salutation、Quark、HEI、Obscure Tooltips |

### 测试 (230+ 项)
| 包 | 测试文件 | 覆盖范围 |
|---|---|---|
| `api.camera` | `CameraAttitudeTest`, `CameraValueTypesTest`, `CameraApiTest`, `CameraMeasurementTest` | 四元数、API 契约、测量 |
| `camera` | `ShoulderCameraRigTest`, `FreeLookCameraRigTest`, `CameraPresentationTransformTest` | rig 逻辑、变换 |
| `camera` | `CameraSessionOwnerTest`, `CameraProxyEntityTest`, `CameraPickingServiceTest` | session、代理、拾取 |
| `camera` | `ValkyrienCameraCompatTest`, `CameraPresentationPolicyTest` | 兼容、策略 |
| `api.input` | `InputApiTest`, `CameraMouseInputEventTest` | 输入仲裁 |
| `api.flight` | `FlightApiTest`, `FlightAttitudeTest`, `FlightControllerInputEventTest` | Flight API |
| `flight` | `FlightOrientationMathTest`, `FlightRollMathTest`, `FlightHudMathTest` | 飞行数学 |
| `flight` | `FlightHudViewportTest`, `FlightHudComponentRegistryTest`, `FlightHudOverlayControllerTest` | HUD |
| `flight.network` | `FlightRollNetworkProtocolTest` | 网络协议 |

### 许可证文件
| 文件 | 来源 |
|---|---|
| `META-INF/licenses/ShoulderSurfing.txt` | MIT |
| `META-INF/licenses/Omnilook.txt` | Unlicense (public domain) |
| `META-INF/licenses/ModNameTooltip.txt` | MIT |
| `META-INF/licenses/ChatAnimation.txt` | MIT |
| `addons/ui-enhancements-controller/vendor/controlify-upstream/LICENSE` | Controlify (SDL3 controller support) |
| 控制器 JAR `META-INF/LICENSE-controlify-sdl.txt` | controlify-sdl (MPL-2.0) |
| `addons/ui-enhancements-controller/LICENSE-SDL3` | SDL 3 (zlib) |
| `vendor/salutation-upstream/LICENSE` | Apache 2.0 |

## 3. 架构核心设计（速查）

### 坐标约定
- 世界：`+X east`, `+Y up`, `+Z south`
- 相机局部：`+Z forward`, `+Y up`, `+X left`
- 四元数 = "局部到世界"的旋转

### 两套姿态
- `bodyAttitude`：玩家/载具物理姿态（Flight、网络同步、玩家模型读取）
- `viewAttitude`：最终渲染姿态（相机、碰撞、拾取、HUD 读取）
- 默认 `view = body`；free-look 激活后 `view = absoluteLook`

### 阶段仲裁（排他，每阶段只有一个所有者）
`ANCHOR` → `BODY_ATTITUDE` → `VIEW_COUPLING` → `POSITION_RIG` → `COLLISION` → `PICKING` → `LENS` → `LISTENER`

### 输入仲裁（手柄将接入此层）
`InputAction` 按游戏含义划分（非设备），priority 降序 + id 升序确定性仲裁。
`PASS / CLAIM / BLOCK` 三种 disposition，每个 sampleId 只结算一次。

手柄适配器只需注册 `InputDeviceSource` 和 binding，不接触 Mixin 或 `EntityPlayerSP.turn()`。
必须支持 dead-zone、响应曲线、反转、按钮边沿、断连时自动归零。
禁止将手柄视为鼠标 delta 再喂给原版。

### 安全约束
- 外置 Shoulder Surfing 或 Omnilook 存在时 → 整套内建相机 fail closed
- Drone 默认禁止交互，断开玩家移动/Flight/热栏
- 所有退出路径（死亡/断线/失焦/维度切换）无条件释放 session + 清空输入

## 4. 控制器 addon Phase 0/1 完成清单

Phase 0 目标：冻结契约、建立独立 addon 构建基础设施；Phase 1 目标：完成 SDL3 设备抽象层。

| 交付物 | 状态 | 说明 |
|---|---|---|
| 独立 addon 声明 | ✅ | `settings.gradle` 包含 `addons:ui-enhancements-controller`，并在根客户端运行前置任务中自动放入 mods |
| addon 构建文件 | ✅ | Java 25、NFR/UIE/ModularUI `compileOnly` 依赖、SDL3 FFM bindings、JUnit 5 |
| Forge 直接依赖 | ✅ | `@Mod` 和 `mcmod.info` 均直接要求 NFR 本体与 UIE |
| SDL Maven 仓库 | ✅ | 只在控制器 addon 声明，UIE 主包不再解析 SDL |
| SDL Java 依赖可拉取 | ✅ | `sdl-api:release-3.4.14-7`、`sdl-backend-ffm:release-3.4.14-7` 已解析并随控制器 JAR 嵌入 |
| SDL 多平台 native | ✅ | SDL 3.4.14 的 Windows/Linux/macOS x86-64、ARM64 六个 classifier 随控制器 JAR 嵌入 |
| native 自动加载 | ✅ | 按 OS/架构选择资源、内容哈希解压、FFM 加载；显式路径和系统库作为回退 |
| native smoke test | ✅ | Windows x86-64 已完成真实解压、ABI 版本校验、SDL 初始化和退出 |
| Cleanroom 客户端启动 | ✅ | addon/嵌套依赖加载成功，SDL 3.4.14 ABI 匹配，2255 条映射和本机 joystick 枚举成功 |
| 独立 addon 可编译 | ✅ | `:addons:ui-enhancements-controller:compileJava` 通过 |
| UIE 主包无控制器依赖 | ✅ | `:addons:ui-enhancements:compileJava`/JAR 通过，包内无 SDL/Controlify 内容 |
| Controlify 参考资料 | ✅ | 已迁移至控制器 addon，包含 SDL3 游戏控制器数据库和许可证 |
| SDL 设备层 | ✅ | `SdlDeviceManager`、`SdlControllerDevice`、`SdlDeviceSource` 已实现 |
| 输入安全 | ✅ | 失焦/断连返回空样本；按钮边沿在采样层处理，dead-zone 在动作绑定层处理 |
| 默认物理绑定 | ✅ | `SdlBindingProvider` 注册相机、Drone、Flight 和常用按钮映射 |
| 独立配置与设置页 | ✅ | TOML 持久化 dead-zone、灵敏度、轴反转、振动偏好；设置页支持 Apply/Cancel |
| `InputApi` 接口契约 | ✅ | `InputDeviceSource`、`InputBinding`、`InputApi.registerDeviceSource()` 已定义 |
| 第三方许可证 | ✅ | 控制器 JAR 注入 `META-INF/LICENSE-Controlify.txt` |

### 架构迁移记录

原先的 `addons/ui-enhancements/controller-jvm` 仅是无源码的构建占位。现已改为
`addons/ui-enhancements-controller` 独立 addon：它直接依赖 NFR 设置/API 与 UIE Input API，
并独立携带 SDL Java 依赖；UIE 主包不再声明 SDL 仓库、SDL `contain` 依赖或 Controlify 资源。

`dev.isxander.sdl:sdl-natives:release-3.4.14` 以 classifier 裸文件发布，不会出现在 POM
依赖列表中。控制器构建已显式解析六个 `.dll/.so/.dylib` classifier，并按 Controlify
资源布局打入 JAR。`SdlRuntime` 默认加载匹配的内嵌 native；仍支持
`-Ddev.isxander.sdl.library=<absolute SDL3 path>` 覆盖，以及不支持平台上的系统库回退。

## 5. 相机平台各阶段完成状态

| 阶段 | 代码状态 | 客户端待确认 |
|---|---|---|
| 0 契约/基线 | ✅ 完成 | 无 |
| 1 四元数与只读 frame | ✅ 完成 | pitch ±90 与连续 roll 实际画面 |
| 2 Flight 共用底层 | ✅ 完成 | Flight + FreeLook 同时操作 |
| 3 Omnilook/free-look | ✅ 完成 | 外部视角切换及 shader/chunk 刷新 |
| 4 Shoulder | ✅ 完成 | 墙角/低顶/载具/Valkyrien |
| 5 拾取/准星/透明度 | ✅ 完成 | 实际交互一致性、盔甲/glint/shader 像素 |
| 6 Drone/输入 | ✅ 完成 | 持续挖掘、热栏、交互开关与世界切换 |
| 7 兼容/清理 | ✅ 源码检查完成 | 外置模组及完整第三方兼容矩阵 |

## 6. 控制器移植后续阶段

### Phase 1 — SDL3 设备抽象层
- ✅ 在 `addons/ui-enhancements-controller` 中完成 `SdlDeviceManager`：初始化 SDL、枚举手柄、监听热插拔
- ✅ 完成 `SdlControllerDevice`：读取标准 gamepad 和通用 joystick 的轴/按钮/帽子状态
- ✅ 完成 `SdlDeviceSource implements InputDeviceSource`：输出每帧 `InputDeviceSample`
- ✅ 完成原始线性轴、按钮 pressed/released 边沿、断连中性样本；dead-zone 下移到动作绑定层
- ✅ 已覆盖 dead-zone 和边沿状态单测；真实热插拔仍需客户端实机验证
- ✅ SDL 3.4.14 六个平台 native 内嵌和自动加载；Windows x86-64 smoke test 已通过

### Phase 2 — 绑定与映射
- ✅ 实现默认绑定（参考 `addons/ui-enhancements-controller/vendor/controlify-upstream/controllers/` 中的数据）
- ✅ 实现 `InputBindingProvider`：手柄轴/按钮 → `InputAction` 映射
- ✅ 保留源端原始线性轴值；dead-zone、轴反转和灵敏度缩放在动作绑定层处理，参数由独立 TOML 持久化
- ✅ 默认映射已改为可持久化 profile；覆盖所有 `InputAction`，支持监听下一输入、清除、恢复默认和冲突提示
- ✅ 原版及 Forge/mod `GameSettings.keyBindings` 全量可绑定；NFR 的 `KeyBinding` Mixin 按实例合并 down/press，不通过键码广播
- 参考 `addons/ui-enhancements-controller/vendor/controlify-upstream/src-main/resources/assets/controlify/controllers/gamecontrollerdb-sdl3.txt`

### Phase 3 — UIE 集成
- ✅ 独立 addon 初始化时注册 `InputDeviceSource` 和 `InputBindingProvider`
- ✅ 右摇杆接入 `CAMERA_LOOK_X/Y` 与 `FLIGHT_PITCH/ROLL`，左摇杆接入 `FLIGHT_YAW`
- ✅ 左摇杆/扳机接入 Drone 平移；Drone 模式下遵循 `DroneInputGuard` 的断开规则
- ✅ 断连/失焦时自动归零，不泄漏到下一帧
- ✅ 同时采样所有已连接设备，NFR 下拉菜单选择唯一目标设备；断连后自动回退

#### 模拟移动与视角路由（本轮补充）

- 左摇杆保持有符号线性值，默认 `LEFT_STICK_Y (inverted)` → `PLAYER_MOVE_FORWARD`、
  `LEFT_STICK_X (inverted)` → `PLAYER_MOVE_STRAFE`。这与 Forge 1.12.2 的
  `MovementInput` 约定一致：前进和左移为正，后退和右移为负；潜行时两个轴都乘 `0.3`。
- `PlayerMovementInputBridge` 在 `InputUpdateEvent` 写入最终值。它读取 Input API 合并后的
  动作值，而不是把手柄值再次叠加到键盘值，避免同时按键时重复放大；Drone 的 BLOCK 上下文
  会清空玩家移动。
- 右摇杆先映射为按帧时间缩放的虚拟鼠标单位（1200 units/s）。普通视角、肩视角和
  “自由视角控制玩家”交给原版 `player.turn()`；Drone 和“自由视角控制相机”交给相机 rig；
  Flight 活跃时由 `FLIGHT_PITCH/FLIGHT_ROLL` 消费，避免重复旋转。
- 原版及 Forge 注册的 `KeyBinding` 捕获轴时保存 `+`/`-` 半方向；旧的无方向记录按
  `ANY_DIRECTION` 兼容加载。

实现参考：MrCrayfish Controllable 1.12.2 `0.11.1`，commit
`d2b47f279e5bae5e47f83aa613c7902d8437c58f`（`ControllerInput.java`）。

### Phase 4 — 配置与 UI
- ✅ 独立配置文件 `config/neofontrender-ui-enhancements-controller.toml`
- ✅ NFR 设置页：dead-zone、相机/飞行灵敏度、各轴反转、振动偏好
- ✅ 嵌入式控制器工作台：标准手柄可视化（含 L3/R3）、六轴线性数值、Arc3D raw/dead-zone/mapped 历史和响应曲线
- ✅ 完整 UIE 动作绑定列表，以及原版/Forge 注册按键列表；两者都使用页面内输入监听组件
- ✅ GUI 控制动作也进入同一绑定层：虚拟光标、滚动、确认、返回、次操作、快速移动和四向导航均可在 NFR 页面重映射
- ✅ GUI 运行时参考 Modern Controlify 的 VirtualMouse/ScreenProcessor 边界，以及 Controllable 1.12.2 的槽位吸附、创造栏/列表滚动和 `GuiScreen` 反射事件调用
- ✅ 左摇杆光标、A/X 的 press-drag-release、B 返回、Y 容器快速移动、十字键空间导航已接入独立 Controller Mixin；窗口级系统光标仅在手柄接管时隐藏
- ✅ 配置每帧动态生效；设置页 Apply 持久化、Cancel 回滚
- ⚠️ 振动偏好已持久化，但 UIE Input API 尚无 haptics 输出通道，当前不会实际触发振动
- ⏳ 手柄按键提示 HUD（参考 Controlify 的 controller theme 系统）
- ⏳ 按稳定硬件 GUID 保存多设备独立 profile；当前已支持运行时目标设备切换

## 7. 尚需客户端实机验证的场景

这些是发布门禁，不改变 API 和所有权边界：

1. **Shoulder + Flight + Free-look 组合**：三者同时激活时的姿态一致性
2. **低顶/墙角/载具**：8 点碰撞在狭窄空间的正确性
3. **Valkyrien ship collision**：指定版本的船体碰撞
4. **第三方 shader**：OptiFine/shader pack 下的分辨率和渲染
5. **盔甲/附魔 glint/持有物**：玩家透明度模式下的像素表现
6. **Forge overlay 优先级**：Better Combat、TiC、Matter Overdrive 准星共存
7. **外置 Shoulder Surfing/Omnilook**：真实 jar 共存时的 fail-closed 行为
8. **Drone 持续挖掘/热栏/交互开关**：输入断开的完整性
9. **世界切换/断线/死亡**：session 和输入清理无残留
10. **控制器实机交互**：设置页 Apply/Cancel、轴方向/灵敏度、模式切换、热插拔和断连归零
11. **GUI 控制器交互**：虚拟光标命中按钮/槽位、容器拖拽、Y 快速移动、创造栏/Forge 列表滚动、物理鼠标接管后的光标恢复

## 8. 构建与测试

```bash
# 编译控制器 addon
./gradlew.bat :addons:ui-enhancements-controller:compileJava

# 编译 UIE 主 addon（不含控制器运行时）
./gradlew.bat :addons:ui-enhancements:compileJava

# 运行控制器 addon 单测
./gradlew.bat :addons:ui-enhancements-controller:test

# 打包控制器 addon
./gradlew.bat :addons:ui-enhancements-controller:jar

# 当前平台的 SDL3 解压/FFM/初始化 smoke test
./gradlew.bat :addons:ui-enhancements-controller:smokeTestBundledSdl

# 运行测试
./gradlew.bat :addons:ui-enhancements:test

# 客户端运行
./gradlew.bat :addons:ui-enhancements:runClient
```

## 9. 已知技术债

1. `FlightManeuverInput` 继续通过 `FlightInputAdapter` 服务旧调用方（兼容层）
2. 原版第三人称视锥裁剪由精确的 `orientCamera` 距离注入处理，未新增全局裁剪器
3. `FlightOrientationEvent` 保留为旧式局部增量边界（欧拉增量 → 局部 delta quaternion）
4. `ui-enhancements-controller` 要求 Java 25（FFM / JEP-454）；已内嵌六个平台 SDL3 native，不支持的平台可通过 `dev.isxander.sdl.library` 指定自备库
5. 振动配置已预留，但 UIE Input API 暂无输出/触觉通道；按钮提示和按硬件 GUID 保存的多设备档案仍待实现
6. 复杂第三方 GUI 若不暴露 `GuiSlot`/`scrollBy`，暂时只能使用虚拟光标点击；后续可按 Controlify 的 screen-specific processor registry 增加兼容处理
