package neofontrender.addons.mixin.compat;

import net.minecraft.entity.EntityLivingBase;
import neofontrender.addons.hud.compositor.HudGlStateSnapshot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayDeque;
import java.util.Deque;

/** Restores GL state leaked by DragonCore's EntityViewComponent HUD renderer. */
@Pseudo
@Mixin(targets = "eos.moe.dragoncore.nh", remap = false)
public abstract class MixinDragonCoreEntityViewState {
    private final ThreadLocal<Deque<HudGlStateSnapshot>> nfrUi$states =
            ThreadLocal.withInitial(ArrayDeque::new);

    @Inject(
            method = "drawEntityOnScreen(I I I D D Lnet/minecraft/entity/EntityLivingBase;)V",
            at = @At("HEAD"), remap = false, require = 0)
    private void nfrUi$captureEntityHudState(int x, int y, int scale,
                                              double mouseX, double mouseY,
                                              EntityLivingBase entity, CallbackInfo ci) {
        nfrUi$states.get().push(HudGlStateSnapshot.capture());
    }

    @Inject(
            method = "drawEntityOnScreen(I I I D D Lnet/minecraft/entity/EntityLivingBase;)V",
            at = @At("RETURN"), remap = false, require = 0)
    private void nfrUi$restoreEntityHudState(int x, int y, int scale,
                                               double mouseX, double mouseY,
                                               EntityLivingBase entity, CallbackInfo ci) {
        Deque<HudGlStateSnapshot> states = nfrUi$states.get();
        HudGlStateSnapshot snapshot = states.pollFirst();
        if (snapshot != null) snapshot.close();
        if (states.isEmpty()) nfrUi$states.remove();
    }
}
