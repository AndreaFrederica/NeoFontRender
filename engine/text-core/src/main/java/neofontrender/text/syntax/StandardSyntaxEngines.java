package neofontrender.text.syntax;

/** Factory for common game-independent syntax-engine configurations. */
public final class StandardSyntaxEngines {
    private StandardSyntaxEngines() {}

    public static TextSyntaxEngine minecraft() {
        return TextSyntaxEngine.builder()
                .register(MinecraftLegacySyntaxProvider.INSTANCE)
                .build();
    }

    public static TextSyntaxEngine minecraftWithBrilliantDefaults() {
        return TextSyntaxEngine.builder()
                .register(BrilliantSyntaxProvider.defaults())
                .register(MinecraftLegacySyntaxProvider.INSTANCE)
                .build();
    }

    /** Standard client syntax with both independent animation compatibility providers. */
    public static TextSyntaxEngine minecraftWithAnimationCompatibility() {
        return TextSyntaxEngine.builder()
                .register(new TextAnimatorCompatibilityProvider())
                .register(BrilliantSyntaxProvider.defaults())
                .register(MinecraftLegacySyntaxProvider.INSTANCE)
                .build();
    }
}
