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

    private ThaumcraftTooltipCompat() {}

    public static boolean isEnabled() {
        return TooltipConfig.enabled && NeofontrenderConfig.compatThaumcraftTooltip()
                && Arc3DRuntimeSupport.isAvailable();
    }

    public static void draw(GuiScreen screen, FontRenderer font, List<String> lines,
                            int x, int y, int color) {
        Minecraft minecraft = Minecraft.getMinecraft();
        int screenWidth = screen == null ? minecraft.currentScreen == null
                ? minecraft.displayWidth : minecraft.currentScreen.width : screen.width;
        int screenHeight = screen == null ? minecraft.currentScreen == null
                ? minecraft.displayHeight : minecraft.currentScreen.height : screen.height;

        // "@@" is a Thaumcraft control marker for a compact line, not visible text. Keep the
        // marker as metadata while passing only the visible text through Forge's event path.
        DecodedLines decoded = decodeLines(lines);
        // UtilsFX only applies its 0.5 scale in the non-Unicode font path. Unicode mode still
        // strips the marker, but uses ordinary line size and spacing.
        COMPACT_LINES.set(font.getUnicodeFlag()
                ? new boolean[decoded.lines.size()] : decoded.compact);
        try {
            // Passing through Forge keeps other tooltip integrations observable while allowing
            // the existing modern tooltip handler to replace the panel and text in one place.
            GuiUtils.drawHoveringText(ItemStack.EMPTY, decoded.lines, x, y, screenWidth, screenHeight,
                    240, font);
        } finally {
            COMPACT_LINES.remove();
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
}
