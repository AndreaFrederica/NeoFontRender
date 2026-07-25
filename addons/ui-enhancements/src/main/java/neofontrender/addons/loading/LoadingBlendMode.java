package neofontrender.addons.loading;

import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.opengl.GL11;

/** Blend modes shared by the loading backdrop and its translucent overlays. */
final class LoadingBlendMode {
    static final int SOURCE_RGB = GL11.GL_SRC_ALPHA;
    static final int DESTINATION_RGB = GL11.GL_ONE_MINUS_SRC_ALPHA;
    static final int SOURCE_ALPHA = GL11.GL_ONE;
    static final int DESTINATION_ALPHA = GL11.GL_ONE_MINUS_SRC_ALPHA;

    private LoadingBlendMode() {
    }

    static void enableSourceOver() {
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(SOURCE_RGB, DESTINATION_RGB,
                SOURCE_ALPHA, DESTINATION_ALPHA);
    }

    static void restoreMinecraftDefault() {
        GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA,
                GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
    }
}
