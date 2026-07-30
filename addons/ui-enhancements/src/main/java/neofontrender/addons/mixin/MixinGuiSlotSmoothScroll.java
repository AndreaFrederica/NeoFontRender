package neofontrender.addons.mixin;

import net.minecraft.client.gui.GuiSlot;
import neofontrender.addons.language.LanguageListSearchAccess;
import neofontrender.addons.scrolling.SmoothScrollConfigAccess;
import neofontrender.addons.scrolling.SmoothScrollController;
import org.lwjgl.opengl.GL11;
import org.lwjglx.input.Mouse;
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

    @Inject(method = "drawScreen", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiSlot;bindAmountScrolled()V"), require = 1)
    private void nfrUi$update(int mouseX, int mouseY, float partialTicks, CallbackInfo callback) {
        if (!SmoothScrollConfigAccess.vanillaListsEnabled()) {
            nfrUi$scroller.sync(amountScrolled);
            return;
        }
        // 1.7.10 handles scrollbar drags and trough paging inside drawScreen polling, before
        // bindAmountScrolled. While the mouse drives the list, mirror vanilla's position
        // directly instead of easing toward a stale target that swallows the movement.
        if (Mouse.isButtonDown(0) && initialClickY != -1.0F) {
            nfrUi$scroller.sync(amountScrolled);
            return;
        }
        amountScrolled = (Object) this instanceof LanguageListSearchAccess
                ? nfrUi$scroller.updateContinuous(amountScrolled, func_148135_f())
                : nfrUi$scroller.update(amountScrolled, func_148135_f());
    }

    @Inject(method = "drawScreen", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiSlot;drawSelectionBox(IIII)V"))
    private void nfrUi$beginSubpixelListTranslation(int mouseX, int mouseY, float partialTicks,
                                                    CallbackInfo callback) {
        GL11.glPushMatrix();
        float fraction = amountScrolled - (float) Math.floor(amountScrolled);
        GL11.glTranslatef(0.0F, -fraction, 0.0F);
    }

    @Inject(method = "drawScreen", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiSlot;drawSelectionBox(IIII)V",
            shift = At.Shift.AFTER))
    private void nfrUi$endSubpixelListTranslation(int mouseX, int mouseY, float partialTicks,
                                                  CallbackInfo callback) {
        GL11.glPopMatrix();
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
