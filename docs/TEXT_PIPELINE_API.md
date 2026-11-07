# Structured text pipeline API

Neo Font Render parses every string into the game-independent `StructuredText`
protocol before a renderer owns it. Minecraft formatting, extension syntax,
inline objects, source mappings, CJK break opportunities, post-processing, and
GL execution therefore share one representation.

## Registering text contributions

Game integrations use `neofontrender.api.text.StructuredTextApi`:

```java
StructuredTextRegistration registration = StructuredTextApi.register(myProviderOrPlugin);
// Later:
registration.close();
```

Use a `TextSyntaxProvider` for control syntax which changes semantic state. A
provider reports a trigger and returns a `SyntaxMatch`; it must not rasterize or
mutate renderer state. Minecraft section-sign codes, Brilliant effect codes,
and the Tinkers PUA protocol use this stage.

Use a `StructuredTextMiddleware` for post-syntax structural transformations.
Middleware receives immutable `StructuredText` and returns either the same
instance or a rewritten value with a valid `SourceMap`. UIE Markdown, LaTeX,
SVG, local images, external-image descriptors, and Gosling aliases use this
stage through a Minecraft-free `TextPipelinePlugin`.

Use `LineBreakOpportunityProvider` for layout opportunities such as CJK
kinsoku handling. Line breaking is not syntax rewriting. Use
`InlineContentResolver` only to resolve deferred inline descriptors, such as a
downloaded image, into a stable renderer-independent ARGB raster.

The optional Typst addon follows the same contract without using UIE or an image
codec. Its `StructuredTextMiddleware` recognizes bounded Typst syntax, submits
CPU work to its asynchronous cache, and publishes the completed result as an
`InlineRaster`. The Minecraft-free `engine:typst-render` JNI module compiles and
rasterizes Typst into straight RGBA; PNG, `BufferedImage`, subprocess pipes, and
OpenGL are not part of that module.

All contribution IDs are namespaced. Priorities are deterministic and IDs
break ties. Call `StructuredTextApi.invalidate()` whenever enabled state,
resolved content, or metrics change; this advances the shared revision and
invalidates route-owned layouts.

## Rendering routes

`TextRenderRouteApi` is the only `FontRenderer` dispatcher. Built-in routes are:

```text
neofontrender:modern_structured
neofontrender:vanilla_compatibility
neofontrender:vanilla_passthrough
```

The modern route sends `StructuredText` to the configured AWT or Cosmic
backend, then through registered `TextPostProcessor` and `TextGlComponent`
stages. The vanilla compatibility route reconstructs vanilla style runs around
the same inline spans. Plain text selected for vanilla remains an explicit,
observable passthrough.

Drawing, measurement, trimming, wrapping, hit testing, and inline bounds are
methods of the selected `TextRenderRouteLayout`; consumers should not parse or
measure the source independently.

## Inline compatibility

`neofontrender.text.InlineContent` is the structured, renderer-independent
descriptor used by the pipeline. Its `InlineLayout` separates logical layout
from raster density: row spans are measured in line-height units, column spans
in em units, automatic width preserves the source aspect ratio, and an optional
maximum width scales automatic-width content down without distortion. The same
layout is consumed by AWT, Cosmic, vanilla compatibility, GL drawing, bounds,
and interaction adapters.

LaTeX and Typst accept the same optional suffix:

```text
$x^2$[rows=2,columns=8,max-width=12em,supersample=4,align=center]
<typst:$ integral_0^1 x^2 dif x $>[height=2lh,width=auto,max-width=20em,supersample=4]
```

`rows`/`height` control logical height, `columns`/`width` control logical width,
`max-columns`/`max-width` constrain it, and `align` is one of `baseline`, `top`,
`center`, or `bottom`. `flow=block` isolates the object on mapped line
boundaries; the default is `flow=inline`. `supersample` (with `oversample` as an alias) controls
only source raster density and never changes layout size. The legacy LaTeX
`scale` option remains an alias for multiplying logical row height.

The older
`neofontrender.api.text.pipeline.InlineContent` remains only as a Minecraft
drawable/interaction adapter for existing UIE menus and clipboard previews. It
does not scan strings, select routes, or own layout.

## Standalone plugins

A plugin JAR exposes `TextPipelinePlugin` with Java `ServiceLoader`. The
standalone laboratory and UIE isolated loader link only the Minecraft-free
plugin module, so Forge and Minecraft classes in the containing mod are not
loaded. The same plugin can therefore be tested with both the AWT and Cosmic
laboratory backends before it is installed in a game.
