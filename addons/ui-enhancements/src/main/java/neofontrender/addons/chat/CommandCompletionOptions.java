package neofontrender.addons.chat;

import java.util.Arrays;
import java.util.List;

/** Saved preferences are independent; unavailable optional implementations resolve to UIE. */
public final class CommandCompletionOptions {
    public static final String UIE = "uie";
    public static final String PREGEN = "pregenerator";
    public static final String HIDDEN = "hidden";

    private CommandCompletionOptions() {}

    static String engine(String value, boolean available) {
        return PREGEN.equals(value) && available ? PREGEN : UIE;
    }

    static String display(String value, boolean available) {
        return HIDDEN.equals(value) ? HIDDEN : engine(value, available);
    }

    static List<String> engines() {
        return PregenCompletionBridge.available() ? Arrays.asList(UIE, PREGEN) : Arrays.asList(UIE);
    }

    static List<String> displays() {
        return PregenCompletionBridge.available()
                ? Arrays.asList(UIE, PREGEN, HIDDEN) : Arrays.asList(UIE, HIDDEN);
    }

    static String engine() {
        return engine(EnhancedChatConfig.completionEngine, PregenCompletionBridge.available());
    }

    static String display() {
        return display(EnhancedChatConfig.completionDisplay, PregenCompletionBridge.available());
    }
}
