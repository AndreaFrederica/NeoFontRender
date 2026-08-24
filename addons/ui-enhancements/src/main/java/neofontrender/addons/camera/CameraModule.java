package neofontrender.addons.camera;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import neofontrender.addons.ui.UiEnhancementModule;
import neofontrender.addons.api.camera.CameraApi;
import neofontrender.addons.api.camera.CameraRigRequest;
import neofontrender.addons.input.VanillaInputBridge;
import neofontrender.addons.input.PlayerMovementInputBridge;
import neofontrender.addons.input.InputCommandEdges;
import neofontrender.addons.api.input.InputAction;
import neofontrender.addons.api.input.InputApi;
import neofontrender.addons.api.input.InputFrame;
import neofontrender.addons.api.input.InputFlushReason;
import neofontrender.api.client.settings.NfrSettingsPageRegistry;

/** Boots camera bridges without enabling a camera mode by default. */
public final class CameraModule implements UiEnhancementModule {
    private final CameraSessionOwner interactiveSession = new CameraSessionOwner();
    private final InputCommandEdges commandEdges = new InputCommandEdges();
    private net.minecraft.util.ResourceLocation interactiveRigId;
    @Override public void preInit() {
        ShoulderCameraConfig.load();
        FreeLookConfig.load();
        DroneCameraConfig.load();
        CameraPerspectiveConfig.load();
    }

    @Override public void init() {
        VanillaInputBridge.install();
        CameraApi.installBackend(new CameraApiBackend());
        ClientRegistry.registerKeyBinding(CameraKeyBindings.TOGGLE_DRONE);
        ClientRegistry.registerKeyBinding(CameraKeyBindings.TOGGLE_FREE_LOOK);
        ClientRegistry.registerKeyBinding(CameraKeyBindings.TOGGLE_SHOULDER);
        ClientRegistry.registerKeyBinding(CameraKeyBindings.SWAP_SHOULDER);
        ClientRegistry.registerKeyBinding(CameraKeyBindings.EXIT_CAMERA);
        ClientRegistry.registerKeyBinding(CameraKeyBindings.ADJUST_LEFT);
        ClientRegistry.registerKeyBinding(CameraKeyBindings.ADJUST_RIGHT);
        ClientRegistry.registerKeyBinding(CameraKeyBindings.ADJUST_UP);
        ClientRegistry.registerKeyBinding(CameraKeyBindings.ADJUST_DOWN);
        ClientRegistry.registerKeyBinding(CameraKeyBindings.ADJUST_IN);
        ClientRegistry.registerKeyBinding(CameraKeyBindings.ADJUST_OUT);
        ClientRegistry.registerKeyBinding(CameraKeyBindings.ROLL_LEFT);
        ClientRegistry.registerKeyBinding(CameraKeyBindings.ROLL_RIGHT);
        ClientRegistry.registerKeyBinding(CameraKeyBindings.FREELOOK_TOGGLE_CONTROL);
        ClientRegistry.registerKeyBinding(CameraKeyBindings.FREELOOK_MOVE_UP);
        ClientRegistry.registerKeyBinding(CameraKeyBindings.FREELOOK_MOVE_DOWN);
        ClientRegistry.registerKeyBinding(CameraKeyBindings.FREELOOK_MOVE_LEFT);
        ClientRegistry.registerKeyBinding(CameraKeyBindings.FREELOOK_MOVE_RIGHT);
        ClientRegistry.registerKeyBinding(CameraKeyBindings.FREELOOK_MOVE_FORWARD);
        ClientRegistry.registerKeyBinding(CameraKeyBindings.FREELOOK_MOVE_BACKWARD);
        ClientRegistry.registerKeyBinding(CameraKeyBindings.FREELOOK_MOVE_RESET);
        ClientRegistry.registerKeyBinding(CameraKeyBindings.DRONE_ROTATE_LEFT);
        ClientRegistry.registerKeyBinding(CameraKeyBindings.DRONE_ROTATE_RIGHT);
        CameraPerspectiveController.initialize();
        NfrSettingsPageRegistry.register(new CameraSettingsPage());
        MinecraftForge.EVENT_BUS.register(PlayerMovementInputBridge.INSTANCE);
        MinecraftForge.EVENT_BUS.register(CameraRenderBridge.INSTANCE);
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void clientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
            FreeLookCameraRig.beginTick();
            return;
        }
        if (event.phase != TickEvent.Phase.END) return;
        net.minecraft.client.entity.EntityPlayerSP player =
                net.minecraft.client.Minecraft.getMinecraft().player;
        if (player == null || !player.isEntityAlive()) {
            closeInteractiveSession();
            return;
        }
        CameraPerspectiveController.tick();
        if (!interactiveSession.isActive()) interactiveRigId = null;
        if (!CameraRuntime.isFreeLookPerspectiveValid()) closeInteractiveSession();
        // Match Shoulder Surfing's key handler: opening a screen must not toggle modes or
        // consume its adjustment keys while the player is typing or navigating a UI.
        InputFrame input = InputApi.getFrame(0.0F);
        boolean exitPressed = commandEdges.pressed(input, InputAction.CAMERA_EXIT_DRONE);
        boolean dronePressed = commandEdges.pressed(input, InputAction.CAMERA_TOGGLE_DRONE);
        boolean freeLookPressed = commandEdges.pressed(input, InputAction.CAMERA_TOGGLE_FREELOOK);
        boolean shoulderPressed = commandEdges.pressed(input, InputAction.CAMERA_TOGGLE_SHOULDER);
        boolean swapShoulderPressed = commandEdges.pressed(input, InputAction.CAMERA_SWAP_SHOULDER);
        boolean freeLookControlPressed = commandEdges.pressed(
                input, InputAction.CAMERA_TOGGLE_FREELOOK_CONTROL);
        if (net.minecraft.client.Minecraft.getMinecraft().currentScreen != null) {
            CameraRuntime.advanceCameraTick();
            return;
        }
        if (CameraKeyBindings.EXIT_CAMERA.isPressed()
                || exitPressed) closeInteractiveSession();
        if (CameraKeyBindings.TOGGLE_DRONE.isPressed()
                || dronePressed) {
            toggle(CameraRigRequest.drone(100));
        }
        if (FreeLookConfig.toggleMode) {
            if (CameraKeyBindings.TOGGLE_FREE_LOOK.isPressed()
                    || freeLookPressed) {
                toggle(CameraRigRequest.freeLook(100));
            }
        } else {
            if (CameraKeyBindings.TOGGLE_FREE_LOOK.isKeyDown()
                    || input.get(InputAction.CAMERA_TOGGLE_FREELOOK).isDown()) {
                if (!interactiveSession.isActive())
                    acquireInteractive(CameraRigRequest.freeLook(100));
            } else if (interactiveSession.isActive()
                    && interactiveRigId != null
                    && "free_look".equals(interactiveRigId.getPath())) {
                closeInteractiveSession();
            }
        }
        if (CameraKeyBindings.TOGGLE_SHOULDER.isPressed()
                || shoulderPressed) {
            toggle(CameraRigRequest.shoulder(100));
        }
        if (CameraKeyBindings.SWAP_SHOULDER.isPressed()
                || swapShoulderPressed) {
            CameraRuntime.swapShoulder();
        }
        if (CameraRuntime.isShoulderActive()) {
            if (CameraKeyBindings.ADJUST_LEFT.isPressed()) ShoulderCameraConfig.adjustX(ShoulderCameraConfig.cameraStepSize);
            if (CameraKeyBindings.ADJUST_RIGHT.isPressed()) ShoulderCameraConfig.adjustX(-ShoulderCameraConfig.cameraStepSize);
            if (CameraKeyBindings.ADJUST_UP.isPressed()) ShoulderCameraConfig.adjustY(ShoulderCameraConfig.cameraStepSize);
            if (CameraKeyBindings.ADJUST_DOWN.isPressed()) ShoulderCameraConfig.adjustY(-ShoulderCameraConfig.cameraStepSize);
            if (CameraKeyBindings.ADJUST_IN.isPressed()) ShoulderCameraConfig.adjustZ(-ShoulderCameraConfig.cameraStepSize);
            if (CameraKeyBindings.ADJUST_OUT.isPressed()) ShoulderCameraConfig.adjustZ(ShoulderCameraConfig.cameraStepSize);
        }
        if (CameraRuntime.isFreeLookActive()
                && (CameraKeyBindings.FREELOOK_TOGGLE_CONTROL.isPressed()
                || freeLookControlPressed)) {
            CameraRuntime.toggleFreeLookControl();
        }
        if (CameraRuntime.isFreeLookActive()) {
            double step = FreeLookConfig.moveStepSize;
            if (CameraKeyBindings.FREELOOK_MOVE_UP.isKeyDown()) FreeLookCameraRig.adjustMoveOffset(0, step, 0);
            if (CameraKeyBindings.FREELOOK_MOVE_DOWN.isKeyDown()) FreeLookCameraRig.adjustMoveOffset(0, -step, 0);
            // CameraAttitude.right() = rotate(-1,0,0), so positive X = left, negative X = right
            if (CameraKeyBindings.FREELOOK_MOVE_LEFT.isKeyDown()) FreeLookCameraRig.adjustMoveOffset(step, 0, 0);
            if (CameraKeyBindings.FREELOOK_MOVE_RIGHT.isKeyDown()) FreeLookCameraRig.adjustMoveOffset(-step, 0, 0);
            // CameraAttitude.forward() = rotate(0,0,1), positive Z = forward, negative Z = backward
            if (CameraKeyBindings.FREELOOK_MOVE_FORWARD.isKeyDown()) FreeLookCameraRig.adjustMoveOffset(0, 0, step);
            if (CameraKeyBindings.FREELOOK_MOVE_BACKWARD.isKeyDown()) FreeLookCameraRig.adjustMoveOffset(0, 0, -step);
            if (CameraKeyBindings.FREELOOK_MOVE_RESET.isPressed()) FreeLookCameraRig.resetMoveOffset();
        }
        CameraRuntime.advanceCameraTick();
    }

    @SubscribeEvent
    public void worldUnload(WorldEvent.Unload event) {
        if (event.getWorld() != null && event.getWorld().isRemote) {
            closeInteractiveSession();
            commandEdges.clear();
            CameraRuntime.releaseApiViewProxy();
            VanillaInputBridge.reset();
            InputApi.flush(InputFlushReason.WORLD_CHANGE);
        }
    }

    @SubscribeEvent
    public void disconnected(FMLNetworkEvent.ClientDisconnectionFromServerEvent event) {
        closeInteractiveSession();
        commandEdges.clear();
        CameraRuntime.releaseApiViewProxy();
        VanillaInputBridge.reset();
        InputApi.flush(InputFlushReason.DISCONNECT);
    }

    private void toggle(CameraRigRequest request) {
        boolean sameMode = request != null && ((request.id().equals(interactiveRigId)
                && interactiveSession.isActive())
                || CameraPerspectiveController.isActiveRig(request.id())
                || ("drone".equals(request.id().getPath())
                && CameraRuntime.isDroneActive())
                || ("free_look".equals(request.id().getPath()) && CameraRuntime.isFreeLookActive())
                || ("shoulder".equals(request.id().getPath()) && CameraRuntime.isShoulderActive()));
        if (interactiveSession.isActive()
                || CameraPerspectiveController.hasActiveMode()
                || CameraRuntime.isDroneActive() || CameraRuntime.isFreeLookActive()
                || CameraRuntime.isShoulderActive()) {
            closeInteractiveSession();
            if (sameMode) return;
        }
        acquireInteractive(request);
    }

    private void acquireInteractive(CameraRigRequest request) {
        interactiveRigId = interactiveSession.adopt(CameraApi.acquire(request))
                ? request.id() : null;
    }

    private void closeInteractiveSession() {
        RuntimeException failure = null;
        try {
            interactiveSession.close();
        } catch (RuntimeException error) {
            failure = error;
        } finally {
            interactiveRigId = null;
        }
        try {
            // F5-created sessions are owned by the perspective coordinator rather than this
            // optional keybinding path, so the explicit exit action must close those too.
            CameraPerspectiveController.closeActiveMode();
        } catch (RuntimeException error) {
            if (failure == null) failure = error;
            else failure.addSuppressed(error);
        }
        if (failure != null) throw failure;
    }

}
