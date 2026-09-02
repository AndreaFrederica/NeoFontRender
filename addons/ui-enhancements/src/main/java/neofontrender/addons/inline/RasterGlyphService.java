package neofontrender.addons.inline;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.util.ResourceLocation;
import neofontrender.api.text.pipeline.InlineContent;
import neofontrender.api.text.pipeline.TextPipelineEngine;
import neofontrender.addons.ui.NfrUiEnhancements;

import java.awt.image.BufferedImage;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Shared CPU-raster and client-thread texture cache for formula and vector providers. */
enum RasterGlyphService {
    INSTANCE;

    private final ExecutorService workers = Executors.newFixedThreadPool(2, runnable -> {
        Thread thread = new Thread(runnable, "NFR Embedded Content Rasterizer");
        thread.setDaemon(true);
        return thread;
    });
    private final Map<String, Handle> handles = new ConcurrentHashMap<>();

    InlineContent glyph(String key, String description, int displayHeight, boolean tint,
                        boolean matchFontLineHeight, RasterJob job) {
        Handle existing = handles.get(key);
        int maxEntries = EmbeddedContentConfig.rasterCacheEntries();
        if (existing == null && handles.size() >= maxEntries) {
            evictOldTextures(maxEntries - 1, pixelBudget());
            existing = handles.get(key);
            if (existing == null && handles.size() >= maxEntries) return null;
        }
        Handle handle = handles.computeIfAbsent(key, ignored -> {
            Handle created = new Handle();
            workers.execute(() -> rasterize(created, key, job));
            return created;
        });
        handle.lastAccess = System.nanoTime();
        if (handle.state == State.FAILED) return null;
        return new RasterInlineContent(handle, description, displayHeight, tint, matchFontLineHeight);
    }

    void trimToConfiguredBudget() {
        evictOldTextures(EmbeddedContentConfig.rasterCacheEntries(), pixelBudget());
    }

    private void rasterize(Handle handle, String key, RasterJob job) {
        try {
            BufferedImage image = job.render();
            if (image == null || image.getWidth() <= 0 || image.getHeight() <= 0) {
                throw new IllegalArgumentException("Rasterizer returned an empty image");
            }
            Minecraft.getMinecraft().addScheduledTask(() -> upload(handle, key, image));
        } catch (Throwable failure) {
            handle.state = State.FAILED;
            TextPipelineEngine.invalidate();
            NfrUiEnhancements.LOGGER.debug("Embedded content could not be rasterized: {}", key, failure);
        }
    }

    private void upload(Handle handle, String key, BufferedImage image) {
        try {
            DynamicTexture texture = new DynamicTexture(image);
            texture.setBlurMipmap(true, false);
            ResourceLocation location = Minecraft.getMinecraft().getTextureManager()
                    .getDynamicTextureLocation("nfr_embedded_content", texture);
            handle.width = image.getWidth();
            handle.height = image.getHeight();
            handle.texture = texture;
            handle.location = location;
            handle.state = State.READY;
            TextPipelineEngine.invalidate();
            evictOldTextures(EmbeddedContentConfig.rasterCacheEntries(), pixelBudget());
        } catch (Throwable failure) {
            handle.state = State.FAILED;
            TextPipelineEngine.invalidate();
            NfrUiEnhancements.LOGGER.debug("Embedded content texture upload failed: {}", key, failure);
        }
    }

    private float pixelBudget() {
        return EmbeddedContentConfig.rasterCacheMegapixels() * 1024.0F * 1024.0F;
    }

    /** Evict least-recently-used ready entries until both budgets are satisfied. */
    private void evictOldTextures(int maximumEntries, float maximumPixels) {
        while (handles.size() > maximumEntries || readyPixels() > maximumPixels) {
            Map.Entry<String, Handle> oldest = null;
            for (Map.Entry<String, Handle> entry : handles.entrySet()) {
                if (entry.getValue().state == State.LOADING) continue;
                if (oldest == null || entry.getValue().lastAccess < oldest.getValue().lastAccess) {
                    oldest = entry;
                }
            }
            if (oldest == null || !handles.remove(oldest.getKey(), oldest.getValue())) return;
            Handle handle = oldest.getValue();
            if (handle.location != null) {
                Minecraft.getMinecraft().getTextureManager().deleteTexture(handle.location);
            }
            handle.location = null;
            handle.texture = null;
            handle.state = State.FAILED;
            TextPipelineEngine.invalidate();
        }
    }

    private long readyPixels() {
        long pixels = 0L;
        for (Handle handle : handles.values()) {
            if (handle.state == State.READY) pixels += (long) handle.width * handle.height;
        }
        return pixels;
    }

    @FunctionalInterface
    interface RasterJob { BufferedImage render() throws Exception; }

    enum State { LOADING, READY, FAILED }

    static final class Handle {
        volatile State state = State.LOADING;
        volatile int width;
        volatile int height;
        volatile DynamicTexture texture;
        volatile ResourceLocation location;
        volatile long lastAccess = System.nanoTime();
    }
}
