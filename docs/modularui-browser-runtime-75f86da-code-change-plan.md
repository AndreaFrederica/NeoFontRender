# ModularUI `75f86da` Browser Runtime 代码改造审核

状态：ModularUI 本体 Phase 0-4 已实现；可选 Graal addon 与 RTS/NFR/UIE addon 集成不属于 MUI 本体，尚未实现

日期：2026-08-29

## 1. 基线与结论

唯一代码基线：

- 仓库：`D:/Projects/sfr/other_mods/ModularUI`
- 分支：`master`
- commit：`75f86dabac6f91a224eb2a0716bf3838ff38f6a3`
- 版本：`3.2.0-nfr.1`
- 上游基线：`f2aa721`
- 当前工作树：包含尚未提交的 Phase 0-4 实现
- 构建：Java 17 toolchain + Jabel，`options.release=8`，发布运行时仍兼容 Java 8

结论是可以实现，而且不需要 RTSUI。推荐结构如下：

```text
ModularUI main jar, Java 8 bytecode, client + server
  XML/DOM/component/event/style/store/manifest
  Java DOM API
  DocumentSyncHandler 和 endpoint API
  不含任何 JS engine

  modularui-js-graal, Java 25 client-only optional addon
  GraalJS 25.x
  JS DOM/store/event facade
  ESM loader、scheduler、sandbox

NFR
  MUI component/theme contributor

UIE
  camera/input/navigation/HUD capability

RTS addon, 独立 client + server mod
  XML 页面和组件
  Java endpoint/capability/业务
  AE2/RS/JEI 等兼容层
```

最重要的同步决策是：

1. DOM 是客户端的 UI 结构，服务端不复制 DOM，也不接收任意 DOM mutation。
2. XML、Java 和 JS 创建的节点都绑定同一种 store/channel；节点来源不影响同步协议。
3. 每个 synced panel 在构建期只注册一个固定 `DocumentSyncHandler`，内部多路复用动态 logical channel。
4. 运行时增删 DOM 只增删 binding/subscription，不向已锁定的 `PanelSyncManager` 注册新 handler。
5. 真实 `ModularSlot` 是例外。它的 container ordinal 必须预先固定，不能跟随动态 DOM 增删。

## 2. 实际 fork 对设计的影响

`75f86da` 新增了完整的 device-neutral navigation API，并修改了 Widget 的结构/几何失效传播。这部分应该复用，不应在 DOM runtime 中再做一棵平行导航树。

### 2.1 可直接复用的代码

| 现有代码 | 可复用能力 | DOM runtime 的用法 |
|---|---|---|
| `AbstractWidget.initialise()/dispose()` | late mount、递归 dispose | Widget projection 的 mount/unmount 后端 |
| `AbstractParentWidget.addChild/remove()` | 动态 child、late initialise | mutation batch 的最终 Widget 操作 |
| `PanelManager.doSafe()` | dispatch 中延迟最终 dispose | 防止事件回调中销毁 screen 造成悬空访问 |
| `PanelManager` navigation revisions | 结构与几何缓存版本 | DOM batch 完成时统一失效 |
| `ModularGuiContext` | hover、focus、drag、变换后的命中结果 | 构建 immutable target snapshot |
| `ModularPanel` | legacy input、click-through、slot/JEI 行为 | HYBRID 模式的 default action |
| `NavigationInfo` | role、action、label、focusable | element semantic property 后端 |
| `ModularNavigationGeometry/Revealer` | screen bounds、clip、scroll reveal | DOM navigation 直接复用 |
| `ThemeManager`/Gson | JSON 和 resource reload 经验 | stylesheet 后端与 reload 集成参考 |
| `PanelSyncManager`/`SyncHandler` | 已有 panel/network transport | 承载一个 multiplexed document handler |

### 2.2 不能直接当 DOM 使用的部分

`IWidget` 树是 render tree，不是 logical DOM。它没有 attribute、class、listener table、component scope、stable node identity 或 transaction。DOM 必须是 source of truth，Widget 只是 projection。

`ModularNavigationAccess` 当前使用 name/type/index 生成 path，并在 `NavigationTreeEntry` 中直接保存 `IWidget`。运行时在前面插入一个无 id sibling 会改变后续 path，所以 path 只能继续作为 legacy capture key，不能作为 DOM identity。

`ModularPanel.onMousePressed()` 会依次尝试同一点的多个 hovered Widget。DOM event 需要唯一 target 和 ancestor path。这两者必须通过 LEGACY/HYBRID/DOM 三种模式兼容，不能一次性替换。

`PanelSyncManager.initialize()` 后锁定 handler 注册；`WidgetTree.collectSyncValues()` 又按 BFS 分配 `auto_sync` id。动态 DOM 若参与这条路径，client/server 很容易因树形或时序不同而错配。

## 3. 运行时所有权

### 3.1 唯一真实 DOM

MUI core 持有唯一真实 DOM：

```text
MuiDocument
  MuiNode
    MuiElement
    MuiText
```

Java 与 JS 操作的是同一个 document：

- Java 使用类型安全 `MuiDocument/MuiElement` API。
- JS 使用 stable `NodeHandle` 和白名单 proxy。
- JS 不得到原始 `IWidget`、`Minecraft` object 或 Java DOM object。
- Widget projection 只能由 `MuiDocumentHost` 创建和更新。

每个客户端 `ModularScreen` 都有一个 `MuiDocument`，不只 XML 页面才有。传统 Java builder 创建的
Panel/Widget 树会在 screen 首次打开时执行一次 `adoptWidgetTree()`：为每个现存 Widget 分配 stable
`NodeHandle`，建立父子节点、事件目标和 Widget host 映射。此后 Java-built、XML-built 和 JS-built UI
共享同一套 query/event/mutation API。

adoption 不复制 Widget。传统代码保存的 `ButtonWidget` 等 Java 引用仍指向正在渲染的同一个对象；DOM
element 是该对象的受控结构/属性视图。已存在的 value sync handler 和 slot 也不重新注册。

### 3.2 Legacy adoption 与 source of truth

adoption 前，Java Widget tree 是初始结构来源；adoption commit 后，`MuiDocument` 成为结构变更的协调者，
DOM 与 Widget tree 必须在同一个 mutation transaction 中更新：

```text
Java builder Widget tree
  -> adoptWidgetTree()
  -> MuiDocument + WidgetHost identity map

后续 Java add/remove/replace ----+
                                +-> DocumentMutationCoordinator
JS/XML DOM mutation ------------+      -> validate
                                       -> update DOM + Widget tree atomically
```

传统 Java 动态 UI 仍可使用 Widget API，但所有受支持的 child mutation 入口都必须进入 coordinator。
`getChildren()` 当前实际返回可修改 list，直接修改它既绕过 initialise/dispose，也绕过 navigation dirty，
本来就不是有效的动态更新方式。改造时应返回真正的 unmodifiable view，并提供正式的
`addWidget/removeWidget/moveWidget/replaceWidget` API；开发模式每帧校验 DOM/Widget child identity，发现
反射或旧代码绕过 API 修改时给出明确错误，不静默生成两棵不同的树。

第三方直接实现 `IWidget` 的控件也会被收养。未注册专用 adapter 时映射为 `mui:legacy-widget`：

- 支持 stable handle、父子结构、query、通用 event、focus/navigation、remove 和 lifecycle。
- 暴露通用只读属性：Widget class id、name、enabled/visible、navigation metadata。
- 通用可写属性只开放已经能安全映射到 `IWidget` API 的部分。
- 控件专有 property/default action/style 需要 mod 注册 `LegacyWidgetAdapter<T>`。
- 未知控件不是“没有 DOM”，而是 opaque DOM element，避免 JS 反射任意 Java method。

### 3.3 线程模型

无 JS 页面全部在 client thread 工作，不需要额外线程。

启用 GraalJS 后：

```text
client/render thread                         MuiScriptScheduler thread

hit test/input snapshot  -----------------> DOM event + JS listener
apply Widget mutation batch <--------------- logical DOM mutation batch
remote store result       -----------------> Promise/store update
host command batch        <----------------- capability request
```

`MuiDocument` 的可变状态由 `DocumentExecutor` 串行拥有。Java client-thread 调用可直接进入 transaction；JS 调用在 script thread 生成命令并在安全点提交。client thread 永不等待 JS。

## 4. 新增 package 与主要类型

建议先冻结下列公共包，不在首版追求完整 WHATWG API。

### 4.1 DOM API

```text
com.cleanroommc.modularui.api.dom
  MuiDocument
  MuiNode
  MuiElement
  MuiText
  NodeId
  NodeHandle
  DomException
  MutationScope
  MutationResult
  DocumentGeneration
  LegacyWidgetElement
  LegacyWidgetAdapter<T extends IWidget>
```

规则：

- `NodeId` 在一个 document generation 内单调分配且不复用。
- `NodeHandle = documentId + generation + nodeId`，跨 reload 可检测 stale。
- `id` attribute 不是 identity；改变 `id` 不改变 `NodeHandle`。
- 删除节点后 handle 永久 stale，不能因新节点复用数值而重新有效。
- 每个 public mutation 要么处于显式 `MutationScope`，要么自动创建一个单操作 transaction。

### 4.2 DOM event API

```text
com.cleanroommc.modularui.api.event
  IEventTarget
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
  InputModifiers
```

实现包：

```text
com.cleanroommc.modularui.screen.event
  MuiEventDispatcher
  EventTargetPath
  EventListenerRegistry
  PointerCaptureManager
  LegacyInteractionDefaultAction
  InputDispatchMode
```

首版事件语义：capture、target、bubble、once、passive、`preventDefault()`、`stopPropagation()`、`stopImmediatePropagation()`、显式 pointer capture。

### 4.3 XML/component/style

```text
com.cleanroommc.modularui.api.markup
  MuiApplicationDescriptor
  MuiManifestRegistry
  MuiResourceResolver
  MuiMarkupDiagnostic

com.cleanroommc.modularui.api.component
  MuiElementDescriptor
  MuiElementFactory
  MuiComponentDescriptor
  MuiComponentContributor
  MuiPropertyDescriptor<T>

com.cleanroommc.modularui.markup
  MuiXmlParser
  MuiDocumentCompiler
  MuiComponentCompiler
  MuiManifestLoader

com.cleanroommc.modularui.style
  MuiStylesheetParser
  MuiSelector
  MuiCascade
  MuiComputedStyle
  MuiStylePropertyRegistry
```

XML 使用 JRE 自带 StAX，不新增 XML 库。必须关闭 DTD、external entity、external schema 和任意 URL resolve，并设置 document byte、depth、attribute、node 和 text 长度限制。

Gson 已经是 MUI 依赖，继续用于 manifest 和 JSON stylesheet。不要引入 CSS engine、HTML parser、Vue、Pinia 或 React。

### 4.4 Store 与 UI 无关事件总线

```text
com.cleanroommc.modularui.api.store
  MuiStore
  StoreDefinition
  StoreState
  StoreGetter<T>
  StoreAction<R>
  StoreSubscription
  StoreScope
  StorePatch

com.cleanroommc.modularui.api.bus
  ScopedEventBus
  EventKey<T>
  EventSubscription
  SubscriptionScope
```

store 参考 Pinia 的 `state/getter/action/$patch/$subscribe`，但内部是 MUI 自己的 Java 实现。UI 无关 bus 不依赖 Widget、DOM、Minecraft 或 network；RTS 可以在其上定义业务 event，但 MUI core 不内置 RTS event type。

### 4.5 Dynamic document sync

```text
com.cleanroommc.modularui.api.sync.document
  DocumentEndpointCatalog
  DocumentEndpoint<P, S>
  DocumentCommand<P, R>
  EndpointContext
  EndpointSubscription
  RemoteStore
  RemoteStoreState
  DocumentSyncLimits

com.cleanroommc.modularui.value.sync.document
  DocumentSyncHandler
  DocumentFrameCodec
  MuiValue
  MuiValueCodec
  ClientDocumentChannel
  ServerDocumentChannel
  DocumentChannelRegistry
```

MUI 提供 transport 和 lifecycle，RTS/NFR/其他 mod 注册 endpoint。endpoint catalog 是 Java API，不由 XML 或 JS 自行增加权限。

## 5. 对现有类的具体修改

### 5.1 `IWidget`、`AbstractWidget`、`Widget`

`api/widget/IWidget.java`：

- 让 `IWidget` 继承带 default method 的 `IEventTarget`，保持已编译第三方 Widget 尽可能兼容。
- 新增 `getNodeHandle()`；Widget 未 mount 时返回 empty，传统和声明式 Widget mount/adopt 后都返回 stable handle。
- 不在公共 API 暴露 `LocatedWidget` 或 `TransformationMatrix`。
- default event API 使用 `WidgetExtensionRegistry` 的 weak-identity sidecar，确保第三方直接实现 `IWidget`
  的类也能在 mount 前注册 listener；不能假设所有控件都继承 `AbstractWidget`。

`widget/AbstractWidget.java`：

- 内置 Widget 可缓存 sidecar/handle 以减少查表，但唯一注册仍由 screen document controller 管理。
- `dispose()` 在清空 parent/context 前通知 screen document controller。
- dispose 时取消 listener、pointer capture、focus、binding 和 component scope。
- `setName()` 和 navigation metadata 变化仍调用现有 structure revision。

`widget/Widget.java`：

- 新增 fluent `onEvent(...)` 和 `bindNode(...)` builder。
- 旧 `listenGuiAction()` 保留并标记迁移路径，首版不删除。

第三方 Widget 的 listener、handle 和 adoption metadata 由 weak-identity sidecar 持有；dispose 后主动移除，
WeakReference 只是遗漏清理时的最后保护，不能依赖 GC 管理正常生命周期。

### 5.2 `AbstractParentWidget` 与 mutation batch

现有 `addChild/remove/removeAll` 已正确做 late initialise/dispose，但每次都会立即增加 navigation revision。增加 document batch 后：

- 不改变这些 protected API 的 legacy 行为。
- `PanelManager.beginUiMutationBatch()/endUiMutationBatch()` 记录 nesting。
- batch 中只设置 structure/geometry dirty bit。
- 最外层成功结束时各 revision 最多递增一次并统一 schedule resize。
- batch 失败时 logical DOM 不 commit；已创建但未 mount 的 Widget 直接 dispose。
- 首版不尝试对任意旧 Widget 操作做通用 rollback，DOM applier 必须先 validate 全 batch 再触碰 Widget tree。
- 将正式 Java child mutation API 接入 `DocumentMutationCoordinator`；若 screen 尚未 adopt，则保持当前直接构建行为。
- adoption 后 Java child mutation 先生成 logical mutation，再由同一 applier 调用现有 initialise/dispose 后端。
- `getChildren()` 返回真正只读 view，禁止外部直接改内部 list。
- `DelegatingWidget`、dynamic widget 和 Page/Scroll 等自行替换 child 的代码必须迁移到同一 coordinator hook。

### 5.3 `ModularScreen`

新增字段：

```text
MuiScreenDocumentController documentController（每个 client screen 必有）
InputDispatchMode inputDispatchMode，默认 HYBRID
```

新增 API：

```text
getDocument()
adoptWidgetTree()
installCompiledDocument(MuiCompiledDocument)
getDocumentController()
setInputDispatchMode(LEGACY | HYBRID | DOM)
```

输入顺序：

1. 保留 `onMouseInputPre()` 的原生 draggable/JEI 前置约束。
2. controller 从 `ModularGuiContext` 的 hit snapshot 选择唯一 DOM target。
3. LEGACY：紧急兼容/诊断模式，完全走当前逻辑，不承诺 event listener 被调用。
4. HYBRID：默认模式，先发 DOM event；未阻止 default 时调用当前 `ModularPanel` legacy 方法一次。
5. DOM：DOM event 后只执行已注册的 native default action。

HYBRID 在没有新 listener 时与旧行为一致；一旦传统 Java UI 注册 DOM listener，listener 就能传播、取消默认
行为或触发 DOM mutation。因此“传统 UI 支持事件”不要求先把 Button/TextField 等全部重写。

首次 `PanelManager.tryInit()` 打开 panel 后、resize 前完成 legacy adoption。`onUpdate()` 处理 store/network
result；`onFrameUpdate()` 在 `PanelManager.checkDirty()` 前应用已验证的 mutation batch。screen close/dispose
必须关闭 document generation并拒绝迟到 batch。

`ModularScreen.getSyncManager()` 依赖已经安装的 `screenWrapper/ModularContainer`，因此 synced document 不能在
screen 构造器里查找 handler。`construct()` 在 `ModularContainer.initializeClient()` 之后调用
`documentController.onScreenConstructed()`，再按 panel name + 固定 key 解析 `DocumentSyncHandler`；client-only
screen 则安装 local store transport。重复 construct、overlay 和没有 handler 都要显式区分，不能靠空指针失败。

### 5.4 `ClientScreenHandler`

这里继续负责从 LWJGL/Forge 采样原始输入，但改为构建 immutable `MuiInputSnapshot`：

```text
timestamp、pointer position、button/buttons、scroll、key/char、modifiers、input phase
```

不要在 script thread 查询 `Mouse`、`Keyboard`、`GuiScreen` 或 Minecraft 单例。

ESC/Inventory key 当前在 `keyTyped()` 中直接关闭 top panel。改造后顺序应是：先发可取消 `KeyEvent`/`ActionEvent(BACK)`，未处理时才调用现有 close default。

### 5.5 `ModularPanel`

保留现有 `onMousePressed/release/drag/scroll/key` 作为 legacy default action。首版不要拆散 slot、JEI、resize 和 MenuPanel 行为。

新增 package-private adapter，使 dispatcher 能调用一次明确的 default action，并在调用前验证 target/panel 仍有效。标准 Widget 按 Button、TextField、Slider、Scroll/Paged、Slot、MenuPanel 顺序逐类迁移。

### 5.6 `ModularGuiContext` 与 `LocatedWidget`

`ModularGuiContext` 增加每帧 immutable `HitTestSnapshot`，复用现有 `belowMouse/hovered` 计算，不重复遍历 Widget tree。

focus 改变时同时发 `FOCUS_IN/FOCUS_OUT` 和 `FOCUS/BLUR`，但实际 `IFocusedWidget.onFocus/onRemoveFocus` 仍是 native default。节点 dispose 时若持有 focus，先清 focus 再清 context。

`LocatedWidget` 仅在 internal 层为 path 中每个 target 计算 local coordinate；不进入公共 DOM/JS API。

### 5.7 `PanelManager`

新增：

- mutation batch nesting 和 deferred structure/geometry dirty bit。
- panel open/close/dispose document lifecycle hook。
- capture owner 因 panel/modal/Widget 失效时发送 cancel。
- `getTopInteractionPanel()` 和只读 hit candidate API，避免 dispatcher复制 panel/modal 规则。

现有 `navigationStructureRevision`/`navigationGeometryRevision` 保留。不要再新增一套 DOM revision 给 UIE；DOM 自身 generation 用于 stale handle，导航 cache 继续使用当前两个 revision。

`doSafe()` 当前未用 `try/finally` 恢复 `cantDisposeNow`，而且 boolean 不支持嵌套 safe dispatch。事件 listener
和脚本接入会增加异常和递归分发路径。Phase 0 应将 boolean 改为 `safeDepth` counter：进入时递增，
`finally` 中递减，只有回到 0 才执行等待中的 dispose。否则异常会让 PanelManager 永久不可 dispose，嵌套调用
也可能在外层尚未结束时提前允许 dispose。

### 5.8 navigation API

新增公共 stable identity：

```text
NavigationTargetHandle
  legacyPath（兼容）
  documentId/generation/nodeId（DOM-backed 时存在）
```

修改：

- `NavigationTreeEntry` 新增 handle 字段和兼容旧构造器。
- `ModularNavigationAccess.perform(screen, handle, action)` 优先按 stable handle 定位。
- 旧 `perform(screen, path, action)` 保留。
- capture 时 DOM-backed Widget 的 identity 不使用 sibling index。
- `ModularNavigationDispatcher` 先发 `ActionEvent`；未处理时调用现有 `INavigationActionHandler/Interactable`。
- `ModularNavigationGeometry` 和 `Revealer` 不需要重写。

### 5.9 `ValueSyncHandler`

当前只有一个 `Runnable changeListener`，DOM/store bridge 若调用 `setChangeListener()` 会覆盖 Widget 原有 listener。改为：

```text
ValueSubscription addChangeListener(Runnable)
boolean removeChangeListener(...)
```

内部使用 lazy list 和 dispatch snapshot。旧 `setChangeListener/getChangeListener` 暂时保留：它只管理一个 legacy slot，不清除新 subscription。`onValueChanged()` 同时通知 legacy listener 和 subscriber snapshot。

### 5.10 `PanelSyncManager`

修复两个碰撞问题：

- `putSyncValue()` 在目标 key 已被另一个 handler 占用时必须抛异常，不能静默覆盖。
- `registerSyncedAction()` 同名且不是同一注册时必须抛异常。

新增 helper：

```java
DocumentSyncHandler documentChannel(
        String key,
        DocumentEndpointCatalog catalog,
        DocumentSyncLimits limits);
```

helper 仍只在 panel build 阶段调用，并最终注册一个普通 `SyncHandler`。不要给 `DocumentSyncHandler` 特殊解锁权限。

### 5.11 packet 与 payload 限额

`NetworkUtils.readByteBuf()` 当前直接信任传入 VarInt。新增：

```text
readByteBuf(PacketBuffer, int maxBytes)
readPacketBuffer(PacketBuffer, int maxBytes)
```

先校验 `0 <= length <= maxBytes` 且不超过 readable bytes，再分配/copy。旧无上限 overload 可保留给兼容代码，但新的 document channel 禁止调用它。

`PacketSyncHandler.read()` 应设一个 MUI 总 payload 上限；`DocumentFrameCodec` 另设更小的 per-frame、string、collection、depth 和 patch-op 上限。

Generic list/map/set handler 也应补 collection size 上限，但这是独立 hardening，不作为 browser runtime 的前置耦合。

### 5.12 resource 与 reload

`AssetHelper` 直接引用 client-only `Minecraft/IResource`，不能被 common XML compiler 使用。新增 `MuiResourceResolver` 抽象：

- `ClientMuiResourceResolver`：client-only，使用 Minecraft resource manager 读取 XML/style/texture。
- `ModOwnedResourceResolver`：common，只读取已安装 mod 自己 JAR/source 中经过归一化的资源，不接受 resource pack 覆盖。
- `ScriptResourceResolver`：由 Graal addon 实现，只允许已信任 origin 的 `.mjs`。

普通动态 UI 的服务端不需要解析 XML。`ModOwnedResourceResolver` 主要用于固定 slot/protocol descriptor、manifest metadata 或未来 common validation，不能成为远程脚本加载器。

reload 使用 generation swap：后台 parse/validate 新 AST，client thread 原子安装新 registry/document generation；失败时保留旧 generation。

## 6. `DocumentSyncHandler` 协议

### 6.1 为什么是一条固定 handler

在 `IGuiHolder.buildUI()` 中 client/server 都会执行同一 panel builder，随后 `WidgetTree.collectSyncValues()` 做 BFS auto-sync，最终 `PanelSyncManager.initialize()` 锁定注册。最小兼容方案是：

```java
DocumentSyncHandler documentSync = syncManager.isClient()
        ? DocumentSyncHandler.client(limits)
        : DocumentSyncHandler.server(serverEndpointCatalog, limits);
syncManager.syncValue("mui_document", 0, documentSync);
```

这个 key、id 和 handler 类型在两端固定。其内部 channel 数量可以在 screen 打开后动态变化，不改变 `PanelSyncManager` 的拓扑或 `ItemSlotSH.init()` 顺序。

client handler 只持有 channel/store facade，不实例化 server endpoint executor。server handler 持有 catalog、player
context、permission 和 subscription。两边用 HELLO/READY 协商 frame version 与 limits；endpoint key/schema 在
SUBSCRIBE 时逐项验证，不要求 client 构建 server implementation。

### 6.2 frame

建议首版 opcode：

```text
C2S HELLO(protocolVersion, clientFeatures)
S2C READY(protocolVersion, limits)
C2S SUBSCRIBE(requestId, endpointKey, schemaVersion, params)
S2C SUBSCRIBED(requestId, channelId, revision, snapshot)
S2C PATCH(channelId, baseRevision, revision, patch)
C2S COMMAND(channelId, commandKey, requestId, expectedRevision, payload)
S2C RESULT(requestId, revision, payload)
S2C ERROR(requestId, code, safeMessage)
C2S/S2C UNSUBSCRIBE(channelId, reason)
S2C RESET(channelId, revision, snapshot)
```

服务端分配 `channelId`。客户端不能自选 channel id 或伪造 endpoint 实现。

### 6.3 endpoint lifecycle

```text
Document mount
  -> RemoteStore.subscribe(endpoint, params)
  -> server validates catalog/schema/permission/params
  -> snapshot installs store revision
  -> DOM binding renders state

Runtime DOM remove
  -> binding scope closes
  -> store refcount--
  -> zero refcount sends UNSUBSCRIBE
  -> no SyncHandler registration/removal

Document close/reload
  -> close all channel scopes
  -> reject late frame by document/session generation
```

多个节点可共享一个 `RemoteStore` subscription。移动节点不应 unsubscribe/resubscribe。

### 6.4 state、event 与 command

- server snapshot/patch 是 authoritative state。
- DOM event 默认只在客户端传播，不自动上网。
- 只有显式 store action/endpoint command 生成 C2S frame。
- 服务端重新做权限、距离、资源、revision 和业务参数验证。
- JS 只能调用 manifest 授权并由 Java catalog 暴露的 command。
- server domain event 可更新 endpoint state，但不能直接执行客户端 DOM mutation。

### 6.5 codec 与安全

首版使用自有、受限的 `MuiValueCodec`，而不是 Java serialization、任意 NBT 或任意 Gson object reflection。允许：null、boolean、int、long、double、UTF-8 string、byte array、list 和 string-key map。

默认建议上限需要通过测试后冻结：

| 项目 | 初始值 |
|---|---:|
| 单 frame | 256 KiB |
| 单 document 未处理入站 | 1 MiB |
| string | 32 KiB UTF-8 |
| collection entries | 4096 |
| nesting depth | 32 |
| active channels | 128 |
| pending commands | 256 |
| patch ops/frame | 4096 |

任何负长度、VarInt overflow、未知 opcode、越界 channel、revision 倒退或超预算都应关闭对应 channel；严重 framing 错误关闭整个 document sync session并记录限速日志。

## 7. 真实 slot 的例外

`ItemSlotSH.init()` 会调用 `ModularContainer.registerSlot()`，插入顺序成为 vanilla container slot ordinal。因此：

1. 固定 inventory UI：在 Java/common `SlotTable` 中预先注册所有真实 slot，DOM 只改变位置、样式、显隐和 enabled。
2. 数量有明确上限：预分配 slot pool，DOM 把 pool entry 投影到当前 row。
3. 大型动态仓储列表：使用 virtual item cell + `RemoteStore`，交互发送 command，不创建真实 container slot。
4. 真正改变真实 slot 数量：关闭并重新打开 container，生成新的固定 slot table。

当前 Phase 2 已增加 `SlotViewUpdate` 作为后台 rebuild 与客户端应用之间的不可变边界：

```text
worker thread
  -> 计算 visible slot NodeHandle 顺序
  -> SlotViewUpdate(visibleParent, parkingParent, fixedSlotTable, visibleSlots)
  -> enqueueSlotViewUpdate()

client/frame safe point
  -> 校验 document generation、parent 和全部 slot handle
  -> 单个 DOM transaction 内 keyed move
  -> 可见 slot 移入 visibleParent
  -> 其余 slot 移入 disabled parkingParent
  -> 根据新祖先 enabled 状态同步 ItemSlotSH enabled
```

这条路径不删除 `ItemSlot` DOM node，不重新调用 `ItemSlotSH.init()`，也不改变
`ModularContainer.inventorySlots`。因此 NodeHandle、listener、sync key 和 vanilla slot ordinal 全部保持稳定。
后台线程不能访问 live DOM/Widget/Container；直接访问会得到 `INVALID_STATE`，只能提交捕获不可变数据的
`DomUpdate`/`SlotViewUpdate`。当前通用 DOM mutation 明确拒绝 `Grid`，物品格首版应使用支持 child move 的
普通 parent + layout，或等待专用 Grid projection adapter。

完整 XML `ProtocolPlan/fingerprint` 只在页面确实声明真实 slot 或其他固定 handler topology 时需要。普通 storage browser、RTS toolbar、world viewport 和动态列表不需要服务端解析页面 XML。

## 8. Java/JS 运行时 DOM 修改示例

Java：

```java
try (MutationScope tx = document.mutate()) {
    MuiElement row = document.createElement("mui:row");
    row.setAttribute("class", "material-row");
    row.bind(remoteStore.select("materials/stone"));
    document.querySelector("#materials").appendChild(row);
    tx.commit();
}
```

JS：

```js
const row = document.createElement("mui:row");
row.classList.add("material-row");
row.bind(stores.materials.select("stone"));
document.querySelector("#materials").appendChild(row);
```

两者最终都产生同一种 logical mutation：

```text
logical DOM transaction
  -> schema/component/style validation
  -> immutable UiMutationBatch
  -> client thread atomic Widget apply
  -> one resize/navigation invalidation
```

绑定的 `RemoteStore` 独立于节点 identity。节点移动不会影响 channel；节点删除只关闭 binding scope；节点重新创建可复用已有 store subscription。

## 9. 分阶段实施

截至 2026-08-29 的实现状态：

- Phase 0：完成。`PanelManager` safe nesting、同步 key collision、bounded network payload 和多 value subscriber 已实现。
- Phase 1：完成。传统 `IWidget` 已具备 DOM-style event target；默认 HYBRID，支持 capture/target/bubble、
  once/passive/cancel/propagation control、pointer capture、focus/action event、immutable hit snapshot 和主动 dispose cleanup。
- Phase 2：`MuiDocument/MuiNode/MuiElement/MuiText`、stable handle/stale 检测、transactional
  structural mutation、传统 Screen 自动 adoption、opaque third-party Widget、listener canonical alias、
  DOM/Widget 双向 child mutation、只读 child view、跨 parent 无 remount move、每 batch 一次 navigation
  invalidation、client-thread ownership、后台 update queue、固定真实槽位表校验、`SlotViewUpdate`、
  `NavigationTargetHandle`、per-screen native element registry/factory、typed attribute/property bridge、
  `SingleChildWidget`/`Grid` DOM adapter 已实现。未知 tag 保持 logical-only；native Widget 必须直接挂在
  projected Widget parent 下；每个 batch 在创建/挂载 Widget 前完成结构验证。
  `DynamicSyncedWidget`、`PagedWidget` 等拥有额外生命周期/同步协议的特殊 parent 暂不接受通用 DOM child
  mutation，仍需各自 adapter 明确协议后再开放。
- Phase 2 集成验证：native container/button/text 创建和属性映射、text data 刷新、unknown logical wrapper 拒绝、
  single-child 整批拒绝、Grid columns/重排/跨 parent move、registry freeze/custom descriptor、invalid typed
  property 原子性均有回归测试。5000 节点单 transaction/query/stale 压力测试已通过。
- Phase 3（基础层已实现）：新增 `api.markup` 的 immutable application descriptor、manifest registry、
  resource resolver 和 markup exception；新增 `MuiXmlParser`，使用 JRE StAX 将受限 XML 编译到现有
  `MuiDocument`，支持 namespace 前缀、属性、文本、单 transaction 提交以及 DTD/external entity/深度/节点/
  文本/属性数量限制。新增 `MuiComponentDescriptor`、`MuiComponentRegistry`、contributor hook、
  `MuiComponentCompiler` 和 `MuiDocumentCompiler`，支持 scoped root、`${prop}` 属性/文本插值、default/named
  slot、递归/深度限制以及临时文档展开后一次性提交；资源缺失或展开失败不会向目标文档提交半棵树。
  `ModularScreen.installCompiledDocument` 已接入主 panel 投影；JSON stylesheet parser/selector/cascade/computed
  style 和首批 `MuiStyleApplier` 也已接入，支持 geometry/box/text/background/theme-key 与
  enabled/visibility/focusable/tab-index、overflow/scrollbar。Java fallback `MuiStore` 已提供
  JSON-like state、原子 patch/reset、revision、订阅和 DOM attribute/text binding；projected scroll
  widget 已提供 scroll properties、scrollTo 和 scroll event。
- 验证：使用 Java 8 test launcher；真实 `ItemSlot -> ItemSlotSH -> ModularContainer -> SlotViewUpdate`
  集成测试、XML parser/manifest/component/style、DOM/event/store/event bus 和 document sync tests 均纳入
  全量回归；当前为 85 tests、0 failure/error/skip。已检查全部 997 个 MUI main classfile，均为 major 52。
- Phase 4：endpoint catalog、bounded codec/frame、client/server session、RemoteStore、固定
  `DocumentSyncHandler`、protocol XML、显式 wire factory registry、OpenGui template fingerprint handshake
  和固定 handler/action/real-slot topology 校验均已实现。
- Phase 5-6：尚未实现，且必须分别位于可选 Graal client addon 与独立 RTS/NFR/UIE addon，不进入 MUI core。

### Phase 0：先修基础契约

- 给 `PanelManager.doSafe()` 加 `try/finally`。
- 给 sync handler/action key 增加 collision check。
- 增加 bounded ByteBuf API。
- `ValueSyncHandler` 多 subscriber。
- 为现有 input/focus/panel/slot/navigation 行为补回归测试。

退出条件：无 browser runtime 时所有现有测试和游戏内行为不变。

### Phase 1：event substrate，传统 UI 默认 HYBRID

- `IEventTarget`、event object、listener registry、path dispatcher。
- immutable hit snapshot。
- focus event 和 pointer capture lifecycle。
- 所有传统 Widget 的 weak-identity event sidecar。
- LEGACY/HYBRID/DOM mode；回归测试通过后默认 HYBRID，保留 LEGACY 作为 opt-out。

退出条件：没有 listener 时 HYBRID 不改变 Button、TextField、Slot、JEI、Panel drag/resize 行为；传统
Java Widget 注册 listener 后能收到完整传播事件。

### Phase 2：Java DOM 与 Widget projection

- `MuiDocument/NodeHandle/MutationScope`。
- 每个传统 screen 自动 `adoptWidgetTree()`，包括第三方 opaque Widget。
- 正式 Java child mutation API 与 direct-list mutation 封锁。
- element registry 和基础 native elements。
- batch validate/apply、generation、dispose。
- navigation stable handle。

退出条件：现有 Java-built screen 可 query/监听/动态修改 DOM；纯 Java 动态创建、移动、删除 5000 节点，
focus/capture/navigation 无 stale 引用。

### Phase 3：XML、component、style、Java store

- 当前已完成 XML parser/manifest registry/component-template、screen compiler 安装、JSON
  selector/cascade/computed-style 基础层和首批 Widget style adapter；后续接 Pinia 风格 Java store。
- secure StAX、manifest、namespace registry。
- XML component、typed prop、slot、scoped root。
- JSON stylesheet/cascade/computed style，以及客户端 `MuiStyleApplier`（geometry、box、text、
  background、theme-key、enabled/visibility）。未映射属性仍保留在 computed style，不影响旧 theme。
- `ModularScreen#setStylesheet` 是可选客户端能力；样式重算只发生在 owner/client thread，
  不进入 `PanelSyncManager` 或同步 packet。DOM node identity、slot ordinal 和 sync handler 不变。
- `MuiDocument#setAttribute` 现采用先写入、observer 失败回滚的原子顺序，保证 style observer
  读取到新值；`ModularGuiContext` 已把 hover/below-mouse/focus 生命周期映射为
  `data-hover`、`data-below-mouse`、`data-focus`、`data-active` 属性，供 `:hover`/`:focus`/`:active`
  等 selector 动态重算。
- `MuiStyleApplier` 已将 `Flow` 的 `gap/wrap/justify/align` 和 `Grid` 的
  `grid-columns/columns` 接到现有布局 API，并在重算时恢复原布局配置；`grid-rows` 及更复杂
  的 layout 语义仍等待独立 adapter。
- `focusable` 和 `tab-index` 已通过 `NavigationInfo` immutable copy 接入导航元数据；样式清除
  会恢复原始 navigation override。`pointer-events: none` 已接入每帧 hit-test 过滤：跳过该
  Widget 作为 pointer target，但保留 DOM/event parent。`overflow` 已接入 `AbstractScrollWidget`：`auto`/`scroll` 补齐缺失轴，`hidden`/`visible` 移除可选轴；ListWidget/TextField 的内部必需轴受保护，样式重算和清除均恢复 baseline。滚动条样式可覆盖轨道背景色、drawable、滚轮速度和 edge-cancel；滚动尺寸、当前位置及厚度仍属于 Widget 运行时状态。
- Projected scroll widgets now expose client-local `scrollLeft`/`scrollTop`/`scrollWidth`/`scrollHeight`
  properties and `scrollTo` setters. The controller polls native positions at tick/frame boundaries and emits
  a non-bubbling, non-cancelable `ScrollEvent`; no scroll state enters document synchronization.
- 新增 `MuiCssStylesheetParser` 和 `MuiStylesheetParser.parseCss(...)`，支持受限 CSS 文件：
  注释、selector list、`:root` 自定义变量、`var(--name)`、基础 declaration，以及同 JSON
  格式相同的大小/规则/声明数量限制。CSS 与 JSON 最终共用同一个 `MuiCascade`，不会引入完整
  CSS engine。
- CSS `@media` 已支持 `min/max-width`、`min/max-height`、`orientation` 和 `screen/all`，由
  `MuiMediaEnvironment` 参与 cascade 计算；`ModularScreen.onResize` 会更新 viewport 并重算样式。
  `@import` 通过 `MuiResourceResolver` 加载 UTF-8 资源，限制 import 深度/数量/总大小并拒绝循环；
  支持 `"resource.css"`、`url("resource.css")` 以及其后的单个 `screen`/`all`/feature
  media 条件，并将外层 `@media` 条件传递到导入规则。未提供 resolver 时显式拒绝
  import，避免任意文件或 URL 访问；不支持逗号媒体列表和相对路径自动解析。
- Pinia 风格 Java store 和 binding。
- UI 无关 `MuiEventBus`：typed topic、priority/order、scope auto-dispose、thread confinement 和 listener
  exception isolation；它不参与 DOM capture/bubble，也不跨网络。

XML、JSON stylesheet 和 CSS 子集的可用语法、限制及 Widget 映射详见
[`modularui-markup-style-syntax.md`](modularui-markup-style-syntax.md)。

退出条件：无 Graal 安装时，XML-only 页面和组件库完整可用。

### Phase 4：dynamic document sync

- 已完成 `DocumentSyncHandler`、`DocumentFrameCodec`、`MuiValueCodec`、endpoint catalog。
- 已完成 `RemoteStore`、refcount、revision、snapshot/patch/reset、command/result/error。
- 已完成 frame/string/collection/depth/channel/pending-command/patch quotas、permission/schema/params gate、
  单调 request id 防重放、dispose/late response 生命周期。
- 已完成 `MuiProtocolPlan` 的 canonical encoding、SHA-256 fingerprint 和固定 slot ordinal collision check。
- 已完成 secure protocol XML parser/compiler、显式 Java wire type factory registry 和 plan installer；XML 不反射
  Java class，也不能注册任意服务端 endpoint。
- 已完成 OpenGui template contract handshake。client 在 `readGuiData()` 后、`createPanel()` 前比较
  plan format/schema id/schema version/SHA-256 fingerprint；不一致时 fail closed，并发送 `CloseGuiPacket`。
- 固定模板关闭 BFS implicit `auto_sync`，要求所有 mod-owned handler/action/real slot 都由 plan 显式安装；
  安装后检查缺失、替换和额外注册，container construct 后再检查真实 slot 位于声明的连续 leading ordinal。
- framework 自动 cursor/player inventory slot 在 plan 校验之后追加，不属于 mod-owned template topology。
- 固定 slot 的运行时展示继续使用已实现的 fixed slot table + `SlotViewUpdate` parking/move 方案。

动态同步 API、线程与服务端 endpoint 示例详见
[`modularui-document-sync-api.md`](modularui-document-sync-api.md)。
固定模板 API、protocol XML 和 OpenGui 校验详见
[`modularui-fixed-protocol-template.md`](modularui-fixed-protocol-template.md)。

退出条件：Java 8 dedicated server 无 XML DOM/JS runtime，也能服务动态 client document；DOM 重排不改变 wire channel。

### Phase 5：可选 Graal addon

- client-only shared `Engine`、per-document `Context`。
- `js.ecmascript-version=latest`、ESM、Promise/timer。
- NodeHandle proxy、DOM/store/event facade。
- capability sandbox、statement/wall-time/queue budget。

退出条件：无 Graal 时 MUI 主 jar正常加载；有 Graal 时 JS 线程不阻塞 render thread。

### Phase 6：NFR component pack 与 RTS slice

- NFR 以 contributor 注册 component/theme，不把 browser runtime 放进 NFR。
- UIE navigation 改用 stable handle；camera/input/HUD 继续是 capability。
- RTS 独立 addon实现 world viewport、storage endpoint、单方块 placement 和 server validation。

退出条件：多人 dedicated server 完成打开、动态列表、相机、放置、关闭、断线和重连。

## 10. 测试清单

必须新增：

- capture/target/bubble、once/passive/stop/prevent 顺序。
- listener 中 remove target、close panel、throw exception。
- pointer capture 在 dispose/modal/screen close 时 cancel。
- one mutation batch/one navigation revision/one resize schedule。
- sibling 插入后 `NodeHandle` 和 navigation handle不变。
- stale generation mutation/result不触碰新 document。
- Java 与 JS 创建节点得到相同行为。
- native registry/factory 创建 Widget、typed attribute/property bridge、unknown tag logical-only 行为。
- single-child/Grid adapter 的整批约束、columns 重建、跨 parent identity-preserving move。
- registry 在首次 screen adoption 后冻结；自定义 descriptor 只允许 pre-open 注册。
- 传统 Java-built Widget 自动 adoption 后可被 `querySelector()` 查询。
- 传统 Widget Java 引用、DOM element 和 navigation handle指向同一 identity。
- 第三方未知 Widget 使用 opaque element，事件传播正常且不能反射调用 Java API。
- adoption 后 Java child API 与 DOM mutation 双向保持一致。
- 直接修改 `getChildren()` 被拒绝，不能静默破坏 DOM/Widget 一致性。
- runtime add/remove/reorder不改变 `PanelSyncManager` handler key。
- subscribe/refcount/unsubscribe、revision gap/reset、late result。
- endpoint 未注册、未授权、参数非法、command replay。
- frame/string/list/map/depth/channel/pending command 上限。
- 固定 slot ordinal client/server 一致。
- Java 8 dedicated server classloading 不触碰 client/Graal class。
- 无 Document 的现有 UI golden regression。

## 11. 审核建议

建议批准以下五项作为实现前的冻结决策：

1. `NodeHandle` 是 DOM/navigation 的稳定 identity，path 仅保留兼容。
2. `DocumentSyncHandler` 多路同步数据，绝不同步 DOM。
3. `ProtocolPlan/SlotTable` 只约束真实 slot 等固定 container 协议。
4. 每个传统 `ModularScreen` 自动 adoption 为 DOM，未知第三方 Widget 使用 opaque adapter。
5. event 默认 HYBRID；LEGACY 仅作兼容 opt-out，标准控件逐类迁移后再考虑 DOM-only 默认。

这五项已经冻结并用于 Phase 0-4 的现有实现。后续工作必须保持相同边界，不能让 Graal 或 RTS addon
绕过固定 handler/slot contract。

## 12. 主要证据位置

| 结论 | 实际源码 |
|---|---|
| Java 8 发布边界 | `build.gradle:306-307,1213`、`buildscript.properties:19` |
| 输入原始采样 | `screen/ClientScreenHandler.java:259-360` |
| screen 输入入口 | `screen/ModularScreen.java:356-535` |
| legacy target/click-through | `screen/ModularPanel.java:333-636` |
| focus/hover/drag | `screen/viewport/ModularGuiContext.java:155-430` |
| Widget mount/dispose | `widget/AbstractWidget.java:79-145` |
| 动态 child | `widget/AbstractParentWidget.java:74-124` |
| sync BFS | `widget/WidgetTree.java:183-214` |
| handler 锁定/碰撞 | `value/sync/PanelSyncManager.java:60-65,173-201,309-313` |
| 单 change listener | `value/sync/ValueSyncHandler.java:13,38-49` |
| packet payload 无显式上限 | `network/NetworkUtils.java:41-54`、`network/packets/PacketSyncHandler.java:37-52` |
| slot ordinal 注册 | `value/sync/ItemSlotSH.java:35-42`、`screen/ModularContainer.java:145-156` |
| navigation path 不稳定 | `screen/navigation/ModularNavigationAccess.java:40-90` |
| navigation entry 保存 Widget | `api/navigation/NavigationTreeEntry.java:14-40` |
| navigation revisions | `screen/PanelManager.java:45-47,431-445` |
| client-only asset helper | `utils/AssetHelper.java:1-36` |
