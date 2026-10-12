package neofontrender.text.pipeline;

import neofontrender.text.StructuredText;
import neofontrender.text.syntax.StandardSyntaxEngines;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StructuredTextRewriterTest {
    @Test
    void removedControlRangeBelongsToTheCurrentPlainBoundary() {
        StructuredText input = StandardSyntaxEngines.minecraft().parse("A#112233B");
        StructuredText result = StructuredTextRewriter.rewrite(input, Arrays.asList(
                StructuredTextRewriter.text(1, 8, "", style -> style),
                StructuredTextRewriter.text(8, 9, "B", style -> style)), "test:rewrite");

        assertEquals("AB", result.plainText());
        assertEquals(1, result.sourceMap().sourceStart(1));
        assertEquals(8, result.sourceMap().sourceEnd(1));
        assertEquals(9, result.sourceMap().sourceEnd(2));
    }
}
