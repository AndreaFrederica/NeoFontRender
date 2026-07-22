package neofontrender.addons.tooltips;

import neofontrender.api.arc3d.Arc3DApi;
import org.junit.jupiter.api.Test;
import org.lwjgl.Version;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Arc3DRuntimeSupportTest {
    @Test
    void arc3dCoreLinksAgainstHostLwjglApi() {
        assertTrue(Arc3DApi.isAvailable());
        assertEquals(0.5F, Arc3DApi.lerp(0.0F, 1.0F, 0.5F));
        assertEquals(0xFF00FF00, Arc3DApi.hsv(120.0F, 1.0F, 1.0F, 255));
        assertTrue(Version.getVersion().startsWith("3.4.1"));
    }
}
