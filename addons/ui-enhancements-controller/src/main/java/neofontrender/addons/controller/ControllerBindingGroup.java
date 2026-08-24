package neofontrender.addons.controller;

import neofontrender.addons.api.input.InputAction;

/** Stable controller contexts shared by runtime routing and the settings workbench. */
enum ControllerBindingGroup {
    PLAYER("player"),
    GUI("gui"),
    CAMERA("camera"),
    FLIGHT("flight");

    private final String key;

    ControllerBindingGroup(String key) {
        this.key = key;
    }

    String title() {
        return ControllerText.tr("gui.bindings_group_" + key);
    }

    boolean contains(InputAction action) {
        if (action == null) return false;
        String name = action.name();
        switch (this) {
            case PLAYER: return name.startsWith("PLAYER_");
            case GUI: return name.startsWith("GUI_");
            case FLIGHT: return name.startsWith("FLIGHT_");
            case CAMERA: return name.startsWith("CAMERA_");
            default: return false;
        }
    }

    static ControllerBindingGroup of(InputAction action) {
        for (ControllerBindingGroup group : values()) {
            if (group.contains(action)) return group;
        }
        throw new IllegalArgumentException("Unclassified controller action " + action);
    }
}
