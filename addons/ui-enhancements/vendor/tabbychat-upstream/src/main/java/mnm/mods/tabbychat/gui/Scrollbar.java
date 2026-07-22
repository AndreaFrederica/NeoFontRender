package mnm.mods.tabbychat.gui;

import mnm.mods.tabbychat.core.GuiNewChatTC;
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
        if (GuiNewChatTC.getInstance().getChatOpen()) {
            int scroll = chat.getScrollPos();
            int max = chat.getBounds().height;
            int lines = max / mc.fontRenderer.FONT_HEIGHT;
            int total = chat.getChat().size();
            if (total <= lines) {
                return;
            }
            total -= lines;
            int size = Math.max(max / 2 - total, 10);
            float perc = Math.abs((float) scroll / (float) total - 1) * Math.abs((float) size / (float) max - 1);
            int pos = (int) (perc * max);

            int color = ChatStyleConfig.enabled
                    ? ChatStyleRenderer.color(ChatStyleConfig.scrollbar, mc.gameSettings.chatOpacity) : -1;
            Gui.drawRect(0, pos, Math.max(1, ChatStyleConfig.enabled ? ChatStyleConfig.borderWidth : 1), pos + size, color);
            super.drawComponent(mouseX, mouseY);
        }
    }

}
