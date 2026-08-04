package neofontrender.client.gui.views;

import com.cleanroommc.modularui.widgets.textfield.TextFieldWidget;
import net.minecraft.client.resources.I18n;
import neofontrender.api.color.TextColorPaletteRegistry;
import neofontrender.client.gui.component.base.NfrLabeledTextField;
import neofontrender.client.gui.component.base.NfrOptionsGrid;
import neofontrender.client.gui.component.base.NfrStringValue;
import neofontrender.client.gui.component.business.NfrSettingsControls;
import neofontrender.client.gui.model.NfrSettingsDraft;

import java.util.Arrays;

/** Compatibility fixes for third-party mods. */
public final class NfrCompatibilitySettingsView extends NfrContentView<NfrCompatibilitySettingsView> {
    public NfrCompatibilitySettingsView(NfrSettingsDraft d, NfrSettingsControls c) { this(options(d, c)); }

    private NfrCompatibilitySettingsView(NfrOptionsGrid options) {
        super(section(options, options::preferredHeight));
    }

    private static NfrOptionsGrid options(NfrSettingsDraft d, NfrSettingsControls c) {
        return c.grid()
                .add(c.dropdown("text_color_palette_provider",
                        "neofontrender.gui.option.text_color_palette_provider",
                        () -> d.textColorPaletteProvider,
                        value -> d.textColorPaletteProvider = value,
                        TextColorPaletteRegistry.providerIds(),
                        NfrCompatibilitySettingsView::paletteProviderName).size(260, 24))
                .add(new NfrLabeledTextField(
                        I18n.format("neofontrender.gui.option.custom_text_color_palette"),
                        new TextFieldWidget().setMaxLength(512)
                                .value(new NfrStringValue(() -> d.customTextColorPalette,
                                        value -> d.customTextColorPalette = value))))
                .add(c.toggle("neofontrender.gui.option.compat_tinkers_antique",
                        "neofontrender.tooltip.compat_tinkers_antique",
                        () -> d.compatTinkersAntique, value -> d.compatTinkersAntique = value))
                .add(c.dropdown("enchantment_backend", "neofontrender.gui.option.enchantment_backend",
                        () -> d.enchantmentBackend, value -> d.enchantmentBackend = value,
                        Arrays.asList("awt", "cosmic", "auto", "vanilla"),
                        value -> net.minecraft.client.resources.I18n.format(
                                "neofontrender.gui.enchantment_backend." + value)).size(260, 24))
                .add(new NfrLabeledTextField(
                        net.minecraft.client.resources.I18n.format("neofontrender.gui.option.enchantment_fonts"),
                        new TextFieldWidget().setMaxLength(1024)
                                .value(new NfrStringValue(() -> d.enchantmentFonts,
                                        value -> d.enchantmentFonts = value))));
    }

    private static String paletteProviderName(String value) {
        String key = "neofontrender.gui.color_palette_provider." + value;
        String translated = I18n.format(key);
        return key.equals(translated) ? TextColorPaletteRegistry.displayName(value) : translated;
    }
}
