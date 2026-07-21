package neofontrender.mixin;

import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import neofontrender.core.config.NeofontrenderConfig;
import neofontrender.core.font.FontManager;

/**
 * Initialises the replacement font system once the game client is ready.
 */
@Mixin(Minecraft.class)
public class MixinMinecraft {

    @Inject(method = "startGame", at = @At("RETURN"))
    private void sfr$onStartGame(CallbackInfo ci) {
        Minecraft mc = Minecraft.getMinecraft();
        if (!NeofontrenderConfig.isLoaded()) {
            NeofontrenderConfig.load();
        }
        if (mc.getTextureManager() != null) {
            FontManager.INSTANCE.init(mc.getTextureManager());
        }
        if (mc.getResourceManager() != null) {
            FontManager.INSTANCE.reload(mc.getResourceManager());
        }
    }
}
