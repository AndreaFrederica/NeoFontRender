package neofontrender.core.config;

import neofontrender.Tags;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

class BuildVersionMetadataTest {
    @Test
    void forgeRegistrationAndJavaConstantUseTheResolvedBuildVersion() throws Exception {
        Properties versions = new Properties();
        try (var stream = getClass().getResourceAsStream("/META-INF/neofontrender/versions.properties")) {
            assertNotNull(stream);
            versions.load(stream);
        }
        String expected = versions.getProperty("mod_version");
        assertNotNull(expected);
        assertEquals(expected, Tags.VERSION);
        try (var stream = getClass().getResourceAsStream("/neofontrender/NeoFontRender.class")) {
            assertNotNull(stream);
            ClassNode mod = new ClassNode();
            new ClassReader(stream).accept(mod, ClassReader.SKIP_CODE);
            var registration = mod.visibleAnnotations.stream()
                    .filter(a -> a.desc.equals("Lnet/minecraftforge/fml/common/Mod;"))
                    .findFirst().orElseThrow();
            int versionIndex = registration.values.indexOf("version");
            assertTrue(versionIndex >= 0);
            assertEquals(expected, registration.values.get(versionIndex + 1));
        }
    }
}
