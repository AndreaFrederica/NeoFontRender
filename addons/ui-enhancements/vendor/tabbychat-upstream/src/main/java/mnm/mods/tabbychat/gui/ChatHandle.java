package mnm.mods.tabbychat.gui;

import mnm.mods.util.Color;
import mnm.mods.util.Location;
import mnm.mods.util.gui.GuiComponent;
import net.minecraft.client.resources.I18n;

import java.awt.Dimension;
import javax.annotation.Nonnull;

public class ChatHandle extends GuiComponent {

    ChatHandle() {
        setLocation(new Location(0, 0, ChatTray.CONTROL_SIZE, ChatTray.CONTROL_SIZE));
    }

    @Override
    public void drawComponent(int mouseX, int mouseY) {
        this.drawHorizontalLine(2, 9, 2, getPrimaryColorProperty().getHex());
        this.drawVerticalLine(9, 2, 9, getPrimaryColorProperty().getHex());
        if (isHovered()) {
            String label = I18n.format("neofontrender_ui_enhancements.chat.controls.resize");
            drawCaption(label, -mc.fontRenderer.getStringWidth(label) - 6, ChatTray.CONTROL_SIZE);
        }
    }

    @Nonnull
    @Override
    public Color getPrimaryColorProperty() {
        int opac = (int)(mc.gameSettings.chatOpacity * 255);
        return isHovered() ? Color.of(255, 255, 160, opac) : Color.of(255, 255, 255, opac);
    }

    @Nonnull
    @Override
    public Dimension getMinimumSize() {
        return new Dimension(ChatTray.CONTROL_SIZE, ChatTray.CONTROL_SIZE);
    }
}
