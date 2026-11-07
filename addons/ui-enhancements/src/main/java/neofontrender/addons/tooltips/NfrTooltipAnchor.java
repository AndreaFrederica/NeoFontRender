package neofontrender.addons.tooltips;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Marker-line grammar for document visual anchors:
 * {@code <nfr:anchor family="items" producer="ae2" custom="..."/>}.
 * <p>
 * The marker declares which content family is drawn at its line, which mod inserted it,
 * and an optional integration-defined instance id. It is a single, self-closing line:
 * the layout treats it as a zero-advance position carrier, so the real height of the
 * anchored content stays owned by the modern layout.
 */
final class NfrTooltipAnchor {
    static final String PREFIX = "<nfr:anchor";
    static final String FAMILY_ITEMS = "items";
    static final String FAMILY_TEXT = "text";

    final String family;
    final String producer;
    /** Integration-defined instance id, or {@code null} to accept the whole family. */
    final String custom;
    /** Recognized-but-unknown attributes, kept so newer producers do not break older parsers. */
    final Map<String, String> unknownAttributes;

    private NfrTooltipAnchor(String family, String producer, String custom,
                             Map<String, String> unknownAttributes) {
        this.family = family;
        this.producer = producer;
        this.custom = custom;
        this.unknownAttributes = Collections.unmodifiableMap(unknownAttributes);
    }

    static boolean isAnchorLine(String line) {
        return anchorStart(line) >= 0;
    }

    // GuiScreen.getItemToolTip prefixes non-title lines with GRAY. Formatting
    // belongs to the transport, not the marker grammar or its layout position.
    private static int anchorStart(String line) {
        if (line == null) return -1;
        int index = 0;
        while (index < line.length()) {
            if (Character.isWhitespace(line.charAt(index))) index++;
            else if (index + 1 < line.length() && line.charAt(index) == '\u00a7'
                    && isFormattingCode(line.charAt(index + 1))) index += 2;
            else break;
        }
        return line.startsWith(PREFIX, index) ? index : -1;
    }

    private static boolean isFormattingCode(char code) {
        code = Character.toLowerCase(code);
        return code >= '0' && code <= '9' || code >= 'a' && code <= 'f'
                || code >= 'k' && code <= 'o' || code == 'r';
    }

    static String format(String family, String producer, String custom) {
        if (custom == null || custom.isEmpty()) {
            return neofontrender.api.client.tooltip.NfrTooltipApi.visualAnchorLine(family, producer);
        }
        StringBuilder marker = new StringBuilder(PREFIX)
                .append(" family=\"").append(family).append('"')
                .append(" producer=\"").append(producer).append('"');
        if (custom != null && !custom.isEmpty()) {
            marker.append(" custom=\"").append(custom).append('"');
        }
        return marker.append("/>").toString();
    }

    /** Replaces every run of reserved placeholder lines with one marker at the same position. */
    static List<String> foldReservedBlocks(List<String> lines, Predicate<String> isReserved,
                                           String family, String producer) {
        if (lines == null || lines.isEmpty() || isReserved == null) return lines;
        List<String> folded = null;
        int size = lines.size();
        int index = 0;
        while (index < size) {
            if (!isReserved.test(lines.get(index))) {
                if (folded != null) folded.add(lines.get(index));
                index++;
                continue;
            }
            if (folded == null) folded = new ArrayList<>(lines.subList(0, index));
            folded.add(format(family, producer, null));
            while (index < size && isReserved.test(lines.get(index))) index++;
        }
        return folded == null ? lines : folded;
    }

    static FamilyScan scanFamily(List<String> lines, String family) {
        if (lines == null || lines.isEmpty() || family == null) return FamilyScan.EMPTY;
        NfrTooltipAnchor first = null;
        int firstIndex = -1;
        int count = 0;
        Set<String> producers = new LinkedHashSet<>();
        for (int i = 0; i < lines.size(); i++) {
            NfrTooltipAnchor anchor = parse(lines.get(i));
            if (anchor == null || !family.equals(anchor.family)) continue;
            if (first == null) {
                first = anchor;
                firstIndex = i;
            }
            count++;
            producers.add(anchor.producer);
        }
        return count == 0 ? FamilyScan.EMPTY : new FamilyScan(first, firstIndex, count, producers);
    }

    /** Parses one marker line, or returns {@code null} when the line is not a valid marker. */
    static NfrTooltipAnchor parse(String line) {
        int start = anchorStart(line);
        if (start < 0) return null;
        int end = line.length();
        while (end > start) {
            if (Character.isWhitespace(line.charAt(end - 1))) end--;
            else if (end >= start + 2 && line.charAt(end - 2) == '\u00a7'
                    && isFormattingCode(line.charAt(end - 1))) end -= 2;
            else break;
        }
        if (end <= start || line.charAt(end - 1) != '>') return null;
        String body = line.substring(start + PREFIX.length(), end - 1).trim();
        if (body.endsWith("/")) body = body.substring(0, body.length() - 1).trim();
        Map<String, String> attributes = new LinkedHashMap<>();
        int index = 0;
        while (index < body.length()) {
            while (index < body.length() && Character.isWhitespace(body.charAt(index))) index++;
            if (index >= body.length()) break;
            int keyStart = index;
            while (index < body.length() && body.charAt(index) != '='
                    && !Character.isWhitespace(body.charAt(index))) {
                index++;
            }
            String key = body.substring(keyStart, index);
            while (index < body.length() && Character.isWhitespace(body.charAt(index))) index++;
            if (key.isEmpty() || index >= body.length() || body.charAt(index) != '=') return null;
            index++;
            while (index < body.length() && Character.isWhitespace(body.charAt(index))) index++;
            if (index >= body.length() || body.charAt(index) != '"') return null;
            index++;
            int valueStart = index;
            while (index < body.length() && body.charAt(index) != '"') index++;
            if (index >= body.length()) return null;
            String value = body.substring(valueStart, index);
            index++;
            if (!isToken(value)) return null;
            attributes.put(key, value);
        }
        String family = attributes.remove("family");
        String producer = attributes.remove("producer");
        String custom = attributes.remove("custom");
        if (family == null || producer == null) return null;
        return new NfrTooltipAnchor(family, producer, custom, attributes);
    }

    private static boolean isToken(String value) {
        if (value == null || value.isEmpty()) return false;
        for (int i = 0; i < value.length(); i++) {
            char character = value.charAt(i);
            boolean allowed = character >= 'a' && character <= 'z'
                    || character >= '0' && character <= '9'
                    || character == '_' || character == '.' || character == '-';
            if (!allowed) return false;
        }
        return true;
    }

    /** Result of scanning one document for anchors of a single content family. */
    static final class FamilyScan {
        static final FamilyScan EMPTY = new FamilyScan(null, -1, 0, Collections.emptySet());

        final NfrTooltipAnchor anchor;
        final int firstIndex;
        final int count;
        final Set<String> producers;

        FamilyScan(NfrTooltipAnchor anchor, int firstIndex, int count, Set<String> producers) {
            this.anchor = anchor;
            this.firstIndex = firstIndex;
            this.count = count;
            this.producers = producers;
        }

        boolean found() {
            return count > 0;
        }
    }
}
