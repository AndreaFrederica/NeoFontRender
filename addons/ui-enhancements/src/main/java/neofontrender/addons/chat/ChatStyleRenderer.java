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

    /**
     * Draws one continuous panel whose rows fade independently from bottom to top.
     * Only the outer edge is bordered, so adjacent chat rows never double the border width.
     */
    public static void fadingPanel(int width, int height, int rowHeight, float[] bottomUpFade,
                                   int fill, int border, float minecraftOpacity) {
        if (width <= 0 || height <= 0 || rowHeight <= 0 || bottomUpFade.length == 0) return;
        int borderWidth = Math.min(ChatStyleConfig.borderWidth, Math.min(width / 2, height / 2));
        int interiorLeft = borderWidth;
        int interiorRight = width - borderWidth;
        int interiorTop = borderWidth;
        int interiorBottom = height - borderWidth;
        int contentTop = Math.max(0, height - bottomUpFade.length * rowHeight);

        float topFade = clamp(bottomUpFade[bottomUpFade.length - 1]);
        float bottomFade = clamp(bottomUpFade[0]);
        if (borderWidth > 0) {
            Gui.drawRect(0, 0, width, borderWidth,
                    withFade(border, minecraftOpacity, topFade));
            Gui.drawRect(0, interiorBottom, width, height,
                    withFade(border, minecraftOpacity, bottomFade));
        }

        int paddingBottom = Math.min(contentTop, interiorBottom);
        if (interiorTop < paddingBottom) {
            Gui.drawRect(interiorLeft, interiorTop, interiorRight, paddingBottom,
                    withFade(fill, minecraftOpacity, topFade));
            if (borderWidth > 0) {
                Gui.drawRect(0, interiorTop, borderWidth, paddingBottom,
                        withFade(border, minecraftOpacity, topFade));
                Gui.drawRect(interiorRight, interiorTop, width, paddingBottom,
                        withFade(border, minecraftOpacity, topFade));
            }
        }

        for (int row = 0; row < bottomUpFade.length; row++) {
            int y2 = height - row * rowHeight;
            int y1 = y2 - rowHeight;
            int clippedTop = Math.max(interiorTop, y1);
            int clippedBottom = Math.min(interiorBottom, y2);
            if (clippedTop >= clippedBottom) continue;
            float fade = clamp(bottomUpFade[row]);
            Gui.drawRect(interiorLeft, clippedTop, interiorRight, clippedBottom,
                    withFade(fill, minecraftOpacity, fade));
            if (borderWidth > 0) {
                Gui.drawRect(0, clippedTop, borderWidth, clippedBottom,
                        withFade(border, minecraftOpacity, fade));
                Gui.drawRect(interiorRight, clippedTop, width, clippedBottom,
                        withFade(border, minecraftOpacity, fade));
            }
        }
    }

    public static int color(int color, float minecraftOpacity) {
        return ChatStyleConfig.withOpacity(color, minecraftOpacity);
    }

    private static int withFade(int color, float minecraftOpacity, float fade) {
        return ChatStyleConfig.withOpacity(color, minecraftOpacity * fade);
    }

    private static float clamp(float value) {
        return Math.max(0.0F, Math.min(1.0F, value));
    }
}
