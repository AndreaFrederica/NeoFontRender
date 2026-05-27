# Neo Font Render

A font rendering enhancement mod for Minecraft 1.12.2.
A complete reimplementation of SmoothFont, built from the ground up.

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

## Credits

- **Author**: AndreaFrederica

## License

MIT License - see [LICENSE](LICENSE) for details.
