package neofontrender.addons.vendor.tabbychat.gui.settings;

import neofontrender.addons.vendor.tabbychat.foundation.ILocation;
import neofontrender.addons.vendor.tabbychat.foundation.Location;
import neofontrender.addons.vendor.tabbychat.foundation.gui.GuiButton;
import neofontrender.addons.vendor.tabbychat.foundation.gui.config.SettingPanel;
import net.minecraft.client.gui.Gui;
import neofontrender.addons.vendor.tabbychat.foundation.render.GlState;

import java.awt.Dimension;
import javax.annotation.Nonnull;

public class SettingsButton extends GuiButton {

    private SettingPanel<?> settings;
    private int displayX = 30;
    private boolean active;

    SettingsButton(SettingPanel<?> settings) {
        super(settings.getDisplayString());
        this.settings = settings;
        this.setLocation(new Location(0, 0, 75, 20));
        settings.getSecondaryColor().ifPresent(this::setSecondaryColor);
    }

    public SettingPanel<?> getSettings() {
        return settings;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    @Override
    public void drawComponent(int mouseX, int mouseY) {
        if (active && displayX > 20) {
            displayX -= 2;
        } else if (!active && displayX < 30) {
            displayX += 2;
        }
        ILocation loc = this.getLocation();
        GlState.enableAlpha();
        getSecondaryColor().ifPresent(color ->
                Gui.drawRect(displayX - 30, 2, loc.getWidth() + displayX - 30, loc.getHeight() - 2, color.getHex()));
        String string = mc.fontRenderer.trimStringToWidth(getText(), loc.getWidth());
        mc.fontRenderer.drawString(string, displayX - 20, 6, getPrimaryColorProperty().getHex());
    }

    @Nonnull
    @Override
    public Dimension getMinimumSize() {
        return getLocation().getSize();
    }
}
