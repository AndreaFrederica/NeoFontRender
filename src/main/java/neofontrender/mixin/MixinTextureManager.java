package neofontrender.mixin;

import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.ITextureObject;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.util.ResourceLocation;
import neofontrender.core.config.NeofontrenderConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Tweaks texture parameters for vanilla font pages so that linear interpolation
 * (LERP) can be forced on them when requested by the user.
 */
@Mixin(TextureManager.class)
public class MixinTextureManager {

    @Inject(
            method = "loadTexture",
            at = @At("RETURN")
    )
    private void sfr$onLoadTexture(ResourceLocation resourceLoc, ITextureObject textureObj,
                                   CallbackInfoReturnable<Boolean> cir) {
        if (!Boolean.TRUE.equals(cir.getReturnValue()) || !NeofontrenderConfig.isLoaded()
                || resourceLoc == null || resourceLoc.getPath() == null
                || !(textureObj instanceof AbstractTexture)) {
            return;
        }
        String path = resourceLoc.getPath();
        if (path.startsWith("textures/font/") || path.startsWith("font/")) {
            if (NeofontrenderConfig.useVanillaEngine()) {
                ((AbstractTexture) textureObj).setBlurMipmap(false, false);
                return;
            }
            ((AbstractTexture) textureObj).setBlurMipmap(
                    NeofontrenderConfig.renderingInterpolation(),
                    NeofontrenderConfig.renderingMipmap());
        }
    }
}
