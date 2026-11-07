package neofontrender.typst.pipeline;

import neofontrender.text.InlineContent;
import neofontrender.text.StructuredText;
import neofontrender.text.pipeline.StructuredTextPipeline;
import neofontrender.text.syntax.TextSyntaxEngine;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TypstPipelinePluginTest {
    @TempDir Path temporaryDirectory;

    @Test
    void failedImportCanBeRetriedAfterRepairingThePackage() throws Exception {
        Path cached = temporaryDirectory.resolve("packages/preview/nfr-retry/1.0.0");
        java.nio.file.Files.createDirectories(cached);
        java.nio.file.Files.writeString(cached.resolve("typst.toml"),
                "[package]\nname = \"nfr-retry\"\nversion = \"1.0.0\"\nentrypoint = \"lib.typ\"\n");
        java.nio.file.Files.writeString(cached.resolve("lib.typ"), "#unknown-function()");
        try (TypstPipelinePlugin plugin = new TypstPipelinePlugin(
                new TypstPipelinePlugin.Config(() -> temporaryDirectory, () -> true,
                        () -> 1.0D, () -> 4096, () -> {}))) {
            StructuredTextPipeline pipeline = new StructuredTextPipeline(
                    TextSyntaxEngine.builder().build(), plugin.structuredMiddlewares());
            String source = "<typst:#include \"@preview/nfr-retry:1.0.0\"</typst>";
            InlineContent failed = awaitStatus(pipeline, source, "failed");
            assertTrue(failed.attributes().get("error").contains("unknown"));
            assertTrue(plugin.pollEvents().stream().anyMatch(e -> e.failed()));
            java.nio.file.Files.writeString(cached.resolve("lib.typ"), "Fixed");
            plugin.retryFailed();
            assertTrue(awaitStatus(pipeline, source, "ready").resolved());
        }
    }

    private static InlineContent awaitStatus(StructuredTextPipeline pipeline, String source, String expected)
            throws Exception {
        long deadline = System.nanoTime() + 10_000_000_000L;
        InlineContent content;
        do {
            content = pipeline.parse(source).inlineSpans().getFirst().content();
            if (expected.equals(content.attributes().get("status"))) return content;
            Thread.sleep(10);
        } while (System.nanoTime() < deadline);
        throw new AssertionError(content.attributes());
    }

    @Test
    void publishesNativeRasterThroughStructuredMiddleware() throws Exception {
        AtomicInteger invalidations = new AtomicInteger();
        try (TypstPipelinePlugin plugin = new TypstPipelinePlugin(
                new TypstPipelinePlugin.Config(() -> temporaryDirectory, () -> true,
                        () -> 2.0D, () -> 4096, invalidations::incrementAndGet))) {
            StructuredTextPipeline pipeline = new StructuredTextPipeline(
                    TextSyntaxEngine.builder().build(), plugin.structuredMiddlewares());
            String source = "before <typst:$ x^2 $>"
                    + "[rows=2,columns=9,supersample=4,align=center] after";
            StructuredText parsed = pipeline.parse(source);
            assertEquals(1, parsed.inlineSpans().size());
            assertEquals("typst", parsed.inlineSpans().get(0).content().kind());
            assertEquals("loading", parsed.inlineSpans().get(0).content()
                    .attributes().get("status"));
            assertEquals(2.0F, parsed.inlineSpans().get(0).content().layout().rows());
            assertEquals(9.0F, parsed.inlineSpans().get(0).content().layout().columns());
            assertEquals("4.0", parsed.inlineSpans().get(0).content()
                    .attributes().get("supersample"));

            long deadline = System.nanoTime() + 10_000_000_000L;
            while (!parsed.inlineSpans().get(0).content().resolved()
                    && System.nanoTime() < deadline) {
                Thread.sleep(10L);
                parsed = pipeline.parse(source);
            }
            InlineContent content = parsed.inlineSpans().get(0).content();
            assertTrue(content.resolved(), content.attributes().toString());
            assertNotNull(content.raster());
            assertTrue(content.raster().width() > 0);
            assertTrue(content.raster().height() > 0);
            assertFalse(parsed.appliedMiddlewareIds().isEmpty());
            assertTrue(invalidations.get() > 0);

            StructuredText cached = pipeline.parse(source);
            assertSame(content.raster(), cached.inlineSpans().get(0).content().raster());
        }
    }

    @Test
    void includesGuiScaleInTypstRasterDensity() throws Exception {
        try (TypstPipelinePlugin plugin = new TypstPipelinePlugin(
                new TypstPipelinePlugin.Config(() -> temporaryDirectory, () -> true,
                        () -> 2.0D, () -> 4096, () -> {}, () -> 2.0D))) {
            StructuredTextPipeline pipeline = new StructuredTextPipeline(
                    TextSyntaxEngine.builder().build(), plugin.structuredMiddlewares());
            String source = "<typst:$x^2$>";
            InlineContent content;
            long deadline = System.nanoTime() + 10_000_000_000L;
            do {
                content = pipeline.parse(source).inlineSpans().getFirst().content();
                if ("ready".equals(content.attributes().get("status"))) break;
                Thread.sleep(10L);
            } while (System.nanoTime() < deadline);
            assertEquals("4.0", content.attributes().get("supersample"));
            assertTrue(content.resolved(), content.attributes().toString());
        }
    }

    @Test
    void adaptsGutterToKeepScriptGlyphsInsideRaster() throws Exception {
        try (TypstPipelinePlugin plugin = new TypstPipelinePlugin(
                new TypstPipelinePlugin.Config(() -> temporaryDirectory, () -> true,
                        () -> 2.0D, () -> 4096, () -> {}))) {
            StructuredTextPipeline pipeline = new StructuredTextPipeline(
                    TextSyntaxEngine.builder().build(), plugin.structuredMiddlewares());
            String source = "<typst:$x_2$>";
            InlineContent content;
            long deadline = System.nanoTime() + 10_000_000_000L;
            do {
                content = pipeline.parse(source).inlineSpans().getFirst().content();
                if ("ready".equals(content.attributes().get("status"))) break;
                Thread.sleep(10L);
            } while (System.nanoTime() < deadline);
            assertTrue(content.resolved(), content.attributes().toString());
            int width = content.raster().width();
            int height = content.raster().height();
            int[] argb = content.raster().argb();
            int minX = width, minY = height, maxX = -1, maxY = -1;
            for (int y = 0; y < height; y++) for (int x = 0; x < width; x++) {
                if ((argb[y * width + x] >>> 24) == 0) continue;
                minX = Math.min(minX, x); minY = Math.min(minY, y);
                maxX = Math.max(maxX, x); maxY = Math.max(maxY, y);
            }
            assertTrue(minX >= 2 && minY >= 2 && width - 1 - maxX >= 2
                    && height - 1 - maxY >= 2,
                    "adaptive gutter must leave two transparent texels: "
                            + minX + "," + minY + ".." + maxX + "," + maxY
                            + " in " + width + "x" + height);
        }
    }
}
