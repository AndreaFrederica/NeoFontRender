package neofontrender.core.font.support;

/** Filtering rules for Minecraft bitmap font textures used beside modern backends. */
public final class VanillaFontTexturePolicy {
    private VanillaFontTexturePolicy() {}

    public static boolean forceNearest(String path) {
        return "textures/font/ascii_sga.png".equals(path)
                || "font/ascii_sga.png".equals(path);
    }
}
