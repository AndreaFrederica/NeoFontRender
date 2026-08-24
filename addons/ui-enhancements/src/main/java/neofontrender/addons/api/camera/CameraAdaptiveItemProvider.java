package neofontrender.addons.api.camera;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.ResourceLocation;

/** Extends Shoulder-style adaptive aiming without requiring a plugin annotation or reflection. */
public interface CameraAdaptiveItemProvider {
    ResourceLocation id();
    default int priority() { return 0; }

    /** Return null to pass, otherwise override the lower-priority/configured result. */
    Boolean isAiming(EntityLivingBase entity, boolean configuredResult);
}
