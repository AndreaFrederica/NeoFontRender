package neofontrender.addons.controller.sdl;

import java.util.Map;

/** Selects a connected SDL instance while retaining a stable preferred-device identity. */
final class SdlDevicePreference {
    private SdlDevicePreference() {}

    static Integer choose(Map<Integer, String> available, Integer current, String preferred) {
        if (preferred != null && !preferred.isEmpty()) {
            for (Map.Entry<Integer, String> entry : available.entrySet()) {
                if (preferred.equals(entry.getValue())) return entry.getKey();
            }
        }
        if (current != null && available.containsKey(current)) return current;
        return available.isEmpty() ? null : available.keySet().iterator().next();
    }
}
