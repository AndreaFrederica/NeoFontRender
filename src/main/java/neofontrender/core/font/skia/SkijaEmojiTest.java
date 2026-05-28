package neofontrender.core.font.skia;

import io.github.humbleui.skija.Canvas;
import io.github.humbleui.skija.Data;
import io.github.humbleui.skija.EncodedImageFormat;
import io.github.humbleui.skija.FontMgr;
import io.github.humbleui.skija.FontStyle;
import io.github.humbleui.skija.Image;
import io.github.humbleui.skija.Surface;
import io.github.humbleui.skija.Typeface;
import io.github.humbleui.skija.paragraph.FontCollection;
import io.github.humbleui.skija.paragraph.Paragraph;
import io.github.humbleui.skija.paragraph.ParagraphBuilder;
import io.github.humbleui.skija.paragraph.ParagraphStyle;
import io.github.humbleui.skija.paragraph.TextStyle;
import io.github.humbleui.skija.paragraph.TypefaceFontProvider;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Standalone test: reproduces the production renderer's font setup
 * to verify emoji rendering with different font family orderings.
 */
public class SkijaEmojiTest {

    private static final String EMOJI_RESOURCE = "/assets/neofontrender/fonts/noto_color_emoji_regular.ttf";
    private static final String TEST_TEXT = "Hello 😀 World ❤️ Test";

    public static void main(String[] args) throws Exception {
        Path outputDir = args.length > 0
                ? Paths.get(args[0])
                : Paths.get("build", "skija-debug");
        Files.createDirectories(outputDir);

        // Test 1: Production order (Sarasa first, emoji last)
        testWithOrder(outputDir.resolve("emoji_test_prod_order.png"),
                new String[]{
                        "Sarasa UI SC",
                        "Serif",
                        "Monospaced",
                        "Segoe UI Emoji",
                        "Apple Color Emoji",
                        "Noto Color Emoji",
                        "Noto Emoji",
                        "Droid Sans Fallback"
                }, true);

        // Test 2: Emoji first
        testWithOrder(outputDir.resolve("emoji_test_emoji_first.png"),
                new String[]{
                        "Segoe UI Emoji",
                        "Sarasa UI SC",
                        "Serif",
                        "Monospaced",
                        "Apple Color Emoji",
                        "Noto Color Emoji",
                        "Noto Emoji",
                        "Droid Sans Fallback"
                }, true);

        // Test 3: Emoji first, with bundled Noto Color Emoji registered
        testWithOrder(outputDir.resolve("emoji_test_bundled_emoji_first.png"),
                new String[]{
                        "Noto Color Emoji",
                        "Sarasa UI SC",
                        "Serif",
                        "Monospaced",
                        "Segoe UI Emoji",
                        "Apple Color Emoji",
                        "Noto Emoji",
                        "Droid Sans Fallback"
                }, true);

        // Test 4: Only system emoji fonts, no bundled
        testWithOrder(outputDir.resolve("emoji_test_system_only.png"),
                new String[]{
                        "Sarasa UI SC",
                        "Segoe UI Emoji",
                        "Noto Emoji"
                }, false);

        // Test 5: Check what Sarasa UI SC resolves to for emoji
        testSarasaEmoji(outputDir.resolve("emoji_test_sarasa_check.png"));

        System.out.println("All tests written to " + outputDir.toAbsolutePath());
    }

    private static void testWithOrder(Path output, String[] families, boolean registerBundled) throws IOException {
        System.out.println("Testing: " + output.getFileName() + " (families: " + String.join(", ", families) + ")");

        try (TypefaceFontProvider provider = new TypefaceFontProvider();
             FontCollection fonts = new FontCollection()) {

            Typeface emojiTypeface = null;
            if (registerBundled) {
                emojiTypeface = loadBundledEmoji();
                provider.registerTypeface(emojiTypeface, "Noto Color Emoji");
                // Also register under the resource path name
                provider.registerTypeface(emojiTypeface, "neofontrender:fonts/noto_color_emoji_regular.ttf");
                System.out.println("  Bundled Noto Color Emoji registered in fontProvider (family: " + emojiTypeface.getFamilyName() + ")");
            }

            fonts.setAssetFontManager(provider);
            fonts.setDefaultFontManager(FontMgr.getDefault());
            fonts.setEnableFallback(true);

            TextStyle textStyle = new TextStyle()
                    .setColor(0xFF111111)
                    .setFontSize(24.0F)
                    .setHeight(1.25F)
                    .setFontFamilies(families);

            ParagraphStyle paragraphStyle = new ParagraphStyle();

            try (ParagraphBuilder builder = new ParagraphBuilder(paragraphStyle, fonts)) {
                builder.pushStyle(textStyle);
                builder.addText(TEST_TEXT);

                try (Paragraph paragraph = builder.build()) {
                    paragraph.layout(600.0F);
                    int unresolved = paragraph.getUnresolvedGlyphsCount();
                    System.out.println("  Unresolved glyphs: " + unresolved);

                    int width = Math.max(1, (int) Math.ceil(paragraph.getMaxIntrinsicWidth()) + 48);
                    int height = Math.max(1, (int) Math.ceil(paragraph.getHeight()) + 48);

                    try (Surface surface = Surface.makeRasterN32Premul(width, height)) {
                        Canvas canvas = surface.getCanvas();
                        canvas.clear(0xFFFFFFFF);
                        paragraph.paint(canvas, 24.0F, 24.0F);

                        try (Image image = surface.makeImageSnapshot();
                             Data png = image.encodeToData(EncodedImageFormat.PNG)) {
                            Files.write(output, png.getBytes());
                            System.out.println("  Written: " + output.toAbsolutePath());
                        }
                    }
                }
            }

            if (emojiTypeface != null) {
                emojiTypeface.close();
            }
        }
    }

    private static void testSarasaEmoji(Path output) throws IOException {
        System.out.println("Testing Sarasa UI SC emoji rendering directly...");

        try (FontCollection fonts = new FontCollection()) {
            fonts.setDefaultFontManager(FontMgr.getDefault());
            fonts.setEnableFallback(false);

            TextStyle textStyle = new TextStyle()
                    .setColor(0xFF111111)
                    .setFontSize(24.0F)
                    .setHeight(1.25F)
                    .setFontFamilies(new String[]{"Sarasa UI SC"});

            try (ParagraphBuilder builder = new ParagraphBuilder(new ParagraphStyle(), fonts)) {
                builder.pushStyle(textStyle);
                builder.addText("Sarasa only: 😀 ❤️ 🎉");

                try (Paragraph paragraph = builder.build()) {
                    paragraph.layout(600.0F);
                    int unresolved = paragraph.getUnresolvedGlyphsCount();
                    System.out.println("  Sarasa without fallback - unresolved: " + unresolved);

                    int width = Math.max(1, (int) Math.ceil(paragraph.getMaxIntrinsicWidth()) + 48);
                    int height = Math.max(1, (int) Math.ceil(paragraph.getHeight()) + 48);

                    try (Surface surface = Surface.makeRasterN32Premul(width, height)) {
                        Canvas canvas = surface.getCanvas();
                        canvas.clear(0xFFFFFFFF);
                        paragraph.paint(canvas, 24.0F, 24.0F);

                        try (Image image = surface.makeImageSnapshot();
                             Data png = image.encodeToData(EncodedImageFormat.PNG)) {
                            Files.write(output, png.getBytes());
                            System.out.println("  Written: " + output.toAbsolutePath());
                        }
                    }
                }
            }
        }
    }

    private static Typeface loadBundledEmoji() throws IOException {
        try (InputStream input = SkijaEmojiTest.class.getResourceAsStream(EMOJI_RESOURCE)) {
            if (input == null) {
                throw new IOException("Missing resource: " + EMOJI_RESOURCE);
            }
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int read;
            while ((read = input.read(buffer)) >= 0) {
                output.write(buffer, 0, read);
            }
            try (Data data = Data.makeFromBytes(output.toByteArray())) {
                Typeface tf = FontMgr.getDefault().makeFromData(data);
                if (tf == null) {
                    throw new IOException("Skija failed to create typeface from " + EMOJI_RESOURCE);
                }
                return tf;
            }
        }
    }
}
