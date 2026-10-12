package neofontrender.addons.camera;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;

class ValkyrienCameraCompatTest {
    @Test
    void optionalHooksAreInactiveWhenValkyrienSkiesIsAbsent() {
        assertFalse(ValkyrienCameraCompat.isAvailable());
    }
}
