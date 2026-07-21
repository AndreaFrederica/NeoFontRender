# Changelog

## [0.3.4]

- Match dropdown menus to the Neo Font Render dark UI, including menu borders and hover states.
- Keep scrollable settings content correctly sized after GUI-scale and window-aspect-ratio changes.
- Improve the Font page split layout at narrow logical resolutions.

## [0.3.3]

- Add configurable shadow masking, focused compatibility fixes, and experimental hex-color chat rendering.
- Expand the font settings UI with reusable setting controls, stable dropdown menus, and scrollable category navigation.
- Improve system-font handling and fallback selection for Cosmic and Skia renderers.

## [0.3.2]

- Fix Cosmic face overrides so resolved regular, bold, italic, and bold italic faces are used during actual layout.
- Allow the font picker to fill every font selector field in the GUI, including fallback and Cosmic override fields.
- Add configurable variable font weight support across Cosmic, Skia, and AWT renderers.

## [0.3.1]

- Improve Cosmic font family and face selection, including regular, bold, italic, and bold italic overrides.
- Add an option for non-regular Cosmic overrides to switch fonts without additionally applying bold or italic styling.
- Support variable-font weight selection and named weight instances.
- Hide installed font style variants from the default family list.
- Ship Windows x86_64/ARM64 and separate GNU/glibc and musl Cosmic natives for Linux x86_64, AArch64, and LoongArch64.

## [0.3.0]

- Allow the Core package to use system fonts and system emoji when optional resource fonts are not installed.
- Add Mod and active rendering-core version information to the F3 diagnostics.
- Publish modular Core, Resources, Skia, and Full distributions under the 0.3.0 release.

All notable changes to Neo Font Render will be documented in this file.

## [0.2.5] - 2026-06-03

### Added
- Added `/neofontrender export` diagnostic command with multi-scenario PNG export and report.txt (GL state, filter decision analysis).
- Added `rendering.forceBlendForText` config option (default: true). MC disables GL_BLEND in some paths (e.g. renderItemOverlayIntoGUI) because the vanilla bitmap font uses 1-bit alpha. Skia and AWT anti-aliased textures have multi-bit alpha edges that need blend for correct compositing. Detailed config comment explains the mismatch.
- Added `PipelineStageTest` offline diagnostic tool for tracing pixel pipeline stages.
- Added `getFontCollection()` accessor on SkijaTextRenderer.

### Fixed
- Fixed GL_NEAREST fallback in `useLinearFiltering()`: removed overly aggressive ratio-based exclusion conditions [5][6] that triggered at screenScale=3, rasterScale=6 (1/ratio=2.0 integer), causing jagged text in normal GUI rendering. Matches SmoothFont behavior.
- Fixed missing GL_BLEND in inventory item counts and other MC code paths that disable blend before drawStringWithShadow. Both SkijaTextRenderer.draw() and BakedGlyph.render() now force blend on when `forceBlendForText=true`.

## [0.2.3] - 2026-05-29

### Added
- Added bundled Sarasa UI SC and Noto Color Emoji as built-in font resources.

### Changed
- Changed the default primary font to the bundled Sarasa UI SC resource.
- Renamed bundled font assets to lowercase resource paths.
- Updated README badges and release metadata for version 0.2.3.

### Fixed
- Fixed the ModularUI dependency declaration to require version 3.1.6+ without creating an FML load-order cycle.

## [0.2.2] - Unreleased

### Fixed
- Fixed the ModularUI config screen crash on newer runtime API versions by making `TextWidget` color calls compatible with both `color(Integer)` and `color(int)` signatures.
- Fixed the in-game mod version metadata so the `@Mod` version now matches the packaged jar version.

## [0.2.1] - Unreleased

### Added
- Added a backend abstraction layer with `TextRenderBackend` and `TextRenderResult` so advanced text renderers no longer couple directly to the FontRenderer mixin surface.
- Added `SkijaRuntimeSupport` to detect Java runtime and embedded native compatibility before enabling the Skija renderer.
- Added multi-platform embedded Skija natives for Windows x64, Linux x64, Linux arm64, macOS x64, and macOS arm64 packaging.
- Added architecture notes for the current text renderer split and Arc3D / ModernUI migration research under `docs/`.

### Changed
- Reorganized the font codebase by actual renderer responsibility into `core.font.backend`, `core.font.awt`, `core.font.skia`, `core.font.support`, and `core.font.layout`.
- Updated `FontManager` to select renderers through the backend abstraction and to fall back to the AWT atlas renderer when Skija is unsupported or initialization fails.
- Updated FontRenderer integration, commands, and GUI test surfaces to consume backend abstractions instead of directly referencing Skija implementation types.
- Kept the layout cache package as a future-facing seam instead of forcing it into the current Skija hot path.
- Changed the default renderer preset to Skia with oversample `12.0`, adaptive raster scale on, interpolation off, mipmap on, enhanced pipeline off, shader compensation off, brightness `0.0`, and texture edge bleed off.

## [0.1.0]

### Added
- Initial project scaffold based on CleanroomMC TemplateDevEnv.
- CoreMod ASM transformer skeleton.
- Mixin configuration placeholder.
- Access Transformer definitions for FontRenderer and GlStateManager.
