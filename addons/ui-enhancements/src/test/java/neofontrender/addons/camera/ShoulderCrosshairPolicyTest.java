package neofontrender.addons.camera;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShoulderCrosshairPolicyTest {
    @Test
    void matchesOriginalDynamicAndFirstPersonRules() {
        assertFalse(ShoulderCrosshairType.ADAPTIVE.dynamic(false));
        assertTrue(ShoulderCrosshairType.ADAPTIVE.dynamic(true));
        assertTrue(ShoulderCrosshairType.DYNAMIC.dynamic(false));
        assertFalse(ShoulderCrosshairType.STATIC.dynamic(true));
        assertTrue(ShoulderCrosshairType.STATIC_WITH_1PP.switchesToFirstPerson(true));
        assertTrue(ShoulderCrosshairType.DYNAMIC_WITH_1PP.switchesToFirstPerson(true));
        assertFalse(ShoulderCrosshairType.DYNAMIC_WITH_1PP.switchesToFirstPerson(false));
    }

    @Test
    void appliesVisibilityMatrix() {
        assertTrue(ShoulderCrosshairVisibility.ALWAYS.render(null, false));
        assertFalse(ShoulderCrosshairVisibility.NEVER.render(null, true));
        assertTrue(ShoulderCrosshairVisibility.WHEN_AIMING.render(null, true));
        assertFalse(ShoulderCrosshairVisibility.WHEN_AIMING.render(null, false));
        assertTrue(ShoulderCrosshairVisibility.WHEN_AIMING_OR_IN_RANGE.render(null, true));
    }

    @Test
    void playerModeProjectsPlayerAimAndRoutesInteractionThroughIt() {
        ShoulderCrosshairPolicy policy = ShoulderCrosshairPolicy.resolve(
                "player", ShoulderCrosshairType.STATIC, false);
        assertTrue(policy.renderPrimary());
        assertTrue(policy.projectPlayerAim());
        assertTrue(policy.interactionUsesPlayerRay());
        assertFalse(policy.showSecondaryCameraMarker());
    }

    @Test
    void dualModeKeepsCameraInteractionAndAddsASecondaryMarker() {
        ShoulderCrosshairPolicy policy = ShoulderCrosshairPolicy.resolve(
                "dual", ShoulderCrosshairType.DYNAMIC, false);
        assertTrue(policy.projectPlayerAim());
        assertFalse(policy.interactionUsesPlayerRay());
        assertTrue(policy.showSecondaryCameraMarker());
    }

    @Test
    void cameraModeFollowsUpstreamStaticDynamicRouting() {
        ShoulderCrosshairPolicy staticPolicy = ShoulderCrosshairPolicy.resolve(
                "camera", ShoulderCrosshairType.STATIC, false);
        ShoulderCrosshairPolicy dynamicPolicy = ShoulderCrosshairPolicy.resolve(
                "camera", ShoulderCrosshairType.DYNAMIC, false);
        assertFalse(staticPolicy.projectPlayerAim());
        assertFalse(staticPolicy.interactionUsesPlayerRay());
        assertTrue(dynamicPolicy.projectPlayerAim());
        assertTrue(dynamicPolicy.interactionUsesPlayerRay());
    }

    @Test
    void offModeSuppressesDrawingWithoutChangingTypeBasedInteraction() {
        ShoulderCrosshairPolicy policy = ShoulderCrosshairPolicy.resolve(
                "off", ShoulderCrosshairType.DYNAMIC, false);
        assertFalse(policy.renderPrimary());
        assertFalse(policy.projectPlayerAim());
        assertTrue(policy.interactionUsesPlayerRay());
    }
}
