# TinkersAntique 字体渲染问题分析

## 根因

**不是 TinkersAntique 自带字体**，而是它的 **自定义颜色编码系统** 与 **Sarasa 字体的 Nerd Font 覆盖** 冲突。

---

## 详细机制

### 1. TinkersAntique 的自定义颜色系统

TinkersAntique 使用 `\uE700-\uE7FF`（Unicode 私有使用区 PUA）作为自定义 RGB 颜色标记：

```java
// CustomFontColor.java:12
protected static int MARKER = 0xE700;

// 每个颜色编码为 3 个字符：\uE7RR \uE7GG \uE7BB
public static String encodeColor(int r, int g, int b) {
    return String.format("%c%c%c",
        ((char) (MARKER + (r & 0xFF))),   // \uE700 + R
        ((char) (MARKER + (g & 0xFF))),   // \uE700 + G
        ((char) (MARKER + (b & 0xFF))));  // \uE700 + B
}
```

这些字符在原版字体中是 **不可见的**（零宽度），仅用于设置后续文字的颜色。

### 2. 原版 CustomFontRenderer 的处理

```java
// CustomFontRenderer.java:95-131
@Override
protected float renderUnicodeChar(char letter, boolean italic) {
    // 识别 \uE700-\uE7FF 范围
    if((int) letter >= CustomFontColor.MARKER && 
       (int) letter <= CustomFontColor.MARKER + 0xFF) {
        // 设置 RGB 颜色，返回 0（不占宽度，不渲染字形）
        this.setColor(r/255f, g/255f, b/255f, 1f);
        return 0;  // ← 关键：零宽度，字符不可见
    }
    return super.renderUnicodeChar(letter, italic);
}
```

### 3. SmoothFont 的拦截

当 SmoothFont 的 text backend 激活时：

```
FontRenderer.drawString("文字\uE7FF\uE780\uE800轻盈")
       ↓ MixinFontRenderer.sfr$onDrawString 拦截
       ↓ 取消原版渲染
       ↓ 调用 backend.renderFormatted()
       ↓
TextRenderBackend 不认识 \uE700-\uE7FF
       ↓ 将这些字符当作普通文字
       ↓ 调用 TrueType 字体渲染
       ↓
Sarasa 字体在 \uE700-\uE7FF 范围有 Nerd Font 图标
       ↓
渲染出 ⚓ 🪶 𝗳 等图标！
```

### 4. 冲突来源

| 组件 | 作用 |
|---|---|
| `CustomFontColor.MARKER = 0xE700` | 使用 U+E700-E7FF 作为颜色标记 |
| Sarasa 字体 | 包含 Nerd Font，在 U+E700-E7FF 有图标字形 |
| SmoothFont text backend | 不认识这些标记，当作普通字符渲染 |

截图中出现的图标：
- ⚓ (U+2693) — 可能是某个 modifier 的颜色标记恰好映射到此
- 🪶 (U+1FAB6) — 羽毛/翅膀图标
- 这些都是 Sarasa 字体在 PUA 区域的字形

---

## 影响范围

所有使用 `CustomFontColor.encodeColor()` 的地方都会受影响：

| 用途 | 文件 |
|---|---|
| Modifier 颜色显示 | `ModifierNBT.getColorString()` |
| 材料文字颜色 | `Material.getLocalizedName()` |
| 耐久/攻击/速度数值颜色 | `HeadMaterialStats` 各常量 |
| 手柄系数颜色 | `HandleMaterialStats.COLOR_Modifier` |
| 弓箭属性颜色 | `BowMaterialStats` 各常量 |
| 书本内容 | `ContentModifier` |

** tooltip 中每一行 modifier 都会先调用 `data.getColorString()`，所以每个 modifier 名前都会有 3 个 `\uE7XX` 字符。**

---

## 解决方案

### 方案 A：渲染前转换成现代多颜色文本（采用）

PUA 协议不应散落到 `MixinFontRenderer`、Skia、Cosmic、SFR 和 vanilla 的各个绘制与测量
循环里。NeoFontRender 在统一的 raw-text 预处理管线中识别协议：

1. `TinkersAntiqueTextPreprocessor` 解码每三个 PUA 字符，并从可见文本中移除控制字符。
2. 预处理结果保留 raw 字符串到可见字符串的边界映射，供裁剪和换行返回正确的 raw 索引。
3. 解码结果转换成 `ModernText` 多颜色 run。
4. `ModernTextApi` 在 API 层组合各 run 的布局；Cosmic、Skia 直接处理，SFR 和 vanilla
   通过现代 AWT 适配器处理。

这样颜色切换、绘制、宽度测量、裁剪和换行共用同一个中间表示，PUA 字符不会进入任何
字体后端，也不会因 Nerd Font 覆盖而显示成图标。

### 方案 B：使用不含 Nerd Font 的 Sarasa 版本

使用 `Sarasa Goshikku`（不含 Nerd Font）或 `Sarasa Mono SC` 而非 `Sarasa Term SC Nerd`。

### 方案 C：TinkersAntique 侧修改

将颜色标记改到 SmoothFont 不会渲染的范围，或者改用 § 格式码系统。但这需要改 mod 源码，不现实。

---

## 建议

**方案 A 是最合理的**。字体替换 mod 应在字体后端之前识别常见的 PUA 颜色协议，而不是
要求每个后端分别理解某个旧 mod 的私有编码。

需要检测的 PUA 范围：
- `U+E700-U+E7FF` — TinkersAntique 颜色标记
- 可能还有其他 mod 使用的 PUA 范围

兼容开关为 `compat.tinkersantique.enabled`，默认开启。
