package neofontrender.addons.cursor;

import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.util.ResourceLocation;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

/** A validated raster cursor discovered from the game directory or a resource pack. */
public final class CursorAsset {
    public enum Source { LOCAL, RESOURCE_PACK }

    private static final int MAX_SIZE = 128;

    private final String id;
    private final String displayName;
    private final Source source;
    private final BufferedImage image;
    private final int hotspotX;
    private final int hotspotY;
    private ResourceLocation previewTexture;
    private DynamicTexture dynamicTexture;

    CursorAsset(String id, String displayName, Source source, BufferedImage input,
                int hotspotX, int hotspotY) {
        if (input == null || input.getWidth() < 1 || input.getHeight() < 1) {
            throw new IllegalArgumentException("Cursor image is empty");
        }
        this.id = id;
        this.displayName = displayName;
        this.source = source;

        double scale = Math.min(1.0D, Math.min((double) MAX_SIZE / input.getWidth(),
                (double) MAX_SIZE / input.getHeight()));
        int width = Math.max(1, (int) Math.round(input.getWidth() * scale));
        int height = Math.max(1, (int) Math.round(input.getHeight() * scale));
        this.image = width == input.getWidth() && height == input.getHeight()
                ? toArgb(input) : resize(input, width, height);
        this.hotspotX = clamp((int) Math.round(hotspotX * scale), 0, width - 1);
        this.hotspotY = clamp((int) Math.round(hotspotY * scale), 0, height - 1);
    }

    public String id() { return id; }
    public String displayName() { return displayName; }
    public Source source() { return source; }
    public int width() { return image.getWidth(); }
    public int height() { return image.getHeight(); }
    public int hotspotX() { return hotspotX; }
    public int hotspotY() { return hotspotY; }
    BufferedImage image() { return image; }

    ResourceLocation previewTexture() {
        if (previewTexture == null) {
            dynamicTexture = new DynamicTexture(image);
            previewTexture = net.minecraft.client.Minecraft.getMinecraft().getTextureManager()
                    .getDynamicTextureLocation("uie_cursor", dynamicTexture);
        }
        return previewTexture;
    }

    void releasePreview() {
        if (previewTexture != null) {
            net.minecraft.client.Minecraft.getMinecraft().getTextureManager()
                    .deleteTexture(previewTexture);
        }
        previewTexture = null;
        dynamicTexture = null;
    }

    private static BufferedImage toArgb(BufferedImage source) {
        BufferedImage converted = new BufferedImage(source.getWidth(), source.getHeight(),
                BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = converted.createGraphics();
        graphics.drawImage(source, 0, 0, null);
        graphics.dispose();
        return converted;
    }

    private static BufferedImage resize(BufferedImage source, int width, int height) {
        BufferedImage resized = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = resized.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        graphics.drawImage(source, 0, 0, width, height, null);
        graphics.dispose();
        return resized;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
