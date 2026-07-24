package neofontrender.addons.chat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiNewChat;
import neofontrender.addons.mixin.tabbychat.IGuiIngame;
import neofontrender.addons.vendor.tabbychat.TabbyChat;
import neofontrender.addons.vendor.tabbychat.core.GuiNewChatTC;

import java.util.ArrayList;
import java.util.List;

/** Applies the enhanced-chat master switch to the live GuiIngame instance. */
final class ChatRuntimeController {
    private ChatRuntimeController() {}

    static void sync() {
        if (ExternalChatCompat.tabbyChatLoaded()) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.ingameGUI == null) return;
        GuiNewChat current = mc.ingameGUI.getChatGUI();
        boolean wantTabbed = EnhancedChatConfigAccess.tabbedChatEnabled();
        boolean hasTabbed = current instanceof GuiNewChatTC;
        if (wantTabbed == hasTabbed) return;

        List<String> sent = new ArrayList<>(current.getSentMessages());
        GuiNewChat replacement = wantTabbed ? TabbyChat.getInstance().getChatGui() : new GuiNewChat(mc);
        replacement.getSentMessages().clear();
        replacement.getSentMessages().addAll(sent);
        ((IGuiIngame) mc.ingameGUI).setPersistantChatGUI(replacement);
        if (wantTabbed) ChatHistoryManager.INSTANCE.scheduleRestore();
    }
}
