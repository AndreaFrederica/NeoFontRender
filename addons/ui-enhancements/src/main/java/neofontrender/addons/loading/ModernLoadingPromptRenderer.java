package neofontrender.addons.loading;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import neofontrender.addons.tooltips.AddonI18n;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Modern presentation for Forge startup queries shown while a world is loading. */
public final class ModernLoadingPromptRenderer {
    private static final int MARGIN = 12;
    private static final int PANEL_MAX_WIDTH = 520;
    private static final int HEADER_HEIGHT = 39;
    private static final int FOOTER_HEIGHT = 45;
    private static final int BODY_LINE_HEIGHT = 11;

    private ModernLoadingPromptRenderer() {}

    public static Layout layout(FontRenderer font, String text, int screenWidth, int screenHeight) {
        int panelWidth = Math.max(180, Math.min(PANEL_MAX_WIDTH, screenWidth - MARGIN * 2));
        int bodyWidth = Math.max(80, panelWidth - 28);
        List<String> lines = wrap(font, text, bodyWidth);
        int availableBody = Math.max(BODY_LINE_HEIGHT,
                screenHeight - MARGIN * 2 - HEADER_HEIGHT - FOOTER_HEIGHT);
        int visibleLines = Math.max(1, Math.min(lines.size(), availableBody / BODY_LINE_HEIGHT));
        int panelHeight = HEADER_HEIGHT + visibleLines * BODY_LINE_HEIGHT + FOOTER_HEIGHT;
        int left = (screenWidth - panelWidth) / 2;
        int top = Math.max(MARGIN, (screenHeight - panelHeight) / 2);
        return new Layout(left, top, panelWidth, panelHeight, bodyWidth, lines, visibleLines);
    }

    public static void draw(FontRenderer font, String text, boolean confirmation,
                            int screenWidth, int screenHeight, int scrollLine) {
        WorldLoadingRenderer.INSTANCE.renderPromptBackdrop(screenWidth, screenHeight);
        Layout layout = layout(font, text, screenWidth, screenHeight);
        int scroll = Math.max(0, Math.min(layout.maxScroll(), scrollLine));
        int right = layout.left + layout.width;
        int bottom = layout.top + layout.height;

        Gui.drawRect(layout.left - 3, layout.top - 3, right + 3, bottom + 3, 0x70000000);
        Gui.drawRect(layout.left, layout.top, right, bottom, 0xFF3B444E);
        Gui.drawRect(layout.left + 1, layout.top + 1, right - 1, bottom - 1, 0xF5161B20);
        Gui.drawRect(layout.left + 1, layout.top + 1, right - 1, layout.top + 3,
                WorldLoadingConfig.accentColor);

        String titleKey = confirmation
                ? "neofontrender_ui_enhancements.loading.prompt.confirmation"
                : "neofontrender_ui_enhancements.loading.prompt.notification";
        font.drawString(AddonI18n.tr(titleKey), layout.left + 14, layout.top + 15,
                0xFFF4F7FA, false);
        Gui.drawRect(layout.left + 14, layout.top + HEADER_HEIGHT - 1,
                right - 14, layout.top + HEADER_HEIGHT, 0xFF303841);

        int y = layout.top + HEADER_HEIGHT + 5;
        for (int index = 0; index < layout.visibleLines; index++) {
            int lineIndex = scroll + index;
            if (lineIndex >= layout.lines.size()) break;
            String line = layout.lines.get(lineIndex);
            if (!line.isEmpty()) font.drawString(line, layout.left + 14, y, 0xFFD5DBE2, false);
            y += BODY_LINE_HEIGHT;
        }

        if (layout.maxScroll() > 0) {
            int trackTop = layout.top + HEADER_HEIGHT + 5;
            int trackBottom = layout.top + layout.height - FOOTER_HEIGHT - 5;
            int trackHeight = Math.max(1, trackBottom - trackTop);
            int thumbHeight = Math.max(10, trackHeight * layout.visibleLines / layout.lines.size());
            int thumbRange = Math.max(0, trackHeight - thumbHeight);
            int thumbTop = trackTop + (layout.maxScroll() == 0 ? 0
                    : thumbRange * scroll / layout.maxScroll());
            Gui.drawRect(right - 7, trackTop, right - 5, trackBottom, 0xFF293038);
            Gui.drawRect(right - 7, thumbTop, right - 5, thumbTop + thumbHeight,
                    WorldLoadingConfig.accentColor);
        }

        Gui.drawRect(layout.left + 14, bottom - FOOTER_HEIGHT,
                right - 14, bottom - FOOTER_HEIGHT + 1, 0xFF303841);
    }

    private static List<String> wrap(FontRenderer font, String text, int width) {
        if (text == null || text.isEmpty()) return Collections.singletonList("");
        List<String> result = new ArrayList<>();
        String[] paragraphs = text.split("\\n", -1);
        for (String paragraph : paragraphs) {
            if (paragraph.isEmpty()) {
                result.add("");
                continue;
            }
            List<String> wrapped = font.listFormattedStringToWidth(paragraph, width);
            if (wrapped.isEmpty()) result.add("");
            else result.addAll(wrapped);
        }
        return result;
    }

    public static final class Layout {
        public final int left;
        public final int top;
        public final int width;
        public final int height;
        public final int bodyWidth;
        public final List<String> lines;
        public final int visibleLines;

        private Layout(int left, int top, int width, int height, int bodyWidth,
                       List<String> lines, int visibleLines) {
            this.left = left;
            this.top = top;
            this.width = width;
            this.height = height;
            this.bodyWidth = bodyWidth;
            this.lines = Collections.unmodifiableList(lines);
            this.visibleLines = visibleLines;
        }

        public int maxScroll() {
            return Math.max(0, lines.size() - visibleLines);
        }

        public int buttonY() {
            return top + height - 33;
        }
    }
}
