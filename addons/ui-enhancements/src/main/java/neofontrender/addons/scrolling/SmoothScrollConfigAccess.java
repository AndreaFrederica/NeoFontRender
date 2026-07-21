package neofontrender.addons.scrolling;

/** Runtime bridge used by early-loaded mixins without exposing mutable fields. */
public final class SmoothScrollConfigAccess {
    private SmoothScrollConfigAccess() {}
    public static boolean enabled() { return SmoothScrollConfig.enabled; }
    public static int wheelStep() { return SmoothScrollConfig.wheelStep; }
}
