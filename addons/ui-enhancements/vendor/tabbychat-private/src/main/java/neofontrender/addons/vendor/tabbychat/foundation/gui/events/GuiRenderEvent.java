package neofontrender.addons.vendor.tabbychat.foundation.gui.events;

import neofontrender.addons.vendor.tabbychat.foundation.gui.GuiComponent;

public class GuiRenderEvent extends AbstractMouseEvent {

    private float ticks;

    public GuiRenderEvent(GuiComponent component, int mouseX, int mouseY, float ticks) {
        super(component, mouseX, mouseY);
        this.ticks = ticks;
    }

    public float getTicks() {
        return ticks;
    }
}
