package neofontrender.addons.tooltips;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.client.config.GuiUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Compatibility bridge for Thaumcraft 6's UtilsFX custom tooltip renderer. */
public final class ThaumcraftTooltipCompat {
    private ThaumcraftTooltipCompat() {}

    public static boolean isEnabled() {
        return TooltipConfig.enabled && Arc3DRuntimeSupport.isAvailable();
    }

    public static void draw(GuiScreen screen, FontRenderer font, List<String> lines,
                            int x, int y, int color) {
        Minecraft minecraft = Minecraft.getMinecraft();
        int screenWidth = screen == null ? minecraft.currentScreen == null
                ? minecraft.displayWidth : minecraft.currentScreen.width : screen.width;
        int screenHeight = screen == null ? minecraft.currentScreen == null
                ? minecraft.displayHeight : minecraft.currentScreen.height : screen.height;

        // "@@" is a Thaumcraft control marker for a compact line, not visible text. The original
        // UtilsFX renderer strips it before drawing; decode it before entering Forge's event path.
        List<String> decodedLines = sanitizeLines(lines);
        // Passing through Forge keeps other tooltip integrations observable while allowing the
        // existing modern tooltip handler to replace the panel and text in one place.
        GuiUtils.drawHoveringText(ItemStack.EMPTY, decodedLines, x, y, screenWidth, screenHeight,
                240, font);
    }

    static List<String> sanitizeLines(List<String> lines) {
        if (lines == null || lines.isEmpty()) return Collections.emptyList();
        List<String> decoded = new ArrayList<>(lines.size());
        for (String line : lines) {
            if (line != null && line.startsWith("@@")) decoded.add(line.substring(2));
            else decoded.add(line);
        }
        return decoded;
    }
}
