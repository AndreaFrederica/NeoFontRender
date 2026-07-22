package neofontrender.addons.vendor.tabbychat.foundation.render;

import org.lwjgl.opengl.GL11;

/** Minecraft 1.7.10 replacement for the later GlStateManager calls. */
public final class GlState {

    public enum LogicOp {
        OR_REVERSE(GL11.GL_OR_REVERSE);

        private final int value;

        LogicOp(int value) {
            this.value = value;
        }
    }

    private GlState() {}

    public static void enableBlend() { GL11.glEnable(GL11.GL_BLEND); }
    public static void disableBlend() { GL11.glDisable(GL11.GL_BLEND); }
    public static void enableAlpha() { GL11.glEnable(GL11.GL_ALPHA_TEST); }
    public static void disableAlpha() { GL11.glDisable(GL11.GL_ALPHA_TEST); }
    public static void enableTexture2D() { GL11.glEnable(GL11.GL_TEXTURE_2D); }
    public static void disableTexture2D() { GL11.glDisable(GL11.GL_TEXTURE_2D); }
    public static void enableColorLogic() { GL11.glEnable(GL11.GL_COLOR_LOGIC_OP); }
    public static void disableColorLogic() { GL11.glDisable(GL11.GL_COLOR_LOGIC_OP); }
    public static void colorLogicOp(LogicOp operation) { GL11.glLogicOp(operation.value); }
    public static void blendFunc(int source, int destination) { GL11.glBlendFunc(source, destination); }
    public static void color(float red, float green, float blue, float alpha) { GL11.glColor4f(red, green, blue, alpha); }
    public static void pushMatrix() { GL11.glPushMatrix(); }
    public static void popMatrix() { GL11.glPopMatrix(); }
    public static void translate(double x, double y, double z) { GL11.glTranslated(x, y, z); }
    public static void scale(double x, double y, double z) { GL11.glScaled(x, y, z); }
    public static void rotate(float angle, float x, float y, float z) { GL11.glRotatef(angle, x, y, z); }
}
