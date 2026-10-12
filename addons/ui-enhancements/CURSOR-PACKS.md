# Revo UI Cursor Packs

Revo UI loads static PNG and SVG cursor images from either source:

- Local: `.minecraft/neofontrender/cursors/`
- Resource pack: `assets/<namespace>/neofontrender/cursors/`

Local images are discovered recursively and can be selected immediately. A `cursors.json`
manifest is optional locally, but is required in each resource-pack namespace because
Minecraft 1.12 cannot enumerate arbitrary resource-pack files.

```json
{
  "cursors": [
    {
      "name": "Arrow",
      "texture": "arrow.svg",
      "hotspotX": 1,
      "hotspotY": 1
    },
    {
      "name": "Text",
      "texture": "text.png",
      "hotspotX": 8,
      "hotspotY": 8
    }
  ]
}
```

For a local pack, paths are relative to the manifest. For a resource pack they are relative
to `assets/<namespace>/neofontrender/cursors/`; a full `namespace:path` is also accepted.

Images are limited to 128 x 128 pixels. Larger PNG images and SVG viewports are scaled down,
including hotspot coordinates. SVG files are limited to 512 KiB and must be static and
self-contained. Scripts, external references, embedded images, DTDs, entities, imports,
animations, and `foreignObject` are rejected.
