# ModularUI Browser Core 与现有同步系统集成审核

状态：提案，等待审核；本文不代表已经实现

日期：2026-08-29

代码审计基线：

- ModularUI：`D:/Projects/sfr/other_mods/ModularUI`
- commit：`75f86dabac6f91a224eb2a0716bf3838ff38f6a3`
- 版本：`3.2.0-nfr.1`
- Minecraft：`1.12.2`
- MUI 发布字节码：Java 8；源码使用 Java 17/Jabel
- 关联总设计：`docs/modularui-browser-runtime-architecture.md`

> `75f86da` 实际 fork 的类级修改方案和最新同步结论以
> `docs/modularui-browser-runtime-75f86da-code-change-plan.md` 为准。本文后文的完整
> ProtocolPlan 仍适用于真实 `ModularSlot` 等固定 container 协议，但普通动态 DOM 数据改为由一个
> 稳定 `DocumentSyncHandler` 多路复用，不要求服务端构建 DOM 或解析页面 XML。

本文修订总设计中的两项旧决策：

1. XML、logical DOM、DOM event、JSON stylesheet、XML component、Java store/binding 和 manifest 不再放入 NFR 的独立 browser runtime，而是内置 ModularUI 主体。
2. GraalJS 仍是独立、可选、仅客户端安装的扩展；dedicated server 和不使用 JS 的客户端不加载或携带 Graal。

## 1. 审核结论

可以支持。`75f86da` 复核后，同步不再按“ClientDocument 或整棵固定 SyncedDocument”二选一，
而是把动态数据通道与固定 container 拓扑分开：

```text
Dynamic Document
  XML/Java/可选 JS 可以动态增删、移动和替换元素
  构建期只注册一个稳定 DocumentSyncHandler
  handler 内部动态 multiplex endpoint channel
  同步 store state/patch/command，不同步 DOM
  server 不构建 DOM，也不需要解析页面 XML

Fixed Container Protocol
  只覆盖真实 ModularSlot 和其他固定 handler topology
  Java common SlotTable 或受约束 ProtocolPlan
  显式 stable key/type/version/order/fingerprint
  DOM 只改变真实 slot 的位置、样式、显隐和 enabled
```

建议批准的制品边界：

```text
ModularUI main jar (Java 8 bytecode, client + server)
  manifest/schema
  secure XML parser/compiler
  XML component library
  logical DOM and Java DOM API
  capture/target/bubble event dispatcher
  UI-independent scoped event bus
  JSON stylesheet/cascade/computed style
  Java store/binding/action registry
  native element registry
  DocumentSyncHandler/endpoint/store bridge
  optional fixed SlotTable/ProtocolPlan
  no Graal dependency

ModularUI GraalJS addon (Java 25, client only, optional)
  Graal Engine/Context
  ESM resolver
  JS DOM facade
  Pinia-style JS store facade
  timers/Promise jobs/script scheduler
  sandbox/capability bridge

NFR
  font/theme tokens and native component contributor

UIE
  camera/input/navigation/HUD capabilities

RTS addon (client + server)
  application XML/components/controllers
  protocol factories and business capabilities
  server validation/task/storage/workflow/compat
```

这满足四个部署目标：

| 环境 | MUI 主体 | Graal addon | XML/DOM | synced UI |
|---|---:|---:|---:|---:|
| Java 8 dedicated server | 是 | 否 | 不构建 DOM；只运行 endpoint/可选固定 slot plan | 是 |
| 不使用 JS 的客户端 | 是 | 否 | 完整 Java/XML UI | 是 |
| 使用 JS 的 Java 25 客户端 | 是 | 可选安装 | 完整动态 UI | 是；真实 slot 拓扑受限 |
| 纯 Java 旧 MUI 模组 | 是 | 否 | 不要求迁移 | 行为保持兼容 |

## 2. 当前同步实现的代码事实

### 2.1 panel 在两端分别构建

`UIFactory.createPanel()` 的接口文档明确说明会在 client 和 server 调用：

- `api/UIFactory.java:32-40`
- `factory/GuiManager.java:73-98`：server 构建 panel、收集 handler、构造 container
- `factory/GuiManager.java:103-126`：client 再构建一次 panel、收集 handler、构造 container/screen

所以 synced XML 不能只由客户端资源管理器读取。服务端至少必须得到同一个协议模板。

### 2.2 legacy 自动 key 取决于 BFS 顺序

`WidgetTree.collectSyncValues()`：

```text
sync name = "auto_sync:" + panelName
遍历 = foreachChildBFS
id = 0, 1, 2 ...
只为尚未注册的 ISynced handler 分配 key
```

证据：`widget/WidgetTree.java:193-203`。

因此以下任何差异都会改变 wire key：

- client-only 条件分支插入一个 synced widget；
- XML component 在两端展开数量不同；
- JS 移动或删除协议节点；
- resource pack 改变协议节点的 BFS 顺序；
- server 和 client 使用不同 component 版本。

包不会按 DOM id 找元素，而可能按相同的 `auto_sync:<panel>:<n>` 被交给错误 handler。

### 2.3 网络按 network id、panel、handler key 寻址

`PacketSyncHandler` 序列化：

```text
networkId
panel
key
action
payload
```

证据：`network/packets/PacketSyncHandler.java:20-51`。

接收路径为：

```text
ModularNetworkSide.receivePacket
  -> activeScreens[networkId]
  -> ModularSyncManager.receiveWidgetUpdate(panel, key, ...)
  -> PanelSyncManager.syncHandlers[key]
  -> SyncHandler.readOnClient/readOnServer
```

证据：

- `network/ModularNetworkSide.java:70-80`
- `value/sync/ModularSyncManager.java:153-161`
- `value/sync/PanelSyncManager.java:123-138`

DOM node handle 与 SyncHandler key 必须是两个独立身份体系。

### 2.4 构建结束后 handler map 锁定

`ModularSyncManager.construct()` 调用 `open()`，随后 `PanelSyncManager.initialize()` 初始化所有 handler 并设置 `locked = true`：

- `value/sync/ModularSyncManager.java:56-64, 135-140`
- `value/sync/PanelSyncManager.java:59-65`

锁定后的普通 `syncValue()` 会抛出异常。`getOrCreateSyncHandler()` 只有在构建阶段，或 MUI 显式打开临时注册窗口时，才能新增 handler：

- `value/sync/PanelSyncManager.java:173-201`
- `value/sync/PanelSyncManager.java:332-350`

这意味着 DOM 在 panel 打开后可以动态添加视觉 Widget，但不能顺便产生新的隐式同步协议。

### 2.5 DynamicSyncHandler 不是通用 DOM 动态协议

`DynamicSyncHandler` 的 provider 会在两端执行。它临时允许调用 `getOrCreateSyncHandler()`，随后检查动态 Widget 子树中是否还有未注册 handler；有则直接失败：

- `value/sync/DynamicSyncHandler.java:57-68`
- `value/sync/DynamicSyncHandler.java:104-141`

该类已经标注 `@ApiStatus.Obsolete`。新 XML/DOM 协议不应建立在它之上，只应继承“动态协议必须显式稳定注册”这一约束。

### 2.6 slot 还有独立的顺序协议

只修复 handler key 仍不够。`PanelSyncManager` 使用 insertion-ordered map，并在初始化时按该顺序调用 `SyncHandler.init()`：

- `value/sync/PanelSyncManager.java:36`
- `value/sync/PanelSyncManager.java:60-63`

`ItemSlotSH.init()` 会立即调用 `ModularContainer.registerSlot()`，从而确定 vanilla container 的 slot index：

- `value/sync/ItemSlotSH.java:34-42`
- `screen/ModularContainer.java:144-174`

客户端点击最终仍进入 vanilla `GuiContainer` 的 slot click 路径：`screen/ClientScreenHandler.java:370-387`。

所以 ProtocolPlan 必须固定：

- handler key；
- handler wire type/version；
- handler 注册顺序；
- 所有真实 slot 的 container ordinal；
- slot group、shift-click priority 和 phantom/real 属性。

视觉 DOM 可以重新布局 slot，但不能改变 container slot table。

### 2.7 secondary panel 也在两端重建

`PanelSyncHandler.openPanel()` 在 client/server 分别执行 builder，再收集新 panel 的 handler：

- `value/sync/PanelSyncHandler.java:55-92`
- `value/sync/PanelSyncHandler.java:121-138` 明确指出无法保证重建后的 handlers 相同

因此 secondary panel 也必须拥有 ProtocolPlan/fingerprint，不能只校验主 panel。

### 2.8 当前测试没有覆盖同步拓扑契约

`src/test` 目前只有 camera、format、sizer、WidgetTree 等测试。`WidgetTreeTest` 只验证树查询，没有覆盖：

- 两端 BFS 顺序不同；
- 重复 sync key；
- 锁定后注册；
- stable key 类型不一致；
- slot ordinal 不一致；
- dynamic/secondary panel 重建。

这些必须在启用 synced XML 前补齐。

## 3. 两种 Document 模式

manifest 中必须显式声明 mode，不能靠是否出现 `sync:*` 属性猜测：

```json
{
  "formatVersion": 1,
  "applications": [
    {
      "id": "rtsbuilding:hud",
      "mode": "client",
      "document": "rtsbuilding:mui/hud.xml"
    },
    {
      "id": "rtsbuilding:storage",
      "mode": "synced-template",
      "protocol": "rtsbuilding:storage@1",
      "document": "META-INF/modularui/rtsbuilding/storage.xml",
      "styles": ["rtsbuilding:mui/style/storage.style.json"]
    }
  ]
}
```

### 3.1 Dynamic Document

- DOM 只在客户端构建；传统 Java UI 和 XML UI 都自动拥有 Document。
- XML 可来自 mod JAR 或允许的 resource pack。
- Java 或 JS 可以动态增删、移动、替换任意元素。
- client-only 页面不创建同步节点；需要服务端数据时在 build 阶段只注册一个稳定 `DocumentSyncHandler`。
- handler 内部 multiplex endpoint channel，同步 snapshot/patch/command/result，不同步 DOM。

### 3.2 Fixed Container Protocol

- 只有真实 `ModularSlot` 或其他固定 handler topology 需要本节协议。
- SlotTable/可选 protocol XML 必须是 mod-owned、client/server 都可读取的 JAR 资源。
- 普通 resource pack 不能覆盖 protocol XML、协议 component 或 manifest。
- style、texture 和不影响协议的 client decoration 可以按策略覆盖。
- 所有固定 slot/handler/action/panel entry 必须由 SlotTable/ProtocolPlan 显式注册。
- 禁止 `WidgetTree.collectSyncValues()` 为文档协议节点生成 `auto_sync:*`。
- JS/Java DOM 可以重排视觉节点，但不能改变真实 slot 数量或 ordinal。

## 4. ProtocolPlan 设计

### 4.1 编译产物

XML compiler 输出两个不可变产物：

```text
MuiProtocolPlan (common, Java 8, client + server)
  schema id/version
  ordered panels
  ordered handlers
  ordered container slots
  synced actions
  protocol component dependencies
  canonical fingerprint

MuiRenderPlan (client)
  logical DOM descriptors
  native element descriptors
  component expansion
  style references
  bindings and Java/script action names
```

服务端只需要执行 ProtocolPlan 并返回一个最小 `ModularPanel`；客户端再将 RenderPlan 映射到 Widget tree。视觉 parent/child 顺序不再决定 wire key 或 slot ordinal。

### 4.2 protocol entry

每个 entry 至少包含：

```text
panelId
stableName + numericId
wireTypeId
wireSchemaVersion
registrationOrdinal
direction/permissions
slot metadata (optional)
factory binding id
```

`wireTypeId` 不能用 Java class name。建议形如：

```text
modularui:value/int@1
modularui:item-slot@1
modularui:panel@1
rtsbuilding:storage-page@1
```

Java factory 通过显式 registry id 注册，不允许 XML 反射 class name。

### 4.3 XML 示例

```xml
<mui:document xmlns:mui="urn:modularui:core"
              xmlns:sync="urn:modularui:sync"
              xmlns:nfr="urn:neofontrender:components">
  <mui:body>
    <nfr:label text="Storage" />
    <nfr:item-slot id="input"
                   sync:name="rtsbuilding:storage/input"
                   sync:id="0"
                   sync:type="modularui:item-slot@1"
                   sync:factory="rtsbuilding:storage-input" />
  </mui:body>
</mui:document>
```

`id="input"` 是 DOM identity；`sync:name + sync:id` 是 wire identity。两者不自动互相推导。

### 4.4 fingerprint

fingerprint 使用 JDK SHA-256，对 compiler 生成的 canonical ProtocolPlan 编码计算，不直接 hash XML 字节。canonical 内容至少包含：

- compiler format version；
- schema id/version；
- panel graph；
- ordered handler table；
- wire type/version；
- ordered real slot table；
- synced action signatures；
- protocol component ids/versions。

style、位置、颜色、文本和纯视觉 DOM 不进入 protocol fingerprint。

### 4.5 open handshake

当前 `OpenGuiPacket` 只发送 window id、network id、factory name 和 GuiData（`network/packets/OpenGuiPacket.java:20-63`），没有模板校验。

需要增加可选的 template contract：

```text
hasTemplateContract
schemaId
schemaVersion
protocolFingerprint
```

客户端必须在 `GuiManager.openFromClient()` 构建/激活 screen 前比较本地 plan：

- 一致：继续构建。
- 不一致：拒绝打开，发送明确的 close/reject packet，并显示版本错误。
- 禁止“尽量继续”或回退到 BFS 猜测。

旧 Java-built UIFactory 不声明 contract 时保持原协议和包格式兼容策略。

### 4.6 duplicate 与 type 检查

当前 `PanelSyncManager.putSyncValue()` 对同一 key 的另一个 handler 会直接 `put` 覆盖；`registerSyncedAction()` 也会覆盖同名 action。ProtocolPlan 安装路径必须更严格：

- 相同 key + 相同实例：幂等；
- 相同 key + 不同实例：编译或安装失败；
- 相同 key + 不同 wire type/version：失败；
- 相同 handler 实例 + 不同 key：失败；
- duplicate action/panel/slot ordinal：失败。

建议同时修复公共 registrar，使旧 API 在开发环境也尽早报告 collision。

## 5. 动态规则

### 5.1 synced UI 中允许动态修改

- 文本、颜色、tooltip、enabled/visible 等展示属性；
- 布局和纯视觉容器；
- 不包含 `ISynced` 的 client-only child subtree；
- list 行、modal、菜单等客户端投影；
- 通过 stable key 引用已经存在的 handler；
- 调用 manifest 中已注册的 synced action/command capability。

### 5.2 synced UI 中禁止动态修改

- 新增未列入 ProtocolPlan 的 handler/action/panel；
- 删除或替换协议 entry 的 wire type；
- 改变真实 container slot table；
- 让 JS 或 resource pack 决定协议 entry 数量；
- 把 DOM handle 当 sync key；
- 在服务端执行 JS 来“保持两端一致”。

### 5.3 动态数据集合

动态 storage list、任务列表等优先同步为一个稳定的 list/page handler，然后在客户端投影成任意数量的 DOM row。不要为每个 row 动态创建 handler。

确实需要动态真实 slot 时，必须由单独的版本化 protocol mutation 设计处理，并同时维护 slot ordinal；不纳入第一版 XML synced template。

## 6. 现有同步数据如何进入 store/DOM

不能另做一套 JS 网络同步。推荐数据流：

```text
server source of truth
  -> existing SyncHandler.detectAndSendChanges
  -> PacketSyncHandler
  -> client SyncHandler.readOnClient (client/main thread)
  -> MuiSyncProjection immutable update
  -> Java store (no-JS Document)
     or bounded queue -> JS-thread Pinia facade
  -> binding invalidation
  -> DOM mutation batch
  -> Widget host apply at safe client-frame boundary
```

反向命令：

```text
Java/JS event handler
  -> registered action/capability id
  -> client-thread validation
  -> existing SyncedAction/SyncHandler or addon C2S packet
  -> server permission/domain validation
  -> source-of-truth update
  -> normal S2C projection
```

规则：

- store 是 client view state，不取代 `PanelSyncManager`。
- 服务端状态不能由 JS store 直接写入。
- handler 的 `readOnClient()` 仍在现有网络/client thread 完成。
- 跨到 JS 线程只传 immutable DTO，不传 `Widget`、`SyncHandler`、Minecraft object 或 Graal `Value`。
- backpressure 时可以合并同 key 的 value update，不能丢 close、slot click result、protocol error。

## 7. DOM event 与 UI 无关 event bus

### 7.1 当前 MUI 不是 DOM event system

当前 `Interactable` 只有各 Widget 的 mouse/key callback 和 `Result`；`ModularPanel` 按 focused/hovered widget 列表逐个调用：

- `api/widget/Interactable.java`
- `screen/ModularPanel.java:333-636`

`Widget` 中还留有 `TODO replace with proper event system`：`widget/Widget.java:68`。

它支持消费/停止某次 legacy dispatch，但没有：

- immutable target path；
- capture/target/bubble；
- listener table；
- `once/passive/AbortSignal`；
- scoped cleanup；
- custom event/detail；
- DOM 与 Widget identity 分离。

### 7.2 内置 MUI 的 DOM event core

DOM event dispatcher 应放进 MUI 主体，并支持 Java listener；即使没有 Graal，XML/Java UI 也能使用：

```text
MuiEventTarget
MuiEvent
MuiEventListener
MuiEventPhase
MuiListenerOptions
MuiAbortController
```

JS addon 只把 JS function 适配成同一个 listener contract，不实现第二套传播系统。

### 7.3 独立线程的语义限制

JS 运行在独立线程时，render/client thread 不能等待 JS，因此不能承诺所有浏览器默认行为都能被同步 `preventDefault()`：

- Java listener 与 no-JS Document 可在 client thread 同步完成。
- JS listener 在 document executor 上按 capture/target/bubble 顺序执行。
- 可延迟的 default action 可等待到下一个安全帧应用，但 client thread 本身不阻塞。
- vanilla slot click、IME、连续 camera drag 等即时 native action 由 preflight policy 决定，事后的 JS event 只能观察，不能谎称已经取消。
- `disabled`、`pointer-events`、modal ownership 等输入所有权必须预先投影到 client-thread snapshot。

### 7.4 UI 无关 event bus

MUI 主体可以提供一个不依赖 Widget/DOM 的 Java 8 scoped bus：

```text
MuiEventBus
  typed topic/event id
  subscribe -> AutoCloseable subscription
  priority/order
  owner scope auto-dispose
  thread-affinity assertion
  exception isolation/diagnostics
```

它用于 application/store/capability 之间的通知，不参与 DOM capture/bubble，也不跨网络。RTS 的具体 domain event 类型和 server workflow 仍归 RTS addon；MUI 只提供可复用机制，不拥有业务 schema。

## 8. XML、component、style 和 Java binding

### 8.1 MUI 主体内置

- secure StAX parser；
- immutable descriptor/compiler；
- namespace-aware native element registry；
- XML composite component、typed prop、slot、custom event；
- manifest/application/component registry；
- JSON selector stylesheet；
- Java action registry 和 store binding；
- resource reload generation/swap。

### 8.2 XML component 不要求 JS

XML 可以在 Java native element 上定义组件库：

```xml
<mui:component xmlns:mui="urn:modularui:core"
               xmlns:nfr="urn:neofontrender:components"
               name="rtsbuilding:search-toolbar">
  <mui:props>
    <mui:prop name="query" type="string" default="" />
  </mui:props>
  <mui:template>
    <nfr:row>
      <nfr:text-field value="{store:search.query}" />
      <nfr:button on:click="rtsbuilding:search.clear">Clear</nfr:button>
      <mui:slot name="actions" />
    </nfr:row>
  </mui:template>
</mui:component>
```

`on:click` 解析为 registry action id，不是 Java class/method 反射。可选 JS controller 由 Graal addon 接管；没有 addon 时，声明了 required JS 的 application 明确拒绝启动，而 XML-only/Java-bound application 正常运行。

### 8.3 parser 安全

使用 JDK StAX，不引入 DOM4J/JDOM/Xerces。必须关闭：

```text
DTD
external entity
external schema
XInclude
任意 URL/file/classpath escape
```

synced template 使用 mod-owned common resolver；client document 才能按 manifest policy 使用 resource pack resolver。

## 9. Java store 与 Pinia 风格 JS facade

MUI 主体提供 Java store/binding 基础，Graal addon 提供 Pinia 风格外观：

```text
MuiStoreDefinition
MuiStoreInstance
MuiStateSnapshot
MuiStoreAction
MuiStoreSubscription
MuiBinding
MuiSyncProjection
```

Java API 负责：

- store id/scope/lifecycle；
- immutable snapshot 与事务 `$patch` 等价物；
- getter dependency invalidation；
- action hooks；
- XML binding；
- sync projection。

JS facade 可提供 `defineStore()`、state/getters/actions、`$patch`、`$subscribe`、`$onAction`，但不依赖 Pinia/Vue，也不宣称兼容 Pinia plugin ecosystem。

Arc3D 不能替代 store、action、catalog 或 domain state。Arc3D 只适合 render/math/color adapter。

## 10. 依赖选择

### 10.1 MUI 主体不新增重型依赖

| 用途 | 选择 | 原因 |
|---|---|---|
| XML | JDK 8 `java.xml` StAX | client/server 都存在，无额外 jar |
| JSON | MUI 已有 Gson | manifest/style/descriptor |
| collection | MUI 已有 fastutil + JDK collections | registry/cache/listener table |
| hash | JDK `MessageDigest` SHA-256 | ProtocolPlan fingerprint |
| async abstraction | 自己的 bounded queue/executor SPI | 不引入 RxJava/Reactor |
| event bus | MUI 自己的 scoped typed bus | 不引入 Guava EventBus |

### 10.2 可选 Graal addon

| 用途 | 坐标 | 版本 | 分发 |
|---|---|---:|---|
| Polyglot API | `org.graalvm.polyglot:polyglot` | `25.3.4.1` | nested jar |
| JavaScript language/runtime | `org.graalvm.polyglot:js` | `25.3.4.1` | 全部 transitives as nested jars |

Graal addon：

- 仅 client side；
- Java 25 toolchain；
- 不 relocate，不 minimize；
- 保留 `META-INF/services`、language metadata 和 licenses；
- 每客户端一个 scheduler thread，一个 shared Engine，每 Document 一个 Context；
- 不提供 QuickJS/Javet 多后端抽象，先把一种现代 ECMAScript runtime 做完整。

dedicated server 不安装 addon，也不需要 JavaScript。

## 11. MUI API 修改清单

### 11.1 browser core

1. `MuiApplicationRegistry` / `MuiManifestLoader`
2. `MuiXmlCompiler` / `MuiComponentRegistry`
3. `MuiDocument` / `MuiNode` / `MuiElement`
4. `MuiElementRegistry` / `MuiNativeElementFactory`
5. `MuiStyleSheet` / `MuiStylePropertyRegistry`
6. `MuiEventTarget` / `MuiEventDispatcher`
7. `MuiEventBus`
8. `MuiStoreRegistry` / `MuiBindingEngine`
9. `MuiDocumentExecutor` SPI（direct Java 与 optional script executor）
10. batched Widget host mutation/layout/navigation invalidation

### 11.2 sync contract

1. `MuiDocumentMode`
2. `MuiProtocolPlan` / `MuiProtocolEntry`
3. `MuiProtocolTypeRegistry`
4. `MuiProtocolRegistrar`
5. `MuiTemplateContract` / fingerprint
6. UIFactory/UISettings 的 optional template contract
7. OpenGui packet 的 optional contract fields 与 reject path
8. `MuiSyncProjection`，把现有 SyncHandler 更新投影到 store
9. strict duplicate key/type/action/slot checks
10. 禁止 SyncedDocument 使用 implicit `auto_sync`

### 11.3 compatibility

- 旧 Java builder UI 不要求 manifest/XML，但客户端 Widget tree 会自动 adoption 为 DOM。
- 旧 UIFactory 可继续使用 auto sync；只对 SyncedDocument 禁止。
- 没有 Graal addon 时不加载任何 Graal class。
- common/browser API 不引用 `net.minecraft.client.*`、Graal 或 NFR/UIE class。
- client-only render adapter 继续使用 `@SideOnly(Side.CLIENT)`。

## 12. 测试门槛

### 12.1 现有同步回归

- legacy Java UIFactory 的 key 和 slot order 不变；
- old client-only screen 不创建 Document 时输入行为不变；
- old DynamicSyncHandler 行为不变；
- secondary panel open/close/reopen 不回归。

### 12.2 ProtocolPlan contract

- 相同 XML 两次编译 fingerprint 相同；
- attribute 顺序/无关 whitespace 不改变 fingerprint；
- handler type/version、顺序、slot ordinal 改变必须改变 fingerprint；
- duplicate handler/action/panel/slot ordinal 失败；
- server/client mismatch 在构建 screen 前拒绝；
- synced document 中出现 implicit handler 失败；
- client visual DOM 重排不改变 protocol plan。

### 12.3 server matrix

- Java 8 dedicated server 无 Graal class 仍能解析并执行 ProtocolPlan；
- server 不加载 `net.minecraft.client.*`；
- server 不读取 resource pack override 作为 protocol source；
- no-JS client 与 JS client 可连接同一 server，协议 fingerprint 相同。

### 12.4 data/event/thread

- SyncHandler update -> Java store -> binding；
- SyncHandler update -> JS queue -> store，且不阻塞 client thread；
- DOM capture/target/bubble 顺序；
- scoped listener/timer/subscription 在 unmount 时释放；
- immediate 与 deferrable default action 语义明确；
- queue overflow 不丢 protocol error/close/critical action。

## 13. 推荐实施顺序

### Phase 0：先加同步契约测试

- 为现有 BFS key、lock、duplicate、slot order、secondary panel 建测试。
- 冻结 `ProtocolPlan` canonical encoding 和 fingerprint。
- 不改网络行为。

### Phase 1：MUI browser core，无 JS

- manifest/XML/component/native registry；
- Java DOM/event/event bus/store/binding；
- JSON stylesheet；
- client-only XML Document。

### Phase 2：SyncedTemplate

- ProtocolPlan common compiler；
- server skeletal panel + client RenderPlan；
- strict registrar；
- OpenGui fingerprint handshake；
- sync projection/store bridge。

### Phase 3：可选 Graal addon

- client-only packaging spike；
- document executor/ESM/DOM facade；
- Pinia-style facade；
- sandbox/watchdog/queues。

### Phase 4：NFR component pack 和 RTS vertical slice

- NFR native elements/theme token adapter；
- UIE camera/input/navigation capability；
- RTS storage page + one synced value/list + one real slot；
- dedicated server/no-JS/JS 三种部署验收。

## 14. 审核项

请按以下顺序确认：

1. 是否接受“除 GraalJS 外全部进入 MUI 主体”的制品边界。
2. 是否接受所有传统/声明式 UI 都有客户端 Document，服务端永不复制 DOM。
3. 是否接受普通动态数据由单个 `DocumentSyncHandler` 多路复用，真实 slot/固定 handler 才使用 ProtocolPlan。
4. 是否接受 protocol XML 只能来自 mod-owned common resources，resource pack 只能改允许的视觉资源。
5. 是否接受 OpenGui 增加 ProtocolPlan fingerprint 校验，不一致直接拒绝打开。
6. 是否接受真实 slot order 是协议的一部分，不能随视觉 DOM 重排。
7. 是否接受 DOM event 与 UI 无关 event bus 都在 MUI core，但 RTS domain schema 仍属于 RTS addon。
8. 是否接受 store 只投影现有同步数据，不新增 JS 网络真相源。
9. 是否接受 GraalJS 独立客户端线程导致部分 native default action 不能被事后 `preventDefault()`。
10. 是否先完成 Phase 0-2，再开始 RTS 页面和 Graal 集成。

## 15. 最终判断

把 XML/DOM/style/event/store 内置 MUI 不会天然破坏现有同步；真正危险的是让动态 DOM 继续驱动现有 BFS 自动 handler/slot 拓扑。

普通动态数据应通过一个构建期注册的 `DocumentSyncHandler` 多路复用 endpoint channel；这样运行时 DOM
增删只影响 binding/subscription，不改变 `PanelSyncManager` topology。只有真实 slot 和其他固定 container
协议才需要 `ProtocolPlan/SlotTable + explicit stable key + order + fingerprint`。现有
`PanelSyncManager` 和 `PacketSyncHandler` 可以继续作为底层 transport，但必须补 key collision 和 payload
limit。GraalJS 不参与服务端，也不决定网络或 slot 拓扑；它只是客户端 Document 的可选行为层。
