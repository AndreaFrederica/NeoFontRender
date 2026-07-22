package neofontrender.addons.vendor.tabbychat.foundation.gui.config;

import neofontrender.addons.vendor.tabbychat.foundation.config.Value;
import neofontrender.addons.vendor.tabbychat.foundation.gui.GuiText;
import neofontrender.addons.vendor.tabbychat.foundation.gui.config.GuiSetting.GuiSettingWrapped;

/**
 * A Gui input that wraps a {@link GuiText}.
 *
 * @author Matthew
 */
public class GuiSettingString extends GuiSettingWrapped<String, GuiText> {

    public GuiSettingString(Value<String> setting) {
        super(setting, new GuiText());
    }

}
