package neofontrender.addons.mixin;

import net.minecraftforge.client.resource.IResourceType;
import net.minecraftforge.fml.client.FMLClientHandler;
import neofontrender.addons.loading.ResourceReloadRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(targets = "net.minecraft.client.gui.GuiLanguage$List")
public abstract class MixinGuiLanguageResourceReload {
    @Redirect(method = "elementClicked", at = @At(value = "INVOKE",
            target = "Lnet/minecraftforge/fml/client/FMLClientHandler;refreshResources([Lnet/minecraftforge/client/resource/IResourceType;)V",
            remap = false))
    private void nfrUi$showLanguageReload(FMLClientHandler handler, IResourceType[] inclusion) {
        ResourceReloadRenderer.INSTANCE.run(ResourceReloadRenderer.Operation.LANGUAGE,
                () -> handler.refreshResources(inclusion));
    }
}
