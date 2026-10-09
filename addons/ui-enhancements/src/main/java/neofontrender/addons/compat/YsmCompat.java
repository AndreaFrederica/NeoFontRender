package neofontrender.addons.compat;

import net.minecraft.launchwrapper.Launch;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.MixinEnvironment;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;

/** Optional, class-resource-gated compatibility rule for the verified YSM player renderer ABI. */
public final class YsmCompat implements ModCompat {
    public static final String RENDER_MIXIN =
            "neofontrender.addons.mixin.MixinYsmPlayerBodyPose";

    private static final Logger LOGGER = LogManager.getLogger("Revo UI YSM Compatibility");
    private static final AtomicBoolean REPORTED_UNSUPPORTED = new AtomicBoolean();

    @Override public String id() { return "ysm-body-pose"; }
    @Override public String displayName() { return "YSM player body pose"; }

    /** Always active so the registry can veto these optional mixins when YSM is absent. */
    @Override public boolean isActive() { return true; }

    @Override
    public boolean shouldApplyMixin(String mixinClassName) {
        if (!isYsmMixin(mixinClassName)) return true;
        // This plugin runs in Mixin's DEFAULT phase, before Forge creates its sided delegate.
        // The UIE configuration also places this optional renderer mixin in the client section.
        if (MixinEnvironment.getCurrentEnvironment().getSide() != MixinEnvironment.Side.CLIENT) {
            return false;
        }
        ClassLoader loader = Launch.classLoader;
        if (loader == null) {
            throw new IllegalStateException("YSM mixin gating requires the initialized LaunchClassLoader");
        }
        return permits(mixinClassName, loader, true, YsmPoseConfig::enabled);
    }

    static boolean permits(String mixinClassName, ClassLoader loader, boolean client,
                           BooleanSupplier enabled) {
        if (!isYsmMixin(mixinClassName)) return true;
        if (!client) return false;
        YsmClassAbi.Result abi = YsmClassAbi.inspect(loader);
        if (abi.status() == YsmClassAbi.Result.Status.UNSUPPORTED
                && REPORTED_UNSUPPORTED.compareAndSet(false, true)) {
            YsmClassAbi.VersionMetadata metadata = YsmClassAbi.versionMetadata(loader);
            String message = "Optional YSM player body-pose integration disabled: modID="
                    + YsmClassAbi.MOD_ID + ", version=" + metadata.version()
                    + " (version source: " + metadata.reason() + "), unsupported ABI: "
                    + abi.reason() + ". This UI Enhancements build supports the verified YSM 1.1.9 seam.";
            if (metadata.cause() == null) LOGGER.warn(message);
            else LOGGER.warn(message, metadata.cause());
        }
        return abi.supported() && enabled.getAsBoolean();
    }

    private static boolean isYsmMixin(String mixinClassName) {
        return RENDER_MIXIN.equals(mixinClassName);
    }
}
