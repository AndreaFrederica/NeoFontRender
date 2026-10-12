package neofontrender.addons.controller;

import java.util.EnumMap;

final class ControllerUiRepeatState {
    enum Pulse {
        UP, DOWN, LEFT, RIGHT
    }

    private static final int INITIAL_DELAY_TICKS = 10;
    private static final int REPEAT_INTERVAL_TICKS = 3;
    private final EnumMap<Pulse, Integer> heldTicks = new EnumMap<>(Pulse.class);

    boolean pulse(Pulse pulse, boolean down) {
        int ticks = down ? heldTicks.getOrDefault(pulse, 0) + 1 : 0;
        heldTicks.put(pulse, ticks);
        return ticks == 1 || ticks >= INITIAL_DELAY_TICKS
                && (ticks - INITIAL_DELAY_TICKS) % REPEAT_INTERVAL_TICKS == 0;
    }

    void reset() { heldTicks.clear(); }
}
