package neofontrender.addons.camera;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.MovingObjectPosition.MovingObjectType;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Vec3;
import net.minecraftforge.common.MinecraftForge;
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
import neofontrender.addons.api.camera.CameraRay;
import neofontrender.addons.input.DroneInputGuard;
import neofontrender.addons.input.FreeLookInputGuard;
import neofontrender.addons.input.VanillaInputBridge;
import neofontrender.addons.compat.CameraExternalCompat;
import neofontrender.addons.api.input.InputAction;
import neofontrender.addons.api.input.InputFrame;
import neofontrender.addons.api.input.InputApi;
import neofontrender.addons.api.flight.FlightApi;
import neofontrender.addons.api.flight.FlightCameraTracking;
import neofontrender.addons.api.flight.FlightAttitude;

/** Client-thread camera state. Rendering and API consumers share its last immutable frame. */
public final class CameraRuntime {
    private static final Minecraft MC = Minecraft.getMinecraft();
    private static final ResourceLocation DRONE_ID = new ResourceLocation(
            "neofontrender_ui_enhancements", "drone");
    private static final ResourceLocation FREE_LOOK_ID = new ResourceLocation(
            "neofontrender_ui_enhancements", "free_look");
    private static final ResourceLocation CURSOR_LOOK_ID = new ResourceLocation(
            "neofontrender_ui_enhancements", "cursor_look");
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
    private static EntityLivingBase previousViewEntity;
    private static CameraProxyEntity apiViewEntity;
    private static EntityLivingBase apiPreviousViewEntity;
    private static int apiPreviousThirdPersonView;
    private static int previousThirdPersonView;
    private static boolean perspectiveOverridden;
    private static int freeLookExpectedPerspective;
    private static boolean freeLookFront;
    private static boolean freeLookControlsPlayer;
    private static CursorLookController cursorLookController;
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
        return proxy != null && MC.renderViewEntity == proxy;
    }

    /** Detached rigs store the final origin in their render-view adapter. */
    public static synchronized boolean usesDetachedThirdPersonPresentation() {
        return droneViewEntity != null && MC.renderViewEntity == droneViewEntity
                && MC.gameSettings.thirdPersonView > 0;
    }

    /**
     * Player-anchored rigs provide their own quaternion-relative orbit displacement, while a
     * detached rig already stores the final origin in its adapter. Neither may also receive
     * vanilla's four-block third-person displacement.
     */
    public static synchronized boolean suppressesVanillaThirdPersonDisplacement() {
        return usesDetachedThirdPersonPresentation()
                || ((isShoulderActive() || isLookCameraActive())
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
        return (isShoulderActive() || isLookCameraActive())
                && MC.renderViewEntity == MC.thePlayer;
    }

    /** Camera-local GL translation for rigs whose render-view entity remains the player. */
    static synchronized CameraVector anchoredViewTranslation(CameraFrame frame) {
        if (frame == null || (!isShoulderActive() && !isLookCameraActive())) return null;
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
        EntityPlayerSP player = MC.thePlayer;
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
        } else if (isCursorLookActive() && CursorLookConfig.useShoulderOffset) {
            ShoulderCameraRig.Sample sample = ShoulderCameraRig.resolve(
                    player, view, bodyPosition, value);
            position = sample.position;
            targetPosition = sample.target;
            shoulderPosition = position;
        } else if (isLookCameraActive()) {
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
                        player, view, bodyPosition, vanillaThirdPersonDistance());
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
                !isDroneActive() && !isLookCameraActive() && !isShoulderActive()
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
        boolean cursorLookRequest = CURSOR_LOOK_ID.equals(request.id());
        boolean shoulder = SHOULDER_ID.equals(request.id());
        if (!drone && !freeLook && !cursorLookRequest && !shoulder) return new RejectedSession();
        if (!CameraExternalCompat.internalCameraAllowed()) return new RejectedSession();
        // Releasing an API-only detached lease first keeps the session restore target from
        // pointing at a stale adapter when a built-in rig takes ownership.
        releaseApiViewProxy();
        invalidateSample();
        resetKinematics();
        if (drone) {
            EntityPlayerSP player = MC.thePlayer;
            if (player == null) return new RejectedSession();
            CameraVector position = new CameraVector(player.posX,
                    player.posY + player.getEyeHeight(), player.posZ);
            CameraAttitude attitude = CameraAttitude.fromMinecraftDegrees(
                    player.rotationPitch, player.rotationYaw, 0.0D);
            droneMotion = new DroneMotionController(position, attitude);
            dronePosition = position;
            droneAttitude = attitude;
            lastDroneInputNanos = 0L;
            previousViewEntity = MC.renderViewEntity;
            droneViewEntity = new CameraProxyEntity(player.worldObj);
            syncDroneViewEntity(position, attitude);
            MC.renderViewEntity = droneViewEntity;
            enterBuiltInPresentation();
            droneInput = DroneInputGuard.enter();
        } else if (freeLook || cursorLookRequest) {
            EntityPlayerSP player = MC.thePlayer;
            if (player == null) return new RejectedSession();
            CameraAttitude attitude = bodyAttitude(player, renderPartialTicks()).attitude;
            freeLookController = new FreeLookController(attitude);
            freeLookControlsPlayer = freeLook && FreeLookConfig.controlPlayerByDefault;
            cursorLookController = new CursorLookController();
            if (cursorLookRequest) cursorLookController.reset(MC.displayWidth, MC.displayHeight);
            if (cursorLookRequest && CursorLookConfig.useShoulderOffset) {
                CameraVector anchor = interpolatedEye(player, renderPartialTicks());
                ShoulderCameraRig.reset(player, attitude, anchor);
            }
            freeLookInput = FreeLookInputGuard.enter(freeLookControlsPlayer,
                    cursorLookRequest ? CURSOR_LOOK_ID : FREE_LOOK_ID);
            freeLookFront = MC.gameSettings.thirdPersonView == 2;
            enterBuiltInPresentation();
            freeLookExpectedPerspective = 1;
        } else if (shoulder) {
            EntityPlayerSP player = MC.thePlayer;
            if (player == null) return new RejectedSession();
            CameraAttitude body = bodyAttitude(player, renderPartialTicks()).attitude;
            CameraVector anchor = interpolatedEye(player, renderPartialTicks());
            ShoulderCameraRig.reset(player, body, anchor);
            shoulderPosition = ShoulderCameraRig.resolve(player, body, anchor,
                    renderPartialTicks()).position;
            enterBuiltInPresentation();
        }
        Session session = new Session(request, drone, freeLook, cursorLookRequest, shoulder);
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

    public static synchronized boolean isCursorLookActive() {
        return activeSession instanceof Session && activeSession.isActive()
                && ((Session) activeSession).cursorLook;
    }

    static synchronized boolean isLookCameraActive() {
        return isFreeLookActive() || isCursorLookActive();
    }

    static synchronized boolean isFreeLookPerspectiveValid() {
        return CameraPresentationPolicy.freeLookPerspectiveValid(isLookCameraActive(),
                MC.gameSettings.thirdPersonView, freeLookExpectedPerspective);
    }

    public static synchronized boolean isShoulderActive() {
        return activeSession instanceof Session && activeSession.isActive()
                && ((Session) activeSession).shoulder;
    }

    /** Advances tick-owned rig state before render frames interpolate it. */
    static synchronized void advanceCameraTick() {
        EntityPlayerSP player = MC.thePlayer;
        boolean cursorShoulder = isCursorLookActive() && CursorLookConfig.useShoulderOffset;
        if ((!isShoulderActive() && !cursorShoulder) || player == null) return;
        CameraAttitude body = bodyAttitude(player, 1.0F).attitude;
        CameraAttitude view = cursorShoulder && freeLookController != null
                ? freeLookController.resolve(body) : body;
        ShoulderCameraRig.tick(player, view, interpolatedEye(player, 1.0F));
        invalidateSample();
    }

    static synchronized String activeRigId() {
        if (isDroneActive()) return DRONE_ID.toString();
        if (isFreeLookActive()) return FREE_LOOK_ID.toString();
        if (isCursorLookActive()) return CURSOR_LOOK_ID.toString();
        if (isShoulderActive()) return SHOULDER_ID.toString();
        return null;
    }

    public static synchronized void swapShoulder() {
        if (!isShoulderActive()
                && !(isCursorLookActive() && CursorLookConfig.useShoulderOffset)) return;
        ShoulderCameraConfig.offsetX = -ShoulderCameraConfig.offsetX;
        ShoulderCameraConfig.save();
        shoulderPosition = null;
        invalidateSample();
    }

    public static synchronized void toggleFreeLookControl() {
        if (!isLookCameraActive()) return;
        if (isCursorLookActive()) {
            if (cursorLookController != null) cursorLookController.toggleControlTarget();
            invalidateSample();
            return;
        }
        freeLookControlsPlayer = !freeLookControlsPlayer;
        if (freeLookInput != null) freeLookInput.close();
        freeLookInput = FreeLookInputGuard.enter(
                freeLookControlsPlayer, FREE_LOOK_ID);
        invalidateSample();
    }

    public static synchronized boolean freeLookControlsPlayer() {
        return isFreeLookActive() && freeLookControlsPlayer;
    }

    public static synchronized boolean cursorLookControlsCamera() {
        return isCursorLookActive() && cursorLookController != null
                && cursorLookController.controlsCamera();
    }

    public static synchronized boolean isFreeLookCursorMode() {
        return isCursorLookActive();
    }

    /** Camera yaw used to reinterpret movement input; null leaves vanilla body-relative movement. */
    public static synchronized Float cursorLookMovementYaw(float partialTicks) {
        EntityPlayerSP player = MC.thePlayer;
        if (!isCursorLookActive() || !CursorLookConfig.cameraRelativeMovement
                || FlightApi.isActive() || player == null) return null;
        CameraVector forward = frame(partialTicks).viewBasis().forward();
        double horizontal = Math.sqrt(forward.x * forward.x + forward.z * forward.z);
        if (horizontal < 1.0E-7D) return player.rotationYaw;
        return (float) Math.toDegrees(Math.atan2(-forward.x, forward.z));
    }

    public static synchronized void toggleFreeLookCursorMode() {
        // Kept as a binary-compatible no-op for integrations compiled against the old toggle.
    }

    /** Returns the current cursor in scaled-screen coordinates for the existing crosshair renderer. */
    public static synchronized float[] freeLookCursorPosition(float partialTicks) {
        if (!isCursorLookActive() || cursorLookController == null
                || !cursorLookController.initialized()) return null;
        cursorLookController.ensureViewport(MC.displayWidth, MC.displayHeight);
        ScaledResolution resolution =
                new ScaledResolution(MC, MC.displayWidth, MC.displayHeight);
        double sx = resolution.getScaledWidth() / (double) Math.max(1, MC.displayWidth);
        double sy = resolution.getScaledHeight() / (double) Math.max(1, MC.displayHeight);
        return new float[]{(float) (cursorLookController.x() * sx),
                (float) (cursorLookController.y() * sy)};
    }

    /** Returns the world ray emitted by the virtual cursor for the authoritative camera frame. */
    static synchronized CameraRay freeLookCursorRay(float partialTicks) {
        if (!isCursorLookActive() || cursorLookController == null
                || !cursorLookController.initialized()) return null;
        cursorLookController.ensureViewport(MC.displayWidth, MC.displayHeight);
        CameraMeasurement measurement = CameraApi.measure(partialTicks);
        double sx = measurement.lens().width() / (double) Math.max(1, MC.displayWidth);
        double sy = measurement.lens().height() / (double) Math.max(1, MC.displayHeight);
        return measurement.screenRay(cursorLookController.x() * sx,
                cursorLookController.y() * sy);
    }

    /** Keeps gameplay look direction aligned with the cursor for weapons reading player yaw/pitch. */
    public static synchronized void synchronizeCursorPlayerAim(float partialTicks) {
        EntityPlayerSP player = MC.thePlayer;
        // Active flight/vehicle controllers own the physical body pose, matching Shoulder mode.
        if (!isCursorLookActive() || FlightApi.isActive()
                || player == null || player.worldObj == null) return;
        CameraFrame frame = CameraApi.getFrame(partialTicks);
        CameraVector direction = CameraPickingService.cursorPlayerAimDirection(player, frame);
        synchronizeCursorPlayerAim(direction);
    }

    /** Applies the direction derived from the same authoritative pick written to objectMouseOver. */
    static synchronized void synchronizeCursorPlayerAim(CameraVector direction) {
        EntityPlayerSP player = MC.thePlayer;
        if (!isCursorLookActive() || FlightApi.isActive()
                || player == null || player.worldObj == null) return;
        if (direction == null || direction.lengthSquared() < 1.0E-12D) return;
        double horizontal = Math.sqrt(direction.x * direction.x + direction.z * direction.z);
        float targetYaw = (float) Math.toDegrees(Math.atan2(-direction.x, direction.z));
        float pitch = (float) -Math.toDegrees(Math.atan2(direction.y, horizontal));
        CursorAimPose pose = CursorAimPose.resolve(player.rotationYaw, targetYaw, pitch,
                CursorLookConfig.headOnlyAim);
        player.rotationYaw = pose.bodyYaw;
        player.rotationPitch = pose.pitch;
        player.rotationYawHead = pose.headYaw;
        invalidateSample();
    }

    /** Cursor-owned head pitch/yaw and physical body yaw used by the local player renderer. */
    public static synchronized float[] freeLookCursorPose(float partialTicks) {
        EntityPlayerSP player = MC.thePlayer;
        return isCursorLookActive() && player != null
                ? new float[]{player.rotationYawHead, player.rotationPitch, player.rotationYaw}
                : null;
    }

    /** Returns the cached scaled-screen offset. Ray tracing is performed at RenderWorldLast. */
    public static synchronized float[] shoulderCrosshairOffset(float partialTicks) {
        if (!isShoulderActive() && !isLookCameraActive()) return null;
        EntityPlayerSP player = MC.thePlayer;
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
        if ((!isShoulderActive() && !isLookCameraActive()) || !shoulderCrosshairProjected) {
            return null;
        }
        return new float[]{shoulderCrosshairTargetX, shoulderCrosshairTargetY};
    }

    /** Updates player-aim and optional camera-ray projections while world matrices are current. */
    public static synchronized void updateShoulderCrosshairProjection(float partialTicks) {
        EntityPlayerSP player = MC.thePlayer;
        if ((!isShoulderActive() && !isLookCameraActive()) || player == null || MC.playerController == null) {
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
            MovingObjectPosition hit = CameraPickingService.tracePlayerRay(player, reach,
                    partialTicks, false);
            boolean miss = hit == null || hit.typeOfHit == MovingObjectType.MISS
                    || hit.hitVec == null;
            CameraVector aimPoint = !miss
                    ? new CameraVector(hit.hitVec.xCoord, hit.hitVec.yCoord, hit.hitVec.zCoord)
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
            MovingObjectPosition hit = CameraPickingService.traceCameraRay(player,
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
        EntityPlayerSP player = MC.thePlayer;
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
        if (isShoulderActive() && MC.thePlayer != null && !shoulderCrosshairPolicy(
                CameraPickingService.isAdaptiveAiming(MC.thePlayer)).renderPrimary()) return false;
        EntityPlayerSP player = MC.thePlayer;
        if (player == null) return true;
        int perspective = shoulderFirstPersonOverride ? 0 : isShoulderActive() ? 3
                : isLookCameraActive() ? 3 : MC.gameSettings.thirdPersonView;
        boolean aiming = CameraPickingService.isAdaptiveAiming(player);
        return ShoulderCameraConfig.visibilityRule(perspective)
                .render(MC.objectMouseOver, aiming);
    }

    public static synchronized boolean shoulderPlayerTransparency() {
        if (!isShoulderActive() || shoulderFirstPersonOverride
                || !ShoulderCameraConfig.playerTransparency || MC.thePlayer == null) {
            return false;
        }
        CameraFrame sample = frame(renderPartialTicks());
        CameraVector local = sample.viewAttitude().conjugate().rotate(
                sample.position().subtract(sample.bodyPosition()));
        double halfWidth = MC.thePlayer.width * 0.5D;
        double eye = MC.thePlayer.getEyeHeight();
        return Math.abs(local.x) < halfWidth
                && local.y < MC.thePlayer.height - eye && local.y > -eye;
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
        if (MC.thePlayer == null) return 1.0F;
        CameraFrame sample = frame(renderPartialTicks());
        CameraVector local = sample.viewAttitude().conjugate().rotate(
                sample.position().subtract(sample.bodyPosition()));
        double halfWidth = Math.max(1.0E-4D, MC.thePlayer.width * 0.5D);
        double eye = MC.thePlayer.getEyeHeight();
        double verticalLimit = local.y >= 0.0D
                ? Math.max(1.0E-4D, MC.thePlayer.height - eye) : Math.max(1.0E-4D, eye);
        double xAlpha = Math.min(1.0D, Math.abs(local.x) / halfWidth);
        double yAlpha = Math.min(1.0D, Math.abs(local.y) / verticalLimit);
        double geometric = Math.max(0.15D, Math.min(1.0D,
                Math.sqrt(xAlpha * xAlpha + yAlpha * yAlpha)));
        double configured = ShoulderCameraConfig.playerTransparencyPercent / 100.0D;
        return (float) Math.max(0.15D, Math.min(1.0D, geometric * configured));
    }

    public static synchronized boolean shoulderSkipPlayerRendering() {
        if (!isShoulderActive() || MC.thePlayer == null) return false;
        CameraFrame sample = frame(renderPartialTicks());
        double distance = sample.position().subtract(sample.bodyPosition()).length();
        if (shoulderFirstPersonOverride || (ShoulderCameraConfig.keepCameraOutOfHeadMultiplier > 0.0D
                && distance < MC.thePlayer.width * ShoulderCameraConfig.keepCameraOutOfHeadMultiplier)) {
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
        if (isDroneActive() || isLookCameraActive()) return true;
        if (isShoulderActive()) return CameraExternalCompat.internalCameraAllowed();
        EntityPlayerSP player = MC.thePlayer;
        return player != null && !lastFrame.isVanillaPassThrough()
                && FlightApi.queryCameraTracking(player, renderPartialTicks()) != null
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
        if (!isDroneActive() && !isLookCameraActive() && !isShoulderActive()
                && droneViewEntity == null) return;
        dronePosition = null;
        shoulderPosition = null;
        droneAttitude = null;
        droneMotion = null;
        freeLookController = null;
        freeLookControlsPlayer = false;
        cursorLookController = null;
        invalidateSample();
        lastDroneInputNanos = 0L;
        if (droneViewEntity != null && MC.renderViewEntity == droneViewEntity) {
            MC.renderViewEntity = previousViewEntity == null ? MC.thePlayer : previousViewEntity;
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

    private static float[] projectedOffset(MovingObjectPosition hit, float partialTicks) {
        if (hit == null || hit.hitVec == null) return null;
        return projectedOffset(new CameraVector(hit.hitVec.xCoord, hit.hitVec.yCoord, hit.hitVec.zCoord), partialTicks, false);
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
        ScaledResolution resolution =
                new ScaledResolution(MC, MC.displayWidth, MC.displayHeight);
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
        if (MC.renderViewEntity == apiViewEntity) {
            MC.renderViewEntity = apiPreviousViewEntity == null ? MC.thePlayer : apiPreviousViewEntity;
        }
        MC.gameSettings.thirdPersonView = apiPreviousThirdPersonView;
        apiViewEntity = null;
        apiPreviousViewEntity = null;
        refreshView(CameraViewChangeReason.MODE_EXIT, finalFrame);
    }

    /** Called after the legacy mouse event chain; cursor aiming is synchronized separately. */
    public static synchronized boolean updateViewInput(int originalDeltaX, int originalDeltaY,
                                                        int adjustedDeltaX, int adjustedDeltaY,
                                                        boolean eventCanceled, boolean invertMouse,
                                                        boolean forward, boolean back, boolean left,
                                                        boolean right, boolean up, boolean down,
                                                        long nowNanos) {
        if (!isDroneActive() && !isLookCameraActive()) return false;
        InputFrame routedInput = InputApi.getFrame(0.0F);
        double seconds = lastDroneInputNanos == 0L ? 0.0D
                : (nowNanos - lastDroneInputNanos) / 1_000_000_000.0D;
        lastDroneInputNanos = nowNanos;
        double roll = routedInput.get(InputAction.CAMERA_ROLL).getAxis();
        if (freeLookController != null) freeLookController.roll(roll, seconds);
        if (droneMotion != null) droneMotion.roll(roll, seconds);
        int routedDeltaX = VanillaInputBridge.resolveCameraDelta(
                originalDeltaX, adjustedDeltaX, eventCanceled,
                routedInput.get(InputAction.CAMERA_LOOK_X),
                routedInput.disposition(InputAction.CAMERA_LOOK_X),
                routedInput.getContext().getFrameSeconds());
        int routedDeltaY = VanillaInputBridge.resolveCameraDelta(
                originalDeltaY, adjustedDeltaY, eventCanceled,
                routedInput.get(InputAction.CAMERA_LOOK_Y),
                routedInput.disposition(InputAction.CAMERA_LOOK_Y),
                routedInput.getContext().getFrameSeconds());
        // Player-control mode hands the physical deltas back to vanilla player.turn().
        // The detached camera and cursor remain unchanged.
        if (isFreeLookActive() && freeLookControlsPlayer) {
            invalidateSample();
            return false;
        }
        if (isCursorLookActive()) {
            if (cursorLookController != null && cursorLookController.controlsCamera()) {
                if (freeLookController != null) freeLookController.look(
                        routedDeltaX, routedDeltaY, invertMouse,
                        MC.gameSettings.mouseSensitivity);
                if (MC.renderGlobal != null) MC.renderGlobal.onStaticEntitiesChanged();
                invalidateSample();
                return true;
            }
            if (cursorLookController == null) cursorLookController = new CursorLookController();
            cursorLookController.update(MC.displayWidth, MC.displayHeight,
                    routedDeltaX, routedDeltaY, CursorLookConfig.speed);
            invalidateSample();
            return true;
        }
        // In free-look player-control mode, let vanilla's player.turn() handle the mouse.
        // Roll remains a detached-camera action; don't process or zero the mouse deltas here.
        if (droneMotion != null) {
            // Keyboard yaw rotation for drone camera
            int keyboardYaw = 0;
            if (CameraKeyBindings.DRONE_ROTATE_LEFT.getIsKeyPressed()) keyboardYaw -= 1;
            if (CameraKeyBindings.DRONE_ROTATE_RIGHT.getIsKeyPressed()) keyboardYaw += 1;
            int droneLookX = VanillaInputBridge.resolveDroneCameraDeltaX(
                    originalDeltaX, adjustedDeltaX, eventCanceled,
                    routedInput.get(InputAction.CAMERA_LOOK_X),
                    routedInput.disposition(InputAction.CAMERA_LOOK_X),
                    routedInput.getContext().getFrameSeconds());
            droneMotion.look(droneLookX + keyboardYaw * 50, routedDeltaY, invertMouse);
        }
        if (freeLookController != null) freeLookController.look(routedDeltaX, routedDeltaY,
                invertMouse, MC.gameSettings.mouseSensitivity);
        if (isLookCameraActive() && MC.renderGlobal != null) {
            // Omnilook invalidates entity display state while the detached view is active.
            MC.renderGlobal.onStaticEntitiesChanged();
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

    /**
     * 1.7.10 has no Minecraft.getRenderPartialTicks(): the field is a private Timer instance with
     * no accessor, so the render partial ticks of the most recently evaluated sample are reused.
     * That is the same frozen value vanilla feeds into the frame currently being rendered; before
     * the first sample it degrades to 0.0F and the next evaluated frame corrects the pose.
     */
    private static float renderPartialTicks() {
        return samplePartialTicks >= 0.0F ? samplePartialTicks : 0.0F;
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

    /**
     * 1.7.10 keeps a constant 4.0 third-person distance: EntityRenderer.thirdPersonDistance is
     * never adjustable, and orientCamera interpolates from thirdPersonDistanceTemp, which
     * updateRenderer copies from it on every frame. 1.12.2 had to read its smoothed field through
     * a mixin accessor; there is no such state to read here.
     */
    private static double vanillaThirdPersonDistance() {
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
        // 1.7.10 EntityRenderer has no per-render-view shader selection: loadEntityShader and the
        // entity outline pass only exist from 1.8 on, so there is no shader state to refresh here.
        if (MC.renderGlobal != null) MC.renderGlobal.onStaticEntitiesChanged();
        MinecraftForge.EVENT_BUS.post(
                new CameraViewChangedEvent(finalFrame == null
                        ? CameraApi.getFrame(renderPartialTicks())
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
        if (apiViewEntity != null || frame == null || MC.theWorld == null) return;
        apiPreviousViewEntity = MC.renderViewEntity;
        apiPreviousThirdPersonView = MC.gameSettings.thirdPersonView;
        apiViewEntity = new CameraProxyEntity(MC.theWorld);
        syncViewEntity(apiViewEntity, frame.position(), frame.viewAttitude());
        MC.renderViewEntity = apiViewEntity;
        MC.gameSettings.thirdPersonView = 0;
        refreshView(CameraViewChangeReason.MODE_ENTER, frame);
    }

    private static CameraVector resolveDroneCollision(CameraVector from, CameraVector to) {
        if (from == null || to == null) return to;
        if (!DroneCameraConfig.collision || MC.theWorld == null) {
            CameraVector external = CameraApi.resolveCollision(
                    new CameraCollisionQuery(from, to, 0.1D));
            return external == null ? to : external;
        }
        Vec3 start = Vec3.createVectorHelper(from.x, from.y, from.z);
        Vec3 end = Vec3.createVectorHelper(to.x, to.y, to.z);
        // 1.7.10 Vec3 has no subtract helper that returns this - other, so the delta is explicit.
        Vec3 movement = Vec3.createVectorHelper(end.xCoord - start.xCoord,
                end.yCoord - start.yCoord, end.zCoord - start.zCoord);
        double length = Math.sqrt(movement.xCoord * movement.xCoord
                + movement.yCoord * movement.yCoord + movement.zCoord * movement.zCoord);
        if (length < 1.0E-8D) return from;
        double allowed = length;
        for (int i = 0; i < 8; i++) {
            Vec3 corner = Vec3.createVectorHelper((i & 1) == 0 ? -0.1D : 0.1D,
                    (i & 2) == 0 ? -0.1D : 0.1D,
                    (i & 4) == 0 ? -0.1D : 0.1D);
            Vec3 rayStart = start.addVector(corner.xCoord, corner.yCoord, corner.zCoord);
            MovingObjectPosition hit = MC.theWorld.func_147447_a(rayStart,
                    rayStart.addVector(movement.xCoord, movement.yCoord, movement.zCoord),
                    false, true, false);
            if (hit != null && hit.hitVec != null) {
                allowed = Math.min(allowed,
                        Math.max(0.0D, rayStart.distanceTo(hit.hitVec) - 0.01D));
            }
        }
        double ratio = Math.min(1.0D, allowed / length);
        Vec3 resolved = start.addVector(movement.xCoord * ratio, movement.yCoord * ratio,
                movement.zCoord * ratio);
        CameraVector vanilla = new CameraVector(resolved.xCoord, resolved.yCoord, resolved.zCoord);
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
            double yaw = player.prevRotationYaw + MathHelper.wrapAngleTo180_float(
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
        private final boolean cursorLook;
        private final boolean shoulder;
        private boolean closed;

        Session(CameraRigRequest request, boolean drone, boolean freeLook, boolean cursorLook, boolean shoulder) {
            this.request = request;
            this.drone = drone;
            this.freeLook = freeLook;
            this.cursorLook = cursorLook;
            this.shoulder = shoulder;
        }

        @Override public synchronized boolean isActive() { return !closed; }

        @Override public synchronized void close() {
            if (closed) return;
            closed = true;
            synchronized (CameraRuntime.class) {
                if (activeSession == this) activeSession = null;
                if (drone || freeLook || cursorLook || shoulder) {
                    clearDronePose();
                    if (droneInput != null) {
                        droneInput.close();
                        droneInput = null;
                    }
                }
                if ((freeLook || cursorLook) && freeLookInput != null) {
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
