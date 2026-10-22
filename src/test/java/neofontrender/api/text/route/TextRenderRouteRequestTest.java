package neofontrender.api.text.route;

import neofontrender.text.InlineContent;
import neofontrender.text.InlineSpan;
import neofontrender.text.SourceMap;
import neofontrender.text.StructuredText;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TextRenderRouteRequestTest {
    @Test
    void vanillaFormattingAloneDoesNotForceCompatibilityRoute() {
        StructuredText text = structured(List.of("minecraft:legacy_formatting"),
                Collections.emptyList(), Collections.emptyList());
        assertFalse(TextRenderRouteRequest.requiresStructuredRendering(text, false));
    }

    @Test
    void inlineOrExternalSyntaxRequiresAConsumerRoute() {
        InlineContent content = new InlineContent("test", "key", "test", 18,
                false, true, null, Collections.emptyMap());
        StructuredText inline = structured(Collections.emptyList(),
                Collections.emptyList(), List.of(new InlineSpan(0, 1, content)));
        assertTrue(TextRenderRouteRequest.requiresStructuredRendering(inline, false));

        StructuredText external = structured(List.of("example:syntax"),
                Collections.emptyList(), Collections.emptyList());
        assertTrue(TextRenderRouteRequest.requiresStructuredRendering(external, false));
    }

    private static StructuredText structured(List<String> providers, List<String> middleware,
                                             List<InlineSpan> inline) {
        return new StructuredText("x", "x", Collections.emptyList(), Collections.emptyList(),
                Collections.emptyList(), new SourceMap(1, new int[]{0, 1}, new int[]{0, 1}),
                providers, inline, middleware);
    }
}
