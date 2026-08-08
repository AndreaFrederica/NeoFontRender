package neofontrender.addons.flight;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import neofontrender.addons.hud.compositor.HudSurface;
import neofontrender.addons.api.flight.FlightApi;
import neofontrender.addons.api.flight.FlightHudRenderContext;
import neofontrender.addons.api.flight.FlightHudRenderEvent;
import neofontrender.addons.api.flight.FlightHudEditorScreen;
import neofontrender.addons.api.flight.FlightHudAttitude;
import neofontrender.addons.api.flight.FlightEulerAngles;
import neofontrender.addons.api.flight.FlightRenderPose;
import neofontrender.addons.api.flight.FlightState;
import neofontrender.addons.api.flight.FlightTelemetryEvent;
import net.minecraftforge.common.MinecraftForge;

import java.awt.Rectangle;

/** Non-interactive flight instruments hosted by UIE's common HUD compositor. */
final class FlightHudSurface implements HudSurface {
    static final FlightHudSurface INSTANCE = new FlightHudSurface();
    private static final String ID = "neofontrender_ui_enhancements:flight_hud";
    private final Arc3DFlightHudRenderer renderer = new Arc3DFlightHudRenderer();
    private final FlightHudTelemetry telemetry = new FlightHudTelemetry();

    private FlightHudSurface() {}

    @Override public String id() { return ID; }

    @Override
    public Rectangle bounds() {
        Minecraft mc = Minecraft.getMinecraft();
        ScaledResolution resolution = new ScaledResolution(mc);
        FlightHudTheme theme = FlightHudThemeManager.INSTANCE.current();
        return FlightHudViewport.fit(resolution.getScaledWidth(),
                resolution.getScaledHeight(), theme.canvasWidth, theme.canvasHeight,
                FlightRollConfig.hudScalePercent);
    }

    @Override
    public boolean visible() {
        Minecraft mc = Minecraft.getMinecraft();
        return FlightRollConfig.flightHud && !mc.gameSettings.hideGUI
                && (mc.currentScreen == null
                || mc.currentScreen instanceof FlightHudEditorScreen)
                && FlightRollController.hudVisible();
    }

    boolean hidesVanillaCrosshair() {
        return FlightHudThemeManager.INSTANCE.current().crosshairMode
                == neofontrender.addons.api.flight.FlightHudCrosshairMode.HIDE_VANILLA;
    }

    @Override
    public void render(float partialTicks) {
        FlightHudTheme theme = FlightHudThemeManager.INSTANCE.current();
        Rectangle bounds = bounds();
        Minecraft mc = Minecraft.getMinecraft();
        FlightRenderPose renderPose = FlightApi.getRenderPose(mc.player, partialTicks);
        FlightHudAttitude attitude = renderPose == null
                ? FlightApi.queryHudAttitude(mc.player, partialTicks) : null;
        float pitch = FlightRollController.hudPitch();
        float yaw = FlightRollController.hudYaw(partialTicks);
        float roll = FlightRollController.hudRoll(partialTicks);
        if (renderPose != null) {
            FlightEulerAngles angles = renderPose.getCameraAngles();
            pitch = angles.pitchDegrees;
            yaw = angles.yawDegrees;
            roll = angles.rollDegrees;
        } else if (attitude != null) {
            FlightEulerAngles angles = attitude.getAttitude().toMinecraftEuler(pitch, yaw, roll);
            pitch = angles.pitchDegrees;
            yaw = angles.yawDegrees;
            roll = angles.rollDegrees;
        }
        FlightHudTelemetry.Sample sample = telemetry.sample(mc.player, partialTicks, theme,
                pitch, yaw);
        FlightTelemetryEvent telemetryEvent = new FlightTelemetryEvent(
                mc.player, partialTicks, sample.publicSnapshot());
        MinecraftForge.EVENT_BUS.post(telemetryEvent);
        sample = FlightHudTelemetry.Sample.from(telemetryEvent.getTelemetry());
        FlightState state = FlightApi.getState(partialTicks);
        FlightHudRenderContext context = new FlightHudRenderContext(bounds,
                theme.canvasWidth, theme.canvasHeight, partialTicks,
                telemetryEvent.getTelemetry(), state, theme.id, theme.style,
                theme.lineWidth, theme.textScale, theme.crosshairMode,
                theme.publicColors(),
                FlightHudGraphicsCanvas.INSTANCE);
        if (MinecraftForge.EVENT_BUS.post(new FlightHudRenderEvent.Pre(context))) return;
        renderer.draw(bounds, theme, sample, roll,
                pitch, FlightRollController.hudInputX(),
                FlightRollController.hudInputY(), context);
        MinecraftForge.EVENT_BUS.post(new FlightHudRenderEvent.Post(context));
    }

    @Override public boolean acceptsPointer() { return false; }
}
