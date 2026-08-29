package mnm.mods.tabbychat.gui;

import mnm.mods.tabbychat.core.GuiNewChatTC;
import neofontrender.addons.chat.ChatHudWindowController;
import mnm.mods.util.gui.GuiComponent;
import net.minecraft.client.gui.Gui;
import neofontrender.addons.chat.ChatStyleConfig;
import neofontrender.addons.chat.ChatStyleRenderer;

public class Scrollbar extends GuiComponent {

    private ChatArea chat;

    public Scrollbar(ChatArea chat) {
        this.chat = chat;
    }

    @Override
    public void drawComponent(int mouseX, int mouseY) {
        if (ChatHudWindowController.isChatExpanded()) {
            int scroll = chat.getScrollPixels();
            int max = chat.getBounds().height;
            int total = chat.getMaximumScrollPixels();
            if (total <= 0) {
                return;
            }
            int content = chat.getContentPixelHeight();
            int size = Math.max(10, Math.min(max,
                    Math.round(max * (max / (float) Math.max(max, content)))));
            float progress = Math.max(0.0F, Math.min(1.0F, scroll / (float) total));
            int pos = Math.round((1.0F - progress) * (max - size));

            int color = ChatStyleConfig.enabled
                    ? ChatStyleRenderer.color(ChatStyleConfig.scrollbar, mc.gameSettings.chatOpacity) : -1;
            Gui.drawRect(0, pos, Math.max(1, ChatStyleConfig.enabled ? ChatStyleConfig.borderWidth : 1), pos + size, color);
            super.drawComponent(mouseX, mouseY);
        }
    }

}
