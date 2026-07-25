package neofontrender.client.gui.views;

import neofontrender.client.gui.component.base.NfrLabeledSlider;
import neofontrender.client.gui.component.base.NfrOptionsGrid;
import neofontrender.client.gui.component.business.NfrSettingsControls;
import neofontrender.client.gui.model.NfrSettingsDraft;

/** Rendering route: sampling and brightness. */
public final class NfrRenderingSettingsView extends NfrContentView<NfrRenderingSettingsView> {
    public NfrRenderingSettingsView(NfrSettingsDraft d, NfrSettingsControls c) {
        this(options(d, c), c.brightness());
    }

    private NfrRenderingSettingsView(NfrOptionsGrid options, NfrLabeledSlider brightness) {
        super(section(options, options::preferredHeight), section(brightness, width -> brightness.preferredHeight()));
    }

    private static NfrOptionsGrid options(NfrSettingsDraft d, NfrSettingsControls c) {
        return c.grid()
                .add(c.toggle("neofontrender.gui.option.linear", "neofontrender.tooltip.linear",
                        () -> d.interpolation, value -> d.interpolation = value))
                .add(c.toggle("neofontrender.gui.option.mipmap", "neofontrender.tooltip.mipmap",
                        () -> d.mipmap, value -> d.mipmap = value));
    }
}
