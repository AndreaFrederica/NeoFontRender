# ModularUI 固定协议模板 API

本文描述 MUI core 已实现的 fixed-container contract。它解决 client/server 固定 handler、action 和真实
`ModularSlot` 拓扑必须完全一致的问题，不同步 DOM，也不允许 XML 反射 Java class。

## 1. 打开流程

~~~text
common protocol XML / Java builder
  -> MuiProtocolPlan canonical bytes
  -> SHA-256 MuiTemplateContract

UIFactory.getProtocolTemplate(guiData)
  -> explicit MuiProtocolTypeRegistry factories
  -> install handler/action/slot before createPanel()
  -> createPanel()
  -> verify exact declared registration
  -> ModularContainer.construct()
  -> verify real slot ordinals

server OpenGuiPacket(contract)
  -> client readGuiData()
  -> compare remote/local contract before createPanel()
  -> match: open UI
  -> mismatch: reject and send CloseGuiPacket
~~~

旧 `UIFactory` 默认返回 `null`，继续使用原有 `WidgetTree.collectSyncValues()` BFS `auto_sync`，行为不变。
只要 factory 返回固定模板，该 UI 就禁止 implicit auto-sync。

## 2. Protocol XML

~~~xml
<protocol id="example:machine_screen" schema-version="1">
    <handler key="state" id="0" type="example:value" version="1"/>
    <action key="refresh" type="example:command" version="1"/>
    <slot key="input" id="0" type="example:item" version="1" ordinal="0"/>
    <slot key="output" id="0" type="example:item" version="1" ordinal="1"/>
    <component src="protocol-components/player-inventory.xml" ordinal-offset="2"/>
</protocol>
~~~

协议组件使用单独的 common-side 资源，并以相对 slot ordinal 声明可复用片段：

~~~xml
<protocol-component>
    <slot key="player" id="0" type="example:player-slot" version="1" ordinal="0"/>
    <slot key="player" id="1" type="example:player-slot" version="1" ordinal="1"/>
</protocol-component>
~~~

通过受信任 resolver 解析并展开：

~~~java
MuiProtocolPlan plan = MuiProtocolXmlParser.parse(owner, protocolXml, resourceResolver);
~~~

也接受根名 `<mui-protocol>`。其余语法是封闭集合：

| 元素 | 必需属性 | 可选属性 | 含义 |
|---|---|---|---|
| `protocol` | `id`, `schema-version` | 无 | 固定模板 identity |
| `handler` | `key`, `type`, `version` | `id`，默认 0 | 一个 `SyncHandler` |
| `action` | `key`, `type`, `version` | 无 | 一个 `MuiProtocolAction` |
| `slot` | `key`, `type`, `version`, `ordinal` | `id`，默认 0 | 一个真实 `ItemSlotSH` |
| `component` | `src` | `ordinal-offset`，默认 0 | 展开一个 `<protocol-component>` common resource |

所有 version、id、ordinal 都是非负十进制整数。action 没有 numeric id。entry 不能包含 child 或非空文本；
unknown element/attribute、重复 `(key,id)`、重复 slot ordinal 和跨 handler/slot 的最终 sync key 碰撞都会失败。

解析器使用 secure JRE StAX，关闭 DTD、external entity 和 external schema。主协议与展开组件合计最多 256 KiB、
4096 entries，组件递归深度最多 32，循环引用直接失败。组件只能通过调用方提供的受信任 resolver 加载。
protocol XML 必须来自 client/server 一致的 mod-owned common resource，不能由 resource pack 或远程内容覆盖。

## 3. Canonical contract

`MuiProtocolPlan.FORMAT_VERSION` 当前为 `2`。fingerprint 是 canonical binary encoding 的 SHA-256，包含：

- plan format version；
- schema id 和 schema version；
- entry 顺序；
- 每项 kind、key、wire type、type version、numeric id 和 order/slot ordinal。

XML whitespace、attribute 顺序、协议组件路径和拆分方式不进入 fingerprint；协议组件展开后的 entry 才进入。
视觉 DOM、文本、位置、CSS/JSON style 和普通界面组件展开也不进入 fixed protocol fingerprint。

`OpenGuiPacket` 传输 plan format、schema id/version 和固定 32-byte fingerprint。factory name 限 32 bytes，
GUI data 限 1 MiB；截断 fingerprint、非法长度和超限 payload 都在解码/编码边界拒绝。

## 4. 显式 factory registry

~~~java
MuiProtocolTypeRegistry registry = new MuiProtocolTypeRegistry()
        .registerHandler("example:value", 1,
                (context, entry) -> createStateHandler(context.getGuiData()))
        .registerAction("example:command", 1,
                (context, entry) -> new MuiProtocolAction(
                        false, true, packet -> refresh(context.getGuiData())))
        .registerSlot("example:item", 1,
                (context, entry) -> new ItemSlotSH(createSlot(
                        context.getGuiData(), entry.getOrder())));

MuiProtocolPlan plan = MuiProtocolXmlParser.parse(protocolXml);
MuiProtocolTemplate template = new MuiProtocolTemplate(plan, registry);
~~~

registry 的 key 是 `(entry kind, type, version)`。重复注册、freeze 后注册、missing factory、null 返回值和错误
runtime type 都会失败。构造 `MuiProtocolTemplate` 会冻结 registry。XML 永远不会读取 class name 或调用反射。

factory 可通过 `MuiProtocolInstallContext` 读取 `GuiData`、`UISettings`、`PanelSyncManager`、当前 installation
和 side。factory 应只创建 entry 对应的对象，不应在内部动态扩展协议拓扑。

## 5. UIFactory 接入

~~~java
public final class MachineUiFactory implements UIFactory<MachineGuiData> {
    private final MuiProtocolTemplate protocolTemplate;

    @Override
    public MuiProtocolTemplate getProtocolTemplate(MachineGuiData guiData) {
        return this.protocolTemplate;
    }

    @Override
    public ModularPanel createPanel(MachineGuiData data,
                                    PanelSyncManager sync,
                                    UISettings settings) {
        MuiProtocolInstallation protocol = settings.getProtocolInstallation();
        SyncHandler state = protocol.getHandler("state", 0);
        ItemSlotSH input = protocol.getSlot("input", 0);
        return buildPanel(state, input);
    }
}
~~~

安装发生在 `createPanel()` 之前，因此 Java builder、UI XML/native element 和传统 Widget 都可以按 stable key
取得已安装对象。固定模板 UI 中，Widget 自己创建但未声明的 sync handler 会被拒绝；按 key/id 引用已安装
handler 是允许的。

## 6. Slot 拓扑

plan 中真实 slot ordinal 必须从 0 连续。安装只接受非 phantom `ItemSlotSH`。`ModularContainer.construct()`
初始化 handler 后，MUI 验证每个声明 slot 的底层 `ModularSlot` 确实位于对应 `inventorySlots` ordinal。

这些 ordinal 只描述 mod-owned leading slots。MUI framework 随后管理的 cursor/player inventory slot 不进入 plan。
运行时 DOM 可以移动或 parking 已有 slot 的视觉节点，但不能增加、删除或重排 container 协议槽位。需要改变真实
slot 数量时必须关闭并重新打开 container；动态仓储页应使用 virtual item cell 或预分配 slot pool。

## 7. 与动态文档同步的关系

固定模板决定 transport topology；`DocumentSyncHandler` 则在一个已声明 handler 内多路复用运行时 endpoint/channel。
常见组合是在 plan 中声明一个 document handler，再由客户端 DOM binding 动态订阅 `RemoteStore`。

DOM mutation、XML component、Java/可选 JS 创建节点都不会注册新的 `PanelSyncManager` handler。DOM event 也不会
自动发网络包；网络写入必须通过显式 endpoint command，并由服务端重新校验权限、revision 和业务参数。

GraalJS 不属于 MUI 主 jar。无 JS 的 Java/XML UI、固定 container 模板和动态文档同步均可单独工作，dedicated
server 只需要 Java 8 MUI core，不加载客户端 DOM 页面或任何 JS runtime。
