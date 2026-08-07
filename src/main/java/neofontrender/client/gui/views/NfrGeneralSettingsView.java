package neofontrender.client.gui.views;

import net.minecraft.client.resources.I18n;
import neofontrender.client.NeofontrenderBranding;
import neofontrender.client.gui.component.base.NfrOptionsGrid;
import neofontrender.client.gui.component.business.NfrSettingsControls;
import neofontrender.client.gui.model.NfrSettingsDraft;

/** General route: engine, shaping and font-rendering behavior. */
public final class NfrGeneralSettingsView extends NfrContentView<NfrGeneralSettingsView> {
    public NfrGeneralSettingsView(NfrSettingsDraft draft, NfrSettingsControls controls, int route) {
        this(options(draft, controls, route));
    }

    private NfrGeneralSettingsView(NfrOptionsGrid options) {
        super(section(options, options::preferredHeight));
    }

    private static NfrOptionsGrid options(NfrSettingsDraft d, NfrSettingsControls c, int route) {
        return c.grid()
                .add(c.engine(route))
                .add(c.antialias())
                .add(c.style())
                .add(c.toggleText(() -> I18n.format("neofontrender.gui.option.enabled"),
                        () -> I18n.format("neofontrender.tooltip.enabled", NeofontrenderBranding.displayName()),
                        () -> d.enabled, value -> d.enabled = value, () -> c.reload(route)))
                .add(c.toggle("options.forceUnicodeFont", "neofontrender.tooltip.force_unicode_font",
                        () -> d.forceUnicodeFont, value -> d.forceUnicodeFont = value,
                        () -> c.reload(route)))
                .add(c.toggle("neofontrender.gui.option.string_mode", "neofontrender.tooltip.string_mode",
                        () -> d.advancedStringMode, value -> d.advancedStringMode = value))
                .add(c.toggle("neofontrender.gui.option.autobase", "neofontrender.tooltip.autobase",
                        () -> d.autoBaseline, value -> d.autoBaseline = value))
                .add(c.toggle("neofontrender.gui.option.fractional", "neofontrender.tooltip.fractional",
                        () -> d.fractionalMetrics, value -> d.fractionalMetrics = value))
                .add(c.toggle("neofontrender.gui.option.builtins", "neofontrender.tooltip.builtins",
                        () -> d.builtinFallbacks, value -> d.builtinFallbacks = value));
    }
}
