package neofontrender.addons.mixin;

import net.minecraft.entity.item.EntityArmorStand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Exposes the vanilla armor stand flag setter to the render-only preview backend. */
@Mixin(EntityArmorStand.class)
public interface InvokerEntityArmorStandPreview {
    @Invoker("setNoBasePlate")
    void nfrUi$setNoBasePlate(boolean value);
}
