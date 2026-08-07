package neofontrender.addons.flight;

import neofontrender.addons.api.flight.FlightApi;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Immutable built-in component registry with a public-API fallback for namespaced types. */
final class FlightHudComponentRegistry {
    static final FlightHudComponentRegistry BUILT_INS = createBuiltIns();

    private final Map<String, BuiltInFlightHudComponent> components;

    private FlightHudComponentRegistry(Map<String, BuiltInFlightHudComponent> components) {
        this.components = Collections.unmodifiableMap(new LinkedHashMap<>(components));
    }

    boolean render(FlightHudFrame frame, FlightHudTheme.Element element) {
        BuiltInFlightHudComponent component = components.get(element.type);
        if (component != null) {
            component.render(frame, element);
            return true;
        }
        FlightHudGraphics.State state = new FlightHudGraphics.State();
        state.begin();
        try {
            return FlightApi.renderHudComponent(element.type, frame.publicContext,
                    frame.theme.publicElement(element));
        } finally {
            state.restore();
        }
    }

    boolean contains(String type) { return components.containsKey(type); }
    int size() { return components.size(); }

    private static FlightHudComponentRegistry createBuiltIns() {
        Map<String, BuiltInFlightHudComponent> values = new LinkedHashMap<>();
        values.put("STATUS", FlightHudStatusComponent.INSTANCE);
        values.put("FLIGHT_REFERENCE", FlightHudFlightReferenceComponent.INSTANCE);
        values.put("AIRSPEED_TAPE", FlightHudTapeComponent.AIRSPEED);
        values.put("ALTITUDE_TAPE", FlightHudTapeComponent.ALTITUDE);
        values.put("VERTICAL_SPEED", FlightHudVerticalSpeedComponent.INSTANCE);
        values.put("HEADING_RIBBON", FlightHudNavigationComponent.RIBBON);
        values.put("HEADING_ARC", FlightHudNavigationComponent.ARC);
        values.put("HEADING_DIAL", FlightHudNavigationComponent.DIAL);
        values.put("GROUND_SPEED", FlightHudAuxiliaryComponent.GROUND_SPEED);
        values.put("DATUM", FlightHudAuxiliaryComponent.DATUM);
        values.put("AOA_GAUGE", FlightHudAuxiliaryComponent.AOA);
        values.put("ENERGY_GAUGE", FlightHudAuxiliaryComponent.ENERGY);
        values.put("INPUT_STICK", FlightHudAuxiliaryComponent.INPUT);
        return new FlightHudComponentRegistry(values);
    }
}
