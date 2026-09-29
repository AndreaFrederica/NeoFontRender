# ModularUI XML 与样式语法参考

本文档描述当前 ModularUI browser runtime 已实现的 XML、组件模板、JSON stylesheet 和 CSS 子集。它对应当前源码，不是完整 HTML/CSS 规范。

本文的 UI XML 只描述客户端 DOM/Widget 结构。固定 container handler/action/slot 使用另一份 common-side
protocol XML，语法见 [`modularui-fixed-protocol-template.md`](modularui-fixed-protocol-template.md)。

## 1. 处理模型

~~~text
XML source
  -> MuiXmlParser
  -> MuiDocument / MuiElement DOM
  -> component expansion
  -> Widget projection

JSON stylesheet 或 CSS subset
  -> MuiStylesheetParser
  -> MuiCascade
  -> MuiComputedStyle
  -> MuiStyleApplier（当前 Widget 适配器）
~~~

XML 负责结构和属性，样式表负责样式。XML 不会因为写入一个未知标签就自动创建任意 Java Widget；只有注册过的 native element 才能投影到 Widget，未知标签仍可作为逻辑 DOM 节点存在。

## 2. XML 文档

### 2.1 基本语法

~~~xml
<mui:column xmlns:mui="urn:mui" id="root" class="toolbar">
    <mui:text id="title">Inventory</mui:text>
    <mui:button enabled="true">Open</mui:button>
</mui:column>
~~~

当前支持：

- 一个完整根元素；
- 普通 XML 元素、属性和嵌套关系；
- 前缀名称，例如 mui:button；解析器保留前缀；
- XML 属性值，统一以字符串保存；
- 普通文本和 CDATA 文本；
- 标准 XML 转义和实体由 JRE StAX 处理；
- 注释和处理指令会被忽略，不会生成 DOM 节点；
- 只有非空白文本会生成 MuiText 节点。

XML namespace 用于组织名称。解析器不会强制某个 URI 必须存在；native element、组件和 manifest 是否注册由 ModularUI 注册表决定。

### 2.2 文档 API

~~~java
MuiDocument document = new MuiDocument();
MuiElement root = MuiXmlParser.parse(xmlSource, document);
~~~

编译到已有父节点并展开组件：

~~~java
MuiElement root = new MuiDocumentCompiler(registry, resolver)
        .compile("client", xmlSource, document, parent);
~~~

ModularScreen.installCompiledDocument(...) 是安装到主 panel 的便捷入口。

安装时点取决于文档是否包含已预注册的真实物品槽：

~~~java
// 不包含同步 ItemSlot 的纯客户端/静态文档可以在 createScreen() 中安装。

// 包含同步 ItemSlot 的文档必须等容器和 Widget tree 都初始化完成。
@Override
public void onContainerReady(ModularContainer container) {
    MuiElement root = installCompiledDocument(owner, xml, components, resolver);
    root.querySelector("#input-slot").appendChild(
            getDocument().getElementById("machine_slot_0"));
}
~~~

`onContainerReady(...)` 对每个 container-backed screen 只调用一次，位于初始 panel/Widget 初始化之后、
`onOpen()` 之前。此时 named sync handler、固定 `ModularContainer` slot table 和 `ItemSlot` Widget 均已有效。
不要在 `createScreen()` 中提前 adoption 真实槽位，也不要通过放宽 DOM slot 校验规避该生命周期。

### 2.3 DOM 结构操作

~~~java
MuiElement row = document.createElement("mui:row");
row.setAttribute("class", "material-row");
parent.appendChild(row);
row.removeAttribute("class");

List<MuiNode> children = row.getChildNodes();
MuiElement found = document.querySelector("#title");
~~~

节点有稳定 NodeHandle/NodeId。结构变更应通过 MutationScope 批量提交：

~~~java
try (MutationScope tx = document.beginMutation()) {
    parent.appendChild(child);
    tx.commit();
}
~~~

DOM 操作必须在 document 所属线程执行；worker 线程只能提交受支持的 immutable update，由 document controller 在客户端线程 flush。

### 2.4 组件模板

组件在编译前注册，注册表冻结后不能继续添加组件。

~~~java
registry.register(MuiComponentDescriptor
        .builder("mui:material-row", "ui/material-row.xml")
        .build());
~~~

调用已注册组件：

~~~xml
<mui:material-row label="Stone" class="compact"/>
~~~

也可以使用通用调用形式：

~~~xml
<mui:component component="mui:material-row" label="Stone"/>
~~~

组件模板中的属性值和文本支持 ${property} 插值：

~~~xml
<mui:component-root>
    <mui:row class="${class}">
        <mui:text>${label}</mui:text>
    </mui:row>
</mui:component-root>
~~~

规则：

- mui:component-root 的第一个元素子节点作为组件输出根；
- 调用组件时的普通属性会补到输出根，但不会覆盖模板中已存在的同名属性；
- component、name 和 slot 不会作为普通属性传播；
- 组件引用必须存在，递归引用和超过最大展开深度会失败；
- 组件展开在目标 document 提交前完成，失败不会提交半成品。

### 2.5 Slot

模板使用 mui:slot：

~~~xml
<mui:component-root>
    <mui:column>
        <mui:text class="label">${label}</mui:text>
        <mui:slot name="actions">
            <mui:text>No actions</mui:text>
        </mui:slot>
    </mui:column>
</mui:component-root>
~~~

调用方按 slot 属性分配内容：

~~~xml
<mui:material-row label="Stone">
    <mui:button slot="actions">Use</mui:button>
</mui:material-row>
~~~

- 无 name 的 slot 接收没有 slot 属性的内容；
- 有 name 的 slot 接收相同 slot 值的元素；
- 没有匹配内容时使用 slot 内的 fallback 子节点；
- slot 本身不会出现在最终组件树中。

### 2.6 XML 安全与默认限制

### 2.6 声明式事件与 Java action

XML 可以用浏览器风格的事件属性引用当前 document 中显式注册的 Java action：

~~~xml
<mui:button onclick="inventory:sort">Sort</mui:button>
<mui:container onpointerdown="inventory:begin-drag"/>
~~~

~~~java
MuiActionRegistration registration = document.getActions().register(
        "inventory:sort", invocation -> {
            invocation.getEvent().preventDefault();
            sortItems();
        });
~~~

action 名称区分大小写，必须是 `namespace:name` 形式；两部分可使用字母、数字、`_`、`-` 和 `.`。
属性值不是 Java/JavaScript 表达式，因此 `onclick="sort()"`、类名、反射调用和内联源码都会被拒绝。
当前支持：

| XML 属性 | 触发事件 |
|---|---|
| `onclick` | 主鼠标键 `pointerdown`，或 `NavigationAction.ACTIVATE`；这是当前统一的“激活”语义 |
| `onaction` | 任意 `ActionEvent.ACTION` |
| `onpointerdown` | `PointerEvent.DOWN` |
| `onpointerup` | `PointerEvent.UP` |
| `onpointermove` | `PointerEvent.MOVE` |
| `onpointercancel` | `PointerEvent.CANCEL` |
| `onwheel` | `PointerEvent.WHEEL` |

handler 可从 `MuiActionInvocation` 取得 action 名、触发元素和原始 `MuiEvent`，所以仍可使用
`preventDefault`、`stopPropagation` 和 DOM/store API。绑定随属性增删即时更新；运行时通过 Java 或未来 JS
创建的元素只要设置这些属性，也会使用同一个 registry。删除节点或关闭 document 会释放绑定，关闭
`MuiActionRegistration` 会撤销 Java handler。

action registry 是客户端 document 本地能力，不会自动发送网络消息，也不改变既有 sync handler、slot
ordinal 或 protocol fingerprint。需要服务端操作时，Java action 必须显式调用已经注册的 MUI 同步 API。

当前 `onclick` 还不是浏览器在 pointer up 后合成的独立 `click` event；后续加入 click synthesis 时可在不改变
action 名和 registry 的情况下调整触发层。

### 2.7 XML 安全与默认限制

MuiXmlParser.Limits.defaults() 当前为：

| 项目 | 默认值 |
|---|---:|
| 单个 XML source | 256 KiB |
| 最大嵌套深度 | 64 |
| 每个元素最大属性数 | 64 |
| 最大节点数（元素和文本） | 10,000 |
| 单个文本节点 | 64 KiB |

DOCTYPE、DTD、外部实体、外部 schema 和外部 URL resolve 被禁止。解析失败时目标 DOM 不提交部分结果。

## 3. JSON stylesheet

### 3.1 文件格式

~~~json
{
  "formatVersion": 1,
  "variables": {
    "--accent": "#55c2ff",
    "--gap": 6
  },
  "rules": [
    {
      "selector": "mui|button.primary:hover",
      "style": {
        "color": "var(--accent)",
        "gap": "var(--gap)",
        "width": 96
      }
    }
  ]
}
~~~

要求：

- 根对象的 formatVersion 省略时按 1 处理；当前只支持 1；
- rules 必须是数组；每条 rule 必须有 selector 和对象类型的 style；
- variables 必须是对象，变量名必须以 -- 开头；
- JSON 值可以保留为 boolean、number、string、array 或 object；
- 样式属性名会 trim 并转为小写；
- JSON stylesheet 最大 256 KiB、最多 4096 条规则、每条最多 256 个声明。

~~~java
MuiCascade cascade = MuiStylesheetParser.parse(jsonSource);
screen.setStylesheet(cascade);
~~~

## 4. CSS 子集

### 4.1 基本规则

~~~css
mui|button {
    width: 96px;
    padding: 4px 8px;
    color: #ffffff;
}

mui|button, .primary {
    enabled: true;
}
~~~

支持：

- /* comment */ 注释；
- 一个 rule 的 selector list，用逗号分隔；
- { property: value; } declaration block；
- declaration 之间用分号分隔，最后一个分号可省略；
- CSS 值按字符串读取，再由样式适配器解释；
- :root rule 中的 --name 声明会成为全局 stylesheet variable；
- var(--name) 替换，最多递归 16 层。

~~~java
MuiCascade cascade = MuiStylesheetParser.parseCss(cssSource);
~~~

CSS 带资源导入时必须提供 owner 和授权 resolver：

~~~java
MuiCascade cascade = MuiStylesheetParser.parseCss(
        "client", cssSource, authorizedResolver);
~~~

### 4.2 选择器

| 形式 | 示例 |
|---|---|
| 标签 | mui|button、mui:button、button |
| 通配标签 | * |
| id | #submit |
| class | .primary |
| 存在属性 | [disabled] |
| 属性相等 | [data-state="open"] |
| 后代组合 | .toolbar mui|button |
| 直接子元素 | .toolbar > mui|button |
| 伪状态 | :hover、:active、:focus、:focus-visible、:disabled、:checked |
| 根节点 | :root |

class 按空白分隔匹配。标签选择目前按 local name 比较，因此 mui|button、mui:button 和 button 会匹配同一个 local tag。

状态伪类读取 DOM 状态属性：

- :hover -> data-hover="true" 或 hover="true"；
- :active -> data-active="true" 或 active="true"；
- :focus / :focus-visible -> 对应 data-* 或普通属性；
- :disabled -> enabled="false" 或存在 disabled；
- :checked -> checked="true"。

不支持 :has、函数型伪类、属性运算符（~=、^=、$= 等）和复杂 selector list 语法。

### 4.3 @media

~~~css
@media screen and (min-width: 400px) and (orientation: landscape) {
    mui|button {
        width: 120px;
    }
}
~~~

支持的条件：

- screen、all；
- min-width、max-width；
- min-height、max-height；
- orientation: portrait、orientation: landscape；
- 用 and 组合多个条件；
- px 或整数像素值，例如 400px、400；
- @media 嵌套，条件按 AND 合并。

viewport 来自 ModularScreen.onResize(width, height)，样式重算不会改变 DOM identity、slot ordinal 或同步 handler。

当前不支持 not、only、逗号媒体列表、em/rem、container query、prefers-* 和动态表达式。

### 4.4 @import

~~~css
@import "common.css";
@import url("wide.css") screen and (min-width: 640px);
~~~

支持：

- "resource.css"；
- url("resource.css") 或 url(resource.css)；
- resource id 后的单个 media 条件；
- 在 @media 内使用 import，导入规则会继承外层条件；
- 导入资源必须由 MuiResourceResolver.open(owner, resourceId) 提供。

resolver 不提供时，任何 @import 都会抛出 MuiMarkupException。解析器不会自行访问 filesystem、HTTP 或任意 URL。

限制：

| 项目 | 限制 |
|---|---:|
| 单个 CSS source/import | 256 KiB |
| 所有 import 合计 | 1 MiB |
| import 数量 | 128 |
| import 嵌套深度 | 16 |

循环 import 会被拒绝。当前不支持逗号媒体列表和相对路径自动解析，resource id 原样交给 resolver。

## 5. 当前可映射的样式属性

MuiCascade 会保留所有合法声明，但 MuiStyleApplier 目前只把下面这些属性映射到现有 Widget：

| 属性 | 值示例 | 当前效果 |
|---|---|---|
| left、top、right、bottom | 8px、25% | Widget 几何位置 |
| width、height | 120px、50% | Widget 尺寸 |
| padding | 4px 8px | 四边 Box，支持 1-4 值简写 |
| margin | 2px 4px | 四边 Box，支持 1-4 值简写 |
| background | #101820 | Rectangle 颜色或 JSON drawable |
| border-style、border-color、border-width | solid/dashed、#3c4a57、1px | Widget overlay 边框；dashed 还支持 border-dash、border-gap |
| color | #ffffff | TextWidget 颜色 |
| font-size | 1.25 | TextWidget scale |
| text-align | 对应 Alignment 名称 | TextWidget 对齐 |
| enabled | true / false | Widget enabled |
| visibility | hidden 或其他值 | hidden 时禁用 Widget |
| pointer-events | auto / none | none 时从 pointer hit-test 中跳过，但保留 DOM/event parent |
| focusable | true / false | NavigationInfo 是否进入焦点导航 |
| tab-index | 整数 | NavigationInfo.order 导航顺序元数据 |
| theme-key | inventory.button | Widget theme override |
| gap | 4px | Flow 子间距 |
| wrap | true / false | Flow 换行 |
| justify | start、center、space-between 等 | Flow 主轴对齐 |
| align | start、center、end 等 | Flow 交叉轴对齐 |
| grid-columns、columns | 3 | Grid DOM 列数 |
| overflow、overflow-x、overflow-y | auto、scroll、hidden、visible | `AbstractScrollWidget` 的滚动轴适配；`auto`/`scroll` 创建缺失轴，`hidden`/`visible` 移除可选轴 |
| scrollbar | drawable JSON 对象 | 设置滚动条 drawable，应用到当前存在的水平/垂直轴 |
| scrollbar-background | #20252b | 设置 ScrollArea 滚动条轨道背景色 |
| scrollbar-speed | 7 | 设置当前轴的鼠标滚轮步长 |
| scrollbar-cancel-edge | true / false | 设置滚动到边缘时是否继续拦截滚轮事件 |
| progress-render | bar | `ProgressWidget` 使用 CSS 轨道/填充绘制，保留原有同步进度值 |
| progress-track-color、progress-fill-color | #27323d、#38bda6 | CSS progress bar 的轨道和填充颜色 |

样式重新计算时，适配器从 Widget 初始 baseline 恢复，再应用新 computed style，因此删除 rule 或改变 media 条件不会累积旧值。

以下属性当前只会留在 MuiComputedStyle，不会自动改变 Widget：opacity、tooltip、navigation role，以及尚未实现的 grid-rows 等。`overflow` 和 scrollbar 属性只作用于 `AbstractScrollWidget`。样式重算会从 baseline 恢复滚动条背景色、速度、edge-cancel 和 drawable；滚动尺寸、当前位置以及不可变的滚动条厚度仍由 Widget/布局代码负责，不由样式直接改写。普通 Widget 没有 viewport 语义，因此不会被修改。

XML 元素上的 style 属性目前不是 CSS declaration string，而是可选的 JSON object 字符串；它会作为 inline style 参与 cascade：

~~~xml
<mui:button style='{"width": 120, "color": "#ffffff"}'>Open</mui:button>
~~~

CSS 写法（例如 style="width: 120px"）当前不会被 inline parser 解析。复杂动态样式应放入 stylesheet 或通过 DOM/style API 更新。

## 6. 在线 DevTools 后端

`ModularScreen.openDevTools()` 返回当前屏幕的客户端调试会话。会话可以导出
DOM/Widget 树快照、选择 `NodeHandle`、读取 computed style，并设置运行时样式覆盖：

~~~java
MuiDevToolsSession tools = screen.openDevTools();
tools.select(screen.getDocument().getElementById("input-slot").getHandle());
tools.setStyle("border-color", "#38bda6"); // 立即重算，不重启
tools.setInlineStyle("width", "24px");
~~~

运行时覆盖只保存在客户端会话中，优先级高于 inline style，但不会生成 DOM mutation，
也不会进入 `PanelSyncManager`、远端 store 或任何服务端数据包。关闭会话、清理覆盖或销毁
节点时会恢复 Widget baseline。所有调用必须在屏幕 owner thread 执行。

这套后端不依赖 JavaScript；传统 MUI Widget 和 XML 投影节点共用同一 `NodeHandle`、事件树
和样式重算路径。后续 F12 风格 overlay 只需用普通 MUI Widget 展示 `NodeSnapshot`，再把属性
编辑框绑定到 `setStyle`/`setInlineStyle`，即可实时修改而不重启游戏。

### 6.1 Java state binding

XML-only 页面可以直接使用 Java store，不需要 JavaScript runtime：

~~~java
MuiStore store = new MuiStore(Collections.singletonMap("label", "Ready"));
StoreSubscription binding = MuiStoreBinding.text(store, "label", title);
store.set("label", "Updated");
~~~

`MuiStore` 受创建线程约束。允许 `null`、boolean、number、string、`byte[]`、list 和 string-key map；
循环对象和任意 Java object 会被拒绝。`patch`、`replace` 和 `reset` 每个 mutation batch 最多通知一次，
并提供单调 revision 与不可变 previous/current snapshot。`byte[]` 在写入和读取时复制，不能通过外部引用
修改内部状态。binding 通过 `StoreSubscription` 显式释放；已经销毁的 DOM node 会忽略后续更新。

普通 `MuiStore` 不会自动序列化到 `PanelSyncManager`。需要服务端数据时使用固定
`DocumentSyncHandler -> RemoteStore -> MuiStore` 投影，详见
[`modularui-document-sync-api.md`](modularui-document-sync-api.md)。

### 6.2 Scroll DOM properties

投影到 `AbstractScrollWidget` 的元素提供客户端本地 viewport 属性：

~~~java
int left = element.getScrollLeft();
int top = element.getScrollTop();
int width = element.getScrollWidth();
int height = element.getScrollHeight();
element.scrollTo(left, top);
~~~

`scrollWidth`/`scrollHeight` 取 viewport 尺寸和 native `ScrollData` 内容尺寸的较大值。`scrollTo`
会限制到 native scroll bounds；非滚动元素返回零并忽略 setter。native input、动画或 DOM setter 导致的
位置变化会向投影元素发送不冒泡、不可取消的 `ScrollEvent.SCROLL`。滚动属性和事件是客户端渲染状态，
永远不会进入 `PanelSyncManager` 或 document sync packet。

## 6. Cascade 与运行时更新

样式优先级为：

1. selector specificity；
2. 同 specificity 时按 stylesheet 出现顺序；
3. inline map（调用 MuiCascade.compute(element, inline, environment) 时）优先于 stylesheet。

DOM 属性变化、data-hover/data-focus 等交互状态变化以及 viewport resize 都可以触发 computed style 重新计算。样式是客户端渲染状态，不会直接改变服务端同步值；DOM 结构或明确绑定的业务属性才进入 DOM/sync 管线。

## 7. 当前明确不支持

- 完整 HTML parser 行为和浏览器 DOM API；
- CSS @keyframes、@supports、@font-face；
- CSS 动画、transition、复杂 calc/function 值；
- CSS 变量的 fallback 语法 var(--x, fallback)；
- selector :has、nth-*、函数型伪类和复杂属性匹配；
- 自动从 XML style="..." 属性解析 CSS；
- 未注册 native element 到 Java Widget 的自动映射。

这些限制是当前受限 runtime 的设计边界，不代表未来不能增加对应 adapter 或 parser 扩展。
# Native XML widgets and bindings

The built-in XML element registry can create layout, display, input, and fixed slot views directly. Common tags include
`mui:container`, `mui:scroll`, `mui:list`, `mui:row`, `mui:column`, `mui:grid`, `mui:button`, `mui:text`,
`mui:scrolling-text`, `mui:progress`, `mui:item-slot`, `mui:fluid-slot`, `mui:item-display`, `mui:fluid-display`,
`mui:slider`, `mui:toggle`, `mui:cycle-button`, `mui:text-field`, `mui:text-editor`, `mui:code-editor`,
`mui:slot-group`, `mui:sort-buttons`, and `mui:category-list`.

Synced widgets refer to a handler installed by the common client/server protocol with a stable semantic `bind` key.
`class` and DOM `id` remain presentation/query identities and must not grant server capabilities. The lower-level optional
`bind-id` is supported for legacy indexed registries, but new markup should prefer one descriptive key per binding:

```xml
<mui:item-slot class="machine-slot input-slot" bind="machine.input"/>
<mui:progress bind="cook_progress"/>
<mui:slider bind="speed" min="0" max="100" step="5"/>
```

Bindings are immutable once a DOM element has materialized as a Widget. Runtime DOM changes may insert, remove, or move
views bound to existing protocol entries, but cannot create a new sync handler, change a real container slot ordinal, or
retarget an existing view. A protocol change requires a newly negotiated UI/container.

CSS remains client-only. It may change layout and presentation, but it never grants permissions or changes the fixed
client/server protocol.
