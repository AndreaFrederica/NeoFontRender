package neofontrender.addons.mixin;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiBossOverlay;
import net.minecraft.world.BossInfo;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import neofontrender.addons.flight.FlightHudOverlayController;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Keeps BOSSHEALTH/BOSSINFO events active while hiding only Minecraft's bar and label. */
@Mixin(GuiBossOverlay.class)
public abstract class MixinGuiBossOverlayFlightHud {
    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void nfrUi$suppressVanillaBossBar(int x, int y, BossInfo info,
                                              CallbackInfo callback) {
        if (hideVanillaBossBars()) callback.cancel();
    }

    @Redirect(method = "renderBossHealth",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/FontRenderer;drawStringWithShadow(Ljava/lang/String;FFI)I"),
            require = 1)
    private int nfrUi$suppressVanillaBossLabel(FontRenderer font, String text,
                                                float x, float y, int color) {
        return hideVanillaBossBars() ? 0 : font.drawStringWithShadow(text, x, y, color);
    }

    private static boolean hideVanillaBossBars() {
        return FlightHudOverlayController.shouldSuppressVanilla(
                RenderGameOverlayEvent.ElementType.BOSSHEALTH);
    }
}
