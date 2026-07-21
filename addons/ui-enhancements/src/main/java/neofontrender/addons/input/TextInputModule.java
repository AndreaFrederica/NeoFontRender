package neofontrender.addons.input;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import neofontrender.addons.ui.UiEnhancementModule;
import neofontrender.api.client.settings.NfrSettingsPageRegistry;

public final class TextInputModule implements UiEnhancementModule {
    @Override public void preInit() { TextInputConfig.load(); }

    @Override public void init() {
        NfrSettingsPageRegistry.register(new TextInputSettingsPage());
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void beforeScreenDraw(GuiScreenEvent.DrawScreenEvent.Pre event) {
        TextCursorManager.beginFrame();
    }
}
