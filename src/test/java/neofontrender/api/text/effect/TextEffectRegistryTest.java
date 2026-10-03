package neofontrender.api.text.effect;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TextEffectRegistryTest {
    @Test
    void particleModeIsIndependentFromShaderType() {
        TextEffectDefinition neon = TextEffectRegistry.get("textanimator:neon");
        TextEffectDefinition flame = TextEffectRegistry.get("brilliant_text:flame");

        assertEquals(4.0F, neon.shaderType());
        assertEquals(TextEffectParticleMode.NONE, neon.particleMode());
        assertEquals(3.0F, flame.shaderType());
        assertEquals(TextEffectParticleMode.FLAME, flame.particleMode());
    }

    @Test
    void customEffectsDefaultToNoParticles() {
        TextEffectDefinition custom = new TextEffectDefinition() {
            @Override public String id() { return "test:custom"; }
            @Override public float shaderType() { return 99.0F; }
        };

        assertEquals(TextEffectParticleMode.NONE, custom.particleMode());
    }
}
