package neofontrender.client.gui;

import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.factory.ClientGUI;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.ModularScreen;
import com.cleanroommc.modularui.utils.Alignment;
import com.cleanroommc.modularui.widgets.TextWidget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.resources.I18n;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
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
import neofontrender.core.config.NeofontrenderConfig;
import neofontrender.core.font.FontManager;
import neofontrender.core.font.skia.SkijaRuntimeSupport;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/** Settings entry point and route controller. Page content lives under {@code gui.views}. */
@SideOnly(Side.CLIENT)
public final class NeofontrenderConfigScreen {
    private static GuiScreen returnScreen;

    private NeofontrenderConfigScreen() {}

    public static void open() {
        open(null);
    }

    public static void open(GuiScreen parent) {
        returnScreen = parent;
        openRoute(new NfrSettingsDraft(), NfrSettingsRoute.FONT);
    }

    private static void openRoute(NfrSettingsDraft draft, NfrSettingsRoute route) {
        ClientGUI.open(createScreen(buildPanel(draft, route)));
    }

    private static ModularPanel buildPanel(NfrSettingsDraft draft, NfrSettingsRoute route) {
        ModularPanel panel = new ModularPanel("font_config_" + route.name().toLowerCase())
                .relativeToScreen()
                .full();
        configureRootBackground(panel);
        panel.child(new NfrSettingsPage(buildLayout(draft, route)).relativeToParent().full());
        return panel;
    }

    private static NfrSettingsLayout buildLayout(NfrSettingsDraft draft, NfrSettingsRoute route) {
        TextWidget title = new TextWidget(IKey.str(tr("neofontrender.gui.title")
                + (route == NfrSettingsRoute.FONT ? "" : " / " + tr(route.titleKey))))
                .alignment(Alignment.CenterLeft)
                .color(0xFFFFFF);
        NfrSettingsTabs tabs = tabs(draft, route);
        NfrScrollablePane tabsPane = new NfrScrollablePane(tabs);
        NfrSettingsControls controls = new NfrSettingsControls(draft, () -> preview(draft),
                id -> reloadAndOpen(draft, NfrSettingsRoute.byId(id)), NeofontrenderConfigScreen::skiaAvailable);
        IWidget view = view(draft, route, controls);
        IWidget auxiliary = route == NfrSettingsRoute.FONT
                ? controls.action("neofontrender.gui.button.preview", 90, 20, () -> preview(draft))
                : controls.action("neofontrender.gui.button.back", 80, 20,
                        () -> openRoute(draft, NfrSettingsRoute.FONT));
        NfrSettingsFooter footer = new NfrSettingsFooter(auxiliary,
                controls.action("neofontrender.gui.button.apply", 80, 20, () -> {
                    apply(draft);
                    closeToParent();
                }),
                controls.action("neofontrender.gui.button.cancel", 80, 20, () -> {
                    draft.restoreOriginal();
                    reloadFontManager();
                    closeToParent();
                }));
        return new NfrSettingsLayout(title, tabsPane, view, footer, tabs::preferredHeight, draft.categoryScroll);
    }

    private static IWidget view(NfrSettingsDraft draft, NfrSettingsRoute route, NfrSettingsControls controls) {
        switch (route) {
            case FONT:
                return new NfrFontSettingsView(draft, controls, () -> preview(draft),
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

    private static NfrSettingsTabs tabs(NfrSettingsDraft draft, NfrSettingsRoute selected) {
        List<NfrSettingsTabs.Tab> items = new ArrayList<>();
        for (NfrSettingsRoute route : NfrSettingsRoute.values()) {
            items.add(new NfrSettingsTabs.Tab(() -> tr(route.titleKey), route == selected,
                    () -> openRoute(draft, route)));
        }
        return new NfrSettingsTabs(items, scroll -> draft.categoryScroll = scroll);
    }

    private static ModularScreen createScreen(ModularPanel panel) {
        return new ParentBackgroundScreen(panel).useTheme("neofontrender_modern").pausesGame(false);
    }

    private static void configureRootBackground(ModularPanel panel) {
        if (Minecraft.getMinecraft().theWorld == null) {
            panel.disableThemeBackground(true);
            panel.disableHoverThemeBackground(true);
        }
    }

    private static void reloadAndOpen(NfrSettingsDraft draft, NfrSettingsRoute route) {
        draft.writeToConfig(false);
        reloadFontManager();
        openRoute(draft, route);
    }

    private static void preview(NfrSettingsDraft draft) {
        if (!draft.enabled || "vanilla".equals(NfrSettingsControls.normalizeEngine(draft.engine))) return;
        draft.writeToConfig(false);
        reloadFontManager();
    }

    private static void apply(NfrSettingsDraft draft) {
        draft.writeToConfig(true);
        reloadFontManager();
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
            if (minecraft.theWorld == null && returnScreen != null) {
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
            if (returnScreen == null || minecraft.theWorld != null) return;
            ScaledResolution resolution = new ScaledResolution(minecraft, minecraft.displayWidth,
                    minecraft.displayHeight);
            int width = resolution.getScaledWidth();
            int height = resolution.getScaledHeight();
            if (returnScreen.width != width || returnScreen.height != height) {
                returnScreen.setWorldAndResolution(minecraft, width, height);
            }
        }
    }
}
