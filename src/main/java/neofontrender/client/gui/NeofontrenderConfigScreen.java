package neofontrender.client.gui;

import com.cleanroommc.modularui.api.GuiAxis;
import com.cleanroommc.modularui.api.layout.ILayoutWidget;
import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.api.value.IDoubleValue;
import com.cleanroommc.modularui.api.value.IStringValue;
import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.factory.ClientGUI;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.ModularScreen;
import com.cleanroommc.modularui.screen.RichTooltip;
import com.cleanroommc.modularui.screen.viewport.ModularGuiContext;
import com.cleanroommc.modularui.theme.WidgetThemeEntry;
import com.cleanroommc.modularui.utils.Alignment;
import com.cleanroommc.modularui.widget.ParentWidget;
import com.cleanroommc.modularui.widget.Widget;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.cleanroommc.modularui.widgets.ListWidget;
import com.cleanroommc.modularui.widgets.SliderWidget;
import com.cleanroommc.modularui.widgets.TextWidget;
import com.cleanroommc.modularui.widgets.textfield.TextFieldWidget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.resources.I18n;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import neofontrender.core.config.NeofontrenderConfig;
import neofontrender.core.font.FontManager;
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

    private NeofontrenderConfigScreen() {
    }

    public static void open() {
        openMain(new Staged());
    }

    private static void openMain(Staged staged) {
        ClientGUI.open(new ModularScreen("neofontrender", buildPanel(staged)).useTheme("neofontrender_modern").pausesGame(false));
    }

    private static void openAdvanced(Staged staged) {
        ClientGUI.open(new ModularScreen("neofontrender_advanced", buildAdvancedPanel(staged)).useTheme("neofontrender_modern").pausesGame(false));
    }

    private static ModularPanel buildPanel(Staged staged) {
        ModularPanel panel = new ModularPanel("font_config")
                .relativeToScreen()
                .full();
        panel.child(new FontConfigLayout(staged).relativeToParent().full());
        return panel;
    }

    private static ModularPanel buildAdvancedPanel(Staged staged) {
        ModularPanel panel = new ModularPanel("font_config_advanced")
                .relativeToScreen()
                .full();
        panel.child(new AdvancedConfigLayout(staged).relativeToParent().full());
        return panel;
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
        return withTooltip(new TextButton(() -> tr(labelKey) + ": " + onOff(getter.get()), true)
                .onMousePressed(mouseButton -> {
                    setter.accept(!getter.get());
                    preview.run();
                    return true;
                }), tooltipKey);
    }

    private static <T extends ButtonWidget<?>> T withTooltip(T button, String tooltipKey) {
        if (tooltipKey != null && !tooltipKey.isEmpty()) {
            button.tooltip(new RichTooltip()
                    .showUpTimer(8)
                    .addLine(tr(tooltipKey)));
        }
        return button;
    }

    private static ButtonWidget<?> sourceButton(Staged staged, FilteredFontList[] listRef) {
        return new TextButton(() -> sourceButtonName(staged.fontSource), true)
                .onMousePressed(mouseButton -> {
                    staged.fontSource = (staged.fontSource + 1) % 3;
                    if (listRef[0] != null) {
                        listRef[0].reloadFonts();
                    }
                    return true;
                });
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

    private static ButtonWidget<?> toggleButton(String text, int width, int height, Supplier<Boolean> getter, Consumer<Boolean> setter, Runnable preview) {
        return new TextButton(() -> text + ": " + (getter.get() ? "ON" : "OFF"), true)
                .onMousePressed(mouseButton -> {
                    setter.accept(!getter.get());
                    preview.run();
                    return true;
                });
    }

    private static ButtonWidget<?> styleButton(Staged staged, int width, int height) {
        return withTooltip(new TextButton(() -> tr("neofontrender.gui.option.style") + ": " + styleName(staged.fontStyle), true)
                .onMousePressed(mouseButton -> {
                    staged.fontStyle = (staged.fontStyle + 1) & 3;
                    preview(staged);
                    return true;
                }), "neofontrender.tooltip.style");
    }

    private static ButtonWidget<?> aaModeButton(Staged staged, int width, int height) {
        return withTooltip(new TextButton(() -> tr("neofontrender.gui.option.antialias") + ": " + aaModeName(staged.antialiasMode), true)
                .onMousePressed(mouseButton -> {
                    staged.antialiasMode = nextAaMode(staged.antialiasMode);
                    staged.antialias = !"off".equals(staged.antialiasMode);
                    preview(staged);
                    return true;
                }), "neofontrender.tooltip.antialias");
    }

    private static ButtonWidget<?> engineButton(Staged staged, int width, int height) {
        return withTooltip(new TextButton(() -> tr("neofontrender.gui.option.engine") + ": " + engineName(staged.engine), true)
                .onMousePressed(mouseButton -> {
                    staged.engine = nextEngine(staged.engine);
                    staged.enabled = !"vanilla".equals(staged.engine);
                    preview(staged);
                    return true;
                }), "neofontrender.tooltip.engine");
    }

    private static String nextEngine(String engine) {
        String normalized = normalizeEngine(engine);
        if ("sfr".equals(normalized)) {
            return "skia";
        }
        if ("skia".equals(normalized)) {
            return "vanilla";
        }
        return "sfr";
    }

    private static String engineName(String engine) {
        String normalized = normalizeEngine(engine);
        if ("skia".equals(normalized)) {
            return tr("neofontrender.gui.engine.skia");
        }
        if ("vanilla".equals(normalized)) {
            return tr("neofontrender.gui.engine.vanilla");
        }
        return tr("neofontrender.gui.engine.sfr");
    }

    private static String normalizeEngine(String engine) {
        if (engine == null) {
            return "sfr";
        }
        String normalized = engine.trim().toLowerCase(Locale.ROOT).replace('-', '_');
        if ("skija".equals(normalized) || "skia".equals(normalized)) {
            return "skia";
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
            List<String> fonts = new ArrayList<>(Arrays.asList(names));
            Collections.sort(fonts, String.CASE_INSENSITIVE_ORDER);
            return fonts;
        } catch (Throwable t) {
            return Collections.singletonList("SansSerif");
        }
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
        return lower.endsWith(".ttf") || lower.endsWith(".otf");
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

    private static final class FontConfigLayout extends ParentWidget<FontConfigLayout> implements ILayoutWidget {
        private static final int PAD = 12;
        private static final int GAP = 12;

        private final TextWidget title = header(tr("neofontrender.gui.title"));
        private final FontSidebar sidebar;
        private final SettingsPane settings;
        private final ButtonWidget<?> previewButton;
        private final ButtonWidget<?> advancedButton;
        private final ButtonWidget<?> applyButton;
        private final ButtonWidget<?> cancelButton;

        private FontConfigLayout(Staged staged) {
            TextFieldWidget[] fontNameField = new TextFieldWidget[1];
            TextFieldWidget[] fontPathField = new TextFieldWidget[1];
            FilteredFontList[] listRef = new FilteredFontList[1];

            this.sidebar = new FontSidebar(staged, fontNameField, fontPathField, listRef);
            this.settings = new SettingsPane(staged, fontNameField, fontPathField);
            this.previewButton = actionButtonKey("neofontrender.gui.button.preview", 90, 20, () -> preview(staged));
            this.advancedButton = actionButtonKey("neofontrender.gui.button.advanced", 90, 20, () -> openAdvanced(staged));
            this.applyButton = actionButtonKey("neofontrender.gui.button.apply", 74, 20, () -> {
                apply(staged);
                ClientGUI.close();
            });
            this.cancelButton = actionButtonKey("neofontrender.gui.button.cancel", 74, 20, () -> {
                staged.restoreOriginal();
                reloadFontManager();
                ClientGUI.close();
            });

            child(title);
            child(sidebar);
            child(settings);
            child(previewButton);
            child(advancedButton);
            child(applyButton);
            child(cancelButton);
        }

        @Override
        public boolean layoutWidgets() {
            int width = getArea().w();
            int height = getArea().h();
            int titleHeight = 16;
            int footerHeight = 24;
            int contentTop = PAD + titleHeight + 10;
            int contentBottom = height - PAD - footerHeight - 8;
            int contentHeight = Math.max(80, contentBottom - contentTop);

            place(title, PAD, PAD, Math.max(0, width - PAD * 2), titleHeight);

            if (width < 760) {
                int sidebarHeight = clamp(contentHeight * 42 / 100, 170, Math.max(170, contentHeight - 220));
                int settingsTop = contentTop + sidebarHeight + GAP;
                place(sidebar, PAD, contentTop, Math.max(0, width - PAD * 2), sidebarHeight);
                place(settings, PAD, settingsTop, Math.max(0, width - PAD * 2), Math.max(80, contentBottom - settingsTop));
            } else {
                int sidebarWidth = clamp(width / 3, 300, 420);
                int settingsX = PAD + sidebarWidth + GAP;
                place(sidebar, PAD, contentTop, sidebarWidth, contentHeight);
                place(settings, settingsX, contentTop, Math.max(0, width - settingsX - PAD), contentHeight);
            }

            int buttonWidth = Math.min(140, Math.max(90, (width - PAD * 2 - GAP * 3) / 4));
            int y = height - PAD - footerHeight;
            place(previewButton, PAD, y, buttonWidth, footerHeight);
            place(advancedButton, PAD + buttonWidth + GAP, y, buttonWidth, footerHeight);
            place(cancelButton, width - PAD - buttonWidth, y, buttonWidth, footerHeight);
            place(applyButton, width - PAD - buttonWidth * 2 - GAP, y, buttonWidth, footerHeight);
            return true;
        }
    }

    private static final class AdvancedConfigLayout extends ParentWidget<AdvancedConfigLayout> implements ILayoutWidget {
        private static final int PAD = 12;
        private static final int GAP = 12;

        private final TextWidget title = header(tr("neofontrender.gui.title.advanced"));
        private final PipelineInfoWidget pipelineInfo;
        private final AdvancedOptionsSection options;
        private final BrightnessSection brightness;
        private final ButtonWidget<?> backButton;
        private final ButtonWidget<?> applyButton;
        private final ButtonWidget<?> cancelButton;

        private AdvancedConfigLayout(Staged staged) {
            this.pipelineInfo = new PipelineInfoWidget(staged);
            this.options = new AdvancedOptionsSection(staged);
            this.brightness = new BrightnessSection(staged);
            this.backButton = actionButtonKey("neofontrender.gui.button.back", 80, 20, () -> openMain(staged));
            this.applyButton = actionButtonKey("neofontrender.gui.button.apply", 80, 20, () -> {
                apply(staged);
                ClientGUI.close();
            });
            this.cancelButton = actionButtonKey("neofontrender.gui.button.cancel", 80, 20, () -> {
                staged.restoreOriginal();
                reloadFontManager();
                ClientGUI.close();
            });

            child(title);
            child(pipelineInfo);
            child(options);
            child(brightness);
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

            int contentWidth = Math.max(0, width - PAD * 2);
            int infoHeight = 136;
            place(pipelineInfo, PAD, y, contentWidth, infoHeight);
            y += infoHeight + GAP;

            int optionsHeight = width < 520 ? 172 : 112;
            place(options, PAD, y, contentWidth, optionsHeight);
            y += optionsHeight + GAP;

            place(brightness, PAD, y, contentWidth, 46);

            int buttonWidth = Math.min(140, Math.max(90, (width - PAD * 2 - GAP * 3) / 4));
            int footerY = height - PAD - footerHeight;
            place(backButton, PAD, footerY, buttonWidth, footerHeight);
            place(cancelButton, width - PAD - buttonWidth, footerY, buttonWidth, footerHeight);
            place(applyButton, width - PAD - buttonWidth * 2 - GAP, footerY, buttonWidth, footerHeight);
            return true;
        }
    }

    private static final class FontSidebar extends ParentWidget<FontSidebar> implements ILayoutWidget {
        private final TextWidget searchLabel = label(tr("neofontrender.gui.label.search_fonts"));
        private final TextFieldWidget searchField;
        private final ButtonWidget<?> sourceButton;
        private final ButtonWidget<?> refreshButton;
        private final ButtonWidget<?> openFolderButton;
        private final TextWidget sourceTitle;
        private final FilteredFontList fontList;

        private FontSidebar(Staged staged, TextFieldWidget[] nameField, TextFieldWidget[] pathField, FilteredFontList[] listRef) {
            this.searchField = new TextFieldWidget()
                    .setMaxLength(128)
                    .value(new StringValue(() -> staged.search, v -> {
                        staged.search = v;
                        if (listRef[0] != null) {
                            listRef[0].refresh();
                        }
                    }));
            this.sourceButton = sourceButton(staged, listRef);
            this.refreshButton = actionButtonKey("neofontrender.gui.button.refresh", 72, 18, () -> {
                if (listRef[0] != null) {
                    listRef[0].reloadFonts();
                }
            });
            this.openFolderButton = actionButtonKey("neofontrender.gui.button.open_folder", 100, 18, NeofontrenderConfigScreen::openFontFolder);
            this.sourceTitle = dynamicLabel(() -> sourceTitle(staged.fontSource));
            this.fontList = new FilteredFontList(staged, nameField, pathField);
            listRef[0] = this.fontList;

            child(searchLabel);
            child(searchField);
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

            int gap = 6;
            int first = Math.min(88, Math.max(64, (width - gap * 2) / 4));
            int second = first;
            int third = Math.max(80, width - first - second - gap * 2);
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
        private final FieldBlock fontName;
        private final FieldBlock fontPath;
        private final FieldBlock fallbacks;
        private final MetricsSection metrics;
        private final OversampleSection oversample;
        private final OptionsSection options;
        private final PreviewWidget preview;

        private SettingsPane(Staged staged, TextFieldWidget[] fontNameField, TextFieldWidget[] fontPathField) {
            fontNameField[0] = new TextFieldWidget()
                    .setMaxLength(256)
                    .value(new StringValue(() -> staged.fontName, v -> staged.fontName = v));
            fontPathField[0] = new TextFieldWidget()
                    .setMaxLength(512)
                    .value(new StringValue(() -> staged.fontPath, v -> staged.fontPath = v));

            this.fontName = new FieldBlock(tr("neofontrender.gui.label.font_name"), fontNameField[0]);
            this.fontPath = new FieldBlock(tr("neofontrender.gui.label.local_path"), fontPathField[0]);
            this.fallbacks = new FieldBlock(tr("neofontrender.gui.label.fallbacks"), new TextFieldWidget()
                    .setMaxLength(512)
                    .value(new StringValue(() -> staged.fontFallbacks, v -> staged.fontFallbacks = v)));
            this.metrics = new MetricsSection(staged);
            this.oversample = new OversampleSection(staged);
            this.options = new OptionsSection(staged);
            this.preview = new PreviewWidget(staged);

            child(fontName);
            child(fontPath);
            child(fallbacks);
            child(metrics);
            child(oversample);
            child(options);
            child(preview);
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
            int metricsHeight = width < 360 ? 84 : fieldHeight;
            place(metrics, 0, y, width, metricsHeight);
            y += metricsHeight + gap;
            place(oversample, 0, y, width, 46);
            y += 46 + gap;
            int optionsHeight = width < 520 ? 142 : 82;
            place(options, 0, y, width, optionsHeight);
            y += optionsHeight + gap;
            place(preview, 0, y, width, Math.max(0, height - y));
            return true;
        }
    }

    private static final class FieldBlock extends ParentWidget<FieldBlock> implements ILayoutWidget {
        private final TextWidget label;
        private final TextFieldWidget field;

        private FieldBlock(String label, TextFieldWidget field) {
            this.label = label(label);
            this.field = field;
            child(this.label);
            child(this.field);
        }

        @Override
        public boolean layoutWidgets() {
            int width = getArea().w();
            place(label, 0, 0, width, 12);
            place(field, 0, 16, width, Math.max(18, getArea().h() - 16));
            return true;
        }
    }

    private static final class MetricsSection extends ParentWidget<MetricsSection> implements ILayoutWidget {
        private final FieldBlock size;
        private final FieldBlock baseline;

        private MetricsSection(Staged staged) {
            this.size = new FieldBlock(tr("neofontrender.gui.label.size"), new TextFieldWidget()
                    .setMaxLength(8)
                    .value(new StringValue(() -> staged.fontSize, v -> staged.fontSize = v)));
            this.baseline = new FieldBlock(tr("neofontrender.gui.label.baseline"), new TextFieldWidget()
                    .setMaxLength(8)
                    .value(new StringValue(() -> staged.baselineShift, v -> staged.baselineShift = v)));
            child(size);
            child(baseline);
        }

        @Override
        public boolean layoutWidgets() {
            int width = getArea().w();
            if (width < 360) {
                int half = Math.max(18, (getArea().h() - 8) / 2);
                place(size, 0, 0, width, half);
                place(baseline, 0, half + 8, width, half);
                return true;
            }
            int gap = 10;
            int item = Math.max(0, (width - gap) / 2);
            place(size, 0, 0, item, getArea().h());
            place(baseline, item + gap, 0, Math.max(0, width - item - gap), getArea().h());
            return true;
        }
    }

    private static final class OversampleSection extends ParentWidget<OversampleSection> implements ILayoutWidget {
        private final TextWidget label;
        private final SliderWidget slider;

        private OversampleSection(Staged staged) {
            this.label = dynamicLabel(() -> tr("neofontrender.gui.label.oversample") + ": " + staged.oversample + "x"
                    + (staged.adaptiveRasterScale ? " (" + tr("neofontrender.gui.option.autoscale") + ")" : " (" + tr("neofontrender.gui.manual") + ")"));
            this.slider = new SliderWidget()
                    .value(new DoubleValue(
                            () -> (double) parseFloat(staged.oversample, 8.0F, 1.0F, 16.0F),
                            v -> {
                                staged.oversample = String.format(Locale.ROOT, "%.1f", Math.max(1.0D, Math.min(16.0D, v)));
                                preview(staged);
                            }))
                    .bounds(1.0D, 16.0D)
                    .stopper(1.0D, 2.0D, 3.0D, 4.0D, 6.0D, 8.0D, 12.0D, 16.0D);
            child(label);
            child(slider);
        }

        @Override
        public boolean layoutWidgets() {
            place(label, 0, 0, getArea().w(), 12);
            place(slider, 0, 18, getArea().w(), Math.max(20, getArea().h() - 18));
            return true;
        }
    }

    private static final class OptionsSection extends ParentWidget<OptionsSection> implements ILayoutWidget {
        private final ButtonWidget<?> enabled;
        private final ButtonWidget<?> engine;
        private final ButtonWidget<?> skiaString;
        private final ButtonWidget<?> autoBase;
        private final ButtonWidget<?> aa;
        private final ButtonWidget<?> fractional;
        private final ButtonWidget<?> style;
        private final ButtonWidget<?> builtins;
        private final ButtonWidget<?> autoScale;

        private OptionsSection(Staged staged) {
            this.enabled = toggleButtonKey("neofontrender.gui.option.enabled", "neofontrender.tooltip.enabled", 80, 20, () -> staged.enabled, v -> staged.enabled = v, () -> preview(staged));
            this.engine = engineButton(staged, 96, 20);
            this.skiaString = toggleButtonKey("neofontrender.gui.option.string_mode", "neofontrender.tooltip.string_mode", 80, 20, () -> staged.skiaAdvancedStringMode, v -> staged.skiaAdvancedStringMode = v, () -> preview(staged));
            this.autoBase = toggleButtonKey("neofontrender.gui.option.autobase", "neofontrender.tooltip.autobase", 80, 20, () -> staged.autoBaseline, v -> staged.autoBaseline = v, () -> preview(staged));
            this.aa = aaModeButton(staged, 140, 20);
            this.fractional = toggleButtonKey("neofontrender.gui.option.fractional", "neofontrender.tooltip.fractional", 80, 20, () -> staged.fractionalMetrics, v -> staged.fractionalMetrics = v, () -> preview(staged));
            this.style = styleButton(staged, 80, 20);
            this.builtins = toggleButtonKey("neofontrender.gui.option.builtins", "neofontrender.tooltip.builtins", 80, 20, () -> staged.builtinFallbacks, v -> staged.builtinFallbacks = v, () -> preview(staged));
            this.autoScale = toggleButtonKey("neofontrender.gui.option.autoscale", "neofontrender.tooltip.autoscale", 80, 20, () -> staged.adaptiveRasterScale, v -> staged.adaptiveRasterScale = v, () -> preview(staged));
            child(enabled);
            child(engine);
            child(skiaString);
            child(autoBase);
            child(aa);
            child(fractional);
            child(style);
            child(builtins);
            child(autoScale);
        }

        @Override
        public boolean layoutWidgets() {
            int width = getArea().w();
            int gap = 8;
            if (width < 520) {
                int buttonHeight = 22;
                int buttonWidth = Math.max(0, (width - gap) / 2);
                IWidget[] buttons = {engine, skiaString, enabled, autoBase, aa, fractional, style, builtins, autoScale};
                for (int i = 0; i < buttons.length; i++) {
                    int col = i & 1;
                    int row = i / 2;
                    int x = col == 0 ? 0 : buttonWidth + gap;
                    place(buttons[i], x, row * (buttonHeight + gap), col == 0 ? buttonWidth : width - x, buttonHeight);
                }
                return true;
            }
            int buttonHeight = 22;
            int third = Math.max(0, (width - gap * 2) / 3);
            IWidget[] buttons = {engine, skiaString, enabled, autoBase, aa, fractional, style, builtins, autoScale};
            for (int i = 0; i < buttons.length; i++) {
                int col = i % 3;
                int row = i / 3;
                int x = col * (third + gap);
                place(buttons[i], x, row * (buttonHeight + gap), col == 2 ? Math.max(0, width - x) : third, buttonHeight);
            }
            return true;
        }
    }

    private static final class AdvancedOptionsSection extends ParentWidget<AdvancedOptionsSection> implements ILayoutWidget {
        private final ButtonWidget<?> autoScale;
        private final ButtonWidget<?> interpolation;
        private final ButtonWidget<?> mipmap;
        private final ButtonWidget<?> pipeline;
        private final ButtonWidget<?> shader;
        private final ButtonWidget<?> edgeBleed;
        private final ButtonWidget<?> gpuOffscreen;
        private final ButtonWidget<?> gpuCpuSubmit;
        private final ButtonWidget<?> integerScale;
        private final ButtonWidget<?> highMag;
        private final ButtonWidget<?> anisotropic;
        private final ButtonWidget<?> debugStats;

        private AdvancedOptionsSection(Staged staged) {
            this.autoScale = toggleButtonKey("neofontrender.gui.option.autoscale", "neofontrender.tooltip.autoscale", 80, 20, () -> staged.adaptiveRasterScale, v -> staged.adaptiveRasterScale = v, () -> preview(staged));
            this.interpolation = toggleButtonKey("neofontrender.gui.option.linear", "neofontrender.tooltip.linear", 80, 20, () -> staged.interpolation, v -> staged.interpolation = v, () -> preview(staged));
            this.mipmap = toggleButtonKey("neofontrender.gui.option.mipmap", "neofontrender.tooltip.mipmap", 80, 20, () -> staged.mipmap, v -> staged.mipmap = v, () -> preview(staged));
            this.pipeline = toggleButtonKey("neofontrender.gui.option.pipeline", "neofontrender.tooltip.pipeline", 80, 20, () -> staged.enhancedTextPipeline, v -> staged.enhancedTextPipeline = v, () -> preview(staged));
            this.shader = toggleButtonKey("neofontrender.gui.option.shader", "neofontrender.tooltip.shader", 80, 20, () -> staged.shaderTextPipeline, v -> staged.shaderTextPipeline = v, () -> preview(staged));
            this.edgeBleed = toggleButtonKey("neofontrender.gui.option.edge_bleed", "neofontrender.tooltip.edge_bleed", 80, 20, () -> staged.textureEdgeBleed, v -> staged.textureEdgeBleed = v, () -> preview(staged));
            this.gpuOffscreen = toggleButtonKey("neofontrender.gui.option.gpu_offscreen", "neofontrender.tooltip.gpu_offscreen", 80, 20, () -> staged.skiaGpuOffscreen, v -> {
                staged.skiaGpuOffscreen = v;
                if (v) {
                    staged.skiaGpuSubmitViaCpuTexture = true;
                }
            }, () -> preview(staged));
            this.gpuCpuSubmit = toggleButtonKey("neofontrender.gui.option.gpu_cpu_submit", "neofontrender.tooltip.gpu_cpu_submit", 80, 20, () -> staged.skiaGpuSubmitViaCpuTexture, v -> {
                staged.skiaGpuSubmitViaCpuTexture = v;
                if (v) {
                    staged.skiaGpuOffscreen = true;
                }
            }, () -> preview(staged));
            this.integerScale = toggleButtonKey("neofontrender.gui.option.integer_scale", "neofontrender.tooltip.integer_scale", 80, 20, () -> staged.excludeIntegerScale, v -> staged.excludeIntegerScale = v, () -> preview(staged));
            this.highMag = toggleButtonKey("neofontrender.gui.option.high_mag", "neofontrender.tooltip.high_mag", 80, 20, () -> staged.excludeHighMagnification, v -> staged.excludeHighMagnification = v, () -> preview(staged));
            this.anisotropic = toggleButtonKey("neofontrender.gui.option.anisotropic", "neofontrender.tooltip.anisotropic", 80, 20, () -> staged.anisotropicFiltering, v -> staged.anisotropicFiltering = v, () -> preview(staged));
            this.debugStats = toggleButtonKey("neofontrender.gui.option.debug_stats", "neofontrender.tooltip.debug_stats", 80, 20, () -> staged.debugRenderStats, v -> staged.debugRenderStats = v, () -> preview(staged));
            child(autoScale);
            child(interpolation);
            child(mipmap);
            child(pipeline);
            child(shader);
            child(edgeBleed);
            child(gpuOffscreen);
            child(gpuCpuSubmit);
            child(integerScale);
            child(highMag);
            child(anisotropic);
            child(debugStats);
        }

        @Override
        public boolean layoutWidgets() {
            int width = getArea().w();
            int gap = 8;
            int buttonHeight = 22;
            if (width < 520) {
                int buttonWidth = Math.max(0, (width - gap) / 2);
                IWidget[] buttons = {autoScale, interpolation, mipmap, pipeline, shader, edgeBleed, gpuOffscreen, gpuCpuSubmit, integerScale, highMag, anisotropic, debugStats};
                for (int i = 0; i < buttons.length; i++) {
                    int col = i & 1;
                    int row = i / 2;
                    int x = col == 0 ? 0 : buttonWidth + gap;
                    place(buttons[i], x, row * (buttonHeight + gap), col == 0 ? buttonWidth : width - x, buttonHeight);
                }
                return true;
            }
            int third = Math.max(0, (width - gap * 2) / 3);
            IWidget[] buttons = {autoScale, interpolation, mipmap, pipeline, shader, edgeBleed, gpuOffscreen, gpuCpuSubmit, integerScale, highMag, anisotropic, debugStats};
            for (int i = 0; i < buttons.length; i++) {
                int col = i % 3;
                int row = i / 3;
                int x = col * (third + gap);
                place(buttons[i], x, row * (buttonHeight + gap), col == 2 ? Math.max(0, width - x) : third, buttonHeight);
            }
            return true;
        }
    }

    private static final class BrightnessSection extends ParentWidget<BrightnessSection> implements ILayoutWidget {
        private final TextWidget label;
        private final SliderWidget slider;

        private BrightnessSection(Staged staged) {
            this.label = dynamicLabel(() -> tr("neofontrender.gui.label.brightness") + ": " + staged.brightness);
            this.slider = new SliderWidget()
                    .value(new DoubleValue(
                            () -> (double) parseFloat(staged.brightness, 3.0F, 0.0F, 12.0F),
                            v -> {
                                staged.brightness = String.format(Locale.ROOT, "%.1f", Math.max(0.0D, Math.min(12.0D, v)));
                                preview(staged);
                            }))
                    .bounds(0.0D, 12.0D)
                    .stopper(0.0D, 1.0D, 2.0D, 3.0D, 4.0D, 6.0D, 8.0D, 12.0D);
            child(label);
            child(slider);
        }

        @Override
        public boolean layoutWidgets() {
            place(label, 0, 0, getArea().w(), 12);
            place(slider, 0, 18, getArea().w(), Math.max(20, getArea().h() - 18));
            return true;
        }
    }

    private static final class PipelineInfoWidget extends Widget<PipelineInfoWidget> {
        private final Staged staged;

        private PipelineInfoWidget(Staged staged) {
            this.staged = staged;
        }

        @Override
        public void draw(ModularGuiContext context, WidgetThemeEntry<?> widgetTheme) {
            super.draw(context, widgetTheme);
            FontRenderTuning.updateFromCurrentGlState();
            float configured = parseFloat(staged.oversample, 8.0F, 1.0F, 16.0F);
            float effective = FontRenderTuning.rasterScale(configured);
            float guiScale = FontRenderTuning.currentGuiScale();
            String filtering = FontRenderTuning.useLinearFiltering(effective)
                    ? tr("neofontrender.gui.filter.linear")
                    : tr("neofontrender.gui.filter.nearest");
            int x = getArea().x() + 8;
            int y = getArea().y() + 8;
            Gui.drawRect(getArea().x() + 4, getArea().y() + 4, getArea().ex() - 4, getArea().ey() - 4, 0x66000000);
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
        private final int originalFontStyle = NeofontrenderConfig.fontStyle();
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
        private final boolean originalDebugRenderStats = NeofontrenderConfig.debugRenderStats();
        private final String originalBrightness = Float.toString(NeofontrenderConfig.renderingBrightness());
        private final boolean originalTextureEdgeBleed = NeofontrenderConfig.textureEdgeBleed();

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
        private boolean debugRenderStats = originalDebugRenderStats;
        private String brightness = originalBrightness;
        private boolean textureEdgeBleed = originalTextureEdgeBleed;
        private String fontName = originalFontName;
        private String fontPath = originalFontName.endsWith(".ttf") || originalFontName.endsWith(".otf") ? originalFontName : "";
        private String fontFallbacks = originalFontFallbacks;
        private int fontStyle = originalFontStyle;
        private String fontSize = originalFontSize;
        private String oversample = originalOversample;
        private boolean autoBaseline = originalAutoBaseline;
        private String baselineShift = originalBaselineShift;
        private boolean antialias = originalAntialias;
        private String antialiasMode = originalAntialiasMode;
        private boolean fractionalMetrics = originalFractionalMetrics;
        private String search = "";
        private int fontSource = SOURCE_SYSTEM;
        private boolean builtinFallbacks = originalBuiltinFallbacks;

        private String selectedFont() {
            String path = fontPath == null ? "" : fontPath.trim();
            return path.isEmpty() ? fontName.trim() : path;
        }

        private boolean matchesSearch(String font) {
            String query = search == null ? "" : search.trim().toLowerCase(Locale.ROOT);
            return query.isEmpty() || font.toLowerCase(Locale.ROOT).contains(query);
        }

        private boolean isSelected(FontEntry font) {
            String path = font.path == null ? "" : font.path;
            return path.isEmpty()
                    ? fontPath.isEmpty() && fontName.equals(font.displayName)
                    : selectedFont().equals(path);
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
            NeofontrenderConfig.setDebugRenderStats(debugRenderStats);
            NeofontrenderConfig.setRenderingBrightness(parseFloat(brightness, 3.0F, 0.0F, 12.0F));
            NeofontrenderConfig.setTextureEdgeBleed(textureEdgeBleed);
                NeofontrenderConfig.setFontName(selectedFont().isEmpty()
                    ? "neofontrender:fonts/sarasa_ui_sc_regular.ttf"
                    : selectedFont());
            NeofontrenderConfig.setFontFallbacks(parseFonts(fontFallbacks));
            NeofontrenderConfig.setFontStyle(fontStyle);
            NeofontrenderConfig.setFontSize(parseFloat(fontSize, 10.0F, 4.0F, 64.0F));
            NeofontrenderConfig.setFontOversample(parseFloat(oversample, 8.0F, 1.0F, 16.0F));
            NeofontrenderConfig.setFontAutoBaseline(autoBaseline);
            NeofontrenderConfig.setFontBaselineShift(parseFloat(baselineShift, 0.0F, -16.0F, 16.0F));
            NeofontrenderConfig.setFontAntialias(antialias);
            NeofontrenderConfig.setFontAntialiasMode(antialias ? antialiasMode : "off");
            NeofontrenderConfig.setFontFractionalMetrics(fractionalMetrics);
            NeofontrenderConfig.setBuiltinFallbacksEnabled(builtinFallbacks);
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
            NeofontrenderConfig.setDebugRenderStats(originalDebugRenderStats);
            NeofontrenderConfig.setRenderingBrightness(parseFloat(originalBrightness, 3.0F, 0.0F, 12.0F));
            NeofontrenderConfig.setTextureEdgeBleed(originalTextureEdgeBleed);
            NeofontrenderConfig.setFontName(originalFontName);
            NeofontrenderConfig.setFontFallbacks(parseFonts(originalFontFallbacks));
            NeofontrenderConfig.setFontStyle(originalFontStyle);
            NeofontrenderConfig.setFontSize(parseFloat(originalFontSize, 8.0F, 4.0F, 64.0F));
            NeofontrenderConfig.setFontOversample(parseFloat(originalOversample, 8.0F, 1.0F, 16.0F));
            NeofontrenderConfig.setFontAutoBaseline(originalAutoBaseline);
            NeofontrenderConfig.setFontBaselineShift(parseFloat(originalBaselineShift, 0.0F, -16.0F, 16.0F));
            NeofontrenderConfig.setFontAntialias(originalAntialias);
            NeofontrenderConfig.setFontAntialiasMode(originalAntialias ? originalAntialiasMode : "off");
            NeofontrenderConfig.setFontFractionalMetrics(originalFractionalMetrics);
            NeofontrenderConfig.setBuiltinFallbacksEnabled(originalBuiltinFallbacks);
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
        public void drawForeground(ModularGuiContext context) {
            super.drawForeground(context);
            Minecraft mc = Minecraft.getMinecraft();
            String text = label.get();
            int maxWidth = Math.max(1, getArea().w() - 8);
            String visible = mc.fontRenderer.trimStringToWidth(text, maxWidth);
            int x = getArea().x() + 4;
            if (centered) {
                x = getArea().x() + Math.max(4, (getArea().w() - mc.fontRenderer.getStringWidth(visible)) / 2);
            }
            int y = getArea().y() + Math.max(0, (getArea().h() - mc.fontRenderer.FONT_HEIGHT) / 2);
            mc.fontRenderer.drawString(visible, x, y, 0xFFFFFF);
        }
    }

    private static final class FilteredFontList extends ListWidget<IWidget, FilteredFontList> {
        private final Staged staged;
        private final TextFieldWidget[] nameField;
        private final TextFieldWidget[] pathField;
        private List<FontEntry> fonts;

        private FilteredFontList(Staged staged, TextFieldWidget[] nameField, TextFieldWidget[] pathField) {
            this.staged = staged;
            this.nameField = nameField;
            this.pathField = pathField;
            scrollDirection(GuiAxis.Y);
            collapseDisabledChild();
            reloadFonts();
        }

        private void reloadFonts() {
            while (!getChildren().isEmpty()) {
                remove(0);
            }
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
            for (FontEntry font : fonts) {
                child(fontButton(font));
            }
            refresh();
        }

        private void refresh() {
            for (int i = 0; i < getChildren().size() && i < fonts.size(); i++) {
                Object child = getChildren().get(i);
                if (child instanceof IWidget) {
                    ((IWidget) child).setEnabled(staged.matchesSearch(fonts.get(i).displayName));
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
                ? "> " + font.displayName : font.displayName));
            label.alignment(Alignment.CenterLeft);
            label.color(0xFFFFFF);
            label.paddingLeft(6);
            button.child(label);
            button.onMousePressed(mouseButton -> {
                staged.fontName = font.displayName;
                staged.fontPath = font.path;
                if (nameField[0] != null) {
                    nameField[0].setText(font.displayName);
                }
                if (pathField[0] != null) {
                    pathField[0].setText(font.path);
                }
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
                if (!child.isEnabled()) {
                    continue;
                }
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
            int x = getArea().x() + 8;
            int y = getArea().y() + 8;
            Gui.drawRect(getArea().x() + 4, getArea().y() + 4, getArea().ex() - 4, getArea().ey() - 4, 0x66000000);
            Minecraft mc = Minecraft.getMinecraft();
            mc.fontRenderer.drawString(tr("neofontrender.gui.preview.font", staged.selectedFont()), x, y, 0xFFFFFF);
            mc.fontRenderer.drawString(tr("neofontrender.gui.preview.sample"), x, y + 14, 0xD8D8D8);
            mc.fontRenderer.drawString(tr("neofontrender.gui.preview.styles"), x, y + 28, 0xFFFFFF);
        }
    }
}
