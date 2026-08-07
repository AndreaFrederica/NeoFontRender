package neofontrender.addons.chat;

import com.cleanroommc.modularui.api.layout.ILayoutWidget;
import com.cleanroommc.modularui.api.widget.Interactable;
import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.drawable.Rectangle;
import com.cleanroommc.modularui.screen.viewport.ModularGuiContext;
import com.cleanroommc.modularui.theme.WidgetThemeEntry;
import com.cleanroommc.modularui.utils.Platform;
import com.cleanroommc.modularui.widget.ParentWidget;
import com.cleanroommc.modularui.widget.sizer.Area;
import com.cleanroommc.modularui.widget.sizer.AreaResizer;
import com.cleanroommc.modularui.widgets.ListWidget;
import com.cleanroommc.modularui.widgets.menu.ContextMenuButton;
import com.cleanroommc.modularui.widgets.menu.Menu;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import neofontrender.addons.tooltips.AddonI18n;
import neofontrender.client.gui.component.base.NfrLayout;
import neofontrender.client.gui.component.base.NfrTextButton;

import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * NFR-styled history row with a native ModularUI context menu and a real delete button.
 */
public final class NfrHistoryRow extends ParentWidget<NfrHistoryRow> implements ILayoutWidget {
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");
    private static final int ROW_HEIGHT = 22;
    private static final int PAD = 4;
    private static final int DELETE_WIDTH = 20;
    private static final int MENU_WIDTH = 96;
    private static final int MENU_ROW_HEIGHT = 20;
    private static final int MENU_BACKGROUND = 0xC8000000;
    private static final int MENU_BORDER = 0x8064748B;
    private static final int MENU_HOVER = 0xB8333333;

    private final String time;
    private final String badge;
    private final int badgeColor;
    private final String text;
    private final MessageButton messageButton;
    private final NfrTextButton deleteButton;

    public NfrHistoryRow(long timestamp, ChatSource source, String player, String group,
                         String text, Runnable copy, Runnable delete) {
        this.time = TIME.format(Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()));
        this.badge = badge(source, player, group);
        this.badgeColor = badgeColor(source);
        this.text = text == null ? "" : text;
        disableThemeBackground(true);
        messageButton = new MessageButton(copy, delete);
        deleteButton = new NfrTextButton(() -> "x", true)
                .disableThemeBackground(true)
                .onMousePressed(mouseButton -> {
                    if (mouseButton != 0) return false;
                    delete.run();
                    return true;
                });
        child(messageButton);
        child(deleteButton);
    }

    @Override
    public boolean layoutWidgets() {
        int width = getArea().w();
        NfrLayout.place(messageButton, 0, 0, Math.max(0, width - DELETE_WIDTH), ROW_HEIGHT);
        NfrLayout.place(deleteButton, Math.max(0, width - DELETE_WIDTH), 2,
                Math.min(DELETE_WIDTH, width), ROW_HEIGHT - 4);
        return true;
    }

    @Override
    public void draw(ModularGuiContext context, WidgetThemeEntry<?> theme) {
        super.draw(context, theme);
    }

    private final class MessageButton extends ContextMenuButton<MessageButton> {
        private final Runnable copy;
        private final Runnable delete;

        private MessageButton(Runnable copy, Runnable delete) {
            super("nfr_history_row_menu");
            this.copy = copy;
            this.delete = delete;
            requiresClick();
            openCustom();
            disableThemeBackground(true);
        }

        @Override
        public void openMenu(boolean soft) {
            if (!isOpen()) setMenu(createFreshMenu());
            super.openMenu(soft);
        }

        @Override
        public @NotNull Interactable.Result onMousePressed(int mouseButton) {
            if (mouseButton == 1) {
                openMenu(false);
                return Interactable.Result.SUCCESS;
            }
            if (mouseButton == 0) {
                copy.run();
                return Interactable.Result.SUCCESS;
            }
            return Interactable.Result.IGNORE;
        }

        private Menu<?> createFreshMenu() {
            ListWidget<IWidget, ?> options = new ListWidget<>()
                    .widthRel(1f)
                    .background(new Rectangle().color(MENU_BACKGROUND));
            options.child(menuOption("copy", copy));
            options.child(menuOption("delete", delete));

            Menu<?> menu = new Menu<>()
                    .width(MENU_WIDTH)
                    .coverChildrenHeight()
                    .background(new Rectangle().color(MENU_BORDER))
                    .padding(1)
                    .child(options);
            menu.resizer().relative(new AreaResizer(menuAnchor()));
            return menu;
        }

        private NfrTextButton menuOption(String key, Runnable action) {
            return new NfrTextButton(() -> AddonI18n.tr(
                    "neofontrender_ui_enhancements.gui.history." + key), false)
                    .height(MENU_ROW_HEIGHT)
                    .background(new Rectangle().color(0x00000000))
                    .hoverBackground(new Rectangle().color(MENU_HOVER))
                    .onMousePressed(mouseButton -> {
                        if (mouseButton != 0) return false;
                        closeMenu(false);
                        action.run();
                        return true;
                    });
        }

        private Area menuAnchor() {
            ModularGuiContext context = getContext();
            Minecraft minecraft = Minecraft.getMinecraft();
            ScaledResolution resolution = new ScaledResolution(minecraft, minecraft.displayWidth, minecraft.displayHeight);
            int mouseX = context == null ? getArea().x() : context.getMouseX();
            int mouseY = context == null ? getArea().y() : context.getMouseY();
            int height = MENU_ROW_HEIGHT * 2 + 2;
            int x = Math.max(1, Math.min(mouseX + 2, resolution.getScaledWidth() - MENU_WIDTH - 1));
            int y = Math.max(1, Math.min(mouseY + 2, resolution.getScaledHeight() - height - 1));
            return new Area(x, y, 0, 0);
        }

        @Override
        public void draw(ModularGuiContext context, WidgetThemeEntry<?> theme) {
        Minecraft mc = Minecraft.getMinecraft();
        int w = getArea().w();
        boolean hovered = context.isHovered(this);
        if (hovered) Gui.drawRect(0, 1, w, ROW_HEIGHT - 1, 0x22FFFFFF);
        Platform.setupDrawFont();
        int y = (ROW_HEIGHT - mc.fontRenderer.FONT_HEIGHT) / 2;
        mc.fontRenderer.drawString(time, PAD, y + 1, 0xFF9AA5B1);
        int badgeX = PAD + mc.fontRenderer.getStringWidth(time) + 6;
        mc.fontRenderer.drawString(badge, badgeX, y + 1, badgeColor);
        int textX = badgeX + mc.fontRenderer.getStringWidth(badge) + 6;
        String body = mc.fontRenderer.trimStringToWidth(text, Math.max(0, w - textX - 4));
        mc.fontRenderer.drawString(body, textX, y + 1, 0xFFE6EAF0);
        }
    }

    static int rowHeight() {
        return ROW_HEIGHT;
    }

    private static String badge(ChatSource source, String player, String group) {
        String privateMessage = AddonI18n.tr("neofontrender_ui_enhancements.chat.source.private");
        String server = AddonI18n.tr("neofontrender_ui_enhancements.chat.source.server");
        String playerLabel = AddonI18n.tr("neofontrender_ui_enhancements.chat.source.player");
        String groupLabel = AddonI18n.tr("neofontrender_ui_enhancements.chat.source.group");
        switch (source) {
            case PLAYER: return player == null || player.isEmpty() ? "[" + playerLabel + "]"
                    : "[" + playerLabel + ":" + player + "]";
            case PRIVATE: return player == null || player.isEmpty() ? "[" + privateMessage + "]"
                    : "[" + privateMessage + ":" + player + "]";
            case GROUP: return group == null || group.isEmpty() ? "[" + groupLabel + "]"
                    : "[" + groupLabel + ":" + group + "]";
            default: return "[" + server + "]";
        }
    }

    private static int badgeColor(ChatSource source) {
        if (source == ChatSource.PLAYER) return 0xFF81C995;
        if (source == ChatSource.PRIVATE) return 0xFFE8A1CF;
        if (source == ChatSource.GROUP) return 0xFF7FA8D9;
        return 0xFF9DB7DF;
    }
}
