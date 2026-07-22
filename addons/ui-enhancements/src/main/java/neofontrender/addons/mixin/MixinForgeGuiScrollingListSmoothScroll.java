package neofontrender.addons.mixin;

import cpw.mods.fml.client.GuiScrollingList;
import neofontrender.addons.scrolling.SmoothScrollConfigAccess;
import neofontrender.addons.scrolling.SmoothScrollController;
import org.lwjgl.input.Mouse;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Applies the same eased wheel behavior to Forge's scrolling list. */
@Mixin(value = GuiScrollingList.class, remap = false)
public abstract class MixinForgeGuiScrollingListSmoothScroll {
    @Shadow private float scrollDistance;
    @Shadow private float initialMouseClickY;
    @Shadow @Final protected int top;
    @Shadow @Final protected int bottom;
    @Shadow protected abstract int getContentHeight();
    @Unique private final SmoothScrollController nfrUi$scroller = new SmoothScrollController();

    @Inject(method = "drawScreen", at = @At("HEAD"))
    private void nfrUi$update(int mouseX, int mouseY, float partialTicks, CallbackInfo callback) {
        if (!SmoothScrollConfigAccess.forgeListsEnabled()) {
            nfrUi$scroller.sync(scrollDistance);
            return;
        }
        scrollDistance = nfrUi$scroller.update(scrollDistance, nfrUi$getMaxScroll());
    }

    @Redirect(
            method = "drawScreen",
            at = @At(value = "INVOKE", target = "Lorg/lwjgl/input/Mouse;getEventDWheel()I"))
    private int nfrUi$smoothWheel() {
        int wheel = Mouse.getEventDWheel();
        if (SmoothScrollConfigAccess.forgeListsEnabled() && wheel != 0) {
            float delta = wheel > 0 ? -SmoothScrollConfigAccess.wheelStep() : SmoothScrollConfigAccess.wheelStep();
            nfrUi$scroller.scrollBy(delta, nfrUi$getMaxScroll(), scrollDistance);
            return 0;
        }
        return wheel;
    }

    @Inject(method = "drawScreen", at = @At("RETURN"))
    private void nfrUi$syncDrag(int mouseX, int mouseY, float partialTicks, CallbackInfo callback) {
        if (Mouse.isButtonDown(0) && initialMouseClickY >= 0.0F) nfrUi$scroller.sync(scrollDistance);
    }

    @Unique
    private float nfrUi$getMaxScroll() {
        return Math.max(0, getContentHeight() - (bottom - top - 4));
    }
}
