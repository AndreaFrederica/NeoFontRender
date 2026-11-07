package neofontrender.core.font.cosmic;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class CosmicCharacterCompositionNativeTest {
    private static long engine;
    private static CosmicMonospaceComposer composer;

    @BeforeAll
    static void createEngine() throws Exception {
        CosmicRuntimeSupport.Compatibility compatibility = CosmicRuntimeSupport.ensureLoaded();
        assertTrue(compatibility.isSupported(), compatibility.getMessage());
        byte[] font;
        try (InputStream stream = CosmicCharacterCompositionNativeTest.class.getResourceAsStream(
                "/assets/neofontrender/fonts/noto_sans_sc-regular.otf")) {
            assertNotNull(stream);
            font = stream.readAllBytes();
        }
        engine = CosmicNative.createEngine(new byte[][]{font}, new String[]{"character-test"},
                "Noto Sans SC", new String[0], "", "", "", "", false, 0, 9, "zh-CN");
        assertNotEquals(0, engine);
        composer = new CosmicMonospaceComposer((text, flags, size, scale) ->
                CosmicNative.monospaceSplitPointsSized(engine, text, flags, size, scale), () -> 0L);
    }

    @AfterAll
    static void closeEngine() {
        if (engine != 0) CosmicNative.destroyEngine(engine);
    }

    @Test
    void proportionalFontStillSharesItsTabularDigits() {
        assertEquals(List.of("1", "2", "3", ".", "4", "5"),
                composer.split("123.45", 0, 9, 12, true));
        // Noto substitutes distinct Latin-context digit glyphs when a unit suffix is present.
        // They have the same advances, but independently shaping the digits would change glyphs.
        assertEquals(List.of("123.45MB"), composer.split("123.45MB", 0, 9, 12, true));
        assertEquals(List.of("office"), composer.split("office", 0, 9, 12, true));
    }

    @Test
    void independentBackgroundEngineMatchesForegroundPixelsWhileForegroundMeasures() throws Exception {
        byte[] font;
        try (InputStream stream = getClass().getResourceAsStream("/assets/neofontrender/fonts/noto_sans_sc-regular.otf")) {
            assertNotNull(stream);
            font = stream.readAllBytes();
        }
        String text = "中文 123.45MB office !=";
        byte[] expected = CosmicNative.renderSized(engine, text, 0xFFCCDDFF, 1, 9, 4);
        float width = CosmicNative.measureSized(engine, text, 1, 9);
        var initialized = new java.util.concurrent.CountDownLatch(1);
        var release = new java.util.concurrent.CountDownLatch(1);
        var destroyed = new java.util.concurrent.CountDownLatch(1);
        try (var worker = new CosmicAsyncWork<>(() -> CosmicNative.createEngine(new byte[][]{font},
                new String[]{"character-test"}, "Noto Sans SC", new String[0],
                "", "", "", "", false, 0, 9, "zh-CN"), handle -> {
                    CosmicNative.destroyEngine(handle);
                    destroyed.countDown();
                })) {
            worker.request("pixels", handle -> {
                assertNotEquals(engine, handle);
                initialized.countDown();
                try { assertTrue(release.await(10, java.util.concurrent.TimeUnit.SECONDS)); }
                catch (InterruptedException error) { throw new AssertionError(error); }
                byte[] pixels = CosmicNative.renderSized(handle, text, 0xFFCCDDFF, 1, 9, 4);
                return new CosmicAsyncWork.Payload(pixels, pixels.length);
            });
            assertTrue(initialized.await(10, java.util.concurrent.TimeUnit.SECONDS));
            try {
                for (int i = 0; i < 10; i++) assertEquals(width, CosmicNative.measureSized(engine, text, 1, 9));
            } finally { release.countDown(); }
            assertTimeoutPreemptively(java.time.Duration.ofSeconds(10), () -> {
                while (worker.stats().ready() == 0) Thread.sleep(1);
            });
            assertArrayEquals(expected, (byte[]) worker.take("pixels").value());
        } finally { release.countDown(); }
        assertTrue(destroyed.await(10, java.util.concurrent.TimeUnit.SECONDS));
    }

    @Test
    void cachesCjkCharactersAndKeepsNativeAdvancesAtDifferentSizesAndStyles() {
        for (String text : List.of("中文状态测试", "ひらがな", "カタカナ", "한국어", "剩余：123个！")) {
            for (int flags = 0; flags < 4; flags++) {
                for (float size : new float[]{9, 16}) {
                    List<String> parts = composer.split(text, flags, size, 12, true);
                    assertTrue(parts.size() > 1, text + " flags=" + flags);
                    assertEquals(text, String.join("", parts));
                    float measured = 0;
                    float rasterAdvance = 0;
                    for (String part : parts) {
                        measured += CosmicNative.measureSized(engine, part, flags, size);
                        rasterAdvance += ByteBuffer.wrap(CosmicNative.renderSized(
                                engine, part, 0xFFFFFFFF, flags, size, 12)).order(ByteOrder.LITTLE_ENDIAN).getFloat(20);
                    }
                    assertEquals(CosmicNative.measureSized(engine, text, flags, size), measured, 0.01F, text);
                    assertEquals(measured, rasterAdvance, 0.01F, text);
                }
            }
        }
    }

    @Test
    void changingOneThousandNumbersNeedsOnlyTenDigitTextures() {
        Set<String> characterTextures = new HashSet<>();
        for (int value = 1000; value < 2000; value++) {
            List<String> parts = composer.split(Integer.toString(value), 0, 9, 12, true);
            assertEquals(4, parts.size());
            characterTextures.addAll(parts);
        }
        assertEquals(10, characterTextures.size());
    }

    @Test
    void installedMonospaceProgrammingFontPreservesItsRealLigatures() throws Exception {
        java.nio.file.Path path = java.nio.file.Path.of(System.getProperty("user.home"),
                "AppData/Local/Microsoft/Windows/Fonts/FiraCode-Regular.ttf");
        org.junit.jupiter.api.Assumptions.assumeTrue(java.nio.file.Files.isRegularFile(path));
        long fira = CosmicNative.createEngine(new byte[][]{java.nio.file.Files.readAllBytes(path)},
                new String[]{"fira-test"}, "Fira Code", new String[0], "", "", "", "", false, 0, 9, "en-US");
        try {
            CosmicMonospaceComposer real = new CosmicMonospaceComposer((text, flags, size, scale) ->
                    CosmicNative.monospaceSplitPointsSized(fira, text, flags, size, scale), () -> 0L);
            assertEquals(List.of("a", "b", "c", "1", "2", "3"), real.split("abc123", 0, 9, 12, true));
            for (String text : List.of("!=", "->", "===", "=>")) {
                assertEquals(List.of(text), real.split(text, 0, 9, 12, true), text);
            }
        } finally {
            CosmicNative.destroyEngine(fira);
        }
    }
}
