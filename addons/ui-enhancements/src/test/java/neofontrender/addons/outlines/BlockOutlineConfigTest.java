package neofontrender.addons.outlines;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BlockOutlineConfigTest {
    @Test
    void editorRulesAcceptCommaAndNewlineSeparatedEntries() {
        assertEquals(Arrays.asList(
                        "minecraft:stone=2;#66000000",
                        "minecraft:glass:0=3;#66FFFFFF"),
                BlockOutlineConfig.parseEditorRules(
                        "minecraft:stone=2;#66000000,\nminecraft:glass:0=3;#66FFFFFF"));
    }

    @Test
    void emptyEditorRulesBecomeAnEmptyList() {
        assertEquals(Collections.emptyList(), BlockOutlineConfig.parseEditorRules("  ,\n  "));
        assertEquals("minecraft:stone=2;#66000000, minecraft:glass=1;#66FFFFFF",
                BlockOutlineConfig.editorRules(Arrays.asList(
                        "minecraft:stone=2;#66000000", "minecraft:glass=1;#66FFFFFF")));
    }
}
