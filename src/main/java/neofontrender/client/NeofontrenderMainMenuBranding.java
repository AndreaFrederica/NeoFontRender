package neofontrender.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraftforge.client.event.GuiScreenEvent;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.opengl.GL11;
import neofontrender.Tags;
import neofontrender.core.config.NeofontrenderConfig;

public final class NeofontrenderMainMenuBranding {

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onDrawMainMenu(GuiScreenEvent.DrawScreenEvent.Post event) {
        if (!NeofontrenderConfig.showMainMenuBranding()) return;
        boolean cmm = NeofontrenderCustomMainMenu.ownsScreen(event.gui);
        boolean fancy = NeofontrenderFancyMenu.ownsScreen(event.gui);
        if (!cmm && event.gui.getClass() != GuiMainMenu.class) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        String label = NeofontrenderBranding.displayName() + " " + Tags.VERSION;
        int existingLines = FMLCommonHandler.instance().getBrandings(true).size();
        int x = 2;
        int y = event.gui.height - (10 + existingLines * (mc.fontRenderer.FONT_HEIGHT + 1));
        if (cmm || fancy) {
            MainMenuBrandingLayout.Position position = MainMenuBrandingLayout.place(
                    event.gui.width, event.gui.height, mc.fontRenderer.getStringWidth(label),
                    mc.fontRenderer.FONT_HEIGHT, cmm ? MainMenuTextProbe.cmm(event.gui, mc.fontRenderer)
                            : MainMenuTextProbe.fancy(event.gui, mc.fontRenderer));
            if (position == null) return;
            x = position.x;
            y = position.y;
        }
        // 1.7.10 has no GlStateManager; guard the GL lighting state directly so a leftover
        // GL_LIGHTING from an earlier overlay pass cannot darken the branding text.
        boolean lighting = GL11.glIsEnabled(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        try {
            event.gui.drawString(mc.fontRenderer, label, x, y, 0xFFFFFF);
        } finally {
            GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
            if (lighting) GL11.glEnable(GL11.GL_LIGHTING);
        }
    }
}
