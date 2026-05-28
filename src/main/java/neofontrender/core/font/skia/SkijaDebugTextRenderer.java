package neofontrender.core.font.skia;

import io.github.humbleui.skija.Canvas;
import io.github.humbleui.skija.Data;
import io.github.humbleui.skija.EncodedImageFormat;
import io.github.humbleui.skija.FontMgr;
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
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Standalone Skija smoke test for the full text -> paragraph -> bitmap -> PNG path.
 *
 * <p>This deliberately does not touch Minecraft classes. It proves that the
 * embedded Skija native library can load under the current Java 8 toolchain and
 * that Paragraph rendering can rasterize CJK and emoji text before we wire it
 * into FontRenderer.</p>
 */
public final class SkijaDebugTextRenderer {

    private static final String EMOJI_FONT_RESOURCE = "/assets/neofontrender/fonts/noto_color_emoji_regular.ttf";
    private static final String EMOJI_ALIAS = "Neo Noto Color Emoji";

    private SkijaDebugTextRenderer() {
    }

    public static void main(String[] args) throws Exception {
        Path output = args.length > 0
                ? Paths.get(args[0])
                : Paths.get("build", "skija-debug", "debug-font.png");
        renderDebugPng(output);
        System.out.println("Skija debug PNG written to " + output.toAbsolutePath());
    }

    public static void renderDebugPng(Path output) throws IOException {
        Files.createDirectories(output.toAbsolutePath().getParent());

        try (TypefaceFontProvider provider = new TypefaceFontProvider();
             Typeface emoji = loadBundledTypeface();
             FontCollection fonts = new FontCollection();
             TextStyle textStyle = new TextStyle();
             ParagraphStyle paragraphStyle = new ParagraphStyle()) {

            provider.registerTypeface(emoji, EMOJI_ALIAS);
            fonts.setAssetFontManager(provider);
            fonts.setDefaultFontManager(FontMgr.getDefault());
            fonts.setEnableFallback(true);

            textStyle
                    .setColor(0xFF111111)
                    .setFontSize(30.0F)
                    .setHeight(1.25F)
                    .setFontFamilies(new String[]{
                            "Segoe UI",
                            "Microsoft YaHei UI",
                            "Noto Sans CJK SC",
                            EMOJI_ALIAS,
                            "Segoe UI Emoji",
                            "Apple Color Emoji",
                            "Noto Color Emoji",
                            "SansSerif"
                    });
            paragraphStyle.setTextStyle(textStyle);

            try (ParagraphBuilder builder = new ParagraphBuilder(paragraphStyle, fonts)) {
                builder.pushStyle(textStyle);
                builder.addText("Neo Font Render / Skija paragraph smoke test\n");
                builder.addText("Latin: AAAA aaä 12345\n");
                builder.addText("CJK: 你好世界 こんにちは 안녕하세요\n");
                builder.addText("Emoji: \u2764\uFE0F \u2615\uFE0F \uD83D\uDE00 \uD83D\uDC69\uD83C\uDFFD\u200D\u2695\uFE0F\n");
                builder.addText("ZWJ: \uD83D\uDC68\u200D\uD83D\uDC69\u200D\uD83D\uDC67\u200D\uD83D\uDC66");

                try (Paragraph paragraph = builder.build()) {
                    paragraph.layout(920.0F);
                    int width = Math.max(1, (int) Math.ceil(paragraph.getMaxIntrinsicWidth()) + 48);
                    int height = Math.max(1, (int) Math.ceil(paragraph.getHeight()) + 48);

                    try (Surface surface = Surface.makeRasterN32Premul(width, height)) {
                        Canvas canvas = surface.getCanvas();
                        canvas.clear(0xFFFFFFFF);
                        paragraph.paint(canvas, 24.0F, 24.0F);

                        try (Image image = surface.makeImageSnapshot();
                             Data png = image.encodeToData(EncodedImageFormat.PNG)) {
                            Files.write(output, png.getBytes());
                        }
                    }

                    if (paragraph.getUnresolvedGlyphsCount() > 0) {
                        System.out.println("Skija unresolved glyphs: " + paragraph.getUnresolvedGlyphsCount());
                    }
                }
            }
        }
    }

    private static Typeface loadBundledTypeface() throws IOException {
        byte[] bytes = readResource(EMOJI_FONT_RESOURCE);
        try (Data data = Data.makeFromBytes(bytes)) {
            Typeface typeface = FontMgr.getDefault().makeFromData(data);
            if (typeface == null) {
                throw new IOException("Skija failed to create typeface from " + EMOJI_FONT_RESOURCE);
            }
            return typeface;
        }
    }

    private static byte[] readResource(String path) throws IOException {
        try (InputStream input = SkijaDebugTextRenderer.class.getResourceAsStream(path)) {
            if (input == null) {
                throw new IOException("Missing classpath resource " + path);
            }
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int read;
            while ((read = input.read(buffer)) >= 0) {
                output.write(buffer, 0, read);
            }
            return output.toByteArray();
        }
    }
}
