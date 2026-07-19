package neofontrender.core.font.layout;

import neofontrender.core.font.backend.TextRenderResult;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Backend-neutral cache for measured and draw-ready text layouts.
 *
 * <p>This is intentionally small: future backends can reuse the same request
 * keys while deciding how much of the layout pipeline they own.</p>
 */
public final class TextLayoutCache {

    private final Map<TextRenderRequest, CachedTextLayout> layoutCache;
    private final Map<TextMeasureRequest, Float> measureCache;

    public TextLayoutCache(final int maxEntries) {
        this.layoutCache = Collections.synchronizedMap(
                new LinkedHashMap<TextRenderRequest, CachedTextLayout>(maxEntries, 0.75F, true) {
                    @Override
                    protected boolean removeEldestEntry(Map.Entry<TextRenderRequest, CachedTextLayout> eldest) {
                        if (size() <= maxEntries) {
                            return false;
                        }
                        closeQuietly(eldest.getValue().rendered());
                        return true;
                    }
                });
        this.measureCache = Collections.synchronizedMap(
                new LinkedHashMap<TextMeasureRequest, Float>(maxEntries, 0.75F, true) {
                    @Override
                    protected boolean removeEldestEntry(Map.Entry<TextMeasureRequest, Float> eldest) {
                        return size() > maxEntries;
                    }
                });
    }

    public CachedTextLayout getLayout(TextRenderRequest request,
                                      Supplier<Float> measureLoader,
                                      Function<Float, ? extends TextRenderResult> renderLoader) {
        CachedTextLayout cached = layoutCache.get(request);
        if (cached != null) {
            return cached;
        }
        float advance = measureLoader.get();
        CachedTextLayout layout = new CachedTextLayout(request, advance, renderLoader.apply(advance));
        layoutCache.put(request, layout);
        return layout;
    }

    public float getMeasured(TextMeasureRequest request, Supplier<Float> loader) {
        Float cached = measureCache.get(request);
        if (cached != null) {
            return cached;
        }
        float measured = loader.get();
        measureCache.put(request, measured);
        return measured;
    }

    public void clear() {
        for (CachedTextLayout layout : layoutCache.values()) {
            closeQuietly(layout.rendered());
        }
        layoutCache.clear();
        measureCache.clear();
    }

    private static void closeQuietly(Object value) {
        if (value instanceof AutoCloseable) {
            try {
                ((AutoCloseable) value).close();
            } catch (Exception ignored) {
            }
        }
    }
}