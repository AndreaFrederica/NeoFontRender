package neofontrender.addons.compat;

/** A single side-effect of an active {@link ModCompat}. */
public final class CompatImpact {
    public static final String KIND_DISABLED_MIXIN = "disabled_mixin";

    /** Impact category; see {@code KIND_*} constants. */
    public final String kind;

    /** Affected target (mixin class name, method, etc.). */
    public final String target;

    /** Translation key for the explanatory sentence. */
    public final String reasonKey;

    public CompatImpact(String kind, String target, String reasonKey) {
        this.kind = kind;
        this.target = target;
        this.reasonKey = reasonKey;
    }
}
