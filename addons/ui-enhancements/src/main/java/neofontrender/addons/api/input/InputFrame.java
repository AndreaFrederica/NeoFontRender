package neofontrender.addons.api.input;

import net.minecraft.util.ResourceLocation;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

/** Immutable, single-sample result of device mapping and context routing. */
public final class InputFrame {
    private static final InputFrame EMPTY = new InputFrame(
            new InputFrameContext(0L, 0.0F, 0.0D, false),
            new EnumMap<>(InputAction.class), new EnumMap<>(InputAction.class),
            new EnumMap<>(InputAction.class));

    private final InputFrameContext context;
    private final Map<InputAction, InputValue> values;
    private final Map<InputAction, InputDisposition> dispositions;
    private final Map<InputAction, ResourceLocation> owners;

    InputFrame(InputFrameContext context, Map<InputAction, InputValue> values,
               Map<InputAction, InputDisposition> dispositions,
               Map<InputAction, ResourceLocation> owners) {
        this.context = Objects.requireNonNull(context, "context");
        this.values = immutableValues(values);
        this.dispositions = immutableDispositions(dispositions);
        this.owners = Collections.unmodifiableMap(new EnumMap<>(owners));
    }

    public static InputFrame empty() { return EMPTY; }
    public InputFrameContext getContext() { return context; }
    public long getSampleId() { return context.getSampleId(); }
    public float getPartialTicks() { return context.getPartialTicks(); }
    public double getFrameSeconds() { return context.getFrameSeconds(); }
    public InputValue get(InputAction action) { return values.getOrDefault(action, InputValue.NEUTRAL); }
    public InputDisposition disposition(InputAction action) {
        return dispositions.getOrDefault(action, InputDisposition.PASS);
    }
    public ResourceLocation owner(InputAction action) { return owners.get(action); }
    public Map<InputAction, InputValue> values() { return values; }

    private static Map<InputAction, InputValue> immutableValues(Map<InputAction, InputValue> source) {
        EnumMap<InputAction, InputValue> result = new EnumMap<>(InputAction.class);
        for (InputAction action : InputAction.values()) {
            result.put(action, source.getOrDefault(action, InputValue.NEUTRAL));
        }
        return Collections.unmodifiableMap(result);
    }

    private static Map<InputAction, InputDisposition> immutableDispositions(
            Map<InputAction, InputDisposition> source) {
        EnumMap<InputAction, InputDisposition> result = new EnumMap<>(InputAction.class);
        for (InputAction action : InputAction.values()) {
            result.put(action, source.getOrDefault(action, InputDisposition.PASS));
        }
        return Collections.unmodifiableMap(result);
    }
}
