package neofontrender.addons.api.camera;

import net.minecraft.util.ResourceLocation;

import java.util.Objects;

/** Minimal request for a multi-stage camera owner. */
public final class CameraRigRequest {
    public static final ResourceLocation DRONE_ID = new ResourceLocation(
            "neofontrender_ui_enhancements", "drone");
    public static final ResourceLocation FREE_LOOK_ID = new ResourceLocation(
            "neofontrender_ui_enhancements", "free_look");
    public static final ResourceLocation SHOULDER_ID = new ResourceLocation(
            "neofontrender_ui_enhancements", "shoulder");
    private final ResourceLocation id;
    private final int priority;

    public CameraRigRequest(ResourceLocation id, int priority) {
        this.id = Objects.requireNonNull(id, "id");
        this.priority = priority;
    }

    public ResourceLocation id() { return id; }
    public int priority() { return priority; }

    public static CameraRigRequest drone(int priority) { return new CameraRigRequest(DRONE_ID, priority); }
    public static CameraRigRequest freeLook(int priority) { return new CameraRigRequest(FREE_LOOK_ID, priority); }
    public static CameraRigRequest shoulder(int priority) { return new CameraRigRequest(SHOULDER_ID, priority); }
}
