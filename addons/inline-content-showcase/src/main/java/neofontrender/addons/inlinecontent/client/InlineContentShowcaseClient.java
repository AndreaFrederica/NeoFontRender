package neofontrender.addons.inlinecontent.client;

import net.minecraft.client.settings.KeyBinding;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraft.util.ResourceLocation;
import neofontrender.addons.inlinecontent.InlineContentShowcaseMod;
import neofontrender.addons.inlinecontent.ShowcaseItems;
import org.lwjgl.input.Keyboard;

@Mod.EventBusSubscriber(modid = InlineContentShowcaseMod.MOD_ID, value = Side.CLIENT)
public final class InlineContentShowcaseClient {
    private static final InlineContentShowcaseClient INSTANCE = new InlineContentShowcaseClient();
    private static final KeyBinding OPEN = new KeyBinding(
            "key.neofontrender_inline_content_showcase.open", Keyboard.KEY_F9,
            "key.categories.neofontrender_inline_content_showcase");

    private InlineContentShowcaseClient() {}

    @SubscribeEvent
    public static void registerModels(ModelRegistryEvent event) {
        // Forge keys model maps by Item.delegate. Its name (and hash) is assigned only by
        // item registration, after preInit. Binding earlier aliases all unnamed delegates.
        for (var item : ShowcaseItems.ALL) {
            ResourceLocation location = item.getRegistryName();
            ModelLoader.setCustomModelResourceLocation(item, 0, new ModelResourceLocation(location, "inventory"));
        }
    }

    public static void init() {
        ClientRegistry.registerKeyBinding(OPEN);
        MinecraftForge.EVENT_BUS.register(INSTANCE);
        ClientCommandHandler.instance.registerCommand(new InlineContentShowcaseCommand());
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END && OPEN.isPressed()) InlineContentShowcaseScreen.open();
    }
}
