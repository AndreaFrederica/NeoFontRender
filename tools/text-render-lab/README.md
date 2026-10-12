# NeoFontRender Text Render Lab

The laboratory is a standalone desktop application for exercising the shared
structured-text pipeline without Minecraft or Forge. Its backend selector runs
either Java2D/AWT or the same cosmic-text Rust/JNI rasterizer used by the mod.
It also loads the Minecraft-free Typst JNI pipeline plugin and renders Typst
tokens through the same direct RGBA path used in game.

The GUI covers Minecraft and Brilliant syntax, structured spans, source maps,
CJK break opportunities, native shaping, color emoji, bold, italic, underline,
strikethrough, animated obfuscation, shadow preview, PNG export, and JSON trace
export.

Use the shared suffix to exercise formula sizing and source resolution in both
backends:

```text
$\\frac{a+b}{c}$[rows=1.5,supersample=4]
<typst:$ integral_0^1 x^2 dif x $>[rows=2,max-width=16em,supersample=4,align=center]
```

`rows`/`height` use line-height units, `columns`/`width` use em units, and
`supersample` changes raster density without changing the logical box.

Build and run the host-specific executable JAR:

```powershell
.\gradlew.bat :tools:text-render-lab:build
$labJar = Get-ChildItem tools\text-render-lab\build\libs\neofontrender-text-render-lab-*.jar | Select-Object -First 1
java -jar $labJar.FullName
```

Run its packaged-resource and backend self-test without opening the GUI:

```powershell
$labJar = Get-ChildItem tools\text-render-lab\build\libs\neofontrender-text-render-lab-*.jar | Select-Object -First 1
java -jar $labJar.FullName --self-test
```

The JAR is platform-specific because it embeds the Cosmic and Typst native libraries. The
build classifier records the target platform. AWT remains usable when Cosmic
cannot initialize, and the GUI reports that failure explicitly.
