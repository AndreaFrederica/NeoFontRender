package neofontrender.client.render.sign;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntitySign;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import neofontrender.core.config.NeofontrenderConfig;

import java.util.HashMap;
import java.util.Map;

/**
 * Conservative block-occlusion cache for complete sign TESRs. Expensive ray tests are spread over
 * frames; stale or ambiguous results retain the sign so this optimization cannot create hard pops.
 */
public final class SignOcclusionCuller {
    private static final Map<BlockPos, Record> CACHE = new HashMap<>();
    private static World currentWorld;
    private static long frameToken;
    private static int checksRemaining;
    private static int testedThisFrame;
    private static int culledThisFrame;
    private static int cachedThisFrame;
    private static int budgetMissesThisFrame;

    private SignOcclusionCuller() {
    }

    public static void beginFrame(World world) {
        if (world != currentWorld) {
            currentWorld = world;
            CACHE.clear();
        }
        frameToken++;
        checksRemaining = NeofontrenderConfig.signOcclusionChecksPerFrame();
        testedThisFrame = 0;
        culledThisFrame = 0;
        cachedThisFrame = 0;
        budgetMissesThisFrame = 0;
        if (CACHE.size() > 8192) {
            CACHE.clear();
        }
    }

    public static boolean shouldCull(TileEntitySign sign, World world,
                                     double cameraX, double cameraY, double cameraZ) {
        if (!NeofontrenderConfig.signBlockOcclusionCulling() || sign == null || world == null) {
            return false;
        }
        BlockPos pos = sign.getPos();
        double dx = pos.getX() + 0.5D - cameraX;
        double dy = pos.getY() + 0.75D - cameraY;
        double dz = pos.getZ() + 0.5D - cameraZ;
        float minDistance = NeofontrenderConfig.signOcclusionMinDistance();
        if (dx * dx + dy * dy + dz * dz < minDistance * minDistance) {
            return false;
        }

        long now = net.minecraft.client.Minecraft.getSystemTime();
        Record record = CACHE.get(pos);
        long ttl = NeofontrenderConfig.signOcclusionCacheMillis();
        if (record != null && now - record.checkedAt <= ttl
                && record.cameraDistanceSq(cameraX, cameraY, cameraZ) <= 0.25D) {
            cachedThisFrame++;
            if (record.occluded) {
                culledThisFrame++;
            }
            return record.occluded;
        }
        if (checksRemaining <= 0) {
            budgetMissesThisFrame++;
            // A stale hidden result is unsafe after camera movement; retain until refreshed.
            return record != null && record.occluded
                    && record.cameraDistanceSq(cameraX, cameraY, cameraZ) <= 0.25D;
        }

        checksRemaining--;
        testedThisFrame++;
        boolean occluded = testBoard(sign, world, new Vec3d(cameraX, cameraY, cameraZ));
        CACHE.put(pos.toImmutable(), new Record(occluded, now, cameraX, cameraY, cameraZ, frameToken));
        if (occluded) {
            culledThisFrame++;
        }
        return occluded;
    }

    public static String debugLine() {
        return "NFR sign occlusion: tested=" + testedThisFrame + " cached=" + cachedThisFrame
                + " culled=" + culledThisFrame + " budget_miss=" + budgetMissesThisFrame;
    }

    private static boolean testBoard(TileEntitySign sign, World world, Vec3d camera) {
        Block block = sign.getBlockType();
        boolean standing = block == Blocks.STANDING_SIGN;
        float rotation = rotationDegrees(standing, sign.getBlockMetadata());
        double radians = Math.toRadians(-rotation);
        double sin = Math.sin(radians);
        double cos = Math.cos(radians);
        double localY = standing ? 0.833333D : 0.520833D;
        double localZ = standing ? 0.046667D : -0.390833D;
        double centerX = sign.getPos().getX() + 0.5D + sin * localZ;
        double centerY = sign.getPos().getY() + localY;
        double centerZ = sign.getPos().getZ() + 0.5D + cos * localZ;
        double axisX = cos * 0.46D;
        double axisZ = -sin * 0.46D;
        Vec3d[] samples = {
                new Vec3d(centerX, centerY, centerZ),
                new Vec3d(centerX + axisX, centerY + 0.24D, centerZ + axisZ),
                new Vec3d(centerX - axisX, centerY + 0.24D, centerZ - axisZ),
                new Vec3d(centerX + axisX, centerY - 0.24D, centerZ + axisZ),
                new Vec3d(centerX - axisX, centerY - 0.24D, centerZ - axisZ)
        };
        for (Vec3d target : samples) {
            if (!isBlockedByOpaqueCube(world, camera, target, sign.getPos())) {
                return false;
            }
        }
        return true;
    }

    private static boolean isBlockedByOpaqueCube(World world, Vec3d start, Vec3d target,
                                                  BlockPos signPos) {
        Vec3d delta = target.subtract(start);
        double length = Math.sqrt(delta.x * delta.x + delta.y * delta.y + delta.z * delta.z);
        if (length <= 0.2D) {
            return false;
        }
        Vec3d direction = delta.scale(1.0D / length);
        Vec3d end = target.subtract(direction.scale(0.12D));
        Vec3d cursor = start;
        for (int i = 0; i < 24; i++) {
            RayTraceResult hit = world.rayTraceBlocks(cursor, end, false, true, false);
            if (hit == null || hit.typeOfHit != RayTraceResult.Type.BLOCK || hit.getBlockPos() == null) {
                return false;
            }
            BlockPos hitPos = hit.getBlockPos();
            if (hitPos.equals(signPos)) {
                return false;
            }
            IBlockState state = world.getBlockState(hitPos);
            if (state.isOpaqueCube()) {
                return true;
            }
            if (hit.hitVec == null || hit.hitVec.squareDistanceTo(end) < 0.0004D) {
                return false;
            }
            // Continue through glass, foliage and partial geometry; only a full opaque cube is a
            // sufficiently stable reason to skip the complete renderer.
            cursor = hit.hitVec.add(direction.scale(0.01D));
        }
        return false;
    }

    private static float rotationDegrees(boolean standing, int metadata) {
        if (standing) {
            return metadata * 360.0F / 16.0F;
        }
        if (metadata == 2) {
            return 180.0F;
        }
        if (metadata == 4) {
            return 90.0F;
        }
        if (metadata == 5) {
            return -90.0F;
        }
        return 0.0F;
    }

    private static final class Record {
        private final boolean occluded;
        private final long checkedAt;
        private final double cameraX;
        private final double cameraY;
        private final double cameraZ;
        @SuppressWarnings("unused")
        private final long frameToken;

        private Record(boolean occluded, long checkedAt, double cameraX, double cameraY,
                       double cameraZ, long frameToken) {
            this.occluded = occluded;
            this.checkedAt = checkedAt;
            this.cameraX = cameraX;
            this.cameraY = cameraY;
            this.cameraZ = cameraZ;
            this.frameToken = frameToken;
        }

        private double cameraDistanceSq(double x, double y, double z) {
            double dx = x - cameraX;
            double dy = y - cameraY;
            double dz = z - cameraZ;
            return dx * dx + dy * dy + dz * dz;
        }
    }
}
