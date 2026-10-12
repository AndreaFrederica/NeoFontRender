package neofontrender.api.client.input;

/** Implemented on Minecraft KeyBinding by NFR so optional input addons can emulate one binding. */
public interface NfrKeyBindingControllerInput {
    void nfr$setControllerInput(boolean down, boolean pressed);
    void nfr$clearControllerInput();
}
