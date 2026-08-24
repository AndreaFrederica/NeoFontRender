package neofontrender.addons.api.input;

import net.minecraft.util.ResourceLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Stable input-routing facade shared by camera, Flight and future controller integrations.
 *
 * <p>The UIE client bridge publishes exactly one frame for each input sample. Integrations may
 * register contexts and observers, but must not cache a mutable device value between frames.</p>
 */
public final class InputApi {
    public static final int API_VERSION = 1;
    private static final Logger LOGGER = LogManager.getLogger("Revo UI Input API");
    private static final AtomicLong SAMPLE_IDS = new AtomicLong();
    private static final CopyOnWriteArrayList<DeviceEntry> DEVICES = new CopyOnWriteArrayList<>();
    private static final CopyOnWriteArrayList<BindingEntry> BINDINGS = new CopyOnWriteArrayList<>();
    private static final CopyOnWriteArrayList<ContextEntry> CONTEXTS = new CopyOnWriteArrayList<>();
    private static final CopyOnWriteArrayList<ObserverEntry> OBSERVERS = new CopyOnWriteArrayList<>();
    private static volatile InputFrame currentFrame = InputFrame.empty();
    private static volatile List<ResourceLocation> currentContextIds = java.util.Collections.emptyList();
    private static volatile long lastFrameNanos;

    private InputApi() {}

    public static int getApiVersion() { return API_VERSION; }

    /** Returns the most recently published immutable frame, or an empty frame before client input starts. */
    public static InputFrame getFrame(float partialTicks) { return currentFrame; }

    /** Starts a frame at the Minecraft mouse sampling boundary with neutral logical values. */
    public static InputFrame beginFrame(float partialTicks, boolean gameFocused) {
        long now = System.nanoTime();
        long previous = lastFrameNanos;
        lastFrameNanos = now;
        double seconds = previous == 0L ? 0.0D
                : Math.max(0.0D, Math.min(0.1D, (now - previous) / 1_000_000_000.0D));
        InputFrameContext context = new InputFrameContext(SAMPLE_IDS.incrementAndGet(), partialTicks,
                seconds, gameFocused, gameFocused ? null : InputFlushReason.FOCUS_LOST);
        return gameFocused ? sample(context) : flush(context);
    }

    /** Convenience gate used by legacy consumers while they migrate to action values. */
    public static boolean isBlocked(InputAction action) {
        return currentFrame.disposition(Objects.requireNonNull(action, "action"))
                == InputDisposition.BLOCK;
    }

    /** Registers a physical or virtual device source. Device disconnects must return an empty sample. */
    public static InputRegistration registerDeviceSource(ResourceLocation id, int priority,
                                                        InputDeviceSource source) {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(source, "source");
        DeviceEntry entry = new DeviceEntry(id, priority, source);
        DEVICES.removeIf(value -> value.id.equals(entry.id));
        DEVICES.add(entry);
        sortDevices();
        return once(() -> DEVICES.remove(entry));
    }

    /** Registers mappings from source controls to logical actions. */
    public static InputRegistration registerBindingProvider(ResourceLocation id, int priority,
                                                            InputBindingProvider provider) {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(provider, "provider");
        BindingEntry entry = new BindingEntry(id, priority, provider);
        BINDINGS.removeIf(value -> value.id.equals(entry.id));
        BINDINGS.add(entry);
        BINDINGS.sort(Comparator.comparingInt((BindingEntry value) -> value.priority)
                .reversed().thenComparing(value -> value.id));
        return once(() -> BINDINGS.remove(entry));
    }

    /** Registers a dynamic context. Returning {@code null} from the provider makes it inactive. */
    public static InputRegistration registerContextProvider(ResourceLocation id, int priority,
                                                            InputContextProvider provider) {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(provider, "provider");
        ContextEntry entry = new ContextEntry(id, priority, provider);
        CONTEXTS.removeIf(value -> value.id.equals(entry.id));
        CONTEXTS.add(entry);
        sortContexts();
        return once(() -> CONTEXTS.remove(entry));
    }

    /** Installs a static context until the returned handle is closed. */
    public static InputRegistration pushContext(InputContext context) {
        Objects.requireNonNull(context, "context");
        return registerContextProvider(context.getId(), context.getPriority(), frame -> context);
    }

    /** Registers an ordered observer for post-routing, read-only input work such as HUD indicators. */
    public static InputRegistration registerFrameObserver(ResourceLocation id, int priority,
                                                          InputFrameObserver observer) {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(observer, "observer");
        ObserverEntry entry = new ObserverEntry(id, priority, observer);
        OBSERVERS.removeIf(value -> value.id.equals(entry.id));
        OBSERVERS.add(entry);
        OBSERVERS.sort(Comparator.comparingInt((ObserverEntry value) -> value.priority)
                .reversed().thenComparing(value -> value.id));
        return once(() -> OBSERVERS.remove(entry));
    }

    /**
     * UIE platform bridge only. Publishes a routed snapshot from already normalized logical
     * values; device sampling and binding are intentionally kept out of Minecraft Mixins.
     */
    public static InputFrame publish(InputFrameContext context,
                                     Map<InputAction, InputValue> unmappedValues) {
        Objects.requireNonNull(context, "context");
        Objects.requireNonNull(unmappedValues, "unmappedValues");
        List<InputContext> active = activeContexts(context);
        EnumMap<InputAction, InputValue> values = new EnumMap<>(InputAction.class);
        EnumMap<InputAction, InputDisposition> dispositions = new EnumMap<>(InputAction.class);
        EnumMap<InputAction, ResourceLocation> owners = new EnumMap<>(InputAction.class);

        for (InputAction action : InputAction.values()) {
            InputValue value = unmappedValues.getOrDefault(action, InputValue.NEUTRAL);
            InputContext owner = firstOwner(active, action);
            if (owner == null) {
                values.put(action, value);
                dispositions.put(action, InputDisposition.PASS);
            } else {
                InputDisposition disposition = owner.disposition(action);
                values.put(action, disposition == InputDisposition.BLOCK ? InputValue.NEUTRAL : value);
                dispositions.put(action, disposition);
                owners.put(action, owner.getId());
            }
        }

        InputFrame frame = new InputFrame(context, values, dispositions, owners);
        List<ResourceLocation> contextIds = new ArrayList<>();
        for (InputContext inputContext : active) contextIds.add(inputContext.getId());
        currentContextIds = java.util.Collections.unmodifiableList(contextIds);
        currentFrame = frame;
        notifyObservers(frame);
        return frame;
    }

    /** Samples registered devices, applies registered bindings, then resolves the context stack. */
    public static InputFrame sample(InputFrameContext context) {
        Objects.requireNonNull(context, "context");
        List<InputDeviceSample> samples = new ArrayList<>();
        for (DeviceEntry entry : DEVICES) {
            try {
                InputDeviceSample sample = entry.source.sample(context);
                if (sample != null) samples.add(sample);
            } catch (RuntimeException error) {
                throw callbackFailure("device:" + entry.id, "sample", error);
            }
        }

        List<InputBinding> bindings = new ArrayList<>();
        for (BindingEntry entry : BINDINGS) {
            try {
                entry.provider.bind(context, bindings::add);
            } catch (RuntimeException error) {
                throw callbackFailure("binding:" + entry.id, "bind", error);
            }
        }

        EnumMap<InputAction, InputValue> values = new EnumMap<>(InputAction.class);
        for (InputBinding binding : bindings) {
            if (binding == null) continue;
            for (InputDeviceSample sample : samples) {
                merge(values, binding.getAction(), binding.map(sample.get(binding.getControl())));
            }
        }
        return publish(context, values);
    }

    /** Publishes a neutral frame, which clears persistent device values across a mode boundary. */
    public static InputFrame flush(InputFrameContext context) {
        return publish(context, java.util.Collections.emptyMap());
    }

    /** Emits a neutral sample at the current focus boundary; the reason is retained for diagnostics. */
    public static InputFrame flush(InputFlushReason reason) {
        Objects.requireNonNull(reason, "reason");
        InputFrame previous = currentFrame;
        return publish(new InputFrameContext(SAMPLE_IDS.incrementAndGet(),
                previous.getPartialTicks(), 0.0D, previous.getContext().isGameFocused(), reason),
                java.util.Collections.emptyMap());
    }

    private static List<InputContext> activeContexts(InputFrameContext frame) {
        List<InputContext> active = new ArrayList<>();
        for (ContextEntry entry : CONTEXTS) {
            try {
                InputContext context = entry.provider.context(frame);
                if (context != null) active.add(context);
            } catch (RuntimeException error) {
                throw callbackFailure("context:" + entry.id, "context", error);
            }
        }
        active.sort(Comparator.comparingInt(InputContext::getPriority).reversed()
                .thenComparing(value -> value.getId().toString()));
        return active;
    }

    private static InputContext firstOwner(List<InputContext> contexts, InputAction action) {
        for (InputContext context : contexts) {
            if (context.disposition(action) != InputDisposition.PASS) return context;
        }
        return null;
    }

    private static void notifyObservers(InputFrame frame) {
        for (ObserverEntry entry : OBSERVERS) {
            try {
                entry.observer.onInputFrame(frame);
            } catch (RuntimeException error) {
                throw callbackFailure("observer:" + entry.id, "onInputFrame", error);
            }
        }
    }

    private static void sortContexts() {
        CONTEXTS.sort(Comparator.comparingInt((ContextEntry value) -> value.priority)
                .reversed().thenComparing(value -> value.id));
    }

    private static void sortDevices() {
        DEVICES.sort(Comparator.comparingInt((DeviceEntry value) -> value.priority)
                .reversed().thenComparing(value -> value.id));
    }

    private static void merge(Map<InputAction, InputValue> values, InputAction action,
                              InputValue candidate) {
        InputValue previous = values.get(action);
        if (previous == null) {
            values.put(action, candidate);
            return;
        }
        float axis = Math.abs(candidate.getAxis()) > Math.abs(previous.getAxis())
                ? candidate.getAxis() : previous.getAxis();
        values.put(action, new InputValue(axis, previous.isDown() || candidate.isDown(),
                previous.isPressed() || candidate.isPressed(),
                previous.isReleased() || candidate.isReleased()));
    }

    private static InputRegistration once(Runnable action) {
        return new InputRegistration() {
            private boolean closed;
            @Override public synchronized void close() {
                if (closed) return;
                closed = true;
                action.run();
            }
        };
    }

    public static InputDiagnostics diagnostics() {
        InputFrame frame = currentFrame;
        EnumMap<InputAction, ResourceLocation> owners = new EnumMap<>(InputAction.class);
        EnumMap<InputAction, InputDisposition> dispositions = new EnumMap<>(InputAction.class);
        for (InputAction action : InputAction.values()) {
            ResourceLocation owner = frame.owner(action);
            if (owner != null) owners.put(action, owner);
            dispositions.put(action, frame.disposition(action));
        }
        return new InputDiagnostics(frame.getSampleId(), frame.getContext().getFlushReason(),
                currentContextIds, ids(DEVICES), ids(BINDINGS), ids(CONTEXTS), ids(OBSERVERS),
                owners, dispositions);
    }

    private static RuntimeException callbackFailure(String owner, String operation,
                                                    RuntimeException error) {
        LOGGER.error("Input API callback {} failed for {}", operation, owner, error);
        return error;
    }

    private static List<String> ids(List<?> entries) {
        List<String> result = new ArrayList<>();
        for (Object entry : entries) {
            if (entry instanceof DeviceEntry) result.add(((DeviceEntry) entry).id.toString());
            else if (entry instanceof BindingEntry) result.add(((BindingEntry) entry).id.toString());
            else if (entry instanceof ContextEntry) result.add(((ContextEntry) entry).id.toString());
            else if (entry instanceof ObserverEntry) result.add(((ObserverEntry) entry).id.toString());
        }
        return result;
    }

    private static final class ContextEntry {
        final ResourceLocation id;
        final int priority;
        final InputContextProvider provider;

        ContextEntry(ResourceLocation id, int priority, InputContextProvider provider) {
            this.id = id;
            this.priority = priority;
            this.provider = provider;
        }
    }

    private static final class DeviceEntry {
        final ResourceLocation id;
        final int priority;
        final InputDeviceSource source;

        DeviceEntry(ResourceLocation id, int priority, InputDeviceSource source) {
            this.id = id;
            this.priority = priority;
            this.source = source;
        }
    }

    private static final class BindingEntry {
        final ResourceLocation id;
        final int priority;
        final InputBindingProvider provider;

        BindingEntry(ResourceLocation id, int priority, InputBindingProvider provider) {
            this.id = id;
            this.priority = priority;
            this.provider = provider;
        }
    }

    private static final class ObserverEntry {
        final ResourceLocation id;
        final int priority;
        final InputFrameObserver observer;

        ObserverEntry(ResourceLocation id, int priority, InputFrameObserver observer) {
            this.id = id;
            this.priority = priority;
            this.observer = observer;
        }
    }
}
