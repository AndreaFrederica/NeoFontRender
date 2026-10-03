package neofontrender.core.font.cosmic;

import neofontrender.text.StructuredText;
import neofontrender.text.syntax.StandardSyntaxEngines;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CosmicAnimatedShadowContractTest {
    @Test
    void animatedTextBypassesStaticNativeShadowComposition() {
        StructuredText animated = StandardSyntaxEngines.minecraftWithAnimationCompatibility()
                .parse("<wave>animated</wave>");
        StructuredText ordinary = StandardSyntaxEngines.minecraftWithAnimationCompatibility()
                .parse("ordinary");

        assertFalse(CosmicTextRenderer.canUseNativeModernShadow(animated));
        assertTrue(CosmicTextRenderer.canUseNativeModernShadow(ordinary));
    }
}
