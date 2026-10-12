package neofontrender.addons.zoom;

import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.event.world.WorldEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Keyboard;

public final class ZoomHandler {
    public static final ZoomHandler INSTANCE = new ZoomHandler();
    static final KeyBinding ZOOM_KEY = new KeyBinding(
            "key.neofontrender_ui_enhancements.zoom",
            Keyboard.KEY_C,
            "key.categories.neofontrender_ui_enhancements");

    private final boolean optiFinePresent = ZoomCompat.optiFinePresent();
    private final ZoomTransition transition = new ZoomTransition();
    private boolean zooming;
    private boolean smoothCamEnabled;
    private boolean previousSmoothCamera;

    private ZoomHandler() {}

    /**
     * Applies the zoom state machine to the given base FOV. 1.7.10 has no
     * EntityViewRenderEvent.FOVModifier event, so the EntityRenderer mixin calls this from
     * getFOVModifier every frame instead.
     */
    public float modifyFov(float baseFov) {
        Minecraft minecraft = Minecraft.getMinecraft();
        boolean requested = shouldZoom(minecraft);
        int duration = ZoomConfig.smoothTransition ? ZoomConfig.transitionDurationMillis : 0;
        float amount = transition.update(requested, System.nanoTime(), duration);
        boolean active = amount > 0.0F;
        updateSmoothCamera(minecraft, active);
        if (active) {
            float zoomedFov = ZoomMath.zoomedFov(baseFov, ZoomConfig.magnification, amount);
            ZoomMouseScaling.update(baseFov, zoomedFov,
                    ZoomConfig.mouseSensitivityAdjustmentPercent / 100.0F);
            return zoomedFov;
        }
        ZoomMouseScaling.reset();
        return baseFov;
    }

    @SubscribeEvent
    public void clientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END && zooming) {
            Minecraft minecraft = Minecraft.getMinecraft();
            if (minecraft == null || minecraft.theWorld == null || minecraft.thePlayer == null
                    || minecraft.currentScreen != null) {
                resetState();
            }
        }
    }

    @SubscribeEvent
    public void guiOpened(GuiOpenEvent event) {
        if (event.gui != null) resetState();
    }

    @SubscribeEvent
    public void worldUnloaded(WorldEvent.Unload event) {
        if (event.world.isRemote) resetState();
    }

    private boolean shouldZoom(Minecraft minecraft) {
        return !optiFinePresent
                && ZoomConfig.enabled
                && minecraft != null
                && minecraft.theWorld != null
                && minecraft.thePlayer != null
                && minecraft.currentScreen == null
                && ZOOM_KEY.getIsKeyPressed();
    }

    /**
     * Uses Minecraft's normal smooth-camera path so the original player.turn() invocations stay
     * available to camera mods such as RollTheSky. Enabling the filter during the FOV transition
     * also gives it time to warm up before the zoom settles.
     */
    private void updateSmoothCamera(Minecraft minecraft, boolean active) {
        if (active) {
            if (!zooming) {
                zooming = true;
                previousSmoothCamera = minecraft.gameSettings.smoothCamera;
            }
            if (ZoomConfig.smoothCamera && !smoothCamEnabled) {
                smoothCamEnabled = true;
                minecraft.gameSettings.smoothCamera = true;
            }
        } else if (zooming) {
            zooming = false;
            if (smoothCamEnabled) {
                smoothCamEnabled = false;
                minecraft.gameSettings.smoothCamera = previousSmoothCamera;
            }
        }
    }

    private void resetState() {
        Minecraft minecraft = Minecraft.getMinecraft();
        if (smoothCamEnabled && minecraft != null && minecraft.gameSettings != null) {
            minecraft.gameSettings.smoothCamera = previousSmoothCamera;
        }
        transition.reset();
        zooming = false;
        smoothCamEnabled = false;
        previousSmoothCamera = false;
        ZoomMouseScaling.reset();
    }

    boolean isZooming() {
        return zooming || shouldZoom(Minecraft.getMinecraft());
    }
}
