package neofontrender.addons.controller.sdl;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SdlDeviceIdentityTest {
    @Test
    void serialSurvivesPathAndInstanceChanges() {
        assertEquals(
                SdlDeviceIdentity.create("AABB", " serial-7 ", "path-a", "Pad"),
                SdlDeviceIdentity.create("aabb", "serial-7", "path-b", "Renamed Pad"));
    }

    @Test
    void pathDistinguishesDevicesWithoutSerialNumbers() {
        assertEquals("0300abcd|path|hid/device-2",
                SdlDeviceIdentity.create("0300ABCD", null, " hid/device-2 ", "Pad"));
    }

    @Test
    void nameIsTheLastAvailableFallback() {
        assertEquals("unknown|name|Generic Controller",
                SdlDeviceIdentity.create(null, null, null, " Generic Controller "));
    }
}
