package neofontrender.addons.vendor.tabbychat.foundation.gui.config;

import com.google.common.eventbus.Subscribe;
import neofontrender.addons.vendor.tabbychat.foundation.Color;
import neofontrender.addons.vendor.tabbychat.foundation.ILocation;
import neofontrender.addons.vendor.tabbychat.foundation.config.Value;
import neofontrender.addons.vendor.tabbychat.foundation.gui.GuiSelectColor;
import neofontrender.addons.vendor.tabbychat.foundation.render.GuiTextures;
import neofontrender.addons.vendor.tabbychat.foundation.gui.events.ActionPerformedEvent;
import net.minecraft.client.gui.Gui;
import net.minecraft.util.ResourceLocation;

import java.util.function.Consumer;

/**
 * A setting for colors. When clicked, shows a {@link GuiSelectColor}.
 *
 * @author Matthew
 */
public class GuiSettingColor extends GuiSetting<Color> implements Consumer<Color> {

    private static final ResourceLocation TRANSPARENCY = new ResourceLocation(
            "neofontrender_tabbychat", "foundation/textures/transparency.png");

    private Color value;

    public GuiSettingColor(Value<Color> setting) {
        super(setting);
    }

    @Subscribe
    public void selectColor(ActionPerformedEvent event) {
        getParent().ifPresent(p -> p.setOverlay(new GuiSelectColor(GuiSettingColor.this, getValue())));
    }

    @Override
    public void drawComponent(int mouseX, int mouseY) {
        ILocation loc = getLocation();
        mc.getTextureManager().bindTexture(TRANSPARENCY);
        GuiTextures.drawScaled(0, 0, 0, 0, loc.getWidth(), loc.getHeight(), 6, 6);
        Gui.drawRect(0, 0, loc.getWidth(), loc.getHeight(), getValue().getHex());
        Gui.drawRect(0, 0, loc.getWidth(), 1, 0xffaabbcc);
        Gui.drawRect(1, 0, 0, loc.getHeight(), 0xffaabbcc);
        Gui.drawRect(loc.getWidth() - 1, 0, loc.getWidth(), loc.getHeight(), 0xffaabbcc);
        Gui.drawRect(0, loc.getHeight(), loc.getWidth(), loc.getHeight() - 1, 0xffaabbcc);
    }

    @Override
    public void accept(Color input) {
        setValue(input);
        getParent().ifPresent( p -> p.setOverlay(null));
    }

    @Override
    public Color getValue() {
        return value;
    }

    @Override
    public void setValue(Color color) {
        this.value = color;
    }

}
