package neofontrender.addons.camera;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import neofontrender.addons.api.camera.CameraAttitude;
import neofontrender.addons.api.camera.CameraEulerAngles;
import neofontrender.addons.api.camera.CameraFrame;
import neofontrender.addons.api.camera.CameraRigRequest;
import neofontrender.addons.api.camera.CameraSession;
import neofontrender.addons.api.camera.CameraVector;
import neofontrender.addons.api.camera.CameraMeasurement;
import neofontrender.addons.api.camera.CameraProjection;
import neofontrender.addons.api.camera.CameraApi;
import neofontrender.addons.api.camera.CameraCollisionQuery;
import neofontrender.addons.api.camera.CameraViewChangedEvent;
import neofontrender.addons.api.camera.CameraViewChangeReason;
import neofontrender.addons.input.DroneInputGuard;
import neofontrender.addons.input.FreeLookInputGuard;
import neofontrender.addons.compat.CameraExternalCompat;
import neofontrender.addons.api.input.InputAction;
import neofontrender.addons.api.input.InputFrame;
import neofontrender.addons.api.flight.FlightApi;
import neofontrender.addons.api.flight.FlightCameraTracking;
import neofontrender.addons.api.flight.FlightAttitude;
import neofontrender.addons.mixin.AccessorEntityRendererCameraDistance;

/** Client-thread camera state. Rendering and API consumers share its last immutable frame. */
public final class CameraRuntime {
    private static final Minecraft MC = Minecraft.getMinecraft();
    private static final ResourceLocation DRONE_ID = new ResourceLocation(
            "neofontrender_ui_enhancements", "drone");
    private static final ResourceLocation FREE_LOOK_ID = new ResourceLocation(
            "neofontrender_ui_enhancements", "free_look");
    private static final ResourceLocation SHOULDER_ID = new ResourceLocation(
            "neofontrender_ui_enhancements", "shoulder");
    private static long sampleId;
    private static float samplePartialTicks = -1.0F;
    private static boolean sampleValid;
    private static CameraFrame lastFrame = new CameraFrame(0L, 0.0F,
            CameraAttitude.IDENTITY, CameraAttitude.IDENTITY,
            new CameraVector(0.0D, 0.0D, 0.0D), new CameraVector(0.0D, 0.0D, 0.0D), true);
    private static CameraSession activeSession;
    private static DroneInputGuard droneInput;
    private static FreeLookInputGuard freeLookInput;
    private static FreeLookController freeLookController;
    private static CameraVector dronePosition;
    private static CameraVector shoulderPosition;
    private static CameraAttitude droneAttitude;
    private static DroneMotionController droneMotion;
    private static long lastDroneInputNanos;
    private static CameraProxyEntity droneViewEntity;
    private static Entity previousViewEntity;
    private static CameraProxyEntity apiViewEntity;
    private static Entity apiPreviousViewEntity;
    private static int apiPreviousThirdPersonView;
    private static int previousThirdPersonView;
    private static boolean perspectiveOverridden;
    private static int freeLookExpectedPerspective;
    private static boolean freeLookFront;
    private static boolean freeLookControlsPlayer;
    private static boolean shoulderFirstPersonOverride;
    private static float shoulderCrosshairTargetX;
    private static float shoulderCrosshairTargetY;
    private static boolean shoulderCrosshairProjected;
    private static float shoulderSecondaryCrosshairX;
    private static float shoulderSecondaryCrosshairY;
    private static boolean shoulderSecondaryCrosshairProjected;
    private static boolean shoulderTransparencyRenderScope;
    private static CameraVector previousKinematicPosition;
    private static CameraAttitude previousKinematicAttitude;
    private static long previousKinematicNanos;
    private static float renderedVerticalFov = Float.NaN;

    private CameraRuntime() {}

    public static synchronized CameraFrame currentFrame() { return lastFrame; }

    static synchronized void applyEvaluatedFrame(CameraFrame frame, boolean uiViewProxy) {
        if (uiViewProxy && droneViewEntity == null) acquireApiViewProxy(frame);
        else if (!uiViewProxy && apiViewEntity != null) releaseApiViewProxy(frame);
        if (frame != null && droneViewEntity != null) {
            syncDroneViewEntity(frame.position(), frame.viewAttitude());
        }
        if (frame != null && apiViewEntity != null) {
            syncViewEntity(apiViewEntity, frame.position(), frame.viewAttitude());
        }
    }

    static synchronized boolean usesDirectViewProxy() {
        Entity proxy = droneViewEntity != null ? droneViewEntity : apiViewEntity;
        return proxy != null && MC.getRenderViewEntity() == proxy;
    }

    /** Detached rigs store the final origin in their render-view adapter. */
    public static synchronized boolean usesDetachedThirdPersonPresentation() {
        return droneViewEntity != null && MC.getRenderViewEntity() == droneViewEntity
                && MC.gameSettings.thirdPersonView > 0;
    }

    /**
     * Player-anchored rigs provide their own quaternion-relative orbit displacement, while a
     * detached rig already stores the final origin in its adapter. Neither may also receive
     * vanilla's four-block third-person displacement.
     */
    public static synchronized boolean suppressesVanillaThirdPersonDisplacement() {
        return usesDetachedThirdPersonPresentation()
                || ((isShoulderActive() || isFreeLookActive())
                && MC.gameSettings.thirdPersonView > 0);
    }

    /** RenderPlayer normally suppresses the local user when a detached proxy is the view entity. */
    public static synchronized boolean shouldRenderDetachedLocalPlayer() {
        return usesDetachedThirdPersonPresentation() && isDroneActive();
    }

    /**
     * Whether a player-anchored custom rig owns the presentation while the player remains the
     * Minecraft render-view entity. Only RenderPlayer's local-user shortcut is bypassed here;
     * the render-view identity remains the player for movement, picking, and other consumers.
     */
    public static synchronized boolean isPlayerAnchoredCameraActive() {
        return (isShoulderActive() || isFreeLookActive())
                && MC.getRenderViewEntity() == MC.player;
    }

    /** Camera-local GL translation for rigs whose render-view entity remains the player. */
    static synchronized CameraVector anchoredViewTranslation(CameraFrame frame) {
        if (frame == null || (!isShoulderActive() && !isFreeLookActive())) return null;
        return CameraPresentationTransform.translation(frame.viewAttitude(),
                frame.bodyPosition(), frame.position());
    }

    /** Whether vanilla's first-person 0.05-block nudge must be removed at CameraSetup. */
    static synchronized boolean compensatesFirstPersonNudge() {
        return (usesDirectViewProxy() && !usesDetachedThirdPersonPresentation())
                || (isShoulderActive() && shoulderFirstPersonOverride
                && MC.gameSettings.thirdPersonView == 0);
    }

    static synchronized void renderedVerticalFov(float value) {
        if (Float.isFinite(value) && value > 0.0F) renderedVerticalFov = value;
    }

    static synchronized double renderedVerticalFov() {
        return Float.isFinite(renderedVerticalFov) ? renderedVerticalFov
                : MC.gameSettings.fovSetting;
    }

    /** Starts a new render/input sample. All consumers of this sample reuse one immutable frame. */
    public static synchronized void beginSample(float partialTicks) {
        samplePartialTicks = clampPartial(partialTicks);
        sampleValid = false;
        sampleId++;
    }

    public static synchronized CameraFrame frame(float partialTicks) {
        float value = clampPartial(partialTicks);
        if (sampleValid && Math.abs(samplePartialTicks - value) < 1.0E-5F) return lastFrame;
        if (!sampleValid && samplePartialTicks < 0.0F) {
            samplePartialTicks = value;
            sampleId++;
        }
        EntityPlayerSP player = MC.player;
        if (player == null) return lastFrame;
        BodySample bodySample = bodyAttitude(player, value);
        CameraAttitude body = bodySample.attitude;
        CameraAttitude view = droneAttitude != null ? droneAttitude
                : freeLookController != null ? freeLookController.resolve(body) : body;
        if (isFreeLookActive() && freeLookFront) {
            view = view.multiply(CameraAttitude.axisAngle(
                    new CameraVector(0.0D, 1.0D, 0.0D), Math.PI));
        }
        CameraVector bodyPosition = interpolatedEye(player, value);
        CameraVector position;
        CameraVector targetPosition;
        if (isDroneActive()) {
            position = dronePosition == null ? bodyPosition : dronePosition;
            targetPosition = position;
        } else if (isFreeLookActive()) {
            FreeLookCameraRig.Sample sample = FreeLookCameraRig.resolve(
                    player, view, bodyPosition, value);
            position = sample.position;
            targetPosition = sample.target;
        } else if (isShoulderActive()) {
            boolean aiming = CameraPickingService.isAdaptiveAiming(player);
            shoulderFirstPersonOverride = shoulderCrosshairPolicy(aiming).switchToFirstPerson();
            updateBuiltInPresentation();
            ShoulderCameraRig.Sample sample = shoulderFirstPersonOverride
                    ? new ShoulderCameraRig.Sample(bodyPosition, bodyPosition)
                    : ShoulderCameraRig.resolve(player, view, bodyPosition, value);
            position = sample.position;
            targetPosition = sample.target;
            shoulderPosition = position;
        } else {
            if (MC.gameSettings.thirdPersonView > 0) {
                if (MC.gameSettings.thirdPersonView == 2) {
                    view = view.multiply(CameraAttitude.axisAngle(
                            new CameraVector(0.0D, 1.0D, 0.0D), Math.PI));
                }
                FreeLookCameraRig.Sample sample = FreeLookCameraRig.resolveVanilla(
                        player, view, bodyPosition, vanillaThirdPersonDistance(value));
                position = sample.position;
                targetPosition = sample.target;
            } else {
                position = bodyPosition;
                targetPosition = position;
            }
            shoulderPosition = null;
            shoulderFirstPersonOverride = false;
        }
        if (isDroneActive()) syncDroneViewEntity(position, view);
        Kinematics kinematics = sampleKinematics(position, view);
        CameraVector velocity = isDroneActive() && droneMotion != null
                ? droneMotion.velocity() : kinematics.linear;
        lastFrame = new CameraFrame(sampleId, value, body, view,
                bodyPosition, position, targetPosition, velocity, kinematics.angular,
                !isDroneActive() && !isFreeLookActive() && !isShoulderActive()
                        && !bodySample.flightAuthoritative);
        samplePartialTicks = value;
        sampleValid = true;
        return lastFrame;
    }

    public static synchronized CameraSession acquire(CameraRigRequest request) {
        if (request == null) throw new NullPointerException("request");
        if (activeSession != null && activeSession.isActive()) return new RejectedSession();
        boolean drone = DRONE_ID.equals(request.id());
        boolean freeLook = FREE_LOOK_ID.equals(request.id());
        boolean shoulder = SHOULDER_ID.equals(request.id());
        if (!drone && !freeLook && !shoulder) return new RejectedSession();
        if (!CameraExternalCompat.internalCameraAllowed()) return new RejectedSession();
        // Releasing an API-only detached lease first keeps the session restore target from
        // pointing at a stale adapter when a built-in rig takes ownership.
        releaseApiViewProxy();
        invalidateSample();
        resetKinematics();
        if (drone) {
            EntityPlayerSP player = MC.player;
            if (player == null) return new RejectedSession();
            CameraVector position = new CameraVector(player.posX,
                    player.posY + player.getEyeHeight(), player.posZ);
            CameraAttitude attitude = CameraAttitude.fromMinecraftDegrees(
                    player.rotationPitch, player.rotationYaw, 0.0D);
            droneMotion = new DroneMotionController(position, attitude);
            dronePosition = position;
            droneAttitude = attitude;
            lastDroneInputNanos = 0L;
            previousViewEntity = MC.getRenderViewEntity();
            droneViewEntity = new CameraProxyEntity(player.world);
            syncDroneViewEntity(position, attitude);
            MC.setRenderViewEntity(droneViewEntity);
            enterBuiltInPresentation();
            droneInput = DroneInputGuard.enter();
        } else if (freeLook) {
            EntityPlayerSP player = MC.player;
            if (player == null) return new RejectedSession();
            CameraAttitude attitude = bodyAttitude(player, MC.getRenderPartialTicks()).attitude;
            freeLookController = new FreeLookController(attitude);
            freeLookControlsPlayer = FreeLookConfig.controlPlayerByDefault;
            freeLookInput = FreeLookInputGuard.enter(freeLookControlsPlayer);
            freeLookFront = MC.gameSettings.thirdPersonView == 2;
            enterBuiltInPresentation();
            freeLookExpectedPerspective = 1;
        } else if (shoulder) {
            EntityPlayerSP player = MC.player;
            if (player == null) return new RejectedSession();
            CameraAttitude body = bodyAttitude(player, MC.getRenderPartialTicks()).attitude;
            CameraVector anchor = interpolatedEye(player, MC.getRenderPartialTicks());
            ShoulderCameraRig.reset(player, body, anchor);
            shoulderPosition = ShoulderCameraRig.resolve(player, body, anchor,
                    MC.getRenderPartialTicks()).position;
            enterBuiltInPresentation();
        }
        Session session = new Session(request, drone, freeLook, shoulder);
        activeSession = session;
        refreshView(CameraViewChangeReason.MODE_ENTER);
        return session;
    }

    public static synchronized boolean isDroneActive() {
        return activeSession instanceof Session && activeSession.isActive()
                && ((Session) activeSession).drone;
    }

    /** Whether a Drone session is explicitly allowed to pass player interaction input through. */
    public static synchronized boolean isDroneInteractionAllowed() {
        return isDroneActive() && DroneCameraConfig.allowCameraInteraction;
    }

    /** Configuration snapshot used while the modal input context is being constructed. */
    public static synchronized boolean isDroneInteractionSettingEnabled() {
        return DroneCameraConfig.allowCameraInteraction;
    }

    public static synchronized boolean isFreeLookActive() {
        return activeSession instanceof Session && activeSession.isActive()
                && ((Session) activeSession).freeLook;
    }

    static synchronized boolean isFreeLookPerspectiveValid() {
        return CameraPresentationPolicy.freeLookPerspectiveValid(isFreeLookActive(),
                MC.gameSettings.thirdPersonView, freeLookExpectedPerspective);
    }

    public static synchronized boolean isShoulderActive() {
        return activeSession instanceof Session && activeSession.isActive()
                && ((Session) activeSession).shoulder;
    }

    /** Advances tick-owned rig state before render frames interpolate it. */
    static synchronized void advanceCameraTick() {
        EntityPlayerSP player = MC.player;
        if (!isShoulderActive() || player == null) return;
        CameraAttitude body = bodyAttitude(player, 1.0F).attitude;
        ShoulderCameraRig.tick(player, body, interpolatedEye(player, 1.0F));
        invalidateSample();
    }

    static synchronized String activeRigId() {
        if (isDroneActive()) return DRONE_ID.toString();
        if (isFreeLookActive()) return FREE_LOOK_ID.toString();
        if (isShoulderActive()) return SHOULDER_ID.toString();
        return null;
    }

    public static synchronized void swapShoulder() {
        if (!isShoulderActive()) return;
        ShoulderCameraConfig.offsetX = -ShoulderCameraConfig.offsetX;
        ShoulderCameraConfig.save();
        shoulderPosition = null;
        invalidateSample();
    }

    public static synchronized void toggleFreeLookControl() {
        if (!isFreeLookActive()) return;
        freeLookControlsPlayer = !freeLookControlsPlayer;
        if (freeLookInput != null) freeLookInput.close();
        freeLookInput = FreeLookInputGuard.enter(freeLookControlsPlayer);
        invalidateSample();
    }

    public static synchronized boolean freeLookControlsPlayer() {
        return isFreeLookActive() && freeLookControlsPlayer;
    }

    /** Returns the cached scaled-screen offset. Ray tracing is performed at RenderWorldLast. */
    public static synchronized float[] shoulderCrosshairOffset(float partialTicks) {
        if (!isShoulderActive() && !isFreeLookActive()) return null;
        EntityPlayerSP player = MC.player;
        if (player == null) return null;
        boolean aiming = CameraPickingService.isAdaptiveAiming(player);
        ShoulderCrosshairPolicy policy = shoulderCrosshairPolicy(aiming);
        if (!policy.renderPrimary() || !policy.projectPlayerAim()
                || !shoulderCrosshairProjected) return null;
        // The target was sampled for this render frame in RenderWorldLast. Do not use
        // partialTicks as a smoothing factor here: it is a position in the tick, not elapsed
        // time, and this method may be called more than once by compatible HUD renderers.
        return new float[]{shoulderCrosshairTargetX, shoulderCrosshairTargetY};
    }

    /** Player-eye projection used by weapon aiming, independent of Shoulder display policy. */
    public static synchronized float[] playerAimCrosshairOffset(float partialTicks) {
        if ((!isShoulderActive() && !isFreeLookActive()) || !shoulderCrosshairProjected) {
            return null;
        }
        return new float[]{shoulderCrosshairTargetX, shoulderCrosshairTargetY};
    }

    /** Updates player-aim and optional camera-ray projections while world matrices are current. */
    public static synchronized void updateShoulderCrosshairProjection(float partialTicks) {
        EntityPlayerSP player = MC.player;
        if ((!isShoulderActive() && !isFreeLookActive()) || player == null || MC.playerController == null) {
            clearShoulderCrosshairProjection();
            return;
        }
        boolean aiming = CameraPickingService.isAdaptiveAiming(player);
        ShoulderCrosshairPolicy policy = shoulderCrosshairPolicy(aiming);
        if (policy.projectPlayerAim() || aiming) {
            double reach = MC.playerController.getBlockReachDistance();
            if (MC.playerController.extendedReach()) reach = Math.max(reach,
                    MC.playerController.isInCreativeMode() ? 6.0D : 3.0D);
            // A miss is represented by the ray endpoint. Use the configured long endpoint for
            // the projected player-aim marker so camera/player parallax converges to the actual
            // eye-ray direction instead of projecting a short interaction-range segment.
            if (ShoulderCameraConfig.useCustomRaytraceDistance) {
                reach = Math.max(reach, Math.max(0.0D, ShoulderCameraConfig.customRaytraceDistance));
            }
            CameraFrame frame = CameraApi.getFrame(partialTicks);
            RayTraceResult hit = CameraPickingService.tracePlayerRay(player, reach,
                    partialTicks, false);
            boolean miss = hit == null || hit.typeOfHit == RayTraceResult.Type.MISS
                    || hit.hitVec == null;
            CameraVector aimPoint = !miss
                    ? new CameraVector(hit.hitVec.x, hit.hitVec.y, hit.hitVec.z)
                    : frame.bodyPosition().add(frame.bodyBasis().forward().normalize().scale(reach));
            float[] offset = projectedOffset(aimPoint, partialTicks, miss);
            if (offset == null) {
                shoulderCrosshairProjected = false;
            } else {
                shoulderCrosshairTargetX = offset[0];
                shoulderCrosshairTargetY = offset[1];
                shoulderCrosshairProjected = true;
            }
        } else {
            shoulderCrosshairProjected = false;
        }
        if (policy.showSecondaryCameraMarker()) {
            RayTraceResult hit = CameraPickingService.traceCameraRay(player,
                    MC.playerController.getBlockReachDistance(), partialTicks, false);
            float[] offset = projectedOffset(hit, partialTicks);
            shoulderSecondaryCrosshairProjected = offset != null;
            if (offset != null) {
                shoulderSecondaryCrosshairX = offset[0];
                shoulderSecondaryCrosshairY = offset[1];
            }
        } else {
            shoulderSecondaryCrosshairProjected = false;
        }
    }

    public static synchronized boolean shoulderCrosshairDual() {
        EntityPlayerSP player = MC.player;
        return isShoulderActive() && player != null && shoulderCrosshairPolicy(
                CameraPickingService.isAdaptiveAiming(player)).showSecondaryCameraMarker();
    }

    /** Returns the secondary camera-origin marker used by dual Shoulder mode. */
    public static synchronized float[] shoulderSecondaryCrosshairOffset(float partialTicks) {
        return shoulderCrosshairDual() && shoulderSecondaryCrosshairProjected
                ? new float[]{shoulderSecondaryCrosshairX, shoulderSecondaryCrosshairY} : null;
    }

    /** Applies the original per-perspective visibility rule to the UIE crosshair. */
    public static synchronized boolean shoulderCrosshairVisible(float partialTicks) {
        if (!CameraExternalCompat.internalCameraAllowed()) return true;
        if (isShoulderActive() && MC.player != null && !shoulderCrosshairPolicy(
                CameraPickingService.isAdaptiveAiming(MC.player)).renderPrimary()) return false;
        EntityPlayerSP player = MC.player;
        if (player == null) return true;
        int perspective = shoulderFirstPersonOverride ? 0 : isShoulderActive() ? 3
                : isFreeLookActive() ? 3 : MC.gameSettings.thirdPersonView;
        boolean aiming = CameraPickingService.isAdaptiveAiming(player);
        return ShoulderCameraConfig.visibilityRule(perspective)
                .render(MC.objectMouseOver, aiming);
    }

    public static synchronized boolean shoulderPlayerTransparency() {
        if (!isShoulderActive() || shoulderFirstPersonOverride
                || !ShoulderCameraConfig.playerTransparency || MC.player == null) {
            return false;
        }
        CameraFrame sample = frame(MC.getRenderPartialTicks());
        CameraVector local = sample.viewAttitude().conjugate().rotate(
                sample.position().subtract(sample.bodyPosition()));
        double halfWidth = MC.player.width * 0.5D;
        double eye = MC.player.getEyeHeight();
        return Math.abs(local.x) < halfWidth
                && local.y < MC.player.height - eye && local.y > -eye;
    }

    public static synchronized void beginShoulderTransparencyRender() {
        shoulderTransparencyRenderScope = true;
    }

    public static synchronized void endShoulderTransparencyRender() {
        shoulderTransparencyRenderScope = false;
    }

    public static synchronized boolean isShoulderTransparencyRenderActive() {
        return shoulderTransparencyRenderScope;
    }

    public static synchronized float shoulderPlayerAlpha() {
        if (MC.player == null) return 1.0F;
        CameraFrame sample = frame(MC.getRenderPartialTicks());
        CameraVector local = sample.viewAttitude().conjugate().rotate(
                sample.position().subtract(sample.bodyPosition()));
        double halfWidth = Math.max(1.0E-4D, MC.player.width * 0.5D);
        double eye = MC.player.getEyeHeight();
        double verticalLimit = local.y >= 0.0D
                ? Math.max(1.0E-4D, MC.player.height - eye) : Math.max(1.0E-4D, eye);
        double xAlpha = Math.min(1.0D, Math.abs(local.x) / halfWidth);
        double yAlpha = Math.min(1.0D, Math.abs(local.y) / verticalLimit);
        double geometric = Math.max(0.15D, Math.min(1.0D,
                Math.sqrt(xAlpha * xAlpha + yAlpha * yAlpha)));
        double configured = ShoulderCameraConfig.playerTransparencyPercent / 100.0D;
        return (float) Math.max(0.15D, Math.min(1.0D, geometric * configured));
    }

    public static synchronized boolean shoulderSkipPlayerRendering() {
        if (!isShoulderActive() || MC.player == null) return false;
        CameraFrame sample = frame(MC.getRenderPartialTicks());
        double distance = sample.position().subtract(sample.bodyPosition()).length();
        if (shoulderFirstPersonOverride || (ShoulderCameraConfig.keepCameraOutOfHeadMultiplier > 0.0D
                && distance < MC.player.width * ShoulderCameraConfig.keepCameraOutOfHeadMultiplier)) {
            return true;
        }
        return ShoulderCameraConfig.hidePlayerWhenLookingUp
                && ShoulderCameraConfig.hidePlayerWhenLookingUpAngle > 0.0D
                && sample.viewBasis().forward().y >= Math.cos(
                        Math.toRadians(ShoulderCameraConfig.hidePlayerWhenLookingUpAngle));
    }

    static synchronized boolean isShoulderLeft() { return ShoulderCameraConfig.offsetX > 0.0D; }

    /** True when the Forge render boundary must use {@link CameraFrame#viewAttitude()}. */
    public static synchronized boolean isViewOverrideActive() {
        if (isDroneActive() || isFreeLookActive()) return true;
        if (isShoulderActive()) return CameraExternalCompat.internalCameraAllowed();
        EntityPlayerSP player = MC.player;
        return player != null && !lastFrame.isVanillaPassThrough()
                && FlightApi.queryCameraTracking(player, MC.getRenderPartialTicks()) != null
                && CameraExternalCompat.internalCameraAllowed();
    }

    public static synchronized void setDronePose(CameraVector position, CameraAttitude attitude) {
        if (!isDroneActive()) return;
        dronePosition = position;
        droneAttitude = attitude;
        droneMotion = new DroneMotionController(position, attitude);
        syncDroneViewEntity(position, attitude);
        invalidateSample();
    }

    public static synchronized void clearDronePose() {
        if (!isDroneActive() && !isFreeLookActive() && !isShoulderActive()
                && droneViewEntity == null) return;
        dronePosition = null;
        shoulderPosition = null;
        droneAttitude = null;
        droneMotion = null;
        freeLookController = null;
        invalidateSample();
        lastDroneInputNanos = 0L;
        if (droneViewEntity != null && MC.getRenderViewEntity() == droneViewEntity) {
            MC.setRenderViewEntity(previousViewEntity == null ? MC.player : previousViewEntity);
        }
        droneViewEntity = null;
        previousViewEntity = null;
        if (perspectiveOverridden) {
            MC.gameSettings.thirdPersonView = previousThirdPersonView;
            perspectiveOverridden = false;
        }
        freeLookFront = false;
        clearShoulderCrosshairProjection();
        resetKinematics();
    }

    private static ShoulderCrosshairPolicy shoulderCrosshairPolicy(boolean aiming) {
        return ShoulderCrosshairPolicy.resolve(ShoulderCameraConfig.crosshairMode,
                ShoulderCameraConfig.crosshairType(aiming), aiming);
    }

    private static float[] projectedOffset(RayTraceResult hit, float partialTicks) {
        if (hit == null || hit.hitVec == null) return null;
        return projectedOffset(new CameraVector(hit.hitVec.x, hit.hitVec.y, hit.hitVec.z), partialTicks, false);
    }

    private static float[] projectedOffset(CameraVector point, float partialTicks) {
        return projectedOffset(point, partialTicks, false);
    }

    private static float[] projectedOffset(CameraVector point, float partialTicks,
                                            boolean allowOutsideDepth) {
        if (point == null) return null;
        CameraMeasurement measurement = CameraApi.measure(partialTicks);
        CameraProjection projected = measurement.project(point);
        if (projected.visibility() == CameraProjection.Visibility.BEHIND_CAMERA
                || (!allowOutsideDepth
                && projected.visibility() == CameraProjection.Visibility.OUTSIDE_DEPTH_RANGE)
                || projected.visibility() == CameraProjection.Visibility.INVALID) return null;
        net.minecraft.client.gui.ScaledResolution resolution =
                new net.minecraft.client.gui.ScaledResolution(MC);
        int viewportWidth = measurement.lens().width();
        int viewportHeight = measurement.lens().height();
        float sx = resolution.getScaledWidth() / (float) viewportWidth;
        float sy = resolution.getScaledHeight() / (float) viewportHeight;
        return new float[]{(float) ((projected.pixelX() - viewportWidth * 0.5D) * sx),
                (float) ((projected.pixelY() - viewportHeight * 0.5D) * sy)};
    }

    private static void clearShoulderCrosshairProjection() {
        shoulderCrosshairTargetX = 0.0F;
        shoulderCrosshairTargetY = 0.0F;
        shoulderCrosshairProjected = false;
        shoulderSecondaryCrosshairX = 0.0F;
        shoulderSecondaryCrosshairY = 0.0F;
        shoulderSecondaryCrosshairProjected = false;
    }

    public static synchronized void shutdown() {
        if (activeSession != null) activeSession.close();
        clearDronePose();
    }

    static synchronized void releaseApiViewProxy() {
        releaseApiViewProxy(lastFrame);
    }

    private static void releaseApiViewProxy(CameraFrame finalFrame) {
        if (apiViewEntity == null) return;
        if (MC.getRenderViewEntity() == apiViewEntity) {
            MC.setRenderViewEntity(apiPreviousViewEntity == null ? MC.player : apiPreviousViewEntity);
        }
        MC.gameSettings.thirdPersonView = apiPreviousThirdPersonView;
        apiViewEntity = null;
        apiPreviousViewEntity = null;
        refreshView(CameraViewChangeReason.MODE_EXIT, finalFrame);
    }

    /** Called after the legacy mouse event chain; it never mutates player rotation or position. */
    public static synchronized boolean updateViewInput(int originalDeltaX, int originalDeltaY,
                                                        int adjustedDeltaX, int adjustedDeltaY,
                                                        boolean eventCanceled, boolean invertMouse,
                                                        boolean forward, boolean back, boolean left,
                                                        boolean right, boolean up, boolean down,
                                                        long nowNanos) {
        if (!isDroneActive() && !isFreeLookActive()) return false;
        InputFrame routedInput = neofontrender.addons.api.input.InputApi.getFrame(0.0F);
        double seconds = lastDroneInputNanos == 0L ? 0.0D
                : (nowNanos - lastDroneInputNanos) / 1_000_000_000.0D;
        lastDroneInputNanos = nowNanos;
        double roll = routedInput.get(InputAction.CAMERA_ROLL).getAxis();
        if (freeLookController != null) freeLookController.roll(roll, seconds);
        if (droneMotion != null) droneMotion.roll(roll, seconds);
        // In free-look player-control mode, let vanilla's player.turn() handle the mouse.
        // Roll remains a detached-camera action; don't process or zero the mouse deltas here.
        if (isFreeLookActive() && freeLookControlsPlayer) {
            invalidateSample();
            return false;
        }
        int routedDeltaX = neofontrender.addons.input.VanillaInputBridge.resolveCameraDelta(
                originalDeltaX, adjustedDeltaX, eventCanceled,
                routedInput.get(InputAction.CAMERA_LOOK_X),
                routedInput.disposition(InputAction.CAMERA_LOOK_X),
                routedInput.getContext().getFrameSeconds());
        int routedDeltaY = neofontrender.addons.input.VanillaInputBridge.resolveCameraDelta(
                originalDeltaY, adjustedDeltaY, eventCanceled,
                routedInput.get(InputAction.CAMERA_LOOK_Y),
                routedInput.disposition(InputAction.CAMERA_LOOK_Y),
                routedInput.getContext().getFrameSeconds());
        if (droneMotion != null) {
            // Keyboard yaw rotation for drone camera
            int keyboardYaw = 0;
            if (CameraKeyBindings.DRONE_ROTATE_LEFT.isKeyDown()) keyboardYaw -= 1;
            if (CameraKeyBindings.DRONE_ROTATE_RIGHT.isKeyDown()) keyboardYaw += 1;
            int droneLookX = neofontrender.addons.input.VanillaInputBridge.resolveDroneCameraDeltaX(
                    originalDeltaX, adjustedDeltaX, eventCanceled,
                    routedInput.get(InputAction.CAMERA_LOOK_X),
                    routedInput.disposition(InputAction.CAMERA_LOOK_X),
                    routedInput.getContext().getFrameSeconds());
            droneMotion.look(droneLookX + keyboardYaw * 50, routedDeltaY, invertMouse);
        }
        if (freeLookController != null) freeLookController.look(routedDeltaX, routedDeltaY,
                invertMouse, MC.gameSettings.mouseSensitivity);
        if (isFreeLookActive() && MC.renderGlobal != null) {
            // Omnilook invalidates entity display state while the detached view is active.
            MC.renderGlobal.setDisplayListEntitiesDirty();
        }
        if (droneMotion != null) {
            // The vanilla booleans remain a transition fallback for early bootstrap frames.
            InputFrame input = routedInput;
            double x = input.get(InputAction.CAMERA_TRANSLATE_X).getAxis();
            double y = input.get(InputAction.CAMERA_TRANSLATE_Y).getAxis();
            double z = input.get(InputAction.CAMERA_TRANSLATE_Z).getAxis();
            if (x == 0.0D && y == 0.0D && z == 0.0D && input.getSampleId() == 0L) {
                x = (left ? 1.0D : 0.0D) - (right ? 1.0D : 0.0D);
                y = (up ? 1.0D : 0.0D) - (down ? 1.0D : 0.0D);
                z = (forward ? 1.0D : 0.0D) - (back ? 1.0D : 0.0D);
            }
            CameraVector previous = droneMotion.position();
            droneMotion.move(x, y, z, seconds);
            dronePosition = resolveDroneCollision(previous, droneMotion.position());
            if (dronePosition.subtract(droneMotion.position()).length() > 1.0E-7D)
                droneMotion.stop();
            droneMotion.setPosition(dronePosition);
            droneAttitude = droneMotion.attitude();
        }
        invalidateSample();
        return true;
    }

    /** Forge render compatibility boundary. Camera internals never persist these Euler angles. */
    public static synchronized CameraEulerAngles renderEuler(float referencePitch,
                                                              float referenceYaw,
                                                              float referenceRoll) {
        return lastFrame.viewAttitude().toMinecraftEuler(referencePitch, referenceYaw,
                referenceRoll);
    }

    private static float clampPartial(float value) {
        return Float.isFinite(value) ? Math.max(0.0F, Math.min(1.0F, value)) : 0.0F;
    }

    private static CameraVector interpolatedEye(EntityPlayerSP player, float partialTicks) {
        // Match EntityRenderer.orientCamera exactly so CameraFrame and the rendered world share
        // one interpolation anchor. lastTickPos can diverge for locally controlled entities.
        double x = player.prevPosX + (player.posX - player.prevPosX) * partialTicks;
        double y = player.prevPosY + (player.posY - player.prevPosY) * partialTicks
                + player.getEyeHeight();
        double z = player.prevPosZ + (player.posZ - player.prevPosZ) * partialTicks;
        return new CameraVector(x, y, z);
    }

    private static double vanillaThirdPersonDistance(float partialTicks) {
        if (MC.entityRenderer instanceof AccessorEntityRendererCameraDistance) {
            float previous = ((AccessorEntityRendererCameraDistance) MC.entityRenderer)
                    .nfrUi$getThirdPersonDistancePrev();
            return previous + (4.0D - previous) * partialTicks;
        }
        return 4.0D;
    }

    private static Kinematics sampleKinematics(CameraVector position, CameraAttitude attitude) {
        long now = System.nanoTime();
        CameraVector linear = new CameraVector(0.0D, 0.0D, 0.0D);
        CameraVector angular = new CameraVector(0.0D, 0.0D, 0.0D);
        if (previousKinematicPosition != null && previousKinematicAttitude != null
                && previousKinematicNanos != 0L) {
            double dt = (now - previousKinematicNanos) / 1_000_000_000.0D;
            if (dt >= 1.0D / 1000.0D && dt <= 0.25D) {
                linear = position.subtract(previousKinematicPosition).scale(1.0D / dt);
                CameraAttitude delta = attitude.multiply(previousKinematicAttitude.conjugate());
                double w = Math.max(-1.0D, Math.min(1.0D, delta.w));
                if (w < 0.0D) delta = new CameraAttitude(-delta.x, -delta.y, -delta.z, -delta.w);
                double angle = 2.0D * Math.acos(Math.max(-1.0D, Math.min(1.0D, delta.w)));
                double sine = Math.sqrt(Math.max(0.0D, 1.0D - delta.w * delta.w));
                if (sine > 1.0E-8D && angle > 1.0E-8D) {
                    angular = new CameraVector(delta.x / sine, delta.y / sine, delta.z / sine)
                            .scale(angle / dt);
                }
            }
        }
        previousKinematicPosition = position;
        previousKinematicAttitude = attitude;
        previousKinematicNanos = now;
        return new Kinematics(linear, angular);
    }

    private static void resetKinematics() {
        previousKinematicPosition = null;
        previousKinematicAttitude = null;
        previousKinematicNanos = 0L;
    }

    private static void invalidateSample() {
        sampleValid = false;
        sampleId++;
    }

    static synchronized void refreshView(CameraViewChangeReason reason) {
        refreshView(reason, null);
    }

    private static void refreshView(CameraViewChangeReason reason, CameraFrame finalFrame) {
        if (MC.entityRenderer != null) {
            MC.entityRenderer.loadEntityShader(isDroneActive() || isFreeLookActive()
                    || isShoulderActive() || apiViewEntity != null || MC.gameSettings.thirdPersonView != 0
                    ? null : MC.getRenderViewEntity());
        }
        if (MC.renderGlobal != null) MC.renderGlobal.setDisplayListEntitiesDirty();
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(
                new CameraViewChangedEvent(finalFrame == null
                        ? neofontrender.addons.api.camera.CameraApi.getFrame(MC.getRenderPartialTicks())
                        : finalFrame, reason));
    }

    /** Called from the narrow getMouseOver RETURN injection after vanilla has populated its fields. */
    public static synchronized void synchronizePicking(float partialTicks) {
        if (CameraApi.isRenderOverrideActive()) CameraPickingService.synchronize(partialTicks);
    }

    private static void syncDroneViewEntity(CameraVector position, CameraAttitude attitude) {
        syncViewEntity(droneViewEntity, position, attitude);
    }

    private static void syncViewEntity(CameraProxyEntity entity, CameraVector position,
                                       CameraAttitude attitude) {
        if (entity == null || position == null || attitude == null) return;
        CameraEulerAngles angles = attitude.toMinecraftEuler(entity.rotationPitch,
                entity.rotationYaw, 0.0D);
        entity.setCameraPose(position.x, position.y, position.z,
                angles.yawDegrees, angles.pitchDegrees);
    }

    private static void acquireApiViewProxy(CameraFrame frame) {
        if (apiViewEntity != null || frame == null || MC.world == null) return;
        apiPreviousViewEntity = MC.getRenderViewEntity();
        apiPreviousThirdPersonView = MC.gameSettings.thirdPersonView;
        apiViewEntity = new CameraProxyEntity(MC.world);
        syncViewEntity(apiViewEntity, frame.position(), frame.viewAttitude());
        MC.setRenderViewEntity(apiViewEntity);
        MC.gameSettings.thirdPersonView = 0;
        refreshView(CameraViewChangeReason.MODE_ENTER, frame);
    }

    private static CameraVector resolveDroneCollision(CameraVector from, CameraVector to) {
        if (from == null || to == null) return to;
        if (!DroneCameraConfig.collision || MC.world == null) {
            CameraVector external = CameraApi.resolveCollision(
                    new CameraCollisionQuery(from, to, 0.1D));
            return external == null ? to : external;
        }
        Vec3d start = new Vec3d(from.x, from.y, from.z);
        Vec3d end = new Vec3d(to.x, to.y, to.z);
        Vec3d movement = end.subtract(start);
        double length = Math.sqrt(movement.x * movement.x + movement.y * movement.y
                + movement.z * movement.z);
        if (length < 1.0E-8D) return from;
        double allowed = length;
        for (int i = 0; i < 8; i++) {
            Vec3d corner = new Vec3d((i & 1) == 0 ? -0.1D : 0.1D,
                    (i & 2) == 0 ? -0.1D : 0.1D,
                    (i & 4) == 0 ? -0.1D : 0.1D);
            Vec3d rayStart = start.add(corner);
            RayTraceResult hit = MC.world.rayTraceBlocks(rayStart,
                    rayStart.add(movement), false, true, false);
            if (hit != null && hit.hitVec != null) {
                allowed = Math.min(allowed,
                        Math.max(0.0D, rayStart.distanceTo(hit.hitVec) - 0.01D));
            }
        }
        Vec3d resolved = start.add(movement.scale(Math.min(1.0D, allowed / length)));
        CameraVector vanilla = new CameraVector(resolved.x, resolved.y, resolved.z);
        CameraVector external = CameraApi.resolveCollision(new CameraCollisionQuery(from, vanilla, 0.1D));
        return external == null ? vanilla : external;
    }

    /**
     * A built-in proxy represents the final camera origin. Keep third-person presentation so
     * vanilla hides first-person hands; the camera presentation mixin removes only the duplicate
     * third-person displacement. Restore the player's setting when the lease closes.
     */
    private static void enterBuiltInPresentation() {
        if (perspectiveOverridden) return;
        previousThirdPersonView = MC.gameSettings.thirdPersonView;
        MC.gameSettings.thirdPersonView = 1;
        perspectiveOverridden = true;
    }

    private static void updateBuiltInPresentation() {
        if (!perspectiveOverridden) return;
        MC.gameSettings.thirdPersonView = CameraPresentationPolicy.builtInPerspective(
                isShoulderActive(), shoulderFirstPersonOverride);
    }

    /**
     * Flight camera tracking is already a UIE quaternion in the same local-axis convention as
     * CameraAttitude. Reuse it directly so Flight physics/body state and camera measurements
     * cannot disagree because of separate Euler reconstruction.
     */
    private static BodySample bodyAttitude(EntityPlayerSP player, float partialTicks) {
        FlightCameraTracking tracking = FlightApi.queryCameraTracking(player, partialTicks);
        if (tracking == null) {
            double pitch = player.prevRotationPitch
                    + (player.rotationPitch - player.prevRotationPitch) * partialTicks;
            double yaw = player.prevRotationYaw + net.minecraft.util.math.MathHelper.wrapDegrees(
                    player.rotationYaw - player.prevRotationYaw) * partialTicks;
            return new BodySample(CameraAttitude.fromMinecraftDegrees(pitch, yaw, 0.0D), false);
        }
        FlightAttitude attitude = tracking.getAttitude();
        return new BodySample(new CameraAttitude(attitude.x, attitude.y, attitude.z, attitude.w), true);
    }

    private static final class BodySample {
        final CameraAttitude attitude;
        final boolean flightAuthoritative;

        BodySample(CameraAttitude attitude, boolean flightAuthoritative) {
            this.attitude = attitude;
            this.flightAuthoritative = flightAuthoritative;
        }
    }

    private static final class Kinematics {
        final CameraVector linear;
        final CameraVector angular;

        Kinematics(CameraVector linear, CameraVector angular) {
            this.linear = linear;
            this.angular = angular;
        }
    }

    private static final class Session implements CameraSession {
        private final CameraRigRequest request;
        private final boolean drone;
        private final boolean freeLook;
        private final boolean shoulder;
        private boolean closed;

        Session(CameraRigRequest request, boolean drone, boolean freeLook, boolean shoulder) {
            this.request = request;
            this.drone = drone;
            this.freeLook = freeLook;
            this.shoulder = shoulder;
        }

        @Override public synchronized boolean isActive() { return !closed; }

        @Override public synchronized void close() {
            if (closed) return;
            closed = true;
            synchronized (CameraRuntime.class) {
                if (activeSession == this) activeSession = null;
                if (drone || freeLook || shoulder) {
                    clearDronePose();
                    if (droneInput != null) {
                        droneInput.close();
                        droneInput = null;
                    }
                }
                if (freeLook && freeLookInput != null) {
                    freeLookInput.close();
                    freeLookInput = null;
                    freeLookController = null;
                }
                refreshView(CameraViewChangeReason.MODE_EXIT);
            }
        }
    }

    private static final class RejectedSession implements CameraSession {
        @Override public boolean isActive() { return false; }
        @Override public void close() {}
    }
}
