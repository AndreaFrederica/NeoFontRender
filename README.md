<p align="center">
  <img src="logo.svg" alt="Neo Font Render" width="200">
</p>

<h1 align="center">Neo Font Render</h1>

<p align="center">
  A font rendering enhancement mod for Minecraft 1.12.2.<br>
  An alternative to SmoothFont with similar features and a modern implementation.
</p>

<p align="center">
  <a href="https://sirrus.cc">Website</a> |
  <a href="https://github.com/AndreaFrederica/NeoFontRender">GitHub</a>
</p>

## Overview

This mod improves Minecraft's font rendering by:
- Replacing bitmap fonts with customizable system/external TrueType fonts
- Adding anti-aliasing and subpixel positioning
- Providing floating-point character metrics
- Applying brightness correction via OpenGL shaders
- Supporting Mipmap and anisotropic filtering

## Development Environment

This project uses **CleanroomMC's TemplateDevEnv**:
- Gradle 8.10.1 + RetroFuturaGradle 2.0.2
- Forge 14.23.5.2847 for Minecraft 1.12.2
- Java 21 toolchain (compiled to Java 8)
- Full CoreMod + Mixin support via MixinBooter

## Building

```bash
./gradlew build
```

## Running

```bash
./gradlew runClient
```

## Architecture

- `asm/` — CoreMod ASM transformers (runtime class patching)
- `client/` — Client-side proxy & rendering hooks
- `common/` — Common proxy & shared logic
- `config/` — Configuration system
- `core/` — Font rasterization, texture generation, metrics
- `util/` — Helpers and utilities
- `mixin/` — Optional Mixin-based patches (reserved for future use)

## Links

- **Website**: https://sirrus.cc
- **Author**: AndreaFrederica

## License

MIT License - see [LICENSE](LICENSE) for details.
