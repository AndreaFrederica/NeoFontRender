package neofontrender.addons.controller.sdl;

import java.util.Locale;

/** Builds a restart-stable identity without relying on SDL's transient joystick instance ID. */
final class SdlDeviceIdentity {
    private SdlDeviceIdentity() {}

    static String create(String guid, String serial, String path, String name) {
        String stableGuid = normalize(guid).toLowerCase(Locale.ROOT);
        if ("null".equals(stableGuid)) stableGuid = "";
        if (stableGuid.isEmpty()) stableGuid = "unknown";
        String stableSerial = normalize(serial);
        if (!stableSerial.isEmpty()) return stableGuid + "|serial|" + stableSerial;
        String stablePath = normalize(path);
        if (!stablePath.isEmpty()) return stableGuid + "|path|" + stablePath;
        return stableGuid + "|name|" + normalize(name);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
