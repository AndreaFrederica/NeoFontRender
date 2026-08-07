package neofontrender.addons.electricelytra.client;

import neofontrender.addons.api.flight.FlightAttitude;

import java.util.HashMap;
import java.util.Map;

/** Client display-only quaternion interpolation for remote powered-wing pilots. */
public final class ElectricRemoteAttitudes {
    private static final Map<Integer, State> STATES = new HashMap<>();

    private ElectricRemoteAttitudes() {}

    public static synchronized void update(int entityId, FlightAttitude attitude) {
        State state = STATES.computeIfAbsent(entityId, ignored -> new State(attitude));
        state.previous = state.current;
        state.current = attitude;
        state.lastUpdateMillis = System.currentTimeMillis();
    }

    public static synchronized FlightAttitude sample(int entityId, float partialTicks) {
        State state = STATES.get(entityId);
        if (state == null || System.currentTimeMillis() - state.lastUpdateMillis > 2_000L) {
            STATES.remove(entityId);
            return null;
        }
        return state.previous.slerp(state.current,
                Math.max(0.0D, Math.min(1.0D, partialTicks)));
    }

    public static synchronized void clear() { STATES.clear(); }

    private static final class State {
        FlightAttitude previous;
        FlightAttitude current;
        long lastUpdateMillis;
        State(FlightAttitude attitude) {
            previous = current = attitude;
            lastUpdateMillis = System.currentTimeMillis();
        }
    }
}
