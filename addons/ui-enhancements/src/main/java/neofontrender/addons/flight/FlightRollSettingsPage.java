package neofontrender.addons.flight;

import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.screen.RichTooltip;
import neofontrender.addons.tooltips.AddonI18n;
import neofontrender.addons.ui.NfrUiEnhancements;
import neofontrender.api.client.settings.NfrSettingsPage;
import neofontrender.api.client.settings.NfrSettingsPageContext;
import neofontrender.api.client.settings.NfrSettingsPageSession;
import neofontrender.client.gui.component.base.NfrDecimalSlider;
import neofontrender.client.gui.component.base.NfrDoubleValue;
import neofontrender.client.gui.component.base.NfrOptionsGrid;
import neofontrender.client.gui.component.business.NfrSettingsControls;
import neofontrender.client.gui.views.NfrContentView;

import java.util.Arrays;
import java.util.Locale;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleSupplier;
import java.util.function.Supplier;

final class FlightRollSettingsPage implements NfrSettingsPage {
    @Override public String id() { return NfrUiEnhancements.MOD_ID + ":flight_roll"; }
    @Override public String titleKey() { return "neofontrender_ui_enhancements.gui.flight_roll.category"; }
    @Override public String title() { return AddonI18n.tr(titleKey()); }
    @Override public int order() { return 1016; }
    @Override public NfrSettingsPageSession createSession() { return new Session(); }

    private static final class Session implements NfrSettingsPageSession {
        private final boolean originalEnabled = FlightRollConfig.enabled;
        private final boolean originalMomentum = FlightRollConfig.momentumMouse;
        private final float originalRollSensitivity = FlightRollConfig.rollSensitivity;
        private final float originalPitchSensitivity = FlightRollConfig.pitchSensitivity;
        private final float originalMaximumSpeed = FlightRollConfig.maximumRollSpeed;
        private final int originalDeadzone = FlightRollConfig.momentumDeadzonePercent;
        private final boolean originalInvertPitch = FlightRollConfig.invertPitch;
        private final boolean originalInvertYaw = FlightRollConfig.invertYaw;
        private final boolean originalInvertRoll = FlightRollConfig.invertRoll;
        private final float originalControllerPitch = FlightRollConfig.controllerPitchSensitivity;
        private final float originalControllerYaw = FlightRollConfig.controllerYawSensitivity;
        private final float originalControllerRoll = FlightRollConfig.controllerRollSensitivity;
        private final boolean originalBarrels = FlightRollConfig.barrelRolls;
        private final int originalDuration = FlightRollConfig.barrelDurationTicks;
        private final boolean originalRemote = FlightRollConfig.remotePlayerRoll;
        private final boolean originalHud = FlightRollConfig.flightHud;
        private final String originalHudTheme = FlightRollConfig.hudTheme;
        private final String originalSpeedUnit = FlightRollConfig.hudSpeedUnit;
        private final String originalAltitudeUnit = FlightRollConfig.hudAltitudeUnit;
        private final String originalVerticalSpeedUnit = FlightRollConfig.hudVerticalSpeedUnit;
        private final boolean originalHorizon = FlightRollConfig.hudHorizon;
        private final boolean originalInputIndicator = FlightRollConfig.hudInputIndicator;
        private final int originalHudScale = FlightRollConfig.hudScalePercent;
        private final boolean originalHideVanillaCrosshair =
                CrosshairConfig.hideVanillaDuringFlightHud;
        private final boolean originalHideForgeCrosshair =
                CrosshairConfig.hideForgeLayerDuringFlightHud;
        private final boolean originalHideHotbar = FlightRollConfig.hudHideHotbar;
        private final boolean originalHidePlayerStatus = FlightRollConfig.hudHidePlayerStatus;
        private final boolean originalHideExperience = FlightRollConfig.hudHideExperience;
        private final boolean originalHideChat = FlightRollConfig.hudHideChat;
        private final boolean originalHideBossBars = FlightRollConfig.hudHideBossBars;
        private final boolean originalHidePotionIcons = FlightRollConfig.hudHidePotionIcons;
        private final boolean originalHideSubtitles = FlightRollConfig.hudHideSubtitles;
        private final boolean originalHidePlayerList = FlightRollConfig.hudHidePlayerList;
        private final boolean originalHideText = FlightRollConfig.hudHideText;
        private final boolean originalHideFirstPersonHand =
                FlightRollConfig.hudHideFirstPersonHand;

        @Override public IWidget createView(NfrSettingsPageContext context) {
            NfrSettingsControls c = context.controls();
            NfrOptionsGrid grid = c.grid()
                    .add(c.toggleText(() -> tr("gui.flight_roll.enabled"),
                            () -> tr("tooltip.flight_roll.enabled"),
                            () -> FlightRollConfig.enabled, value -> FlightRollConfig.enabled = value))
                    .add(c.toggleText(() -> tr("gui.flight_roll.momentum"),
                            () -> tr("tooltip.flight_roll.momentum"),
                            () -> FlightRollConfig.momentumMouse,
                            value -> FlightRollConfig.momentumMouse = value))
                    .add(multiplierSlider("gui.flight_roll.roll_sensitivity",
                            () -> FlightRollConfig.rollSensitivity,
                            value -> FlightRollConfig.rollSensitivity = (float) value))
                    .add(multiplierSlider("gui.flight_roll.pitch_sensitivity",
                            () -> FlightRollConfig.pitchSensitivity,
                            value -> FlightRollConfig.pitchSensitivity = (float) value))
                    .add(slider("gui.flight_roll.maximum_speed",
                            () -> FlightRollConfig.maximumRollSpeed,
                            value -> FlightRollConfig.maximumRollSpeed = (float) value,
                            30.0D, 720.0D, 1.0D,
                            () -> Math.round(FlightRollConfig.maximumRollSpeed) + "°/s", null))
                    .add(slider("gui.flight_roll.deadzone",
                            () -> FlightRollConfig.momentumDeadzonePercent,
                            value -> FlightRollConfig.momentumDeadzonePercent = (int) Math.round(value),
                            0.0D, 30.0D, 1.0D,
                            () -> FlightRollConfig.momentumDeadzonePercent + "%", null))
                    .add(c.toggleText(() -> tr("gui.flight_roll.invert_pitch"),
                            () -> tr("tooltip.flight_roll.invert"),
                            () -> FlightRollConfig.invertPitch,
                            value -> FlightRollConfig.invertPitch = value))
                    .add(c.toggleText(() -> tr("gui.flight_roll.invert_yaw"),
                            () -> tr("tooltip.flight_roll.invert"),
                            () -> FlightRollConfig.invertYaw,
                            value -> FlightRollConfig.invertYaw = value))
                    .add(c.toggleText(() -> tr("gui.flight_roll.invert_roll"),
                            () -> tr("tooltip.flight_roll.invert"),
                            () -> FlightRollConfig.invertRoll,
                            value -> FlightRollConfig.invertRoll = value))
                    .add(controllerSlider("gui.flight_roll.controller_pitch_sensitivity",
                            () -> FlightRollConfig.controllerPitchSensitivity,
                            value -> FlightRollConfig.controllerPitchSensitivity = (float) value))
                    .add(controllerSlider("gui.flight_roll.controller_yaw_sensitivity",
                            () -> FlightRollConfig.controllerYawSensitivity,
                            value -> FlightRollConfig.controllerYawSensitivity = (float) value))
                    .add(controllerSlider("gui.flight_roll.controller_roll_sensitivity",
                            () -> FlightRollConfig.controllerRollSensitivity,
                            value -> FlightRollConfig.controllerRollSensitivity = (float) value))
                    .add(c.toggleText(() -> tr("gui.flight_roll.barrels"),
                            () -> tr("tooltip.flight_roll.barrels"),
                            () -> FlightRollConfig.barrelRolls, value -> FlightRollConfig.barrelRolls = value))
                    .add(slider("gui.flight_roll.duration",
                            () -> FlightRollConfig.barrelDurationTicks,
                            value -> FlightRollConfig.barrelDurationTicks = (int) Math.round(value),
                            6.0D, 40.0D, 1.0D,
                            () -> FlightRollConfig.barrelDurationTicks + " ticks", null))
                    .add(c.toggleText(() -> tr("gui.flight_roll.remote"),
                            () -> tr("tooltip.flight_roll.remote"),
                            () -> FlightRollConfig.remotePlayerRoll,
                            value -> FlightRollConfig.remotePlayerRoll = value))
                    .add(c.toggleText(() -> tr("gui.flight_roll.hud"),
                            () -> tr("tooltip.flight_roll.hud"),
                            () -> FlightRollConfig.flightHud,
                            value -> FlightRollConfig.flightHud = value))
                    .add(c.dropdownText("flight_hud_theme",
                            () -> tr("gui.flight_roll.hud_theme"),
                            () -> FlightRollConfig.hudTheme,
                            value -> FlightRollConfig.hudTheme = value,
                            FlightHudThemeManager.INSTANCE.themeIds(),
                            FlightHudThemeManager.INSTANCE::displayName).size(260, 24))
                    .add(c.action("neofontrender_ui_enhancements.gui.flight_roll.hud_reload",
                            260, 24, FlightHudThemeManager.INSTANCE::reloadNow))
                    .add(c.dropdownText("flight_hud_speed_unit",
                            () -> tr("gui.flight_roll.hud_speed_unit"),
                            () -> FlightRollConfig.hudSpeedUnit,
                            value -> FlightRollConfig.hudSpeedUnit = value,
                            Arrays.asList("KNOTS", "KPH", "MPS", "BPS"),
                            value -> tr("gui.flight_roll.unit." + value)).size(260, 24))
                    .add(c.dropdownText("flight_hud_altitude_unit",
                            () -> tr("gui.flight_roll.hud_altitude_unit"),
                            () -> FlightRollConfig.hudAltitudeUnit,
                            value -> FlightRollConfig.hudAltitudeUnit = value,
                            Arrays.asList("FEET", "METERS", "BLOCKS"),
                            value -> tr("gui.flight_roll.unit." + value)).size(260, 24))
                    .add(c.dropdownText("flight_hud_vertical_speed_unit",
                            () -> tr("gui.flight_roll.hud_vertical_speed_unit"),
                            () -> FlightRollConfig.hudVerticalSpeedUnit,
                            value -> FlightRollConfig.hudVerticalSpeedUnit = value,
                            Arrays.asList("FPM", "MPS", "BPS"),
                            value -> tr("gui.flight_roll.unit." + value)).size(260, 24))
                    .add(c.toggleText(() -> tr("gui.flight_roll.hud_horizon"),
                            () -> tr("tooltip.flight_roll.hud_horizon"),
                            () -> FlightRollConfig.hudHorizon,
                            value -> FlightRollConfig.hudHorizon = value))
                    .add(c.toggleText(() -> tr("gui.flight_roll.hud_input"),
                            () -> tr("tooltip.flight_roll.hud_input"),
                            () -> FlightRollConfig.hudInputIndicator,
                            value -> FlightRollConfig.hudInputIndicator = value))
                    .add(slider("gui.flight_roll.hud_scale",
                            () -> FlightRollConfig.hudScalePercent,
                            value -> FlightRollConfig.hudScalePercent = (int) Math.round(value),
                            50.0D, 100.0D, 1.0D,
                            () -> FlightRollConfig.hudScalePercent + "%",
                            "tooltip.flight_roll.hud_scale"))
                    .add(hudVisibilityToggle(c, "hud_hide_crosshair",
                            "tooltip.crosshair.hide_flight",
                            () -> CrosshairConfig.hideVanillaDuringFlightHud,
                            value -> CrosshairConfig.hideVanillaDuringFlightHud = value))
                    .add(hudVisibilityToggle(c, "hud_hide_mod_crosshairs",
                            "tooltip.crosshair.hide_forge_flight",
                            () -> CrosshairConfig.hideForgeLayerDuringFlightHud,
                            value -> CrosshairConfig.hideForgeLayerDuringFlightHud = value))
                    .add(hudVisibilityToggle(c, "hud_hide_hotbar",
                            () -> FlightRollConfig.hudHideHotbar,
                            value -> FlightRollConfig.hudHideHotbar = value))
                    .add(hudVisibilityToggle(c, "hud_hide_player_status",
                            () -> FlightRollConfig.hudHidePlayerStatus,
                            value -> FlightRollConfig.hudHidePlayerStatus = value))
                    .add(hudVisibilityToggle(c, "hud_hide_experience",
                            () -> FlightRollConfig.hudHideExperience,
                            value -> FlightRollConfig.hudHideExperience = value))
                    .add(hudVisibilityToggle(c, "hud_hide_chat",
                            () -> FlightRollConfig.hudHideChat,
                            value -> FlightRollConfig.hudHideChat = value))
                    .add(hudVisibilityToggle(c, "hud_hide_boss_bars",
                            () -> FlightRollConfig.hudHideBossBars,
                            value -> FlightRollConfig.hudHideBossBars = value))
                    .add(hudVisibilityToggle(c, "hud_hide_potion_icons",
                            () -> FlightRollConfig.hudHidePotionIcons,
                            value -> FlightRollConfig.hudHidePotionIcons = value))
                    .add(hudVisibilityToggle(c, "hud_hide_subtitles",
                            () -> FlightRollConfig.hudHideSubtitles,
                            value -> FlightRollConfig.hudHideSubtitles = value))
                    .add(hudVisibilityToggle(c, "hud_hide_player_list",
                            () -> FlightRollConfig.hudHidePlayerList,
                            value -> FlightRollConfig.hudHidePlayerList = value))
                    .add(hudVisibilityToggle(c, "hud_hide_text",
                            () -> FlightRollConfig.hudHideText,
                            value -> FlightRollConfig.hudHideText = value))
                    .add(hudVisibilityToggle(c, "hud_hide_first_person_hand",
                            "tooltip.flight_roll.hud_hide_first_person_hand",
                            () -> FlightRollConfig.hudHideFirstPersonHand,
                            value -> FlightRollConfig.hudHideFirstPersonHand = value));
            return new PageView(grid);
        }

        @Override public void apply() {
            FlightRollConfig.save();
            CrosshairConfig.save();
        }

        @Override public void cancel() {
            FlightRollConfig.enabled = originalEnabled;
            FlightRollConfig.momentumMouse = originalMomentum;
            FlightRollConfig.rollSensitivity = originalRollSensitivity;
            FlightRollConfig.pitchSensitivity = originalPitchSensitivity;
            FlightRollConfig.maximumRollSpeed = originalMaximumSpeed;
            FlightRollConfig.momentumDeadzonePercent = originalDeadzone;
            FlightRollConfig.invertPitch = originalInvertPitch;
            FlightRollConfig.invertYaw = originalInvertYaw;
            FlightRollConfig.invertRoll = originalInvertRoll;
            FlightRollConfig.controllerPitchSensitivity = originalControllerPitch;
            FlightRollConfig.controllerYawSensitivity = originalControllerYaw;
            FlightRollConfig.controllerRollSensitivity = originalControllerRoll;
            FlightRollConfig.barrelRolls = originalBarrels;
            FlightRollConfig.barrelDurationTicks = originalDuration;
            FlightRollConfig.remotePlayerRoll = originalRemote;
            FlightRollConfig.flightHud = originalHud;
            FlightRollConfig.hudTheme = originalHudTheme;
            FlightRollConfig.hudSpeedUnit = originalSpeedUnit;
            FlightRollConfig.hudAltitudeUnit = originalAltitudeUnit;
            FlightRollConfig.hudVerticalSpeedUnit = originalVerticalSpeedUnit;
            FlightRollConfig.hudHorizon = originalHorizon;
            FlightRollConfig.hudInputIndicator = originalInputIndicator;
            FlightRollConfig.hudScalePercent = originalHudScale;
            CrosshairConfig.hideVanillaDuringFlightHud = originalHideVanillaCrosshair;
            CrosshairConfig.hideForgeLayerDuringFlightHud = originalHideForgeCrosshair;
            FlightRollConfig.hudHideHotbar = originalHideHotbar;
            FlightRollConfig.hudHidePlayerStatus = originalHidePlayerStatus;
            FlightRollConfig.hudHideExperience = originalHideExperience;
            FlightRollConfig.hudHideChat = originalHideChat;
            FlightRollConfig.hudHideBossBars = originalHideBossBars;
            FlightRollConfig.hudHidePotionIcons = originalHidePotionIcons;
            FlightRollConfig.hudHideSubtitles = originalHideSubtitles;
            FlightRollConfig.hudHidePlayerList = originalHidePlayerList;
            FlightRollConfig.hudHideText = originalHideText;
            FlightRollConfig.hudHideFirstPersonHand = originalHideFirstPersonHand;
        }
    }

    private static IWidget hudVisibilityToggle(NfrSettingsControls controls, String key,
                                               Supplier<Boolean> getter,
                                               java.util.function.Consumer<Boolean> setter) {
        return hudVisibilityToggle(controls, key, "tooltip.flight_roll.hud_visibility",
                getter, setter);
    }

    private static IWidget hudVisibilityToggle(NfrSettingsControls controls, String key,
                                               String tooltipKey, Supplier<Boolean> getter,
                                               java.util.function.Consumer<Boolean> setter) {
        return controls.toggleText(() -> tr("gui.flight_roll." + key),
                () -> tr(tooltipKey),
                getter, setter);
    }

    private static IWidget multiplierSlider(String key, DoubleSupplier getter, DoubleConsumer setter) {
        return slider(key, getter, setter, 0.1D, 4.0D, 0.01D,
                () -> String.format(Locale.ROOT, "%.2fx", getter.getAsDouble()), null);
    }

    private static IWidget controllerSlider(String key, DoubleSupplier getter, DoubleConsumer setter) {
        return slider(key, getter, setter, 0.1D, 4.0D, 0.01D,
                () -> String.format(Locale.ROOT, "%.2fx", getter.getAsDouble()),
                "tooltip.flight_roll.controller");
    }

    private static IWidget slider(String key, DoubleSupplier getter, DoubleConsumer setter,
                                  double min, double max, double step,
                                  Supplier<String> display, String tooltipKey) {
        NfrDecimalSlider slider = new NfrDecimalSlider(() -> tr(key), display);
        slider.value(new NfrDoubleValue(getter::getAsDouble,
                value -> setter.accept(Math.max(min, Math.min(max,
                        Math.round(value / step) * step)))));
        slider.bounds(min, max);
        if (tooltipKey != null) {
            slider.tooltip(new RichTooltip().showUpTimer(8).addLine(tr(tooltipKey)));
        }
        return slider.size(260, 24);
    }

    private static String tr(String key) {
        return AddonI18n.tr("neofontrender_ui_enhancements." + key);
    }

    private static final class PageView extends NfrContentView<PageView> {
        private PageView(NfrOptionsGrid grid) { super(section(grid, grid::preferredHeight)); }
    }
}
