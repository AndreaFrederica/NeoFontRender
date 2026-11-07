package neofontrender.typst;

import java.nio.file.Files;
import java.nio.file.Path;

/** Standalone CI probe: load the packaged resource through JNI on each native runner. */
public final class TypstNativeSmoke {
    public static void main(String[] args) throws Exception {
        Path cache = Path.of(args[0]);
        try (TypstEngine engine = TypstEngine.open(cache)) {
            String prefix = "#set page(width:auto,height:auto,margin:4pt,fill:none)\n#set text(fill:white)\n";
            visible(engine.render(prefix + "$integral_0^1 x^2 dif x = 1/3$", 2));
            visible(engine.render(prefix + Files.readString(Path.of(args[1])), 2));
            engine.installPackage("@preview/chemformula:0.1.3");
            visible(engine.render(prefix + "#import \"@preview/chemformula:0.1.3\":ch;#ch(\"SO4^2-\")", 2));
            System.out.println("Typst JNI, periodic table and HTTPS package rendering passed on "
                    + System.getProperty("os.name") + "/" + System.getProperty("os.arch"));
        }
    }

    private static void visible(TypstRaster raster) {
        if (raster.width() < 1 || raster.height() < 1) throw new AssertionError("Empty raster");
        for (int pixel : raster.argb()) if ((pixel >>> 24) != 0) return;
        throw new AssertionError("No visible pixels");
    }
}
