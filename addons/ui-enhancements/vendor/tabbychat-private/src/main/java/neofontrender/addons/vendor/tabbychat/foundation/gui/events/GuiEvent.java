package neofontrender.addons.vendor.tabbychat.foundation.gui.events;

import neofontrender.addons.vendor.tabbychat.foundation.gui.GuiComponent;

/**
 * A class for the gui events.
 *
 * @author Matthew
 */
public abstract class GuiEvent {

    private GuiComponent component;

    public GuiEvent(GuiComponent component) {
        this.component = component;
    }

    public GuiComponent getComponent() {
        return component;
    }
}
