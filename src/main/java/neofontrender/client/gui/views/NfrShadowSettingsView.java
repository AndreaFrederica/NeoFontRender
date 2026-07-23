package neofontrender.client.gui.views;

import neofontrender.client.gui.component.base.NfrOptionsGrid;
import neofontrender.client.gui.component.business.NfrSettingsControls;

/** Shadow route. */
public final class NfrShadowSettingsView extends NfrContentView<NfrShadowSettingsView> {
    public NfrShadowSettingsView(NfrSettingsControls controls) {
        this(options(controls));
    }

    private NfrShadowSettingsView(NfrOptionsGrid options) {
        super(section(options, options::preferredHeight));
    }

    private static NfrOptionsGrid options(NfrSettingsControls c) {
        neofontrender.client.gui.model.NfrSettingsDraft d = c.draft();
        return c.grid()
                .add(c.shadowMode())
                .add(c.toggle("neofontrender.gui.option.shadow_modern",
                        "neofontrender.tooltip.shadow_modern",
                        () -> d.modernShadow, value -> d.modernShadow = value))
                .add(c.decimalSlider("neofontrender.gui.option.shadow_offset_x",
                        () -> d.shadowOffsetX, value -> d.shadowOffsetX = value, -8.0F, 8.0F, 0.1F))
                .add(c.decimalSlider("neofontrender.gui.option.shadow_offset_y",
                        () -> d.shadowOffsetY, value -> d.shadowOffsetY = value, -8.0F, 8.0F, 0.1F))
                .add(c.decimalSlider("neofontrender.gui.option.shadow_blur",
                        () -> d.shadowBlurRadius, value -> d.shadowBlurRadius = value, 0.0F, 6.0F, 0.1F))
                .add(c.decimalSlider("neofontrender.gui.option.shadow_opacity",
                        () -> d.shadowOpacity, value -> d.shadowOpacity = value, 0.0F, 1.0F, 0.01F))
                .add(c.shadowColor());
    }
}
