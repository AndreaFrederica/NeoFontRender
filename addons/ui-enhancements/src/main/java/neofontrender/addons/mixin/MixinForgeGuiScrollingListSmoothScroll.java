package neofontrender.addons.mixin;

import net.minecraftforge.fml.client.GuiScrollingList;
import neofontrender.addons.api.ui.navigation.UiNavigationApi;
import neofontrender.addons.scrolling.SmoothScrollConfigAccess;
import neofontrender.addons.scrolling.SmoothScrollController;
import neofontrender.addons.scrolling.SyntheticScrollAccess;
import org.lwjgl.input.Mouse;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = GuiScrollingList.class, remap = false)
public abstract class MixinForgeGuiScrollingListSmoothScroll implements SyntheticScrollAccess {
    @Shadow private float scrollDistance;
    @Shadow private float initialMouseClickY;
    @Shadow @Final protected int top;
    @Shadow @Final protected int bottom;
    @Shadow protected abstract int getContentHeight();
    @Unique private final SmoothScrollController nfrUi$scroller = new SmoothScrollController();

    @Inject(method = "drawScreen", at = @At("HEAD"))
    private void nfrUi$update(int mouseX, int mouseY, CallbackInfo ci) {
        if (!SmoothScrollConfigAccess.forgeListsEnabled()) {
            nfrUi$scroller.sync(scrollDistance);
            return;
        }
        scrollDistance = nfrUi$scroller.update(scrollDistance, nfrUi$getMaxScroll());
    }

    @Redirect(method = "handleMouseInput", at = @At(value = "INVOKE", target = "Lorg/lwjgl/input/Mouse;getEventDWheel()I"))
    private int nfrUi$smoothWheel() {
        int wheel = Mouse.getEventDWheel();
        if (SmoothScrollConfigAccess.forgeListsEnabled() && nfrUi$scrollWheel(wheel)) {
            return 0;
        }
        return wheel;
    }

    @Redirect(method = "drawScreen", at = @At(value = "INVOKE",
            target = "Lorg/lwjgl/input/Mouse;isButtonDown(I)Z", remap = false))
    private boolean nfrUi$syntheticButtonState(int button) {
        return UiNavigationApi.isPointerButtonDown(button);
    }

    @Override
    public boolean nfrUi$scrollWheel(int wheel) {
        if (wheel == 0) return false;
        if (!SmoothScrollConfigAccess.forgeListsEnabled()) {
            scrollDistance = Math.max(0.0F, Math.min(nfrUi$getMaxScroll(), scrollDistance
                    + (wheel > 0 ? -SmoothScrollConfigAccess.wheelStep()
                    : SmoothScrollConfigAccess.wheelStep())));
            nfrUi$scroller.sync(scrollDistance);
            return true;
        }
        nfrUi$scroller.scrollBy(wheel > 0 ? -SmoothScrollConfigAccess.wheelStep()
                : SmoothScrollConfigAccess.wheelStep(), nfrUi$getMaxScroll(), scrollDistance);
        return true;
    }

    @Inject(method = "drawScreen", at = @At("RETURN"))
    private void nfrUi$syncDrag(int mouseX, int mouseY, CallbackInfo ci) {
        if (UiNavigationApi.isPointerButtonDown(0) && initialMouseClickY >= 0.0F) nfrUi$scroller.sync(scrollDistance);
    }

    @Unique private float nfrUi$getMaxScroll() {
        int max = getContentHeight() - (bottom - top - 4);
        if (max < 0) max /= 2;
        return Math.max(0, max);
    }
}
