# Tooltip preview styles

UI Enhancements loads model-preview styles from resource packs at:

```text
assets/neofontrender_ui_enhancements/tooltip_previews/styles.json
```

It also loads `*.json` files from the game directory:

```text
neofontrender/tooltip_preview_styles/
```

User files are read in case-insensitive filename order after resource-pack definitions. A later
definition with the same `id` replaces the earlier definition and emits a diagnostic. Resource
reload applies resource-pack and user-file changes. The addon creates `example.json` in the user
directory on first initialization.

## File structure

A file contains either a root array or a `styles` array:

```json
{
  "styles": [
    {
      "id": "examplemod:base",
      "scale": 3.0,
      "pitch": -25,
      "effects": ["shimmer", "pulse_frame"],
      "sound": {
        "enabled": true,
        "event": "ui.button.click",
        "volume": 0.25,
        "pitch": 1.1,
        "cooldownMillis": 250
      }
    },
    {
      "id": "examplemod:blade",
      "extends": "examplemod:base",
      "items": ["examplemod:charged_blade"],
      "priority": 100,
      "roll": -40,
      "rotationSpeed": -22,
      "width": 36,
      "height": 72,
      "nbt": {
        "display.Name": "Charged Blade",
        "stats": { "level": 3 },
        "runes": [4, 7]
      }
    }
  ]
}
```

`extends` performs a recursive object merge. Child scalar and array values replace parent values;
nested objects such as `sound` merge by key. An inheritance cycle invalidates every style in that
cycle. An unknown parent emits a diagnostic and leaves the local definition usable.

## Match rules

A style must contain at least one matcher. All non-empty matcher groups must match:

| Field | Behavior |
| --- | --- |
| `items` | Exact item ids, `namespace:*`, or `*` |
| `mods` | Exact namespaces or `*` |
| `rarities` | Lowercase rarity names or `*` |
| `nbt` | Recursive partial match against the stack tag |

NBT object entries are partial matches. Dotted keys such as `display.Name` traverse compounds.
Arrays match a prefix, so the actual NBT list may contain more entries. JSON booleans and numbers
match numeric NBT values; strings match NBT strings.

Styles are sorted by descending `priority`, and the first matching style is used. The global
preview blacklist is checked first, followed by the optional whitelist. A whitelist entry also
explicitly enables its matching ordinary item. With the default `tools` scope, UIE recognizes the
vanilla tool and weapon families plus any mod item that declares a non-empty Forge tool class. A
matching style can also enable an ordinary item. Set `tooltip.preview.item.scope=all` to preview
every item that is not filtered out, provided item previews are enabled globally.

## Appearance fields

| Field | Meaning |
| --- | --- |
| `scale` | Model scale; armor and item previews have separate defaults |
| `pitch` | X-axis rotation in degrees |
| `roll` | Z-axis rotation for item previews |
| `rotationSpeed` | Y-axis degrees per second; negative values reverse direction |
| `width`, `height` | Reserved layout size in GUI pixels |
| `effects` | Ordered list of registered effect ids |
| `sound` | Boolean or sound object overriding global appearance-sound settings |
| `armorModel` | `armor_stand` or `player` |
| `armorMode` | `single_piece` or `full_set` |

The bundled production style list is empty. Vanilla and modded items therefore use the regular
preview transform without automatically receiving decorative effects. Resource packs and user
styles can opt into the built-in effect ids: `shimmer`, `pulse_frame`, `ray_glow`, `icon_particles`,
`inward_particles`, and `line_particles`. `icon_particles` renders small copies of the previewed
item, including its enchantment effect. Particle count and animation speed remain controlled by
the global tooltip settings. Effects declare any pixels they paint outside the model rectangle, so
the common layout can keep those decorations on-screen.

Armor-slot detection first uses Forge's item hook, then vanilla equipment detection, then the
`ItemArmor` slot. This includes Elytra and modded wearable items that declare an armor slot.
Non-`ItemArmor` chest equipment uses the player model automatically because player render layers
usually own those visuals. `full_set` combines the hovered piece with the local player's other
equipped armor. Player previews use the local profile, skin model and main-hand preference without
modifying the real player entity.
