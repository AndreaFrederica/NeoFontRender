package neofontrender.addons.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.EntityRenderer;
import neofontrender.addons.api.input.CameraMouseInputEvent;
import net.minecraftforge.common.MinecraftForge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Publishes UIE's non-invasive mouse-input API without replacing player.turn(). */
@Mixin(EntityRenderer.class)
public abstract class MixinEntityRendererMouseInputEvent {
    @Shadow private Minecraft mc;

    @Inject(
            method = "updateCameraAndRender",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/util/MouseHelper;mouseXYChange()V",
                    shift = At.Shift.AFTER
            ),
            require = 1
    )
    private void nfrUi$postCameraMouseInput(float partialTicks, CallbackInfo ci) {
        CameraMouseInputEvent event = new CameraMouseInputEvent(
                mc.thePlayer, partialTicks, mc.mouseHelper.deltaX, mc.mouseHelper.deltaY);
        if (MinecraftForge.EVENT_BUS.post(event)) {
            mc.mouseHelper.deltaX = 0;
            mc.mouseHelper.deltaY = 0;
        } else {
            mc.mouseHelper.deltaX = event.getDeltaX();
            mc.mouseHelper.deltaY = event.getDeltaY();
        }
    }
}
