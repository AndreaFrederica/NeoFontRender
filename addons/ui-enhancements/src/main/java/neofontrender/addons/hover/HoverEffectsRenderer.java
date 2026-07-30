package neofontrender.addons.hover;

import cpw.mods.fml.client.config.GuiUtils;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

import java.awt.Rectangle;

public final class HoverEffectsRenderer {
    private static float forgeButtonAlpha = 1.0F;

    private HoverEffectsRenderer() {}

    public static void drawVanillaButtonPart(GuiButton button, int x, int y, int textureX,
                                             int textureY, int width, int height) {
        if (!HoverEffectsConfigAccess.buttonsEnabled() || !button.enabled
                || !isStandardButtonTextureState(textureY)) {
            button.drawTexturedModalRect(x, y, textureX, textureY, width, height);
            return;
        }

        float progress = hoverProgress(button);
        // Keep a fully covered base while the hovered texture fades in. Fading both layers makes
        // their combined opacity dip in the middle and exposes the screen behind the button.
        drawVanillaPart(button, x, y, textureX, 66, width, height, 1.0F);
        drawVanillaPart(button, x, y, textureX, 86, width, height, progress);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
    }

    public static void drawForgeButton(GuiButton button, ResourceLocation texture, int x, int y,
                                       int textureX, int textureY, int width, int height,
                                       int textureWidth, int textureHeight, int topBorder,
                                       int bottomBorder, int leftBorder, int rightBorder, float zLevel) {
        if (!HoverEffectsConfigAccess.buttonsEnabled() || !button.enabled
                || !isStandardButtonTextureState(textureY)) {
            GuiUtils.drawContinuousTexturedBox(texture, x, y, textureX, textureY, width, height,
                    textureWidth, textureHeight, topBorder, bottomBorder, leftBorder, rightBorder, zLevel);
            return;
        }

        float progress = hoverProgress(button);
        drawForgeLayer(texture, x, y, textureX, 66, width, height, textureWidth, textureHeight,
                topBorder, bottomBorder, leftBorder, rightBorder, zLevel, 1.0F);
        drawForgeLayer(texture, x, y, textureX, 86, width, height, textureWidth, textureHeight,
                topBorder, bottomBorder, leftBorder, rightBorder, zLevel, progress);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
    }

    public static int buttonTextColor(GuiButton button, int originalColor) {
        if (!HoverEffectsConfigAccess.buttonsEnabled() || !button.enabled || button.packedFGColour != 0
                || !isStandardButtonTextColor(originalColor)) {
            return originalColor;
        }
        return interpolateColor(0xFFE0E0E0, 0xFFFFFFA0, hoverProgress(button)) & 0x00FFFFFF;
    }

    public static float hoverProgress(GuiButton button) {
        return ((HoverAnimationAccess) button).nfrUi$hoverProgress();
    }

    public static float visualProgress(GuiButton button) {
        // 1.7.10 leaves GuiButton#isMouseOver unmapped (func_146115_a).
        if (!HoverEffectsConfigAccess.buttonsEnabled()) {
            return button.func_146115_a() && button.enabled ? 1.0F : 0.0F;
        }
        return hoverProgress(button);
    }

    public static int interpolateColor(int from, int to, float amount) {
        float t = Math.max(0.0F, Math.min(1.0F, amount));
        int a = lerp(from >>> 24, to >>> 24, t);
        int r = lerp(from >>> 16 & 0xFF, to >>> 16 & 0xFF, t);
        int g = lerp(from >>> 8 & 0xFF, to >>> 8 & 0xFF, t);
        int b = lerp(from & 0xFF, to & 0xFF, t);
        return a << 24 | r << 16 | g << 8 | b;
    }

    public static int multiplyAlpha(int color, float amount) {
        int alpha = Math.round((color >>> 24) * Math.max(0.0F, Math.min(1.0F, amount)));
        return color & 0x00FFFFFF | alpha << 24;
    }

    public static void drawIngredientGridHighlight(Rectangle area, float progress) {
        int color = multiplyAlpha(HoverEffectsConfigAccess.slotColor(), progress);
        boolean lighting = GL11.glIsEnabled(GL11.GL_LIGHTING);
        boolean depth = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glColorMask(true, true, true, false);
        Gui.drawRect(area.x, area.y, area.x + area.width, area.y + area.height, color);
        GL11.glColorMask(true, true, true, true);
        if (lighting) GL11.glEnable(GL11.GL_LIGHTING);
        if (depth) GL11.glEnable(GL11.GL_DEPTH_TEST);
    }

    public static void applyForgeButtonAlpha() {
        if (forgeButtonAlpha < 0.999F) {
            GL11.glColor4f(1.0F, 1.0F, 1.0F, forgeButtonAlpha);
        }
    }

    private static void drawVanillaPart(GuiButton button, int x, int y, int textureX,
                                        int textureY, int width, int height, float alpha) {
        if (alpha <= 0.001F) return;
        GL11.glColor4f(1.0F, 1.0F, 1.0F, alpha);
        button.drawTexturedModalRect(x, y, textureX, textureY, width, height);
    }

    private static void drawForgeLayer(ResourceLocation texture, int x, int y, int textureX,
                                       int textureY, int width, int height, int textureWidth,
                                       int textureHeight, int topBorder, int bottomBorder,
                                       int leftBorder, int rightBorder, float zLevel, float alpha) {
        if (alpha <= 0.001F) return;
        float previous = forgeButtonAlpha;
        forgeButtonAlpha = alpha;
        try {
            GuiUtils.drawContinuousTexturedBox(texture, x, y, textureX, textureY, width, height,
                    textureWidth, textureHeight, topBorder, bottomBorder, leftBorder, rightBorder, zLevel);
        } finally {
            forgeButtonAlpha = previous;
        }
    }

    private static int lerp(int from, int to, float amount) {
        return Math.round(from + (to - from) * amount);
    }

    private static boolean isStandardButtonTextureState(int textureY) {
        return textureY == 66 || textureY == 86;
    }

    private static boolean isStandardButtonTextColor(int color) {
        return color == 0xE0E0E0 || color == 0xFFFFA0;
    }
}
