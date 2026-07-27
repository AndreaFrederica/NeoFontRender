package neofontrender.addons.mixin;

import net.minecraft.client.renderer.texture.ITextureObject;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.util.ResourceLocation;
import neofontrender.addons.loading.ResourceReloadRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(TextureManager.class)
public abstract class MixinTextureManagerResourceReloadPulse {
    @Inject(method = "loadTexture", at = {@At("HEAD"), @At("RETURN")})
    private void nfrUi$animateBetweenTextureLoads(ResourceLocation location,
                                                   ITextureObject texture,
                                                   CallbackInfoReturnable<Boolean> cir) {
        ResourceReloadRenderer.INSTANCE.pulse();
    }
}
