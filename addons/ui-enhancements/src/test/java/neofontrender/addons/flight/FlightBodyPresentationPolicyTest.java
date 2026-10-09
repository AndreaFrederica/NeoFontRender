package neofontrender.addons.flight;

import neofontrender.addons.api.flight.FlightAttitude;
import neofontrender.addons.api.flight.FlightBodyPose;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FlightBodyPresentationPolicyTest {
    @Test
    void legacyAircraftBasisKeepsItsElytraOnlyRootOwnership() {
        FlightAttitude attitude = FlightAttitude.fromMinecraftDegrees(-21.0D, 76.0D, 43.0D);

        FlightBodyPresentationPolicy.Decision decision = FlightBodyPresentationPolicy.resolve(
                new FlightBodyPose(attitude), false);

        assertFalse(decision.ownsRootQuaternion());
        assertSame(attitude, decision.rootAttitude());
        assertFalse(decision.neutralizesRelativeHead());
        assertEquals(0.0F, decision.relativeHeadYawDegrees());
        assertEquals(0.0F, decision.relativeHeadPitchDegrees());
        assertFalse(decision.isElytraFlying());

        FlightBodyPresentationPolicy.Decision elytra = FlightBodyPresentationPolicy.resolve(
                new FlightBodyPose(attitude), true);
        assertTrue(elytra.ownsRootQuaternion());
        assertTrue(elytra.neutralizesRelativeHead());
        assertTrue(elytra.isElytraFlying());
    }

    @Test
    void missingPoseUsesTheNativePathForBothElytraStates() {
        FlightBodyPresentationPolicy.Decision standing = FlightBodyPresentationPolicy.resolve(
                null, false);
        FlightBodyPresentationPolicy.Decision elytra = FlightBodyPresentationPolicy.resolve(
                null, true);

        assertFalse(standing.ownsRootQuaternion());
        assertFalse(standing.neutralizesRelativeHead());
        assertNull(standing.rootAttitude());
        assertFalse(standing.isElytraFlying());
        assertFalse(elytra.ownsRootQuaternion());
        assertFalse(elytra.neutralizesRelativeHead());
        assertNull(elytra.rootAttitude());
        assertFalse(elytra.isElytraFlying());
    }

    @Test
    void explicitModelUpYBasisOwnsRootForElytraAndNonElytraPresentation() {
        FlightBodyPose pose = FlightBodyPose.modelUpY(FlightAttitude.IDENTITY);

        FlightBodyPresentationPolicy.Decision standing = FlightBodyPresentationPolicy.resolve(
                pose, false);
        FlightBodyPresentationPolicy.Decision elytra = FlightBodyPresentationPolicy.resolve(
                pose, true);

        assertTrue(standing.ownsRootQuaternion());
        assertFalse(standing.isElytraFlying());
        assertTrue(elytra.ownsRootQuaternion());
        assertTrue(elytra.isElytraFlying());
    }
}
