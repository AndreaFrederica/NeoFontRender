package neofontrender.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.opengl.GL11;

public final class NeofontrenderMainMenuBranding {

    @SubscribeEvent
    public void onDrawMainMenu(GuiScreenEvent.DrawScreenEvent.Post event) {
        if (!(event.getGui() instanceof GuiMainMenu)) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        int existingLines = FMLCommonHandler.instance().getBrandings(true).size();
        int y = event.getGui().height - (10 + existingLines * (mc.fontRenderer.FONT_HEIGHT + 1));
        boolean lighting = GL11.glIsEnabled(GL11.GL_LIGHTING);
        GlStateManager.disableLighting();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        try {
            event.getGui().drawString(mc.fontRenderer,
                    NeofontrenderBranding.displayName() + " " + neofontrender.Tags.VERSION,
                    2, y, 0xFFFFFF);
        } finally {
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            if (lighting) GlStateManager.enableLighting();
        }
    }
}
