package neofontrender.addons.tooltips;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.client.config.GuiUtils;
import neofontrender.core.config.NeofontrenderConfig;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Compatibility bridge for Thaumcraft 6's UtilsFX custom tooltip renderer. */
public final class ThaumcraftTooltipCompat {
    static final int COMPACT_LINE_HEIGHT = 7;
    private static final ThreadLocal<boolean[]> COMPACT_LINES = new ThreadLocal<>();
    private static final ThreadLocal<Context> CONTEXT = new ThreadLocal<>();

    private ThaumcraftTooltipCompat() {}

    public static boolean isEnabled() {
        return TooltipConfig.enabled && NeofontrenderConfig.compatThaumcraftTooltip()
                && Arc3DRuntimeSupport.isAvailable();
    }

    public static void draw(GuiScreen screen, FontRenderer font, List<String> lines,
                            int x, int y, int color) {
        draw(screen, font, lines, x, y, color, false);
    }

    public static void draw(GuiScreen screen, FontRenderer font, List<String> lines,
                            int x, int y, int color, boolean right) {
        Minecraft minecraft = Minecraft.getMinecraft();
        int screenWidth = screen == null ? minecraft.currentScreen == null
                ? minecraft.displayWidth : minecraft.currentScreen.width : screen.width;
        int screenHeight = screen == null ? minecraft.currentScreen == null
                ? minecraft.displayHeight : minecraft.currentScreen.height : screen.height;

        // "@@" is a Thaumcraft control marker for a compact line, not visible text. Keep the
        // marker as metadata while passing only the visible text through Forge's event path.
        DecodedLines decoded = decodeLines(lines);
        List<String> eventLines = applySubTipColor(decoded.lines, color);
        // UtilsFX only applies its 0.5 scale in the non-Unicode font path. Unicode mode still
        // strips the marker, but uses ordinary line size and spacing.
        Context context = new Context(x, y, color, right,
                font.getUnicodeFlag() ? new boolean[decoded.lines.size()] : decoded.compact);
        CONTEXT.set(context);
        COMPACT_LINES.set(context.compact);
        try {
            // Passing through Forge keeps other tooltip integrations observable while allowing
            // the existing modern tooltip handler to replace the panel and text in one place.
            GuiUtils.drawHoveringText(ItemStack.EMPTY, eventLines, x, y, screenWidth, screenHeight,
                    240, font);
        } finally {
            COMPACT_LINES.remove();
            CONTEXT.remove();
        }
    }

    static List<String> sanitizeLines(List<String> lines) {
        return decodeLines(lines).lines;
    }

    static boolean[] compactLineFlags(List<String> lines) {
        return decodeLines(lines).compact;
    }

    static boolean[] consumeCompactLines(List<String> eventLines) {
        boolean[] compact = COMPACT_LINES.get();
        COMPACT_LINES.remove();
        if (compact == null || eventLines == null || compact.length != eventLines.size()) {
            return null;
        }
        return compact;
    }

    static Context consumeContext(List<String> eventLines) {
        Context context = CONTEXT.get();
        CONTEXT.remove();
        COMPACT_LINES.remove();
        if (context == null || eventLines == null || context.compact.length != eventLines.size()) {
            return null;
        }
        return context;
    }

    private static List<String> applySubTipColor(List<String> lines, int subTipColor) {
        if (subTipColor == -99 || lines.isEmpty()) return lines;
        List<String> colored = new ArrayList<>(lines.size());
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            String prefix = "\u00A7" + Integer.toHexString(i == 0 ? subTipColor : 7);
            colored.add(prefix + (line == null ? "" : line));
        }
        return colored;
    }

    private static DecodedLines decodeLines(List<String> lines) {
        if (lines == null || lines.isEmpty()) return new DecodedLines(
                Collections.emptyList(), new boolean[0]);
        List<String> decoded = new ArrayList<>(lines.size());
        boolean[] compact = new boolean[lines.size()];
        int index = 0;
        for (String line : lines) {
            compact[index] = line != null && line.startsWith("@@");
            // Match UtilsFX.replaceAll("@@", "") after it has inspected the leading marker.
            decoded.add(line == null ? null : line.replace("@@", ""));
            index++;
        }
        return new DecodedLines(decoded, compact);
    }

    private static final class DecodedLines {
        final List<String> lines;
        final boolean[] compact;

        DecodedLines(List<String> lines, boolean[] compact) {
            this.lines = lines;
            this.compact = compact;
        }
    }

    static final class Context {
        final int cursorX;
        final int cursorY;
        final int subTipColor;
        final boolean right;
        final boolean[] compact;

        Context(int cursorX, int cursorY, int subTipColor, boolean right, boolean[] compact) {
            this.cursorX = cursorX;
            this.cursorY = cursorY;
            this.subTipColor = subTipColor;
            this.right = right;
            this.compact = compact == null ? new boolean[0] : compact.clone();
        }
    }
}
