package neofontrender.addons.tooltips;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.client.config.GuiUtils;

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

        // Passing through Forge keeps other tooltip integrations observable while allowing the
        // existing modern tooltip handler to replace the panel and text in one place.
        GuiUtils.drawHoveringText(ItemStack.EMPTY, lines, x, y, screenWidth, screenHeight,
                240, font);
    }
}
