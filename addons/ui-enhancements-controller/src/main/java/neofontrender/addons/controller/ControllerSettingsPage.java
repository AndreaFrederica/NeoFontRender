package neofontrender.addons.controller;

import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.screen.RichTooltip;
import net.minecraft.client.resources.I18n;
import neofontrender.api.client.settings.NfrSettingsPage;
import neofontrender.api.client.settings.NfrSettingsPageContext;
import neofontrender.api.client.settings.NfrSettingsPageSession;
import neofontrender.client.gui.component.base.NfrDecimalSlider;
import neofontrender.client.gui.component.base.NfrDoubleValue;
import neofontrender.client.gui.component.base.NfrOptionsGrid;
import neofontrender.client.gui.component.business.NfrSettingsControls;
import neofontrender.client.gui.views.NfrContentView;

import java.util.Locale;
import java.util.List;
import java.util.Map;
import net.minecraft.util.ResourceLocation;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

final class ControllerSettingsPage implements NfrSettingsPage {
    private static final String PREFIX = ControllerAddonMod.MOD_ID + ".";

    @Override public String id() { return ControllerAddonMod.MOD_ID + ":controller"; }
    @Override public String titleKey() { return PREFIX + "gui.category"; }
    @Override public int order() { return 1100; }
    @Override public NfrSettingsPageSession createSession() { return new Session(); }

    private static final class Session implements NfrSettingsPageSession {
        private final float originalDeadzone = ControllerConfig.deadzone();
        private final float originalLookSensitivity = ControllerConfig.lookSensitivity();
        private final float originalFlightSensitivity = ControllerConfig.flightSensitivity();
        private final float originalCursorSensitivity = ControllerConfig.cursorSensitivity();
        private final float originalCursorBaseSpeed = ControllerConfig.cursorBaseSpeed();
        private final float originalCursorMaxSpeed = ControllerConfig.cursorMaxSpeed();
        private final float originalCursorAcceleration = ControllerConfig.cursorAcceleration();
        private final float originalCursorSmoothing = ControllerConfig.cursorSmoothing();
        private final boolean originalInvertLookX = ControllerConfig.invertLookX();
        private final boolean originalInvertLookY = ControllerConfig.invertLookY();
        private final boolean originalInvertFlightPitch = ControllerConfig.invertFlightPitch();
        private final boolean originalInvertFlightYaw = ControllerConfig.invertFlightYaw();
        private final boolean originalInvertFlightRoll = ControllerConfig.invertFlightRoll();
        private final boolean originalVibrationEnabled = ControllerConfig.vibrationEnabled();
        private final boolean originalSlotSnapping = ControllerConfig.slotSnapping();
        private final List<ControllerBindingSpec> originalBindings = ControllerBindings.snapshot();
        private final Map<String, ControllerKeyBindingAssignment> originalGameBindings =
                ControllerForgeBindings.snapshot();

        @Override
        public IWidget createView(NfrSettingsPageContext context) {
            NfrSettingsControls controls = context.controls();
            NfrOptionsGrid generalGrid = controls.grid()
                    .add(slider("deadzone", ControllerConfig::deadzone,
                            ControllerConfig::setDeadzone, 0.0F, 0.5F, 0.01F, true))
                    .add(toggle(controls, "vibration", ControllerConfig::vibrationEnabled,
                            ControllerConfig::setVibrationEnabled));
            NfrOptionsGrid cameraGrid = controls.grid()
                    .add(slider("look_sensitivity", ControllerConfig::lookSensitivity,
                            ControllerConfig::setLookSensitivity, 0.1F, 4.0F, 0.05F, false))
                    .add(toggle(controls, "invert_look_x", ControllerConfig::invertLookX,
                            ControllerConfig::setInvertLookX))
                    .add(toggle(controls, "invert_look_y", ControllerConfig::invertLookY,
                            ControllerConfig::setInvertLookY));
            NfrOptionsGrid guiGrid = controls.grid()
                    .add(slider("cursor_sensitivity", ControllerConfig::cursorSensitivity,
                            ControllerConfig::setCursorSensitivity, 0.25F, 3.0F, 0.05F, false))
                    .add(slider("cursor_base_speed", ControllerConfig::cursorBaseSpeed,
                            ControllerConfig::setCursorBaseSpeed, 20.0F, 300.0F, 5.0F, "px/s"))
                    .add(slider("cursor_max_speed", ControllerConfig::cursorMaxSpeed,
                            ControllerConfig::setCursorMaxSpeed, 60.0F, 720.0F, 10.0F, "px/s"))
                    .add(slider("cursor_acceleration", ControllerConfig::cursorAcceleration,
                            ControllerConfig::setCursorAcceleration, 0.0F, 2_000.0F, 25.0F, "px/s^2"))
                    .add(slider("cursor_smoothing", ControllerConfig::cursorSmoothing,
                            ControllerConfig::setCursorSmoothing, 0.0F, 1.0F, 0.05F, true))
                    .add(toggle(controls, "slot_snapping", ControllerConfig::slotSnapping,
                            ControllerConfig::setSlotSnapping));
            NfrOptionsGrid flightGrid = controls.grid()
                    .add(slider("flight_sensitivity", ControllerConfig::flightSensitivity,
                            ControllerConfig::setFlightSensitivity, 0.1F, 4.0F, 0.05F, false))
                    .add(toggle(controls, "invert_flight_pitch",
                            ControllerConfig::invertFlightPitch,
                            ControllerConfig::setInvertFlightPitch))
                    .add(toggle(controls, "invert_flight_yaw",
                            ControllerConfig::invertFlightYaw,
                            ControllerConfig::setInvertFlightYaw))
                    .add(toggle(controls, "invert_flight_roll",
                            ControllerConfig::invertFlightRoll,
                            ControllerConfig::setInvertFlightRoll));
            ControllerOptionsPanel general = new ControllerOptionsPanel(
                    tr("gui.settings_group_general"), generalGrid);
            ControllerOptionsPanel camera = new ControllerOptionsPanel(
                    tr("gui.settings_group_camera"), cameraGrid);
            ControllerOptionsPanel gui = new ControllerOptionsPanel(
                    tr("gui.settings_group_gui"), guiGrid);
            ControllerOptionsPanel flight = new ControllerOptionsPanel(
                    tr("gui.settings_group_flight"), flightGrid);
            ControllerWorkbenchModel model = new ControllerWorkbenchModel();
            ControllerTestPanel test = new ControllerTestPanel(model);
            ControllerBindingsPanel playerBindings = new ControllerBindingsPanel(
                    model, ControllerBindingGroup.PLAYER);
            ControllerBindingsPanel guiBindings = new ControllerBindingsPanel(
                    model, ControllerBindingGroup.GUI);
            ControllerBindingsPanel cameraBindings = new ControllerBindingsPanel(
                    model, ControllerBindingGroup.CAMERA);
            ControllerBindingsPanel flightBindings = new ControllerBindingsPanel(
                    model, ControllerBindingGroup.FLIGHT);
            ControllerForgeBindingsPanel gameBindings = new ControllerForgeBindingsPanel(model);
            return new ControllerWorkbenchView(model, general, gui, camera, flight, test,
                    playerBindings, guiBindings, cameraBindings, flightBindings, gameBindings,
                    general::preferredHeight, gui::preferredHeight,
                    camera::preferredHeight, flight::preferredHeight);
        }

        @Override public void apply() { ControllerConfig.save(); }

        @Override
        public void cancel() {
            ControllerConfig.setDeadzone(originalDeadzone);
            ControllerConfig.setLookSensitivity(originalLookSensitivity);
            ControllerConfig.setFlightSensitivity(originalFlightSensitivity);
            ControllerConfig.setCursorSensitivity(originalCursorSensitivity);
            ControllerConfig.setCursorBaseSpeed(originalCursorBaseSpeed);
            ControllerConfig.setCursorMaxSpeed(originalCursorMaxSpeed);
            ControllerConfig.setCursorAcceleration(originalCursorAcceleration);
            ControllerConfig.setCursorSmoothing(originalCursorSmoothing);
            ControllerConfig.setInvertLookX(originalInvertLookX);
            ControllerConfig.setInvertLookY(originalInvertLookY);
            ControllerConfig.setInvertFlightPitch(originalInvertFlightPitch);
            ControllerConfig.setInvertFlightYaw(originalInvertFlightYaw);
            ControllerConfig.setInvertFlightRoll(originalInvertFlightRoll);
            ControllerConfig.setVibrationEnabled(originalVibrationEnabled);
            ControllerConfig.setSlotSnapping(originalSlotSnapping);
            ControllerBindings.restore(originalBindings);
            ControllerForgeBindings.restore(originalGameBindings);
        }
    }

    private static IWidget toggle(NfrSettingsControls controls, String key,
                                  Supplier<Boolean> getter, Consumer<Boolean> setter) {
        return controls.toggleText(() -> tr("gui." + key), () -> tr("tooltip." + key),
                getter, setter);
    }

    private static IWidget slider(String key, Supplier<Float> getter, Consumer<Float> setter,
                                  float min, float max, float step, boolean percentage) {
        return slider(key, getter, setter, min, max, step, value -> percentage
                ? Math.round(value * 100.0F) + "%"
                : String.format(Locale.ROOT, "%.2fx", value));
    }

    private static IWidget slider(String key, Supplier<Float> getter, Consumer<Float> setter,
                                  float min, float max, float step, String unit) {
        return slider(key, getter, setter, min, max, step,
                value -> Math.round(value) + " " + unit);
    }

    private static IWidget slider(String key, Supplier<Float> getter, Consumer<Float> setter,
                                  float min, float max, float step,
                                  Function<Float, String> formatter) {
        NfrDecimalSlider slider = new NfrDecimalSlider(
                () -> tr("gui." + key), () -> formatter.apply(getter.get()));
        slider.value(new NfrDoubleValue(() -> (double) getter.get(), value -> {
                    double clipped = Math.max(min, Math.min(max, value));
                    setter.accept((float) (Math.round(clipped / step) * step));
                }))
                .bounds(min, max);
        slider.tooltip(new RichTooltip().showUpTimer(8).addLine(tr("tooltip." + key)));
        return slider.size(260, 24);
    }

    private static String tr(String key) { return I18n.format(PREFIX + key); }

}
