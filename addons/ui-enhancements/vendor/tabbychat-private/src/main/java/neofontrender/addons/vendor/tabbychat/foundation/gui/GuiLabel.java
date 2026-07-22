package neofontrender.addons.vendor.tabbychat.foundation.gui;

import neofontrender.addons.vendor.tabbychat.foundation.text.FancyFontRenderer;
import net.minecraft.client.Minecraft;
import neofontrender.addons.vendor.tabbychat.foundation.render.GlState;
import net.minecraft.util.IChatComponent;

/**
 * Gui component label used to show text on the screen.
 *
 * @author Matthew
 */
public class GuiLabel extends GuiComponent {

    private FancyFontRenderer fr;
    private IChatComponent text;
    private float angle;

    public GuiLabel() {
        this.fr = new FancyFontRenderer(Minecraft.getMinecraft().fontRenderer);
    }

    /**
     * Creates a label from a chat component.
     *
     * @param chat The text
     */
    public GuiLabel(IChatComponent chat) {
        this();
        this.setText(chat);
    }

    @Override
    public void drawComponent(int mouseX, int mouseY) {
        if (getText() == null)
            return;
        GlState.pushMatrix();
        GlState.rotate(angle, 0, 0, angle);
        if (angle < 180) {
            GlState.translate(-angle / 1.5, -angle / 4, 0);
        } else {
            GlState.translate(-angle / 15, angle / 40, 0);
        }

        fr.drawChat(getText(), 0, 1, getPrimaryColorProperty().getHex(), true);

        GlState.popMatrix();
        super.drawComponent(mouseX, mouseY);
    }

    /**
     * Sets the string of this label
     *
     * @param text The string
     */
    public void setText(IChatComponent text) {
        this.text = text;
    }

    /**
     * Gets the string of this label
     *
     * @return The string
     */
    public IChatComponent getText() {
        return text;
    }

    public float getAngle() {
        return angle;
    }

    public void setAngle(float angle) {
        this.angle = angle;
    }

}
