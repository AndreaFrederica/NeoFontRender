# UIE UI 导航树与焦点系统设计及实施计划

状态：Phase 0-6 已完成基础实现；Phase 7 复杂页面扩展待后续实施
日期：2026-08-13
目标版本：Minecraft 1.12.2 / Forge / Cleanroom
涉及模块：ModularUI fork、NFR 本体、UI Enhancements（UIE）、UI Enhancements Controller addon

## 1. 决策摘要

本计划确认以下架构决策：

1. **UI 导航树、逻辑焦点、方向导航、动作分派和焦点视觉均由 UIE 持有。**
2. **Controller addon 不再扫描 `GuiScreen`、`GuiButton`、容器槽位或 ModularUI Widget。**它只把手柄输入映射成 UIE 的抽象 UI 意图。
3. **UIE 对外提供只读树快照和输入无关的导航 API。**手柄只是第一个调用方，未来键盘导航、无障碍、自动化测试也可以复用。
4. **允许修改并维护 ModularUI fork。**fork 提供框架级的导航语义、稳定树访问、屏幕几何、目标 Widget 动作分派、滚动显露和结构 revision；不包含 UIE、Controller 或 Xbox 概念。
5. **UIE 仍是跨 UI 框架的导航平台。**全局逻辑焦点、空间导航、输入 modality、焦点视觉及统一 `UiTreeSnapshot` 仍归 UIE，因为 Vanilla/Forge 页面并不经过 ModularUI。
6. **NFR 本体中的 UI 组件不反向依赖 UIE。**NFR 已经依赖 ModularUI，因此 NFR 控件可以实现 ModularUI 的通用导航语义接口；UIE Adapter 仅作为旧版/第三方 Widget 的 fallback 和覆盖机制。
7. **树节点描述语义，不描述物理按键。**节点支持 `ACTIVATE`、`INCREMENT`、`QUICK_MOVE` 等动作，不出现 Xbox A/B/X/Y。
8. **不保留旧 GUI 导航 API 的兼容层。**目前没有已发布版本，最终切换完成后直接删除旧的坐标点导航实现。
9. **首版不同时重构 `InputApi` 的 GUI 采样边界。**Controller bridge 继续读取 SDL 快照，随后调用 UIE 导航 API。待树和焦点稳定后，再单独评估是否将 GUI 输入帧统一进入 `InputApi`。

期望调用链：

```text
SDL/controller state
  -> ControllerBindings
  -> ControllerUiInputBridge
  -> UiNavigationApi (UIE)
  -> UiNavigationRuntime
     -> UiFocusManager
     -> UiTreeSnapshot
     -> UiActionDispatcher
     -> active UiTreeSession/provider
  -> vanilla GuiScreen lifecycle
     or ModularUI NavigationBridge normal interaction lifecycle
```

## 2. 本次范围

### 2.1 必须完成

- UIE 对外 UI 树 API。
- UIE 全局逻辑焦点及焦点恢复。
- 方向导航、导航分组、滚动显露和模态焦点范围。
- UIE 焦点高亮绘制。
- ModularUI 通用导航语义、几何、动作分派、reveal 和 revision API。
- ModularUI 树 Provider。
- NFR 常用组件声明 ModularUI 导航语义。
- UIE 保留 ModularUI Widget Adapter 注册表，覆盖未升级的第三方 Widget。
- 原版普通按钮、容器槽位和 `GuiSlot` 的基础 Provider。
- Controller addon 改为 UIE 导航 API 的客户端。
- 页面返回、按钮激活、滑块调整、下拉菜单、文本输入、列表导航和容器基础动作。
- 对外 Provider/Adapter 注册入口。

### 2.2 后续扩展，不阻塞第一版

- 屏幕键盘。
- 配方书完整导航。
- 创造栏分类和搜索的全部专用动作。
- JEI/HEI 导航树。
- 控制器按钮提示条和震动反馈。
- 屏幕阅读器或无障碍描述输出。
- 把 GUI 设备采样正式统一到 `InputApi`。

### 2.3 明确不做

- 不把 SDL、Controller 配置或 Xbox 按键概念放进 UIE。
- 不把 UIE 的 `UiNode`、焦点管理或物理输入概念放进 ModularUI。
- 不让 ModularUI 负责 Vanilla/Forge 页面导航。
- 不依赖鼠标位置猜测 ModularUI 的 Widget 层级。
- 不让 NFR 本体依赖 UIE。
- 不在本计划中迁移配置或保留旧版本兼容数据。

## 3. 当前代码审计

### 3.1 当前 Controller GUI 实现

当前主要实现位于：

- `addons/ui-enhancements-controller/src/main/java/neofontrender/addons/controller/ControllerGuiInputRuntime.java`
- `addons/ui-enhancements-controller/src/main/java/neofontrender/addons/controller/ControllerGuiNavigation.java`
- `addons/ui-enhancements-controller/src/main/java/neofontrender/addons/controller/ControllerVirtualCursor.java`
- `addons/ui-enhancements-controller/src/main/java/neofontrender/addons/controller/mixin/MixinEntityRendererControllerGui.java`

当前运行时存在以下职责混合：

- 直接轮询 `SdlDeviceManager`。
- 读取 Controller binding。
- 维护虚拟光标。
- 通过反射调用 `GuiScreen.mouseClicked/mouseReleased/mouseClickMove`。
- 通过反射读取 `GuiScreen.buttonList`。
- 直接识别 `GuiContainer`、`GuiContainerCreative`、`GuiSlot`。
- 直接收集按钮和槽位中心坐标。
- 在 Controller addon 内实现方向选择算法。
- 直接关闭 GUI。
- 绘制虚拟光标。

这意味着 Controller addon 同时是设备适配器、UI 框架适配器和焦点系统。结果是：

- 普通页面没有真实逻辑焦点。
- 列表条目、嵌套控件、Tab 和弹窗没有结构语义。
- D-pad 只把鼠标跳到另一个坐标。
- 页面返回直接 `displayGuiScreen(null)`，绕过父页面和 Panel 生命周期。
- 所有页面被迫共享同一套虚拟鼠标逻辑。
- 其他输入设备无法复用导航能力。

### 3.2 ModularUI 可用能力

ModularUI 已经提供真实 Widget 树：

```text
GuiScreen implements IMuiScreen
  -> IMuiScreen.getScreen()
  -> ModularScreen.getPanelManager()
  -> PanelManager.getOpenPanels()
  -> ModularPanel.getChildren()
  -> IWidget.getChildren()
```

确认可用的关键接口：

- `IWidget.getChildren()`：公开父子树。
- `IWidget.getParent()`：公开父链。
- `IWidget.getArea()`：Widget 本地/布局区域。
- `IWidget.isEnabled()`、`areAncestorsEnabled()`：可操作性。
- `IWidget.canBeSeen(IViewportStack)`：视觉裁剪判断。
- `LocatedWidget.of(widget)`：累积 Panel、滚动和其他 viewport 变换。
- `PanelManager.getOpenPanels()`：顶层到下层的打开 Panel。
- `PanelManager.getTopMostPanel()`、`closeTopPanel()`：弹窗层级和返回。
- `ModularGuiContext.focus()`：文本输入焦点。
- `Interactable`：鼠标、键盘、滚动和拖动生命周期。

限制：

- ModularUI 的 Widget 树是布局/渲染树，不是控制器语义树。
- `Interactable` 不能说明 Widget 是按钮、滑块、Tab 还是槽位。
- 普通按钮不是 `IFocusedWidget`；`IFocusedWidget` 主要服务文本输入。
- `getArea()` 不能脱离 viewport 变换和裁剪单独使用。
- 弹窗下层 Panel 的节点仍存在，但可能不允许交互。
- 直接调用某个 Widget 的 `onMousePressed()` 会绕过 Panel 保存的 press/release/tap 状态。

允许修改 ModularUI 后，应在框架中补齐这些缺口，而不是由 UIE 复制其内部算法：

- `INavigationElement`：Widget 自述 role、label、动作能力、导航分组和显式邻居。
- `NavigationTreeView`：按 Panel 层级公开只读树、稳定路径、状态与 revision。
- `NavigationGeometry`：公开已应用 viewport 变换和裁剪后的屏幕 bounds。
- `NavigationBridge`：以目标 Widget 为参数执行 press/release/tap、键盘和滚动动作，复用 Panel 的状态机。
- `INavigationViewport.revealChild(...)`：由滚动容器自己执行最小滚动显露。

因此 UIE 仍建立跨框架的不可变语义树，但 ModularUI Provider 应消费这些公共能力，不复制 `TransformationMatrix`、Panel 鼠标状态机或 `ScrollData` 内部逻辑，也不把裸 `IWidget` 暴露给 Controller。

### 3.3 NFR/UIE 组件分布

NFR 设置基础组件位于根项目：

- `src/main/java/neofontrender/client/gui/component/base/`
- `src/main/java/neofontrender/client/gui/component/business/`
- `src/main/java/neofontrender/client/gui/views/`
- `src/main/java/neofontrender/client/gui/layouts/`

UIE 设置页面位于：

- `addons/ui-enhancements/src/main/java/neofontrender/addons/**/**SettingsPage.java`

Controller addon 已经依赖 NFR 和 UIE，NFR 与 UIE 均依赖 ModularUI。允许的依赖方向是：

```text
ModularUI fork <- NFR <- UIE <- Controller addon
ModularUI fork <-------- UIE
```

因此跨框架树和焦点实现在 UIE；NFR 组件不能实现 UIE 接口，但可以实现 ModularUI 的 `INavigationElement`。UIE Adapter 注册表仍保留，用于：

- 未实现新接口的 ModularUI 标准/第三方 Widget。
- 不便修改源码的外部 Widget。
- 对已有 Widget 语义进行 owner-specific 覆盖。

### 3.4 ModularUI 依赖交付现状

当前 NFR 根项目运行依赖和 UIE 编译依赖都固定在 Maven 坐标 `com.cleanroommc:modularui:3.1.6`：

- `gradle/scripts/dependencies.gradle` 使用 `modImplementation`。
- `addons/ui-enhancements/build.gradle` 使用 `compileOnly`。
- `NeoFontRender` 声明 `required-after:modularui@[3.1.6,)`。

本地可修改源码位于 `D:/Projects/sfr/other_mods/ModularUI`。实施前必须先确定 fork 版本和唯一解析方式，不能让编译时使用 fork、运行时却加载原版 `3.1.6`。

推荐使用新版本 `3.2.0-nfr.1`（最终字符串必须先用 Forge `DefaultArtifactVersion`/FML 依赖范围测试确认排序），发布到 `mavenLocal()` 或项目内本地 Maven 仓库，然后同时更新根项目和 UIE 的依赖坐标。根项目已有 `mavenLocal()`，UIE 子项目当前没有，必须补同一个受限仓库或改为统一的项目仓库配置。开发期可以使用 dependency substitution，但发布 jar 不应依赖 composite build 路径。

解析验证不能只看 Gradle task 成功，还要输出根项目和 UIE 的 dependency insight，确认二者解析到同一个 fork artifact 文件与版本。fork 保持 mod id `modularui`，它是原版 ModularUI 的替换包，不能让两个 jar 同时存在。

### 3.5 现有 Input API 的边界

`InputApi.beginFrame(partialTicks, mc.inGameHasFocus)` 当前在相机鼠标采样位置执行。打开 GUI 时 `inGameHasFocus` 通常为 false，因此当前实现会发布 flush/neutral frame。与此同时，`SdlDeviceSource` 会在 `!frame.isGameFocused()` 时返回断开样本。

所以第一阶段不能简单改成：

```text
UIE navigation runtime -> InputApi.getFrame() -> GUI actions
```

否则打开 GUI 后恰好拿不到 Controller 输入。

首版明确采用：

```text
ControllerUiInputBridge -> SdlDeviceManager snapshot -> ControllerBindings -> UiNavigationApi
```

这保持现有设备生命周期，避免本次同时改变世界内输入采样。后续如统一 GUI 输入，应给 `InputFrameContext` 增加明确的 client/window/screen focus 语义，而不是复用 `gameFocused`。

## 4. 目标职责边界

### 4.1 UIE 所有权

UIE 拥有：

- 当前 `GuiScreen` 对应的树 Session。
- 当前不可变树快照。
- 逻辑焦点和活动焦点范围。
- 焦点恢复及树 revision。
- 方向导航算法。
- 发起节点显露（reveal）；具体 ModularUI 滚动由其 viewport 实现。
- 抽象动作分派和结果归一化。
- 跨框架的 modal scope；ModularUI Panel 层级由其 Provider 映射。
- 合成 UI 指针及正常事件路由。
- 焦点高亮。
- Provider、Widget Adapter 和动作扩展注册表。
- 屏幕交互策略：焦点、虚拟光标或混合。

### 4.2 ModularUI fork 所有权

ModularUI fork 拥有：

- Widget 的可选通用导航语义接口。
- Panel/Widget 只读树视图和结构 revision。
- 稳定 Widget path 的框架级生成规则。
- Widget 变换后 bounds、祖先 viewport 裁剪和可见性计算。
- 对指定 Widget 的合法 press/release/tap/key/scroll 分派。
- 文本编辑焦点与 `ModularGuiContext` 的同步。
- 滚动容器将后代显露到 viewport 的通用实现。
- Panel 打开/关闭及 topmost modal 层级。

ModularUI fork 不拥有：

- UIE `UiNode`/`UiTreeSnapshot` 类型。
- 跨 Vanilla、Forge、ModularUI 的全局逻辑焦点。
- 空间方向导航算法或基岩版输入策略。
- Controller、SDL、按键绑定或 glyph。
- NFR 专属组件类判断。

### 4.3 Controller addon 所有权

Controller addon 保留：

- SDL 初始化、设备发现和目标手柄选择。
- 原始控制采样、归一化和绑定。
- 按键边沿和长按输入来源。
- 手柄输入模式切换。
- 连续虚拟光标速度曲线。
- 虚拟光标贴图和手柄专属提示。
- 物理按键到 UIE `UiIntent` 的映射。

Controller addon 删除：

- `GuiScreen` 反射。
- 按钮/槽位/列表扫描。
- UI 焦点存储。
- 页面方向选择算法。
- ModularUI 类型识别。
- 页面返回生命周期实现。
- 容器点击语义实现。

### 4.4 Provider 所有权

每个 `UiTreeProvider` 负责：

- 判断是否支持当前 `GuiScreen`。
- 创建屏幕生命周期内的 `UiTreeSession`。
- 构造语义节点及稳定 ID。
- 执行底层框架特有动作。
- 将节点滚动到可见区域。
- 处理屏幕级返回。
- 声明页面交互策略。

## 5. 公共 API 设计

建议包路径：

```text
neofontrender.addons.api.ui.navigation
```

这些类属于 UIE jar 的公开 API，不放进 NFR 本体。

### 5.1 基础枚举

```java
public enum UiDirection {
    UP, DOWN, LEFT, RIGHT
}

public enum UiRole {
    ROOT,
    PANEL,
    GROUP,
    BUTTON,
    TOGGLE,
    CYCLE,
    SLIDER,
    TEXT_INPUT,
    LIST,
    LIST_ITEM,
    TAB_LIST,
    TAB,
    DROPDOWN,
    MENU,
    MENU_ITEM,
    INVENTORY,
    INVENTORY_SLOT,
    FLUID_SLOT,
    SCROLL_VIEW,
    CANVAS,
    CUSTOM
}

public enum UiAction {
    ACTIVATE,
    SECONDARY,
    INCREMENT,
    DECREMENT,
    SCROLL_UP,
    SCROLL_DOWN,
    QUICK_MOVE,
    TAKE_HALF,
    DROP,
    PAGE_PREVIOUS,
    PAGE_NEXT,
    BACK,
    BEGIN_EDIT,
    END_EDIT
}

public enum UiInteractionMode {
    FOCUS,
    CURSOR,
    CONTAINER,
    HYBRID,
    TEXT_INPUT
}
```

`UiInteractionMode` 是页面策略，不是当前设备模式：

- `FOCUS`：普通按钮页、设置页、确认框。
- `CURSOR`：画布、地图或无法构建可靠节点的页面。
- `CONTAINER`：槽位光标和容器语义。
- `HYBRID`：创造栏、配方书、带画布和按钮的复杂页面。
- `TEXT_INPUT`：聊天或文本编辑优先页面。

### 5.2 节点 ID 和几何

```java
public final class UiNodeId {
    private final ResourceLocation namespace;
    private final String path;
}

public final class UiRect {
    public final int left;
    public final int top;
    public final int right;
    public final int bottom;
}
```

要求：

- ID 在同一屏幕 Session 中稳定。
- UI rebuild 后同一个业务控件应尽量得到相同 ID。
- 不使用 `System.identityHashCode()` 作为公开 ID。
- ModularUI 优先使用 `panel name + named widget path`。
- 未命名 Widget 使用父节点路径、同角色兄弟序号和 Adapter 类型组合。
- 原版按钮优先使用屏幕类、按钮 id 和语义角色。
- 容器槽位使用 container/window identity 和 `slotNumber`。

### 5.3 只读节点和树快照

```java
public interface UiNode {
    UiNodeId id();
    UiNodeId parentId();
    UiRole role();
    String label();
    UiRect bounds();
    UiRect visibleBounds();
    List<UiNodeId> children();
    Set<UiAction> actions();
    UiNavigationHints navigation();
    boolean enabled();
    boolean visible();
    boolean focusable();
}

public interface UiTreeSnapshot {
    long revision();
    GuiScreen screen();
    UiInteractionMode interactionMode();
    List<UiNodeId> roots();
    UiNode node(UiNodeId id);
    Collection<UiNode> nodes();
    UiNodeId focusedNodeId();
    UiNodeId activeScopeId();
}
```

公开节点是不可变快照，不暴露 `IWidget`、`GuiButton` 或 `Slot`。底层对象引用只保存在 Provider Session 的私有 handle 表中。

### 5.4 导航提示

```java
public final class UiNavigationHints {
    String group;
    int order;
    UiAxis primaryAxis;
    boolean wrap;
    boolean trapFocus;
    boolean preferChildren;
    Map<UiDirection, UiNodeId> explicitNeighbors;
}
```

用途：

- 设置左侧分类属于 `settings.tabs`。
- 右侧内容属于 `settings.content`。
- 下拉菜单打开后 `trapFocus=true`。
- 滑块左右输入先执行增减，上下输入离开。
- Tab 列表可以明确相邻项并允许循环。
- 不规则布局仍可用显式邻居覆盖空间算法。

### 5.5 Provider 和 Session

```java
public interface UiTreeProvider {
    boolean supports(GuiScreen screen);
    UiTreeSession open(GuiScreen screen);
}

public interface UiTreeSession extends AutoCloseable {
    GuiScreen screen();
    UiTreeSnapshot snapshot();
    UiActionResult perform(UiNodeId node, UiAction action);
    UiActionResult reveal(UiNodeId node);
    UiActionResult back();
    void refresh();
    @Override void close();
}
```

注册入口：

```java
UiRegistration UiNavigationApi.registerProvider(
        ResourceLocation id,
        int priority,
        UiTreeProvider provider);
```

Provider 选择规则：

1. 按优先级从高到低。
2. 第一个 `supports(screen)` 返回 true 的 Provider 获得屏幕。
3. 同优先级按注册 id 排序，保证确定性。
4. Provider 回调异常要记录 owner、operation 和 screen class，不静默降级。
5. 若没有 Provider，创建 `CURSOR` 模式的空树 Session。

### 5.6 导航服务

```java
public final class UiNavigationApi {
    public static UiTreeSnapshot currentTree();
    public static UiFocusState focusState();
    public static UiNavigationResult navigate(UiDirection direction, UiInputSource source);
    public static UiActionResult perform(UiAction action, UiInputSource source);
    public static UiActionResult back(UiInputSource source);
    public static void movePointer(double x, double y, UiInputSource source);
    public static UiInteractionLease acquire(UiInputSource source);
}
```

`UiInputSource` 使用 `ResourceLocation owner` 和通用 modality：

```text
POINTER
KEYBOARD
CONTROLLER
AUTOMATION
ACCESSIBILITY
```

它不包含 Controller 型号或物理按钮。

`UiInteractionLease` 用于标记当前由谁驱动导航视觉：

- Controller 开始操作时 acquire。
- 真实鼠标产生有效移动/滚轮/按键时，UIE 切换为 POINTER。
- 屏幕关闭、窗口失焦或 Controller 断开时释放。
- 逻辑焦点可以保留，但非焦点 modality 下隐藏 Controller 高亮。
- 任何 lease 都不能跳过 Provider 的正常动作生命周期。

## 6. UIE 内部实现

建议实现包：

```text
neofontrender.addons.navigation
```

### 6.1 `UiNavigationModule`

职责：

- 在 UIE `preInit` 注册内建 Provider 和 Adapter。
- 在 UIE `init` 注册 Forge event runtime。
- 注册 ModularUI Provider、标准 fallback Adapter 和 UIE 自身的显式 override。
- 不读取任何 Controller 类。

将它加入 `NfrUiEnhancements.MODULES`，与其他 UIE feature 一样参与生命周期。

### 6.2 `UiNavigationRuntime`

职责：

- 监听 `GuiOpenEvent`。
- 屏幕改变时关闭旧 Session、清除按下状态并打开新 Session。
- 在 GUI draw 前按需刷新树。
- 维护 dirty/revision，而不是每次查询都无条件重建。
- 屏幕 resize、ModularUI Panel 数量变化、Widget rebuild、列表滚动后标记 dirty。
- 保证所有公开 API 仅在 Minecraft client thread 修改状态。
- 对外返回不可变快照。

状态建议：

```text
currentScreen
currentProviderId
currentSession
currentSnapshot
focusManager
pointerState
activeInputSource
dirtyReason
lastRefreshFrame
```

### 6.3 `UiFocusManager`

焦点状态包含：

```text
focusedNodeId
activeScopeId
lastFocusedByScope
inputSource
focusVisible
editing
```

树刷新后的恢复顺序：

1. 原 ID 仍存在、可见、启用、可聚焦：保留。
2. 原 ID 存在但暂时不可见：调用 `reveal()`，下一次刷新再判断。
3. 原节点消失：尝试同一父节点下最近的可聚焦兄弟。
4. 父节点也消失：沿旧祖先路径向上查找保存的 scope focus。
5. 最后选择活动 scope 中第一个可聚焦节点。
6. 空树则清除焦点。

模态规则：

- 最上层 Panel 或 Menu 可建立新的 `activeScopeId`。
- scope 设置 `trapFocus` 时，方向导航不得逃到下层页面。
- B/BACK 先关闭当前 scope，再恢复下层 scope 的上次焦点。
- 主 Panel 关闭时才关闭整个屏幕。

### 6.4 `UiSpatialNavigator`

选择优先级：

1. 当前节点的显式邻居。
2. 当前 group 内符合方向的节点。
3. 同一 scope 内其他 group 的节点。
4. 按 lane 偏移、前向距离、欧氏距离和声明 order 评分。

评分必须是纯函数并独立测试。建议基础模型：

```text
score = crossAxisPenalty
      + forwardDistance
      + euclideanTieBreak
      + groupTransitionPenalty
      + orderTieBreak
```

规则：

- 候选中心必须位于目标半平面。
- 同行/同列优先，但不能完全排斥轻微错位。
- 重叠节点使用 z/scope 和 order 解决。
- `wrap=true` 只在当前 group 内循环。
- 滑块、文本编辑和菜单可以先消费方向动作。
- 长按重复由输入客户端产生，空间导航函数本身不计时。

### 6.5 `UiActionDispatcher`

分派顺序：

1. 读取当前焦点。
2. 验证节点 revision、enabled、visible 和 action capability。
3. 若节点不可见但可 reveal，先 reveal 并返回 `DEFERRED`。
4. 调用当前 Session 的 `perform(nodeId, action)`。
5. 若动作改变树，标记 dirty。
6. 刷新后恢复/更新焦点。
7. 返回 `HANDLED`、`IGNORED`、`DEFERRED`、`STALE` 或 `FAILED`。

按角色默认方向行为：

| 当前节点 | LEFT/RIGHT | UP/DOWN |
|---|---|---|
| 普通按钮 | 导航 | 导航 |
| 滑块 | `DECREMENT/INCREMENT` | 导航 |
| 横向 Tab | 前后 Tab | 离开 Tab 组 |
| 纵向 Tab | 离开 Tab 组 | 前后 Tab |
| 下拉菜单项 | 可选快速切值 | 前后菜单项 |
| 文本编辑状态 | 交给文本框 | 交给文本框或显式退出编辑 |
| 容器槽位 | 槽位导航 | 槽位导航 |

### 6.6 `UiPointerState`

UIE 需要维护一个输入无关的合成指针，因为正常 `GuiScreen` 和 ModularUI 的事件生命周期都依赖鼠标坐标。

职责：

- 保存物理指针与合成指针位置。
- 焦点变化时把合成指针移动到节点可见区域中心。
- Controller `CURSOR/HYBRID` 模式可连续更新合成指针。
- `ACTIVATE` 时按下、释放使用同一坐标和屏幕。
- 真实鼠标接管时恢复物理指针位置和 POINTER modality。
- 屏幕变化或窗口失焦时释放所有合成按键。
- 坐标更新按 render frame，而不是只按 20 TPS。

UIE 应接管当前 Controller mixin 的“替换 Forge drawScreen 鼠标参数”职责。Controller addon 只把虚拟光标位置发布给 `UiNavigationApi.movePointer()`。

### 6.7 `UiFocusRenderer`

首版使用 UIE 统一覆盖层绘制焦点，不修改所有 NFR 组件：

- 在 `GuiScreenEvent.DrawScreenEvent.Post` 绘制。
- 使用节点 `visibleBounds`，不能使用未裁剪 bounds。
- 不绘制在当前模态 scope 之外。
- Controller modality 下显示；真实鼠标接管后隐藏。
- 文本框编辑时使用不同状态，避免和插入光标混淆。
- 容器槽位可使用紧凑边框；普通控件使用外侧焦点框。
- 焦点绘制不能改变布局或 Widget 尺寸。

后续如需要更精细视觉，可以让 ModularUI navigation metadata 或 UIE 节点装饰器提供 focus render hint，但不改变焦点所有权。

## 7. ModularUI Provider 设计

这一层拆成两部分：先在 ModularUI fork 中提供稳定的框架能力，再由 UIE Provider 转换成统一 `UiTreeSnapshot`。UIE 不再直接依赖 `TransformationMatrix`、`MouseData`、`ScrollData` 等 internal 类型。

### 7.1 ModularUI fork 公共导航契约

建议在 ModularUI 新增：

```text
com.cleanroommc.modularui.api.navigation/
  INavigationElement.java
  NavigationAction.java
  NavigationActionResult.java
  NavigationAxis.java
  NavigationGeometry.java
  NavigationInfo.java
  NavigationRole.java
  NavigationTreeEntry.java
  NavigationTreeView.java

com.cleanroommc.modularui.screen.navigation/
  ModularNavigationAccess.java
  ModularNavigationDispatcher.java
  ModularNavigationGeometry.java
  ModularNavigationRevealer.java
```

`NavigationInfo` 是输入设备无关的 Widget 语义，至少包含：

```java
public final class NavigationInfo {
    String id();                  // Widget 本地稳定 id，可空
    NavigationRole role();
    String label();               // 当前语言下可读名称，可空
    Set<NavigationAction> actions();
    String group();               // 可空
    int order();
    NavigationAxis primaryAxis();
    boolean focusable();
    boolean wrap();
    boolean trapFocus();
}
```

约束：

- 不引用 UIE、Controller、SDL、Forge `KeyBinding` 或 Xbox glyph。
- `NavigationRole`/`NavigationAction` 是 ModularUI 自身的框架枚举；UIE Provider 显式映射到 `UiRole`/`UiAction`。
- 未声明语义的布局 Widget 仍可出现在树视图中，但默认不可聚焦。
- `Interactable` 不再自动等同于按钮；未知 `Interactable` 由 UIE fallback Adapter 决定是否暴露。
- `IFocusedWidget` 保持“键盘编辑焦点”含义，不扩成所有按钮的导航焦点。

`INavigationElement` 应是可选接口；同时给 `AbstractWidget` 提供通用 builder/setter 存储 `NavigationInfo`，使标准 Widget、NFR Widget 和第三方 Widget 无需继承新基类即可声明语义。接口只负责读取，不持有 UIE 焦点状态。

### 7.2 只读树与 revision

`ModularNavigationAccess.capture(ModularScreen)` 返回 `NavigationTreeView`：

- 根按 `PanelManager.getOpenPanels()` 的顺序组织。
- 每项包含 Widget、父项、子项、稳定 path、enabled/visible 状态和 `NavigationInfo`。
- 公开集合不可变，capture 期间使用 `PanelManager.doSafe(...)` 避免 Panel dispose/rebuild 竞争。
- 稳定 path 优先使用 Panel name 和 Widget `navigation id`/name；未命名节点才使用同父节点下的类型及结构序号。
- 顶层 modal Panel 明确标记为 active scope；下层 Panel 保留在诊断树但不可导航。

ModularUI 分开维护单调递增的 `structureRevision` 和 `geometryRevision`：

- `structureRevision`：Panel open/close/dispose/reopen，Widget add/remove/replace/reorder，Widget name/navigation metadata、enabled/visible 或动作能力改变。
- `geometryRevision`：resize、Panel animation、viewport transform、滚动位置或可见裁剪改变。
- 结构变化同时使几何失效，因此递增两者；纯滚动/动画只递增 `geometryRevision`。

首版允许 UIE 每 render frame 查询两个 revision。两者都未变时复用整个快照；仅 geometry 变化时更新节点 bounds/visibleBounds，不运行焦点消失恢复；structure 变化时重建语义树并执行焦点恢复。不能通过每帧深度遍历计算 hash 代替 revision，因为 Controller 设置页会动态 rebuild，且复杂页面树可能很大。

### 7.3 公共几何 API

`NavigationGeometry` 直接返回：

```text
absoluteBounds      应用完整 Widget/viewport 变换后的屏幕包围盒
visibleBounds       与所有祖先 viewport 和屏幕裁剪区求交后的区域
visible             当前是否至少有可交互可见面积
topPanelInteractive 是否属于当前可交互 Panel 层
```

实现位于 ModularUI 内部，可以继续使用 `LocatedWidget`、`GuiViewportStack` 和 `TransformationMatrix`，但这些 internal 类型不泄漏到返回值。矩形需要变换四角后取包围盒，不能只变换左上角。

### 7.4 目标 Widget 动作分派

新增 `ModularNavigationDispatcher.perform(IWidget target, NavigationAction action)`，统一验证：

- target 仍属于当前 screen 的打开 Panel。
- target 及祖先 enabled。
- target 位于 active Panel/scope。
- action 被 `NavigationInfo` 声明支持。
- 执行期间用 `PanelManager.doSafe(...)`。

动作不能由 UIE 直接调用 Widget 的 `onMousePressed()`。Dispatcher 在 ModularUI 内复用/抽取 `ModularPanel` 当前的 press、release、tap、focus、sync handler 和变换上下文逻辑：

- `ACTIVATE`/`SECONDARY` 对目标执行完整 press + release + tap 生命周期。
- `BEGIN_EDIT` 调用 `ModularGuiContext.focus()`，只接受 `IFocusedWidget`。
- `END_EDIT` 解除编辑焦点，但不关闭 UIE 页面焦点。
- `INCREMENT`/`DECREMENT` 交给实现语义值接口的 Slider/Cycle Widget，不能用不精确的鼠标坐标模拟。
- `SCROLL_*` 交给目标或最近可滚动祖先。
- Dispatcher 返回 handled、tree changed、screen changed、rejected/stale 等结构化结果。

真实指针或 UIE 虚拟光标模式仍走现有基于 hover 的 `ModularScreen.onMousePressed/onMouseRelease` 路径。目标 Widget Dispatcher 专用于焦点导航动作，两条路径共享底层 helper 和状态清理规则。

### 7.5 滚动显露 API

新增 `ModularNavigationAccess.reveal(IWidget target)`：

1. 验证 target 属于当前树。
2. 沿父链收集实现导航 viewport/reveal 接口的容器。
3. 从内到外执行最小滚动，使目标完整或最大程度可见。
4. 每层滚动后重新计算下一外层的几何。
5. clamp 滚动值并递增 `geometryRevision`。
6. 返回是否移动以及最终可见状态。

`AbstractScrollWidget` 提供默认实现，内部直接操作自己的 `ScrollArea`/`ScrollData`。UIE 不调用不存在或不稳定的 `ScrollData.scrollTo()`，也不复制不同滚动 Widget 的计算。

### 7.6 标准 Widget 语义

ModularUI fork 为标准 Widget 提供默认语义和动作：

| Widget | Role | 动作 |
|---|---|---|
| `ButtonWidget` | `BUTTON` | `ACTIVATE`, optional `SECONDARY` |
| `AbstractCycleButtonWidget` | `CYCLE` | `ACTIVATE`, `INCREMENT`, `DECREMENT` |
| `SliderWidget` | `SLIDER` | `INCREMENT`, `DECREMENT` |
| `BaseTextFieldWidget` | `TEXT_INPUT` | `BEGIN_EDIT`, `END_EDIT`, `ACTIVATE` |
| `AbstractScrollWidget` | `SCROLL_VIEW` | `SCROLL_UP`, `SCROLL_DOWN` |
| `ItemSlot` | `INVENTORY_SLOT` | `ACTIVATE`, `SECONDARY`, `QUICK_MOVE`, `TAKE_HALF`, `DROP` |
| `FluidSlot` | `FLUID_SLOT` | `ACTIVATE`, `SECONDARY` |
| `ContextMenuButton` | `DROPDOWN` | `ACTIVATE`, `BACK` |
| `MenuPanel` | `MENU` scope | `BACK` |

默认语义必须保守。例如没有 click handler/sync handler 的纯装饰 `ButtonWidget` 不应自动成为可聚焦按钮；Slider 的 step 必须来自 stopper/each 或显式 navigation step，不能猜固定百分比。

### 7.7 UIE ModularUI Provider

建议包：

```text
neofontrender.addons.navigation.modularui
```

屏幕识别：

```java
boolean supports(GuiScreen screen) {
    return screen instanceof IMuiScreen;
}
```

通过 `((IMuiScreen) screen).getScreen()` 取得 `ModularScreen`。

Provider 的职责缩小为：

- 将 `NavigationTreeView` 和 `NavigationInfo` 映射成 UIE 不可变节点。
- 将 ModularUI stable path 转成 `UiNodeId`。
- 将 top Panel 映射为 active modal scope。
- 把 UIE action 映射给 `ModularNavigationDispatcher`。
- 把 UIE reveal 映射给 `ModularNavigationAccess.reveal()`。
- 将 ModularUI structure/geometry revision 映射到 UIE snapshot revision；只有结构变化触发焦点恢复。
- BACK 优先关闭 top Panel；只剩 main Panel 时走正常 screen close。

Provider 不再自行计算 viewport 变换、操纵 `ScrollData` 或模拟 Widget press 状态。

### 7.8 UIE Widget Adapter fallback

```java
UiRegistration UiNavigationApi.registerModularWidgetAdapter(
        ResourceLocation id,
        Class<? extends IWidget> type,
        int priority,
        ModularWidgetAdapter adapter);
```

选择最具体类型；相同类型按 priority 和 id 确定。首批内建映射：

- 新接口不可表达的 owner-specific override。
- 尚未迁移的第三方 Widget。
- 诊断期间临时补语义。

选择顺序为显式 UIE Adapter override、Widget 自带 `NavigationInfo`、ModularUI 标准默认语义、不可聚焦。未知 `Interactable` 不自动暴露为 `CUSTOM + ACTIVATE`，只记录诊断，避免误触装饰或 click-through Widget。

### 7.9 NFR 组件语义

NFR 组件构造时直接声明 ModularUI 通用语义：

| NFR 组件 | 声明语义 |
|---|---|
| `NfrTextButton` | label supplier、`BUTTON` |
| `NfrCategoryButton` | `TAB`、selected state、`settings.tabs` group |
| `NfrSettingsTabs` | `TAB_LIST`、纵向、wrap policy |
| `NfrContentButton` | 根据内容标为 button/toggle |
| `NfrCycleButton` | `CYCLE` |
| `NfrDecimalSlider` | `SLIDER`、真实 step |
| `NfrTrackSliderWidget` | `SLIDER` |
| `NfrLabeledSlider` | group，焦点委托内部 slider |
| `NfrOptionDropdown` | `DROPDOWN`、弹出 menu scope |
| `NfrLabeledTextField` | group，焦点委托内部 text field |
| `NfrScrollablePane` | `SCROLL_VIEW`、reveal child |
| `NfrColorPickerButton` | `DROPDOWN/DIALOG` |

组合组件不能让父节点和内部可操作子节点重复聚焦。例如 `NfrLabeledSlider` 自身为不可聚焦 group，只把标签合并进内部 slider 的 accessible label。

`NfrOptionDropdown`、颜色选择器和其他动态 Panel 仍由 ModularUI 的 Panel/revision API 表达，不在 UIE 中识别它们的私有字段。文本框进入编辑时同步 `ModularGuiContext` 焦点；UIE 继续保留页面导航焦点，第一次 BACK 退出编辑，第二次才关闭 scope/page。

## 8. Vanilla/Forge Provider 设计

建议包：

```text
neofontrender.addons.navigation.vanilla
```

### 8.1 通用 `GuiScreen`

- `buttonList` 中 visible/enabled 的 `GuiButton` 映射成 `BUTTON`。
- ID 使用 screen class + button id；重复 id 加稳定序号并记录诊断。
- `ACTIVATE` 走正常 `mouseClicked/mouseReleased` 生命周期。
- BACK 模拟页面正常 ESC/返回语义，不直接无条件 `displayGuiScreen(null)`。
- 普通屏幕默认为 `FOCUS`。

建议用 UIE 自己的 Mixin accessor/invoker 替代 Controller 中的运行时反射：

- `AccessorGuiScreenNavigation`
- `InvokerGuiScreenNavigation`

### 8.2 `GuiContainer`

- Panel root 下创建 `INVENTORY` group。
- 每个可交互 `Slot` 创建 `INVENTORY_SLOT`。
- ID 使用 `slotNumber`。
- bounds 使用 `guiLeft + slot.xPos/yPos`。
- `ACTIVATE` -> `ClickType.PICKUP`, button 0。
- `SECONDARY/TAKE_HALF` -> `ClickType.PICKUP`, button 1。
- `QUICK_MOVE` -> `ClickType.QUICK_MOVE`。
- `DROP` 在手上有物品时对 outside slot 执行正确的 container click。
- 容器默认为 `CONTAINER`，允许连续虚拟光标和槽位方向导航。
- 不把空槽位一律排除；手上持有物品时空槽位必须可达。

### 8.3 `GuiSlot` 和 Forge `GuiScrollingList`

- 列表本身为 `LIST/SCROLL_VIEW`。
- 每个条目为 `LIST_ITEM`。
- 上下导航以条目顺序优先，不只按坐标。
- 目标条目不在可见区域时先滚动。
- 条目内部按钮作为子节点。
- 标题或分隔条可标为不可聚焦。
- 对外提供第三方列表条目 Adapter，因为 1.12.2 `GuiSlot` 本身不公开统一条目对象。

### 8.4 后续专用处理器

在基础 Provider 稳定后增加：

- `GuiInventory` / `GuiCrafting` + `GuiRecipeBook`。
- `GuiContainerCreative` 分类、搜索和翻页。
- `GuiChat` 和 `GuiTextField`。
- 世界列表、服务器列表、资源包列表、语言列表。
- 附魔、铁砧、信标等非标准点击区域。
- JEI/HEI 扩展 Provider。

## 9. Controller addon 改造

### 9.1 新 `ControllerUiInputBridge`

替换 `ControllerGuiInputRuntime` 的焦点/树职责，只做：

```text
poll selected controller snapshot
  -> resolve GUI InputAction
  -> detect edge/repeat
  -> translate to UiNavigationApi call
```

映射建议：

| Controller action | UIE 调用 |
|---|---|
| `GUI_NAV_UP/DOWN/LEFT/RIGHT` | `navigate(direction)` |
| `GUI_ACCEPT` | `perform(ACTIVATE)` |
| `GUI_SECONDARY` | `perform(SECONDARY)` |
| `GUI_QUICK_MOVE` | `perform(QUICK_MOVE)` |
| `GUI_BACK` | `back()` |
| `GUI_SCROLL_Y` | `SCROLL_UP/DOWN` 或 pointer scroll |
| `GUI_CURSOR_X/Y` | `movePointer()`，仅 CURSOR/CONTAINER/HYBRID |

长按 repeat 仍在 Controller bridge，因为 repeat 频率属于输入设备体验；UIE 每次收到一次明确导航意图。

### 9.2 虚拟光标

`ControllerVirtualCursor` 保留在 Controller addon，但只计算连续位置：

- 使用 render-frame delta 积分，而不是 Client Tick 固定步进。
- 输出位置给 `UiNavigationApi.movePointer()`。
- 视觉贴图仍可由 Controller addon 绘制。
- 点击、hover 和底层事件坐标由 UIE 的 `UiPointerState` 统一提供。
- 槽位吸附目标来自 `UiTreeSnapshot` 的 `INVENTORY_SLOT` 节点，不再扫描 `GuiContainer`。

### 9.3 删除内容

完整切换后删除：

- `ControllerGuiNavigation.java`
- `ControllerGuiNavigationTest.java`
- `ControllerGuiInputRuntime` 中所有 `GuiButton`、`GuiContainer`、`GuiSlot` 和反射逻辑
- Controller mixin 中对 Forge draw 参数的直接所有权；对应能力迁入 UIE

可以保留类名 `ControllerGuiInputRuntime` 但大幅缩减，也可以直接重命名为 `ControllerUiInputBridge`。由于未发布，建议直接重命名，避免旧职责名称继续误导。

## 10. 树更新和生命周期

### 10.1 Session 生命周期

```text
GuiOpenEvent(new screen)
  -> close old UiTreeSession
  -> release synthetic buttons
  -> open provider session
  -> build revision 1
  -> choose/restored initial focus

screen structure/layout changes
  -> mark dirty(reason)
  -> rebuild once before next navigation/draw
  -> increment revision if semantic tree changed
  -> restore focus

GuiOpenEvent(null) / window focus lost
  -> release input lease
  -> release synthetic buttons
  -> close session
  -> clear visible focus
```

### 10.2 Dirty 原因

建议枚举并保留诊断：

```text
SCREEN_OPENED
SCREEN_RESIZED
WIDGET_TREE_CHANGED
PANEL_STACK_CHANGED
SCROLLED
ACTION_PERFORMED
VISIBILITY_CHANGED
PROVIDER_REQUESTED
```

不要每帧无条件重建完整树。允许 Provider 在无法收到结构通知时用轻量 fingerprint 检查：

- screen identity
- Panel identity/order
- Widget count and named paths
- layout/scroll generation
- container slot count

### 10.3 Stale action

任何动作都携带当前 snapshot revision 和 node id。若动作执行前 revision 已变化：

1. 刷新树。
2. 用稳定 ID 重新查找节点。
3. 仍存在才执行。
4. 否则返回 `STALE`，不点击旧坐标。

## 11. 焦点视觉和输入模式

UIE 维护逻辑焦点，但只在适当 modality 显示：

| 最近有效输入 | 逻辑焦点 | 焦点框 | 合成指针 |
|---|---|---|---|
| Controller D-pad/摇杆导航 | 保留/更新 | 显示 | 移至焦点 |
| Controller cursor | 保留最近节点或 hover 节点 | 可弱化 | 显示 |
| 真实鼠标移动/点击 | 可保留 | 隐藏 | 释放给物理鼠标 |
| 键盘 Tab/方向 | 保留/更新 | 显示 | 可隐藏 |
| 屏幕关闭 | 清除 Session | 隐藏 | 释放 |

“有效鼠标移动”需要阈值，避免因 GLFW/LWJGL 合成或窗口坐标回写导致模式反复抖动。

## 12. 对外扩展契约

### 12.1 第三方屏幕 Provider

其他模组可以注册完整 Provider。适合：

- 自绘画布。
- 非标准容器。
- 大量隐藏点击区域。
- 特殊页面状态机。

### 12.2 ModularUI Widget Adapter

优先建议其他模组让自定义 Widget 实现 ModularUI `INavigationElement` 或设置 navigation metadata；这使语义对所有 ModularUI 导航消费者可见。无法修改 Widget 源码时，再向 UIE 注册 Adapter，无需接管整个 Screen。

### 12.3 节点装饰器

建议提供低风险的后处理扩展：

```java
UiRegistration registerNodeDecorator(id, priority, UiNodeDecorator decorator);
```

可用于：

- 添加 label/tooltip。
- 改 role。
- 添加导航 group/order。
- 禁用某个错误识别的节点。
- 添加专用 action。

装饰器不能改变底层 Widget 树，也不能直接执行输入。

### 12.4 诊断 API

公开只读诊断：

```text
active screen class
active provider id
tree revision
node count
focused node id/role
active scope
input modality/lease owner
last navigation result
last action result
last dirty reason
unknown interactable classes
```

这对用户测试 Controller 页面非常重要。

## 13. 逐阶段实施计划

Phase 0-6 已按本节完成基础实现。旧 Controller GUI runtime 已在 Phase 6 删除，
不再保留双路径 fallback；不具备结构 Provider 的页面由 UIE 的 cursor Session 处理。

### Phase 0：ModularUI fork 导航基础

状态：已完成。fork 版本为 `com.cleanroommc:modularui:3.2.0-nfr.1`，已发布到本地 Maven。

仓库：

```text
D:/Projects/sfr/other_mods/ModularUI
```

新增公共 API：

```text
src/main/java/com/cleanroommc/modularui/api/navigation/
  INavigationElement.java
  NavigationAction.java
  NavigationActionResult.java
  NavigationAxis.java
  NavigationGeometry.java
  NavigationInfo.java
  NavigationRole.java
  NavigationTreeEntry.java
  NavigationTreeView.java
```

新增内部实现：

```text
src/main/java/com/cleanroommc/modularui/screen/navigation/
  ModularNavigationAccess.java
  ModularNavigationDispatcher.java
  ModularNavigationGeometry.java
  ModularNavigationRevealer.java
```

修改：

- `PanelManager`：structure/geometry revision、Panel 层变更通知、安全树 capture。
- Widget add/remove/replace、enabled/name/metadata 路径递增 structure + geometry revision；resize/scroll/animation 路径只递增 geometry revision。
- `ModularPanel`：抽取可按目标 Widget 调用的 press/release/tap helper，现有鼠标路径也复用。
- `AbstractWidget`/`IWidget`：可选 navigation metadata builder/read API。
- `AbstractScrollWidget`：`revealDescendant` 默认实现。
- `ButtonWidget`、Cycle、Slider、TextField、Menu、Item/Fluid Slot：标准语义和离散动作。

交付：

- 选定 fork 版本，例如 `3.2.0-nfr.1`。
- 发布到 `mavenLocal()` 或项目内本地 Maven 仓库。
- 根项目 `gradle/scripts/dependencies.gradle` 和 UIE `build.gradle` 使用同一 fork 坐标；UIE 增加受限的 fork 仓库来源。
- 更新 NFR 的 `required-after:modularui` 最低版本，确保运行时不会加载缺少 API 的原版 `3.1.6`。
- 用 Gradle dependency insight 核对根项目运行依赖与 UIE 编译依赖的 resolved artifact 路径和版本完全一致。
- 用 Forge 版本比较测试验证 fork 版本满足新的 `required-after` 下界，而原版 `3.1.6` 不满足。
- 在最终交付说明中明确玩家必须同时更新 ModularUI、NFR、UIE 和 Controller addon。

测试：

- stable path、structure revision 和 geometry revision。
- fork 版本在 Forge/FML 比较器中的排序和依赖下界。
- 嵌套 viewport 的 absolute/visible bounds。
- top Panel/modal 可交互状态。
- 指定 Widget 完整 press/release/tap 生命周期。
- Slider/Cycle 离散动作。
- 文本框 focus/unfocus。
- 单层和嵌套滚动 reveal。
- action 期间 Widget rebuild/Panel close 不产生 CME 或 stale 调用。

验收：

- 新 API 不引用 NFR、UIE 或 Controller。
- 现有真实鼠标/键盘路径测试不回归。
- UIE 能只依赖公共 navigation 包完成 Provider，不访问 ModularUI internal 类型。
- fork jar 的 mod id 仍为 `modularui`，同一实例中不能与原版 ModularUI 共存。

### Phase 1：UIE 公共模型和纯逻辑核心

状态：已完成。

新增：

```text
addons/ui-enhancements/src/main/java/neofontrender/addons/api/ui/navigation/
  UiAction.java
  UiActionResult.java
  UiDirection.java
  UiFocusState.java
  UiInputModality.java
  UiInputSource.java
  UiInteractionLease.java
  UiInteractionMode.java
  UiNavigationApi.java
  UiNavigationHints.java
  UiNavigationResult.java
  UiNode.java
  UiNodeId.java
  UiRect.java
  UiRegistration.java
  UiRole.java
  UiTreeProvider.java
  UiTreeSession.java
  UiTreeSnapshot.java

addons/ui-enhancements/src/main/java/neofontrender/addons/navigation/
  ImmutableUiNode.java
  ImmutableUiTreeSnapshot.java
  UiFocusManager.java
  UiNavigationRegistry.java
  UiSpatialNavigator.java
  UiTreeValidator.java
```

测试：

```text
UiSpatialNavigatorTest
UiFocusManagerTest
UiTreeValidatorTest
UiNavigationRegistryTest
```

验收：

- API 不引用 Controller 或 SDL 类型。
- 快照不可变。
- Provider 优先级确定。
- 稳定 ID、父子闭合、重复 ID 都有验证。
- 方向导航、wrap、显式邻居和 modal scope 通过纯单元测试。
- 尚不注册运行时，不改变游戏行为。

### Phase 2：UIE Runtime、焦点和统一视觉

状态：已完成。

新增：

```text
UiNavigationModule.java
UiNavigationRuntime.java
UiActionDispatcher.java
UiPointerState.java
UiFocusRenderer.java
UiNavigationDiagnostics.java
FallbackCursorTreeProvider.java
```

修改：

- `NfrUiEnhancements.java`：注册 `UiNavigationModule`。
- UIE mixin/accessor 配置测试：加入必要的 GUI input/draw seam。

验收：

- 打开/切换/关闭屏幕正确创建和释放 Session。
- 空 Provider 页面为 `CURSOR`，不产生假焦点。
- 鼠标接管会隐藏焦点视觉。
- 屏幕关闭和窗口失焦释放合成按键。
- Runtime 没有 Controller 类依赖。

### Phase 3：ModularUI Provider 和 NFR 语义

状态：已完成基础控件接入。

新增：

```text
navigation/modularui/ModularUiTreeProvider.java
navigation/modularui/ModularUiTreeSession.java
navigation/modularui/ModularUiTreeBuilder.java
navigation/modularui/ModularWidgetAdapter.java
navigation/modularui/ModularWidgetAdapterRegistry.java
navigation/modularui/ModularUiActionExecutor.java
navigation/modularui/ModularNavigationMappings.java
```

修改 NFR 组件：

```text
NfrTextButton
NfrCategoryButton
NfrSettingsTabs
NfrContentButton / NfrCycleButton
NfrDecimalSlider / NfrTrackSliderWidget / NfrLabeledSlider
NfrOptionDropdown
NfrLabeledTextField
NfrScrollablePane
NfrColorPickerButton
```

这些组件只实现/设置 ModularUI navigation metadata，不导入 UIE 类型。

重点测试/夹具：

- 嵌套 ParentWidget。
- `NfrScrollablePane` 中部分不可见节点。
- 两层嵌套滚动。
- 设置左侧 Tab 和右侧内容两个 group。
- `NfrOptionDropdown` 打开 MenuPanel 后 scope 切换。
- `NfrLabeledSlider` 不产生父/子重复焦点。
- `NfrLabeledTextField` label 合并和编辑焦点同步。
- Panel 关闭后恢复下层焦点。
- ModularUI 两类 revision 均未变化时 UIE 不重建快照；纯 geometry 变化不触发焦点恢复。
- UIE Provider 不引用 `TransformationMatrix`、`ScrollData` 或 Panel 私有 press state。

验收页面：

- NFR 主设置页。
- Controller 设置页。
- UIE 任一含 toggle/dropdown/slider/text field 的页面。
- Chat history ModularUI 页面。

### Phase 4：Controller 作为 UIE 导航客户端

状态：已完成。Controller 只提交 UI intent，并仅绘制虚拟光标图形。

新增/重命名：

```text
ControllerUiInputBridge.java
ControllerUiRepeatState.java
ControllerVirtualCursorRuntime.java（如需要从现有类拆分）
```

修改：

- `ControllerAddonMod.java`：注册新 bridge。
- `ControllerBindings.java`：保持现有 GUI action，必要时新增 page/tab/take-half/drop 动作。
- `ControllerVirtualCursor.java`：使用 frame delta。
- `MixinEntityRendererControllerGui.java`：先变成薄转发，随后由 UIE pointer seam 取代。

临时切换规则：

- UIE Provider 支持的屏幕只走新 bridge。
- 不支持的屏幕走旧 cursor fallback。
- 两条路径必须有互斥 gate 和自动测试。

验收：

- NFR/UIE 设置页不再由 Controller 扫描 Widget。
- D-pad 更新 UIE focus。
- A/B/X/Y 只提交抽象动作。
- Controller 断开后 UIE focus lease 释放。
- 真实鼠标接管稳定，无模式抖动。
- 虚拟光标按 render frame 平滑移动。

### Phase 5：Vanilla 基础 Provider

状态：已完成基础实现，包含按钮、容器槽位、`GuiSlot` 和 Forge
`GuiScrollingList`；复杂专用页面仍归 Phase 7。

新增：

```text
navigation/vanilla/VanillaGuiTreeProvider.java
navigation/vanilla/VanillaGuiTreeSession.java
navigation/vanilla/VanillaButtonTreeBuilder.java
navigation/vanilla/VanillaContainerTreeBuilder.java
navigation/vanilla/VanillaListAdapterRegistry.java
navigation/vanilla/VanillaUiActionExecutor.java
navigation/vanilla/VanillaUiRevealController.java
```

新增或复用 accessor/invoker：

```text
AccessorGuiScreenNavigation
InvokerGuiScreenNavigation
Accessor/InvokerGuiContainer navigation methods
AccessorGuiSlotNavigation
```

验收：

- `GuiOptions` 和普通按钮页。
- `GuiInventory`、箱子、工作台。
- 世界、服务器、资源包、语言等 `GuiSlot` 页面。
- Forge `GuiScrollingList` 页面。
- 返回行为回到正确父页面。
- 列表导航自动滚动目标条目。

### Phase 6：删除旧实现并收紧 API

状态：已完成。旧坐标扫描 runtime、方向选择器和 Controller GUI mixin 已删除，
并增加模块边界回归测试。

删除/清理：

- `ControllerGuiNavigation.java` 和对应测试。
- `ControllerGuiInputRuntime` 的反射、树扫描、容器处理、滚动扫描。
- Controller addon 的 draw-coordinate mixin，若 UIE pointer seam 已完全替代。
- 不再使用的反射字段和 warning 状态。

加强测试：

- UIE Provider 和 Controller bridge 不会双重处理。
- Controller jar 不再包含 `GuiButton`/`GuiSlot` 扫描代码。
- UIE jar 不包含 SDL 或 Controller addon 类引用。
- Mixin 配置引用和字节码边界测试。

### Phase 7：复杂原版页面与扩展生态

状态：待后续实施。

逐项增加：

1. 配方书打开/关闭、分类、配方、分页、制作。
2. 创造栏分类、搜索框和页切换。
3. `GuiChat` 文本编辑。
4. 附魔、铁砧、信标等专用点击区域。
5. JEI/HEI Provider 或 node decorator。
6. 动作提示条和 Controller glyph。

每一种页面用专用 Session/Decorator，不继续向一个通用 runtime 堆 `instanceof`。

## 14. 预期修改文件清单

### ModularUI fork 必改

- 新增 `src/main/java/com/cleanroommc/modularui/api/navigation/**`。
- 新增 `src/main/java/com/cleanroommc/modularui/screen/navigation/**`。
- `PanelManager.java`、`ModularPanel.java`、`ModularGuiContext.java`。
- `IWidget.java`、`AbstractWidget.java` 及 Widget 树变更入口。
- `AbstractScrollWidget.java` 和 scroll data 实现。
- 标准按钮、Cycle、Slider、TextField、Menu、Item/Fluid Slot Widget。
- ModularUI 的单元测试/测试 GUI 和版本发布配置。

### NFR/构建边界必改

- `gradle/scripts/dependencies.gradle`：根项目运行时使用 fork 坐标。
- `addons/ui-enhancements/build.gradle`：UIE 增加受限 fork 仓库并使用同一 fork 坐标。
- `src/main/java/neofontrender/NeoFontRender.java`：提高 ModularUI 最低版本。
- NFR 常用组件声明 ModularUI navigation metadata。

### UIE 必改

- `addons/ui-enhancements/src/main/java/neofontrender/addons/ui/NfrUiEnhancements.java`
- `addons/ui-enhancements/src/main/resources/mixins.neofontrender_ui_enhancements.json`
- `addons/ui-enhancements/src/test/java/neofontrender/addons/mixin/UiEnhancementsMixinConfigTest.java`
- 新增 `addons/ui-enhancements/src/main/java/neofontrender/addons/api/ui/navigation/**`
- 新增 `addons/ui-enhancements/src/main/java/neofontrender/addons/navigation/**`
- 新增 `addons/ui-enhancements/src/test/java/neofontrender/addons/navigation/**`

### Controller 必改

- `addons/ui-enhancements-controller/src/main/java/neofontrender/addons/controller/ControllerAddonMod.java`
- `addons/ui-enhancements-controller/src/main/java/neofontrender/addons/controller/ControllerGuiInputRuntime.java`（最终替换/删除）
- `addons/ui-enhancements-controller/src/main/java/neofontrender/addons/controller/ControllerGuiNavigation.java`（最终删除）
- `addons/ui-enhancements-controller/src/main/java/neofontrender/addons/controller/ControllerVirtualCursor.java`
- `addons/ui-enhancements-controller/src/main/java/neofontrender/addons/controller/ControllerBindings.java`
- `addons/ui-enhancements-controller/src/main/java/neofontrender/addons/controller/mixin/MixinEntityRendererControllerGui.java`（最终删除或降为无状态转发）
- Controller mixin JSON 和测试。

### NFR 本体

首版会修改 NFR 组件，使其声明 ModularUI 通用导航语义，但不加入 UIE 类型。若 metadata supplier 无法取得必要 label/step/selected 状态，可增加组件自身的通用只读 getter。

可能需要的小范围 getter：

- `NfrTextButton.label()`
- `NfrCategoryButton.isSelected()`
- `NfrSettingsTabs` 的只读 tab 信息/方向
- `NfrDecimalSlider` 的 step 或 value adjustment helper
- `NfrLabeledSlider.slider()` / label
- `NfrLabeledTextField.field()` / label

这些 getter 是组件自身合理 API，不代表 NFR 依赖 UIE。

## 15. 测试计划

### 15.1 纯逻辑单元测试

- 重复节点 ID 被拒绝。
- 缺失 parent/child 引用被拒绝。
- 方向半平面筛选。
- 同行/同列优先级。
- group transition penalty。
- 显式邻居覆盖。
- wrap 与 trap focus。
- 树 revision 后焦点恢复。
- 删除焦点节点后的兄弟 fallback。
- modal scope 打开/关闭。
- stale action 不执行旧节点。
- action capability 检查。

### 15.2 Provider 测试

- ModularUI stable path 映射成稳定 UIE ID。
- fork 两类 revision 未变时复用快照；geometry 变化只刷新几何，structure 变化重建树。
- fork 提供的 absolute/visible bounds 无二次变换。
- 顶层 Panel active scope 正确映射。
- NFR 组合组件 metadata 无重复焦点。
- UIE Adapter 能覆盖 Widget 自带语义，异常 Adapter 不破坏树。
- Vanilla button ID 和 bounds。
- Container slot action 映射。
- List reveal 只做最小滚动。

### 15.3 Controller bridge 测试

- 按下边沿只执行一次。
- 长按 repeat 频率确定。
- 断开/切换手柄释放 lease 和按键。
- FOCUS 页面不连续移动 cursor。
- CURSOR 页面不误触方向焦点。
- HYBRID 页面两种模式切换不会双重执行。
- 鼠标接管后 Controller held state 不残留。

### 15.4 构建命令

每阶段至少执行：

```powershell
Set-Location D:\Projects\sfr\other_mods\ModularUI
.\gradlew.bat test
.\gradlew.bat build
.\gradlew.bat publishToMavenLocal

Set-Location D:\Projects\sfr\smoothfont-replacement
.\gradlew.bat compileJava
.\gradlew.bat test
.\gradlew.bat :addons:ui-enhancements:compileJava
.\gradlew.bat :addons:ui-enhancements:test
.\gradlew.bat :addons:ui-enhancements-controller:compileJava
.\gradlew.bat :addons:ui-enhancements-controller:test
.\gradlew.bat :addons:ui-enhancements-controller:jar

.\gradlew.bat dependencyInsight --dependency modularui --configuration minecraftRuntimeLibraries
.\gradlew.bat :addons:ui-enhancements:dependencyInsight --dependency modularui --configuration compileClasspath
```

上面的根项目 configuration 名称需在实施时以 `dependencies` 输出确认；如果 Unimined 生成的实际 configuration 不同，使用包含 `modularui` 的运行 configuration。验收关注的是 resolved 版本与 artifact 路径一致，而不是固定 task 参数名称。

涉及主项目 API/getter 时增加：

```powershell
.\gradlew.bat compileJava
.\gradlew.bat test
```

仅文档阶段执行：

```powershell
git diff --check
```

### 15.5 人工验收矩阵

用户自行游戏内测试时建议按以下矩阵记录：

| 页面 | 焦点移动 | 激活 | 返回 | 滚动 | 鼠标接管 | 重建恢复 |
|---|---|---|---|---|---|---|
| NFR 主设置页 | | | | | | |
| Controller 设置页 | | | | | | |
| 下拉菜单 | | | | | | |
| 文本输入 | | | | | | |
| Chat history | | | | | | |
| 原版 Options | | | | | | |
| 世界列表 | | | | | | |
| 玩家背包 | | | | | | |
| 箱子/工作台 | | | | | | |
| 创造栏 | | | | | | |

## 16. 风险和控制措施

### 16.1 ModularUI 内部 API 风险

维护 fork 会增加一个必须同步发布的运行依赖，同时 `TransformationMatrix` 等实现仍是 internal。控制措施：

- internal 类型只由 ModularUI 自己的 navigation 实现使用。
- ModularUI 公共 navigation API 和 UIE 公共 API 都不泄漏 internal 类型。
- fork 使用独立版本且提高 NFR 最低依赖版本，禁止原版 `3.1.6` 静默满足依赖。
- 构建、运行和发布全部解析同一个 fork 坐标。
- 为几何、revision、dispatcher 和 reveal 写 ModularUI 侧回归测试。
- 保持 fork 改动通用且边界清楚，便于未来向上游贡献或 rebase。

### 16.2 合成指针和真实鼠标争用

控制措施：

- 单一 `UiPointerState`。
- 明确 modality lease。
- 真实鼠标阈值和合成事件标记。
- 屏幕/焦点边界统一 release。
- 新旧 runtime 互斥。

### 16.3 动态 Widget rebuild

Controller 设置页和 NFR route 会重建 Widget。控制措施：

- 稳定语义 ID。
- revision 校验。
- action 后 refresh。
- scope 内 last-focused 恢复。
- 不缓存裸 `IWidget` 到屏幕 Session 之外。

### 16.4 列表和滚动变换

控制措施：

- bounds 与 visibleBounds 分离。
- 多层 viewport 逐级裁剪。
- reveal 内到外执行。
- 下一 frame 再定位焦点。
- 不在滚动前点击旧坐标。

### 16.5 范围膨胀

控制措施：

- 先打通 NFR/ModularUI，再做原版复杂页面。
- 配方书、创造栏和 JEI 独立 Phase。
- 公共模型只包含已知需要的 role/action；新增枚举要有实际 Provider 用例。
- 不在树重构中同时重构世界内输入和飞行逻辑。

## 17. 审阅检查点

开始编码前需要确认：

- [ ] 树、焦点、方向导航、动作分派和合成指针均归 UIE。
- [ ] ModularUI fork 只提供框架语义、树访问、几何、dispatcher、reveal 和 revision，不持有 UIE 全局焦点。
- [ ] Controller addon 只提交意图，不扫描 UI。
- [ ] 第一版 Controller 继续直接读取 SDL 快照，不改 `InputApi` GUI focus 语义。
- [ ] NFR 组件声明 ModularUI 通用语义，不反向依赖 UIE；UIE Adapter 只做 fallback/override。
- [ ] 根项目、UIE 和玩家运行环境统一使用带 navigation API 的 ModularUI fork 版本。
- [ ] UIE 公开不可变树快照，不公开 ModularUI/Vanilla 对象。
- [ ] ModularUI 页面优先于原版页面实施。
- [ ] 首版焦点视觉为 UIE 统一覆盖层。
- [ ] 未发布版本允许最终直接删除旧实现。
- [ ] 配方书、创造栏和 JEI 放在基础架构之后。

建议批准后的执行顺序：

```text
Phase 0 ModularUI fork API
  -> Phase 1 UIE API/core
  -> Phase 2 runtime/focus/pointer
  -> Phase 3 ModularUI Provider/NFR semantics
  -> Phase 4 Controller bridge
  -> Phase 5 vanilla base
  -> Phase 6 delete legacy
  -> Phase 7 complex screens
```

该顺序先让 ModularUI 提供合法、稳定的框架接缝，再让 UIE 成为可独立测试的跨框架导航平台，最后接入手柄。这样不会继续在 Controller runtime 或 UIE Provider 中复制 ModularUI 的 `instanceof`、坐标变换、滚动和 press-state 规则。
