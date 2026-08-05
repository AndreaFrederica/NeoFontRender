package neofontrender.addons.flight;

import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.screen.RichTooltip;
import net.minecraftforge.fml.common.Loader;
import neofontrender.addons.tooltips.AddonI18n;
import neofontrender.addons.ui.NfrUiEnhancements;
import neofontrender.api.client.settings.NfrSettingsPageContext;
import neofontrender.api.client.settings.NfrSettingsSection;
import neofontrender.api.client.settings.NfrSettingsSectionContribution;
import neofontrender.api.client.settings.NfrSettingsSectionRegistry;
import neofontrender.api.client.settings.NfrSettingsSectionSession;
import neofontrender.client.gui.component.base.NfrOptionDropdown;

import java.util.Arrays;
import java.util.List;

/** Adds the optional Shoulder Surfing repair to NFR's built-in Fixes page. */
final class ShoulderSurfingFixSettings {
    private ShoulderSurfingFixSettings() {}

    static void register() {
        if (!Loader.isModLoaded("shouldersurfing")) return;
        NfrSettingsSectionRegistry.register(new NfrSettingsSectionContribution() {
            @Override public String id() {
                return NfrUiEnhancements.MOD_ID + ":shoulder_surfing_crosshair_fix";
            }
            @Override public NfrSettingsSection section() { return NfrSettingsSection.FIXES; }
            @Override public int order() { return 1100; }
            @Override public NfrSettingsSectionSession createSession() { return new Session(); }
        });
    }

    private static final class Session implements NfrSettingsSectionSession {
        private final String original = ShoulderSurfingFixConfig.mode();

        @Override public List<IWidget> createControls(NfrSettingsPageContext context) {
            NfrOptionDropdown dropdown = context.controls().dropdownText(
                    "shoulder_surfing_crosshair_mode",
                    () -> tr("gui.fixes.shoulder_surfing_crosshair"),
                    ShoulderSurfingFixConfig::mode,
                    ShoulderSurfingFixConfig::setMode,
                    Arrays.asList(ShoulderSurfingFixConfig.MODE_PATCHED,
                            ShoulderSurfingFixConfig.MODE_ADAPTIVE,
                            ShoulderSurfingFixConfig.MODE_STATIC,
                            ShoulderSurfingFixConfig.MODE_DUAL,
                            ShoulderSurfingFixConfig.MODE_OFF),
                    value -> tr("gui.fixes.shoulder_surfing_crosshair." + value));
            dropdown.tooltip(new RichTooltip().showUpTimer(8)
                    .addLine(tr("tooltip.fixes.shoulder_surfing_crosshair")));
            dropdown.size(260, 24);
            return java.util.Collections.singletonList(dropdown);
        }

        @Override public void apply() { ShoulderSurfingFixConfig.save(); }
        @Override public void cancel() { ShoulderSurfingFixConfig.setMode(original); }
    }

    private static String tr(String suffix) {
        return AddonI18n.tr("neofontrender_ui_enhancements." + suffix);
    }
}
