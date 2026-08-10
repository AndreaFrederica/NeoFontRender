package neofontrender.addons.api.camera;

import net.minecraftforge.fml.common.eventhandler.Event;

import java.util.Objects;

/** Published after shader and world-renderer state has been refreshed for a view change. */
public final class CameraViewChangedEvent extends Event {
    private final CameraFrame frame;
    private final CameraViewChangeReason reason;

    public CameraViewChangedEvent(CameraFrame frame, CameraViewChangeReason reason) {
        this.frame = Objects.requireNonNull(frame, "frame");
        this.reason = Objects.requireNonNull(reason, "reason");
    }

    public CameraFrame frame() { return frame; }
    public CameraViewChangeReason reason() { return reason; }
}
