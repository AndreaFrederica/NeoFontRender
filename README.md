<p align="center">
  <img src="logo.svg" alt="Neo Font Render" width="200">
</p>

<h1 align="center">Neo Font Render</h1>

<p align="center">
  Modern text shaping and font rendering for Minecraft 1.7.10 in an lwjgl3ify runtime.<br>
  <a href="README.zh-CN.md">简体中文</a> · <a href="https://github.com/AndreaFrederica/NeoFontRender">GitHub</a>
</p>

## What it does

Neo Font Render replaces Minecraft 1.7.10's bitmap-font path with configurable modern renderers.

- Cosmic Text is the default renderer, with native text shaping and rasterization.
- The built-in SFR/AWT renderer remains available for compatibility and troubleshooting.
- Vanilla rendering can be restored without disabling the mod's input and compatibility fixes.
- System fonts, local TTF/OTF files, bundled Noto Sans SC, Noto Color Emoji, and fallback chains are supported.
- Includes Unicode/IME input fixes, sign-editor paste and wrapping, configurable sign optimizations, an in-game settings screen, and diagnostic commands.

<p align="center">
  <img src="docs/screenshot.png" alt="Neo Font Render configuration screen" width="800">
</p>

## Requirements and installation

- Minecraft 1.7.10 with lwjgl3ify.
- Java 25.
- [ModularUI2 2.3.81+](https://github.com/GTNewHorizons/ModularUI2).

Download the distribution that fits your installation and put it in the `mods` folder. The `full` package is the usual choice.

| File | Use it when |
| --- | --- |
| `neofontrender-<version>-full.jar` | You want the complete, all-in-one installation. |
| `neofontrender-<version>-core.jar` | You want the small core renderer and system fonts. |
| `neofontrender-resources-<version>.jar` | You use `core` and also want bundled font resources. |

Do not install `full` together with the split `core` or `resources` packages.

### Optional UI Enhancements addon

[NeoFontRender UI Enhancements](addons/ui-enhancements/README.md) is the optional companion mod for
modern tooltips, configurable HUD bars, enhanced chat, smooth scrolling, text-field cursors, and
screen transitions. It keeps its own configuration file while embedding its settings pages in the
NeoFontRender settings screen.

- [Addon documentation and build instructions](addons/ui-enhancements/README.md)
- [Download UI Enhancements releases](https://github.com/AndreaFrederica/NeoFontRender/releases?q=ui-enhancements)

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
name = "Noto Sans SC"
path = "neofontrender:fonts/noto_sans_sc-regular.otf"
fallbacks = ["Serif", "Monospaced"]
size = 8.5

[rendering]
engine = "cosmic"
interpolation = true
advancedStringMode = true
```

`rendering.engine` accepts `cosmic`, `sfr`, or `vanilla`. Cosmic can fall back to configured system
fonts and bundled resources when a glyph is missing. Select `sfr` for the AWT compatibility path or
`vanilla` to restore Minecraft's renderer.

### Compatibility

- ModularUI2 supplies the settings UI; Neo Font Render also handles Unicode editing in its text inputs.
- Optional Tinkers' Construct and Mantle integration renders the 1.7.10 manual with the selected font while preserving Mantle's measurement and wrapping. It is enabled by `compat.tinkersconstruct.enabled`.
- The UI Enhancements addon detects NEI when installed and applies its tooltip integration without making NEI a required dependency.

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
        .font("Noto Sans SC")
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

The build uses RetroFuturaGradle and the GTNH Gradle conventions on Gradle 9.3.1 with a Java 25 toolchain.

```bash
./gradlew runClient25
./gradlew build
./gradlew packageVariants
```

`packageVariants` creates the full, core, and resources distribution jars in `build/libs`. The local build compiles the Cosmic JNI library with Cargo; CI assembles the Windows, Linux, and macOS native bundle used by both full and core packages.

## Project

- License: [MIT](LICENSE)
- Contributors: [AndreaFrederica](https://github.com/AndreaFrederica), [baka-gourd](https://github.com/baka-gourd), [DHJComical](https://github.com/DHJComical)
- Design notes: [docs](docs/)
