package neofontrender.addons.chat;

import java.util.ArrayList;
import java.util.List;

/** Saved preferences are independent; unavailable optional implementations resolve to UIE. */
public final class CommandCompletionOptions {
    public static final String UIE = "uie";
    public static final String HIDDEN = "hidden";
    public static final String CLEANROOM = "cleanroom";

    private CommandCompletionOptions() {}

    static String engine(String value) {
        if (CLEANROOM.equals(value) && CleanroomCommandCompletionCompat.available()) return CLEANROOM;
        return UIE;
    }

    static String display(String value) {
        return HIDDEN.equals(value) ? HIDDEN : engine(value);
    }

    static List<String> engines() {
        List<String> result = new ArrayList<>();
        result.add(UIE);
        if (CleanroomCommandCompletionCompat.available()) result.add(CLEANROOM);
        return result;
    }

    static List<String> displays() {
        List<String> result = new ArrayList<>(engines());
        result.add(HIDDEN);
        return result;
    }

    static String engine() {
        return engine(EnhancedChatConfig.completionEngine);
    }

    static String display() {
        return display(EnhancedChatConfig.completionDisplay);
    }
}
