# MUI XML Showcase

独立的客户端 addon，用当前 ModularUI XML/CSS/DOM API 复现 NFR 设置界面的组件组织方式。
它只依赖 `modularui`，不链接 NFR 的 Java UI 组件，因此可用于验证声明式 UI 是否真的脱离原实现工作。

## 打开方式

- 游戏中按 `F8`；
- 或执行客户端命令 `/mui_xml_showcase`。

界面包含两个页面。Overview 验证 store binding、toggle、按钮、slider、textfield 和实时文本；
Components 验证 capture/target 事件以及挂载后的 DOM 节点新增、删除和 Widget projection。

## 同步容器测试

addon 还注册了两个可放置方块：

- `neofontrender_mui_xml_showcase:xml_chest`：27 个 TileEntity 槽位和 36 个玩家槽位；
- `neofontrender_mui_xml_showcase:xml_furnace`：真实输入/燃料/输出规则、vanilla 熔炼配方、燃料和进度同步。

可从红石 creative tab 取得，或使用 `/give`。右键方块会通过 MUI 的 `TileEntityGuiFactory`
在服务端与客户端建立相同的固定 slot table。客户端随后加载 `screens/chest.xml` 或
`screens/furnace.xml` 直接声明 `mui:item-slot` 和 `mui:progress`；common-side protocol XML
固定同步 key 与槽位 ordinal，Java 只把这些 binding 映射到 TileEntity 和玩家背包。
界面使用可选文本 `id` 做单节点查询、可重复 `class` 做批量查询，并以独立的语义 `bind` key 连接协议；
不再把数字 `bind-id` 暴露给页面。DOM 变更只改变视图父子关系，不重建 `ModularSlot`、不改变 container ordinal，也不让 XML 直接修改
服务端 inventory。熔炉进度同样先注册为 MUI sync value，再由 XML 的 `mui:progress` 直接绑定。
机器 inventory 容量由展开后的 common protocol 中 `showcase:machine-slot` 条目数得出；Java 子类不再重复填写
箱子的 27 或熔炉的 3。网格 `columns` 仍只在客户端 screen XML/CSS 中声明。

DevTools Source 会列出实际加载的 screen、CSS、manifest、组件、protocol XML 及其 protocol component。
主 screen XML 可以在运行时 Apply 并原子替换当前 DOM；固定 protocol source 只读，因为它已经参与
客户端/服务端容器契约握手。修改协议需要同时更新两端资源并重新打开容器。

## 资源组织

```text
assets/neofontrender_mui_xml_showcase/mui/
  mui-app.json              UI 注册清单
  screens/showcase.xml      页面结构
  components/*.xml          可复用 XML component
  protocols/*.xml           固定同步协议入口
  protocol-components/*.xml 服务端/客户端共用的固定协议 component
  styles/tokens.css         NFR 色彩 token
  styles/showcase.css       布局、状态、@import 和 @media
```

`mui-app.json` 注册 `nfr:app-shell`、`nfr:nav-item`、`nfr:card`、`nfr:toggle`、
`nfr:field`、`nfr:action` 和 `nfr:player-inventory`。这些组件完全由 XML template、props 和 named slot 构成：

| NFR Java UI 语义 | XML component / native element |
| --- | --- |
| 设置页壳、页签和 footer | `nfr:app-shell`、`nfr:nav-item` |
| 信息/设置 panel | `nfr:card` |
| 文本按钮、category/cycle button | `nfr:action`、`mui:button` |
| toggle indicator + label | `nfr:toggle` |
| responsive labeled field | `nfr:field` |
| labeled slider | `nfr:field` + `nfr:slider` |
| labeled text field | `nfr:field` + `nfr:text-field` |
| 玩家主背包和快捷栏 | `nfr:player-inventory` |

Slider 和 textfield 必须保留输入法、焦点、键盘导航和拖动行为，因此由 addon 注册两个小型 native
element bridge；结构、组合和样式仍在 XML/CSS 中。Java controller 不建静态 Widget tree，只负责 DOM
事件、`MuiStore` binding 和运行时 mutation。

按钮行为直接在 XML 中声明安全的 action 引用：

```xml
<nfr:action label="Add row" onclick="showcase:add-row"/>
```

Java controller 向当前 document 的 allowlist 注册实现：

```java
document.getActions().register("showcase:add-row", invocation -> addRuntimeRow());
```

XML 不执行 Java 表达式；只有显式注册的 namespaced action 可以被调用。

## 构建与验证

先在实际 MUI 仓库构建开发 jar，再指向它运行测试：

```powershell
cd D:\Projects\sfr\other_mods\ModularUI
.\gradlew.bat build -DDISABLE_BUILDSCRIPT_UPDATE_CHECK=true

cd D:\Projects\sfr\smoothfont-replacement
.\gradlew.bat :addons:mui-xml-showcase:test :addons:mui-xml-showcase:jar `
  --configure-on-demand `
  -Plocal_modularui_jar="D:/Projects/sfr/other_mods/ModularUI/build/libs/modularui-3.2.0-nfr.1-dev.jar"
```

测试会验证 manifest 安全解析、component/slot 完整展开、CSS `@import`/`@media`、DOM event、store
响应、运行时 DOM mutation、Widget projection 和 native element bridge。可安装 jar 位于：

```text
build/libs/neofontrender-mui-xml-showcase-0.1.0.jar
```

XML 与 CSS 子集的完整语法见仓库 `docs/modularui-markup-style-syntax.md`。
