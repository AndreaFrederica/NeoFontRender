package neofontrender.addons.tooltips;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.Optional;
import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.IReloadableResourceManager;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.WorldEvent;
import neofontrender.addons.build.UiBuildFeatures;
import neofontrender.addons.notifications.CompatibilityNotificationPrompt;
import neofontrender.addons.notifications.CompatibilityNotificationsSettingsPage;
import neofontrender.addons.ui.NfrUiEnhancements;
import neofontrender.addons.ui.UiEnhancementModule;
import neofontrender.api.client.settings.NfrSettingsPageRegistry;
import neofontrender.api.client.tooltip.NfrTooltipApi;
import org.apache.logging.log4j.Logger;

/** Modern tooltip feature module and its independent settings-page registration. */
public final class TooltipModule implements UiEnhancementModule {
    static final Logger LOGGER = NfrUiEnhancements.LOGGER;

    @Override
    public void preInit() {
        TooltipConfig.load();
        TooltipPreviewRenderers.initialize();
        PreviewStyleRegistry.INSTANCE.initialize();
        if (UiBuildFeatures.DIAGNOSTIC_LOGS) {
            LOGGER.info("Tooltip module preInit; enabled={}, style={}",
                    TooltipConfig.enabled, TooltipConfig.renderStyle);
        }
        Arc3DRuntimeSupport.verify();
    }

    @Override
    public void init() {
        NfrSettingsPageRegistry.register(new ModernTooltipSettingsPage());
    }

    /** Registers hooks that require an initialized Forge client runtime. */
    public static void registerRuntimeHooks() {
        ItemZoomKeyBindings.register();
        NfrTooltipApi.register(TooltipDocumentRenderer::render);
        ((IReloadableResourceManager) Minecraft.getMinecraft().getResourceManager())
                .registerReloadListener(PreviewStyleRegistry.INSTANCE);
        NfrTooltipApi.registerDocumentFinalizer(ModNameTooltipHandler::finalizeDocument);
        NfrTooltipApi.registerDocumentProvider(BuiltinPreviewProvider.INSTANCE);
        CompatibilityNotificationPrompt.register();
        NfrSettingsPageRegistry.register(new CompatibilityNotificationsSettingsPage());
        if (UiBuildFeatures.DIAGNOSTIC_LOGS) {
            LOGGER.info("Registered Revo UI tooltip settings page: {}",
                    NfrUiEnhancements.MOD_ID + ":tooltips");
        }
        MinecraftForge.EVENT_BUS.register(new TooltipModule());
        MinecraftForge.EVENT_BUS.register(new ModNameTooltipHandler());
        MinecraftForge.EVENT_BUS.register(new ItemZoomOverlay());
        MinecraftForge.EVENT_BUS.register(new AdvancedTooltipHandler());
        if (Loader.isModLoaded("NotEnoughItems")) {
            registerNeiCompatIfGtnhLibLoaded();
        }
    }

    @Optional.Method(modid = "NotEnoughItems")
    private static void registerNeiCompatIfGtnhLibLoaded() {
        if (Loader.isModLoaded("gtnhlib")) registerNeiCompat();
    }

    @Optional.Method(modid = "gtnhlib")
    private static void registerNeiCompat() {
        MinecraftForge.EVENT_BUS.register(new NeiTooltipCompat());
    }

    /** Preserves the original Mica source immediately before the current GuiScreen is drawn. */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void captureOriginalMicaScene(GuiScreenEvent.DrawScreenEvent.Pre event) {
        if (!TooltipConfig.lowBrightnessMicaEnhancement && isMicaEnabled()) {
            MicaBackdrop.captureScene();
        }
    }

    /** Preserves world and HUD before UIE's screen gradient for low-brightness enhancement. */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void captureMicaSceneAfterHud(RenderGameOverlayEvent.Post event) {
        if (!TooltipConfig.lowBrightnessMicaEnhancement) return;
        if (event.type != RenderGameOverlayEvent.ElementType.ALL) return;
        if (isMicaEnabled()) MicaBackdrop.captureScene();
    }

    @SubscribeEvent
    public void screenChanged(GuiOpenEvent event) {
        MicaBackdrop.invalidateScene();
        ItemZoomOverlay.reset();
    }

    @SubscribeEvent
    public void worldUnloaded(WorldEvent.Unload event) {
        if (event.world != null && event.world.isRemote) {
            TooltipPreviewRenderers.releaseWorld(event.world);
        }
    }

    private static boolean isMicaEnabled() {
        return TooltipConfig.enabled && "mica".equals(TooltipConfig.renderStyle);
    }
}
