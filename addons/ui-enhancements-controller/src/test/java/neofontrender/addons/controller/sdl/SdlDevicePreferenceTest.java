package neofontrender.addons.controller.sdl;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class SdlDevicePreferenceTest {
    @Test
    void restoresPreferredDeviceWhenItReconnects() {
        Map<Integer, String> devices = devices(1, "fallback", 7, "preferred");
        assertEquals(7, SdlDevicePreference.choose(devices, 1, "preferred"));
    }

    @Test
    void keepsCurrentFallbackWhilePreferredDeviceIsAbsent() {
        Map<Integer, String> devices = devices(1, "fallback", 2, "other");
        assertEquals(2, SdlDevicePreference.choose(devices, 2, "preferred"));
    }

    @Test
    void selectsFirstConnectedDeviceOnlyWhenNoSelectionCanBeRestored() {
        assertEquals(3, SdlDevicePreference.choose(devices(3, "first", 4, "second"), 9, "missing"));
        assertNull(SdlDevicePreference.choose(new LinkedHashMap<>(), 9, "missing"));
    }

    private static Map<Integer, String> devices(int firstId, String firstKey,
                                                int secondId, String secondKey) {
        Map<Integer, String> devices = new LinkedHashMap<>();
        devices.put(firstId, firstKey);
        devices.put(secondId, secondKey);
        return devices;
    }
}
