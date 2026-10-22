package neofontrender.core.font.linebreak;

import neofontrender.text.layout.CjkLineBreakProvider;

/** Minecraft compatibility facade over the game-independent CJK layout policy. */
public final class CjkLineBreakRules {
    private CjkLineBreakRules() {}

    public static boolean canBreakBetween(int previous, int next) {
        return CjkLineBreakProvider.canBreakBetween(previous, next);
    }

    public static boolean isForbiddenAtLineStart(int codePoint) {
        return CjkLineBreakProvider.isForbiddenAtLineStart(codePoint);
    }

    public static boolean isForbiddenAtLineEnd(int codePoint) {
        return CjkLineBreakProvider.isForbiddenAtLineEnd(codePoint);
    }
}
