package neofontrender.addons.mixin;

import net.minecraft.client.gui.GuiSlot;
import net.minecraft.client.renderer.GlStateManager;
import neofontrender.addons.api.ui.navigation.UiNavigationApi;
import neofontrender.addons.language.LanguageListSearchAccess;
import neofontrender.addons.scrolling.SmoothScrollConfigAccess;
import neofontrender.addons.scrolling.SmoothScrollController;
import neofontrender.addons.scrolling.SyntheticScrollAccess;
import org.lwjgl.input.Mouse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiSlot.class)
public abstract class MixinGuiSlotSmoothScroll implements SyntheticScrollAccess {
    @Shadow protected float amountScrolled;
    @Shadow protected int initialClickY;
    @Shadow public abstract int getMaxScroll();
    @Unique private final SmoothScrollController nfrUi$scroller = new SmoothScrollController();
    @Unique private Boolean nfrUi$continuousInterpolation;

    @Inject(method = "drawScreen", at = @At("HEAD"))
    private void nfrUi$update(int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
        if (!SmoothScrollConfigAccess.vanillaListsEnabled()) {
            nfrUi$scroller.sync(amountScrolled);
            return;
        }
        amountScrolled = nfrUi$usesContinuousInterpolation()
                ? nfrUi$scroller.updateContinuous(amountScrolled, getMaxScroll())
                : nfrUi$scroller.update(amountScrolled, getMaxScroll());
    }

    @Inject(method = "drawScreen", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiSlot;drawSelectionBox(IIIIF)V"))
    private void nfrUi$beginSubpixelListTranslation(int mouseX, int mouseY, float partialTicks,
                                                    CallbackInfo ci) {
        GlStateManager.pushMatrix();
        float fraction = amountScrolled - (float) Math.floor(amountScrolled);
        GlStateManager.translate(0.0F, -fraction, 0.0F);
    }

    @Inject(method = "drawScreen", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiSlot;drawSelectionBox(IIIIF)V",
            shift = At.Shift.AFTER))
    private void nfrUi$endSubpixelListTranslation(int mouseX, int mouseY, float partialTicks,
                                                  CallbackInfo ci) {
        GlStateManager.popMatrix();
    }

    @Redirect(method = "handleMouseInput", at = @At(value = "INVOKE", target = "Lorg/lwjgl/input/Mouse;getEventDWheel()I"))
    private int nfrUi$smoothWheel() {
        int wheel = Mouse.getEventDWheel();
        if (SmoothScrollConfigAccess.vanillaListsEnabled() && nfrUi$scrollWheel(wheel)) {
            return 0;
        }
        return wheel;
    }

    @Redirect(method = "handleMouseInput", at = @At(value = "INVOKE",
            target = "Lorg/lwjgl/input/Mouse;isButtonDown(I)Z", remap = false))
    private boolean nfrUi$syntheticButtonState(int button) {
        return UiNavigationApi.isPointerButtonDown(button);
    }

    @Override
    public boolean nfrUi$scrollWheel(int wheel) {
        if (wheel == 0) return false;
        if (!SmoothScrollConfigAccess.vanillaListsEnabled()) {
            amountScrolled = Math.max(0.0F, Math.min(getMaxScroll(), amountScrolled
                    + (wheel > 0 ? -SmoothScrollConfigAccess.wheelStep()
                    : SmoothScrollConfigAccess.wheelStep())));
            nfrUi$scroller.sync(amountScrolled);
            return true;
        }
        nfrUi$scroller.scrollBy(wheel > 0 ? -SmoothScrollConfigAccess.wheelStep()
                : SmoothScrollConfigAccess.wheelStep(), getMaxScroll(), amountScrolled);
        return true;
    }

    @Inject(method = "handleMouseInput", at = @At("RETURN"))
    private void nfrUi$syncDrag(CallbackInfo ci) {
        if (UiNavigationApi.isPointerButtonDown(0) && initialClickY >= 0) nfrUi$scroller.sync(amountScrolled);
    }

    @Unique
    private boolean nfrUi$usesContinuousInterpolation() {
        if (nfrUi$continuousInterpolation == null) {
            String className = getClass().getName();
            nfrUi$continuousInterpolation = (Object) this instanceof LanguageListSearchAccess
                    || className.startsWith("com.cleanroommc.client.modlist.");
        }
        return nfrUi$continuousInterpolation;
    }
}
