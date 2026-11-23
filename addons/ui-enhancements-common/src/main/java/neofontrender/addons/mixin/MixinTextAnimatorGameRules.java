package neofontrender.addons.mixin;

import net.minecraft.world.GameRules;
import neofontrender.addons.textanimator.TextAnimatorNamePolicy;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRules.class)
public abstract class MixinTextAnimatorGameRules {
    @Inject(method = "<init>", at = @At("RETURN"))
    private void nfrUi$registerTextAnimatorRule(CallbackInfo callbackInfo) {
        GameRules rules = (GameRules) (Object) this;
        if (!rules.hasRule(TextAnimatorNamePolicy.ANVIL_NAMING_RULE)) {
            rules.addGameRule(TextAnimatorNamePolicy.ANVIL_NAMING_RULE, "false",
                    GameRules.ValueType.BOOLEAN_VALUE);
        }
    }
}
