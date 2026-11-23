# NFR Render Showcase

Press **F9** (or use `/nfr_inline_showcase`) to browse mathematics, SVG chemistry,
Markdown, Typst chemistry, the periodic table and individual elements. Enable the corresponding
renderer in the laboratory settings. Typst requires the optional Typst Renderer
addon; this showcase does not embed another copy of its engine.

The **NFR Render Showcase / NFR 渲染展示** creative tab contains 157 items:
118 independent element items from hydrogen to oganesson, 36 chemical samples,
the existing crown-ether and meta-cresol demonstrators, and a periodic-table card.
Right-click a showcase item to open its corresponding gallery page. F9 and the
`/nfr_inline_showcase` command open the general showcase screen.
Tooltips use the same structured Typst tokens as the gallery. The gallery has
previous/next buttons, mouse-wheel and arrow-key navigation, and a source-copy
button. Long source is copied for use in an editor; the table is larger than
vanilla Minecraft's chat packet limit.

## Chemical samples

`src/main/resources/assets/neofontrender_inline_content_showcase/chemistry.tsv`
is the shared catalog. Every sample renders a molecular formula and a second
chemical notation or reaction with `@preview/chemformula:0.1.3`:

- Small molecules: water, methane, ammonia, carbon dioxide, hydrogen peroxide.
- Acids and bases: sulfuric, nitric and hydrochloric acids, sodium hydroxide.
- Salts and ions: sodium chloride, calcium carbonate, sodium bicarbonate,
  copper sulfate, potassium permanganate, ammonium sulfate, ferricyanide.
- Organic compounds: ethanol, acetic acid, acetone, ethyl acetate, benzene,
  phenol, aniline, glycine, aspirin, caffeine, glucose, sucrose.
- Isotope notation: carbon-14 and heavy water.
- Larger organic structures: naphthalene, anthracene, ibuprofen, L-tryptophan,
  adenosine and riboflavin (vitamin B2). These, aspirin and caffeine use
  `@preview/typed-smiles:0.10.0` for ring systems, heterocycles and side chains.
  Every organic sample now has a structure token in the gallery; the compact
  formula and reaction notation remain available alongside it.

Benzene, phenol, aniline and the existing meta-cresol demonstrator also have
explicit bond drawings made with `@preview/cetz:0.5.2`. Condensed formulas for
the other compounds are labelled chemical notation, not full stereochemical
structure diagrams. These are static rendering specimens, not a chemistry
simulation or reaction solver. Packages download on first use into the Typst
addon's normal package cache.

## Periodic table

`elements.tsv` defines the full set of element items, their registry IDs,
atomic numbers, symbols, categories, icon bases, and Chinese/English names. Each gets a
category icon with its symbol badge, a Typst-rendered element information card, and
an entry in the element gallery. The full table is an additional item, not a
replacement for the 118 element items. Example registry IDs are
`neofontrender_inline_content_showcase:element_hydrogen` and
`neofontrender_inline_content_showcase:element_oganesson`.

`src/main/resources/assets/neofontrender_inline_content_showcase/typst/periodic-table.typ`
contains the actual Typst program. It draws all 118 elements, atomic numbers,
group numbers, ten category colors and a legend, with separate lanthanide and
actinide rows. No external packages or pre-rendered image are used. The gallery
sets the shared inline size from the available viewport; the tooltip uses a
bounded size. Superheavy-element categories are conventional showcase labels.

## Icon pack

`gradle/icons.gradle` generates original 32×32 transparent pixel-art item PNGs
and Minecraft models during `processResources`. The reusable bases are:

| Catalog icon | Base silhouette | Example |
| --- | --- | --- |
| `gas` | Gas cylinder | Hydrogen, oxygen, methane |
| `liquid` | Liquid-filled reagent vial | Bromine, mercury, water |
| `solid` | Solid chunk | Carbon, silicon, phenol |
| `powder` | Powder mound on a weighing dish | Sulfur, calcium carbonate, caffeine |
| `metal` | Metal ingot | Iron, copper, gold |
| `element` | Symbol card | Elements 100–118 with uncertain bulk properties |
| `crystal` | Crystal cluster | Copper sulfate, naphthalene |
| `flask` | Conical flask | Acid/solution specimens |
| `tablet` | Pressed tablet | Aspirin, ibuprofen |

Neutral templates are packaged under `textures/items/base/`. Add a row to
`chemistry.tsv` with a base name and accent RGB; for elements, choose the last
column of `elements.tsv`. The generator adds the element symbol badge and
periodic-category color to the same base painter. It bakes the finished icon
into one PNG, so all items use a single texture layer at game runtime.
Gas/liquid element assignments use room conditions (about 20 °C); powders,
tablets and ingots denote the selected specimen form rather than a separate
state of matter. Catalog colors are display colors, not physical-color claims.

Models bind in `ModelRegistryEvent`, after Forge assigns the items' delegate
keys. Binding in `preInit` lets unnamed delegates alias one another, which
previously put the showcase vial on an electric elytra. Both addons now bind
after item registration. `pack.mcmeta` declares format 3 so Forge loads the
lowercase 1.12 language paths correctly.

## Verification

```text
gradlew :addons:inline-content-showcase:build
gradlew :addons:inline-content-showcase:typstShowcaseSmoke
```

The opt-in smoke task runs the real structured pipeline and Typst JNI renderer
on 93 chemistry/table tokens and all 118 element tokens, then repeats the table and two difficult
notation cases at GUI scale 2. It exports individual PNGs, chemistry and element
contact sheets and all 157 item icons to `build/reports/typst-showcase` in this addon. Package
downloads may be required. To reuse an existing package cache, pass
`-PshowcaseTypstLibrary=/absolute/path/to/typst-library` (the parent of `packages`).
