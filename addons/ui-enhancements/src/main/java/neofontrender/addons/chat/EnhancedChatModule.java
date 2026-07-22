package neofontrender.addons.chat;

import cpw.mods.fml.common.FMLCommonHandler;
import neofontrender.addons.ui.UiEnhancementModule;
import neofontrender.api.client.settings.NfrSettingsPageRegistry;

/** Activates vanilla chat history, persistence, and smooth scrolling controls. */
public final class EnhancedChatModule implements UiEnhancementModule {
    @Override
    public void preInit() {
        EnhancedChatConfig.load();
        ChatHistoryManager.INSTANCE.initialize();
    }

    @Override
    public void init() {
        NfrSettingsPageRegistry.register(new EnhancedChatSettingsPage());
        FMLCommonHandler.instance().bus().register(ChatHistoryManager.INSTANCE);
    }
}
