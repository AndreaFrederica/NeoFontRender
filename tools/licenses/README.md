# Settings license catalog

The settings page discovers `META-INF/neofontrender/third-party-licenses.json`
through the runtime classloader. Core, UI Enhancements, controller support and
Typst each ship their own catalog. Third-party info-page contributions remain
supported. Do not add a second hard-coded UIE license list.

Each entry records a section, component, version, license expression, upstream
source and verification evidence. The page displays the component/version/license;
the JSON preserves links and provenance for reviewers. Long expressions wrap.

## Refreshing

From the repository root, with dependencies already cached:

```powershell
./gradlew.bat -I tools/licenses/maven-inventory.gradle nfrLicenseInventory --offline
python tools/licenses/update_catalogs.py "$env:TEMP/nfr-maven-inventory.json"
```

The generator resolves Maven runtime/contained artifacts and their POM license
metadata, using reviewed overrides where a POM is incomplete. Cargo metadata is
read with `--locked --offline`; normal and build dependencies, including conditional
platform alternatives, are included. Dev-only dependencies and our JNI wrapper
packages are excluded. These native sections are dependency-tree notices, not a
claim that every listed build tool is linked into every distributed binary.

`curated.json` covers bundled sources, fonts, data and runtime integrations outside
those graphs. Its evidence points to repository notices or upstream licenses.
Update it when adding/removing bundled resources. Re-run generation after changing
Gradle dependencies, Cargo locks, vendored sources or font assets.

Verified 2026-09-12 against resolved dependency versions, Cargo package manifests,
bundled LICENSE/NOTICE files, and upstream sources. Specific checks include H2
2.3.232 (MPL-2.0 OR EPL-1.0), JSON-java 20240303 (public domain), Jazzy and
jaudiotagger source-JAR headers (LGPL-2.1-or-later), and Typst asset notices.
Typst fonts are not uniformly Apache-2.0: NewCM10-Regular has GPL font/distribution
exceptions, other NewComputerModern fonts use the GUST font license, and Libertinus
uses OFL-1.1. JLaTeXMath's linking exception and additional font terms are retained.
LavaPlayer 2.2.7's native build recipe also pins Opus, libogg, libvorbis,
libsamplerate, mpg123 (non-Windows builds), and FDK-AAC. Their upstream versioned
COPYING/NOTICE files were checked separately from Maven metadata; FDK-AAC retains
its custom Fraunhofer license, and libsamplerate 0.1.9 uses BSD-2-Clause.

This inventory is a settings-page summary, not a replacement for original license
texts or a legal determination about independent feature implementations. Mods
which are only detected for compatibility (for example CMM/FancyMenu) are not
listed as embedded dependencies. Tips attribution concerns copied tips/translations;
its reserved Runelic font is not bundled by this project.
