package neofontrender.addons.compat;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ModCompatRegistryBootstrapTest {
    @Test
    void runtimeMenuRulesDoNotTouchForgeDuringMixinSelection() {
        // No Forge mod discovery has run in this JVM, just as when Minecraft is transformed.
        assertTrue(ModCompatRegistry.shouldApplyMixin(
                "neofontrender.addons.mixin.MixinMinecraft"));
    }

    @Test
    void bootstrapSafeBattleTowersRuleStillAllowsMixinWhenModIsAbsent() {
        assertTrue(ModCompatRegistry.shouldApplyMixin(
                "neofontrender.addons.mixin.MixinMinecraftServerSpawnProgressChunk"));
    }
}
