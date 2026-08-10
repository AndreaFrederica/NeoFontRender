package neofontrender.addons.camera;

import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import neofontrender.addons.api.camera.CameraApi;
import neofontrender.addons.api.camera.CameraRigRequest;
import neofontrender.addons.api.camera.CameraSession;
import neofontrender.addons.api.camera.CameraViewChangeReason;
import net.minecraft.util.ResourceLocation;

import java.util.List;
import neofontrender.addons.compat.CameraExternalCompat;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/** Owns the single F5 cycle and keeps custom camera sessions atomic with vanilla perspectives. */
public final class CameraPerspectiveController {
    private static final Logger LOGGER = LogManager.getLogger("UIE Camera Perspective");
    enum Mode { VANILLA_FIRST, VANILLA_THIRD, SHOULDER, FREE_LOOK, DRONE, VANILLA_FRONT }

    private static final Minecraft MC = Minecraft.getMinecraft();
    private static Mode activeMode = Mode.VANILLA_FIRST;
    private static int activeIndex;
    private static boolean initialized;
    private static boolean pendingDefault;
    private static final CameraSessionOwner SESSION = new CameraSessionOwner();

    private CameraPerspectiveController() {}

    static void initialize() {
        initialized = false;
        pendingDefault = true;
    }

    /** Redirect target for the one vanilla F5 keypress call. */
    public static boolean consumePerspectiveKey(KeyBinding binding) {
        if (binding != MC.gameSettings.keyBindTogglePerspective) return binding.isPressed();
        if (!binding.isPressed()) return false;
        if (!CameraExternalCompat.internalCameraAllowed()) return true;
        // Returning false is intentional: Minecraft wraps the original perspective
        // mutation in this boolean. Once UIE owns the cycle, the selected mode has
        // already been applied and vanilla must not increment thirdPersonView again.
        if (!CameraPerspectiveConfig.f5CycleEnabled || MC.player == null) return true;
        synchronizeMode();
        List<Mode> modes = modes();
        if (modes.isEmpty()) return true;
        activeIndex = CameraPresentationPolicy.nextIndex(modes, activeMode);
        activateRequired(modes.get(activeIndex), "F5 cycle");
        return false;
    }

    static void tick() {
        if (MC.player == null) {
            if (initialized || SESSION.isActive()) closeActiveMode();
            initialized = false;
            pendingDefault = true;
            return;
        }
        if (!CameraExternalCompat.internalCameraAllowed()) {
            closeActiveMode();
            pendingDefault = false;
            synchronizeMode();
            return;
        }
        if (pendingDefault) {
            pendingDefault = false;
            activateRequired(mode(CameraPerspectiveConfig.defaultMode), "configured default");
        }
        if (!initialized) synchronizeMode();
        if ((activeMode == Mode.FREE_LOOK && !customModeActive(CameraRuntime.isFreeLookActive()))
                || (activeMode == Mode.SHOULDER && !customModeActive(CameraRuntime.isShoulderActive()))
                || (activeMode == Mode.DRONE && !customModeActive(CameraRuntime.isDroneActive()))) {
            synchronizeMode();
        }
    }

    private static boolean activate(Mode mode) {
        Mode previous = activeMode;
        closeActiveSession();
        switch (mode) {
            case VANILLA_FIRST: setVanillaPerspective(0); break;
            case VANILLA_THIRD: setVanillaPerspective(1); break;
            case VANILLA_FRONT: setVanillaPerspective(2); break;
            case SHOULDER: if (!open(CameraRigRequest.shoulder(100))) return false; break;
            case FREE_LOOK: if (!open(CameraRigRequest.freeLook(100))) return false; break;
            case DRONE: if (!open(CameraRigRequest.drone(100))) return false; break;
            default: return false;
        }
        activeMode = mode;
        initialized = true;
        if (CameraPerspectiveConfig.rememberLastPerspective) {
            CameraPerspectiveConfig.defaultMode = id(mode);
            CameraPerspectiveConfig.save();
        }
        LOGGER.info("Camera perspective mode changed from {} to {}", previous, mode);
        return true;
    }

    private static void activateRequired(Mode mode, String source) {
        try {
            if (!activate(mode)) {
                throw new IllegalStateException("Camera mode " + mode
                        + " was rejected during " + source);
            }
        } catch (RuntimeException error) {
            LOGGER.error("Failed to activate camera mode {} during {}", mode, source, error);
            throw error;
        }
    }

    private static boolean open(CameraRigRequest request) {
        return SESSION.adopt(CameraApi.acquire(request));
    }

    private static void synchronizeMode() {
        if (SESSION.isActive()) {
            List<Mode> modes = modes();
            activeIndex = CameraPresentationPolicy.currentIndex(modes, activeMode);
            initialized = true;
            return;
        }
        if (CameraRuntime.isDroneActive()) activeMode = Mode.DRONE;
        else if (CameraRuntime.isShoulderActive()) activeMode = Mode.SHOULDER;
        else if (CameraRuntime.isFreeLookActive()) activeMode = Mode.FREE_LOOK;
        else switch (MC.gameSettings.thirdPersonView) {
            case 1: activeMode = Mode.VANILLA_THIRD; break;
            case 2: activeMode = Mode.VANILLA_FRONT; break;
            default: activeMode = Mode.VANILLA_FIRST; break;
        }
        List<Mode> modes = modes();
        activeIndex = CameraPresentationPolicy.currentIndex(modes, activeMode);
        initialized = true;
    }

    static void closeActiveMode() {
        closeActiveSession();
        synchronizeMode();
    }

    private static void closeActiveSession() {
        RuntimeException failure = null;
        try {
            SESSION.close();
        } catch (RuntimeException error) {
            failure = error;
        }
        try {
            CameraRuntime.shutdown();
        } catch (RuntimeException error) {
            if (failure == null) failure = error;
            else failure.addSuppressed(error);
        }
        if (failure != null) throw failure;
    }

    static boolean isActiveRig(ResourceLocation id) {
        return SESSION.isActive() && id(id, modeId(activeMode));
    }

    static boolean hasActiveMode() { return SESSION.isActive(); }

    private static boolean id(ResourceLocation requested, String path) {
        return requested != null && "neofontrender_ui_enhancements".equals(requested.getNamespace())
                && path.equals(requested.getPath());
    }

    private static String modeId(Mode mode) {
        switch (mode) {
            case SHOULDER: return "shoulder";
            case FREE_LOOK: return "free_look";
            case DRONE: return "drone";
            default: return "";
        }
    }

    private static boolean customModeActive(boolean builtInActive) {
        return builtInActive || SESSION.isActive();
    }

    private static List<Mode> modes() {
        java.util.ArrayList<Mode> result = new java.util.ArrayList<>();
        result.add(Mode.VANILLA_FIRST);
        if (!CameraExternalCompat.internalCameraAllowed()) {
            result.add(Mode.VANILLA_THIRD);
            if (!CameraPerspectiveConfig.skipThirdPersonFront) result.add(Mode.VANILLA_FRONT);
            return result;
        }
        if (CameraPerspectiveConfig.replaceDefaultPerspective && CameraPerspectiveConfig.shoulderInF5)
            result.add(Mode.SHOULDER);
        else result.add(Mode.VANILLA_THIRD);
        if (!CameraPerspectiveConfig.replaceDefaultPerspective && CameraPerspectiveConfig.shoulderInF5) result.add(Mode.SHOULDER);
        if (CameraPerspectiveConfig.freeLookInF5) result.add(Mode.FREE_LOOK);
        if (CameraPerspectiveConfig.droneInF5) result.add(Mode.DRONE);
        if (!CameraPerspectiveConfig.skipThirdPersonFront) result.add(Mode.VANILLA_FRONT);
        return result;
    }

    private static void setVanillaPerspective(int perspective) {
        MC.gameSettings.thirdPersonView = perspective;
        CameraRuntime.refreshView(CameraViewChangeReason.PERSPECTIVE_CHANGED);
    }

    private static Mode mode(String id) {
        if ("vanilla_third".equals(id)) return Mode.VANILLA_THIRD;
        if ("shoulder".equals(id)) return Mode.SHOULDER;
        if ("free_look".equals(id)) return Mode.FREE_LOOK;
        if ("drone".equals(id)) return Mode.DRONE;
        if ("vanilla_front".equals(id)) return Mode.VANILLA_FRONT;
        return Mode.VANILLA_FIRST;
    }

    private static String id(Mode mode) {
        switch (mode) {
            case VANILLA_THIRD: return "vanilla_third";
            case SHOULDER: return "shoulder";
            case FREE_LOOK: return "free_look";
            case DRONE: return "drone";
            case VANILLA_FRONT: return "vanilla_front";
            default: return "vanilla_first";
        }
    }
}
