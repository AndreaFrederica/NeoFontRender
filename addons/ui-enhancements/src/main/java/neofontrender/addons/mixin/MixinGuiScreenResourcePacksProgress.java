package neofontrender.addons.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreenResourcePacks;
import neofontrender.addons.loading.ResourceReloadRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(GuiScreenResourcePacks.class)
public abstract class MixinGuiScreenResourcePacksProgress {
    @Redirect(method = "actionPerformed", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/Minecraft;refreshResources()V"))
    private void nfrUi$showResourcePackReload(Minecraft minecraft) {
        ResourceReloadRenderer.INSTANCE.run(ResourceReloadRenderer.Operation.RESOURCE_PACKS,
                minecraft::refreshResources);
    }
}
