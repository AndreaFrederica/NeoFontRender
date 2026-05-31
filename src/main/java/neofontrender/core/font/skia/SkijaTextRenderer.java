package neofontrender.core.font.skia;

import io.github.humbleui.skija.Bitmap;
import io.github.humbleui.skija.Canvas;
import io.github.humbleui.skija.Data;
import io.github.humbleui.skija.FontMgr;
import io.github.humbleui.skija.FontStyle;
import io.github.humbleui.skija.ImageInfo;
import io.github.humbleui.skija.Paint;
import io.github.humbleui.skija.PixelGeometry;
import io.github.humbleui.skija.Surface;
import io.github.humbleui.skija.SurfaceProps;
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
import neofontrender.core.font.backend.TextRenderBackend;
import neofontrender.core.font.backend.TextRenderResult;
import neofontrender.core.font.support.FontBrightnessEstimator;
import neofontrender.core.font.support.FontPixelUtils;
import neofontrender.core.font.support.FontRenderPipeline;
import neofontrender.core.font.support.FontRenderTuning;
import org.lwjgl.opengl.GL11;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * First Skija-backed renderer path. It renders shaped paragraph runs into
 * cached Minecraft dynamic textures. A glyph atlas can replace this later
 * without changing the FontRenderer mixin dispatch surface.
 */
public final class SkijaTextRenderer implements TextRenderBackend {

    private static final int DEFAULT_CACHE_SIZE = 512;
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
    private final Map<RenderKey, RenderedText> renderCache = Collections.synchronizedMap(
            new LinkedHashMap<RenderKey, RenderedText>(DEFAULT_CACHE_SIZE, 0.75F, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<RenderKey, RenderedText> eldest) {
                    if (size() <= maxRenderCacheEntries()) {
                        return false;
                    }
                    eldest.getValue().close();
                    return true;
                }
            });
    private final Map<MeasureKey, Float> measureCache = Collections.synchronizedMap(
            new LinkedHashMap<MeasureKey, Float>(DEFAULT_CACHE_SIZE, 0.75F, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<MeasureKey, Float> eldest) {
                    return size() > maxMeasureCacheEntries();
                }
            });
    private int nextTextureId = 0;
    private float autoRefBaseline = Float.NaN;

    public SkijaTextRenderer(TextureManager textureManager, IResourceManager resourceManager) throws IOException {
        this.textureManager = textureManager;
        this.resourceManager = resourceManager;
        this.fontProvider = new TypefaceFontProvider();
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

    public FontCollection getFontCollection() {
        return fontCollection;
    }

    public float measure(String text, boolean bold, boolean italic) {
        if (text == null || text.isEmpty()) {
            return 0.0F;
        }
        float scale = currentRasterScale();
        MeasureKey key = MeasureKey.plain(text, bold, italic, scale);
        Float cached = measureCache.get(key);
        if (cached != null) {
            return cached;
        }
        float measured;
        try (Paragraph paragraph = buildParagraph(text, 0xFFFFFFFF, bold, italic, scale)) {
            paragraph.layout(100000.0F * scale);
            measured = Math.max(paragraph.getMaxIntrinsicWidth(), paragraph.getLongestLine()) / scale;
        }
        measureCache.put(key, measured);
        trimMeasureCache();
        return measured;
    }

    public float measureFormatted(String text, int baseArgb, boolean shadow) {
        if (text == null || text.isEmpty()) {
            return 0.0F;
        }
        float scale = currentRasterScale();
        MeasureKey key = MeasureKey.formatted(text, shadow, scale);
        Float cached = measureCache.get(key);
        if (cached != null) {
            return cached;
        }
        float measured;
        try (Paragraph paragraph = buildFormattedParagraph(text, baseArgb, shadow, scale)) {
            paragraph.layout(100000.0F * scale);
            measured = Math.max(paragraph.getMaxIntrinsicWidth(), paragraph.getLongestLine()) / scale;
        }
        measureCache.put(key, measured);
        trimMeasureCache();
        return measured;
    }

    public RenderedText render(String text, int argb, boolean bold, boolean italic) {
        if (text == null || text.isEmpty()) {
            return RenderedText.EMPTY;
        }
        float scale = currentRasterScale();
        RenderKey key = RenderKey.plain(text, argb, bold, italic, scale);
        RenderedText cached = renderCache.get(key);
        if (cached != null) {
            cached.touch();
            return cached;
        }
        try {
            RenderedText rendered = rasterize(text, argb, bold, italic, scale, key.hashCode());
            renderCache.put(key, rendered);
            trimRenderCache();
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
        int normalizedArgb = normalizeBaseArgb(baseArgb);
        float scale = currentRasterScale();
        RenderKey key = RenderKey.formatted(text, normalizedArgb, shadow, scale);
        RenderedText cached = renderCache.get(key);
        if (cached != null) {
            cached.touch();
            return cached;
        }
        try {
            RenderedText rendered = rasterizeFormatted(text, normalizedArgb, shadow, scale, key.hashCode());
            renderCache.put(key, rendered);
            trimRenderCache();
            return rendered;
        } catch (Throwable t) {
            NeoFontRender.LOGGER.error("Failed to render formatted Skija text '{}'", text, t);
            return RenderedText.EMPTY;
        }
    }

    public void prewarmBasicLatin() {
        FontBrightnessEstimator.reset();
        int fontRes = Math.round(NeofontrenderConfig.fontSize() * NeofontrenderConfig.fontOversample());
        for (int cp = 32; cp <= 126; cp++) {
            String text = new String(Character.toChars(cp));
            measure(text, false, false);
            if (cp != ' ') {
                RenderedText rt = render(text, 0xFFFFFFFF, false, false);
                if (rt != null && rt.getTexture() != null && (cp == '1' || cp == '/' || cp == 'I')) {
                    int[] pixels = rt.getTexture().getTextureData();
                    FontBrightnessEstimator.feedSample(pixels, rt.width, rt.height, fontRes);
                }
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

    private static int computeBorder(float oversample) {
        int border = Math.max(4, (int) (oversample * 8.0f / 16.0f) * 2);
        border += border % 2;
        return Math.max(border, 4);
    }

    private RenderedText rasterize(String text, int argb, boolean bold, boolean italic, float oversample, int cacheHash) throws IOException {
        float measuredWidth = Math.max(1.0F, measure(text, bold, italic));
        int border = computeBorder(oversample);
        int width = Math.max(1, (int) Math.ceil((measuredWidth + border * 2.0F) * oversample));
        try (Paragraph paragraph = buildParagraph(text, argb, bold, italic, oversample)) {
            paragraph.layout(width);
            return rasterizeParagraph(paragraph, cacheHash, measuredWidth, width, oversample, border);
        }
    }

    private RenderedText rasterizeFormatted(String text, int argb, boolean shadow, float oversample, int cacheHash) throws IOException {
        float measuredWidth = Math.max(1.0F, measureFormatted(text, argb, shadow));
        int border = computeBorder(oversample);
        int width = Math.max(1, (int) Math.ceil((measuredWidth + border * 2.0F) * oversample));
        try (Paragraph paragraph = buildFormattedParagraph(text, argb, shadow, oversample)) {
            paragraph.layout(width);
            return rasterizeParagraph(paragraph, cacheHash, measuredWidth, width, oversample, border);
        }
    }

    private RenderedText rasterizeParagraph(Paragraph paragraph, int cacheHash, float measuredWidth, int width,
                                           float oversample, int border) throws IOException {
        int height;
        float verticalOffset;

        height = Math.max(1, (int) Math.ceil((paragraph.getHeight() + border * 2.0F) * oversample));
        float paintX = border * oversample;
        float paintY = border * oversample / 2.0f;
        float baselineInTexture = (paintY + paragraph.getAlphabeticBaseline()) / oversample;
        if (NeofontrenderConfig.fontAutoBaseline()) {
            float refBaseline = Float.isNaN(autoRefBaseline) ? computeAutoRefBaseline() : autoRefBaseline;
            verticalOffset = refBaseline + NeofontrenderConfig.fontBaselineShift() - baselineInTexture;
        } else {
            verticalOffset = NeofontrenderConfig.fontReferenceBaseline() + NeofontrenderConfig.fontBaselineShift() - baselineInTexture;
        }
        float horizontalOffset = -paintX / oversample;

        ImageInfo imageInfo = ImageInfo.makeN32Premul(width, height);
        try (Surface surface = Surface.makeRaster(imageInfo, imageInfo.getMinRowBytes(), skiaSurfaceProps())) {
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

            boolean premultiplied = NeofontrenderConfig.enablePremultipliedAlpha();
            if (!premultiplied) {
                // Convert Skia premultiplied output to straight alpha
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
            }
            if (NeofontrenderConfig.textureEdgeBleed()) {
                FontPixelUtils.normalizeTransparentRgb(pixels, width, height);
            }

            float textureScale = FontRenderTuning.textureScale(oversample);
            int uploadWidth = width;
            int uploadHeight = height;
            if (textureScale > 1.0F) {
                int scale = Math.round(textureScale);
                pixels = FontPixelUtils.scaleNearest(pixels, width, height, scale);
                uploadWidth = width * scale;
                uploadHeight = height * scale;
            }
            float actualRasterScale = oversample * textureScale;

            DynamicTexture texture = new DynamicTexture(uploadWidth, uploadHeight);
            int[] target = texture.getTextureData();
            System.arraycopy(pixels, 0, target, 0, Math.min(pixels.length, target.length));
            texture.updateDynamicTexture();
            FontRenderTuning.applyFontTextureFilter(texture, actualRasterScale);

            ResourceLocation location = new ResourceLocation("neofontrender",
            "skia/" + Integer.toHexString(cacheHash) + "_" + nextTextureId++);
            textureManager.loadTexture(location, texture);
            FontRenderTuning.applyFontTextureFilter(texture, actualRasterScale);
            return new RenderedText(location, texture, measuredWidth,
                    uploadWidth, uploadHeight,
                    uploadWidth / actualRasterScale, uploadHeight / actualRasterScale,
                    actualRasterScale, horizontalOffset, verticalOffset);
        }
    }

    private float computeAutoRefBaseline() {
        try (Paragraph para = buildParagraph("ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789", 0xFFFFFFFF, false, false, 1.0f)) {
            para.layout(10000);
            float alphabeticBaseline = para.getAlphabeticBaseline();
            float mcBaseline = 7.0f;
            autoRefBaseline = mcBaseline + (alphabeticBaseline - mcBaseline) / 2.0f;
            NeoFontRender.LOGGER.debug("Auto reference baseline computed: {}", autoRefBaseline);
            return autoRefBaseline;
        }
    }

    private float currentRasterScale() {
        return FontRenderTuning.rasterScale(NeofontrenderConfig.fontOversample());
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
        try (TextStyle textStyle = makeTextStyle(argb, bold, italic, scale);
             Paint foreground = makeForegroundPaint(argb)) {
            textStyle.setForeground(foreground);
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

    private static Paint makeForegroundPaint(int argb) {
        return new Paint()
                .setColor(opaqueRgb(argb))
                .setAntiAlias(!"off".equals(NeofontrenderConfig.fontAntialiasMode()));
    }

    private static SurfaceProps skiaSurfaceProps() {
        PixelGeometry geometry = skiaPixelGeometry(NeofontrenderConfig.fontAntialiasMode());
        if (geometry == PixelGeometry.UNKNOWN && NeofontrenderConfig.fontLcdSubpixel()) {
            geometry = PixelGeometry.RGB_H;
        }
        return new SurfaceProps(false, geometry);
    }

    private static PixelGeometry skiaPixelGeometry(String mode) {
        if (mode == null) {
            return PixelGeometry.UNKNOWN;
        }
        switch (mode) {
            case "lcd_hrgb":
                return PixelGeometry.RGB_H;
            case "lcd_hbgr":
                return PixelGeometry.BGR_H;
            case "lcd_vrgb":
                return PixelGeometry.RGB_V;
            case "lcd_vbgr":
                return PixelGeometry.BGR_V;
            default:
                return PixelGeometry.UNKNOWN;
        }
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

    private void trimRenderCache() {
        synchronized (renderCache) {
            int max = maxRenderCacheEntries();
            Iterator<Map.Entry<RenderKey, RenderedText>> iterator = renderCache.entrySet().iterator();
            while (renderCache.size() > max && iterator.hasNext()) {
                Map.Entry<RenderKey, RenderedText> eldest = iterator.next();
                eldest.getValue().close();
                iterator.remove();
            }

            long ttlMillis = renderCacheTtlMillis();
            if (ttlMillis <= 0L) {
                return;
            }
            int min = minRenderCacheEntries(max);
            long now = System.currentTimeMillis();
            iterator = renderCache.entrySet().iterator();
            while (renderCache.size() > min && iterator.hasNext()) {
                Map.Entry<RenderKey, RenderedText> eldest = iterator.next();
                if (!eldest.getValue().isExpired(now, ttlMillis)) {
                    break;
                }
                eldest.getValue().close();
                iterator.remove();
            }
        }
    }

    private void trimMeasureCache() {
        synchronized (measureCache) {
            int max = maxMeasureCacheEntries();
            Iterator<MeasureKey> iterator = measureCache.keySet().iterator();
            while (measureCache.size() > max && iterator.hasNext()) {
                iterator.next();
                iterator.remove();
            }
        }
    }

    private static int maxRenderCacheEntries() {
        return Math.max(1, NeofontrenderConfig.skiaTextCacheMaxEntries());
    }

    private static int minRenderCacheEntries(int max) {
        return Math.max(0, Math.min(max, NeofontrenderConfig.skiaTextCacheMinEntries()));
    }

    private static int maxMeasureCacheEntries() {
        return Math.max(1, NeofontrenderConfig.skiaMeasureCacheMaxEntries());
    }

    private static long renderCacheTtlMillis() {
        return (long) (NeofontrenderConfig.skiaTextCacheTtlSeconds() * 1000.0F);
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

    public static final class RenderedText implements TextRenderResult, AutoCloseable {
        private static final RenderedText EMPTY = new RenderedText(null, null, 0.0F, 0, 0, 0.0F, 0.0F, 1.0F, 0.0F, 0.0F);
        private static final java.util.Map<ResourceLocation, Float> LAST_FILTER_SCALE = new java.util.HashMap<>();

        private final ResourceLocation location;
        private final DynamicTexture texture;
        private final float advance;
        private final int width;
        private final int height;
        private final float drawWidth;
        private final float drawHeight;
        private final float rasterScale;
        private final float horizontalOffset;
        private final float verticalOffset;
        private volatile long lastAccessMillis;

        private RenderedText(ResourceLocation location, DynamicTexture texture, float advance,
                             int width, int height, float drawWidth, float drawHeight,
                             float rasterScale, float horizontalOffset, float verticalOffset) {
            this.location = location;
            this.texture = texture;
            this.advance = advance;
            this.width = width;
            this.height = height;
            this.drawWidth = drawWidth;
            this.drawHeight = drawHeight;
            this.rasterScale = rasterScale;
            this.horizontalOffset = horizontalOffset;
            this.verticalOffset = verticalOffset;
            this.lastAccessMillis = System.currentTimeMillis();
        }

        DynamicTexture getTexture() {
            return texture;
        }

        public float advance() {
            return advance;
        }

        public void draw(float x, float y, float alpha) {
            if (location == null || texture == null || width <= 0 || height <= 0) {
                return;
            }
            touch();
            net.minecraft.client.Minecraft.getMinecraft().getTextureManager().bindTexture(location);
            GlStateManager.enableTexture2D();
            GlStateManager.enableAlpha();
            GlStateManager.color(1.0F, 1.0F, 1.0F, alpha);

            // CRITICAL: Some MC code paths (e.g. RenderItem.renderItemOverlayIntoGUI)
            // disable GL_BLEND before calling drawStringWithShadow. Alpha-blended text
            // textures MUST be drawn with blending enabled, otherwise semi-transparent
            // edge pixels write their raw RGB directly to the framebuffer causing dark
            // fringes / jagged edges. See rendering.forceBlendForText config comment.
            if (NeofontrenderConfig.forceBlendForText() && !GL11.glIsEnabled(GL11.GL_BLEND)) {
                GlStateManager.enableBlend();
                if (NeofontrenderConfig.enablePremultipliedAlpha()) {
                    GlStateManager.tryBlendFuncSeparate(GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);
                } else {
                    GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);
                }
            }

            Float lastScale = LAST_FILTER_SCALE.get(location);
            if (lastScale == null || Math.abs(lastScale - rasterScale) > 0.01f) {
                FontRenderTuning.applyBoundTextureFilter(rasterScale);
                LAST_FILTER_SCALE.put(location, rasterScale);
            }
            float left = FontRenderTuning.alignToPixel(x + horizontalOffset);
            float top = FontRenderTuning.alignToPixel(y + verticalOffset);

            try (FontRenderPipeline.State ignored = FontRenderPipeline.begin(rasterScale)) {
                Tessellator tessellator = Tessellator.getInstance();
                BufferBuilder buffer = tessellator.getBuffer();
                buffer.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);
                buffer.pos(left, top, 0.0D).tex(0.0D, 0.0D).color(1.0F, 1.0F, 1.0F, alpha).endVertex();
                buffer.pos(left, top + drawHeight, 0.0D).tex(0.0D, 1.0D).color(1.0F, 1.0F, 1.0F, alpha).endVertex();
                buffer.pos(left + drawWidth, top + drawHeight, 0.0D).tex(1.0D, 1.0D).color(1.0F, 1.0F, 1.0F, alpha).endVertex();
                buffer.pos(left + drawWidth, top, 0.0D).tex(1.0D, 0.0D).color(1.0F, 1.0F, 1.0F, alpha).endVertex();
                tessellator.draw();
            }
        }

        @Override
        public void close() {
            if (texture != null) {
                texture.deleteGlTexture();
            }
        }

        private void touch() {
            lastAccessMillis = System.currentTimeMillis();
        }

        private boolean isExpired(long nowMillis, long ttlMillis) {
            return nowMillis - lastAccessMillis >= ttlMillis;
        }
    }

    private static final class RenderKey {
        private final String text;
        private final int argb;
        private final boolean formatted;
        private final boolean primaryStyle;
        private final boolean secondaryStyle;
        private final int scaleBucket;

        private RenderKey(String text, int argb, boolean formatted, boolean primaryStyle, boolean secondaryStyle,
                          float scale) {
            this.text = text;
            this.argb = argb;
            this.formatted = formatted;
            this.primaryStyle = primaryStyle;
            this.secondaryStyle = secondaryStyle;
            this.scaleBucket = scaleBucket(scale);
        }

        private static RenderKey plain(String text, int argb, boolean bold, boolean italic, float scale) {
            return new RenderKey(text, argb, false, bold, italic, scale);
        }

        private static RenderKey formatted(String text, int argb, boolean shadow, float scale) {
            return new RenderKey(text, argb, true, shadow, false, scale);
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
            return argb == other.argb
                    && formatted == other.formatted
                    && primaryStyle == other.primaryStyle
                    && secondaryStyle == other.secondaryStyle
                    && scaleBucket == other.scaleBucket
                    && text.equals(other.text);
        }

        @Override
        public int hashCode() {
            int result = text.hashCode();
            result = 31 * result + argb;
            result = 31 * result + (formatted ? 1 : 0);
            result = 31 * result + (primaryStyle ? 1 : 0);
            result = 31 * result + (secondaryStyle ? 1 : 0);
            result = 31 * result + scaleBucket;
            return result;
        }
    }

    private static final class MeasureKey {
        private final String text;
        private final boolean formatted;
        private final boolean primaryStyle;
        private final boolean secondaryStyle;
        private final int scaleBucket;

        private MeasureKey(String text, boolean formatted, boolean primaryStyle, boolean secondaryStyle, float scale) {
            this.text = text;
            this.formatted = formatted;
            this.primaryStyle = primaryStyle;
            this.secondaryStyle = secondaryStyle;
            this.scaleBucket = scaleBucket(scale);
        }

        private static MeasureKey plain(String text, boolean bold, boolean italic, float scale) {
            return new MeasureKey(text, false, bold, italic, scale);
        }

        private static MeasureKey formatted(String text, boolean shadow, float scale) {
            return new MeasureKey(text, true, shadow, false, scale);
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
            return formatted == other.formatted
                    && primaryStyle == other.primaryStyle
                    && secondaryStyle == other.secondaryStyle
                    && scaleBucket == other.scaleBucket
                    && text.equals(other.text);
        }

        @Override
        public int hashCode() {
            int result = text.hashCode();
            result = 31 * result + (formatted ? 1 : 0);
            result = 31 * result + (primaryStyle ? 1 : 0);
            result = 31 * result + (secondaryStyle ? 1 : 0);
            result = 31 * result + scaleBucket;
            return result;
        }
    }

    private static int scaleBucket(float scale) {
        return Math.round(scale * 100.0F);
    }

}
