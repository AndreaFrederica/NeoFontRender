package neofontrender.addons.camera;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CursorAimPoseTest {
    @Test
    void bodyAimKeepsHeadAndBodyTogether() {
        CursorAimPose pose = CursorAimPose.resolve(10.0F, 60.0F, -15.0F, false);

        assertEquals(60.0F, pose.bodyYaw, 1.0E-6F);
        assertEquals(60.0F, pose.headYaw, 1.0E-6F);
        assertEquals(-15.0F, pose.pitch, 1.0E-6F);
    }

    @Test
    void headOnlyAimLeavesBodyStableInsideNaturalRange() {
        CursorAimPose pose = CursorAimPose.resolve(10.0F, 60.0F, 20.0F, true);

        assertEquals(10.0F, pose.bodyYaw, 1.0E-6F);
        assertEquals(60.0F, pose.headYaw, 1.0E-6F);
    }

    @Test
    void bodyOnlyFollowsTheAmountBeyondTheHeadLimit() {
        CursorAimPose pose = CursorAimPose.resolve(10.0F, 130.0F, 0.0F, true);

        assertEquals(55.0F, pose.bodyYaw, 1.0E-6F);
        assertEquals(130.0F, pose.headYaw, 1.0E-6F);
        assertEquals(CursorAimPose.HEAD_YAW_LIMIT, pose.headYaw - pose.bodyYaw, 1.0E-6F);
    }

    @Test
    void wrappedTargetsDoNotSpinTheBodyTheLongWayAround() {
        CursorAimPose pose = CursorAimPose.resolve(170.0F, -170.0F, 0.0F, true);

        assertEquals(170.0F, pose.bodyYaw, 1.0E-6F);
        assertEquals(190.0F, pose.headYaw, 1.0E-6F);
    }
}
