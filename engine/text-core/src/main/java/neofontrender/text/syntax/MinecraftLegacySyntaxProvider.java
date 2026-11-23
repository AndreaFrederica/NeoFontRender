package neofontrender.text.syntax;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/** Complete documented Minecraft section-sign formatting syntax. */
public final class MinecraftLegacySyntaxProvider implements FixedCodeSyntaxProvider {
    public static final char PREFIX = '\u00A7';
    public static final MinecraftLegacySyntaxProvider INSTANCE =
            new MinecraftLegacySyntaxProvider(vanillaColors());

    private final int[] colors;
    private final Set<Character> codes;
    private final boolean vanillaCompatibility;

    public MinecraftLegacySyntaxProvider(int[] colors) {
        this(colors, true);
    }

    public MinecraftLegacySyntaxProvider(int[] colors, boolean vanillaCompatibility) {
        if (colors == null || colors.length < 16) {
            throw new IllegalArgumentException("At least 16 colors are required");
        }
        this.colors = Arrays.copyOf(colors, 16);
        this.vanillaCompatibility = vanillaCompatibility;
        LinkedHashSet<Character> values = new LinkedHashSet<>();
        for (char code : "0123456789abcdefklmnor".toCharArray()) values.add(code);
        codes = Collections.unmodifiableSet(values);
    }

    @Override public String id() { return "minecraft:legacy_formatting"; }
    @Override public int priority() { return 0; }
    @Override public char trigger() { return PREFIX; }
    @Override public Set<Character> codes() { return codes; }
    @Override public boolean isFallback() { return vanillaCompatibility; }

    @Override
    public SyntaxMatch match(SyntaxCursor cursor) {
        if (cursor.remaining() < 2 || cursor.charAt(0) != PREFIX) return null;
        char code = Character.toLowerCase(cursor.charAt(1));
        int colorIndex = "0123456789abcdef".indexOf(code);
        if (colorIndex >= 0) return SyntaxMatch.operations(2, SyntaxOperation.color(colors[colorIndex]));
        switch (code) {
            case 'k': return SyntaxMatch.operations(2, SyntaxOperation.obfuscated(true));
            case 'l': return SyntaxMatch.operations(2, SyntaxOperation.bold(true));
            case 'm': return SyntaxMatch.operations(2, SyntaxOperation.strikethrough(true));
            case 'n': return SyntaxMatch.operations(2, SyntaxOperation.underline(true));
            case 'o': return SyntaxMatch.operations(2, SyntaxOperation.italic(true));
            case 'r': return SyntaxMatch.operations(2, SyntaxOperation.reset());
            default: return vanillaCompatibility
                    ? SyntaxMatch.operations(2, SyntaxOperation.color(colors[15])) : null;
        }
    }

    private static int[] vanillaColors() {
        return new int[]{
                0x000000, 0x0000AA, 0x00AA00, 0x00AAAA,
                0xAA0000, 0xAA00AA, 0xFFAA00, 0xAAAAAA,
                0x555555, 0x5555FF, 0x55FF55, 0x55FFFF,
                0xFF5555, 0xFF55FF, 0xFFFF55, 0xFFFFFF
        };
    }
}
