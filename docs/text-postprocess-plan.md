# Structured Text Rendering Pipeline Plan

## Status and scope

This document supersedes the earlier post-processing-only plan. The registry,
post-process context, modern shadow processor, Brilliant metadata bridge, and
initial GL component boundary already exist, but they are not the final
architecture described here.

The approved target is one structured protocol with two registered execution
routes: the modern AWT/Cosmic route and a vanilla `FontRenderer` compatibility
route. Minecraft formatting semantics are parsed once by the shared syntax
engine. Neither route owns a second parser, and the modern route must not call
vanilla `FontRenderer` to obtain formatting effects.

Two requirements are mandatory:

1. Syntax is parsed once into a shared structured-text protocol. Render
   backends, measurement, wrapping, trimming, and post-processors must not carry
   independent legacy-format parsers.
2. The complete syntax, layout, raster, post-process, and GL execution path must
   be testable outside Minecraft. A standalone desktop test laboratory is a
   deliverable, not an optional debugging utility.

## Implementation snapshot (2026-09-04)

Implemented in the current tree:

- `engine:text-core` owns `StructuredText`, `SourceMap`, Minecraft/Brilliant
  syntax providers, structured slicing, CJK break opportunities, and the shared
  animation-frame scope.
- Modern AWT and Cosmic drawing, measuring, trimming, and line fitting consume
  the structured protocol. The vanilla compatibility adapter consumes the same
  `StructuredText` for contributed syntax and inline objects; ordinary vanilla
  text remains an explicit passthrough route.
- Minecraft color/style/reset semantics, dynamic obfuscation, decorations,
  Brilliant effect spans, and modern shadow composition execute without asking
  vanilla `FontRenderer` to render an effect.
- Brilliant is mounted as a `TextGlComponent`; it restores GL state, reports
  shader/FBO fallback diagnostics, and composites effect and passthrough regions
  independently.
- The in-game workflow panel expands registered syntax, middleware,
  post-process, and GL components and includes the most recent actual hit/apply
  state.
- `tools:text-render-lab` runs the production core protocol without Minecraft,
  provides reusable headless AWT and production Cosmic JNI raster adapters,
  PNG/JSON export, frame stepping, and a `verifyGolden` animation/pixel task.
  Its platform-specific executable JAR contains `text-core`, the test fonts,
  and the Cosmic native library, and verifies itself through `java -jar`.
- `addons:ui-enhancements-text` is the Minecraft-free UIE plugin loaded through
  `TextPipelinePlugin`. Markdown, LaTeX, SVG, local images, external-image
  descriptors, Gosling aliases, and the UIE CJK break provider now contribute
  directly to `StructuredText`; they record actual middleware hits and emit
  `InlineSpan` objects backed by renderer-independent ARGB rasters.
- UIE registers that plugin through the public `StructuredTextApi`. Modern AWT,
  Cosmic, and the vanilla compatibility adapter split layouts around the same
  inline spans. The former pre-routing dispatcher branch is removed;
  it must never pre-empt post-processing again.
- The standalone laboratory embeds the same UIE plugin and can replace it at
  runtime from a complete UIE mod JAR. Its isolated loader scans nested JARs and
  only loads `TextPipelinePlugin` providers, so Forge/Minecraft classes in the
  containing mod are never linked.

Explicit follow-ups rather than silent emulation:

- An offscreen GL context is still reported as unavailable until the
  Minecraft-free GL device adapter is extracted. AWT and Cosmic raster suites
  are active; GL capability must never be reported as passing when no context
  exists.
- Full UAX #14/BiDi paragraph shaping remains a later stage. Current CJK
  behavior supplies kinsoku-aware break opportunities and preserves source
  boundaries in both routes.

## Target pipeline

```text
input adapters
    -> TextSyntaxEngine
    -> StructuredText middleware
    -> inline-content resolution
    -> Unicode segmentation and CJK break analysis
    -> font fallback and shaping measurement
    -> paragraph line fitting
    -> per-line shaping and positioning
    -> animated glyph resolution (section-sign k)
    -> AWT or Cosmic rasterization
    -> underline/strikethrough geometry
    -> Brilliant and shadow post-processors
    -> standard GL component
    -> GUI/HUD frame flush or explicit fallback
    -> final framebuffer
```

Route selection happens after the shared structural stages and before backend
layout:

```text
StructuredText
    -> TextRenderRouteApi
         -> neofontrender:modern_structured
              -> configured AWT or Cosmic backend
              -> post-process graph
         -> neofontrender:vanilla_compatibility
              -> vanilla text runs + shared inline GL adapter
         -> neofontrender:vanilla_passthrough
              -> original FontRenderer method
```

Selection is based on the configured renderer and backend readiness, not on
whether the public modern API can lazily create an AWT adapter. Re-entrant calls
made by the vanilla compatibility route run under `ScopedFontRenderBypass`.
Every operation (`draw`, `measure`, `trim`, `size-to-width`, hit testing, and
inline bounds) is performed by the selected route's layout object so rendering
and interaction cannot disagree.

Every stage receives immutable input and returns immutable output or an
explicitly scoped builder result. Runtime diagnostics record the actual nodes
and edges used by a draw; the settings panel must not infer execution from
configuration alone.

## Common contribution protocol

All pipeline extensions share common contribution metadata:

```text
id              namespaced stable identifier
stage           typed pipeline stage
priority        deterministic ordering inside a stage
enabled         runtime availability predicate
revision        cache invalidation source
diagnostics     last result, skip reason, and failure state
```

Stage-specific interfaces remain strongly typed. A single untyped
`process(Object)` API would make stage ordering and cache ownership unsafe.
The common registry may expose all contributions for diagnostics while routing
them to the appropriate stage interface.

Contribution types are:

```text
TextSyntaxProvider
StructuredTextMiddleware
InlineContentResolver
LineBreakOpportunityProvider
ParagraphLayoutPolicy
TextPostProcessor
TextGlComponent
```

## Standard syntax engine

`TextSyntaxEngine` is the only scanner for control syntax. Providers register
rules rather than scanning and rewriting the complete string independently.
It supports two rule forms:

1. Fixed control rules, optimized for pairs such as `section-sign + code`.
2. Variable-length matchers for forms such as RGB codes or tagged extensions.

A provider conceptually exposes:

```java
interface TextSyntaxProvider extends TextPipelineContribution {
    TextTrigger trigger();
    SyntaxMatch match(SyntaxCursor cursor);
}
```

`SyntaxMatch` reports consumed source length, optional emitted text, semantic
operations, and diagnostic ownership. It never mutates renderer state and does
not return a replacement string as its primary representation.

Supported semantic operations include:

```text
SET_COLOR
RESET_STYLE
SET_OBFUSCATED
SET_WEIGHT
SET_SLANT
ADD_DECORATION
BEGIN_EFFECT
END_EFFECT
EMIT_INLINE_CONTENT
```

The engine performs one left-to-right reduction of these operations into
structured spans. Exact rules win over generic matchers. Variable rules use
longest-match ordering, then provider priority and ID for deterministic ties.
Duplicate ownership of an exact syntax rule is rejected unless the registering
provider explicitly names the provider it replaces.

Unmatched syntax is retained as `UnresolvedSyntax`. A final compatibility
policy decides whether it is rendered literally or receives vanilla unknown
code behavior. It must never be silently consumed by an arbitrary backend.

## Built-in Minecraft syntax provider

`minecraft:legacy_formatting` is a built-in `TextSyntaxProvider`, not a raw
string middleware and not a renderer-specific parser. It owns the documented
Minecraft rules:

```text
section-sign 0-f  -> set palette color and clear legacy style flags
section-sign k    -> enable obfuscated glyph selection
section-sign l    -> enable bold weight
section-sign m    -> enable strikethrough
section-sign n    -> enable underline
section-sign o    -> enable italic slant
section-sign r    -> restore caller color and clear legacy style flags
```

Recognition and execution are separate. The syntax provider records
`obfuscated=true`; the animated glyph resolver later performs the per-frame
glyph substitution. Bold and italic affect shaping/rasterization. Underline and
strikethrough produce geometry after glyph positioning. Reset and color are
state-reduction operations.

For example:

```text
section-sign r section-sign 0 k[space]section-sign k Minecraft

RESET, COLOR(black), TEXT("k "), OBFUSCATED(true), TEXT("Minecraft")

Styled span 0: "k "        black, normal
Styled span 1: "Minecraft" black, obfuscated
```

## Brilliant syntax provider

Brilliant Text registers its configured fixed codes with the syntax engine:

```text
section-sign g -> SET_EFFECT(brilliant_text:g)
section-sign s -> SET_EFFECT(brilliant_text:s)
section-sign q -> SET_EFFECT(brilliant_text:q)
section-sign v -> SET_EFFECT(brilliant_text:flame)
```

Its activation predicate preserves the original leading-effect rule. Effect
descriptors declare termination events instead of manually parsing Minecraft
codes:

```text
termination events: COLOR_CHANGE, RESET
non-terminating events: OBFUSCATED, WEIGHT, SLANT, DECORATION
```

Consequently `section-sign g section-sign l Text` retains both the Brilliant
effect and bold style, while a later color code closes the Brilliant span.
Only registered extension codes are consumed.

## Structured-text protocol

All input adapters converge on one representation:

```java
StructuredText
    String plainText
    List<StyledSpan> styleSpans
    List<StructuredEffectSpan> effectSpans
    List<InlineSpan> inlineSpans
    List<UnresolvedSyntax> unresolvedSyntax
    SourceMap sourceMap
```

A `StyledSpan` holds an immutable style snapshot:

```text
color override
obfuscated flag
font weight
font slant
underline flag
strikethrough flag
language/script hints
```

`StructuredEffectSpan` remains namespaced and extensible for post-processing metadata.
Style and effect spans share the same plain-text coordinate system and source
map. Keeping hot shaping attributes strongly typed prevents arbitrary effect
maps from polluting glyph and layout cache keys.

Adapters are required for legacy strings, `ModernText`, and structured
Minecraft components. Existing public APIs may remain as facades, but their
internal output must be `StructuredText`.

## CJK and paragraph layout

CJK processing is not syntax middleware and is not post-processing. It consumes
`StructuredText` after syntax has been removed from the visible character
stream and before final glyph positioning.

Paragraph layout is intentionally split into cooperating stages:

```text
StructuredText
    -> grapheme-cluster segmentation
    -> script/language itemization and BiDi analysis
    -> inline-content metrics
    -> break-opportunity providers
         - explicit newline
         - Unicode UAX #14
         - CJK character boundaries
         - western word boundaries
    -> shaping measurement and font fallback
    -> line-width fitting
    -> paragraph policies
         - CJK prohibited-start/prohibited-end punctuation
         - optional hanging punctuation
         - optional punctuation compression/justification
    -> final visual lines
    -> line-fragment reshaping when shaping context requires it
```

The first CJK implementation consists of a `CjkLineBreakProvider` and a
`CjkKinsokuPolicy`. Format controls have zero visible width because they no
longer exist in `plainText`. Every accepted break maps back through `SourceMap`
so book pagination, chat trimming, selection, cursor movement, and substring
APIs continue to use original-source boundaries.

Effect and style spans are sliced across visual lines without reparsing. A
line-wide Brilliant effect is clipped into a region for each visual line it
intersects.

## Animated obfuscated glyphs

Layouts containing `obfuscated=true` are marked animated. Static layout data
stores character positions, advances, and references to same-advance candidate
buckets; it does not cache the final random glyph IDs.

At draw time, a deterministic selection is derived from frame ID, layout ID,
and glyph index. The next frame produces a new selection. Repeated draws inside
one frame, including foreground and shadow, share the same resolved glyph
snapshot. No font texture is rerasterized merely to animate obfuscated text.

AWT uses the existing `FontSet` advance buckets. Cosmic receives an equivalent
candidate-bucket abstraction after shaping. Static-FBO consumers must inspect
the animated flag and invalidate their cached draw every frame.

## Raster and decoration stages

AWT and Cosmic consume identical styled glyph runs. They do not inspect section
signs. Backend-specific work is limited to shaping, glyph rasterization, atlas
management, and draw geometry.

Underline and strikethrough are positioned from shaped run metrics and emitted
as ordinary geometry. They are not screen-space shader effects. This preserves
scale, clipping, rotation, color, and shadow behavior.

## Post-processing and GL execution

Post-processors consume a draw-ready text result plus structured effect spans
and render context. The foreground branch may contain Brilliant color,
outline, glow, flame, and particle passes. The shadow processor composites a
shadow source behind the processed foreground according to `ShadowRenderSpec`.

Brilliant remains attached through a standard `TextGlComponent`. Its final
implementation follows a pooled, frame-aware model:

```text
post-processor
    -> enqueue passthrough/effect regions
    -> pooled FBO capture
    -> one draw per passthrough or effect region
    -> shader passes
    -> particle queue
    -> GUI/HUD post-event flush
    -> restore prior framebuffer, viewport, matrices, textures, and blend state
```

World, rotated, or perspective draws use an explicit capability decision and
reported fallback until a projection-aware implementation is provided. Shader
compile/link logs, framebuffer completeness, GL errors, fallback reasons,
capture counts, composite counts, and context classification are retained in a
render-thread diagnostics snapshot.

## Game-independent module boundary

Production code must be split so the engine is not coupled to Minecraft or
Forge singletons. The intended dependency direction is:

```text
text-engine-core
    <- awt/cosmic raster adapters
    <- GL execution abstraction
    <- Minecraft integration adapter
    <- standalone render laboratory
```

The core and reusable render layers must not import:

```text
net.minecraft.*
net.minecraftforge.*
Minecraft.getMinecraft()
GlStateManager
Forge event classes
```

Minecraft-specific code supplies configuration snapshots, fonts/resources,
frame IDs, draw contexts, framebuffer targets, and lifecycle events through
interfaces. The same production syntax engine, layout engine, shader resources,
and post-process graph run in the standalone application.

Planned Gradle modules are:

```text
engine/text-core              pure syntax, structure, CJK, layout protocol
engine/text-render            backend-neutral glyph/result/post-process model
engine/typst-render           Minecraft-free Typst JNI compiler and RGBA raster engine
engine/text-render-awt        standalone-capable AWT raster backend
engine/text-render-cosmic     standalone-capable Cosmic adapter
engine/text-render-gl         GL device abstraction and production passes
tools/text-render-lab         desktop GUI and integration-test runner
```

Exact module names may be adjusted to match repository conventions, but the
dependency boundary and forbidden-import checks are required.

## Standalone text render laboratory

`tools:text-render-lab` is a desktop GUI executable that runs without a game
installation, Minecraft classes, Forge, a resource pack, or a running client.
It provides:

```text
source editor with raw/control-code visibility
syntax-provider registry and enable/order controls
plain text, token, semantic-operation, span, and SourceMap inspectors
AWT/Cosmic backend selector
font, size, language, direction, and paragraph-width controls
CJK break opportunities, prohibited breaks, and final line boxes
glyph bounds, baselines, advances, atlas pages, and fallback-font inspection
section-sign k animation with frame stepping and pause
shadow settings and Brilliant effect controls
FBO/shader status and GL-state diagnostics
actual runtime flow graph built from trace events
PNG output, JSON trace export, and golden-image comparison
```

The preferred implementation is a normal desktop control surface with an
embedded GL canvas. The tool may use a standalone GL context implementation,
but it must execute the same production shader sources and render-pass logic as
the mod. GL access is hidden behind `GlDevice` and `RenderTarget` interfaces so
Minecraft's LWJGL binding and the laboratory binding are adapters, not forks of
the algorithms.

Required launch tasks are:

```text
gradlew :tools:text-render-lab:run
gradlew :tools:text-render-lab:build
gradlew :engine:text-core:test
gradlew :tools:text-render-lab:test
gradlew :tools:text-render-lab:verifyGolden
java -jar tools/text-render-lab/build/libs/neofontrender-text-render-lab-<version>-<platform>.jar
java -jar tools/text-render-lab/build/libs/neofontrender-text-render-lab-<version>-<platform>.jar --self-test
java -jar tools/text-render-lab/build/libs/neofontrender-text-render-lab-<version>-<platform>.jar --plugin <uie-or-plugin.jar>
```

## Out-of-game verification matrix

Pure unit tests cover:

```text
syntax registration, replacement, conflicts, priorities, and failure isolation
all Minecraft color/style/reset codes alone and in combinations
unknown and disabled extension-code policy
Brilliant leading activation and color/reset termination
source-boundary composition, trimming, selection, cursor, and substring mapping
grapheme clusters, surrogate pairs, combining marks, emoji, and BiDi boundaries
UAX #14 and CJK prohibited-start/prohibited-end cases
style/effect slicing over wrapped lines
layout cache revisions and provider configuration changes
```

Raster and animation tests cover:

```text
AWT and Cosmic visual metrics for identical StructuredText
bold, italic, underline, and strikethrough bounds
obfuscated frames change while advances remain stable
foreground and shadow share one obfuscated glyph snapshot per frame
font fallback and color-glyph behavior
deterministic bundled fonts and fixed raster settings
```

Offscreen GL integration tests cover:

```text
shader compile/link and uniform binding
FBO completeness, resize, pooling, and region clipping
passthrough, recolor, outline, glow, flame, particles, and shadow ordering
straight/premultiplied alpha expectations
framebuffer, viewport, matrix, texture, scissor, depth, and blend restoration
reported fallback when a capability is unavailable
pixel output against versioned golden images with explicit tolerances
```

Tests must be runnable in one process without starting Minecraft. Hardware GL
tests may additionally support a software-rendered CI context, but lack of a
GPU must produce an explicit skipped-capability report rather than silently
passing the GL path.

## Runtime information panel

The in-game panel and standalone laboratory consume the same immutable trace
protocol. Trace events identify actual execution:

```text
request/input adapter
structured middleware hits and skips
syntax-rule matches and unresolved controls
structured spans
inline and paragraph providers
CJK break candidates and rejected breaks
font fallback, shaping, raster backend, and cache result
animated-glyph resolver
decoration geometry
post-processor branches
GL capture, shader passes, flush, or fallback
```

GL state is sampled on the render thread into a diagnostics snapshot. Neither
GUI is allowed to issue arbitrary GL queries while drawing its information
panel.

## Migration order

1. Introduce core contribution metadata, `StructuredText`, style/effect spans,
   semantic operations, and SourceMap.
2. Implement `TextSyntaxEngine` and the Minecraft/Brilliant providers with
   complete non-GL tests.
3. Adapt public input forms and all measurement/trim/wrap APIs to structured
   text; retain old public names only as deprecated forwarding facades where a
   compiled addon still needs them.
4. Move CJK handling to break-opportunity and paragraph-policy providers.
5. Remove the AWT, Cosmic, pipeline, and Mixin duplicate format state machines.
6. Add modern AWT/Cosmic style execution and animated obfuscated glyphs.
7. Complete decoration geometry and shadow post-processing on structured runs.
8. Replace the provisional Brilliant capture implementation with the pooled,
   frame-aware standard GL component and full diagnostics.
9. Build the standalone laboratory early enough to drive backend and GL work,
   then make its automated suites required for subsequent migration steps.
10. Switch all modern `FontRenderer` interception to the new path only after
    the formatting, CJK, measurement, and fallback matrices pass.
11. Replace the legacy inline branch with a vanilla compatibility route, so both
    modern and vanilla rendering consume the same `StructuredText`, source map,
    inline resolver registry, and paragraph API.

## Completion criteria

The migration is complete only when:

- AWT and Cosmic consume the same `StructuredText` and contain no private
  section-sign parser.
- Every documented Minecraft format code behaves correctly without delegating
  rendering to vanilla `FontRenderer`.
- Brilliant effects and Minecraft styles compose by semantic rules.
- CJK wrapping, source boundaries, measurement, trimming, and rendering agree.
- The entire production pipeline can be executed and inspected in the
  standalone laboratory.
- Unit, raster, animation, offscreen GL, and golden-image suites run without
  launching Minecraft.
- The runtime flow graph reports actual branches and explicit fallback causes.
