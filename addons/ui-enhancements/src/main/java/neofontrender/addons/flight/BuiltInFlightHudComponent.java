package neofontrender.addons.flight;

/** Internal adapter for one reusable built-in schema-3 HUD component. */
@FunctionalInterface
interface BuiltInFlightHudComponent {
    void render(FlightHudFrame frame, FlightHudTheme.Element element);
}
