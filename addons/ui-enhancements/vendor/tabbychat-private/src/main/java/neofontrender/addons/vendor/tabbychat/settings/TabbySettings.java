package neofontrender.addons.vendor.tabbychat.settings;

import neofontrender.addons.vendor.tabbychat.Reference;
import neofontrender.addons.vendor.tabbychat.SettingsCodec;
import neofontrender.addons.vendor.tabbychat.foundation.config.SettingsFile;

import java.io.File;

public class TabbySettings extends SettingsFile {

    public GeneralSettings general = new GeneralSettings();
    public AdvancedSettings advanced = new AdvancedSettings();
    private final File generalFile;
    private final File advancedFile;

    public TabbySettings() {
        super(Reference.MOD_ID, "tabbychat2");
        generalFile = resolve("config/generalsettings.json");
        advancedFile = resolve("config/advancedsettings.json");
    }

    @Override
    public void loadConfig() {
        if (!generalFile.exists() && !advancedFile.exists()) saveConfig();
        general = SettingsCodec.readGeneral(loadJson(generalFile));
        advanced = SettingsCodec.readAdvanced(loadJson(advancedFile));
    }

    @Override
    public void saveConfig() {
        saveJson(generalFile, SettingsCodec.writeGeneral(general));
        saveJson(advancedFile, SettingsCodec.writeAdvanced(advanced));
    }
}
