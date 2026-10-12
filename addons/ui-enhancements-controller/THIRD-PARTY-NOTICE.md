# Controller Runtime Third-Party Notice

This addon embeds the `controlify-sdl` Java API and FFM backend, version
`release-3.4.14-7`. They are distributed under the Mozilla Public License 2.0.
The corresponding source is available at:

https://github.com/isXander/controlify-sdl

This addon also embeds SDL 3.4.14 native libraries published by the same project.
They are built by the `controlify-sdl` native workflow; its Windows build applies
the GameInput detection backport documented in that workflow. SDL is distributed
under the zlib license. The build recipe and corresponding SDL source are available
at:

https://github.com/isXander/controlify-sdl/blob/main/.github/workflows/build-natives.yml
https://github.com/libsdl-org/SDL/tree/release-3.4.14

The controller mapping database and implementation references derived from
Controlify remain covered by the Controlify license included separately in the
addon JAR.
