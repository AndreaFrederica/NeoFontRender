# Cleanroom 0.6.13-alpha 升级核查

核查日期：2026-09-21。升级前备份提交：`24c08ae`（移除 UIE 旧配置迁移）。

## 版本与项目改动

- 原版本：`0.6.12-alpha`；新版本：`0.6.13-alpha`，官方发布于 2026-09-12。
- GitHub releases 与官方 Maven metadata 均确认该版本为核查时的最新发布版。
- `gradle.properties` 新增 `cleanroom_version`，主项目及 7 个依赖 Cleanroom 的附属模块统一读取该属性。
- 未改变 Java 25、Unimined、ModularUI 或其他显式依赖版本。

## 上游实际差异

对比两个发布标签，共 3 个提交、5 个文件，其中 4 个 Java 文件发生变化。

| 改动 | 实际行为及对本项目的影响 |
| --- | --- |
| `SideTransformer` 优化（`381edb2`） | 先检查类字节中是否包含 `SideOnly` 注解描述符；不存在时直接返回，避免构造 ASM 类树。属于类加载阶段优化，没有更换注解或修改我们的 Mixin 目标。 |
| Mod 列表重构（`6bc8dd7`） | `ModListExtended` 自己处理滚轮、滚动条拖动、鼠标命中和行坐标；统一可见区域裁剪及滚动条显示条件，增加条目删除与保留指定条目的接口。需要关注滚动、点击边界、缩放和拖动。 |
| 选中状态 | `ModListSelection` 负责绘制选中框；移除选中条目或清理其他条目时会清空对应选择。 |
| 列表图标缓存 | `ModListScreen` 直接复用 `ModData.itemIcon`，去掉条目内重复的图标字段；渲染失败时更新同一缓存为草方块。 |
| 更新日志（`fe96cc1`） | 仅补写 **0.6.12** 的旧版本说明，不能当作 0.6.13 新增修复。 |

两个版本 POM 的 92 项依赖坐标、版本与 scope 对比一致；本次没有随 Cleanroom 升级 LWJGL、Mixin 等传递依赖。

我们的 UIE 没有直接引用此次修改的 Mod 列表类，也没有对这些类注入 Mixin。不过 UIE 的通用 `GuiSlot` 平滑滚动与导航需要留意子类自行重写输入/绘制方法的情况；编译成功不能代替实际交互验证。

## 验证

- `.\gradlew.bat build --no-daemon --console=plain` 成功，96 个任务完成，耗时约 14 分钟。
- 使用 `HTTP_PROXY/HTTPS_PROXY=http://127.0.0.1:7890` 完成依赖下载和构建。
- `.\gradlew.bat runClient --no-daemon --console=plain` 已成功进入 Cleanroom 客户端；日志确认 `Cleanroom v0.6.13-alpha Initialized`、22 个 Mod 建立连接并进入单人世界。
- 没有新的崩溃报告，也没有发现 Mixin 注入失败、`NoSuchMethodError`、`NoClassDefFoundError` 或 `ClassNotFoundException`。
- 日志中的两条 ERROR 是开发环境既有提示：Maven library 目录格式提示，以及开发环境缺少 Forge binary patch set；它们不影响客户端启动和进入世界。
- 客户端目前保持运行，便于继续手动检查 UIE tooltip 和设置页面。尚未通过自动操作覆盖完整的物品悬停、F3 排版和预览动画回归。

## 来源

- [官方发布说明](https://github.com/CleanroomMC/Cleanroom/releases/tag/0.6.13-alpha)
- [完整标签差异](https://github.com/CleanroomMC/Cleanroom/compare/0.6.12-alpha...0.6.13-alpha)
- [官方 Maven 版本清单](https://repo.cleanroommc.com/releases/com/cleanroommc/cleanroom/maven-metadata.xml)
