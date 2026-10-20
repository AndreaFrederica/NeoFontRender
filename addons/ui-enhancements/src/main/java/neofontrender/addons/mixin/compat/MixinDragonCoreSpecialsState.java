package neofontrender.addons.mixin.compat;

import net.minecraftforge.client.event.RenderLivingEvent;
import neofontrender.addons.hud.compositor.HudGlStateSnapshot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Isolates DragonCore's custom entity name/health-bar event renderer. */
@Pseudo
@Mixin(targets = "eos.moe.dragoncore.np", remap = false)
public abstract class MixinDragonCoreSpecialsState {
    private static final ThreadLocal<HudGlStateSnapshot> nfrUi$state = new ThreadLocal<>();

    @Inject(method = "ALLATORIxDEMO(Lnet/minecraftforge/client/event/RenderLivingEvent$Specials$Pre;)V",
            at = @At("HEAD"), remap = false, require = 0)
    private static void nfrUi$captureSpecials(RenderLivingEvent.Specials.Pre<?> event, CallbackInfo ci) {
        nfrUi$state.set(HudGlStateSnapshot.capture());
    }

    @Inject(method = "ALLATORIxDEMO(Lnet/minecraftforge/client/event/RenderLivingEvent$Specials$Pre;)V",
            at = @At("RETURN"), remap = false, require = 0)
    private static void nfrUi$restoreSpecials(RenderLivingEvent.Specials.Pre<?> event, CallbackInfo ci) {
        HudGlStateSnapshot snapshot = nfrUi$state.get();
        nfrUi$state.remove();
        if (snapshot != null) snapshot.close();
    }
}
