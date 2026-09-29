package neofontrender.client.licenses;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

class ThirdPartyLicenseCatalogTest {
    @TempDir Path temp;

    private static String entry(String section, String name) {
        return "[{\"section\":\"" + section + "\",\"name\":\"" + name
                + "\",\"version\":\"1\",\"license\":\"MIT OR Apache-2.0\","
                + "\"source\":\"https://example.org\",\"evidence\":\"LICENSE\"}]";
    }

    @Test void loadsOnlyPresentModulesAndDeduplicatesRepeatedResources() throws Exception {
        URL core = resource("core", entry("10_core", "Core"));
        URL addon = resource("addon", entry("30_uie", "Addon"));
        URL duplicate = resource("duplicate", entry("10_core", "Core"));
        try (URLClassLoader loader = new URLClassLoader(new URL[]{core}, null)) {
            assertEquals(1, ThirdPartyLicenseCatalog.load(loader).size());
        }
        try (URLClassLoader loader = new URLClassLoader(new URL[]{addon, duplicate, core}, null)) {
            var entries = ThirdPartyLicenseCatalog.load(loader);
            assertEquals(2, entries.size());
            assertEquals("Core", entries.get(0).name);
            assertEquals("MIT OR Apache-2.0", entries.get(0).license);
            assertEquals("Addon", entries.get(1).name);
        }
    }

    @Test void rejectsIncompleteOrMalformedEntriesInsteadOfSilentlyDroppingThem() {
        assertThrows(IOException.class, () -> ThirdPartyLicenseCatalog.read(stream("[{\"name\":\"Missing license\"}]")));
        assertThrows(IOException.class, () -> ThirdPartyLicenseCatalog.read(stream("not json")));
    }

    @Test void bundledCoreCatalogIncludesFontsAndTransitiveNativeDependencies() throws Exception {
        try (var stream = getClass().getClassLoader().getResourceAsStream(ThirdPartyLicenseCatalog.RESOURCE)) {
            assertNotNull(stream);
            var entries = ThirdPartyLicenseCatalog.read(stream);
            assertTrue(entries.stream().anyMatch(e -> e.name.equals("SRIC Minecraft Words") && e.license.equals("Apache-2.0")));
            assertTrue(entries.stream().anyMatch(e -> e.name.equals("cosmic-text")));
            assertTrue(entries.stream().anyMatch(e -> e.name.equals("swash")));
        }
    }

    private URL resource(String directory, String json) throws IOException {
        Path root = temp.resolve(directory);
        Path file = root.resolve(ThirdPartyLicenseCatalog.RESOURCE);
        Files.createDirectories(file.getParent());
        Files.writeString(file, json);
        return root.toUri().toURL();
    }

    private static ByteArrayInputStream stream(String json) {
        return new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
    }
}
