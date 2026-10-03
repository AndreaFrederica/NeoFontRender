package neofontrender.api.text.animation;

import neofontrender.text.animation.TextAnimationFrame;

/** Public instance-lifetime API for stateful structured-text effects. */
public final class TextAnimationApi {
    private TextAnimationApi() {}

    /** Allocates a process-unique ID that will never be assigned to another logical instance. */
    public static long createInstanceId() {
        return TextAnimationFrame.allocateInstanceId();
    }

    /** Makes an instance ID visible to every animation layer rendered inside this scope. */
    public static TextAnimationScope openInstance(long instanceId) {
        TextAnimationFrame.Scope scope = TextAnimationFrame.openInstance(instanceId);
        return scope::close;
    }
}
