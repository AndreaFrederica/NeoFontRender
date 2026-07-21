package mnm.mods.tabbychat.settings;

import com.google.gson.reflect.TypeToken;
import mnm.mods.tabbychat.Reference;
import mnm.mods.util.config.SettingsFile;
import neofontrender.addons.chat.NfrTabbySettingsBridge;

import java.io.File;

public class TabbySettings extends SettingsFile {

    public GeneralSettings general = new GeneralSettings();
    public AdvancedSettings advanced = new AdvancedSettings();
    private final File generalFile;
    private final File advancedFile;

    public TabbySettings() {
        super(Reference.MOD_ID, "tabbychat2");
        generalFile = new File(Reference.MOD_ID + "/config/generalsettings.json");
        advancedFile = new File(Reference.MOD_ID + "/config/advancedsettings.json");
    }

    @Override
    public void loadConfig() {
        NfrTabbySettingsBridge.loadGlobal(this);
    }

    @Override
    public void saveConfig() {
        NfrTabbySettingsBridge.saveGlobal(this);
    }
}
