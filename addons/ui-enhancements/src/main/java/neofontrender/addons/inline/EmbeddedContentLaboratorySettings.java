package neofontrender.addons.inline;

import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.widgets.textfield.TextFieldWidget;
import neofontrender.api.text.pipeline.TextPipelineEngine;
import neofontrender.addons.tooltips.AddonI18n;
import neofontrender.addons.ui.NfrUiEnhancements;
import neofontrender.api.client.settings.NfrSettingsPageContext;
import neofontrender.api.client.settings.NfrSettingsSection;
import neofontrender.api.client.settings.NfrSettingsSectionContribution;
import neofontrender.api.client.settings.NfrSettingsSectionRegistry;
import neofontrender.api.client.settings.NfrSettingsSectionSession;
import neofontrender.client.gui.component.base.NfrLabeledTextField;
import neofontrender.client.gui.component.base.NfrStringValue;

import java.util.ArrayList;
import java.util.List;

/** Places optional embedded-content providers beside NFR's other experimental switches. */
public final class EmbeddedContentLaboratorySettings {
    private EmbeddedContentLaboratorySettings() {}

    public static void register() {
        NfrSettingsSectionRegistry.register(new NfrSettingsSectionContribution() {
            @Override public String id() { return NfrUiEnhancements.MOD_ID + ":embedded_content"; }
            @Override public NfrSettingsSection section() { return NfrSettingsSection.LABORATORY; }
            @Override public int order() { return 1000; }
            @Override public NfrSettingsSectionSession createSession() { return new Session(); }
        });
    }

    private static final class Session implements NfrSettingsSectionSession {
        private final boolean latex = EmbeddedContentConfig.latexEnabled;
        private final boolean svg = EmbeddedContentConfig.svgEnabled;
        private final boolean fullSvg = EmbeddedContentConfig.fullSvgEnabled;
        private final boolean markdown = EmbeddedContentConfig.markdownEnabled;
        private final boolean latexMatchLineHeight = EmbeddedContentConfig.latexMatchLineHeight;
        private final String latexFontFamily = EmbeddedContentConfig.latexFontFamily;
        private final float latexOversample = EmbeddedContentConfig.latexOversample;
        private final int rasterCacheEntries = EmbeddedContentConfig.rasterCacheEntries;
        private final float rasterCacheMegapixels = EmbeddedContentConfig.rasterCacheMegapixels;

        @Override
        public List<IWidget> createControls(NfrSettingsPageContext context) {
            List<IWidget> controls = new ArrayList<>();
            controls.add(context.controls().toggleText(
                    () -> tr("gui.laboratory.latex"),
                    () -> tr("tooltip.laboratory.latex"),
                    () -> EmbeddedContentConfig.latexEnabled,
                    value -> EmbeddedContentConfig.latexEnabled = value));
            controls.add(context.controls().toggleText(
                    () -> tr("gui.laboratory.svg"),
                    () -> tr("tooltip.laboratory.svg"),
                    () -> EmbeddedContentConfig.svgEnabled,
                    value -> EmbeddedContentConfig.svgEnabled = value));
            controls.add(context.controls().toggleText(
                    () -> tr("gui.laboratory.full_svg"),
                    () -> tr("tooltip.laboratory.full_svg"),
                    () -> EmbeddedContentConfig.fullSvgEnabled(),
                    value -> EmbeddedContentConfig.fullSvgEnabled = value));
            controls.add(context.controls().toggleText(
                    () -> tr("gui.laboratory.markdown"),
                    () -> tr("tooltip.laboratory.markdown"),
                    () -> EmbeddedContentConfig.markdownEnabled,
                    value -> EmbeddedContentConfig.markdownEnabled = value));
            controls.add(context.controls().toggleText(
                    () -> tr("gui.laboratory.latex_match_line_height"),
                    () -> tr("tooltip.laboratory.latex_match_line_height"),
                    () -> EmbeddedContentConfig.latexMatchLineHeight,
                    value -> EmbeddedContentConfig.latexMatchLineHeight = value));
            controls.add(new NfrLabeledTextField(
                    tr("gui.laboratory.latex_font_family"),
                    new TextFieldWidget().setMaxLength(256).value(new NfrStringValue(
                            () -> EmbeddedContentConfig.latexFontFamily,
                            value -> EmbeddedContentConfig.latexFontFamily = value == null ? "" : value.trim()))));
            controls.add(context.controls().decimalSlider(
                    () -> tr("gui.laboratory.latex_oversample"),
                    () -> EmbeddedContentConfig.latexOversample,
                    value -> EmbeddedContentConfig.latexOversample = EmbeddedContentConfig.clampOversample(value),
                    1.0F, 8.0F, 0.5F));
            controls.add(context.controls().decimalSlider(
                    () -> tr("gui.laboratory.raster_cache_entries"),
                    () -> (float) EmbeddedContentConfig.rasterCacheEntries,
                    value -> EmbeddedContentConfig.rasterCacheEntries =
                            EmbeddedContentConfig.clampCacheEntries(Math.round(value)),
                    16.0F, 1024.0F, 16.0F));
            controls.add(context.controls().decimalSlider(
                    () -> tr("gui.laboratory.raster_cache_megapixels"),
                    () -> EmbeddedContentConfig.rasterCacheMegapixels,
                    value -> EmbeddedContentConfig.rasterCacheMegapixels =
                            EmbeddedContentConfig.clampCacheMegapixels(value),
                    4.0F, 128.0F, 1.0F));
            return controls;
        }

        @Override public void preview() { TextPipelineEngine.invalidate(); }

        @Override public void apply() {
            if (!EmbeddedContentConfig.svgEnabled) EmbeddedContentConfig.fullSvgEnabled = false;
            EmbeddedContentConfig.save();
            RasterGlyphService.INSTANCE.trimToConfiguredBudget();
            TextPipelineEngine.invalidate();
        }

        @Override public void cancel() {
            EmbeddedContentConfig.latexEnabled = latex;
            EmbeddedContentConfig.svgEnabled = svg;
            EmbeddedContentConfig.fullSvgEnabled = fullSvg;
            EmbeddedContentConfig.markdownEnabled = markdown;
            EmbeddedContentConfig.latexMatchLineHeight = latexMatchLineHeight;
            EmbeddedContentConfig.latexFontFamily = latexFontFamily;
            EmbeddedContentConfig.latexOversample = latexOversample;
            EmbeddedContentConfig.rasterCacheEntries = rasterCacheEntries;
            EmbeddedContentConfig.rasterCacheMegapixels = rasterCacheMegapixels;
            TextPipelineEngine.invalidate();
        }
    }

    private static String tr(String suffix) {
        return AddonI18n.tr("neofontrender_ui_enhancements." + suffix);
    }
}
