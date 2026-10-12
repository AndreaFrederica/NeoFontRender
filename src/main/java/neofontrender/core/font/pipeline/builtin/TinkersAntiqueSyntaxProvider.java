package neofontrender.core.font.pipeline.builtin;

import neofontrender.core.config.NeofontrenderConfig;
import neofontrender.text.syntax.SyntaxCursor;
import neofontrender.text.syntax.SyntaxMatch;
import neofontrender.text.syntax.SyntaxOperation;
import neofontrender.text.syntax.TextSyntaxProvider;

/** Structured syntax provider for Tinkers' three-PUA-byte RGB protocol. */
public final class TinkersAntiqueSyntaxProvider implements TextSyntaxProvider {
    public static final TinkersAntiqueSyntaxProvider INSTANCE =
            new TinkersAntiqueSyntaxProvider();
    public static final char MARKER_START = '\uE700';
    public static final char MARKER_END = '\uE7FF';

    private TinkersAntiqueSyntaxProvider() {}

    @Override public String id() { return "neofontrender:tinkers_rgb"; }
    @Override public int priority() { return 100; }
    @Override public char trigger() { return MARKER_START; }
    @Override public boolean acceptsTrigger(char value) { return isMarker(value); }

    public boolean isEnabled() {
        return NeofontrenderConfig.compatTinkersAntique();
    }

    @Override
    public SyntaxMatch match(SyntaxCursor cursor) {
        if (!isEnabled() || cursor.remaining() <= 0 || !isMarker(cursor.charAt(0))) return null;
        int consumed = 0;
        int rgb = 0xFFFFFF;
        while (consumed < cursor.remaining() && isMarker(cursor.charAt(consumed))) consumed++;
        int complete = consumed - consumed % 3;
        for (int index = 0; index < complete; index += 3) {
            rgb = value(cursor.charAt(index)) << 16
                    | value(cursor.charAt(index + 1)) << 8
                    | value(cursor.charAt(index + 2));
        }
        if (complete != consumed) rgb = 0xFFFFFF;
        return SyntaxMatch.operations(consumed, SyntaxOperation.colorOverride(rgb));
    }

    public static boolean isMarker(char value) {
        return value >= MARKER_START && value <= MARKER_END;
    }

    private static int value(char marker) {
        return marker - MARKER_START;
    }
}
