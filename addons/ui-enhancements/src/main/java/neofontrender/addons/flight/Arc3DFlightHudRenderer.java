package neofontrender.addons.flight;

import neofontrender.addons.api.flight.FlightHudRenderContext;

import java.awt.Rectangle;

/** Thin HUD compositor: builds one frame and delegates every JSON element to the registry. */
final class Arc3DFlightHudRenderer {
    private final FlightHudComponentRegistry components;

    Arc3DFlightHudRenderer() { this(FlightHudComponentRegistry.BUILT_INS); }

    Arc3DFlightHudRenderer(FlightHudComponentRegistry components) {
        this.components = components;
    }

    void draw(Rectangle bounds, FlightHudTheme theme, FlightHudTelemetry.Sample sample,
              float roll, float pitch, float inputX, float inputY,
              FlightHudRenderContext publicContext) {
        FlightHudFrame frame = new FlightHudFrame(bounds, theme, sample,
                roll, pitch, inputX, inputY, publicContext);
        FlightHudGraphics.State state = new FlightHudGraphics.State();
        state.begin();
        try {
            FlightHudGraphics.withGuiScissor(bounds.x, bounds.y,
                    bounds.x + bounds.width, bounds.y + bounds.height, () -> {
                        for (FlightHudTheme.Element element : theme.elements) {
                            if (element.enabled) components.render(frame, element);
                        }
                    });
        } finally {
            state.restore();
        }
    }
}
