package neofontrender.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import org.lwjgl.opengl.GL11;
import neofontrender.core.config.NeofontrenderConfig;

public final class NeofontrenderMainMenuBranding {

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onDrawMainMenu(GuiScreenEvent.DrawScreenEvent.Post event) {
        if (!NeofontrenderConfig.showMainMenuBranding()) return;
        boolean cmm = NeofontrenderCustomMainMenu.ownsScreen(event.getGui());
        boolean fancy = NeofontrenderFancyMenu.ownsScreen(event.getGui());
        if (!cmm && event.getGui().getClass() != GuiMainMenu.class) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        String label = NeofontrenderBranding.displayName() + " " + neofontrender.Tags.VERSION;
        int existingLines = FMLCommonHandler.instance().getBrandings(true).size();
        int x = 2;
        int y = event.getGui().height - (10 + existingLines * (mc.fontRenderer.FONT_HEIGHT + 1));
        if (cmm || fancy) {
            MainMenuBrandingLayout.Position position = MainMenuBrandingLayout.place(
                    event.getGui().width, event.getGui().height, mc.fontRenderer.getStringWidth(label),
                    mc.fontRenderer.FONT_HEIGHT, cmm ? MainMenuTextProbe.cmm(event.getGui(), mc.fontRenderer)
                            : MainMenuTextProbe.fancy(event.getGui(), mc.fontRenderer));
            if (position == null) return;
            x = position.x;
            y = position.y;
        }
        boolean lighting = GL11.glIsEnabled(GL11.GL_LIGHTING);
        GlStateManager.disableLighting();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        try {
            event.getGui().drawString(mc.fontRenderer,
                    label, x, y, 0xFFFFFF);
        } finally {
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            if (lighting) GlStateManager.enableLighting();
        }
    }
}
