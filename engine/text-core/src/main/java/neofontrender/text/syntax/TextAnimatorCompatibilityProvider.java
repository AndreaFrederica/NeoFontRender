package neofontrender.text.syntax;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import neofontrender.text.animation.TextAnimationRenderMode;

/** Parser compatibility for TextAnimator's angle-bracket effect tags. */
public final class TextAnimatorCompatibilityProvider implements TextSyntaxProvider {
    public static final String GROUP_PREFIX = "textanimator_active:effect_";
    private static final Pattern OPEN = Pattern.compile("^<([a-zA-Z][a-zA-Z0-9_]*)(?:\\s+([^>]*))?>");
    private static final Pattern CLOSE = Pattern.compile("^</([a-zA-Z][a-zA-Z0-9_]*)\\s*>");
    private static final Set<String> EFFECTS = Collections.unmodifiableSet(new LinkedHashSet<>(Arrays.asList(
            "wave", "wiggle", "shake", "bounce", "swing", "pend", "turb", "fade",
            "pulse", "rainb", "grad", "shadow", "neon", "glitch", "scroll", "typewriter")));

    private final boolean enabled;
    private final boolean anyPosition;
    private final String effectsFilter;
    private final int typewriterSpeed;
    private final String typewriterMode;
    private final double pulseMinimum;
    private final double pulseMaximum;

    public TextAnimatorCompatibilityProvider() { this(true, true, "all", 5, "by_char", 0.6, 1.0); }
    public TextAnimatorCompatibilityProvider(boolean enabled, boolean anyPosition) {
        this(enabled, anyPosition, "all", 5, "by_char", 0.6, 1.0);
    }
    public TextAnimatorCompatibilityProvider(boolean enabled, boolean anyPosition, String effectsFilter) {
        this(enabled, anyPosition, effectsFilter, 5, "by_char", 0.6, 1.0);
    }
    public TextAnimatorCompatibilityProvider(boolean enabled, boolean anyPosition, String effectsFilter,
                                             int typewriterSpeed, String typewriterMode) {
        this(enabled, anyPosition, effectsFilter, typewriterSpeed, typewriterMode, 0.6, 1.0);
    }

    public TextAnimatorCompatibilityProvider(boolean enabled, boolean anyPosition, String effectsFilter,
                                             int typewriterSpeed, String typewriterMode,
                                             double pulseMinimum, double pulseMaximum) {
        this.enabled = enabled;
        this.anyPosition = anyPosition;
        String filter = effectsFilter == null ? "all" : effectsFilter.trim().toLowerCase(Locale.ROOT);
        this.effectsFilter = "none".equals(filter) || "no_rainbow".equals(filter) ? filter : "all";
        this.typewriterSpeed = Math.max(1, Math.min(9, typewriterSpeed));
        this.typewriterMode = "by_word".equals(typewriterMode) ? "by_word" : "by_char";
        this.pulseMinimum = clampBrightness(pulseMinimum, 0.0, 1.0);
        this.pulseMaximum = Math.max(this.pulseMinimum, clampBrightness(pulseMaximum, 0.0, 1.0));
    }

    @Override public String id() { return "textanimator:compatibility"; }
    @Override public int priority() { return 90; }
    @Override public char trigger() { return '<'; }
    @Override public boolean isEnabled() { return enabled; }
    @Override
    public SyntaxMatch match(SyntaxCursor cursor) {
        if (!anyPosition && !cursor.lineLeading()) return null;
        String value = cursor.remainingText();
        Matcher close = CLOSE.matcher(value);
        if (close.find() && EFFECTS.contains(close.group(1).toLowerCase(Locale.ROOT))) {
            String group = GROUP_PREFIX + close.group(1).toLowerCase(Locale.ROOT);
            if (!group.equals(cursor.topActiveEffectGroup())) return null;
            return SyntaxMatch.operations(close.end(), SyntaxOperation.endEffect(
                    group));
        }
        Matcher open = OPEN.matcher(value);
        if (!open.find()) return null;
        String name = open.group(1).toLowerCase(Locale.ROOT);
        if (!EFFECTS.contains(name)) return null;
        // Typewriter is allowed after chat prefixes (for example a player name). The
        // provider-level anyPosition switch is the single position policy for every effect.
        if ("typewriter".equals(name)
                && cursor.hasActiveEffectGroup(GROUP_PREFIX + name)) return null;
        Map<String, String> parameters = ParameterParser.parse(open.group(2));
        if ("typewriter".equals(name)) {
            parameters.put("speed", Integer.toString(this.typewriterSpeed));
            parameters.put("mode", this.typewriterMode);
        }
        if ("pulse".equals(name) && !parameters.containsKey("base")
                && !parameters.containsKey("a") && !parameters.containsKey("min")
                && !parameters.containsKey("max")) {
            parameters.put("_defaultMin", Double.toString(this.pulseMinimum));
            parameters.put("_defaultMax", Double.toString(this.pulseMaximum));
        }
        EffectDescriptor descriptor = new EffectDescriptor(GROUP_PREFIX + name,
                "textanimator:" + name, parameters, EnumSet.noneOf(SyntaxEvent.class), false,
                renderMode(name), true);
        return SyntaxMatch.operations(open.end(), SyntaxOperation.beginEffect(descriptor));
    }

    private TextAnimationRenderMode renderMode(String name) {
        boolean disabled = "none".equals(effectsFilter)
                || ("no_rainbow".equals(effectsFilter)
                && ("rainb".equals(name) || "grad".equals(name)));
        return disabled ? TextAnimationRenderMode.WHOLE_RUN : TextAnimationRenderMode.GLYPH;
    }

    private static double clampBrightness(double value, double minimum, double maximum) {
        return Double.isFinite(value) ? Math.max(minimum, Math.min(maximum, value)) : minimum;
    }

    private static final class ParameterParser {
        static Map<String, String> parse(String raw) {
            java.util.LinkedHashMap<String, String> result = new java.util.LinkedHashMap<>();
            if (raw == null || raw.trim().isEmpty()) return result;
            for (String token : raw.trim().split("\\s+")) {
                int equals = token.indexOf('=');
                if (equals < 0) {
                    result.put(token.toLowerCase(Locale.ROOT), "true");
                } else if (equals > 0 && equals + 1 < token.length()) {
                    result.put(token.substring(0, equals).toLowerCase(Locale.ROOT), token.substring(equals + 1));
                }
            }
            return result;
        }
    }
}
