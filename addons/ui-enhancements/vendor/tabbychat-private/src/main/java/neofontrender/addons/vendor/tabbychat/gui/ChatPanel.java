package neofontrender.addons.vendor.tabbychat.gui;

import neofontrender.addons.vendor.tabbychat.core.GuiNewChatTC;
import neofontrender.addons.vendor.tabbychat.foundation.gui.GuiPanel;
import neofontrender.addons.vendor.tabbychat.foundation.gui.ILayout;

public class ChatPanel extends GuiPanel {

    public ChatPanel() {
        super();
    }

    public ChatPanel(ILayout layout) {
        super(layout);
    }

    @Override
    public boolean isVisible() {
        return super.isVisible() && GuiNewChatTC.getInstance().getChatOpen();
    }
}
