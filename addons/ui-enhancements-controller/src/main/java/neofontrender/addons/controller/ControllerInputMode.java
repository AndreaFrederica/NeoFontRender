package neofontrender.addons.controller;

import net.minecraft.client.Minecraft;
import neofontrender.addons.api.camera.CameraApi;
import neofontrender.addons.api.flight.FlightApi;
import neofontrender.addons.api.input.InputAction;

/** Selects the controller action sets that may publish into the current UIE input frame. */
public enum ControllerInputMode {
    GAMEPLAY,
    GUI,
    CAMERA,
    FLIGHT;

    public static ControllerInputMode current() {
        Minecraft minecraft = Minecraft.getMinecraft();
        return select(minecraft.currentScreen != null, FlightApi.isActive(),
                CameraApi.isDroneActive() || CameraApi.isFreeLookActive());
    }

    static ControllerInputMode select(boolean gui, boolean flight, boolean detachedCamera) {
        if (gui) return GUI;
        if (detachedCamera) return CAMERA;
        if (flight) return FLIGHT;
        return GAMEPLAY;
    }

    public boolean accepts(InputAction action) {
        ControllerBindingGroup group = ControllerBindingGroup.of(action);
        switch (this) {
            case GUI:
                return group == ControllerBindingGroup.GUI;
            case CAMERA:
                return group == ControllerBindingGroup.CAMERA;
            case FLIGHT:
                return group == ControllerBindingGroup.FLIGHT
                        || group == ControllerBindingGroup.CAMERA
                        || isPlayerCommand(action);
            case GAMEPLAY:
            default:
                return group == ControllerBindingGroup.PLAYER
                        || group == ControllerBindingGroup.CAMERA;
        }
    }

    private static boolean isPlayerCommand(InputAction action) {
        switch (action) {
            case PLAYER_ATTACK:
            case PLAYER_USE:
            case PLAYER_PICK_BLOCK:
            case PLAYER_DROP:
            case PLAYER_INVENTORY:
            case PLAYER_SWAP_HANDS:
            case PLAYER_HOTBAR:
                return true;
            default:
                return false;
        }
    }
}
