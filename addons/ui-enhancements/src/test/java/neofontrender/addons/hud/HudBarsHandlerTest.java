package neofontrender.addons.hud;

import net.minecraftforge.client.event.RenderGameOverlayEvent;
import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HudBarsHandlerTest {
    @Test
    void claimsStatusLayersBeforeLowPriorityCompatibilityRenderers() throws Exception {
        Method render = HudBarsHandler.class.getDeclaredMethod(
                "render", RenderGameOverlayEvent.Pre.class);
        SubscribeEvent subscription = render.getAnnotation(SubscribeEvent.class);

        assertEquals(EventPriority.NORMAL, subscription.priority());
    }
}
