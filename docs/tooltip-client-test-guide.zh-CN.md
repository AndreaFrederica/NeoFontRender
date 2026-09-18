# Tooltip 客户端测试流程

本次验证日期：2026-09-18。代码版本：本次迁移提交，分支 `feat/text-render-pipeline-rewrite`。

## 1. 已完成的构建与自动测试

- Gradle 构建与客户端准备任务成功，退出码为 0。
- Tooltip 公共 API：7 项测试通过。
- UIE Tooltip：79 项测试通过；UIE Mixin 配置：9 项测试通过。
- 合计 95 项，失败、错误、跳过均为 0。两个 test 任务通过 `--rerun` 实际重新执行。
- 根项目客户端启动所用的六个附属模块均已运行 `prepareCombinedClientRun`。
- UIE 构建产物与 `run/client/mods/neofontrender-ui-enhancements-dev.jar` 的 SHA-256 一致：
  `91BA17B744A9442C47A0916A3D2D4DA1A9BA4E6ED91DA79DEF92C4AF52B6FFAA`。
- 当前 `run/client/mods`（包括版本子目录）没有 Obscure。没有重置已有设置或存档。
- 尚未启动游戏；以下视觉检查需要在客户端完成。

本次执行的可复现命令（PowerShell）：

```powershell
Set-Location 'D:\Projects\sfr\smoothfont-replacement'
.\gradlew.bat :test --tests 'neofontrender.api.client.tooltip.NfrTooltipApiTest' --rerun `
  :addons:ui-enhancements:test --tests 'neofontrender.addons.tooltips.*' `
  --tests 'neofontrender.addons.mixin.UiEnhancementsMixinConfigTest' --rerun `
  :addons:ui-enhancements:prepareCombinedClientRun `
  :addons:ui-enhancements-controller:prepareCombinedClientRun `
  :addons:electric-elytra:prepareCombinedClientRun `
  :addons:mui-xml-showcase:prepareCombinedClientRun `
  :addons:inline-content-showcase:prepareCombinedClientRun `
  :addons:typst-renderer:prepareCombinedClientRun `
  --no-daemon --offline --console=plain
```

## 2. 启动

已有客户端先正常退出。在 PowerShell 执行：

```powershell
Set-Location 'D:\Projects\sfr\smoothfont-replacement'
.\gradlew.bat :runClient --no-daemon --offline --console=plain
```

这会使用 `D:\Projects\sfr\smoothfont-replacement\run\client` 作为游戏目录，启动前也会执行模块同步。无需再次手动拷贝 JAR。

进入允许作弊的创造模式测试世界。游戏内按默认快捷键 `O` 打开 NFR 设置，选择「现代工具提示」。若快捷键被修改或冲突，可在「选项 → 控制」中查看 NFR 设置键。每次修改后点「应用」。

设置页的大演示面板会使用同一套 TooltipLayout 和视觉块渲染，选择有预览的物品时可以先检查标题、正文、面板和侧边预览的相对位置；模型持续旋转、装备姿态和物品栏遮挡仍需在背包中悬停实际物品确认。

## 3. 起始设置

现有配置保留了上次的组合：

| 设置 | 当前值 |
| --- | --- |
| 工具提示替换 | 开 |
| 物品标题图标 | 开 |
| 物品图标外框 / 背景 / 稀有度标签 | 均关 |
| 物品图标圆角 | 开，半径 3 |
| 物品模型预览 / 护甲模型预览 | 均开 |
| 物品预览范围 | 工具与武器 |
| 护甲模型 / 内容 | 盔甲架 / 当前装备 |
| 盔甲架底座 | 关 |
| 实验室中的 F3 Tooltip 布局边界 | 开 |

首次观察先关闭游戏 F3。为了排除自定义样式影响，基础测试可暂时关闭「预览装饰效果」。测试图标圆角时需要开启外框或背景；两者都关闭时只有物品纹理，不会看到圆角容器。

## 4. 准备物品

在游戏聊天框逐条执行，下列是 Minecraft 1.12.2 的命令格式：

```text
/gamemode 1
/give @p minecraft:stick 1 0
/give @p minecraft:stone 1 0
/give @p minecraft:diamond_sword 1 0
/give @p minecraft:diamond_pickaxe 1 0
/give @p minecraft:diamond_chestplate 1 0
/give @p minecraft:diamond_helmet 1 0
/give @p minecraft:diamond_leggings 1 0
/give @p minecraft:diamond_boots 1 0
/give @p minecraft:golden_apple 1 1
/give @p minecraft:elytra 1 0
/give @p minecraft:diamond_sword 1 0 {display:{Name:"用于检验自动换行的超长钻石剑名称甲乙丙丁戊己庚辛",Lore:["第一行说明：检查正文与标题间距","Second line: preview and divider"]},ench:[{id:16s,lvl:5s}]}
```

需要装备测试时再取一份胸甲作为背包中的悬停对象，以便角色身上仍穿着完整套装。

## 5. 标题组合与外观

先用木棍，在「工具与武器」范围下不会出现侧边三维模型。暂时关闭「显示物品所属 Mod 名称」和高级信息，并保持 F3+H 的高级提示关闭，检查只有名字的标题。

下表中的开关均在「现代工具提示」中。图标关闭时，即使外框和背景开关仍为开，也应隐藏对应装饰且释放图标槽位。

| 组合 | 标题图标 | 稀有度 | 图标外框 | 图标背景 |
| --- | --- | --- | --- | --- |
| A | 关 | 关 | 任意 | 任意 |
| B | 关 | 开 | 任意 | 任意 |
| C | 开 | 关 | 关 | 关 |
| D | 开 | 关 | 开 | 关 |
| E | 开 | 关 | 关 | 开 |
| F | 开 | 关 | 开 | 开 |
| G | 开 | 开 | 关 | 关 |
| H | 开 | 开 | 开 | 关 |
| I | 开 | 开 | 关 | 开 |
| J | 开 | 开 | 开 | 开 |

每种组合检查：文字与图标合理对齐，关闭元素后没有其专属残留空白；无正文时不应出现多余标题分隔线。有稀有度时，标题与稀有度应作为一块对齐。分别开启和关闭「标题居中」再检查。

随后恢复 Mod 名称等常用信息，用长名称钻石剑检查多行标题、稀有度、正文与 Divider 不重叠。

外观专项：

1. 外框和背景分别单开、同时开启，测试「物品图标圆角」开关及半径 0、3、6。
2. 关闭「外框跟随稀有度」，修改外框颜色和透明度，应显示自定义颜色；重新开启后颜色跟随稀有度。
3. 单独修改图标背景色和透明度，确认不影响整个 Tooltip 背景。
4. 切换中文与英文，检查木棍和附魔金苹果的稀有度标签，无裸翻译键。
5. 关闭稀有度标签后，大 Tooltip 的自适应边框颜色仍可生效；两者是独立配置。

## 6. 三维模型与动画

| 项目 | 操作 | 检查目标 |
| --- | --- | --- |
| 工具、武器 | 悬停钻石剑和镐 20 秒 | 持续旋转，无闪烁或周期性重置 |
| 普通物品 | 范围从「工具与武器」改成「所有物品」，悬停木棍和石头 | 增加侧边模型；原来的标题小图标仍由独立开关控制 |
| 渲染层级 | 把剑和胸甲放在背包中央，周围各槽放其他物品 | 预览不被相邻槽位图标遮盖，不出现透明剪洞 |
| 盔甲架 | 悬停胸甲，切换底座开关，各观察 20 秒 | 装备位置正确，底部和底座连续转动，无抽搐 |
| 玩家模型 | 护甲预览模型从盔甲架切成玩家 | 同一件装备正确穿在玩家模型上 |
| 套装 | 穿上全套装备，切换当前装备 / 完整已装备套装 | 全套来源于角色已装备内容，不凭空生成同材质全套 |
| 玩家姿态 | 测试复制手持物品、蹲姿、idle/swing | 各自生效，不改变实际角色状态 |
| 鞘翅 | 悬停鞘翅 | 使用适合鞘翅的玩家预览 |
| 开关独立性 | 分别关闭物品模型、护甲模型、标题图标 | 仅相应预览消失；剩余文本和预览仍正常 |
| 入场 | 鼠标移开 1 秒再回来；分别关闭 / 开启两种入场动画 | 出现一次入场效果，停留时不反复闪烁 |

当前动画语义：标题二维图标只有入场缩放，不自转。三维工具默认 -20 度/秒，护甲 +20 度/秒，约 18 秒一圈。「模型预览动画」控制入场淡入和缩放；关闭该选项后模型仍会持续旋转。「效果动画速度」也不是模型转速。

生产内置样式不默认绘制胸甲周围的青色装饰框。若出现额外外框，先关闭 F3，再关闭「预览装饰效果」区分调试边界与显式样式。

## 7. F3 与屏幕边缘回归

1. 设置页「实验室 → F3 Tooltip 布局边界」开启并应用。回到世界按 F3，再按 E 打开背包。
2. 依次悬停木棍、长名称剑、胸甲；检查没有卡死，行框、标题框、模型框与实际内容位置一致，说明文字在外部可读。
3. 在背包中快速切换物品，再移出物品区域，检查无残留框、明显卡顿或重复入场。
4. 关闭背包和 F3，再重新悬停；调试框应消失，物品配置中的外框仍按自己的开关显示。
5. 将最大宽度依次改为 80、120、160，用长名称剑检查换行和模型空间。
6. 用 GUI 比例 2、3、自动及文字缩放 0.75、1、1.5，检查屏幕边缘槽位与小窗口下的裁切、面板越界和文字遮挡。
7. 应用配置、正常退出并重新启动，检查开关和颜色持久化。

F3+H 用于原版高级物品信息，不等于 F3 Tooltip 布局开关。外部调试说明在极小窗口可能没有足够空位，也需要记录重叠情况。

## 8. 可选功能回归

- 白名单加入 `minecraft:stick`，在工具范围下应允许它显示模型；再将它加入黑名单，应由黑名单阻止模型。检查后恢复原列表。
- 开启预览音效，首次进入模型预览触发音效；静止悬停不应每帧重复播放。
- 自定义样式和资源包测试可参考 `docs/tooltip-preview-styles.md`。基础验证使用当前内置空样式即可；不需要安装 Obscure 或 LegendaryTooltips。

## 9. 布局路径与验收范围

设置页的大型预览和游戏内工具提示共用 `TooltipLayout` 的测量结果与 `ModernTooltipRenderer.drawContent`。标题、正文、Divider、侧边预览和 F3 调试框都应以同一份几何结果定位；设置页预览会在演示区域不足时对整个面板和内容统一缩放，避免面板被裁切。

自动测试只覆盖可重复的几何、配置和渲染契约。OpenGL 状态恢复、物品栏遮挡、模型旋转、盔甲架底座以及不同 GUI 比例仍需按本指南在客户端人工验收，并记录实际截图或日志。

## 10. 问题记录与文件位置

每个问题记录：物品 ID、是否有长名称或附魔、图标/外框/背景/稀有度开关、模型类型、GUI 比例、文字缩放、是否开启 F3、复现步骤。截图尽量保留完整 Tooltip 和周围背包槽位，旋转或抽搐问题录制 5–10 秒。

| 内容 | 绝对路径 |
| --- | --- |
| 设置 | `D:\Projects\sfr\smoothfont-replacement\run\client\config\neofontrender-ui-enhancements.toml` |
| 用户预览样式 | `D:\Projects\sfr\smoothfont-replacement\run\client\neofontrender\tooltip_preview_styles\` |
| 游戏日志 | `D:\Projects\sfr\smoothfont-replacement\run\client\logs\latest.log` |
| 崩溃报告 | `D:\Projects\sfr\smoothfont-replacement\run\client\crash-reports\` |
| 构建验证日志 | `D:\Projects\sfr\smoothfont-replacement\build\tooltip-client-verification.log` |
| UIE 测试报告 | `D:\Projects\sfr\smoothfont-replacement\addons\ui-enhancements\build\reports\tests\test\index.html` |
| API 测试报告 | `D:\Projects\sfr\smoothfont-replacement\build\reports\tests\test\index.html` |

如果 F3 再次卡死，记录触发时间并保留当次日志；卡死未必生成 crash-report。下一次启动会覆盖 latest.log，先将其复制到单独文件再重启。
