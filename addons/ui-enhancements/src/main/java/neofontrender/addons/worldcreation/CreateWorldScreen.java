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
import com.cleanroommc.modularui.utils.Platform;
import com.cleanroommc.modularui.widget.ParentWidget;
import com.cleanroommc.modularui.widget.Widget;
import com.cleanroommc.modularui.widgets.TextWidget;
import com.cleanroommc.modularui.widgets.textfield.TextFieldWidget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiCreateWorld;
import net.minecraft.client.resources.I18n;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.GameRules;
import net.minecraft.world.WorldType;
import neofontrender.addons.mixin.GuiCreateWorldAccessor;
import neofontrender.addons.tooltips.AddonI18n;
import neofontrender.client.gui.component.base.NfrContentButton;
import neofontrender.client.gui.component.base.NfrLabeledTextField;
import neofontrender.client.gui.component.base.NfrLayout;
import neofontrender.client.gui.component.base.NfrStringValue;
import neofontrender.client.gui.component.base.NfrTextButton;
import neofontrender.client.gui.component.base.NfrToggleIndicator;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

/** High-version Game / World / More create-world screen backed by vanilla state and actions. */
public final class CreateWorldScreen {
    private static final String OWNER = "neofontrender_ui_enhancements";
    private static final Map<GuiCreateWorld, Session> SESSIONS = new WeakHashMap<>();

    private CreateWorldScreen() {}

    public static void open(GuiCreateWorld host) {
        Session session;
        synchronized (SESSIONS) {
            session = SESSIONS.computeIfAbsent(host, Session::new);
        }
        ModularPanel panel = new ModularPanel("create_world").relativeToScreen().full();
        panel.disableThemeBackground(true).disableHoverThemeBackground(true);
        panel.child(new Layout(session).relativeToParent().full());
        ClientGUI.open(new BackgroundScreen(host, panel).useTheme("neofontrender_modern"));
    }

    private enum Page { GAME, WORLD, MORE }

    private static final class Session {
        private final GuiCreateWorld host;
        private final GuiCreateWorldAccessor access;
        private final Map<String, String> rules = new LinkedHashMap<>();
        private Page page = Page.GAME;
        private EnumDifficulty difficulty = EnumDifficulty.NORMAL;

        private Session(GuiCreateWorld host) {
            this.host = host;
            this.access = (GuiCreateWorldAccessor) host;
            GameRules defaults = new GameRules();
            for (String rule : defaults.getRules()) rules.put(rule, defaults.getString(rule));
        }

        private void setName(String value) {
            access.nfrUi$getWorldNameField().setText(value);
            access.nfrUi$setWorldName(value);
            access.nfrUi$calcSaveDirName();
        }

        private void setSeed(String value) {
            access.nfrUi$getWorldSeedField().setText(value);
            access.nfrUi$setWorldSeed(value);
        }

        private void action(int id) {
            try {
                access.nfrUi$performAction(new GuiButton(id, 0, 0, ""));
            } catch (IOException exception) {
                throw new IllegalStateException("Create-world action " + id + " failed", exception);
            }
        }

        private void create() {
            access.nfrUi$calcSaveDirName();
            CreateWorldGameRulesState.setPending(GuiGameRulesList.collectOverrides(rules));
            CreateWorldDifficultyState.setPending(access.nfrUi$getHardCoreMode()
                    ? EnumDifficulty.HARD : difficulty);
            discard();
            action(0);
        }

        private void cancel() {
            CreateWorldGameRulesState.clearPending();
            CreateWorldDifficultyState.clearPending();
            discard();
            Minecraft.getMinecraft().displayGuiScreen(access.nfrUi$getParentScreen());
        }

        private void discard() {
            synchronized (SESSIONS) {
                SESSIONS.remove(host);
            }
        }

        private String modeLabel() {
            return I18n.format("selectWorld.gameMode") + ": "
                    + I18n.format("selectWorld.gameMode." + access.nfrUi$getGameMode());
        }

        private String difficultyLabel() {
            EnumDifficulty shown = access.nfrUi$getHardCoreMode() ? EnumDifficulty.HARD : difficulty;
            return I18n.format("options.difficulty") + ": " + I18n.format(shown.getTranslationKey());
        }

        private void cycleDifficulty() {
            difficulty = EnumDifficulty.byId(difficulty.getId() + 1);
        }

        private String worldTypeLabel() {
            WorldType type = currentWorldType();
            return I18n.format("selectWorld.mapType") + ": "
                    + (type == null ? "?" : I18n.format(type.getTranslationKey()));
        }

        private WorldType currentWorldType() {
            int index = access.nfrUi$getSelectedIndex();
            return index >= 0 && index < WorldType.WORLD_TYPES.length ? WorldType.WORLD_TYPES[index] : null;
        }
    }

    private static final class Layout extends ParentWidget<Layout> implements ILayoutWidget {
        private static final int TAB_HEIGHT = 28;
        private static final int FOOTER_HEIGHT = 38;
        private static final int CONTENT_WIDTH = 300;

        private final Session session;
        private final Chrome chrome = new Chrome();
        private final FormPage gamePage = new FormPage();
        private final FormPage worldPage = new FormPage();
        private final FormPage morePage = new FormPage();
        private final TabButton gameTab;
        private final TabButton worldTab;
        private final TabButton moreTab;
        private final NfrTextButton create;
        private final NfrTextButton cancel;

        private Layout(Session session) {
            this.session = session;
            gameTab = tab("game", Page.GAME);
            worldTab = tab("world", Page.WORLD);
            moreTab = tab("more", Page.MORE);
            create = button(() -> I18n.format("selectWorld.create"), session::create);
            cancel = button(() -> I18n.format("gui.cancel"), session::cancel);
            buildPages();
            child(chrome);
            child(gamePage);
            child(worldPage);
            child(morePage);
            child(gameTab);
            child(worldTab);
            child(moreTab);
            child(create);
            child(cancel);
            syncPage();
        }

        private void buildPages() {
            TextFieldWidget name = new TextFieldWidget().setMaxLength(32).autoUpdateOnChange(true)
                    .value(new NfrStringValue(session.access.nfrUi$getWorldNameField()::getText, session::setName));
            gamePage.add(new NfrLabeledTextField(I18n.format("selectWorld.enterName"), name), 52, 8);
            gamePage.add(button(session::modeLabel, () -> session.action(2)), 20, 7);
            NfrTextButton difficulty = button(session::difficultyLabel, session::cycleDifficulty);
            difficulty.setEnabledIf(widget -> !session.access.nfrUi$getHardCoreMode());
            gamePage.add(difficulty, 20, 7);
            NfrContentButton cheats = toggle(() -> I18n.format("selectWorld.allowCommands") + ": "
                            + I18n.format(session.access.nfrUi$getAllowCheats() ? "options.on" : "options.off"),
                    session.access::nfrUi$getAllowCheats, () -> session.action(6));
            cheats.setEnabledIf(widget -> !session.access.nfrUi$getHardCoreMode());
            gamePage.add(cheats, 20, 0);

            TextFieldWidget seed = new TextFieldWidget().setMaxLength(128).autoUpdateOnChange(true)
                    .value(new NfrStringValue(session.access.nfrUi$getWorldSeedField()::getText, session::setSeed));
            worldPage.add(button(session::worldTypeLabel, () -> session.action(5)), 20, 7);
            NfrTextButton customize = button(() -> I18n.format("selectWorld.customizeType"), () -> session.action(8));
            customize.setEnabledIf(widget -> {
                WorldType type = session.currentWorldType();
                return type != null && type.isCustomizable();
            });
            worldPage.add(customize, 20, 8);
            worldPage.add(new NfrLabeledTextField(I18n.format("selectWorld.enterSeed"), seed), 52, 8);
            worldPage.add(toggle(() -> I18n.format("selectWorld.mapFeatures") + ": "
                            + I18n.format(session.access.nfrUi$getGenerateStructuresEnabled() ? "options.on" : "options.off"),
                    session.access::nfrUi$getGenerateStructuresEnabled, () -> session.action(4)), 20, 7);
            NfrContentButton bonus = toggle(() -> I18n.format("selectWorld.bonusItems") + ": "
                            + I18n.format(session.access.nfrUi$getBonusChestEnabled() ? "options.on" : "options.off"),
                    session.access::nfrUi$getBonusChestEnabled, () -> session.action(7));
            bonus.setEnabledIf(widget -> !session.access.nfrUi$getHardCoreMode());
            worldPage.add(bonus, 20, 0);

            morePage.add(new TextWidget(IKey.str(tr("more_description")))
                    .alignment(Alignment.Center).color(0xA9B5C5), 24, 8);
            morePage.add(button(() -> tr("rules"),
                    () -> GameRulesScreen.openForCreation(session.host, session.rules)), 20, 0);
        }

        private TabButton tab(String key, Page page) {
            return new TabButton(() -> tr(key), () -> session.page == page, () -> {
                session.page = page;
                syncPage();
            });
        }

        private void syncPage() {
            gamePage.setEnabled(session.page == Page.GAME);
            worldPage.setEnabled(session.page == Page.WORLD);
            morePage.setEnabled(session.page == Page.MORE);
        }

        @Override
        public boolean layoutWidgets() {
            int width = getArea().w();
            int height = getArea().h();
            int third = width / 3;
            NfrLayout.place(chrome, 0, 0, width, height);
            NfrLayout.place(gameTab, 0, 0, third, TAB_HEIGHT);
            NfrLayout.place(worldTab, third, 0, third, TAB_HEIGHT);
            NfrLayout.place(moreTab, third * 2, 0, width - third * 2, TAB_HEIGHT);
            int contentWidth = Math.min(CONTENT_WIDTH, Math.max(200, width - 32));
            int contentX = (width - contentWidth) / 2;
            int contentY = TAB_HEIGHT + 18;
            int contentHeight = Math.max(0, height - contentY - FOOTER_HEIGHT - 8);
            NfrLayout.place(gamePage, contentX, contentY, contentWidth, contentHeight);
            NfrLayout.place(worldPage, contentX, contentY, contentWidth, contentHeight);
            NfrLayout.place(morePage, contentX, contentY, contentWidth, contentHeight);
            int gap = 10;
            int buttonWidth = Math.min(150, Math.max(90, (contentWidth - gap) / 2));
            int footerY = height - 29;
            NfrLayout.place(create, width / 2 - gap / 2 - buttonWidth, footerY, buttonWidth, 20);
            NfrLayout.place(cancel, width / 2 + gap / 2, footerY, buttonWidth, 20);
            return true;
        }
    }

    private static final class FormPage extends ParentWidget<FormPage> implements ILayoutWidget {
        private final Map<IWidget, int[]> rows = new LinkedHashMap<>();

        private void add(IWidget widget, int height, int gapAfter) {
            rows.put(widget, new int[]{height, gapAfter});
            child(widget);
        }

        @Override
        public boolean layoutWidgets() {
            int total = 0;
            for (int[] row : rows.values()) total += row[0] + row[1];
            int y = Math.max(0, (getArea().h() - total) / 3);
            for (Map.Entry<IWidget, int[]> entry : rows.entrySet()) {
                int[] row = entry.getValue();
                NfrLayout.place(entry.getKey(), 0, y, getArea().w(), row[0]);
                y += row[0] + row[1];
            }
            return true;
        }
    }

    private static final class Chrome extends Widget<Chrome> {
        @Override
        public void draw(ModularGuiContext context, WidgetThemeEntry<?> theme) {
            int width = getArea().w();
            int height = getArea().h();
            Gui.drawRect(0, 0, width, Layout.TAB_HEIGHT, 0xB0101317);
            Gui.drawRect(0, Layout.TAB_HEIGHT, width, height - Layout.FOOTER_HEIGHT, 0x48101317);
            Gui.drawRect(0, height - Layout.FOOTER_HEIGHT, width, height, 0xC0101317);
            Gui.drawRect(0, Layout.TAB_HEIGHT - 1, width, Layout.TAB_HEIGHT, 0x804A535B);
            Gui.drawRect(0, height - Layout.FOOTER_HEIGHT, width,
                    height - Layout.FOOTER_HEIGHT + 1, 0x804A535B);
        }
    }

    private static final class TabButton extends NfrTextButton {
        private final BooleanSupplier selected;

        private TabButton(Supplier<String> label, BooleanSupplier selected, Runnable action) {
            super(label, true);
            this.selected = selected;
            disableThemeBackground(true).disableHoverThemeBackground(true);
            onMousePressed(button -> {
                action.run();
                return true;
            });
        }

        @Override
        public void draw(ModularGuiContext context, WidgetThemeEntry<?> theme) {
            if (isHovering() || selected.getAsBoolean()) {
                Gui.drawRect(0, 0, getArea().w(), getArea().h(),
                        selected.getAsBoolean() ? 0x80252B31 : 0x402A3138);
            }
            super.draw(context, theme);
            if (selected.getAsBoolean()) {
                Gui.drawRect(0, getArea().h() - 2, getArea().w(), getArea().h(), 0xFFFFFFFF);
            }
        }
    }

    private static final class BackgroundScreen extends ModularScreen {
        private final GuiCreateWorld host;

        private BackgroundScreen(GuiCreateWorld host, ModularPanel panel) {
            super(OWNER, panel);
            this.host = host;
        }

        @Override
        public void drawScreen() {
            host.width = getScreenArea().w();
            host.height = getScreenArea().h();
            host.drawDefaultBackground();
            super.drawScreen();
        }
    }

    private static NfrTextButton button(Supplier<String> label, Runnable action) {
        return new NfrTextButton(label, true).onMousePressed(mouseButton -> {
            action.run();
            return true;
        });
    }

    private static NfrContentButton toggle(Supplier<String> label, Supplier<Boolean> selected,
                                           Runnable action) {
        NfrContentButton button = new NfrContentButton(label, false, new NfrToggleIndicator(selected));
        button.onMousePressed(mouseButton -> {
            action.run();
            return true;
        });
        return button;
    }

    private static String tr(String key) {
        return AddonI18n.tr("neofontrender_ui_enhancements.create_world." + key);
    }
}
