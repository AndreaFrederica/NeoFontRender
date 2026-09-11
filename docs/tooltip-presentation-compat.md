# Tooltip layout and presentation compatibility

With modern tooltips enabled, NFR measures and wraps text even when a foreign
tooltip panel has priority. Obscure TextComponent uses the same width and line
advance helpers as the native modern renderer, including inline formula heights.

| Installed / active | Legendary priority | Obscure priority | Panel | Other content |
| --- | --- | --- | --- | --- |
| Legendary only | On | Either | Rectangular panel using Legendary Color event | Legendary frame, shine and shadow; NFR text |
| Obscure only | Either | On | Obscure | Obscure header, preview and effects; NFR text |
| Both | On | Either | Legendary | Obscure header, preview and effects; Legendary decorations; NFR text |
| Both | Off | On | Obscure | Legendary decorations and Obscure content; NFR text |
| Either / both | Off | Off | NFR | Installed tooltip effects and NFR text |

The priority switches select presentation, not text layout. Disabling modern
tooltips restores the original foreign renderers. Legendary's fixed-height name
separator is suppressed only inside a modern layout scope; the measured NFR
separator is controlled by NFR settings. Shadow, textured frame and shine remain
Legendary's responsibility. Its PostText event receives bounds aligned to the
actual panel, including the Obscure header and preview area. Other PostText
subscribers retain their original event coordinates.

Inspected runtime jars: LegendaryTooltips 1.12.2-1.1.11 and Obscure Tooltips
1.12.2-3.10.1. Legendary customizes Forge Color and PostText events; it does not
own the text renderer. Obscure normally omits Color and synthesizes PostText
coordinates around its text body; the bridge supplies color initialization and
the full panel geometry for Legendary.

Regression coverage includes panel priority, decoration bounds, nested/failed
render scope cleanup, inline row advances, width limits, header height and mixin
registration. In-game validation should exercise a framed item with long text,
LaTeX/SVG content, scaled text, and an armor/tool preview, then toggle both panel
priorities and test at screen edges. Automated tests do not verify OpenGL output.
