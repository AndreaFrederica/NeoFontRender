package neofontrender.addons.input;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.IReloadableResourceManager;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import neofontrender.addons.ui.UiEnhancementModule;
import neofontrender.addons.cursor.CursorConfig;
import neofontrender.addons.cursor.CursorAssetCatalog;
import neofontrender.addons.cursor.CursorManager;
import neofontrender.addons.cursor.CursorSettingsPage;
import neofontrender.api.client.settings.NfrSettingsPageRegistry;
import org.lwjgl.opengl.Display;

public final class TextInputModule implements UiEnhancementModule {
    @Override public void preInit() {
        CursorConfig.load();
    }

    @Override public void init() {
        NfrSettingsPageRegistry.register(new CursorSettingsPage());
        if (Minecraft.getMinecraft().getResourceManager() instanceof IReloadableResourceManager) {
            ((IReloadableResourceManager) Minecraft.getMinecraft().getResourceManager())
                    .registerReloadListener(CursorAssetCatalog.INSTANCE);
        }
        CursorAssetCatalog.INSTANCE.refresh();
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
        if (event.getGui() == null) TextCursorManager.restoreDefault();
    }

    @SubscribeEvent
    public void clientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END && !Display.isActive()) CursorManager.restoreDefault();
    }

}
