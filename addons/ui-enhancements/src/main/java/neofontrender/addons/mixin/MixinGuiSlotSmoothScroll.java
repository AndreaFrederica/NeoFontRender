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

@Mixin(GuiSlot.class)
public abstract class MixinGuiSlotSmoothScroll {
    @Shadow protected float amountScrolled;
    @Shadow protected int initialClickY;
    @Shadow public abstract int getMaxScroll();
    @Unique private final SmoothScrollController nfrUi$scroller = new SmoothScrollController();

    @Inject(method = "drawScreen", at = @At("HEAD"))
    private void nfrUi$update(int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
        amountScrolled = nfrUi$scroller.update(amountScrolled, getMaxScroll());
    }

    @Redirect(method = "handleMouseInput", at = @At(value = "INVOKE", target = "Lorg/lwjgl/input/Mouse;getEventDWheel()I"))
    private int nfrUi$smoothWheel() {
        int wheel = Mouse.getEventDWheel();
        if (SmoothScrollConfigAccess.enabled() && wheel != 0) {
            nfrUi$scroller.scrollBy(wheel > 0 ? -SmoothScrollConfigAccess.wheelStep() : SmoothScrollConfigAccess.wheelStep(),
                    getMaxScroll(), amountScrolled);
            return 0;
        }
        return wheel;
    }

    @Inject(method = "handleMouseInput", at = @At("RETURN"))
    private void nfrUi$syncDrag(CallbackInfo ci) {
        if (Mouse.isButtonDown(0) && initialClickY >= 0) nfrUi$scroller.sync(amountScrolled);
    }
}
