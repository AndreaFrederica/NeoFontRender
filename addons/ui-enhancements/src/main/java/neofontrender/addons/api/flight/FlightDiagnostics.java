package neofontrender.addons.api.flight;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Immutable last-arbitration snapshot for Flight integrations. */
public final class FlightDiagnostics {
    private final String capabilityProviderId, bodyPoseProviderId, hudAttitudeProviderId;
    private final String maneuverHandlerId, cameraTrackingProviderId, hudComponentType;
    private final List<String> controlProviderIds;

    public FlightDiagnostics(String capabilityProviderId, List<String> controlProviderIds,
                             String bodyPoseProviderId, String hudAttitudeProviderId,
                             String maneuverHandlerId, String cameraTrackingProviderId,
                             String hudComponentType) {
        this.capabilityProviderId = capabilityProviderId;
        this.controlProviderIds = Collections.unmodifiableList(new ArrayList<>(
                controlProviderIds == null ? Collections.emptyList() : controlProviderIds));
        this.bodyPoseProviderId = bodyPoseProviderId;
        this.hudAttitudeProviderId = hudAttitudeProviderId;
        this.maneuverHandlerId = maneuverHandlerId;
        this.cameraTrackingProviderId = cameraTrackingProviderId;
        this.hudComponentType = hudComponentType;
    }

    public String capabilityProviderId() { return capabilityProviderId; }
    public List<String> controlProviderIds() { return controlProviderIds; }
    public String bodyPoseProviderId() { return bodyPoseProviderId; }
    public String hudAttitudeProviderId() { return hudAttitudeProviderId; }
    public String maneuverHandlerId() { return maneuverHandlerId; }
    public String cameraTrackingProviderId() { return cameraTrackingProviderId; }
    public String hudComponentType() { return hudComponentType; }
}
