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
import neofontrender.addons.api.camera.CameraRay;

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
        boolean playerRoute = usesPlayerInteractionRay(CameraRuntime.isShoulderActive(),
                policy.interactionUsesPlayerRay(), CameraRuntime.isFreeLookActive(),
                CameraRuntime.freeLookControlsPlayer());
        boolean cameraRoute = !playerRoute;
        if (CameraRuntime.isFreeLookCursorMode()) {
            CursorPick cursor = cursorPick(player, frame, reach,
                    false, false, true, true);
            minecraft.objectMouseOver = cursor.hit;
            minecraft.pointedEntity = cursor.hit != null
                    && cursor.hit.typeOfHit == RayTraceResult.Type.ENTITY
                    ? cursor.hit.entityHit : null;
            CameraRuntime.synchronizeCursorPlayerAim(cursor.aimDirection);
            return;
        }
        RayPlan plan = CameraRuntime.isFreeLookActive() && cameraRoute
                ? shoulderRay(frame, reach, true)
                : CameraRuntime.isShoulderActive() && cameraRoute
                ? shoulderRay(frame, reach, ShoulderCameraConfig.limitPlayerReach)
                : new RayPlan(cameraRoute ? frame.position() : frame.bodyPosition(),
                        cameraRoute ? frame.viewBasis().forward() : frame.bodyBasis().forward(), reach);
        if (plan == null) {
            minecraft.objectMouseOver = null;
            minecraft.pointedEntity = null;
            return;
        }
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
        if (CameraRuntime.isFreeLookCursorMode()) {
            RayPlan plan = cursorInteractionRay(frame, reach);
            if (plan == null) return null;
            return trace(entity, vec(plan.origin), vec(plan.direction), plan.distance,
                    useLiquids, false, true);
        }
        return trace(entity, vec(frame.position()), vec(frame.viewBasis().forward()), reach,
                useLiquids, false, true);
    }

    /** Whether a vanilla block-only ray call must be replaced rather than allowed to fall through. */
    public static boolean overridesInteractionBlockRay(Entity entity) {
        if (!CameraApi.isRenderOverrideActive() || entity == null
                || entity != Minecraft.getMinecraft().player) return false;
        if (CameraRuntime.isFreeLookActive() && CameraRuntime.freeLookControlsPlayer()) return false;
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
        RayPlan plan = CameraRuntime.isFreeLookCursorMode()
                ? cursorInteractionRay(frame, reach)
                : CameraRuntime.isFreeLookActive()
                ? shoulderRay(frame, reach, true)
                : CameraRuntime.isShoulderActive()
                ? shoulderRay(frame, reach, ShoulderCameraConfig.limitPlayerReach)
                : new RayPlan(frame.position(), frame.viewBasis().forward(), reach);
        if (plan == null) return null;
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
        if (CameraRuntime.isFreeLookCursorMode()) {
            RayPlan plan = cursorInteractionRay(frame, reach);
            if (plan == null) return null;
            return trace(entity, vec(plan.origin), vec(plan.direction), plan.distance,
                    useLiquids, false, true);
        }
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
        double maximum = blockingDistance(block, from, reach);
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
        double maximum = blockingDistance(block, from, reach);
        RayTraceResult hit = traceEntity(entity, from, direction, maximum);
        return hit == null ? block : hit;
    }

    /** Camera-aligned ray beginning at the point nearest the player's eyes. */
    static RayPlan shoulderRay(CameraFrame frame, double reach, boolean limitPlayerReach) {
        CameraVector forward = frame.viewBasis().forward().normalize();
        RayPlan visual = new RayPlan(frame.position(), forward, reach);
        if (limitPlayerReach) {
            return constrainToPlayerReach(visual, frame.bodyPosition(), reach);
        }
        double closest = Math.max(0.0D,
                frame.bodyPosition().subtract(frame.position()).dot(forward));
        return new RayPlan(frame.position().add(forward.scale(closest)), forward,
                Math.max(0.0D, reach));
    }

    /**
     * Keeps the screen-space ray intact while restricting interaction to the forward half of
     * the player's reach sphere. Space between a detached camera and the player is never allowed
     * to consume reach or become an interaction target.
     */
    static RayPlan constrainToPlayerReach(RayPlan visual, CameraVector reachCenter,
                                          double reach) {
        if (visual == null || reachCenter == null || !Double.isFinite(reach)
                || reach <= 0.0D) return null;
        CameraVector direction = visual.direction.normalize();
        if (direction.lengthSquared() < 1.0E-12D) return null;
        CameraVector toCenter = reachCenter.subtract(visual.origin);
        double closest = toCenter.dot(direction);
        CameraVector closestPoint = visual.origin.add(direction.scale(closest));
        CameraVector lateral = closestPoint.subtract(reachCenter);
        double remainingSquared = reach * reach - lateral.dot(lateral);
        if (remainingSquared <= 1.0E-12D) return null;
        double end = closest + Math.sqrt(remainingSquared);
        double start = Math.max(0.0D, closest);
        if (end <= start + 1.0E-9D) return null;
        return new RayPlan(visual.origin.add(direction.scale(start)), direction, end - start);
    }

    static boolean usesPlayerInteractionRay(boolean shoulderActive,
                                            boolean shoulderUsesPlayerRay,
                                            boolean freeLookActive,
                                            boolean freeLookControlsPlayer) {
        return freeLookActive && freeLookControlsPlayer
                || shoulderActive && shoulderUsesPlayerRay;
    }

    private static RayPlan cursorRay(CameraFrame frame, double reach) {
        CameraRay ray = CameraRuntime.freeLookCursorRay(frame.partialTicks());
        if (ray == null) return new RayPlan(frame.position(), frame.viewBasis().forward(), reach);
        return new RayPlan(ray.origin(), ray.direction(), reach);
    }

    private static RayPlan cursorInteractionRay(CameraFrame frame, double reach) {
        return constrainToPlayerReach(cursorRay(frame, reach), frame.bodyPosition(), reach);
    }

    /** Resolves the authoritative cursor target and derives only the player's facing from it. */
    private static CursorPick cursorPick(Entity entity, CameraFrame frame, double reach,
                                         boolean stopOnLiquid,
                                         boolean ignoreBlocksWithoutBounds,
                                         boolean returnLastMiss,
                                         boolean includeEntities) {
        RayPlan camera = cursorRay(frame, reach);
        RayPlan interaction = constrainToPlayerReach(camera, frame.bodyPosition(), reach);
        Vec3d from = interaction == null ? null : vec(interaction.origin);
        CameraHit provided = includeEntities && interaction != null
                ? CameraApi.pick(new CameraPickingRequest(
                interaction.origin, interaction.direction, interaction.distance,
                CameraPickingPurpose.PLAYER_INTERACTION, stopOnLiquid, true)) : null;
        RayTraceResult targetHit = provided != null && provided.nativeResult() != null
                ? provided.nativeResult()
                : interaction == null
                ? null
                : includeEntities
                ? trace(entity, from, vec(interaction.direction), interaction.distance, stopOnLiquid,
                        ignoreBlocksWithoutBounds, returnLastMiss)
                : entity.world.rayTraceBlocks(from,
                        from.add(vec(interaction.direction).scale(interaction.distance)), stopOnLiquid,
                        ignoreBlocksWithoutBounds, returnLastMiss);
        CameraVector target = cursorVisualAimTarget(entity, camera, stopOnLiquid,
                includeEntities, CursorLookConfig.aimDistance);
        return new CursorPick(camera, targetHit, cursorAimDirection(frame, camera, target));
    }

    /** Long visual pick used only for pose; interaction reach changes cannot alter this target. */
    private static CameraVector cursorVisualAimTarget(Entity entity, RayPlan camera,
                                                      boolean stopOnLiquid,
                                                      boolean includeEntities,
                                                      double aimDistance) {
        if (entity == null || entity.world == null || camera == null) return null;
        double distance = cursorAimDistance(camera, aimDistance);
        CameraHit provided = CameraApi.pick(new CameraPickingRequest(
                camera.origin, camera.direction, distance,
                CameraPickingPurpose.CROSSHAIR, stopOnLiquid, includeEntities));
        RayTraceResult hit = provided != null && provided.nativeResult() != null
                ? provided.nativeResult()
                : trace(entity, vec(camera.origin), vec(camera.direction), distance,
                        stopOnLiquid, false, true);
        CameraVector visualTarget = hit != null && hit.typeOfHit != RayTraceResult.Type.MISS
                && hit.hitVec != null
                ? new CameraVector(hit.hitVec.x, hit.hitVec.y, hit.hitVec.z) : null;
        return cursorAimTarget(camera, visualTarget, aimDistance);
    }

    static CameraVector cursorAimTarget(RayPlan camera, CameraVector visualTarget,
                                        double missAimDistance) {
        if (visualTarget != null) return visualTarget;
        if (camera == null) return null;
        double distance = cursorAimDistance(camera, missAimDistance);
        return camera.origin.add(camera.direction.scale(distance));
    }

    private static double cursorAimDistance(RayPlan camera, double configuredDistance) {
        double configured = Double.isFinite(configuredDistance)
                ? configuredDistance : camera.distance;
        return Math.max(camera.distance, Math.max(0.0D, configured));
    }

    static CameraVector cursorAimDirection(CameraFrame frame, RayPlan camera,
                                           CameraVector target) {
        CameraVector origin = frame.bodyPosition();
        CameraVector fallback = camera == null ? frame.viewBasis().forward() : camera.direction;
        CameraVector segment = target == null ? fallback
                : target.subtract(origin);
        double length = segment.length();
        return length < 1.0E-9D ? fallback.normalize() : segment.scale(1.0D / length);
    }

    static CameraVector cursorPlayerAimDirection(Entity entity, CameraFrame frame) {
        if (entity == null || entity.world == null || frame == null) return null;
        double distance = playerInteractionReach();
        return cursorPick(entity, frame, distance,
                false, false, true, true).aimDirection;
    }

    static double playerInteractionReach() {
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft.playerController == null) return 5.0D;
        double reach = minecraft.playerController.getBlockReachDistance();
        if (minecraft.playerController.extendedReach()) reach = Math.max(reach,
                minecraft.playerController.isInCreativeMode() ? 6.0D : 3.0D);
        return reach;
    }

    private static double blockingDistance(RayTraceResult hit, Vec3d origin, double fallback) {
        return hit == null || hit.typeOfHit == RayTraceResult.Type.MISS || hit.hitVec == null
                ? fallback : origin.distanceTo(hit.hitVec);
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

    private static final class CursorPick {
        final RayPlan cameraRay;
        final RayTraceResult hit;
        final CameraVector aimDirection;

        CursorPick(RayPlan cameraRay, RayTraceResult hit, CameraVector aimDirection) {
            this.cameraRay = cameraRay;
            this.hit = hit;
            this.aimDirection = aimDirection;
        }
    }
}
