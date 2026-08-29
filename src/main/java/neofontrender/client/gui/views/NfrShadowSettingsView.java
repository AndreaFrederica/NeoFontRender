package neofontrender.client.gui.views;

import com.cleanroommc.modularui.widgets.textfield.TextFieldWidget;
import net.minecraft.client.resources.I18n;
import neofontrender.client.gui.component.base.NfrLabeledTextField;
import neofontrender.client.gui.component.base.NfrOptionsGrid;
import neofontrender.client.gui.component.base.NfrStringValue;
import neofontrender.client.gui.component.business.NfrSettingsControls;
import neofontrender.client.gui.component.business.NfrShadowPreview;
import neofontrender.client.gui.model.NfrSettingsDraft;

/** Shadow route. */
public final class NfrShadowSettingsView extends NfrContentView<NfrShadowSettingsView> {
    public NfrShadowSettingsView(NfrSettingsControls controls) {
        this(controls.draft(), options(controls), remapRules(controls.draft()));
    }

    private NfrShadowSettingsView(NfrSettingsDraft draft, NfrOptionsGrid options,
                                  NfrLabeledTextField remapRules) {
        super(section(new NfrShadowPreview(draft), width -> 174),
                section(options, options::preferredHeight),
                section(remapRules, width -> 42));
    }

    private static NfrOptionsGrid options(NfrSettingsControls c) {
        neofontrender.client.gui.model.NfrSettingsDraft d = c.draft();
        Runnable draftOnly = () -> { };
        return c.grid()
                .add(c.shadowMode(draftOnly))
                .add(c.toggle("neofontrender.gui.option.shadow_modern",
                        "neofontrender.tooltip.shadow_modern",
                        () -> d.modernShadow, value -> d.modernShadow = value, draftOnly))
                .add(c.shadowColorMode(draftOnly))
                .add(c.shadowColoredFunction(draftOnly))
                .add(c.decimalSlider("neofontrender.gui.option.shadow_colored_ratio",
                        () -> d.shadowColoredRatio, value -> d.shadowColoredRatio = value,
                        0.0F, 1.0F, 0.01F, draftOnly))
                .add(c.decimalSlider("neofontrender.gui.option.shadow_offset_x",
                        () -> d.shadowOffsetX, value -> d.shadowOffsetX = value,
                        -8.0F, 8.0F, 0.1F, draftOnly))
                .add(c.decimalSlider("neofontrender.gui.option.shadow_offset_y",
                        () -> d.shadowOffsetY, value -> d.shadowOffsetY = value,
                        -8.0F, 8.0F, 0.1F, draftOnly))
                .add(c.decimalSlider("neofontrender.gui.option.shadow_blur",
                        () -> d.shadowBlurRadius, value -> d.shadowBlurRadius = value,
                        0.0F, 6.0F, 0.1F, draftOnly))
                .add(c.decimalSlider("neofontrender.gui.option.shadow_opacity",
                        () -> d.shadowOpacity, value -> d.shadowOpacity = value,
                        0.0F, 1.0F, 0.01F, draftOnly))
                .add(c.shadowColor(draftOnly));
    }

    private static NfrLabeledTextField remapRules(NfrSettingsDraft draft) {
        return new NfrLabeledTextField(
                I18n.format("neofontrender.gui.option.shadow_color_overrides"),
                new TextFieldWidget().setMaxLength(2048)
                        .value(new NfrStringValue(() -> draft.shadowColorOverrides,
                                value -> draft.shadowColorOverrides = value)));
    }
}
