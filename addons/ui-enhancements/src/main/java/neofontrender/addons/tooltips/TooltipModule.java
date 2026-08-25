package neofontrender.addons.tooltips;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import neofontrender.addons.ui.NfrUiEnhancements;
import neofontrender.addons.ui.UiEnhancementModule;
import neofontrender.api.client.settings.NfrSettingsPageRegistry;
import org.apache.logging.log4j.Logger;

/** Modern tooltip feature module and its independent settings-page registration. */
public final class TooltipModule implements UiEnhancementModule {
    static final Logger LOGGER = NfrUiEnhancements.LOGGER;

    @Override
    public void preInit() {
        TooltipConfig.load();
        Arc3DRuntimeSupport.verify();
    }

    @Override
    public void init() {
        NfrSettingsPageRegistry.register(new ModernTooltipSettingsPage());
        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.register(new ModNameTooltipHandler());
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

    private static boolean isMicaEnabled() {
        return TooltipConfig.enabled && "mica".equals(TooltipConfig.renderStyle);
    }
}
