package neofontrender.addons.vendor.tabbychat.foundation.render;

import net.minecraft.client.renderer.Tessellator;

/** Texture drawing helpers missing from the Minecraft 1.7.10 Gui API. */
public final class GuiTextures {

    private GuiTextures() {}

    public static void drawScaled(int x, int y, float u, float v, int width, int height,
            float textureWidth, float textureHeight) {
        Tessellator tessellator = Tessellator.instance;
        tessellator.startDrawingQuads();
        tessellator.addVertexWithUV(x, y + height, 0, u / textureWidth, (v + height) / textureHeight);
        tessellator.addVertexWithUV(x + width, y + height, 0,
                (u + width) / textureWidth, (v + height) / textureHeight);
        tessellator.addVertexWithUV(x + width, y, 0,
                (u + width) / textureWidth, v / textureHeight);
        tessellator.addVertexWithUV(x, y, 0, u / textureWidth, v / textureHeight);
        tessellator.draw();
    }
}
