package neofontrender.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import neofontrender.Tags;

public final class NeofontrenderMainMenuBranding {

    private static final String BRANDING = Tags.MOD_NAME + " " + Tags.VERSION;

    @SubscribeEvent
    public void onDrawMainMenu(GuiScreenEvent.DrawScreenEvent.Post event) {
        if (!(event.getGui() instanceof GuiMainMenu)) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        int existingLines = FMLCommonHandler.instance().getBrandings(true).size();
        int y = event.getGui().height - (10 + existingLines * (mc.fontRenderer.FONT_HEIGHT + 1));
        event.getGui().drawString(mc.fontRenderer, BRANDING, 2, y, 0xFFFFFF);
    }
}
