package neofontrender.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiOptions;
import net.minecraft.client.resources.I18n;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import neofontrender.client.gui.NeofontrenderConfigScreen;

import java.util.List;

/**
 * Adds a "Neo Font Render" button to the vanilla Options screen using Forge events.
 * The button is placed in the first available slot of the vanilla options button grid,
 * avoiding overlap with buttons added by other mods.
 */
public final class NeofontrenderOptionsButtonHandler {

    private static final int BUTTON_ID = 9200;
    private static final int BUTTON_WIDTH = 150;
    private static final int BUTTON_HEIGHT = 20;
    private static final int ROW_SPACING = 24;

    @SubscribeEvent
    public void onInitGui(GuiScreenEvent.InitGuiEvent.Post event) {
        if (!(event.getGui() instanceof GuiOptions)) {
            return;
        }
        GuiOptions gui = (GuiOptions) event.getGui();
        List<GuiButton> buttons = event.getButtonList();

        int leftX = gui.width / 2 - 155;
        int rightX = gui.width / 2 + 5;
        int baseY = gui.height / 6 + 42;

        int[] y = findFreeGridSlot(buttons, leftX, rightX, baseY);
        buttons.add(new GuiButton(
                BUTTON_ID,
                y[0],
                y[1],
                BUTTON_WIDTH,
                BUTTON_HEIGHT,
                I18n.format("neofontrender.options.button")
        ));
    }

    /**
     * Finds the next free slot in the vanilla options button grid.
     * Fills row by row, left column first, then right column, and keeps moving down
     * if other mods have already occupied a slot.
     *
     * @return int[]{x, y}
     */
    private static int[] findFreeGridSlot(List<GuiButton> buttons, int leftX, int rightX, int baseY) {
        int y = baseY;
        while (true) {
            if (!isOccupied(buttons, leftX, y)) {
                return new int[]{leftX, y};
            }
            if (!isOccupied(buttons, rightX, y)) {
                return new int[]{rightX, y};
            }
            y += ROW_SPACING;
        }
    }

    private static boolean isOccupied(List<GuiButton> buttons, int x, int y) {
        for (GuiButton button : buttons) {
            if (button.x == x && button.y == y) {
                return true;
            }
        }
        return false;
    }

    @SubscribeEvent
    public void onActionPerformed(GuiScreenEvent.ActionPerformedEvent.Pre event) {
        if (!(event.getGui() instanceof GuiOptions)) {
            return;
        }
        GuiButton button = event.getButton();
        if (button.enabled && button.id == BUTTON_ID) {
            NeofontrenderConfigScreen.open(Minecraft.getMinecraft().currentScreen);
            event.setCanceled(true);
        }
    }
}
