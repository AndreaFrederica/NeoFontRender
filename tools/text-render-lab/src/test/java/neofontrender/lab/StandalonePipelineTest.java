package neofontrender.lab;

import neofontrender.text.StructuredText;
import neofontrender.text.layout.CjkLineBreakProvider;
import neofontrender.text.pipeline.StructuredTextMiddleware;
import neofontrender.text.pipeline.StructuredTextPipeline;
import neofontrender.text.syntax.StandardSyntaxEngines;
import neofontrender.typst.pipeline.TypstPipelinePlugin;
import neofontrender.uie.text.v3.UiEnhancementsTextPlugin;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class StandalonePipelineTest {
    @TempDir Path temporary;

    @Test
    void completeCorePipelineRunsWithoutMinecraftClasses() {
        StructuredText text = StandardSyntaxEngines.minecraftWithBrilliantDefaults()
                .parse("\u00A7g\u00A7lGold\u00A7r \u4e2d\u6587\u3002");

        assertEquals("Gold \u4e2d\u6587\u3002", text.plainText());
        assertFalse(text.effects().isEmpty());
        assertFalse(new CjkLineBreakProvider().opportunities(text).isEmpty());
        assertThrows(ClassNotFoundException.class,
                () -> Class.forName("net.minecraft.client.Minecraft"));
    }

    @Test
    void completeTypstPasteDoesNotLeakFragmentsIntoLocalImagePaths() {
        UiEnhancementsTextPlugin.Config uiConfig = new UiEnhancementsTextPlugin.Config(
                () -> false, () -> false, () -> false, () -> true, () -> false, () -> false,
                temporary.resolve("gallery"), 2.0F, false);
        try (TypstPipelinePlugin typst = new TypstPipelinePlugin()) {
            List<StructuredTextMiddleware> middleware = new ArrayList<>();
            middleware.addAll(new UiEnhancementsTextPlugin(uiConfig).structuredMiddlewares());
            middleware.addAll(typst.structuredMiddlewares());
            StructuredTextPipeline pipeline = new StructuredTextPipeline(
                    StandardSyntaxEngines.minecraftWithBrilliantDefaults(), middleware);
            String source = "<typst:$ integral_0^1 x^2 dif x $>"
                    + "[rows=2,max-width=16em,supersample=4,align=center]\n"
                    + "<typst:$ cases(x^2 & x >= 0, -x & x < 0) $>"
                    + "[rows=2,columns=14,supersample=4]\n"
                    + "<typst:#set text(size: 18pt)\n*Typst block*\n"
                    + "$ sum_(i=1)^n i = (n(n+1))/2 $\n</typst>"
                    + "[flow=block,width=auto,max-width=20em,supersample=4]\n"
                    + "\\<typst:$x^2$>";

            StructuredText parsed = assertDoesNotThrow(() -> pipeline.parse(source));

            assertEquals(3, parsed.inlineSpans().stream()
                    .filter(span -> "typst".equals(span.content().kind())).count());
            assertFalse(parsed.appliedMiddlewareIds().contains(
                    UiEnhancementsTextPlugin.ID + "/local_image"));
        }
    }
}
