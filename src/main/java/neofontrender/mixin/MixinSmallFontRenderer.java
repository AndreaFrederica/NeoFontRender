package neofontrender.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.util.ResourceLocation;
import neofontrender.core.config.NeofontrenderConfig;
import neofontrender.core.font.FontManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Routes Mantle's independent manual font renderer through Neo Font Render.
 */
@Pseudo
@Mixin(targets = "mantle.client.SmallFontRenderer", remap = false)
public abstract class MixinSmallFontRenderer {
    private static final String NFR$DEFAULT_FONT_TEXTURE =
            "minecraft:textures/font/ascii.png";

    @Shadow @Final private ResourceLocation locationFontTexture;

    @Inject(method = "drawString(Ljava/lang/String;IIIZ)I", at = @At("HEAD"),
            cancellable = true, require = 1, remap = false)
    private void nfr$drawString(String text, int x, int y, int color, boolean dropShadow,
                                CallbackInfoReturnable<Integer> cir) {
        if (nfr$shouldReplace()) {
            cir.setReturnValue(nfr$fontRenderer().drawString(text, x, y, color, dropShadow));
        }
    }

    @Inject(method = "getStringWidth(Ljava/lang/String;)I", at = @At("HEAD"),
            cancellable = true, require = 1, remap = false)
    private void nfr$getStringWidth(String text, CallbackInfoReturnable<Integer> cir) {
        if (nfr$shouldReplace()) {
            cir.setReturnValue(nfr$fontRenderer().getStringWidth(text));
        }
    }

    @Inject(method = "getCharWidth(C)I", at = @At("HEAD"),
            cancellable = true, require = 1, remap = false)
    private void nfr$getCharWidth(char character, CallbackInfoReturnable<Integer> cir) {
        if (nfr$shouldReplace()) {
            cir.setReturnValue(nfr$fontRenderer().getCharWidth(character));
        }
    }

    @Inject(method = "trimStringToWidth(Ljava/lang/String;IZ)Ljava/lang/String;",
            at = @At("HEAD"), cancellable = true, require = 1, remap = false)
    private void nfr$trimStringToWidth(String text, int width, boolean reverse,
                                       CallbackInfoReturnable<String> cir) {
        if (nfr$shouldReplace()) {
            cir.setReturnValue(nfr$fontRenderer().trimStringToWidth(text, width, reverse));
        }
    }

    @Inject(method = "drawSplitString(Ljava/lang/String;IIII)V", at = @At("HEAD"),
            cancellable = true, require = 1, remap = false)
    private void nfr$drawSplitString(String text, int x, int y, int width, int color,
                                     CallbackInfo ci) {
        if (nfr$shouldReplace()) {
            nfr$fontRenderer().drawSplitString(text, x, y, width, color);
            ci.cancel();
        }
    }

    @Inject(method = "splitStringWidth(Ljava/lang/String;I)I", at = @At("HEAD"),
            cancellable = true, require = 1, remap = false)
    private void nfr$splitStringWidth(String text, int width,
                                      CallbackInfoReturnable<Integer> cir) {
        if (nfr$shouldReplace()) {
            cir.setReturnValue(nfr$fontRenderer().splitStringWidth(text, width));
        }
    }

    private boolean nfr$shouldReplace() {
        return NeofontrenderConfig.compatTinkersConstruct()
                && (FontManager.INSTANCE.isSfrActive()
                || FontManager.INSTANCE.isTextBackendActive())
                && NFR$DEFAULT_FONT_TEXTURE.equals(this.locationFontTexture.toString());
    }

    private static FontRenderer nfr$fontRenderer() {
        return Minecraft.getMinecraft().fontRenderer;
    }
}
