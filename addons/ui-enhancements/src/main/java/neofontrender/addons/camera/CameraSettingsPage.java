package neofontrender.addons.camera;

import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.widgets.textfield.TextFieldWidget;
import neofontrender.addons.tooltips.AddonI18n;
import neofontrender.addons.ui.NfrUiEnhancements;
import neofontrender.api.client.settings.NfrSettingsPage;
import neofontrender.api.client.settings.NfrSettingsPageContext;
import neofontrender.api.client.settings.NfrSettingsPageSession;
import neofontrender.client.gui.component.base.NfrOptionsGrid;
import neofontrender.client.gui.component.base.NfrLabeledTextField;
import neofontrender.client.gui.component.base.NfrStringValue;
import neofontrender.client.gui.component.business.NfrSettingsControls;
import neofontrender.client.gui.views.NfrContentView;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;

/** Unified UI for F5 camera modes, free-look, Drone safety and Shoulder presentation. */
final class CameraSettingsPage implements NfrSettingsPage {
    @Override public String id() { return NfrUiEnhancements.MOD_ID + ":camera"; }
    @Override public String titleKey() { return "neofontrender_ui_enhancements.gui.camera.category"; }
    @Override public String title() { return AddonI18n.tr(titleKey()); }
    @Override public int order() { return 1017; }
    @Override public NfrSettingsPageSession createSession() { return new Session(); }

    private static final class Session implements NfrSettingsPageSession {
        private final boolean f5 = CameraPerspectiveConfig.f5CycleEnabled;
        private final boolean f5Shoulder = CameraPerspectiveConfig.shoulderInF5;
        private final boolean f5Free = CameraPerspectiveConfig.freeLookInF5;
        private final boolean f5Cursor = CameraPerspectiveConfig.cursorLookInF5;
        private final boolean f5Drone = CameraPerspectiveConfig.droneInF5;
        private final boolean replaceThird = CameraPerspectiveConfig.replaceDefaultPerspective;
        private final boolean skipFront = CameraPerspectiveConfig.skipThirdPersonFront;
        private final boolean rememberLast = CameraPerspectiveConfig.rememberLastPerspective;
        private final String defaultMode = CameraPerspectiveConfig.defaultMode;
        private final boolean freeToggle = FreeLookConfig.toggleMode;
        private final double pitchLimit = FreeLookConfig.pitchLimitDegrees;
        private final double response = FreeLookConfig.orientationResponse;
        private final double mouseResponse = FreeLookConfig.mouseResponse;
        private final double freeLookRollSpeed = FreeLookConfig.rollSpeedDegrees;
        private final double freeLookDistance = FreeLookConfig.distance;
        private final boolean freeLookCollision = FreeLookConfig.collision;
        private final double cursorLookSpeed = CursorLookConfig.speed;
        private final double cursorLookAimDistance = CursorLookConfig.aimDistance;
        private final boolean cursorLookShoulderOffset = CursorLookConfig.useShoulderOffset;
        private final boolean cursorLookHeadOnlyAim = CursorLookConfig.headOnlyAim;
        private final boolean cursorLookCameraRelativeMovement = CursorLookConfig.cameraRelativeMovement;
        private final boolean droneCollision = DroneCameraConfig.collision;
        private final boolean droneInteraction = DroneCameraConfig.allowCameraInteraction;
        private final double droneSpeed = DroneCameraConfig.speed;
        private final double droneResponse = DroneCameraConfig.translationResponse;
        private final double droneSensitivity = DroneCameraConfig.lookSensitivity;
        private final double droneRollSpeed = DroneCameraConfig.rollSpeedDegrees;
        private final double offsetX = ShoulderCameraConfig.offsetX;
        private final double offsetY = ShoulderCameraConfig.offsetY;
        private final double offsetZ = ShoulderCameraConfig.offsetZ;
        private final boolean shoulderCollision = ShoulderCameraConfig.collision;
        private final boolean climbing = ShoulderCameraConfig.centerWhenClimbing;
        private final double downAngle = ShoulderCameraConfig.centerWhenLookingDownDegrees;
        private final String crosshair = ShoulderCameraConfig.crosshairMode;
        private final String crosshairType = ShoulderCameraConfig.crosshairType;
        private final java.util.Map<Integer, String> visibility = new java.util.HashMap<>(ShoulderCameraConfig.crosshairVisibility);
        private final boolean transparency = ShoulderCameraConfig.playerTransparency;
        private final int transparencyPercent = ShoulderCameraConfig.playerTransparencyPercent;
        private final double transition = ShoulderCameraConfig.transitionSpeed;
        private final boolean dynamicOffsets = ShoulderCameraConfig.dynamicallyAdjustOffsets;
        private final boolean limitReach = ShoulderCameraConfig.limitPlayerReach;
        private final boolean customRay = ShoulderCameraConfig.useCustomRaytraceDistance;
        private final double rayDistance = ShoulderCameraConfig.customRaytraceDistance;
        private final double hideAngle = ShoulderCameraConfig.hidePlayerWhenLookingUpAngle;
        private final boolean hidePlayer = ShoulderCameraConfig.hidePlayerWhenLookingUp;
        private final double minX = ShoulderCameraConfig.minOffsetX, minY = ShoulderCameraConfig.minOffsetY,
                minZ = ShoulderCameraConfig.minOffsetZ, maxX = ShoulderCameraConfig.maxOffsetX,
                maxY = ShoulderCameraConfig.maxOffsetY, maxZ = ShoulderCameraConfig.maxOffsetZ;
        private final boolean unlimitedX = ShoulderCameraConfig.unlimitedOffsetX,
                unlimitedY = ShoulderCameraConfig.unlimitedOffsetY,
                unlimitedZ = ShoulderCameraConfig.unlimitedOffsetZ;
        private final double cameraStep = ShoulderCameraConfig.cameraStepSize;
        private final double keepOutOfHead = ShoulderCameraConfig.keepCameraOutOfHeadMultiplier;
        private final boolean valkyrienCollision = ShoulderCameraConfig.valkyrienShipCollision;
        private final List<String> adaptiveHoldItems = new ArrayList<>(ShoulderCameraConfig.adaptiveHoldItems);
        private final List<String> adaptiveUseItems = new ArrayList<>(ShoulderCameraConfig.adaptiveUseItems);
        private final List<String> adaptiveHoldProperties = new ArrayList<>(ShoulderCameraConfig.adaptiveHoldProperties);
        private final List<String> adaptiveUseProperties = new ArrayList<>(ShoulderCameraConfig.adaptiveUseProperties);
        private final double sprintX = ShoulderCameraConfig.sprintXMultiplier,
                sprintY = ShoulderCameraConfig.sprintYMultiplier, sprintZ = ShoulderCameraConfig.sprintZMultiplier,
                passengerX = ShoulderCameraConfig.passengerXMultiplier,
                passengerY = ShoulderCameraConfig.passengerYMultiplier,
                passengerZ = ShoulderCameraConfig.passengerZMultiplier;

        @Override public IWidget createView(NfrSettingsPageContext context) {
            NfrSettingsControls c = context.controls();
            NfrOptionsGrid grid = c.grid()
                    .add(toggle(c, "f5_enabled", () -> CameraPerspectiveConfig.f5CycleEnabled,
                            value -> CameraPerspectiveConfig.f5CycleEnabled = value))
                    .add(toggle(c, "f5_shoulder", () -> CameraPerspectiveConfig.shoulderInF5,
                            value -> CameraPerspectiveConfig.shoulderInF5 = value))
                    .add(toggle(c, "f5_freelook", () -> CameraPerspectiveConfig.freeLookInF5,
                            value -> CameraPerspectiveConfig.freeLookInF5 = value))
                    .add(toggle(c, "f5_cursorlook", () -> CameraPerspectiveConfig.cursorLookInF5,
                            value -> CameraPerspectiveConfig.cursorLookInF5 = value))
                    .add(toggle(c, "f5_drone", () -> CameraPerspectiveConfig.droneInF5,
                            value -> CameraPerspectiveConfig.droneInF5 = value))
                    .add(toggle(c, "f5_replace_third", () -> CameraPerspectiveConfig.replaceDefaultPerspective,
                            value -> CameraPerspectiveConfig.replaceDefaultPerspective = value))
                    .add(toggle(c, "f5_skip_front", () -> CameraPerspectiveConfig.skipThirdPersonFront,
                            value -> CameraPerspectiveConfig.skipThirdPersonFront = value))
                    .add(toggle(c, "f5_remember", () -> CameraPerspectiveConfig.rememberLastPerspective,
                            value -> CameraPerspectiveConfig.rememberLastPerspective = value))
                    .add(c.dropdownText("f5_default", () -> tr("gui.camera.f5_default"),
                            () -> CameraPerspectiveConfig.defaultMode,
                            value -> CameraPerspectiveConfig.defaultMode = CameraPerspectiveConfig.normalizeMode(value),
                            Arrays.asList("vanilla_first", "vanilla_third", "shoulder", "free_look", "cursor_look", "drone", "vanilla_front"),
                            value -> tr("gui.camera.mode." + value)).size(260, 24))
                    .add(toggle(c, "freelook_toggle", () -> FreeLookConfig.toggleMode,
                            value -> FreeLookConfig.toggleMode = value))
                    .add(slider(c, "freelook_pitch", () -> FreeLookConfig.pitchLimitDegrees,
                            value -> FreeLookConfig.pitchLimitDegrees = value, 1, 90, 1))
                    .add(slider(c, "freelook_response", () -> FreeLookConfig.orientationResponse,
                            value -> FreeLookConfig.orientationResponse = value, 0, 120, 1))
                    .add(slider(c, "freelook_mouse_response", () -> FreeLookConfig.mouseResponse,
                            value -> FreeLookConfig.mouseResponse = value, 0, 4, 0.01))
                    .add(slider(c, "freelook_roll_speed", () -> FreeLookConfig.rollSpeedDegrees,
                            value -> FreeLookConfig.rollSpeedDegrees = value, 0, 720, 1))
                    .add(slider(c, "freelook_distance", () -> FreeLookConfig.distance,
                            value -> FreeLookConfig.distance = value, 0, 16, 0.1))
                    .add(toggle(c, "freelook_collision", () -> FreeLookConfig.collision,
                            value -> FreeLookConfig.collision = value))
                    .add(slider(c, "cursorlook_speed", () -> CursorLookConfig.speed,
                            value -> CursorLookConfig.speed = value, 0.1, 4, 0.1))
                    .add(slider(c, "cursorlook_aim_distance", () -> CursorLookConfig.aimDistance,
                            value -> CursorLookConfig.aimDistance = value, 16, 4096, 16))
                    .add(toggle(c, "cursorlook_shoulder_offset",
                            () -> CursorLookConfig.useShoulderOffset,
                            value -> CursorLookConfig.useShoulderOffset = value))
                    .add(toggle(c, "cursorlook_head_only_aim",
                            () -> CursorLookConfig.headOnlyAim,
                            value -> CursorLookConfig.headOnlyAim = value))
                    .add(toggle(c, "cursorlook_camera_relative_movement",
                            () -> CursorLookConfig.cameraRelativeMovement,
                            value -> CursorLookConfig.cameraRelativeMovement = value))
                    .add(toggle(c, "drone_collision", () -> DroneCameraConfig.collision,
                            value -> DroneCameraConfig.collision = value))
                    .add(toggle(c, "drone_interaction", () -> DroneCameraConfig.allowCameraInteraction,
                            value -> DroneCameraConfig.allowCameraInteraction = value))
                    .add(slider(c, "drone_speed", () -> DroneCameraConfig.speed,
                            value -> DroneCameraConfig.speed = value, 0.1, 128, 0.1))
                    .add(slider(c, "drone_response", () -> DroneCameraConfig.translationResponse,
                            value -> DroneCameraConfig.translationResponse = value, 0, 120, 1))
                    .add(slider(c, "drone_sensitivity", () -> DroneCameraConfig.lookSensitivity,
                            value -> DroneCameraConfig.lookSensitivity = value, 0.0001, 0.02, 0.0001))
                    .add(slider(c, "drone_roll_speed", () -> DroneCameraConfig.rollSpeedDegrees,
                            value -> DroneCameraConfig.rollSpeedDegrees = value, 0, 720, 1))
                    .add(slider(c, "shoulder_offset_x", () -> ShoulderCameraConfig.offsetX,
                            value -> ShoulderCameraConfig.offsetX = value, -8, 8, 0.05))
                    .add(slider(c, "shoulder_offset_y", () -> ShoulderCameraConfig.offsetY,
                            value -> ShoulderCameraConfig.offsetY = value, -8, 8, 0.05))
                    .add(slider(c, "shoulder_distance", () -> ShoulderCameraConfig.offsetZ,
                            value -> ShoulderCameraConfig.offsetZ = value, -16, 16, 0.05))
                    .add(slider(c, "shoulder_min_x", () -> ShoulderCameraConfig.minOffsetX,
                            value -> ShoulderCameraConfig.minOffsetX = value, -32, 32, 0.05))
                    .add(slider(c, "shoulder_max_x", () -> ShoulderCameraConfig.maxOffsetX,
                            value -> ShoulderCameraConfig.maxOffsetX = value, -32, 32, 0.05))
                    .add(slider(c, "shoulder_min_y", () -> ShoulderCameraConfig.minOffsetY,
                            value -> ShoulderCameraConfig.minOffsetY = value, -32, 32, 0.05))
                    .add(slider(c, "shoulder_max_y", () -> ShoulderCameraConfig.maxOffsetY,
                            value -> ShoulderCameraConfig.maxOffsetY = value, -32, 32, 0.05))
                    .add(slider(c, "shoulder_min_z", () -> ShoulderCameraConfig.minOffsetZ,
                            value -> ShoulderCameraConfig.minOffsetZ = value, -64, 64, 0.05))
                    .add(slider(c, "shoulder_max_z", () -> ShoulderCameraConfig.maxOffsetZ,
                            value -> ShoulderCameraConfig.maxOffsetZ = value, -64, 64, 0.05))
                    .add(toggle(c, "shoulder_unlimited_x", () -> ShoulderCameraConfig.unlimitedOffsetX,
                            value -> ShoulderCameraConfig.unlimitedOffsetX = value))
                    .add(toggle(c, "shoulder_unlimited_y", () -> ShoulderCameraConfig.unlimitedOffsetY,
                            value -> ShoulderCameraConfig.unlimitedOffsetY = value))
                    .add(toggle(c, "shoulder_unlimited_z", () -> ShoulderCameraConfig.unlimitedOffsetZ,
                            value -> ShoulderCameraConfig.unlimitedOffsetZ = value))
                    .add(slider(c, "shoulder_step", () -> ShoulderCameraConfig.cameraStepSize,
                            value -> ShoulderCameraConfig.cameraStepSize = value, 0.001, 2, 0.001))
                    .add(slider(c, "shoulder_head_clearance", () -> ShoulderCameraConfig.keepCameraOutOfHeadMultiplier,
                            value -> ShoulderCameraConfig.keepCameraOutOfHeadMultiplier = value, 0, 4, 0.05))
                    .add(slider(c, "shoulder_sprint_x", () -> ShoulderCameraConfig.sprintXMultiplier,
                            value -> ShoulderCameraConfig.sprintXMultiplier = value, 0, 4, 0.05))
                    .add(slider(c, "shoulder_sprint_y", () -> ShoulderCameraConfig.sprintYMultiplier,
                            value -> ShoulderCameraConfig.sprintYMultiplier = value, 0, 4, 0.05))
                    .add(slider(c, "shoulder_sprint_z", () -> ShoulderCameraConfig.sprintZMultiplier,
                            value -> ShoulderCameraConfig.sprintZMultiplier = value, 0, 4, 0.05))
                    .add(slider(c, "shoulder_passenger_x", () -> ShoulderCameraConfig.passengerXMultiplier,
                            value -> ShoulderCameraConfig.passengerXMultiplier = value, 0, 4, 0.05))
                    .add(slider(c, "shoulder_passenger_y", () -> ShoulderCameraConfig.passengerYMultiplier,
                            value -> ShoulderCameraConfig.passengerYMultiplier = value, 0, 4, 0.05))
                    .add(slider(c, "shoulder_passenger_z", () -> ShoulderCameraConfig.passengerZMultiplier,
                            value -> ShoulderCameraConfig.passengerZMultiplier = value, 0, 4, 0.05))
                    .add(toggle(c, "shoulder_valkyrien_collision", () -> ShoulderCameraConfig.valkyrienShipCollision,
                            value -> ShoulderCameraConfig.valkyrienShipCollision = value))
                    .add(toggle(c, "shoulder_collision", () -> ShoulderCameraConfig.collision,
                            value -> ShoulderCameraConfig.collision = value))
                    .add(toggle(c, "shoulder_climbing", () -> ShoulderCameraConfig.centerWhenClimbing,
                            value -> ShoulderCameraConfig.centerWhenClimbing = value))
                    .add(slider(c, "shoulder_transition", () -> ShoulderCameraConfig.transitionSpeed,
                            value -> ShoulderCameraConfig.transitionSpeed = value, 0.05, 1, 0.05))
                    .add(toggle(c, "shoulder_dynamic_offsets", () -> ShoulderCameraConfig.dynamicallyAdjustOffsets,
                            value -> ShoulderCameraConfig.dynamicallyAdjustOffsets = value))
                    .add(toggle(c, "shoulder_limit_reach", () -> ShoulderCameraConfig.limitPlayerReach,
                            value -> ShoulderCameraConfig.limitPlayerReach = value))
                    .add(toggle(c, "shoulder_custom_ray", () -> ShoulderCameraConfig.useCustomRaytraceDistance,
                            value -> ShoulderCameraConfig.useCustomRaytraceDistance = value))
                    .add(slider(c, "shoulder_ray_distance", () -> ShoulderCameraConfig.customRaytraceDistance,
                            value -> ShoulderCameraConfig.customRaytraceDistance = value, 4, 512, 1))
                    .add(slider(c, "shoulder_down_angle", () -> ShoulderCameraConfig.centerWhenLookingDownDegrees,
                            value -> ShoulderCameraConfig.centerWhenLookingDownDegrees = value, 0, 90, 1))
                    .add(c.dropdownText("shoulder_crosshair", () -> tr("gui.camera.shoulder_crosshair"),
                            () -> ShoulderCameraConfig.crosshairMode,
                            value -> ShoulderCameraConfig.crosshairMode = ShoulderCameraConfig.normalizeCrosshairMode(value),
                            Arrays.asList("camera", "player", "dual", "off"), value -> tr("gui.camera.crosshair." + value)).size(260, 24))
                    .add(c.dropdownText("shoulder_crosshair_type", () -> tr("gui.camera.shoulder_crosshair_type"),
                            () -> ShoulderCameraConfig.crosshairType,
                            value -> ShoulderCameraConfig.crosshairType = ShoulderCameraConfig.normalizeCrosshairType(value),
                            Arrays.asList("adaptive", "dynamic", "static", "static_with_1pp", "dynamic_with_1pp"),
                            value -> tr("gui.camera.crosshair_type." + value)).size(260, 24))
                    .add(visibility(c, "shoulder_visibility_first", 0))
                    .add(visibility(c, "shoulder_visibility_third_back", 1))
                    .add(visibility(c, "shoulder_visibility_front", 2))
                    .add(visibility(c, "shoulder_visibility_shoulder", 3))
                    .add(toggle(c, "shoulder_transparency", () -> ShoulderCameraConfig.playerTransparency,
                            value -> ShoulderCameraConfig.playerTransparency = value))
                    .add(slider(c, "shoulder_transparency_percent", () -> (double) ShoulderCameraConfig.playerTransparencyPercent,
                            value -> ShoulderCameraConfig.playerTransparencyPercent = (int) Math.round(value), 0, 100, 1))
                    .add(slider(c, "shoulder_hide_up_angle", () -> ShoulderCameraConfig.hidePlayerWhenLookingUpAngle,
                            value -> ShoulderCameraConfig.hidePlayerWhenLookingUpAngle = value, 0, 90, 1))
                    .add(toggle(c, "shoulder_hide_player", () -> ShoulderCameraConfig.hidePlayerWhenLookingUp,
                            value -> ShoulderCameraConfig.hidePlayerWhenLookingUp = value));
            NfrOptionsGrid adaptive = c.grid()
                    .add(listField("gui.camera.shoulder_adaptive_hold_items",
                            () -> ShoulderCameraConfig.adaptiveHoldItems,
                            value -> ShoulderCameraConfig.adaptiveHoldItems = value))
                    .add(listField("gui.camera.shoulder_adaptive_use_items",
                            () -> ShoulderCameraConfig.adaptiveUseItems,
                            value -> ShoulderCameraConfig.adaptiveUseItems = value))
                    .add(listField("gui.camera.shoulder_adaptive_hold_properties",
                            () -> ShoulderCameraConfig.adaptiveHoldProperties,
                            value -> ShoulderCameraConfig.adaptiveHoldProperties = value))
                    .add(listField("gui.camera.shoulder_adaptive_use_properties",
                            () -> ShoulderCameraConfig.adaptiveUseProperties,
                            value -> ShoulderCameraConfig.adaptiveUseProperties = value));
            return new PageView(grid, adaptive);
        }

        @Override public void apply() {
            CameraPerspectiveConfig.save();
            FreeLookConfig.save();
            CursorLookConfig.save();
            DroneCameraConfig.save();
            ShoulderCameraConfig.save();
        }

        @Override public void cancel() {
            CameraPerspectiveConfig.f5CycleEnabled = f5;
            CameraPerspectiveConfig.shoulderInF5 = f5Shoulder;
            CameraPerspectiveConfig.freeLookInF5 = f5Free;
            CameraPerspectiveConfig.cursorLookInF5 = f5Cursor;
            CameraPerspectiveConfig.droneInF5 = f5Drone;
            CameraPerspectiveConfig.replaceDefaultPerspective = replaceThird;
            CameraPerspectiveConfig.skipThirdPersonFront = skipFront;
            CameraPerspectiveConfig.rememberLastPerspective = rememberLast;
            CameraPerspectiveConfig.defaultMode = defaultMode;
            FreeLookConfig.toggleMode = freeToggle;
            FreeLookConfig.pitchLimitDegrees = pitchLimit;
            FreeLookConfig.orientationResponse = response;
            FreeLookConfig.mouseResponse = mouseResponse;
            FreeLookConfig.rollSpeedDegrees = freeLookRollSpeed;
            FreeLookConfig.distance = freeLookDistance;
            FreeLookConfig.collision = freeLookCollision;
            CursorLookConfig.speed = cursorLookSpeed;
            CursorLookConfig.aimDistance = cursorLookAimDistance;
            CursorLookConfig.useShoulderOffset = cursorLookShoulderOffset;
            CursorLookConfig.headOnlyAim = cursorLookHeadOnlyAim;
            CursorLookConfig.cameraRelativeMovement = cursorLookCameraRelativeMovement;
            DroneCameraConfig.collision = droneCollision;
            DroneCameraConfig.allowCameraInteraction = droneInteraction;
            DroneCameraConfig.speed = droneSpeed;
            DroneCameraConfig.translationResponse = droneResponse;
            DroneCameraConfig.lookSensitivity = droneSensitivity;
            DroneCameraConfig.rollSpeedDegrees = droneRollSpeed;
            ShoulderCameraConfig.offsetX = offsetX;
            ShoulderCameraConfig.offsetY = offsetY;
            ShoulderCameraConfig.offsetZ = offsetZ;
            ShoulderCameraConfig.collision = shoulderCollision;
            ShoulderCameraConfig.centerWhenClimbing = climbing;
            ShoulderCameraConfig.centerWhenLookingDownDegrees = downAngle;
            ShoulderCameraConfig.crosshairMode = crosshair;
            ShoulderCameraConfig.crosshairType = crosshairType;
            ShoulderCameraConfig.crosshairVisibility.clear();
            ShoulderCameraConfig.crosshairVisibility.putAll(visibility);
            ShoulderCameraConfig.playerTransparency = transparency;
            ShoulderCameraConfig.playerTransparencyPercent = transparencyPercent;
            ShoulderCameraConfig.transitionSpeed = transition;
            ShoulderCameraConfig.dynamicallyAdjustOffsets = dynamicOffsets;
            ShoulderCameraConfig.limitPlayerReach = limitReach;
            ShoulderCameraConfig.useCustomRaytraceDistance = customRay;
            ShoulderCameraConfig.customRaytraceDistance = rayDistance;
            ShoulderCameraConfig.hidePlayerWhenLookingUpAngle = hideAngle;
            ShoulderCameraConfig.hidePlayerWhenLookingUp = hidePlayer;
            ShoulderCameraConfig.minOffsetX = minX; ShoulderCameraConfig.minOffsetY = minY;
            ShoulderCameraConfig.minOffsetZ = minZ; ShoulderCameraConfig.maxOffsetX = maxX;
            ShoulderCameraConfig.maxOffsetY = maxY; ShoulderCameraConfig.maxOffsetZ = maxZ;
            ShoulderCameraConfig.unlimitedOffsetX = unlimitedX;
            ShoulderCameraConfig.unlimitedOffsetY = unlimitedY;
            ShoulderCameraConfig.unlimitedOffsetZ = unlimitedZ;
            ShoulderCameraConfig.cameraStepSize = cameraStep;
            ShoulderCameraConfig.keepCameraOutOfHeadMultiplier = keepOutOfHead;
            ShoulderCameraConfig.valkyrienShipCollision = valkyrienCollision;
            ShoulderCameraConfig.sprintXMultiplier = sprintX;
            ShoulderCameraConfig.sprintYMultiplier = sprintY;
            ShoulderCameraConfig.sprintZMultiplier = sprintZ;
            ShoulderCameraConfig.passengerXMultiplier = passengerX;
            ShoulderCameraConfig.passengerYMultiplier = passengerY;
            ShoulderCameraConfig.passengerZMultiplier = passengerZ;
            ShoulderCameraConfig.adaptiveHoldItems = new ArrayList<>(adaptiveHoldItems);
            ShoulderCameraConfig.adaptiveUseItems = new ArrayList<>(adaptiveUseItems);
            ShoulderCameraConfig.adaptiveHoldProperties = new ArrayList<>(adaptiveHoldProperties);
            ShoulderCameraConfig.adaptiveUseProperties = new ArrayList<>(adaptiveUseProperties);
        }
    }

    private static IWidget toggle(NfrSettingsControls c, String key,
                                  java.util.function.Supplier<Boolean> getter,
                                  java.util.function.Consumer<Boolean> setter) {
        return c.toggleText(() -> tr("gui.camera." + key), () -> tr("tooltip.camera." + key), getter, setter);
    }

    private static IWidget slider(NfrSettingsControls c, String key,
                                  java.util.function.DoubleSupplier getter,
                                  java.util.function.DoubleConsumer setter,
                                  double min, double max, double step) {
        return c.decimalSlider(() -> tr("gui.camera." + key), () -> (float) getter.getAsDouble(),
                value -> setter.accept(value.doubleValue()), (float) min, (float) max, (float) step);
    }

    private static IWidget visibility(NfrSettingsControls c, String key, int perspective) {
        return c.dropdownText(key, () -> tr("gui.camera." + key),
                () -> ShoulderCameraConfig.crosshairVisibility.getOrDefault(perspective, "always"),
                value -> ShoulderCameraConfig.crosshairVisibility.put(perspective,
                        ShoulderCameraConfig.normalizeVisibility(value)),
                Arrays.asList("always", "never", "when_aiming", "when_in_range", "when_aiming_or_in_range"),
                value -> tr("gui.camera.visibility." + value)).size(260, 24);
    }

    private static NfrLabeledTextField listField(String label, java.util.function.Supplier<List<String>> getter,
                                                  java.util.function.Consumer<List<String>> setter) {
        return new NfrLabeledTextField(tr(label), new TextFieldWidget().setMaxLength(2048)
                .value(new NfrStringValue(() -> String.join(", ", getter.get()),
                        value -> setter.accept(parseList(value))))).size(260, 46);
    }

    private static List<String> parseList(String value) {
        List<String> result = new ArrayList<>();
        if (value == null) return result;
        for (String entry : value.split(",")) {
            String normalized = entry.trim();
            if (!normalized.isEmpty() && !result.contains(normalized)) result.add(normalized);
        }
        return result;
    }

    private static String tr(String key) { return AddonI18n.tr("neofontrender_ui_enhancements." + key); }

    private static final class PageView extends NfrContentView<PageView> {
        private PageView(NfrOptionsGrid grid, NfrOptionsGrid adaptive) {
            super(section(grid, grid::preferredHeight), section(adaptive, adaptive::preferredHeight));
        }
    }
}
