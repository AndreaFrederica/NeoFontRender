package neofontrender.addons.mixin;

import net.minecraft.client.Minecraft;
import neofontrender.addons.loading.ResourceReloadRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * 1.7.10's language list calls Minecraft.refreshResources directly; there is no selective
 * FMLClientHandler.reload entry point to wrap like on 1.12.
 */
@Mixin(targets = "net.minecraft.client.gui.GuiLanguage$List")
public abstract class MixinGuiLanguageResourceReload {
    @Redirect(method = "elementClicked", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/Minecraft;refreshResources()V"))
    private void nfrUi$showLanguageReload(Minecraft minecraft) {
        ResourceReloadRenderer.INSTANCE.run(ResourceReloadRenderer.Operation.LANGUAGE,
                minecraft::refreshResources);
    }
}
