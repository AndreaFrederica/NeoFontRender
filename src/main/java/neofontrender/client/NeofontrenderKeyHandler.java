package neofontrender.client;

import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.client.event.InputUpdateEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.input.Keyboard;
import neofontrender.client.gui.NeofontrenderConfigScreen;

/**
 * Handles the mod's key bindings on the client side.
 */
@SideOnly(Side.CLIENT)
public final class NeofontrenderKeyHandler {

    public static final KeyBinding OPEN_CONFIG = new KeyBinding(
            "key.neofontrender.openConfig",
            Keyboard.KEY_O,
            "key.categories.neofontrender"
    );

    private NeofontrenderKeyHandler() {}

    public static void init() {
        ClientRegistry.registerKeyBinding(OPEN_CONFIG);
        MinecraftForge.EVENT_BUS.register(new NeofontrenderKeyHandler());
    }

    @SubscribeEvent
    public void onKeyInput(InputEvent.KeyInputEvent event) {
        if (OPEN_CONFIG.isPressed()) {
            NeofontrenderConfigScreen.open();
        }
    }
}
