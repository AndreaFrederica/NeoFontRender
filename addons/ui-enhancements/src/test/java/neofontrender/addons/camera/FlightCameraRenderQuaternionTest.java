package neofontrender.addons.camera;

import neofontrender.addons.api.camera.CameraAttitude;
import neofontrender.addons.api.camera.CameraFrame;
import neofontrender.addons.api.camera.CameraVector;
import neofontrender.addons.api.flight.FlightAttitude;
import neofontrender.addons.api.flight.FlightEulerAngles;
import net.minecraftforge.client.event.EntityViewRenderEvent;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FlightCameraRenderQuaternionTest {
    private static final double MATRIX_EPSILON = 1.0E-6D;

    @Test
    void mouseOnlyRenderSamplesAcrossBothPitchPolesUseTheFrameQuaternionMatrix() {
        float[] asynchronousPartialTicks = {0.78F, 0.96F, 0.02F, 0.09F, 0.17F, 0.31F, 0.54F};
        assertRenderOnlySequence("positive", 89.75D, 35.0D, 14.0D,
                new double[][]{
                        {89.50D, 35.0D, 14.0D}, {89.75D, 35.0D, 14.0D},
                        {90.00D, 35.0D, 14.0D}, {90.25D, 35.0D, 14.0D},
                        {90.50D, 35.4D, 14.3D}, {91.00D, 36.0D, 14.9D},
                        {92.00D, 37.2D, 15.5D}
                }, asynchronousPartialTicks);
        assertRenderOnlySequence("negative", -89.75D, -20.0D, -10.0D,
                new double[][]{
                        {-89.50D, -20.0D, -10.0D}, {-89.75D, -20.0D, -10.0D},
                        {-90.00D, -20.0D, -10.0D}, {-90.25D, -20.0D, -10.0D},
                        {-90.50D, -19.6D, -9.7D}, {-91.00D, -19.0D, -9.1D},
                        {-92.00D, -17.8D, -8.5D}
                }, asynchronousPartialTicks);
    }

    @Test
    void sixCardinalCameraDirectionsKeepTheirFrameBasis() {
        double[][] directions = {
                {0.0D, 0.0D}, {0.0D, 90.0D}, {0.0D, 180.0D}, {0.0D, 270.0D},
                {90.0D, 0.0D}, {-90.0D, 0.0D}
        };
        for (int index = 0; index < directions.length; index++) {
            CameraAttitude attitude = cameraAttitude(FlightAttitude.fromMinecraftDegrees(
                    directions[index][0], directions[index][1], 0.0D));
            CameraFrame frame = frame(index, 0.5F, attitude);

            assertRenderBasis(frame, submitQuaternionView(frame, false, false, false));
        }
    }

    @Test
    void vanillaCameraSetupKeepsItsEulerAndDoesNotSubmitAQuaternionMatrix() {
        CameraFrame vanillaFrame = frame(1L, 0.5F, cameraAttitude(
                FlightAttitude.fromMinecraftDegrees(12.0D, 34.0D, 5.0D)), true);
        EntityViewRenderEvent.CameraSetup event = cameraSetup(34.0F, 12.0F, 5.0F);
        boolean[] submitted = {false};

        boolean applied = CameraRenderBridge.submitQuaternionView(event, vanillaFrame,
                false, false, false, true, matrix -> submitted[0] = true);

        assertFalse(applied);
        assertFalse(submitted[0]);
        assertEquals(34.0F, event.getYaw());
        assertEquals(12.0F, event.getPitch());
        assertEquals(5.0F, event.getRoll());
    }

    @Test
    void vanillaPerspectivesZeroOneAndTwoComposeToTheFrameView() {
        CameraAttitude body = cameraAttitude(FlightAttitude.fromMinecraftDegrees(
                37.0D, -42.0D, 19.0D));
        CameraAttitude frontView = body.multiply(CameraAttitude.axisAngle(
                new CameraVector(0.0D, 1.0D, 0.0D), Math.PI));
        float[] nativeFrontHalfTurn = {
                -1.0F, 0.0F, 0.0F, 0.0F,
                0.0F, 1.0F, 0.0F, 0.0F,
                0.0F, 0.0F, -1.0F, 0.0F,
                0.0F, 0.0F, 0.0F, 1.0F
        };
        float[] identity = {
                1.0F, 0.0F, 0.0F, 0.0F,
                0.0F, 1.0F, 0.0F, 0.0F,
                0.0F, 0.0F, 1.0F, 0.0F,
                0.0F, 0.0F, 0.0F, 1.0F
        };

        for (int perspective = 0; perspective <= 2; perspective++) {
            boolean nativeFront = CameraPresentationPolicy.nativeFrontRotationApplied(perspective);
            CameraFrame frame = frame(perspective, 0.5F,
                    nativeFront ? frontView : body);
            float[] submitted = submitQuaternionView(frame, false, false, false, nativeFront);
            float[] nativePrefix = nativeFront ? nativeFrontHalfTurn : identity;
            assertMatrixEquals(frame.viewBasis().openGlViewMatrix(),
                    multiply(nativePrefix, submitted),
                    "vanilla perspective " + perspective
                            + " must compose to its authoritative frame view");
        }
    }

    @Test
    void internalFreeLookFrontKeepsItsQuaternionWhenBuiltInModeUsesRearPerspective() {
        CameraAttitude body = cameraAttitude(FlightAttitude.fromMinecraftDegrees(
                -28.0D, 73.0D, 11.0D));
        CameraAttitude internalFrontView = body.multiply(CameraAttitude.axisAngle(
                new CameraVector(0.0D, 1.0D, 0.0D), Math.PI));
        CameraFrame frame = frame(3L, 0.5F, internalFrontView);

        assertFalse(CameraPresentationPolicy.nativeFrontRotationApplied(1));
        float[] submitted = submitQuaternionView(frame, false, true, false, false);

        assertMatrixEquals(frame.viewBasis().openGlViewMatrix(), submitted,
                "internal FreeLook front is captured in Q after UIE enters perspective 1");
    }

    private static void assertRenderOnlySequence(String label, double tickPitch,
                                                 double tickYaw, double tickRoll,
                                                 double[][] renderAngles,
                                                 float[] partialTicks) {
        FlightAttitude tickAttitude = FlightAttitude.fromMinecraftDegrees(tickPitch,
                tickYaw, tickRoll);
        FlightEulerAngles playerPose = tickAttitude.toMinecraftEuler(
                tickPitch, tickYaw, tickRoll);
        double renderReferencePitch = playerPose.pitchDegrees;
        double renderReferenceYaw = playerPose.yawDegrees;
        double renderReferenceRoll = playerPose.rollDegrees;
        float[] previousSubmittedMatrix = null;
        FlightEulerAngles previousRenderPose = null;
        double maximumSubmittedBasisStep = 0.0D;
        double maximumOldEulerError = 0.0D;
        double maximumEulerComponentStep = 0.0D;

        for (int index = 0; index < renderAngles.length; index++) {
            FlightAttitude renderAttitude = FlightAttitude.fromMinecraftDegrees(
                    renderAngles[index][0], renderAngles[index][1], renderAngles[index][2]);
            CameraAttitude requestedView = cameraAttitude(renderAttitude);
            CameraFrame frame = frame(index, partialTicks[index], requestedView);

            // Simulate several mouse-only render samples while tick yaw/pitch stay frozen. The
            // partial-tick reset makes the sample stream asynchronous across a tick boundary.
            float[] submittedMatrix = submitQuaternionView(frame, false, false, false);
            assertRenderBasis(frame, submittedMatrix);
            if (previousSubmittedMatrix != null) {
                maximumSubmittedBasisStep = Math.max(maximumSubmittedBasisStep,
                        matrixAngularDistance(previousSubmittedMatrix, submittedMatrix));
            }

            FlightEulerAngles renderPose = renderAttitude.toMinecraftEuler(
                    renderReferencePitch, renderReferenceYaw, renderReferenceRoll);
            CameraAttitude branchSelectedView = CameraAttitude.fromMinecraftDegrees(
                    renderPose.pitchDegrees, renderPose.yawDegrees, renderPose.rollDegrees);
            assertTrue(requestedView.angularDistance(branchSelectedView) < 1.0E-6D,
                    "nearest Euler branch must preserve the sampled quaternion");
            if (previousRenderPose != null) {
                maximumEulerComponentStep = Math.max(maximumEulerComponentStep,
                        maxComponentStep(previousRenderPose, renderPose));
            }
            renderReferencePitch = renderPose.pitchDegrees;
            renderReferenceYaw = renderPose.yawDegrees;
            renderReferenceRoll = renderPose.rollDegrees;

            // This is the legacy UIE behavior: vanilla player yaw/pitch are still the tick
            // sample, while FlightRollController contributes only this render's roll.
            CameraAttitude legacyEventView = CameraAttitude.fromMinecraftDegrees(
                    playerPose.pitchDegrees, playerPose.yawDegrees, renderPose.rollDegrees);
            double legacyError = requestedView.angularDistance(legacyEventView);
            maximumOldEulerError = Math.max(maximumOldEulerError, legacyError);
            if (label.equals("positive") && renderAngles[index][0] == 90.25D) {
                // Preserve the original behavior RED: with tick yaw/pitch held at 89.75°, the
                // old Euler event path misses the mouse-only 90.25° render sample by 0.5°.
                assertEquals(Math.toRadians(0.5D), legacyError, 1.0E-8D);
            }
            previousSubmittedMatrix = submittedMatrix;
            previousRenderPose = renderPose;
        }

        assertTrue(maximumSubmittedBasisStep <= Math.toRadians(3.0D),
                label + " submitted basis angular step was "
                        + Math.toDegrees(maximumSubmittedBasisStep) + " degrees");
        assertTrue(maximumEulerComponentStep <= 3.0D,
                label + " nearest Euler branch component step was "
                        + maximumEulerComponentStep + " degrees");
        assertTrue(maximumOldEulerError >= Math.toRadians(0.5D),
                label + " legacy tick/render path should show the preserved mismatch");
        System.out.printf("[G-POLE] %s maxSubmittedBasisStep=%.6fdeg, "
                        + "maxNearestEulerComponentStep=%.6fdeg, maxLegacyTickRenderError=%.6fdeg%n",
                label, Math.toDegrees(maximumSubmittedBasisStep), maximumEulerComponentStep,
                Math.toDegrees(maximumOldEulerError));
    }

    private static float[] submitQuaternionView(CameraFrame frame, boolean shoulder,
                                                boolean lookCamera, boolean drone) {
        return submitQuaternionView(frame, shoulder, lookCamera, drone, false);
    }

    private static float[] submitQuaternionView(CameraFrame frame, boolean shoulder,
                                                boolean lookCamera, boolean drone,
                                                boolean nativeFrontRotationApplied) {
        EntityViewRenderEvent.CameraSetup event = cameraSetup(215.0F, 23.0F, -14.0F);
        float[][] submitted = {null};

        assertTrue(CameraRenderBridge.submitQuaternionView(event, frame, shoulder,
                lookCamera, drone, nativeFrontRotationApplied,
                matrix -> submitted[0] = matrix.clone()));
        assertEquals(0.0F, event.getPitch());
        assertEquals(0.0F, event.getYaw());
        assertEquals(0.0F, event.getRoll());
        return submitted[0];
    }

    private static EntityViewRenderEvent.CameraSetup cameraSetup(float yaw,
                                                                  float pitch,
                                                                  float roll) {
        return new EntityViewRenderEvent.CameraSetup(null, null, null, 0.0D,
                yaw, pitch, roll);
    }

    private static CameraFrame frame(long sampleId, float partialTicks,
                                     CameraAttitude attitude) {
        return frame(sampleId, partialTicks, attitude, false);
    }

    private static CameraFrame frame(long sampleId, float partialTicks,
                                     CameraAttitude attitude, boolean vanillaPassThrough) {
        CameraVector origin = new CameraVector(0.0D, 0.0D, 0.0D);
        return new CameraFrame(sampleId, partialTicks, attitude, attitude, origin,
                origin, vanillaPassThrough);
    }

    private static CameraAttitude cameraAttitude(FlightAttitude attitude) {
        return new CameraAttitude(attitude.x, attitude.y, attitude.z, attitude.w);
    }

    private static void assertRenderBasis(CameraFrame frame, float[] matrix) {
        assertEquals(16, matrix.length);
        assertMapsTo(matrix, frame.viewBasis().forward(), 0.0D, 0.0D, -1.0D);
        assertMapsTo(matrix, frame.viewBasis().right(), 1.0D, 0.0D, 0.0D);
        assertMapsTo(matrix, frame.viewBasis().up(), 0.0D, 1.0D, 0.0D);
    }

    private static void assertMapsTo(float[] matrix, CameraVector world,
                                     double x, double y, double z) {
        assertEquals(x, matrix[0] * world.x + matrix[4] * world.y + matrix[8] * world.z,
                MATRIX_EPSILON);
        assertEquals(y, matrix[1] * world.x + matrix[5] * world.y + matrix[9] * world.z,
                MATRIX_EPSILON);
        assertEquals(z, matrix[2] * world.x + matrix[6] * world.y + matrix[10] * world.z,
                MATRIX_EPSILON);
    }

    private static double matrixAngularDistance(float[] from, float[] to) {
        double trace = 0.0D;
        for (int column = 0; column < 3; column++) {
            for (int row = 0; row < 3; row++) {
                int offset = column * 4 + row;
                trace += from[offset] * to[offset];
            }
        }
        return Math.acos(Math.max(-1.0D, Math.min(1.0D, (trace - 1.0D) * 0.5D)));
    }

    private static float[] multiply(float[] left, float[] right) {
        float[] result = new float[16];
        for (int column = 0; column < 4; column++) {
            for (int row = 0; row < 4; row++) {
                float value = 0.0F;
                for (int inner = 0; inner < 4; inner++) {
                    value += left[inner * 4 + row] * right[column * 4 + inner];
                }
                result[column * 4 + row] = value;
            }
        }
        return result;
    }

    private static void assertMatrixEquals(float[] expected, float[] actual, String message) {
        assertEquals(expected.length, actual.length);
        for (int index = 0; index < expected.length; index++) {
            assertEquals(expected[index], actual[index], MATRIX_EPSILON,
                    message + " at matrix element " + index);
        }
    }

    private static double maxComponentStep(FlightEulerAngles from, FlightEulerAngles to) {
        return Math.max(Math.abs(to.pitchDegrees - from.pitchDegrees),
                Math.max(Math.abs(to.yawDegrees - from.yawDegrees),
                        Math.abs(to.rollDegrees - from.rollDegrees)));
    }
}
