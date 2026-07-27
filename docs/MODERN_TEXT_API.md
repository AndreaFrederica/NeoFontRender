# Modern text API

NeoFontRender exposes an engine-independent client API from the main mod:

```java
import neofontrender.api.text.ModernTextApi;
import neofontrender.api.text.ModernTextLayout;

ModernTextLayout title = ModernTextApi.layout(
        "Loading world", 24.0F, 0xFFFFFFFF);
title.draw(16.0F, 32.0F);

float width = ModernTextApi.measure("Loading world", 24.0F);
ModernTextApi.draw("Loading world", 16.0F, 32.0F, 24.0F, 0xFFFFFFFF);
```

Arbitrary RGB runs use `ModernText`. Run colors replace the base RGB while retaining the alpha
passed to the API:

```java
ModernText colored = ModernText.builder()
        .append("Durability: ")
        .append("120", 0x78A0CD)
        .append("/200", 0xB9B95A)
        .build();

ModernTextApi.drawFormatted(
        colored, 16.0F, 32.0F, 12.0F, 0xFFFFFFFF, false);
```

`fontSize` is a real logical size in Minecraft GUI units. It does not enlarge the normal UI-size
glyph texture with a model-view transform. NeoFontRender automatically dispatches to Cosmic,
Skia, or its native-size AWT atlas adapter, including when the configured renderer is SFR or
vanilla.

Use `layoutFormatted`, `measureFormatted`, and `drawFormatted` for Minecraft section-sign color
and style codes. The `shadow` argument applies Minecraft shadow-pass colors; callers that want a
complete foreground plus shadow should submit both passes at their desired offsets.

For NeoFontRender's configured blurred, single-layout shadow, use
`layoutFormattedWithShadow` or `drawFormattedWithShadow`. This is supported by the Cosmic and
Skia backends at arbitrary logical font sizes. Check `canRenderModernShadow(text)` when a fallback
is needed; SFR/vanilla's AWT adapter and native color-glyph runs currently use the normal
two-pass fallback.

Raw-string overloads pass through NeoFontRender's compatibility preprocessing pipeline before
layout. When TinkersAntique compatibility is enabled, three-character `U+E700-U+E7FF` RGB
sequences become colored `ModernText` runs and never reach a font as visible PUA glyphs.

All API calls must run on Minecraft's client render thread. A `ModernTextLayout` is cache-owned:
do not close it, and obtain a new layout after a resource reload or font-setting change.
