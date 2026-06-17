package neofontrender.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiOptions;
import net.minecraft.client.resources.I18n;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import neofontrender.client.gui.NeofontrenderConfigScreen;

/**
 * Adds a "Neo Font Render" button to the vanilla Options screen using Forge events.
 */
public final class NeofontrenderOptionsButtonHandler {

    private static final int BUTTON_ID = 9200;

    @SubscribeEvent
    public void onInitGui(GuiScreenEvent.InitGuiEvent.Post event) {
        if (!(event.getGui() instanceof GuiOptions)) {
            return;
        }
        GuiOptions gui = (GuiOptions) event.getGui();
        event.getButtonList().add(new GuiButton(
                BUTTON_ID,
                gui.width / 2 - 155,
                gui.height / 6 + 138,
                150,
                20,
                I18n.format("neofontrender.options.button")
        ));
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
