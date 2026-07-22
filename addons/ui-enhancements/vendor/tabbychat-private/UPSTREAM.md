# TabbyChat Private Backport

This directory contains a privately namespaced backport of TabbyChat 2 Reforged
for Minecraft 1.7.10.

## Source

- Repository: https://github.com/mattmess1221/TabbyChat-2
- Imported commit: `e6b42ad980e14a6638e7a3d1a31e63a271129c52`
- Imported commit subject: `2.4.5`
- Legacy 1.7-era API reference: `ba7ecc7e59f4cdd874d6f9985f285241c0a18dcb`
  (`beta2a` tag)

The upstream repository does not have a `2.4.5` Git tag. The imported revision
is identified by its commit hash and commit subject.

## Namespace And Resource Changes

- `mnm.mods.tabbychat.*` was moved to
  `neofontrender.addons.vendor.tabbychat.*`.
- `mnm.mods.util.*` was moved to
  `neofontrender.addons.vendor.tabbychat.foundation.*`.
- TabbyChat and foundation resources were moved under the private resource
  domain `assets/neofontrender_tabbychat/`.

The standalone loading plugin, standalone mod lifecycle metadata, LiteLoader
metadata, pack metadata templates, and standalone Mixin configuration were not
imported. NeoFontRender's shared addon must own those integration points.

## Minecraft 1.7.10 Adaptation

- Modern text APIs were translated to `IChatComponent`, `ChatStyle`, and the
  Minecraft 1.7 click/hover event API.
- Modern GUI, sound, networking, Forge, and LWJGL calls were translated to the
  corresponding Minecraft 1.7.10 APIs.
- Modern tab-completer integration was changed to the Minecraft 1.7.10
  `GuiChat.func_146406_a` completion callback.
- Standalone `@Mod` lifecycle ownership was replaced by the explicit
  `TabbyChat.start(TabbyChatHost)` host boundary.
- Reflective Gson generic decoding was replaced with explicit settings,
  channel, filter, and message codecs. Settings writes are atomic and chat
  history saving is functional.

## Integration Boundary

The source and resources in this directory are intentionally isolated from the
shared addon build. A later integration change must add the private source set
and resources, provide Jazzy, register the required Mixins, and start TabbyChat
through `MinecraftTabbyChatHost` from the addon lifecycle.

