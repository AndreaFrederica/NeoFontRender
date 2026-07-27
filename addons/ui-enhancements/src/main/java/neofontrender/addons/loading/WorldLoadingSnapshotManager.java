package neofontrender.addons.loading;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiIngameMenu;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.util.ResourceLocation;
import neofontrender.addons.ui.NfrUiEnhancements;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.IntBuffer;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Stores one user-owned screenshot inside each singleplayer save and exposes it as a temporary
 * loading texture on the next visit. No image assets are bundled with the addon.
 */
public enum WorldLoadingSnapshotManager {
    INSTANCE;

    private static final String SNAPSHOT_DIRECTORY = "neofontrender-ui";
    private static final String SNAPSHOT_FILE = "last-exit.png";
    private static final int MAX_CAPTURE_WIDTH = 1920;
    private static final int MAX_CAPTURE_HEIGHT = 1080;
    private final ExecutorService writer = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "NFR World Snapshot Writer");
        thread.setDaemon(true);
        return thread;
    });

    private BufferedImage cleanExitFrame;
    private String cleanExitWorld;
    private boolean cleanCaptureRequested;
    private volatile BufferedImage pendingExitFrame;
    private volatile String pendingExitWorld;
    private DynamicTexture texture;
    private ResourceLocation textureLocation;
    private int textureWidth;
    private int textureHeight;

    /** Requests a world-only frame when the pause menu opens; no framebuffer is read here. */
    public void requestCleanExitFrame() {
        if (!WorldLoadingConfig.lastExitSnapshot) return;
        Minecraft mc = Minecraft.getMinecraft();
        IntegratedServer server = mc.getIntegratedServer();
        if (mc.theWorld == null || server == null) return;
        cleanCaptureRequested = true;
        cleanExitWorld = server.getFolderName();
    }

    /**
     * Reads a freshly rendered world at EntityRenderer's pre-overlay boundary. The mixin calls
     * this only after renderWorld ran in the same updateCameraAndRender invocation, so a paused or
     * skipped world frame can never reuse an older framebuffer containing a complete GuiScreen.
     */
    public void captureRequestedWorldFrame() {
        if (!cleanCaptureRequested) return;
        Minecraft mc = Minecraft.getMinecraft();
        IntegratedServer server = mc.getIntegratedServer();
        if (!WorldLoadingConfig.lastExitSnapshot || mc.theWorld == null || server == null
                || !(mc.currentScreen instanceof GuiIngameMenu)
                || !server.getFolderName().equals(cleanExitWorld)) {
            cleanCaptureRequested = false;
            return;
        }
        cleanCaptureRequested = false;
        try {
            cleanExitFrame = scaledCapture(captureScreenshot(mc));
        } catch (Throwable throwable) {
            NfrUiEnhancements.LOGGER.warn("Could not capture the world-only exit frame", throwable);
            cleanExitFrame = null;
            cleanExitWorld = null;
        }
    }

    /** Called at the head of Minecraft.loadWorld(null), before the integrated server is discarded. */
    public void saveCurrentWorldAndRelease() {
        Minecraft mc = Minecraft.getMinecraft();
        IntegratedServer server = mc.getIntegratedServer();
        // launchIntegratedServer starts with loadWorld(null) even though there is no old world.
        // Keep the snapshot prepared at launch HEAD across that harmless cleanup call.
        if (mc.theWorld == null) return;
        if (!WorldLoadingConfig.lastExitSnapshot || server == null) {
            releaseActive();
            return;
        }

        String folder = server.getFolderName();
        BufferedImage image = folder != null && folder.equals(cleanExitWorld) ? cleanExitFrame : null;
        // Never fall back to this framebuffer: loadWorld(null) runs after the pause/menu HUD.
        cleanCaptureRequested = false;
        cleanExitFrame = null;
        cleanExitWorld = null;
        if (image != null) enqueueWrite(folder, image);
        releaseActive();
    }

    /** Loads the matching save's last screenshot before its integrated server starts. */
    public void prepareForWorld(String folder) {
        releaseActive();
        if (!WorldLoadingConfig.lastExitSnapshot) return;
        BufferedImage image = folder != null && folder.equals(pendingExitWorld) ? pendingExitFrame : null;
        File file = snapshotFile(folder);
        if (image == null && (file == null || !file.isFile())) return;
        try {
            if (image == null) image = ImageIO.read(file);
            if (image == null || image.getWidth() <= 0 || image.getHeight() <= 0) return;
            textureWidth = image.getWidth();
            textureHeight = image.getHeight();
            texture = new DynamicTexture(image);
            textureLocation = Minecraft.getMinecraft().getTextureManager()
                    .getDynamicTextureLocation("nfr_world_loading", texture);
            // 1.7.10 textures default to nearest filtering; linear keeps downscaled snapshots
            // smooth, matching the blur-without-mipmap setup used on 1.12.
            Minecraft.getMinecraft().getTextureManager().bindTexture(textureLocation);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        } catch (Throwable throwable) {
            NfrUiEnhancements.LOGGER.warn("Could not load world snapshot {}", file, throwable);
            releaseActive();
        }
    }

    public boolean hasActive() {
        return textureLocation != null && textureWidth > 0 && textureHeight > 0;
    }

    /** Draws the image with aspect-fill cropping so no stretching or letterboxing is introduced. */
    public boolean draw(int width, int height, float alpha) {
        if (!hasActive() || width <= 0 || height <= 0 || alpha <= 0.0F) return false;
        float imageAspect = textureWidth / (float) textureHeight;
        float screenAspect = width / (float) height;
        float u0 = 0.0F;
        float u1 = 1.0F;
        float v0 = 0.0F;
        float v1 = 1.0F;
        if (imageAspect > screenAspect) {
            float visible = screenAspect / imageAspect;
            u0 = (1.0F - visible) * 0.5F;
            u1 = 1.0F - u0;
        } else if (imageAspect < screenAspect) {
            float visible = imageAspect / screenAspect;
            v0 = (1.0F - visible) * 0.5F;
            v1 = 1.0F - v0;
        }

        Minecraft.getMinecraft().getTextureManager().bindTexture(textureLocation);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        LoadingBlendMode.enableSourceOver();
        GL11.glColor4f(1.0F, 1.0F, 1.0F, Math.max(0.0F, Math.min(1.0F, alpha)));
        Tessellator tessellator = Tessellator.instance;
        tessellator.startDrawingQuads();
        tessellator.addVertexWithUV(0.0D, height, 0.0D, u0, v1);
        tessellator.addVertexWithUV(width, height, 0.0D, u1, v1);
        tessellator.addVertexWithUV(width, 0.0D, 0.0D, u1, v0);
        tessellator.addVertexWithUV(0.0D, 0.0D, 0.0D, u0, v0);
        tessellator.draw();
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        return true;
    }

    public void releaseActive() {
        if (textureLocation != null) {
            Minecraft.getMinecraft().getTextureManager().deleteTexture(textureLocation);
        }
        texture = null;
        textureLocation = null;
        textureWidth = 0;
        textureHeight = 0;
    }

    private void enqueueWrite(String folder, BufferedImage image) {
        File destination = snapshotFile(folder);
        if (destination == null) return;
        pendingExitWorld = folder;
        pendingExitFrame = image;
        writer.execute(() -> {
            writeAtomically(destination.toPath(), image);
            if (pendingExitFrame == image) {
                pendingExitFrame = null;
                pendingExitWorld = null;
            }
        });
    }

    private static void writeAtomically(Path destination, BufferedImage image) {
        Path temporary = null;
        try {
            Files.createDirectories(destination.getParent());
            temporary = Files.createTempFile(destination.getParent(), "last-exit-", ".png");
            if (!ImageIO.write(image, "png", temporary.toFile())) {
                throw new IOException("No PNG writer is available");
            }
            try {
                Files.move(temporary, destination, StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException ignored) {
                Files.move(temporary, destination, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (Throwable throwable) {
            NfrUiEnhancements.LOGGER.warn("Could not save world snapshot {}", destination, throwable);
            if (temporary != null) {
                try {
                    Files.deleteIfExists(temporary);
                } catch (IOException ignored) {
                }
            }
        }
    }

    private static File snapshotFile(String folder) {
        if (folder == null || folder.trim().isEmpty()) return null;
        try {
            File saves = new File(Minecraft.getMinecraft().mcDataDir, "saves").getCanonicalFile();
            File world = new File(saves, folder).getCanonicalFile();
            if (!world.toPath().startsWith(saves.toPath())) return null;
            return new File(new File(world, SNAPSHOT_DIRECTORY), SNAPSHOT_FILE);
        } catch (IOException exception) {
            NfrUiEnhancements.LOGGER.warn("Could not resolve snapshot path for world {}", folder, exception);
            return null;
        }
    }

    /**
     * 1.7.10's ScreenShotHelper only saves PNG files asynchronously; the BufferedImage-returning
     * createScreenshot does not exist yet. This mirrors its pixel path, including the framebuffer
     * texture crop, and returns the frame directly.
     */
    private static BufferedImage captureScreenshot(Minecraft mc) {
        int width = mc.displayWidth;
        int height = mc.displayHeight;
        Framebuffer framebuffer = mc.getFramebuffer();
        boolean framebufferEnabled = OpenGlHelper.isFramebufferEnabled();
        if (framebufferEnabled) {
            width = framebuffer.framebufferTextureWidth;
            height = framebuffer.framebufferTextureHeight;
        }
        int pixelCount = width * height;
        IntBuffer pixelBuffer = BufferUtils.createIntBuffer(pixelCount);
        int[] pixelValues = new int[pixelCount];
        GL11.glPixelStorei(GL11.GL_PACK_ALIGNMENT, 1);
        GL11.glPixelStorei(GL11.GL_UNPACK_ALIGNMENT, 1);
        if (framebufferEnabled) {
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, framebuffer.framebufferTexture);
            GL11.glGetTexImage(GL11.GL_TEXTURE_2D, 0,
                    GL12.GL_BGRA, GL12.GL_UNSIGNED_INT_8_8_8_8_REV, pixelBuffer);
        } else {
            GL11.glReadPixels(0, 0, width, height,
                    GL12.GL_BGRA, GL12.GL_UNSIGNED_INT_8_8_8_8_REV, pixelBuffer);
        }
        pixelBuffer.get(pixelValues);
        flipVertically(pixelValues, width, height);
        if (!framebufferEnabled) {
            BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            image.setRGB(0, 0, width, height, pixelValues, 0, width);
            return image;
        }
        BufferedImage image = new BufferedImage(
                framebuffer.framebufferWidth, framebuffer.framebufferHeight,
                BufferedImage.TYPE_INT_RGB);
        int top = framebuffer.framebufferTextureHeight - framebuffer.framebufferHeight;
        for (int y = top; y < framebuffer.framebufferTextureHeight; y++) {
            for (int x = 0; x < framebuffer.framebufferWidth; x++) {
                image.setRGB(x, y - top,
                        pixelValues[y * framebuffer.framebufferTextureWidth + x]);
            }
        }
        return image;
    }

    /** Vertical flip; the equivalent TextureUtil helper has no MCP name on 1.7.10. */
    private static void flipVertically(int[] pixels, int width, int height) {
        int[] row = new int[width];
        for (int y = 0; y < height / 2; y++) {
            System.arraycopy(pixels, y * width, row, 0, width);
            System.arraycopy(pixels, (height - 1 - y) * width, pixels, y * width, width);
            System.arraycopy(row, 0, pixels, (height - 1 - y) * width, width);
        }
    }

    private static BufferedImage scaledCapture(BufferedImage source) {
        if (source == null) return null;
        double scale = Math.min(1.0D, Math.min(
                MAX_CAPTURE_WIDTH / (double) source.getWidth(),
                MAX_CAPTURE_HEIGHT / (double) source.getHeight()));
        if (scale >= 1.0D) return source;
        int width = Math.max(1, (int) Math.round(source.getWidth() * scale));
        int height = Math.max(1, (int) Math.round(source.getHeight() * scale));
        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = result.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            graphics.drawImage(source, 0, 0, width, height, null);
        } finally {
            graphics.dispose();
        }
        return result;
    }
}
