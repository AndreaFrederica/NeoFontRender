# Text pipeline API

Neo Font Render exposes its client text extension API under
`neofontrender.api.text.pipeline`. Core compatibility transforms, UI Enhancements embedded
content, CJK paragraph layout, and third-party extensions all use the same registry.

Choose the narrowest stage that owns the behavior:

- `RawTextMiddleware` transforms a whole string while retaining source-boundary mappings.
- `InlineContentMiddleware` replaces one atomic source range with measured drawable content.
- `ParagraphLayoutMiddleware` optionally owns paragraph or component line layout.

Every middleware has a namespaced `id()`, optional `priority()`, and dynamic `isEnabled()` state.
Registration replaces an existing middleware with the same ID and returns a handle:

```java
TextMiddlewareRegistration registration = TextPipelineApi.register(new MyMiddleware());
// Later:
registration.close();
```

Closing an older handle does not remove a newer same-ID replacement. Registration order is not
semantic: higher priority runs first, then IDs provide deterministic ordering. A provider failure
is logged once and isolated so lower-priority providers can continue.

## Inline content

An inline provider declares cheap first-character triggers and performs bounded CPU-only parsing:

```java
public final class MyTokenMiddleware implements InlineContentMiddleware {
    public String id() { return "example:my_token"; }
    public int priority() { return 0; }
    public TextTrigger trigger() { return TextTrigger.exact('['); }

    public InlineContentMatch match(CharSequence source, int index) {
        if (!matchesMyToken(source, index)) return null;
        return new InlineContentMatch(index, tokenEnd, myContent);
    }
}
```

`match` runs during measurement and rendering. It must not perform disk or network I/O, use an
unbounded parser, or call back into the text pipeline. `InlineContent.advance()` and `height()`
must stay stable for a cached layout. NFR indexes exact ASCII triggers and only calls relevant
providers at a source position. Layouts are cached per `FontRenderer` in a bounded 512-entry LRU.

When a provider's dynamic state or rendered metrics change, call `TextPipelineApi.invalidate()`.
This advances the shared revision and invalidates NFR and consumer-owned layout caches. With all
inline providers disabled, the normal FontRenderer path is an O(1), allocation-free branch.

## Raw transformations

Raw middleware receives a `ProcessedText`. If it transforms `input.visibleText()`, return a result
whose boundaries still refer to the original `input.rawText()`. `ProcessedText.compose(input,
transformation)` rebases a transformation onto the previous stage's original source mapping.
Returning `null` passes the current representation to the next middleware.

## Images and embedded content

UI Enhancements exposes policy-gated image helpers under
`neofontrender.addons.api.content`. `InlineImages.external(uri, description)` cannot bypass the
user's external-image switch, host allowlist, blocklist, HTTPS/DNS checks, redirect validation, or
size limits. `InlineImages.local(alias, description)` resolves the asynchronous local gallery.

Built-in experimental LaTeX, SVG, full-SVG compatibility and Markdown switches live in the NFR
Laboratory settings. Markdown supports bounded single-line bold, italic, strikethrough, inline
code and link labels. Block Markdown is intentionally outside the FontRenderer pipeline.
