package neofontrender.addons.inline;

import org.junit.jupiter.api.Test;

import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertEquals;

class InlineImageServiceCachePathTest {
    @Test
    void imageCacheIsASiblingOfTheFontsDirectory() {
        assertEquals(Paths.get("instance", "neofontrender", "image-cache"),
                InlineImageService.cacheRoot(Paths.get("instance")));
    }

    @Test
    void localGalleryIsAlsoASiblingOfTheFontsDirectory() {
        assertEquals(Paths.get("instance", "neofontrender", "images"),
                LocalImageCatalog.galleryRoot(Paths.get("instance")));
    }
}
