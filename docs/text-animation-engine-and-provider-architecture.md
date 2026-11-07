# Text Animation Engine and Provider Architecture

## Purpose

NFR exposes one glyph-level rendering engine while keeping syntax compatibility modules independent. The engine owns animation and visual composition; providers own syntax, defaults, configuration, and source mapping.

```text
Structured text pipeline
|- BrilliantSyntaxProvider -> WHOLE_RUN post-process effects
`- TextAnimatorCompatibilityProvider -> GLYPH animation effects
```

Additional providers (RGBChat, future compatibility layers) may use the same engine without sharing parser state.

## Provider boundaries

### Brilliant Text

`BrilliantTextProvider` parses the legacy `§` codes and emits `brilliant_text:*` effect spans. It owns Brilliant defaults, code bindings, and the experimental non-line-leading option. Brilliant source projection is enabled only when this provider is enabled.

### TextAnimator compatibility

`TextAnimatorCompatibilityProvider` parses the original TextAnimator tags and parameters, including `wave`, `wiggle`, `shake`, `bounce`, `swing`, `pend`, `turb`, `fade`, `pulse`, `rainb`, `grad`, `shadow`, `neon`, `glitch`, `scroll`, and `typewriter`. It preserves the original nesting, parameter aliases, animation modes, typewriter speed, and typewriter character/word modes. Its public settings are named **TextAnimator compatibility**; internal engine code uses `TextAnimationEngine`.

## Generic rendering contract

Providers produce structured effect spans. Each `EffectDescriptor` carries an
`animationRenderMode`: `GLYPH` requests per-cluster animation, while
`WHOLE_RUN` keeps the provider's native shaping and renders the span as one
object. The mode is copied into `StructuredEffectSpan`, so it survives slices,
middleware rewrites, and route caching. A provider therefore may register both
modes in the same parser; RGBChat-style effects should use `WHOLE_RUN`.

For `GLYPH` spans, the Cosmic backend asks its native shaping engine for the
actual UTF-16 cluster ranges and converts each cluster into a
glyph context containing source range, glyph index, transform, color, alpha,
shadow state, and optional masks. Emoji, joiner sequences, combining marks,
ligatures, and other shaping-sensitive sequences therefore stay in one render
object. The AWT backend conservatively keeps shaping-sensitive text on its
whole-run path. Ordinary non-effect text is never split by either backend.
Layout advance remains owned by the text layout and is never changed by
animation.

This supports ordinary transforms (`wave`, `wiggle`, `shake`, `bounce`,
`swing`, `pend`, `turb`, and `scroll`), color/visibility effects (`fade`,
`pulse`, `rainb`, `grad`, and `typewriter`), and multi-pass effects (`shadow`,
`neon`, and `glitch`) through one sampler contract implemented by both
renderers. The sampler returns an ordered list of concrete ARGB draw layers
and an ordered list of glow specifications. Nested color effects therefore
compose in source order, nested glitches retain every generated slice, and
multiple nested neon tags each retain their own pass count, radius, and alpha.

Animated shadow passes are sampled separately from foreground passes. A
glitch shadow preserves the original shadow layer while its sliced layers use
the source mod's red/green channel mutations and foreground-aligned offsets.
Cosmic's static native shadow composition is bypassed for animated text so a
single cached shadow bitmap cannot erase these per-layer semantics. Both
backends still reuse their normal static shadow path for non-animated text.

Brilliant remains `WHOLE_RUN`: its outline, glow, animated flame distortion,
and particles run after the shaped text has been captured, so it does not
sacrifice emoji or ligature shaping.

The flame post-process must always retain the captured base alpha mask. The
animated UV sample is only an optional edge/warp contribution; if it lands on
a transparent texel, the original source alpha is still rendered. Flame
particle emission follows the upstream `1/20` per-render-pass probability and
is bounded by a renderer-wide queue cap, preventing repeated HUD/chat draws
from accumulating enough particles to obscure the text.

Inline routing segments a structured line only at effect boundaries. Plain
prefixes, suffixes, RGBChat-style whole-run spans, and other non-glyph effects
continue through the ordinary shaped-string path. Only a span whose descriptor
declares `GLYPH` requests native-cluster animation.

## Provider state and source editing

Provider enablement is part of the parser revision and cache key. A disabled provider must not leave recognized effect spans, inline previews, or source-edit projection entries behind.

The public `SourceEditProjection` API enforces this invariant too: unresolved
syntax becomes an editable source span only when an enabled structured syntax
provider recognized the text. This prevents a disabled provider from leaving a
stale Tabby source-expansion target; existing structured-text revisions still
invalidate route and layout caches when settings change.

The source editor follows this invariant:

| Provider state | Display | Source projection | Source expansion service |
|---|---|---|---|
| Enabled | Rendered/preview form | Available for recognized spans | Available |
| Disabled | Original source text | Disabled for that provider | Disabled |
| Unknown/unavailable | Original source text | No fabricated span | Disabled |

When a provider is disabled, its syntax is treated as ordinary source text. The editor must not partially render a stale result from the parse cache. `SourceEditProjection` and raw/preview routing therefore consult the active provider set and invalidate their caches whenever provider state changes.

This rule applies independently to Brilliant and TextAnimator. Disabling TextAnimator must not disable Brilliant source editing, and disabling Brilliant must not disable TextAnimator compatibility.

## Configuration

User-visible settings are separate:

```text
Laboratory
|- Brilliant Text
|  `- Brilliant non-line-leading
`- TextAnimator compatibility
   |- Text Animation: All / None / No Rainbow
   |- Typewriter Speed
   `- Typewriter Mode: By Character / By Word
```

The engine may expose global performance controls, but provider settings remain provider-owned. `All / None / No Rainbow` applies only to TextAnimator effects.

## Server behavior

Animation tags are client-rendered source syntax. Servers only enforce chat legality, length, and permissions. They do not need to understand or serialize animation state. The `§` permission is separate from provider enablement and is controlled by the shared UIE server policy.

TextAnimator's original anvil compatibility is implemented in UIE common code.
The `textanimator:anvilNaming` game rule is created on integrated and dedicated
servers and defaults to `false`. When disabled, recognized TextAnimator tags
are stripped from renamed item text; unknown or malformed angle-bracket text
is preserved. Enabling the rule keeps recognized tags in item names.

## Backend behavior

Cosmic shapes each animated span once and returns UTF-16 source ranges for its
native glyph clusters. Java creates one render object per returned cluster,
then applies animation layers without changing the shaped layout advance. This
keeps supplementary CJK characters, combining sequences, emoji ZWJ sequences,
and ligatures intact while still allowing ordinary Han characters and other
independent clusters to animate separately.

AWT uses the same effect sampler and multi-layer drawing contract. Its legacy
layout API cannot expose all native cluster boundaries reliably, so text that
contains joiners, combining marks, variation selectors, or shaping-sensitive
scripts is conservatively kept whole-run. Independent BMP and supplementary
code points, including ordinary Han characters, use its glyph animation path.

`shake` is a backend glyph transform, not a shader effect. Its implementation
matches TextAnimator's per-character displacement (`0.6 * a` pixels with a
time/codepoint/index direction seed). It is intentionally not registered as a
generic shader region. On AWT, any shaping-sensitive code point in the same
effect span can force the complete span to `WHOLE_RUN`, which means no shake is
applied to that span. Cosmic can retain cluster-level animation when its native
shaping data is available. The TextAnimator `effects = none` setting likewise
selects `WHOLE_RUN` for every effect by design.

### Shake timing and pulse brightness

The shake direction seed stays 64-bit through hashing. The frame clock provides
epoch milliseconds; converting `millis * 0.01` to `int` saturated at
`Integer.MAX_VALUE`, freezing all glyphs at the same displacement. Tests must use
realistic epoch timestamps as well as the 32-bit seed boundary, check motion over
multiple frames and characters, and keep foreground/shadow displacement equal.
This fix belongs to the shared sampler and applies to both Cosmic and AWT.

Pulse keeps TextAnimator's original formula:
`brightness = base + a * 0.25 * (0.5 + 0.5 * sin(phase))`.
Thus `<pulse base=0.6 a=0.4 f=1.5>Power Rising</pulse>` only varies from 60% to
70% brightness, with a period of about 2.09 seconds. Constant gray appearance is
not sufficient evidence of a stopped animation: compare sampled colors and native
raster pixels at multiple frames. Pulse changes RGB, leaving opacity and the
shadow pass unchanged.

The provider defaults for tags without brightness parameters are configurable in
Laboratory → TextAnimator compatibility. The shipped defaults are `min=0.6` and
`max=1.0`, so a plain `<pulse>Power Rising</pulse>` uses a visible 60%-100%
range. This does not alter tags that explicitly provide `base/a` or `min/max`.

For a clearly visible 60%-100% pulse in content that should be independent of
client settings, use either:

- Original TextAnimator syntax: `<pulse base=0.6 a=1.6 f=1.5>Power Rising</pulse>`.
- NFR's explicit endpoint extension: `<pulse min=0.6 max=1 f=1.5>Power Rising</pulse>`.

Providing `min` or `max` selects endpoint mode; these endpoints take precedence
over `base/a`. The `f` speed and `w`
per-character phase retain their existing meaning. Existing tags without endpoint
parameters keep their original brightness, including nested color-effect order.
The extension requires no new provider, post-process shader, or backend branch.

The source mod contains a `scroll` implementation while its public effect
registration is commented out. NFR intentionally registers the tag: existing
content that uses it receives the upstream behavior, while this does not alter
ordinary text or any other provider.

Typewriter timing uses a bounded per-content track cache and advances at most
once per rendered frame, matching the source mod instead of catching up in one
large jump after the text was not drawn. Character mode indexes shaped source
clusters; word mode indexes whitespace-delimited reveal groups.

## Verification contract

Core tests cover every registered TextAnimator tag, parameter aliases, nesting,
animation filtering, typewriter modes, multi-layer glitches, multiple neon
layers, color ordering, shadow preservation, provider disablement, and source
projection. Cosmic smoke tests additionally assert cluster ranges for
supplementary CJK, combining text, an emoji ZWJ family, and an `ffi` ligature.
UIE tests cover shared chat-character policy and TextAnimator anvil filtering.

## Compatibility requirements

- Original TextAnimator tag names and parameter aliases remain valid.
- Unknown tags remain ordinary text.
- Effects may stack across providers, with render mode selected per effect span.
- Source coordinates remain UTF-16 source coordinates while glyph indexes count Unicode codepoints.
- Animated glyph objects follow native shaping clusters rather than blindly splitting UTF-16 code units.
- Disabling a provider invalidates parsed text, layout, and source projection caches.
- No provider may install a global font or parser bypass.
