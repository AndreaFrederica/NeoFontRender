package neofontrender.addons.chat;

import cpw.mods.fml.common.FMLCommonHandler;
import net.minecraft.client.Minecraft;
import net.minecraftforge.common.MinecraftForge;
import neofontrender.addons.chat.network.SelfMessageCapability;
import neofontrender.addons.inline.InlineGlyphMiddleware;
import neofontrender.addons.ui.UiEnhancementModule;
import neofontrender.addons.vendor.tabbychat.TabbyChat;
import neofontrender.addons.vendor.tabbychat.host.MinecraftTabbyChatHost;
import neofontrender.api.client.settings.NfrSettingsPageRegistry;
import speiger.src.salutation.Salutation;

/** Activates vanilla chat history, persistence, and smooth scrolling controls. */
public final class EnhancedChatModule implements UiEnhancementModule {
    @Override
    public void preInit() {
        SelfMessageCapability.initialize();
        EnhancedChatConfig.load();
        EmojiAndImageConfig.load();
        ChatStyleConfig.load();
        // 1.7.10 builds its inline providers through the addon's own glyph registry; main's
        // replacement text pipeline lives in modules this branch does not build.
        InlineGlyphMiddleware.initialize();
        // Keep Salutation's command/translation facade initialized; its copied client UI was removed.
        Salutation.initialize();
        ChatHistoryManager.INSTANCE.initialize();
    }

    @Override
    public void init() {
        // The vendored TabbyChat must be started before any chat screen can open: MixinGuiChat
        // resolves TabbyChat.getInstance() while initialising. Startup creates GuiText fields,
        // so it must run at init when Minecraft.fontRenderer already exists (preInit is earlier).
        // An external TabbyChat mod owns the chat instead and keeps the embedded backend disabled.
        if (!ExternalChatCompat.tabbyChatLoaded() && !TabbyChat.isStarted()) {
            TabbyChat.start(new MinecraftTabbyChatHost(Minecraft.getMinecraft()));
        }
        NfrSettingsPageRegistry.register(new EnhancedChatSettingsPage());
        NfrSettingsPageRegistry.register(new ChatRulesSettingsPage());
        NfrSettingsPageRegistry.register(new EmojiAndImageSettingsPage());
        if (!ExternalChatCompat.tabbyChatLoaded()) NfrSettingsPageRegistry.register(new TabbedChatSettingsPage());
        if (!ExternalChatCompat.tabbyChatLoaded()) NfrSettingsPageRegistry.register(new ChatStyleSettingsPage());
        // Tick and network events are published on FML's bus in 1.7.10; Forge's bus only delivers
        // the client-side GUI and chat events.
        FMLCommonHandler.instance().bus().register(ChatHistoryManager.INSTANCE);
        FMLCommonHandler.instance().bus().register(ChatMessageProcessor.INSTANCE);
        FMLCommonHandler.instance().bus().register(ChatHudWindowController.INSTANCE);
        MinecraftForge.EVENT_BUS.register(ChatCopyController.INSTANCE);
        MinecraftForge.EVENT_BUS.register(ChatMessageProcessor.INSTANCE);
        MinecraftForge.EVENT_BUS.register(ChatSearchController.INSTANCE);
        MinecraftForge.EVENT_BUS.register(EmojiCompletionController.INSTANCE);
        MinecraftForge.EVENT_BUS.register(MentionCompletionController.INSTANCE);
        MinecraftForge.EVENT_BUS.register(ChatCommandCompletionController.INSTANCE);
        MinecraftForge.EVENT_BUS.register(ChatHudWindowController.INSTANCE);
        ChatKeyBindings.register();
    }
}
