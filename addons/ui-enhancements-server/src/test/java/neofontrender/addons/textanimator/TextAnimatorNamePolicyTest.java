package neofontrender.addons.textanimator;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TextAnimatorNamePolicyTest {
    @Test
    void stripsOnlyRecognizedAnimationTagsWhenRuleIsDisabled() {
        String source = "<wave a=2>剑</wave> <unknown>kept</unknown> <typewriter>name";
        assertEquals("剑 <unknown>kept</unknown> name",
                TextAnimatorNamePolicy.filter(source, false));
        assertEquals(source, TextAnimatorNamePolicy.filter(source, true));
    }

    @Test
    void keepsMalformedAndIncompleteTagsLiteral() {
        assertEquals("math < 3 and <wave", TextAnimatorNamePolicy.filter(
                "math < 3 and <wave", false));
    }
}
