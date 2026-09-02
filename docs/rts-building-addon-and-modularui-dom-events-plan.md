# RTS Building 独立 Addon、ModularUI DOM 事件与 XML UI 计划

状态：架构分析与实施计划，尚未开始实现

浏览器式 ModularUI runtime 的独立审核规格见：
`docs/modularui-browser-runtime-architecture.md`。该文档覆盖制品边界、Pinia 风格 store、
Arc3D 复用边界、线程与安全模型、完整依赖 BOM 和 API 修改清单；本文继续作为 RTS 功能总盘点。

现有 ModularUI 同步系统的复核及最新修订见：
`docs/modularui-browser-runtime-sync-integration-review.md`。最新方案将 XML/DOM/event/style/store
内置 MUI Java 8 主体，仅把 GraalJS 作为可选 client addon，并为 synced XML 增加 ProtocolPlan、
显式 stable key、固定 slot table 和 fingerprint handshake。

目标版本：Minecraft 1.12.2 / Cleanroom / Java 25 client / Java 8 server-compatible MUI

参考实现：`D:/Projects/sfr/other_mods/RTSbuilding`（Minecraft 1.21.1）

ModularUI：`D:/Projects/sfr/other_mods/ModularUI`，commit `75f86dabac6f91a224eb2a0716bf3838ff38f6a3`，版本 `3.2.0-nfr.1`

实际 fork 的类级修改方案见 `docs/modularui-browser-runtime-75f86da-code-change-plan.md`。

## 1. 结论

RTSbuilding 的核心体验可以在 1.12.2 重现，但应作为一个独立、客户端和服务端都安装的 addon 实现，而不是 UIE 内部功能。

确定的职责边界如下：

```text
ModularUI fork
  ├─ Widget 树、布局、主题、焦点、Panel、输入事件、导航语义
  └─ 可选 XML 声明式构建层

NFR 主模组
  └─ 字体、通用视觉基础组件、NFR 主题与通用设置界面能力

UIE
  └─ 相机平台、输入 Action 平台、GUI 导航平台、相机相关渲染策略

RTS Building addon
  ├─ RTS UI 组件和页面
  ├─ 相机模式状态与服务端相机会话
  ├─ 放置、破坏、交互、仓储、合成、蓝图、任务和历史
  ├─ 网络协议、权限验证和持久化
  └─ AE2、Refined Storage、JEI 等 1.12 专用兼容层
```

不采用以下方案：

- 不复用 RTSbuilding 的 UI Core/UI Kit 作为运行时 UI 框架。
- 不在 ModularUI 上再保留一套平行的焦点、弹层、指针捕获和事件路由器。
- 不把 RTS 页面、业务组件或服务端服务塞进 UIE。
- 不使用 NFR HUD compositor 承载 RTS 主界面。
- 不直接使用 UIE 内部类；RTS 只依赖公开的 camera、input 和 navigation API。

当前 ModularUI 没有 XML UI、持久 DOM 或脚本运行时，但现有 Widget 树、布局、主题、资源系统和 value/sync API 足以作为 render tree。目标应提升为浏览器式 ModularUI runtime：XML 描述 DOM，JSON 描述 stylesheet/theme，Java 注册 native component/event/store/action，GraalJS 作为可选客户端动态行为层。实现顺序仍先做 client-only Document，但同步 XML 不再无限期延后；它必须通过 common ProtocolPlan 与现有 `PanelSyncManager` 集成，不能让动态 DOM 驱动 BFS auto-sync 拓扑。

## 2. 复现范围和功能归属

### 2.1 视角与输入

需要复现：

- 俯视相机进入和退出。
- 相机锚点、Home 选择和移动半径。
- 键盘平移、垂直移动、鼠标拖拽平移、旋转和滚轮缩放。
- 本地平滑预测、服务端权威范围限制和位置校正。
- 屏幕任意位置到世界射线的转换。
- 相机视角下的方块、流体和实体拾取。
- 打开 RTS 界面时继续采样键鼠和手柄 Action。
- 退出、死亡、换维度、断线和外部切换视角时完整释放会话。

实现方式：

- RTS 注册自己的 `rts:builder_camera` `CameraProvider`。
- RTS 获取并持有原子 `CameraSession`，不直接使用 UIE 内置 Drone rig。
- UIE 的 client-only render-view proxy 应取代旧实现中仅为渲染服务的相机实体。
- 服务端保存纯数据 `RtsCameraSession`，不依赖 UIE 客户端类型。
- 客户端高频更新本地预测位姿，按限频协议发送服务端；服务端返回校正状态。

### 2.2 主界面

需要复现：

- 顶部命令栏。
- 底部大型材料/工具工作区。
- 大型快捷栏。
- 搜索、排序、分类和分页。
- 浮动窗口、可移动窗口、对话框和模态确认。
- 模式轮盘、方块状态轮盘和快速建造控制面板。
- Blueprint、Storage、Craft、Workflow、Guide、Settings、Plugin 等页面。
- Tooltip、进度、错误和状态反馈。

实现方式：

- 主界面是透明、全屏、`pausesGame(false)` 的 `ModularScreen`。
- 根 `ModularPanel` 禁用主题背景，Minecraft 世界在界面后正常渲染。
- 世界输入区域本身是透明的 `RtsWorldViewport` Widget。
- 浮动窗口和 Modal 使用 ModularUI Panel 栈，而不是 RTS 自己维护 z-order 路由器。
- HUD compositor 只保留给关闭主界面后仍需显示的通知或短时状态，不参与主界面布局。

### 2.3 建造和世界交互

需要复现：

- 单方块放置、破坏、普通使用和空手交互。
- Pick block 和材料选择。
- 方块朝向预设、放置后旋转和状态组合。
- 流体放置、回收和内部流体缓存。
- 掉落物吸收、工具租用、材料提取和操作音效。
- 玩家移动目标和远程视角交互。
- Smart Fill、批量提交和待确认预览。

这些属于 RTS addon 的客户端意图层、服务端验证管线和执行服务，不能由 UIE 相机 API 代替。

### 2.4 形状、范围操作和挖掘

需要复现：

- 线、墙、平面、方框、圆、球、圆柱等形状生成。
- Fill mode、形状参数和世界 Ghost preview。
- Ultimine/连锁挖掘。
- 区域挖掘、区域摧毁和便利摧毁模式。
- 选择盒、轴向手柄、拖拽调整和键盘微调。
- 破坏工具等级、最大体积、目标数和每 tick 工作量限制。

形状生成器可以保留为无 Minecraft UI 依赖的纯逻辑模块；执行必须进入服务端可预算任务引擎。

### 2.5 蓝图

需要复现：

- 世界区域捕获和命名。
- 蓝图库、文件操作、详情和材料窗口。
- Ghost 预览、旋转、镜像、变换和材料替换。
- 大型蓝图分片执行、暂停、恢复和失败报告。
- Vanilla structure NBT、Sponge schematic、Litematic 和 Building Gadgets 模板读取。
- BlockEntity NBT 清理和 1.12 方块状态/metadata 转换。

格式解析逻辑可以按许可证复用或移植，但 1.21 block state、data component 和 registry 访问必须改写为 1.12 模型。

### 2.6 仓储、大型快捷栏和漏斗

需要复现：

- 单个和批量连接容器。
- 去重后的网络端点、断开、更新和详情。
- 服务端分页、搜索、拼音搜索、排序和缓存失效。
- Item/fluid 条目、计数和最近使用记录。
- 大型快捷栏、工具槽、自动存入、快速移动和 carried stack 回收。
- GUI binding、远程菜单和菜单槽位导入。
- Funnel 目标和 Funnel workflow。
- 可选跨维度仓储及受限 chunk ticket。

核心必须围绕 1.12 `IItemHandler`/`IFluidHandler` 重新抽象。AE2 和 Refined Storage 只通过可选 adapter 接入，不泄漏各自类型到核心服务接口。

### 2.7 合成

需要复现：

- 可合成条目查询和合成终端。
- 配方搜索、数量选择、清空、补充和执行反馈。
- 从连接仓储填充 crafting grid。
- JEI 搜索同步和配方转移。

合成核心由 RTS 服务端实现；JEI 只负责客户端 recipe/transfer adapter，服务端仍要重新验证配方、材料和输出空间。

### 2.8 Workflow、持久任务和历史

需要复现：

- Placement、Mining、Destruction、Blueprint 和 Funnel task。
- 每玩家公平调度、全局每 tick 时间预算和单 slice 工作量预算。
- 暂停、恢复、取消、超时和进度同步。
- 服务重启后的任务恢复、原子 NBT 存储、蓝图 blob 和 tombstone。
- Undo/redo、已放置方块恢复和副作用提交屏障。
- 操作诊断、trace id 和有限速反馈。

这是完整复现中风险最高的服务端部分，不能简化为客户端逐方块发包。

### 2.9 Progression、插件和设置

需要复现：

- 可选生存进度系统和能力解锁。
- Home anchor。
- 队伍共享状态。
- 插件安装、卸载、同步和能力策略。
- Guide、设置页、开发诊断和配置迁移。

设置 UI 使用 RTS 自己的 ModularUI 页面；配置和策略数据由 RTS addon 持有。

### 2.10 客户端世界渲染

需要复现：

- 放置、破坏、Pending、Blueprint、Shape 和 Ultimine Ghost。
- Selection box、corner bracket、axis handle 和交互目标提示。
- 放置/破坏动画、声音和世界状态反馈。
- 可选范围裁剪和可见性 invalidation。

这些不是 ModularUI Widget 绘制，而是 RTS addon 的世界渲染层。它们读取同一份 UIE `CameraFrame`，屏幕投影和拾取不得另算一套相机矩阵。

### 2.11 模组兼容

必须按 1.12 API 重写：

- Applied Energistics 2。
- Refined Storage。
- JEI。
- 1.12 对应的队伍、任务和领地模组。
- 存在 1.12 版本的背包、远程容器和 Building Gadgets。

不能直接移植或在 1.12 不存在：

- 现代 Create/Flywheel 接口。
- Jade；需要按实际目标改为 HWYLA/WAILA 或不提供。
- 现代 Open Parties and Claims、Sophisticated Backpacks、Sable、Beyond Dimensions 等接口。

所有 compat 类必须延迟加载或位于独立 compat source/module，避免可选模组缺失时 JVM 验证失败。

## 3. 独立 addon 结构

建议 Gradle 模块：

```text
addons/rts-building
```

建议包结构：

```text
neofontrender.addons.rts
  api
  bootstrap
  client
    camera
    input
    render
    gui
      component
        base
        business
      layouts
      model
      pages
      views
      screen
  common
    action
    blueprint
    shape
    storage
    workflow
  network
  server
    session
    pipeline
    service
    task
    persistence
  compat
    ae2
    refinedstorage
    jei
    teams
    claims
```

初期采用一个通用客户端/服务端 JAR。只有在可选依赖的类加载隔离无法可靠完成时，再将 compat 拆成附属 JAR；不为了形式上的模块化提前拆分核心客户端和服务端 artifact。

Gradle 依赖方向：

```gradle
compileOnly project(':')
compileOnly project(':addons:ui-enhancements')
compileOnly("com.cleanroommc:modularui:${modularui_version}") { transitive = false }
```

运行时元数据应声明所使用的 NFR、UIE 和 ModularUI fork 最低版本。RTS 是独立 addon，不等于它必须无依赖运行。

## 4. ModularUI UI 组件组织

### 4.1 可直接复用的 NFR 基础组件

- `NfrTextButton`
- `NfrContentButton`
- `NfrCycleButton`
- `NfrOptionDropdown`
- `NfrDecimalSlider`
- `NfrLabeledSlider`
- `NfrLabeledTextField`
- `NfrScrollablePane`
- `NfrOptionsGrid`
- `NfrResponsiveFieldGrid`
- `NfrLayout`

NFR README 已把 `neofontrender.client.gui.component.base` 声明为提供给依赖模组的可复用组件目录。

### 4.2 RTS addon 自己实现的基础组件

- `RtsIconButton`
- `RtsModeButton`
- `RtsTabStrip`
- `RtsSearchField`
- `RtsItemGrid`
- `RtsStorageSlot`
- `RtsQuickbar`
- `RtsWindow`
- `RtsModalPanel`
- `RtsRadialMenu`
- `RtsSplitPane`
- `RtsDockLayout`
- `RtsProgressRow`
- `RtsWorldTooltip`
- `RtsWorldViewport`

其中只有被至少两个非 RTS 页面或模组证明通用的控件，才考虑上移到 NFR。仓储槽、蓝图窗口、Workflow row 等业务组件不能进入 NFR。

### 4.3 页面组合

```text
RtsBuilderScreen
  RtsBuilderLayout
    RtsTopCommandBar
    RtsWorldViewport
    RtsBottomWorkspace
    RtsFloatingWindowLayer
    RtsModalLayer
```

Panel/Widget 的职责：

- ModularUI 管理命中、焦点、滚动、拖拽、Panel 顺序和 Modal 边界。
- View 只组合业务组件和订阅 model。
- Model 表示界面会话状态，不持有 Widget 引用。
- 网络响应先更新 model，再由 View 刷新控件。
- 世界点击只有在 `RtsWorldViewport` 成为最终事件目标时才产生 RTS action。

### 4.4 主题

RTS addon 发布自己的：

```text
assets/neofontrender_rts/themes.json
assets/neofontrender_rts/themes/rts_modern.json
```

`rts_modern` 可以继承 `neofontrender_modern`，再覆盖 RTS 的窗口、槽位、选择态和危险操作颜色。不要直接修改 NFR 主题来适配 RTS，也不要硬编码 RTS 旧 UI 贴图。

旧项目的媒体资源受 `LICENSE-ASSETS` 约束，新 UI 默认不复用其受限资产；源码复用则继续遵守 LGPL 要求。

## 5. UIE/NFR/ModularUI API 缺口

### 5.1 UIE 必须补齐

#### 输入焦点语义

当前 `InputApi.beginFrame(partialTicks, gameFocused)` 在打开 GUI 后通常发布 neutral frame。建议把输入采样条件拆成：

```text
windowFocused
worldFocused
screenActive
interactionMode
```

设备连接和 Action 采样以 `windowFocused` 为边界；玩家移动/攻击等世界 Action 是否有效由 context/owner 决定。这样 RTS 主界面打开时仍可控制相机，同时不会把按键泄漏给玩家身体。

#### GUI 坐标到相机 viewport 坐标

`CameraApi.screenRay(x, y, partialTicks)` 当前使用渲染 viewport 像素，ModularUI 事件使用 GUI logical coordinate。建议新增明确坐标空间：

```text
FRAMEBUFFER_PIXEL
GUI_LOGICAL
NORMALIZED_DEVICE
```

转换统一处理 GUI scale、display/framebuffer 尺寸、shader resolution 和 viewport offset。RTS 不应自行复制换算。

#### Camera presentation policy

建议相机会话可以声明：

- 是否隐藏手持物。
- 是否隐藏原版准星。
- 是否关闭 view bobbing/hurt tilt。
- 是否使用 UI view proxy。
- vanilla block outline 和 selection target 的处理策略。

这些是相机平台行为，应由 UIE 通过 lease/policy 合并，不能让每个相机 addon 自己加 Mixin。

### 5.2 NFR 可选增强

- 公共主题 ID 和 style token。
- 通用 `NfrIconButton`。
- 被多个页面复用后再上移的 SearchField/TabStrip。

第一条 RTS vertical slice 不要求修改 NFR。

### 5.3 ModularUI 需要通用事件和可选 markup 扩展

现有 `Interactable` 足以做第一版世界 viewport，然而它的事件返回值和传播语义不够明确，且 `IGuiAction` 已在源码中标注待替换为 proper event system。DOM 风格事件系统可以作为 ModularUI 的通用能力实施，但必须保持旧 Widget 二进制和行为兼容。

当前 ModularUI UI 只能通过 Java builder 构造。XML UI 不是 RTS 的硬阻塞，但若要让页面结构更清晰、支持资源级组合和 addon 自定义标签，应在 ModularUI 增加通用 markup parser/compiler/registry API；实现范围和边界见第 15 节。

## 6. UI 无关的 RTS 应用/领域事件总线

RTS 还需要一套与 Widget、Screen 和 ModularUI 完全无关的事件通道。它解决的是业务模块之间的状态变化通知，不解决鼠标/键盘如何传播。

两套事件不能混用：

```text
原始输入
  → ModularUI DOM event
  → RtsWorldViewport / Button / TextField
  → RtsCommand
  → application service / server packet
  → state commit
  → RtsDomainEvent
  → model refresh / diagnostics / sound / network sync
```

### 6.1 三种概念必须分开

#### Command/Intent

表示调用方希望系统执行什么，例如：

- `EnterBuilderMode`
- `MoveCamera`
- `PlaceBlock`
- `RequestStoragePage`
- `PauseWorkflow`

Command 有唯一处理者，可以成功或失败，可以返回明确结果。UI 点击和世界 viewport 输入最终转换成 Command，而不是广播“请某个监听器帮忙放方块”的事件。

#### State/Model

表示当前事实，例如：

- 当前相机会话。
- 当前 Builder mode。
- 当前 storage page snapshot/revision。
- 当前 selection、blueprint session 和 workflow 列表。

Model/store 是状态真相源。新 View 订阅后先读取 snapshot，不能依赖“刚好重放到一条历史事件”才能正确显示。

#### Domain/Application Event

表示已经完成的事实，例如：

- `CameraSessionStarted`
- `BuilderModeChanged`
- `StoragePageUpdated`
- `StorageInvalidated`
- `WorkflowProgressed`
- `WorkflowCompleted`
- `PlacementCommitted`
- `OperationRejected`

Event 可以有多个观察者，不返回业务结果，不能取消已经提交的状态变化。

### 6.2 所有权和包结构

该总线属于 RTS addon：

```text
neofontrender.addons.rts.common.event
  RtsEvent
  RtsEventType<E>
  RtsEventListener<E>
  RtsEventBus
  RtsSubscription
  RtsSubscriptionScope
  RtsEventEnvelope
```

具体事件按领域放置：

```text
common/camera/event
common/storage/event
common/workflow/event
common/blueprint/event
common/action/event
client/event
server/event
```

公共 event API 不导入 ModularUI、LWJGL、`GuiScreen`、Widget 或客户端渲染类型。跨侧事件 payload 优先使用 RTS 自己的不可变 value object；不要让 UI 组件类型进入事件。

不要把这套总线放进 NFR 或 UIE。只有将来至少有两个独立 addon 证明需要完全相同的应用事件基础设施时，才考虑抽成更通用的库。

### 6.3 总线范围

不使用单个 JVM 全局静态总线。至少分成：

```text
RtsClientEventBus    每个客户端运行时一个，限定 Minecraft client thread
RtsServerEventBus    每个 server/runtime 一个，限定 server tick thread
RtsSessionEventBus   可选，每个 Builder 会话一个，关闭会话时整体释放
```

服务端和客户端不是通过共享 event bus 通信。网络 packet 是明确的边界适配器：

```text
server domain event
  → server sync subscriber
  → S2C packet
  → client packet handler（切回 client thread）
  → update client model
  → client application event
```

禁止把任意 event 自动序列化广播，否则会泄漏内部实现、扩大协议面并绕过权限和流量控制。

### 6.4 建议 API

```java
public interface RtsEvent {
    RtsEventType<? extends RtsEvent> type();
}

public interface RtsEventBus {
    <E extends RtsEvent> RtsSubscription subscribe(
            RtsEventType<E> type,
            RtsEventListener<? super E> listener);

    void publish(RtsEvent event);
}

public interface RtsSubscription extends AutoCloseable {
    boolean isActive();
    @Override void close();
}
```

`RtsEventType` 使用稳定 namespace ID，而不是只使用 Java `Class` 作为公开身份。实现内部仍可用 typed key 保证泛型匹配。

`RtsEventEnvelope` 可以提供通用元数据，但不要塞入业务 payload：

```text
sequence
createdAtNanos
traceId
origin（client/server/network/recovery）
player/session id（可选）
```

### 6.5 分发语义

- 同步分发，始终在发布者所属的 client/server 主线程执行。
- 网络线程和后台 IO 线程必须先 schedule 回正确主线程，再修改状态和 publish。
- 同一事件的 listener 按注册顺序获取稳定快照。
- listener 之间不应依赖顺序；存在顺序依赖的步骤应写成明确 service pipeline。
- listener 异常逐个隔离、记录 event type/trace/listener，但不阻止后续观察者。
- publish 期间再次 publish 时，嵌套事件进入同一线程 FIFO 队列，由最外层 drain，避免无限递归和深度优先顺序漂移。
- Event 在状态提交成功后发布。需要阻止操作的逻辑属于 validator/policy/interceptor，不属于可取消 event。
- 总线不保存 retained/latest event，也不承担 event sourcing。需要当前状态时直接读取 model snapshot。

首版不支持 listener priority。优先级往往会把隐藏的控制流依赖合法化；确实需要优先级的框架扩展点应单独设计为 policy/provider registry。

### 6.6 生命周期

订阅必须返回可关闭 handle，并支持 scope 聚合：

```java
try (RtsSubscriptionScope scope = new RtsSubscriptionScope()) {
    scope.add(bus.subscribe(StorageEvents.PAGE_UPDATED, model::onPageUpdated));
    scope.add(bus.subscribe(WorkflowEvents.PROGRESSED, model::onWorkflowProgressed));
}
```

- Screen/View dispose 时关闭自己的 client subscription scope。
- Builder session 退出时关闭 session scope。
- 玩家离线时服务端释放 player/session listener。
- Server stop 时关闭 server bus，并拒绝之后的 publish。
- 默认使用强引用和显式生命周期，不依赖 weak listener 掩盖泄漏。

### 6.7 与 UI model 的关系

推荐数据流：

```text
packet/domain service
  → StorageModel.apply(snapshot)
  → model revision +1
  → publish StoragePageUpdated(revision, queryKey)
  → View 检查 revision 并刷新可见控件
```

Event 不携带可变 Widget，也不要求 View 存在。关闭 UI 后 client model 仍可更新；重新打开页面时 View 从 model snapshot 构造。

对于纯 UI 临时状态，例如某个按钮 hover 或窗口拖到哪个像素，不进入 domain bus。它留在 Widget/页面 model。只有会被多个非 UI 模块观察、持久化或同步的状态变化才发布应用事件。

### 6.8 与 Forge EventBus 的关系

Forge EventBus 继续用于：

- Minecraft/Forge 生命周期入口。
- tick、world unload、player login/logout 等平台事件。
- 少量明确承诺给第三方模组的公共、稳定扩展事件。

RTS 内部高频事件不全部发布到 Forge EventBus。内部 typed bus 更容易控制线程、生命周期、协议和 API 稳定性。若第三方确实需要某个事件，使用 bridge 选择性映射为公开 Forge event，而不是直接公开内部总线上的所有类型。

### 6.9 与旧 RTS 实现的关系

旧项目只有一个窄用途 `RtsWorkflowEventBus`：`CopyOnWriteArrayList`、调用线程同步分发、listener 异常隔离。这些原则可以保留，但新实现需要补齐：

- typed event key。
- subscription handle/scope。
- client/server 实例隔离。
- 主线程断言。
- 嵌套 publish FIFO 队列。
- trace/envelope。
- shutdown 和泄漏诊断。

旧 UI Reducer 的 `Action → Transition` 仍可以作为纯函数保留，但它不是事件总线。Reducer 负责计算局部状态；总线只在提交后通知外部观察者。

### 6.10 测试要求

- 类型匹配和错误类型注册拒绝。
- FIFO 注册顺序。
- dispatch 中 subscribe/unsubscribe 的 snapshot 行为。
- `once` 如需提供时的递归安全。
- listener 异常隔离。
- nested publish 的 FIFO 顺序和最大安全 drain 限制。
- client/server 错线程 publish 失败。
- scope close 幂等且释放全部 listener。
- bus shutdown 后无事件泄漏。
- View 关闭后 model 仍能更新，重新打开可读取完整 snapshot。
- server event 到 packet 再到 client event 不产生回环或重复应用。

## 7. ModularUI 当前事件系统审查

### 7.1 已有能力

当前 fork 已具备：

- 真实的 `IWidget` 父子树。
- `LocatedWidget` 和每个命中 Widget 的完整变换矩阵。
- Panel 从顶到底的稳定顺序。
- Hover 候选列表和 click-through/hover-through。
- `Interactable` 的 press/release/tap/drag/scroll/key 回调。
- `IGNORE`、`ACCEPT`、`STOP`、`SUCCESS` 四种返回结果。
- 焦点 Widget 优先处理部分键盘和指针事件。
- 按下目标保存，以及 drag/release 回到按下目标的基本捕获行为。
- Panel modal boundary：`disablePanelsBelow()`。
- Panel 外部点击关闭：`closeOnOutOfBoundsClick()`。
- ESC 关闭顶层 Panel。
- 独立、不可变的导航树和语义 Action dispatcher。

这些能力说明无需另建 RTS UI 路由器。

### 7.2 与浏览器 DOM 的关键差距

| 语义 | 当前 ModularUI | 浏览器式目标 |
|---|---|---|
| 目标选择 | 依次尝试同一点的 hovered Widget | 先确定唯一 target |
| 传播路径 | 在重叠候选之间迭代 | root → parent → target → parent → root |
| 阶段 | 无 capture/target/bubble | 三阶段明确 |
| 停止传播 | `STOP/SUCCESS` 混合处理和停止含义 | `stopPropagation()` |
| 停止同目标 listener | 无 | `stopImmediatePropagation()` |
| 默认行为 | 与 Widget 回调本身混在一起 | `preventDefault()` 单独控制 |
| handled | boolean/Result 含义依入口不同 | 独立 `handled` 状态 |
| 指针捕获 | 隐式保存 pressed Widget | 显式 set/release pointer capture |
| 事件坐标 | 从可变 context 查询 | 事件携带 global/local/delta 坐标 |
| 修饰键 | 回调内查询 LWJGL/GuiScreen | 输入边界采样后写入事件 |
| 键盘目标 | focused + hovered 候选混合 | focused target 的祖先路径 |
| listener 生命周期 | screen 全局表，Widget 初始化时注册 | listener 属于 EventTarget |
| mutation safety | `doSafe` 延迟部分树操作 | dispatch path/listener snapshot |
| 取消事件 | 无 pointercancel | capture owner 失效时发 cancel |

特别需要注意：现有 `IGuiAction` listener 虽然函数返回 boolean，但 `ModularScreen` 调用时忽略返回值。它适合观察，不适合作为可消费的全局拦截器。

### 7.3 旧 RTS UI Core 也不是真正 DOM 模型

旧 RTS `UiEventRouter` 提供的是：

- 顶层 layer 命中。
- 按 mouse button 的 pointer capture。
- keyboard focus。
- modal layer。
- Escape stack。
- handled、stop UI propagation、block world、capture pointer、request focus 的强语义结果。

它没有父子 target path，也没有 capture/target/bubble。升级 ModularUI 时应吸收其强返回语义，但不应照搬它的平行 layer stack。

## 8. DOM 风格事件 API 提案

以下名称是设计占位，实施前可以调整。

### 8.1 公共 API 包

```text
com.cleanroommc.modularui.api.event
  MuiEvent
  MuiEventType<E>
  MuiEventPhase
  MuiEventListener<E>
  EventListenerOptions
  EventSubscription
  PointerEvent
  KeyEvent
  FocusEvent
  ActionEvent
  PointerType
  InputModifiers
```

框架实现放在：

```text
com.cleanroommc.modularui.screen.event
  MuiEventDispatcher
  EventTargetPath
  EventListenerRegistry
  PointerCaptureManager
  LegacyInteractionDefaultAction
```

公共 API 不暴露 `LocatedWidget`、`TransformationMatrix`、hover list 或其他 internal 类型。

### 8.2 EventTarget

给 `IWidget` 增加默认事件目标能力，或让它继承新的 `IEventTarget`：

```java
<E extends MuiEvent> EventSubscription addEventListener(
        MuiEventType<E> type,
        MuiEventListener<? super E> listener,
        EventListenerOptions options);
```

`AbstractWidget` 持有实际 listener registry；`Widget` 提供 fluent builder：

```java
worldViewport.onEvent(PointerEvents.DOWN, event -> {
    event.setPointerCapture();
    event.consume();
});
```

新增到接口的方法必须是 default method，避免直接破坏已经编译的第三方 Widget。没有注册新 listener 的控件继续走 legacy path。

### 8.3 事件对象

基础 `MuiEvent` 至少包含：

```text
type
timestamp
phase
target
currentTarget
screen
panel
handled
defaultPrevented
propagationStopped
immediatePropagationStopped
```

方法：

```text
markHandled()
preventDefault()
stopPropagation()
stopImmediatePropagation()
consume() = handled + preventDefault + stopPropagation
```

这几个状态必须分开：监听器可以观察并标记 handled，但允许祖先继续收到事件；也可以阻止默认按钮行为但不阻止 bubble。

`PointerEvent` 还包含：

```text
pointerId
pointerType
button
buttons
screenX/screenY
localX/localY（按 currentTarget 动态解析）
deltaX/deltaY
scrollX/scrollY
modifiers
clickCount
```

Minecraft 1.12 首版只有一个 mouse pointer，但捕获应按 `pointerId + button` 保存，以保留同时按住不同鼠标键的能力。

### 8.4 传播算法

对一个已选 target 构建不可变路径：

```text
Panel root → ... → target parent → target
```

调用顺序：

```text
CAPTURE: Panel root → target parent
TARGET:  target capture listeners → target normal listeners
BUBBLE:  target parent → Panel root
DEFAULT: 若未 preventDefault，执行框架/legacy 默认行为
```

规则：

- `stopPropagation()` 停止进入下一个 EventTarget，但允许当前 target 余下 listener 执行。
- `stopImmediatePropagation()` 同时停止当前 target 余下 listener。
- `preventDefault()` 不自动停止传播。
- listener 列表在进入一个 target 时取快照；分发中新增 listener 不参与本次事件。
- `once` listener 在第一次调用前先从 registry 移除，避免回调递归时再次执行。
- target 或 ancestor 在分发中 dispose 时，本次路径仍是快照，但后续 default action 必须检查有效性。
- 异常需要带 event type、phase、target path 记录，并沿用 ModularUI 当前 GUI error handling 策略。

事件只在一个 Panel 内沿 Widget 祖先传播，不跨越到下层 Panel。Overlay/Panel target 选择仍由 PanelManager 先完成。

### 8.5 Target 选择与 click-through 兼容

这是实施中最重要的兼容点。

浏览器使用唯一 target；当前 ModularUI 会在顶层 Widget 未处理且允许 click-through 时继续尝试其下面的重叠 Widget。不能直接把这两种模型混为一条不明确的管线。

建议提供 screen 级 dispatch mode：

```text
LEGACY  当前行为，兼容问题的逐 Screen opt-out
HYBRID  初始默认；新事件先分发，未 preventDefault 时执行 legacy default action
DOM     只使用唯一 target 和 DOM 默认行为
```

迁移步骤：

1. 每个现有 Screen 自动把 Java Widget 树 adoption 为 DOM，默认使用 `HYBRID`。
2. 没有新 listener 时，HYBRID 必须严格回归为当前 legacy default action 行为。
3. 标准 Button、TextField、Slider、Scroll、Slot 逐个把 legacy 回调变成 default action。
4. 发现无法立即修复的第三方分发兼容问题时，允许该 Screen 显式退回 `LEGACY` 并输出诊断。
5. `IGuiAction` 标记 deprecated，但至少保留一个完整兼容周期。
6. 标准控件和第三方验证完成后再评审 DOM-only；不自动切换已有 Screen。

HYBRID 模式下只对最上层有效 target 运行一次 capture/target/bubble。若没有新 listener 阻止默认行为，再进入原有 legacy dispatcher；不能针对每一个 click-through 候选重复运行祖先 capture/bubble。

长期可以增加类似 CSS `pointer-events` 的策略：

```text
AUTO
NONE
CHILDREN_ONLY
SELF_ONLY
```

它比继续扩大 `canClickThrough()` 更符合唯一 target 模型。

### 8.6 Pointer capture

新增显式 API：

```text
event.setPointerCapture()
event.releasePointerCapture()
widget.hasPointerCapture(pointerId, button)
```

行为：

- capture 后 MOVE/DRAG/UP 送到 capture owner，即使指针离开 bounds。
- capture 只改变 target，不跳过其 ancestor capture/bubble path。
- UP 后自动释放。
- Widget dispose、Panel 关闭、Screen 关闭、窗口失焦时释放，并尽可能发送 `POINTER_CANCEL`。
- keyboard focus 与 pointer capture 完全分开。
- Modal 打开时，旧 capture owner 若位于被 Modal 隔离的下层 Panel，应收到 cancel 并释放。

### 8.7 Focus 和键盘

键盘 target：

1. 有效 focused Widget。
2. 否则当前 top/modal Panel root。
3. 否则 main Panel root。

沿 target path 执行 capture/target/bubble。文本输入和组合输入不得经过 RTS 全局热键 default action。

焦点变化建议产生：

- `FOCUS_IN`/`FOCUS_OUT`：bubble。
- `FOCUS`/`BLUR`：不 bubble。

初期可以继续由 `IFocusedWidget` 执行默认 focus 行为，新事件只提供可观察路径。后续再统一 focus controller。

ESC 应作为可取消的 KeyEvent 先分发；如果未被处理/阻止，默认行为才是关闭顶层 Panel。这样 Modal、轮盘或正在编辑的控件可以比关闭 Screen 更早处理一次 BACK。

### 8.8 Hover、pointer move 和 enter/leave

当前 hover 回调没有事件对象，且不是祖先传播模型。建议：

- `POINTER_MOVE` 正常 bubble。
- `POINTER_OVER`/`POINTER_OUT` bubble。
- `POINTER_ENTER`/`POINTER_LEAVE` 不 bubble，并按 old/new target path 差集生成。
- 保留旧 `onMouseStartHover` 等方法作为默认兼容回调。

需要复用每帧已有 hit-test 结果，不能为每类 hover 事件重新遍历 Widget 树。

### 8.9 Navigation Action

ModularUI fork 已有 device-neutral navigation API。DOM 事件实施后，Navigation dispatcher 不应长期伪造 mouse press/release；更合适的是分发可取消的 `ActionEvent`：

```text
ACTIVATE
SECONDARY
INCREMENT
DECREMENT
SCROLL
PAGE
QUICK_MOVE
```

若没有新 listener 处理，再执行当前 `INavigationActionHandler`/`Interactable` 默认行为。这让键鼠、手柄和未来触摸输入共享业务 Action，同时保留各自原始输入事件。

## 9. ModularUI 建议修改位置

主要改动点：

- `api/widget/IWidget.java`：默认 EventTarget API。
- `widget/AbstractWidget.java`：listener registry 和 dispose 清理。
- `widget/Widget.java`：fluent listener builder；逐步替代 `listenGuiAction`。
- `screen/ClientScreenHandler.java`：把 LWJGL/Forge 原始输入转换成结构化事件。
- `screen/ModularScreen.java`：统一 dispatcher 入口和 dispatch mode。
- `screen/ModularPanel.java`：legacy default action、modal boundary 和 focus default。
- `screen/PanelManager.java`：选择目标 Panel、命中目标和 capture 失效处理。
- `screen/viewport/ModularGuiContext.java`：复用 hover/hit-test snapshot。
- `screen/viewport/LocatedWidget.java`：内部构建 target path 及每层 local coordinate transform。
- `screen/navigation/ModularNavigationDispatcher.java`：后期接入 `ActionEvent`。

不要把 DOM dispatcher 实现在 UIE。UIE 可以消费 ModularUI 提供的公共事件/导航语义，但 Widget 树、变换、默认行为和命中规则必须由 ModularUI 自己拥有。

## 10. 测试计划

### 10.1 纯事件单元测试

- capture/target/bubble 顺序。
- target capture listener 与普通 listener 的顺序。
- `stopPropagation` 和 `stopImmediatePropagation`。
- `preventDefault` 不停止 bubble。
- `consume` 的组合语义。
- listener priority、once、移除和 dispatch 中注册。
- dispatch 中 Widget dispose/reparent。
- 嵌套 transform 下每个 currentTarget 的 local coordinate。
- 多鼠标键 capture、释放和 cancel。
- focused keyboard target。
- Modal Panel 隔离。
- ESC default action。

### 10.2 Legacy/HYBRID 回归测试

- 原有 Button press/tap 只触发一次。
- TextField 焦点和字符输入不回归。
- Slider drag 离开 bounds 后继续。
- Scroll 到边界后的旧 pass-through 行为不变。
- 重叠 Widget 和 `canClickThrough()` 行为不变。
- Slot、carried stack、JEI ghost ingredient drag 不回归。
- Context menu 外部点击关闭。
- Panel drag/resize。
- OverlayStack 与普通 Screen 的优先级。
- Navigation Action 仍只执行一次。

### 10.3 RTS 验收场景

- 点击浮动窗口时不触发世界操作。
- Modal 外部区域阻断 `RtsWorldViewport`。
- 在世界区域 press 后拖到窗口上，capture owner 仍得到 drag/up。
- 搜索框聚焦时 WASD 输入文本，不移动相机。
- 搜索框失焦后 WASD 控制相机。
- 右键旋转、中键平移、滚轮缩放与 UI 控件竞争正确。
- GUI scale、窗口 resize 和 shader viewport 下 screen ray 一致。
- 手柄导航和世界相机 Action 不同时获得所有权。

### 10.4 性能门槛

- 一次输入只复用一次 hit-test snapshot。
- pointer move 不重复构造整棵 Widget tree snapshot。
- path 长度与 target 深度成正比，不扫描无关分支。
- 首版不池化会暴露给 listener 的可变 Event 对象，先保证生命周期正确；用 benchmark 证明分配成为瓶颈后再优化。

### 10.5 XML markup 测试

- parser 禁用 DTD、external entity 和 XInclude，并对畸形 XML 给出确定错误。
- 文档大小、深度、节点数、单节点属性数和 include 深度限制。
- namespace/tag/attribute 未注册、重复 id、非法枚举和类型转换错误。
- 错误包含 resource location、行列号和 node path。
- 同一模板重复编译得到相同的 Widget 结构和稳定 id。
- action、binding、事件 listener 的解析、类型检查和 controller 缺项错误。
- include/template 的循环检测、参数作用域和 namespace 隔离。
- XML component 的 typed props、默认值、named/default slot、局部 id/style scope 和 custom event。
- XML component 递归组合、循环依赖、最大实例深度和单文档实例数限制。
- 同一个 component module 只加载一次，每个 element instance 拥有独立 lifecycle state，不能创建独立 Graal context。
- addon 自定义标签不能覆盖冻结后的 ModularUI 核心标签。
- resource reload 后 cache 失效；已打开 Screen 的安全 rebuild 不遗留 focus、pointer capture 或 listener。
- XML 构建的 Button、TextField、Scroll、Modal 和 `RtsWorldViewport` 与等价 Java builder 行为一致。

### 10.6 GraalJS 和跨线程测试

- Graal context 只能在其 script scheduler 所在线程 enter/execute，render thread 不直接调用 `Value.execute()`。
- input/domain snapshot 到 script event queue，再到 mutation/command batch 的顺序和背压。
- render thread 不等待 JS；延迟、超时或关闭的 context 不阻塞 frame。
- statement limit、单 turn wall-time watchdog、队列长度、DOM node 数和 module 大小限制。
- context 超限后强制关闭、清理 listener/promise/timer，并显示可诊断的失败页。
- `HostAccess`、host class lookup、IO、network、native access、thread 和 polyglot access 全部拒绝测试。
- manifest capability 只能请求 Java owner 预先批准的子集，不能自授予权限。
- 服务端 resource pack 和未信任 resource origin 不能执行 JS。
- mutation batch 在安全帧原子应用；失败 batch 不留下半更新 Widget tree。
- 多个 document/context 之间不能共享 JS object、listener 或 capability handle。

## 11. 实施顺序

### Phase 0：冻结行为和测试基线

- 为当前 Interactable、focus、Panel、click-through 和 drag 行为补回归测试。
- 记录 `ClientScreenHandler → ModularScreen → ModularPanel → Widget` 的所有入口。
- 确认 Forge event cancellation、JEI 和 OverlayStack 的边界。

### Phase 1：只读事件路径

- 新增公共 Event 类型、listener registry 和 target path。
- 支持 capture/target/bubble，但只作为观察通道。
- 传统 Java Widget 树自动 adoption，未知第三方 Widget 使用 opaque element。
- 默认 HYBRID；没有 listener 时继续执行完全相同的 legacy default action。

### Phase 2：取消、默认行为和 pointer capture

- 实现 stop/prevent/consume。
- 实现显式 pointer capture 和 cancel。
- 引入 HYBRID mode。
- 将旧 Interactable 调用封装成 legacy default action。

### Phase 3：迁移标准 Widget

- Button。
- TextField/focus。
- Slider/drag。
- Scroll/Paged。
- Slot 和 recipe-viewer interaction。
- MenuPanel 和 Panel drag/resize。

每迁移一类都必须同时保留 LEGACY 测试。

### Phase 4：Navigation Action 统一

- Navigation dispatcher 发 `ActionEvent`。
- 未处理时调用旧 navigation/Interactable 默认行为。
- UIE ModularUI provider 只使用公共 API。

### Phase 5：client-only Document/DOM 原型

- 新增安全 parser、不可变 AST、validator、标签 factory registry 和 compiler。
- 首批只注册 panel、容器、text、button、image、scroll、textfield 等稳定核心标签。
- 新增持久 `MuiDocument`/`MuiElement`、query API、mutation queue 和 Widget render-tree bridge。
- 支持 XML composite component definition、typed props、slots、scoped style 和递归组合。
- 新增显式 `UiController`、`UiActionRegistry` 和 typed `UiBindingContext`，作为无脚本 fallback 和 native bridge。
- XML 事件属性接入已完成的 DOM 事件 API。
- 支持 addon namespace 和 RTS 自定义组件标签。
- 保留 Java builder 兼容入口；新动态页面以 XML document/component library 为主。
- 首个可运行切片先构建 client-only `ModularScreen`；随后必须在引入 GraalJS 前完成
  `ProtocolPlan + stable key + ordered slot table + fingerprint handshake`，再开放受约束的 synced/container XML。

### Phase 5B：SyncedTemplate 协议层

- common Java 8 compiler 从 mod-owned XML 生成 `MuiProtocolPlan`。
- server 构建协议 handler/slot table，client 构建同一协议 plan 和额外 RenderPlan。
- synced document 禁止 BFS `auto_sync`，协议节点全部使用显式 key/type/version/order。
- OpenGui 在 screen 激活前校验 protocol fingerprint，不一致直接拒绝。
- JS 只能动态修改非协议子树，不能决定 handler 或真实 slot 拓扑。

### Phase 6：GraalJS runtime 和 UI manifest

- 使用与 Java 25 对齐的 GraalJS 25.x Polyglot API，生产环境固定 `js.ecmascript-version=latest`。
- 增加 `mui.manifest.json`，注册 document、stylesheet、ES module、native component 依赖和 capability request。
- 一个共享 `Engine`，每个活动 document/app 一个隔离 `Context`，由独立 script scheduler 驱动。
- render thread 与 script thread 只交换不可变 event/state snapshot、mutation batch 和 host command/result。
- 实现 resource-only ES module loader、Promise/timer/microtask 调度和每帧 job budget。
- 实现严格 HostAccess、resource limits、origin/trust policy 和失败 context 回收。

### Phase 7：RTS vertical slice

- 独立 addon 启动。
- 建立 client/server 分离的 RTS application/domain event bus、subscription scope 和线程断言。
- 建立 Command → service/model commit → DomainEvent 的最小闭环。
- 用 XML 组合 `RtsBuilderScreen` 的稳定静态结构，并由 Java 实现透明 `RtsWorldViewport` 等业务组件。
- RTS camera provider/session。
- GUI logical coordinate screen ray。
- 服务端相机会话。
- 一个 vanilla 方块的放置和破坏闭环。

### Phase 8：RTS 功能扩展

- 仓储和大型快捷栏。
- Shape/Ultimine/批量任务。
- Workflow/history。
- Blueprint。
- Craft terminal。
- Progression/plugin。
- 按优先级逐个实现 1.12 compat。

## 12. 关键风险

### 12.1 事件重复执行

HYBRID 中新 listener 和 legacy default action 都可能触发业务。必须规定：新 listener 只做观察时不 `preventDefault`；接管行为时必须 `preventDefault`，并为每个 migrated Widget 写“一次且仅一次”测试。

### 12.2 click-through 与唯一 target 冲突

不能伪装两者完全等价。LEGACY 保留候选迭代；DOM 模式使用唯一 target 和 pointer-events policy；HYBRID 只在一次 DOM dispatch 后选择是否进入 legacy fallback。

### 12.3 变换和 local coordinate

不能只用 Widget `Area` 累加坐标。必须复用 ModularUI viewport/transform 栈，并为旋转、缩放、滚动和 scissor 下的 target path 写测试。

### 12.4 生命周期

Panel 关闭、Widget dispose、Screen 切换和窗口失焦必须清理 focus、pointer capture、pressed/tap state 和 listener。不得把失效 Widget 引用保存在全局表中。

### 12.5 第三方兼容

添加 default interface method 可以保持多数二进制兼容，但改变 `ModularScreen` 的分发顺序仍可能改变第三方行为。
因此 HYBRID 上线前必须先做无 listener golden regression，并保留逐 Screen `LEGACY` opt-out。传统 UI 要满足
DOM/event 能力，不能把 LEGACY 当成长期全局默认；第三方直接实现 `IWidget` 时由 weak-identity sidecar 和
opaque DOM adapter 提供基础能力，不要求它继承 MUI 的具体基类。

### 12.6 服务端安全

DOM 事件系统只决定客户端 UI 的意图路由，不构成权限边界。RTS 服务端仍必须验证：

- 玩家和相机会话身份。
- 维度、锚点和最大半径。
- 服务端射线/目标合理性。
- chunk 是否可访问。
- claims/teams 权限。
- 材料、工具、流体和合成配方。
- 每 tick 预算、请求大小和重放/重复 submission id。

### 12.7 JS host escape 和脚本注入

XML 不得写 Java 类名、构造函数或 setter 名。GraalJS 不得启用 `Java.type`、任意 host class lookup、filesystem、network、native access 或 thread creation；只暴露经过 capability registry 包装的 DOM 与业务接口。脚本来源按 origin/trust policy 校验，服务端资源包不能自动获得脚本执行权。仓库内已有的 EvalEx 不作为 UI 脚本引擎。

### 12.8 synced UI 树不一致

客户端资源包可以覆盖 asset，但 ModularUI container/sync handler 要求两端以一致顺序和 key 构建协议树。第一阶段禁止用 XML 构建 synced/container UI。后续若开放，必须使用不可被资源包覆盖的共同模板、稳定显式 sync key、确定性 factory、模板 fingerprint 握手和 fail-closed 校验。

### 12.9 热重载状态丢失

首版不实现浏览器 Virtual DOM diff。资源 reload 只重新解析、校验和替换 template cache；已打开 Screen 默认在关闭重开后生效。若开发期加入安全帧 rebuild，只按稳定 id 白名单恢复 scroll、focus 和 text draft，不能迁移任意 Widget 实例状态。

## 13. 当前决策和待确认项

已经确定：

- RTS 是独立 addon。
- 主 UI 使用 ModularUI，不使用 RTSUI 和 NFR HUD compositor。
- UI 目录参考 NFR 的 base/business/layouts/model/pages/views。
- UIE 只拥有相机、输入和跨框架导航平台。
- 可以按职责修改 ModularUI、NFR 和 UIE 的公共 API。
- DOM 风格事件必须在 ModularUI 内实现，并兼容现有 Interactable。
- RTS 拥有独立于 UI 的 typed application/domain event bus；它不负责输入传播或网络自动广播。
- XML UI 作为 ModularUI 浏览器式 Document runtime 的文档格式，挂载后的 render tree 仍使用普通 Widget。
- XML 页面可以由 GraalJS 完全动态修改；RTS native component 和 host capability 位于独立 addon。
- XML/JS 不允许反射或绕过 host capability，脚本不能直接伪造已提交的 domain event。
- GraalJS 使用 Polyglot API 和 `js.ecmascript-version=latest`，当前目标版本支持 ECMAScript 2026；生产环境不启用 `staging`。
- JS context 在独立 `MuiScriptScheduler` 线程执行，render thread 不等待 JS。
- JS 拥有逻辑 DOM；Widget/render tree、Minecraft 和 OpenGL 只归 client thread，通过不可变 event/mutation/command batch 通信。
- client application manifest 注册 XML、JSON stylesheet、ES module、native element 依赖和 capability request；
  synced protocol manifest/XML 必须位于 mod-owned common resource，不能由 resource pack 覆盖。
- XML 可以在 Java native primitive 上声明 composite component library，并由其他 XML 通过 namespaced tag 复用。
- 实施顺序先支持 client-only Screen，再通过 ProtocolPlan 开放受约束的 synced/container UI；
  GraalJS 不参与 server 构建，也不能改变同步协议拓扑。

建议采用、尚待实施验证：

- `LEGACY/HYBRID/DOM` 三种 dispatch mode。
- 新事件先作为 sidecar，再把旧回调迁移为 default action。
- 指针捕获按 `pointerId + button` 保存。
- Navigation 使用独立 `ActionEvent`，不再长期伪造鼠标点击。

第一轮实现前仍需决定：

- DOM API 的最终命名使用 `MuiEvent` 还是 `UiEvent`。
- listener 是否在首版支持 priority；capture/once 必须支持，passive 可以后置。
- `pointer-events` policy 是否随 Phase 1 一起加入，还是 DOM mode 前再加入。
- Screen dispatch mode 是构造参数、fluent setter，还是 UI settings 的一部分。
- API version 和 ModularUI fork 新版本号。
- XML 资源路径最终采用 `assets/<modid>/mui/*.xml` 还是 `assets/<modid>/ui/*.xml`。
- markup API 是 ModularUI 主 artifact 内的可选包，还是同仓库独立 artifact；1.12 部署复杂度下优先前者。
- GraalJS adapter/runtime 是 ModularUI 的独立 artifact/JAR，还是可选依赖打入主 JAR；优先独立 artifact，避免所有静态 UI 用户承担体积和启动成本。
- 全局单 script scheduler 与每活动 document 独立 executor 的最终选择；MVP 优先单 scheduler + context resource limit。
- 哪些 DOM event 属于 native immediate，哪些允许 script-deferred cancellation。
- component boundary 首版采用 scoped component root，还是实现完整 Shadow DOM；MVP 优先 scoped root + slot/event retargeting 子集。

## 14. 第一轮建议范围

第一轮不要同时实现 RTS。先在 ModularUI fork 完成一个受控原型：

1. `MuiEvent`、`PointerEvent`、phase、listener registry。
2. 对一个测试 Widget 树实现 capture/target/bubble。
3. 支持 stop、preventDefault 和显式 pointer capture。
4. 默认 HYBRID，验证无 listener 与当前行为一致；同时验证逐 Screen LEGACY opt-out。
5. 用 Button、TextField、重叠 click-through、Modal 和拖拽五类场景验证兼容。

原型通过后，再让 `RtsWorldViewport` 成为第一个真实 HYBRID 消费者。这样事件框架的设计由通用控件测试和 RTS 世界交互共同验证，但 RTS 不承担维护第二套事件系统。

第二轮让全部 Java-built Screen 自动 adoption 为 Document，并用同一组等价 Java/XML 页面测试
render-tree bridge。DOM mutation 稳定后再接 GraalJS 独立线程和 manifest；不要在 DOM 事件语义尚未稳定时同时固化 `on:*` XML/JS 契约。

## 15. ModularUI XML UI 可行性与设计

### 15.1 可行性结论和当前缺口

可以支持，但不是打开一个 parser 就能使用。当前 ModularUI fork 中没有 XML UI、声明式 Widget factory 或 binding registry：

- `ModularScreen` 接收 Java `Function<ModularGuiContext, ModularPanel>` 构建主 Panel。
- `ModularPanel`/`ParentWidget` 已提供真实父子树，Widget fluent API 提供布局和外观配置。
- theme 已通过 JSON resource 加载，`DrawableSerialization` 已有受控的 drawable type registry，可借鉴其注册方式。
- `IValue`、`ISyncOrValue` 和 typed sync handler 可作为 binding adapter 的底层能力。
- 当前不存在通用 `WidgetFactory`、XML schema、controller/action resolver、binding context 或 template cache。

因此 XML 是持久 `MuiDocument` 的初始 DOM 来源，而不只是一次性 Java builder：加载时解析、校验并 mount 到现有 `ModularPanel`/Widget render tree；运行中 GraalJS 可以增删节点、修改 attribute/class/property 和注册 listener，mutation 在安全帧批量同步到 Widget tree。它不替代 Java native component，也不另写布局和渲染器。

### 15.2 XML 的责任边界

| 内容 | XML | GraalJS | Java host/component | JSON style |
|---|---:|---:|---:|---:|
| 初始 DOM 层级、id 和 class | 是 | 可动态修改 | 可选 | 否 |
| DOM listener | action id 可选 | 是 | native listener | 否 |
| 动态节点、属性和状态 | 初始值 | 是 | native 状态 | selector 响应 |
| 业务命令和异步结果 | 否 | 调用 capability | 实现/验证 | 否 |
| native component 内部实现 | 否 | 通过 DOM API 使用 | 是 | 外观规则 |
| 颜色、drawable 和控件视觉状态 | 引用 | 修改 class/property | 注册 token | 是 |
| 任意 Java class/filesystem/network | 否 | 否 | 仅显式 capability | 否 |

XML 负责初始页面，GraalJS 负责类似浏览器 JavaScript 的动态行为，Java 负责 native component 和受控 host capability。例如 `RtsItemGrid`、`RtsWorldViewport` 仍是 Java 类，只是在 DOM 中有对应的 `<rts:item-grid>` 和 `<rts:world-viewport>` element。

### 15.3 构建管线

```text
ResourceLocation
  → secure StAX parser
  → immutable UiDocument/UiNode AST
  → namespace + schema validation
  → CompiledUiTemplate
  → persistent MuiDocument/MuiElement tree
  → JSON computed style + UiElementFactory registry
  → mounted ModularPanel/Widget render tree
  ↔ queued GraalJS events/mutations/host commands
```

parser 与 compiler 必须分开：parser 只认识 XML 结构和 source location，不创建 Widget；compiler 做 tag/attribute 类型校验并生成可缓存 template；mount 阶段建立持久 element 与 client Widget 的映射。GraalJS 只能操作 document/element proxy，不能拿到底层 Widget 或 Minecraft object。

建议公共 API：

```text
com.cleanroommc.modularui.api.markup
  UiDocument
  UiNode
  UiSourceLocation
  UiElementType
  UiElementFactory
  UiElementRegistry
  UiAttributeCodec
  UiBuildContext
  UiController
  UiActionRegistry
  UiBindingContext
  UiTemplate
  UiDocumentLoader
```

建议内部实现：

```text
com.cleanroommc.modularui.markup
  XmlUiParser
  XmlUiCompiler
  XmlUiValidator
  CompiledUiTemplate
  ResourceUiDocumentLoader
  MarkupReloadListener
```

AST 和诊断类型应保持 common-side 可加载；实际 Widget factory 和 `ModularScreen` 实例化入口可以是 client-only。这样不会让 dedicated server 因验证 API 的类签名而加载 Minecraft client class。

### 15.4 XML 形态

建议资源位于 `assets/<modid>/mui/*.xml`，并强制使用 namespace：

```xml
<?xml version="1.0" encoding="UTF-8"?>
<mui:screen xmlns:mui="urn:modularui:1"
            xmlns:on="urn:modularui:event:1"
            xmlns:bind="urn:modularui:binding:1"
            xmlns:rts="urn:neofontrender:rts:1"
            id="builder"
            theme="rts_modern">
    <mui:panel id="root" width="100%" height="100%" background="none">
        <rts:world-viewport id="world"/>
        <rts:top-command-bar id="top"/>
        <rts:item-grid id="materials"
                       bind:items="storage.entries"
                       on:activate="select_material"/>
    </mui:panel>
</mui:screen>
```

namespace URI 解析到注册 owner/version，标签解析到显式 `UiElementType`。XML 中不能出现 Java fully-qualified class name。未知 namespace、未知标签和未知属性默认都是编译错误，不静默忽略。

首版只允许一个 `screen` 根和一个主 `panel`。浮动 Panel 可以由 controller 根据 action 打开预编译的另一个 template；不要让 XML 自己操作全局 PanelManager。

### 15.5 Factory 和 addon 扩展

每种标签由显式 factory 注册：

```java
registry.register(RtsMarkup.NAMESPACE, "item-grid", (node, context) ->
        new RtsItemGrid(context.requireBinding(node, "items", RtsItemList.class)));
```

真实 API 不要求采用该确切签名，但必须满足：

- factory 声明可接受的 attributes、child content model、结果类型和 side。
- attribute 经 typed codec 转为 `int`、`boolean`、enum、颜色、尺寸或 `ResourceLocation`，不由每个 factory 随意解析。
- schema 由 factory/attribute descriptor 在 Java 中注册，不加载外部 XSD。
- 通用 id/layout/visibility/theme attributes 由 compiler 统一处理；组件属性由其 factory 处理。
- parent factory 决定是否接受 children，以及 children 加入哪个 slot。
- 注册 key 包含 namespace，RTS 只能注册自己的 namespace。
- 核心和 addon 注册完成后 registry freeze；重复 key 和 freeze 后注册直接失败。
- resource reload 只能重新编译 document，不能改变 factory registry。

这允许 NFR 将真正通用的组件注册为 `nfr:*`，RTS 注册业务组件为 `rts:*`，但依赖方向仍是 RTS → NFR/ModularUI，不需要把 RTS 代码放入其中。

### 15.6 Controller、action 与 DOM 事件

XML 事件属性的值只是 action id：

```xml
<mui:button id="confirm" on:activate="confirm_build"/>
<rts:world-viewport on:pointer-down="begin_world_gesture"/>
```

`UiController` 可以在创建 Screen 时显式注册这些 action，作为定型 UI、native 行为和无脚本 fallback。动态页面还可由 ES module 使用 `addEventListener` 注册 JS function。两种入口进入同一 DOM listener 表，但 Java listener 在 render/native 线程域，JS listener 通过 event snapshot 投递到 script scheduler。需要 capture 时使用 listener option 或受支持的声明式事件名。

Java action handler 接收受限的 `UiActionContext`；JS handler 接收不可变/受生命周期约束的 DOM Event proxy。二者都不能通过字符串反射访问 Java。RTS action 的正常路径仍然是：

```text
XML/JS listener → host capability/RtsCommandGateway → application service
                → committed model state → RtsDomainEvent → state snapshot/DOM refresh
```

XML action 不得直接 publish `RtsDomainEvent`，否则会绕过 command 的唯一处理者、权限验证和失败结果。

DOM 事件 API 应先稳定再公开 XML `on:*` 契约。旧 `Interactable` callback 可以由 factory 适配，但 XML 不应同时绑定新 listener 和同一 legacy callback，避免一次操作执行两次。

### 15.7 Typed binding

建议用独立 `bind:` namespace 区分静态 attribute 与动态 value：

```xml
<mui:text bind:text="selection.display_name"/>
<mui:text-field bind:value="search.query"/>
<rts:item-grid bind:items="storage.entries"/>
```

`UiBindingContext` 是显式 map：`binding id → typed getter/setter/IValue adapter`，用于 Java controller 和无脚本页面。GraalJS 页面可直接维护 JS state 并通过 element property/mutation 更新 UI，也可以订阅 host capability 发布的只读 state snapshot。

约束如下：

- 不支持任意 Java property path；点号只是 opaque binding id 的命名约定。
- XML attribute 本身不执行 inline JavaScript；复杂逻辑放在 manifest 注册的 ES module。
- one-way/two-way 由该元素属性的 binding contract 决定，不能由任意字符串切换。
- 文本格式化、列表过滤和派生状态在 Java view-model/provider 中完成。
- listener subscription 由 Screen/Panel scope 持有并在 dispose 时统一释放。
- JS/model 不持有 Widget 引用，底层 Widget 也不直接订阅全局 domain bus。

XML 编译层本身不增加 `if`/`for` 模板语言；GraalJS 使用标准 DOM API 动态增删树。大型虚拟化列表仍由 `RtsItemGrid`、`ListWidget` 或专用 Java native component 管理，避免脚本创建数万个独立 Widget。

### 15.8 布局、theme 和 drawable

XML 不应再实现 CSS。width/height、anchor、margin、padding、grow 等通用属性只映射现有 ModularUI sizer/layout API；支持集合以当前 API 能稳定表达和测试的能力为准。

theme 继续使用现有 JSON：

- XML 的 `theme`/`style` 只引用已注册 theme 或 widget theme key。
- drawable 优先引用 theme、texture/resource id 或现有受控 drawable serialization。
- 不在 XML attribute 内嵌 JSON drawable。
- RTS 视觉 token 和 Widget theme 放在 RTS resource 中，不污染 NFR 默认 theme。

这样资源职责明确，也避免维护 XML style 与 JSON theme 两套级联规则。

### 15.9 XML component、template 和 include

XML component 是核心能力，不只是静态 include。它允许在 Java native primitive 上定义新的 namespaced element，并由其他 XML 像普通标签一样实例化。component 支持 typed props、default/named slot、局部 style scope、custom event 和可选 ES module controller；详细模型见第 17 节。

纯片段 include 仍可支持：

```xml
<mui:include src="neofontrender_rts:mui/common/top_bar.xml"/>
```

只允许 `ResourceLocation`，禁止 filesystem path、URL 和 classpath 任意访问。compiler 维护 component/include dependency graph，检测循环并限制最大深度和实例数。template 参数只能传 literal、property 或 binding id，不能通过 XML 执行 inline JavaScript。

复用优先级调整为：底层输入/渲染/高性能能力做 Java native element；常规可复用 UI 组合优先做 XML component；只复用一次的静态片段才用 include。不要用大量 include 模拟组件、继承或宏语言。

### 15.10 Resource reload 和状态恢复

推荐 cache key 至少包含 resource id、resource manager generation 和 registry/schema version。reload listener 在资源准备阶段 parse/validate/compile，主线程 apply 阶段原子替换 template cache；失败时保留上一份有效 cache 并报告完整诊断。

首版 reload 行为：

1. 修改资源后重新加载并更新 template cache。
2. 已打开 Screen 不原地 patch，关闭重开后使用新 template。
3. 开发期可增加“下一安全帧完整 rebuild”，但必须先取消 pointer capture、清理 focus/listener，再替换树。
4. 只通过稳定 id 恢复明确允许的 scroll position、focus 和未提交 text draft。

不实现浏览器式 Virtual DOM、diff/reconciliation 或任意 Widget 实例复用。这些机制会放大 1.12 渲染生命周期、sync handler 和第三方 Widget 的不确定性，对 RTS 首版没有必要。

### 15.11 Client-only 与 synced/container UI

| 能力 | 第一阶段 | 后续前提 |
|---|---|---|
| client-only `ModularScreen` 静态树 | 支持 | 正常 resource reload |
| controller action/value binding | 支持 | 显式 registry 和 lifecycle scope |
| addon 自定义 Widget 标签 | 支持 | namespace 隔离和 side 声明 |
| client-only 动态列表组件 | 支持 | 组件自己维护 children |
| server-backed command/packet | 支持 | handler 显式发送，服务端重验 |
| synced/container Widget 树 | 不支持 | 确定性协议模板和 fingerprint |
| resource pack 覆盖 synced 模板 | 禁止 | 原则上继续禁止 |

synced UI 不能直接沿用 client asset 语义，因为资源包可能让客户端 XML 与服务端构建结果不同，而 sync handler/slot 注册依赖一致的 key、顺序和数量。若未来确实要支持：

- 模板必须来自双方相同的 mod protocol resource，不允许 resource pack 覆盖。
- 动态 Widget 和 sync handler 使用显式稳定 key，不能依赖遍历序号。
- 所有 factory 必须声明 common/client/server 可用性并保证确定性。
- 打开容器前交换 compiled template fingerprint；不一致则拒绝打开并给出版本错误。
- server action、slot 和 sync handler 仍由 Java controller 提供，XML 不能创建任意网络入口。

在这些规则和测试齐全前，RTS 的 container/sync 部分继续使用 Java 构建，XML 只组合客户端主界面。

### 15.12 Parser 安全和错误诊断

JDK StAX 足够，不需要新增重量级 XML framework。parser 必须：

- 禁用 DTD、external general/parameter entities 和外部 schema 获取。
- 不支持 XInclude。
- 限制单文件字节数、最大深度、节点总数、单节点属性数和字符串长度。
- 限制 include 总数/深度，并检测循环。
- 拒绝重复 id、保留 namespace、未知 attribute 和不合法 child。
- 对 resource pack 输入按不可信数据处理。

每条错误至少包含：resource location、行号、列号、node path、element/attribute、期望类型或允许值。示例：

```text
neofontrender_rts:mui/builder.xml:18:27
/screen/panel/item-grid[@id='materials']
unknown binding 'storage.entry'; expected RtsItemList for bind:items
```

开发环境可聚合多个独立 validation error；运行环境 fail closed，不用部分构建的 Widget 树继续打开 Screen。

### 15.13 RTS 中的组织方式

建议 addon 内保持 NFR 主模组式分层：

```text
neofontrender.addons.rts.client.gui
  component/base       Java 通用 RTS Widget
  component/business   仓储、蓝图、workflow 等业务组件
  markup               RTS namespace/factory 注册
  controller           页面 action 和 binding 装配
  model                UI session/view model
  pages                 Screen/Panel 生命周期入口
  views                 Java 动态视图和复杂组合

assets/neofontrender_rts/mui
  builder.xml
  windows/
  dialogs/
  common/
```

`RtsBuilderScreen` 的 Java 入口负责创建 model/controller/build context，再加载 `builder.xml`。XML 文件不获得 service locator；controller 只把该页面实际需要的 command gateway、bindings 和 action 暴露进去。

### 15.14 XML MVP 验收范围

Document/DOM 与 GraalJS runtime 达到以下范围即可进入 RTS vertical slice：

1. 安全解析一个 client-only Screen/Panel 静态树。
2. 支持稳定 id、常用尺寸/定位、theme 引用和基础 Widget 标签。
3. 支持 JS `createElement`、append/remove/replace、attribute/class/property 和 event listener。
4. 支持 `on:*` Java action、typed `bind:*` 作为无脚本 fallback。
5. RTS 能注册 namespace，并从 XML 创建 `RtsWorldViewport` 与一个业务列表组件。
6. Java builder 与 XML/JS 构建的基准页面在布局、事件、focus 和 dispose 上行为一致。
7. reload cache、跨线程调度、诊断和安全限制有自动化测试。

明确不在 MVP 中：synced/container XML、完整 CSS、Java 反射、Node.js API、任意 filesystem/network、Virtual DOM framework 和保状态脚本热替换。

## 16. GraalJS 动态 UI Runtime

### 16.1 引擎选型

目标 Java 25 下可以使用 GraalJS，并且它是当前最符合“尽可能新的 JavaScript”的 JVM 方案。以 2026-08-29 可用版本为准：

- Maven Central 最新 `25.3.4.1`。
- GraalVM 25.3 文档声明兼容 ECMAScript 2026。
- `js.ecmascript-version=latest` 使用当前引擎支持的最新正式规范和已完成提案，也是默认值。
- `staging` 会启用受支持但尚未完成的提案，只允许开发诊断 opt-in，不能作为发布默认。

依赖使用 Polyglot API，不使用兼容性的 `javax.script`/`js-scriptengine`：

```gradle
def graalJsVersion = "25.3.4.1"

implementation("org.graalvm.polyglot:polyglot:${graalJsVersion}")
runtimeOnly("org.graalvm.polyglot:js:${graalJsVersion}")
```

`org.graalvm.polyglot:js-community` 在当前 POM 中已标记 deprecated，应使用 `org.graalvm.polyglot:js`。实际集成前需要验证 Cleanroom dev/runtime、ShadowJar service/resource merge、包体积和非 GraalVM OpenJDK 25 上的执行模式；不要直接 relocate Graal/Truffle 包后假设其 service discovery 仍正常。

引擎版本必须在构建中固定。manifest 可以声明最低 JS runtime/API version，但不能自己要求下载或替换 engine。升级 GraalJS 与升级 `MuiDomApi` 分开版本化，使新语言语法不迫使所有 UI host API 同步破坏性升级。

### 16.2 GraalJS 不等于浏览器或 Node.js

GraalJS 提供 ECMAScript language runtime，不自动提供 HTML DOM、CSSOM、`window`、`fetch`、WebSocket、localStorage、Node.js `fs/process` 或 npm module resolution。这些能力由 ModularUI 有选择地实现：

```text
ECMAScript syntax/runtime     GraalJS
document/element/event        ModularUI DOM API
style/computed style          ModularUI JSON stylesheet engine
render/layout/focus/input     ModularUI Widget runtime
game/mod functionality        Java capability providers
module loading                ResourceLocation-only loader
```

“支持最新 JS”只承诺语言级别；Web API 由 `MuiDomApi` 单独做版本和兼容矩阵，不能宣称完整浏览器兼容。

### 16.3 UI 注册 manifest

额外 JSON 注册文件是合适的。建议每个提供 UI 的 mod 发布：

```text
assets/<modid>/mui/manifest.json
```

示例：

```json
{
  "format": 1,
  "applications": {
    "rts_builder": {
      "document": "neofontrender_rts:mui/builder.xml",
      "styles": [
        "neofontrender_rts:mui/common.style.json",
        "neofontrender_rts:mui/builder.style.json"
      ],
      "scripts": [
        {
          "src": "neofontrender_rts:mui/builder.mjs",
          "type": "module"
        }
      ],
      "nativeElements": [
        "rts:world-viewport",
        "rts:item-grid"
      ],
      "capabilities": [
        "rts:camera",
        "rts:commands",
        "rts:storage.read"
      ]
    }
  }
}
```

职责：

- `manifest.json`：application/screen 注册、入口文档、stylesheet、module、native element 依赖和 capability request。
- `*.xml`：初始 DOM。
- `*.style.json`：selector 和视觉/布局规则，底层继续复用现有 theme/drawable。
- `*.mjs`：动态行为、DOM mutation、event listener 和 host capability 调用。
- Java mod init：实际注册 native element factory 和 capability provider。

manifest 的 `capabilities` 只是请求列表。Java owner/policy 先注册“该 origin 最多允许什么”，runtime 只授予二者交集；JSON 不能给自己增加 camera、network 或 command 权限。重复 application id、缺失资源、未知 native element 或 API version 不兼容都应在 resource reload 阶段报错。

### 16.4 独立脚本线程模型

GraalJS 不在 Minecraft client/render thread 执行。推荐所有活动 document 共用一个有界 `MuiScriptScheduler`，每个 application/document 拥有独立 `Context`，context 同一时刻只由 scheduler 线程进入。

```text
Minecraft client/render thread                 MuiScriptScheduler thread

input + Widget hit test
  → immutable UiEventPacket        ─────────→  DOM event dispatch
                                                JS listener/microtask
                                                logical DOM mutation
                                                host command request
  ← UiMutationBatch/CommandBatch   ←─────────  end of script turn
safe frame apply
layout/render/native default action
  → immutable state/result         ─────────→  Promise/state event
```

线程所有权：

- script thread：Graal `Context`、JS object、JS listener、timer/microtask queue、逻辑 DOM mutation transaction。
- client/render thread：Widget/render tree、layout、hit-test、focus、pointer capture、Minecraft/Forge API 和所有 OpenGL 调用。
- resource reload worker：XML/JSON/module 字节读取、parse 和静态 validation；不执行 JS，不创建 Widget。

两侧不能共享可变 `Element`、Widget、Minecraft object 或 Polyglot `Value`。跨线程只传 immutable DTO、stable element id、document generation 和有上限的 payload。

### 16.5 与浏览器事件语义的差异

真正浏览器中 DOM JavaScript 和大量默认行为通常位于同一 renderer main thread；完全搬到独立线程后，Minecraft client thread 不能同步等待 JS，否则死循环仍会冻结游戏。因此必须明确两类事件：

#### Native immediate event

TextField 编辑、IME、focus、scroll physics、Panel drag/resize、pointer capture、连续相机手势等由 Java native element 在 client thread 立即执行。JS 收到状态/event snapshot，但不能追溯取消已经执行的 native 操作。

#### Script-deferred action

`ACTIVATE`、确认、切换页面、提交建造、打开窗口等高层 action 先投递脚本线程；JS 的 `preventDefault()`、`stopPropagation()` 和 mutation/command 在后续安全帧提交。通常增加一帧延迟，但不会阻塞 render thread。

每种 event type 必须在 schema 中声明 `dispatchDomain` 和 `defaultActionPolicy`，不能让脚本猜测是否可同步取消。高频 `pointermove`/scroll 可按 document/target 合并，press/release/key/action 不得静默丢失。

如果未来要求某个 JS listener 具有严格同步浏览器语义，只能让它进入 client thread 并承担卡死风险；本方案明确不提供这种模式。需要零延迟的逻辑写成受控 Java native component。

### 16.6 DOM mutation 和 render-tree bridge

JS 操作的是逻辑 DOM：

```js
const list = document.querySelector("#materials");
const row = document.createElementNS("urn:modularui:1", "mui:panel");
row.classList.add("material-row");
row.setAttribute("data-key", entry.key);
list.appendChild(row);
```

一次 script turn 中的修改先写入 transaction，结束后生成 `UiMutationBatch`。client thread 在安全帧验证 document generation、node limit、parent/child schema 和 native factory，然后原子 apply：

- create/remove/move/replace element。
- set/remove attribute。
- class/state/property update。
- listener metadata update。
- text/content update。

失败 batch 整批拒绝，并把结构化异常送回对应 Promise/error event，不能让 logical DOM 与 Widget tree 各成功一半。对高频相同 property mutation 做 last-write-wins 合并。

这不是 Virtual DOM framework。DOM 自身就是 source of truth，Widget tree 是 native render projection；后续可以在 JS 上层实现组件框架，但 ModularUI core 不内置 React 式 reconciliation。

### 16.7 Java host capability

JS 不直接拿 `Minecraft`、player、world、Widget、event bus 或 network channel。Java addon 注册最小 capability：

```text
rts:camera         readSnapshot(), requestMode(), setIntent()
rts:commands       submit(commandDto) -> Promise<ResultDto>
rts:storage.read   query(filterDto) -> Promise<PageDto>
mui:clipboard      readText()/writeText()，需要显式策略
```

返回值只允许 primitive、immutable DTO、list/map snapshot 或受控 proxy。改变游戏状态的调用返回 Promise，并在服务端继续做权限和数据验证。脚本只能发送 Command，不能直接发布伪造的 `RtsDomainEvent`。

推荐 Context policy 至少包括：

```text
HostAccess.EXPLICIT 或更严格的 scoped/proxy policy
host class lookup = deny all
polyglot access = none
native access = false
thread creation = false
environment access = none
IO = false，ES module 由受控 ResourceLocation loader 提供
```

优先用 `ProxyObject`/`ProxyExecutable` 构建 API surface，而不是向 JS 暴露带大量 public method 的普通 Java object。

### 16.8 ES module 和最新语法

脚本入口使用 `.mjs`/ES module，不使用全局拼接脚本。生产配置显式设置：

```java
Context.newBuilder("js")
        .option("js.ecmascript-version", "latest");
```

module resolver 只接受 manifest 或 module graph 中允许的 `ResourceLocation`。禁止相对路径逃逸、绝对 filesystem path、HTTP URL 和 Node built-in module。两种实现路线：

1. 构建时把 UI JS bundle 成单个 ESM，运行时只加载一个可信 resource；首版优先。
2. 实现只读 ResourceLocation module filesystem/resolver；需要动态 addon import 时再加入。

标准 `Promise`、`async/await` 和 module syntax 由 GraalJS 提供；`setTimeout`、`requestAnimationFrame`、`queueMicrotask`、console 和 host Promise completion 由 `MuiScriptScheduler` 接入。每帧只执行有限 job，剩余任务延后，不能无限 drain microtask queue。

### 16.9 JSON stylesheet

现有 ModularUI theme JSON 可以继续作为 drawable/widget theme 后端，但它还不是浏览器式 stylesheet。新增 `*.style.json` 只实现受控 selector/cascade：

```json
{
  "format": 1,
  "rules": [
    {
      "selector": "rts|item-grid.materials",
      "style": {
        "theme": "rts_item_grid",
        "gap": 2
      }
    },
    {
      "selector": ".material-row:selected",
      "style": {
        "theme": "rts_item_row_selected"
      }
    }
  ]
}
```

MVP selector 仅支持 namespace/type、`#id`、`.class` 和有限 pseudo-state；不支持任意 CSS parser、复杂 sibling selector、脚本表达式或 CSS layout。JS 通过 class/property/state 触发 style invalidation，computed style 最终映射到现有 ModularUI layout/theme API。

### 16.10 Origin、信任与资源包

脚本执行权不能等同于普通资源覆盖权：

- 安装在 mod JAR 中并由该 mod Java bootstrap 注册的 module：可执行，仍受 capability sandbox。
- 本地用户目录脚本：默认关闭，可由用户显式信任 origin/hash。
- 普通 resource pack：可覆盖 XML/stylesheet/texture，但默认不能新增或替换 executable module。
- 服务器下发 resource pack/module：永不自动执行。
- manifest、XML 或 style 引用脚本不能绕过 origin policy。

即使安装 mod 本身已经能执行 Java，也应维持此边界，因为 UI resource reload、服务器资源和用户主题的信任模型与安装代码不同。

### 16.11 执行预算和故障隔离

每个 context 至少设置：

- `ResourceLimits.statementLimit(...)`。
- 单 turn wall-time watchdog；超时从控制线程 interrupt/force-close context。
- 最大 module/source 字节数、DOM node 数、mutation 数、事件队列长度、timer 数和 host request 数。
- pointermove 等可合并事件的背压策略。
- host DTO 深度、集合长度和字符串长度限制。

render thread 永不等待 context。context 超限或抛出未处理 fatal error 后：

1. 标记 document script-failed，停止投递新 JS event。
2. 取消 timer、pending Promise/host request 和 listener handle。
3. 关闭该 context，不影响其他 document。
4. 保留最后一份有效 native render tree或显示错误 Panel。
5. 输出 module URI、行列、stack、document id 和 trace id。

共享 `Engine` 用于代码缓存；隔离和销毁单位是 `Context`。不要让不同 mod/origin 共用一个 global JS realm。

## 17. XML Component Library

### 17.1 可以由 XML 定义新组件

XML 不仅写 Screen，还应能在 Java native element 上定义可复用的 composite element：

```text
Java native primitives
  mui:panel / mui:text / mui:image / mui:button / mui:scroll / mui:text-field
  rts:world-viewport / rts:virtual-item-grid
            ↓ compose
XML component library
  nfr:icon-button / rts:mode-button / rts:window / rts:toolbar
            ↓ compose
XML application document
  rts_builder / storage / blueprint / settings
```

XML component 是运行时注册的新 element type，不生成 Java class。它具有稳定 qualified name、typed props、template、slot、局部 stylesheet、custom event 和可选 JS lifecycle/controller。component 也可以组合其他 XML component。

### 17.2 Component 文件格式

建议一个 component 一个 XML resource：

```xml
<?xml version="1.0" encoding="UTF-8"?>
<mui:component xmlns:mui="urn:modularui:1"
               xmlns:bind="urn:modularui:binding:1"
               name="rts:icon-button">
    <mui:props>
        <mui:prop name="icon" type="resource" required="true"/>
        <mui:prop name="label" type="text" default=""/>
        <mui:prop name="disabled" type="boolean" default="false" reflect="true"/>
    </mui:props>

    <mui:events>
        <mui:event name="activate" bubbles="true" composed="true" cancelable="true"/>
    </mui:events>

    <mui:template>
        <mui:button id="button"
                    class="root"
                    bind:disabled="props.disabled">
            <mui:image class="icon" bind:src="props.icon"/>
            <mui:text class="label" bind:text="props.label"/>
            <mui:slot/>
            <mui:slot name="badge"/>
        </mui:button>
    </mui:template>

    <mui:style src="neofontrender_rts:mui/components/icon-button.style.json"/>
    <mui:script src="neofontrender_rts:mui/components/icon-button.mjs"
                export="IconButton"/>
</mui:component>
```

没有动态行为的 component 可以完全省略 `<mui:script>`。`props.*` 是 compiler 认识的 component-local typed binding，不是任意 Java property path，也不执行 inline JS。

### 17.3 使用 XML component

注册后，页面或另一个 component 直接使用其标签：

```xml
<rts:icon-button id="confirm"
                 class="primary"
                 icon="neofontrender_rts:icons/confirm"
                 label="rts.action.confirm"
                 on:activate="confirm_build">
    <mui:text slot="badge" bind:text="state.pending_count"/>
</rts:icon-button>
```

JS 也可以动态创建同一 component：

```js
const button = document.createElementNS("urn:neofontrender:rts:1", "icon-button");
button.icon = "neofontrender_rts:icons/confirm";
button.label = "rts.action.confirm";
button.addEventListener("activate", confirmBuild);
toolbar.appendChild(button);
```

无论来自初始 XML 还是 JS mutation，都走同一个 component registry、prop validation、slot projection 和 render-tree bridge。

### 17.4 Manifest 注册

component 由 UI manifest 注册，不通过扫描 XML 文件名猜测：

```json
{
  "format": 1,
  "components": {
    "rts:icon-button": "neofontrender_rts:mui/components/icon-button.xml",
    "rts:mode-button": "neofontrender_rts:mui/components/mode-button.xml",
    "rts:window": "neofontrender_rts:mui/components/window.xml"
  },
  "nativeElements": [
    "rts:world-viewport",
    "rts:virtual-item-grid"
  ]
}
```

`components` 注册 XML composite element；`nativeElements` 只声明 application 依赖，真实 factory 必须已由对应 Java mod 注册。manifest 不能出现 Java class name。

资源加载阶段先构建 manifest/component dependency graph，再编译 application document。规则：

- qualified name 必须属于 manifest owner 已获准的 namespace。
- 禁止覆盖 `mui:*` 或另一个 mod 已注册的 tag。
- component 循环依赖是编译错误。
- 缺失 prop、非法 prop type、未知 slot 和重复 scoped id 是编译错误。
- registry generation 变化会使依赖 template cache 全部失效。

### 17.5 Component boundary

每个 component instance 在逻辑 DOM 中包含：

```text
ComponentHost <rts:icon-button>
  ComponentRoot（内部 scope）
    cloned template nodes
    projected slot nodes
```

MVP 不实现完整 WHATWG Shadow DOM，但实现必要子集：

- template 内 `id` 只在该 component root 内唯一。
- component stylesheet 默认只作用于本 root。
- 外部 selector 默认匹配 host，不穿透内部 root。
- component script 可通过 `this.root` 查询内部 element。
- 外部页面通过公开 prop/method/event 使用组件，不依赖内部节点。
- 跨 boundary 的 event 根据 `bubbles/composed` 决定是否向外传播；向外时 target retarget 为 component host。
- slot 支持 default 和 named slot，首版不支持动态 selector slot。

这个 scoped root 模型已经能避免组件库中的 id/class/style 相互污染；等真实需求出现后再扩展完整 Shadow DOM API。

### 17.6 Props、attribute 和 property

component prop 必须声明类型：

```text
string / text / boolean / int / float / color / resource
enum / object-schema / list-schema / callback-token
```

规则：

- XML attribute 先经 codec 转换为 typed property。
- `reflect=true` 才把 property change 映射回 string attribute。
- object/list 不序列化进 attribute，只能由 binding、JS property 或 host state snapshot 设置。
- required/default/readOnly 在 component compile/mount 时校验。
- property update 触发对应内部 binding、style invalidation 或 lifecycle callback，不重新解析整个 XML。
- component 对外不暴露内部 Widget 或 Polyglot `Value`。

### 17.7 Component JS lifecycle

可选 ES module 导出 component controller：

```js
export class IconButton {
  connectedCallback() {
    this.root.querySelector("#button")
      .addEventListener("activate", event => {
        this.host.dispatchEvent(new MuiEvent("activate", {
          bubbles: true,
          composed: true,
          cancelable: true
        }));
      });
  }

  propertyChangedCallback(name, oldValue, newValue) {
    // Optional derived state only. Basic prop bindings do not need JS.
  }

  disconnectedCallback() {
    // Timers/subscriptions created through the component scope are auto-disposed.
  }
}
```

同一 module 在一个 document context 中只 evaluate 一次；每个 component instance 创建轻量 controller object 和 lifecycle scope，不能为每个组件创建一个 Graal `Context` 或线程。scope 自动拥有 listener、timer、host subscription 和 pending request，disconnect 时统一取消。

### 17.8 Java native element 的使用标准

以下能力应继续由 Java native element 实现：

- Minecraft world/render interaction 和 OpenGL 绘制。
- TextField、IME、focus、pointer capture 等低延迟状态机。
- 虚拟化超大列表、slot/recipe viewer 和高频 drag/scroll。
- 相机连续输入、screen ray 和 world viewport。
- 需要现有 ModularUI sync handler/container 协议的控件。
- profiler 证明 JS/DOM projection 无法满足帧预算的组件。

普通 Button 组合、toolbar、window chrome、tab strip、search bar、dialog、progress row、设置 field 和页面布局优先用 XML component。这样组件库可以只靠资源迭代，不需要每改一个组合控件都重新编译 Java。

### 17.9 XML component 不是安全边界

纯 XML component 只能使用已注册 element/property/event，因此风险较低；带脚本的 component 继承其 application/origin 的 JS trust 和 capability，不因“组件库”身份获得额外权限。

第三方 component library manifest 可以发布 tags、style 和 module，但 consumer application 仍需声明依赖；capability 由最终 application owner/policy 授权。component 不能在内部偷偷扩大 manifest capability request。

### 17.10 Component MVP

最小可用组件系统应先实现：

1. manifest 中 `qualified tag → component XML` 注册。
2. typed primitive props、required/default/reflect。
3. template clone、scoped id 和 default/named slot。
4. scoped JSON stylesheet。
5. custom event 的 bubbles/composed/cancelable。
6. XML-only component，不依赖 GraalJS 即可运行。
7. GraalJS lifecycle module、instance scope 和自动 dispose。
8. JS `createElementNS` 动态创建 composite/native element。
9. dependency cycle、深度、实例数和 mutation budget 测试。

先用 `NfrIconButton` 或新的 `RtsIconButton` 做基准：分别以 Java Widget 和 XML component 实现，对比布局、事件、focus、reload、分配和帧耗时；行为和预算达标后再把 toolbar/window/tab 等组合控件迁到 XML 库。
