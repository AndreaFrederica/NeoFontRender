package neofontrender.addons.mixin;

import net.minecraftforge.fml.client.GuiScrollingList;
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

@Mixin(value = GuiScrollingList.class, remap = false)
public abstract class MixinForgeGuiScrollingListSmoothScroll {
    @Shadow private float scrollDistance;
    @Shadow private float initialMouseClickY;
    @Shadow @Final protected int top;
    @Shadow @Final protected int bottom;
    @Shadow protected abstract int getContentHeight();
    @Unique private final SmoothScrollController nfrUi$scroller = new SmoothScrollController();

    @Inject(method = "drawScreen", at = @At("HEAD"))
    private void nfrUi$update(int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
        scrollDistance = nfrUi$scroller.update(scrollDistance, nfrUi$getMaxScroll());
    }

    @Redirect(method = "handleMouseInput", at = @At(value = "INVOKE", target = "Lorg/lwjgl/input/Mouse;getEventDWheel()I"))
    private int nfrUi$smoothWheel() {
        int wheel = Mouse.getEventDWheel();
        if (SmoothScrollConfigAccess.enabled() && wheel != 0) {
            nfrUi$scroller.scrollBy(wheel > 0 ? -SmoothScrollConfigAccess.wheelStep() : SmoothScrollConfigAccess.wheelStep(),
                    nfrUi$getMaxScroll(), scrollDistance);
            return 0;
        }
        return wheel;
    }

    @Inject(method = "drawScreen", at = @At("RETURN"))
    private void nfrUi$syncDrag(int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
        if (Mouse.isButtonDown(0) && initialMouseClickY >= 0.0F) nfrUi$scroller.sync(scrollDistance);
    }

    @Unique private float nfrUi$getMaxScroll() {
        int max = getContentHeight() - (bottom - top - 4);
        if (max < 0) max /= 2;
        return Math.max(0, max);
    }
}
