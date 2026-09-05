# ModularUI Browser Runtime 架构审核规格

状态：提案，等待审核；本文不代表已经实现

日期：2026-08-29

> 2026-08-29 同步审计修订：本文最初按“完整 browser runtime 位于 NFR 独立 addon、v1 仅 client-only”编写。
> 用户随后决定把 XML/DOM/event/style/store 等非 JS 能力内置 ModularUI，仅将 GraalJS 保留为可选客户端扩展。
> 最新制品边界、动态数据通道、固定 slot 协议与实际类级改造方案以
> `docs/modularui-browser-runtime-75f86da-code-change-plan.md` 为准；本文中与该文冲突的早期
> SyncedDocument/ProtocolPlan 设计视为历史提案。

目标平台：Minecraft 1.12.2 / Cleanroom / Java 25

分析基线：

- NFR/UIE：`a01a5cf8d5764e70711b27c1b558ec9de44046eb`
- ModularUI：`D:/Projects/sfr/other_mods/ModularUI`，commit `75f86dabac6f91a224eb2a0716bf3838ff38f6a3`，版本 `3.2.0-nfr.1`
- RTSbuilding 参考项目：`853e8224`，Minecraft 1.21.1 / NeoForge
- 配套功能分析：`docs/rts-building-addon-and-modularui-dom-events-plan.md`

本文回答四个问题：

1. ModularUI 能否成为类似浏览器的动态 UI runtime。
2. XML、JSON stylesheet、ES module、Java native element 应如何分工。
3. 需要修改 ModularUI、NFR、UIE 的哪些 API，以及需要哪些库。
4. 旧 RTSbuilding 的哪些结构能迁移，哪些必须删除或重写。

## 1. 待审核的总决策

建议批准以下目标结构：

```text
XML                        初始 Document、页面和 XML composite component
JSON stylesheet            selector、cascade、布局、视觉和状态样式
ES module / GraalJS         动态 DOM、事件监听、Pinia 风格 store、Promise、timer
Java native element         文本输入、物品槽、世界 viewport 等即时控件
Java host capability        camera、input、network、Minecraft 和 addon 业务能力
ModularUI                   Widget render tree、Panel、布局、焦点、绘制、输入接入
UIE                         camera/input/navigation 平台，不拥有 RTS 页面
NFR                         字体、视觉 token 和通用 MUI native component pack
RTS addon                   页面、组件、业务、协议、服务端任务和兼容层
```

关键结论：

- 不引入或移植旧 RTSUI runtime。
- 不把浏览器 runtime 塞进 UIE。
- XML/DOM/event/style/store/manifest 内置 ModularUI Java 8 主制品。
- 不把 GraalJS 塞进 ModularUI 的 Java 8 主制品；另建可选 Java 25 client addon：暂名 `modularui-js-graal`。
- RTS 仍是另一个独立 client+server addon；它依赖 ModularUI browser core、UIE 公共 API 和 NFR/MUI 组件，只有声明 JS 的页面才要求 Graal addon。
- 先实现 client-only `ModularScreen`，随后在 Graal 接入前实现受约束的 SyncedDocument/ProtocolPlan。
- UI 脚本状态采用 Pinia 风格 API，但不依赖 Pinia 或 Vue。
- Arc3D 只用于渲染实现和少量数学/颜色操作，不承担 store、事件、catalog 或领域状态。

### 1.1 审核决策表

| ID | 提案 | 推荐结论 | 不批准的影响 |
|---|---|---|---|
| D01 | 非 JS browser core 内置 MUI，GraalJS 是独立可选 client addon | 批准 | 全部独立会让无 JS XML 重复维护 host；全部内置会污染 Java 8/server 分发 |
| D02 | XML 只描述结构和 component template | 批准 | XML 内混入脚本/Java 反射后无法安全热重载 |
| D03 | 样式使用受控 JSON stylesheet，不实现完整 CSS | 批准 | 完整 CSS parser/layout 成本远超 RTS 需求 |
| D04 | 需要 JS 的页面使用 GraalJS `25.3.4.1` 和 ESM | 批准 | Java/XML-only 页面不应被迫携带 Graal |
| D05 | store API 参考 Pinia，但自己实现最小 reactivity | 批准 | 直接引入 Pinia 会连带 Vue runtime |
| D06 | 每客户端一个专用 JS 线程，Context 按 Document 隔离 | 批准 | render thread 上执行脚本会造成不可控卡顿 |
| D07 | render thread 永不等待 JS | 批准 | 同步等待会把脚本延迟传染到每一帧 |
| D08 | DOM event 与 application event bus 严格分离 | 批准 | 输入传播、业务通知和网络事件会互相污染 |
| D09 | resource pack 默认不能覆盖 JS/manifest | 批准 | 服务器资源包或普通材质包会获得代码执行能力 |
| D10 | Graal 依赖以 nested jars 分发，不 relocate/minimize | 批准 | shading 可能破坏 ServiceLoader 和 Graal 资源发现 |
| D11 | synced XML 通过 ProtocolPlan/stable key/slot table/fingerprint 受约束支持 | 批准 | 无约束 DOM 会让 client/server handler 与 slot 协议失配 |
| D12 | Arc3D 类型不进入 DOM/store/network 公共 DTO | 批准 | UI 业务层将被图形实现细节绑定 |

## 2. 当前代码架构的事实

### 2.1 ModularUI 当前能力

ModularUI 已经提供适合作为 render tree 的基础，但不是浏览器 runtime。

| 当前模块 | 已有能力 | 缺口或约束 |
|---|---|---|
| `IWidget` / `AbstractWidget` | Widget 状态、Area、theme、lifecycle | 没有持久 DOM node、attribute、listener table |
| `AbstractParentWidget` | 动态增删 child；late initialise 和 dispose | 没有 component boundary、slot、keyed reconciliation |
| `WidgetTree` | resize、draw、update、sync collection | 不是 logical DOM，也没有 mutation transaction |
| `StandardResizer` | pixel/relative unit、margin/padding | 没有 selector/computed style 层 |
| Flow/Grid/Scroll | Row、Column、wrap、Grid、滚动 | 能作为 style property 的布局后端 |
| `PanelManager` | panel stack、z-order、close/dispose、安全延迟销毁 | 可直接承载 modal/window，不再做平行 layer stack |
| `ClientScreenHandler` | hover list、click-through、legacy input dispatch | 没有 capture/target/bubble |
| `Interactable.Result` | handled/stop 一类即时结果 | 不是 DOM `Event` |
| `IGuiAction` | Screen/Widget callback | 不是 scoped listener，也不能动态卸载/传播 |
| `NavigationInfo` | role、label、actions、focusable | 已可导出给 UIE semantic tree |
| `ThemeManager` | Gson JSON、theme inheritance、resource reload | theme 是 widget type theme，不是 selector stylesheet |
| `DrawableSerialization` | 显式 drawable type registry | 可参考其 qualified registry 方式 |
| `PanelSyncManager` | 固定构建期 sync handler 和 action | 构建后锁定，不适合任意动态 DOM |

当前 `assets/modularui/guis/test.json` 没有对应通用 loader，不能视为已有声明式 UI。

ModularUI 使用 Java 17 toolchain/Jabel 语法，但 `options.release=8`，公开制品是 Java 8 bytecode。这个兼容承诺决定了 GraalJS 必须位于独立 Java 17+ 制品。

### 2.2 NFR 主模组当前 UI 组织

NFR 的 Java UI 已形成清晰的组织方式，可以作为 RTS addon 和 XML component pack 的目录模板：

```text
client/gui/
  component/base/          通用原子和组合控件
  component/business/      业务组合组件
  layouts/                 页面级布局规则
  model/                   draft/value/session state
  pages/                   route/page metadata
  views/                   页面内容
```

可复用的现有控件包括 `NfrTextButton`、`NfrContentButton`、`NfrCycleButton`、
`NfrOptionDropdown`、`NfrDecimalSlider`、`NfrLabeledSlider`、
`NfrLabeledTextField`、`NfrScrollablePane`、`NfrOptionsGrid` 和
`NfrResponsiveFieldGrid`。

但这些类目前位于 `neofontrender.client.gui.*`，不是稳定 component registration API。
浏览器 runtime 不应通过反射构造它们。NFR 需要新增显式 component contributor，或由新 addon
提供同等视觉的 native element factory。

### 2.3 UIE 当前公共能力

UIE 已经覆盖 RTS 需要的大部分平台能力：

| API | 已有能力 | RTS 用法 |
|---|---|---|
| `CameraApi` | provider、modifier、lens、projection、frustum、screen ray、picking、collision、observer | 注册 `rts:builder_camera` provider |
| `CameraSession` | 原子控制租约 | RTS screen/session 打开时获取，任意退出路径关闭 |
| `CameraProvider` | authoritative frame、view ownership、UI view proxy | 替换旧 camera entity 渲染方案 |
| `InputApi` | device/binding/context/observer、immutable frame | RTS mode claim/block camera/player actions |
| `InputAction` | translate/look/zoom、attack/use/pick、GUI navigation | v1 不必新增 RTS 专属 action |
| `UiNavigationApi` | provider、semantic tree session、focus/action | 让手柄/键盘导航动态 DOM |
| ModularUI adapter | capture MUI navigation tree、pointer capture、reveal/back | 新 DOM element 只需正确声明 `NavigationInfo` |
| HUD compositor | surface、z-order、clip、focus、pointer capture | 只用于 screen 外通知，不承载 RTS 主界面 |

DOM runtime 自己负责 input event propagation。UIE navigation 负责把外部输入转换为语义 action，
再从 MUI adapter 进入同一 element activation path；两者不能各自决定一次点击的业务结果。

### 2.4 旧 RTSbuilding 的真实规模

参考项目不是一个 UI demo，而是完整 client/server 产品。生产源码约 674 个 Java 文件：

| 区域 | 约计 Java 文件 | 含义 |
|---|---:|---|
| client | 344 | screen、renderer、controller、camera、input、state、theme |
| server | 319 | session、service、storage、task、workflow、persistence |
| network | 128 | camera、builder、craft、storage、plugin 等 payload |
| `uiCore` | 113 | reducer、action/state、event router、纯模型 |
| `uiKit` | 130 | canvas、layout、theme、animation、scroll |

旧项目已有良好的服务端边界：

```text
network intent
  -> service facade / validation pipeline
  -> storage session / progression / protection
  -> shared-budget durable task engine
  -> effect accumulator and commit barrier
  -> S2C state/effect payload
```

这个边界应保留。不能把世界修改、存储访问、合成或任务调度迁进 JS。

## 3. 目标仓库与制品边界

### 3.1 推荐制品

```text
ModularUI fork repository
  com.cleanroommc:modularui:<next-nfr-version>
    Java 8 bytecode
    DOM host SPI、native element SPI、event source hooks、style adapter hooks
    不依赖 GraalJS

NFR repository
  neofontrender:<main>
    字体、Arc3D facade、NFR theme 和 component contributor

  neofontrender.addons:modularui-browser-runtime:0.1.0
    Java 25
    manifest/XML/style/DOM/reactivity/store/GraalJS/scheduler
    client-only mod

  neofontrender.addons:rts-building:<future>
    Java 25
    client+server mod
    RTS application、native elements、capabilities、协议和业务

UIE addon
  neofontrender-ui-enhancements
    camera/input/navigation 公共平台
```

`modularui-browser-runtime` 是独立 addon，但产品语义上仍是“ModularUI 的浏览器宿主”。
它不归 UIE 所有，也不要求所有 ModularUI 用户下载约 67 MiB 的 Graal 运行库。

### 3.2 推荐 package 结构

```text
com.cleanroommc.modularui.api.browser
  MuiDocumentHost
  MuiElementRegistry
  MuiNativeElementFactory
  MuiElementDescriptor
  MuiStylePropertyRegistry
  MuiInputEventSource
  MuiMutationSink
  MuiRegistration

neofontrender.addons.muibrowser
  api/
    application/
    capability/
    event/
    script/
    store/
  manifest/
  xml/
  dom/
  component/
  style/
  renderbridge/
  script/graal/
  scheduler/
  reload/
  diagnostics/

neofontrender.api.client.mui
  NfrMuiComponentContributor
  NfrMuiThemeTokens

neofontrender.addons.rtsbuilding
  client/application/
  client/component/nativeelement/
  client/component/xml/
  client/capability/
  client/camera/
  client/model/
  common/domain/
  common/shape/
  common/blueprint/
  network/
  server/session/
  server/service/
  server/storage/
  server/task/
  server/workflow/
  compat/
```

## 4. 总体运行结构

```text
Minecraft client/render thread
  ModularScreen / PanelManager
  Widget render tree
  hit-test / native focus / text input / drag / OpenGL
  UIE navigation adapter
       | immutable InputEventPacket / HostEventPacket
       v
bounded MPSC ingress queue
       v
MuiScriptScheduler thread (每客户端一个)
  one shared Graal Engine
  one Context per live Document
  logical DOM
  capture/target/bubble listeners
  Pinia-style reactive stores
  Promise jobs / timers / ESM
  mutation transaction builder
       | immutable MutationBatch / HostCommandBatch
       v
bounded client-thread apply queue
       v
safe frame boundary
  validate generation + node ids
  atomic apply to Widget tree
  style/layout/navigation revision invalidation
```

### 4.1 所有权规则

- logical DOM、JS object、Graal `Value` 只属于 script thread。
- Widget、Minecraft、OpenGL、MUI `Area` 只属于 client/render thread。
- 跨线程只传 immutable Java DTO，不传 Widget、DOM object 或 Polyglot `Value`。
- 一个 Document 对应一个 `Context`、一个 origin、一个 capability grant 和一个 generation。
- 所有 mutation 带 document id、generation 和 sequence；旧 generation 的 batch 直接丢弃。
- render thread 永不 `join()`、`Future.get()` 或等待 JS 回答。

### 4.2 Document 生命周期

```text
DISCOVERED
  -> VALIDATED          manifest/schema/capability/component graph 成功
  -> PARSED             XML/style 编译成 immutable descriptors
  -> STARTING           建立 Context，执行 entry ESM
  -> MOUNTING           生成第一批 render mutations
  -> ACTIVE             input/event/timer/reload 正常工作
  -> RELOADING          建新 generation，旧 generation 仍渲染
  -> ACTIVE             新 generation 首批提交后原子切换
  -> CLOSING            停止接收事件、取消 timer/subscription
  -> CLOSED             close Context，释放 Widget/Panel/capability

任意阶段 -> FAILED      显示 native error panel，不执行半成品脚本
```

热重载采用 build-new-then-swap。解析或脚本启动失败时保留旧 Document，不把屏幕变成半棵树。

## 5. DOM 模型

### 5.1 v1 node 类型

- `Document`
- `Element`
- `Text`
- `DocumentFragment`
- 内部 `ComponentRoot`，不向 stylesheet 暴露完整 Shadow DOM

不实现 HTML parser、HTML quirks、iframe、canvas DOM、custom element upgrade algorithm、
MutationObserver 全规范或 WHATWG Shadow DOM。

### 5.2 v1 JS DOM API

| 类别 | API |
|---|---|
| 查询 | `getElementById`、`querySelector`、`querySelectorAll`、`matches`、`closest` |
| 创建 | `createElementNS`、`createTextNode`、`createDocumentFragment` |
| 结构 | `append`、`prepend`、`before`、`after`、`replaceWith`、`remove` |
| 属性 | `getAttribute`、`setAttribute`、`removeAttribute`、`toggleAttribute` |
| class | `classList.add/remove/toggle/contains` |
| 内容 | `textContent`；不提供 `innerHTML` |
| 事件 | `addEventListener`、`removeEventListener`、`dispatchEvent` |
| 状态 | `focus`、`blur`、`click`、`hidden`、`disabled` |
| 调度 | `queueMicrotask`、`setTimeout`、`clearTimeout`、`requestAnimationFrame` |

`innerHTML` 暂不实现，避免再维护一套 fragment parser、安全策略和 script 执行规则。

### 5.3 identity 与 reconciliation

- XML `id` 在 Document 内唯一。
- `key` 用于同一 parent 下的稳定 child identity。
- element id 使用 runtime 分配的 64-bit handle，不使用 Widget BFS 序号。
- component instance 有独立 instance id 和 scoped store/lifecycle cleanup scope。
- DOM mutation 先修改 logical tree，再合并为最小 `MutationBatch`。
- v1 不追求 React/Vue virtual DOM diff；直接 DOM mutation 是主模型。

## 6. XML application 与 XML component

### 6.1 application XML

```xml
<?xml version="1.0" encoding="UTF-8"?>
<mui:document xmlns:mui="urn:modularui:core"
              xmlns:nfr="urn:neofontrender:components"
              xmlns:rts="urn:rtsbuilding:components">
  <mui:head>
    <mui:title>RTS Building</mui:title>
  </mui:head>
  <mui:body id="app" class="rts-screen">
    <rts:world-viewport id="world" input-policy="world" />
    <rts:top-command-bar id="commands" />
    <rts:storage-workspace id="storage" />
    <nfr:modal-host id="modals" />
  </mui:body>
</mui:document>
```

XML 只创建元素、属性、文本、slot 和静态绑定声明。禁止：

- inline Java class name；
- XML 反射调用；
- `onclick="javascript:..."`；
- 文件系统、URL 或任意 classpath 资源；
- DTD、DOCTYPE、external entity 和 XInclude。

解析使用 JDK StAX，并显式设置：

```text
XMLInputFactory.SUPPORT_DTD = false
javax.xml.stream.isSupportingExternalEntities = false
XMLConstants.ACCESS_EXTERNAL_DTD = ""
XMLConstants.ACCESS_EXTERNAL_SCHEMA = ""
```

未知 namespace/tag/attribute 在开发模式是错误，在生产模式仍默认 fail document，不静默降级为普通容器。

### 6.2 XML 定义 component library

```xml
<mui:component xmlns:mui="urn:modularui:core"
               xmlns:nfr="urn:neofontrender:components"
               xmlns:rts="urn:rtsbuilding:components"
               name="rts:search-toolbar">
  <mui:props>
    <mui:prop name="query" type="string" default="" reflect="true" />
    <mui:prop name="busy" type="boolean" default="false" />
  </mui:props>
  <mui:events>
    <mui:event name="query-change" detail="rts:QueryChange" bubbles="true" />
  </mui:events>
  <mui:template>
    <nfr:row class="search-toolbar">
      <nfr:text-field value="{prop:query}" event="query-change" />
      <nfr:spinner hidden="{not:prop:busy}" />
      <mui:slot name="actions" />
    </nfr:row>
  </mui:template>
  <mui:style src="rtsbuilding:mui/style/search-toolbar.style.json" />
  <mui:controller module="rtsbuilding:mui/component/search-toolbar.mjs" />
</mui:component>
```

XML component：

- 是 registry 中的新 qualified element type，不生成 Java class；
- 可以组合 native element 和其他 XML component；
- 支持 typed props、default、reflect、named/default slot、custom event；
- 可选 ESM controller 提供 `setup/mounted/updated/unmounted`；
- stylesheet 默认 scope 到 component root；
- capability 永远继承 application grant，component 不能扩大权限。

namespace URI 与 registry owner 的映射由 component library manifest 显式导出。例如
`urn:rtsbuilding:components` 映射到 `rtsbuilding`，因此
`{urn:rtsbuilding:components}search-toolbar` 的 canonical element id 是
`rtsbuilding:search-toolbar`。XML 前缀 `rts` 只是文档内别名，不能作为 identity。

递归 component dependency graph 必须在 resource reload 时检测循环。默认最大展开深度 64。

### 6.3 native element

native element 只由 Java 显式注册：

```java
MuiRegistration register(
        ResourceLocation qualifiedName,
        MuiElementDescriptor descriptor,
        MuiNativeElementFactory factory);
```

factory 接收受控 descriptor/initial props，返回 Widget host adapter。manifest 不能填写 factory class name。

必须 native 的首批元素：

- text field / IME；
- item/fluid slot；
- scroll viewport；
- modal/panel host；
- world viewport；
- drag handle、resize handle；
- tooltip anchor；
- 对连续相机拖动有即时反馈的 input surface。

## 7. application manifest

资源路径固定为：

```text
assets/<modid>/mui/manifest.json
```

示例：

```json
{
  "formatVersion": 1,
  "runtime": {
    "api": "1.x",
    "ecmascript": "latest"
  },
  "elementNamespaces": [
    {
      "uri": "urn:rtsbuilding:components",
      "owner": "rtsbuilding"
    }
  ],
  "applications": [
    {
      "id": "rtsbuilding:builder",
      "document": "rtsbuilding:mui/application/builder.xml",
      "styles": [
        "neofontrender:mui/style/base.style.json",
        "rtsbuilding:mui/style/builder.style.json"
      ],
      "entry": "rtsbuilding:mui/application/builder.mjs",
      "components": [
        "rtsbuilding:top-command-bar",
        "rtsbuilding:storage-workspace"
      ],
      "nativeElements": [
        "rtsbuilding:world-viewport",
        "neofontrender:item-slot"
      ],
      "capabilities": [
        "rtsbuilding:camera",
        "rtsbuilding:commands",
        "rtsbuilding:state"
      ]
    }
  ],
  "components": [
    {
      "name": "rtsbuilding:search-toolbar",
      "source": "rtsbuilding:mui/component/search-toolbar.xml"
    }
  ]
}
```

manifest 是注册表，不是权限文件。最终 grant 是：

```text
manifest request
  intersection application owner policy
  intersection runtime global policy
  = document capability grant
```

manifest、component name 和 resource id 必须属于 owner namespace，除非依赖方显式导出。

## 8. JSON stylesheet

现有 MUI theme 继续作为 drawable/widget theme 后端。新 stylesheet 是其上的 selector/computed-style 层。

```json
{
  "formatVersion": 1,
  "namespaces": {
    "mui": "urn:modularui:core",
    "nfr": "urn:neofontrender:components",
    "rts": "urn:rtsbuilding:components"
  },
  "variables": {
    "--surface": "#d9181b20",
    "--accent": "#55c2ff",
    "--gap": 6
  },
  "rules": [
    {
      "selector": "rts|storage-workspace",
      "style": {
        "position": "absolute",
        "left": 8,
        "right": 8,
        "bottom": 8,
        "height": 224,
        "flow": "column",
        "gap": "var(--gap)",
        "background": { "type": "color", "color": "var(--surface)" }
      }
    },
    {
      "selector": ".toolbar nfr|button:hover",
      "style": { "backgroundColor": "#80445a68" }
    }
  ]
}
```

### 8.1 v1 selector

支持：

- qualified tag；
- `#id`；
- `.class`；
- `[attribute]` 和 `[attribute=value]`；
- descendant 和 direct-child combinator；
- `:hover`、`:active`、`:focus`、`:focus-visible`、`:disabled`、`:checked`；
- `:root` 和 component scoped root。

不支持通用 CSS parser、复杂 `:has()`、CSS animation grammar、媒体查询语言和浏览器单位全集。
selector 中的 `rts|storage-workspace` 使用 stylesheet 顶层 `namespaces`，前缀本身同样不参与
element identity。

### 8.2 cascade

顺序固定为：

```text
MUI defaults
  < NFR base theme
  < imported component stylesheet
  < application stylesheet
  < id/class/tag specificity
  < inline structured style object（仅 JS API，不来自 XML 字符串）
```

相同 specificity 使用 manifest/import order，再使用 rule order。所有 computed property 有注册的 parser、
inheritance flag、initial value、layout/paint invalidation kind 和 Widget adapter。

### 8.3 首批 style property

- geometry：`position`、`left/top/right/bottom`、`width/height`、min/max；
- box：margin、padding、border、background、opacity、visibility；
- layout：flow、wrap、gap、align、justify、grid rows/columns；
- text：color、font size、line height、align、wrap、ellipsis；
- input：pointer events、cursor、focusable、tab order、input policy；
- scroll：overflow x/y、scrollbar；
- MUI bridge：theme key、drawable、tooltip、navigation role。

## 9. GraalJS 与 ESM

### 9.1 版本

固定直接依赖：

```gradle
implementation("org.graalvm.polyglot:polyglot:25.3.4.1")
runtimeOnly("org.graalvm.polyglot:js:25.3.4.1")
```

`org.graalvm.polyglot:js` 是 POM，会引入 `js-language` 和 Truffle runtime。
GraalJS 当前文档声明兼容 ECMAScript 2026。生产配置使用：

```text
js.ecmascript-version=latest
js.load=false
js.print=false（由受控 console 替代）
js.polyglot-builtin=false
js.graal-builtin=false
```

不启用 `staging`、Nashorn compatibility、Node mode 或 `javax.script`。

实际 `js-language-25.3.4.1.jar` class major version 是 61，即 Java 17 bytecode；Java 25 满足要求，
Java 8 ModularUI 制品不满足。

### 9.2 module resolver

- 只解析 manifest entry 和其静态/动态 import graph；
- module id 使用 `ResourceLocation`；
- 相对 import 只允许在当前 owner 的 `assets/<modid>/mui/` 内归一化；
- 禁止绝对路径、`..` 逃逸、HTTP(S)、`file:`、Node builtin 和 classpath 扫描；
- source 在 resource reload 阶段读入 immutable cache，JS thread 不直接读文件；
- v1 直接运行现代 `.mjs`，不引入 runtime Babel、TypeScript 或 npm loader。

### 9.3 全局对象

提供：

- ECMAScript builtins；
- `document`、`Event`、`CustomEvent`；
- `console` 的受控实现；
- timer/microtask/animation-frame；
- `defineStore/useStore`；
- `watch`、`watchEffect`、`computed`；
- `capabilities` 的只读 proxy；
- application event bus 的 scoped facade。

不提供：

- `Java`、`Polyglot`、`load/read`；
- `Minecraft`、Widget、Forge event bus；
- filesystem、environment、process、socket；
- 浏览器 `fetch`、WebSocket、localStorage；
- Node `require`、Buffer、npm package resolution。

## 10. Pinia 风格 store

### 10.1 为什么只参考 Pinia

Pinia 的 `defineStore/state/getters/actions/$patch/$subscribe` 很适合声明式组件，比所有交互都写 reducer 更简洁。
但 Pinia 本身依赖 Vue reactivity。直接引入会把 Vue runtime 和浏览器假设带入 GraalJS，没有必要。

runtime 自己用 ES `Proxy`、effect dependency graph 和 mutation transaction 实现最小 store。

### 10.2 API

```js
export const useStorageStore = defineStore("rts:storage", {
  state: () => ({
    query: "",
    page: 0,
    loading: false,
    entries: []
  }),

  getters: {
    visibleCount: state => state.entries.length,
    canPrevious: state => state.page > 0
  },

  actions: {
    async refresh() {
      this.loading = true
      try {
        const page = await capabilities.state.requestStoragePage({
          query: this.query,
          page: this.page
        })
        this.$patch({ entries: page.entries })
      } finally {
        this.loading = false
      }
    }
  }
})
```

首批 API：

- `defineStore(id, options)`；
- `useStore(id or definition)`；
- state property read/write；
- computed getters；
- sync/async actions；
- `$patch(object|function)`；
- `$reset()`；
- `$subscribe(callback, options)`；
- `$onAction(callback)`；
- `$dispose()`；
- dev-only snapshot/inspection。

store 不会像 Vue template 那样自动“渲染整个 XML”。动态视图通过同一 runtime 的轻量 reactive
primitive 更新真实 DOM：

```js
const storage = useStorageStore()
const count = document.querySelector("#visible-count")

const stop = watchEffect(() => {
  count.textContent = String(storage.visibleCount)
  count.toggleAttribute("data-loading", storage.loading)
})

document.addEventListener("mui:unmount", stop, { once: true })
```

`watchEffect` 读取 store getter/state 时收集 dependency；effect 重新运行产生的 DOM mutation 与当前
macrotask 合并。component controller 的 lifecycle scope 会自动停止其 effect，因此实际 component
通常不需要手写 `mui:unmount`。v1 XML 不实现任意 JavaScript expression；`{prop:name}` 只允许
component prop/slot 的结构绑定，application store 绑定留在 ESM controller，避免在 XML 中再造一门
不完整的脚本语言。

### 10.3 scope 与真相源

- 默认 Document scope：同一 Document 创建的主 Panel、modal 和浮动 Panel 共享 store。
- 两个独立 Document 对应两个 Graal Context，绝不直接共享 store object 或 Polyglot `Value`。
- 跨 Document 共享状态通过 application event bus 或 Java capability 的 structured-clone snapshot；
  每个 Context 保留自己的 reactive projection。
- component local state 不自动进入全局 store。
- store 只保存 JSON-like value：null、boolean、number、string、array、plain object 和受控 typed handle。
- 不保存 Java object、Widget、DOM node、Graal `Value`、Minecraft registry object。
- 服务端状态以 Java/server 为真相源；S2C snapshot 更新 store，store 不能绕过 command capability 改世界。

### 10.4 reducer 的位置

旧 RTS `uiCore` 的 reducer/action/state 有两种迁移方式：

- 普通筛选、分页、开关、窗口状态：迁移成 Pinia 风格 store/action。
- blueprint capture、quick-build、workflow 等严格状态机：保留纯 Java reducer，或在 JS store action 内调用纯函数 reducer。

不要求统一成一种模式。catalog/registry 仍是应用扩展语义，不由 Arc3D 替代。

## 11. 三套事件机制

### 11.1 Native input preflight

运行于 client thread，用于不能等待脚本的行为：

- hit-test；
- pointer capture；
- native focus；
- text field/IME；
- scroll position；
- drag/resize；
- 连续 camera pan/orbit/zoom；
- 根据静态 computed style 决定 input 是否被 UI 占用。

它不是公开 application event bus。

### 11.2 DOM event

运行于 JS thread，对 logical DOM 执行 capture -> target -> bubble。

支持：

- `addEventListener(type, listener, { capture, once, passive, signal })`；
- `stopPropagation`、`stopImmediatePropagation`；
- `preventDefault`、`defaultPrevented`；
- `composedPath`（component boundary 的受控版本）；
- pointer、click、input、change、keydown/up、focus、submit、custom event；
- listener scope 在 unmount/AbortSignal 时自动释放。

独立线程意味着不能谎称完全等价浏览器同步语义：

| 事件 | 策略 |
|---|---|
| `click`、`submit`、`contextmenu`、`beforetoggle` | 默认 action 延迟到 JS propagation 后，可取消，通常下一帧执行 |
| pointer move、drag、scroll、text edit | native immediate；JS 收到观察/业务事件，`preventDefault` 不能回滚已应用的即时反馈 |
| input ownership | 由 render snapshot/computed style 同步决定，不等待 JS |

### 11.3 UI 无关 application/domain event bus

它用于同一 addon 内模块解耦，例如：

```text
rts:camera/session-opened
rts:storage/snapshot-updated
rts:task/progress
rts:blueprint/selected
rts:plugin/changed
```

规则：

- event type 是 namespaced id，不是 DOM target/type；
- payload 是 immutable schema DTO；
- subscribe 返回 `AutoCloseable`/scope registration；
- 明确 thread policy：`CLIENT`、`SERVER`、`SCRIPT`；
- 同一 bus 不自动跨网络；C2S/S2C payload adapter 明确桥接；
- 不用于 pointer/key propagation；
- 不使用 Forge 全局 event bus 作为内部业务总线；
- JS 只能通过 capability 获得允许的 topic view。

推荐核心接口：

```java
public interface MuiApplicationBus {
    MuiSubscription subscribe(MuiEventType<?> type, MuiEventExecutor executor,
                              MuiEventListener<?> listener);
    void publish(MuiEventType<?> type, Object immutablePayload);
}
```

## 12. 跨线程调度与 transaction

### 12.1 scheduler

- 每个 Minecraft client 一个命名 daemon thread：`MUI Script Runtime`。
- 一个 shared `Engine`，每个 Document 一个 thread-confined `Context`。
- 另有一个 JDK watchdog scheduler；它不执行 JS，只在 hard deadline 到达时调用
  `Context.interrupt(Duration)`，失败后 `close(true)`。
- ingress 是有界 MPSC queue，按 document 保序。
- task 类别：lifecycle、input、host event、timer、animation frame、reload、close。
- close/reload 有高优先级，连续 pointer move 可 coalesce。
- timer 不创建线程，只把 due task 放回 scheduler queue。

### 12.2 mutation batch

一个 JS macrotask 内的 DOM 和 store 修改合并成 transaction：

```text
create/remove/move node
set/remove attribute
set text
set listener flags relevant to native preflight
set pseudo/native state request
open/close panel request
focus/scroll request
host command
```

client thread 应用前验证：

- document generation；
- monotonic sequence；
- parent/child handle 存在性；
- element descriptor 和 prop type；
- batch/node/字符串/payload 上限；
- capability command grant。

任何单条结构 mutation 非法时拒绝整个 batch，避免逻辑树和 Widget 树部分分叉。

### 12.3 默认预算

以下是 v1 推荐默认值，实际实现前以 benchmark 校准：

| 项目 | 默认限制 |
|---|---:|
| live DOM nodes / Document | 20,000 |
| component expansion depth | 64 |
| mutation ops / batch | 4,096 |
| pending ingress tasks | 4,096 |
| active timers / Document | 1,024 |
| event/custom DTO serialized size | 1 MiB |
| script statement limit / macrotask | 1,000,000 |
| soft macrotask warning | 8 ms |
| hard watchdog | 50 ms，关闭该 Context |

render thread 绝不因为 soft/hard watchdog 同步等待。
每个 macrotask 前调用 `Context.resetLimits()`，因此 statement limit 是每任务预算，而不是
Document 生命周期累计预算。

## 13. Host capability

### 13.1 安全 Context

最低配置：

```java
Context.newBuilder("js")
    .allowHostAccess(HostAccess.NONE)
    .allowHostClassLookup(name -> false)
    .allowHostClassLoading(false)
    .allowPolyglotAccess(PolyglotAccess.NONE)
    .allowNativeAccess(false)
    .allowCreateThread(false)
    .allowCreateProcess(false)
    .allowEnvironmentAccess(EnvironmentAccess.NONE)
    .allowIO(IOAccess.NONE)
    .allowValueSharing(false)
    .resourceLimits(ResourceLimits.newBuilder()
        .statementLimit(limit, source -> true)
        .build())
    .option("js.ecmascript-version", "latest")
    .option("js.load", "false")
    .option("js.print", "false")
    .build();
```

默认只向 bindings 注入 `ProxyObject`、`ProxyArray` 和 `ProxyExecutable`，所以使用
`HostAccess.NONE`。只有某个经过单独审核的 facade 确实需要 `@HostAccess.Export` 时，该 Document
才切换为 `HostAccess.EXPLICIT`；不使用宽泛的 `SCOPED`、`CONSTRAINED` 或 `ALL`。
返回 Promise 的 capability 通过 message id 完成，不让 Java future 或 object 泄漏进 JS。

### 13.2 RTS capability mapping

| capability | Java owner | thread hop | 功能 |
|---|---|---|---|
| `rtsbuilding:camera` | RTS client + UIE `CameraApi` | script -> client | open/close session、pose intent、project/ray/pick snapshot |
| `rtsbuilding:input` | RTS client + UIE `InputApi` | client -> script | high-level edge/state snapshot |
| `rtsbuilding:state` | RTS client controller | 双向 message | 读 immutable view、订阅 snapshot |
| `rtsbuilding:commands` | RTS network facade | script -> client -> server | placement/mining/storage/craft intent |
| `rtsbuilding:blueprints` | RTS client repository | script -> bounded IO worker | list/import/export metadata |
| `rtsbuilding:navigation` | MUI/UIE adapter | script -> client | focus/reveal/back/semantic action |

JS 不直接生成网络 packet。每个 command 有 Java schema、size/range validation、rate limit、permission 和
server-side revalidation。

## 14. NFR component pack

### 14.1 推荐注册方式

MUI core 提供 `MuiElementRegistry` SPI，NFR 提供显式 contributor：

```java
public interface NfrMuiComponentContributor {
    void register(MuiElementRegistry elements, MuiStylePropertyRegistry styles);
}
```

不要扫描 classpath，不通过 XML 写 Java class name，不把 NFR 内部 Widget 包暴露给 JS。

### 14.2 首批 native tags

| tag | 基础实现 | 主要 props/events |
|---|---|---|
| `nfr|text` | MUI text + NFR font | text、wrap、ellipsis |
| `nfr|button` | `NfrTextButton` 适配 | label、disabled；click/secondary |
| `nfr|icon-button` | MUI button/drawable | icon、tooltip、aria label |
| `nfr|text-field` | MUI TextField | value、placeholder；input/change/submit |
| `nfr|toggle` | button + indicator | checked；change |
| `nfr|slider` | NFR decimal slider | min/max/step/value；input/change |
| `nfr|select` | NFR dropdown | options/value；change |
| `nfr|scroll-view` | `NfrScrollablePane` | scroll position；scroll |
| `nfr|row/column/grid` | MUI parent/layout | 只承担 native layout host |
| `nfr|modal-host` | MUI PanelManager | open/close/top scope |
| `nfr|item-slot` | MUI item drawable/slot host | read-only/client intent mode |

业务组件如 storage toolbar、quick-build selector、workflow row、blueprint dialog 应由 RTS 自己用 XML
组合，不进入 NFR 通用库。

## 15. Arc3D 的边界

Arc3D Core 不能替代 reducer、action/state、store、catalog 或 event bus。当前 NFR 稳定 facade
`Arc3DApi` 公开的能力只有 availability、lerp、HSV 和 ARGB interpolation；UIE 内部主要使用
`icyllis.arc3d.core.Color` 和 `MathUtil`。

| 旧 RTS/MUI 概念 | Arc3D 可否替代 | 决策 |
|---|---|---|
| reducer/action/state | 否 | Java 状态机或 Pinia 风格 JS store |
| catalog/registry | 否 | namespaced application/component registry |
| DOM/event bus | 否 | browser runtime 自己实现 |
| `UiRect` 辅助计算 | 部分 | 内部可转 Arc3D Rect，公共 DTO 保持整数/轻量结构 |
| matrix/vector/clip math | 是，渲染内部 | 不跨 JS/network boundary |
| color conversion/interpolation | 是 | 通过 `Arc3DApi` 或 renderer internal API |
| draw path/canvas | 可选，未来 | MUI Widget/drawable 仍是 v1 render backend |

原因是三套 rectangle 的语义不同：MUI `Area` 含相对布局和 resize 状态，UIE `UiRect` 是导航快照，
Arc3D/Tiqian `Rect` 是数学或文本布局值。强行统一只会增加转换歧义。

## 16. RTS 迁移架构

### 16.1 保留、重写、删除

| 旧区域 | 处理 | 新归属 |
|---|---|---|
| `uiCore` reducer/action/state | 选择性迁移 | RTS Java domain state 或 JS store |
| `uiCore` catalog/contribution | 参考扩展点 | manifest/component/application registry |
| `uiCore` event router/focus/capture/layer | 删除 | DOM event + MUI focus/PanelManager |
| `uiKit` canvas/layout/theme | 删除 runtime 部分 | MUI Widget/layout/stylesheet/NFR component |
| `uiKit` pure geometry algorithms | 按需迁移 | RTS layout helper 或 style implementation |
| client controller/state adapter | 重写 | immutable view model + capability bridge |
| camera motion math | 移植算法 | RTS CameraProvider，接 UIE CameraApi |
| shape/smart-fill/blueprint transform | 优先迁移纯逻辑 | RTS common/domain |
| network payload | 全部重写 | Forge 1.12 SimpleNetworkWrapper/明确 DTO |
| server service/task/workflow | 按 1.12 API 重写 | RTS server，保持预算/验证边界 |
| AE2/RS/JEI/Create 等 compat | 全部按 1.12 API 重写 | 独立 compat package |

### 16.2 推荐模块依赖

```text
rts-common-domain
  no UI, no client types
  shape / blueprint model / reducer / command DTO

rts-client-application
  depends MUI browser API + NFR component API + UIE API
  XML/JSON/mjs resources + native viewport/capabilities

rts-network
  explicit C2S intent and S2C snapshot/effect messages

rts-server-domain
  session/service/storage/task/workflow/persistence
  never depends GraalJS, NFR UI or UIE client classes

rts-compat-*
  optional classloading guards and version-specific adapters
```

### 16.3 首个纵向切片

第一批只实现：

1. 打开/关闭 RTS screen 和 camera session。
2. world viewport 的 pan/orbit/zoom 和 screen ray。
3. XML top bar + storage browser + NFR 基础控件。
4. server 返回只读 storage page snapshot。
5. 选择一种 block 并提交单方块 placement intent。
6. 退出、死亡、换维度、断线和 resource reload 全部释放。

纵向切片通过后再加 quick build、blueprint、craft、workflow、plugin 和 compat。

## 17. synced/container UI 限制

当前 `UIFactory.createPanel()` 要在 client/server 构建等价 Widget tree，`WidgetTree.collectSyncValues()`
按遍历顺序分配 sync id，`PanelSyncManager` 又在构建后锁定注册。

本节原先把 synced XML 整体延后。对 `D:/Projects/sfr/other_mods/ModularUI` commit
`75f86dabac6f91a224eb2a0716bf3838ff38f6a3` 复核后，现改为在 GraalJS 之前实现受约束的
动态数据通道与固定 slot 协议；实际类级方案见
`docs/modularui-browser-runtime-75f86da-code-change-plan.md`。

仍然明确禁止：

- 把 client resource pack XML 当作 server container template；
- 在 synced panel 中任意 JS 增删 synced widget；
- 让 resource pack 覆盖协议相关 component；
- 用 DOM node handle 直接当 ModularUI sync id。

开放 synced XML 前必须实现：

- common Java 8 compiler 生成的 server-known `ProtocolPlan`；
- stable explicit sync key 和 wire type/version，禁止 synced document 使用 BFS `auto_sync`；
- 固定 handler 注册顺序与真实 container slot ordinal；
- client/server protocol fingerprint handshake；
- protocol component whitelist 与 duplicate key/type 检查；
- protocol XML/resource pack override exclusion；
- server 无 GraalJS 时的 deterministic construction；
- SyncHandler 到 Java/JS store 的 projection，而不是第二套 JS 网络同步。

## 18. 安全与信任模型

### 18.1 origin

每个 application origin 至少包含：

```text
owner mod id
application id
resource source (mod jar / local override / resource pack)
manifest hash
module graph hash
capability grant
```

### 18.2 脚本信任

- mod JAR 内 manifest/module：可执行，但仍受 capability sandbox。
- 普通 resource pack：可覆盖 XML/style/texture，默认不能覆盖 manifest/module。
- 服务端 resource pack：永不自动执行 JS。
- 本地 loose script：默认关闭；启用时按 origin/hash 明确信任。
- XML component library 的 module 继承最终 application 权限，不自授予。

### 18.3 失败隔离

- 一个 Document 一个 Context；超限只关闭该 Document。
- callback exception 记录 application/module/source/line/event/node path。
- mutation validation 失败拒绝整批并触发 native error boundary。
- queue overflow 优先 coalesce pointer move，再丢低优先级 observation，绝不丢 close/reload。
- capability 在 close/reload 时统一撤销，未完成 Promise 以 `AbortError` reject。

## 19. 依赖 BOM

### 19.1 平台与 mod 前置

| 前置 | 审核基线 | 依赖方式 | 说明 |
|---|---:|---|---|
| Minecraft | `1.12.2` | platform | 唯一目标游戏版本 |
| Java | MUI core `8` bytecode；Graal addon/client `25` | runtime/toolchain | server/no-JS client 不依赖 Graal |
| Cleanroom Loader | `0.6.1-alpha` | required platform | 以 UIE 当前构建基线为准，发布前再锁正式兼容范围 |
| ModularUI | `3.2.0-nfr.1` / commit `75f86da` | required mod/API | browser core/document sync 实施后应升 API 版本 |
| NFR | `0.6.0` | required for NFR component pack | 字体、Arc3D facade、视觉组件 |
| UIE | `0.7.0` | required by RTS，browser runtime 本身 optional | camera/input/navigation |

RTS addon 的 metadata 应要求带 browser core 的 ModularUI、NFR component pack 和 UIE；只有声明 JS 的
application 才要求 `modularui-js-graal`。MUI core 不反向硬依赖 NFR、RTS、UIE 或 Graal。

### 19.2 生产直接依赖

| 用途 | 坐标/来源 | 版本 | 分发 | 许可证 | 决策 |
|---|---|---:|---|---|---|
| Polyglot API | `org.graalvm.polyglot:polyglot` | `25.3.4.1` | nested jar | UPL-1.0 | 使用 |
| GraalJS runtime | `org.graalvm.polyglot:js` POM | `25.3.4.1` | transitives as nested jars | MIT/UPL-1.0 | 使用 |
| XML parser | JDK `java.xml` StAX | Java 25 | JDK | GPL+Classpath（JDK） | 使用，不加库 |
| JSON | 现有 Gson | MUI baseline `2.8.9` API | 已由平台/MUI提供 | Apache-2.0 | 使用，不重复嵌入 |
| collections | 现有 fastutil | 平台/MUI版本 | 已提供 | Apache-2.0 | 仅 Java 实现内部使用 |
| matrix/math | MUI embedded JOML | `1.10.8` | MUI 已嵌入 | MIT | 不重复嵌入 |
| render math/color | NFR Arc3D | `2026.2.0` facade | NFR 已提供 | LGPL-3.0 | 可选实现依赖 |

### 19.3 GraalJS 传递内容与体积

Maven Central 实测 jar 体积合计约 `66.93 MiB`，包含：

| artifact | 约 MiB | 许可证 |
|---|---:|---|
| `js-language` | 26.56 | UPL/MIT |
| `truffle-api` | 16.20 | UPL |
| Graal shaded `icu4j` | 17.70 | Unicode/ICU |
| `regex` | 3.73 | UPL |
| `truffle-runtime` | 0.89 | UPL |
| `polyglot` | 0.51 | UPL |
| `collections/nativeimage/word/jniutils/nativebridge` | 0.98 合计 | UPL |
| `truffle-compiler/xz` | 0.36 合计 | UPL |

发布时：

- 保留各 jar，不 relocate；
- 禁止 Shadow minimize；
- 保留 `META-INF/services`、language metadata 和 Graal resources；
- 使用 Cleanroom/FML `ContainedDeps` + `NonModDeps=true`；
- 在 addon `META-INF` 复制 UPL、MIT、Unicode/ICU、XZ notices；
- CI 解包最终 mod jar，验证所有 nested jar 和 service entry。

约 67 MiB 是采用现代完整 ECMAScript runtime 的主要成本，需要单独审核 D10。

### 19.4 测试依赖

| 用途 | 坐标 | 版本 | 是否进发布 jar |
|---|---|---:|---|
| MUI Java 8 contract tests | JUnit Jupiter | `5.9.2`，沿用 MUI | 否 |
| Java 25 runtime/addon tests | JUnit Jupiter | `6.0.3`，沿用 NFR | 否 |
| microbenchmark | JMH | `1.37` | 否 |

不需要 Mockito、Awaitility、XMLUnit、JSON Schema validator；测试优先使用真实 parser、fake scheduler、
fake capability 和 deterministic clock。

### 19.5 明确不引入

- Pinia/Vue；
- React/Svelte；
- Nashorn、Rhino、LuaJ；
- `javax.script` / `js-scriptengine`；
- Node.js/npm runtime；
- Babel/TypeScript runtime transpiler；
- Jackson；
- Guava EventBus；
- RxJava/Reactor；
- CSS parser/layout engine；
- Xerces/JDOM/dom4j；
- 浏览器/Chromium/CEF；
- EvalEx 作为 UI 脚本。

## 20. 构建与打包

### 20.1 runtime addon Gradle 原则

```gradle
java {
    toolchain { languageVersion = JavaLanguageVersion.of(25) }
}

dependencies {
    compileOnly(project(":"))
    compileOnly("com.cleanroommc:modularui:<approved-version>") { transitive = false }
    implementation("org.graalvm.polyglot:polyglot:25.3.4.1")
    contain("org.graalvm.polyglot:js:25.3.4.1")
}
```

实际配置需确保 `polyglot` 和全部 runtime transitives 都进入 `contain` resolution；不能只嵌入 POM。
新增 `verifyGraalContainedDeps` 检查 artifact 数量、版本一致、ServiceLoader 文件和许可证。

### 20.2 versioning

分别版本化：

- `MuiBrowserApiVersion`：DOM/manifest/style/capability 契约；
- `MuiManifestFormatVersion`；
- `MuiStyleFormatVersion`；
- GraalJS engine version；
- component library version；
- RTS network protocol version。

升级 ECMAScript engine 不应自动破坏 host API；manifest 也不能自己下载 engine。

## 21. API 修改清单

### 21.1 ModularUI 必须新增

1. `MuiDocumentHost`：把 element handle 绑定到 Widget host。
2. `MuiElementRegistry`：qualified native element descriptor/factory。
3. `MuiStylePropertyRegistry`：typed property 到 resizer/theme/drawable/navigation 的 adapter。
4. `MuiInputEventSource`：在原 input dispatch 前后生成 immutable event packet。
5. stable Widget navigation handle：动态 tree 变更时保持 element identity。
6. batched navigation/layout invalidation：一个 mutation batch 只递增一次 revision。
7. panel lifecycle hook：Document close 能统一撤销、dispose 和清 capture/focus。

XML parser、DOM/event/style/store/ProtocolPlan 放进 MUI core；Graal Context 和 RTS capability 不放进 MUI core。

### 21.2 NFR 必须新增

1. 稳定的 `api.client.mui` component contributor。
2. NFR theme token 的只读 descriptor，不让脚本拿 renderer object。
3. 现有基础控件的 prop/event adapter，或等价新 native element。
4. component pack 的 semantic navigation metadata 和 contract tests。

### 21.3 UIE v1 不必新增

现有 Camera/Input/Navigation API 足够完成首个 RTS slice。实现时若发现下列缺口，再独立评审：

- custom namespaced `InputAction`，而不是继续扩大 enum；
- camera session owner diagnostics 的公共 event；
- UIE navigation provider 对 DOM element stable id 的直接 adapter；
- HUD compositor hosting a read-only MUI Document。

不要为了预想需求先扩 API。

## 22. 测试与验收

### 22.1 MUI core contract tests

- add/remove/move Widget host lifecycle；
- one batch/one layout and navigation invalidation；
- pointer hit path、capture release、focus cleanup；
- panel close/reopen/dispose；
- 传统 Java-built UI 自动 adoption；无 listener 时 HYBRID 不改变旧 UI 行为；
- Java 8 bytecode verification。

### 22.2 parser/style/component tests

- XML namespace、typed prop、slot、text whitespace；
- XXE/DOCTYPE/external entity 全部拒绝；
- component cycle、duplicate id、unknown tag、depth limit；
- selector specificity、cascade、pseudo state、scoped style；
- malformed resource reload 保留旧 generation。

### 22.3 JS/store tests

- ESM static/dynamic import 和 path escape；
- ECMAScript feature smoke（Promise、Proxy、class fields、top-level await 等）；
- getter dependency invalidation；
- nested `$patch` 合并一次 transaction；
- action success/failure/async hooks；
- unmount 后 subscription/timer/listener 全释放；
- statement limit/watchdog/queue overflow；
- host access、IO、thread/process/class lookup escape tests。

### 22.4 event/thread tests

- capture -> target -> bubble 顺序；
- once/passive/abort/stop/preventDefault；
- deferred default action 只执行一次；
- immediate native action 不声称可被事后取消；
- event coalescing 不丢 click/up/close；
- stale generation batch 不触碰新 Widget tree；
- render thread 测试中禁止任何 blocking future wait。

### 22.5 RTS vertical tests

- camera lease 所有退出路径释放；
- screen ray/selection 与世界 viewport 坐标一致；
- storage snapshot -> store -> XML list；
- click -> DOM event -> capability command -> C2S validation -> S2C result；
- 无 UIE、无 NFR component pack、无 server capability 时明确 fail closed；
- controller/keyboard/mouse 使用同一 semantic action path。

### 22.6 benchmark

JMH 只测纯部分：selector matching、computed style、reactive getter、mutation coalescing、XML compile。
游戏内 harness 测 cold start、首屏 mount、500/5,000 node update、input-to-next-frame latency 和 reload。

硬性原则：render thread 的 event enqueue 和 batch apply 都有独立 p50/p95/p99，不能只测平均 FPS。

## 23. 实施阶段

### Phase 0：契约冻结

- 审核 D01-D12。
- 冻结 manifest/XML/style namespace 和 version 字段。
- 写 API skeleton 和无实现 contract tests。
- 做 Graal nested-jar 启动 spike，验证 Cleanroom Java 25 classloading 和约 67 MiB 分发。

退出条件：Graal smoke module 能在隔离 Context 执行，sandbox escape tests 通过。

### Phase 1：MUI DOM host SPI 和 input source

- element/widget host identity；
- batched mutation apply；
- native preflight；
- lifecycle/focus/capture/navigation cleanup；
- 保持所有旧 Java-built UI 行为不变。

退出条件：纯 Java fake DOM 能动态 mount/update/unmount Widget tree。

### Phase 2：XML、component、stylesheet，无 JS

- secure StAX parser；
- manifest discovery；
- native element registry；
- XML composite/props/slots；
- JSON selector/cascade/computed style；
- resource reload generation swap。

退出条件：同一测试页面能由 Java builder 和 XML 生成等价 semantic/render tree。

### Phase 3：GraalJS、DOM event 和 Pinia 风格 store

- scheduler/queues/transactions；
- ESM resolver；
- DOM API/events；
- reactive store；
- capability sandbox；
- diagnostics/watchdog。

退出条件：动态 list、modal、async capability 和 hot reload 全部无 render-thread wait。

### Phase 4：NFR component pack

- 基础 element adapters；
- theme token/style mapping；
- semantic navigation；
- visual/interaction snapshots。

退出条件：NFR 设置页的一个代表性 view 可用 XML+store 重建，但不要求立即迁移现有设置页。

### Phase 5：RTS vertical slice

- camera/input capability；
- world viewport；
- top bar/storage browser；
- single placement；
- server session/validation/network。

退出条件：多人 dedicated server 环境完成打开、操作、断线和重连。

### Phase 6：RTS 功能扩展

按风险依次加入 quick build、destruction、blueprint、craft、workflow、plugin、AE2/RS/JEI compat。

## 24. 主要风险

| 风险 | 严重度 | 控制措施 |
|---|---|---|
| Graal runtime 增加约 67 MiB | 高 | 独立可选 addon、nested jar spike、明确许可证 |
| JS 独立线程与同步浏览器事件差异 | 高 | native immediate/deferred default 明确分级 |
| logical DOM 与 Widget tree 分叉 | 高 | generation+sequence、atomic batch、reject whole batch |
| resource pack 代码执行 | 高 | JS/manifest 默认不可覆盖、origin/hash trust |
| dynamic UI 破坏 MUI sync ids/slot ordinal | 高 | ProtocolPlan、stable key、ordered slot table、fingerprint handshake |
| Context 失控阻塞 script queue | 高 | statement limit、watchdog、per-document Context、bounded queue |
| NFR 内部控件成为不稳定 API | 中 | explicit descriptor/factory contributor，不反射 |
| 两套焦点/导航冲突 | 中 | MUI 唯一 native focus，UIE 只做 semantic adapter |
| Pinia API 预期等同 Vue | 中 | 文档明确兼容子集，不宣称可运行 Pinia plugin |
| Arc3D 类型蔓延业务层 | 中 | 只在 renderer adapter 内转换 |

## 25. 审核结果填写区

请优先确认：

1. 是否接受非 JS browser core 内置 MUI、GraalJS 单独作为可选 client addon。
2. 是否接受 GraalJS `25.3.4.1` 及约 67 MiB nested runtime 成本。
3. 是否接受 Pinia 风格兼容子集，而不直接依赖 Pinia/Vue。
4. 是否接受 render thread 永不等待 JS，以及 immediate event 的 `preventDefault` 限制。
5. 是否接受先做 client-only screen，再以 ProtocolPlan 开放受约束的 synced/container XML。
6. 是否接受普通/服务端 resource pack 永不覆盖可执行 module。
7. 是否接受 MUI browser core、optional Graal addon、NFR component pack、UIE、RTS addon 的所有权边界。

建议审核通过后只启动 Phase 0 spike，不直接开始 RTS 全功能移植。

## 26. 审核证据索引

### 26.1 ModularUI fork

仓库：`D:/Projects/sfr/other_mods/ModularUI`，commit `75f86dabac6f91a224eb2a0716bf3838ff38f6a3`

| 结论 | 主要源码 |
|---|---|
| Java 8 bytecode / Jabel 边界 | `buildscript.properties`、`build.gradle` |
| Widget 动态 child 和 lifecycle | `src/main/java/com/cleanroommc/modularui/widget/AbstractWidget.java`、`AbstractParentWidget.java` |
| render/update/resize/sync tree | `src/main/java/com/cleanroommc/modularui/widget/WidgetTree.java` |
| Panel z-order/close/dispose/revision | `src/main/java/com/cleanroommc/modularui/screen/PanelManager.java` |
| legacy input dispatch | `src/main/java/com/cleanroommc/modularui/screen/ClientScreenHandler.java`、`ModularScreen.java` |
| resizer/layout backend | `src/main/java/com/cleanroommc/modularui/widget/sizer/StandardResizer.java` |
| JSON theme resource reload | `src/main/java/com/cleanroommc/modularui/theme/ThemeManager.java` |
| 显式 drawable registry | `src/main/java/com/cleanroommc/modularui/drawable/DrawableSerialization.java` |
| sync 构建期锁定 | `src/main/java/com/cleanroommc/modularui/value/sync/PanelSyncManager.java` |
| client/server panel factory | `src/main/java/com/cleanroommc/modularui/api/UIFactory.java`、`factory/GuiManager.java` |

### 26.2 NFR 与 UIE

仓库：`D:/Projects/sfr/smoothfont-replacement`

| 结论 | 主要源码 |
|---|---|
| NFR screen/session/route 组织 | `src/main/java/neofontrender/client/gui/NeofontrenderConfigScreen.java` |
| base/business/layout/view 分层 | `src/main/java/neofontrender/client/gui/component`、`layouts`、`model`、`pages`、`views` |
| NFR component 代表实现 | `NfrTextButton.java`、`NfrOptionDropdown.java`、`NfrScrollablePane.java` |
| Arc3D 稳定 facade 边界 | `src/main/java/neofontrender/api/arc3d/Arc3DApi.java` |
| camera facade/provider/session/picking | `addons/ui-enhancements/src/main/java/neofontrender/addons/api/camera` |
| input frame/context/action | `addons/ui-enhancements/src/main/java/neofontrender/addons/api/input` |
| semantic navigation API | `addons/ui-enhancements/src/main/java/neofontrender/addons/api/ui/navigation` |
| ModularUI navigation adapter | `addons/ui-enhancements/src/main/java/neofontrender/addons/navigation/modularui` |
| HUD compositor ownership 边界 | `addons/ui-enhancements/src/main/java/neofontrender/addons/hud/compositor` |
| Java 25 和 contained dependency 打包 | `addons/ui-enhancements/build.gradle` |

### 26.3 RTSbuilding 参考实现

仓库：`D:/Projects/sfr/other_mods/RTSbuilding`

| 结论 | 主要源码 |
|---|---|
| NeoForge 1.21.1 dependencies/test | `build.gradle`、`gradle.properties` |
| Java 8 isolated UI Core/Kit | `gradle/rts-ui.gradle`、`src/uiCore/java`、`src/uiKit/java` |
| screen owner 拆分 | `client/screen/standalone/BuilderScreen*.java` |
| client state/command facade | `client/controller/ClientRtsController.java` |
| camera motion/session | `client/service/Camera*`、`client/camera` |
| server tick 编排 | `server/service/ServerTickOrchestrator.java` |
| service facade | `server/api`、`server/service/impl` |
| storage/session/cache | `server/storage` |
| 预算任务与持久化 | `server/task` |
| workflow/event | `server/workflow` |
| 协议规模与边界 | `network/camera`、`builder`、`storage`、`craft`、`plugin` |
| 外部 mod 兼容 | `compat/ae2`、`refinedstorage`、`jei`、`create` 等 |

### 26.4 外部资料

- GraalJS compatibility / ECMAScript 2026：
  `https://www.graalvm.org/jdk25/reference-manual/js/JavaScriptCompatibility/`
- Graal Polyglot embedding：
  `https://www.graalvm.org/jdk25/reference-manual/embed-languages/`
- Polyglot `25.3.4.1` POM：
  `https://repo1.maven.org/maven2/org/graalvm/polyglot/polyglot/25.3.4.1/polyglot-25.3.4.1.pom`
- GraalJS `25.3.4.1` aggregate POM：
  `https://repo1.maven.org/maven2/org/graalvm/polyglot/js/25.3.4.1/js-25.3.4.1.pom`
- Pinia core concepts，仅作为 API 设计参考：
  `https://pinia.vuejs.org/core-concepts/`

本文的 jar 体积来自 2026-08-29 对 Maven Central 对应版本实际 artifact 的下载长度求和；
不是最终 mod jar 大小承诺。Phase 0 必须用最终 Cleanroom contained-jar 构建再次核对。
