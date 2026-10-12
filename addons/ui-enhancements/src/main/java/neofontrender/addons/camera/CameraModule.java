package neofontrender.addons.camera;

import cpw.mods.fml.client.registry.ClientRegistry;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.common.network.FMLNetworkEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.WorldEvent;
import neofontrender.addons.api.camera.CameraApi;
import neofontrender.addons.api.camera.CameraRigRequest;
import neofontrender.addons.api.input.InputAction;
import neofontrender.addons.api.input.InputApi;
import neofontrender.addons.api.input.InputFlushReason;
import neofontrender.addons.api.input.InputFrame;
import neofontrender.addons.input.InputCommandEdges;
import neofontrender.addons.input.PlayerMovementInputBridge;
import neofontrender.addons.input.VanillaInputBridge;
import neofontrender.addons.ui.UiEnhancementModule;
import neofontrender.api.client.settings.NfrSettingsPageRegistry;

/** Boots camera bridges without enabling a camera mode by default. */
public final class CameraModule implements UiEnhancementModule {
    private final CameraSessionOwner interactiveSession = new CameraSessionOwner();
    private final InputCommandEdges commandEdges = new InputCommandEdges();
    private ResourceLocation interactiveRigId;
    @Override public void preInit() {
        ShoulderCameraConfig.load();
        FreeLookConfig.load();
        CursorLookConfig.load();
        DroneCameraConfig.load();
        CameraPerspectiveConfig.load();
    }

    @Override public void init() {
        VanillaInputBridge.install();
        CameraApi.installBackend(new CameraApiBackend());
        ClientRegistry.registerKeyBinding(CameraKeyBindings.TOGGLE_DRONE);
        ClientRegistry.registerKeyBinding(CameraKeyBindings.TOGGLE_FREE_LOOK);
        ClientRegistry.registerKeyBinding(CameraKeyBindings.TOGGLE_CURSOR_LOOK);
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
        FMLCommonHandler.instance().bus().register(this);
    }

    /** Avoid owning Minecraft's hotbar redirect so inventory mods can compose with this gate. */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void mouseInput(MouseEvent event) {
        if (event.dwheel == 0) return;
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft.thePlayer == null || minecraft.theWorld == null
                || minecraft.currentScreen != null || !minecraft.inGameHasFocus) return;
        // Forge posts this before hotbar scrolling and BogoSorter's redirect. Cancel the
        // wheel event instead of competing to redirect InventoryPlayer.changeCurrentItem.
        if (InputApi.isBlocked(InputAction.PLAYER_HOTBAR)) event.setCanceled(true);
    }

    @SubscribeEvent
    public void clientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
            FreeLookCameraRig.beginTick();
            return;
        }
        if (event.phase != TickEvent.Phase.END) return;
        EntityPlayerSP player = Minecraft.getMinecraft().thePlayer;
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
        if (Minecraft.getMinecraft().currentScreen != null) {
            CameraRuntime.advanceCameraTick();
            return;
        }
        boolean exitPressed = pressed(CameraKeyBindings.EXIT_CAMERA,
                input, InputAction.CAMERA_EXIT_DRONE);
        boolean dronePressed = pressed(CameraKeyBindings.TOGGLE_DRONE,
                input, InputAction.CAMERA_TOGGLE_DRONE);
        boolean freeLookPressed = pressed(CameraKeyBindings.TOGGLE_FREE_LOOK,
                input, InputAction.CAMERA_TOGGLE_FREELOOK);
        boolean shoulderPressed = pressed(CameraKeyBindings.TOGGLE_SHOULDER,
                input, InputAction.CAMERA_TOGGLE_SHOULDER);
        boolean swapShoulderPressed = pressed(CameraKeyBindings.SWAP_SHOULDER,
                input, InputAction.CAMERA_SWAP_SHOULDER);
        boolean freeLookControlPressed = pressed(CameraKeyBindings.FREELOOK_TOGGLE_CONTROL,
                input, InputAction.CAMERA_TOGGLE_FREELOOK_CONTROL);
        boolean cursorLookPressed = pressed(CameraKeyBindings.TOGGLE_CURSOR_LOOK,
                input, InputAction.CAMERA_TOGGLE_CURSOR_LOOK);
        if (exitPressed) closeInteractiveSession();
        if (dronePressed) {
            toggle(CameraRigRequest.drone(100));
        }
        if (FreeLookConfig.toggleMode) {
            if (freeLookPressed) {
                toggle(CameraRigRequest.freeLook(100));
            }
        } else {
            if (CameraKeyBindings.TOGGLE_FREE_LOOK.getIsKeyPressed()
                    || input.get(InputAction.CAMERA_TOGGLE_FREELOOK).isDown()) {
                if (!interactiveSession.isActive())
                    acquireInteractive(CameraRigRequest.freeLook(100));
            } else if (interactiveSession.isActive()
                    && interactiveRigId != null
                    && "free_look".equals(interactiveRigId.getResourcePath())) {
                closeInteractiveSession();
            }
        }
        if (shoulderPressed) {
            toggle(CameraRigRequest.shoulder(100));
        }
        if (swapShoulderPressed) {
            CameraRuntime.swapShoulder();
        }
        if (CameraRuntime.isShoulderActive()
                || (CameraRuntime.isCursorLookActive() && CursorLookConfig.useShoulderOffset)) {
            if (CameraKeyBindings.ADJUST_LEFT.isPressed()) ShoulderCameraConfig.adjustX(ShoulderCameraConfig.cameraStepSize);
            if (CameraKeyBindings.ADJUST_RIGHT.isPressed()) ShoulderCameraConfig.adjustX(-ShoulderCameraConfig.cameraStepSize);
            if (CameraKeyBindings.ADJUST_UP.isPressed()) ShoulderCameraConfig.adjustY(ShoulderCameraConfig.cameraStepSize);
            if (CameraKeyBindings.ADJUST_DOWN.isPressed()) ShoulderCameraConfig.adjustY(-ShoulderCameraConfig.cameraStepSize);
            if (CameraKeyBindings.ADJUST_IN.isPressed()) ShoulderCameraConfig.adjustZ(-ShoulderCameraConfig.cameraStepSize);
            if (CameraKeyBindings.ADJUST_OUT.isPressed()) ShoulderCameraConfig.adjustZ(ShoulderCameraConfig.cameraStepSize);
        }
        if (CameraRuntime.isLookCameraActive() && freeLookControlPressed) {
            CameraRuntime.toggleFreeLookControl();
        }
        if (cursorLookPressed) {
            toggle(CameraRigRequest.cursorLook(100));
        }
        if (CameraRuntime.isFreeLookActive()
                || (CameraRuntime.isCursorLookActive() && !CursorLookConfig.useShoulderOffset)) {
            double step = FreeLookConfig.moveStepSize;
            if (CameraKeyBindings.FREELOOK_MOVE_UP.getIsKeyPressed()) FreeLookCameraRig.adjustMoveOffset(0, step, 0);
            if (CameraKeyBindings.FREELOOK_MOVE_DOWN.getIsKeyPressed()) FreeLookCameraRig.adjustMoveOffset(0, -step, 0);
            // CameraAttitude.right() = rotate(-1,0,0), so positive X = left, negative X = right
            if (CameraKeyBindings.FREELOOK_MOVE_LEFT.getIsKeyPressed()) FreeLookCameraRig.adjustMoveOffset(step, 0, 0);
            if (CameraKeyBindings.FREELOOK_MOVE_RIGHT.getIsKeyPressed()) FreeLookCameraRig.adjustMoveOffset(-step, 0, 0);
            // CameraAttitude.forward() = rotate(0,0,1), positive Z = forward, negative Z = backward
            if (CameraKeyBindings.FREELOOK_MOVE_FORWARD.getIsKeyPressed()) FreeLookCameraRig.adjustMoveOffset(0, 0, step);
            if (CameraKeyBindings.FREELOOK_MOVE_BACKWARD.getIsKeyPressed()) FreeLookCameraRig.adjustMoveOffset(0, 0, -step);
            if (CameraKeyBindings.FREELOOK_MOVE_RESET.isPressed()) FreeLookCameraRig.resetMoveOffset();
        }
        CameraRuntime.advanceCameraTick();
    }

    @SubscribeEvent
    public void worldUnload(WorldEvent.Unload event) {
        if (event.world != null && event.world.isRemote) {
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
                || ("drone".equals(request.id().getResourcePath())
                && CameraRuntime.isDroneActive())
                || ("free_look".equals(request.id().getResourcePath())
                && CameraRuntime.isFreeLookActive())
                || ("cursor_look".equals(request.id().getResourcePath())
                && CameraRuntime.isCursorLookActive())
                || ("shoulder".equals(request.id().getResourcePath())
                && CameraRuntime.isShoulderActive()));
        if (interactiveSession.isActive()
                || CameraPerspectiveController.hasActiveMode()
                || CameraRuntime.isDroneActive() || CameraRuntime.isLookCameraActive()
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

    private boolean pressed(KeyBinding binding, InputFrame frame, InputAction action) {
        boolean routed = commandEdges.pressed(frame, action);
        boolean physical = binding.isPressed();
        if (physical) commandEdges.consumePhysicalPress(action);
        return physical || routed;
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
