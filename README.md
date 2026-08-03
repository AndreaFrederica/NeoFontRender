<p align="center">
  <img src="logo.svg" alt="Neo Font Render" width="200">
</p>

<h1 align="center">Neo Font Render</h1>

<p align="center">
  Modern text shaping and font rendering for Minecraft 1.12.2 on Cleanroom.<br>
  <a href="README.zh-CN.md">简体中文</a> · <a href="https://github.com/AndreaFrederica/NeoFontRender">GitHub</a><br><br>
  <a href="https://www.curseforge.com/minecraft/mc-mods/neofontrender"><img src="https://img.shields.io/badge/CurseForge-NFR-orange" alt="CurseForge"></a>
  <a href="https://www.curseforge.com/minecraft/mc-mods/neo-font-render-ui-enhancements"><img src="https://img.shields.io/badge/CurseForge-UI%20Enhancements-orange" alt="CurseForge UIE"></a>
  <a href="https://www.mcmod.cn/class/27362.html"><img src="https://img.shields.io/badge/MCMOD-Neo%20Font%20Render-blue" alt="MCMOD"></a>
</p>

## What it does

Neo Font Render replaces Minecraft 1.12.2's bitmap-font path with configurable modern renderers.

- **Cosmic Text** — default renderer with native text shaping, ligatures, kerning, BiDi, and emoji ZWJ sequences.
- **SFR/AWT** — built-in Java2D AWT compatibility renderer for troubleshooting.
- System fonts, local TTF/OTF/TTC files, bundled Noto Sans SC, Noto Color Emoji, and fallback chains.
- Variable font weight axis support, per-style Cosmic face overrides (regular/bold/italic).
- Adaptive raster scale (1.5x–14x), mipmapping, anisotropic filtering, and GL interpolation.
- Modern single-pass shadow with configurable blur radius, offset, opacity, and color.
- Classic shadow with mode control (all/mask/emoji/none) and shadow masking rules.
- Advanced string mode for full-span shaped-text rendering across the entire formatted string.
- Enhanced and shader text pipelines for improved anti-aliased edge quality.
- Brightness compensation with auto-detection from sample glyph rasterization.
- Segment cache for efficient partial-text rendering without advanced string mode.
- Unicode/IME input fixes, CJK line-break rules, text undo/redo.
- Sign-editor paste (Ctrl+V) with multi-line wrapping and configurable sign optimizations (LOD, frustum culling, occlusion culling).
- TinkersAntique PUA marker compatibility and enchantment table font replacement.
- Forge loading screen and ModernSplash font override.
- In-game modular tabbed settings screen (12 tabs) with extension API for other mods.
- F3 debug overlay with engine, cache, and sign occlusion stats.
- Emoji test diagnostic screen, `/neofontrender` command suite.

<p align="center">
  <img src="docs/screenshot-font.png" alt="Font settings" width="400">&nbsp;&nbsp;
  <img src="docs/screenshot-general.png" alt="General settings" width="400"><br>
  <img src="docs/screenshot-shadow.png" alt="Shadow settings" width="400">&nbsp;&nbsp;
  <img src="docs/screenshot-tooltips.png" alt="Modern Tooltips settings" width="400">
</p>

## Modules

The project ships as a main mod and several optional modules. All UIE modules share a single settings screen.

> **License note:** The main mod is MIT. UI Enhancements source code is MIT, but the distributed
> JAR links against LGPL-3.0 libraries (Arc3D Core, ModularUI) and embeds LGPL-2.1 (Jazzy) and
> Apache-2.0 (TabbyChat, Salutation, jieba-analysis) components, so the combined work is
> effectively LGPL-3.0. See [NOTICE.md](addons/ui-enhancements/NOTICE.md) and the bundled
> `META-INF/LICENSE-*` files for full details.

<table>
<thead>
<tr>
  <th></th>
  <th>Module</th>
  <th>Mod ID</th>
  <th>License</th>
  <th>Description</th>
</tr>
</thead>
<tbody>
<tr>
  <td><img src="src/main/resources/assets/neofontrender/logo.png" width="32"></td>
  <td><b>Neo Font Render</b></td>
  <td><code>neofontrender</code></td>
  <td>MIT</td>
  <td>Core font renderer with Cosmic Text and SFR/AWT engines, system/bundled font support, Unicode/IME fixes, sign optimizations, and the modular settings screen.</td>
</tr>
<tr>
  <td><img src="addons/ui-enhancements/src/main/resources/assets/neofontrender_ui_enhancements/logo.png" width="32"></td>
  <td><b>NFR UI Enhancements</b></td>
  <td><code>neofontrender_ui_enhancements</code></td>
  <td>LGPL-3.0</td>
  <td>Visual and interactive addons: chat (TabbyChat + Salutation embedded), tooltips, HUD bars, smooth scrolling, text input, screen effects, hover animation, world loading, zoom.</td>
</tr>
<tr>
  <td><img src="addons/ui-enhancements/src/main/resources/assets/neofontrender_ui_enhancements/logo.png" width="32"></td>
  <td><b>UIE Server Companion</b></td>
  <td><code>neofontrender_ui_enhancements_server</code></td>
  <td>MIT</td>
  <td>Server-side self-message network support for the chat module. Optional, only needed on dedicated servers.</td>
</tr>
</tbody>
</table>

### Embedded mods (bundled inside UIE)

<table>
<thead>
<tr>
  <th>Mod</th>
  <th>Mod ID</th>
  <th>License</th>
  <th>Description</th>
</tr>
</thead>
<tbody>
<tr>
  <td><b>TabbyChat 2 Reforged</b></td>
  <td><code>tabbychat2</code></td>
  <td>Apache-2.0</td>
  <td>Chat channel tabs, filters, anti-spam, per-channel history and logging. Standalone JAR not required.</td>
</tr>
<tr>
  <td><b>Salutation 1.12.2</b></td>
  <td><code>salutation</code></td>
  <td>Apache-2.0</td>
  <td>Command tree, argument parsers, multiline chat backend, sleep-chat screen, and advanced tab completion. Standalone JAR not required.</td>
</tr>
</tbody>
</table>

### UI Enhancements feature modules

| Module | Description |
| --- | --- |
| **Modern Tooltips** | Full tooltip replacement with rounded corners, soft shadows, adaptive border colors (rarity/enchantment-based), Mica translucent backdrop, center-titled layout, mod name display. Supports HEI, Obscure Tooltips, and Quark map compat. |
| **Smooth Scrolling** | Animated wheel scrolling for vanilla `GuiSlot` lists, Forge scrolling lists, creative inventory grid, and chat history. Configurable duration and step size. |
| **Screen Effects** | Background Gaussian blur (post-processing shader), four-corner color gradient overlay, and fade-in/fade-out transitions. Per-screen-type control for menus, containers, and chat. |
| **HUD Bars** | Modern replacement for vanilla health, food, armor, toughness, air, and mount bars. 6 visual themes (modern, flat, glass, segmented, minimal, classic), smooth animated fill, per-stat color customization, numeric/icon display. AppleCore integration. |
| **Enhanced Chat** | See sub-features below. |
| **Zoom** | Hold-to-zoom keybind with configurable magnification (2–8x), smooth camera, mouse sensitivity adjustment, and animated FOV transitions. |
| **Hover Effects** | Smooth cross-fade animations for vanilla/Forge buttons, inventory slots, JEI/HEI ingredient grids, and ModularUI slots. |
| **World Loading** | Modern loading overlay for world join and dimension change. Progress bar, percentage, spinner, bottom gradient shade, fade-out transition, last-exit frame snapshot. |
| **Resource Reload** | Progress overlay during resource pack and language reload with progress bar, percentage, and spinner. |
| **Main Menu** | "Continue Game" button to directly rejoin the last played world or server. |
| **Create World** | Three layout themes for the create-world screen: vanilla, tabbed, and modernui. |
| **Text Input** | OS-style I-beam cursor over vanilla and ModularUI text fields. |

### Chat sub-features (inside UIE)

| Feature | Description |
| --- | --- |
| **Tabbed chat** | Embedded TabbyChat 2 with channel tabs, per-channel history and logging, anti-spam, timestamps, spelling check, unread flashing. |
| **Chat search** | Full-text search across chat history with `Ctrl+F`. |
| **Source classification** | Regex-based message routing into Player/Server/Private channels. |
| **Message filtering** | Regex-based message blocking and per-player muting. |
| **@mention completion** | Live `@player` suggestions from online player list with notification sound. |
| **Command completion** | Scrollable tab-completion suggestions while typing. |
| **Player links** | Clickable player names with double-click interaction, avatar tooltips, and context menu (private message, whisper, copy name, mute). |
| **Player heads** | Renders cached player head avatars next to chat messages. |
| **Item icons** | Displays item icons beside `SHOW_ITEM` chat components. |
| **Timestamps** | Configurable timestamps prepended to chat messages. |
| **Copy & paste** | Drag-to-select text copying with formatting code options, `Ctrl+C/X/V/A` keybindings. |
| **Right-click menu** | Context menu on chat messages for copy, player actions. |
| **Keep-open policies** | Per-source-type control for keeping chat open after sending. |
| **HUD window** | Floating chat window with compositor support for persistent/detached layout. |
| **Persistence** | Per-server/world persistent received and sent message history (JSON). |
| **Message animations** | Entrance animations for new messages and chat input open/close. |
| **Color theme** | Full color theme for the chat panel (background, border, input, tray, tabs, scrollbar, text — 11 color slots). |
| **Spellcheck** | Jazzy (English) + Jieba (Chinese) spell checking. |

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

### UI Enhancements

Install `neofontrender-ui-enhancements-<version>.jar` alongside the main mod. The server companion is optional.

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
- Design notes: [docs](docs/)
