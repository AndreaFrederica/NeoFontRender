package neofontrender.client;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import neofontrender.core.config.NeofontrenderConfig;
import neofontrender.core.font.FontManager;

/** Adds a compact NeoFontRender status block to the vanilla debug overlay. */
public final class NeofontrenderDebugOverlayHandler {

    @SubscribeEvent
    public void onOverlayText(RenderGameOverlayEvent.Text event) {
        if (!NeofontrenderConfig.debugRenderStats()) {
            return;
        }
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft.theWorld == null) {
            return;
        }
        event.right.add("NeoFontRender");
        event.right.add("  engine: " + NeofontrenderConfig.renderingEngine());
        event.right.add("  enabled: " + NeofontrenderConfig.enabled());
        event.right.add("  SFR/Cosmic: "
                + FontManager.INSTANCE.isSfrActive() + "/"
                + FontManager.INSTANCE.isCosmicActive());
    }
}
