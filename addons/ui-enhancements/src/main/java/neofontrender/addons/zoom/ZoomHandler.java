package neofontrender.addons.zoom;

import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.client.event.EntityViewRenderEvent;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Keyboard;

final class ZoomHandler {
    static final ZoomHandler INSTANCE = new ZoomHandler();
    static final KeyBinding ZOOM_KEY = new KeyBinding(
            "key.neofontrender_ui_enhancements.zoom",
            Keyboard.KEY_C,
            "key.categories.neofontrender_ui_enhancements");

    private final boolean optiFinePresent = ZoomCompat.optiFinePresent();
    private final ZoomTransition transition = new ZoomTransition();
    private boolean zooming;
    private boolean managedSmoothCamera;
    private boolean smoothCameraBeforeZoom;

    private ZoomHandler() {}

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void changeFov(EntityViewRenderEvent.FOVModifier event) {
        Minecraft minecraft = Minecraft.getMinecraft();
        boolean requested = shouldZoom(minecraft);
        int duration = ZoomConfig.smoothTransition ? ZoomConfig.transitionDurationMillis : 0;
        float amount = transition.update(requested, System.nanoTime(), duration);
        boolean active = amount > 0.0F;
        boolean settled = amount >= 0.99F;
        updateState(minecraft, active, settled);
        if (active) {
            float baseFov = event.getFOV();
            float zoomedFov = ZoomMath.zoomedFov(baseFov, ZoomConfig.magnification, amount);
            ZoomMouseScaling.update(baseFov, zoomedFov,
                    ZoomConfig.mouseSensitivityAdjustmentPercent / 100.0F);
            event.setFOV(zoomedFov);
        } else {
            ZoomMouseScaling.reset();
        }
    }

    @SubscribeEvent
    public void clientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END && zooming) {
            Minecraft minecraft = Minecraft.getMinecraft();
            if (minecraft == null || minecraft.world == null || minecraft.player == null
                    || minecraft.currentScreen != null) {
                resetState(minecraft);
            }
        }
    }

    @SubscribeEvent
    public void guiOpened(GuiOpenEvent event) {
        if (event.getGui() != null) resetState(Minecraft.getMinecraft());
    }

    @SubscribeEvent
    public void worldUnloaded(WorldEvent.Unload event) {
        if (event.getWorld().isRemote) resetState(Minecraft.getMinecraft());
    }

    private boolean shouldZoom(Minecraft minecraft) {
        return !optiFinePresent
                && ZoomConfig.enabled
                && minecraft != null
                && minecraft.world != null
                && minecraft.player != null
                && minecraft.currentScreen == null
                && ZOOM_KEY.isKeyDown();
    }

    private void updateState(Minecraft minecraft, boolean active, boolean settled) {
        if (minecraft == null || minecraft.gameSettings == null) return;
        if (active) {
            if (!zooming) {
                zooming = true;
                managedSmoothCamera = ZoomConfig.smoothCamera;
                smoothCameraBeforeZoom = minecraft.gameSettings.smoothCamera;
            }
            // Only enable smooth camera after the FOV transition completes to avoid
            // double-smoothing (vanilla cinematic + smoothstep) during the animation.
            if (managedSmoothCamera && settled) minecraft.gameSettings.smoothCamera = true;
        } else if (zooming) {
            if (managedSmoothCamera) minecraft.gameSettings.smoothCamera = smoothCameraBeforeZoom;
            zooming = false;
            managedSmoothCamera = false;
        }
    }

    private void resetState(Minecraft minecraft) {
        transition.reset();
        ZoomMouseScaling.reset();
        updateState(minecraft, false, false);
    }
}
