package neofontrender.addons.mixin;

import net.minecraft.client.Minecraft;
import neofontrender.addons.loading.ResourceReloadRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class MixinMinecraftResourceReloadProgress {
    @Inject(method = "refreshResources", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/resources/LanguageManager;parseLanguageMetadata(Ljava/util/List;)V"))
    private void nfrUi$showLanguageMetadataPhase(CallbackInfo ci) {
        ResourceReloadRenderer.INSTANCE.languageMetadataPhase();
    }

    @Inject(method = "refreshResources", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/RenderGlobal;loadRenderers()V"))
    private void nfrUi$showRendererRefreshPhase(CallbackInfo ci) {
        ResourceReloadRenderer.INSTANCE.rendererRefreshPhase();
    }
}
