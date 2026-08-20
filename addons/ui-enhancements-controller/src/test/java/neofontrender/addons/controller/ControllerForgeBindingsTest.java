package neofontrender.addons.controller;

import net.minecraft.client.settings.KeyBinding;
import neofontrender.addons.controller.sdl.ControllerControls;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ControllerForgeBindingsTest {
    @AfterEach
    void clear() {
        ControllerForgeBindings.restore(Collections.emptyMap());
    }

    @Test
    void roundTripsVanillaAndModDescriptionIds() {
        ControllerForgeBindings.assign("key.jump", ControllerControls.SOUTH);
        ControllerForgeBindings.assign("key.example_mod.special", ControllerControls.TOUCHPAD);
        List<String> records = ControllerForgeBindings.serialize();

        ControllerForgeBindings.restore(Collections.emptyMap());
        ControllerForgeBindings.load(records);

        assertEquals(ControllerControls.SOUTH, ControllerForgeBindings.control("key.jump"));
        assertEquals(ControllerControls.TOUCHPAD,
                ControllerForgeBindings.control("key.example_mod.special"));
    }

    @Test
    void ignoresMalformedRecordsIndependently() {
        ControllerForgeBindings.assign("key.inventory", ControllerControls.START);
        String valid = ControllerForgeBindings.serialize().get(0);
        ControllerForgeBindings.load(Arrays.asList("not-a-record", "%%%|bad", valid));
        assertEquals(ControllerControls.START,
                ControllerForgeBindings.control("key.inventory"));
        assertNull(ControllerForgeBindings.control("bad"));
    }

    @Test
    void registeredListIncludesVanillaDefaultsAndModRegistrations() {
        KeyBinding vanilla = new KeyBinding("key.jump", 57, "key.categories.movement");
        KeyBinding mod = new KeyBinding("key.example_mod.special", 0,
                "key.categories.example_mod");

        List<KeyBinding> bindings = ControllerForgeBindings.registeredFrom(
                new KeyBinding[] { vanilla, mod });

        assertEquals(2, bindings.size());
        assertTrue(bindings.contains(vanilla));
        assertTrue(bindings.contains(mod));
    }

    @Test
    void roundTripsAndAppliesAxisHalfDirection() {
        ControllerForgeBindings.assign("key.forward", new ControllerKeyBindingAssignment(
                ControllerControls.LEFT_STICK_Y, ControllerKeyBindingAssignment.NEGATIVE));

        List<String> records = ControllerForgeBindings.serialize();
        ControllerForgeBindings.restore(Collections.emptyMap());
        ControllerForgeBindings.load(records);

        ControllerKeyBindingAssignment assignment =
                ControllerForgeBindings.assignment("key.forward");
        assertEquals(ControllerKeyBindingAssignment.NEGATIVE, assignment.axisDirection());
        assertTrue(assignment.isDown(neofontrender.addons.api.input.InputValue.axis(-0.8F)));
        assertTrue(!assignment.isDown(neofontrender.addons.api.input.InputValue.axis(0.8F)));
    }

    @Test
    void legacyAxisRecordKeepsAnyDirectionCompatibility() {
        ControllerForgeBindings.assign("key.forward", ControllerControls.LEFT_STICK_Y);
        String legacy = ControllerForgeBindings.serialize().get(0);
        legacy = legacy.substring(0, legacy.lastIndexOf('|'));

        ControllerForgeBindings.load(Collections.singletonList(legacy));

        ControllerKeyBindingAssignment assignment =
                ControllerForgeBindings.assignment("key.forward");
        assertTrue(assignment.isDown(neofontrender.addons.api.input.InputValue.axis(-0.8F)));
        assertTrue(assignment.isDown(neofontrender.addons.api.input.InputValue.axis(0.8F)));
    }
}
