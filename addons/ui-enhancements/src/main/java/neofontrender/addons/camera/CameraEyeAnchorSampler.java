package neofontrender.addons.camera;

import net.minecraft.client.entity.EntityPlayerSP;
import neofontrender.addons.api.camera.CameraApi;
import neofontrender.addons.api.camera.CameraEyeAnchor;
import neofontrender.addons.api.camera.CameraEyeAnchorContext;
import neofontrender.addons.api.camera.CameraVector;

import java.util.Objects;

/** Shared pre-rig eye sampling boundary used by CameraRuntime and its real-world tests. */
final class CameraEyeAnchorSampler {
    private CameraEyeAnchorSampler() {}

    static Sample sample(EntityPlayerSP player, long sampleId, float partialTicks) {
        Objects.requireNonNull(player, "player");
        float partial = clampPartial(partialTicks);
        CameraVector nativeEye = nativeEye(player, partial);
        CameraEyeAnchor anchor = CameraApi.sampleEyeAnchor(new CameraEyeAnchorContext(
                player, sampleId, partial, nativeEye));
        return new Sample(nativeEye, anchor);
    }

    static CameraVector nativeEye(EntityPlayerSP player, float partialTicks) {
        double partial = clampPartial(partialTicks);
        return new CameraVector(
                player.prevPosX + (player.posX - player.prevPosX) * partial,
                player.prevPosY + (player.posY - player.prevPosY) * partial + player.getEyeHeight(),
                player.prevPosZ + (player.posZ - player.prevPosZ) * partial);
    }

    private static float clampPartial(float partialTicks) {
        return Float.isFinite(partialTicks) ? Math.max(0.0F, Math.min(1.0F, partialTicks)) : 0.0F;
    }

    static final class Sample {
        private final CameraVector nativeEye;
        private final CameraEyeAnchor acceptedAnchor;

        private Sample(CameraVector nativeEye, CameraEyeAnchor acceptedAnchor) {
            this.nativeEye = nativeEye;
            this.acceptedAnchor = acceptedAnchor;
        }

        CameraVector bodyPosition() {
            return acceptedAnchor == null ? nativeEye : acceptedAnchor.toCameraVector();
        }
        boolean hasAcceptedAnchor() { return acceptedAnchor != null; }
    }
}
