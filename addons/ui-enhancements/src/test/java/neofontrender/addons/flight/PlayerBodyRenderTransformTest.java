package neofontrender.addons.flight;

import neofontrender.addons.api.flight.FlightAttitude;
import neofontrender.addons.api.flight.FlightBodyPose;
import neofontrender.addons.api.flight.FlightVector;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class PlayerBodyRenderTransformTest {
    private static final double EPSILON = 1.0E-6D;
    private static final double HALF_PI = Math.PI / 2.0D;
    private static final double PI = Math.PI;

    @Test
    void legacyAircraftPoseMatchesThePreviousRendererRotationSequence() {
        FlightAttitude aircraft = compose(
                axis(0.0D, 1.0D, 0.0D, -HALF_PI),
                axis(1.0D, 0.0D, 0.0D, Math.toRadians(30.0D)),
                axis(0.0D, 0.0D, 1.0D, HALF_PI));
        PlayerBodyRenderTransform transform = PlayerBodyRenderTransform.fromFlightPose(
                new FlightBodyPose(aircraft));

        for (FlightVector point : new FlightVector[]{
                new FlightVector(1.0D, 0.0D, 0.0D),
                new FlightVector(0.0D, 1.0D, 0.0D),
                new FlightVector(0.0D, 0.0D, 1.0D),
                new FlightVector(0.3D, -0.7D, 1.2D)}) {
            FlightVector oldSequence = oldLegacyRoot(aircraft, point);
            FlightVector converted = transform.ysmRootAttitude().rotate(point.x, point.y, point.z);
            assertVectorEquals(oldSequence, converted);
        }
    }

    @Test
    void legacyIdentityAndExplicitModelUpYHaveDistinctMeaning() {
        PlayerBodyRenderTransform legacy = PlayerBodyRenderTransform.fromFlightPose(
                new FlightBodyPose(FlightAttitude.IDENTITY));
        PlayerBodyRenderTransform modelUp = PlayerBodyRenderTransform.fromFlightPose(
                FlightBodyPose.modelUpY(FlightAttitude.IDENTITY));

        assertVectorEquals(new FlightVector(0.0D, 0.0D, 1.0D), legacy.modelUpToWorld());
        assertVectorEquals(new FlightVector(0.0D, 1.0D, 0.0D), modelUp.modelUpToWorld());
        assertVectorEquals(new FlightVector(0.0D, -1.0D, 0.0D), legacy.modelForwardToWorld());
        assertVectorEquals(new FlightVector(0.0D, 0.0D, 1.0D), modelUp.modelForwardToWorld());
    }

    @Test
    void explicitModelUpYSupportsAllSixCardinalUpDirections() {
        FlightAttitude[] attitudes = {
                FlightAttitude.IDENTITY,
                axis(0.0D, 0.0D, 1.0D, -HALF_PI),
                axis(0.0D, 0.0D, 1.0D, HALF_PI),
                axis(1.0D, 0.0D, 0.0D, HALF_PI),
                axis(1.0D, 0.0D, 0.0D, -HALF_PI),
                axis(1.0D, 0.0D, 0.0D, PI)
        };
        FlightVector[] expectedUp = {
                new FlightVector(0.0D, 1.0D, 0.0D),
                new FlightVector(1.0D, 0.0D, 0.0D),
                new FlightVector(-1.0D, 0.0D, 0.0D),
                new FlightVector(0.0D, 0.0D, 1.0D),
                new FlightVector(0.0D, 0.0D, -1.0D),
                new FlightVector(0.0D, -1.0D, 0.0D)
        };

        for (int index = 0; index < attitudes.length; index++) {
            PlayerBodyRenderTransform transform = PlayerBodyRenderTransform.fromFlightPose(
                    FlightBodyPose.modelUpY(attitudes[index]));
            assertVectorEquals(expectedUp[index], transform.modelUpToWorld());
        }
    }

    @Test
    void nonCommutingLegacyQuaternionHasTheReviewedModelUpAndForward() {
        FlightAttitude aircraft = compose(
                axis(0.0D, 1.0D, 0.0D, -HALF_PI),
                axis(1.0D, 0.0D, 0.0D, Math.toRadians(30.0D)),
                axis(0.0D, 0.0D, 1.0D, HALF_PI));
        PlayerBodyRenderTransform transform = PlayerBodyRenderTransform.fromFlightPose(
                new FlightBodyPose(aircraft));

        assertVectorEquals(new FlightVector(-Math.sqrt(3.0D) / 2.0D, -0.5D, 0.0D),
                transform.modelUpToWorld());
        assertVectorEquals(new FlightVector(0.0D, 0.0D, 1.0D),
                transform.modelForwardToWorld());
    }

    @Test
    void vanillaYawRootMatchesTheNativeUprightRotation() {
        FlightAttitude bodyYawNinety = FlightAttitude.fromMinecraftDegrees(0.0D, 90.0D, 0.0D);
        PlayerBodyRenderTransform transform = PlayerBodyRenderTransform.fromFlightPose(
                FlightBodyPose.modelUpY(bodyYawNinety));

        FlightVector modelFront = transform.ysmRootAttitude().rotate(0.0D, 0.0D, -1.0D);
        assertVectorEquals(new FlightVector(-1.0D, 0.0D, 0.0D), modelFront);
    }

    @Test
    void quaternionSignDoesNotChangeRootOrHeadLook() {
        FlightAttitude pose = compose(
                axis(0.0D, 1.0D, 0.0D, Math.toRadians(-65.0D)),
                axis(1.0D, 0.0D, 0.0D, Math.toRadians(32.0D)),
                axis(0.0D, 0.0D, 1.0D, Math.toRadians(47.0D)));
        FlightAttitude negated = new FlightAttitude(-pose.x, -pose.y, -pose.z, -pose.w);
        PlayerBodyRenderTransform positive = PlayerBodyRenderTransform.fromFlightPose(
                FlightBodyPose.modelUpY(pose));
        PlayerBodyRenderTransform negative = PlayerBodyRenderTransform.fromFlightPose(
                FlightBodyPose.modelUpY(negated));

        assertVectorEquals(positive.modelUpToWorld(), negative.modelUpToWorld());
        FlightVector sample = new FlightVector(1.0D, 2.0D, 3.0D);
        assertVectorEquals(positive.ysmRootAttitude().rotate(sample.x, sample.y, sample.z),
                negative.ysmRootAttitude().rotate(sample.x, sample.y, sample.z));
        FlightAttitude midpoint = pose.slerp(negated, 0.5D);
        assertVectorEquals(pose.rotate(1.0D, 0.0D, 0.0D), midpoint.rotate(1.0D, 0.0D, 0.0D));
        assertVectorEquals(pose.rotate(0.0D, 1.0D, 0.0D), midpoint.rotate(0.0D, 1.0D, 0.0D));
        assertVectorEquals(pose.rotate(0.0D, 0.0D, 1.0D), midpoint.rotate(0.0D, 0.0D, 1.0D));
        assertYsmAnglesEqual(positive.ysmBoneAngles(
                        FlightAttitude.fromMinecraftDegrees(20.0D, 30.0D, 0.0D)),
                negative.ysmBoneAngles(
                        FlightAttitude.fromMinecraftDegrees(20.0D, 30.0D, 0.0D)));
    }

    @Test
    void headAnglesUseYsmBoneFieldSignsAndRemainRelativeToBodyYaw() {
        PlayerBodyRenderTransform upright = PlayerBodyRenderTransform.fromFlightPose(
                FlightBodyPose.modelUpY(FlightAttitude.IDENTITY));
        PlayerBodyRenderTransform.YsmBoneAngles look =
                upright.ysmBoneAnglesFromMinecraftLocalDegrees(20.0D, 30.0D);

        assertEquals(-30.0D, look.yawDegrees, EPSILON);
        assertEquals(-20.0D, look.pitchDegrees, EPSILON);
        PlayerBodyRenderTransform.MinecraftLocalAngles minecraft =
                upright.minecraftLocalAngles(FlightAttitude.fromMinecraftDegrees(
                        20.0D, 30.0D, 0.0D));
        assertEquals(30.0D, minecraft.yawDegrees, EPSILON);
        assertEquals(20.0D, minecraft.pitchDegrees, EPSILON);
        PlayerBodyRenderTransform.MinecraftLocalAngles sampled =
                PlayerBodyRenderTransform.fromFlightPose(FlightBodyPose.modelUpY(
                        FlightAttitude.IDENTITY,
                        FlightAttitude.fromMinecraftDegrees(20.0D, 30.0D, 0.0D)))
                        .minecraftLocalAngles();
        assertEquals(30.0D, sampled.yawDegrees, EPSILON);
        assertEquals(20.0D, sampled.pitchDegrees, EPSILON);

        PlayerBodyRenderTransform bodyRolled = PlayerBodyRenderTransform.fromFlightPose(
                FlightBodyPose.modelUpY(axis(0.0D, 0.0D, 1.0D, HALF_PI)));
        PlayerBodyRenderTransform.YsmBoneAngles bodyRelative =
                bodyRolled.ysmBoneAnglesFromMinecraftLocalDegrees(15.0D, 25.0D);
        assertEquals(-25.0D, bodyRelative.yawDegrees, EPSILON);
        assertEquals(-15.0D, bodyRelative.pitchDegrees, EPSILON);

        FlightAttitude bodyYawThirty = FlightAttitude.fromMinecraftDegrees(0.0D, 30.0D, 0.0D);
        FlightAttitude headYawSixty = FlightAttitude.fromMinecraftDegrees(0.0D, 60.0D, 0.0D);
        PlayerBodyRenderTransform bodyRotated = PlayerBodyRenderTransform.fromFlightPose(
                FlightBodyPose.modelUpY(bodyYawThirty, headYawSixty));
        PlayerBodyRenderTransform.YsmBoneAngles relative = bodyRotated.ysmBoneAngles();

        assertEquals(-30.0D, relative.yawDegrees, EPSILON);
        assertEquals(0.0D, relative.pitchDegrees, EPSILON);
    }

    @Test
    void headLookUsesTheBodyInverseForNonUprightPose() {
        FlightAttitude bodyRollNinety = axis(0.0D, 0.0D, 1.0D, HALF_PI);
        FlightAttitude headYawFortyFive = FlightAttitude.fromMinecraftDegrees(
                0.0D, 45.0D, 0.0D);
        PlayerBodyRenderTransform bodyRolled = PlayerBodyRenderTransform.fromFlightPose(
                FlightBodyPose.modelUpY(bodyRollNinety, headYawFortyFive));
        PlayerBodyRenderTransform.YsmBoneAngles local = bodyRolled.ysmBoneAngles();

        assertEquals(0.0D, local.yawDegrees, EPSILON);
        assertEquals(45.0D, local.pitchDegrees, EPSILON);
    }

    @Test
    void absentSampledHeadQuaternionPreservesTheNeutralLegacyBoneAngles() {
        PlayerBodyRenderTransform transform = PlayerBodyRenderTransform.fromFlightPose(
                new FlightBodyPose(FlightAttitude.fromMinecraftDegrees(-25.0D, 60.0D, 70.0D)));

        assertEquals(0.0D, transform.ysmBoneAngles().yawDegrees, EPSILON);
        assertEquals(0.0D, transform.ysmBoneAngles().pitchDegrees, EPSILON);
        assertEquals(0.0D, transform.minecraftLocalAngles().yawDegrees, EPSILON);
        assertEquals(0.0D, transform.minecraftLocalAngles().pitchDegrees, EPSILON);
    }

    @Test
    void nullPoseAndDisabledPresentationLeaveTheNativePathUnselected() {
        assertNull(PlayerBodyRenderTransform.resolve(null, true));
        assertNull(PlayerBodyRenderTransform.resolve(
                FlightBodyPose.modelUpY(FlightAttitude.IDENTITY), false));
    }

    @Test
    void rootRotationLeavesTheTranslatedRenderOriginUnchanged() {
        PlayerBodyRenderTransform transform = PlayerBodyRenderTransform.fromFlightPose(
                FlightBodyPose.modelUpY(FlightAttitude.IDENTITY));
        FlightVector renderTranslation = new FlightVector(12.0D, 3.0D, -7.0D);
        FlightVector localOrigin = new FlightVector(0.0D, 0.0D, 0.0D);
        FlightVector modelPoint = new FlightVector(2.0D, 4.0D, 5.0D);

        FlightVector renderedOrigin = transform.modelPointInWorld(renderTranslation, localOrigin);
        FlightVector renderedPoint = transform.modelPointInWorld(renderTranslation, modelPoint);

        assertVectorEquals(renderTranslation, renderedOrigin);
        assertVectorEquals(new FlightVector(10.0D, 7.0D, -12.0D), renderedPoint);
    }

    private static FlightVector oldLegacyRoot(FlightAttitude aircraft, FlightVector modelPoint) {
        FlightVector afterXMinusNinety = new FlightVector(
                modelPoint.x, modelPoint.z, -modelPoint.y);
        FlightVector afterYOneEighty = new FlightVector(
                -afterXMinusNinety.x, afterXMinusNinety.y, -afterXMinusNinety.z);
        return aircraft.rotate(afterYOneEighty.x, afterYOneEighty.y, afterYOneEighty.z);
    }

    private static FlightAttitude compose(FlightAttitude... rotations) {
        FlightAttitude result = FlightAttitude.IDENTITY;
        for (FlightAttitude rotation : rotations) result = result.multiply(rotation);
        return result;
    }

    private static FlightAttitude axis(double x, double y, double z, double radians) {
        return FlightAttitude.axisAngle(x, y, z, radians);
    }

    private static void assertYsmAnglesEqual(PlayerBodyRenderTransform.YsmBoneAngles expected,
                                             PlayerBodyRenderTransform.YsmBoneAngles actual) {
        assertEquals(expected.yawDegrees, actual.yawDegrees, EPSILON);
        assertEquals(expected.pitchDegrees, actual.pitchDegrees, EPSILON);
    }

    private static void assertVectorEquals(FlightVector expected, FlightVector actual) {
        assertEquals(expected.x, actual.x, EPSILON);
        assertEquals(expected.y, actual.y, EPSILON);
        assertEquals(expected.z, actual.z, EPSILON);
    }
}
