package neofontrender.addons.api.camera;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CameraMeasurementTest {
    @Test
    void projectsAndUnprojectsCenterWithQuaternionBasis() {
        CameraFrame frame = new CameraFrame(9L, 0.0F, CameraAttitude.IDENTITY,
                CameraAttitude.IDENTITY, new CameraVector(0.0D, 0.0D, 0.0D),
                new CameraVector(0.0D, 0.0D, 0.0D), false);
        CameraMeasurement measurement = new CameraMeasurement(frame, 1920, 1080, 90.0D);
        CameraProjection projection = measurement.project(new CameraVector(0.0D, 0.0D, 5.0D));
        assertEquals(CameraProjection.Visibility.VISIBLE, projection.visibility());
        assertEquals(960.0D, projection.pixelX(), 1.0E-8D);
        assertEquals(540.0D, projection.pixelY(), 1.0E-8D);
        CameraRay ray = measurement.screenRay(960.0D, 540.0D);
        assertEquals(1.0D, ray.direction().z, 1.0E-8D);
        assertTrue(measurement.distanceTo(new CameraVector(0.0D, 0.0D, 5.0D)) > 4.9D);
        assertEquals(0.0D, measurement.angularSeparationDegrees(
                new CameraVector(0.0D, 0.0D, 5.0D)), 1.0E-8D);
        assertEquals(45.0D, measurement.bearingDegrees(
                new CameraVector(-5.0D, 0.0D, 5.0D)), 1.0E-8D);
        assertTrue(measurement.isInsideFrustum(new CameraVector(0.0D, 0.0D, 5.0D)));
        float[] matrix = frame.viewBasis().openGlViewMatrix();
        assertEquals(-1.0F, matrix[0], 1.0E-8F);
        assertEquals(1.0F, matrix[5], 1.0E-8F);
        assertEquals(-1.0F, matrix[10], 1.0E-8F);
    }

    @Test
    void appliesNearAndFarDepthClipping() {
        CameraFrame frame = frame(CameraAttitude.IDENTITY);
        CameraMeasurement measurement = new CameraMeasurement(frame,
                new CameraLens(800, 600, 70.0D, 0.5D, 10.0D));

        assertEquals(CameraProjection.Visibility.BEHIND_CAMERA,
                measurement.project(new CameraVector(0.0D, 0.0D, -1.0D)).visibility());
        assertEquals(CameraProjection.Visibility.OUTSIDE_DEPTH_RANGE,
                measurement.project(new CameraVector(0.0D, 0.0D, 0.49D)).visibility());
        assertEquals(CameraProjection.Visibility.VISIBLE,
                measurement.project(new CameraVector(0.0D, 0.0D, 0.5D)).visibility());
        assertEquals(CameraProjection.Visibility.VISIBLE,
                measurement.project(new CameraVector(0.0D, 0.0D, 10.0D)).visibility());
        assertEquals(CameraProjection.Visibility.OUTSIDE_DEPTH_RANGE,
                measurement.project(new CameraVector(0.0D, 0.0D, 10.01D)).visibility());
    }

    @Test
    void screenProjectionRemainsQuaternionNativeAtPitchAndRollBoundaries() {
        CameraAttitude attitude = CameraAttitude.fromMinecraftDegrees(90.0D, 37.0D, 63.0D);
        CameraMeasurement measurement = new CameraMeasurement(frame(attitude),
                new CameraLens(1280, 720, 95.0D, 0.05D, 100.0D));
        CameraVector centerWorld = measurement.viewToWorld(new CameraVector(0.0D, 0.0D, 8.0D));
        CameraProjection center = measurement.project(centerWorld);

        assertEquals(CameraProjection.Visibility.VISIBLE, center.visibility());
        assertEquals(640.0D, center.pixelX(), 1.0E-7D);
        assertEquals(360.0D, center.pixelY(), 1.0E-7D);
        assertTrue(measurement.screenRay(640.0D, 360.0D).direction()
                .dot(attitude.forward()) > 1.0D - 1.0E-10D);

        double pixelX = 947.0D;
        double pixelY = 143.0D;
        CameraRay ray = measurement.screenRay(pixelX, pixelY);
        CameraProjection roundTrip = measurement.project(ray.origin().add(ray.direction().scale(12.0D)));
        assertEquals(CameraProjection.Visibility.VISIBLE, roundTrip.visibility());
        assertEquals(pixelX, roundTrip.pixelX(), 1.0E-7D);
        assertEquals(pixelY, roundTrip.pixelY(), 1.0E-7D);

        CameraVector bearingTarget = measurement.viewToWorld(new CameraVector(-5.0D, 0.0D, 5.0D));
        CameraVector elevationTarget = measurement.viewToWorld(new CameraVector(0.0D, 5.0D, 5.0D));
        assertEquals(45.0D, measurement.bearingDegrees(bearingTarget), 1.0E-7D);
        assertEquals(45.0D, measurement.elevationDegrees(elevationTarget), 1.0E-7D);
    }

    @Test
    void frontFacingQuaternionProjectsNegativeWorldZ() {
        CameraAttitude front = CameraAttitude.axisAngle(
                new CameraVector(0.0D, 1.0D, 0.0D), Math.PI);
        CameraMeasurement measurement = new CameraMeasurement(frame(front), 640, 480, 70.0D);
        CameraProjection projection = measurement.project(new CameraVector(0.0D, 0.0D, -5.0D));

        assertEquals(CameraProjection.Visibility.VISIBLE, projection.visibility());
        assertEquals(320.0D, projection.pixelX(), 1.0E-7D);
        assertEquals(240.0D, projection.pixelY(), 1.0E-7D);
    }

    @Test
    void measuresBoundsHorizonAndRelativePoseFromQuaternionBasis() {
        CameraAttitude attitude = CameraAttitude.axisAngle(
                new CameraVector(0.0D, 0.0D, 1.0D), Math.PI * 0.25D);
        CameraMeasurement measurement = new CameraMeasurement(frame(attitude),
                new CameraLens(800, 600, 90.0D, 0.1D, 100.0D));
        AxisAlignedBounds visible = new AxisAlignedBounds(
                new CameraVector(-1.0D, -1.0D, 4.0D), new CameraVector(1.0D, 1.0D, 6.0D));
        AxisAlignedBounds outside = new AxisAlignedBounds(
                new CameraVector(100.0D, 0.0D, 4.0D), new CameraVector(102.0D, 1.0D, 6.0D));

        assertTrue(measurement.isWithinFrustum(visible));
        assertTrue(measurement.projectBounds(visible).isVisible());
        assertTrue(measurement.projectBounds(visible).width() > 0.0D);
        assertTrue(!measurement.isWithinFrustum(outside));
        CameraHorizon horizon = measurement.horizon();
        assertTrue(horizon.isVisible());
        assertTrue(Math.abs(horizon.angleDegrees()) > 1.0D);
        CameraRelativePose relative = measurement.relativeTo(
                measurement.viewToWorld(new CameraVector(0.0D, 0.0D, 10.0D)));
        assertEquals(10.0D, relative.distance(), 1.0E-8D);
        assertEquals(0.0D, relative.angularSeparationDegrees(), 1.0E-8D);
    }

    private static CameraFrame frame(CameraAttitude attitude) {
        return new CameraFrame(1L, 0.0F, CameraAttitude.IDENTITY, attitude,
                new CameraVector(0.0D, 0.0D, 0.0D),
                new CameraVector(0.0D, 0.0D, 0.0D), false);
    }
}
