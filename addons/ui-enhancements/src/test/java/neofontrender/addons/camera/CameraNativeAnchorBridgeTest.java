package neofontrender.addons.camera;

import net.minecraftforge.client.event.EntityViewRenderEvent;
import neofontrender.addons.api.camera.CameraAttitude;
import neofontrender.addons.api.camera.CameraFrame;
import neofontrender.addons.api.camera.CameraVector;
import neofontrender.addons.api.flight.FlightAttitude;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** Behavior regression for the production CameraSetup bridge and patched native consumers. */
class CameraNativeAnchorBridgeTest {
    private static final double EPSILON = 1.0E-5D;
    private static final CameraVector NATIVE_BODY_ORIGIN = new CameraVector(8.25D, 71.0D, -3.5D);

    @Test
    void authoritativeRenderOriginComposesToFramePositionAcrossNativeCameraBranches() {
        double[][] directions = {
                {0.0D, 0.0D, 0.0D}, {0.0D, 90.0D, 0.0D}, {0.0D, 180.0D, 0.0D},
                {0.0D, 270.0D, 0.0D}, {90.0D, 0.0D, 0.0D}, {-90.0D, 0.0D, 0.0D},
                {90.0D, 137.0D, 31.0D}, {-90.0D, 273.0D, -47.0D},
                {38.0D, -42.0D, 19.0D}, {-71.0D, 133.0D, -26.0D}
        };
        float[] effectiveEyeHeights = {1.62F, 1.78F, 2.62F};
        double maximumTranslationError = 0.0D;
        String largestCase = "";

        for (int perspective = 0; perspective <= 2; perspective++) {
            for (int attitudeIndex = 0; attitudeIndex < directions.length; attitudeIndex++) {
                CameraAttitude attitude = attitude(directions[attitudeIndex]);
                for (float effectiveEyeHeight : effectiveEyeHeights) {
                    CameraVector acceptedEye = NATIVE_BODY_ORIGIN.add(
                            new CameraVector(0.35D, effectiveEyeHeight + 0.22D, -0.4D));
                    CameraVector framePosition = acceptedEye.add(new CameraVector(
                            0.65D, -0.3D, 0.9D));
                    CameraFrame frame = new CameraFrame(4L, 0.63F, attitude, attitude,
                            acceptedEye, framePosition, false);

                    boolean nativeAnchor = CameraPresentationPolicy.usesNativeCameraAnchor(
                            true, !frame.isVanillaPassThrough(), false, false);
                    double nativeThirdPersonDistance = perspective == 0 ? 0.0D
                            : CameraPresentationPolicy.vanillaThirdPersonDistance(
                                    4.0D, nativeAnchor && perspective > 0);
                    CameraVector originTranslation = CameraPresentationPolicy.nativeCameraOriginTranslation(
                            NATIVE_BODY_ORIGIN, effectiveEyeHeight, framePosition);
                    List<float[]> operations = submitFromProductionCameraSetup(frame,
                            perspective == 2, perspective == 0, originTranslation);

                    // RenderManager/terrain inputs are X-B_last; orientCamera supplies its
                    // native camera prefix and -f3Y tail but does not issue a GL T(-B).
                    float[] actual = multiply(nativePrefix(perspective),
                            translation(0.0D, 0.0D, -nativeThirdPersonDistance));
                    for (float[] operation : operations) actual = multiply(actual, operation);
                    actual = multiply(actual, translation(0.0D, -effectiveEyeHeight, 0.0D));
                    float[] expected = multiply(frame.viewBasis().openGlViewMatrix(),
                            translation(NATIVE_BODY_ORIGIN.x - framePosition.x,
                                    NATIVE_BODY_ORIGIN.y - framePosition.y,
                                    NATIVE_BODY_ORIGIN.z - framePosition.z));
                    double error = maxMatrixError(expected, actual);
                    if (error > maximumTranslationError) {
                        maximumTranslationError = error;
                        largestCase = "perspective=" + perspective
                                + ", attitude=" + attitudeIndex
                                + ", effectiveEyeHeight=" + effectiveEyeHeight;
                    }
                }
            }
        }

        assertTrue(maximumTranslationError <= EPSILON,
                "patched orientCamera plus RenderManager-relative coordinates must render "
                        + "Q*T(B_last-frame.position); production CameraSetup bridge differs by "
                        + maximumTranslationError + " at " + largestCase);
    }

    @Test
    void activeRenderInfoCorrectionUsesLastTickBaselineAndRetainsNearPlaneBias() {
        double partialTicks = 0.63D;
        CameraVector previousPosition = new CameraVector(10.2D, 68.9D, -5.4D);
        CameraVector lastTickPosition = new CameraVector(10.7D, 69.5D, -4.8D);
        CameraVector currentPosition = new CameraVector(11.3D, 70.2D, -4.1D);
        CameraVector previousBase = CameraPresentationPolicy.interpolateNativePosition(
                previousPosition, currentPosition, partialTicks);
        CameraVector renderBase = CameraPresentationPolicy.interpolateNativePosition(
                lastTickPosition, currentPosition, partialTicks);
        CameraVector framePosition = new CameraVector(10.95D, 71.42D, -4.51D);
        CameraVector nearPlaneBias = new CameraVector(0.031D, -0.017D, 0.083D);

        CameraVector activeRenderInfoBeforeCorrection = framePosition
                .add(previousBase.subtract(renderBase)).add(nearPlaneBias);
        CameraVector actual = CameraPresentationPolicy.adjustActiveRenderInfoOrigin(
                activeRenderInfoBeforeCorrection, previousBase, renderBase);
        CameraVector expected = framePosition.add(nearPlaneBias);

        assertTrue(renderBase.subtract(previousBase).length() > 0.1D,
                "the fixture must preserve the observed prevPos/lastTickPos divergence");
        assertTrue(actual.subtract(expected).length() <= EPSILON,
                "ActiveRenderInfo must keep its near-plane bias while moving from prevPos to "
                        + "the exact lastTickPos render baseline; expected=" + expected
                        + ", actual=" + actual);
        CameraVector unchanged = CameraPresentationPolicy.adjustActiveRenderInfoOrigin(
                activeRenderInfoBeforeCorrection, previousBase, previousBase);
        assertTrue(unchanged.subtract(activeRenderInfoBeforeCorrection).length() <= EPSILON,
                "matching native interpolation baselines must leave vanilla ActiveRenderInfo intact");
    }

    private static List<float[]> submitFromProductionCameraSetup(CameraFrame frame,
                                                                  boolean nativeFrontRotation,
                                                                  boolean nativeFirstPersonNudge,
                                                                  CameraVector originTranslation) {
        EntityViewRenderEvent.CameraSetup event = new EntityViewRenderEvent.CameraSetup(
                null, null, null, 0.0D, 0.0F, 0.0F, 0.0F);
        List<float[]> operations = new ArrayList<>();
        assertTrue(CameraRenderBridge.submitNativeCameraView(event, frame,
                nativeFrontRotation, nativeFirstPersonNudge, originTranslation,
                matrix -> operations.add(matrix.clone()),
                (x, y, z) -> operations.add(translation(x, y, z))));
        return operations;
    }

    /** orientCamera's front half-turn and first-person nudge precede CameraSetup. */
    private static float[] nativePrefix(int perspective) {
        if (perspective == 2) return rotationY(Math.PI);
        if (perspective == 0) return translation(0.0D, 0.0D, 0.05D);
        return identity();
    }

    private static CameraAttitude attitude(double[] angles) {
        FlightAttitude flight = FlightAttitude.fromMinecraftDegrees(
                angles[0], angles[1], angles[2]);
        return new CameraAttitude(flight.x, flight.y, flight.z, flight.w);
    }

    private static float[] identity() {
        return new float[]{1.0F, 0.0F, 0.0F, 0.0F,
                0.0F, 1.0F, 0.0F, 0.0F,
                0.0F, 0.0F, 1.0F, 0.0F,
                0.0F, 0.0F, 0.0F, 1.0F};
    }

    private static float[] rotationY(double radians) {
        float[] result = identity();
        result[0] = (float) Math.cos(radians);
        result[2] = (float) -Math.sin(radians);
        result[8] = (float) Math.sin(radians);
        result[10] = (float) Math.cos(radians);
        return result;
    }

    private static float[] translation(double x, double y, double z) {
        float[] result = identity();
        result[12] = (float) x;
        result[13] = (float) y;
        result[14] = (float) z;
        return result;
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

    private static double maxMatrixError(float[] expected, float[] actual) {
        double maximum = 0.0D;
        for (int index = 0; index < expected.length; index++) {
            maximum = Math.max(maximum, Math.abs(expected[index] - actual[index]));
        }
        return maximum;
    }
}
