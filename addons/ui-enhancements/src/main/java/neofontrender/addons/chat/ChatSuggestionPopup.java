package neofontrender.addons.chat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.renderer.GlStateManager;

import java.util.List;

/** Shared suggestion popup used by Salutation command completion and @player completion. */
public final class ChatSuggestionPopup {
    public static final int ROW_HEIGHT = 12;
    public static final int MAX_VISIBLE = 10;

    private ChatSuggestionPopup() {}

    public static Layout draw(GuiTextField input, List<String> candidates, int first, int selected,
                              ExternalChatCompat.InputGeometry tabbyGeometry,
                              int mouseX, int mouseY, FontRenderer font) {
        return draw(input, candidates, first, selected, tabbyGeometry, mouseX, mouseY, font, false);
    }

    public static Layout draw(GuiTextField input, List<String> candidates, int first, int selected,
                              ExternalChatCompat.InputGeometry tabbyGeometry,
                              int mouseX, int mouseY, FontRenderer font, boolean playerHeads) {
        if (input == null || candidates == null || candidates.isEmpty()) return Layout.EMPTY;
        int safeFirst = Math.max(0, Math.min(first, candidates.size() - 1));
        int rows = Math.min(candidates.size() - safeFirst, MAX_VISIBLE);
        if (rows <= 0) return Layout.EMPTY;

        Minecraft minecraft = Minecraft.getMinecraft();
        int maxTextWidth = 0;
        for (String candidate : candidates) maxTextWidth = Math.max(maxTextWidth, font.getStringWidth(candidate));
        int contentOffset = playerHeads ? ChatHeadRenderer.TEXT_OFFSET : 0;
        int panelWidth = Math.min(maxTextWidth + 8 + contentOffset, minecraft.currentScreen.width);
        int panelHeight = rows * ROW_HEIGHT + 2;

        int inputX = tabbyGeometry == null ? input.x : tabbyGeometry.x;
        int inputY = tabbyGeometry == null ? input.y : tabbyGeometry.y;
        int inputWidth = tabbyGeometry == null ? input.width : tabbyGeometry.width;
        int inputHeight = tabbyGeometry == null ? input.height : tabbyGeometry.height;
        float scale = tabbyGeometry == null ? 1.0F : tabbyGeometry.scale;
        int cursor = Math.min(input.getCursorPosition(), input.getText().length());
        String beforeCursor = input.getText().substring(0, Math.max(0, cursor));
        int wordStart = beforeCursor.length();
        while (wordStart > 0 && !Character.isWhitespace(beforeCursor.charAt(wordStart - 1))) wordStart--;
        int prefixWidth = Math.round(font.getStringWidth(beforeCursor.substring(0, wordStart)) * scale);
        int panelX = Math.max(0, Math.min(inputX + Math.min(prefixWidth, inputWidth),
                minecraft.currentScreen.width - panelWidth));
        int panelY = inputY - panelHeight - 3;
        if (panelY < 0) panelY = Math.min(minecraft.currentScreen.height - panelHeight,
                inputY + inputHeight + 3);

        Layout layout = new Layout(panelX, panelY, panelWidth, panelHeight, rows);
        int hovered = layout.rowAt(mouseX, mouseY);
        GlStateManager.pushMatrix();
        GlStateManager.translate(panelX, panelY, 600.0F);
        GlStateManager.disableDepth();
        GlStateManager.enableBlend();
        int textColor;
        int highlight;
        if (tabbyGeometry != null) {
            ChatStyleRenderer.panel(panelWidth, panelHeight,
                    ChatStyleConfig.inputBackground, ChatStyleConfig.border,
                    minecraft.gameSettings.chatOpacity);
            textColor = ChatStyleRenderer.color(ChatStyleConfig.text, minecraft.gameSettings.chatOpacity);
            highlight = ChatStyleRenderer.color(ChatStyleConfig.hoveredTab, minecraft.gameSettings.chatOpacity);
        } else {
            Gui.drawRect(0, 0, panelWidth, panelHeight, 0xB0101010);
            Gui.drawRect(0, 0, panelWidth, 1, 0xC0606060);
            Gui.drawRect(0, panelHeight - 1, panelWidth, panelHeight, 0xC0606060);
            textColor = 0xFFFFFFFF;
            highlight = 0xC0505050;
        }
        for (int row = 0; row < rows; row++) {
            int candidate = safeFirst + row;
            if (row == hovered || candidate == selected) {
                Gui.drawRect(1, row * ROW_HEIGHT + 1,
                        panelWidth - 1, (row + 1) * ROW_HEIGHT + 1, highlight);
            }
            String candidateText = candidates.get(candidate);
            if (playerHeads) {
                ChatHeadRenderer.renderCandidate(candidateText, 3, row * ROW_HEIGHT + 3, 1.0F);
            }
            int textX = 4 + contentOffset;
            String value = font.trimStringToWidth(candidateText, panelWidth - textX - 3);
            font.drawStringWithShadow(value, textX, row * ROW_HEIGHT + 3, textColor);
        }
        GlStateManager.enableDepth();
        GlStateManager.popMatrix();
        return layout;
    }

    public static final class Layout {
        private static final Layout EMPTY = new Layout(0, 0, 0, 0, 0);
        private final int x;
        private final int y;
        private final int width;
        private final int height;
        private final int rows;

        private Layout(int x, int y, int width, int height, int rows) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.rows = rows;
        }

        public int rowAt(int mouseX, int mouseY) {
            if (rows <= 0 || mouseX < x || mouseX > x + width
                    || mouseY < y + 1 || mouseY >= y + height - 1) return -1;
            int row = (mouseY - y - 1) / ROW_HEIGHT;
            return row < rows ? row : -1;
        }

        public boolean isVisible() { return rows > 0; }
    }
}
