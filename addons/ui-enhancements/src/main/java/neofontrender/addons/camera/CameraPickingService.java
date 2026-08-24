package neofontrender.addons.camera;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.Entity;
import net.minecraft.util.EntitySelectors;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import neofontrender.addons.api.camera.CameraFrame;
import neofontrender.addons.api.camera.CameraVector;
import neofontrender.addons.api.camera.CameraApi;
import neofontrender.addons.api.camera.CameraHit;
import neofontrender.addons.api.camera.CameraPickingPurpose;
import neofontrender.addons.api.camera.CameraPickingRequest;

import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Camera-frame picking for view overrides. Adapted from the MIT Shoulder Surfing ray strategy,
 * but consumes UIE's quaternion frame instead of entity yaw/pitch or ASM-replaced call sites.
 */
public final class CameraPickingService {
    private static final Logger LOGGER = LogManager.getLogger("UIE Camera Picking");
    private CameraPickingService() {}

    static boolean isAdaptiveAiming(EntityPlayerSP player) { return adaptiveCrosshair(player); }

    static void synchronize(float partialTicks) {
        Minecraft minecraft = Minecraft.getMinecraft();
        EntityPlayerSP player = minecraft.player;
        if (player == null || player.world == null || minecraft.playerController == null) return;
        CameraFrame frame = CameraApi.getFrame(partialTicks);
        if (frame.isVanillaPassThrough()) return;
        if (CameraRuntime.isDroneActive() && !DroneCameraConfig.allowCameraInteraction) {
            minecraft.objectMouseOver = null;
            minecraft.pointedEntity = null;
            return;
        }

        double reach = minecraft.playerController.getBlockReachDistance();
        if (minecraft.playerController.extendedReach()) reach = Math.max(reach,
                minecraft.playerController.isInCreativeMode() ? 6.0D : 3.0D);
        boolean aiming = adaptiveCrosshair(player);
        ShoulderCrosshairPolicy policy = ShoulderCrosshairPolicy.resolve(
                ShoulderCameraConfig.crosshairMode,
                ShoulderCameraConfig.crosshairType(aiming), aiming);
        boolean playerRoute = CameraRuntime.isShoulderActive()
                && policy.interactionUsesPlayerRay();
        boolean cameraRoute = !playerRoute;
        RayPlan plan = CameraRuntime.isShoulderActive() && cameraRoute
                ? shoulderRay(frame, reach, ShoulderCameraConfig.limitPlayerReach)
                : new RayPlan(cameraRoute ? frame.position() : frame.bodyPosition(),
                        cameraRoute ? frame.viewBasis().forward() : frame.bodyBasis().forward(), reach);
        CameraVector originValue = plan.origin;
        CameraVector directionValue = plan.direction;
        Vec3d from = vec(originValue);
        Vec3d direction = vec(directionValue);
        CameraHit provided = CameraApi.pick(new CameraPickingRequest(originValue, directionValue,
                plan.distance, CameraPickingPurpose.PLAYER_INTERACTION, false, true));
        boolean shoulder = CameraRuntime.isShoulderActive();
        RayTraceResult result = provided != null && provided.nativeResult() != null
                ? provided.nativeResult() : trace(player, from, direction, plan.distance,
                        false, shoulder, !shoulder);
        minecraft.objectMouseOver = result;
        minecraft.pointedEntity = result != null && result.typeOfHit == RayTraceResult.Type.ENTITY
                ? result.entityHit : null;
    }

    public static RayTraceResult traceCameraRay(Entity entity, double reach, float partialTicks, boolean useLiquids) {
        if (!CameraApi.isRenderOverrideActive() || entity == null || entity != Minecraft.getMinecraft().player)
            return null;
        CameraFrame frame = CameraApi.getFrame(partialTicks);
        return trace(entity, vec(frame.position()), vec(frame.viewBasis().forward()), reach,
                useLiquids, false, true);
    }

    /** Whether a vanilla block-only ray call must be replaced rather than allowed to fall through. */
    public static boolean overridesInteractionBlockRay(Entity entity) {
        if (!CameraApi.isRenderOverrideActive() || entity == null
                || entity != Minecraft.getMinecraft().player) return false;
        if (!CameraRuntime.isShoulderActive()) return true;
        EntityPlayerSP player = Minecraft.getMinecraft().player;
        boolean aiming = adaptiveCrosshair(player);
        return !ShoulderCrosshairPolicy.resolve(ShoulderCameraConfig.crosshairMode,
                ShoulderCameraConfig.crosshairType(aiming), aiming).interactionUsesPlayerRay();
    }

    /** Block-only item/entity route preserving the exact vanilla rayTraceBlocks flags. */
    public static RayTraceResult traceInteractionBlocks(Entity entity, double reach,
                                                        float partialTicks,
                                                        boolean stopOnLiquid,
                                                        boolean ignoreBlocksWithoutBounds,
                                                        boolean returnLastMiss) {
        if (!overridesInteractionBlockRay(entity) || entity.world == null) return null;
        CameraFrame frame = CameraApi.getFrame(partialTicks);
        RayPlan plan = CameraRuntime.isShoulderActive()
                ? shoulderRay(frame, reach, ShoulderCameraConfig.limitPlayerReach)
                : new RayPlan(frame.position(), frame.viewBasis().forward(), reach);
        Vec3d from = vec(plan.origin);
        Vec3d to = from.add(vec(plan.direction).scale(plan.distance));
        return entity.world.rayTraceBlocks(from, to, stopOnLiquid,
                ignoreBlocksWithoutBounds, returnLastMiss);
    }

    /** Shoulder Surfing's projected dynamic marker ray: player eyes and player look direction. */
    public static RayTraceResult traceShoulderCrosshairRay(Entity entity, double reach,
                                                           float partialTicks, boolean useLiquids) {
        if (!CameraRuntime.isShoulderActive() || entity == null
                || entity != Minecraft.getMinecraft().player) return null;
        return tracePlayerRay(entity, reach, partialTicks, useLiquids);
    }

    public static RayTraceResult tracePlayerRay(Entity entity, double reach, float partialTicks,
                                                boolean useLiquids) {
        if (!CameraApi.isRenderOverrideActive() || entity == null
                || entity != Minecraft.getMinecraft().player) return null;
        CameraFrame frame = CameraApi.getFrame(partialTicks);
        return trace(entity, vec(frame.bodyPosition()), vec(frame.bodyBasis().forward()), reach,
                useLiquids, false, true);
    }

    /** Executes an explicit API ray without substituting the active camera frame. */
    public static RayTraceResult traceRay(Entity entity, CameraVector origin, CameraVector direction,
                                          double reach, boolean useLiquids, boolean includeEntities) {
        if (entity == null || entity.world == null || origin == null || direction == null) return null;
        Vec3d from = vec(origin);
        Vec3d look = vec(direction.normalize());
        Vec3d to = from.add(look.scale(Math.max(0.0D, reach)));
        RayTraceResult block = entity.world.rayTraceBlocks(from, to, useLiquids, false, true);
        if (!includeEntities) return block;
        double maximum = block == null || block.hitVec == null ? reach : from.distanceTo(block.hitVec);
        RayTraceResult hit = traceEntity(entity, from, look, maximum);
        return hit == null ? block : hit;
    }

    private static RayTraceResult trace(Entity entity, Vec3d from, Vec3d direction,
                                        double reach, boolean stopOnLiquid,
                                        boolean ignoreBlocksWithoutBounds,
                                        boolean returnLastMiss) {
        Vec3d to = from.add(direction.scale(reach));
        RayTraceResult block = entity.world.rayTraceBlocks(from, to, stopOnLiquid,
                ignoreBlocksWithoutBounds, returnLastMiss);
        double maximum = block == null || block.hitVec == null ? reach : from.distanceTo(block.hitVec);
        RayTraceResult hit = traceEntity(entity, from, direction, maximum);
        return hit == null ? block : hit;
    }

    /** Quaternion form of ShoulderHelper.shoulderSurfingLook from Shoulder Surfing 2.9.6. */
    static RayPlan shoulderRay(CameraFrame frame, double reach, boolean limitPlayerReach) {
        CameraVector forward = frame.viewBasis().forward().normalize();
        CameraVector offset = frame.position().subtract(frame.bodyPosition());
        double parallel = offset.dot(forward);
        CameraVector headOffset = offset.subtract(forward.scale(parallel));
        double distanceSquared = Math.max(0.0D, reach) * Math.max(0.0D, reach);
        double lateralSquared = headOffset.dot(headOffset);
        if (limitPlayerReach && lateralSquared < distanceSquared) distanceSquared -= lateralSquared;
        double distanceFromCamera = Math.sqrt(Math.max(0.0D, distanceSquared))
                + Math.abs(parallel);
        CameraVector origin = frame.bodyPosition().add(headOffset);
        CameraVector end = frame.position().add(forward.scale(distanceFromCamera));
        CameraVector segment = end.subtract(origin);
        return new RayPlan(origin, segment.normalize(), segment.length());
    }

    /** Mirrors Shoulder Surfing's configurable adaptive-item/property predicate. */
    private static boolean adaptiveCrosshair(EntityPlayerSP player) {
        if (player == null) return false;
        ItemStack active = player.getActiveItemStack();
        boolean configured = matches(active, ShoulderCameraConfig.adaptiveUseItems,
                ShoulderCameraConfig.adaptiveUseProperties);
        for (ItemStack held : player.getHeldEquipment()) {
            configured |= matches(held, ShoulderCameraConfig.adaptiveHoldItems,
                    ShoulderCameraConfig.adaptiveHoldProperties);
        }
        return CameraApi.resolveAdaptiveAiming(player, configured);
    }

    private static boolean matches(ItemStack stack, List<String> itemIds, List<String> properties) {
        if (stack == null || stack.isEmpty()) return false;
        Item item = stack.getItem();
        ResourceLocation name = item.getRegistryName();
        if (name != null && itemIds.contains(name.toString())) return true;
        for (String property : properties) {
            try {
                if (item.getPropertyGetter(new ResourceLocation(property)) != null) return true;
            } catch (RuntimeException error) {
                LOGGER.error("Failed to query adaptive camera item property {}", property, error);
                throw error;
            }
        }
        return false;
    }

    private static RayTraceResult traceEntity(Entity cameraEntity, Vec3d from, Vec3d direction,
                                              double maximum) {
        Vec3d to = from.add(direction.scale(maximum));
        Vec3d searchTo = from.add(direction.scale(Math.min(64.0D, maximum)));
        AxisAlignedBB search = new AxisAlignedBB(
                Math.min(from.x, searchTo.x), Math.min(from.y, searchTo.y),
                Math.min(from.z, searchTo.z), Math.max(from.x, searchTo.x),
                Math.max(from.y, searchTo.y), Math.max(from.z, searchTo.z))
                .grow(1.0D);
        List<Entity> candidates = cameraEntity.world.getEntitiesInAABBexcluding(cameraEntity, search,
                entity -> entity != null && EntitySelectors.NOT_SPECTATING.apply(entity)
                        && entity.canBeCollidedWith());
        Entity nearest = null;
        Vec3d nearestHit = null;
        double nearestDistance = maximum;
        for (Entity candidate : candidates) {
            AxisAlignedBB bounds = candidate.getEntityBoundingBox().grow(candidate.getCollisionBorderSize());
            RayTraceResult hit = bounds.calculateIntercept(from, to);
            if (bounds.contains(from)) {
                if (nearestDistance >= 0.0D) {
                    nearest = candidate;
                    nearestHit = hit == null ? from : hit.hitVec;
                    nearestDistance = 0.0D;
                }
            } else if (hit != null) {
                double distance = from.distanceTo(hit.hitVec);
                if (distance < nearestDistance || nearestDistance == 0.0D) {
                    if (candidate != cameraEntity.getRidingEntity() || candidate.canRiderInteract()
                            || nearestDistance == 0.0D) {
                        nearest = candidate;
                        nearestHit = hit.hitVec;
                        nearestDistance = distance;
                    }
                }
            }
        }
        return nearest == null ? null : new RayTraceResult(nearest, nearestHit);
    }

    private static Vec3d vec(CameraVector value) { return new Vec3d(value.x, value.y, value.z); }

    static final class RayPlan {
        final CameraVector origin;
        final CameraVector direction;
        final double distance;

        RayPlan(CameraVector origin, CameraVector direction, double distance) {
            this.origin = origin;
            this.direction = direction;
            this.distance = distance;
        }
    }
}
