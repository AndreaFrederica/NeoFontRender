package neofontrender.addons.api.camera;

import net.minecraft.client.entity.EntityPlayerSP;

import java.util.Objects;

/** Inputs captured once before UIE's camera rigs and collision queries are evaluated. */
public final class CameraEyeAnchorContext {
    private final EntityPlayerSP player;
    private final long sampleId;
    private final float partialTicks;
    private final CameraVector nativeEye;

    public CameraEyeAnchorContext(EntityPlayerSP player, long sampleId, float partialTicks,
                                  CameraVector nativeEye) {
        this.player = Objects.requireNonNull(player, "player");
        if (sampleId < 0L) throw new IllegalArgumentException("sampleId must be non-negative");
        if (!Float.isFinite(partialTicks) || partialTicks < 0.0F || partialTicks > 1.0F) {
            throw new IllegalArgumentException("partialTicks must be finite and in [0, 1]");
        }
        this.sampleId = sampleId;
        this.partialTicks = partialTicks;
        this.nativeEye = Objects.requireNonNull(nativeEye, "nativeEye");
    }

    public EntityPlayerSP player() { return player; }
    public long sampleId() { return sampleId; }
    public float partialTicks() { return partialTicks; }
    public CameraVector nativeEye() { return nativeEye; }
}
