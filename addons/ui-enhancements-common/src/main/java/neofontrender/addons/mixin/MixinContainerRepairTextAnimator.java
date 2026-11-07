package neofontrender.addons.mixin;

import net.minecraft.inventory.ContainerRepair;
import net.minecraft.world.World;
import neofontrender.addons.textanimator.TextAnimatorNamePolicy;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(ContainerRepair.class)
public abstract class MixinContainerRepairTextAnimator {
    @Shadow @Final private World world;

    @ModifyVariable(method = "updateItemName", at = @At("HEAD"), argsOnly = true)
    private String nfrUi$filterTextAnimatorTags(String name) {
        boolean allowed = world != null && world.getGameRules()
                .getBoolean(TextAnimatorNamePolicy.ANVIL_NAMING_RULE);
        return TextAnimatorNamePolicy.filter(name, allowed);
    }
}
