package neofontrender.client;

import icyllis.arc3d.core.Color;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import neofontrender.api.arc3d.Arc3DApi;
import neofontrender.core.config.NeofontrenderConfig;
import neofontrender.core.font.FontManager;
import neofontrender.core.font.cosmic.CosmicTextRenderer;
import org.lwjgl.opengl.GL11;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import java.awt.Rectangle;

/** Optional Java-GC-style time-series view of the text cache generations. */
public final class CosmicCacheOverlayRenderer {
    private static final int PANEL_WIDTH = 540;
    private static final int PANEL_HEIGHT = 230;
    private static final int HISTORY = 120;
    // One color per graph row: text, measure, character texture, character width, async, adaptive.
    // Derive the history dimensions from this list so adding a row cannot outgrow its palette.
    private static final int[] GENERATIONAL_COLORS = {
            0xFF43C7FF, 0xFF58D5FF, 0xFFB36BFF, 0xFFD084FF, 0xFF59D6A0, 0xFFFFB454
    };
    private static final int SINGLE_COLOR = 0xFF718096;
    private static final int ADAPTIVE_MAX_ENTRIES = 256;
    private static final int ROWS = GENERATIONAL_COLORS.length;
    private static final int[] youngHistory = new int[ROWS * HISTORY];
    private static final int[] oldHistory = new int[ROWS * HISTORY];
    private static int historyCount;
    private static int historyCursor;
    private static boolean historyMode;
    private static boolean historyInitialized;
    private static volatile boolean compositorSurfaceRegistered;
    private static boolean dragging;
    private static boolean positionDirty;
    private static int dragOffsetX;
    private static int dragOffsetY;

    public static boolean isPinned() {
        return NeofontrenderConfig.cacheOverlayPinned();
    }

    /** The pinned HUD must not steal ordinary gameplay clicks; Alt is the explicit edit modifier. */
    public static boolean editModeActive() {
        return Keyboard.isKeyDown(Keyboard.KEY_LMENU) || Keyboard.isKeyDown(Keyboard.KEY_RMENU);
    }

    /** Called by the optional UI enhancements HUD compositor bridge. */
    public static void markCompositorSurfaceRegistered() {
        compositorSurfaceRegistered = true;
    }

    public static boolean needsDirectPinnedRender() {
        return isPinned() && !compositorSurfaceRegistered;
    }

    private static int panelX(int width) {
        int saved = NeofontrenderConfig.cacheOverlayX();
        return saved <= 0 ? Math.max(4, width / 2 - PANEL_WIDTH / 2) : clampX(saved, width);
    }

    private static int panelY(int height) {
        int saved = NeofontrenderConfig.cacheOverlayY();
        return saved <= 0 ? Math.max(4, height - PANEL_HEIGHT - 8) : clampY(saved, height);
    }

    public static Rectangle panelBounds(int width, int height) {
        return new Rectangle(panelX(width), panelY(height), PANEL_WIDTH, PANEL_HEIGHT);
    }

    @SubscribeEvent
    public void onOverlay(RenderGameOverlayEvent.Post event) {
        if (event.getType() != RenderGameOverlayEvent.ElementType.ALL
                || !NeofontrenderConfig.debugCacheOverlay()) return;
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft == null || minecraft.gameSettings == null) return;
        if (!minecraft.gameSettings.showDebugInfo && !isPinned()) return;
        CosmicTextRenderer renderer = FontManager.INSTANCE.getCosmicTextRenderer();
        if (renderer == null || !renderer.isReady()) return;
        net.minecraft.client.gui.ScaledResolution resolution = event.getResolution();
        boolean direct = !isPinned() ? minecraft.gameSettings.showDebugInfo : !compositorSurfaceRegistered;
        if (direct) {
            draw(minecraft, renderer.cacheVisualState(), resolution.getScaledWidth(), resolution.getScaledHeight(),
                    isPinned() ? panelX(resolution.getScaledWidth()) : -1,
                    isPinned() ? panelY(resolution.getScaledHeight()) : -1);
        }
    }

    /** Handles pointer input from either the Forge event bus or the optional HUD compositor. */
    @SubscribeEvent
    public void onMouse(MouseEvent event) {
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft == null || !NeofontrenderConfig.debugCacheOverlay()) return;
        if (!editModeActive()) {
            finishDrag();
            return;
        }
        net.minecraft.client.gui.ScaledResolution resolution = new net.minecraft.client.gui.ScaledResolution(minecraft);
        int x = event.getX() * resolution.getScaledWidth() / Math.max(1, minecraft.displayWidth);
        int y = resolution.getScaledHeight()
                - event.getY() * resolution.getScaledHeight() / Math.max(1, minecraft.displayHeight) - 1;
        if (handlePointer(x, y, event.getButton(), event.isButtonstate())) event.setCanceled(true);
    }

    /**
     * Receives scaled-screen coordinates from the UI Enhancements HUD compositor. The old
     * compositor surface was marked non-interactive, so pinned panels could render but never
     * reach this drag state machine.
     */
    public static boolean handlePointer(int x, int y, int button, boolean pressed) {
        if (!NeofontrenderConfig.debugCacheOverlay()) return false;
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft == null) return false;
        if (!editModeActive()) {
            finishDrag();
            return false;
        }
        net.minecraft.client.gui.ScaledResolution resolution = new net.minecraft.client.gui.ScaledResolution(minecraft);
        Rectangle bounds = new Rectangle(panelX(resolution.getScaledWidth()), panelY(resolution.getScaledHeight()),
                PANEL_WIDTH, PANEL_HEIGHT);
        if (button == 0 && pressed) {
            if (!bounds.contains(x, y) || y > bounds.y + 22) return false;
            if (y <= bounds.y + 22 && x >= bounds.x + PANEL_WIDTH - 55) {
                NeofontrenderConfig.setCacheOverlayPinned(!isPinned());
                NeofontrenderConfig.save();
                dragging = false;
            } else {
                dragging = true;
                dragOffsetX = x - bounds.x;
                dragOffsetY = y - bounds.y;
            }
            return true;
        }
        if (dragging && button < 0) {
            if (Mouse.isButtonDown(0)) {
                int nextX = clampX(x - dragOffsetX, resolution.getScaledWidth());
                int nextY = clampY(y - dragOffsetY, resolution.getScaledHeight());
                NeofontrenderConfig.setCacheOverlayPosition(nextX, nextY);
                positionDirty = true;
                return true;
            }
            finishDrag();
            return true;
        }
        if (button == 0 && !pressed) {
            boolean wasDragging = dragging;
            finishDrag();
            return wasDragging || bounds.contains(x, y);
        }
        return dragging;
    }

    private static void finishDrag() {
        if (!dragging && !positionDirty) return;
        dragging = false;
        if (positionDirty) {
            NeofontrenderConfig.save();
            positionDirty = false;
        }
    }

    public static void renderPinned(float partialTicks) {
        if (!isPinned()) return;
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft == null || minecraft.fontRenderer == null) return;
        net.minecraft.client.gui.ScaledResolution resolution = new net.minecraft.client.gui.ScaledResolution(minecraft);
        CosmicTextRenderer renderer = FontManager.INSTANCE.getCosmicTextRenderer();
        if (renderer == null || !renderer.isReady()) return;
        draw(minecraft, renderer.cacheVisualState(), resolution.getScaledWidth(), resolution.getScaledHeight(),
                panelX(resolution.getScaledWidth()), panelY(resolution.getScaledHeight()));
    }

    private static void draw(Minecraft minecraft, CosmicTextRenderer.CacheVisualState state,
                             int screenWidth, int screenHeight, int requestedX, int requestedY) {
        sample(state);
        int x = requestedX >= 0 ? clampX(requestedX, screenWidth) : Math.max(4, screenWidth / 2 - PANEL_WIDTH / 2);
        int y = requestedY >= 0 ? clampY(requestedY, screenHeight) : Math.max(4, screenHeight - PANEL_HEIGHT - 8);
        float opacity = NeofontrenderConfig.cacheOverlayOpacity();
        int panel = withOpacity(0xFF1A202B, opacity);
        int border = withOpacity(0xFF4C5B70, Math.min(1.0F, opacity + 0.12F));
        boolean texture = GL11.glIsEnabled(GL11.GL_TEXTURE_2D);
        boolean blend = GL11.glIsEnabled(GL11.GL_BLEND);
        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA,
                GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        quad(x, y, x + PANEL_WIDTH, y + PANEL_HEIGHT, panel, panel);
        quad(x, y, x + PANEL_WIDTH, y + 1, border, border);
        quad(x, y + PANEL_HEIGHT - 1, x + PANEL_WIDTH, y + PANEL_HEIGHT, border, border);
        quad(x, y, x + 1, y + PANEL_HEIGHT, border, border);
        quad(x + PANEL_WIDTH - 1, y, x + PANEL_WIDTH, y + PANEL_HEIGHT, border, border);
        drawHistoryGraph(x + 7, y + 35, state);
        GlStateManager.enableTexture2D();
        FontRenderer font = minecraft.fontRenderer;
        String title = "NFR cache heap / generations  "
                + (state.generational ? "YOUNG  ->  OLD" : "SINGLE LRU");
        if (NeofontrenderConfig.cacheOverlayFps()) {
            title += "  FPS " + Minecraft.getDebugFPS();
        }
        drawClipped(font, title, x + 8, y + 5, PANEL_WIDTH - 52, 0xFFE6ECF3);
        font.drawString("past", x + 73, y + 21, 0xFF8593A6);
        font.drawString("now", x + 326, y + 21, 0xFF8593A6);
        if (state.generational) {
            font.drawString("young", x + 350, y + 21, 0xFF43C7FF);
            font.drawString("old", x + 389, y + 21, 0xFFD084FF);
        }
        font.drawString("adapt", x + 414, y + 21, 0xFFFFB454);
        current(font, "text", state.render, x + 350, y + 43,
                hitRate(state.renderHits, state.renderMisses));
        current(font, "measure", state.measure, x + 350, y + 75,
                hitRate(state.measureHits, state.measureMisses));
        current(font, "char tex", state.characterRender, x + 350, y + 107,
                hitRate(state.characterHits, state.characterMisses));
        current(font, "char width", state.characterMeasure, x + 350, y + 139,
                hitRate(state.characterHits, state.characterMisses));
        current(font, "async", asyncGeneration(state), x + 350, y + 171,
                state.async.completed <= 0 ? 0.0 : hitRate(state.async.completed,
                        state.async.completed + state.async.dropped),
                " D" + state.async.dropped);
        current(font, "adaptive", adaptiveGeneration(state), x + 350, y + 203,
                adaptiveHitRate(state), " C" + state.adaptiveCompiled);
        font.drawString("PIN", x + PANEL_WIDTH - 34, y + 5, isPinned() ? 0xFF43C7FF : 0xFF9DAABD);
        if (!texture) GlStateManager.disableTexture2D();
        if (!blend) GlStateManager.disableBlend();
    }

    private static void sample(CosmicTextRenderer.CacheVisualState state) {
        if (!historyInitialized || historyMode != state.generational) {
            historyInitialized = true;
            historyMode = state.generational;
            historyCount = 0;
            historyCursor = 0;
        }
        int slot = historyCursor;
        put(0, slot, state.render);
        put(1, slot, state.measure);
        put(2, slot, state.characterRender);
        put(3, slot, state.characterMeasure);
        CosmicTextRenderer.CacheGeneration async = asyncGeneration(state);
        put(4, slot, async);
        put(5, slot, adaptiveGeneration(state));
        historyCursor = (historyCursor + 1) % HISTORY;
        historyCount = Math.min(HISTORY, historyCount + 1);
    }

    private static void put(int row, int slot, CosmicTextRenderer.CacheGeneration value) {
        youngHistory[row * HISTORY + slot] = value.young;
        oldHistory[row * HISTORY + slot] = value.old;
    }

    private static void drawHistoryGraph(int x, int y, CosmicTextRenderer.CacheVisualState state) {
        final int graphX = x + 60;
        final int graphWidth = 275;
        final int rowHeight = 28;
        final int graphHeight = 22;
        for (int row = 0; row < ROWS; row++) {
            quad(graphX, y + row * rowHeight, graphX + graphWidth,
                    y + row * rowHeight + graphHeight,
                    withOpacity(0x603C4656, NeofontrenderConfig.cacheOverlayOpacity()),
                    withOpacity(0x603C4656, NeofontrenderConfig.cacheOverlayOpacity()));
        }
        BufferBuilder buffer = Tessellator.getInstance().getBuffer();
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        int count = Math.max(1, historyCount);
        int first = historyCount < HISTORY ? 0 : historyCursor;
        float slotWidth = graphWidth / (float) count;
        for (int row = 0; row < ROWS; row++) {
            CosmicTextRenderer.CacheGeneration generation = generation(row, state);
            int rowColor = state.generational ? GENERATIONAL_COLORS[row] : SINGLE_COLOR;
            int capacity = Math.max(1, generation.youngMax + generation.oldMax);
            for (int column = 0; column < count; column++) {
                int slot = (first + column) % HISTORY;
                int young = youngHistory[row * HISTORY + slot];
                int old = oldHistory[row * HISTORY + slot];
                int totalHeight = Math.min(graphHeight, Math.round(graphHeight * (young + old) / (float) capacity));
                if (totalHeight <= 0) continue;
                float left = graphX + column * slotWidth;
                float right = graphX + (column + 1) * slotWidth - 0.5F;
                int bottom = y + row * rowHeight + graphHeight;
                int youngHeight = state.generational
                        ? Math.min(totalHeight, Math.round(graphHeight * young / (float) capacity)) : totalHeight;
                int oldHeight = totalHeight - youngHeight;
                if (youngHeight > 0) {
                    bar(buffer, left, bottom - youngHeight - oldHeight, right,
                            bottom - oldHeight, rowColor);
                }
                if (oldHeight > 0) {
                    bar(buffer, left, bottom - oldHeight, right, bottom,
                            state.generational ? Arc3DApi.lerpArgb(rowColor, 0xFFFFFFFF, 0.16F) : rowColor);
                }
            }
        }
        Tessellator.getInstance().draw();
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft != null) {
            FontRenderer font = minecraft.fontRenderer;
            font.drawString("text", x, y + 7, 0xFFD4DCE8);
            font.drawString("measure", x, y + 35, 0xFFD4DCE8);
            font.drawString("char tex", x, y + 63, 0xFFD4DCE8);
            font.drawString("char width", x, y + 91, 0xFFD4DCE8);
            font.drawString("async", x, y + 119, 0xFFD4DCE8);
            font.drawString("adaptive", x, y + 147, 0xFFFFD08A);
        }
    }

    private static CosmicTextRenderer.CacheGeneration generation(int row,
                                                                  CosmicTextRenderer.CacheVisualState state) {
        return switch (row) {
            case 0 -> state.render;
            case 1 -> state.measure;
            case 2 -> state.characterRender;
            case 3 -> state.characterMeasure;
            case 4 -> asyncGeneration(state);
            default -> adaptiveGeneration(state);
        };
    }

    private static CosmicTextRenderer.CacheGeneration asyncGeneration(CosmicTextRenderer.CacheVisualState state) {
        CosmicTextRenderer.CacheVisualState.AsyncState async = state.async;
        return new CosmicTextRenderer.CacheGeneration(async.queued + async.running,
                async.ready, Math.max(1, async.capacity), Math.max(1, async.capacity));
    }

    private static CosmicTextRenderer.CacheGeneration adaptiveGeneration(CosmicTextRenderer.CacheVisualState state) {
        int compiled = Math.min(state.adaptiveEntries, state.adaptiveCompiledEntries);
        return new CosmicTextRenderer.CacheGeneration(
                Math.max(0, state.adaptiveEntries - compiled), compiled,
                ADAPTIVE_MAX_ENTRIES, ADAPTIVE_MAX_ENTRIES);
    }

    private static double adaptiveHitRate(CosmicTextRenderer.CacheVisualState state) {
        long total = state.adaptiveHits + state.adaptiveRejected;
        return total <= 0 ? 0.0 : state.adaptiveHits / (double) total;
    }

    private static void current(FontRenderer font, String label,
                                CosmicTextRenderer.CacheGeneration value, int x, int y, double rate) {
        current(font, label, value, x, y, rate, "");
    }

    private static void current(FontRenderer font, String label,
                                CosmicTextRenderer.CacheGeneration value, int x, int y, double rate,
                                String suffix) {
        String text = value.youngMax > 0 && value.young > 0
                ? "Y " + value.young + "/" + value.youngMax + " O " + value.old + "/" + value.oldMax
                : "O " + value.old + "/" + value.oldMax;
        font.drawString(label, x, y, 0xFFB8C4D4);
        drawClipped(font, text + "  " + percent(rate) + suffix, x, y + 11, PANEL_WIDTH - 350 - 8,
                0xFF9DAABD);
    }

    private static double hitRate(long hits, long misses) {
        long total = hits + misses;
        return total <= 0 ? 0.0 : hits / (double) total;
    }

    private static String percent(double value) {
        return String.format(java.util.Locale.ROOT, "%.1f%%", Math.max(0.0, Math.min(1.0, value)) * 100.0);
    }

    private static void drawClipped(FontRenderer font, String text, int x, int y, int maxWidth, int color) {
        if (text == null || maxWidth <= 0) return;
        String value = text;
        while (value.length() > 1 && font.getStringWidth(value) > maxWidth) {
            value = value.substring(0, value.length() - 1);
        }
        font.drawStringWithShadow(value, x, y, color);
    }

    private static int withOpacity(int color, float opacity) {
        int alpha = Math.max(0, Math.min(255, Math.round(Color.alpha(color) * opacity)));
        return (color & 0x00FFFFFF) | (alpha << 24);
    }

    private static int clampX(int value, int width) {
        return Math.max(4, Math.min(Math.max(4, width - PANEL_WIDTH - 4), value));
    }

    private static int clampY(int value, int height) {
        return Math.max(4, Math.min(Math.max(4, height - PANEL_HEIGHT - 4), value));
    }

    private static void bar(BufferBuilder buffer, float left, float top, float right, float bottom, int color) {
        vertex(buffer, left, top, color);
        vertex(buffer, left, bottom, color);
        vertex(buffer, right, bottom, color);
        vertex(buffer, right, top, color);
    }

    private static void quad(float left, float top, float right, float bottom,
                             int leftColor, int rightColor) {
        if (right <= left || bottom <= top) return;
        BufferBuilder buffer = Tessellator.getInstance().getBuffer();
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        vertex(buffer, left, top, leftColor);
        vertex(buffer, left, bottom, leftColor);
        vertex(buffer, right, bottom, rightColor);
        vertex(buffer, right, top, rightColor);
        Tessellator.getInstance().draw();
    }

    private static void vertex(BufferBuilder buffer, float x, float y, int color) {
        buffer.pos(x, y, 0.0D).color(Color.red(color), Color.green(color),
                Color.blue(color), Color.alpha(color)).endVertex();
    }
}

