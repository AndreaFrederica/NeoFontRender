package neofontrender.client.gui.views;

import neofontrender.client.gui.component.base.NfrOptionsGrid;
import neofontrender.client.gui.component.business.NfrPipelineInfoPanel;
import neofontrender.client.gui.component.business.NfrSettingsControls;
import neofontrender.client.gui.model.NfrSettingsDraft;

/** Advanced pipeline route with a live pipeline summary. */
public final class NfrAdvancedSettingsView extends NfrContentView<NfrAdvancedSettingsView> {
    public NfrAdvancedSettingsView(NfrSettingsDraft d, NfrSettingsControls c, boolean skiaAvailable) {
        this(pipeline(d, skiaAvailable), options(d, c));
    }

    private NfrAdvancedSettingsView(NfrPipelineInfoPanel pipeline, NfrOptionsGrid options) {
        super(section(pipeline, width -> pipeline.preferredHeight()), section(options, options::preferredHeight));
    }

    private static NfrOptionsGrid options(NfrSettingsDraft d, NfrSettingsControls c) {
        return c.grid()
                .add(c.toggle("neofontrender.gui.option.pipeline", "neofontrender.tooltip.pipeline",
                        () -> d.enhancedTextPipeline, value -> d.enhancedTextPipeline = value))
                .add(c.toggle("neofontrender.gui.option.shader", "neofontrender.tooltip.shader",
                        () -> d.shaderTextPipeline, value -> d.shaderTextPipeline = value))
                .add(c.toggle("neofontrender.gui.option.edge_bleed", "neofontrender.tooltip.edge_bleed",
                        () -> d.textureEdgeBleed, value -> d.textureEdgeBleed = value))
                .add(c.toggle("neofontrender.gui.option.debug_stats", "neofontrender.tooltip.debug_stats",
                        () -> d.debugRenderStats, value -> d.debugRenderStats = value));
    }

    private static NfrPipelineInfoPanel pipeline(NfrSettingsDraft d, boolean skiaAvailable) {
        return new NfrPipelineInfoPanel(() -> new NfrPipelineInfoPanel.Snapshot(
                NfrSettingsControls.engineName(d.engine, skiaAvailable), d.oversample,
                d.enhancedTextPipeline, d.shaderTextPipeline, d.textureEdgeBleed,
                d.adaptiveRasterScale, d.interpolation, d.mipmap,
                d.excludeIntegerScale, d.excludeHighMagnification, d.anisotropicFiltering,
                d.skiaGpuOffscreen, d.skiaGpuSubmitViaCpuTexture, d.debugRenderStats));
    }
}
