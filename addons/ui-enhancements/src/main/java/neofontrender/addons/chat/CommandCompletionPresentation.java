package neofontrender.addons.chat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraftforge.client.ClientCommandHandler;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/** Shared visual state for command suggestions rendered by embedded chat inputs. */
public final class CommandCompletionPresentation {
    private static final int[] ARGUMENT_COLORS = {
            0xFFFFAA00, 0xFFFFFF55, 0xFF55FFFF,
            0xFF5555FF, 0xFFAA00FF, 0xFFFF55FF
    };
    private static final Map<GuiTextField, Snapshot> SNAPSHOTS = new WeakHashMap<>();
    private static final Set<String> KNOWN_COMMANDS = new HashSet<>();
    private static WeakReference<NetHandlerPlayClient> lastConnection =
            new WeakReference<>(null);

    private CommandCompletionPresentation() {}

    public static boolean enabled(GuiTextField field) {
        return field != null && EnhancedChatConfigAccess.tabbedChatEnabled()
                && EnhancedChatConfigAccess.commandCompletionEnabled();
    }

    public static void update(GuiTextField field, List<String> candidates, int selected) {
        if (!enabled(field) || candidates == null || candidates.isEmpty()) {
            clear(field);
            return;
        }
        List<String> plain = new ArrayList<>(candidates.size());
        for (String candidate : candidates) {
            String value = CommandCompletionCandidates.plain(candidate);
            if (!value.isEmpty()) plain.add(value);
        }
        if (plain.isEmpty()) {
            clear(field);
            return;
        }
        SNAPSHOTS.put(field, new Snapshot(plain, selected));
        String text = field.getText();
        int cursor = Math.max(0, Math.min(field.getCursorPosition(), text.length()));
        rememberRootCandidates(text.substring(0, cursor), plain);
    }

    public static void clear(GuiTextField field) {
        if (field != null) SNAPSHOTS.remove(field);
    }

    public static String ghostSuffix(GuiTextField field) {
        if (!enabled(field)) return "";
        Snapshot snapshot = SNAPSHOTS.get(field);
        if (snapshot == null || snapshot.candidates.isEmpty()) return "";
        return ghostSuffix(field.getText(), field.getCursorPosition(),
                snapshot.candidates, snapshot.selected);
    }

    static String ghostSuffix(String text, int cursor, List<String> candidates, int selected) {
        if (text == null || candidates == null || candidates.isEmpty()) return "";
        cursor = Math.max(0, Math.min(cursor, text.length()));
        if (cursor != text.length()) return "";
        ChatCommandCompletionController.TokenRange range =
                ChatCommandCompletionController.tokenRange(text, cursor);
        String typed = text.substring(range.start, cursor);
        String candidate = candidates.get(selected >= 0 && selected < candidates.size()
                ? selected : 0);
        candidate = ChatCommandCompletionController.insertionValue(text, range, candidate);
        if (typed.isEmpty() || !startsWithIgnoreCase(candidate, typed)) return "";
        return candidate.substring(typed.length());
    }

    public static List<ColoredRange> coloredRanges(GuiTextField field) {
        if (!enabled(field)) return java.util.Collections.emptyList();
        String text = field.getText();
        if (text.isEmpty() || !text.startsWith("/")) {
            return java.util.Collections.emptyList();
        }
        refreshKnownCommands();
        List<ColoredRange> ranges = new ArrayList<>();
        int token = 0;
        int cursor = 0;
        while (cursor < text.length()) {
            while (cursor < text.length() && Character.isWhitespace(text.charAt(cursor))) cursor++;
            if (cursor >= text.length()) break;
            int start = cursor;
            while (cursor < text.length() && !Character.isWhitespace(text.charAt(cursor))) cursor++;
            int color;
            if (token == 0) {
                String root = text.substring(start, cursor);
                if (root.startsWith("/")) root = root.substring(1);
                color = KNOWN_COMMANDS.contains(root.toLowerCase(Locale.ROOT))
                        ? 0xFF55FF55 : 0xFFFF5555;
            } else {
                color = ARGUMENT_COLORS[(token - 1) % ARGUMENT_COLORS.length];
            }
            ranges.add(new ColoredRange(start, cursor, color));
            token++;
        }
        return ranges;
    }

    static void rememberRootCandidates(String input, List<String> candidates) {
        refreshKnownCommands();
        if (input == null || !input.startsWith("/") || containsWhitespace(input)) return;
        for (String candidate : candidates) {
            String command = CommandCompletionCandidates.plain(candidate);
            if (command.startsWith("/")) command = command.substring(1);
            int separator = firstWhitespace(command);
            if (separator >= 0) command = command.substring(0, separator);
            if (!command.isEmpty()) KNOWN_COMMANDS.add(command.toLowerCase(Locale.ROOT));
        }
    }

    private static void refreshKnownCommands() {
        Minecraft minecraft = Minecraft.getMinecraft();
        NetHandlerPlayClient connection = minecraft.player == null
                ? null : minecraft.player.connection;
        if (connection != lastConnection.get()) {
            KNOWN_COMMANDS.clear();
            lastConnection = new WeakReference<>(connection);
        }
        for (String command : ClientCommandHandler.instance.getCommands().keySet()) {
            KNOWN_COMMANDS.add(command.toLowerCase(Locale.ROOT));
        }
    }

    private static boolean startsWithIgnoreCase(String value, String prefix) {
        return prefix.length() <= value.length()
                && value.regionMatches(true, 0, prefix, 0, prefix.length());
    }

    private static boolean containsWhitespace(String value) {
        return firstWhitespace(value) >= 0;
    }

    private static int firstWhitespace(String value) {
        for (int index = 0; index < value.length(); index++) {
            if (Character.isWhitespace(value.charAt(index))) return index;
        }
        return -1;
    }

    public static final class ColoredRange {
        public final int start;
        public final int end;
        public final int color;

        private ColoredRange(int start, int end, int color) {
            this.start = start;
            this.end = end;
            this.color = color;
        }
    }

    private static final class Snapshot {
        private final List<String> candidates;
        private final int selected;

        private Snapshot(List<String> candidates, int selected) {
            this.candidates = candidates;
            this.selected = selected;
        }
    }
}
