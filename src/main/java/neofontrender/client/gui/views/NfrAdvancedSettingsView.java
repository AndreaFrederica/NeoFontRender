package neofontrender.client.gui.views;

import neofontrender.build.BuildFeatures;
import neofontrender.api.text.gl.TextGlComponentApi;
import neofontrender.api.text.postprocess.TextPostProcessApi;
import neofontrender.client.gui.component.base.NfrOptionsGrid;
import neofontrender.client.gui.component.business.NfrPipelineInfoPanel;
import neofontrender.client.gui.component.business.NfrSettingsControls;
import neofontrender.client.gui.component.business.NfrSdfPreview;
import neofontrender.client.gui.model.NfrSettingsDraft;
import neofontrender.core.font.support.FontRenderTuning;

/** Advanced pipeline route with a live pipeline summary. */
public final class NfrAdvancedSettingsView extends NfrContentView<NfrAdvancedSettingsView> {
    public NfrAdvancedSettingsView(NfrSettingsDraft d, NfrSettingsControls c) {
        this(pipeline(d), options(d, c), new NfrSdfPreview(d));
    }

    private NfrAdvancedSettingsView(NfrPipelineInfoPanel pipeline, NfrOptionsGrid options,
                                    NfrSdfPreview sdfPreview) {
        super(section(pipeline, width -> pipeline.preferredHeight()),
                section(sdfPreview, width -> sdfPreview.preferredHeight()),
                section(options, options::preferredHeight));
    }

    private static NfrOptionsGrid options(NfrSettingsDraft d, NfrSettingsControls c) {
        Runnable draftOnly = () -> { };
        NfrOptionsGrid options = c.grid()
                .add(c.toggle("neofontrender.gui.option.pipeline", "neofontrender.tooltip.pipeline",
                        () -> d.enhancedTextPipeline, value -> d.enhancedTextPipeline = value))
                .add(c.toggle("neofontrender.gui.option.shader", "neofontrender.tooltip.shader",
                        () -> d.shaderTextPipeline, value -> d.shaderTextPipeline = value))
                .add(c.toggle("neofontrender.gui.option.vanilla_formatting", "neofontrender.tooltip.vanilla_formatting",
                        () -> d.vanillaFormattingCompatibility,
                        value -> d.vanillaFormattingCompatibility = value))
                .add(c.toggle("neofontrender.gui.option.sdf", "neofontrender.tooltip.sdf",
                        () -> d.sdfEnabled, value -> d.sdfEnabled = value, draftOnly))
                .add(c.sdfDistanceRange(draftOnly))
                .add(c.sdfEdgeSoftness(draftOnly));
        if (BuildFeatures.RENDER_STATS) {
            options.add(c.toggle("neofontrender.gui.option.debug_stats", "neofontrender.tooltip.debug_stats",
                    () -> d.debugRenderStats, value -> d.debugRenderStats = value));
        }
        return options;
    }

    private static NfrPipelineInfoPanel pipeline(NfrSettingsDraft d) {
        return new NfrPipelineInfoPanel(() -> new NfrPipelineInfoPanel.Snapshot(
                NfrSettingsControls.engineName(d.engine), d.oversample,
                d.enhancedTextPipeline, d.shaderTextPipeline,
                d.adaptiveRasterScale, d.interpolation, d.mipmap,
                d.excludeIntegerScale, d.excludeHighMagnification, d.anisotropicFiltering,
                BuildFeatures.RENDER_STATS && d.debugRenderStats,
                rawMiddlewareStatuses(), syntaxProviderStatuses(), structuredStatus(d),
                TextPostProcessApi.processorIds(), postProcessorStatuses(),
                glComponentStatuses(), glAvailable(), FontRenderTuning.currentDrawContext()));
    }

    private static java.util.List<String> rawMiddlewareStatuses() {
        java.util.List<String> result = new java.util.ArrayList<>();
        for (String id : neofontrender.core.font.pipeline.StructuredTextRuntime.middlewareIds()) {
            result.add(id + ":enabled");
        }
        return result;
    }

    private static java.util.List<String> syntaxProviderStatuses() {
        java.util.List<String> result = new java.util.ArrayList<>();
        for (String id : neofontrender.core.font.pipeline.StructuredTextRuntime.providerIds()) {
            result.add(id + ":enabled");
        }
        return result;
    }

    private static String structuredStatus(NfrSettingsDraft draft) {
        neofontrender.core.font.pipeline.StructuredTextRuntime.Diagnostics diagnostics =
                neofontrender.core.font.pipeline.StructuredTextRuntime.lastDiagnostics();
        return "styles=" + diagnostics.styleSpanCount + ",effects="
                + diagnostics.effectSpanCount + ",inline=" + diagnostics.inlineSpanCount
                + (diagnostics.appliedMiddlewareIds.isEmpty() ? "" : ",middleware="
                + String.join("+", diagnostics.appliedMiddlewareIds))
                + (diagnostics.animated ? ",animated" : "")
                + (diagnostics.unresolvedCount == 0 ? "" : ",unresolved=" + diagnostics.unresolvedCount)
                + ",cjk=" + (draft.fixCjkLineBreak ? "on" : "off");
    }

    private static java.util.List<String> postProcessorStatuses() {
        java.util.List<String> result = new java.util.ArrayList<>();
        for (TextPostProcessApi.ProcessorInfo info : TextPostProcessApi.processorInfos()) {
            result.add(info.id + ":" + (info.enabled ? "enabled" : "disabled"));
        }
        return result;
    }

    private static java.util.List<String> glComponentStatuses() {
        java.util.List<String> result = new java.util.ArrayList<>();
        for (TextGlComponentApi.ComponentInfo info : TextGlComponentApi.componentInfos()) {
            result.add(info.id + ":" + (info.available ? "available" : "unavailable"));
        }
        return result;
    }

    private static boolean glAvailable() {
        for (TextGlComponentApi.ComponentInfo info : TextGlComponentApi.componentInfos()) {
            if (info.available) return true;
        }
        return false;
    }
}
