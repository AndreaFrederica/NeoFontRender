package neofontrender.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraftforge.client.event.GuiScreenEvent;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import neofontrender.Tags;

public final class NeofontrenderMainMenuBranding {

    private static final String BRANDING = Tags.MOD_NAME + " " + Tags.VERSION;

    @SubscribeEvent
    public void onDrawMainMenu(GuiScreenEvent.DrawScreenEvent.Post event) {
        if (!(event.gui instanceof GuiMainMenu)) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        int existingLines = FMLCommonHandler.instance().getBrandings(true).size();
        int y = event.gui.height - (10 + existingLines * (mc.fontRenderer.FONT_HEIGHT + 1));
        event.gui.drawString(mc.fontRenderer, BRANDING, 2, y, 0xFFFFFF);
    }
}
