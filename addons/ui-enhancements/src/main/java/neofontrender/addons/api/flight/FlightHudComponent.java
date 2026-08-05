package neofontrender.addons.api.flight;

/** Standard renderer for one schema-3 HUD component type. */
@FunctionalInterface
public interface FlightHudComponent {
    void render(FlightHudRenderContext context, FlightHudElement element);
}
