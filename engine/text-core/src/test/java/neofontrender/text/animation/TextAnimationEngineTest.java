package neofontrender.text.animation;

import neofontrender.text.StructuredText;
import neofontrender.text.syntax.StandardSyntaxEngines;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

class TextAnimationEngineTest {
    @Test
    void exposesActiveEffectsPerGlyph() {
        StructuredText text = StandardSyntaxEngines.minecraftWithAnimationCompatibility()
                .parse("<wave>Hello</wave>");
        TextAnimationEngine.GlyphAnimationFrame frame = new TextAnimationEngine().frame(text, 42L);
        assertTrue(frame.glyphs().stream().allMatch(TextAnimationEngine.GlyphAnimation::animated));
        assertTrue(frame.timeMillis() == 42L);
    }

    @Test
    void typewriterUsesAStatefulTimelineInsteadOfAbsoluteApplicationTime() {
        StructuredText text = StandardSyntaxEngines.minecraftWithAnimationCompatibility()
                .parse("<typewriter>timeline-unique</typewriter>");
        TextAnimationEngine.GlyphAnimationFrame frame = new TextAnimationEngine().frame(text, 1000L);
        TextAnimationSampler sampler = new TextAnimationSampler();
        assertFalse(sampler.sample(frame.glyphs().get(0), 1000L).visible);
        assertFalse(sampler.sample(frame.glyphs().get(0), 1020L).visible);
        assertTrue(sampler.sample(frame.glyphs().get(0), 1021L).visible);
        assertFalse(sampler.sample(frame.glyphs().get(1), 1021L).visible);
    }

    @Test
    void supplementaryCjkUsesOneAnimationIndexAndKeepsUtf16SpanCoordinates() {
        StructuredText text = StandardSyntaxEngines.minecraftWithAnimationCompatibility()
                .parse("<wave>A\uD840\uDC00中</wave>");
        TextAnimationEngine.GlyphAnimationFrame frame = new TextAnimationEngine().frame(text, 0L);

        assertTrue(frame.glyphs().get(1).codePoint() == 0x20000);
        assertTrue(frame.glyphs().get(1).animationIndex() == 1);
        assertTrue(frame.glyphs().get(3).animationIndex() == 2);
        assertTrue(frame.glyphs().get(3).animated());
    }
}
