# Third-Party Notices

## TabbyChat 2 Reforged

This directory is derived from TabbyChat 2 Reforged:

- https://github.com/mattmess1221/TabbyChat-2
- commit `e6b42ad980e14a6638e7a3d1a31e63a271129c52`

The upstream repository distributes this work under the Apache License 2.0.
The license text is preserved in `LICENSE-APACHE-2.0.txt`. Files in this
directory have been modified for private namespaces and Minecraft 1.7.10 as
described in `UPSTREAM.md`.

## Jazzy Spell Checker

The spell-checking source requires:

- Maven coordinate: `net.sf.jazzy:jazzy:0.5.2-rtext-1.4.1-2`
- POM license: GNU Lesser General Public License 2.1
- Project URL declared by the POM: http://jazzy.sourceforge.net/

The Jazzy binary is not stored in this vendor directory. It was used only as a
temporary compile-time classpath during backport verification. Before the
private source is enabled in a distributed addon, the integration must choose
and document an LGPL-2.1-compliant dependency or shading strategy and include
all license/source-relocation materials required by that strategy.

