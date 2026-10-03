package neofontrender.text.syntax;

/** Semantic events that may terminate independently registered effects. */
public enum SyntaxEvent {
    COLOR_CHANGE,
    RESET,
    OBFUSCATED,
    WEIGHT,
    SLANT,
    DECORATION,
    EFFECT
}
