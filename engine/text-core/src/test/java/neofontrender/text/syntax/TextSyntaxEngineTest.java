package neofontrender.text.syntax;

import neofontrender.text.StructuredEffectSpan;
import neofontrender.text.StructuredText;
import neofontrender.text.StyledSpan;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class TextSyntaxEngineTest {
    private final TextSyntaxEngine engine = StandardSyntaxEngines.minecraftWithBrilliantDefaults();

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
