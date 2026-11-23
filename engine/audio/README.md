# UIE audio engine

Client audio implementation, pinned to LavaPlayer 2.2.7 (Java 11+).
This module must not be included in the server addon. Public contracts must not
expose LavaPlayer classes. Local decoding produces stereo 48 kHz S16LE PCM.

`LavaPcmStream` is the production decoder: the addon's Paulscode codec pulls
20 ms frames from it on the game's streaming thread. Decoder completion is not
audible completion: the codec reports end-of-stream and the caller must wait for
the device to stop the source before advancing the playlist. Seeking is done by
reopening the stream at the target offset, which discards queued device buffers.

The Minecraft adapter lives in `addons/ui-enhancements` under
`neofontrender.addons.audio`: a dedicated Paulscode streaming codec feeds
`LavaPcmStream` into the game's SoundSystem, `VanillaMusic` plus
`MixinMusicTickerControl` extend vanilla scene track pools, and ModularUI/NFR
settings pages expose both systems. The addon packages the relocated shadow
artifact (`audio-bundled.jar`) via ContainedDeps; the server addon has no
dependency on this module.

## Resource-pack albums

A resource pack can expose read-only albums at
`assets/<namespace>/music/albums.json`. They appear beside local playlists and
can also be assigned to Minecraft scenes. Album metadata is rebuilt on every
resource reload, so disabling a pack removes its albums without leaving stale
library entries.

```json
{
  "albums": {
    "overworld": {
      "title": "Overworld Sessions",
      "artist": "Example Artist",
      "cover": "example:textures/music/overworld.png",
      "year": 2026,
      "genre": "Ambient",
      "tracks": [
        {
          "source": "res:example:sounds/music/overworld/day.ogg",
          "title": "First Light",
          "artist": "Example Artist",
          "composer": "Example Composer",
          "trackNumber": 1
        }
      ]
    }
  }
}
```

`source` is the actual resource file, including `sounds/` and its extension.
When the namespace after `res:` is omitted, the album file's namespace is
used. `cover` is retained as a resource location for the dedicated player UI;
the settings-page playlist intentionally does not render album artwork.
Consumers can enumerate immutable metadata snapshots with `UieAudio.albums()`
and start a concrete entry with `UieAudio.playAlbum(albumId, trackIndex)`.

## Integration contract

- Independent players and vanilla music have separate state and public entry points.
- SceneMusicPolicy consumes Minecraft's ambient music type, not a track ID.
- The vanilla adapter must select concrete sound entries without losing scene identity.
- Music focus is an owned token; releasing it must not undo a user's explicit pause.
- Device reload invalidates old output handles. Never feed a destroyed SoundSystem.
- Music controls use ModularUI and NFR settings components exclusively; implement
  missing playlist components in the existing NFR style, not vanilla buttons/screens.
- Settings belong to UIE's TOML; persistent track/playlist data has separate storage.
- The server addon must not depend on this module or load LavaPlayer.

## Verification

Run `:engine:audio:test` with JDK 25. Production classes target Java 11.
Compressed format smoke tests require ffmpeg on PATH solely to generate fixtures;
ffmpeg is not a runtime dependency. Tests decode WAV/MP3/Vorbis/FLAC through
`LavaPcmStream` itself, including seek offsets and close semantics. They do not
verify audible output or a running Minecraft client.

## Verification status

Done: engine tests decode WAV/MP3/Vorbis/FLAC through the production
`LavaPcmStream` (including seek and close), playlist and scene policy tests,
addon compile/test, shadow relocation audit (no unrelocated third-party
packages), addon jar contains `audio-bundled.jar` and mixin config.
Client startup verified on Cleanroom 0.6.8 / Java 25: all 22 mods loaded,
both music mixins applied without errors, SoundSystem started, world joined.

Not yet verified interactively in-game: audible output through the Paulscode
codec path, scene switching, device/resource reload recovery, pause
interactions. Listen to actual playback and record results before calling
the feature complete.
