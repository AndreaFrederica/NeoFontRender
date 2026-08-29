package neofontrender.client.gui.views;

import neofontrender.build.BuildFeatures;
import neofontrender.client.gui.component.base.NfrOptionsGrid;
import neofontrender.client.gui.component.business.NfrPipelineInfoPanel;
import neofontrender.client.gui.component.business.NfrSettingsControls;
import neofontrender.client.gui.component.business.NfrSdfPreview;
import neofontrender.client.gui.model.NfrSettingsDraft;

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
                BuildFeatures.RENDER_STATS && d.debugRenderStats));
    }
}
