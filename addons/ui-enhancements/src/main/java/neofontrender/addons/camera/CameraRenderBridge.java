package neofontrender.addons.camera;

import net.minecraftforge.client.event.RenderWorldLastEvent;
import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import neofontrender.addons.api.camera.CameraApi;
import neofontrender.addons.api.camera.CameraBasis;
import neofontrender.addons.api.camera.CameraFrame;
import neofontrender.addons.api.camera.CameraVector;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

import java.nio.FloatBuffer;

/**
 * Applies the authoritative view quaternion at the EntityRenderer camera seam.
 *
 * <p>Forge 1.7.10 never fires {@code EntityViewRenderEvent.CameraSetup} or
 * {@code EntityViewRenderEvent.FOVModifier}, so the EntityRenderer injections call
 * {@link #applyCameraSetup(float)} where Forge 1.12.2 posts CameraSetup and
 * {@link #modifyFov(float, float)} where it posts FOVModifier.
 */
public final class CameraRenderBridge {
    public static final CameraRenderBridge INSTANCE = new CameraRenderBridge();
    private final FloatBuffer viewMatrix = BufferUtils.createFloatBuffer(16);

    private CameraRenderBridge() {}

    /**
     * Applies the anchored camera-local displacement, and the authoritative quaternion when the
     * active rig renders from its own frame. Must run before vanilla applies the render-view
     * entity's pitch/yaw rotation, exactly where Forge 1.12.2 posts CameraSetup.
     *
     * <p>Shoulder camera: only the offset is applied in camera-local space;
     * {@code anchoredViewTranslation()} converts the world displacement with
     * {@code CameraPresentationTransform.translation()}. Drone/Free-look/Cursor-look detach the
     * view, so the authoritative frame quaternion replaces vanilla's Euler orientation instead.
     *
     * @return true when the quaternion replaced vanilla's Euler orientation, so the caller must
     *         skip the render-view entity pitch/yaw {@code glRotatef} pair
     */
    public boolean applyCameraSetup(float renderPartialTicks) {
        if (!CameraApi.isRenderOverrideActive()) return false;
        CameraFrame frame = CameraApi.getFrame(renderPartialTicks);
        CameraVector translation = CameraRuntime.anchoredViewTranslation(frame);
        if (translation != null) {
            GL11.glTranslatef((float) translation.x, (float) translation.y,
                    (float) translation.z);
        }
        if (!CameraPresentationPolicy.usesQuaternionView(CameraRuntime.isShoulderActive(),
                CameraRuntime.isLookCameraActive(), CameraApi.isDroneActive())) return false;
        applyQuaternionView(frame);
        return true;
    }

    /**
     * 1.7.10 seam for the removed FOVModifier event. The EntityRenderer getFOVModifier injection
     * must render the returned value instead of its own result.
     */
    public float modifyFov(float originalFov, float renderPartialTicks) {
        CameraRuntime.renderedVerticalFov(originalFov);
        if (!CameraApi.hasLensProviders()) return originalFov;
        float lensFov = (float) CameraApi.lens(renderPartialTicks).verticalFovDegrees();
        CameraRuntime.renderedVerticalFov(lensFov);
        return lensFov;
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void renderWorldLast(RenderWorldLastEvent event) {
        CameraRuntime.updateShoulderCrosshairProjection(event.partialTicks);
    }

    private void applyQuaternionView(CameraFrame frame) {
        CameraBasis basis = frame.viewBasis();
        viewMatrix.clear();
        viewMatrix.put(basis.openGlViewMatrix());
        viewMatrix.flip();
        GL11.glMultMatrix(viewMatrix);
    }
}
