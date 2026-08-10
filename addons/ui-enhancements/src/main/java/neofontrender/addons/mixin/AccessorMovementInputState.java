package neofontrender.addons.mixin;

import net.minecraft.util.MovementInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Accesses state declared by MovementInput. Cleanroom keeps these fields on the superclass;
 * they must not be shadowed from MovementInputFromOptions.
 */
@Mixin(MovementInput.class)
public interface AccessorMovementInputState {
    @Accessor("moveForward")
    void nfrUi$setMoveForward(float value);

    @Accessor("moveStrafe")
    void nfrUi$setMoveStrafe(float value);

    @Accessor("jump")
    void nfrUi$setJump(boolean value);

    @Accessor("sneak")
    void nfrUi$setSneak(boolean value);
}
