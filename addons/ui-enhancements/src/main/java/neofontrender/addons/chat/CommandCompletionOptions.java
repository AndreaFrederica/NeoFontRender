package neofontrender.addons.chat;

import java.util.Arrays;
import java.util.List;

/** Saved preferences are independent; unavailable optional implementations resolve to UIE. */
public final class CommandCompletionOptions {
    public static final String UIE = "uie";
    public static final String PREGEN = "pregenerator";
    public static final String HIDDEN = "hidden";
    public static final String CLEANROOM = "cleanroom";

    private CommandCompletionOptions() {}

    static String engine(String value, boolean available) {
        if (PREGEN.equals(value) && available) return PREGEN;
        if (CLEANROOM.equals(value) && CleanroomCommandCompletionCompat.available()) return CLEANROOM;
        return UIE;
    }

    static String display(String value, boolean available) {
        return HIDDEN.equals(value) ? HIDDEN : engine(value, available);
    }

    static List<String> engines() {
        java.util.ArrayList<String> result = new java.util.ArrayList<>();
        result.add(UIE);
        if (PregenCompletionBridge.available()) result.add(PREGEN);
        if (CleanroomCommandCompletionCompat.available()) result.add(CLEANROOM);
        return result;
    }

    static List<String> displays() {
        java.util.ArrayList<String> result = new java.util.ArrayList<>(engines());
        result.add(HIDDEN);
        return result;
    }

    static String engine() {
        return engine(EnhancedChatConfig.completionEngine, PregenCompletionBridge.available());
    }

    static String display() {
        return display(EnhancedChatConfig.completionDisplay, PregenCompletionBridge.available());
    }
}
