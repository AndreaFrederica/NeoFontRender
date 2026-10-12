package neofontrender.addons.controller;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ControllerInputHistoryTest {
    @Test
    void deduplicatesFramesAndRetainsTheNewestWindow() {
        ControllerInputHistory history = new ControllerInputHistory();
        history.add(1L, 0.1F, 0.0F, 0.0F);
        history.add(1L, 0.9F, 0.9F, 0.9F);
        for (int index = 2; index <= ControllerInputHistory.CAPACITY + 2; index++) {
            history.add(index, index, index, index);
        }
        assertEquals(ControllerInputHistory.CAPACITY, history.size());
        assertEquals(3.0F, history.raw(0));
        assertEquals(ControllerInputHistory.CAPACITY + 2.0F,
                history.raw(history.size() - 1));
    }
}
