package neofontrender.addons.flight;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import neofontrender.addons.hud.compositor.HudSurface;
import neofontrender.addons.api.flight.FlightApi;
import neofontrender.addons.api.flight.FlightHudCrosshairMode;
import neofontrender.addons.api.flight.FlightHudRenderContext;
import neofontrender.addons.api.flight.FlightHudRenderEvent;
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
        ScaledResolution resolution = new ScaledResolution(mc,
                mc.displayWidth, mc.displayHeight);
        FlightHudTheme theme = FlightHudThemeManager.INSTANCE.current();
        return FlightHudViewport.fit(resolution.getScaledWidth(),
                resolution.getScaledHeight(), theme.canvasWidth, theme.canvasHeight,
                FlightRollConfig.hudScalePercent);
    }

    @Override
    public boolean visible() {
        Minecraft mc = Minecraft.getMinecraft();
        return FlightRollConfig.flightHud && !mc.gameSettings.hideGUI
                && mc.currentScreen == null && FlightRollController.hudVisible();
    }

    boolean hidesVanillaCrosshair() {
        return FlightHudThemeManager.INSTANCE.current().crosshairMode
                == FlightHudCrosshairMode.HIDE_VANILLA;
    }

    @Override
    public void render(float partialTicks) {
        FlightHudTheme theme = FlightHudThemeManager.INSTANCE.current();
        Rectangle bounds = bounds();
        Minecraft mc = Minecraft.getMinecraft();
        FlightHudTelemetry.Sample sample = telemetry.sample(mc.thePlayer, partialTicks, theme);
        FlightTelemetryEvent telemetryEvent = new FlightTelemetryEvent(
                mc.thePlayer, partialTicks, sample.publicSnapshot());
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
        renderer.draw(bounds, theme, sample, FlightRollController.hudRoll(partialTicks),
                FlightRollController.hudPitch(), FlightRollController.hudInputX(),
                FlightRollController.hudInputY(), context);
        MinecraftForge.EVENT_BUS.post(new FlightHudRenderEvent.Post(context));
    }

    @Override public boolean acceptsPointer() { return false; }
}
