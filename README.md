<p align="center">
  <img src="logo.svg" alt="Neo Font Render" width="200">
</p>

<h1 align="center">Neo Font Render</h1>

<p align="center">
  Modern text shaping and font rendering for Minecraft 1.12.2 on Cleanroom.<br>
  <a href="README.zh-CN.md">简体中文</a> · <a href="https://github.com/AndreaFrederica/NeoFontRender">GitHub</a>
</p>

## What it does

Neo Font Render replaces Minecraft 1.12.2's bitmap-font path with configurable modern renderers.

- Cosmic Text is the default renderer, with native text shaping and rasterization.
- Skia provides an optional paragraph renderer for complex shaping, ligatures, BiDi, and color emoji.
- The built-in SFR/AWT renderer remains available for compatibility and troubleshooting.
- System fonts, local TTF/OTF files, bundled Sarasa UI SC, Noto Color Emoji, and fallback chains are supported.
- Includes Unicode/IME input fixes, sign-editor paste and wrapping, configurable sign optimizations, an in-game settings screen, and diagnostic commands.

<p align="center">
  <img src="docs/screenshot.png" alt="Neo Font Render configuration screen" width="800">
</p>

## Requirements and installation

- Minecraft 1.12.2 with Cleanroom.
- Java 25.
- [ModularUI 3.1.6+](https://github.com/CleanroomMC/ModularUI).

Download the distribution that fits your installation and put it in the `mods` folder. The `full` package is the usual choice.

| File | Use it when |
| --- | --- |
| `neofontrender-<version>-full.jar` | You want the complete, all-in-one installation. |
| `neofontrender-<version>-core.jar` | You want the small core renderer and system fonts. |
| `neofontrender-resources-<version>.jar` | You use `core` and also want bundled font resources. |
| `neofontrender-skia-<version>.jar` | You use `core` and want the optional Skija runtime. |

Do not install `full` together with the split `core`, `resources`, or `skia` packages.

## Getting started

Open the configuration screen with `O`; `P` opens the emoji test screen. The main configuration file is:

```text
.minecraft/config/neofontrender.toml
```

Place custom font files in:

```text
.minecraft/neofontrender/fonts/
```

New installations use these rendering defaults:

```toml
[font]
size = 8.5

[rendering]
engine = "cosmic"
interpolation = true
skiaGpuOffscreen = true
skiaGpuSubmitViaCpuTexture = false
```

Cosmic and Skia can fall back to configured system fonts and bundled resources when a glyph is missing. If a renderer is unavailable, select `sfr` in the settings screen to use the compatibility renderer.

Useful commands:

```text
/neofontrender info
/neofontrender fonts
/neofontrender reload
/neofontrender gui
```

## Integration API

Other client mods can update the active font without touching Neo Font Render's internal config or
renderer classes. `apply()` is safe to call from any thread: it schedules the update on the client
thread, saves it by default, and reloads the font backend once.

```java
import neofontrender.api.FontStyle;
import neofontrender.api.NeoFontRenderApi;
import neofontrender.api.RenderingEngine;

NeoFontRenderApi.updateFont()
        .font("Sarasa UI SC")
        .fallbackFonts("Noto Color Emoji", "SansSerif")
        .size(8.5F)
        .style(FontStyle.PLAIN)
        .engine(RenderingEngine.COSMIC)
        .apply();
```

Use `.persist(false)` for a session-only change. `NeoFontRenderApi.getFontState()` exposes an
immutable snapshot of the configured font and active backend. Mods with an optional dependency
should check `Loader.isModLoaded("neofontrender")` before referencing the API. Reusable GUI controls
intended for dependent mods are available under `neofontrender.client.gui.component.base`.

`font(...)` clears Cosmic's per-style face overrides so the selected family applies consistently
across backends. Use `primaryFont(...)` together with `cosmicFaceOverrides(...)` when a mod needs
separate regular, bold, italic, and bold-italic font files.

## Development

The project uses the current [CleanroomModTemplate](https://github.com/CleanroomMC/CleanroomModTemplate): Gradle 9.6, Unimined, Cleanroom Loader, and a Java 25 toolchain.

```bash
./gradlew runClient
./gradlew build
./gradlew packageVariants
```

`packageVariants` creates the full, core, resources, and Skia distribution jars in `build/libs`. The local build compiles the Cosmic JNI library with Cargo; CI assembles a multi-platform native bundle.

## Project

- License: [MIT](LICENSE)
- Contributors: [AndreaFrederica](https://github.com/AndreaFrederica), [baka-gourd](https://github.com/baka-gourd), [DHJComical](https://github.com/DHJComical)
- Design notes: [docs](docs/)
