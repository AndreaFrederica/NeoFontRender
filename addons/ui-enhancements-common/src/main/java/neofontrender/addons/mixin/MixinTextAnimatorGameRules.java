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
            // 1.7.10 game rules carry no declared value type; the boolean text below is what the
            // 1.12 ValueType.BOOLEAN_VALUE registered on the newer platform.
            rules.addGameRule(TextAnimatorNamePolicy.ANVIL_NAMING_RULE, "false");
        }
    }
}
