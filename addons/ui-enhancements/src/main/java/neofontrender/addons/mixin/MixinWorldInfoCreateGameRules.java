package neofontrender.addons.mixin;

import net.minecraft.world.GameRules;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.storage.WorldInfo;
import neofontrender.addons.worldcreation.CreateWorldGameRulesState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

/**
 * Applies the game rules chosen on the create-world screen. The
 * {@code WorldInfo(WorldSettings, String)} constructor only runs when a brand-new save is
 * created (existing saves load via the NBT constructor), and its {@code gameRules} field is
 * initialized at declaration, so the vanilla defaults are ready to be overridden here.
 */
@Mixin(WorldInfo.class)
public abstract class MixinWorldInfoCreateGameRules {
    @Inject(method = "<init>(Lnet/minecraft/world/WorldSettings;Ljava/lang/String;)V",
            at = @At("RETURN"))
    private void nfrUi$applyCreateWorldGameRules(WorldSettings settings, String name, CallbackInfo ci) {
        Map<String, String> pending = CreateWorldGameRulesState.consumePending();
        if (pending == null || pending.isEmpty()) return;
        GameRules rules = ((WorldInfo) (Object) this).getGameRulesInstance();
        for (Map.Entry<String, String> entry : pending.entrySet()) {
            if (rules.hasRule(entry.getKey())) {
                rules.setOrCreateGameRule(entry.getKey(), entry.getValue());
            }
        }
    }
}
