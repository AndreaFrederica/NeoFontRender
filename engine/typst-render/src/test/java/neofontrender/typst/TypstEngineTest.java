package neofontrender.typst;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TypstEngineTest {
    @TempDir Path temporaryDirectory;

    @Test
    void rendersStraightAlphaArgbWithoutAnImageCodec() throws Exception {
        try (TypstEngine engine = TypstEngine.open(temporaryDirectory)) {
            TypstRaster raster = engine.render(
                    "#set page(width: auto, height: auto, margin: 0pt, fill: none)\n"
                            + "$ integral_0^1 x^2 dif x $", 2.0F);
            assertTrue(raster.width() > 0);
            assertTrue(raster.height() > 0);
            assertEquals(raster.width() * raster.height(), raster.argb().length);
            boolean visible = false;
            boolean transparent = false;
            for (int pixel : raster.argb()) {
                visible |= (pixel >>> 24) != 0;
                transparent |= (pixel >>> 24) == 0;
            }
            assertTrue(visible, "formula must contain visible pixels");
            assertTrue(transparent, "transparent page background must survive JNI");
        }
    }

    @Test
    void rejectsUseAfterClose() throws Exception {
        TypstEngine engine = TypstEngine.open(temporaryDirectory);
        engine.close();
        assertThrows(IllegalStateException.class, () -> engine.render("test", 1.0F));
    }

    @Test
    void installsCachedPackageOfflineAndReportsCompileErrors() throws Exception {
        Path cached = temporaryDirectory.resolve("packages/preview/nfr-test/1.0.0");
        java.nio.file.Files.createDirectories(cached);
        java.nio.file.Files.writeString(cached.resolve("typst.toml"),
                "[package]\nname = \"nfr-test\"\nversion = \"1.0.0\"\nentrypoint = \"lib.typ\"\n");
        java.nio.file.Files.writeString(cached.resolve("lib.typ"), "#let greeting = [Hello]");
        try (TypstEngine engine = TypstEngine.open(temporaryDirectory)) {
            engine.installPackage("@preview/nfr-test:1.0.0");
            TypstRaster raster = engine.render("#set page(width: auto, height: auto, margin: 0pt)\n"
                    + "#import \"@preview/nfr-test:1.0.0\": greeting; #greeting", 1);
            assertTrue(raster.width() > 0);
            assertTrue(engine.pollEvents().stream().anyMatch(e -> e.state().equals("ready")));
            assertTrue(engine.pollEvents().isEmpty(), "events are consumed once");
            assertThrows(IllegalStateException.class, () -> engine.render("#unknown-function()", 1));
            assertTrue(engine.pollEvents().stream().anyMatch(e -> e.failed() && e.detail().contains("unknown")));
            assertThrows(IllegalStateException.class, () -> engine.installPackage("@local/test:1.0.0"));
        }
    }

    @Test
    void pollingDoesNotWaitForTheJavaCompilerMonitor() throws Exception {
        try (TypstEngine engine = TypstEngine.open(temporaryDirectory)) {
            synchronized (engine) {
                var poll = java.util.concurrent.CompletableFuture.supplyAsync(engine::pollEvents);
                assertTrue(poll.get(2, java.util.concurrent.TimeUnit.SECONDS).isEmpty());
            }
        }
    }
}
