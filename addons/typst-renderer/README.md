# NFR Typst Renderer

This is an optional client-only submod for Revo UI. It recognizes bounded
`<typst:...></typst>` tokens (the short `<typst:...>` form is also accepted) and renders the complete Typst language through the pinned
Typst compiler and raster renderer.

The Minecraft-free `engine:typst-render` module owns a persistent Typst compiler
through a coarse-grained JNI API. Typst rasterizes directly into a tiny-skia
pixmap and returns straight RGBA bytes; there is no subprocess, pipe, Base64,
PNG encoder, or Java image decoder in the render path. The addon converts the
RGBA payload into the standard `InlineRaster` protocol and registers its
middleware with NeoFontRender's structured-text pipeline.

Typst uses the shared inline layout suffix. For example:

```text
<typst:$ sum_(i=1)^n i = (n(n+1))/2 $>[rows=2,width=auto,max-width=18em,supersample=4]
```

`rows` (or `height=2lh`) controls displayed line height, `columns` (or
`width=12em`) requests an explicit width, and `max-width` constrains automatic
aspect-ratio sizing. `supersample` controls only the native raster density and
is clamped to `0.25-16`; `oversample` is accepted as an alias.

## Bundled native engine

The Typst source is pinned as `vendor/typst` in `.gitmodules`. With Rust 1.92 or
newer installed, the normal addon build compiles the JNI library and packages
the current platform binary and Java engine classes inside the addon JAR:

```text
gradlew.bat :addons:typst-renderer:build
```

On first use, the engine extracts the content-addressed native library into the
system temporary directory and validates its ABI before creating a native
engine handle. Enable the addon with `enabled = true`.

## Library and package storage

The addon derives its data directory from the core font directory. If fonts are
stored in `gameDir/neofontrender/fonts`, Typst uses
`gameDir/neofontrender/typst`, with automatically downloaded Typst Universe
packages under `gameDir/neofontrender/typst/packages`. It does not use the
user-wide Typst cache. The directory is bound to the native engine when it is
created.

## Package management and status

NFR settings includes a **Typst Packages** page. It lists cached package versions
and total disk usage, installs an exact `@preview/name:version`, deletes a selected
version after confirmation, and retries failed renders/downloads. Package operations
take effect immediately; Apply/Cancel controls the HUD preference. Imports can
download a deleted version again when rendered.

The default-enabled **Download status HUD** works in GUI screens and in-game. It
reports connection, downloaded bytes (and a percentage when the server supplies a
size), extraction, installation, and rendering success/failure. Successful notices
expire after eight seconds. Failures remain until dismissed or superseded by a
retry; details are retained on the package page, can be copied, and are logged.
HUD polling never waits for the native compiler or downloader. Cache operations
are serialized with rendering, and the original render failure is retained in the
inline content's `error` attribute.

Each built addon JAR targets its build host's operating system and architecture.
Only the JNI raster engine is bundled; unused Typst CLI features, file watchers,
exporters, and an HTTP server are not shipped. The package loader and HTTPS
downloader are the only optional `typst-kit` capabilities enabled.
