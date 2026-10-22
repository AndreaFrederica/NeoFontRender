package neofontrender.addons.hud.compositor;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/** Routes world mouse events to interactive HUD surfaces before Minecraft consumes them. */
public final class HudCompositorInputHandler {
    public static final HudCompositorInputHandler INSTANCE = new HudCompositorInputHandler();

    private HudCompositorInputHandler() {}

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void mouseInput(MouseEvent event) {
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft == null || minecraft.displayWidth <= 0 || minecraft.displayHeight <= 0) return;
        ScaledResolution resolution = new ScaledResolution(minecraft);
        int x = event.getX() * resolution.getScaledWidth() / minecraft.displayWidth;
        int y = resolution.getScaledHeight()
                - event.getY() * resolution.getScaledHeight() / minecraft.displayHeight - 1;
        if (HudWindowCompositor.INSTANCE.mouseInput(x, y, event.getButton(), event.isButtonstate())) {
            event.setCanceled(true);
        }
    }
}
