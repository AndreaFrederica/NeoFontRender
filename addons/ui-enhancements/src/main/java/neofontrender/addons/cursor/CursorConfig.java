package neofontrender.addons.cursor;

import neofontrender.addons.ui.UiEnhancementsConfig;
import neofontrender.api.config.NfrConfigFile;

/** UIE-owned cursor settings. Native cursor shape selection remains in CursorManager. */
public final class CursorConfig {
    static boolean enabled = true;
    static boolean textFields = true;
    static boolean buttons = true;
    static boolean disabledButtons = false;
    static String defaultImage = "";
    static String textImage = "";
    static String buttonImage = "";
    static String disabledButtonImage = "";

    private CursorConfig() {}

    public static void load() {
        NfrConfigFile file = UiEnhancementsConfig.file();
        file.define("cursor.enabled", true, "Enable semantic native cursors while a GUI is open.")
                .define("cursor.textFields", true, "Use the text cursor over editable text fields.")
                .define("cursor.buttons", true, "Use the hand cursor over enabled buttons.")
                .define("cursor.disabledButtons", false, "Use the forbidden cursor over disabled buttons.")
                .define("cursor.images.default", "", "Custom default cursor image ID; empty uses the system cursor.")
                .define("cursor.images.text", "", "Custom text cursor image ID; empty uses the system cursor.")
                .define("cursor.images.button", "", "Custom button cursor image ID; empty uses the system cursor.")
                .define("cursor.images.disabledButton", "", "Custom disabled-button cursor image ID; empty uses the system cursor.");
        enabled = file.getBoolean("cursor.enabled", true);
        textFields = file.getBoolean("cursor.textFields", true);
        buttons = file.getBoolean("cursor.buttons", true);
        disabledButtons = file.getBoolean("cursor.disabledButtons", false);
        defaultImage = file.getString("cursor.images.default", "");
        textImage = file.getString("cursor.images.text", "");
        buttonImage = file.getString("cursor.images.button", "");
        disabledButtonImage = file.getString("cursor.images.disabledButton", "");
        file.save();
    }

    public static void save() {
        UiEnhancementsConfig.file().set("cursor.enabled", enabled)
                .set("cursor.textFields", textFields)
                .set("cursor.buttons", buttons)
                .set("cursor.disabledButtons", disabledButtons)
                .set("cursor.images.default", defaultImage)
                .set("cursor.images.text", textImage)
                .set("cursor.images.button", buttonImage)
                .set("cursor.images.disabledButton", disabledButtonImage)
                .save();
    }

    static String imageFor(CursorType type) {
        switch (type) {
            case DEFAULT: return defaultImage;
            case TEXT: return textImage;
            case LINK:
            case BUTTON: return buttonImage;
            case FORBIDDEN: return disabledButtonImage;
            default: return "";
        }
    }

    static Snapshot snapshot() { return new Snapshot(); }

    static final class Snapshot {
        private final boolean enabledValue = enabled;
        private final boolean textFieldsValue = textFields;
        private final boolean buttonsValue = buttons;
        private final boolean disabledButtonsValue = disabledButtons;
        private final String defaultImageValue = defaultImage;
        private final String textImageValue = textImage;
        private final String buttonImageValue = buttonImage;
        private final String disabledButtonImageValue = disabledButtonImage;

        void restore() {
            enabled = enabledValue;
            textFields = textFieldsValue;
            buttons = buttonsValue;
            disabledButtons = disabledButtonsValue;
            defaultImage = defaultImageValue;
            textImage = textImageValue;
            buttonImage = buttonImageValue;
            disabledButtonImage = disabledButtonImageValue;
            CursorManager.restoreDefault();
        }
    }
}
