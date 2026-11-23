package neofontrender.addons.hud.compositor;

import java.awt.Rectangle;
import neofontrender.client.CosmicCacheOverlayRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;

/** Optional UIE bridge that hosts the NFR cache graph in the shared HUD compositor. */
public final class CosmicCacheHudSurface implements HudSurface {
    public CosmicCacheHudSurface() {
        CosmicCacheOverlayRenderer.markCompositorSurfaceRegistered();
    }

    @Override public String id() { return "neofontrender:cosmic-cache"; }

    @Override public Rectangle bounds() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null) return new Rectangle();
        ScaledResolution resolution = new ScaledResolution(mc);
        return CosmicCacheOverlayRenderer.panelBounds(resolution.getScaledWidth(), resolution.getScaledHeight());
    }

    @Override public boolean visible() {
        return CosmicCacheOverlayRenderer.isPinned();
    }

    @Override public void render(float partialTicks) {
        CosmicCacheOverlayRenderer.renderPinned(partialTicks);
    }

    @Override public boolean acceptsPointer() { return CosmicCacheOverlayRenderer.editModeActive(); }

    @Override public void mouseInput(int mouseX, int mouseY, int button, boolean pressed) {
        CosmicCacheOverlayRenderer.handlePointer(mouseX, mouseY, button, pressed);
    }
}
