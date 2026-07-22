package neofontrender.addons.tooltips;

import com.cleanroommc.modularui.api.widget.IWidget;
import neofontrender.api.client.settings.NfrSettingsPage;
import neofontrender.api.client.settings.NfrSettingsPageContext;
import neofontrender.api.client.settings.NfrSettingsPageSession;
import neofontrender.client.gui.component.base.NfrOptionsGrid;
import neofontrender.client.gui.component.business.NfrSettingsControls;
import neofontrender.client.gui.views.NfrContentView;
import neofontrender.addons.ui.NfrUiEnhancements;

import java.util.Arrays;
import java.util.List;

final class ModernTooltipSettingsPage implements NfrSettingsPage {
    @Override public String id() { return NfrUiEnhancements.MOD_ID + ":tooltips"; }
    @Override public String titleKey() { return "neofontrender_ui_enhancements.gui.tooltips.category"; }
    @Override public String title() { return AddonI18n.tr(titleKey()); }
    @Override public NfrSettingsPageSession createSession() { return new Session(); }

    private static final class Session implements NfrSettingsPageSession {
        private final TooltipConfig.Snapshot original = TooltipConfig.snapshot();

        @Override public IWidget createView(NfrSettingsPageContext context) {
            NfrSettingsControls c = context.controls();
            NfrOptionsGrid grid = c.grid()
                    .add(c.toggleText(() -> tr("gui.enabled"), () -> tr("tooltip.enabled"),
                            () -> TooltipConfig.enabled, value -> TooltipConfig.enabled = value))
                    .add(c.dropdownText("tooltip_style", () -> tr("gui.style"),
                            () -> TooltipConfig.renderStyle,
                            value -> TooltipConfig.renderStyle = TooltipConfig.normalizeStyle(value),
                            Arrays.asList("modernui", "mica", "legacy"),
                            value -> tr("gui.style." + value)).size(260, 24))
                    .add(c.toggleText(() -> tr("gui.legendary"), () -> tr("tooltip.legendary"),
                            () -> TooltipConfig.yieldToLegendaryTooltips, value -> TooltipConfig.yieldToLegendaryTooltips = value))
                    .add(c.toggleText(() -> tr("gui.obscure_yield"), () -> tr("tooltip.obscure_yield"),
                            () -> TooltipConfig.yieldToObscureTooltips, value -> TooltipConfig.yieldToObscureTooltips = value))
                    .add(c.toggleText(() -> tr("gui.hei_custom"), () -> tr("tooltip.hei_custom"),
                            () -> TooltipConfig.heiCustomTooltips, value -> TooltipConfig.heiCustomTooltips = value))
                    .add(c.toggleText(() -> tr("gui.mod_name"), () -> tr("tooltip.mod_name"),
                            () -> TooltipConfig.modNameEnabled, value -> TooltipConfig.modNameEnabled = value))
                    .add(c.dropdownText("tooltip_mod_name_format", () -> tr("gui.mod_name_format"),
                            () -> TooltipConfig.modNameFormat,
                            value -> TooltipConfig.modNameFormat = value,
                            Arrays.asList("", "blue italic", "gray italic", "dark_gray italic",
                                    "aqua italic", "gold italic", "blue", "gray"),
                            ModernTooltipSettingsPage::modNameFormatLabel).size(260, 24))
                    .add(c.toggleText(() -> tr("gui.rounded"), () -> "",
                            () -> TooltipConfig.rounded, value -> TooltipConfig.rounded = value))
                    .add(c.toggleText(() -> tr("gui.center_title"), () -> "",
                            () -> TooltipConfig.centerTitle, value -> TooltipConfig.centerTitle = value))
                    .add(c.toggleText(() -> tr("gui.title_break"), () -> "",
                            () -> TooltipConfig.titleBreak, value -> TooltipConfig.titleBreak = value))
                    .add(c.toggleText(() -> tr("gui.adaptive_border"), () -> "",
                            () -> TooltipConfig.adaptiveBorder, value -> TooltipConfig.adaptiveBorder = value))
                    .add(c.dropdownText("tooltip_border_shading", () -> tr("gui.border_shading"),
                            () -> TooltipConfig.borderShading,
                            value -> TooltipConfig.borderShading = TooltipConfig.normalizeBorderShading(value),
                            Arrays.asList("gradient", "solid", "horizontal", "vertical", "spectrum"),
                            value -> tr("gui.border_shading." + value)).size(260, 24))
                    .add(c.dropdownText("tooltip_border_cycle", () -> tr("gui.border_cycle"),
                            () -> Integer.toString(TooltipConfig.borderCycleMillis),
                            value -> TooltipConfig.borderCycleMillis = Integer.parseInt(value),
                            Arrays.asList("250", "500", "1000", "2000", "4000", "8000"),
                            value -> value + " ms").size(260, 24))
                    .add(c.dropdownText("tooltip_corner_radius", () -> tr("gui.corner_radius"),
                            () -> number(TooltipConfig.cornerRadius), value -> TooltipConfig.cornerRadius = Float.parseFloat(value),
                            Arrays.asList("0", "2", "4", "6", "8", "12", "16"), value -> value).size(260, 24))
                    .add(c.dropdownText("tooltip_border_width", () -> tr("gui.border_width"),
                            () -> number(TooltipConfig.borderWidth), value -> TooltipConfig.borderWidth = Float.parseFloat(value),
                            Arrays.asList("0.5", "1", "1.25", "1.5", "2", "3", "4"), value -> value).size(260, 24))
                    .add(c.dropdownText("tooltip_shadow_radius", () -> tr("gui.shadow_radius"),
                            () -> number(TooltipConfig.shadowRadius), value -> TooltipConfig.shadowRadius = Float.parseFloat(value),
                            Arrays.asList("0", "2", "4", "6", "8", "10", "12"), value -> value).size(260, 24))
                    .add(c.dropdownText("tooltip_shadow_alpha", () -> tr("gui.shadow_alpha"),
                            () -> Integer.toString(TooltipConfig.shadowAlpha), value -> TooltipConfig.shadowAlpha = Integer.parseInt(value),
                            Arrays.asList("0", "32", "48", "72", "96", "128", "160", "192"), value -> value).size(260, 24))
                    .add(c.dropdownText("tooltip_shadow_x", () -> tr("gui.shadow_x"),
                            () -> number(TooltipConfig.shadowOffsetX), value -> TooltipConfig.shadowOffsetX = Float.parseFloat(value),
                            Arrays.asList("-4", "-2", "-1", "0", "1", "2", "4"), value -> value).size(260, 24))
                    .add(c.dropdownText("tooltip_shadow_y", () -> tr("gui.shadow_y"),
                            () -> number(TooltipConfig.shadowOffsetY), value -> TooltipConfig.shadowOffsetY = Float.parseFloat(value),
                            Arrays.asList("-4", "-2", "-1", "0", "1", "2", "4"), value -> value).size(260, 24))
                    .add(c.dropdownText("tooltip_shadow_steps", () -> tr("gui.shadow_steps"),
                            () -> Integer.toString(TooltipConfig.shadowSteps), value -> TooltipConfig.shadowSteps = Integer.parseInt(value),
                            Arrays.asList("4", "6", "8", "12", "16", "24", "32"), value -> value).size(260, 24))
                    .add(c.dropdownText("tooltip_corner_segments", () -> tr("gui.corner_segments"),
                            () -> Integer.toString(TooltipConfig.cornerSegments), value -> TooltipConfig.cornerSegments = Integer.parseInt(value),
                            Arrays.asList("4", "6", "8", "12", "16", "24", "32"), value -> value).size(260, 24))
                    .add(c.dropdownText("tooltip_aa_width", () -> tr("gui.aa_width"),
                            () -> number(TooltipConfig.antialiasWidth), value -> TooltipConfig.antialiasWidth = Float.parseFloat(value),
                            Arrays.asList("0", "0.35", "0.55", "0.75", "1", "1.5"), value -> value).size(260, 24))
                    .add(c.toggleText(() -> tr("gui.text_shadow"), () -> "",
                            () -> TooltipConfig.textShadow, value -> TooltipConfig.textShadow = value))
                    .add(c.dropdownText("tooltip_divider_alpha", () -> tr("gui.divider_alpha"),
                            () -> Integer.toString(TooltipConfig.dividerAlpha), value -> TooltipConfig.dividerAlpha = Integer.parseInt(value),
                            Arrays.asList("0", "64", "96", "128", "160", "176", "208", "255"), value -> value).size(260, 24))
                    .add(c.dropdownText("tooltip_h_padding", () -> tr("gui.horizontal_padding"),
                            () -> Integer.toString(TooltipConfig.horizontalPadding), value -> TooltipConfig.horizontalPadding = Integer.parseInt(value),
                            integerValues(1, 12), value -> value).size(260, 24))
                    .add(c.dropdownText("tooltip_v_padding", () -> tr("gui.vertical_padding"),
                            () -> Integer.toString(TooltipConfig.verticalPadding), value -> TooltipConfig.verticalPadding = Integer.parseInt(value),
                            integerValues(1, 12), value -> value).size(260, 24))
                    .add(c.dropdownText("tooltip_line_height", () -> tr("gui.line_height"),
                            () -> Integer.toString(TooltipConfig.lineHeight), value -> TooltipConfig.lineHeight = Integer.parseInt(value),
                            integerValues(8, 16), value -> value).size(260, 24))
                    .add(c.dropdownText("tooltip_title_gap", () -> tr("gui.title_gap"),
                            () -> Integer.toString(TooltipConfig.titleGap), value -> TooltipConfig.titleGap = Integer.parseInt(value),
                            integerValues(0, 10), value -> value).size(260, 24))
                    .add(c.dropdownText("tooltip_cursor_offset", () -> tr("gui.cursor_offset"),
                            () -> Integer.toString(TooltipConfig.cursorOffset), value -> TooltipConfig.cursorOffset = Integer.parseInt(value),
                            Arrays.asList("0", "4", "8", "10", "12", "16", "20", "24", "32"), value -> value).size(260, 24))
                    .add(c.dropdownText("tooltip_max_width", () -> tr("gui.max_width"),
                            () -> Integer.toString(TooltipConfig.maxWidth), value -> TooltipConfig.maxWidth = Integer.parseInt(value),
                            Arrays.asList("0", "80", "120", "160", "200", "240", "320"),
                            value -> "0".equals(value) ? tr("gui.unlimited") : value).size(260, 24))
                    .add(cornerColorPicker(c, "tooltip_fill_ul", "gui.fill_color", 0, TooltipConfig.fillColors))
                    .add(cornerColorPicker(c, "tooltip_fill_ur", "gui.fill_color", 1, TooltipConfig.fillColors))
                    .add(cornerColorPicker(c, "tooltip_fill_lr", "gui.fill_color", 2, TooltipConfig.fillColors))
                    .add(cornerColorPicker(c, "tooltip_fill_ll", "gui.fill_color", 3, TooltipConfig.fillColors))
                    .add(cornerColorPicker(c, "tooltip_border_ul", "gui.border_color", 0, TooltipConfig.borderColors))
                    .add(cornerColorPicker(c, "tooltip_border_ur", "gui.border_color", 1, TooltipConfig.borderColors))
                    .add(cornerColorPicker(c, "tooltip_border_lr", "gui.border_color", 2, TooltipConfig.borderColors))
                    .add(cornerColorPicker(c, "tooltip_border_ll", "gui.border_color", 3, TooltipConfig.borderColors))
                    .add(colorPicker(c, "tooltip_shadow_color", "gui.shadow_color", () -> TooltipConfig.shadowColor,
                            value -> TooltipConfig.shadowColor = value))
                    .add(colorPicker(c, "tooltip_text_color", "gui.text_color", () -> TooltipConfig.textColor,
                            value -> TooltipConfig.textColor = value))
                    .add(colorPicker(c, "tooltip_title_color", "gui.title_color", () -> TooltipConfig.titleColor,
                            value -> TooltipConfig.titleColor = value));
            return new PageView(grid);
        }

        @Override public void apply() { TooltipConfig.save(); }
        @Override public void cancel() { original.restore(); }

        private static String number(float value) {
            return value == (int) value ? Integer.toString((int) value) : Float.toString(value);
        }

        private static String tr(String suffix) {
            return AddonI18n.tr("neofontrender_ui_enhancements." + suffix);
        }

        private static List<String> integerValues(int min, int max) {
            String[] values = new String[max - min + 1];
            for (int value = min; value <= max; value++) values[value - min] = Integer.toString(value);
            return Arrays.asList(values);
        }

        private static IWidget colorPicker(NfrSettingsControls controls, String name, String label,
                                           java.util.function.IntSupplier getter,
                                           java.util.function.IntConsumer setter) {
            return controls.colorText(name, () -> tr(label), getter, setter, true).size(260, 24);
        }

        private static IWidget cornerColorPicker(NfrSettingsControls controls, String name, String label,
                                                 int corner, int[] colors) {
            String[] suffixes = {"gui.corner.ul", "gui.corner.ur", "gui.corner.lr", "gui.corner.ll"};
            return controls.colorText(name, () -> tr(label) + " · " + tr(suffixes[corner]),
                    () -> colors[corner], value -> colors[corner] = value, true).size(260, 24);
        }
    }

    private static final class PageView extends NfrContentView<PageView> {
        private PageView(NfrOptionsGrid grid) { super(section(grid, grid::preferredHeight)); }
    }

    private static String modNameFormatLabel(String value) {
        String suffix = value == null || value.isEmpty() ? "none" : value.replace(' ', '_');
        return AddonI18n.tr("neofontrender_ui_enhancements.gui.mod_name_format." + suffix);
    }
}
