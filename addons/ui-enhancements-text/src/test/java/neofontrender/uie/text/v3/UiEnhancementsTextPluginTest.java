package neofontrender.uie.text.v3;

import neofontrender.text.InlineSpan;
import neofontrender.text.StructuredText;
import neofontrender.text.pipeline.StructuredTextPipeline;
import neofontrender.text.syntax.StandardSyntaxEngines;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class UiEnhancementsTextPluginTest {
    @TempDir Path temporary;

    @Test
    void everyUiEnhancementsComponentUsesTheStructuredPluginProtocol() throws Exception {
        BufferedImage image = new BufferedImage(12, 8, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setColor(Color.CYAN);
        graphics.fillRect(0, 0, 12, 8);
        graphics.dispose();
        ImageIO.write(image, "png", temporary.resolve("badge.png").toFile());

        UiEnhancementsTextPlugin.Config config = new UiEnhancementsTextPlugin.Config(
                () -> true, () -> true, () -> true, () -> true, () -> true, () -> true,
                temporary, 2.0F, false);
        UiEnhancementsTextPlugin plugin = new UiEnhancementsTextPlugin(config);
        StructuredTextPipeline pipeline = new StructuredTextPipeline(
                StandardSyntaxEngines.minecraftWithBrilliantDefaults(),
                plugin.structuredMiddlewares(), plugin.lineBreakProviders());
        String svg = "<svg xmlns='http://www.w3.org/2000/svg' width='20' height='10'>"
                + "<rect width='20' height='10' fill='#00ff00'/></svg>";
        String svgData = "data:image/svg+xml;base64," + Base64.getEncoder().encodeToString(
                svg.getBytes(StandardCharsets.UTF_8));
        String source = "**bold** $x^2$ <svg:" + svgData
                + "> :badge: <img:https://example.com/a.png> :grinning: 中文。";
        StructuredText result = awaitReady(pipeline, source);

        Set<String> hits = Set.copyOf(result.appliedMiddlewareIds());
        assertTrue(hits.contains(UiEnhancementsTextPlugin.ID + "/markdown"));
        assertTrue(hits.contains(UiEnhancementsTextPlugin.ID + "/latex"));
        assertTrue(hits.contains(UiEnhancementsTextPlugin.ID + "/svg"));
        assertTrue(hits.contains(UiEnhancementsTextPlugin.ID + "/local_image"));
        assertTrue(hits.contains(UiEnhancementsTextPlugin.ID + "/external_image"));
        assertTrue(hits.contains(UiEnhancementsTextPlugin.ID + "/gosling_emoji"));
        assertEquals(5, result.inlineSpans().size());
        assertEquals(3, result.inlineSpans().stream().filter(
                span -> span.content().resolved()).count());
        assertFalse(pipeline.breakOpportunities(result).isEmpty());
        assertEquals(Set.of("latex", "svg", "local_image", "external_image", "gosling_emoji"),
                result.inlineSpans().stream().map(span -> span.content().kind())
                        .collect(Collectors.toSet()));
        assertThrows(ClassNotFoundException.class,
                () -> Class.forName("net.minecraft.client.Minecraft"));
    }

    @Test
    void structuredSliceRetainsInlineObjectAndOriginalSourceBoundary() {
        UiEnhancementsTextPlugin plugin = new UiEnhancementsTextPlugin();
        StructuredTextPipeline pipeline = new StructuredTextPipeline(
                StandardSyntaxEngines.minecraftWithBrilliantDefaults(),
                plugin.structuredMiddlewares(), plugin.lineBreakProviders());
        StructuredText parsed = pipeline.parse("before $a+b$ after");
        InlineSpan span = parsed.inlineSpans().get(0);
        StructuredText sliced = parsed.slice(span.start(), span.end());

        assertEquals("\uFFFC", sliced.plainText());
        assertEquals(1, sliced.inlineSpans().size());
        assertEquals("$a+b$", sliced.sourceText());
    }

    @Test
    void latexLineHeightPolicyComesFromTheRuntimeConfiguration() {
        UiEnhancementsTextPlugin.Config config = new UiEnhancementsTextPlugin.Config(
                () -> false, () -> true, () -> false, () -> false, () -> false, () -> false,
                null, 2.0F, false, () -> false);
        StructuredTextPipeline pipeline = new StructuredTextPipeline(
                StandardSyntaxEngines.minecraftWithBrilliantDefaults(),
                new UiEnhancementsTextPlugin(config).structuredMiddlewares());

        StructuredText result = pipeline.parse("$x^2$[scale=1.5]");

        assertEquals(1, result.inlineSpans().size());
        assertFalse(result.inlineSpans().get(0).content().matchLineHeight());
        assertEquals(21, result.inlineSpans().get(0).content().displayHeight());
    }

    @Test
    void latexUsesSharedLayoutAndSupersampleSuffix() {
        UiEnhancementsTextPlugin plugin = new UiEnhancementsTextPlugin();
        StructuredTextPipeline pipeline = new StructuredTextPipeline(
                StandardSyntaxEngines.minecraftWithBrilliantDefaults(),
                plugin.structuredMiddlewares(), plugin.lineBreakProviders());

        StructuredText result = pipeline.parse(
                "$x^2$[rows=2,columns=8,max-width=12em,supersample=4,align=center]");

        assertEquals(1, result.inlineSpans().size());
        neofontrender.text.InlineContent content = result.inlineSpans().get(0).content();
        assertEquals(2.0F, content.layout().rows());
        assertEquals(8.0F, content.layout().columns());
        assertEquals(12.0F, content.layout().maximumColumns());
        assertEquals(neofontrender.text.InlineLayout.Alignment.CENTER,
                content.layout().alignment());
        assertEquals("4.0", content.attributes().get("supersample"));
        assertEquals("\uFFFC", result.plainText());
    }

    @Test
    void rawUnicodeEmojiFallsThroughToTheFontPipeline() {
        StructuredTextPipeline pipeline = new StructuredTextPipeline(
                StandardSyntaxEngines.minecraftWithBrilliantDefaults(),
                new UiEnhancementsTextPlugin().structuredMiddlewares());

        StructuredText result = pipeline.parse("A\uD83D\uDE00\uFE0F\u4E2D");

        assertEquals("A\uD83D\uDE00\uFE0F\u4E2D", result.plainText());
        assertTrue(result.inlineSpans().isEmpty());
        assertFalse(result.appliedMiddlewareIds().contains(
                UiEnhancementsTextPlugin.ID + "/gosling_emoji"));
    }

    @Test
    void normalizesSafeMinecraftSvgResources() {
        assertEquals("/assets/neofontrender_inline_content_showcase/structures/aminobenzo_18_crown_6.svg",
                UiEnhancementsTextPlugin.classpathSvgResource(
                        " resource:neofontrender_inline_content_showcase:structures\\aminobenzo_18_crown_6.svg "));
        assertNull(UiEnhancementsTextPlugin.classpathSvgResource("resource:test:../formula.svg"));
        assertNull(UiEnhancementsTextPlugin.classpathSvgResource("resource:test:formula.png"));
        assertNull(UiEnhancementsTextPlugin.classpathSvgResource("resource:test:one:formula.svg"));
    }

    @Test
    void typstReplacementFragmentsCannotBecomeLocalImagePaths() {
        UiEnhancementsTextPlugin.Config config = new UiEnhancementsTextPlugin.Config(
                () -> false, () -> false, () -> false, () -> true, () -> false, () -> false,
                temporary, 2.0F, false);
        StructuredTextPipeline pipeline = new StructuredTextPipeline(
                StandardSyntaxEngines.minecraftWithBrilliantDefaults(),
                new UiEnhancementsTextPlugin(config).structuredMiddlewares());
        String residual = "\uFFFC>[rows=2,max-width=16em]\n<typst:#set text(size: 18pt)";

        StructuredText parsed = assertDoesNotThrow(() -> pipeline.parse(residual));

        assertTrue(parsed.inlineSpans().isEmpty());
        assertFalse(parsed.appliedMiddlewareIds().contains(
                UiEnhancementsTextPlugin.ID + "/local_image"));
    }

    @Test
    void standaloneLatexFontResourceIsLoadable() {
        assertNotNull(UiEnhancementsTextPlugin.class.getResourceAsStream(
                "/assets/neofontrender_ui_enhancements/fonts/fira_math-regular.otf"));
        assertEquals("Fira Math", RasterSupport.resolveFamily(
                "neofontrender_ui_enhancements:fonts/fira_math-regular.otf"));
    }

    @Test
    void repeatedLatexEvaluationReusesTheMiddlewareRaster() throws Exception {
        try (UiEnhancementsTextPlugin plugin = new UiEnhancementsTextPlugin()) {
            StructuredTextPipeline pipeline = new StructuredTextPipeline(
                    StandardSyntaxEngines.minecraftWithBrilliantDefaults(),
                    plugin.structuredMiddlewares());
            String source = "$\\frac{x}{y}$[supersample=3]";

            StructuredText first = awaitReady(pipeline, source);
            StructuredText second = pipeline.parse(source);

            assertSame(first.inlineSpans().get(0).content().raster(),
                    second.inlineSpans().get(0).content().raster());
            assertEquals("ready", second.inlineSpans().get(0).content()
                    .attributes().get("status"));
        }
    }

    @Test
    void localGalleryImagesKeepTheirFixedDefaultHeight() throws Exception {
        BufferedImage image = new BufferedImage(40, 20, BufferedImage.TYPE_INT_ARGB);
        ImageIO.write(image, "png", temporary.resolve("wide.png").toFile());
        UiEnhancementsTextPlugin.Config config = new UiEnhancementsTextPlugin.Config(
                () -> false, () -> false, () -> false, () -> true, () -> false, () -> false,
                temporary, 2.0F, false);
        try (UiEnhancementsTextPlugin plugin = new UiEnhancementsTextPlugin(config)) {
            StructuredTextPipeline pipeline = new StructuredTextPipeline(
                    StandardSyntaxEngines.minecraftWithBrilliantDefaults(),
                    plugin.structuredMiddlewares());

            StructuredText parsed = awaitReady(pipeline, ":wide:");
            neofontrender.text.InlineContent content = parsed.inlineSpans().get(0).content();
            neofontrender.text.InlineLayout.Size size = content.layout()
                    .resolve(content.raster(), 9.0F);

            assertEquals(18.0F, size.height());
            assertEquals(36.0F, size.width());
            assertFalse(content.matchLineHeight());
        }
    }

    private static StructuredText awaitReady(StructuredTextPipeline pipeline, String source)
            throws InterruptedException {
        long deadline = System.nanoTime() + 10_000_000_000L;
        StructuredText parsed;
        do {
            parsed = pipeline.parse(source);
            boolean loading = parsed.inlineSpans().stream().anyMatch(
                    span -> "loading".equals(span.content().attributes().get("status")));
            if (!loading) return parsed;
            Thread.sleep(10L);
        } while (System.nanoTime() < deadline);
        fail("Timed out waiting for asynchronous inline rasterization");
        throw new AssertionError("unreachable");
    }
}
