package neofontrender.addons.api.input;

/** Diagnostic reason for a neutral input boundary. */
public enum InputFlushReason {
    MODE_ENTER,
    MODE_EXIT,
    FOCUS_LOST,
    WORLD_CHANGE,
    DISCONNECT,
    SESSION_SUSPENDED
}
