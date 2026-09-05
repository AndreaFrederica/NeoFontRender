package neofontrender.text;

import org.junit.jupiter.api.Test;
import neofontrender.text.pipeline.StructuredTextRewriter;
import neofontrender.text.syntax.TextSyntaxEngine;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InlineRenderOptionsTest {
    @Test
    void parsesSharedRowsColumnsSamplingAndAlignment() {
        String source = "[rows=2,columns=12,max-width=10em,supersample=4,"
                + "align=center,flow=block]tail";
        InlineRenderOptions options = InlineRenderOptions.parse(
                source, 0, InlineLayout.oneLine(), 2.0F);

        assertEquals(source.indexOf(']') + 1, options.end());
        assertEquals(2.0F, options.layout().rows());
        assertEquals(12.0F, options.layout().columns());
        assertEquals(10.0F, options.layout().maximumColumns());
        assertEquals(4.0F, options.supersample());
        assertEquals(InlineLayout.Alignment.CENTER, options.layout().alignment());
        assertEquals(InlineLayout.Flow.BLOCK, options.layout().flow());
    }

    @Test
    void preservesAspectRatioForAutomaticWidthAndMaximum() {
        InlineLayout layout = new InlineLayout(2.0F, Float.NaN, 3.0F,
                InlineLayout.Alignment.BASELINE);
        InlineLayout.Size size = layout.resolve(new InlineRaster(40, 10, new int[400]), 10.0F);

        assertEquals(30.0F, size.width());
        assertEquals(7.5F, size.height());
    }

    @Test
    void legacyFixedHeightDoesNotScaleWithTheActiveFontSize() {
        InlineLayout layout = InlineLayout.legacy(48, false);
        InlineLayout.Size size = layout.resolve(new InlineRaster(80, 40, new int[3200]), 8.5F);

        assertEquals(48.0F, size.height());
        assertEquals(96.0F, size.width());
    }

    @Test
    void supportsLegacyScaleAndRejectsUnknownSuffixes() {
        InlineRenderOptions scaled = InlineRenderOptions.parse(
                "[scale=1.5]", 0, InlineLayout.oneLine(), 2.0F);
        assertEquals(1.5F, scaled.layout().rows());
        assertTrue(scaled.layout().automaticWidth());

        InlineRenderOptions ignored = InlineRenderOptions.parse(
                "[unknown=1]", 0, InlineLayout.oneLine(), 2.0F);
        assertEquals(0, ignored.end());
        assertTrue(ignored.layout().automaticWidth());
    }

    @Test
    void blockFlowProducesAtomicMappedLineBoundaries() {
        StructuredText input = TextSyntaxEngine.builder().build().parse("before TOKEN after");
        InlineLayout layout = new InlineLayout(2.0F, Float.NaN, Float.NaN,
                InlineLayout.Alignment.BASELINE, InlineLayout.Flow.BLOCK);
        InlineContent content = new InlineContent("test", "key", "block", false,
                null, Collections.emptyMap(), layout);

        StructuredText result = StructuredTextRewriter.rewrite(input,
                Collections.singletonList(StructuredTextRewriter.inline(7, 12, content)),
                "test:block");

        assertEquals("before \n\uFFFC\n after", result.plainText());
        assertEquals(1, result.inlineSpans().size());
        assertEquals("\uFFFC", result.plainText().substring(
                result.inlineSpans().get(0).start(), result.inlineSpans().get(0).end()));
        assertEquals(7, result.sourceMap().sourceStart(result.inlineSpans().get(0).start()));
        assertEquals(12, result.sourceMap().sourceEnd(result.inlineSpans().get(0).end()));
    }
}
