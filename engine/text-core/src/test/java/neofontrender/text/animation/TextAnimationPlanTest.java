package neofontrender.text.animation;

import neofontrender.text.StructuredText;
import neofontrender.text.StructuredEffectSpan;
import neofontrender.text.SourceMap;
import neofontrender.text.animation.TextAnimationEngine;
import neofontrender.text.syntax.StandardSyntaxEngines;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collections;

class TextAnimationPlanTest {
    @Test
    void autoSelectsGlyphModeForSimpleLatin() {
        StructuredText text = StandardSyntaxEngines.minecraftWithAnimationCompatibility()
                .parse("<wave>Hello</wave>");
        assertTrue(text.animated());
        assertEquals(TextAnimationRenderMode.GLYPH,
                TextAnimationPlan.forText(text, TextAnimationRenderMode.AUTO).selected());
    }

    @Test
    void emojiKeepsNativeWholeRunShaping() {
        StructuredText text = StandardSyntaxEngines.minecraftWithAnimationCompatibility()
                .parse("<wave>Hi \uD83D\uDE00</wave>");
        TextAnimationPlan plan = TextAnimationPlan.forText(text, TextAnimationRenderMode.GLYPH);
        assertEquals(TextAnimationRenderMode.WHOLE_RUN, plan.selected());
        assertEquals("shaping_sensitive_codepoint", plan.reason());
    }

    @Test
    void clusterAwareBackendCanAnimateEmojiWithoutSplittingItsShapingCluster() {
        StructuredText text = StandardSyntaxEngines.minecraftWithAnimationCompatibility()
                .parse("<wave>Hi \uD83D\uDE00</wave>");
        TextAnimationPlan plan = TextAnimationPlan.forText(text,
                TextAnimationRenderMode.GLYPH, true);
        assertEquals(TextAnimationRenderMode.GLYPH, plan.selected());
        assertEquals("requested_clusters", plan.reason());
    }

    @Test
    void cjkIdeographsRemainEligibleForGlyphAnimation() {
        StructuredText text = StandardSyntaxEngines.minecraftWithAnimationCompatibility()
                .parse("<wave>中文测试漢字かな</wave>");
        assertEquals(TextAnimationRenderMode.GLYPH,
                TextAnimationPlan.forText(text, TextAnimationRenderMode.AUTO).selected());
    }

    @Test
    void supplementaryCjkIdeographIsNotConfusedWithEmoji() {
        StructuredText text = StandardSyntaxEngines.minecraftWithAnimationCompatibility()
                .parse("<wave>\uD840\uDC00</wave>");
        assertEquals(TextAnimationRenderMode.GLYPH,
                TextAnimationPlan.forText(text, TextAnimationRenderMode.AUTO).selected());
    }

    @Test
    void wholeRunEffectsDoNotRequestGlyphAnimation() {
        StructuredText text = new StructuredText("rgbchat", "rgbchat", Collections.emptyList(),
                Collections.singletonList(new StructuredEffectSpan(0, 7, "rgbchat:gradient",
                        Collections.emptyMap(), false, TextAnimationRenderMode.WHOLE_RUN)),
                Collections.emptyList(), new SourceMap(7,
                        new int[]{0, 1, 2, 3, 4, 5, 6, 7},
                        new int[]{0, 1, 2, 3, 4, 5, 6, 7}));
        assertEquals(TextAnimationRenderMode.WHOLE_RUN,
                TextAnimationPlan.forText(text, TextAnimationRenderMode.AUTO).selected());
        assertTrue(new TextAnimationEngine().frame(text, 0).glyphs().stream()
                .allMatch(glyph -> !glyph.animated()));
    }

    @Test
    void glyphAndWholeRunEffectsCanCoexist() {
        StructuredText text = new StructuredText("abcdef", "abcdef", Collections.emptyList(),
                Arrays.asList(
                        new StructuredEffectSpan(0, 3, "rgbchat:gradient", Collections.emptyMap(),
                                false, TextAnimationRenderMode.WHOLE_RUN),
                        new StructuredEffectSpan(3, 6, "textanimator:wave", Collections.emptyMap(),
                                false, TextAnimationRenderMode.GLYPH)),
                Collections.emptyList(), new SourceMap(6,
                        new int[]{0, 1, 2, 3, 4, 5, 6}, new int[]{0, 1, 2, 3, 4, 5, 6}));
        assertEquals(TextAnimationRenderMode.GLYPH,
                TextAnimationPlan.forText(text, TextAnimationRenderMode.AUTO).selected());
        TextAnimationEngine.GlyphAnimationFrame frame = new TextAnimationEngine().frame(text, 0);
        assertTrue(frame.glyphs().get(0).effects().isEmpty());
        assertTrue(frame.glyphs().get(3).animated());
    }
}
