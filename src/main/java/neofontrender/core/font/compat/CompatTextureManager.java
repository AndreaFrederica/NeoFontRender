package neofontrender.core.font.compat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Private texture registry for the compatibility glyph pages.
 *
 * <p>SmoothFont keeps its pages in a {@code FontTextureManager} of its own instead of Minecraft's
 * {@code TextureManager}, and this class mirrors that ownership model. The reason is not style:
 * vanilla frees texture names by number. {@code TextureManager.deleteTexture(location)} resolves
 * the location and then calls {@code TextureUtil.deleteTexture(id)}, and Forge's
 * {@code TextureUtil.prepareImage} deletes and recreates the name on every upload. Any page that
 * shares that id space can therefore free a name a {@code SimpleTexture} still holds (or the
 * reverse), after which the next {@code glGenTextures} hands the number to whoever asks next. A
 * glyph page that ends up owning a panorama name renders as text inside the main-menu skybox.</p>
 *
 * <p>Owning the map here means the engine only ever frees names it created, and it can drop a page
 * by object identity instead of "whatever is registered under this location right now". A mistake
 * in this class is consequently limited to glyph pages.</p>
 */
final class CompatTextureManager {

    private final Map<ResourceLocation, CompatPageTexture> textures = new LinkedHashMap<>();
    private final String owner;

    CompatTextureManager(String owner) {
        this.owner = owner;
    }

    synchronized CompatPageTexture get(ResourceLocation location) {
        return location == null ? null : textures.get(location);
    }

    /** Registers a freshly uploaded page. The caller must not reuse the same location. */
    synchronized void put(ResourceLocation location, CompatPageTexture texture) {
        if (location == null || texture == null) return;
        CompatPageTexture previous = textures.put(location, texture);
        if (previous != null && previous != texture) {
            delete(previous);
        }
    }

    /** Binds the page's own GL name, bypassing vanilla's location lookup. */
    boolean bind(ResourceLocation location) {
        CompatPageTexture texture;
        synchronized (this) {
            texture = location == null ? null : textures.get(location);
        }
        if (texture == null) return false;
        // A glyph page is a normal 2D sampler for the fixed-function text path. Always bind it
        // on texture unit 0; inheriting the lightmap unit from a caller lets the page participate
        // in unrelated GUI quads after the text call returns.
        GlStateManager.setActiveTexture(GL13.GL_TEXTURE0);
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        GlStateManager.bindTexture(texture.getGlTextureId());
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture.getGlTextureId());
        return true;
    }

    /** Drops {@code texture} and frees its name; other pages registered under it are untouched. */
    synchronized void release(CompatPageTexture texture) {
        if (texture == null) return;
        textures.values().remove(texture);
        delete(texture);
    }

    /** Drops the location only if it still points at {@code texture}, then frees the page. */
    synchronized void release(ResourceLocation location, CompatPageTexture texture) {
        if (texture == null) return;
        if (location != null && textures.get(location) == texture) {
            textures.remove(location);
        }
        delete(texture);
    }

    synchronized void clear() {
        for (CompatPageTexture texture : textures.values()) {
            delete(texture);
        }
        textures.clear();
    }

    synchronized int size() {
        return textures.size();
    }

    String owner() {
        return owner;
    }

    /**
     * Frees a name on the thread that owns the GL context. A resource reload or an async mapping
     * thread can reach this engine, and deleting names from there corrupts the render thread's id
     * space; scheduling the deletion is the same trade-off SmoothFont makes with {@code needReload}.
     */
    private void delete(CompatPageTexture texture) {
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft != null && !minecraft.isCallingFromMinecraftThread()) {
            minecraft.addScheduledTask(() -> deleteOnRenderThread(texture));
            return;
        }
        deleteOnRenderThread(texture);
    }

    private void deleteOnRenderThread(CompatPageTexture texture) {
        if (texture == null) return;
        int textureId = texture.getGlTextureId();
        int active = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
        int activeBinding = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);

        // A deleted name must not remain bound on the active unit. Also check unit 0 because the
        // compat renderer deliberately uses it even when the caller entered with another unit.
        unbindIfCurrent(GL13.GL_TEXTURE0, textureId);
        if (active != GL13.GL_TEXTURE0) {
            unbindIfCurrent(active, textureId);
        }
        selectTextureUnit(active);
        if (activeBinding != textureId) {
            // Keep the driver's binding/cache in sync when the active unit was not the page.
            GlStateManager.bindTexture(activeBinding);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, activeBinding);
        }
        texture.deleteGlTexture();
    }

    private static void unbindIfCurrent(int unit, int textureId) {
        selectTextureUnit(unit);
        if (GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D) == textureId) {
            GlStateManager.bindTexture(0);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
        }
    }

    private static void selectTextureUnit(int unit) {
        GlStateManager.setActiveTexture(unit);
        GL13.glActiveTexture(unit);
    }
}
