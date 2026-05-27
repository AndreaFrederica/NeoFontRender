package neofontrender.core.font;

import neofontrender.core.font.providers.AwtTtfGlyphProvider;

import javax.annotation.Nullable;
import java.util.*;

/**
 * A collection of {@link GlyphProvider}s that together form a single font.
 * Caches {@link GlyphInfo} and {@link BakedGlyph} on demand.
 *
 * <p>Equivalent to 1.20.1 {@code net.minecraft.client.gui.font.FontSet}.</p>
 */
public class FontSet implements AutoCloseable {

    private static final Random RANDOM = new Random();
    private static final String OBFUSCATED_CANDIDATES =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

    private final List<GlyphProvider> providers;
    private final FontTexture atlas;
    private final AwtTtfGlyphProvider layoutProvider;
    private final Map<Integer, GlyphInfo> glyphInfos = new HashMap<>();
    private final Map<Integer, BakedGlyph> bakedGlyphs = new HashMap<>();
    private final Map<Integer, List<Integer>> glyphsByWidth = new HashMap<>();

    public FontSet(List<GlyphProvider> providers, FontTexture atlas) {
        this.providers = providers;
        this.atlas = atlas;
        AwtTtfGlyphProvider awtProvider = null;
        for (GlyphProvider provider : providers) {
            if (provider instanceof AwtTtfGlyphProvider) {
                awtProvider = (AwtTtfGlyphProvider) provider;
                break;
            }
        }
        this.layoutProvider = awtProvider;

        // Build width buckets for obfuscated-text support.
        // We scan all supported glyphs and record their advance so that
        // getRandomGlyph can pick a visually-similar replacement.
        Set<Integer> seen = new HashSet<>();
        for (GlyphProvider provider : providers) {
            for (int cp : provider.getSupportedGlyphs()) {
                if (seen.add(cp) && cp != ' ') {
                    addGlyphWidthBucket(cp, provider.getGlyph(cp));
                }
            }
        }
        for (int i = 0; i < OBFUSCATED_CANDIDATES.length(); ) {
            int cp = OBFUSCATED_CANDIDATES.codePointAt(i);
            if (seen.add(cp)) {
                addGlyphWidthBucket(cp, getGlyphInfo(cp));
            }
            i += Character.charCount(cp);
        }
    }

    public float[] layoutPositions(String text, boolean bold) {
        if (layoutProvider != null && layoutProvider.canDisplayText(text)) {
            return layoutProvider.layoutPositions(text, bold);
        }
        float[] positions = new float[text.length() + 1];
        float current = 0.0F;
        for (int i = 0; i < text.length(); ) {
            int cp = text.codePointAt(i);
            GlyphInfo info = getGlyphInfo(cp);
            current += info == null ? 0.0F : info.getAdvance(bold);
            int next = i + Character.charCount(cp);
            for (int pos = i + 1; pos <= next && pos < positions.length; pos++) {
                positions[pos] = current;
            }
            i = next;
        }
        return positions;
    }

    private void addGlyphWidthBucket(int codePoint, @Nullable GlyphInfo info) {
        if (info == null || codePoint == ' ') {
            return;
        }
        int w = (int) Math.ceil(info.getAdvance(false));
        glyphsByWidth.computeIfAbsent(w, k -> new ArrayList<>()).add(codePoint);
    }

    /**
     * Return the {@link GlyphInfo} for a code point, querying providers in order.
     */
    public GlyphInfo getGlyphInfo(int codePoint) {
        return glyphInfos.computeIfAbsent(codePoint, this::findGlyphInfo);
    }

    private GlyphInfo findGlyphInfo(int codePoint) {
        for (GlyphProvider provider : providers) {
            GlyphInfo info = provider.getGlyph(codePoint);
            if (info != null) {
                return info;
            }
        }
        // Should not happen if MissingGlyphProvider is always last
        return null;
    }

    /**
     * Return the baked glyph for a code point, rasterizing on first use.
     */
    @Nullable
    public BakedGlyph getGlyph(int codePoint) {
        return bakedGlyphs.computeIfAbsent(codePoint, this::bakeGlyph);
    }

    @Nullable
    private BakedGlyph bakeGlyph(int codePoint) {
        GlyphInfo info = getGlyphInfo(codePoint);
        if (info == null) {
            return null;
        }
        return info.bake(atlas);
    }

    /**
     * Return a random glyph whose advance is close to the given width,
     * used by obfuscated text rendering ({@code §k}).
     */
    @Nullable
    public BakedGlyph getRandomGlyph(float advance) {
        int w = (int) Math.ceil(advance);
        List<Integer> bucket = glyphsByWidth.get(w);
        if (bucket == null || bucket.isEmpty()) {
            bucket = findNearestGlyphBucket(w);
        }
        if (bucket == null || bucket.isEmpty()) {
            return null;
        }
        int cp = bucket.get(RANDOM.nextInt(bucket.size()));
        return getGlyph(cp);
    }

    @Nullable
    private List<Integer> findNearestGlyphBucket(int width) {
        List<Integer> nearest = null;
        int nearestDistance = Integer.MAX_VALUE;
        for (Map.Entry<Integer, List<Integer>> entry : glyphsByWidth.entrySet()) {
            List<Integer> bucket = entry.getValue();
            if (bucket.isEmpty()) {
                continue;
            }
            int distance = Math.abs(entry.getKey() - width);
            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearest = bucket;
            }
        }
        return nearestDistance <= 2 ? nearest : null;
    }

    @Override
    public void close() {
        for (GlyphProvider provider : providers) {
            provider.close();
        }
        atlas.close();
        glyphInfos.clear();
        bakedGlyphs.clear();
        glyphsByWidth.clear();
    }
}
