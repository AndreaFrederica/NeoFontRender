package neofontrender.addons.loading;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiDownloadTerrain;
import net.minecraft.client.gui.GuiIngameMenu;
import net.minecraft.client.gui.GuiScreenWorking;
import net.minecraft.client.multiplayer.ChunkProviderClient;
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
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.event.world.ChunkEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import neofontrender.addons.mixin.AccessorChunkProviderClient;

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
    private boolean worldJoinClientPhase;

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
            integratedLaunchActive = false;
        } else if (active) {
            finish(System.nanoTime());
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void afterWorldRendered(RenderWorldLastEvent event) {
        WorldLoadingSnapshotManager.INSTANCE.captureRequestedWorldFrame();
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
        displayedProgress = worldJoinClientPhase
                ? clientPhaseProgress(clientReadiness)
                : clientReadiness;
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
    public void renderIntegratedServerLoading(int width, int height, int vanillaProgress) {
        if (!WorldLoadingConfig.enabled || !WorldLoadingConfig.worldJoin
                || !integratedLaunchActive) return;
        Minecraft mc = Minecraft.getMinecraft();
        IntegratedServer server = mc.getIntegratedServer();

        if (server != null && renderedIntegratedServer != server) {
            renderedIntegratedServer = server;
            integratedDisplayedProgress = 0.02F;
        }
        int eventPrepared = server != null && server == trackedIntegratedServer
                ? preparedSpawnChunks.size() : 0;
        int serverPercent = server == null ? 0 : server.percentDone;
        float exact;
        if (WorldLoadingConfig.singleplayerServerProgress && server != null
                && server == trackedIntegratedServer) {
            float spawnProgress = authoritativeSpawnProgress(exactSpawnCounterObserved,
                    exactPreparedSpawnChunks, eventPrepared, serverPercent);
            exact = serverPhaseProgress(spawnProgress);
        } else {
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
        return 0.88F + Math.max(0.0F, Math.min(1.0F, clientReadiness)) * 0.11F;
    }

    private void begin(Minecraft mc, long now) {
        boolean dimensionChange = hasStableWorld && mc.player != null
                && mc.player.dimension != stableDimension;
        boolean continuingIntegratedLaunch = integratedLaunchActive && !dimensionChange;
        sessionEnabled = WorldLoadingConfig.enabled
                && (dimensionChange ? WorldLoadingConfig.dimensionChange : WorldLoadingConfig.worldJoin);
        active = true;
        fading = false;
        worldJoinClientPhase = continuingIntegratedLaunch;
        startedNanos = now;
        lastDrawNanos = now;
        displayedProgress = continuingIntegratedLaunch
                ? Math.max(0.88F, integratedDisplayedProgress)
                : 0.02F;
        progress.reset(now);
        if (!continuingIntegratedLaunch) arc3dBar.reset(now);
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

        GlStateManager.disableLighting();
        GlStateManager.disableDepth();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA,
                GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);

        boolean snapshot = WorldLoadingSnapshotManager.INSTANCE.draw(width, height, alpha);
        if (!snapshot && mc.world == null) {
            Gui.drawRect(0, 0, width, height, scaledAlpha(0xFF11151B, alpha));
        } else if (!snapshot) {
            Gui.drawRect(0, 0, width, height, scaledAlpha(0x18000000, alpha));
        }
        if (WorldLoadingConfig.bottomShade) {
            drawVerticalGradient(0, Math.max(0, height * 3 / 5), width, height,
                    scaledAlpha(0x00000000, alpha), scaledAlpha(0xC8000000, alpha));
        }

        String label = translatedLoadingLabel(visualAmount) + animatedDots(now);
        float titleScale = width >= 700 ? 2.5F : width >= 420 ? 2.0F : 1.55F;
        GlStateManager.pushMatrix();
        GlStateManager.translate(margin, bottom - font.FONT_HEIGHT * titleScale, 0.0F);
        GlStateManager.scale(titleScale, titleScale, 1.0F);
        font.drawString(label, 0, 0, scaledAlpha(WorldLoadingConfig.textColor, alpha), false);
        GlStateManager.popMatrix();

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
        GlStateManager.disableBlend();
    }

    private static String translatedLoadingLabel(float amount) {
        String key = amount < 0.12F
                ? "neofontrender_ui_enhancements.loading.preparing_world"
                : amount < 0.97F
                ? "neofontrender_ui_enhancements.loading.loading_world"
                : "neofontrender_ui_enhancements.loading.finalizing";
        String translated = I18n.format(key);
        if (!key.equals(translated)) return translated;
        String fallbackKey = "neofontrender_ui_enhancements.loading.label";
        String fallback = I18n.format(fallbackKey);
        return fallbackKey.equals(fallback) ? "Loading" : fallback;
    }

    private static String animatedDots(long now) {
        int count = 1 + (int) ((now / 360_000_000L) % 3L);
        return count == 1 ? "." : count == 2 ? ".." : "...";
    }

    private static void drawVerticalGradient(int left, int top, int right, int bottom,
                                             int topColor, int bottomColor) {
        float topA = (topColor >>> 24) / 255.0F;
        float topR = (topColor >> 16 & 255) / 255.0F;
        float topG = (topColor >> 8 & 255) / 255.0F;
        float topB = (topColor & 255) / 255.0F;
        float bottomA = (bottomColor >>> 24) / 255.0F;
        float bottomR = (bottomColor >> 16 & 255) / 255.0F;
        float bottomG = (bottomColor >> 8 & 255) / 255.0F;
        float bottomB = (bottomColor & 255) / 255.0F;
        GlStateManager.disableTexture2D();
        GlStateManager.shadeModel(7425);
        BufferBuilder buffer = Tessellator.getInstance().getBuffer();
        buffer.begin(7, DefaultVertexFormats.POSITION_COLOR);
        buffer.pos(right, top, 0.0D).color(topR, topG, topB, topA).endVertex();
        buffer.pos(left, top, 0.0D).color(topR, topG, topB, topA).endVertex();
        buffer.pos(left, bottom, 0.0D).color(bottomR, bottomG, bottomB, bottomA).endVertex();
        buffer.pos(right, bottom, 0.0D).color(bottomR, bottomG, bottomB, bottomA).endVertex();
        Tessellator.getInstance().draw();
        GlStateManager.shadeModel(7424);
        GlStateManager.enableTexture2D();
    }

    private static int scaledAlpha(int color, float scale) {
        int alpha = Math.round((color >>> 24) * Math.max(0.0F, Math.min(1.0F, scale)));
        return color & 0x00FFFFFF | alpha << 24;
    }
}
