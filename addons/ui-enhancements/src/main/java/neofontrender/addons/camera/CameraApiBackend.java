package neofontrender.addons.camera;

import net.minecraft.client.Minecraft;
import neofontrender.addons.api.camera.CameraApi;
import neofontrender.addons.api.camera.CameraAttitude;
import neofontrender.addons.api.camera.CameraFrame;
import neofontrender.addons.api.camera.CameraRigRequest;
import neofontrender.addons.api.camera.CameraSession;
import neofontrender.addons.api.camera.CameraHit;
import neofontrender.addons.api.camera.CameraPickingRequest;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.RayTraceResult;
import neofontrender.addons.api.camera.CameraVector;
import neofontrender.addons.api.camera.CameraShaderCompatibility;
import neofontrender.addons.compat.CameraExternalCompat;

/** Client implementation behind the API facade. It is deliberately not part of api.camera. */
final class CameraApiBackend implements CameraApi.Backend {
    @Override public CameraFrame getFrame(float partialTicks) { return CameraRuntime.frame(partialTicks); }

    @Override public int viewportWidth() {
        return Math.max(1, Math.round(Minecraft.getMinecraft().displayWidth
                * ShaderCameraCompat.resolutionMultiplier()));
    }
    @Override public int viewportHeight() {
        return Math.max(1, Math.round(Minecraft.getMinecraft().displayHeight
                * ShaderCameraCompat.resolutionMultiplier()));
    }
    @Override public double verticalFov() { return CameraRuntime.renderedVerticalFov(); }
    @Override public double farPlane() {
        return Math.max(0.05D, Minecraft.getMinecraft().gameSettings.renderDistanceChunks
                * 16.0D * Math.sqrt(2.0D));
    }
    @Override public CameraShaderCompatibility shaderCompatibility() { return ShaderCameraCompat.type(); }
    @Override public float shaderResolutionMultiplier() { return ShaderCameraCompat.resolutionMultiplier(); }

    @Override public CameraSession acquire(CameraRigRequest request) { return CameraRuntime.acquire(request); }
    @Override public boolean isDroneActive() { return CameraRuntime.isDroneActive(); }
    @Override public boolean isFreeLookActive() { return CameraRuntime.isFreeLookActive(); }
    @Override public boolean isShoulderActive() { return CameraRuntime.isShoulderActive(); }
    @Override public boolean isShoulderLeft() { return CameraRuntime.isShoulderLeft(); }
    @Override public void swapShoulder() { CameraRuntime.swapShoulder(); }
    @Override public boolean isRenderOverrideActive() { return CameraRuntime.isViewOverrideActive(); }
    @Override public void setDronePose(CameraVector position, CameraAttitude attitude) {
        CameraRuntime.setDronePose(position, attitude);
    }
    @Override public void clearDronePose() {
        if (CameraRuntime.isDroneActive()) CameraRuntime.shutdown();
    }
    @Override public String activeRigId() { return CameraRuntime.activeRigId(); }
    @Override public String sessionOwner() {
        return CameraRuntime.activeRigId() == null ? null : "uie:builtin";
    }
    @Override public String failClosedReason() { return CameraExternalCompat.failClosedReason(); }
    @Override public CameraHit pick(CameraPickingRequest request) {
        if (request == null) return null;
        Entity entity = Minecraft.getMinecraft().player;
        RayTraceResult hit = CameraPickingService.traceRay(entity, request.origin(), request.direction(),
                request.distance(), request.includeFluids(), request.includeEntities());
        if (hit == null || hit.hitVec == null) return null;
        return new CameraHit(hit, new CameraVector(hit.hitVec.x, hit.hitVec.y, hit.hitVec.z));
    }

    @Override public void applyFrame(CameraFrame frame, boolean uiViewProxy) {
        CameraRuntime.applyEvaluatedFrame(frame, uiViewProxy);
    }
}
