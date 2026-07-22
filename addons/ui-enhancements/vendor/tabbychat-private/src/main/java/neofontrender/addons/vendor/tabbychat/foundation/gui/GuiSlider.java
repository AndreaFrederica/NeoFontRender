package neofontrender.addons.vendor.tabbychat.foundation.gui;

import com.google.common.eventbus.Subscribe;
import neofontrender.addons.vendor.tabbychat.foundation.ILocation;
import neofontrender.addons.vendor.tabbychat.foundation.gui.events.GuiMouseEvent;
import neofontrender.addons.vendor.tabbychat.foundation.gui.events.GuiMouseEvent.MouseEvent;
import net.minecraft.client.gui.Gui;
import neofontrender.addons.vendor.tabbychat.foundation.render.GlState;
import neofontrender.addons.vendor.tabbychat.foundation.render.GuiTextures;
import net.minecraft.util.ResourceLocation;
import org.lwjglx.input.Mouse;

/**
 * A slider for double values. Click and drag or scroll to change the value.
 *
 * @author Matthew
 */
public class GuiSlider extends GuiComponent implements IGuiInput<Double> {

    private static final ResourceLocation TRANSPARENCY = new ResourceLocation(
            "neofontrender_tabbychat", "foundation/textures/transparency.png");

    private boolean vertical;
    private double value;

    public GuiSlider(double value, boolean vertical) {
        this.vertical = vertical;
        this.setValue(value);
    }

    @Override
    public void drawComponent(int mouseX, int mouseY) {
        GlState.enableBlend();
        ILocation loc = getLocation();
        Gui.drawRect(0, 0, loc.getWidth(), loc.getHeight(), -1);
        mc.getTextureManager().bindTexture(TRANSPARENCY);
        GuiTextures.drawScaled(1, 1, 0, 0, loc.getWidth() - 2, loc.getHeight() - 2, 6, 6);
        drawMid(loc);
        if (vertical) {
            int nook = Math.abs((int) (loc.getHeight() * getValue()) - loc.getHeight());
            Gui.drawRect(-1, nook - 1, loc.getWidth() + 1, nook + 2, 0xffffffff);
            Gui.drawRect(0, nook, loc.getWidth(), nook + 1, 0xff000000);
        } else {
            int nook = (int) (loc.getWidth() * getValue());
            Gui.drawRect(nook, 0, nook + 1, loc.getHeight(), 0xff000000);
        }
        int midX = loc.getWidth() / 2;
        int midY = loc.getHeight() / 2;
        drawCenteredString(mc.fontRenderer, getFormattedValue(), midX, midY, -1);
        GlState.disableAlpha();
        GlState.disableBlend();
        super.drawComponent(mouseX, mouseY);
    }

    protected void drawMid(ILocation loc) {
        Gui.drawRect(1, 1, loc.getWidth() - 1, loc.getHeight() - 1, getPrimaryColorProperty().getHex());
    }

    public String getFormattedValue() {
        return String.format("%%%.0f", getValue() * 100);
    }

    @Subscribe
    public void moveSlider(GuiMouseEvent event) {
        if (event.getMouseX() < 0
                || event.getMouseY() < 0
                || event.getMouseX() > getLocation().getWidth()
                || event.getMouseY() > getLocation().getHeight()) {
            return;
        }
        if ((event.getType() == MouseEvent.CLICK || event.getType() == MouseEvent.DRAG)
                && Mouse.isButtonDown(0)) {
            double val;
            if (vertical) {
                int y = event.getMouseY();
                val = Math.abs((double) y / (double) getLocation().getHeight() - 1);
            } else {
                int x = event.getMouseX();
                val = (double) x / (double) getLocation().getWidth();
            }
            setValue(val);
        }
        if (event.getType() == MouseEvent.SCROLL) {
            setValue(getValue() + event.getScroll() / 7360D);
        }
    }

    @Override
    public void setValue(Double value) {
        if (value < 0) {
            value = 0D;
        }
        if (value > 1) {
            value = 1D;
        }
        this.value = value;
    }

    @Override
    public Double getValue() {
        return value;
    }

    public boolean isVertical() {
        return vertical;
    }
}
