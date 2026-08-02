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

- **Cosmic Text** — default renderer with native text shaping and rasterization.
- **SFR/AWT** — built-in compatibility renderer for troubleshooting.
- System fonts, local TTF/OTF files, bundled Noto Sans SC, Noto Color Emoji, and fallback chains.
- Unicode/IME input fixes, sign-editor paste and wrapping, configurable sign optimizations.
- In-game settings screen with modular tabbed UI and diagnostic commands.

<p align="center">
  <img src="docs/screenshot.png" alt="Neo Font Render configuration screen" width="800">
</p>

## Supported versions

| Minecraft | Branch | Primary maintainer | Runtime |
| --- | --- | --- | --- |
| 1.12.2 | [`main`](https://github.com/AndreaFrederica/NeoFontRender) | [AndreaFrederica](https://github.com/AndreaFrederica) | Cleanroom + Java 25 |
| 1.7.10 | [`1.7.10`](https://github.com/AndreaFrederica/NeoFontRender/tree/1.7.10) | [DHJComical](https://github.com/DHJComical) | Forge + lwjgl3ify |

The 1.7.10 port shares the core rendering engine and API surface but targets Forge with lwjgl3ify
instead of Cleanroom. See the [`1.7.10` branch](https://github.com/AndreaFrederica/NeoFontRender/tree/1.7.10)
for version-specific documentation and releases.

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

Do not install `full` together with the split `core` or `resources` packages.

## UI Enhancements addon

[NFR UI Enhancements](addons/ui-enhancements/README.md) is the optional companion mod that adds visual and interactive features beyond the font-rendering core. It embeds its settings pages directly into the NFR settings screen.

### Feature modules

| Module | Description |
| --- | --- |
| **Chat** | Enhanced chat with TabbyChat 2 Reforged channels/filters, Salutation command system, search, @mention completion, player links, message source classification, rules engine, HUD window, persistent per-server history, and Jieba Chinese spellcheck. Both TabbyChat and Salutation are embedded — standalone JARs are not required. |
| **Tooltips** | Modern tooltip layout, shading, rarity colors, and LegendaryTooltips interop. |
| **HUD bars** | Arc3D-powered health, absorption, armor, toughness, hunger, saturation, exhaustion, air, and mount-health bars with Forge height-stack coordination. |
| **Scrolling** | Smooth wheel scrolling for vanilla `GuiSlot` and Forge `GuiScrollingList`. |
| **Text input** | Native GLFW I-beam cursors over vanilla and ModularUI text fields. |
| **Screen effects** | Background fade, four-corner gradient, and configurable two-pass Gaussian blur. |
| **Hover** | Smooth hover animation for buttons and slots. |
| **World loading** | Customizable loading screen with per-save screenshot and spawn-preparation progress. |
| **Zoom** | Hold-to-zoom with configurable key and sensitivity. |

### Server companion

[ui-enhancements-server](addons/ui-enhancements-server/) provides server-side self-message network support for the chat module. It is optional and only needed on dedicated servers.

### Download

- [UI Enhancements releases](https://github.com/AndreaFrederica/NeoFontRender/releases?q=ui-enhancements)
- [Addon documentation and build instructions](addons/ui-enhancements/README.md)

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
advancedStringMode = true
```

Cosmic can fall back to configured system fonts and bundled resources when a glyph is missing. If it is unavailable, select `sfr` in the settings screen to use the compatibility renderer.

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

The project uses the current [CleanroomModTemplate](https://github.com/CleanroomMC/CleanroomModTemplate): Gradle 9.6, Unimined, Cleanroom Loader, and a Java 25 toolchain.

```bash
./gradlew runClient
./gradlew build
./gradlew packageVariants
```

`packageVariants` creates the full, core, and resources distribution jars in `build/libs`. The local build compiles the Cosmic JNI library with Cargo; CI assembles a multi-platform native bundle.

## Project

- License: [MIT](LICENSE)
- Contributors: [AndreaFrederica](https://github.com/AndreaFrederica), [baka-gourd](https://github.com/baka-gourd), [DHJComical](https://github.com/DHJComical)
- CurseForge: [Neo Font Render](https://www.curseforge.com/minecraft/mc-mods/neofontrender) · [UI Enhancements](https://www.curseforge.com/minecraft/mc-mods/neo-font-render-ui-enhancements)
- MCMOD: [Neo Font Render](https://www.mcmod.cn/class/27362.html)
- Design notes: [docs](docs/)
