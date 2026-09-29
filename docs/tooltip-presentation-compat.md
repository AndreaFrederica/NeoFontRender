# Tooltip layout and presentation compatibility

When Obscure Tooltips is installed, tooltip rendering has exactly one owner:

| UIE tooltip setting | Renderer | Content and presentation |
| --- | --- | --- |
| Item preview or armor preview enabled | UIE | UIE text, layout, panel, animations and previews, even on items without a model preview. Obscure's tooltip handler is skipped. |
| Both previews disabled, or UIE renderer unavailable | Obscure | Its original tooltip handler, layout, components, panel and effects. UIE's Forge and document renderers yield. |

`tooltip.preview.item.enabled` and `tooltip.preview.armor.enabled` control this
ownership. The old `tooltip.yieldToObscureTooltips` setting is ignored. No UIE
components are inserted into Obscure's renderer and no Obscure component is
inserted into UIE's renderer. If Obscure is absent, UIE's normal tooltip setting
continues to control its renderer independently of the preview switches.

LegendaryTooltips remains a presentation integration of UIE: its Color and
PostText events can decorate the UIE-owned panel, and its resource-pack frames
use UIE's measured bounds. UIE keeps ownership of the text and preview. When
Obscure owns rendering, UIE does not invoke that presentation path.

Automated coverage checks the ownership selector, optional mixin registration,
and Legendary panel bounds. Client validation still needs both preview switches
tested individually and together on ordinary items, tools, armor and screen
edges with Obscure and LegendaryTooltips installed; unit tests cannot validate
OpenGL output or third-party event ordering.
