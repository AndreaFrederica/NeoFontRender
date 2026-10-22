package neofontrender.addons.mixin.compat;

import net.minecraftforge.client.event.RenderLivingEvent;
import neofontrender.addons.hud.compositor.HudGlStateSnapshot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * DragonCore also renders custom player models and health bars through Forge's
 * living-entity events. Those paths are independent of EntityViewComponent and
 * can leak the same fixed-function GL state into the next HUD pass.
 */
@Pseudo
@Mixin(targets = "eos.moe.dragoncore.uha", remap = false)
public abstract class MixinDragonCoreLivingRenderState {
    private final ThreadLocal<Deque<HudGlStateSnapshot>> nfrUi$states =
            ThreadLocal.withInitial(ArrayDeque::new);

    @Inject(method = "onRenderLivingEvent(Lnet/minecraftforge/client/event/RenderLivingEvent$Pre;)V",
            at = @At("HEAD"), remap = false, require = 0)
    private void nfrUi$captureCustomLivingRender(RenderLivingEvent.Pre<?> event, CallbackInfo ci) {
        nfrUi$states.get().push(HudGlStateSnapshot.capture());
    }

    @Inject(method = "onRenderLivingEvent(Lnet/minecraftforge/client/event/RenderLivingEvent$Pre;)V",
            at = @At("RETURN"), remap = false, require = 0)
    private void nfrUi$restoreCustomLivingRender(RenderLivingEvent.Pre<?> event, CallbackInfo ci) {
        nfrUi$restore();
    }

    @Inject(method = "onRenderLivingEvent1(Lnet/minecraftforge/client/event/RenderLivingEvent$Pre;)V",
            at = @At("HEAD"), remap = false, require = 0)
    private void nfrUi$captureLivingHealthBar(RenderLivingEvent.Pre<?> event, CallbackInfo ci) {
        nfrUi$states.get().push(HudGlStateSnapshot.capture());
    }

    @Inject(method = "onRenderLivingEvent1(Lnet/minecraftforge/client/event/RenderLivingEvent$Pre;)V",
            at = @At("RETURN"), remap = false, require = 0)
    private void nfrUi$restoreLivingHealthBar(RenderLivingEvent.Pre<?> event, CallbackInfo ci) {
        nfrUi$restore();
    }

    private void nfrUi$restore() {
        Deque<HudGlStateSnapshot> states = nfrUi$states.get();
        HudGlStateSnapshot snapshot = states.pollFirst();
        if (snapshot != null) snapshot.close();
        if (states.isEmpty()) nfrUi$states.remove();
    }
}
