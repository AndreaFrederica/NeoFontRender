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
}
