package neofontrender.client.gui.views;

import neofontrender.client.gui.component.base.NfrOptionsGrid;
import neofontrender.client.gui.component.business.NfrSettingsControls;
import neofontrender.client.gui.model.NfrSettingsDraft;

/** Performance route: raster scaling, filtering and sign batching. */
public final class NfrPerformanceSettingsView extends NfrContentView<NfrPerformanceSettingsView> {
    public NfrPerformanceSettingsView(NfrSettingsDraft d, NfrSettingsControls c) {
        this(options(d, c));
    }

    private NfrPerformanceSettingsView(NfrOptionsGrid options) {
        super(section(options, options::preferredHeight));
    }

    private static NfrOptionsGrid options(NfrSettingsDraft d, NfrSettingsControls c) {
        return c.grid()
                .add(c.toggle("neofontrender.gui.option.autoscale", "neofontrender.tooltip.autoscale",
                        () -> d.adaptiveRasterScale, value -> d.adaptiveRasterScale = value))
                .add(c.toggle("neofontrender.gui.option.integer_scale", "neofontrender.tooltip.integer_scale",
                        () -> d.excludeIntegerScale, value -> d.excludeIntegerScale = value))
                .add(c.toggle("neofontrender.gui.option.high_mag", "neofontrender.tooltip.high_mag",
                        () -> d.excludeHighMagnification, value -> d.excludeHighMagnification = value))
                .add(c.toggle("neofontrender.gui.option.anisotropic", "neofontrender.tooltip.anisotropic",
                        () -> d.anisotropicFiltering, value -> d.anisotropicFiltering = value))
                .add(c.toggle("neofontrender.gui.option.sign_model_lod", "neofontrender.tooltip.sign_model_lod",
                        () -> d.signModelLod, value -> d.signModelLod = value))
                .add(c.toggle("neofontrender.gui.option.sign_cross_batch", "neofontrender.tooltip.sign_cross_batch",
                        () -> d.signCrossTileBatching, value -> d.signCrossTileBatching = value))
                .add(c.toggle("neofontrender.gui.option.sign_occlusion", "neofontrender.tooltip.sign_occlusion",
                        () -> d.signBlockOcclusionCulling, value -> d.signBlockOcclusionCulling = value));
    }
}
