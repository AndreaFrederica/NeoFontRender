package neofontrender.addons.mixin;

import net.minecraft.client.shader.Shader;
import net.minecraft.client.shader.ShaderGroup;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

/** Typed access to the shader passes needed for live blur radius updates. */
@Mixin(ShaderGroup.class)
public interface AccessorShaderGroup {
    @Accessor("listShaders")
    List<Shader> nfrUi$getShaders();
}
