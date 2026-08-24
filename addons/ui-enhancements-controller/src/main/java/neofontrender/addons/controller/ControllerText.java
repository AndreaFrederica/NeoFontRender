package neofontrender.addons.controller;

import net.minecraft.client.resources.I18n;
import net.minecraft.util.ResourceLocation;
import neofontrender.addons.api.input.InputAction;

import java.util.Locale;

final class ControllerText {
    private static final String PREFIX = ControllerAddonMod.MOD_ID + ".";

    private ControllerText() {}

    static String tr(String key, Object... arguments) {
        return I18n.format(PREFIX + key, arguments);
    }

    static String action(InputAction action) {
        String key = PREFIX + "action." + action.name().toLowerCase(Locale.ROOT);
        String translated = I18n.format(key);
        return key.equals(translated) ? humanize(action.name()) : translated;
    }

    static String control(ResourceLocation control) {
        if (control == null) return tr("gui.unbound");
        String label = ControllerControlCatalog.labelKey(control);
        if (label != null) return tr("control." + label);
        String path = control.getPath();
        if (path.startsWith("joystick/axis/")) return tr("control.joystick_axis",
                path.substring("joystick/axis/".length()));
        if (path.startsWith("joystick/button/")) return tr("control.joystick_button",
                path.substring("joystick/button/".length()));
        if (path.startsWith("joystick/hat/")) return tr("control.joystick_hat",
                path.substring("joystick/hat/".length()).replace('/', ' '));
        return path;
    }

    private static String humanize(String value) {
        StringBuilder result = new StringBuilder();
        for (String part : value.toLowerCase(Locale.ROOT).split("_")) {
            if (result.length() > 0) result.append(' ');
            if (!part.isEmpty()) result.append(Character.toUpperCase(part.charAt(0)))
                    .append(part.substring(1));
        }
        return result.toString();
    }
}
