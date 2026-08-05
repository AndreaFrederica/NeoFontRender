package neofontrender.addons.loading;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiDownloadTerrain;
import net.minecraft.client.gui.GuiIngameMenu;
import net.minecraft.client.gui.GuiScreenWorking;
import net.minecraft.client.multiplayer.ChunkProviderClient;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.resources.I18n;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.world.WorldServer;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.event.world.ChunkEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import neofontrender.addons.tips.TipManager;
import neofontrender.addons.tips.TipRenderer;
import neofontrender.addons.tips.TipsConfig;
import neofontrender.addons.mixin.AccessorChunkProviderClient;
import neofontrender.addons.tooltips.AddonI18n;
import neofontrender.api.text.ModernTextApi;
import org.lwjgl.opengl.GL11;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Independent 1.12 implementation of a bottom-anchored world-loading presentation.
 *
 * <p>No third-party textures, fonts, shaders, or implementation code are used. The indicator and
 * gradients are generated with Minecraft's immediate-mode GUI primitives.</p>
 */
public enum WorldLoadingRenderer {
    INSTANCE;

    private static final int MATERIAL_GRADIENT_SEGMENTS = 24;
    private static final long RENDERER_FRAME_INTERVAL_NANOS = 50_000_000L;
    private static final float[] MATERIAL_GRADIENT_AMOUNTS = createMaterialGradientAmounts();
    private final WorldLoadingProgress progress = new WorldLoadingProgress();
    private final Arc3DLoadingBarRenderer arc3dBar = new Arc3DLoadingBarRenderer();
    private final Arc3DMaterialSpinnerRenderer materialSpinner =
            new Arc3DMaterialSpinnerRenderer();
    private final Set<Long> preparedSpawnChunks = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private boolean active;
    private boolean fading;
    private boolean sessionEnabled;
    private boolean hasStableWorld;
    private int stableDimension;
    private long startedNanos;
    private long fadeStartedNanos;
    private long lastDrawNanos;
    private float displayedProgress;
    private volatile IntegratedServer trackedIntegratedServer;
    private volatile boolean integratedPreparationActive;
    private volatile boolean integratedLaunchActive;
    private volatile int exactPreparedSpawnChunks = -1;
    private volatile boolean exactSpawnCounterObserved;
    private long integratedLaunchStartedNanos;
    private IntegratedServer renderedIntegratedServer;
    private float integratedDisplayedProgress;
    private float clientPhaseStart;
    private float clientPhaseEnd;
    private boolean rendererPreparationActive;
    private boolean rendererPreparationCompleted;
    private boolean rendererPreparationIntegrated;
    private Thread rendererPreparationThread;
    private int rendererChunksCompleted;
    private int rendererChunksTotal;
    private long lastRendererFrameNanos;
    private boolean rendererFrameInProgress;
    private volatile String vanillaStage = "";
    private volatile String vanillaDetail = "";

    @SubscribeEvent
    public void serverWorldLoaded(WorldEvent.Load event) {
        if (!(event.getWorld() instanceof WorldServer) || event.getWorld().provider.getDimension() != 0) return;
        if (!(event.getWorld().getMinecraftServer() instanceof IntegratedServer)) return;
        IntegratedServer server = (IntegratedServer) event.getWorld().getMinecraftServer();
        if (server.serverIsInRunLoop()) return;
        trackedIntegratedServer = server;
        integratedPreparationActive = true;
        preparedSpawnChunks.clear();
    }

    @SubscribeEvent
    public void serverChunkLoaded(ChunkEvent.Load event) {
        if (!(event.getWorld() instanceof WorldServer) || event.getWorld().provider.getDimension() != 0) return;
        if (!(event.getWorld().getMinecraftServer() instanceof IntegratedServer)) return;
        IntegratedServer server = (IntegratedServer) event.getWorld().getMinecraftServer();
        if (server != trackedIntegratedServer || server.serverIsInRunLoop()) return;
        int x = event.getChunk().x;
        int z = event.getChunk().z;
        preparedSpawnChunks.add((x & 0xFFFFFFFFL) | ((z & 0xFFFFFFFFL) << 32));
    }

    @SubscribeEvent
    public void onGuiOpen(GuiOpenEvent event) {
        if (event.getGui() instanceof GuiIngameMenu) {
            WorldLoadingSnapshotManager.INSTANCE.requestCleanExitFrame();
        }
        if (event.getGui() instanceof GuiDownloadTerrain) {
            begin(Minecraft.getMinecraft(), System.nanoTime());
            vanillaStage = I18n.format("multiplayer.downloadingTerrain");
            vanillaDetail = "";
            integratedLaunchActive = false;
        } else if (active) {
            finish(System.nanoTime());
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void beforeScreenDraw(GuiScreenEvent.DrawScreenEvent.Pre event) {
        if (event.getGui() instanceof GuiScreenWorking
                && WorldLoadingConfig.enabled
                && WorldLoadingConfig.worldJoin
                && WorldLoadingSnapshotManager.INSTANCE.hasActive()
                && Minecraft.getMinecraft().getIntegratedServer() != null
                && Minecraft.getMinecraft().world == null) {
            event.setCanceled(true);
            render(event.getGui().width, event.getGui().height, 0.99F, 1.0F, System.nanoTime());
            return;
        }
        if (!(event.getGui() instanceof GuiDownloadTerrain) || !WorldLoadingConfig.enabled) return;
        if (!active) begin(Minecraft.getMinecraft(), System.nanoTime());
        if (!sessionEnabled) return;

        event.setCanceled(true);
        long now = System.nanoTime();
        float clientReadiness = progress.update(loadedChunkCount(), renderDistance(), startedNanos, now);
        displayedProgress = mapClientPhase(
                clientReadiness, clientPhaseStart, clientPhaseEnd);
        lastDrawNanos = now;
        render(event.getGui().width, event.getGui().height, displayedProgress, 1.0F, now);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void afterGameOverlay(RenderGameOverlayEvent.Post event) {
        if (event.getType() != RenderGameOverlayEvent.ElementType.ALL || !fading) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.world == null || mc.currentScreen instanceof GuiDownloadTerrain) return;

        long now = System.nanoTime();
        float alpha = fadeAlpha(now);
        if (alpha <= 0.0F) {
            fading = false;
            WorldLoadingSnapshotManager.INSTANCE.releaseActive();
            return;
        }
        render(event.getResolution().getScaledWidth(), event.getResolution().getScaledHeight(),
                1.0F, alpha, now);
    }

    @SubscribeEvent
    public void clientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (trackedIntegratedServer != null && trackedIntegratedServer.serverIsInRunLoop()) {
            integratedPreparationActive = false;
        }
        if (mc.world == null || mc.player == null || mc.currentScreen instanceof GuiDownloadTerrain) return;
        integratedLaunchActive = false;
        stableDimension = mc.player.dimension;
        hasStableWorld = true;
    }

    /**
     * Starts before Minecraft creates the integrated server. This closes the early-frame gap where
     * vanilla's dirt background used to be presented before WorldEvent.Load was available.
     */
    public void beginIntegratedWorldLaunch() {
        long now = System.nanoTime();
        integratedLaunchActive = WorldLoadingConfig.enabled && WorldLoadingConfig.worldJoin;
        integratedLaunchStartedNanos = now;
        renderedIntegratedServer = null;
        integratedDisplayedProgress = 0.02F;
        exactPreparedSpawnChunks = -1;
        exactSpawnCounterObserved = false;
        arc3dBar.reset(now);
    }

    public boolean isIntegratedLaunchActive() {
        return integratedLaunchActive;
    }

    public boolean isLoadingScreenPresentationActive() {
        return integratedLaunchActive || rendererPreparationActive;
    }

    /** Starts the client-only renderer phase shared by singleplayer, multiplayer, and dimensions. */
    public void beginClientWorldLoad(WorldClient nextWorld) {
        if (nextWorld == null) {
            cancelClientRendererPreparation();
            return;
        }

        Minecraft mc = Minecraft.getMinecraft();
        boolean dimensionChange = hasStableWorld && mc.player != null
                && nextWorld.provider.getDimension() != stableDimension;
        boolean enabled = WorldLoadingConfig.enabled
                && (dimensionChange ? WorldLoadingConfig.dimensionChange : WorldLoadingConfig.worldJoin);
        rendererPreparationCompleted = false;
        rendererPreparationActive = enabled;
        if (!enabled) {
            rendererPreparationThread = null;
            return;
        }

        long now = System.nanoTime();
        rendererPreparationIntegrated = integratedLaunchActive && !dimensionChange;
        rendererPreparationThread = Thread.currentThread();
        rendererChunksCompleted = 0;
        rendererChunksTotal = 0;
        lastRendererFrameNanos = 0L;
        rendererFrameInProgress = false;
        integratedDisplayedProgress = rendererPreparationIntegrated
                ? Math.max(0.88F, integratedDisplayedProgress) : 0.02F;
        vanillaStage = AddonI18n.tr(
                "neofontrender_ui_enhancements.loading.preparing_renderer");
        vanillaDetail = "";
        if (!rendererPreparationIntegrated) arc3dBar.reset(now);
        TipManager.INSTANCE.reset();
    }

    public void finishClientWorldLoad(WorldClient nextWorld) {
        if (nextWorld == null) {
            cancelClientRendererPreparation();
            return;
        }
        if (!rendererPreparationActive) return;
        if (rendererChunksTotal > 0) {
            rendererChunksCompleted = rendererChunksTotal;
            integratedDisplayedProgress = Math.max(integratedDisplayedProgress,
                    rendererPreparationProgress(rendererPreparationIntegrated,
                            rendererChunksCompleted, rendererChunksTotal));
        }
        rendererPreparationActive = false;
        rendererPreparationCompleted = true;
        rendererPreparationThread = null;
        rendererFrameInProgress = false;
    }

    public void beginClientRenderChunkBatch(int total) {
        if (!ownsRendererPreparationThread()) return;
        rendererChunksCompleted = 0;
        rendererChunksTotal = Math.max(1, total);
    }

    public void recordClientRenderChunk() {
        if (!ownsRendererPreparationThread() || rendererChunksTotal <= 0) return;
        rendererChunksCompleted = Math.min(rendererChunksTotal, rendererChunksCompleted + 1);
        integratedDisplayedProgress = Math.max(integratedDisplayedProgress,
                rendererPreparationProgress(rendererPreparationIntegrated,
                        rendererChunksCompleted, rendererChunksTotal));
        requestRendererPreparationFrame(false);
    }

    public void finishClientRenderChunkBatch() {
        if (!ownsRendererPreparationThread() || rendererChunksTotal <= 0) return;
        rendererChunksCompleted = rendererChunksTotal;
        integratedDisplayedProgress = Math.max(integratedDisplayedProgress,
                rendererPreparationProgress(rendererPreparationIntegrated,
                        rendererChunksCompleted, rendererChunksTotal));
        requestRendererPreparationFrame(true);
    }

    private void requestRendererPreparationFrame(boolean force) {
        if (rendererFrameInProgress) return;
        long now = System.nanoTime();
        if (!force && now - lastRendererFrameNanos < RENDERER_FRAME_INTERVAL_NANOS) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.loadingScreen == null || !mc.isCallingFromMinecraftThread()) return;
        lastRendererFrameNanos = now;
        rendererFrameInProgress = true;
        try {
            mc.loadingScreen.setLoadingProgress(
                    Math.round(integratedDisplayedProgress * 100.0F));
        } finally {
            rendererFrameInProgress = false;
        }
    }

    private boolean ownsRendererPreparationThread() {
        return rendererPreparationActive
                && Thread.currentThread() == rendererPreparationThread;
    }

    private void cancelClientRendererPreparation() {
        rendererPreparationActive = false;
        rendererPreparationCompleted = false;
        rendererPreparationIntegrated = false;
        rendererPreparationThread = null;
        rendererChunksCompleted = 0;
        rendererChunksTotal = 0;
        rendererFrameInProgress = false;
    }

    public void beginExactSpawnPreparation(MinecraftServer server) {
        if (!(server instanceof IntegratedServer)) return;
        trackedIntegratedServer = (IntegratedServer) server;
        integratedPreparationActive = true;
        exactPreparedSpawnChunks = 0;
        exactSpawnCounterObserved = false;
        preparedSpawnChunks.clear();
    }

    public void recordExactSpawnChunk(MinecraftServer server) {
        if (server != trackedIntegratedServer || exactPreparedSpawnChunks < 0) return;
        exactSpawnCounterObserved = true;
        exactPreparedSpawnChunks = Math.min(625, exactPreparedSpawnChunks + 1);
    }

    public void finishExactSpawnPreparation(MinecraftServer server) {
        if (server != trackedIntegratedServer || exactPreparedSpawnChunks < 0) return;
        if (exactSpawnCounterObserved) exactPreparedSpawnChunks = 625;
        else exactPreparedSpawnChunks = -1;
    }

    void configChanged() {
        if (!WorldLoadingConfig.enabled) {
            active = false;
            fading = false;
            cancelClientRendererPreparation();
            WorldLoadingSnapshotManager.INSTANCE.releaseActive();
        } else if (!WorldLoadingConfig.lastExitSnapshot) {
            WorldLoadingSnapshotManager.INSTANCE.releaseActive();
        }
    }

    /**
     * Replaces the visual contents of LoadingScreenRenderer while the integrated server prepares
     * the initial world. Vanilla prepares exactly 625 spawn chunks in 1.12.2; chunk events provide
     * a granular real count, while MinecraftServer.percentDone remains an authoritative fallback.
     */
    public void renderLoadingScreen(int width, int height, int vanillaProgress,
                                    String stage, String detail) {
        if (!WorldLoadingConfig.enabled || !isLoadingScreenPresentationActive()) return;
        Minecraft mc = Minecraft.getMinecraft();
        IntegratedServer server = mc.getIntegratedServer();

        if (server != null && renderedIntegratedServer != server) {
            renderedIntegratedServer = server;
            integratedDisplayedProgress = 0.02F;
        }
        float exact;
        if (rendererPreparationActive) {
            vanillaStage = AddonI18n.tr(
                    "neofontrender_ui_enhancements.loading.preparing_renderer");
            vanillaDetail = "";
            exact = rendererPreparationProgress(rendererPreparationIntegrated,
                    rendererChunksCompleted, rendererChunksTotal);
        } else if (WorldLoadingConfig.singleplayerServerProgress && server != null
                && server == trackedIntegratedServer) {
            updateVanillaStage(stage, detail);
            int eventPrepared = preparedSpawnChunks.size();
            int serverPercent = server.percentDone;
            float spawnProgress = authoritativeSpawnProgress(exactSpawnCounterObserved,
                    exactPreparedSpawnChunks, eventPrepared, serverPercent);
            exact = serverPhaseProgress(spawnProgress);
        } else {
            updateVanillaStage(stage, detail);
            float seconds = Math.max(0.0F,
                    (System.nanoTime() - integratedLaunchStartedNanos) / 1_000_000_000.0F);
            float waiting = 0.02F + 0.06F * (1.0F - (float) Math.exp(-seconds / 1.2F));
            // LoadingScreenRenderer may still contain 100% from an unrelated preceding task.
            // Do not feed that stale value into the monotonic Arc3D animation.
            exact = waiting;
        }
        integratedDisplayedProgress = Math.max(integratedDisplayedProgress, exact);
        render(width, height, Math.min(0.99F, integratedDisplayedProgress),
                1.0F, System.nanoTime());
    }

    static float integratedPreparationProgress(int preparedChunks, int serverPercent,
                                               int suppliedPercent) {
        float chunkProgress = Math.max(0.0F, Math.min(1.0F, preparedChunks / 625.0F));
        float reportedProgress = Math.max(0.0F, Math.min(1.0F, serverPercent / 100.0F));
        float suppliedProgress = suppliedPercent >= 0
                ? Math.max(0.0F, Math.min(1.0F, suppliedPercent / 100.0F)) : 0.0F;
        return Math.max(chunkProgress, Math.max(reportedProgress, suppliedProgress));
    }

    static float authoritativeSpawnProgress(boolean exactObserved, int exactChunks,
                                            int eventChunks, int serverPercent) {
        if (exactObserved) {
            return Math.max(0.0F, Math.min(1.0F, exactChunks / 625.0F));
        }
        return integratedPreparationProgress(eventChunks, serverPercent, -1);
    }

    static float serverPhaseProgress(float spawnProgress) {
        return 0.08F + Math.max(0.0F, Math.min(1.0F, spawnProgress)) * 0.80F;
    }

    static float clientPhaseProgress(float clientReadiness) {
        return mapClientPhase(clientReadiness, 0.92F, 0.99F);
    }

    static float multiplayerClientPhaseProgress(float clientReadiness) {
        return mapClientPhase(clientReadiness, 0.12F, 0.97F);
    }

    static float rendererPreparationProgress(boolean integrated, int completed, int total) {
        float amount = total <= 0 ? 0.0F
                : Math.max(0.0F, Math.min(1.0F, completed / (float) total));
        float start = integrated ? 0.88F : 0.02F;
        float end = integrated ? 0.92F : 0.12F;
        return start + amount * (end - start);
    }

    private static float mapClientPhase(float readiness, float start, float end) {
        return start + Math.max(0.0F, Math.min(1.0F, readiness)) * (end - start);
    }

    private void begin(Minecraft mc, long now) {
        boolean dimensionChange = hasStableWorld && mc.player != null
                && mc.player.dimension != stableDimension;
        boolean continuingIntegratedLaunch = integratedLaunchActive && !dimensionChange;
        boolean continuingRendererPreparation = rendererPreparationCompleted;
        rendererPreparationCompleted = false;
        sessionEnabled = WorldLoadingConfig.enabled
                && (dimensionChange ? WorldLoadingConfig.dimensionChange : WorldLoadingConfig.worldJoin);
        active = true;
        fading = false;
        startedNanos = now;
        lastDrawNanos = now;
        clientPhaseStart = continuingIntegratedLaunch ? 0.92F
                : continuingRendererPreparation ? 0.12F : 0.0F;
        clientPhaseEnd = continuingIntegratedLaunch ? 0.99F
                : continuingRendererPreparation ? 0.97F : 1.0F;
        displayedProgress = continuingIntegratedLaunch
                ? Math.max(clientPhaseStart, integratedDisplayedProgress)
                : continuingRendererPreparation ? clientPhaseStart : 0.02F;
        progress.reset(now);
        if (!continuingIntegratedLaunch && !continuingRendererPreparation) arc3dBar.reset(now);
        TipManager.INSTANCE.reset();
    }

    private void finish(long now) {
        active = false;
        if (!sessionEnabled || !WorldLoadingConfig.fadeOut
                || WorldLoadingConfig.fadeOutDurationMillis <= 0) {
            fading = false;
            WorldLoadingSnapshotManager.INSTANCE.releaseActive();
            return;
        }
        displayedProgress = 1.0F;
        fadeStartedNanos = now;
        fading = true;
    }

    private static int loadedChunkCount() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.world == null || mc.world.getChunkProvider() == null) return 0;
        if (!(mc.world.getChunkProvider() instanceof ChunkProviderClient)) return 0;
        return Math.max(0, ((AccessorChunkProviderClient) mc.world.getChunkProvider())
                .nfrUi$getLoadedChunks().size());
    }

    private static int renderDistance() {
        return Math.max(2, Minecraft.getMinecraft().gameSettings.renderDistanceChunks);
    }

    private float fadeAlpha(long now) {
        long duration = WorldLoadingConfig.fadeOutDurationMillis * 1_000_000L;
        if (duration <= 0L) return 0.0F;
        float elapsed = Math.max(0.0F, Math.min(1.0F, (float) (now - fadeStartedNanos) / duration));
        float smooth = elapsed * elapsed * (3.0F - 2.0F * elapsed);
        return 1.0F - smooth;
    }

    private void render(int width, int height, float amount, float alpha, long now) {
        Minecraft mc = Minecraft.getMinecraft();
        FontRenderer font = mc.fontRenderer;
        float visualAmount = arc3dBar.update(amount, now);
        int margin = Math.max(12, Math.min(28, width / 32));
        int bottom = height - Math.max(13, height / 28);
        int textBottom = bottom - 4;

        GlStateManager.disableLighting();
        GlStateManager.disableDepth();
        LoadingBlendMode.enableSourceOver();

        boolean snapshot = WorldLoadingSnapshotManager.INSTANCE.draw(width, height, alpha);
        if (!snapshot && mc.world == null) {
            // A new save or the first visit to a server cannot have a previous-world snapshot.
            // Use Minecraft's original tiled menu background instead of showing an empty panel.
            drawVanillaBackground(width, height, alpha);
        } else if (!snapshot) {
            Gui.drawRect(0, 0, width, height, scaledAlpha(0x18000000, alpha));
        }
        if (WorldLoadingConfig.bottomShade) {
            drawVerticalGradient(0, Math.max(0, height * 3 / 5), width, height,
                    scaledAlpha(0x00000000, alpha), scaledAlpha(0xC8000000, alpha));
        }

        String label = currentStageLabel(visualAmount) + animatedDots(now);
        float titleScale = width >= 700 ? 2.5F : width >= 420 ? 2.0F : 1.55F;
        int titleColor = scaledAlpha(WorldLoadingConfig.textColor, alpha);
        if (ModernTextApi.isAvailable()) {
            // Request a real large logical font from NFR's public engine-independent API. UIE has
            // no knowledge of Cosmic or SFR/AWT and never scales a small cached texture.
            float titleFontSize = Math.max(1.0F, font.FONT_HEIGHT * titleScale);
            ModernTextApi.draw(label, margin, textBottom - titleFontSize,
                    titleFontSize, titleColor);
        } else {
            GlStateManager.pushMatrix();
            GlStateManager.translate(margin, textBottom - font.FONT_HEIGHT * titleScale, 0.0F);
            GlStateManager.scale(titleScale, titleScale, 1.0F);
            font.drawString(label, 0, 0, titleColor, false);
            GlStateManager.popMatrix();
        }

        String detail = currentDetailLabel(label);
        if (!detail.isEmpty()) {
            font.drawString(detail, margin, textBottom + 2,
                    scaledAlpha(0xFFB8C0CC, alpha), false);
        }

        // Tips above the title
        if (TipsConfig.enabled && TipsConfig.showOnWorldLoading) {
            TipManager.INSTANCE.update(now, TipsConfig.cycleTimeMillis);
            float titleFontSize = Math.max(1.0F, font.FONT_HEIGHT * titleScale);
            int titleTop = (int) (textBottom - titleFontSize);
            TipRenderer.draw(width, height, margin, titleTop, alpha, WorldLoadingConfig.textColor);
        }

        int spinnerX = width - margin - 10;
        int spinnerY = bottom - 7;
        if (WorldLoadingConfig.spinner) {
            materialSpinner.draw(spinnerX, spinnerY, WorldLoadingConfig.accentColor, alpha, now);
        }
        if (WorldLoadingConfig.percentage) {
            String percent = Math.round(visualAmount * 100.0F) + "%";
            int right = WorldLoadingConfig.spinner ? spinnerX - 17 : width - margin;
            font.drawString(percent, right - font.getStringWidth(percent), bottom - font.FONT_HEIGHT - 1,
                    scaledAlpha(WorldLoadingConfig.textColor, alpha), false);
        }
        if (WorldLoadingConfig.progressBar) {
            arc3dBar.draw(width, height, visualAmount, WorldLoadingConfig.accentColor, alpha, now);
        }

        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.enableDepth();
        LoadingBlendMode.restoreMinecraftDefault();
        GlStateManager.disableBlend();
    }

    private String currentStageLabel(float amount) {
        String stage = cleanStage(vanillaStage);
        return stage.isEmpty() ? translatedLoadingLabel(amount) : stage;
    }

    private String currentDetailLabel(String title) {
        String detail = cleanStage(vanillaDetail);
        return detail.equalsIgnoreCase(cleanStage(title)) ? "" : detail;
    }

    private void updateVanillaStage(String stage, String detail) {
        String cleanStage = cleanStage(stage);
        String cleanDetail = cleanStage(detail);
        if (!cleanStage.isEmpty()) vanillaStage = cleanStage;
        vanillaDetail = cleanDetail;
    }

    private static String cleanStage(String value) {
        if (value == null) return "";
        String result = value.trim();
        while (result.endsWith(".")) result = result.substring(0, result.length() - 1).trim();
        return result;
    }

    private static String translatedLoadingLabel(float amount) {
        String key = amount < 0.12F
                ? "neofontrender_ui_enhancements.loading.preparing_world"
                : amount < 0.97F
                ? "neofontrender_ui_enhancements.loading.loading_world"
                : "neofontrender_ui_enhancements.loading.finalizing";
        return AddonI18n.tr(key);
    }

    private static String animatedDots(long now) {
        int count = 1 + (int) ((now / 360_000_000L) % 3L);
        return count == 1 ? "." : count == 2 ? ".." : "...";
    }

    private static void drawVerticalGradient(int left, int top, int right, int bottom,
                                             int topColor, int bottomColor) {
        float startA = (topColor >>> 24) / 255.0F;
        float startR = (topColor >> 16 & 255) / 255.0F;
        float startG = (topColor >> 8 & 255) / 255.0F;
        float startB = (topColor & 255) / 255.0F;
        float endA = (bottomColor >>> 24) / 255.0F;
        float endR = (bottomColor >> 16 & 255) / 255.0F;
        float endG = (bottomColor >> 8 & 255) / 255.0F;
        float endB = (bottomColor & 255) / 255.0F;
        // GUI rendering normally keeps alpha test at 0.1. Leaving it enabled discards the
        // transparent end of this gradient and creates a visible horizontal start edge.
        GlStateManager.disableAlpha();
        GlStateManager.disableTexture2D();
        LoadingBlendMode.enableSourceOver();
        GlStateManager.shadeModel(GL11.GL_SMOOTH);
        try {
            BufferBuilder buffer = Tessellator.getInstance().getBuffer();
            buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
            for (int segment = 0; segment < MATERIAL_GRADIENT_SEGMENTS; segment++) {
                float firstPosition = segment / (float) MATERIAL_GRADIENT_SEGMENTS;
                float secondPosition = (segment + 1) / (float) MATERIAL_GRADIENT_SEGMENTS;
                float firstAmount = MATERIAL_GRADIENT_AMOUNTS[segment];
                float secondAmount = MATERIAL_GRADIENT_AMOUNTS[segment + 1];
                float firstY = top + (bottom - top) * firstPosition;
                float secondY = top + (bottom - top) * secondPosition;
                float firstR = lerp(startR, endR, firstAmount);
                float firstG = lerp(startG, endG, firstAmount);
                float firstB = lerp(startB, endB, firstAmount);
                float firstA = lerp(startA, endA, firstAmount);
                float secondR = lerp(startR, endR, secondAmount);
                float secondG = lerp(startG, endG, secondAmount);
                float secondB = lerp(startB, endB, secondAmount);
                float secondA = lerp(startA, endA, secondAmount);
                buffer.pos(right, firstY, 0.0D).color(firstR, firstG, firstB, firstA).endVertex();
                buffer.pos(left, firstY, 0.0D).color(firstR, firstG, firstB, firstA).endVertex();
                buffer.pos(left, secondY, 0.0D).color(secondR, secondG, secondB, secondA).endVertex();
                buffer.pos(right, secondY, 0.0D).color(secondR, secondG, secondB, secondA).endVertex();
            }
            Tessellator.getInstance().draw();
        } finally {
            GlStateManager.shadeModel(GL11.GL_FLAT);
            GlStateManager.enableTexture2D();
            GlStateManager.enableAlpha();
        }
    }

    /** Material standard curve: cubic-bezier(0.4, 0.0, 0.2, 1.0). */
    static float materialGradientCurve(float position) {
        float target = Math.max(0.0F, Math.min(1.0F, position));
        float low = 0.0F;
        float high = 1.0F;
        for (int iteration = 0; iteration < 10; iteration++) {
            float parameter = (low + high) * 0.5F;
            float x = cubicBezier(parameter, 0.4F, 0.2F);
            if (x < target) low = parameter;
            else high = parameter;
        }
        return cubicBezier((low + high) * 0.5F, 0.0F, 1.0F);
    }

    private static float[] createMaterialGradientAmounts() {
        float[] amounts = new float[MATERIAL_GRADIENT_SEGMENTS + 1];
        for (int index = 0; index <= MATERIAL_GRADIENT_SEGMENTS; index++) {
            amounts[index] = materialGradientCurve(index / (float) MATERIAL_GRADIENT_SEGMENTS);
        }
        return amounts;
    }

    private static float cubicBezier(float parameter, float firstControl, float secondControl) {
        float inverse = 1.0F - parameter;
        return 3.0F * inverse * inverse * parameter * firstControl
                + 3.0F * inverse * parameter * parameter * secondControl
                + parameter * parameter * parameter;
    }

    private static float lerp(float start, float end, float amount) {
        return start + (end - start) * amount;
    }

    /** Equivalent to GuiScreen.drawBackground(0), with alpha support for the loading fade. */
    private static void drawVanillaBackground(int width, int height, float opacity) {
        Minecraft.getMinecraft().getTextureManager().bindTexture(Gui.OPTIONS_BACKGROUND);
        GlStateManager.disableLighting();
        GlStateManager.disableFog();
        GlStateManager.enableTexture2D();
        LoadingBlendMode.enableSourceOver();
        int alpha = Math.round(255.0F * Math.max(0.0F, Math.min(1.0F, opacity)));
        BufferBuilder buffer = Tessellator.getInstance().getBuffer();
        buffer.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);
        buffer.pos(0.0D, height, 0.0D)
                .tex(0.0D, height / 32.0F).color(64, 64, 64, alpha).endVertex();
        buffer.pos(width, height, 0.0D)
                .tex(width / 32.0F, height / 32.0F).color(64, 64, 64, alpha).endVertex();
        buffer.pos(width, 0.0D, 0.0D)
                .tex(width / 32.0F, 0.0D).color(64, 64, 64, alpha).endVertex();
        buffer.pos(0.0D, 0.0D, 0.0D)
                .tex(0.0D, 0.0D).color(64, 64, 64, alpha).endVertex();
        Tessellator.getInstance().draw();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private static int scaledAlpha(int color, float scale) {
        int alpha = Math.round((color >>> 24) * Math.max(0.0F, Math.min(1.0F, scale)));
        return color & 0x00FFFFFF | alpha << 24;
    }
}
