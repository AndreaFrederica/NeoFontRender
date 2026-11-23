package neofontrender.addons.chat;

import net.minecraft.util.text.TextFormatting;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Merges Forge client and server completions while preserving their display source. */
public final class CommandCompletionCandidates {
    private CommandCompletionCandidates() {}

    public static Merge merge(String[] serverValues, String[] clientValues) {
        LinkedHashMap<String, Candidate> candidates = new LinkedHashMap<>();
        add(candidates, clientValues, Source.CLIENT);
        add(candidates, serverValues, Source.SERVER);
        return new Merge(new ArrayList<>(candidates.values()));
    }

    public static String plain(String value) {
        String plain = TextFormatting.getTextWithoutFormattingCodes(value);
        return plain == null ? "" : plain;
    }

    public static String styled(String value, Source source) {
        String plain = plain(value);
        if (isPlaceholder(plain)) {
            return TextFormatting.DARK_GRAY + plain + TextFormatting.RESET;
        }
        TextFormatting color = source == Source.CLIENT ? TextFormatting.GOLD : TextFormatting.AQUA;
        return color + plain + TextFormatting.RESET;
    }

    /**
     * Argument placeholders such as FancyMenu's {@code <menu_identifier>} are legitimate entries in
     * a command's completion list, but inserting one types the angle brackets into the command.
     * They stay selectable and are shown in a distinct color so they read as a slot, not a value.
     */
    public static boolean isPlaceholder(String value) {
        String plain = plain(value).trim();
        return plain.length() > 2 && plain.charAt(0) == '<' && plain.charAt(plain.length() - 1) == '>';
    }

    private static void add(Map<String, Candidate> candidates, String[] values, Source source) {
        if (values == null) return;
        for (String raw : values) {
            String value = plain(raw);
            if (value.isEmpty()) continue;
            candidates.putIfAbsent(key(value), new Candidate(value, source));
        }
    }

    private static String key(String value) {
        String normalized = plain(value);
        if (normalized.startsWith("/")) normalized = normalized.substring(1);
        return normalized.toLowerCase(Locale.ROOT);
    }

    public enum Source {
        CLIENT,
        SERVER
    }

    public static final class Merge {
        private final List<Candidate> candidates;
        private final Map<String, Source> sources = new LinkedHashMap<>();
        private List<String> plain;

        private Merge(List<Candidate> candidates) {
            this.candidates = candidates;
            for (Candidate candidate : candidates) {
                sources.put(key(candidate.value), candidate.source);
            }
        }

        public String[] plainValues() {
            return plainValuesList().toArray(new String[0]);
        }

        /** Cached plain values, so callers can compare candidate identity without rebuilding it. */
        public List<String> plainValuesList() {
            if (plain == null) {
                List<String> values = new ArrayList<>(candidates.size());
                for (Candidate candidate : candidates) values.add(candidate.value);
                plain = values;
            }
            return plain;
        }

        public List<String> styledValues() {
            List<String> values = new ArrayList<>(candidates.size());
            for (Candidate candidate : candidates) {
                values.add(styled(candidate.value, candidate.source));
            }
            return values;
        }

        public Source sourceOf(String value) {
            Source source = sources.get(key(value));
            return source == null ? Source.SERVER : source;
        }
    }

    private static final class Candidate {
        private final String value;
        private final Source source;

        private Candidate(String value, Source source) {
            this.value = value;
            this.source = source;
        }
    }
}
