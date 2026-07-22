package neofontrender.addons.mixin;

import net.minecraft.client.gui.GuiSlot;
import neofontrender.addons.scrolling.SmoothScrollConfigAccess;
import neofontrender.addons.scrolling.SmoothScrollController;
import org.lwjgl.input.Mouse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Replaces GuiSlot wheel jumps with a bounded eased target. */
@Mixin(GuiSlot.class)
public abstract class MixinGuiSlotSmoothScroll {
    @Shadow private float amountScrolled;
    @Shadow private float initialClickY;
    @Shadow public abstract int func_148135_f();
    @Unique private final SmoothScrollController nfrUi$scroller = new SmoothScrollController();

    @Inject(method = "drawScreen", at = @At("HEAD"))
    private void nfrUi$update(int mouseX, int mouseY, float partialTicks, CallbackInfo callback) {
        if (!SmoothScrollConfigAccess.vanillaListsEnabled()) {
            nfrUi$scroller.sync(amountScrolled);
            return;
        }
        amountScrolled = nfrUi$scroller.update(amountScrolled, func_148135_f());
    }

    @Redirect(
            method = "drawScreen",
            at = @At(value = "INVOKE", target = "Lorg/lwjgl/input/Mouse;getEventDWheel()I", remap = false))
    private int nfrUi$smoothWheel() {
        int wheel = Mouse.getEventDWheel();
        if (SmoothScrollConfigAccess.vanillaListsEnabled() && wheel != 0) {
            float delta = wheel > 0 ? -SmoothScrollConfigAccess.wheelStep() : SmoothScrollConfigAccess.wheelStep();
            nfrUi$scroller.scrollBy(delta, func_148135_f(), amountScrolled);
            return 0;
        }
        return wheel;
    }

    @Inject(method = "drawScreen", at = @At("RETURN"))
    private void nfrUi$syncDrag(int mouseX, int mouseY, float partialTicks, CallbackInfo callback) {
        if (Mouse.isButtonDown(0) && initialClickY >= 0.0F) nfrUi$scroller.sync(amountScrolled);
    }
}
