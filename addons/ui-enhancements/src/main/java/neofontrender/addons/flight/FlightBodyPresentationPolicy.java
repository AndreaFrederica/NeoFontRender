package neofontrender.addons.flight;

import neofontrender.addons.api.flight.FlightAttitude;
import neofontrender.addons.api.flight.FlightBodyPose;

/** Selects who owns the player-model root quaternion and relative head pose. */
public final class FlightBodyPresentationPolicy {
    private FlightBodyPresentationPolicy() {}

    public static Decision resolve(FlightBodyPose pose, boolean elytraFlying) {
        if (pose == null) return Decision.VANILLA;
        // API 9's +Z aircraft basis retains the existing elytra-only renderer contract.
        // MODEL_UP_Y is the explicit upright-player basis and can own the root while standing.
        boolean ownsRoot = pose.basis == FlightBodyPose.Basis.MODEL_UP_Y || elytraFlying;
        return new Decision(pose.attitude, ownsRoot, elytraFlying);
    }

    /** Stable decision ABI used by the installed UIE body-presentation integration. */
    public static final class Decision {
        private static final Decision VANILLA = new Decision(null, false, false);

        private final FlightAttitude rootAttitude;
        private final boolean ownsRootQuaternion;
        private final boolean elytraFlying;

        private Decision(FlightAttitude rootAttitude, boolean ownsRootQuaternion,
                         boolean elytraFlying) {
            this.rootAttitude = rootAttitude;
            this.ownsRootQuaternion = ownsRootQuaternion;
            this.elytraFlying = elytraFlying;
        }

        public boolean ownsRootQuaternion() {
            return ownsRootQuaternion;
        }

        /** Returns the provider quaternion unchanged; basis conversion belongs to the renderer. */
        public FlightAttitude rootAttitude() {
            return rootAttitude;
        }

        public boolean neutralizesRelativeHead() {
            return ownsRootQuaternion;
        }

        public float relativeHeadYawDegrees() {
            return 0.0F;
        }

        public float relativeHeadPitchDegrees() {
            return 0.0F;
        }

        public boolean isElytraFlying() {
            return elytraFlying;
        }
    }
}
