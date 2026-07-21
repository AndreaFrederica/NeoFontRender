package neofontrender.addons.chat;

import net.minecraftforge.common.MinecraftForge;
import neofontrender.addons.ui.UiEnhancementModule;
import neofontrender.api.client.settings.NfrSettingsPageRegistry;

public final class EnhancedChatModule implements UiEnhancementModule {
    @Override
    public void preInit() {
        EnhancedChatConfig.load();
        ChatHistoryManager.INSTANCE.initialize();
    }

    @Override
    public void init() {
        NfrSettingsPageRegistry.register(new EnhancedChatSettingsPage());
        MinecraftForge.EVENT_BUS.register(ChatHistoryManager.INSTANCE);
    }
}
