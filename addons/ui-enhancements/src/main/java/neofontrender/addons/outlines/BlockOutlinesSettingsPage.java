package neofontrender.addons.outlines;

import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.screen.RichTooltip;
import com.cleanroommc.modularui.widget.Widget;
import com.cleanroommc.modularui.widgets.textfield.TextFieldWidget;
import neofontrender.addons.tooltips.AddonI18n;
import neofontrender.addons.ui.NfrUiEnhancements;
import neofontrender.api.client.settings.NfrSettingsPage;
import neofontrender.api.client.settings.NfrSettingsPageContext;
import neofontrender.api.client.settings.NfrSettingsPageSession;
import neofontrender.client.gui.component.base.NfrLabeledTextField;
import neofontrender.client.gui.component.base.NfrOptionsGrid;
import neofontrender.client.gui.component.base.NfrStringValue;
import neofontrender.client.gui.component.business.NfrSettingsControls;
import neofontrender.client.gui.views.NfrContentView;

import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.Arrays;

final class BlockOutlinesSettingsPage implements NfrSettingsPage {
    @Override public String id() { return NfrUiEnhancements.MOD_ID + ":block_outlines"; }
    @Override public String titleKey() { return "neofontrender_ui_enhancements.gui.outlines.category"; }
    @Override public String title() { return AddonI18n.tr(titleKey()); }
    @Override public int order() { return 1018; }
    @Override public NfrSettingsPageSession createSession() { return new Session(); }

    private static final class Session implements NfrSettingsPageSession {
        private final BlockOutlineConfig.Snapshot original = BlockOutlineConfig.snapshot();

        @Override public IWidget createView(NfrSettingsPageContext context) {
            NfrSettingsControls c = context.controls();
            NfrOptionsGrid general = c.grid()
                    .add(toggle(c, "enabled", () -> BlockOutlineConfig.enabled,
                            value -> BlockOutlineConfig.enabled = value))
                    .add(dropdown(c, "render_mode", () -> BlockOutlineConfig.renderMode, value -> {
                        BlockOutlineConfig.renderMode = BlockOutlineConfig.normalizeRenderMode(value);
                        context.refresh();
                    }, Arrays.asList(BlockOutlineConfig.MODE_GEOMETRY, BlockOutlineConfig.MODE_NATIVE)))
                    .add(c.colorText("outline_global_color", () -> tr("global_color"),
                            () -> BlockOutlineConfig.globalColor,
                            value -> BlockOutlineConfig.globalColor = value, true).size(260, 24))
                    .add(slider(c, "outline_opacity", () -> BlockOutlineConfig.outlineOpacity,
                            value -> BlockOutlineConfig.outlineOpacity = value, 0.0F, 1.0F, 0.01F))
                    .add(slider(c, "outline_brightness", () -> BlockOutlineConfig.outlineBrightness,
                            value -> BlockOutlineConfig.outlineBrightness = value, 0.0F, 4.0F, 0.05F))
                    .add(widthSlider(c, "global_width", () -> BlockOutlineConfig.globalLineWidth,
                            value -> BlockOutlineConfig.globalLineWidth = value))
                    .add(slider(c, "expansion", () -> BlockOutlineConfig.expansion,
                            value -> BlockOutlineConfig.expansion = value, 0.0F, 0.25F, 0.005F));

            NfrOptionsGrid rendering = c.grid()
                    .add(dropdown(c, "depth_mode", () -> BlockOutlineConfig.depthMode, value -> {
                        BlockOutlineConfig.depthMode = BlockOutlineConfig.normalizeDepthMode(value);
                        context.refresh();
                    }, Arrays.asList(BlockOutlineConfig.DEPTH_VISIBLE, BlockOutlineConfig.DEPTH_ALWAYS,
                            BlockOutlineConfig.DEPTH_XRAY)));
            if (BlockOutlineConfig.DEPTH_XRAY.equals(BlockOutlineConfig.depthMode)) {
                rendering.add(slider(c, "xray_hidden_opacity", () -> BlockOutlineConfig.xrayHiddenOpacity,
                        value -> BlockOutlineConfig.xrayHiddenOpacity = value, 0.0F, 1.0F, 0.05F));
            }
            rendering.add(dropdown(c, "blend_mode", () -> BlockOutlineConfig.blendMode,
                    value -> BlockOutlineConfig.blendMode = BlockOutlineConfig.normalizeBlendMode(value),
                    Arrays.asList(BlockOutlineConfig.BLEND_ALPHA, BlockOutlineConfig.BLEND_ADDITIVE)))
                    .add(toggle(c, "fill_enabled", () -> BlockOutlineConfig.fillEnabled, value -> {
                        BlockOutlineConfig.fillEnabled = value;
                        context.refresh();
                    }));
            if (BlockOutlineConfig.fillEnabled) {
                rendering.add(slider(c, "fill_opacity", () -> BlockOutlineConfig.fillOpacity,
                        value -> BlockOutlineConfig.fillOpacity = value, 0.0F, 1.0F, 0.05F));
            }
            rendering.add(toggle(c, "pulse_enabled", () -> BlockOutlineConfig.pulseEnabled, value -> {
                BlockOutlineConfig.pulseEnabled = value;
                context.refresh();
            }));
            if (BlockOutlineConfig.pulseEnabled) {
                rendering.add(slider(c, "pulse_period", () -> BlockOutlineConfig.pulsePeriodMillis,
                                value -> BlockOutlineConfig.pulsePeriodMillis = value, 250.0F, 10000.0F, 50.0F))
                        .add(slider(c, "pulse_minimum_alpha", () -> BlockOutlineConfig.pulseMinimumAlpha,
                                value -> BlockOutlineConfig.pulseMinimumAlpha = value, 0.0F, 1.0F, 0.05F));
            }

            NfrOptionsGrid geometry = null;
            if (BlockOutlineConfig.MODE_GEOMETRY.equals(BlockOutlineConfig.renderMode)) {
                geometry = c.grid()
                        .add(toggle(c, "antialias", () -> BlockOutlineConfig.antialias, value -> {
                            BlockOutlineConfig.antialias = value;
                            context.refresh();
                        }));
                if (BlockOutlineConfig.antialias) {
                    geometry.add(slider(c, "antialias_width", () -> BlockOutlineConfig.antialiasWidth,
                            value -> BlockOutlineConfig.antialiasWidth = value, 0.25F, 4.0F, 0.05F));
                }
                geometry.add(dropdown(c, "pattern", () -> BlockOutlineConfig.pattern, value -> {
                            BlockOutlineConfig.pattern = BlockOutlineConfig.normalizePattern(value);
                            context.refresh();
                        }, Arrays.asList(BlockOutlineConfig.PATTERN_SOLID, BlockOutlineConfig.PATTERN_DASHED,
                                BlockOutlineConfig.PATTERN_DOTTED)))
                        .add(dropdown(c, "cap", () -> BlockOutlineConfig.cap,
                                value -> BlockOutlineConfig.cap = BlockOutlineConfig.normalizeCap(value),
                                Arrays.asList(BlockOutlineConfig.CAP_ROUND, BlockOutlineConfig.CAP_SQUARE)));
                if (!BlockOutlineConfig.PATTERN_SOLID.equals(BlockOutlineConfig.pattern)) {
                    geometry.add(slider(c, "dash_length", () -> BlockOutlineConfig.dashLength,
                                    value -> BlockOutlineConfig.dashLength = value, 1.0F, 64.0F, 0.5F))
                            .add(slider(c, "dash_gap", () -> BlockOutlineConfig.dashGap,
                                    value -> BlockOutlineConfig.dashGap = value, 0.5F, 64.0F, 0.5F));
                }
            }
            NfrOptionsGrid normalRules = new NfrOptionsGrid(260, 42, 8, true)
                    .add(ruleField("block_overrides",
                            () -> BlockOutlineConfig.editorRules(BlockOutlineConfig.blockOverrides),
                            value -> {
                                BlockOutlineConfig.blockOverrides = BlockOutlineConfig.parseEditorRules(value);
                                BlockOutlineResolver.reload();
                            }));
            NfrOptionsGrid noHarvest = c.grid()
                    .add(toggle(c, "no_harvest", () -> BlockOutlineConfig.noHarvestEnabled,
                            value -> BlockOutlineConfig.noHarvestEnabled = value))
                    .add(c.colorText("outline_no_harvest_color", () -> tr("no_harvest_color"),
                            () -> BlockOutlineConfig.noHarvestColor,
                            value -> BlockOutlineConfig.noHarvestColor = value, true).size(260, 24))
                    .add(widthSlider(c, "no_harvest_width", () -> BlockOutlineConfig.noHarvestLineWidth,
                            value -> BlockOutlineConfig.noHarvestLineWidth = value));
            NfrOptionsGrid noHarvestRules = new NfrOptionsGrid(260, 42, 8, true)
                    .add(ruleField("no_harvest_overrides",
                            () -> BlockOutlineConfig.editorRules(BlockOutlineConfig.noHarvestOverrides),
                            value -> {
                                BlockOutlineConfig.noHarvestOverrides = BlockOutlineConfig.parseEditorRules(value);
                                BlockOutlineResolver.reload();
                            }));
            return new PageView(general, rendering, geometry, normalRules, noHarvest, noHarvestRules);
        }

        @Override public void preview() { BlockOutlineResolver.reload(); }
        @Override public void apply() { BlockOutlineConfig.save(); }
        @Override public void cancel() { original.restore(); }
    }

    private static IWidget toggle(NfrSettingsControls controls, String key, Supplier<Boolean> getter,
                                  Consumer<Boolean> setter) {
        return controls.toggleText(() -> tr(key), () -> tooltip(key), getter, setter);
    }

    private static IWidget widthSlider(NfrSettingsControls controls, String key, Supplier<Float> getter,
                                       Consumer<Float> setter) {
        boolean nativeMode = BlockOutlineConfig.MODE_NATIVE.equals(BlockOutlineConfig.renderMode);
        float minimum = nativeMode ? BlockOutlineRenderer.nativeMinimumWidth() : 0.5F;
        float maximum = nativeMode ? BlockOutlineRenderer.nativeMaximumWidth() : 64.0F;
        String modeKey = nativeMode ? key + "_native" : key + "_pixels";
        return slider(controls, modeKey, getter, setter, minimum, Math.max(minimum, maximum), 0.5F);
    }

    private static IWidget slider(NfrSettingsControls controls, String key, Supplier<Float> getter,
                                  Consumer<Float> setter, float minimum, float maximum, float step) {
        return tooltipWidget(controls.decimalSlider(() -> tr(key), getter, setter,
                minimum, maximum, step), key);
    }

    private static IWidget dropdown(NfrSettingsControls controls, String key, Supplier<String> getter,
                                    Consumer<String> setter, Iterable<String> values) {
        return tooltipWidget(controls.dropdownText("outline_" + key, () -> tr(key), getter, setter,
                values, value -> tr(key + "." + value)).size(260, 24), key);
    }

    private static IWidget tooltipWidget(IWidget widget, String key) {
        if (widget instanceof Widget<?>) {
            ((Widget<?>) widget).tooltip(new RichTooltip().showUpTimer(8).addLine(tooltip(key)));
        }
        return widget;
    }

    private static NfrLabeledTextField ruleField(String key, Supplier<String> getter,
                                                 Consumer<String> setter) {
        TextFieldWidget editor = new TextFieldWidget().setMaxLength(8192)
                .value(new NfrStringValue(getter, setter));
        return new NfrLabeledTextField(tr(key), editor)
                .tooltip(new RichTooltip().showUpTimer(8).addLine(tooltip("rule_format")));
    }

    private static String tr(String key) {
        return AddonI18n.tr("neofontrender_ui_enhancements.gui.outlines." + key);
    }

    private static String tooltip(String key) {
        return AddonI18n.tr("neofontrender_ui_enhancements.tooltip.outlines." + key);
    }

    private static final class PageView extends NfrContentView<PageView> {
        private PageView(NfrOptionsGrid general, NfrOptionsGrid rendering, NfrOptionsGrid geometry,
                         NfrOptionsGrid normalRules, NfrOptionsGrid noHarvest,
                         NfrOptionsGrid noHarvestRules) {
            super(sections(general, rendering, geometry, normalRules, noHarvest, noHarvestRules));
        }

        private static NfrContentView.Section[] sections(NfrOptionsGrid... grids) {
            int count = 0;
            for (NfrOptionsGrid grid : grids) if (grid != null) count++;
            NfrContentView.Section[] result = new NfrContentView.Section[count];
            int index = 0;
            for (NfrOptionsGrid grid : grids) {
                if (grid != null) result[index++] = section(grid, grid::preferredHeight);
            }
            return result;
        }
    }
}
