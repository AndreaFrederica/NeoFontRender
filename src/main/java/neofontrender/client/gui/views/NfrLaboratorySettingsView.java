package neofontrender.client.gui.views;

import com.cleanroommc.modularui.api.widget.IWidget;
import neofontrender.client.gui.component.base.NfrOptionsGrid;
import neofontrender.client.gui.component.business.NfrSettingsControls;
import neofontrender.client.gui.model.NfrSettingsDraft;

/** Experimental settings route. */
public final class NfrLaboratorySettingsView extends NfrContentView<NfrLaboratorySettingsView> {
    public NfrLaboratorySettingsView(NfrSettingsDraft d, NfrSettingsControls c,
                                     Iterable<IWidget> contributedControls) {
        this(options(d, c, contributedControls));
    }

    private NfrLaboratorySettingsView(NfrOptionsGrid options) {
        super(section(options, options::preferredHeight));
    }

    private static NfrOptionsGrid options(NfrSettingsDraft d, NfrSettingsControls c,
                                          Iterable<IWidget> contributedControls) {
        NfrOptionsGrid grid = c.grid()
                .add(c.toggle("neofontrender.gui.option.hex_chat", "neofontrender.tooltip.hex_chat",
                        () -> d.laboratoryHexChat, value -> d.laboratoryHexChat = value))
                .add(c.toggle("neofontrender.gui.option.hex_chat_reset_styles",
                        "neofontrender.tooltip.hex_chat_reset_styles",
                        () -> d.laboratoryHexChatResetStyles,
                        value -> d.laboratoryHexChatResetStyles = value))
                .add(c.toggle("neofontrender.gui.option.text_undo_redo", "neofontrender.tooltip.text_undo_redo",
                        () -> d.laboratoryTextUndoRedo, value -> d.laboratoryTextUndoRedo = value))
                .add(c.toggle("neofontrender.gui.option.brilliant", "neofontrender.tooltip.brilliant",
                        () -> d.brilliantTextEnabled, value -> d.brilliantTextEnabled = value))
                .add(c.toggle("neofontrender.gui.option.brilliant_any_position",
                        "neofontrender.tooltip.brilliant_any_position",
                        () -> d.laboratoryBrilliantAnyPosition,
                        value -> d.laboratoryBrilliantAnyPosition = value))
                .add(c.toggle("neofontrender.gui.option.textanimator_enabled",
                        "neofontrender.tooltip.textanimator_enabled",
                        () -> d.laboratoryTextAnimatorEnabled,
                        value -> d.laboratoryTextAnimatorEnabled = value))
                .add(c.toggle("neofontrender.gui.option.textanimator_any_position",
                        "neofontrender.tooltip.textanimator_any_position",
                        () -> d.laboratoryTextAnimatorAnyPosition,
                        value -> d.laboratoryTextAnimatorAnyPosition = value))
                .add(c.dropdown("textanimator_effects", "neofontrender.gui.option.textanimator_effects",
                        () -> d.laboratoryTextAnimatorEffects,
                        value -> d.laboratoryTextAnimatorEffects = value,
                        java.util.Arrays.asList("all", "no_rainbow", "none"),
                        value -> net.minecraft.client.resources.I18n.format(
                                "neofontrender.gui.textanimator.effects." + value)))
                .add(c.dropdown("textanimator_typewriter_mode", "neofontrender.gui.option.textanimator_typewriter_mode",
                        () -> d.laboratoryTextAnimatorTypewriterMode,
                        value -> d.laboratoryTextAnimatorTypewriterMode = value,
                        java.util.Arrays.asList("by_char", "by_word"),
                        value -> net.minecraft.client.resources.I18n.format(
                                "neofontrender.gui.textanimator.mode." + value)))
                .add(c.dropdown("textanimator_typewriter_speed", "neofontrender.gui.option.textanimator_typewriter_speed",
                        () -> d.laboratoryTextAnimatorTypewriterSpeed,
                        value -> d.laboratoryTextAnimatorTypewriterSpeed = value,
                        java.util.Arrays.asList("1", "2", "3", "4", "5", "6", "7", "8", "9"),
                        value -> value))
                .add(c.dropdown("textanimator_pulse_minimum", "neofontrender.gui.option.textanimator_pulse_minimum",
                        () -> d.laboratoryTextAnimatorPulseMinimum,
                        value -> d.laboratoryTextAnimatorPulseMinimum = value,
                        java.util.Arrays.asList("0.0", "0.1", "0.2", "0.3", "0.4", "0.5", "0.6", "0.7", "0.8", "0.9", "1.0"),
                        value -> value))
                .add(c.dropdown("textanimator_pulse_maximum", "neofontrender.gui.option.textanimator_pulse_maximum",
                        () -> d.laboratoryTextAnimatorPulseMaximum,
                        value -> d.laboratoryTextAnimatorPulseMaximum = value,
                        java.util.Arrays.asList("0.0", "0.1", "0.2", "0.3", "0.4", "0.5", "0.6", "0.7", "0.8", "0.9", "1.0"),
                        value -> value))
                .add(c.toggle("neofontrender.gui.option.splash_override", "neofontrender.tooltip.splash_override",
                        () -> d.splashFontOverride, value -> d.splashFontOverride = value))
                .add(c.toggle("neofontrender.gui.option.modern_splash", "neofontrender.tooltip.modern_splash",
                        () -> d.compatModernSplash, value -> d.compatModernSplash = value));
        if (contributedControls != null) {
            for (IWidget widget : contributedControls) {
                if (widget != null) grid.add(widget);
            }
        }
        return grid;
    }
}
