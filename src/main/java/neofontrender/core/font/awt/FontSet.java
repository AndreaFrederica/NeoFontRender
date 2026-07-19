package neofontrender.core.font.awt;

import neofontrender.core.font.awt.providers.AwtTtfGlyphProvider;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

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
    private long glyphInfoHits;
    private long glyphInfoMisses;
    private long bakedGlyphHits;
    private long bakedGlyphMisses;

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

    public GlyphInfo getGlyphInfo(int codePoint) {
        GlyphInfo cached = glyphInfos.get(codePoint);
        if (cached != null || glyphInfos.containsKey(codePoint)) {
            glyphInfoHits++;
            return cached;
        }
        glyphInfoMisses++;
        GlyphInfo loaded = findGlyphInfo(codePoint);
        glyphInfos.put(codePoint, loaded);
        return loaded;
    }

    private GlyphInfo findGlyphInfo(int codePoint) {
        for (GlyphProvider provider : providers) {
            GlyphInfo info = provider.getGlyph(codePoint);
            if (info != null) {
                return info;
            }
        }
        return null;
    }

    @Nullable
    public BakedGlyph getGlyph(int codePoint) {
        BakedGlyph cached = bakedGlyphs.get(codePoint);
        if (cached != null || bakedGlyphs.containsKey(codePoint)) {
            bakedGlyphHits++;
            return cached;
        }
        bakedGlyphMisses++;
        BakedGlyph loaded = bakeGlyph(codePoint);
        bakedGlyphs.put(codePoint, loaded);
        return loaded;
    }

    public void prewarmBasicLatin() {
        for (int cp = 32; cp <= 126; cp++) {
            prewarmGlyph(cp);
        }
        for (int cp = 160; cp <= 255; cp++) {
            prewarmGlyph(cp);
        }
    }

    private void prewarmGlyph(int codePoint) {
        getGlyphInfo(codePoint);
        if (codePoint != ' ' && codePoint != 160) {
            getGlyph(codePoint);
        }
    }

    @Nullable
    private BakedGlyph bakeGlyph(int codePoint) {
        GlyphInfo info = getGlyphInfo(codePoint);
        if (info == null) {
            return null;
        }
        return info.bake(atlas);
    }

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

    public DebugState debugState() {
        int layoutSize = layoutProvider == null ? 0 : layoutProvider.layoutCacheSize();
        int layoutMax = layoutProvider == null ? 0 : layoutProvider.layoutCacheMax();
        long layoutHits = layoutProvider == null ? 0L : layoutProvider.layoutCacheHits();
        long layoutMisses = layoutProvider == null ? 0L : layoutProvider.layoutCacheMisses();
        long layoutEvictions = layoutProvider == null ? 0L : layoutProvider.layoutCacheEvictions();
        return new DebugState(
                glyphInfos.size(), bakedGlyphs.size(), glyphsByWidth.size(),
                glyphInfoHits, glyphInfoMisses, bakedGlyphHits, bakedGlyphMisses,
                layoutSize, layoutMax, layoutHits, layoutMisses, layoutEvictions);
    }

    public static final class DebugState {
        private final int glyphInfoCacheSize;
        private final int bakedGlyphCacheSize;
        private final int glyphWidthBuckets;
        private final long glyphInfoHits;
        private final long glyphInfoMisses;
        private final long bakedGlyphHits;
        private final long bakedGlyphMisses;
        private final int layoutCacheSize;
        private final int layoutCacheMax;
        private final long layoutCacheHits;
        private final long layoutCacheMisses;
        private final long layoutCacheEvictions;

        private DebugState(int glyphInfoCacheSize, int bakedGlyphCacheSize, int glyphWidthBuckets,
                           long glyphInfoHits, long glyphInfoMisses,
                           long bakedGlyphHits, long bakedGlyphMisses,
                           int layoutCacheSize, int layoutCacheMax,
                           long layoutCacheHits, long layoutCacheMisses, long layoutCacheEvictions) {
            this.glyphInfoCacheSize = glyphInfoCacheSize;
            this.bakedGlyphCacheSize = bakedGlyphCacheSize;
            this.glyphWidthBuckets = glyphWidthBuckets;
            this.glyphInfoHits = glyphInfoHits;
            this.glyphInfoMisses = glyphInfoMisses;
            this.bakedGlyphHits = bakedGlyphHits;
            this.bakedGlyphMisses = bakedGlyphMisses;
            this.layoutCacheSize = layoutCacheSize;
            this.layoutCacheMax = layoutCacheMax;
            this.layoutCacheHits = layoutCacheHits;
            this.layoutCacheMisses = layoutCacheMisses;
            this.layoutCacheEvictions = layoutCacheEvictions;
        }

        public int glyphInfoCacheSize() {
            return glyphInfoCacheSize;
        }

        public int bakedGlyphCacheSize() {
            return bakedGlyphCacheSize;
        }

        public int glyphWidthBuckets() {
            return glyphWidthBuckets;
        }

        public long glyphInfoHits() {
            return glyphInfoHits;
        }

        public long glyphInfoMisses() {
            return glyphInfoMisses;
        }

        public long bakedGlyphHits() {
            return bakedGlyphHits;
        }

        public long bakedGlyphMisses() {
            return bakedGlyphMisses;
        }

        public int layoutCacheSize() {
            return layoutCacheSize;
        }

        public int layoutCacheMax() {
            return layoutCacheMax;
        }

        public long layoutCacheHits() {
            return layoutCacheHits;
        }

        public long layoutCacheMisses() {
            return layoutCacheMisses;
        }

        public long layoutCacheEvictions() {
            return layoutCacheEvictions;
        }
    }
}
