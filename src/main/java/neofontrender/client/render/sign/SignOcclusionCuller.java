package neofontrender.client.render.sign;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntitySign;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import neofontrender.build.BuildFeatures;
import neofontrender.core.config.NeofontrenderConfig;

import java.util.HashMap;
import java.util.Map;

/**
 * Conservative block-occlusion cache for complete sign TESRs. Expensive ray tests are spread over
 * frames; stale or ambiguous results retain the sign so this optimization cannot create hard pops.
 */
public final class SignOcclusionCuller {
    private static final Map<Long, Record> CACHE = new HashMap<>();
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
        if (BuildFeatures.RENDER_STATS) {
            testedThisFrame = 0;
            culledThisFrame = 0;
            cachedThisFrame = 0;
            budgetMissesThisFrame = 0;
        }
        if (CACHE.size() > 8192) {
            CACHE.clear();
        }
    }

    public static boolean shouldCull(TileEntitySign sign, World world,
                                     double cameraX, double cameraY, double cameraZ) {
        if (!NeofontrenderConfig.signBlockOcclusionCulling() || sign == null || world == null) {
            return false;
        }
        int posX = sign.xCoord;
        int posY = sign.yCoord;
        int posZ = sign.zCoord;
        double dx = posX + 0.5D - cameraX;
        double dy = posY + 0.75D - cameraY;
        double dz = posZ + 0.5D - cameraZ;
        float minDistance = NeofontrenderConfig.signOcclusionMinDistance();
        if (dx * dx + dy * dy + dz * dz < minDistance * minDistance) {
            return false;
        }

        long now = Minecraft.getSystemTime();
        long key = key(posX, posY, posZ);
        Record record = CACHE.get(key);
        long ttl = NeofontrenderConfig.signOcclusionCacheMillis();
        if (record != null && now - record.checkedAt <= ttl
                && record.cameraDistanceSq(cameraX, cameraY, cameraZ) <= 0.25D) {
            if (BuildFeatures.RENDER_STATS) {
                cachedThisFrame++;
                if (record.occluded) culledThisFrame++;
            }
            return record.occluded;
        }
        if (checksRemaining <= 0) {
            if (BuildFeatures.RENDER_STATS) budgetMissesThisFrame++;
            // A stale hidden result is unsafe after camera movement; retain until refreshed.
            return record != null && record.occluded
                    && record.cameraDistanceSq(cameraX, cameraY, cameraZ) <= 0.25D;
        }

        checksRemaining--;
        if (BuildFeatures.RENDER_STATS) testedThisFrame++;
        boolean occluded = testBoard(sign, world, Vec3.createVectorHelper(cameraX, cameraY, cameraZ));
        CACHE.put(key, new Record(occluded, now, cameraX, cameraY, cameraZ, frameToken));
        if (BuildFeatures.RENDER_STATS && occluded) culledThisFrame++;
        return occluded;
    }

    public static String debugLine() {
        return "NFR sign occlusion: tested=" + testedThisFrame + " cached=" + cachedThisFrame
                + " culled=" + culledThisFrame + " budget_miss=" + budgetMissesThisFrame;
    }

    private static boolean testBoard(TileEntitySign sign, World world, Vec3 camera) {
        Block block = sign.getBlockType();
        boolean standing = block == Blocks.standing_sign;
        float rotation = rotationDegrees(standing, sign.getBlockMetadata());
        double radians = Math.toRadians(-rotation);
        double sin = Math.sin(radians);
        double cos = Math.cos(radians);
        double localY = standing ? 0.833333D : 0.520833D;
        double localZ = standing ? 0.046667D : -0.390833D;
        double centerX = sign.xCoord + 0.5D + sin * localZ;
        double centerY = sign.yCoord + localY;
        double centerZ = sign.zCoord + 0.5D + cos * localZ;
        double axisX = cos * 0.46D;
        double axisZ = -sin * 0.46D;
        Vec3[] samples = {
                Vec3.createVectorHelper(centerX, centerY, centerZ),
                Vec3.createVectorHelper(centerX + axisX, centerY + 0.24D, centerZ + axisZ),
                Vec3.createVectorHelper(centerX - axisX, centerY + 0.24D, centerZ - axisZ),
                Vec3.createVectorHelper(centerX + axisX, centerY - 0.24D, centerZ + axisZ),
                Vec3.createVectorHelper(centerX - axisX, centerY - 0.24D, centerZ - axisZ)
        };
        for (Vec3 target : samples) {
            if (!isBlockedByOpaqueCube(world, camera, target, sign.xCoord, sign.yCoord, sign.zCoord)) {
                return false;
            }
        }
        return true;
    }

    private static boolean isBlockedByOpaqueCube(World world, Vec3 start, Vec3 target,
                                                  int signX, int signY, int signZ) {
        Vec3 delta = target.subtract(start);
        double length = Math.sqrt(delta.xCoord * delta.xCoord + delta.yCoord * delta.yCoord
                + delta.zCoord * delta.zCoord);
        if (length <= 0.2D) {
            return false;
        }
        double inverseLength = 1.0D / length;
        Vec3 direction = Vec3.createVectorHelper(
                delta.xCoord * inverseLength,
                delta.yCoord * inverseLength,
                delta.zCoord * inverseLength);
        Vec3 end = target.subtract(Vec3.createVectorHelper(
                direction.xCoord * 0.12D,
                direction.yCoord * 0.12D,
                direction.zCoord * 0.12D));
        Vec3 cursor = start;
        for (int i = 0; i < 24; i++) {
            MovingObjectPosition hit = world.func_147447_a(cursor, end, false, true, false);
            if (hit == null || hit.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK) {
                return false;
            }
            if (hit.blockX == signX && hit.blockY == signY && hit.blockZ == signZ) {
                return false;
            }
            Block block = world.getBlock(hit.blockX, hit.blockY, hit.blockZ);
            if (block.isOpaqueCube()) {
                return true;
            }
            if (hit.hitVec == null || hit.hitVec.squareDistanceTo(end) < 0.0004D) {
                return false;
            }
            // Continue through glass, foliage and partial geometry; only a full opaque cube is a
            // sufficiently stable reason to skip the complete renderer.
            cursor = hit.hitVec.addVector(direction.xCoord * 0.01D, direction.yCoord * 0.01D,
                    direction.zCoord * 0.01D);
        }
        return false;
    }

    private static long key(int x, int y, int z) {
        return ((long) x & 0x3FFFFFFL) << 38 | ((long) z & 0x3FFFFFFL) << 12 | (y & 0xFFFL);
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
