package neofontrender.client.gui;

import com.cleanroommc.modularui.api.GuiAxis;
import com.cleanroommc.modularui.api.layout.ILayoutWidget;
import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.api.value.IDoubleValue;
import com.cleanroommc.modularui.api.value.IStringValue;
import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.drawable.Rectangle;
import com.cleanroommc.modularui.factory.ClientGUI;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.ModularScreen;
import com.cleanroommc.modularui.screen.RichTooltip;
import com.cleanroommc.modularui.screen.viewport.ModularGuiContext;
import com.cleanroommc.modularui.theme.WidgetThemeEntry;
import com.cleanroommc.modularui.utils.Alignment;
import com.cleanroommc.modularui.utils.Platform;
import com.cleanroommc.modularui.widget.ParentWidget;
import com.cleanroommc.modularui.widget.Widget;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.cleanroommc.modularui.widgets.ListWidget;
import com.cleanroommc.modularui.widget.ScrollWidget;
import com.cleanroommc.modularui.widget.scroll.VerticalScrollData;
import com.cleanroommc.modularui.widgets.SliderWidget;
import com.cleanroommc.modularui.widgets.TextWidget;
import com.cleanroommc.modularui.widgets.menu.ContextMenuButton;
import com.cleanroommc.modularui.widgets.textfield.TextFieldWidget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.resources.I18n;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.fml.common.Loader;
import neofontrender.core.config.NeofontrenderConfig;
import neofontrender.core.font.FontManager;
import neofontrender.core.font.skia.SkijaRuntimeSupport;
import neofontrender.core.font.support.FontRenderTuning;

import java.awt.Desktop;
import java.awt.GraphicsEnvironment;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * ModularUI-based client-only font selection panel.
 */
@SideOnly(Side.CLIENT)
public final class NeofontrenderConfigScreen {

    private static final String[] AA_MODES = {
            "off", "on", "gasp", "lcd_hrgb", "lcd_hbgr", "lcd_vrgb", "lcd_vbgr"
    };
    private static final int SOURCE_SYSTEM = 0;
    private static final int SOURCE_FOLDER = 1;
    private static final int SOURCE_BUILTIN = 2;
    private static final int TARGET_PRIMARY = 0;
    private static final int TARGET_FALLBACK = 1;
    private static final int TARGET_COSMIC_REGULAR = 2;
    private static final int TARGET_COSMIC_BOLD = 3;
    private static final int TARGET_COSMIC_ITALIC = 4;
    private static final int TARGET_COSMIC_BOLD_ITALIC = 5;
    private static final int TARGET_SHADOW_MASK = 6;
    private static final int TARGET_COUNT = 7;
    /**
     * Single source of truth for the settings navigation. Page layouts can still use the stable
     * numeric id while controls only need to know their page metadata.
     */
    private enum SettingsPage {
        FONT(0, "neofontrender.gui.category.font"),
        GENERAL(1, "neofontrender.gui.category.general"),
        RENDERING(2, "neofontrender.gui.category.rendering"),
        PERFORMANCE(3, "neofontrender.gui.category.performance"),
        ADVANCED(4, "neofontrender.gui.category.advanced"),
        CACHE(5, "neofontrender.gui.category.cache"),
        SHADOW(8, "neofontrender.gui.category.shadow"),
        FIXES(9, "neofontrender.gui.category.fixes"),
        LABORATORY(10, "neofontrender.gui.category.laboratory"),
        ABOUT(6, "neofontrender.gui.category.about"),
        LICENSES(7, "neofontrender.gui.category.licenses");

        private final int id;
        private final String titleKey;

        SettingsPage(int id, String titleKey) {
            this.id = id;
            this.titleKey = titleKey;
        }

        private static SettingsPage byId(int id) {
            for (SettingsPage page : values()) {
                if (page.id == id) return page;
            }
            return FONT;
        }
    }

    private static final int PAGE_FONT = SettingsPage.FONT.id;
    private static final int PAGE_GENERAL = SettingsPage.GENERAL.id;
    private static final int PAGE_RENDERING = SettingsPage.RENDERING.id;
    private static final int PAGE_PERFORMANCE = SettingsPage.PERFORMANCE.id;
    private static final int PAGE_ADVANCED = SettingsPage.ADVANCED.id;
    private static final int PAGE_CACHE = SettingsPage.CACHE.id;
    private static final int PAGE_ABOUT = SettingsPage.ABOUT.id;
    private static final int PAGE_LICENSES = SettingsPage.LICENSES.id;
    private static final int PAGE_SHADOW = SettingsPage.SHADOW.id;
    private static final int PAGE_FIXES = SettingsPage.FIXES.id;
    private static final int PAGE_LABORATORY = SettingsPage.LABORATORY.id;

    private static net.minecraft.client.gui.GuiScreen returnScreen;

    private NeofontrenderConfigScreen() {
    }

    public static void open() {
        open(null);
    }

    public static void open(net.minecraft.client.gui.GuiScreen parent) {
        returnScreen = parent;
        openMain(new Staged());
    }

    private static void closeToParent() {
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getMinecraft();
        if (returnScreen != null) {
            mc.displayGuiScreen(returnScreen);
        } else {
            ClientGUI.close();
        }
    }

    private static void openMain(Staged staged) {
        ClientGUI.open(createScreen(buildPanel(staged)));
    }

    private static void openAdvanced(Staged staged) {
        openAdvanced(staged, PAGE_ADVANCED);
    }

    private static void openAdvanced(Staged staged, int page) {
        ClientGUI.open(createScreen(buildAdvancedPanel(staged, page)));
    }

    private static ModularScreen createScreen(ModularPanel panel) {
        return new ParentBackgroundScreen(panel).useTheme("neofontrender_modern").pausesGame(false);
    }

    private static final class ParentBackgroundScreen extends ModularScreen {
        private ParentBackgroundScreen(ModularPanel panel) {
            super("neofontrender", panel);
        }

        @Override
        public void drawScreen() {
            Minecraft mc = Minecraft.getMinecraft();
            if (mc.world == null && returnScreen != null) {
                // Vanilla settings screens render only their background stage. Repainting the
                // whole parent screen here leaves its buttons ghosted behind this UI.
                syncParentBackgroundSize(mc);
                returnScreen.drawDefaultBackground();
            }
            super.drawScreen();
        }

        @Override
        public void onResize(int width, int height) {
            super.onResize(width, height);
            syncParentBackgroundSize(Minecraft.getMinecraft());
        }

        private static void syncParentBackgroundSize(Minecraft mc) {
            if (returnScreen == null || mc.world != null) return;
            ScaledResolution resolution = new ScaledResolution(mc);
            int width = resolution.getScaledWidth();
            int height = resolution.getScaledHeight();
            if (returnScreen.width != width || returnScreen.height != height) {
                // GuiScreen owns the dimensions used by drawDefaultBackground. It is no longer
                // the active screen, so Forge does not resize it when the window/GUI scale changes.
                returnScreen.setWorldAndResolution(mc, width, height);
            }
        }
    }

    private static ModularPanel buildPanel(Staged staged) {
        ModularPanel panel = new ModularPanel("font_config")
                .relativeToScreen()
                .full();
        configureRootBackground(panel);
        panel.child(new FontConfigLayout(staged).relativeToParent().full());
        return panel;
    }

    private static ModularPanel buildAdvancedPanel(Staged staged, int page) {
        ModularPanel panel = new ModularPanel("font_config_advanced")
                .relativeToScreen()
                .full();
        configureRootBackground(panel);
        panel.child(new AdvancedConfigLayout(staged, page).relativeToParent().full());
        return panel;
    }

    private static void configureRootBackground(ModularPanel panel) {
        if (Minecraft.getMinecraft().world == null) {
            // Vanilla menu screens already paint their own background. Keep the full-screen dimmer
            // only in world, where it separates the settings from the moving scene behind it.
            panel.disableThemeBackground(true);
            // A full-screen panel is always hovered. ModularUI otherwise falls back from its hover
            // theme to the normal panel background and paints the dimmer despite the flag above.
            panel.disableHoverThemeBackground(true);
        }
    }

    private static TextWidget header(String text) {
        return new TextWidget(IKey.str(text)).alignment(Alignment.CenterLeft).color(0xFFFFFF);
    }

    private static TextWidget label(String text) {
        return new TextWidget(IKey.str(text)).alignment(Alignment.CenterLeft).color(0xA9B5C5);
    }

    private static TextWidget dynamicLabel(Supplier<String> text) {
        return new TextWidget(IKey.dynamic(text)).alignment(Alignment.CenterLeft).color(0xA9B5C5);
    }

    private static ButtonWidget<?> actionButton(String text, int width, int height, Runnable action) {
        return new TextButton(() -> text, true)
                .size(width, height)
                .onMousePressed(mouseButton -> {
                    action.run();
                    return true;
                });
    }

    private static ButtonWidget<?> actionButtonKey(String key, int width, int height, Runnable action) {
        return actionButton(tr(key), width, height, action);
    }

    private static ButtonWidget<?> toggleButtonKey(String labelKey, String tooltipKey, int width, int height,
                                                   Supplier<Boolean> getter, Consumer<Boolean> setter, Runnable preview) {
        return withTooltip(new ContentButton(() -> tr(labelKey), false, new ToggleIndicator(getter))
                .size(width, height)
                .onMousePressed(mouseButton -> {
                    setter.accept(!getter.get());
                    preview.run();
                    return true;
                }), tooltipKey);
    }

    private static <T extends Widget<?>> T withTooltip(T button, String tooltipKey) {
        if (tooltipKey != null && !tooltipKey.isEmpty()) {
            button.tooltip(new RichTooltip()
                    .showUpTimer(8)
                    .addLine(tr(tooltipKey)));
        }
        return button;
    }

    /**
     * A labeled dropdown for finite settings. The selected display retains the option-row format,
     * while menu items show only their value.
     */
    private static OptionDropdown selectionDropdown(String name, Supplier<String> label,
                                                    Supplier<String> getter, Consumer<String> setter,
                                                    Iterable<String> values,
                                                    java.util.function.Function<String, String> display) {
        return selectionDropdown(name, label, getter, setter, values, display, false);
    }

    private static OptionDropdown compactDropdown(String name, Supplier<String> getter, Consumer<String> setter,
                                                  Iterable<String> values,
                                                  java.util.function.Function<String, String> display) {
        return selectionDropdown(name, () -> "", getter, setter, values, display, true);
    }

    private static OptionDropdown selectionDropdown(String name, Supplier<String> label,
                                                    Supplier<String> getter, Consumer<String> setter,
                                                    Iterable<String> values,
                                                    java.util.function.Function<String, String> display,
                                                    boolean compact) {
        return new OptionDropdown(name, label, getter, setter, values, display, compact);
    }

    /**
     * Keeps ModularUI's menu behavior, but renders the selected state ourselves so a dropdown
     * remains a stable one-line option row at every GUI scale.
     */
    private static final class OptionDropdown extends ContextMenuButton<OptionDropdown> {
        private final Supplier<String> label;
        private final Supplier<String> getter;
        private final Consumer<String> setter;
        private final java.util.function.Function<String, String> display;
        private final boolean compact;

        private OptionDropdown(String name, Supplier<String> label, Supplier<String> getter, Consumer<String> setter,
                               Iterable<String> values, java.util.function.Function<String, String> display,
                               boolean compact) {
            super(name);
            this.label = label;
            this.getter = getter;
            this.setter = setter;
            this.display = display;
            this.compact = compact;
            requiresClick();
            menuList(list -> {
                list.maxSize(144);
                for (String value : values) {
                    TextButton option = new TextButton(() -> display.apply(value), false)
                            .height(20)
                            .onMousePressed(mouseButton -> {
                                setter.accept(value);
                                closeMenu(false);
                                return true;
                            });
                    // ListWidget does not infer a TextButton's width from its menu parent.
                    // Pin every option to the menu width so CJK labels stay on one line.
                    option.relativeToParent().fullWidth();
                    list.child(option);
                }
            });
        }

        @Override
        public void draw(ModularGuiContext context, WidgetThemeEntry<?> widgetTheme) {
            super.draw(context, widgetTheme);
            Platform.setupDrawFont();
            Minecraft mc = Minecraft.getMinecraft();
            int width = getArea().w();
            int height = getArea().h();
            String value = display.apply(getter.get());
            int textY = Math.max(0, (height - mc.fontRenderer.FONT_HEIGHT) / 2);

            if (compact) {
                String visible = mc.fontRenderer.trimStringToWidth(value, Math.max(1, width - 22));
                int textWidth = mc.fontRenderer.getStringWidth(visible);
                mc.fontRenderer.drawString(visible, Math.max(4, (width - 14 - textWidth) / 2), textY, 0xFFFFFF);
            } else {
                // Match TextButton's label inset so dropdown and switch rows share one text column.
                int inset = 4;
                String left = mc.fontRenderer.trimStringToWidth(label.get(), Math.max(1, width / 2 - inset - 8));
                String right = mc.fontRenderer.trimStringToWidth(value, Math.max(1, width / 2 - 18));
                mc.fontRenderer.drawString(left, inset, textY, 0xFFFFFF);
                mc.fontRenderer.drawString(right, Math.max(inset, width - mc.fontRenderer.getStringWidth(right) - 18), textY, 0xE0E0E0);
            }

            int arrowX = Math.max(4, width - 10);
            int arrowY = Math.max(3, height / 2 - 2);
            Gui.drawRect(arrowX, arrowY, arrowX + 5, arrowY + 1, 0xFFB8C2D0);
            Gui.drawRect(arrowX + 1, arrowY + 1, arrowX + 4, arrowY + 2, 0xFFB8C2D0);
            Gui.drawRect(arrowX + 2, arrowY + 2, arrowX + 3, arrowY + 3, 0xFFB8C2D0);
        }
    }

    /** Retained for settings where cycling is more efficient than opening a value list. */
    private static final class CycleButton extends TextButton {
        private CycleButton(Supplier<String> label, Runnable cycle) {
            super(label, true);
            onMousePressed(mouseButton -> {
                cycle.run();
                return true;
            });
        }
    }

    private static IWidget sourceDropdown(Staged staged, FilteredFontList[] listRef) {
        return compactDropdown("font_source",
                () -> Integer.toString(staged.fontSource), value -> {
                    staged.fontSource = Integer.parseInt(value);
                    if (listRef[0] != null) {
                        listRef[0].reloadFonts();
                    }
                }, Arrays.asList("0", "1", "2"), value -> sourceButtonName(Integer.parseInt(value)));
    }

    private static String sourceButtonName(int source) {
        switch (source) {
            case SOURCE_FOLDER:
                return tr("neofontrender.gui.source.game");
            case SOURCE_BUILTIN:
                return tr("neofontrender.gui.source.builtin");
            case SOURCE_SYSTEM:
            default:
                return tr("neofontrender.gui.source.system");
        }
    }

    private static String sourceTitle(int source) {
        switch (source) {
            case SOURCE_FOLDER:
                return tr("neofontrender.gui.source_title.game");
            case SOURCE_BUILTIN:
                return tr("neofontrender.gui.source_title.builtin");
            case SOURCE_SYSTEM:
            default:
                return tr("neofontrender.gui.source_title.system");
        }
    }

    private static String fontTargetName(int target) {
        switch (target) {
            case TARGET_FALLBACK:
                return tr("neofontrender.gui.target.fallback");
            case TARGET_COSMIC_REGULAR:
                return tr("neofontrender.gui.target.cosmic_regular");
            case TARGET_COSMIC_BOLD:
                return tr("neofontrender.gui.target.cosmic_bold");
            case TARGET_COSMIC_ITALIC:
                return tr("neofontrender.gui.target.cosmic_italic");
            case TARGET_COSMIC_BOLD_ITALIC:
                return tr("neofontrender.gui.target.cosmic_bold_italic");
            case TARGET_SHADOW_MASK:
                return tr("neofontrender.gui.target.shadow_mask");
            case TARGET_PRIMARY:
            default:
                return tr("neofontrender.gui.target.primary");
        }
    }

    private static String categoryName(int page) {
        return tr(SettingsPage.byId(page).titleKey);
    }

    private static ButtonWidget<?> toggleButton(String text, int width, int height, Supplier<Boolean> getter, Consumer<Boolean> setter, Runnable preview) {
        return new TextButton(() -> text + ": " + (getter.get() ? "ON" : "OFF"), true)
                .size(width, height)
                .onMousePressed(mouseButton -> {
                    setter.accept(!getter.get());
                    preview.run();
                    return true;
                });
    }

    private static IWidget styleButton(Staged staged, int width, int height) {
        return withTooltip(selectionDropdown("font_style", () -> tr("neofontrender.gui.option.style"),
                () -> Integer.toString(staged.fontStyle), value -> {
                    staged.fontStyle = Integer.parseInt(value);
                    preview(staged);
                }, Arrays.asList("0", "1", "2", "3"), value -> styleName(Integer.parseInt(value)))
                .size(width, height), "neofontrender.tooltip.style");
    }

    private static IWidget shadowModeButton(Staged staged, int width, int height) {
        return withTooltip(selectionDropdown("shadow_mode", () -> tr("neofontrender.gui.option.shadow_mode"),
                () -> staged.shadowMode, value -> {
                    staged.shadowMode = value;
                    preview(staged);
                }, Arrays.asList("mask", "emoji", "all", "none"), value -> tr("neofontrender.gui.shadow_mode." + value))
                .size(width, height), "neofontrender.tooltip.shadow_mode");
    }

    private static IWidget shadowValueButton(Staged staged, boolean opacity, int width, int height) {
        List<String> values = opacity
                ? Arrays.asList("0.0", "0.1", "0.2", "0.3", "0.4", "0.5", "0.6", "0.7", "0.8", "0.9", "1.0")
                : Arrays.asList("0.0", "0.5", "1.0", "1.5", "2.0", "2.5", "3.0", "3.5", "4.0");
        return selectionDropdown("shadow_" + (opacity ? "opacity" : "length"),
                () -> tr(opacity ? "neofontrender.gui.option.shadow_opacity" : "neofontrender.gui.option.shadow_length"),
                () -> String.format(Locale.ROOT, "%.1f", opacity ? staged.shadowOpacity : staged.shadowLength), value -> {
                    if (opacity) staged.shadowOpacity = Float.parseFloat(value);
                    else staged.shadowLength = Float.parseFloat(value);
                    preview(staged);
                }, values, value -> value).size(width, height);
    }

    private static IWidget aaModeButton(Staged staged, int width, int height) {
        return withTooltip(selectionDropdown("antialias", () -> tr("neofontrender.gui.option.antialias"),
                () -> staged.antialiasMode, value -> {
                    staged.antialiasMode = value;
                    staged.antialias = !"off".equals(value);
                    preview(staged);
                }, Arrays.asList(AA_MODES), NeofontrenderConfigScreen::aaModeName)
                .size(width, height), "neofontrender.tooltip.antialias");
    }

    private static IWidget engineButton(Staged staged, int page, int width, int height) {
        List<String> engines = skiaAvailable()
                ? Arrays.asList("sfr", "skia", "cosmic", "vanilla")
                : Arrays.asList("sfr", "cosmic", "vanilla");
        return withTooltip(selectionDropdown("engine", () -> tr("neofontrender.gui.option.engine"),
                () -> normalizeEngine(staged.engine), value -> {
                    staged.engine = value;
                    staged.enabled = !"vanilla".equals(value);
                    reloadAndReopen(staged, page);
                }, engines, NeofontrenderConfigScreen::engineName)
                .size(width, height), "neofontrender.tooltip.engine");
    }

    private static void reloadAndReopen(Staged staged, int page) {
        staged.writeToConfig(false);
        reloadFontManager();
        // Reload destroys backend-owned glyph textures. Rebuild the active widget tree so no
        // fixed label can retain render state created by the previous engine.
        if (page == PAGE_FONT) {
            openMain(staged);
        } else {
            openAdvanced(staged, page);
        }
    }

    private static String nextEngine(String engine) {
        String normalized = normalizeEngine(engine);
        if ("sfr".equals(normalized)) {
            return skiaAvailable() ? "skia" : "cosmic";
        }
        if ("skia".equals(normalized)) {
            return "cosmic";
        }
        if ("cosmic".equals(normalized)) {
            return "vanilla";
        }
        return "sfr";
    }

    private static String engineName(String engine) {
        String normalized = normalizeEngine(engine);
        if ("skia".equals(normalized)) {
            return tr("neofontrender.gui.engine.skia")
                    + (skiaAvailable() ? "" : " (" + tr("neofontrender.gui.unavailable") + ")");
        }
        if ("cosmic".equals(normalized)) {
            return tr("neofontrender.gui.engine.cosmic");
        }
        if ("vanilla".equals(normalized)) {
            return tr("neofontrender.gui.engine.vanilla");
        }
        return tr("neofontrender.gui.engine.sfr");
    }

    private static boolean skiaAvailable() {
        return SkijaRuntimeSupport.checkCompatibility().isSupported();
    }

    private static String normalizeEngine(String engine) {
        if (engine == null) {
            return "sfr";
        }
        String normalized = engine.trim().toLowerCase(Locale.ROOT).replace('-', '_');
        if ("skija".equals(normalized) || "skia".equals(normalized)) {
            return "skia";
        }
        if ("cosmic_text".equals(normalized) || "cosmic".equals(normalized)) {
            return "cosmic";
        }
        if ("vanilla".equals(normalized) || "original".equals(normalized) || "minecraft".equals(normalized)) {
            return "vanilla";
        }
        return "sfr";
    }

    private static String nextAaMode(String mode) {
        String normalized = normalizeAaMode(mode);
        for (int i = 0; i < AA_MODES.length; i++) {
            if (AA_MODES[i].equals(normalized)) {
                return AA_MODES[(i + 1) % AA_MODES.length];
            }
        }
        return "on";
    }

    private static String aaModeName(String mode) {
        String normalized = normalizeAaMode(mode);
        switch (normalized) {
            case "off":
                return tr("neofontrender.gui.aa.off");
            case "gasp":
                return tr("neofontrender.gui.aa.gasp");
            case "lcd_hrgb":
                return tr("neofontrender.gui.aa.lcd_hrgb");
            case "lcd_hbgr":
                return tr("neofontrender.gui.aa.lcd_hbgr");
            case "lcd_vrgb":
                return tr("neofontrender.gui.aa.lcd_vrgb");
            case "lcd_vbgr":
                return tr("neofontrender.gui.aa.lcd_vbgr");
            default:
                return tr("neofontrender.gui.aa.on");
        }
    }

    private static String normalizeAaMode(String mode) {
        if (mode == null) {
            return "on";
        }
        String normalized = mode.trim().toLowerCase(Locale.ROOT).replace('-', '_');
        for (String allowed : AA_MODES) {
            if (allowed.equals(normalized)) {
                return normalized;
            }
        }
        return "on";
    }

    private static String styleName(int style) {
        switch (style & 3) {
            case 1:
                return tr("neofontrender.gui.style.bold");
            case 2:
                return tr("neofontrender.gui.style.italic");
            case 3:
                return tr("neofontrender.gui.style.bold_italic");
            default:
                return tr("neofontrender.gui.style.plain");
        }
    }

    private static List<String> localFonts() {
        try {
            String[] names = GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames(Locale.ROOT);
            Set<String> installed = new LinkedHashSet<>();
            for (String name : names) {
                installed.add(name.toLowerCase(Locale.ROOT));
            }
            List<String> fonts = new ArrayList<>();
            for (String name : names) {
                if (!isExposedStyleFamily(name, installed)) {
                    fonts.add(name);
                }
            }
            Collections.sort(fonts, String.CASE_INSENSITIVE_ORDER);
            return fonts;
        } catch (Throwable t) {
            return Collections.singletonList("SansSerif");
        }
    }

    private static boolean isExposedStyleFamily(String name, Set<String> installedFamilies) {
        String candidate = name;
        boolean stripped = false;
        String[] suffixes = {
                " Extra Bold", " ExtraBold", " Demi Bold", " DemiBold",
                " Semi Bold", " SemiBold", " Extra Light", " ExtraLight",
                " Bold Italic", " Bold Oblique", " Italic", " Oblique",
                " Regular", " Normal", " Medium", " Semilight", " DemiLight",
                " Light", " Thin", " Bold", " Heavy", " Black"
        };
        boolean changed;
        do {
            changed = false;
            for (String suffix : suffixes) {
                if (candidate.length() > suffix.length()
                        && candidate.regionMatches(true, candidate.length() - suffix.length(),
                        suffix, 0, suffix.length())) {
                    candidate = candidate.substring(0, candidate.length() - suffix.length()).trim();
                    stripped = true;
                    changed = true;
                    break;
                }
            }
        } while (changed);
        return stripped && installedFamilies.contains(candidate.toLowerCase(Locale.ROOT));
    }

    private static List<FontEntry> fontFolderFonts() {
        File dir = NeofontrenderConfig.ensureFontDirectory();
        File[] files = dir.listFiles(file -> file.isFile() && isFontFile(file.getName()));
        if (files == null || files.length == 0) {
            return Collections.emptyList();
        }
        Arrays.sort(files, (a, b) -> a.getName().compareToIgnoreCase(b.getName()));
        List<FontEntry> fonts = new ArrayList<>();
        for (File file : files) {
            fonts.add(new FontEntry(file.getName(), file.getAbsolutePath()));
        }
        return fonts;
    }

    private static boolean isFontFile(String name) {
        String lower = name.toLowerCase(Locale.ROOT);
        return lower.endsWith(".ttf") || lower.endsWith(".otf") || lower.endsWith(".ttc");
    }

    private static List<FontEntry> builtinFonts() {
        List<FontEntry> fonts = new ArrayList<>();
        for (NeofontrenderConfig.BuiltinFont font : NeofontrenderConfig.builtinFonts()) {
            fonts.add(new FontEntry(font.displayName(), font.location()));
        }
        return fonts;
    }

    private static void openFontFolder() {
        File dir = NeofontrenderConfig.ensureFontDirectory();
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(dir);
            }
        } catch (IOException | RuntimeException e) {
            neofontrender.NeoFontRender.LOGGER.error("Failed to open font folder '{}'", dir, e);
        }
    }

    private static void preview(Staged staged) {
        if (!staged.enabled || "vanilla".equals(normalizeEngine(staged.engine))) {
            // Preview runs while this screen is still drawing through the current backend. Tearing
            // that backend down here invalidates textures already referenced by fixed UI labels.
            // The disabled/Vanilla state is committed by Apply, which immediately closes the UI.
            return;
        }
        staged.writeToConfig(false);
        reloadFontManager();
    }

    private static void apply(Staged staged) {
        staged.writeToConfig(true);
        reloadFontManager();
    }

    private static void reloadFontManager() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.getResourceManager() != null) {
            FontManager.INSTANCE.reload(mc.getResourceManager());
        }
    }

    private static void place(IWidget child, int x, int y, int width, int height) {
        int absoluteX = x;
        int absoluteY = y;
        if (child.hasParent()) {
            absoluteX += child.getParent().getArea().x();
            absoluteY += child.getParent().getArea().y();
        }
        child.getArea().setRelativePoint(GuiAxis.X, x);
        child.getArea().setRelativePoint(GuiAxis.Y, y);
        child.getArea().setPoint(GuiAxis.X, absoluteX);
        child.getArea().setPoint(GuiAxis.Y, absoluteY);
        child.getArea().setSize(GuiAxis.X, Math.max(0, width));
        child.getArea().setSize(GuiAxis.Y, Math.max(0, height));
        child.resizer().setPosResized(true, true);
        child.resizer().setSizeResized(true, true);
        child.resizer().setMarginPaddingApplied(true);
        if (child instanceof ILayoutWidget) {
            ((ILayoutWidget) child).layoutWidgets();
        }
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    /**
     * Owns the repeated ScrollWidget/content pairing used by settings pages. In particular, the
     * restored position is applied once, rather than during every ModularUI resize pass.
     */
    private static final class ScrollablePane {
        private final ScrollWidget<?> viewport = new ScrollWidget<>(new VerticalScrollData());
        private final IWidget content;
        private boolean restored;

        private ScrollablePane(IWidget content) {
            this.content = content;
            viewport.child(content);
        }

        private ScrollWidget<?> widget() {
            return viewport;
        }

        private void layout(int x, int y, int width, int height, int contentWidth, int contentHeight) {
            place(viewport, x, y, width, height);
            place(content, 0, 0, contentWidth, contentHeight);
            viewport.getScrollArea().getScrollY().setScrollSize(contentHeight);
        }

        private void restoreScrollOnce(int scrollY) {
            if (!restored) {
                viewport.getScrollArea().getScrollY().scrollTo(viewport.getScrollArea(), scrollY);
                restored = true;
            }
        }

        private int scrollY() {
            return viewport.getScrollArea().getScrollY().getScroll();
        }
    }

    private static final class FontConfigLayout extends ParentWidget<FontConfigLayout> implements ILayoutWidget {
        private static final int PAD = 12;
        private static final int GAP = 12;

        private final TextWidget title = header(tr("neofontrender.gui.title"));
        private final Staged staged;
        private final CategoryRail categoryRail;
        private final ScrollablePane categoryPane;
        private final FontSidebar sidebar;
        private final SettingsPane settings;
        private final ScrollablePane settingsPane;
        private final ButtonWidget<?> previewButton;
        private final ButtonWidget<?> applyButton;
        private final ButtonWidget<?> cancelButton;

        private FontConfigLayout(Staged staged) {
            this.staged = staged;
            FontFieldRefs fieldRefs = new FontFieldRefs();
            FilteredFontList[] listRef = new FilteredFontList[1];

            this.sidebar = new FontSidebar(staged, fieldRefs, listRef);
            this.settings = new SettingsPane(staged, fieldRefs);
            this.settingsPane = new ScrollablePane(settings);
            this.categoryRail = new CategoryRail(staged, PAGE_FONT);
            this.categoryPane = new ScrollablePane(categoryRail);
            this.previewButton = actionButtonKey("neofontrender.gui.button.preview", 90, 20, () -> preview(staged));
            this.applyButton = actionButtonKey("neofontrender.gui.button.apply", 74, 20, () -> {
                apply(staged);
                closeToParent();
            });
            this.cancelButton = actionButtonKey("neofontrender.gui.button.cancel", 74, 20, () -> {
                staged.restoreOriginal();
                reloadFontManager();
                closeToParent();
            });

            child(title);
            child(categoryPane.widget());
            child(sidebar);
            child(settingsPane.widget());
            child(previewButton);
            child(applyButton);
            child(cancelButton);
        }

        @Override
        public boolean layoutWidgets() {
            int width = getArea().w();
            int height = getArea().h();
            int footerHeight = 24;
            int contentTop = PAD + 28;
            int contentBottom = height - PAD - footerHeight - GAP;
            int contentHeight = Math.max(0, contentBottom - contentTop);

            place(title, PAD, PAD, Math.max(0, width - PAD * 2), 16);

            // Keep the navigation rail close to the compact options-screen proportion instead of
            // letting it consume one third of a wide monitor.
            int railWidth = clamp(width / 8, 104, 150);
            int sidebarWidth = clamp(width / 5, 180, 240);
            int sidebarX = PAD + railWidth + GAP;
            int settingsX = sidebarX + sidebarWidth + GAP;
            int categoryHeight = categoryRail.preferredHeight();
            categoryPane.layout(PAD, contentTop, railWidth, contentHeight, railWidth, categoryHeight);
            categoryPane.restoreScrollOnce(staged.categoryScroll);
            place(sidebar, sidebarX, contentTop, sidebarWidth, contentHeight);
            int settingsWidth = Math.max(0, width - settingsX - PAD);
            int settingsContentHeight = Math.max(contentHeight, settings.preferredHeight());
            settingsPane.layout(settingsX, contentTop, settingsWidth, contentHeight,
                    Math.max(0, settingsWidth - 6), settingsContentHeight);

            int buttonWidth = Math.min(160, Math.max(90, (width - PAD * 2 - GAP * 2) / 3));
            int y = height - PAD - footerHeight;
            place(previewButton, PAD, y, buttonWidth, footerHeight);
            place(cancelButton, width - PAD - buttonWidth, y, buttonWidth, footerHeight);
            place(applyButton, Math.max(PAD, width - PAD - buttonWidth * 2 - GAP), y, buttonWidth, footerHeight);
            return true;
        }
    }

    private static final class AdvancedConfigLayout extends ParentWidget<AdvancedConfigLayout> implements ILayoutWidget {
        private static final int PAD = 12;
        private static final int GAP = 12;

        private final TextWidget title;
        private final Staged staged;
        private final CategoryRail categoryRail;
        private final ScrollablePane categoryPane;
        private final PipelineInfoWidget pipelineInfo;
        private final OptionsGrid optionsGrid;
        private final BrightnessSection brightness;
        private final CacheSettingsPane cacheSettings;
        private final InfoPage infoPage;
        private final ButtonWidget<?> backButton;
        private final ButtonWidget<?> applyButton;
        private final ButtonWidget<?> cancelButton;
        private final int page;
        private final ScrollablePane contentPane;
        private final ParentWidget<?> contentRoot;

        private AdvancedConfigLayout(Staged staged, int page) {
            this.staged = staged;
            this.page = page;
            this.title = header(tr("neofontrender.gui.title") + " / " + categoryName(page));
            this.categoryRail = new CategoryRail(staged, page);
            this.categoryPane = new ScrollablePane(categoryRail);
            this.pipelineInfo = new PipelineInfoWidget(staged);
            this.optionsGrid = buildAdvancedOptionsGrid(staged, page);
            this.brightness = new BrightnessSection(staged);
            this.cacheSettings = new CacheSettingsPane(staged);
            this.infoPage = new InfoPage(page, staged);
            this.contentRoot = new ParentWidget<>();
            // Keep only the active category in the tree. Disabled widgets still retain layout
            // state in ModularUI, so merely disabling the other pages can leave them visible.
            if (page == PAGE_ADVANCED) {
                this.contentRoot.child(pipelineInfo);
                this.contentRoot.child(optionsGrid);
            } else if (page == PAGE_CACHE) {
                this.contentRoot.child(cacheSettings);
            } else if (page == PAGE_ABOUT || page == PAGE_LICENSES) {
                this.contentRoot.child(infoPage);
            } else {
                this.contentRoot.child(optionsGrid);
                this.contentRoot.child(brightness);
            }
            this.contentPane = new ScrollablePane(contentRoot);
            this.backButton = actionButtonKey("neofontrender.gui.button.back", 80, 20, () -> openMain(staged));
            this.applyButton = actionButtonKey("neofontrender.gui.button.apply", 80, 20, () -> {
                apply(staged);
                closeToParent();
            });
            this.cancelButton = actionButtonKey("neofontrender.gui.button.cancel", 80, 20, () -> {
                staged.restoreOriginal();
                reloadFontManager();
                closeToParent();
            });

            child(title);
            child(categoryPane.widget());
            child(contentPane.widget());
            child(backButton);
            child(applyButton);
            child(cancelButton);
        }

        @Override
        public boolean layoutWidgets() {
            int width = getArea().w();
            int height = getArea().h();
            int footerHeight = 24;
            int y = PAD;

            place(title, PAD, y, Math.max(0, width - PAD * 2), 16);
            y += 28;

            int railWidth = clamp(width / 8, 104, 150);
            int contentX = PAD + railWidth + GAP;
            int contentWidth = Math.max(0, width - contentX - PAD);
            int contentHeight = Math.max(0, height - y - PAD - footerHeight - GAP);
            int categoryHeight = categoryRail.preferredHeight();
            categoryPane.layout(PAD, y, railWidth, contentHeight, railWidth, categoryHeight);
            categoryPane.restoreScrollOnce(staged.categoryScroll);
            int rootY = 0;

            if (page == PAGE_ADVANCED) {
                int infoHeight = pipelineInfo.preferredHeight();
                place(pipelineInfo, 0, rootY, Math.max(0, contentWidth - 6), infoHeight);
                rootY += infoHeight + GAP;
            } else {
                pipelineInfo.setEnabled(false);
            }

            if (page == PAGE_CACHE) {
                optionsGrid.setEnabled(false);
                int cacheHeight = cacheSettings.preferredHeight(contentWidth);
                place(cacheSettings, 0, rootY, Math.max(0, contentWidth - 6), cacheHeight);
                rootY += cacheHeight;
                infoPage.setEnabled(false);
            } else if (page == PAGE_ABOUT || page == PAGE_LICENSES) {
                optionsGrid.setEnabled(false);
                cacheSettings.setEnabled(false);
                int infoHeight = infoPage.preferredHeight();
                place(infoPage, 0, rootY, Math.max(0, contentWidth - 6), infoHeight);
                rootY += infoHeight;
            } else {
                cacheSettings.setEnabled(false);
                infoPage.setEnabled(false);
                int optionsHeight = optionsGrid.preferredHeight(contentWidth);
                place(optionsGrid, 0, rootY, Math.max(0, contentWidth - 6), optionsHeight);
                rootY += optionsHeight + GAP;
            }

            if (page == PAGE_RENDERING) {
                place(brightness, 0, rootY, Math.max(0, contentWidth - 6), brightness.preferredHeight());
                rootY += brightness.preferredHeight();
            } else {
                brightness.setEnabled(false);
            }
            int rootHeight = Math.max(contentHeight, rootY);
            contentPane.layout(contentX, y, contentWidth, contentHeight, contentWidth, rootHeight);

            // Match the font page: one auxiliary action on the left, with Apply and Cancel grouped right.
            int buttonWidth = Math.min(160, Math.max(90, (width - PAD * 2 - GAP * 2) / 3));
            int footerY = height - PAD - footerHeight;
            place(backButton, PAD, footerY, buttonWidth, footerHeight);
            place(cancelButton, width - PAD - buttonWidth, footerY, buttonWidth, footerHeight);
            place(applyButton, Math.max(PAD, width - PAD - buttonWidth * 2 - GAP), footerY, buttonWidth, footerHeight);
            return true;
        }
    }

    private static final class FontSidebar extends ParentWidget<FontSidebar> implements ILayoutWidget {
        private final TextWidget searchLabel = label(tr("neofontrender.gui.label.search_fonts"));
        private final TextFieldWidget searchField;
        private final IWidget targetButton;
        private final IWidget sourceButton;
        private final ButtonWidget<?> refreshButton;
        private final ButtonWidget<?> openFolderButton;
        private final TextWidget sourceTitle;
        private final FilteredFontList fontList;

        private FontSidebar(Staged staged, FontFieldRefs fieldRefs, FilteredFontList[] listRef) {
            this.searchField = new TextFieldWidget()
                    .setMaxLength(128)
                    .value(new StringValue(() -> staged.search, v -> {
                        staged.search = v;
                        if (listRef[0] != null) {
                            listRef[0].refresh();
                        }
                    }));
            List<String> targets = new ArrayList<>();
            for (int target = 0; target < TARGET_COUNT; target++) {
                targets.add(Integer.toString(target));
            }
            this.targetButton = selectionDropdown("font_target", () -> tr("neofontrender.gui.label.target"),
                    () -> Integer.toString(staged.fontTarget), value -> {
                        staged.fontTarget = Integer.parseInt(value);
                        if (listRef[0] != null) {
                            listRef[0].refresh();
                        }
                    }, targets, value -> fontTargetName(Integer.parseInt(value)));
            this.sourceButton = sourceDropdown(staged, listRef);
            this.refreshButton = actionButtonKey("neofontrender.gui.button.refresh", 72, 18, () -> {
                if (listRef[0] != null) {
                    listRef[0].reloadFonts();
                }
            });
            this.openFolderButton = actionButtonKey("neofontrender.gui.button.open_folder", 100, 18, NeofontrenderConfigScreen::openFontFolder);
            this.sourceTitle = dynamicLabel(() -> sourceTitle(staged.fontSource));
            this.fontList = new FilteredFontList(staged, fieldRefs);
            listRef[0] = this.fontList;

            child(searchLabel);
            child(searchField);
            child(targetButton);
            child(sourceButton);
            child(refreshButton);
            child(openFolderButton);
            child(sourceTitle);
            child(fontList);
        }

        @Override
        public boolean layoutWidgets() {
            int width = getArea().w();
            int height = getArea().h();
            int y = 0;
            place(searchLabel, 0, y, width, 12);
            y += 16;
            place(searchField, 0, y, width, 22);
            y += 28;

            place(targetButton, 0, y, width, 22);
            y += 28;

            int gap = 6;
            // Divide the exact search-field width. Fixed minimums previously made the third
            // control protrude into the settings pane on narrow UI scales.
            int available = Math.max(0, width - gap * 2);
            int first = available / 3;
            int second = available / 3;
            int third = Math.max(0, available - first - second);
            place(sourceButton, 0, y, first, 22);
            place(refreshButton, first + gap, y, second, 22);
            place(openFolderButton, first + second + gap * 2, y, third, 22);
            y += 32;

            place(sourceTitle, 0, y, width, 12);
            y += 18;
            place(fontList, 0, y, width, Math.max(30, height - y));
            return true;
        }
    }

    private static final class SettingsPane extends ParentWidget<SettingsPane> implements ILayoutWidget {
        private final Staged staged;
        private final LabeledTextField fontName;
        private final LabeledTextField fontPath;
        private final LabeledTextField fallbacks;
        private final LabeledTextField shadowMasks;
        private final LabeledTextField cosmicRegular;
        private final LabeledTextField cosmicBold;
        private final LabeledTextField cosmicItalic;
        private final LabeledTextField cosmicBoldItalic;
        private final ButtonWidget<?> cosmicVariantFontOnly;
        private final MetricsSection metrics;
        private final OversampleSection oversample;
        private final PreviewWidget preview;

        private SettingsPane(Staged staged, FontFieldRefs fieldRefs) {
            this.staged = staged;
            fieldRefs.fontName = new TextFieldWidget()
                    .setMaxLength(256)
                    .value(new StringValue(() -> staged.fontName, v -> staged.fontName = v));
            fieldRefs.fontPath = new TextFieldWidget()
                    .setMaxLength(512)
                    .value(new StringValue(() -> staged.fontPath, v -> staged.fontPath = v));

            this.fontName = new LabeledTextField(tr("neofontrender.gui.label.font_name"), fieldRefs.fontName);
            this.fontPath = new LabeledTextField(tr("neofontrender.gui.label.local_path"), fieldRefs.fontPath);
            fieldRefs.fallbacks = new TextFieldWidget()
                    .setMaxLength(512)
                    .value(new StringValue(() -> staged.fontFallbacks, v -> staged.fontFallbacks = v));
            this.fallbacks = new LabeledTextField(tr("neofontrender.gui.label.fallbacks"), fieldRefs.fallbacks);
            fieldRefs.shadowMasks = new TextFieldWidget().setMaxLength(512)
                    .value(new StringValue(() -> staged.shadowMaskFonts, v -> staged.shadowMaskFonts = v));
            this.shadowMasks = new LabeledTextField(tr("neofontrender.gui.label.shadow_masks"), fieldRefs.shadowMasks);
            fieldRefs.cosmicRegular = cosmicFaceField(() -> staged.cosmicRegular, v -> staged.cosmicRegular = v);
            this.cosmicRegular = new LabeledTextField(tr("neofontrender.gui.label.cosmic_regular"), fieldRefs.cosmicRegular);
            fieldRefs.cosmicBold = cosmicFaceField(() -> staged.cosmicBold, v -> staged.cosmicBold = v);
            this.cosmicBold = new LabeledTextField(tr("neofontrender.gui.label.cosmic_bold"), fieldRefs.cosmicBold);
            fieldRefs.cosmicItalic = cosmicFaceField(() -> staged.cosmicItalic, v -> staged.cosmicItalic = v);
            this.cosmicItalic = new LabeledTextField(tr("neofontrender.gui.label.cosmic_italic"), fieldRefs.cosmicItalic);
            fieldRefs.cosmicBoldItalic = cosmicFaceField(() -> staged.cosmicBoldItalic, v -> staged.cosmicBoldItalic = v);
            this.cosmicBoldItalic = new LabeledTextField(tr("neofontrender.gui.label.cosmic_bold_italic"), fieldRefs.cosmicBoldItalic);
            this.cosmicVariantFontOnly = toggleButtonKey(
                    "neofontrender.gui.option.cosmic_variant_font_only",
                    "neofontrender.tooltip.cosmic_variant_font_only",
                    260, 24,
                    () -> staged.cosmicVariantOverridesOnlySwitchFont,
                    v -> staged.cosmicVariantOverridesOnlySwitchFont = v,
                    () -> preview(staged));
            boolean cosmic = "cosmic".equals(normalizeEngine(staged.engine));
            this.cosmicRegular.setEnabled(cosmic);
            this.cosmicBold.setEnabled(cosmic);
            this.cosmicItalic.setEnabled(cosmic);
            this.cosmicBoldItalic.setEnabled(cosmic);
            this.cosmicVariantFontOnly.setEnabled(cosmic);
            this.metrics = new MetricsSection(staged);
            this.oversample = new OversampleSection(staged);
            this.preview = new PreviewWidget(staged);

            child(fontName);
            child(fontPath);
            child(fallbacks);
            child(shadowMasks);
            child(cosmicRegular);
            child(cosmicBold);
            child(cosmicItalic);
            child(cosmicBoldItalic);
            child(cosmicVariantFontOnly);
            child(metrics);
            child(oversample);
            child(preview);
        }

        private int preferredHeight() {
            int cosmicFields = "cosmic".equals(normalizeEngine(staged.engine)) ? 4 : 0;
            int cosmicOption = cosmicFields == 0 ? 0 : 32;
            return (5 + cosmicFields) * 46 + cosmicOption + 8 + oversample.preferredHeight() + 8 + 150;
        }

        @Override
        public boolean layoutWidgets() {
            int width = getArea().w();
            int height = getArea().h();
            int y = 0;
            int fieldHeight = 38;
            int gap = 8;

            place(fontName, 0, y, width, fieldHeight);
            y += fieldHeight + gap;
            place(fontPath, 0, y, width, fieldHeight);
            y += fieldHeight + gap;
            place(fallbacks, 0, y, width, fieldHeight);
            y += fieldHeight + gap;
            place(shadowMasks, 0, y, width, fieldHeight);
            y += fieldHeight + gap;
            if ("cosmic".equals(normalizeEngine(staged.engine))) {
                place(cosmicRegular, 0, y, width, fieldHeight);
                y += fieldHeight + gap;
                place(cosmicBold, 0, y, width, fieldHeight);
                y += fieldHeight + gap;
                place(cosmicItalic, 0, y, width, fieldHeight);
                y += fieldHeight + gap;
                place(cosmicBoldItalic, 0, y, width, fieldHeight);
                y += fieldHeight + gap;
                place(cosmicVariantFontOnly, 0, y, width, 24);
                y += 24 + gap;
            }
            place(metrics, 0, y, width, fieldHeight);
            y += fieldHeight + gap;
            int oversampleHeight = oversample.preferredHeight();
            place(oversample, 0, y, width, oversampleHeight);
            y += oversampleHeight + gap;

            place(preview, 0, y, width, Math.max(0, height - y));
            return true;
        }

        private static TextFieldWidget cosmicFaceField(Supplier<String> getter,
                                                       java.util.function.Consumer<String> setter) {
            return new TextFieldWidget()
                    .setMaxLength(512)
                    .value(new StringValue(getter, setter));
        }
    }

    private static final class FontFieldRefs {
        private TextFieldWidget fontName;
        private TextFieldWidget fontPath;
        private TextFieldWidget fallbacks;
        private TextFieldWidget shadowMasks;
        private TextFieldWidget cosmicRegular;
        private TextFieldWidget cosmicBold;
        private TextFieldWidget cosmicItalic;
        private TextFieldWidget cosmicBoldItalic;

        private void refreshFrom(Staged staged) {
            setText(fontName, staged.fontName);
            setText(fontPath, staged.fontPath);
            setText(fallbacks, staged.fontFallbacks);
            setText(shadowMasks, staged.shadowMaskFonts);
            setText(cosmicRegular, staged.cosmicRegular);
            setText(cosmicBold, staged.cosmicBold);
            setText(cosmicItalic, staged.cosmicItalic);
            setText(cosmicBoldItalic, staged.cosmicBoldItalic);
        }

        private static void setText(TextFieldWidget field, String value) {
            if (field != null) {
                field.setText(value == null ? "" : value);
            }
        }
    }

    private static final class LabeledTextField extends ParentWidget<LabeledTextField> implements ILayoutWidget {
        private final TextWidget label;
        private final TextFieldWidget field;

        private LabeledTextField(String label, TextFieldWidget field) {
            this.label = label(label);
            this.field = field;
            child(this.label);
            child(this.field);
        }

        @Override
        public boolean layoutWidgets() {
            int width = getArea().w();
            Minecraft mc = Minecraft.getMinecraft();
            int labelHeight = mc.fontRenderer.FONT_HEIGHT + 3;
            place(label, 0, 0, width, labelHeight);
            place(field, 0, labelHeight + 4, width, Math.max(18, getArea().h() - labelHeight - 4));
            return true;
        }
    }

    private static final class MetricsSection extends ParentWidget<MetricsSection> implements ILayoutWidget {
        private final LabeledTextField size;
        private final LabeledTextField weight;
        private final LabeledTextField baseline;

        private MetricsSection(Staged staged) {
            this.size = new LabeledTextField(tr("neofontrender.gui.label.size"), new TextFieldWidget()
                    .setMaxLength(8)
                    .value(new StringValue(() -> staged.fontSize, v -> staged.fontSize = v)));
            this.weight = new LabeledTextField(tr("neofontrender.gui.label.weight"), new TextFieldWidget()
                    .setMaxLength(4)
                    .value(new StringValue(() -> staged.variableWeight, v -> staged.variableWeight = v)));
            this.baseline = new LabeledTextField(tr("neofontrender.gui.label.baseline"), new TextFieldWidget()
                    .setMaxLength(8)
                    .value(new StringValue(() -> staged.baselineShift, v -> staged.baselineShift = v)));
            child(size);
            child(weight);
            child(baseline);
        }

        @Override
        public boolean layoutWidgets() {
            int width = getArea().w();
            int gap = 10;
            int item = Math.max(0, (width - gap * 2) / 3);
            place(size, 0, 0, item, getArea().h());
            place(weight, item + gap, 0, item, getArea().h());
            place(baseline, (item + gap) * 2, 0, Math.max(0, width - (item + gap) * 2), getArea().h());
            return true;
        }
    }

    private static final class OversampleSection extends ParentWidget<OversampleSection> implements ILayoutWidget {
        private final TextWidget label;
        private final SliderWidget slider;

        private OversampleSection(Staged staged) {
            this.label = dynamicLabel(() -> tr("neofontrender.gui.label.oversample") + ": " + staged.oversample + "x"
                    + (staged.adaptiveRasterScale ? " (" + tr("neofontrender.gui.option.autoscale") + ")" : " (" + tr("neofontrender.gui.manual") + ")"));
            this.slider = new TrackSliderWidget()
                    .value(new DoubleValue(
                            () -> (double) parseFloat(staged.oversample, 8.0F, 1.0F, 16.0F),
                            v -> {
                                staged.oversample = String.format(Locale.ROOT, "%.1f", Math.max(1.0D, Math.min(16.0D, v)));
                                preview(staged);
                            }))
                    .bounds(1.0D, 16.0D)
                    .stopper(1.0D, 2.0D, 3.0D, 4.0D, 6.0D, 8.0D, 12.0D, 16.0D)
                    .sliderSize(8, 14)
                    .stopperSize(2, 6);
            child(label);
            child(slider);
        }

        private int preferredHeight() {
            return Minecraft.getMinecraft().fontRenderer.FONT_HEIGHT + 3 + 6 + 20;
        }

        @Override
        public boolean layoutWidgets() {
            int width = getArea().w();
            Minecraft mc = Minecraft.getMinecraft();
            int labelHeight = mc.fontRenderer.FONT_HEIGHT + 3;
            place(label, 0, 0, width, labelHeight);
            int sliderTop = labelHeight + 6;
            place(slider, 0, sliderTop, width, 20);
            if (getArea().h() <= 0) {
                getArea().setSize(GuiAxis.Y, preferredHeight());
            }
            return true;
        }
    }

    /**
     * ModularUI 3.1.6's Flow keeps absolute child positions from its preliminary resize pass.
     * These screens are positioned manually, so those stale positions can put a grid's buttons on
     * top of adjacent controls. Give the group a deterministic height and position every child
     * from its final area instead of relying on Flow's cover-children resize pass.
     */
    private static final class OptionsGrid extends ParentWidget<OptionsGrid> implements ILayoutWidget {
        private final int itemWidth;
        private final int itemHeight;
        private final int gap;
        private final boolean expandItems;

        private OptionsGrid(int itemWidth, int itemHeight, int gap) {
            this(itemWidth, itemHeight, gap, false);
        }

        private OptionsGrid(int itemWidth, int itemHeight, int gap, boolean expandItems) {
            this.itemWidth = itemWidth;
            this.itemHeight = itemHeight;
            this.gap = gap;
            this.expandItems = expandItems;
        }

        private OptionsGrid add(IWidget widget) {
            child(widget);
            return this;
        }

        private int columns(int width) {
            return Math.max(1, (Math.max(0, width) + gap) / (itemWidth + gap));
        }

        private int preferredHeight(int width) {
            int count = getChildren().size();
            int rows = (count + columns(width) - 1) / columns(width);
            return rows == 0 ? 0 : rows * itemHeight + (rows - 1) * gap;
        }

        @Override
        public boolean layoutWidgets() {
            int width = getArea().w();
            int columns = columns(width);
            int laidOutWidth = expandItems
                    ? Math.max(0, (width - gap * (columns - 1)) / columns)
                    : itemWidth;
            int index = 0;
            for (IWidget widget : getChildren()) {
                int column = index % columns;
                int row = index / columns;
                int x = column * (laidOutWidth + gap);
                int childWidth = Math.min(laidOutWidth, Math.max(0, width - x));
                place(widget, x, row * (itemHeight + gap), childWidth, itemHeight);
                index++;
            }
            return true;
        }
    }

    private static OptionsGrid buildAdvancedOptionsGrid(Staged staged, int page) {
        OptionsGrid grid = new OptionsGrid(260, 24, 8, true);
        if (page == PAGE_GENERAL) {
            return grid
                    .add(engineButton(staged, page, 260, 24))
                    .add(aaModeButton(staged, 260, 24))
                    .add(styleButton(staged, 260, 24))
                    .add(toggleButtonKey("neofontrender.gui.option.enabled", "neofontrender.tooltip.enabled", 260, 24,
                            () -> staged.enabled, v -> staged.enabled = v, () -> reloadAndReopen(staged, page)))
                    .add(toggleButtonKey("neofontrender.gui.option.string_mode", "neofontrender.tooltip.string_mode", 260, 24, () -> staged.skiaAdvancedStringMode, v -> staged.skiaAdvancedStringMode = v, () -> preview(staged)))
                    .add(toggleButtonKey("neofontrender.gui.option.autobase", "neofontrender.tooltip.autobase", 260, 24, () -> staged.autoBaseline, v -> staged.autoBaseline = v, () -> preview(staged)))
                    .add(toggleButtonKey("neofontrender.gui.option.fractional", "neofontrender.tooltip.fractional", 260, 24, () -> staged.fractionalMetrics, v -> staged.fractionalMetrics = v, () -> preview(staged)))
                    .add(toggleButtonKey("neofontrender.gui.option.builtins", "neofontrender.tooltip.builtins", 260, 24, () -> staged.builtinFallbacks, v -> staged.builtinFallbacks = v, () -> preview(staged)));
        }
        if (page == PAGE_RENDERING) {
            return grid
                    .add(toggleButtonKey("neofontrender.gui.option.linear", "neofontrender.tooltip.linear", 260, 24, () -> staged.interpolation, v -> staged.interpolation = v, () -> preview(staged)))
                    .add(toggleButtonKey("neofontrender.gui.option.mipmap", "neofontrender.tooltip.mipmap", 260, 24, () -> staged.mipmap, v -> staged.mipmap = v, () -> preview(staged)))
                    .add(toggleButtonKey("neofontrender.gui.option.gpu_offscreen", "neofontrender.tooltip.gpu_offscreen", 260, 24, () -> staged.skiaGpuOffscreen, v -> {
                    staged.skiaGpuOffscreen = v;
                    if (v) {
                        staged.skiaGpuSubmitViaCpuTexture = true;
                    }
                }, () -> preview(staged)))
                    .add(toggleButtonKey("neofontrender.gui.option.gpu_cpu_submit", "neofontrender.tooltip.gpu_cpu_submit", 260, 24, () -> staged.skiaGpuSubmitViaCpuTexture, v -> {
                    staged.skiaGpuSubmitViaCpuTexture = v;
                    if (v) {
                        staged.skiaGpuOffscreen = true;
                    }
                }, () -> preview(staged)))
                    .add(toggleButtonKey("neofontrender.gui.option.monochrome_text", "neofontrender.tooltip.monochrome_text", 260, 24, () -> staged.skiaMonochromeText, v -> staged.skiaMonochromeText = v, () -> preview(staged)))
                    .add(toggleButtonKey("neofontrender.gui.option.premultiplied_alpha", "neofontrender.tooltip.premultiplied_alpha", 260, 24, () -> staged.premultipliedAlpha, v -> staged.premultipliedAlpha = v, () -> preview(staged)));
        }
        if (page == PAGE_PERFORMANCE) {
            return grid
                    .add(toggleButtonKey("neofontrender.gui.option.autoscale", "neofontrender.tooltip.autoscale", 260, 24, () -> staged.adaptiveRasterScale, v -> staged.adaptiveRasterScale = v, () -> preview(staged)))
                    .add(toggleButtonKey("neofontrender.gui.option.integer_scale", "neofontrender.tooltip.integer_scale", 260, 24, () -> staged.excludeIntegerScale, v -> staged.excludeIntegerScale = v, () -> preview(staged)))
                    .add(toggleButtonKey("neofontrender.gui.option.high_mag", "neofontrender.tooltip.high_mag", 260, 24, () -> staged.excludeHighMagnification, v -> staged.excludeHighMagnification = v, () -> preview(staged)))
                    .add(toggleButtonKey("neofontrender.gui.option.anisotropic", "neofontrender.tooltip.anisotropic", 260, 24, () -> staged.anisotropicFiltering, v -> staged.anisotropicFiltering = v, () -> preview(staged)))
                    .add(toggleButtonKey("neofontrender.gui.option.sign_model_lod", "neofontrender.tooltip.sign_model_lod", 260, 24, () -> staged.signModelLod, v -> staged.signModelLod = v, () -> preview(staged)))
                    .add(toggleButtonKey("neofontrender.gui.option.sign_cross_batch", "neofontrender.tooltip.sign_cross_batch", 260, 24, () -> staged.signCrossTileBatching, v -> staged.signCrossTileBatching = v, () -> preview(staged)))
                    .add(toggleButtonKey("neofontrender.gui.option.sign_occlusion", "neofontrender.tooltip.sign_occlusion", 260, 24, () -> staged.signBlockOcclusionCulling, v -> staged.signBlockOcclusionCulling = v, () -> preview(staged)));
        }
        if (page == PAGE_SHADOW) {
            return grid.add(shadowModeButton(staged, 260, 24))
                    .add(shadowValueButton(staged, false, 260, 24))
                    .add(shadowValueButton(staged, true, 260, 24));
        }
        if (page == PAGE_FIXES) {
            return grid
                    .add(toggleButtonKey("neofontrender.gui.option.fix_ime", "neofontrender.tooltip.fix_ime", 260, 24, () -> staged.fixImeInput, v -> staged.fixImeInput = v, () -> preview(staged)))
                    .add(toggleButtonKey("neofontrender.gui.option.fix_unicode_delete", "neofontrender.tooltip.fix_unicode_delete", 260, 24, () -> staged.fixUnicodeTextDeletion, v -> staged.fixUnicodeTextDeletion = v, () -> preview(staged)))
                    .add(toggleButtonKey("neofontrender.gui.option.sign_paste", "neofontrender.tooltip.sign_paste", 260, 24, () -> staged.allowSignPaste, v -> staged.allowSignPaste = v, () -> preview(staged)));
        }
        if (page == PAGE_LABORATORY) {
            return grid.add(toggleButtonKey("neofontrender.gui.option.hex_chat", "neofontrender.tooltip.hex_chat", 260, 24, () -> staged.laboratoryHexChat, v -> staged.laboratoryHexChat = v, () -> preview(staged)));
        }
        return grid
                .add(toggleButtonKey("neofontrender.gui.option.pipeline", "neofontrender.tooltip.pipeline", 260, 24, () -> staged.enhancedTextPipeline, v -> staged.enhancedTextPipeline = v, () -> preview(staged)))
                .add(toggleButtonKey("neofontrender.gui.option.shader", "neofontrender.tooltip.shader", 260, 24, () -> staged.shaderTextPipeline, v -> staged.shaderTextPipeline = v, () -> preview(staged)))
                .add(toggleButtonKey("neofontrender.gui.option.edge_bleed", "neofontrender.tooltip.edge_bleed", 260, 24, () -> staged.textureEdgeBleed, v -> staged.textureEdgeBleed = v, () -> preview(staged)))
                .add(toggleButtonKey("neofontrender.gui.option.debug_stats", "neofontrender.tooltip.debug_stats", 260, 24, () -> staged.debugRenderStats, v -> staged.debugRenderStats = v, () -> preview(staged)));
    }

    private static final class BrightnessSection extends ParentWidget<BrightnessSection> implements ILayoutWidget {
        private final TextWidget label;
        private final SliderWidget slider;

        private BrightnessSection(Staged staged) {
            this.label = dynamicLabel(() -> tr("neofontrender.gui.label.brightness") + ": " + staged.brightness);
            this.slider = new TrackSliderWidget()
                    .value(new DoubleValue(
                            () -> (double) parseFloat(staged.brightness, 3.0F, 0.0F, 12.0F),
                            v -> {
                                staged.brightness = String.format(Locale.ROOT, "%.1f", Math.max(0.0D, Math.min(12.0D, v)));
                                preview(staged);
                            }))
                    .bounds(0.0D, 12.0D)
                    .stopper(0.0D, 1.0D, 2.0D, 3.0D, 4.0D, 6.0D, 8.0D, 12.0D)
                    .sliderSize(8, 14)
                    .stopperSize(2, 6);
            child(label);
            child(slider);
        }

        private int preferredHeight() {
            return Minecraft.getMinecraft().fontRenderer.FONT_HEIGHT + 3 + 6 + 20;
        }

        @Override
        public boolean layoutWidgets() {
            int width = getArea().w();
            Minecraft mc = Minecraft.getMinecraft();
            int labelHeight = mc.fontRenderer.FONT_HEIGHT + 3;
            place(label, 0, 0, width, labelHeight);
            int sliderTop = labelHeight + 6;
            place(slider, 0, sliderTop, width, 20);
            if (getArea().h() <= 0) {
                getArea().setSize(GuiAxis.Y, preferredHeight());
            }
            return true;
        }
    }

    private static final class CacheSettingsPane extends ParentWidget<CacheSettingsPane> implements ILayoutWidget {
        private final List<LabeledTextField> fields = new ArrayList<>();

        private CacheSettingsPane(Staged staged) {
            addField("neofontrender.gui.cache.text_min", () -> staged.textCacheMin, v -> staged.textCacheMin = v);
            addField("neofontrender.gui.cache.text_max", () -> staged.textCacheMax, v -> staged.textCacheMax = v);
            addField("neofontrender.gui.cache.text_ttl", () -> staged.textCacheTtl, v -> staged.textCacheTtl = v);
            addField("neofontrender.gui.cache.measure_max", () -> staged.measureCacheMax, v -> staged.measureCacheMax = v);
            addField("neofontrender.gui.cache.segment_min", () -> staged.segmentCacheMin, v -> staged.segmentCacheMin = v);
            addField("neofontrender.gui.cache.segment_max", () -> staged.segmentCacheMax, v -> staged.segmentCacheMax = v);
            addField("neofontrender.gui.cache.segment_ttl", () -> staged.segmentCacheTtl, v -> staged.segmentCacheTtl = v);
        }

        private void addField(String key, Supplier<String> getter, Consumer<String> setter) {
            LabeledTextField field = new LabeledTextField(tr(key), new TextFieldWidget()
                    .setMaxLength(10)
                    .value(new StringValue(getter, setter)));
            fields.add(field);
            child(field);
        }

        private int preferredHeight(int width) {
            int columns = width >= 520 ? 2 : 1;
            int rows = (fields.size() + columns - 1) / columns;
            return rows * 42 + Math.max(0, rows - 1) * 10;
        }

        @Override
        public boolean layoutWidgets() {
            int gap = 10;
            int columns = getArea().w() >= 520 ? 2 : 1;
            int itemWidth = Math.max(0, (getArea().w() - gap * (columns - 1)) / columns);
            int rowHeight = 42;
            for (int i = 0; i < fields.size(); i++) {
                int column = i % columns;
                int row = i / columns;
                place(fields.get(i), column * (itemWidth + gap), row * (rowHeight + gap), itemWidth, rowHeight);
            }
            return true;
        }
    }

    private static final class InfoPage extends ParentWidget<InfoPage> implements ILayoutWidget {
        private final int page;
        private final Staged staged;

        private InfoPage(int page, Staged staged) {
            this.page = page;
            this.staged = staged;
        }

        private int preferredHeight() {
            int line = Math.max(18, Minecraft.getMinecraft().fontRenderer.FONT_HEIGHT + 7);
            return page == PAGE_ABOUT ? line * 10 : line * 9;
        }

        @Override
        public boolean layoutWidgets() {
            return true;
        }

        @Override
        public void draw(ModularGuiContext context, WidgetThemeEntry<?> widgetTheme) {
            super.draw(context, widgetTheme);
            Platform.setupDrawFont();
            Minecraft mc = Minecraft.getMinecraft();
            int line = Math.max(18, mc.fontRenderer.FONT_HEIGHT + 7);
            int y = 4;
            if (page == PAGE_ABOUT) {
                String version = Loader.instance().getIndexedModList().containsKey("neofontrender")
                        ? Loader.instance().getIndexedModList().get("neofontrender").getVersion()
                        : "unknown";
                y = drawInfoLine(mc, tr("neofontrender.gui.about.name"), y, 0xFFFFFF, line);
                y = drawInfoLine(mc, tr("neofontrender.gui.about.version") + ": " + version, y, 0xD8D8D8, line);
                y = drawInfoLine(mc, tr("neofontrender.gui.option.engine") + ": " + engineName(staged.engine), y, 0xD8D8D8, line);
                y = drawInfoLine(mc, tr("neofontrender.gui.about.description"), y + line / 2, 0xBFC7D1, line);
                y = drawInfoLine(mc, tr("neofontrender.gui.about.license") + ": MIT License", y, 0xD8D8D8, line);
                y = drawInfoLine(mc, tr("neofontrender.gui.about.contributors") + ":", y, 0xBFC7D1, line);
                y = drawInfoLine(mc, "AndreaFrederica", y, 0xD8D8D8, line);
                y = drawInfoLine(mc, "baka-gourd", y, 0xD8D8D8, line);
                y = drawInfoLine(mc, "DHJComical", y, 0xD8D8D8, line);
                drawInfoLine(mc, "github.com/AndreaFrederica/NeoFontRender", y, 0x00DCE8, line);
                return;
            }
            y = drawInfoLine(mc, tr("neofontrender.gui.licenses.title"), y, 0xFFFFFF, line);
            y = drawInfoLine(mc, "cosmic-text - MIT / Apache-2.0", y + line / 2, 0xD8D8D8, line);
            y = drawInfoLine(mc, "Skija / Skia - Apache-2.0 / BSD-3-Clause", y, 0xD8D8D8, line);
            y = drawInfoLine(mc, "ModularUI - LGPL-3.0", y, 0xD8D8D8, line);
            y = drawInfoLine(mc, "LWJGL - BSD-3-Clause", y, 0xD8D8D8, line);
            y = drawInfoLine(mc, "Sarasa Gothic - SIL OFL-1.1", y, 0xD8D8D8, line);
            y = drawInfoLine(mc, "Noto Color Emoji - SIL OFL-1.1", y, 0xD8D8D8, line);
            drawInfoLine(mc, tr("neofontrender.gui.licenses.notice"), y + line / 2, 0xBFC7D1, line);
        }

        private int drawInfoLine(Minecraft mc, String text, int y, int color, int line) {
            mc.fontRenderer.drawString(mc.fontRenderer.trimStringToWidth(text, Math.max(1, getArea().w() - 12)), 6, y, color);
            return y + line;
        }
    }

    /** SliderWidget 3.1.6 omits its track; draw it inside the same widget before its stopper marks. */
    private static final class TrackSliderWidget extends SliderWidget {
        private final Rectangle track = new Rectangle().color(0xFF475569).cornerRadius(2);

        @Override
        public void drawBackground(ModularGuiContext context, WidgetThemeEntry<?> widgetTheme) {
            int width = Math.max(0, getArea().w() - 8);
            track.draw(context, 4, getArea().h() / 2 - 2, width, 4, widgetTheme.getTheme());
            super.drawBackground(context, widgetTheme);
        }
    }

    private static final class PipelineInfoWidget extends ParentWidget<PipelineInfoWidget> implements ILayoutWidget {
        private final Staged staged;

        private PipelineInfoWidget(Staged staged) {
            this.staged = staged;
        }

        private int preferredHeight() {
            Minecraft mc = Minecraft.getMinecraft();
            int line = Math.max(18, mc.fontRenderer.FONT_HEIGHT + 6);
            return line * 7 + 16;
        }

        @Override
        public boolean layoutWidgets() {
            // AdvancedConfigLayout owns this widget's final bounds. Mutating our own height here
            // makes ModularUI's later resize pass reapply the default position and shifts this
            // panel over the option grid.
            return true;
        }

        @Override
        public void draw(ModularGuiContext context, WidgetThemeEntry<?> widgetTheme) {
            super.draw(context, widgetTheme);
            Platform.setupDrawFont();
            FontRenderTuning.updateFromCurrentGlState();
            float configured = parseFloat(staged.oversample, 8.0F, 1.0F, 16.0F);
            float effective = FontRenderTuning.rasterScale(configured);
            float guiScale = FontRenderTuning.currentGuiScale();
            String filtering = FontRenderTuning.useLinearFiltering(effective)
                    ? tr("neofontrender.gui.filter.linear")
                    : tr("neofontrender.gui.filter.nearest");
            // ModularUI has already translated the GL viewport to this widget's origin. Adding the
            // absolute Area position again double-offsets the panel and makes it cover later rows.
            int x = 8;
            int y = 8;
            Gui.drawRect(4, 4, Math.max(4, getArea().w() - 4), Math.max(4, getArea().h() - 4), 0x66000000);
            Minecraft mc = Minecraft.getMinecraft();
            int line = Math.max(18, mc.fontRenderer.FONT_HEIGHT + 6);
            mc.fontRenderer.drawString(tr("neofontrender.gui.option.engine") + ": " + engineName(staged.engine), x, y, 0xFFFFFF);
            mc.fontRenderer.drawString(String.format(Locale.ROOT, "%s: %.1fx  %s: %.2fx",
                    tr("neofontrender.gui.label.oversample"), configured,
                    tr("neofontrender.gui.label.effective"), effective), x, y + line, 0xD8D8D8);
            mc.fontRenderer.drawString(String.format(Locale.ROOT, "%s: %.2fx  %s: %s",
                    tr("neofontrender.gui.label.gui_scale"), guiScale,
                    tr("neofontrender.gui.label.filter"), filtering), x, y + line * 2, 0xD8D8D8);
            mc.fontRenderer.drawString(tr("neofontrender.gui.option.pipeline") + ": " + onOff(staged.enhancedTextPipeline)
                    + "  " + tr("neofontrender.gui.option.shader") + ": " + onOff(staged.shaderTextPipeline)
                    + "  " + tr("neofontrender.gui.option.edge_bleed") + ": " + onOff(staged.textureEdgeBleed), x, y + line * 3, 0xD8D8D8);
            mc.fontRenderer.drawString(tr("neofontrender.gui.option.autoscale") + ": " + onOff(staged.adaptiveRasterScale)
                    + "  " + tr("neofontrender.gui.option.linear") + ": " + onOff(staged.interpolation)
                    + "  " + tr("neofontrender.gui.option.mipmap") + ": " + onOff(staged.mipmap), x, y + line * 4, 0xD8D8D8);
            mc.fontRenderer.drawString(tr("neofontrender.gui.option.integer_scale") + ": " + onOff(staged.excludeIntegerScale)
                    + "  " + tr("neofontrender.gui.option.high_mag") + ": " + onOff(staged.excludeHighMagnification)
                    + "  " + tr("neofontrender.gui.option.anisotropic") + ": " + onOff(staged.anisotropicFiltering), x, y + line * 5, 0xD8D8D8);
            mc.fontRenderer.drawString(tr("neofontrender.gui.option.gpu_offscreen") + ": " + onOff(staged.skiaGpuOffscreen)
                    + "  " + tr("neofontrender.gui.option.gpu_cpu_submit") + ": " + onOff(staged.skiaGpuSubmitViaCpuTexture)
                    + "  " + tr("neofontrender.gui.option.debug_stats") + ": " + onOff(staged.debugRenderStats), x, y + line * 6, 0xD8D8D8);
        }
    }

    private static final class Staged {
        private final boolean originalEnabled = NeofontrenderConfig.enabled();
        private final String originalFontName = NeofontrenderConfig.fontName();
        private final String originalCosmicRegular = NeofontrenderConfig.cosmicRegularFont();
        private final String originalCosmicBold = NeofontrenderConfig.cosmicBoldFont();
        private final String originalCosmicItalic = NeofontrenderConfig.cosmicItalicFont();
        private final String originalCosmicBoldItalic = NeofontrenderConfig.cosmicBoldItalicFont();
        private final boolean originalCosmicVariantOverridesOnlySwitchFont =
                NeofontrenderConfig.cosmicVariantOverridesOnlySwitchFont();
        private final int originalFontStyle = NeofontrenderConfig.fontStyle();
        private final String originalVariableWeight = Integer.toString(NeofontrenderConfig.fontVariableWeight());
        private final String originalFontSize = Float.toString(NeofontrenderConfig.fontSize());
        private final String originalOversample = Float.toString(NeofontrenderConfig.fontOversample());
        private final boolean originalAutoBaseline = NeofontrenderConfig.fontAutoBaseline();
        private final String originalBaselineShift = Float.toString(NeofontrenderConfig.fontBaselineShift());
        private final boolean originalAntialias = NeofontrenderConfig.fontAntialias();
        private final String originalAntialiasMode = NeofontrenderConfig.fontAntialiasMode();
        private final boolean originalFractionalMetrics = NeofontrenderConfig.fontFractionalMetrics();
        private final String originalFontFallbacks = joinFonts(NeofontrenderConfig.fontFallbacks());
        private final boolean originalBuiltinFallbacks = NeofontrenderConfig.builtinFallbacksEnabled();
        private final String originalEngine = NeofontrenderConfig.renderingEngine();
        private final boolean originalSkiaAdvancedStringMode = NeofontrenderConfig.skiaAdvancedStringMode();
        private final boolean originalAdaptiveRasterScale = NeofontrenderConfig.adaptiveRasterScale();
        private final boolean originalExcludeIntegerScale = NeofontrenderConfig.excludeIntegerScale();
        private final boolean originalExcludeHighMagnification = NeofontrenderConfig.excludeHighMagnification();
        private final boolean originalAnisotropicFiltering = NeofontrenderConfig.anisotropicFiltering();
        private final boolean originalInterpolation = NeofontrenderConfig.renderingInterpolation();
        private final boolean originalMipmap = NeofontrenderConfig.renderingMipmap();
        private final boolean originalEnhancedTextPipeline = NeofontrenderConfig.enhancedTextPipeline();
        private final boolean originalShaderTextPipeline = NeofontrenderConfig.shaderTextPipeline();
        private final boolean originalSkiaGpuOffscreen = NeofontrenderConfig.skiaGpuOffscreen();
        private final boolean originalSkiaGpuSubmitViaCpuTexture = NeofontrenderConfig.skiaGpuSubmitViaCpuTexture();
        private final boolean originalSkiaMonochromeText = NeofontrenderConfig.skiaMonochromeText();
        private final boolean originalPremultipliedAlpha = NeofontrenderConfig.enablePremultipliedAlpha();
        private final boolean originalDebugRenderStats = NeofontrenderConfig.debugRenderStats();
        private final boolean originalSignModelLod = NeofontrenderConfig.signModelLod();
        private final boolean originalSignCrossTileBatching = NeofontrenderConfig.signCrossTileBatching();
        private final boolean originalSignBlockOcclusionCulling = NeofontrenderConfig.signBlockOcclusionCulling();
        private final String originalBrightness = Float.toString(NeofontrenderConfig.renderingBrightness());
        private final boolean originalTextureEdgeBleed = NeofontrenderConfig.textureEdgeBleed();
        private final String originalShadowMode = NeofontrenderConfig.shadowMode();
        private final String originalShadowMaskFonts = NeofontrenderConfig.shadowMaskFonts();
        private final String originalShadowMaskCodepoints = NeofontrenderConfig.shadowMaskCodepoints();
        private final float originalShadowLength = NeofontrenderConfig.shadowLength();
        private final float originalShadowOpacity = NeofontrenderConfig.shadowOpacity();
        private final boolean originalFixImeInput = NeofontrenderConfig.fixImeInput();
        private final boolean originalFixUnicodeTextDeletion = NeofontrenderConfig.fixUnicodeTextDeletion();
        private final boolean originalAllowSignPaste = NeofontrenderConfig.allowSignPaste();
        private final boolean originalLaboratoryHexChat = NeofontrenderConfig.laboratoryHexChat();
        private final String originalTextCacheMin = Integer.toString(NeofontrenderConfig.skiaTextCacheMinEntries());
        private final String originalTextCacheMax = Integer.toString(NeofontrenderConfig.skiaTextCacheMaxEntries());
        private final String originalTextCacheTtl = Float.toString(NeofontrenderConfig.skiaTextCacheTtlSeconds());
        private final String originalMeasureCacheMax = Integer.toString(NeofontrenderConfig.skiaMeasureCacheMaxEntries());
        private final String originalSegmentCacheMin = Integer.toString(NeofontrenderConfig.skiaSegmentTextureCacheMinEntries());
        private final String originalSegmentCacheMax = Integer.toString(NeofontrenderConfig.skiaSegmentTextureCacheMaxEntries());
        private final String originalSegmentCacheTtl = Float.toString(NeofontrenderConfig.skiaSegmentTextureCacheTtlSeconds());

        private boolean enabled = originalEnabled;
        private String engine = originalEngine;
        private boolean skiaAdvancedStringMode = originalSkiaAdvancedStringMode;
        private boolean adaptiveRasterScale = originalAdaptiveRasterScale;
        private boolean excludeIntegerScale = originalExcludeIntegerScale;
        private boolean excludeHighMagnification = originalExcludeHighMagnification;
        private boolean anisotropicFiltering = originalAnisotropicFiltering;
        private boolean interpolation = originalInterpolation;
        private boolean mipmap = originalMipmap;
        private boolean enhancedTextPipeline = originalEnhancedTextPipeline;
        private boolean shaderTextPipeline = originalShaderTextPipeline;
        private boolean skiaGpuOffscreen = originalSkiaGpuOffscreen;
        private boolean skiaGpuSubmitViaCpuTexture = originalSkiaGpuSubmitViaCpuTexture;
        private boolean skiaMonochromeText = originalSkiaMonochromeText;
        private boolean premultipliedAlpha = originalPremultipliedAlpha;
        private boolean debugRenderStats = originalDebugRenderStats;
        private boolean signModelLod = originalSignModelLod;
        private boolean signCrossTileBatching = originalSignCrossTileBatching;
        private boolean signBlockOcclusionCulling = originalSignBlockOcclusionCulling;
        private String brightness = originalBrightness;
        private boolean textureEdgeBleed = originalTextureEdgeBleed;
        private String shadowMode = originalShadowMode;
        private String shadowMaskFonts = originalShadowMaskFonts;
        private String shadowMaskCodepoints = originalShadowMaskCodepoints;
        private float shadowLength = originalShadowLength;
        private float shadowOpacity = originalShadowOpacity;
        private boolean fixImeInput = originalFixImeInput;
        private boolean fixUnicodeTextDeletion = originalFixUnicodeTextDeletion;
        private boolean allowSignPaste = originalAllowSignPaste;
        private boolean laboratoryHexChat = originalLaboratoryHexChat;
        private int categoryScroll;
        private String fontName = originalFontName;
        private String fontPath = isFontFile(originalFontName) ? originalFontName : "";
        private String cosmicRegular = originalCosmicRegular;
        private String cosmicBold = originalCosmicBold;
        private String cosmicItalic = originalCosmicItalic;
        private String cosmicBoldItalic = originalCosmicBoldItalic;
        private boolean cosmicVariantOverridesOnlySwitchFont = originalCosmicVariantOverridesOnlySwitchFont;
        private String fontFallbacks = originalFontFallbacks;
        private int fontStyle = originalFontStyle;
        private String variableWeight = originalVariableWeight;
        private String fontSize = originalFontSize;
        private String oversample = originalOversample;
        private boolean autoBaseline = originalAutoBaseline;
        private String baselineShift = originalBaselineShift;
        private boolean antialias = originalAntialias;
        private String antialiasMode = originalAntialiasMode;
        private boolean fractionalMetrics = originalFractionalMetrics;
        private String search = "";
        private int fontSource = SOURCE_SYSTEM;
        private int fontTarget = TARGET_PRIMARY;
        private boolean builtinFallbacks = originalBuiltinFallbacks;
        private String textCacheMin = originalTextCacheMin;
        private String textCacheMax = originalTextCacheMax;
        private String textCacheTtl = originalTextCacheTtl;
        private String measureCacheMax = originalMeasureCacheMax;
        private String segmentCacheMin = originalSegmentCacheMin;
        private String segmentCacheMax = originalSegmentCacheMax;
        private String segmentCacheTtl = originalSegmentCacheTtl;

        private String selectedFont() {
            String path = fontPath == null ? "" : fontPath.trim();
            return path.isEmpty() ? fontName.trim() : path;
        }

        private boolean matchesSearch(String font) {
            String query = search == null ? "" : search.trim().toLowerCase(Locale.ROOT);
            return query.isEmpty() || font.toLowerCase(Locale.ROOT).contains(query);
        }

        private boolean isSelected(FontEntry font) {
            String value = fontValue(font);
            switch (fontTarget) {
                case TARGET_FALLBACK:
                    return parseFonts(fontFallbacks).contains(value);
                case TARGET_COSMIC_REGULAR:
                    return value.equals(cosmicRegular);
                case TARGET_COSMIC_BOLD:
                    return value.equals(cosmicBold);
                case TARGET_COSMIC_ITALIC:
                    return value.equals(cosmicItalic);
                case TARGET_COSMIC_BOLD_ITALIC:
                    return value.equals(cosmicBoldItalic);
                case TARGET_SHADOW_MASK:
                    return parseFonts(shadowMaskFonts).contains(value);
                case TARGET_PRIMARY:
                default:
                    String path = font.path == null ? "" : font.path;
                    return path.isEmpty()
                            ? fontPath.isEmpty() && fontName.equals(font.displayName)
                            : selectedFont().equals(path);
            }
        }

        private void selectFont(FontEntry font) {
            String value = fontValue(font);
            switch (fontTarget) {
                case TARGET_FALLBACK:
                    List<String> fonts = parseFonts(fontFallbacks);
                    if (fonts.contains(value)) {
                        fonts.remove(value);
                    } else {
                        fonts.add(value);
                    }
                    fontFallbacks = joinFonts(fonts);
                    return;
                case TARGET_COSMIC_REGULAR:
                    cosmicRegular = toggleSingleFont(cosmicRegular, value);
                    return;
                case TARGET_COSMIC_BOLD:
                    cosmicBold = toggleSingleFont(cosmicBold, value);
                    return;
                case TARGET_COSMIC_ITALIC:
                    cosmicItalic = toggleSingleFont(cosmicItalic, value);
                    return;
                case TARGET_COSMIC_BOLD_ITALIC:
                    cosmicBoldItalic = toggleSingleFont(cosmicBoldItalic, value);
                    return;
                case TARGET_SHADOW_MASK:
                    List<String> masks = parseFonts(shadowMaskFonts);
                    if (masks.contains(value)) masks.remove(value); else masks.add(value);
                    shadowMaskFonts = joinFonts(masks);
                    return;
                case TARGET_PRIMARY:
                default:
                    fontName = font.displayName;
                    fontPath = font.path;
            }
        }

        private String toggleSingleFont(String current, String value) {
            String existing = current == null ? "" : current.trim();
            return existing.equals(value) ? "" : value;
        }

        private void writeToConfig(boolean save) {
            NeofontrenderConfig.setEnabled(enabled);
            NeofontrenderConfig.setRenderingEngine(engine);
            NeofontrenderConfig.setSkiaAdvancedStringMode(skiaAdvancedStringMode);
            NeofontrenderConfig.setAdaptiveRasterScale(adaptiveRasterScale);
            NeofontrenderConfig.setExcludeIntegerScale(excludeIntegerScale);
            NeofontrenderConfig.setExcludeHighMagnification(excludeHighMagnification);
            NeofontrenderConfig.setAnisotropicFiltering(anisotropicFiltering);
            NeofontrenderConfig.setRenderingInterpolation(interpolation);
            NeofontrenderConfig.setRenderingMipmap(mipmap);
            NeofontrenderConfig.setEnhancedTextPipeline(enhancedTextPipeline);
            NeofontrenderConfig.setShaderTextPipeline(shaderTextPipeline);
            NeofontrenderConfig.setSkiaGpuOffscreen(skiaGpuOffscreen);
            NeofontrenderConfig.setSkiaGpuSubmitViaCpuTexture(skiaGpuSubmitViaCpuTexture);
            NeofontrenderConfig.setSkiaMonochromeText(skiaMonochromeText);
            NeofontrenderConfig.setEnablePremultipliedAlpha(premultipliedAlpha);
            NeofontrenderConfig.setDebugRenderStats(debugRenderStats);
            NeofontrenderConfig.setSignModelLod(signModelLod);
            NeofontrenderConfig.setSignCrossTileBatching(signCrossTileBatching);
            NeofontrenderConfig.setSignBlockOcclusionCulling(signBlockOcclusionCulling);
            NeofontrenderConfig.setRenderingBrightness(parseFloat(brightness, 3.0F, 0.0F, 12.0F));
            NeofontrenderConfig.setTextureEdgeBleed(textureEdgeBleed);
            NeofontrenderConfig.setShadowMode(shadowMode);
            NeofontrenderConfig.setShadowMaskFonts(shadowMaskFonts);
            NeofontrenderConfig.setShadowMaskCodepoints(shadowMaskCodepoints);
            NeofontrenderConfig.setShadowLength(shadowLength);
            NeofontrenderConfig.setShadowOpacity(shadowOpacity);
            NeofontrenderConfig.setFixImeInput(fixImeInput);
            NeofontrenderConfig.setFixUnicodeTextDeletion(fixUnicodeTextDeletion);
            NeofontrenderConfig.setAllowSignPaste(allowSignPaste);
            NeofontrenderConfig.setLaboratoryHexChat(laboratoryHexChat);
                NeofontrenderConfig.setFontName(selectedFont().isEmpty()
                    ? "neofontrender:fonts/sarasa_ui_sc_regular.ttf"
                    : selectedFont());
            NeofontrenderConfig.setFontFallbacks(parseFonts(fontFallbacks));
            NeofontrenderConfig.setCosmicRegularFont(cosmicRegular);
            NeofontrenderConfig.setCosmicBoldFont(cosmicBold);
            NeofontrenderConfig.setCosmicItalicFont(cosmicItalic);
            NeofontrenderConfig.setCosmicBoldItalicFont(cosmicBoldItalic);
            NeofontrenderConfig.setCosmicVariantOverridesOnlySwitchFont(cosmicVariantOverridesOnlySwitchFont);
            NeofontrenderConfig.setFontStyle(fontStyle);
            NeofontrenderConfig.setFontVariableWeight(parseInt(variableWeight, 0, 0, 1000));
            NeofontrenderConfig.setFontSize(parseFloat(fontSize, 8.5F, 4.0F, 64.0F));
            NeofontrenderConfig.setFontOversample(parseFloat(oversample, 8.0F, 1.0F, 16.0F));
            NeofontrenderConfig.setFontAutoBaseline(autoBaseline);
            NeofontrenderConfig.setFontBaselineShift(parseFloat(baselineShift, 0.0F, -16.0F, 16.0F));
            NeofontrenderConfig.setFontAntialias(antialias);
            NeofontrenderConfig.setFontAntialiasMode(antialias ? antialiasMode : "off");
            NeofontrenderConfig.setFontFractionalMetrics(fractionalMetrics);
            NeofontrenderConfig.setBuiltinFallbacksEnabled(builtinFallbacks);
            NeofontrenderConfig.setSkiaTextCacheMinEntries(parseInt(textCacheMin, 256, 0, 65536));
            NeofontrenderConfig.setSkiaTextCacheMaxEntries(parseInt(textCacheMax, 2048, 1, 131072));
            NeofontrenderConfig.setSkiaTextCacheTtlSeconds(parseFloat(textCacheTtl, 300.0F, 0.0F, 86400.0F));
            NeofontrenderConfig.setSkiaMeasureCacheMaxEntries(parseInt(measureCacheMax, 4096, 1, 262144));
            NeofontrenderConfig.setSkiaSegmentTextureCacheMinEntries(parseInt(segmentCacheMin, 512, 0, 65536));
            NeofontrenderConfig.setSkiaSegmentTextureCacheMaxEntries(parseInt(segmentCacheMax, 4096, 1, 131072));
            NeofontrenderConfig.setSkiaSegmentTextureCacheTtlSeconds(parseFloat(segmentCacheTtl, 600.0F, 0.0F, 86400.0F));
            if (save) {
                NeofontrenderConfig.save();
            }
        }

        private void restoreOriginal() {
            NeofontrenderConfig.setEnabled(originalEnabled);
            NeofontrenderConfig.setRenderingEngine(originalEngine);
            NeofontrenderConfig.setSkiaAdvancedStringMode(originalSkiaAdvancedStringMode);
            NeofontrenderConfig.setAdaptiveRasterScale(originalAdaptiveRasterScale);
            NeofontrenderConfig.setExcludeIntegerScale(originalExcludeIntegerScale);
            NeofontrenderConfig.setExcludeHighMagnification(originalExcludeHighMagnification);
            NeofontrenderConfig.setAnisotropicFiltering(originalAnisotropicFiltering);
            NeofontrenderConfig.setRenderingInterpolation(originalInterpolation);
            NeofontrenderConfig.setRenderingMipmap(originalMipmap);
            NeofontrenderConfig.setEnhancedTextPipeline(originalEnhancedTextPipeline);
            NeofontrenderConfig.setShaderTextPipeline(originalShaderTextPipeline);
            NeofontrenderConfig.setSkiaGpuOffscreen(originalSkiaGpuOffscreen);
            NeofontrenderConfig.setSkiaGpuSubmitViaCpuTexture(originalSkiaGpuSubmitViaCpuTexture);
            NeofontrenderConfig.setSkiaMonochromeText(originalSkiaMonochromeText);
            NeofontrenderConfig.setEnablePremultipliedAlpha(originalPremultipliedAlpha);
            NeofontrenderConfig.setDebugRenderStats(originalDebugRenderStats);
            NeofontrenderConfig.setSignModelLod(originalSignModelLod);
            NeofontrenderConfig.setSignCrossTileBatching(originalSignCrossTileBatching);
            NeofontrenderConfig.setSignBlockOcclusionCulling(originalSignBlockOcclusionCulling);
            NeofontrenderConfig.setRenderingBrightness(parseFloat(originalBrightness, 3.0F, 0.0F, 12.0F));
            NeofontrenderConfig.setTextureEdgeBleed(originalTextureEdgeBleed);
            NeofontrenderConfig.setShadowMode(originalShadowMode);
            NeofontrenderConfig.setShadowMaskFonts(originalShadowMaskFonts);
            NeofontrenderConfig.setShadowMaskCodepoints(originalShadowMaskCodepoints);
            NeofontrenderConfig.setShadowLength(originalShadowLength);
            NeofontrenderConfig.setShadowOpacity(originalShadowOpacity);
            NeofontrenderConfig.setFixImeInput(originalFixImeInput);
            NeofontrenderConfig.setFixUnicodeTextDeletion(originalFixUnicodeTextDeletion);
            NeofontrenderConfig.setAllowSignPaste(originalAllowSignPaste);
            NeofontrenderConfig.setLaboratoryHexChat(originalLaboratoryHexChat);
            NeofontrenderConfig.setFontName(originalFontName);
            NeofontrenderConfig.setFontFallbacks(parseFonts(originalFontFallbacks));
            NeofontrenderConfig.setCosmicRegularFont(originalCosmicRegular);
            NeofontrenderConfig.setCosmicBoldFont(originalCosmicBold);
            NeofontrenderConfig.setCosmicItalicFont(originalCosmicItalic);
            NeofontrenderConfig.setCosmicBoldItalicFont(originalCosmicBoldItalic);
            NeofontrenderConfig.setCosmicVariantOverridesOnlySwitchFont(
                    originalCosmicVariantOverridesOnlySwitchFont);
            NeofontrenderConfig.setFontStyle(originalFontStyle);
            NeofontrenderConfig.setFontVariableWeight(parseInt(originalVariableWeight, 0, 0, 1000));
            NeofontrenderConfig.setFontSize(parseFloat(originalFontSize, 8.0F, 4.0F, 64.0F));
            NeofontrenderConfig.setFontOversample(parseFloat(originalOversample, 8.0F, 1.0F, 16.0F));
            NeofontrenderConfig.setFontAutoBaseline(originalAutoBaseline);
            NeofontrenderConfig.setFontBaselineShift(parseFloat(originalBaselineShift, 0.0F, -16.0F, 16.0F));
            NeofontrenderConfig.setFontAntialias(originalAntialias);
            NeofontrenderConfig.setFontAntialiasMode(originalAntialias ? originalAntialiasMode : "off");
            NeofontrenderConfig.setFontFractionalMetrics(originalFractionalMetrics);
            NeofontrenderConfig.setBuiltinFallbacksEnabled(originalBuiltinFallbacks);
            NeofontrenderConfig.setSkiaTextCacheMinEntries(parseInt(originalTextCacheMin, 256, 0, 65536));
            NeofontrenderConfig.setSkiaTextCacheMaxEntries(parseInt(originalTextCacheMax, 2048, 1, 131072));
            NeofontrenderConfig.setSkiaTextCacheTtlSeconds(parseFloat(originalTextCacheTtl, 300.0F, 0.0F, 86400.0F));
            NeofontrenderConfig.setSkiaMeasureCacheMaxEntries(parseInt(originalMeasureCacheMax, 4096, 1, 262144));
            NeofontrenderConfig.setSkiaSegmentTextureCacheMinEntries(parseInt(originalSegmentCacheMin, 512, 0, 65536));
            NeofontrenderConfig.setSkiaSegmentTextureCacheMaxEntries(parseInt(originalSegmentCacheMax, 4096, 1, 131072));
            NeofontrenderConfig.setSkiaSegmentTextureCacheTtlSeconds(parseFloat(originalSegmentCacheTtl, 600.0F, 0.0F, 86400.0F));
        }
    }

    private static float parseFloat(String value, float fallback, float min, float max) {
        try {
            float parsed = Float.parseFloat(value);
            if (Float.isNaN(parsed) || Float.isInfinite(parsed)) {
                return fallback;
            }
            return Math.max(min, Math.min(max, parsed));
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static int parseInt(String value, int fallback, int min, int max) {
        try {
            return Math.max(min, Math.min(max, Integer.parseInt(value)));
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static List<String> parseFonts(String value) {
        Set<String> fonts = new LinkedHashSet<>();
        if (value != null) {
            for (String part : value.split("[,;]")) {
                String font = part.trim();
                if (!font.isEmpty()) {
                    fonts.add(font);
                }
            }
        }
        return new ArrayList<>(fonts);
    }

    private static String joinFonts(List<String> fonts) {
        return fonts == null || fonts.isEmpty() ? "" : String.join(", ", fonts);
    }

    private static String onOff(boolean value) {
        return value ? tr("neofontrender.gui.on") : tr("neofontrender.gui.off");
    }

    private static String tr(String key, Object... args) {
        return I18n.format(key, args);
    }

    private static final class FontEntry {
        private final String displayName;
        private final String path;

        private FontEntry(String displayName, String path) {
            this.displayName = displayName;
            this.path = path;
        }
    }

    private static class TextButton extends ButtonWidget<TextButton> {
        private final Supplier<String> label;
        private final boolean centered;

        private TextButton(Supplier<String> label, boolean centered) {
            this.label = label;
            this.centered = centered;
        }

        @Override
        public void draw(ModularGuiContext context, WidgetThemeEntry<?> widgetTheme) {
            super.draw(context, widgetTheme);
            // ModularUI 3.1.6 does not apply ScrollWidget's viewport matrix to OpenGL during
            // drawForeground. Button text drawn there stays fixed while its background scrolls.
            // Match TextWidget's GL setup because rectangle backgrounds leave texturing disabled;
            // vanilla Unicode glyph quads otherwise sample as solid white blocks.
            Platform.setupDrawFont();
            Minecraft mc = Minecraft.getMinecraft();
            String text = label.get();
            int inset = textInset();
            int maxWidth = Math.max(1, getArea().w() - inset - 4);
            String visible = mc.fontRenderer.trimStringToWidth(text, maxWidth);
            int x = inset;
            if (centered) {
                x = Math.max(inset, (getArea().w() - mc.fontRenderer.getStringWidth(visible)) / 2);
            }
            int y = Math.max(0, (getArea().h() - mc.fontRenderer.FONT_HEIGHT) / 2);
            mc.fontRenderer.drawString(visible, x, y, 0xFFFFFF);
        }

        protected int textInset() {
            return 4;
        }
    }

    /** Draws the variable right-hand content of a reusable settings button. */
    private interface ButtonContent {
        void draw(TextButton button);
    }

    private static final class ContentButton extends TextButton {
        private final ButtonContent content;

        private ContentButton(Supplier<String> label, boolean centered, ButtonContent content) {
            super(label, centered);
            this.content = content;
        }

        @Override
        public void draw(ModularGuiContext context, WidgetThemeEntry<?> widgetTheme) {
            super.draw(context, widgetTheme);
            content.draw(this);
        }
    }

    /** Compact options-screen switch: a fixed square state indicator on the right. */
    private static final class ToggleIndicator implements ButtonContent {
        private final Supplier<Boolean> selected;

        private ToggleIndicator(Supplier<Boolean> selected) {
            this.selected = selected;
        }

        @Override
        public void draw(TextButton button) {
            int width = button.getArea().w();
            int height = button.getArea().h();
            int size = Math.min(12, Math.max(8, height - 8));
            int x = width - size - 6;
            int y = (height - size) / 2;
            int border = selected.get() ? 0xFF00DCE8 : 0xFFE0E0E0;
            Gui.drawRect(x, y, x + size, y + size, border);
            Gui.drawRect(x + 2, y + 2, x + size - 2, y + size - 2, 0xFF090909);
            if (selected.get()) {
                Gui.drawRect(x + 4, y + 4, x + size - 4, y + size - 4, 0xFF00DCE8);
            }
        }
    }

    /** A right-aligned dynamic value, for enum-like and numeric settings. */
    private static final class ValueIndicator implements ButtonContent {
        private final Supplier<String> value;

        private ValueIndicator(Supplier<String> value) {
            this.value = value;
        }

        @Override
        public void draw(TextButton button) {
            Minecraft mc = Minecraft.getMinecraft();
            String visible = mc.fontRenderer.trimStringToWidth(value.get(), Math.max(1, button.getArea().w() / 2));
            int x = Math.max(4, button.getArea().w() - mc.fontRenderer.getStringWidth(visible) - 6);
            int y = Math.max(0, (button.getArea().h() - mc.fontRenderer.FONT_HEIGHT) / 2);
            mc.fontRenderer.drawString(visible, x, y, 0xE0E0E0);
        }
    }

    private static final class CategoryRail extends ParentWidget<CategoryRail> implements ILayoutWidget {
        private final Staged staged;
        private final int selectedPage;
        private final List<ButtonWidget<?>> buttons = new ArrayList<>();

        private CategoryRail(Staged staged, int selectedPage) {
            this.staged = staged;
            this.selectedPage = selectedPage;
            for (SettingsPage page : SettingsPage.values()) {
                addPage(page);
            }
        }

        private void addPage(SettingsPage page) {
            ButtonWidget<?> button = new CategoryButton(() -> tr(page.titleKey), page.id == selectedPage)
                    .onMousePressed(mouseButton -> {
                        if (getParent() instanceof ScrollWidget) {
                            ScrollWidget<?> scroll = (ScrollWidget<?>) getParent();
                            staged.categoryScroll = scroll.getScrollArea().getScrollY().getScroll();
                        }
                        if (page == SettingsPage.FONT) {
                            openMain(staged);
                        } else {
                            openAdvanced(staged, page.id);
                        }
                        return true;
                    });
            buttons.add(button);
            child(button);
        }

        private int preferredHeight() {
            return buttons.size() * 28;
        }

        @Override
        public boolean layoutWidgets() {
            int y = 0;
            for (ButtonWidget<?> button : buttons) {
                place(button, 0, y, getArea().w(), 24);
                y += 28;
            }
            return true;
        }
    }

    private static final class CategoryButton extends TextButton {
        private final boolean selected;

        private CategoryButton(Supplier<String> label, boolean selected) {
            super(label, false);
            this.selected = selected;
        }

        @Override
        protected int textInset() {
            return 12;
        }

        @Override
        public void drawBackground(ModularGuiContext context, WidgetThemeEntry<?> widgetTheme) {
            if (selected) {
                // drawBackground already runs in local widget coordinates; absolute Area values
                // would be applied twice and paint a large cyan rectangle across the sidebar.
                Gui.drawRect(0, 0, getArea().w(), getArea().h(), 0xB0003438);
                Gui.drawRect(0, 0, 2, getArea().h(), 0xFF00DCE8);
                return;
            }
            super.drawBackground(context, widgetTheme);
        }
    }

    private static final class FilteredFontList extends ListWidget<IWidget, FilteredFontList> {
        private final Staged staged;
        private final FontFieldRefs fieldRefs;
        private List<FontEntry> fonts;

        private FilteredFontList(Staged staged, FontFieldRefs fieldRefs) {
            this.staged = staged;
            this.fieldRefs = fieldRefs;
            scrollDirection(GuiAxis.Y);
            collapseDisabledChild();
            reloadFonts();
        }

        private void reloadFonts() {
            if (staged.fontSource == SOURCE_SYSTEM) {
                List<FontEntry> entries = new ArrayList<>();
                for (String font : localFonts()) {
                    entries.add(new FontEntry(font, ""));
                }
                this.fonts = entries;
            } else if (staged.fontSource == SOURCE_FOLDER) {
                this.fonts = fontFolderFonts();
            } else {
                this.fonts = builtinFonts();
            }
            refresh();
        }

        private void refresh() {
            while (!getChildren().isEmpty()) {
                remove(0);
            }
            for (FontEntry font : fonts) {
                if (staged.matchesSearch(font.displayName)) {
                    child(fontButton(font));
                }
            }
            if (isValid()) {
                getScrollData().scrollTo(getScrollArea(), 0);
                layoutWidgets();
            }
        }

        private ButtonWidget<?> fontButton(FontEntry font) {
            ButtonWidget<?> button = new ButtonWidget<>();
            TextWidget label = new TextWidget(IKey.dynamic(() -> staged.isSelected(font)
                ? selectedFontPrefix() + font.displayName : font.displayName));
            label.alignment(Alignment.CenterLeft);
            label.color(0xFFFFFF);
            label.paddingLeft(6);
            button.child(label);
            button.onMousePressed(mouseButton -> {
                staged.selectFont(font);
                fieldRefs.refreshFrom(staged);
                preview(staged);
                return true;
            });
            button.height(16);
            return button;
        }

        @Override
        public boolean layoutWidgets() {
            int y = getArea().getPadding().getTop();
            int width = Math.max(0, getArea().w() - getArea().getPadding().horizontal());
            for (Object childObject : getChildren()) {
                if (!(childObject instanceof IWidget)) {
                    continue;
                }
                IWidget child = (IWidget) childObject;
                place(child, getArea().getPadding().getLeft(), y, width, 16);
                if (!child.getChildren().isEmpty()) {
                    place(child.getChildren().get(0), 0, 0, width, 16);
                }
                y += 16;
            }
            getScrollData().setScrollSize(y + getArea().getPadding().getBottom());
            return true;
        }
    }

    private static String fontValue(FontEntry font) {
        String path = font.path == null ? "" : font.path.trim();
        return path.isEmpty() ? font.displayName : path;
    }

    private static String selectedFontPrefix() {
        return "> ";
    }

    private static final class StringValue implements IStringValue<String> {
        private final Supplier<String> getter;
        private final Consumer<String> setter;

        private StringValue(Supplier<String> getter, Consumer<String> setter) {
            this.getter = getter;
            this.setter = setter;
        }

        @Override
        public String getStringValue() {
            return getter.get();
        }

        @Override
        public void setStringValue(String value) {
            setter.accept(value);
        }

        @Override
        public String getValue() {
            return getStringValue();
        }

        @Override
        public void setValue(String value) {
            setStringValue(value);
        }

        @Override
        public Class<String> getValueType() {
            return String.class;
        }
    }

    private static final class DoubleValue implements IDoubleValue<Double> {
        private final Supplier<Double> getter;
        private final Consumer<Double> setter;

        private DoubleValue(Supplier<Double> getter, Consumer<Double> setter) {
            this.getter = getter;
            this.setter = setter;
        }

        @Override
        public double getDoubleValue() {
            return getter.get();
        }

        @Override
        public void setDoubleValue(double value) {
            setter.accept(value);
        }

        @Override
        public Double getValue() {
            return getDoubleValue();
        }

        @Override
        public void setValue(Double value) {
            setDoubleValue(value);
        }

        @Override
        public Class<Double> getValueType() {
            return Double.class;
        }
    }

    private static final class PreviewWidget extends Widget<PreviewWidget> {
        private final Staged staged;

        private PreviewWidget(Staged staged) {
            this.staged = staged;
        }

        @Override
        public void draw(ModularGuiContext context, WidgetThemeEntry<?> widgetTheme) {
            super.draw(context, widgetTheme);
            // Custom widget drawing uses local coordinates because ModularUI applies the widget
            // translation before invoking draw(). Absolute Area coordinates would be applied twice.
            int x = 8;
            int y = 8;
            Gui.drawRect(4, 4, Math.max(4, getArea().w() - 4), Math.max(4, getArea().h() - 4), 0x66000000);
            Platform.setupDrawFont();
            Minecraft mc = Minecraft.getMinecraft();
            mc.fontRenderer.drawString(tr("neofontrender.gui.preview.font", staged.selectedFont()), x, y, 0xFFFFFF);
            mc.fontRenderer.drawString(tr("neofontrender.gui.preview.sample"), x, y + 14, 0xD8D8D8);
            mc.fontRenderer.drawString(tr("neofontrender.gui.preview.styles"), x, y + 28, 0xFFFFFF);
        }
    }
}
