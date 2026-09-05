package neofontrender.text.syntax;

import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/** Configurable Brilliant Text controls with optional any-position startup. */
public final class BrilliantSyntaxProvider implements FixedCodeSyntaxProvider {
    public static final String GROUP_ID = "brilliant_text:active";

    private final Map<Character, EffectDescriptor> effects;
    private final Set<Character> codes;
    private final boolean anyPosition;

    public BrilliantSyntaxProvider(Map<Character, Map<String, String>> bindings) {
        this(bindings, false);
    }

    public BrilliantSyntaxProvider(Map<Character, Map<String, String>> bindings, boolean anyPosition) {
        this.anyPosition = anyPosition;
        LinkedHashMap<Character, EffectDescriptor> values = new LinkedHashMap<>();
        if (bindings != null) {
            for (Map.Entry<Character, Map<String, String>> entry : bindings.entrySet()) {
                char code = Character.toLowerCase(entry.getKey());
                if ("0123456789abcdefklmnor".indexOf(code) >= 0) continue;
                String effectId = code == 'v' ? "brilliant_text:flame" : "brilliant_text:" + code;
                values.put(code, new EffectDescriptor(GROUP_ID, effectId, entry.getValue(),
                        EnumSet.of(SyntaxEvent.COLOR_CHANGE, SyntaxEvent.RESET), true));
            }
        }
        effects = Collections.unmodifiableMap(values);
        codes = Collections.unmodifiableSet(new LinkedHashSet<>(values.keySet()));
    }

    @Override public String id() { return "brilliant_text:format_codes"; }
    @Override public int priority() { return 100; }
    @Override public char trigger() { return MinecraftLegacySyntaxProvider.PREFIX; }
    @Override public Set<Character> codes() { return codes; }

    @Override
    public SyntaxMatch match(SyntaxCursor cursor) {
        if (cursor.remaining() < 2 || cursor.charAt(0) != trigger()) return null;
        EffectDescriptor effect = effects.get(Character.toLowerCase(cursor.charAt(1)));
        if (effect == null) return null;
        if (!anyPosition && !cursor.lineLeading() && !cursor.hasActiveEffectGroup(GROUP_ID)) return null;
        return SyntaxMatch.operations(2, SyntaxOperation.beginEffect(effect));
    }

    public static BrilliantSyntaxProvider defaults() {
        Map<Character, Map<String, String>> values = new LinkedHashMap<>();
        values.put('g', colors("FF986B31", "FFFCE670", "FFFCE670"));
        values.put('s', colors("FF4C5E6F", "FFD5EAF8", "00000000"));
        values.put('q', colors("FF60241E", "FFE77B49", "00000000"));
        values.put('v', colors("FFFFFFFF", "00000000", "00000000"));
        return new BrilliantSyntaxProvider(values);
    }

    private static Map<String, String> colors(String text, String outline, String glow) {
        Map<String, String> values = new LinkedHashMap<>();
        values.put("textColor", text);
        values.put("outlineColor", outline);
        values.put("glowColor", glow);
        return values;
    }
}
