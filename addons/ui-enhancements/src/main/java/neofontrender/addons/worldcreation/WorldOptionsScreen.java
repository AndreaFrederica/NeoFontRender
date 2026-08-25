package neofontrender.addons.worldcreation;

import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.api.layout.ILayoutWidget;
import com.cleanroommc.modularui.factory.ClientGUI;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.ModularScreen;
import com.cleanroommc.modularui.screen.viewport.ModularGuiContext;
import com.cleanroommc.modularui.theme.WidgetThemeEntry;
import com.cleanroommc.modularui.utils.Alignment;
import com.cleanroommc.modularui.widget.ParentWidget;
import com.cleanroommc.modularui.widget.Widget;
import com.cleanroommc.modularui.widgets.TextWidget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.command.CommandGameRule;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.GameRules;
import net.minecraft.world.WorldServer;
import neofontrender.addons.tooltips.AddonI18n;
import neofontrender.client.gui.component.base.NfrLayout;
import neofontrender.client.gui.component.base.NfrTextButton;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

/** Integrated-server-only world settings entry used from the vanilla options screen. */
public final class WorldOptionsScreen {
    private static final String OWNER = "neofontrender_ui_enhancements";

    private WorldOptionsScreen() {}

    public static void open(GuiScreen parent) {
        Minecraft mc = Minecraft.getMinecraft();
        if (!mc.isSingleplayer() || mc.getIntegratedServer() == null || mc.world == null) return;
        ModularPanel panel = new ModularPanel("world_options").relativeToScreen().full();
        panel.disableThemeBackground(true).disableHoverThemeBackground(true);
        panel.child(new Layout(parent).relativeToParent().full());
        ClientGUI.open(new BackgroundScreen(parent, panel).useTheme("neofontrender_modern"));
    }

    private static final class Layout extends ParentWidget<Layout> implements ILayoutWidget {
        private final GuiScreen parent;
        private final Widget<?> chrome = new Chrome();
        private final TextWidget title = new TextWidget(IKey.str(tr("title")))
                .alignment(Alignment.Center).color(0xFFFFFF);
        private final NfrTextButton difficulty;
        private final NfrTextButton rules;
        private final NfrTextButton done;

        private Layout(GuiScreen parent) {
            this.parent = parent;
            difficulty = button(this::difficultyLabel, this::cycleDifficulty);
            Minecraft mc = Minecraft.getMinecraft();
            difficulty.setEnabledIf(widget -> mc.world != null
                    && !mc.world.getWorldInfo().isHardcoreModeEnabled()
                    && !mc.world.getWorldInfo().isDifficultyLocked());
            rules = button(() -> tr("game_rules"), this::openRules);
            done = button(() -> tr("done"), () -> ClientGUI.open(parent));
            child(chrome);
            child(title);
            child(difficulty);
            child(rules);
            child(done);
        }

        private String difficultyLabel() {
            Minecraft mc = Minecraft.getMinecraft();
            if (mc.world == null) return net.minecraft.client.resources.I18n.format("options.difficulty");
            return net.minecraft.client.resources.I18n.format("options.difficulty") + ": "
                    + net.minecraft.client.resources.I18n.format(mc.world.getDifficulty().getTranslationKey());
        }

        private void cycleDifficulty() {
            Minecraft mc = Minecraft.getMinecraft();
            if (mc.world == null) return;
            EnumDifficulty next = EnumDifficulty.byId(mc.world.getDifficulty().getId() + 1);
            IntegratedServer server = mc.getIntegratedServer();
            if (server != null) server.addScheduledTask(() -> server.setDifficultyForAllWorlds(next));
        }

        private void openRules() {
            IntegratedServer server = Minecraft.getMinecraft().getIntegratedServer();
            if (server == null) return;
            WorldServer world = server.getWorld(0);
            if (world == null) return;
            Map<String, String> values = new LinkedHashMap<>();
            GameRules gameRules = world.getGameRules();
            for (String rule : gameRules.getRules()) values.put(rule, gameRules.getString(rule));
            GameRulesScreen.open(parentBackground(), values, updated -> applyRules(server, updated));
        }

        private GuiScreen parentBackground() {
            return parent;
        }

        private static void applyRules(IntegratedServer server, Map<String, String> values) {
            server.addScheduledTask(() -> {
                WorldServer world = server.getWorld(0);
                if (world == null) return;
                GameRules rules = world.getGameRules();
                for (Map.Entry<String, String> entry : values.entrySet()) {
                    if (!rules.hasRule(entry.getKey())) continue;
                    String old = rules.getString(entry.getKey());
                    if (old.equals(entry.getValue())) continue;
                    rules.setOrCreateGameRule(entry.getKey(), entry.getValue());
                    CommandGameRule.notifyGameRuleChange(rules, entry.getKey(), server);
                }
            });
        }

        @Override
        public boolean layoutWidgets() {
            int width = getArea().w();
            int height = getArea().h();
            NfrLayout.place(chrome, 0, 0, width, height);
            NfrLayout.place(title, 12, 14, Math.max(0, width - 24), 14);
            int contentWidth = Math.min(300, Math.max(200, width - 32));
            int x = (width - contentWidth) / 2;
            int y = Math.max(48, height / 2 - 38);
            NfrLayout.place(difficulty, x, y, contentWidth, 20);
            NfrLayout.place(rules, x, y + 28, contentWidth, 20);
            NfrLayout.place(done, x, height - 29, contentWidth, 20);
            return true;
        }
    }

    private static final class Chrome extends Widget<Chrome> {
        @Override
        public void draw(ModularGuiContext context, WidgetThemeEntry<?> theme) {
            Gui.drawRect(0, 0, getArea().w(), getArea().h() - 38, 0x68101317);
            Gui.drawRect(0, getArea().h() - 38, getArea().w(), getArea().h(), 0xC0101317);
        }
    }

    private static final class BackgroundScreen extends ModularScreen {
        private final GuiScreen parent;

        private BackgroundScreen(GuiScreen parent, ModularPanel panel) {
            super(OWNER, panel);
            this.parent = parent;
        }

        @Override
        public void drawScreen() {
            parent.width = getScreenArea().w();
            parent.height = getScreenArea().h();
            parent.drawDefaultBackground();
            super.drawScreen();
        }
    }

    private static NfrTextButton button(Supplier<String> label, Runnable action) {
        NfrTextButton button = new NfrTextButton(label, true);
        button.onMousePressed(mouseButton -> {
            action.run();
            return true;
        });
        return button;
    }

    private static String tr(String key) {
        return AddonI18n.tr("neofontrender_ui_enhancements.world_options." + key);
    }
}
