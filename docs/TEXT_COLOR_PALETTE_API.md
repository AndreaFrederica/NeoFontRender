# Text color palette API

Neo Font Render API version 5 exposes the 32 legacy `§0`–`§f` colors used by normal and shadow
passes. Built-in provider IDs are `auto`, `vanilla`, `runtime`, and `custom`. `runtime` snapshots
the final `FontRenderer.colorCode`, including changes made by another mod's mixin.

## Register a provider

```java
NeoFontRenderApi.registerTextColorPaletteProvider(new TextColorPaletteProvider() {
    @Override public String id() { return "example:high_contrast"; }
    @Override public String displayName() { return "High Contrast"; }
    @Override public int priority() { return 200; } // wins automatic selection

    @Override
    public int[] colorCodes(int[] runtimeColorCodes) {
        return new int[] {
            0x000000, 0x0055FF, 0x00D060, 0x00DCDC,
            0xE03030, 0xE050E0, 0xFFB020, 0xD0D0D0,
            0x606060, 0x60A0FF, 0x70F090, 0x70FFFF,
            0xFF7070, 0xFF80D0, 0xFFFF70, 0xFFFFFF
        };
    }
});
```

A provider may return 16 colors; Neo Font Render derives the shadow entries by dividing each RGB
channel by four. Returning 32 colors sets normal and shadow entries explicitly. Call
`NeoFontRenderApi.invalidateTextColorPaletteProviders()` when a dynamic provider changes.

Select and persist a provider on the client thread through the API:

```java
NeoFontRenderApi.selectTextColorPaletteProvider("example:high_contrast");
```

## Configure the built-in custom provider

```java
int[] colors = new int[16];
// fill §0 through §f
NeoFontRenderApi.setCustomTextColorPalette(colors);
NeoFontRenderApi.selectTextColorPaletteProvider("custom");
```

The string overload and settings screen also accept 16 or 32 comma, semicolon, or
whitespace-separated `RRGGBB`, `#RRGGBB`, or `0xRRGGBB` values.
