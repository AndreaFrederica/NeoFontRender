package neofontrender.addons.typst;

import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TypstConfigTest {
    @Test
    void placesTypstLibraryBesideTheFontDirectory() {
        File nfrDirectory = new File("build/test-typst-game/neofontrender").getAbsoluteFile();

        File directory = TypstConfig.libraryDirectory(nfrDirectory);

        assertEquals(new File(nfrDirectory, "typst").toPath().toAbsolutePath().normalize(),
                directory.toPath().toAbsolutePath().normalize());
        assertTrue(directory.isDirectory());
        assertTrue(new File(directory, "packages").isDirectory());
    }
}
