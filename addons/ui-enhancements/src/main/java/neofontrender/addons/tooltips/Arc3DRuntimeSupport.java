package neofontrender.addons.tooltips;

import org.lwjgl.Version;
import neofontrender.api.arc3d.Arc3DApi;

/** Verifies that the contained Arc3D core is linked to Cleanroom's host LWJGL. */
final class Arc3DRuntimeSupport {
    private static boolean available;

    private Arc3DRuntimeSupport() {
    }

    static void verify() {
        try {
            // Exercise classes used by the renderer without allocating native resources.
            float midpoint = Arc3DApi.lerp(0.0F, 1.0F, 0.5F);
            int color = Arc3DApi.hsv(200.0F, 0.5F, midpoint, 255);
            // Arc3D's HSV helper intentionally returns RGB without an alpha channel.
            available = midpoint == 0.5F && (color & 0x00FFFFFF) != 0;
            TooltipModule.LOGGER.info(
                    "Arc3D Core {} is available through NFR; using host LWJGL {} (no bundled natives)",
                    Arc3DApi.ARC3D_VERSION, Version.getVersion());
        } catch (Throwable throwable) {
            available = false;
            TooltipModule.LOGGER.error("Arc3D Core failed to initialize; modern tooltips are disabled", throwable);
        }
    }

    static boolean isAvailable() {
        return available;
    }
}
