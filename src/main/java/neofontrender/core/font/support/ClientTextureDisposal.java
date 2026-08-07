package neofontrender.core.font.support;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.util.ResourceLocation;

/** Deletes GL-backed Minecraft textures only on the client thread that owns the GL context. */
public final class ClientTextureDisposal {
    private ClientTextureDisposal() {}

    public static void delete(ResourceLocation location) {
        if (location == null) return;
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft == null) return;
        Runnable deletion = () -> {
            TextureManager manager = minecraft.getTextureManager();
            if (manager != null) manager.deleteTexture(location);
        };
        if (minecraft.func_152345_ab()) deletion.run();
        else minecraft.func_152344_a(deletion);
    }
}
