package neofontrender.addons.tooltips;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.IReloadableResourceManager;
import neofontrender.addons.ui.NfrUiEnhancements;
import neofontrender.addons.build.UiBuildFeatures;
import neofontrender.addons.ui.UiEnhancementModule;
import neofontrender.api.client.settings.NfrSettingsPageRegistry;
import net.minecraftforge.fml.common.Loader;
import org.apache.logging.log4j.Logger;
import neofontrender.addons.notifications.CompatibilityNotificationPrompt;
import neofontrender.addons.notifications.CompatibilityNotificationsSettingsPage;

/** Modern tooltip feature module and its independent settings-page registration. */
public final class TooltipModule implements UiEnhancementModule {
    static final Logger LOGGER = NfrUiEnhancements.LOGGER;

    @Override
    public void preInit() {
        TooltipConfig.load();
        TooltipPreviewRenderers.initialize();
        PreviewStyleRegistry.INSTANCE.initialize();
        if (UiBuildFeatures.DIAGNOSTIC_LOGS) {
            LOGGER.info("Tooltip module preInit; enabled={}, style={}", TooltipConfig.enabled, TooltipConfig.renderStyle);
        }
        Arc3DRuntimeSupport.verify();
    }

    @Override
    public void init() {
        neofontrender.api.client.tooltip.NfrTooltipApi.register(
                TooltipDocumentRenderer::render);
        ((IReloadableResourceManager) Minecraft.getMinecraft().getResourceManager())
                .registerReloadListener(LegendaryResourceCompat.INSTANCE);
        ((IReloadableResourceManager) Minecraft.getMinecraft().getResourceManager())
                .registerReloadListener(PreviewStyleRegistry.INSTANCE);
        NfrSettingsPageRegistry.register(new ModernTooltipSettingsPage());
        CompatibilityNotificationPrompt.register();
        NfrSettingsPageRegistry.register(new CompatibilityNotificationsSettingsPage());
        // The public API has no AE2 dependency. Install the optional AE2 source
        // only when that mod is present; all other visual sources remain usable.
        if (Loader.isModLoaded("ae2")) NfrAe2TooltipApi.register();
        neofontrender.api.client.tooltip.NfrTooltipApi.registerDocumentFinalizer(
                ModNameTooltipHandler::finalizeDocument);
        neofontrender.api.client.tooltip.NfrTooltipApi.registerDocumentProvider(
                BuiltinPreviewProvider.INSTANCE);
        if (UiBuildFeatures.DIAGNOSTIC_LOGS) {
            LOGGER.info("Registered Revo UI tooltip settings page: {}", NfrUiEnhancements.MOD_ID + ":tooltips");
        }
        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.register(new AdvancedTooltipHandler());
        MinecraftForge.EVENT_BUS.register(new ModernTooltipHandler());
    }

    /** Preserve the original Mica source immediately before the current GuiScreen is drawn. */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void captureOriginalMicaScene(GuiScreenEvent.DrawScreenEvent.Pre event) {
        if (!TooltipConfig.micaSampleUi
                && !TooltipConfig.lowBrightnessMicaEnhancement && isMicaEnabled()) {
            MicaBackdrop.captureScene();
        }
    }

    /** Preserve world and HUD before UIE's screen gradient for low-brightness enhancement. */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void captureMicaSceneAfterHud(RenderGameOverlayEvent.Post event) {
        if (TooltipConfig.micaSampleUi) return;
        if (!TooltipConfig.lowBrightnessMicaEnhancement) return;
        if (event.getType() != RenderGameOverlayEvent.ElementType.ALL) return;
        if (isMicaEnabled()) MicaBackdrop.captureScene();
    }

    @SubscribeEvent
    public void screenChanged(GuiOpenEvent event) {
        MicaBackdrop.invalidateScene();
    }

    @SubscribeEvent
    public void worldUnloaded(WorldEvent.Unload event) {
        if (event.getWorld() != null && event.getWorld().isRemote) {
            TooltipPreviewRenderers.releaseWorld(event.getWorld());
        }
    }

    private static boolean isMicaEnabled() {
        return TooltipConfig.enabled && "mica".equals(TooltipConfig.renderStyle);
    }
}
