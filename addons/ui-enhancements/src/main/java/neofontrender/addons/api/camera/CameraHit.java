package neofontrender.addons.api.camera;

import net.minecraft.entity.Entity;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.MovingObjectPosition.MovingObjectType;

/** Stable camera-picking result retaining the native 1.7.10 hit for integrations that need it. */
public final class CameraHit {
    private final MovingObjectPosition nativeResult;
    private final CameraVector position;
    public CameraHit(MovingObjectPosition nativeResult, CameraVector position) {
        this.nativeResult = nativeResult;
        this.position = position != null ? position : nativeResult != null && nativeResult.hitVec != null
                ? new CameraVector(nativeResult.hitVec.xCoord, nativeResult.hitVec.yCoord,
                        nativeResult.hitVec.zCoord)
                : new CameraVector(0.0D, 0.0D, 0.0D);
    }
    public MovingObjectPosition nativeResult() { return nativeResult; }
    public CameraVector position() { return position; }
    public Entity entity() { return nativeResult == null ? null : nativeResult.entityHit; }
    public MovingObjectType type() {
        return nativeResult == null ? MovingObjectType.MISS : nativeResult.typeOfHit;
    }
    public boolean isBlock() { return nativeResult != null && nativeResult.typeOfHit == MovingObjectType.BLOCK; }
    public boolean isEntity() { return nativeResult != null && nativeResult.typeOfHit == MovingObjectType.ENTITY; }
    public boolean isMiss() { return nativeResult == null || nativeResult.typeOfHit == MovingObjectType.MISS; }

    /**
     * 1.7.10 has neither BlockPos nor EnumFacing, so a block hit is reported as the vanilla
     * integer block coordinates plus the raw {@link MovingObjectPosition#sideHit} index.
     * The values are only meaningful while {@link #isBlock()} is true; a miss reports 0/-1.
     */
    public int blockX() { return nativeResult == null ? 0 : nativeResult.blockX; }
    public int blockY() { return nativeResult == null ? 0 : nativeResult.blockY; }
    public int blockZ() { return nativeResult == null ? 0 : nativeResult.blockZ; }
    public int sideHit() { return nativeResult == null ? -1 : nativeResult.sideHit; }
}
