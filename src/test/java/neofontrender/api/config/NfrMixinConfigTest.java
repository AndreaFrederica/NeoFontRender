package neofontrender.api.config;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class NfrMixinConfigTest {
    @TempDir Path directory;

    @Test
    void createsDefaultsAndPersistsThemBeforeClose() throws Exception {
        Path path = directory.resolve("config/neofontrender-mixins.toml");
        try (CommentedFileConfig config = NfrMixinConfig.load(path)) {
            assertTrue(NfrMixinConfig.enabled(config, "example.MixinOne"));
            assertTrue(Files.size(path) > 0);
            try (CommentedFileConfig saved = NfrMixinConfig.load(path)) {
                assertEquals(Boolean.TRUE, saved.get("mixins.MixinOne"));
                assertTrue(saved.getComment("mixins").contains("restart required"));
            }
        }
    }

    @Test
    void fillsExistingEmptyFile() throws Exception {
        Path path = Files.createFile(directory.resolve("empty.toml"));
        try (CommentedFileConfig config = NfrMixinConfig.load(path)) {
            assertTrue(NfrMixinConfig.enabled(config, "example.MixinOne"));
        }
        try (CommentedFileConfig config = NfrMixinConfig.load(path)) {
            assertEquals(Boolean.TRUE, config.get("mixins.MixinOne"));
        }
    }

    @Test
    void addingDefaultsPreservesOverridesCommentsAndUnknownKeys() throws Exception {
        Path path = directory.resolve("existing.toml");
        Files.writeString(path, "# My switches\n[mixins]\n# Keep disabled\n"
                + "MixinOne = false\nMixinTwo = false\nUnknownMixin = false\n"
                + "[mixins.example]\nMixinTwo = true\nMixinThree = false\n");
        try (CommentedFileConfig config = NfrMixinConfig.load(path)) {
            assertFalse(NfrMixinConfig.enabled(config, "example.MixinOne"));
            assertTrue(NfrMixinConfig.enabled(config, "example.MixinTwo"));
            assertFalse(NfrMixinConfig.enabled(config, "example.MixinThree"));
            assertTrue(NfrMixinConfig.enabled(config, "example.MixinNew"));
        }
        try (CommentedFileConfig config = NfrMixinConfig.load(path)) {
            assertEquals(Boolean.FALSE, config.get("mixins.MixinOne"));
            assertEquals(Boolean.FALSE, config.get("mixins.MixinTwo"));
            assertEquals(Boolean.TRUE, config.get("mixins.example.MixinTwo"));
            assertEquals(Boolean.FALSE, config.get("mixins.UnknownMixin"));
            assertTrue(config.getComment("mixins").contains("My switches"));
            assertTrue(config.getComment("mixins.MixinOne").contains("Keep disabled"));
            assertEquals(Boolean.TRUE, config.get("mixins.MixinNew"));
        }
    }

    @Test
    void readingExistingSwitchDoesNotRewriteFile() throws Exception {
        Path path = directory.resolve("unchanged.toml");
        String original = "[mixins]\nMixinOne=false # custom formatting\n";
        Files.writeString(path, original);
        try (CommentedFileConfig config = NfrMixinConfig.load(path)) {
            assertFalse(NfrMixinConfig.enabled(config, "example.MixinOne"));
            assertFalse(NfrMixinConfig.enabled(config, "example.MixinOne"));
        }
        assertEquals(original, Files.readString(path));
    }
}
