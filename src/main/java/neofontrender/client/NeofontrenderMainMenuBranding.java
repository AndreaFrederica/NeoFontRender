package neofontrender.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraftforge.client.event.GuiScreenEvent;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import neofontrender.Tags;
import org.lwjgl.opengl.GL11;

public final class NeofontrenderMainMenuBranding {

    @SubscribeEvent
    public void onDrawMainMenu(GuiScreenEvent.DrawScreenEvent.Post event) {
        if (!(event.gui instanceof GuiMainMenu)) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        int existingLines = FMLCommonHandler.instance().getBrandings(true).size();
        int y = event.gui.height - (10 + existingLines * (mc.fontRenderer.FONT_HEIGHT + 1));
        // 1.7.10 has no GlStateManager; guard the GL lighting state directly so a leftover
        // GL_LIGHTING from an earlier overlay pass cannot darken the branding text.
        boolean lighting = GL11.glIsEnabled(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        try {
            event.gui.drawString(mc.fontRenderer,
                    NeofontrenderBranding.displayName() + " " + Tags.VERSION,
                    2, y, 0xFFFFFF);
        } finally {
            GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
            if (lighting) GL11.glEnable(GL11.GL_LIGHTING);
        }
    }
}
