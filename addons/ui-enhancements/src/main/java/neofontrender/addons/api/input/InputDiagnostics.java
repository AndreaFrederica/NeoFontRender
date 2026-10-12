package neofontrender.addons.api.input;

import net.minecraft.util.ResourceLocation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** Immutable input routing, ownership and registration snapshot. */
public final class InputDiagnostics {
    private final long sampleId;
    private final InputFlushReason flushReason;
    private final List<ResourceLocation> activeContextIds;
    private final List<String> deviceIds, bindingIds, contextProviderIds, observerIds;
    private final Map<InputAction, ResourceLocation> owners;
    private final Map<InputAction, InputDisposition> dispositions;

    public InputDiagnostics(long sampleId, InputFlushReason flushReason,
                            List<ResourceLocation> activeContextIds, List<String> deviceIds,
                            List<String> bindingIds, List<String> contextProviderIds,
                            List<String> observerIds, Map<InputAction, ResourceLocation> owners,
                            Map<InputAction, InputDisposition> dispositions) {
        this.sampleId = Math.max(0L, sampleId);
        this.flushReason = flushReason;
        this.activeContextIds = immutable(activeContextIds);
        this.deviceIds = immutable(deviceIds);
        this.bindingIds = immutable(bindingIds);
        this.contextProviderIds = immutable(contextProviderIds);
        this.observerIds = immutable(observerIds);
        EnumMap<InputAction, ResourceLocation> ownerCopy = new EnumMap<>(InputAction.class);
        if (owners != null) ownerCopy.putAll(owners);
        this.owners = Collections.unmodifiableMap(ownerCopy);
        EnumMap<InputAction, InputDisposition> dispositionCopy = new EnumMap<>(InputAction.class);
        if (dispositions != null) dispositionCopy.putAll(dispositions);
        this.dispositions = Collections.unmodifiableMap(dispositionCopy);
    }

    public long getSampleId() { return sampleId; }
    public InputFlushReason getFlushReason() { return flushReason; }
    public List<ResourceLocation> getActiveContextIds() { return activeContextIds; }
    public List<String> getDeviceIds() { return deviceIds; }
    public List<String> getBindingIds() { return bindingIds; }
    public List<String> getContextProviderIds() { return contextProviderIds; }
    public List<String> getObserverIds() { return observerIds; }
    public Map<InputAction, ResourceLocation> getOwners() { return owners; }
    public Map<InputAction, InputDisposition> getDispositions() { return dispositions; }
    public ResourceLocation owner(InputAction action) { return owners.get(action); }
    public InputDisposition disposition(InputAction action) {
        return dispositions.getOrDefault(action, InputDisposition.PASS);
    }

    private static <T> List<T> immutable(List<T> values) {
        return Collections.unmodifiableList(new ArrayList<>(
                values == null ? Collections.emptyList() : values));
    }
}
