package neofontrender.addons.camera;

/** One routing decision shared by picking, projection and the existing crosshair renderer. */
final class ShoulderCrosshairPolicy {
    private final boolean renderPrimary;
    private final boolean projectPlayerAim;
    private final boolean interactionUsesPlayerRay;
    private final boolean showSecondaryCameraMarker;
    private final boolean switchToFirstPerson;

    private ShoulderCrosshairPolicy(boolean renderPrimary, boolean projectPlayerAim,
                                    boolean interactionUsesPlayerRay,
                                    boolean showSecondaryCameraMarker,
                                    boolean switchToFirstPerson) {
        this.renderPrimary = renderPrimary;
        this.projectPlayerAim = projectPlayerAim;
        this.interactionUsesPlayerRay = interactionUsesPlayerRay;
        this.showSecondaryCameraMarker = showSecondaryCameraMarker;
        this.switchToFirstPerson = switchToFirstPerson;
    }

    static ShoulderCrosshairPolicy resolve(String mode, ShoulderCrosshairType type,
                                            boolean aiming) {
        ShoulderCrosshairType resolvedType = type == null ? ShoulderCrosshairType.ADAPTIVE : type;
        boolean dynamic = resolvedType.dynamic(aiming);
        boolean firstPerson = resolvedType.switchesToFirstPerson(aiming);
        String resolvedMode = ShoulderCameraConfig.normalizeCrosshairMode(mode);
        if ("player".equals(resolvedMode)) {
            return new ShoulderCrosshairPolicy(true, true, true, false, firstPerson);
        }
        if ("dual".equals(resolvedMode)) {
            return new ShoulderCrosshairPolicy(true, true, false, true, firstPerson);
        }
        if ("off".equals(resolvedMode)) {
            return new ShoulderCrosshairPolicy(false, false, dynamic, false, firstPerson);
        }
        return new ShoulderCrosshairPolicy(true, dynamic, dynamic, false, firstPerson);
    }

    boolean renderPrimary() { return renderPrimary; }
    boolean projectPlayerAim() { return projectPlayerAim; }
    boolean interactionUsesPlayerRay() { return interactionUsesPlayerRay; }
    boolean showSecondaryCameraMarker() { return showSecondaryCameraMarker; }
    boolean switchToFirstPerson() { return switchToFirstPerson; }
}
