package neofontrender.addons.api.camera;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Immutable ownership and arbitration snapshot for camera diagnostics. */
public final class CameraDiagnostics {
    private final long sampleId;
    private final String activeRigId, sessionOwner, frameProviderId, lensProviderId;
    private final String pickingProviderId, collisionProviderId, adaptiveProviderId, failClosedReason;
    private final List<String> appliedModifierIds;
    private final boolean positionOverridden, renderOverridden;

    public CameraDiagnostics(long sampleId, String activeRigId, String sessionOwner,
                             String frameProviderId, List<String> appliedModifierIds,
                             String lensProviderId, String pickingProviderId,
                             String collisionProviderId, String adaptiveProviderId,
                             String failClosedReason,
                             boolean positionOverridden, boolean renderOverridden) {
        this.sampleId = Math.max(0L, sampleId);
        this.activeRigId = activeRigId;
        this.sessionOwner = sessionOwner;
        this.frameProviderId = frameProviderId;
        this.appliedModifierIds = Collections.unmodifiableList(new ArrayList<>(
                appliedModifierIds == null ? Collections.emptyList() : appliedModifierIds));
        this.lensProviderId = lensProviderId;
        this.pickingProviderId = pickingProviderId;
        this.collisionProviderId = collisionProviderId;
        this.adaptiveProviderId = adaptiveProviderId;
        this.failClosedReason = failClosedReason;
        this.positionOverridden = positionOverridden;
        this.renderOverridden = renderOverridden;
    }

    public long sampleId() { return sampleId; }
    public String activeRigId() { return activeRigId; }
    public String sessionOwner() { return sessionOwner; }
    public String frameProviderId() { return frameProviderId; }
    public List<String> appliedModifierIds() { return appliedModifierIds; }
    public String lensProviderId() { return lensProviderId; }
    public String pickingProviderId() { return pickingProviderId; }
    public String collisionProviderId() { return collisionProviderId; }
    public String adaptiveProviderId() { return adaptiveProviderId; }
    public String failClosedReason() { return failClosedReason; }
    public boolean isPositionOverridden() { return positionOverridden; }
    public boolean isRenderOverridden() { return renderOverridden; }
}
