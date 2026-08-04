package neofontrender.addons.chat;

import com.cleanroommc.modularui.api.GuiAxis;
import com.cleanroommc.modularui.api.layout.ILayoutWidget;
import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.factory.ClientGUI;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.ModularScreen;
import com.cleanroommc.modularui.screen.viewport.ModularGuiContext;
import com.cleanroommc.modularui.theme.WidgetThemeEntry;
import com.cleanroommc.modularui.utils.Platform;
import com.cleanroommc.modularui.widget.ParentWidget;
import com.cleanroommc.modularui.widget.Widget;
import com.cleanroommc.modularui.widgets.TextWidget;
import com.cleanroommc.modularui.widgets.textfield.TextFieldWidget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.text.ITextComponent;
import neofontrender.addons.tooltips.AddonI18n;
import neofontrender.client.gui.component.base.NfrLayout;
import neofontrender.client.gui.component.base.NfrOptionDropdown;
import neofontrender.client.gui.component.base.NfrTextButton;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/** NFR-themed history management screen: filter, search, per-scope browsing, copy and delete. */
public final class ChatHistoryScreen {
    private static final int MAX_ROWS = 2000;
    private static final int PAD = 12;
    private static final String ALL = "";
    private static final String SINGLEPLAYER_PREFIX = "singleplayer:";
    private static final String SERVER_PREFIX = "server:";

    private static String pendingSearch = "";

    private ChatHistoryScreen() {}

    public static void open() {
        ModularPanel panel = new ModularPanel("nfr_chat_history").relativeToScreen().full();
        Layout layout = new Layout();
        panel.child(layout.relativeToParent().full());
        ClientGUI.open(new ModularScreen(panel).useTheme("neofontrender_modern").pausesGame(false));
    }

    private static String tr(String key) {
        return AddonI18n.tr("neofontrender_ui_enhancements.gui.history." + key);
    }

    private static String textOf(ChatHistoryStore.ReceivedMessage message) {
        if (message.metadata.source == ChatSource.PRIVATE && !message.metadata.privateBody.isEmpty()) {
            return message.metadata.privateBody;
        }
        try {
            ITextComponent component = ITextComponent.Serializer.jsonToComponent(message.json);
            if (component != null) return component.getUnformattedText();
        } catch (RuntimeException ignored) {}
        return "";
    }

    /** Loads every stored message once; filtering happens in memory so typing stays instant. */
    private static List<Item> loadAll() {
        ChatHistoryStore store = ChatHistoryManager.INSTANCE.store();
        List<Item> items = new ArrayList<>();
        if (store == null) return items;
        for (String scope : store.scopes()) {
            for (ChatHistoryStore.ReceivedMessage message : store.loadReceived(scope)) {
                items.add(new Item(message.rowId, scope, message.metadata, textOf(message)));
            }
        }
        items.sort((a, b) -> Long.compare(b.metadata.timestamp, a.metadata.timestamp));
        return items;
    }

    private static void copy(Item item) {
        GuiScreen.setClipboardString(item.text);
    }

    private static void deleteRow(Item item) {
        ChatHistoryStore store = ChatHistoryManager.INSTANCE.store();
        if (store != null) store.deleteRow(item.rowId);
    }

    private static void clearCurrent(ChatSource filter, String scopeFilter) {
        ChatHistoryStore store = ChatHistoryManager.INSTANCE.store();
        if (store == null) return;
        if (filter != null) {
            store.deleteBySource(filter);
        } else if (!scopeFilter.isEmpty()) {
            store.deleteScope(scopeFilter);
        } else {
            for (String scope : store.scopes()) store.deleteScope(scope);
        }
    }

    private static String scopeDisplay(String scope) {
        if (scope == null || scope.isEmpty()) return tr("scope.all");
        if (scope.startsWith(SINGLEPLAYER_PREFIX)) return tr("scope.singleplayer") + scope.substring(SINGLEPLAYER_PREFIX.length());
        if (scope.startsWith(SERVER_PREFIX)) return tr("scope.server") + scope.substring(SERVER_PREFIX.length());
        return scope;
    }

    private static final class Layout extends ParentWidget<Layout> implements ILayoutWidget {
        private ChatSource filter;
        private String scopeFilter = ALL;
        private final List<Item> allItems;
        private final IWidget title = new TextWidget(tr("title")).color(0xFFFFFF);
        private final NfrTextButton close = new NfrTextButton(() -> tr("close"), true)
                .size(60, 20)
                .onMousePressed(button -> {
                    ClientGUI.close();
                    return true;
                });
        private final List<NfrTextButton> filters = new ArrayList<>();
        private final NfrOptionDropdown scopeDropdown;
        private final TextFieldWidget searchField;
        private final NfrTextButton clearButton = new NfrTextButton(() -> tr("clear"), true)
                .size(90, 20)
                .onMousePressed(button -> {
                    clearCurrent(filter, scopeFilter);
                    reload();
                    refresh();
                    return true;
                });
        private final NfrHistoryList list = new NfrHistoryList();
        private final Widget<?> footer = new Footer();
        private final List<String> scopes;

        private String countText = "";
        private int listWidth;

        private Layout() {
            ChatHistoryStore store = ChatHistoryManager.INSTANCE.store();
            List<String> scopeList = new ArrayList<>();
            if (store != null) scopeList.addAll(store.scopes());
            Collections.sort(scopeList, String.CASE_INSENSITIVE_ORDER);
            this.scopes = scopeList;
            this.allItems = loadAll();
            filters.add(chip("filter.all", () -> filter == null));
            filters.add(chip("filter.private", () -> filter == ChatSource.PRIVATE));
            filters.add(chip("filter.server", () -> filter == ChatSource.SERVER));
            filters.add(chip("filter.player", () -> filter == ChatSource.PLAYER));
            filters.add(chip("filter.group", () -> filter == ChatSource.GROUP));
            scopeDropdown = new NfrOptionDropdown("history_scope", () -> tr("scope.label"),
                    () -> scopeFilter, value -> {
                        scopeFilter = value;
                        refresh();
                    }, scopeValues(), ChatHistoryScreen::scopeDisplay, false);
            searchField = new TextFieldWidget().setMaxLength(64);
            child(title);
            child(close);
            for (NfrTextButton button : filters) child(button);
            child(scopeDropdown);
            child(searchField);
            child(clearButton);
            child(list);
            child(footer);
            refresh();
        }

        private NfrTextButton chip(String key, java.util.function.Supplier<Boolean> selected) {
            NfrTextButton button = new NfrFilterButton(() -> tr(key), true, selected).size(64, 20);
            button.onMousePressed(mouseButton -> {
                filter = selected.get() ? null : filterOf(key);
                refresh();
                return true;
            });
            return button;
        }

        private static ChatSource filterOf(String key) {
            if (key.endsWith(".private")) return ChatSource.PRIVATE;
            if (key.endsWith(".server")) return ChatSource.SERVER;
            if (key.endsWith(".player")) return ChatSource.PLAYER;
            if (key.endsWith(".group")) return ChatSource.GROUP;
            return null;
        }

        private List<String> scopeValues() {
            List<String> values = new ArrayList<>();
            values.add(ALL);
            values.addAll(scopes);
            return values;
        }

        private void reload() {
            allItems.clear();
            allItems.addAll(loadAll());
        }

        private void refresh() {
            String search = pendingSearch == null ? "" : pendingSearch;
            List<NfrHistoryRow> rows = new ArrayList<>();
            int matched = 0;
            String lowerSearch = search.toLowerCase(Locale.ROOT);
            for (Item item : allItems) {
                if (filter != null && item.metadata.source != filter) continue;
                if (!scopeFilter.isEmpty() && !item.scope.equals(scopeFilter)) continue;
                if (!lowerSearch.isEmpty() && !item.text.toLowerCase(Locale.ROOT).contains(lowerSearch)) continue;
                matched++;
                if (rows.size() >= MAX_ROWS) continue;
                rows.add(new NfrHistoryRow(item.metadata.timestamp,
                        item.metadata.source, item.metadata.playerName, item.metadata.group, item.text,
                        () -> copy(item), () -> {
                            deleteRow(item);
                            allItems.remove(item);
                            refresh();
                        }));
            }
            list.setRows(rows);
            String count = tr("count") + ": " + matched;
            if (matched > rows.size()) count += " (" + tr("truncated").replace("%s", Integer.toString(rows.size())) + ")";
            if (matched == 0) count = tr("empty");
            countText = count;
        }

        @Override
        public void onUpdate() {
            super.onUpdate();
            // Poll the typed text here, NOT in layoutWidgets(): refresh() rebuilds the list's
            // children via setRows(), and mutating the widget tree during ModularUI's resize
            // pass re-enters the ResizeNode computation so it never converges, producing the
            // endless "Failed to resize sub tree" flood. onUpdate() runs each client tick,
            // outside the resize pass, so tree mutations are safe here.
            String typed = searchField.getText();
            if (typed == null) typed = "";
            if (!typed.equals(pendingSearch)) {
                pendingSearch = typed;
                refresh();
            }
        }

        @Override
        public boolean layoutWidgets() {
            int width = getArea().w(), height = getArea().h();
            NfrLayout.place(title, PAD, PAD, Math.max(0, width - PAD * 2 - 70), 16);
            NfrLayout.place(close, width - PAD - 60, PAD - 2, 60, 20);
            int filterY = PAD + 24;
            int fx = PAD;
            for (NfrTextButton button : filters) {
                NfrLayout.place(button, fx, filterY, 64, 20);
                fx += 70;
            }
            int scopeY = filterY + 26;
            NfrLayout.place(scopeDropdown, PAD, scopeY, 220, 20);
            NfrLayout.place(searchField, PAD + 230, scopeY, Math.max(0, width - PAD * 2 - 230 - 100), 20);
            NfrLayout.place(clearButton, width - PAD - 90, scopeY, 90, 20);
            int listY = scopeY + 28;
            listWidth = Math.max(0, width - PAD * 2);
            int listHeight = Math.max(40, height - PAD - listY - 30);
            NfrLayout.place(list, PAD, listY, listWidth, listHeight);
            NfrLayout.place(footer, PAD, height - PAD - 16, Math.max(0, width - PAD * 2), 16);
            return true;
        }
    }

    /** Bottom status line with a mutable label. */
    private static final class Footer extends Widget<Footer> {
        @Override
        public void draw(ModularGuiContext context, WidgetThemeEntry<?> theme) {
            super.draw(context, theme);
            if (getParent() instanceof Layout) {
                Platform.setupDrawFont();
                String text = ((Layout) getParent()).countText;
                Minecraft.getMinecraft().fontRenderer.drawString(text, 0, Math.max(0, (getArea().h() - 9) / 2), 0xFF9AA5B1);
            }
        }
    }

    private static final class Item {
        private final long rowId;
        private final String scope;
        private final ChatMessageMetadata metadata;
        private final String text;

        private Item(long rowId, String scope, ChatMessageMetadata metadata, String text) {
            this.rowId = rowId;
            this.scope = scope;
            this.metadata = metadata;
            this.text = text;
        }
    }
}
