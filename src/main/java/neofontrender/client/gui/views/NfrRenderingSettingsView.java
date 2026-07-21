package neofontrender.client.gui.views;

import neofontrender.client.gui.component.base.NfrLabeledSlider;
import neofontrender.client.gui.component.base.NfrOptionsGrid;
import neofontrender.client.gui.component.business.NfrSettingsControls;
import neofontrender.client.gui.model.NfrSettingsDraft;

/** Rendering route: sampling, GPU path and brightness. */
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
                        () -> d.mipmap, value -> d.mipmap = value))
                .add(c.toggle("neofontrender.gui.option.gpu_offscreen", "neofontrender.tooltip.gpu_offscreen",
                        () -> d.skiaGpuOffscreen, value -> d.skiaGpuOffscreen = value))
                .add(c.toggle("neofontrender.gui.option.gpu_cpu_submit", "neofontrender.tooltip.gpu_cpu_submit",
                        () -> d.skiaGpuSubmitViaCpuTexture, value -> {
                            d.skiaGpuSubmitViaCpuTexture = value;
                            if (value) d.skiaGpuOffscreen = true;
                        }))
                .add(c.toggle("neofontrender.gui.option.monochrome_text", "neofontrender.tooltip.monochrome_text",
                        () -> d.skiaMonochromeText, value -> d.skiaMonochromeText = value))
                .add(c.toggle("neofontrender.gui.option.premultiplied_alpha", "neofontrender.tooltip.premultiplied_alpha",
                        () -> d.premultipliedAlpha, value -> d.premultipliedAlpha = value));
    }
}
