package neofontrender.addons.chat;

import net.minecraft.client.gui.Gui;

public final class ChatStyleRenderer {
    private ChatStyleRenderer() {}

    public static void panel(int width, int height, int fill, int border, float minecraftOpacity) {
        if (width <= 0 || height <= 0) return;
        int borderWidth = Math.min(ChatStyleConfig.borderWidth, Math.min(width / 2, height / 2));
        int borderColor = ChatStyleConfig.withOpacity(border, minecraftOpacity);
        int fillColor = ChatStyleConfig.withOpacity(fill, minecraftOpacity);
        if (borderWidth > 0) Gui.drawRect(0, 0, width, height, borderColor);
        Gui.drawRect(borderWidth, borderWidth, width - borderWidth, height - borderWidth, fillColor);
    }

    public static int color(int color, float minecraftOpacity) {
        return ChatStyleConfig.withOpacity(color, minecraftOpacity);
    }
}
