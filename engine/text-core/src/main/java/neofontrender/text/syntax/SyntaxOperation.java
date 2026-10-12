package neofontrender.text.syntax;

import java.util.Objects;

/** Typed state operation returned by a syntax provider. */
public final class SyntaxOperation {
    public enum Kind {
        SET_COLOR,
        SET_COLOR_OVERRIDE,
        RESET_STYLE,
        SET_OBFUSCATED,
        SET_BOLD,
        SET_ITALIC,
        SET_UNDERLINE,
        SET_STRIKETHROUGH,
        BEGIN_EFFECT,
        END_EFFECT
    }

    private final Kind kind;
    private final int color;
    private final boolean enabled;
    private final String effectGroup;
    private final EffectDescriptor effect;

    private SyntaxOperation(Kind kind, int color, boolean enabled,
                            String effectGroup, EffectDescriptor effect) {
        this.kind = Objects.requireNonNull(kind, "kind");
        this.color = color;
        this.enabled = enabled;
        this.effectGroup = effectGroup;
        this.effect = effect;
    }

    public static SyntaxOperation color(int rgb) {
        return new SyntaxOperation(Kind.SET_COLOR, rgb & 0xFFFFFF, true, null, null);
    }

    /** Changes only RGB, preserving the active Minecraft style flags. */
    public static SyntaxOperation colorOverride(int rgb) {
        return new SyntaxOperation(Kind.SET_COLOR_OVERRIDE, rgb & 0xFFFFFF, true, null, null);
    }

    public static SyntaxOperation reset() {
        return new SyntaxOperation(Kind.RESET_STYLE, 0, false, null, null);
    }

    public static SyntaxOperation obfuscated(boolean value) { return flag(Kind.SET_OBFUSCATED, value); }
    public static SyntaxOperation bold(boolean value) { return flag(Kind.SET_BOLD, value); }
    public static SyntaxOperation italic(boolean value) { return flag(Kind.SET_ITALIC, value); }
    public static SyntaxOperation underline(boolean value) { return flag(Kind.SET_UNDERLINE, value); }
    public static SyntaxOperation strikethrough(boolean value) { return flag(Kind.SET_STRIKETHROUGH, value); }

    private static SyntaxOperation flag(Kind kind, boolean value) {
        return new SyntaxOperation(kind, 0, value, null, null);
    }

    public static SyntaxOperation beginEffect(EffectDescriptor effect) {
        return new SyntaxOperation(Kind.BEGIN_EFFECT, 0, true, null,
                Objects.requireNonNull(effect, "effect"));
    }

    public static SyntaxOperation endEffect(String groupId) {
        return new SyntaxOperation(Kind.END_EFFECT, 0, false,
                Objects.requireNonNull(groupId, "groupId"), null);
    }

    public Kind kind() { return kind; }
    public int color() { return color; }
    public boolean enabled() { return enabled; }
    public String effectGroup() { return effectGroup; }
    public EffectDescriptor effect() { return effect; }

    public SyntaxEvent event() {
        switch (kind) {
            case SET_COLOR:
            case SET_COLOR_OVERRIDE: return SyntaxEvent.COLOR_CHANGE;
            case RESET_STYLE: return SyntaxEvent.RESET;
            case SET_OBFUSCATED: return SyntaxEvent.OBFUSCATED;
            case SET_BOLD: return SyntaxEvent.WEIGHT;
            case SET_ITALIC: return SyntaxEvent.SLANT;
            case SET_UNDERLINE:
            case SET_STRIKETHROUGH: return SyntaxEvent.DECORATION;
            default: return SyntaxEvent.EFFECT;
        }
    }
}
