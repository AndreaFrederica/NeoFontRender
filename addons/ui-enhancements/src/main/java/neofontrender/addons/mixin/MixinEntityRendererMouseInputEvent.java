package neofontrender.addons.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.EntityRenderer;
import neofontrender.addons.api.flight.FlightApi;
import neofontrender.addons.api.input.CameraMouseInputEvent;
import neofontrender.addons.api.input.InputAction;
import neofontrender.addons.api.input.InputApi;
import neofontrender.addons.api.input.InputFlushReason;
import neofontrender.addons.api.input.InputFrame;
import neofontrender.addons.camera.CameraRuntime;
import neofontrender.addons.input.VanillaInputBridge;
import net.minecraftforge.common.MinecraftForge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Publishes UIE's non-invasive mouse-input API without replacing player.turn(). */
@Mixin(EntityRenderer.class)
public abstract class MixinEntityRendererMouseInputEvent {
    @Shadow private Minecraft mc;
    @Unique private boolean nfrUi$wasGameFocused;

    @Inject(method = "updateCameraAndRender", at = @At("HEAD"), require = 1)
    private void nfrUi$flushInputOnFocusLoss(float partialTicks, CallbackInfo ci) {
        boolean focused = mc.inGameHasFocus;
        if (nfrUi$wasGameFocused && !focused) {
            VanillaInputBridge.reset();
            InputApi.flush(InputFlushReason.FOCUS_LOST);
        }
        nfrUi$wasGameFocused = focused;
    }

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
        CameraRuntime.beginSample(partialTicks);
        // Publish the routing boundary before Flight or other legacy listeners consume the event.
        // The logical mouse mapping is still owned by the legacy event during this migration step.
        int rawDeltaX = mc.mouseHelper.deltaX;
        int rawDeltaY = mc.mouseHelper.deltaY;
        VanillaInputBridge.capture(mc, rawDeltaX, rawDeltaY);
        InputFrame input = InputApi.beginFrame(partialTicks, mc.inGameHasFocus);
        CameraMouseInputEvent event = new CameraMouseInputEvent(
                mc.thePlayer, partialTicks, rawDeltaX, rawDeltaY);
        boolean eventCanceled = MinecraftForge.EVENT_BUS.post(event);
        if (eventCanceled) {
            mc.mouseHelper.deltaX = 0;
            mc.mouseHelper.deltaY = 0;
        } else {
            mc.mouseHelper.deltaX = event.getDeltaX();
            mc.mouseHelper.deltaY = event.getDeltaY();
        }
        boolean cameraOwnsLook = CameraRuntime.updateViewInput(rawDeltaX, rawDeltaY,
                event.getCameraDeltaX(), event.getCameraDeltaY(), eventCanceled,
                mc.gameSettings.invertMouse, mc.gameSettings.keyBindForward.getIsKeyPressed(),
                mc.gameSettings.keyBindBack.getIsKeyPressed(),
                mc.gameSettings.keyBindLeft.getIsKeyPressed(),
                mc.gameSettings.keyBindRight.getIsKeyPressed(),
                mc.gameSettings.keyBindJump.getIsKeyPressed(),
                mc.gameSettings.keyBindSneak.getIsKeyPressed(), System.nanoTime());
        if (cameraOwnsLook) {
            // Drone owns LOOK, so vanilla must not rotate the player's physical body afterward.
            mc.mouseHelper.deltaX = 0;
            mc.mouseHelper.deltaY = 0;
        } else if (!FlightApi.isActive()) {
            double seconds = input.getContext().getFrameSeconds();
            mc.mouseHelper.deltaX = VanillaInputBridge.resolveCameraDelta(
                    rawDeltaX, mc.mouseHelper.deltaX, eventCanceled,
                    input.get(InputAction.CAMERA_LOOK_X),
                    input.disposition(InputAction.CAMERA_LOOK_X), seconds);
            mc.mouseHelper.deltaY = VanillaInputBridge.resolveCameraDelta(
                    rawDeltaY, mc.mouseHelper.deltaY, eventCanceled,
                    input.get(InputAction.CAMERA_LOOK_Y),
                    input.disposition(InputAction.CAMERA_LOOK_Y), seconds);
        }
        // Do not sample the frame here. Vanilla applies player.turn() after this injection;
        // getMouseOver/rendering will take the authoritative sample once that rotation is current.
    }

    @Inject(method = "getMouseOver", at = @At("RETURN"), require = 1)
    private void nfrUi$synchronizeCameraPicking(float partialTicks, CallbackInfo ci) {
        CameraRuntime.synchronizePicking(partialTicks);
    }
}
