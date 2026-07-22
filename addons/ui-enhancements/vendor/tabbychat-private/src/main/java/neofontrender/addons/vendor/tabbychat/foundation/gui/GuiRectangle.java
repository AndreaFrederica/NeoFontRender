package neofontrender.addons.vendor.tabbychat.foundation.gui;

import neofontrender.addons.vendor.tabbychat.foundation.ILocation;
import net.minecraft.client.gui.Gui;
import neofontrender.addons.vendor.tabbychat.foundation.render.GlState;
import neofontrender.addons.vendor.tabbychat.foundation.render.GuiTextures;
import net.minecraft.util.ResourceLocation;

/**
 * Represents a colored area. Transparency is represented using a transparency
 * grid.
 *
 * @author Matthew
 */
public class GuiRectangle extends GuiComponent {

    private static final ResourceLocation TRANSPARENCY =
            new ResourceLocation("neofontrender_tabbychat", "foundation/textures/transparency.png");

    @Override
    public void drawComponent(int mouseX, int mouseY) {
        GlState.enableAlpha();
        mc.getTextureManager().bindTexture(TRANSPARENCY);
        ILocation loc = getLocation();
        GuiTextures.drawScaled(0, 0, 0, 0, loc.getWidth(), loc.getHeight(), 5, 5);
        Gui.drawRect(0, 0, loc.getWidth(), loc.getHeight(), getPrimaryColorProperty().getHex());
        GlState.disableBlend();
        super.drawComponent(mouseX, mouseY);
    }
}
