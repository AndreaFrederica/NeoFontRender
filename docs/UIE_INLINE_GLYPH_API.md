# UIE inline glyph middleware API

The optional UI Enhancements addon exposes a provider-based inline glyph API under
`neofontrender.addons.api.inline`. It keeps normal text on Neo Font Render while allowing UIE or
another mod to replace an atomic source range with a measured, drawable object.

```java
AutoCloseable registration = InlineGlyphRegistry.register((source, index) -> {
    if (!matchesMyToken(source, index)) return null;
    return new InlineGlyphMatch(index, tokenEnd, myGlyph);
}, 0);
```

Providers must be CPU-only: `match` runs during measurement and rendering and must not perform
disk or network I/O. `InlineGlyph.advance` must remain stable for the lifetime of a visible layout.
Close the returned registration to unregister the provider.

For user-supplied remote images, call `InlineImages.external(uri, description)`. This returns
`null` unless the **Emoji & Images** external-image switch is enabled and the URL passes UIE's current
policy. The public API cannot bypass the policy.

The built-in experimental syntax is:

```text
<img:https://images.example.com/path/picture.png>
```

Security invariants are not configurable: HTTPS only, no credentials/fragments/non-443 custom
ports, redirect revalidation, public DNS addresses only, at most three redirects and 8 MiB encoded
data. Source images are preflight-checked at 16,384 pixels per side / 64 megapixels, then decoded
with subsampling into an at-most 1024-pixel texture. The user allowlist accepts exact hosts and
`*.example.com`; the blocklist always wins. Failed or denied tokens remain ordinary visible text.

The Gosling compatibility provider recognizes standard `:alias:` strings, `<:name:id>`,
`<a:name:id>` and Gosling's Base64-encoded Discord IDs. Raw Unicode emoji deliberately stays in
the normal font/fallback-family pipeline, so configured monochrome or color emoji fonts retain
their glyphs and metrics. The bundled dictionary and picker metadata retain the upstream MIT
license. Image features are disabled by default under **Emoji & Images**.

Validated source images are cached under `<game>/neofontrender/image-cache`, next to (not inside)
the `fonts` directory. The completion popup requests only its currently visible rows, so an alias
search does not enqueue the entire dictionary for download.

Client-owned images can be placed under `<game>/neofontrender/images`. With **Local image gallery**
enabled, `picture.png` is available as `:picture:`; nested paths are flattened with hyphens. The
gallery is rescanned asynchronously, so adding an image never blocks the render thread. Ordinary
image glyphs preserve aspect ratio and expand the containing UIE/chat row up to the inline display
limit, while Gosling emoji remains compact at text height. Hover uses the complete cached texture,
and right-clicking an image glyph offers **Copy image** for the operating-system clipboard.
Third-party providers can resolve the same policy-gated gallery with
`InlineImages.local(alias, description)`.

Holding Alt while hovering requests the decoded image's natural dimensions. UIE never enlarges a
small image in this mode; if the natural dimensions exceed the available screen area, it scales the
preview down proportionally so the panel remains fully visible.
