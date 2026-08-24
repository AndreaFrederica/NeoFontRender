package neofontrender.addons.controller;

import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.util.ResourceLocation;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Controller assignments for vanilla and Forge-registered KeyBinding instances. */
final class ControllerForgeBindings {
    private static Map<String, ControllerKeyBindingAssignment> assignments = new LinkedHashMap<>();

    private ControllerForgeBindings() {}

    static synchronized ResourceLocation control(String description) {
        ControllerKeyBindingAssignment assignment = assignments.get(description);
        return assignment == null ? null : assignment.control();
    }

    static synchronized ControllerKeyBindingAssignment assignment(String description) {
        return assignments.get(description);
    }

    static synchronized void assign(String description, ResourceLocation control) {
        assign(description, control == null ? null : new ControllerKeyBindingAssignment(
                control, ControllerKeyBindingAssignment.ANY_DIRECTION));
    }

    static synchronized void assign(String description, ControllerKeyBindingAssignment assignment) {
        if (description == null || description.isEmpty()) return;
        if (assignment == null) assignments.remove(description);
        else assignments.put(description, assignment);
    }

    static synchronized void clear(String description) {
        if (description != null) assignments.remove(description);
    }

    static synchronized Map<String, ControllerKeyBindingAssignment> snapshot() {
        return new LinkedHashMap<>(assignments);
    }

    static synchronized void restore(Map<String, ControllerKeyBindingAssignment> values) {
        assignments = values == null ? new LinkedHashMap<>() : new LinkedHashMap<>(values);
    }

    static synchronized Map<String, ControllerKeyBindingAssignment> all() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(assignments));
    }

    static synchronized int uses(ResourceLocation control) {
        if (control == null) return 0;
        int count = 0;
        for (ControllerKeyBindingAssignment value : assignments.values()) {
            if (control.equals(value.control())) count++;
        }
        return count;
    }

    static synchronized List<String> serialize() {
        List<String> result = new ArrayList<>();
        for (Map.Entry<String, ControllerKeyBindingAssignment> entry : assignments.entrySet()) {
            String target = Base64.getUrlEncoder().withoutPadding().encodeToString(
                    entry.getKey().getBytes(StandardCharsets.UTF_8));
            ControllerKeyBindingAssignment assignment = entry.getValue();
            result.add(target + "|" + assignment.control() + "|" + assignment.axisDirection());
        }
        return result;
    }

    static synchronized void load(List<String> records) {
        LinkedHashMap<String, ControllerKeyBindingAssignment> parsed = new LinkedHashMap<>();
        if (records != null) for (String record : records) {
            if (record == null) continue;
            String[] parts = record.split("\\|", -1);
            if (parts.length < 2 || parts.length > 3 || parts[0].isEmpty()
                    || parts[1].isEmpty()) continue;
            try {
                String description = new String(Base64.getUrlDecoder().decode(
                        parts[0]), StandardCharsets.UTF_8);
                int direction = parts.length == 3 ? Integer.parseInt(parts[2])
                        : ControllerKeyBindingAssignment.ANY_DIRECTION;
                parsed.put(description, new ControllerKeyBindingAssignment(
                        new ResourceLocation(parts[1]), direction));
            } catch (RuntimeException ignored) {
                // Ignore one malformed user record without discarding the rest of the profile.
            }
        }
        assignments = parsed;
    }

    static List<KeyBinding> registered() {
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft == null || minecraft.gameSettings == null
                || minecraft.gameSettings.keyBindings == null) return Collections.emptyList();
        return registeredFrom(minecraft.gameSettings.keyBindings);
    }

    static List<KeyBinding> registeredFrom(KeyBinding[] bindings) {
        List<KeyBinding> result = new ArrayList<>();
        if (bindings != null) Collections.addAll(result, bindings);
        result.sort(Comparator.comparing(KeyBinding::getKeyCategory)
                .thenComparing(KeyBinding::getKeyDescription));
        return result;
    }
}
