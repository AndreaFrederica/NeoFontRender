package neofontrender.addons.api.flight;

import net.minecraft.util.ResourceLocation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FlightApiTest {
    @Test
    void capabilityPriorityAndRegistrationLifetimeAreDeterministic() {
        FlightRegistration low = FlightApi.registerCapabilityProvider(
                new ResourceLocation("test", "low"), 0,
                (player, capability, builtIn) -> FlightDecision.ALLOW);
        FlightRegistration high = FlightApi.registerCapabilityProvider(
                new ResourceLocation("test", "high"), 100,
                (player, capability, builtIn) -> FlightDecision.DENY);
        try {
            assertEquals(FlightDecision.DENY, FlightApi.queryCapability(
                    null, FlightCapability.CAMERA_ROTATION, false));
            high.close();
            assertEquals(FlightDecision.ALLOW, FlightApi.queryCapability(
                    null, FlightCapability.CAMERA_ROTATION, false));
        } finally {
            high.close();
            low.close();
        }
        assertEquals(FlightDecision.PASS, FlightApi.queryCapability(
                null, FlightCapability.CAMERA_ROTATION, false));
    }

    @Test
    void controllerContributionsAccumulateAndClamp() {
        FlightRegistration first = FlightApi.registerControlProvider(
                new ResourceLocation("test", "first"), 10, input -> input.addRoll(0.75F));
        FlightRegistration second = FlightApi.registerControlProvider(
                new ResourceLocation("test", "second"), 0, input -> input.addRoll(0.75F));
        try {
            FlightControlInput input = new FlightControlInput(null, 0.5F, 1.0D / 60.0D);
            FlightApi.collectControlInput(input);
            assertEquals(1.0F, input.getRoll());
        } finally {
            first.close();
            second.close();
        }
    }

    @Test
    void namespacedThemeRegistrationIsReversible() {
        String json = "{\"schema\":3,\"name\":\"Test\",\"elements\":["
                + "{\"id\":\"radar\",\"type\":\"test:radar\"}]}";
        FlightRegistration registration = FlightApi.registerHudTheme(
                new ResourceLocation("test", "theme"), json);
        assertTrue(FlightApi.registeredHudThemes().containsKey("test:theme"));
        registration.close();
        assertFalse(FlightApi.registeredHudThemes().containsKey("test:theme"));
    }

    @Test
    void apiV9ComponentRegistrationIsReversibleAndDiscoverable() {
        assertEquals(9, FlightApi.getApiVersion());
        ResourceLocation type = new ResourceLocation("test", "radar");
        FlightRegistration registration = FlightApi.registerHudComponent(type,
                (context, element) -> {});
        assertTrue(FlightApi.hasHudComponent("test:radar"));
        assertTrue(FlightApi.registeredHudComponentTypes().contains("test:radar"));
        registration.close();
        assertFalse(FlightApi.hasHudComponent("test:radar"));
        assertFalse(FlightApi.registeredHudComponentTypes().contains("test:radar"));
    }

    @Test
    void customFlightModeCanTakeOverOnlyKeyboardYaw() {
        FlightRegistration registration = FlightApi.registerCapabilityProvider(
                new ResourceLocation("test", "rudder_takeover"), 100,
                (player, capability, builtIn) -> capability == FlightCapability.KEYBOARD_YAW
                        ? FlightDecision.DENY : FlightDecision.PASS);
        try {
            assertEquals(FlightDecision.DENY, FlightApi.queryCapability(
                    null, FlightCapability.KEYBOARD_YAW, true));
            assertEquals(FlightDecision.PASS, FlightApi.queryCapability(
                    null, FlightCapability.CAMERA_ROTATION, true));
            assertEquals(FlightDecision.PASS, FlightApi.queryCapability(
                    null, FlightCapability.PLAYER_ROLL_RENDERING, true));
        } finally {
            registration.close();
        }
    }

    @Test
    void bodyPoseRegistrationIsReversible() {
        FlightRegistration registration = FlightApi.registerBodyPoseProvider(
                new ResourceLocation("test", "body"), 10,
                (player, partialTicks) -> new FlightBodyPose(
                        FlightAttitude.fromMinecraftDegrees(-90.0D, 0.0D, 0.0D)));
        FlightBodyPose pose = FlightApi.queryBodyPose(null, 0.5F);
        assertEquals(1.0D, pose.attitude.forward().y, 0.0001D);
        registration.close();
        assertNull(FlightApi.queryBodyPose(null, 0.5F));
    }

    @Test
    void bodyPoseCarriesCompleteQuaternionAttitude() {
        FlightBodyPose pose = new FlightBodyPose(
                FlightAttitude.fromMinecraftDegrees(-20.0D, 45.0D, 37.5D));
        FlightEulerAngles angles = pose.attitude.toMinecraftEuler(-20.0D, 45.0D, 37.5D);
        assertEquals(-20.0F, angles.pitchDegrees, 0.001F);
        assertEquals(45.0F, angles.yawDegrees, 0.001F);
        assertEquals(37.5F, angles.rollDegrees, 0.001F);
    }

    @Test
    void hudAttitudeProviderOverridesCameraWithBodyAxis() {
        FlightRegistration registration = FlightApi.registerHudAttitudeProvider(
                new ResourceLocation("test", "airframe"), 100,
                (player, partialTicks) -> new FlightHudAttitude(
                        FlightAttitude.fromMinecraftDegrees(-45.0D, 90.0D, 42.0D)));
        try {
            FlightHudAttitude attitude = FlightApi.queryHudAttitude(null, 0.5F);
            FlightEulerAngles angles = attitude.getAttitude()
                    .toMinecraftEuler(-45.0D, 90.0D, 42.0D);
            assertEquals(-45.0F, angles.pitchDegrees, 0.001F);
            assertEquals(90.0F, angles.yawDegrees, 0.001F);
            assertEquals(42.0F, angles.rollDegrees, 0.001F);
        } finally {
            registration.close();
        }
        assertNull(FlightApi.queryHudAttitude(null, 0.5F));
    }

    @Test
    void maneuverAndCameraTrackingRegistrationsAreIndependent() {
        FlightRegistration maneuver = FlightApi.registerManeuverHandler(
                new ResourceLocation("test", "controls"), 100,
                input -> input.getPitch() > 0.25F);
        FlightRegistration camera = FlightApi.registerCameraTrackingProvider(
                new ResourceLocation("test", "camera"), 100,
                (player, partialTicks) -> FlightCameraTracking.rigid(
                        FlightAttitude.fromMinecraftDegrees(-10.0F, 25.0F, 30.0F)));
        try {
            assertTrue(FlightApi.dispatchManeuverInput(new FlightManeuverInput(
                    null, 0.5F, 0.016D, 0.5F, 0.0F, 0.0F)));
            FlightCameraTracking tracking = FlightApi.queryCameraTracking(null, 0.5F);
            assertTrue(tracking.isRigid());
            assertEquals(25.0F, tracking.getAttitude()
                    .toMinecraftEuler(-10.0D, 25.0D, 30.0D).yawDegrees);
        } finally {
            maneuver.close();
            camera.close();
        }
        assertFalse(FlightApi.dispatchManeuverInput(new FlightManeuverInput(
                null, 0.5F, 0.016D, 1.0F, 0.0F, 0.0F)));
        assertNull(FlightApi.queryCameraTracking(null, 0.5F));
    }

    @Test
    void maneuverInputExposesRawKeyboardRudderAxis() {
        FlightManeuverInput right = new FlightManeuverInput(
                null, 0.5F, 0.016D, 0.0F, 0.7F, 0.0F, 1.0F);
        FlightManeuverInput left = new FlightManeuverInput(
                null, 0.5F, 0.016D, 0.0F, -0.7F, 0.0F, -1.0F);
        assertEquals(1.0F, right.getKeyboardYaw());
        assertEquals(-1.0F, left.getKeyboardYaw());
        assertEquals(0.7F, right.getYaw());
        assertEquals(-0.7F, left.getYaw());
    }

    @Test
    void callbackFailurePropagatesAndDiagnosticsExposeSelectedProvider() {
        FlightRegistration selected = FlightApi.registerCapabilityProvider(
                new ResourceLocation("test", "diagnostic_capability"), 100,
                (player, capability, builtIn) -> FlightDecision.ALLOW);
        try {
            assertEquals(FlightDecision.ALLOW, FlightApi.queryCapability(null,
                    FlightCapability.CAMERA_ROTATION, false));
            assertEquals("test:diagnostic_capability", FlightApi.diagnostics().capabilityProviderId());
            assertTrue(FlightApi.capabilityProviderIds().contains("test:diagnostic_capability"));
        } finally {
            selected.close();
        }
        FlightRegistration failing = FlightApi.registerCapabilityProvider(
                new ResourceLocation("test", "failing_capability"), 100,
                (player, capability, builtIn) -> { throw new IllegalStateException("capability failure"); });
        try {
            IllegalStateException error = assertThrows(IllegalStateException.class,
                    () -> FlightApi.queryCapability(null, FlightCapability.CAMERA_ROTATION, false));
            assertEquals("capability failure", error.getMessage());
        } finally {
            failing.close();
        }
    }
}
