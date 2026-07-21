<p align="center">
  <img src="logo.svg" alt="Neo Font Render" width="200">
</p>

<h1 align="center">Neo Font Render</h1>

<p align="center">
  <img src="https://img.shields.io/badge/version-0.2.5-blue" alt="Version 0.2.5">
  <img src="https://img.shields.io/badge/modularui-3.1.6%2B-green" alt="ModularUI 3.1.6+">
</p>

<p align="center">
  A modern font rendering enhancement mod for Minecraft 1.12.2.<br>
  It combines a configurable AWT atlas renderer with a Skia paragraph renderer for advanced shaping and emoji.
</p>

<p align="center">
  <a href="README.zh-CN.md">简体中文</a> |
  <a href="https://sirrus.cc">Website</a> |
  <a href="https://github.com/AndreaFrederica/NeoFontRender">GitHub</a>
</p>

## Overview

Neo Font Render replaces the old bitmap font path in Minecraft 1.12.2 with a configurable text stack that is much closer to a modern UI renderer.

Current highlights:

- Custom system fonts and external TTF files with fallback chains
- Built-in font resources, including bundled Sarasa UI SC and Noto Color Emoji
- Skia paragraph rendering for ligatures, kerning, fallback, BiDi, complex scripts, and emoji
- Automatic fallback to the AWT atlas renderer when Skia is unavailable or initialization fails
- Advanced rendering controls for oversample, adaptive raster scale, mipmap, interpolation, shader compensation, and texture edge handling
- IME / non-BMP input fix so surrogate pairs and emoji are no longer truncated by the legacy input path
- Sign editor paste patch with line wrapping across all 4 lines
- In-game config GUI, emoji test screen, hotkeys, and diagnostic commands

<p align="center">
  <img src="docs/screenshot.png" alt="Config GUI" width="800">
</p>

<p align="center">
  <img src="docs/images/sign-paste-demo.png" alt="Sign paste and emoji demo" width="800">
</p>

## Skia Support

Neo Font Render now supports a Skia-based renderer through `SkijaTextRenderer`.

- Skia mode requires Java 9 or newer
- Embedded native packages are shipped for Windows x64, Linux x64, Linux arm64, macOS x64, and macOS arm64
- If the current runtime is incompatible, the mod logs the reason and falls back to the AWT atlas renderer instead of dropping all the way back to vanilla

This means Java 8 users can still run the mod through the legacy AWT path, while Java 9+ users can opt into the full Skia paragraph renderer.

## Current Patches And Features

The current codebase includes more than just font replacement:

- Full-string Skia rendering path in the FontRenderer mixin for advanced shaped text
- AWT glyph atlas fallback with configurable rasterization and filtering
- Unicode IME fix in GUI input handling for emoji and other non-BMP characters
- Ctrl+V paste support in the sign editor with width-aware wrapping
- Configurable built-in fonts, including bundled Sarasa UI SC and emoji resources
- Runtime texture filter overrides for custom font textures
- In-game emoji test screen and font diagnostics command surface

## Controls And Commands

Default client hotkeys:

- `O` opens the main config screen
- `P` opens the emoji test screen

Available commands:

- `/neofontrender info`
- `/neofontrender fonts`
- `/neofontrender reload`
- `/neofontrender test`
- `/neofontrender gui`
## Configuration

The main config file is created at:

- `.minecraft/config/neofontrender.toml`

External font files can be placed in:

- `.minecraft/neofontrender/fonts/`

`font.name` and `font.fallbacks` accept either system font names or TTF file paths. Below is a full sample config aligned with the current default values and first-load comments:

```toml
# Enable/disable the entire font replacement pipeline.
enabled = true

# Font selection and rasterization settings.
[font]
# Primary font name or TTF file path. Comma/semicolon-separated font family lists are also supported.
name = "neofontrender:fonts/sarasa_ui_sc_regular.ttf"
# Fallback font names or TTF file paths queried after font.name when a glyph is missing.
fallbacks = ["Serif", "Monospaced"]
# Font style: 0=Plain, 1=Bold, 2=Italic, 3=Bold+Italic.
style = 0
# Font size in pixels. 8.0 is close to vanilla 1.12 UI text height.
size = 8.5
# Rasterization oversampling factor. Raster resolution is size * oversample.
oversample = 12.0
# Align each font's measured AWT baseline to the Minecraft reference baseline before manual shift.
autoBaseline = true
# Additional vertical glyph shift in Minecraft pixels after automatic baseline alignment. Positive moves glyphs down.
baselineShift = 0.0
# Minecraft-space baseline used by autoBaseline. Vanilla 8px UI text is approximately 7.0.
referenceBaseline = 7.0
# Enable AWT anti-aliasing during glyph rasterization.
antialias = true
# AWT text anti-aliasing mode: off, on, gasp, lcd_hrgb, lcd_hbgr, lcd_vrgb, lcd_vbgr.
antialiasMode = "on"
# Enable fractional font metrics for more precise positioning.
fractionalMetrics = true
# Always append bundled fonts, such as Noto Color Emoji, to the fallback family.
builtinFallbacks = true

[font.cosmic]
# Optional face overrides. Leave empty to derive variants from font.name.
regular = ""
bold = ""
italic = ""
boldItalic = ""

# Text shadow rendering options.
[shadow]
# Shadow offset distance in pixels.
length = 1.0
# Shadow opacity multiplier (0.0-1.0).
opacity = 0.25

# OpenGL texture rendering options.
[rendering]
# Text renderer engine: vanilla, sfr, or skia.
engine = "skia"
# In Skia mode, render full formatted strings as one paragraph so shaping, ligatures, kerning, emoji ZWJ, and BiDi can work across the whole text.
skiaAdvancedStringMode = true
# Use GL_LINEAR texture filtering instead of GL_NEAREST.
interpolation = true
# Enable mipmapping for font textures.
mipmap = true
# Cap SFR/Skia rasterization scale to the current GUI pixel scale and avoid over-downsample blur.
adaptiveRasterScale = true
# Use a dedicated straight-alpha text draw pipeline. Keep this OFF for color emoji.
enhancedTextPipeline = false
# Use a tiny compatible shader to compensate thin anti-aliased glyph edges.
shaderTextPipeline = false
# Text edge compensation strength used by the enhanced shader pipeline.
brightness = 0.0
# Fill fully-transparent Skia text pixels with neighboring RGB to prevent black fringes.
textureEdgeBleed = false

# Performance tuning options.
[performance]
# Initialize font rasterization on a background thread.
asyncInit = true
# Pre-bake common Basic Latin and Latin-1 glyphs before enabling replacement rendering.
prewarmBasicLatin = true

# Input behavior tweaks.
[input]
# Allow Ctrl+V paste in the vanilla sign editor. This is intentionally config-file only.
allowSignPaste = true

# Debug logging options.
[debug]
# Log IME input fix details to game log.
imeInput = false
```

Practical notes:

- `rendering.engine` supports `vanilla`, `sfr`, and `skia`
- `font.builtinFallbacks = true` appends bundled fonts such as Noto Color Emoji to the fallback chain
- Cosmic resolves `MiSans Demibold` as family `MiSans` at weight 600. The main list hides exposed variants such as `MiSans Bold`; use `font.cosmic.*` only for explicit overrides.
- Variable fonts support the `wght` axis and named weight instances; other axes currently use their font defaults.
- `input.allowSignPaste` is intentionally config-file only
- `debug.imeInput = true` can help diagnose IME / emoji input issues

Example: use Skia with a custom primary font and emoji fallback:

```toml
[font]
name = "Microsoft YaHei UI"
fallbacks = [
  "Noto Sans",
  "./neofontrender/fonts/MyExtraFallback.ttf"
]
builtinFallbacks = true

[rendering]
engine = "skia"
adaptiveRasterScale = true
mipmap = true
```

Example: force the legacy renderer for Java 8 or troubleshooting:

```toml
[rendering]
engine = "sfr"

[input]
allowSignPaste = true
```

## Runtime Notes

- Target game: Minecraft 1.12.2 / Forge 14.23.5.2847
- Default renderer preset: Skia with adaptive raster scale enabled
- Build target: Java 8 bytecode
- Skia runtime requirement: Java 9+

### Known Performance Bottleneck: Per-Quad GL Blend Queries

Anti-aliased replacement fonts require `GL_BLEND` to composite correctly, but Minecraft 1.12.2 turns blend off in many code paths (e.g. `Gui.drawRect`). To guarantee correct rendering, `FontRenderPipeline` currently **captures and restores blend state around every single glyph quad** via:

- `glIsEnabled(GL_BLEND)`
- 3× `glGetInteger` for blend func factors
- `enableBlend()` + `tryBlendFuncSeparate()`
- Restore on `close()`

This means **tens to hundreds of synchronous GPU queries per frame** when a lot of text is on screen. We attempted a string-level blend guard (once per `drawString` / `renderStringAtPos`), but Minecraft's GL state is mutated in too many undocumented places (other mods, vanilla GUI code, etc.) for this to be reliable—text ends up corrupted in buttons, books, and other UI elements.

**If you have ideas for a robust string-level or batch-level blend optimization that doesn't break when MC secretly pollutes state, PRs are very welcome.** See `docs/blend-architecture.md` for the current design.

## Architecture

The current text renderer is organized by actual renderer responsibility:

- `src/main/java/neofontrender/core/font/FontManager.java` - renderer selection, lifecycle, and fallback
- `src/main/java/neofontrender/core/font/backend` - renderer-facing backend abstraction
- `src/main/java/neofontrender/core/font/awt` - legacy AWT atlas renderer and glyph model
- `src/main/java/neofontrender/core/font/skia` - Skia renderer, runtime checks, and debug utilities
- `src/main/java/neofontrender/core/font/support` - shared rendering utilities and GL helpers
- `src/main/java/neofontrender/core/font/layout` - future-facing layout seam, not forced into the current Skia hot path

More detailed notes:

- [docs/current-text-layering-report.md](docs/current-text-layering-report.md)
- [docs/arc3d-modernui-analysis.md](docs/arc3d-modernui-analysis.md)

## Development Environment

This project uses CleanroomMC's TemplateDevEnv with RetroFuturaGradle.

- Gradle wrapper + RetroFuturaGradle 2.0.2
- Forge 14.23.5.2847 for Minecraft 1.12.2
- Azul Java 8 toolchain for compilation
- CoreMod + MixinBooter integration

## Building

```bash
./gradlew build
```

`Build and Deploy` produces Cosmic natives for Windows x86_64/ARM64 and for x86_64,
AArch64, and LoongArch64 Linux, with separate GNU/glibc and musl builds.
Universal packages select the matching architecture and libc at runtime.
For a local prebuilt bundle, pass `-PcosmicNativeBundleDir=/path/to/cosmic-natives`.
Linux host-only builds default to GNU; use `-PcosmicLinuxLibc=musl` when building on a musl host.

## Running

```bash
./gradlew runClient
```

## Links

- **Website**: https://sirrus.cc
- **Author**: AndreaFrederica
- **Chinese README**: [README.zh-CN.md](README.zh-CN.md)

## License

MIT License - see [LICENSE](LICENSE) for details.
