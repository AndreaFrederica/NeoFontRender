package neofontrender.addons.api.camera;

import net.minecraft.entity.Entity;
import net.minecraft.util.math.RayTraceResult;

/** Stable camera-picking result retaining the native 1.12 hit for integrations that need it. */
public final class CameraHit {
    private final RayTraceResult nativeResult;
    private final CameraVector position;
    public CameraHit(RayTraceResult nativeResult, CameraVector position) {
        this.nativeResult = nativeResult;
        this.position = position != null ? position : nativeResult != null && nativeResult.hitVec != null
                ? new CameraVector(nativeResult.hitVec.x, nativeResult.hitVec.y, nativeResult.hitVec.z)
                : new CameraVector(0.0D, 0.0D, 0.0D);
    }
    public RayTraceResult nativeResult() { return nativeResult; }
    public CameraVector position() { return position; }
    public Entity entity() { return nativeResult == null ? null : nativeResult.entityHit; }
    public RayTraceResult.Type type() {
        return nativeResult == null ? RayTraceResult.Type.MISS : nativeResult.typeOfHit;
    }
    public boolean isBlock() { return nativeResult != null && nativeResult.typeOfHit == RayTraceResult.Type.BLOCK; }
    public boolean isEntity() { return nativeResult != null && nativeResult.typeOfHit == RayTraceResult.Type.ENTITY; }
    public boolean isMiss() { return nativeResult == null || nativeResult.typeOfHit == RayTraceResult.Type.MISS; }
    public net.minecraft.util.math.BlockPos blockPosition() {
        return nativeResult == null ? null : nativeResult.getBlockPos();
    }
    public net.minecraft.util.EnumFacing side() {
        return nativeResult == null ? null : nativeResult.sideHit;
    }
}
