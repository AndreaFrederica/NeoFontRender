package neofontrender.addons.worldcreation;

import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.api.layout.ILayoutWidget;
import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.factory.ClientGUI;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.ModularScreen;
import com.cleanroommc.modularui.screen.viewport.ModularGuiContext;
import com.cleanroommc.modularui.theme.WidgetThemeEntry;
import com.cleanroommc.modularui.utils.Alignment;
import com.cleanroommc.modularui.widget.ParentWidget;
import com.cleanroommc.modularui.widget.Widget;
import com.cleanroommc.modularui.widgets.TextWidget;
import com.cleanroommc.modularui.widgets.textfield.TextFieldWidget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.world.GameRules;
import neofontrender.addons.tooltips.AddonI18n;
import neofontrender.client.gui.component.base.NfrContentButton;
import neofontrender.client.gui.component.base.NfrLayout;
import neofontrender.client.gui.component.base.NfrScrollablePane;
import neofontrender.client.gui.component.base.NfrStringValue;
import neofontrender.client.gui.component.base.NfrTextButton;
import neofontrender.client.gui.component.base.NfrToggleIndicator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.regex.Pattern;

/** Reusable component-based game-rule editor for creation and integrated-server worlds. */
public final class GameRulesScreen {
    private static final String OWNER = "neofontrender_ui_enhancements";

    private GameRulesScreen() {}

    public static void openForCreation(GuiScreen background, Map<String, String> values) {
        open(background, values, ignored -> {});
    }

    public static void open(GuiScreen background, Map<String, String> values,
                            Consumer<Map<String, String>> apply) {
        GuiScreen parent = Minecraft.getMinecraft().currentScreen;
        Map<String, String> original = new LinkedHashMap<>(values);
        EditorSession session = new EditorSession(values, original, apply, parent);
        ModularPanel panel = new ModularPanel("game_rules").relativeToScreen().full();
        panel.disableThemeBackground(true).disableHoverThemeBackground(true);
        panel.child(new EditorLayout(session).relativeToParent().full());
        ClientGUI.open(new BackgroundScreen(background, panel).useTheme("neofontrender_modern"));
    }

    private static final class EditorSession {
        private final Map<String, String> values;
        private final Map<String, String> original;
        private final Map<String, String> defaults = new LinkedHashMap<>();
        private final Consumer<Map<String, String>> apply;
        private final GuiScreen parent;
        private String search = "";

        private EditorSession(Map<String, String> values, Map<String, String> original,
                              Consumer<Map<String, String>> apply, GuiScreen parent) {
            this.values = values;
            this.original = original;
            this.apply = apply;
            this.parent = parent;
            GameRules gameRules = new GameRules();
            for (String rule : gameRules.getRules()) {
                defaults.put(rule, gameRules.getString(rule));
                values.putIfAbsent(rule, gameRules.getString(rule));
            }
        }

        private void done() {
            apply.accept(new LinkedHashMap<>(values));
            ClientGUI.open(parent);
        }

        private void cancel() {
            values.clear();
            values.putAll(original);
            ClientGUI.open(parent);
        }
    }

    private static final class EditorLayout extends ParentWidget<EditorLayout> implements ILayoutWidget {
        private static final int PAD = 12;
        private final EditorSession session;
        private final Widget<?> chrome = new Chrome();
        private final TextWidget title = new TextWidget(IKey.str(tr("title")))
                .alignment(Alignment.Center).color(0xFFFFFF);
        private final TextFieldWidget search;
        private final RulesContent content;
        private final NfrScrollablePane scroll;
        private final NfrTextButton done;
        private final NfrTextButton cancel;
        private String lastSearch = "";

        private EditorLayout(EditorSession session) {
            this.session = session;
            search = new TextFieldWidget().setMaxLength(128).autoUpdateOnChange(true)
                    .hintText(tr("search"))
                    .value(new NfrStringValue(() -> session.search, value -> session.search = value));
            content = new RulesContent(session);
            scroll = new NfrScrollablePane(content);
            done = button(() -> tr("done"), session::done);
            cancel = button(() -> tr("cancel"), session::cancel);
            child(chrome);
            child(title);
            child(search);
            child(scroll);
            child(done);
            child(cancel);
            content.rebuild("");
        }

        @Override
        public void onUpdate() {
            super.onUpdate();
            String current = session.search == null ? "" : session.search;
            if (!current.equals(lastSearch)) {
                lastSearch = current;
                content.rebuild(current);
            }
        }

        @Override
        public boolean layoutWidgets() {
            int width = getArea().w();
            int height = getArea().h();
            NfrLayout.place(chrome, 0, 0, width, height);
            NfrLayout.place(title, PAD, 9, Math.max(0, width - PAD * 2), 14);
            int fieldWidth = Math.min(300, Math.max(180, width - PAD * 2));
            NfrLayout.place(search, (width - fieldWidth) / 2, 29, fieldWidth, 20);
            int listY = 56;
            int listHeight = Math.max(40, height - listY - 40);
            int listWidth = Math.min(520, Math.max(220, width - PAD * 2));
            int listX = (width - listWidth) / 2;
            scroll.layout(listX, listY, listWidth, listHeight,
                    Math.max(0, listWidth - 8), content.preferredHeight());
            int buttonWidth = Math.min(150, Math.max(90, (listWidth - 10) / 2));
            NfrLayout.place(done, width / 2 - 5 - buttonWidth, height - 29, buttonWidth, 20);
            NfrLayout.place(cancel, width / 2 + 5, height - 29, buttonWidth, 20);
            return true;
        }
    }

    private static final class RulesContent extends ParentWidget<RulesContent> implements ILayoutWidget {
        private static final int GAP = 5;
        private final EditorSession session;
        private final List<RowSpec> rows = new ArrayList<>();
        private int preferredHeight;

        private RulesContent(EditorSession session) {
            this.session = session;
        }

        private void rebuild(String search) {
            removeAll();
            rows.clear();
            String query = search.trim().toLowerCase(Locale.ROOT);
            Category previous = null;
            GameRules types = new GameRules();
            for (Category category : Category.values()) {
                for (String rule : session.defaults.keySet()) {
                    if (category(rule) != category || !matches(rule, query)) continue;
                    if (previous != category) {
                        add(new CategoryRow(category), 22);
                        previous = category;
                    }
                    boolean bool = types.areSameType(rule, GameRules.ValueType.BOOLEAN_VALUE);
                    boolean numeric = types.areSameType(rule, GameRules.ValueType.NUMERICAL_VALUE);
                    add(new RuleRow(session, rule, bool, numeric), 42);
                }
            }
            if (rows.isEmpty()) add(new EmptyRow(), 28);
            preferredHeight = 0;
            for (RowSpec row : rows) preferredHeight += row.height + GAP;
        }

        private boolean matches(String rule, String query) {
            if (query.isEmpty()) return true;
            return rule.toLowerCase(Locale.ROOT).contains(query)
                    || label(rule).toLowerCase(Locale.ROOT).contains(query)
                    || description(rule).toLowerCase(Locale.ROOT).contains(query);
        }

        private void add(IWidget widget, int height) {
            rows.add(new RowSpec(widget, height));
            child(widget);
        }

        private int preferredHeight() {
            return preferredHeight;
        }

        @Override
        public boolean layoutWidgets() {
            int y = 0;
            for (RowSpec row : rows) {
                NfrLayout.place(row.widget, 0, y, getArea().w(), row.height);
                y += row.height + GAP;
            }
            return true;
        }
    }

    private static final class RuleRow extends ParentWidget<RuleRow> implements ILayoutWidget {
        private final TextWidget name;
        private final TextWidget info;
        private final IWidget editor;
        private final NfrTextButton reset;

        private RuleRow(EditorSession session, String rule, boolean bool, boolean numeric) {
            name = new TextWidget(IKey.str(label(rule))).alignment(Alignment.CenterLeft).color(0xFFFFFF);
            info = new TextWidget(IKey.str(description(rule) + "  " + tr("default") + ": "
                    + session.defaults.get(rule))).alignment(Alignment.CenterLeft).color(0x8F9AA6);
            if (bool) {
                editor = toggle(() -> onOff(session.values.get(rule)),
                        () -> Boolean.parseBoolean(session.values.get(rule)),
                        () -> session.values.put(rule,
                                Boolean.toString(!Boolean.parseBoolean(session.values.get(rule)))));
            } else {
                TextFieldWidget field = new TextFieldWidget().setMaxLength(128).autoUpdateOnChange(true)
                        .value(new NfrStringValue(() -> session.values.get(rule),
                                value -> session.values.put(rule, value)));
                if (numeric) field.setPattern(Pattern.compile("-?\\d*"));
                editor = field;
            }
            reset = button(() -> tr("reset"), () -> session.values.put(rule, session.defaults.get(rule)));
            child(name);
            child(info);
            child(editor);
            child(reset);
        }

        @Override
        public boolean layoutWidgets() {
            int width = getArea().w();
            int editorWidth = Math.min(100, Math.max(70, width / 4));
            int resetWidth = 52;
            int textWidth = Math.max(40, width - editorWidth - resetWidth - 14);
            NfrLayout.place(name, 4, 3, textWidth, 13);
            NfrLayout.place(info, 4, 21, textWidth, 13);
            NfrLayout.place(editor, width - editorWidth - resetWidth - 8, 11, editorWidth, 20);
            NfrLayout.place(reset, width - resetWidth - 2, 11, resetWidth, 20);
            return true;
        }

        @Override
        public void draw(ModularGuiContext context, WidgetThemeEntry<?> theme) {
            Gui.drawRect(0, getArea().h() - 1, getArea().w(), getArea().h(), 0x604A535B);
        }
    }

    private static final class CategoryRow extends Widget<CategoryRow> {
        private final Category category;

        private CategoryRow(Category category) {
            this.category = category;
        }

        @Override
        public void draw(ModularGuiContext context, WidgetThemeEntry<?> theme) {
            Minecraft mc = Minecraft.getMinecraft();
            String text = tr("category." + category.key);
            int y = Math.max(0, (getArea().h() - mc.fontRenderer.FONT_HEIGHT) / 2);
            mc.fontRenderer.drawString(text, 4, y, 0xFFE8D36A, false);
        }
    }

    private static final class EmptyRow extends Widget<EmptyRow> {
        @Override
        public void draw(ModularGuiContext context, WidgetThemeEntry<?> theme) {
            Minecraft mc = Minecraft.getMinecraft();
            String text = tr("empty");
            mc.fontRenderer.drawString(text,
                    (getArea().w() - mc.fontRenderer.getStringWidth(text)) / 2,
                    (getArea().h() - mc.fontRenderer.FONT_HEIGHT) / 2, 0xFF9AA5B1, false);
        }
    }

    private static final class Chrome extends Widget<Chrome> {
        @Override
        public void draw(ModularGuiContext context, WidgetThemeEntry<?> theme) {
            Gui.drawRect(0, 0, getArea().w(), 54, 0xB0101317);
            Gui.drawRect(0, 54, getArea().w(), getArea().h() - 38, 0x58101317);
            Gui.drawRect(0, getArea().h() - 38, getArea().w(), getArea().h(), 0xC0101317);
        }
    }

    private static final class BackgroundScreen extends ModularScreen {
        private final GuiScreen background;

        private BackgroundScreen(GuiScreen background, ModularPanel panel) {
            super(OWNER, panel);
            this.background = background;
        }

        @Override
        public void drawScreen() {
            background.width = getScreenArea().w();
            background.height = getScreenArea().h();
            background.drawDefaultBackground();
            super.drawScreen();
        }
    }

    private enum Category {
        PLAYER("player"), MOBS("mobs"), SPAWNING("spawning"), DROPS("drops"),
        UPDATES("updates"), CHAT("chat"), MISC("misc");
        private final String key;
        Category(String key) { this.key = key; }
    }

    private static Category category(String rule) {
        if (Arrays.asList("keepInventory", "naturalRegeneration", "disableElytraMovementCheck",
                "reducedDebugInfo", "spawnRadius").contains(rule)) return Category.PLAYER;
        if (Arrays.asList("mobGriefing", "maxEntityCramming").contains(rule)) return Category.MOBS;
        if ("doMobSpawning".equals(rule)) return Category.SPAWNING;
        if (Arrays.asList("doTileDrops", "doMobLoot", "doEntityDrops").contains(rule)) return Category.DROPS;
        if (Arrays.asList("doDaylightCycle", "doWeatherCycle", "doFireTick", "randomTickSpeed")
                .contains(rule)) return Category.UPDATES;
        if (Arrays.asList("commandBlockOutput", "logAdminCommands", "sendCommandFeedback",
                "showDeathMessages", "announceAdvancements").contains(rule)) return Category.CHAT;
        return Category.MISC;
    }

    private static String label(String rule) {
        String key = "neofontrender_ui_enhancements.gamerule." + rule;
        String value = AddonI18n.tr(key);
        return key.equals(value) ? rule : value;
    }

    private static String description(String rule) {
        String key = "neofontrender_ui_enhancements.gamerule." + rule + ".description";
        String value = AddonI18n.tr(key);
        return key.equals(value) ? rule : value;
    }

    private static String onOff(String value) {
        return net.minecraft.client.resources.I18n.format(Boolean.parseBoolean(value) ? "options.on" : "options.off");
    }

    private static NfrTextButton button(Supplier<String> label, Runnable action) {
        NfrTextButton button = new NfrTextButton(label, true);
        button.onMousePressed(mouseButton -> {
            action.run();
            return true;
        });
        return button;
    }

    private static NfrContentButton toggle(Supplier<String> label, Supplier<Boolean> selected,
                                           Runnable action) {
        NfrContentButton button = new NfrContentButton(label, true, new NfrToggleIndicator(selected));
        button.onMousePressed(mouseButton -> {
            action.run();
            return true;
        });
        return button;
    }

    private static String tr(String key) {
        return AddonI18n.tr("neofontrender_ui_enhancements.gamerules_editor." + key);
    }

    private static final class RowSpec {
        private final IWidget widget;
        private final int height;
        private RowSpec(IWidget widget, int height) { this.widget = widget; this.height = height; }
    }
}
