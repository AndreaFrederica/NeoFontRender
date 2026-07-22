package neofontrender.addons.tooltips;

import neofontrender.api.arc3d.Arc3DApi;

/** Verifies the Arc3D API supplied by the required Neo Font Render mod. */
final class Arc3DRuntimeSupport {
    private static boolean available;

    private Arc3DRuntimeSupport() {}

    static void verify() {
        try {
            available = Arc3DApi.isAvailable() && Arc3DApi.lerp(0.0F, 1.0F, 0.5F) == 0.5F;
            if (available) {
                TooltipModule.LOGGER.info("Arc3D Core {} is available through Neo Font Render", Arc3DApi.ARC3D_VERSION);
            } else {
                TooltipModule.LOGGER.error("Arc3D Core API reported unavailable; modern tooltips are disabled");
            }
        } catch (LinkageError error) {
            available = false;
            TooltipModule.LOGGER.error("Arc3D Core linkage failed; modern tooltips are disabled", error);
        }
    }

    static boolean isAvailable() {
        return available;
    }
}
