# ModernUI-MC research notes

Reference checkout: `D:\Projects\sfr\Othermod\ModernUI-MC`

## Screen behavior

ModernUI-MC does not replace Minecraft's create-world or language screens on any currently tracked high-version branch. It enhances vanilla screens globally through background blur/color animation, modern text rendering, and smooth selection-list scrolling. The create-world information architecture therefore comes from high-version vanilla Minecraft; the visual softness and motion come from ModernUI-MC.

For the 1.12.2 port, `GuiCreateWorld` remains the authoritative controller. The UIE mixin only moves known vanilla controls and adds tab buttons. It deliberately preserves:

- the original screen instance and `actionPerformed()` implementation;
- dynamic `WorldType.WORLD_TYPES` enumeration and `canBeCreated()` filtering;
- `isCustomizable()`, `onCustomizeButton()`, and `onGUICreateWorldPress()`;
- seed parsing, recreation state, `WorldSettings`, and integrated-server launch;
- vanilla button IDs and Forge init/action events;
- unknown buttons added by other mods.

Subclasses/replacement screens are left untouched, and the feature has a config switch.

## Font architecture

ModernUI-MC builds an ordered `Typeface` from local files, registered resource fonts, and aliased system font families. Layout itemizes text into font runs, then caches vanilla strings, components, and formatted layouts separately. Glyphs are lazily rasterized into atlases which can grow and compact; atlas invalidation clears dependent render data. Non-render threads use pooled layout processors, while GL upload remains render-thread owned.

NFR already had ordered configured fallbacks, Skia/Cosmic font managers, layout/render/measure caches, lazy glyph baking, paged AWT atlases, segment caching, basic-Latin prewarming, and batched sign rendering. The first safe migration implemented here is:

- a Java `SansSerif` composite font as the final automatic system fallback;
- AWT layout split into contiguous provider runs, retaining shaping inside fallback runs instead of measuring every missing-primary character independently.

Further candidates need profiling and a texture invalidation contract before implementation:

1. Cache font-provider selection per code point or Unicode range.
2. Add TTL-based eviction to AWT glyph/layout caches.
3. Replace fixed atlas pages with grow/compact behavior only after render references can be invalidated safely.
4. Pool background layout processors, but never upload GL textures off the render thread.
5. Add cache-memory counters and fallback-hit telemetry before tuning limits.

ModernUI-MC is LGPL-3.0-or-later. Architecture can be reimplemented, but source copied or adapted from it must retain the applicable license and notices.

## View zoom and text LOD

ModernUI-MC implements its OptiFine-like hold-to-zoom behavior by multiplying the
already-computed camera FOV by `0.25` while its key is held, temporarily enabling
smooth camera, and restoring the previous camera setting on release. The UIE port
uses the same observable behavior through Forge's FOV event, while yielding to
OptiFine and composing after other FOV handlers.

NFR does not need a zoom-specific glyph switch. `FontRenderTuning` measures a text
quad's device-space scale from the live model-view and projection matrices. A lower
FOV therefore selects a larger adaptive raster bucket for world text automatically;
orthographic HUD text remains at its normal GUI bucket.

UIE optionally interpolates that FOV change with a reversible smooth-step transition.
Because NFR measures the live projection every frame, intermediate zoom frames also
select raster buckets from their actual on-screen text scale.

Mouse input is compensated independently from the saved sensitivity. UIE measures the
perspective screen-magnification ratio with `tan(fov / 2)`, then inverses Minecraft's
cubic sensitivity curve at the mouse-input read. A configurable `-100%` to `+100%`
strength exponent can reduce or increase final camera movement; `0%` preserves vanilla
movement. The adjustment follows both steady zoom and intermediate transition frames.
