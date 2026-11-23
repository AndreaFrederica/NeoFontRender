package neofontrender.client.gui.component.base;

import com.cleanroommc.modularui.screen.viewport.ModularGuiContext;
import com.cleanroommc.modularui.theme.WidgetThemeEntry;
import com.cleanroommc.modularui.utils.Platform;
import com.cleanroommc.modularui.widget.Widget;
import net.minecraft.client.Minecraft;

/** Reusable titled separator for grouping related settings controls. */
public final class NfrSectionDivider extends Widget<NfrSectionDivider>
        implements NfrPreferredHeight, NfrFullRowWidget {
    private final String title;
    private final int level;

    public NfrSectionDivider(String title) {
        this(title, 1);
    }

    public NfrSectionDivider(String title, int level) {
        this.title = title == null ? "" : title;
        this.level = Math.max(1, Math.min(2, level));
    }

    @Override public int preferredHeight() { return 22; }

    @Override public void draw(ModularGuiContext context, WidgetThemeEntry<?> theme) {
        Platform.setupDrawFont();
        Minecraft mc = Minecraft.getMinecraft();
        int y = Math.max(0, (getArea().h() - mc.fontRenderer.FONT_HEIGHT) / 2);
        int textWidth = mc.fontRenderer.getStringWidth(title);
        int lineY = y + mc.fontRenderer.FONT_HEIGHT / 2;
        int lineColor = level == 1 ? 0x805D7185 : 0x604D6070;
        int textColor = level == 1 ? 0xFFD4E0EC : 0xFFB5C0CC;
        int left = level == 1 ? 4 : 14;
        int gap = level == 1 ? 7 : 6;
        if (textWidth > 0) {
            mc.fontRenderer.drawString(title, left, y, textColor);
            left += textWidth + gap;
        }
        if (left < getArea().w() - 4) {
            com.cleanroommc.modularui.drawable.Rectangle line =
                    new com.cleanroommc.modularui.drawable.Rectangle().color(lineColor);
            line.draw(context, left, lineY, Math.max(1, getArea().w() - left - 4), 1, theme.getTheme());
        }
    }
}
