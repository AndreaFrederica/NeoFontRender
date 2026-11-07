NFR Typst Renderer is an optional client addon for NeoFontRender and Revo UI.

The JNI raster engine links the Typst compiler and raster renderer from the pinned
`vendor/typst` submodule. Typst is distributed under Apache-2.0; see the
submodule's LICENSE and NOTICE files. The platform-specific library is bundled
inside the addon JAR, extracted on first use, and never loaded into the Java
class path.
