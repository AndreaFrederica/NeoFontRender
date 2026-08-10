package neofontrender.addons.api.camera;

import net.minecraft.util.ResourceLocation;

/**
 * A complete camera owner. Providers are consulted by descending priority before
 * UIE's built-in rigs, which lets integrations own a mode without touching internals.
 */
public interface CameraProvider {
    ResourceLocation id();
    default int priority() { return 0; }
    default boolean supports(CameraRigRequest request) { return request != null && id().equals(request.id()); }
    CameraSession acquire(CameraRigRequest request, CameraProviderContext context);

    /** Return an authoritative frame while this provider owns a camera, or null to pass. */
    default CameraFrame frame(CameraFrame fallback, float partialTicks) { return null; }

    /** True while this provider's frame must be applied to the rendered view. */
    default boolean ownsView() { return false; }

    /**
     * Requests a UIE-managed client-only render-view proxy for this provider's final frame.
     * Return false when the integration already owns Minecraft's render-view entity. This method
     * is only consulted while {@link #ownsView()} is true and {@link #frame(CameraFrame, float)}
     * returned the authoritative frame selected for the sample.
     */
    default boolean requiresUiViewProxy() { return false; }
}
