package neofontrender.addons.flight;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.client.event.EntityViewRenderEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;
import net.minecraftforge.fml.relauncher.Side;
import neofontrender.addons.api.input.CameraMouseInputEvent;
import neofontrender.addons.api.flight.FlightApi;
import neofontrender.addons.api.flight.FlightCapability;
import neofontrender.addons.api.flight.FlightCapabilityEvent;
import neofontrender.addons.api.flight.FlightControlInput;
import neofontrender.addons.api.flight.FlightControllerInputEvent;
import neofontrender.addons.api.flight.FlightDecision;
import neofontrender.addons.api.flight.FlightOrientationEvent;
import neofontrender.addons.api.flight.FlightState;
import neofontrender.addons.flight.network.FlightRollNetwork;
import neofontrender.addons.zoom.ZoomMouseScaling;
import org.lwjgl.input.Keyboard;

import java.util.HashMap;
import java.util.Map;

final class FlightRollController implements FlightRollNetwork.ClientListener, FlightApi.Backend {
    static final FlightRollController INSTANCE = new FlightRollController();
    static final KeyBinding ROLL_LEFT = new KeyBinding(
            "key.neofontrender_ui_enhancements.flight_roll_left",
            Keyboard.KEY_NONE, "key.categories.neofontrender_ui_enhancements");
    static final KeyBinding ROLL_RIGHT = new KeyBinding(
            "key.neofontrender_ui_enhancements.flight_roll_right",
            Keyboard.KEY_NONE, "key.categories.neofontrender_ui_enhancements");
    static final KeyBinding YAW_LEFT = new KeyBinding(
            "key.neofontrender_ui_enhancements.flight_yaw_left",
            Keyboard.KEY_A, "key.categories.neofontrender_ui_enhancements");
    static final KeyBinding YAW_RIGHT = new KeyBinding(
            "key.neofontrender_ui_enhancements.flight_yaw_right",
            Keyboard.KEY_D, "key.categories.neofontrender_ui_enhancements");

    private final Minecraft mc = Minecraft.getMinecraft();
    private final Map<Integer, RemoteRoll> remoteRolls = new HashMap<>();
    private float roll;
    private float previousBarrel;
    private float barrel;
    private float barrelProgress = 1.0F;
    private int barrelDirection;
    private int activeBarrelDurationTicks = 20;
    private int syncTicker;
    private boolean companionPresent;
    private boolean handshakeRequested;
    private boolean serverAllows = true;
    private boolean serverSync;
    private float serverMaximumRoll = 180.0F;
    private double momentumX;
    private double momentumY;
    private long lastMouseNanos;
    private float hudInputX;
    private float hudInputY;

    private FlightRollController() {}

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void mouseInput(CameraMouseInputEvent event) {
        if (!active(event.getPlayer())) {
            resetMouseControl();
            return;
        }

        float sensitivity = ZoomMouseScaling.adjustedSensitivity(mc.gameSettings.mouseSensitivity);
        float base = sensitivity * 0.6F + 0.2F;
        float vanillaScale = base * base * base * 8.0F;
        int invertPitch = mc.gameSettings.invertMouse ? -1 : 1;
        double elapsed = frameSeconds();
        double pitchDegrees;
        double yawDegrees = 0.0D;
        double rollDegrees;
        if (FlightRollConfig.momentumMouse) {
            momentumX += event.getDeltaX() * vanillaScale / 300.0D;
            momentumY += event.getDeltaY() * vanillaScale / 300.0D;
            double length = Math.sqrt(momentumX * momentumX + momentumY * momentumY);
            if (length > 1.0D) {
                momentumX /= length;
                momentumY /= length;
                length = 1.0D;
            }
            double deadzone = FlightRollConfig.momentumDeadzonePercent / 100.0D;
            double readyX = length < deadzone ? 0.0D : momentumX;
            double readyY = length < deadzone ? 0.0D : momentumY;
            pitchDegrees = -readyY * FlightRollConfig.maximumRollSpeed * elapsed
                    * FlightRollConfig.pitchSensitivity * invertPitch;
            rollDegrees = readyX * effectiveMaximumRollSpeed() * elapsed
                    * FlightRollConfig.rollSensitivity;
            hudInputX = (float) readyX;
            hudInputY = (float) readyY;
        } else {
            resetMomentumOnly();
            pitchDegrees = -event.getDeltaY() * vanillaScale * 0.15D
                    * FlightRollConfig.pitchSensitivity * invertPitch;
            rollDegrees = event.getDeltaX() * vanillaScale * 0.15D
                    * FlightRollConfig.rollSensitivity;
            hudInputX = clampAxis(event.getDeltaX() * vanillaScale / 20.0F);
            hudInputY = clampAxis(event.getDeltaY() * vanillaScale / 20.0F);
        }

        if (FlightRollConfig.keyboardYaw) {
            double keyboardYaw = (YAW_RIGHT.isKeyDown() ? 1.0D : 0.0D)
                    - (YAW_LEFT.isKeyDown() ? 1.0D : 0.0D);
            yawDegrees += keyboardYaw * FlightRollConfig.maximumRollSpeed * elapsed
                    * FlightRollConfig.yawSensitivity;
        }

        FlightControllerInputEvent controller = new FlightControllerInputEvent(
                event.getPlayer(), event.getPartialTicks(), elapsed);
        MinecraftForge.EVENT_BUS.post(controller);
        FlightControlInput registered = new FlightControlInput(
                event.getPlayer(), event.getPartialTicks(), elapsed);
        FlightApi.collectControlInput(registered);
        controller.setPitch(controller.getPitch() + registered.getPitch());
        controller.setYaw(controller.getYaw() + registered.getYaw());
        controller.setRoll(controller.getRoll() + registered.getRoll());
        pitchDegrees += controller.getPitch() * FlightRollConfig.maximumRollSpeed * elapsed
                * FlightRollConfig.controllerPitchSensitivity;
        yawDegrees += controller.getYaw() * FlightRollConfig.maximumRollSpeed * elapsed
                * FlightRollConfig.controllerYawSensitivity;
        rollDegrees += controller.getRoll() * effectiveMaximumRollSpeed() * elapsed
                * FlightRollConfig.controllerRollSensitivity;
        if (Math.abs(controller.getRoll()) > Math.abs(hudInputX)) hudInputX = controller.getRoll();
        if (Math.abs(controller.getPitch()) > Math.abs(hudInputY)) hudInputY = controller.getPitch();

        if (FlightRollConfig.invertPitch) pitchDegrees = -pitchDegrees;
        if (FlightRollConfig.invertYaw) yawDegrees = -yawDegrees;
        if (FlightRollConfig.invertRoll) rollDegrees = -rollDegrees;
        if (FlightRollConfig.banking) {
            FlightRollMath.BankingDelta banking = FlightRollMath.bankingDelta(
                    roll, event.getPlayer().rotationPitch, elapsed);
            pitchDegrees += banking.pitch;
            yawDegrees += banking.yaw;
        }

        applyLocalRotation(event.getPlayer(), pitchDegrees, yawDegrees, rollDegrees,
                interpolatedBarrel(event.getPartialTicks()));
        // The transformed local-axis rotation replaces vanilla yaw/pitch for this frame.
        event.consumeHorizontal();
        event.consumeVertical();
    }

    @SubscribeEvent
    public void clientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
            previousBarrel = barrel;
            for (RemoteRoll state : remoteRolls.values()) state.beginTick();
            return;
        }
        EntityPlayerSP player = mc.player;
        boolean active = active(player);
        if (active && FlightRollConfig.barrelRolls && barrelProgress >= 1.0F) {
            if (ROLL_LEFT.isPressed()) startBarrel(-1, FlightRollConfig.barrelDurationTicks);
            else if (ROLL_RIGHT.isPressed()) startBarrel(1, FlightRollConfig.barrelDurationTicks);
        }
        if (!active) {
            resetMouseControl();
            barrelProgress = 1.0F;
            barrelDirection = 0;
            barrel = 0.0F;
            float upright = roll - FlightRollMath.wrapDegrees(roll);
            roll = FlightRollMath.approach(roll, upright, 0.25F);
            if (Math.abs(FlightRollMath.wrapDegrees(roll)) < 0.05F) roll = upright;
        } else if (barrelProgress < 1.0F) {
            barrelProgress = Math.min(1.0F,
                    barrelProgress + 1.0F / activeBarrelDurationTicks);
            barrel = barrelProgress >= 1.0F ? barrelDirection * 360.0F
                    : FlightRollMath.barrelAngle(barrelDirection, barrelProgress);
        }
        for (RemoteRoll state : remoteRolls.values()) state.advance();

        if (companionPresent && serverSync && ++syncTicker >= 3) {
            syncTicker = 0;
            FlightRollNetwork.sendRollUpdate(active, active ? renderedRoll(1.0F) : 0.0F);
        }
    }

    @SubscribeEvent
    public void cameraSetup(EntityViewRenderEvent.CameraSetup event) {
        if (!capability(mc.player, FlightCapability.CAMERA_ROTATION, active(mc.player))) return;
        event.setRoll(event.getRoll() + renderedRoll((float) event.getRenderPartialTicks()));
    }

    @SubscribeEvent
    public void connected(FMLNetworkEvent.ClientConnectedToServerEvent event) {
        resetNetwork();
    }

    @SubscribeEvent
    public void channelRegistration(FMLNetworkEvent.CustomPacketRegistrationEvent<?> event) {
        if (event.getSide() != Side.CLIENT
                || !event.getRegistrations().contains(FlightRollNetwork.CHANNEL_NAME)) return;
        if ("REGISTER".equals(event.getOperation()) && !handshakeRequested) {
            handshakeRequested = true;
            FlightRollNetwork.requestHandshake();
        } else if ("UNREGISTER".equals(event.getOperation())) {
            resetNetwork();
        }
    }

    @SubscribeEvent
    public void disconnected(FMLNetworkEvent.ClientDisconnectionFromServerEvent event) {
        resetNetwork();
    }

    private boolean active(EntityPlayerSP player) {
        if (companionPresent && !serverAllows) return false;
        boolean builtIn = FlightRollConfig.enabled && serverAllows && player != null
                && player.isElytraFlying()
                && (FlightRollConfig.allowInWater || !player.isInWater())
                && !Loader.isModLoaded("rollthesky");
        return capability(player, FlightCapability.CONTROL, builtIn);
    }

    private void startBarrel(int direction, int durationTicks) {
        barrelDirection = direction;
        activeBarrelDurationTicks = Math.max(1, Math.min(1200, durationTicks));
        barrelProgress = 0.0F;
        previousBarrel = 0.0F;
        barrel = 0.0F;
    }

    private float renderedRoll(float partialTicks) {
        return roll + interpolatedBarrel(partialTicks);
    }

    private float interpolatedBarrel(float partialTicks) {
        float amount = Math.max(0.0F, Math.min(1.0F, partialTicks));
        return previousBarrel + (barrel - previousBarrel) * amount;
    }

    private float effectiveMaximumRollSpeed() {
        return companionPresent ? Math.min(FlightRollConfig.maximumRollSpeed, serverMaximumRoll)
                : FlightRollConfig.maximumRollSpeed;
    }

    private void applyLocalRotation(EntityPlayerSP player, double pitchDegrees,
                                    double yawDegrees, double rollDegrees,
                                    float barrelOffset) {
        FlightOrientationEvent event = new FlightOrientationEvent(
                player, pitchDegrees, yawDegrees, rollDegrees);
        if (MinecraftForge.EVENT_BUS.post(event)) return;
        pitchDegrees = event.getPitchDegrees();
        yawDegrees = event.getYawDegrees();
        rollDegrees = event.getRollDegrees();
        float totalRoll = roll + barrelOffset;
        FlightOrientationMath.Orientation orientation = FlightOrientationMath.rotate(
                player.rotationPitch, player.rotationYaw, totalRoll,
                pitchDegrees, yawDegrees, rollDegrees);
        float yawDelta = orientation.yaw - player.rotationYaw;
        float pitchDelta = orientation.pitch - player.rotationPitch;
        player.turn(yawDelta / 0.15F, -pitchDelta / 0.15F);
        roll = orientation.roll - barrelOffset;
    }

    private void resetMouseControl() {
        resetMomentumOnly();
        lastMouseNanos = 0L;
        hudInputX = 0.0F;
        hudInputY = 0.0F;
    }

    private void resetMomentumOnly() {
        momentumX = 0.0D;
        momentumY = 0.0D;
    }

    private double frameSeconds() {
        long now = System.nanoTime();
        double elapsed = lastMouseNanos == 0L ? 1.0D / 60.0D
                : Math.min(0.1D, Math.max(0.0D, (now - lastMouseNanos) / 1_000_000_000.0D));
        lastMouseNanos = now;
        return elapsed;
    }

    private static float clampAxis(float value) {
        return Math.max(-1.0F, Math.min(1.0F, value));
    }

    private void resetNetwork() {
        companionPresent = false;
        handshakeRequested = false;
        serverAllows = true;
        serverSync = false;
        serverMaximumRoll = 180.0F;
        remoteRolls.clear();
        syncTicker = 0;
    }

    private boolean capability(EntityPlayerSP player, FlightCapability capability,
                               boolean builtInDefault) {
        FlightDecision decision = FlightApi.queryCapability(player, capability, builtInDefault);
        FlightCapabilityEvent event = new FlightCapabilityEvent(player, capability, builtInDefault);
        if (decision == FlightDecision.ALLOW) event.setResult(net.minecraftforge.fml.common.eventhandler.Event.Result.ALLOW);
        else if (decision == FlightDecision.DENY) event.setResult(net.minecraftforge.fml.common.eventhandler.Event.Result.DENY);
        MinecraftForge.EVENT_BUS.post(event);
        if (event.getResult() == net.minecraftforge.fml.common.eventhandler.Event.Result.ALLOW) return true;
        if (event.getResult() == net.minecraftforge.fml.common.eventhandler.Event.Result.DENY) return false;
        return builtInDefault;
    }

    @Override
    public void onHandshake(int protocolVersion, boolean enabled, boolean syncEnabled,
                            float maximumRoll) {
        companionPresent = protocolVersion == FlightRollNetwork.PROTOCOL_VERSION;
        serverAllows = companionPresent && enabled;
        serverSync = companionPresent && enabled && syncEnabled;
        serverMaximumRoll = maximumRoll;
    }

    @Override
    public void onRemoteRoll(int entityId, boolean rolling, float roll) {
        if (mc.player != null && entityId == mc.player.getEntityId()) return;
        if (!rolling) remoteRolls.remove(entityId);
        else remoteRolls.compute(entityId, (id, state) -> {
            if (state == null) return new RemoteRoll(FlightRollMath.wrapDegrees(roll));
            state.updateTarget(roll);
            return state;
        });
    }

    private static final class RemoteRoll {
        private float previous;
        private float current;
        private float target;

        private RemoteRoll(float roll) {
            previous = current = target = roll;
        }

        private void beginTick() { previous = current; }

        private void advance() {
            current += (target - current) * 0.55F;
            if (Math.abs(target - current) < 0.01F) current = target;
        }

        private void updateTarget(float value) {
            float wrapped = FlightRollMath.wrapDegrees(value);
            while (wrapped - current >= 180.0F) wrapped -= 360.0F;
            while (wrapped - current < -180.0F) wrapped += 360.0F;
            target = wrapped;
        }

        private float interpolated(float partialTicks) {
            float amount = Math.max(0.0F, Math.min(1.0F, partialTicks));
            return previous + (current - previous) * amount;
        }
    }

    static boolean hudVisible() {
        EntityPlayerSP player = INSTANCE.mc.player;
        return INSTANCE.capability(player, FlightCapability.HUD, INSTANCE.active(player));
    }
    static boolean suppressVanillaCrosshair() {
        EntityPlayerSP player = INSTANCE.mc.player;
        return INSTANCE.capability(player, FlightCapability.CROSSHAIR_SUPPRESSION,
                FlightHudSurface.INSTANCE.visible()
                        && CrosshairConfig.hideVanillaDuringFlightHud
                        && FlightHudSurface.INSTANCE.hidesVanillaCrosshair());
    }
    static float hudRoll(float partialTicks) { return INSTANCE.renderedRoll(partialTicks); }
    static float hudPitch() { return INSTANCE.mc.player == null ? 0.0F : INSTANCE.mc.player.rotationPitch; }
    static float hudInputX() { return INSTANCE.hudInputX; }
    static float hudInputY() { return INSTANCE.hudInputY; }

    static float playerRollForEntity(int entityId, float partialTicks) {
        if (INSTANCE.mc.player != null && INSTANCE.mc.player.getEntityId() == entityId) {
            boolean enabled = INSTANCE.capability(INSTANCE.mc.player,
                    FlightCapability.PLAYER_ROLL_RENDERING, INSTANCE.active(INSTANCE.mc.player));
            return enabled ? INSTANCE.renderedRoll(partialTicks) : 0.0F;
        }
        RemoteRoll state = INSTANCE.remoteRolls.get(entityId);
        return FlightRollConfig.remotePlayerRoll && state != null ? state.interpolated(partialTicks) : 0.0F;
    }

    @Override public FlightState state(float partialTicks) {
        EntityPlayerSP player = mc.player;
        return new FlightState(active(player), player == null ? 0.0F : player.rotationPitch,
                player == null ? 0.0F : player.rotationYaw, renderedRoll(partialTicks),
                hudInputX, hudInputY, barrelProgress < 1.0F, companionPresent, serverAllows,
                serverSync, effectiveMaximumRollSpeed());
    }

    @Override public void setRoll(float degrees) { roll = degrees; }

    @Override public void rotate(float pitchDegrees, float yawDegrees, float rollDegrees) {
        EntityPlayerSP player = mc.player;
        if (player != null && capability(player, FlightCapability.CAMERA_ROTATION, active(player))) {
            applyLocalRotation(player, pitchDegrees, yawDegrees, rollDegrees, 0.0F);
        }
    }

    @Override public boolean startBarrelRoll(int direction, int durationTicks) {
        if (!active(mc.player) || barrelProgress < 1.0F) return false;
        startBarrel(direction, durationTicks);
        return true;
    }

    @Override public void resetOrientation() {
        roll = previousBarrel = barrel = 0.0F;
        barrelProgress = 1.0F;
        barrelDirection = 0;
        resetMouseControl();
    }

    @Override public void registriesChanged() {
        FlightHudThemeManager.INSTANCE.reloadNow();
    }

    @Override public java.util.List<String> hudThemes() {
        return FlightHudThemeManager.INSTANCE.themeIds();
    }

    @Override public String selectedHudTheme() { return FlightRollConfig.hudTheme; }

    @Override public boolean selectHudTheme(String id) {
        if (id == null || !FlightHudThemeManager.INSTANCE.themeIds().contains(id)) return false;
        FlightRollConfig.hudTheme = id;
        return true;
    }

    @Override public float playerRoll(int entityId, float partialTicks) {
        return FlightRollController.playerRollForEntity(entityId, partialTicks);
    }

    @Override public void updateRemotePlayerRoll(int entityId, boolean rolling, float degrees) {
        onRemoteRoll(entityId, rolling, degrees);
    }
}
