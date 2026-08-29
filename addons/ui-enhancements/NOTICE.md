# NFR Modern Tooltips notices

This optional addon links against Neo Font Render, which distributes Arc3D Core 2026.2.0.

Arc3D is Copyright (C) BloCamLimb and contributors and is licensed under
LGPL-3.0-or-later. Its upstream source is available at
https://github.com/BloCamLimb/Arc3D.

The tooltip layout and visual design are an independent Minecraft 1.12.2
implementation informed by the behavior documented by ModernUI-MC. No
ModernUI-MC source files are bundled in this addon.

The hold-to-zoom behavior is an independent Minecraft 1.12.2 implementation
informed by ModernUI-MC's documented OptiFine-like zoom behavior.

The integrated mod-name tooltip behavior is derived from Mod Name Tooltip by mezz,
Copyright (c) 2014-2015 mezz, licensed under the MIT License. The complete license
notice is included in META-INF/licenses/ModNameTooltip.txt.

The chat entrance animation behavior is informed by ChatAnimation by Ezzenix,
licensed under the MIT License. The complete license notice is included in
META-INF/licenses/ChatAnimation.txt.

The world-loading presentation is an independent Minecraft 1.12.2 implementation
informed only by the publicly documented behavior and screenshots of Better Loading
Screen. No Better Loading Screen source code, textures, icons, fonts, shaders, or
configuration text are incorporated in this addon.

The per-save last-exit screenshot behavior is an independent implementation informed
by FluxLoading's public feature description. No FluxLoading source code, shaders, or
bundled image resources are incorporated in this addon.

The singleplayer spawn-preparation progress design is independently implemented after
reviewing Loading Progress Bar's public behavior. It uses a targeted Mixin and does not
incorporate that mod's ASM transformer or resources.

Salutation 1.12.2 by Speiger is embedded as an integrated source component, including its
chat screens, multiline chat backend, advanced completer, command tree and argument types.
The small UIE configuration facade replaces only the original CarbonConfig/Forge mod entry
point; the Salutation behavior and public package names are retained. Salutation is Copyright
2025 Speiger and is distributed under the Apache License 2.0. The complete license is included
in META-INF/LICENSE-Salutation-1.12.2.txt and the original source is available at
https://github.com/Speiger/Salutation/tree/1.12.2.

Chinese chat segmentation and dictionary lookup use jieba-analysis 1.0.2 by Huaban,
distributed under the Apache License 2.0. The unmodified library is carried as an embedded
dependency. The complete license is included in META-INF/LICENSE-Jieba-Analysis.txt and its
upstream source is available at https://github.com/huaban/jieba-analysis.

Tip content (gameplay tips and translations) is derived from Tips by Darkhax,
used with attribution. The original Tips mod is available at
https://www.curseforge.com/minecraft/mc-mods/tips and its source at
https://github.com/Darkhax-Minecraft/Tips.
# Gosling / Emojicord compatibility data

The optional experimental image-glyph middleware includes the emoji and picker dictionaries
from TeamFruit's Emojicord/Gosling project and retains compatibility with its message tags.
Those portions are distributed under the MIT License in `LICENSE-Gosling-Emojicord.txt`.

# Camera platform sources

The internal Shoulder Surfing-derived camera rig and collision sampling are
distributed under the MIT License; the complete notice is in
`META-INF/licenses/ShoulderSurfing.txt`.

The internal Omnilook-compatible free-look behavior is based on public-domain
source; the attribution and disclaimer are in `META-INF/licenses/Omnilook.txt`.

# FancyOutlines-derived block outlines

Configurable block-selection outline colors, widths, per-block rules, and
non-harvestable highlighting are adapted from FancyOutlines by Invadermonky.
The upstream source is available at https://github.com/Invadermonky/FancyOutlines
and is distributed under the WTFPL v2. The complete license is included in
`META-INF/licenses/FancyOutlines-WTFPL.txt`.
