package neofontrender.core.font.skia;

import io.github.humbleui.skija.Bitmap;
import io.github.humbleui.skija.Canvas;
import io.github.humbleui.skija.Data;
import io.github.humbleui.skija.FontMgr;
import io.github.humbleui.skija.FontStyle;
import io.github.humbleui.skija.ImageInfo;
import io.github.humbleui.skija.Surface;
import io.github.humbleui.skija.Typeface;
import io.github.humbleui.skija.paragraph.FontCollection;
import io.github.humbleui.skija.paragraph.Paragraph;
import io.github.humbleui.skija.paragraph.ParagraphBuilder;
import io.github.humbleui.skija.paragraph.ParagraphStyle;
import io.github.humbleui.skija.paragraph.DecorationLineStyle;
import io.github.humbleui.skija.paragraph.DecorationStyle;
import io.github.humbleui.skija.paragraph.TextStyle;
import io.github.humbleui.skija.paragraph.TypefaceFontProvider;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.resources.IResource;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.util.ResourceLocation;
import neofontrender.NeoFontRender;
import neofontrender.core.config.NeofontrenderConfig;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * First Skija-backed renderer path. It renders shaped paragraph runs into
 * cached Minecraft dynamic textures. A glyph atlas can replace this later
 * without changing the FontRenderer mixin dispatch surface.
 */
public final class SkijaTextRenderer implements AutoCloseable {

    private static final int MAX_CACHE_SIZE = 512;
    private static final String[] PLATFORM_EMOJI_FONTS = {
            "Segoe UI Emoji",
            "Apple Color Emoji",
            "Noto Color Emoji",
            "Noto Emoji",
            "Droid Sans Fallback"
    };

    private final TextureManager textureManager;
    private final IResourceManager resourceManager;
    private final TypefaceFontProvider fontProvider;
    private final FontCollection fontCollection;
    private final List<Typeface> ownedTypefaces = new ArrayList<>();
    private final String[] fontFamilies;
    private final float oversample;
    private final Map<RenderKey, RenderedText> renderCache = Collections.synchronizedMap(
            new LinkedHashMap<RenderKey, RenderedText>(MAX_CACHE_SIZE, 0.75F, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<RenderKey, RenderedText> eldest) {
                    if (size() <= MAX_CACHE_SIZE) {
                        return false;
                    }
                    eldest.getValue().close();
                    return true;
                }
            });
    private final Map<MeasureKey, Float> measureCache = Collections.synchronizedMap(
            new LinkedHashMap<MeasureKey, Float>(MAX_CACHE_SIZE, 0.75F, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<MeasureKey, Float> eldest) {
                    return size() > MAX_CACHE_SIZE;
                }
            });
    private int nextTextureId = 0;

    public SkijaTextRenderer(TextureManager textureManager, IResourceManager resourceManager) throws IOException {
        this.textureManager = textureManager;
        this.resourceManager = resourceManager;
        this.fontProvider = new TypefaceFontProvider();
        this.oversample = Math.max(1.0F, Math.min(16.0F, NeofontrenderConfig.fontOversample()));
        this.fontFamilies = registerConfiguredFonts();
        this.fontCollection = new FontCollection()
                .setAssetFontManager(fontProvider)
                .setDefaultFontManager(FontMgr.getDefault())
                .setEnableFallback(true);
    }

    public boolean isReady() {
        return fontCollection != null;
    }

    public String[] getFontFamilies() {
        return fontFamilies;
    }

    public float measure(String text, boolean bold, boolean italic) {
        if (text == null || text.isEmpty()) {
            return 0.0F;
        }
        MeasureKey key = new MeasureKey(text, bold, italic);
        Float cached = measureCache.get(key);
        if (cached != null) {
            return cached;
        }
        float width;
        try (Paragraph paragraph = buildParagraph(text, 0xFFFFFFFF, bold, italic, oversample)) {
            paragraph.layout(100000.0F * oversample);
            width = Math.max(paragraph.getMaxIntrinsicWidth(), paragraph.getLongestLine()) / oversample;
        }
        measureCache.put(key, width);
        return width;
    }

    public float measureFormatted(String text, int baseArgb, boolean shadow) {
        if (text == null || text.isEmpty()) {
            return 0.0F;
        }
        try (Paragraph paragraph = buildFormattedParagraph(text, baseArgb, shadow, oversample)) {
            paragraph.layout(100000.0F * oversample);
            return Math.max(paragraph.getMaxIntrinsicWidth(), paragraph.getLongestLine()) / oversample;
        }
    }

    public RenderedText render(String text, int argb, boolean bold, boolean italic) {
        if (text == null || text.isEmpty()) {
            return RenderedText.EMPTY;
        }
        RenderKey key = new RenderKey(text, argb, bold, italic);
        RenderedText cached = renderCache.get(key);
        if (cached != null) {
            return cached;
        }

        try {
            RenderedText rendered = rasterize(key);
            renderCache.put(key, rendered);
            return rendered;
        } catch (Throwable t) {
            NeoFontRender.LOGGER.error("Failed to render Skija text run '{}'", text, t);
            return RenderedText.EMPTY;
        }
    }

    public RenderedText renderFormatted(String text, int baseArgb, boolean shadow) {
        if (text == null || text.isEmpty()) {
            return RenderedText.EMPTY;
        }
        RenderKey key = new RenderKey(text, normalizeBaseArgb(baseArgb), shadow, true);
        RenderedText cached = renderCache.get(key);
        if (cached != null) {
            return cached;
        }

        try {
            RenderedText rendered = rasterizeFormatted(key);
            renderCache.put(key, rendered);
            return rendered;
        } catch (Throwable t) {
            NeoFontRender.LOGGER.error("Failed to render formatted Skija text '{}'", text, t);
            return RenderedText.EMPTY;
        }
    }

    public void prewarmBasicLatin() {
        for (int cp = 32; cp <= 126; cp++) {
            String text = new String(Character.toChars(cp));
            measure(text, false, false);
            if (cp != ' ') {
                render(text, 0xFFFFFFFF, false, false);
            }
        }
        for (int cp = 160; cp <= 255; cp++) {
            String text = new String(Character.toChars(cp));
            measure(text, false, false);
            if (cp != 160) {
                render(text, 0xFFFFFFFF, false, false);
            }
        }
    }

    private RenderedText rasterize(RenderKey key) throws IOException {
        float measuredWidth = Math.max(1.0F, measure(key.text, key.bold, key.italic));
        int width = Math.max(1, (int) Math.ceil((measuredWidth + 4.0F) * oversample));
        try (Paragraph paragraph = buildParagraph(key.text, key.argb, key.bold, key.italic, oversample)) {
            paragraph.layout(width);
            return rasterizeParagraph(paragraph, key, measuredWidth, width);
        }
    }

    private RenderedText rasterizeFormatted(RenderKey key) throws IOException {
        float measuredWidth = Math.max(1.0F, measureFormatted(key.text, key.argb, key.bold));
        int width = Math.max(1, (int) Math.ceil((measuredWidth + 4.0F) * oversample));
        try (Paragraph paragraph = buildFormattedParagraph(key.text, key.argb, key.bold, oversample)) {
            paragraph.layout(width);
            return rasterizeParagraph(paragraph, key, measuredWidth, width);
        }
    }

    private RenderedText rasterizeParagraph(Paragraph paragraph, RenderKey key, float measuredWidth, int width) throws IOException {
        int height;
        float verticalOffset;

        height = Math.max(1, (int) Math.ceil(paragraph.getHeight() + 4.0F * oversample));
        float paintX = 2.0F * oversample;
        float paintY = 1.0F * oversample;
        float baselineInTexture = (paintY + paragraph.getAlphabeticBaseline()) / oversample;
        verticalOffset = NeofontrenderConfig.fontAutoBaseline()
                ? NeofontrenderConfig.fontReferenceBaseline() + NeofontrenderConfig.fontBaselineShift() - baselineInTexture
                : NeofontrenderConfig.fontBaselineShift();
        float horizontalOffset = -paintX / oversample;

        try (Surface surface = Surface.makeRasterN32Premul(width, height)) {
            Canvas canvas = surface.getCanvas();
            canvas.clear(0x00000000);
            paragraph.paint(canvas, paintX, paintY);

            Bitmap bitmap = new Bitmap();
            bitmap.allocN32Pixels(width, height);
            surface.readPixels(bitmap, 0, 0);

            byte[] rawBytes = bitmap.readPixels();
            bitmap.close();

            int[] pixels = new int[width * height];
            ByteBuffer.wrap(rawBytes).order(ByteOrder.LITTLE_ENDIAN).asIntBuffer().get(pixels);

            for (int i = 0; i < pixels.length; i++) {
                int px = pixels[i];
                int a = (px >>> 24);
                if (a > 0 && a < 255) {
                    int r = Math.min(255, ((px >> 16) & 0xFF) * 255 / a);
                    int g = Math.min(255, ((px >> 8) & 0xFF) * 255 / a);
                    int b = Math.min(255, (px & 0xFF) * 255 / a);
                    pixels[i] = (a << 24) | (r << 16) | (g << 8) | b;
                }
            }

            DynamicTexture texture = new DynamicTexture(width, height);
            int[] target = texture.getTextureData();
            System.arraycopy(pixels, 0, target, 0, Math.min(pixels.length, target.length));
            texture.updateDynamicTexture();
            texture.setBlurMipmap(NeofontrenderConfig.renderingInterpolation(), NeofontrenderConfig.renderingMipmap());

            ResourceLocation location = new ResourceLocation("neofontrender",
                    "skia/" + Integer.toHexString(key.hashCode()) + "_" + nextTextureId++);
            textureManager.loadTexture(location, texture);
            texture.setBlurMipmap(NeofontrenderConfig.renderingInterpolation(), NeofontrenderConfig.renderingMipmap());
            return new RenderedText(location, texture, measuredWidth,
                    width, height,
                    width / oversample, height / oversample,
                    horizontalOffset, verticalOffset);
        }
    }

    private Paragraph buildParagraph(String text, int argb, boolean bold, boolean italic, float scale) {
        ParagraphBuilder builder = new ParagraphBuilder(new ParagraphStyle(), fontCollection);
        appendStyledText(builder, text, argb, bold, italic, false, false, scale);
        return builder.build();
    }

    private Paragraph buildFormattedParagraph(String text, int baseArgb, boolean shadow, float scale) {
        ParagraphBuilder builder = new ParagraphBuilder(new ParagraphStyle(), fontCollection);
        boolean bold = false;
        boolean italic = false;
        boolean underline = false;
        boolean strikethrough = false;
        int color = shadow ? shadowBaseArgb(baseArgb) : normalizeBaseArgb(baseArgb);
        int runStart = 0;

        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) != 167 || i + 1 >= text.length()) {
                continue;
            }
            if (i > runStart) {
                appendStyledText(builder, text.substring(runStart, i), color, bold, italic, underline, strikethrough, scale);
            }

            int style = "0123456789abcdefklmnor".indexOf(Character.toLowerCase(text.charAt(i + 1)));
            if (style >= 0 && style < 16) {
                bold = false;
                italic = false;
                underline = false;
                strikethrough = false;
                color = colorFromCode(style, shadow, baseArgb);
            } else if (style == 17) {
                bold = true;
            } else if (style == 18) {
                strikethrough = true;
            } else if (style == 19) {
                underline = true;
            } else if (style == 20) {
                italic = true;
            } else if (style == 21) {
                bold = false;
                italic = false;
                underline = false;
                strikethrough = false;
                color = shadow ? shadowBaseArgb(baseArgb) : normalizeBaseArgb(baseArgb);
            }

            i++;
            runStart = i + 1;
        }

        if (runStart < text.length()) {
            appendStyledText(builder, text.substring(runStart), color, bold, italic, underline, strikethrough, scale);
        }

        return builder.build();
    }

    private void appendStyledText(ParagraphBuilder builder, String text, int argb, boolean bold, boolean italic,
                                  boolean underline, boolean strikethrough, float scale) {
        if (text.isEmpty()) {
            return;
        }
        try (TextStyle textStyle = makeTextStyle(argb, bold, italic, scale)) {
            if (underline || strikethrough) {
                textStyle.setDecorationStyle(new DecorationStyle(
                        underline, false, strikethrough, false, opaqueRgb(argb),
                        DecorationLineStyle.SOLID, 1.0F));
            }
            builder.pushStyle(textStyle);
            builder.addText(text);
            builder.popStyle();
        }
    }

    private TextStyle makeTextStyle(int argb, boolean bold, boolean italic, float scale) {
        int configuredStyle = NeofontrenderConfig.fontStyle();
        boolean effectiveBold = bold || (configuredStyle & 1) != 0;
        boolean effectiveItalic = italic || (configuredStyle & 2) != 0;
        FontStyle style = effectiveBold && effectiveItalic ? FontStyle.BOLD_ITALIC
                : effectiveBold ? FontStyle.BOLD
                : effectiveItalic ? FontStyle.ITALIC
                : FontStyle.NORMAL;
        return new TextStyle()
                .setColor(opaqueRgb(argb))
                .setFontSize(NeofontrenderConfig.fontSize() * scale)
                .setFontStyle(style)
                .setFontFamilies(fontFamilies)
                .setHeight(1.0F);
    }

    private static int normalizeBaseArgb(int argb) {
        return (argb & 0xFC000000) == 0 ? argb | 0xFF000000 : argb;
    }

    private static int opaqueRgb(int argb) {
        return normalizeBaseArgb(argb) | 0xFF000000;
    }

    private static int shadowBaseArgb(int argb) {
        int color = normalizeBaseArgb(argb);
        return (color & 0xFCFCFC) >> 2 | color & 0xFF000000;
    }

    private static int colorFromCode(int index, boolean shadow, int baseArgb) {
        int i = (index >> 3 & 1) * 85;
        int r = (index >> 2 & 1) * 170 + i;
        int g = (index >> 1 & 1) * 170 + i;
        int b = (index & 1) * 170 + i;
        if (index == 6) {
            r += 85;
        }
        if (shadow) {
            r >>= 2;
            g >>= 2;
            b >>= 2;
        }
        int a = normalizeBaseArgb(baseArgb) & 0xFF000000;
        return a | r << 16 | g << 8 | b;
    }

    private String[] registerConfiguredFonts() throws IOException {
        List<String> families = new ArrayList<>();
        for (String name : NeofontrenderConfig.fontFamily()) {
            List<String> registered = registerFont(name);
            for (String family : registered) {
                if (family != null && !family.isEmpty() && !families.contains(family)) {
                    families.add(family);
                }
            }
        }
        for (String emoji : PLATFORM_EMOJI_FONTS) {
            if (!families.contains(emoji)) {
                families.add(emoji);
            }
        }
        if (families.isEmpty()) {
            families.add("SansSerif");
        }
        return families.toArray(new String[0]);
    }

    private List<String> registerFont(String name) throws IOException {
        List<String> aliases = new ArrayList<>();
        if (name == null || name.trim().isEmpty()) {
            return aliases;
        }
        aliases.add(name);
        File file = new File(name);
        if (file.isFile()) {
            Typeface typeface = FontMgr.getDefault().makeFromFile(file.getAbsolutePath());
            ownedTypefaces.add(typeface);
            fontProvider.registerTypeface(typeface, name);
            addTypefaceFamilyAlias(typeface, aliases);
            return aliases;
        }
        if (name.indexOf(':') >= 0) {
            ResourceLocation location = new ResourceLocation(name);
            IResource resource = resourceManager.getResource(location);
            try (InputStream input = resource.getInputStream()) {
                byte[] bytes = readAllBytes(input);
                Typeface typeface = FontMgr.getDefault().makeFromData(Data.makeFromBytes(bytes));
                ownedTypefaces.add(typeface);
                fontProvider.registerTypeface(typeface, name);
                addTypefaceFamilyAlias(typeface, aliases);
                for (NeofontrenderConfig.BuiltinFont builtin : NeofontrenderConfig.builtinFonts()) {
                    if (builtin.location().equals(name)) {
                        fontProvider.registerTypeface(typeface, builtin.displayName());
                        aliases.add(builtin.displayName());
                    }
                }
            }
            return aliases;
        }
        return aliases;
    }

    private void addTypefaceFamilyAlias(Typeface typeface, List<String> aliases) {
        String family = typeface.getFamilyName();
        if (family != null && !family.isEmpty()) {
            fontProvider.registerTypeface(typeface, family);
            aliases.add(family);
        }
    }

    private static byte[] readAllBytes(InputStream input) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int read;
        while ((read = input.read(buffer)) >= 0) {
            output.write(buffer, 0, read);
        }
        return output.toByteArray();
    }

    @Override
    public void close() {
        for (RenderedText text : renderCache.values()) {
            text.close();
        }
        renderCache.clear();
        measureCache.clear();
        for (Typeface typeface : ownedTypefaces) {
            typeface.close();
        }
        ownedTypefaces.clear();
        fontCollection.close();
        fontProvider.close();
    }

    public static final class RenderedText implements AutoCloseable {
        private static final RenderedText EMPTY = new RenderedText(null, null, 0.0F, 0, 0, 0.0F, 0.0F, 0.0F, 0.0F);

        private final ResourceLocation location;
        private final DynamicTexture texture;
        private final float advance;
        private final int width;
        private final int height;
        private final float drawWidth;
        private final float drawHeight;
        private final float horizontalOffset;
        private final float verticalOffset;

        private RenderedText(ResourceLocation location, DynamicTexture texture, float advance,
                             int width, int height, float drawWidth, float drawHeight,
                             float horizontalOffset, float verticalOffset) {
            this.location = location;
            this.texture = texture;
            this.advance = advance;
            this.width = width;
            this.height = height;
            this.drawWidth = drawWidth;
            this.drawHeight = drawHeight;
            this.horizontalOffset = horizontalOffset;
            this.verticalOffset = verticalOffset;
        }

        public float advance() {
            return advance;
        }

        public void draw(float x, float y, float alpha) {
            if (location == null || texture == null || width <= 0 || height <= 0) {
                return;
            }
            net.minecraft.client.Minecraft.getMinecraft().getTextureManager().bindTexture(location);
            GlStateManager.enableTexture2D();
            GlStateManager.enableAlpha();
            GlStateManager.color(1.0F, 1.0F, 1.0F, alpha);
            float left = x + horizontalOffset;
            float top = y + verticalOffset;

            Tessellator tessellator = Tessellator.getInstance();
            BufferBuilder buffer = tessellator.getBuffer();
            buffer.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);
            buffer.pos(left, top, 0.0D).tex(0.0D, 0.0D).color(1.0F, 1.0F, 1.0F, alpha).endVertex();
            buffer.pos(left, top + drawHeight, 0.0D).tex(0.0D, 1.0D).color(1.0F, 1.0F, 1.0F, alpha).endVertex();
            buffer.pos(left + drawWidth, top + drawHeight, 0.0D).tex(1.0D, 1.0D).color(1.0F, 1.0F, 1.0F, alpha).endVertex();
            buffer.pos(left + drawWidth, top, 0.0D).tex(1.0D, 0.0D).color(1.0F, 1.0F, 1.0F, alpha).endVertex();
            tessellator.draw();
        }

        @Override
        public void close() {
            if (texture != null) {
                texture.deleteGlTexture();
            }
        }
    }

    private static final class RenderKey {
        private final String text;
        private final int argb;
        private final boolean bold;
        private final boolean italic;

        private RenderKey(String text, int argb, boolean bold, boolean italic) {
            this.text = text;
            this.argb = argb;
            this.bold = bold;
            this.italic = italic;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof RenderKey)) {
                return false;
            }
            RenderKey other = (RenderKey) obj;
            return argb == other.argb && bold == other.bold && italic == other.italic && text.equals(other.text);
        }

        @Override
        public int hashCode() {
            int result = text.hashCode();
            result = 31 * result + argb;
            result = 31 * result + (bold ? 1 : 0);
            result = 31 * result + (italic ? 1 : 0);
            return result;
        }
    }

    private static final class MeasureKey {
        private final String text;
        private final boolean bold;
        private final boolean italic;

        private MeasureKey(String text, boolean bold, boolean italic) {
            this.text = text;
            this.bold = bold;
            this.italic = italic;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof MeasureKey)) {
                return false;
            }
            MeasureKey other = (MeasureKey) obj;
            return bold == other.bold && italic == other.italic && text.equals(other.text);
        }

        @Override
        public int hashCode() {
            int result = text.hashCode();
            result = 31 * result + (bold ? 1 : 0);
            result = 31 * result + (italic ? 1 : 0);
            return result;
        }
    }
}
