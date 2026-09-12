package neofontrender.addons.camera;

import net.minecraftforge.client.event.EntityViewRenderEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import neofontrender.addons.api.camera.CameraApi;
import neofontrender.addons.api.camera.CameraFrame;
import neofontrender.addons.api.camera.CameraBasis;
import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.BufferUtils;

import java.nio.FloatBuffer;

/** Applies the authoritative view quaternion at Forge's existing camera-angle compatibility seam. */
public final class CameraRenderBridge {
    public static final CameraRenderBridge INSTANCE = new CameraRenderBridge();
    private final FloatBuffer viewMatrix = BufferUtils.createFloatBuffer(16);

    private CameraRenderBridge() {}

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void cameraSetup(EntityViewRenderEvent.CameraSetup event) {
        CameraFrame frame = CameraApi.getFrame((float) event.getRenderPartialTicks());
        if (!CameraApi.isRenderOverrideActive()) return;
        // Shoulder camera: apply offset in camera-local space (vanilla's rotations already
        // baked into GL matrix). anchoredViewTranslation() computes the local-space offset
        // via CameraPresentationTransform.translation(). Do NOT apply quaternion — vanilla's
        // rotations handle orientation. Zero event-post rotations to prevent double-rotation.
        //
        // Drone/Free-look/Cursor-look: apply displacement + the authoritative frame quaternion.
        neofontrender.addons.api.camera.CameraVector translation =
                CameraRuntime.anchoredViewTranslation(frame);
        if (translation != null) {
            GlStateManager.translate((float) translation.x, (float) translation.y,
                    (float) translation.z);
        }
        if (CameraRuntime.usesQuaternionView(frame)) {
            applyQuaternionView(frame);
            event.setPitch(0.0F);
            event.setYaw(0.0F);
            event.setRoll(0.0F);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void cameraFov(EntityViewRenderEvent.FOVModifier event) {
        CameraRuntime.renderedVerticalFov(event.getFOV());
        if (CameraApi.hasLensProviders()) {
            event.setFOV((float) CameraApi.lens((float) event.getRenderPartialTicks())
                    .verticalFovDegrees());
            CameraRuntime.renderedVerticalFov(event.getFOV());
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void renderWorldLast(RenderWorldLastEvent event) {
        CameraRuntime.updateShoulderCrosshairProjection(event.getPartialTicks());
    }

    private void applyQuaternionView(CameraFrame frame) {
        CameraBasis basis = frame.viewBasis();
        viewMatrix.clear();
        viewMatrix.put(basis.openGlViewMatrix());
        viewMatrix.flip();
        GlStateManager.multMatrix(viewMatrix);
    }
}
