package neofontrender.typst;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class TypstPackagesTest {
    @TempDir Path directory;

    @Test void inventoryAndDeletionAreScopedToOneVersion() throws Exception {
        Path first = directory.resolve("packages/preview/example/1.0.0");
        Path second = directory.resolve("packages/preview/example/2.0.0");
        Files.createDirectories(first);
        Files.createDirectories(second);
        Files.writeString(first.resolve("typst.toml"), "first");
        Files.writeString(second.resolve("typst.toml"), "second");
        Files.writeString(directory.resolve("keep.txt"), "unrelated");
        assertEquals(2, TypstPackages.list(directory).size());
        assertEquals(5, TypstPackages.list(directory).getFirst().bytes());
        TypstPackages.delete(directory, "@preview/example:1.0.0");
        assertFalse(Files.exists(first));
        assertTrue(Files.exists(second.resolve("typst.toml")));
        assertTrue(Files.exists(directory.resolve("keep.txt")));
        assertEquals("@preview/example:2.0.0", TypstPackages.list(directory).getFirst().spec());
    }

    @Test void rejectsTraversalAndUnsupportedNamespace() {
        for (String spec : new String[]{"@preview/../example:1.0.0", "@local/example:1.0.0",
                "@preview/example:../../outside", "@preview/example:latest", "@preview/example:1.0.0/.."}) {
            assertThrows(IllegalArgumentException.class, () -> TypstPackages.delete(directory, spec));
        }
    }

    @Test void decodesUnknownSizeAndFailureDetails() {
        var events = TypstEvent.decode(new String[]{"downloading", "@preview/example:1.0.0", "512", "", "",
                "download_failed", "@preview/example:1.0.0", "0", "", "connection refused"});
        assertTrue(events.getFirst().active());
        assertEquals(-1, events.getFirst().total());
        assertEquals(512, events.getFirst().downloaded());
        assertTrue(events.getLast().failed());
        assertEquals("connection refused", events.getLast().detail());
    }
}
