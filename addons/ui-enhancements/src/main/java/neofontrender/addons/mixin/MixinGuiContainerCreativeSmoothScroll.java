package neofontrender.addons.mixin;

import net.minecraft.client.gui.inventory.GuiContainerCreative;
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

@Mixin(GuiContainerCreative.class)
public abstract class MixinGuiContainerCreativeSmoothScroll {
    @Shadow private float currentScroll;
    @Shadow private boolean isScrolling;
    @Unique private final SmoothScrollController nfrUi$scroller = new SmoothScrollController();

    @Redirect(method = "handleMouseInput", at = @At(value = "INVOKE",
            target = "Lorg/lwjgl/input/Mouse;getEventDWheel()I"))
    private int nfrUi$captureWheel() {
        int wheel = Mouse.getEventDWheel();
        if (!SmoothScrollConfigAccess.creativeInventoryEnabled() || wheel == 0) return wheel;
        int hiddenRows = nfrUi$hiddenRows();
        if (hiddenRows <= 0) {
            nfrUi$scroller.sync(0.0F);
            currentScroll = 0.0F;
            return 0;
        }
        float rowsPerNotch = SmoothScrollConfigAccess.wheelStep() / 18.0F;
        float delta = (wheel > 0 ? -rowsPerNotch : rowsPerNotch) / hiddenRows;
        nfrUi$scroller.scrollBy(delta, 1.0F, currentScroll);
        return 0;
    }

    @Inject(method = "drawScreen", at = @At("HEAD"))
    private void nfrUi$update(int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
        if (!SmoothScrollConfigAccess.creativeInventoryEnabled()) {
            nfrUi$scroller.sync(currentScroll);
            return;
        }
        if (nfrUi$hiddenRows() <= 0) {
            currentScroll = 0.0F;
            nfrUi$scroller.sync(0.0F);
            nfrUi$container().scrollTo(0.0F);
            return;
        }
        if (!isScrolling) {
            currentScroll = nfrUi$scroller.update(currentScroll, 1.0F);
            nfrUi$container().scrollTo(currentScroll);
        }
    }

    @Inject(method = "drawScreen", at = @At("RETURN"))
    private void nfrUi$syncDrag(int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
        if (isScrolling) nfrUi$scroller.sync(currentScroll);
    }

    @Inject(method = "setCurrentCreativeTab", at = @At("RETURN"))
    private void nfrUi$tabChanged(CallbackInfo ci) {
        nfrUi$scroller.sync(currentScroll);
    }

    @Unique
    private GuiContainerCreative.ContainerCreative nfrUi$container() {
        return (GuiContainerCreative.ContainerCreative)
                ((AccessorGuiContainer) (Object) this).nfrUi$getInventorySlots();
    }

    @Unique
    private int nfrUi$hiddenRows() {
        return Math.max(0, (nfrUi$container().itemList.size() + 8) / 9 - 5);
    }
}
