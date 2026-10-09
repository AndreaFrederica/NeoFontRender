package neofontrender.addons.camera;

import net.minecraftforge.client.event.EntityViewRenderEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import neofontrender.addons.api.camera.CameraApi;
import neofontrender.addons.api.camera.CameraFrame;
import neofontrender.addons.api.camera.CameraVector;
import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.BufferUtils;

import java.nio.FloatBuffer;
import java.util.Objects;

/** Applies the authoritative view quaternion at Forge's existing camera-angle compatibility seam. */
public final class CameraRenderBridge {
    public static final CameraRenderBridge INSTANCE = new CameraRenderBridge();
    private final FloatBuffer viewMatrix = BufferUtils.createFloatBuffer(16);

    private CameraRenderBridge() {}

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void cameraSetup(EntityViewRenderEvent.CameraSetup event) {
        CameraRuntime.NativeCameraRenderSample nativeSample =
                CameraRuntime.nativeCameraRenderSample(event.getEntity(), event.getRenderPartialTicks());
        if (nativeSample != null && !nativeSample.renderOverrideActive) return;
        if (nativeSample == null && !CameraApi.isRenderOverrideActive()) return;
        CameraFrame frame = nativeSample == null
                ? CameraApi.getFrame((float) event.getRenderPartialTicks()) : nativeSample.frame;

        if (nativeSample != null && nativeSample.ownsNativeOrigin()) {
            CameraVector translation = nativeSample.originTranslation();
            boolean nativeFrontRotation = CameraPresentationPolicy.nativeFrontRotationApplied(
                    nativeSample.perspective);
            boolean nativeFirstPersonNudge = nativeSample.perspective == 0;
            boolean submitted = submitNativeCameraView(event, frame, nativeFrontRotation,
                    nativeFirstPersonNudge, translation, this::applyQuaternionView,
                    this::translateCamera);
            if (!submitted) {
                throw new IllegalStateException("Captured authoritative native camera frame was "
                        + "not accepted by the CameraSetup quaternion bridge: sample="
                        + frame.sampleId() + ", perspective=" + nativeSample.perspective);
            }
            nativeSample.cameraSetupApplied = true;
            return;
        }

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
        submitQuaternionView(event, frame, CameraRuntime.isShoulderActive(),
                CameraRuntime.isLookCameraActive(), CameraApi.isDroneActive(),
                CameraRuntime.nativeFrontRotationApplied(),
                this::applyQuaternionView);
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

    /**
     * The single CameraSetup handoff shared with focused tests: submit the authoritative
     * world-to-view matrix and neutralize Forge's later Euler rotations as one operation.
     */
    static boolean submitQuaternionView(EntityViewRenderEvent.CameraSetup event,
                                        CameraFrame frame,
                                        boolean shoulderActive,
                                        boolean lookCameraActive,
                                        boolean droneActive,
                                        boolean nativeFrontRotationApplied,
                                        ViewMatrixSubmitter matrixSubmitter) {
        Objects.requireNonNull(event, "event");
        Objects.requireNonNull(frame, "frame");
        Objects.requireNonNull(matrixSubmitter, "matrixSubmitter");
        if (!CameraPresentationPolicy.usesQuaternionView(shoulderActive,
                lookCameraActive, droneActive, !frame.isVanillaPassThrough())) {
            return false;
        }
        float[] viewMatrix = frame.viewBasis().openGlViewMatrix();
        if (nativeFrontRotationApplied) compensateVanillaFrontRotation(viewMatrix);
        matrixSubmitter.submit(viewMatrix);
        event.setPitch(0.0F);
        event.setYaw(0.0F);
        event.setRoll(0.0F);
        return true;
    }

    /**
     * Applies the native-camera prefix correction, exact frame quaternion, and final origin
     * translation in the order required by patched orientCamera's existing modelview stack.
     */
    static boolean submitNativeCameraView(EntityViewRenderEvent.CameraSetup event,
                                          CameraFrame frame,
                                          boolean nativeFrontRotationApplied,
                                          boolean nativeFirstPersonNudgeApplied,
                                          CameraVector originTranslation,
                                          ViewMatrixSubmitter matrixSubmitter,
                                          CameraTranslationSubmitter translationSubmitter) {
        Objects.requireNonNull(originTranslation, "originTranslation");
        Objects.requireNonNull(translationSubmitter, "translationSubmitter");
        if (nativeFirstPersonNudgeApplied) translationSubmitter.translate(0.0D, 0.0D, -0.05D);
        boolean submitted = submitQuaternionView(event, frame, false, false, false,
                nativeFrontRotationApplied, matrixSubmitter);
        if (submitted) {
            translationSubmitter.translate(originTranslation.x, originTranslation.y,
                    originTranslation.z);
        }
        return submitted;
    }

    /**
     * Vanilla front third-person leaves a Y half-turn in the model-view matrix before posting
     * CameraSetup. Left-multiply the submitted world-to-view matrix by F^-1 (F is its own
     * inverse) so the existing model-view prefix F times (F^-1 times Q) resolves to Q.
     */
    private static void compensateVanillaFrontRotation(float[] viewMatrix) {
        for (int column = 0; column < 3; column++) {
            int offset = column * 4;
            viewMatrix[offset] = -viewMatrix[offset];
            viewMatrix[offset + 2] = -viewMatrix[offset + 2];
        }
    }

    @FunctionalInterface
    interface ViewMatrixSubmitter {
        void submit(float[] matrix);
    }

    @FunctionalInterface
    interface CameraTranslationSubmitter {
        void translate(double x, double y, double z);
    }

    private void translateCamera(double x, double y, double z) {
        GlStateManager.translate(x, y, z);
    }

    private void applyQuaternionView(float[] matrix) {
        viewMatrix.clear();
        viewMatrix.put(matrix);
        viewMatrix.flip();
        GlStateManager.multMatrix(viewMatrix);
    }
}
