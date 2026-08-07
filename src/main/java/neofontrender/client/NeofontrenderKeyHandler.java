package neofontrender.client;

import net.minecraft.client.settings.KeyBinding;
import cpw.mods.fml.client.registry.ClientRegistry;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.InputEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import org.lwjgl.input.Keyboard;
import neofontrender.client.gui.NeofontrenderConfigScreen;
import neofontrender.client.gui.NeofontrenderEmojiTestScreen;

/**
 * Handles the mod's key bindings on the client side.
 */
@SideOnly(Side.CLIENT)
public final class NeofontrenderKeyHandler {
    private static final String BRAND_SUFFIX = NeofontrenderBranding.suffix();

    public static final KeyBinding OPEN_CONFIG = new KeyBinding(
            "key.neofontrender.openConfig." + BRAND_SUFFIX,
            Keyboard.KEY_O,
            "key.categories.neofontrender." + BRAND_SUFFIX
    );

    public static final KeyBinding OPEN_EMOJI_TEST = new KeyBinding(
            "key.neofontrender.emojiTest",
            Keyboard.KEY_P,
            "key.categories.neofontrender." + BRAND_SUFFIX
    );

    private NeofontrenderKeyHandler() {}

    public static void init() {
        ClientRegistry.registerKeyBinding(OPEN_CONFIG);
        ClientRegistry.registerKeyBinding(OPEN_EMOJI_TEST);
        FMLCommonHandler.instance().bus().register(new NeofontrenderKeyHandler());
    }

    @SubscribeEvent
    public void onKeyInput(InputEvent.KeyInputEvent event) {
        if (OPEN_CONFIG.isPressed()) {
            NeofontrenderConfigScreen.open();
        }
        if (OPEN_EMOJI_TEST.isPressed()) {
            NeofontrenderEmojiTestScreen.open();
        }
    }
}
