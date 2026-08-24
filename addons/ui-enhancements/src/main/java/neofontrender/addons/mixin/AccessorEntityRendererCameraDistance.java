package neofontrender.addons.mixin;

import net.minecraft.client.renderer.EntityRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(EntityRenderer.class)
public interface AccessorEntityRendererCameraDistance {
    @Accessor("thirdPersonDistancePrev")
    float nfrUi$getThirdPersonDistancePrev();
}
