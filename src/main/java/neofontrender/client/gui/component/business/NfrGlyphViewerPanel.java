package neofontrender.client.gui.component.business;

import com.cleanroommc.modularui.api.layout.ILayoutWidget;
import com.cleanroommc.modularui.api.widget.Interactable;
import com.cleanroommc.modularui.screen.viewport.ModularGuiContext;
import com.cleanroommc.modularui.theme.WidgetThemeEntry;
import com.cleanroommc.modularui.utils.Platform;
import com.cleanroommc.modularui.widget.Widget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.resources.I18n;
import neofontrender.api.text.ModernTextApi;
import neofontrender.api.text.ModernTextLayout;

import java.util.Locale;

/**
 * A small font-viewer style glyph browser for the cache page.
 *
 * <p>The viewer intentionally uses code points rather than UTF-16 indices, so supplementary
 * plane characters can be inspected without splitting surrogate pairs. Range fields live in the
 * surrounding settings view; this component owns paging, presets and the visual grid.</p>
 */
public final class NfrGlyphViewerPanel extends Widget<NfrGlyphViewerPanel>
        implements ILayoutWidget, Interactable {
    private static final int COLUMNS = 8;
    private static final int ROWS = 4;
    private static final int PAGE_SIZE = COLUMNS * ROWS;
    private static final int CELL_HEIGHT = 58;
    private static final float GLYPH_SIZE = 22.0F;

    private int rangeStart = 0x20;
    private int rangeEnd = 0x7E;
    private int pageStart = rangeStart;
    private int selectedCodePoint = -1;
    private String startText = "U+0020";
    private String endText = "U+007E";
    private String status = "-";

    public int preferredHeight() {
        return 286;
    }

    public String startText() { return startText; }
    public String endText() { return endText; }

    public void setStartText(String value) {
        startText = value == null ? "" : value;
        Integer parsed = parseCodePoint(startText);
        if (parsed != null) updateParsedRange(parsed, rangeEnd);
    }

    public void setEndText(String value) {
        endText = value == null ? "" : value;
        Integer parsed = parseCodePoint(endText);
        if (parsed != null) updateParsedRange(rangeStart, parsed);
    }

    public void setRange(int start, int end) {
        int safeStart = clampCodePoint(start);
        int safeEnd = clampCodePoint(end);
        if (safeEnd < safeStart) {
            int swap = safeStart;
            safeStart = safeEnd;
            safeEnd = swap;
        }
        rangeStart = safeStart;
        rangeEnd = safeEnd;
        pageStart = rangeStart;
        selectedCodePoint = -1;
        startText = formatCodePoint(rangeStart);
        endText = formatCodePoint(rangeEnd);
        status = "-";
    }

    private void updateParsedRange(int start, int end) {
        int safeStart = clampCodePoint(start);
        int safeEnd = clampCodePoint(end);
        if (safeEnd < safeStart) {
            int swap = safeStart;
            safeStart = safeEnd;
            safeEnd = swap;
        }
        rangeStart = safeStart;
        rangeEnd = safeEnd;
        pageStart = rangeStart;
        selectedCodePoint = -1;
        status = "-";
    }

    public int rangeStart() { return rangeStart; }
    public int rangeEnd() { return rangeEnd; }

    public void presetAscii() { setRange(0x20, 0x7E); }
    public void presetLatin() { setRange(0x20, 0xFF); }
    public void presetCjkPunctuation() { setRange(0x3000, 0x303F); }
    public void presetCjk() { setRange(0x4E00, 0x9FFF); }

    public void previousPage() {
        pageStart = previousValidCodePoint(Math.max(rangeStart, pageStart - PAGE_SIZE));
    }

    public void nextPage() {
        int next = nextValidCodePoint(pageStart + PAGE_SIZE);
        if (next <= rangeEnd) pageStart = next;
    }

    public int pageStart() { return pageStart; }

    public void status(String value) {
        status = value == null || value.isEmpty() ? "-" : value;
    }

    @Override
    public boolean layoutWidgets() { return true; }

    @Override
    public void draw(ModularGuiContext context, WidgetThemeEntry<?> widgetTheme) {
        super.draw(context, widgetTheme);
        int width = Math.max(300, getArea().w());
        int height = Math.max(preferredHeight(), getArea().h());
        int right = Math.max(8, width - 4);
        Gui.drawRect(4, 4, right, height - 4, 0x66131A24);
        Gui.drawRect(4, 4, right, 6, 0xFF00AEB8);

        Platform.setupDrawFont();
        Minecraft minecraft = Minecraft.getMinecraft();
        FontRenderer font = minecraft.fontRenderer;
        font.drawString(I18n.format("neofontrender.gui.cache.glyph_viewer"), 10, 10, 0xFFE6ECF3);
        String range = String.format(Locale.ROOT, "%s - %s   %s: %d..%d",
                startText, endText,
                I18n.format("neofontrender.gui.cache.page"), pageStart, rangeEnd);
        font.drawString(trim(font, range, width - 20), 10, 27, 0xFFD4DCE8);

        int gridTop = 45;
        int gridWidth = Math.max(240, width - 20);
        int cellWidth = Math.max(42, gridWidth / COLUMNS);
        for (int index = 0; index < PAGE_SIZE; index++) {
            int codePoint = codePointAtPage(index);
            int column = index % COLUMNS;
            int row = index / COLUMNS;
            int left = 10 + column * cellWidth;
            int top = gridTop + row * CELL_HEIGHT;
            Gui.drawRect(left, top, left + cellWidth - 2, top + CELL_HEIGHT - 2,
                    codePoint < 0 ? 0x22131A24 : codePoint == selectedCodePoint
                            ? 0xAA31556B : 0x66303D4D);
            if (codePoint < 0) continue;
            String glyph = new String(Character.toChars(codePoint));
            drawGlyph(minecraft, glyph, left, top, cellWidth - 2);
            String label = String.format(Locale.ROOT, "U+%04X", codePoint);
            font.drawString(label, left + 3, top + CELL_HEIGHT - 15, 0xFF9DAABD);
        }
        font.drawString(trim(font, status, width - 20), 10,
                gridTop + ROWS * CELL_HEIGHT + 3, 0xFF8FE3CF);
    }

    private void drawGlyph(Minecraft minecraft, String glyph, int left, int top, int cellWidth) {
        if (ModernTextApi.isAvailable()) {
            try {
                ModernTextLayout layout = ModernTextApi.layout(glyph, GLYPH_SIZE, 0xFFFFFFFF);
                float x = left + Math.max(3.0F, (cellWidth - layout.advance()) * 0.5F);
                layout.draw(x, top + 10.0F);
                return;
            } catch (RuntimeException ignored) {
                // A malformed or unsupported font face falls through to Minecraft's fallback.
            }
        }
        minecraft.fontRenderer.drawString(glyph, left + 8, top + 13, 0xFFFFFFFF);
    }

    @Override
    public Interactable.Result onMousePressed(int mouseButton) {
        if (mouseButton != 0) return Interactable.Result.IGNORE;
        int mouseX = getContext().getMouseX();
        int mouseY = getContext().getMouseY();
        int width = Math.max(300, getArea().w());
        int cellWidth = Math.max(42, Math.max(240, width - 20) / COLUMNS);
        int column = (mouseX - 10) / cellWidth;
        int row = (mouseY - 45) / CELL_HEIGHT;
        if (column < 0 || column >= COLUMNS || row < 0 || row >= ROWS) {
            return Interactable.Result.IGNORE;
        }
        int index = row * COLUMNS + column;
        int codePoint = codePointAtPage(index);
        if (codePoint < 0) return Interactable.Result.IGNORE;
        selectedCodePoint = codePoint;
        status = String.format(Locale.ROOT, "U+%04X  %s", codePoint,
                new String(Character.toChars(codePoint)));
        return Interactable.Result.SUCCESS;
    }

    private int codePointAtPage(int index) {
        int value = pageStart;
        int remaining = index;
        while (remaining > 0 && value <= rangeEnd) {
            value = nextValidCodePoint(value + 1);
            remaining--;
        }
        return value <= rangeEnd ? value : -1;
    }

    private int nextValidCodePoint(int value) {
        int result = Math.max(0, value);
        while (result <= 0x10FFFF && result >= 0xD800 && result <= 0xDFFF) result++;
        return result;
    }

    private int previousValidCodePoint(int value) {
        int result = Math.min(0x10FFFF, value);
        while (result >= 0xD800 && result <= 0xDFFF) result--;
        return Math.max(0, result);
    }

    private static int clampCodePoint(int value) {
        return Math.max(0, Math.min(0x10FFFF, value));
    }

    private static Integer parseCodePoint(String raw) {
        if (raw == null) return null;
        String value = raw.trim().toLowerCase(Locale.ROOT);
        if (value.isEmpty()) return null;
        int radix = 10;
        if (value.startsWith("u+")) { value = value.substring(2); radix = 16; }
        else if (value.startsWith("0x")) { value = value.substring(2); radix = 16; }
        else if (value.matches(".*[a-f].*")) radix = 16;
        try {
            return clampCodePoint(Integer.parseInt(value, radix));
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static String formatCodePoint(int value) {
        return String.format(Locale.ROOT, "U+%04X", value);
    }

    private static String trim(FontRenderer font, String value, int width) {
        return font.trimStringToWidth(value == null ? "-" : value, Math.max(1, width));
    }
}
