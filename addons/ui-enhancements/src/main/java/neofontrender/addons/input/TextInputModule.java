package neofontrender.addons.input;

import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.common.MinecraftForge;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import neofontrender.addons.ui.UiEnhancementModule;

/** Activates native text cursor tracking for vanilla text fields. */
public final class TextInputModule implements UiEnhancementModule {
    @Override
    public void preInit() {
        TextInputConfig.load();
    }

    @Override
    public void init() {
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void beforeScreenDraw(GuiScreenEvent.DrawScreenEvent.Pre event) {
        TextCursorManager.beginFrame();
    }

    @SubscribeEvent
    public void afterScreenDraw(GuiScreenEvent.DrawScreenEvent.Post event) {
        TextCursorManager.endFrame();
    }

    @SubscribeEvent
    public void screenChanged(GuiOpenEvent event) {
        if (event.gui == null) TextCursorManager.restoreDefault();
    }
}
