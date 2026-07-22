package neofontrender.addons.vendor.tabbychat.foundation.gui.config;

import neofontrender.addons.vendor.tabbychat.foundation.config.Value;
import neofontrender.addons.vendor.tabbychat.foundation.gui.GuiCheckbox;
import neofontrender.addons.vendor.tabbychat.foundation.gui.config.GuiSetting.GuiSettingWrapped;

/**
 * A gui input for booleans as a checkbox.
 *
 * @author Matthew
 */
public class GuiSettingBoolean extends GuiSettingWrapped<Boolean, GuiCheckbox> {

    public GuiSettingBoolean(Value<Boolean> setting) {
        super(setting, new GuiCheckbox());
    }
}
