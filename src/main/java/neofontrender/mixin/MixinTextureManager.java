package neofontrender.mixin;

import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.ITextureObject;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.util.ResourceLocation;
import neofontrender.core.config.NeofontrenderConfig;
import neofontrender.core.font.support.VanillaFontTexturePolicy;
import org.lwjgl.opengl.GL11;
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
                || resourceLoc == null || resourceLoc.getResourcePath() == null
                || !(textureObj instanceof AbstractTexture)) {
            return;
        }
        String path = resourceLoc.getResourcePath();
        if (path.startsWith("textures/font/") || path.startsWith("font/")) {
            if (VanillaFontTexturePolicy.forceNearest(path)
                    || NeofontrenderConfig.useVanillaEngine()) {
                setTextureFiltering(textureObj, false, false);
                return;
            }
            setTextureFiltering(textureObj,
                    NeofontrenderConfig.renderingInterpolation(),
                    NeofontrenderConfig.renderingMipmap());
        }
    }

    private static void setTextureFiltering(ITextureObject texture, boolean blur, boolean mipmap) {
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture.getGlTextureId());
        int minFilter = blur
                ? (mipmap ? GL11.GL_LINEAR_MIPMAP_LINEAR : GL11.GL_LINEAR)
                : (mipmap ? GL11.GL_NEAREST_MIPMAP_LINEAR : GL11.GL_NEAREST);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, minFilter);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER,
                blur ? GL11.GL_LINEAR : GL11.GL_NEAREST);
    }
}
