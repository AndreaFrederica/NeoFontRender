package neofontrender.client.gui;

import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.factory.ClientGUI;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.ModularScreen;
import com.cleanroommc.modularui.utils.Alignment;
import com.cleanroommc.modularui.widgets.TextWidget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.resources.I18n;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import neofontrender.client.gui.component.base.NfrScrollablePane;
import neofontrender.client.gui.component.business.NfrSettingsControls;
import neofontrender.client.gui.component.business.NfrSettingsFooter;
import neofontrender.client.gui.component.business.NfrSettingsTabs;
import neofontrender.client.gui.layouts.NfrSettingsLayout;
import neofontrender.client.gui.model.NfrSettingsDraft;
import neofontrender.client.gui.pages.NfrSettingsPage;
import neofontrender.client.gui.pages.NfrSettingsRoute;
import neofontrender.client.gui.views.NfrAboutSettingsView;
import neofontrender.client.gui.views.NfrAdvancedSettingsView;
import neofontrender.client.gui.views.NfrCacheSettingsView;
import neofontrender.client.gui.views.NfrFixesSettingsView;
import neofontrender.client.gui.views.NfrFontSettingsView;
import neofontrender.client.gui.views.NfrGeneralSettingsView;
import neofontrender.client.gui.views.NfrLaboratorySettingsView;
import neofontrender.client.gui.views.NfrLicensesSettingsView;
import neofontrender.client.gui.views.NfrPerformanceSettingsView;
import neofontrender.client.gui.views.NfrRenderingSettingsView;
import neofontrender.client.gui.views.NfrShadowSettingsView;
import neofontrender.api.client.settings.NfrSettingsPageContext;
import neofontrender.api.client.settings.NfrSettingsPageRegistry;
import neofontrender.api.client.settings.NfrSettingsPageSession;
import neofontrender.core.config.NeofontrenderConfig;
import neofontrender.core.font.FontManager;
import neofontrender.core.font.skia.SkijaRuntimeSupport;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

/** Settings entry point and route controller. Page content lives under {@code gui.views}. */
@SideOnly(Side.CLIENT)
public final class NeofontrenderConfigScreen {
    private static net.minecraft.client.gui.GuiScreen returnScreen;

    private NeofontrenderConfigScreen() {}

    public static void open() {
        open(null);
    }

    public static void open(net.minecraft.client.gui.GuiScreen parent) {
        returnScreen = parent;
        ScreenSession session = new ScreenSession(new NfrSettingsDraft());
        openRoute(session, Route.builtin(NfrSettingsRoute.FONT));
    }

    private static void openRoute(ScreenSession session, Route route) {
        ClientGUI.open(createScreen(buildPanel(session, route)));
    }

    private static ModularPanel buildPanel(ScreenSession session, Route route) {
        ModularPanel panel = new ModularPanel("font_config_" + route.panelId())
                .relativeToScreen()
                .full();
        configureRootBackground(panel);
        panel.child(new NfrSettingsPage(buildLayout(session, route)).relativeToParent().full());
        return panel;
    }

    private static NfrSettingsLayout buildLayout(ScreenSession session, Route route) {
        NfrSettingsDraft draft = session.draft;
        TextWidget title = new TextWidget(IKey.str(tr("neofontrender.gui.title")
                + (route.isFont() ? "" : " / " + route.title())))
                .alignment(Alignment.CenterLeft)
                .color(0xFFFFFF);
        NfrSettingsTabs tabs = tabs(session, route);
        NfrScrollablePane tabsPane = new NfrScrollablePane(tabs);
        NfrSettingsControls controls = new NfrSettingsControls(draft, () -> preview(draft),
                id -> reloadAndOpen(session, NfrSettingsRoute.byId(id)), NeofontrenderConfigScreen::skiaAvailable);
        IWidget view = view(session, route, controls);
        IWidget auxiliary = route.isFont()
                ? controls.action("neofontrender.gui.button.preview", 90, 20, () -> preview(draft))
                : controls.action("neofontrender.gui.button.back", 80, 20,
                        () -> openRoute(session, Route.builtin(NfrSettingsRoute.FONT)));
        NfrSettingsFooter footer = new NfrSettingsFooter(auxiliary,
                controls.action("neofontrender.gui.button.apply", 80, 20, () -> {
                    apply(session);
                    closeToParent();
                }),
                controls.action("neofontrender.gui.button.cancel", 80, 20, () -> {
                    draft.restoreOriginal();
                    session.cancelExtensions();
                    reloadFontManager();
                    closeToParent();
                }));
        return new NfrSettingsLayout(title, tabsPane, view, footer, tabs::preferredHeight, draft.categoryScroll);
    }

    private static IWidget view(ScreenSession screen, Route selected, NfrSettingsControls controls) {
        NfrSettingsDraft draft = screen.draft;
        if (selected.extension != null) {
            NfrSettingsPageSession session = screen.extensionSessions.get(selected.extension.id());
            NfrSettingsControls extensionControls = new NfrSettingsControls(draft, session::preview,
                    id -> reloadAndOpen(screen, NfrSettingsRoute.byId(id)), NeofontrenderConfigScreen::skiaAvailable);
            return session.createView(new NfrSettingsPageContext(extensionControls,
                    () -> openRoute(screen, selected)));
        }
        NfrSettingsRoute route = selected.builtin;
        switch (route) {
            case FONT:
                return new NfrFontSettingsView(draft, controls, () -> preview(draft),
                        () -> reloadAndOpen(screen, NfrSettingsRoute.FONT),
                        NeofontrenderConfigScreen::openFontFolder);
            case GENERAL:
                return new NfrGeneralSettingsView(draft, controls, route.id);
            case RENDERING:
                return new NfrRenderingSettingsView(draft, controls);
            case PERFORMANCE:
                return new NfrPerformanceSettingsView(draft, controls);
            case ADVANCED:
                return new NfrAdvancedSettingsView(draft, controls, skiaAvailable());
            case CACHE:
                return new NfrCacheSettingsView(draft, controls);
            case SHADOW:
                return new NfrShadowSettingsView(controls);
            case FIXES:
                return new NfrFixesSettingsView(draft, controls);
            case LABORATORY:
                return new NfrLaboratorySettingsView(draft, controls);
            case ABOUT:
                return new NfrAboutSettingsView(draft, skiaAvailable());
            case LICENSES:
                return new NfrLicensesSettingsView();
            default:
                throw new IllegalArgumentException("Unsupported settings route: " + route);
        }
    }

    private static NfrSettingsTabs tabs(ScreenSession session, Route selected) {
        List<NfrSettingsTabs.Tab> items = new ArrayList<>();
        for (NfrSettingsRoute route : NfrSettingsRoute.values()) {
            if (route != NfrSettingsRoute.ABOUT && route != NfrSettingsRoute.LICENSES) {
                addBuiltinTab(items, session, selected, route);
            }
        }
        for (neofontrender.api.client.settings.NfrSettingsPage page : session.extensionPages) {
            items.add(new NfrSettingsTabs.Tab(page::title, selected.extension == page,
                    () -> openRoute(session, Route.extension(page))));
        }
        addBuiltinTab(items, session, selected, NfrSettingsRoute.ABOUT);
        addBuiltinTab(items, session, selected, NfrSettingsRoute.LICENSES);
        return new NfrSettingsTabs(items, scroll -> session.draft.categoryScroll = scroll);
    }

    private static void addBuiltinTab(List<NfrSettingsTabs.Tab> items, ScreenSession session, Route selected,
                                      NfrSettingsRoute route) {
        items.add(new NfrSettingsTabs.Tab(() -> tr(route.titleKey), selected.builtin == route,
                () -> openRoute(session, Route.builtin(route))));
    }

    private static ModularScreen createScreen(ModularPanel panel) {
        return new ParentBackgroundScreen(panel).useTheme("neofontrender_modern").pausesGame(false);
    }

    private static void configureRootBackground(ModularPanel panel) {
        if (Minecraft.getMinecraft().world == null) {
            panel.disableThemeBackground(true);
            panel.disableHoverThemeBackground(true);
        }
    }

    private static void reloadAndOpen(ScreenSession session, NfrSettingsRoute route) {
        session.draft.writeToConfig(false);
        reloadFontManager();
        openRoute(session, Route.builtin(route));
    }

    private static void preview(NfrSettingsDraft draft) {
        if (!draft.enabled || "vanilla".equals(NfrSettingsControls.normalizeEngine(draft.engine))) return;
        draft.writeToConfig(false);
        reloadFontManager();
    }

    private static void apply(ScreenSession session) {
        session.draft.writeToConfig(true);
        session.applyExtensions();
        reloadFontManager();
    }

    private static final class ScreenSession {
        private final NfrSettingsDraft draft;
        private final List<neofontrender.api.client.settings.NfrSettingsPage> extensionPages;
        private final Map<String, NfrSettingsPageSession> extensionSessions = new LinkedHashMap<>();

        private ScreenSession(NfrSettingsDraft draft) {
            this.draft = draft;
            this.extensionPages = NfrSettingsPageRegistry.snapshot();
            for (neofontrender.api.client.settings.NfrSettingsPage page : extensionPages) {
                NfrSettingsPageSession session = page.createSession();
                if (session == null) throw new IllegalStateException("Settings page returned a null session: " + page.id());
                extensionSessions.put(page.id(), session);
            }
        }

        private void applyExtensions() { for (NfrSettingsPageSession session : extensionSessions.values()) session.apply(); }
        private void cancelExtensions() { for (NfrSettingsPageSession session : extensionSessions.values()) session.cancel(); }
    }

    private static final class Route {
        private final NfrSettingsRoute builtin;
        private final neofontrender.api.client.settings.NfrSettingsPage extension;
        private Route(NfrSettingsRoute builtin, neofontrender.api.client.settings.NfrSettingsPage extension) {
            this.builtin = builtin; this.extension = extension;
        }
        private static Route builtin(NfrSettingsRoute route) { return new Route(route, null); }
        private static Route extension(neofontrender.api.client.settings.NfrSettingsPage page) { return new Route(null, page); }
        private boolean isFont() { return builtin == NfrSettingsRoute.FONT; }
        private String title() { return extension != null ? extension.title() : tr(builtin.titleKey); }
        private String panelId() { return extension != null ? extension.id().replace(':', '_') : builtin.name().toLowerCase(); }
    }

    private static void reloadFontManager() {
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft.getResourceManager() != null) {
            FontManager.INSTANCE.reload(minecraft.getResourceManager());
        }
    }

    private static boolean skiaAvailable() {
        return SkijaRuntimeSupport.checkCompatibility().isSupported();
    }

    private static void openFontFolder() {
        File directory = NeofontrenderConfig.ensureFontDirectory();
        try {
            if (Desktop.isDesktopSupported()) Desktop.getDesktop().open(directory);
        } catch (IOException | RuntimeException exception) {
            neofontrender.NeoFontRender.LOGGER.error("Failed to open font folder '{}'", directory, exception);
        }
    }

    private static void closeToParent() {
        Minecraft minecraft = Minecraft.getMinecraft();
        if (returnScreen != null) minecraft.displayGuiScreen(returnScreen);
        else ClientGUI.close();
    }

    private static String tr(String key) {
        return I18n.format(key);
    }

    private static final class ParentBackgroundScreen extends ModularScreen {
        private ParentBackgroundScreen(ModularPanel panel) {
            super("neofontrender", panel);
        }

        @Override
        public void drawScreen() {
            Minecraft minecraft = Minecraft.getMinecraft();
            if (minecraft.world == null && returnScreen != null) {
                syncParentBackgroundSize(minecraft);
                returnScreen.drawDefaultBackground();
            }
            super.drawScreen();
        }

        @Override
        public void onResize(int width, int height) {
            super.onResize(width, height);
            syncParentBackgroundSize(Minecraft.getMinecraft());
        }

        private static void syncParentBackgroundSize(Minecraft minecraft) {
            if (returnScreen == null || minecraft.world != null) return;
            ScaledResolution resolution = new ScaledResolution(minecraft);
            int width = resolution.getScaledWidth();
            int height = resolution.getScaledHeight();
            if (returnScreen.width != width || returnScreen.height != height) {
                returnScreen.setWorldAndResolution(minecraft, width, height);
            }
        }
    }
}
