package neofontrender.text.syntax;

import neofontrender.text.StructuredEffectSpan;
import neofontrender.text.StructuredText;
import neofontrender.text.StyledSpan;
import neofontrender.text.edit.SourceEditProjection;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class TextSyntaxEngineTest {
    private final TextSyntaxEngine engine = StandardSyntaxEngines.minecraftWithBrilliantDefaults();

    @org.junit.jupiter.api.Test
    void disabledProviderLeavesSourceAndDoesNotAdvertiseIt() {
        TextSyntaxEngine disabled = TextSyntaxEngine.builder()
                .register(new BrilliantSyntaxProvider(BrilliantSyntaxProvider.defaults().codes().stream()
                        .collect(java.util.stream.Collectors.toMap(c -> c, c -> java.util.Collections.emptyMap())), false, false))
                .register(new MinecraftLegacySyntaxProvider(
                        new int[16], false))
                .build();
        StructuredText parsed = disabled.parse("\u00A7vraw");
        org.junit.jupiter.api.Assertions.assertEquals("§vraw", parsed.plainText());
        org.junit.jupiter.api.Assertions.assertFalse(disabled.providerIds().contains("brilliant_text:format_codes"));
        assertTrue(SourceEditProjection.of(parsed).spans().stream()
                .noneMatch(span -> "unresolved".equals(span.kind())));
    }

    @Test
    void parsesEveryMinecraftStyleIntoStructuredSpans() {
        StructuredText text = engine.parse("\u00A70A\u00A7kB\u00A7lC\u00A7mD\u00A7nE\u00A7oF\u00A7rG");

        assertEquals("ABCDEFG", text.plainText());
        assertTrue(text.styleAt(1).obfuscated());
        assertTrue(text.styleAt(2).bold());
        assertTrue(text.styleAt(3).strikethrough());
        assertTrue(text.styleAt(4).underline());
        assertTrue(text.styleAt(5).italic());
        assertEquals(neofontrender.text.TextStyle.DEFAULT, text.styleAt(6));
        assertTrue(text.animated());
        assertEquals(Collections.singletonList("minecraft:legacy_formatting"),
                text.appliedSyntaxProviderIds());
    }

    @Test
    void resetThenBlackThenObfuscatedProducesExpectedRuns() {
        StructuredText text = engine.parse("\u00A7r\u00A70k \u00A7kMinecraft");

        assertEquals("k Minecraft", text.plainText());
        assertEquals(0, text.styleAt(0).rgb());
        assertFalse(text.styleAt(0).obfuscated());
        assertTrue(text.styleAt(2).obfuscated());
    }

    @Test
    void brilliantSurvivesStylesButEndsAtColorOrReset() {
        StructuredText text = engine.parse("\u00A7g\u00A7lGold\u00A7n!\u00A7cRed");

        assertEquals("Gold!Red", text.plainText());
        assertEquals(1, text.effects().size());
        StructuredEffectSpan effect = text.effects().get(0);
        assertEquals(0, effect.start());
        assertEquals(5, effect.end());
        assertTrue(text.styleAt(0).bold());
        assertTrue(text.styleAt(4).underline());
        assertEquals(0xFF5555, text.styleAt(5).rgb());
        assertEquals(java.util.Arrays.asList("brilliant_text:format_codes",
                        "minecraft:legacy_formatting"),
                text.appliedSyntaxProviderIds());
    }

    @Test
    void brilliantStartsAtLineLeadingPositionByDefault() {
        TextSyntaxEngine strict = TextSyntaxEngine.builder()
                .register(BrilliantSyntaxProvider.defaults())
                .register(new MinecraftLegacySyntaxProvider(new int[16], false))
                .build();
        StructuredText started = strict.parse("Text \u00A7gNo");
        StructuredText switched = strict.parse("  \u00A7gGold\u00A7sSilver");

        assertTrue(started.effects().isEmpty());
        assertEquals("Text \u00A7gNo", started.plainText());
        assertEquals(2, switched.effects().size());
        assertEquals("brilliant_text:g", switched.effects().get(0).effectId());
        assertEquals("brilliant_text:s", switched.effects().get(1).effectId());
    }

    @Test
    void brilliantAnyPositionModeAllowsInlineStart() {
        TextSyntaxEngine inline = TextSyntaxEngine.builder()
                .register(new BrilliantSyntaxProvider(java.util.Collections.singletonMap('g',
                        java.util.Collections.emptyMap()), true))
                .register(MinecraftLegacySyntaxProvider.INSTANCE).build();
        assertEquals(1, inline.parse("Text \u00A7gNo").effects().size());
    }

    @Test
    void parsesTextAnimatorNestedTagsAndParameters() {
        TextSyntaxEngine value = TextSyntaxEngine.builder()
                .register(new TextAnimatorCompatibilityProvider(true, true)).build();
        StructuredText text = value.parse("<wave a=1 f=1.0>Hi <rainb>there</rainb></wave>");
        assertEquals("Hi there", text.plainText());
        assertEquals(2, text.effects().size());
        assertEquals("textanimator:rainb", text.effects().get(0).effectId());
        assertEquals("textanimator:wave", text.effects().get(1).effectId());
        assertEquals("1.0", text.effects().get(1).parameters().get("f"));
    }

    @Test
    void textAnimatorAllowsSameEffectNesting() {
        TextSyntaxEngine value = TextSyntaxEngine.builder()
                .register(new TextAnimatorCompatibilityProvider(true, true)).build();
        StructuredText text = value.parse("<wave a=1>A<wave a=2>B</wave>C</wave>");
        assertEquals("ABC", text.plainText());
        assertEquals(2, text.effects().size());
        assertEquals(1, text.effects().get(0).start());
        assertEquals(2, text.effects().get(0).end());
        assertEquals("2", text.effects().get(0).parameters().get("a"));
        assertEquals(0, text.effects().get(1).start());
        assertEquals(3, text.effects().get(1).end());
    }

    @Test
    void textAnimatorDoesNotAnimateWhitespaceBeforeAnInlineTag() {
        TextSyntaxEngine value = TextSyntaxEngine.builder()
                .register(new TextAnimatorCompatibilityProvider(true, true)).build();
        StructuredText text = value.parse("  <wave>X</wave>");
        assertEquals("  X", text.plainText());
        assertEquals(2, text.effects().get(0).start());
    }

    @Test
    void textAnimatorLeavesMismatchedClosingTagLiteral() {
        TextSyntaxEngine value = TextSyntaxEngine.builder()
                .register(new TextAnimatorCompatibilityProvider(true, true)).build();
        StructuredText text = value.parse("<wave><rainb>X</wave>Y</rainb>");
        assertEquals("X</wave>Y", text.plainText());
        assertEquals(2, text.effects().size());
        assertEquals("textanimator:rainb", text.effects().get(0).effectId());
        assertEquals("textanimator:wave", text.effects().get(1).effectId());
    }

    @Test
    void disabledTextAnimatorLeavesTagsLiteral() {
        TextSyntaxEngine value = TextSyntaxEngine.builder()
                .register(new TextAnimatorCompatibilityProvider(false, true)).build();
        assertEquals("<wave>raw</wave>", value.parse("<wave>raw</wave>").plainText());
    }

    @Test
    void textAnimatorEffectFilterParsesDisabledTagsWithoutAnimatingThem() {
        TextSyntaxEngine value = TextSyntaxEngine.builder()
                .register(new TextAnimatorCompatibilityProvider(true, true, "no_rainbow"))
                .build();
        StructuredText text = value.parse("<rainb>raw</rainb> <wave>moving</wave>");
        assertEquals("raw moving", text.plainText());
        assertEquals(2, text.effects().size());
        assertEquals(neofontrender.text.animation.TextAnimationRenderMode.WHOLE_RUN,
                text.effects().get(0).animationRenderMode());
        assertEquals(neofontrender.text.animation.TextAnimationRenderMode.GLYPH,
                text.effects().get(1).animationRenderMode());
    }

    @Test
    void textAnimatorNoneStillConsumesEverySupportedTag() {
        TextSyntaxEngine value = TextSyntaxEngine.builder()
                .register(new TextAnimatorCompatibilityProvider(true, true, "none"))
                .build();
        StructuredText text = value.parse("<wave>A</wave><grad>B</grad><neon>C</neon>");
        assertEquals("ABC", text.plainText());
        assertFalse(text.animated());
        assertTrue(text.effects().stream().allMatch(effect -> effect.animationRenderMode()
                == neofontrender.text.animation.TextAnimationRenderMode.WHOLE_RUN));
    }

    @Test
    void noRainbowDisablesGradientAsWellAsRainbow() {
        TextSyntaxEngine value = TextSyntaxEngine.builder()
                .register(new TextAnimatorCompatibilityProvider(true, true, "no_rainbow"))
                .build();
        StructuredText text = value.parse("<rainb>A</rainb><grad>B</grad><wave>C</wave>");
        assertEquals(neofontrender.text.animation.TextAnimationRenderMode.WHOLE_RUN,
                text.effects().get(0).animationRenderMode());
        assertEquals(neofontrender.text.animation.TextAnimationRenderMode.WHOLE_RUN,
                text.effects().get(1).animationRenderMode());
        assertEquals(neofontrender.text.animation.TextAnimationRenderMode.GLYPH,
                text.effects().get(2).animationRenderMode());
    }

    @Test
    void brilliantEffectsRemainWholeRunPostProcesses() {
        StructuredText text = engine.parse("\u00A7gGold");
        assertEquals(neofontrender.text.animation.TextAnimationRenderMode.WHOLE_RUN,
                text.effects().get(0).animationRenderMode());
    }

    @Test
    void typewriterProviderInjectsConfiguredModeAndSpeed() {
        TextSyntaxEngine value = TextSyntaxEngine.builder()
                .register(new TextAnimatorCompatibilityProvider(true, true, "all", 4, "by_word"))
                .build();
        StructuredText text = value.parse("<typewriter>one two</typewriter>");
        assertEquals("4", text.effects().get(0).parameters().get("speed"));
        assertEquals("by_word", text.effects().get(0).parameters().get("mode"));
    }

    @Test
    void textAnimatorAnyPositionAllowsTypewriterAfterChatPrefix() {
        TextSyntaxEngine value = TextSyntaxEngine.builder()
                .register(new TextAnimatorCompatibilityProvider(true, true)).build();
        StructuredText text = value.parse("<Player> <typewriter>Hello</typewriter>");
        assertEquals("<Player> Hello", text.plainText());
        assertEquals(1, text.effects().size());
        assertEquals("textanimator:typewriter", text.effects().get(0).effectId());
    }

    @Test
    void textAnimatorLeadingOnlyLeavesInlineTypewriterLiteral() {
        TextSyntaxEngine value = TextSyntaxEngine.builder()
                .register(new TextAnimatorCompatibilityProvider(true, false)).build();
        StructuredText text = value.parse("<Player> <typewriter>Hello</typewriter>");
        assertEquals("<Player> <typewriter>Hello</typewriter>", text.plainText());
        assertTrue(text.effects().isEmpty());
    }

    @Test
    void sourceMapIncludesConsumedLeadingControls() {
        StructuredText text = engine.parse("\u00A7gHello\u00A7rWorld");

        assertEquals("HelloWorld", text.plainText());
        assertEquals(0, text.sourceMap().sourceStart(0));
        assertEquals(2, text.sourceMap().sourceEnd(0));
        assertEquals(9, text.sourceMap().sourceEnd(5));
        assertEquals(14, text.sourceMap().sourceEnd(10));
    }

    @Test
    void vanillaCompatibilityConsumesUnknownCodesAndResetsToWhite() {
        StructuredText text = engine.parse("\u00A7cA\u00A7zB\u00A7\u00A7C\u00A7");

        assertEquals("ABC\u00A7", text.plainText());
        assertEquals(0xFF5555, text.styleAt(0).rgb());
        assertEquals(0xFFFFFF, text.styleAt(1).rgb());
        assertEquals(0xFFFFFF, text.styleAt(2).rgb());
    }

    @Test
    void strictCompatibilityLeavesUnknownCodesLiteral() {
        TextSyntaxEngine strict = TextSyntaxEngine.builder()
                .register(new MinecraftLegacySyntaxProvider(new int[16], false))
                .build();
        StructuredText text = strict.parse("\u00A7cA\u00A7zB");
        assertEquals("A\u00A7zB", text.plainText());
    }

    @Test
    void structuredSlicePreservesControlsStylesEffectsAndSourceMap() {
        StructuredText text = engine.parse("\u00A7g\u00A7lGold\u00A7cRed");

        StructuredText slice = text.slice(1, 6);

        assertEquals("oldRe", slice.plainText());
        assertTrue(slice.styleAt(0).bold());
        assertEquals(0xFF5555, slice.styleAt(3).rgb());
        assertEquals(1, slice.effects().size());
        assertEquals(0, slice.effects().get(0).start());
        assertEquals(3, slice.effects().get(0).end());
        assertEquals(neofontrender.text.animation.TextAnimationRenderMode.WHOLE_RUN,
                slice.effects().get(0).animationRenderMode());
        assertEquals(0, slice.sourceMap().sourceStart(0));
        assertEquals(slice.sourceText().length(), slice.sourceMap().sourceEnd(5));
    }

    @Test
    void rejectsDuplicateFixedSyntaxOwnership() {
        Map<Character, Map<String, String>> collision = new LinkedHashMap<>();
        collision.put('k', Collections.emptyMap());
        BrilliantSyntaxProvider invalid = new BrilliantSyntaxProvider(collision);
        // Reserved Minecraft codes are ignored by Brilliant, so force a duplicate provider instead.
        assertThrows(IllegalArgumentException.class, () -> TextSyntaxEngine.builder()
                .register(MinecraftLegacySyntaxProvider.INSTANCE)
                .register(new MinecraftLegacySyntaxProvider(new int[16])));
        assertTrue(invalid.codes().isEmpty());
    }
}
