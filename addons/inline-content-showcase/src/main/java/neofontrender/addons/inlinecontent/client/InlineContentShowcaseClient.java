package neofontrender.addons.inlinecontent.client;

import net.minecraft.client.settings.KeyBinding;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraft.util.ResourceLocation;
import net.minecraft.client.renderer.block.model.ModelBakery;
import neofontrender.addons.inlinecontent.InlineContentShowcaseMod;
import neofontrender.addons.inlinecontent.ShowcaseItems;
import org.lwjgl.input.Keyboard;

public final class InlineContentShowcaseClient {
    private static final InlineContentShowcaseClient INSTANCE = new InlineContentShowcaseClient();
    private static final KeyBinding OPEN = new KeyBinding(
            "key.neofontrender_inline_content_showcase.open", Keyboard.KEY_F9,
            "key.categories.neofontrender_inline_content_showcase");

    private InlineContentShowcaseClient() {}

    public static void preInit() {
        ResourceLocation location = new ResourceLocation(InlineContentShowcaseMod.MOD_ID,
                "aminobenzo_18_crown_6_solution");
        ModelBakery.registerItemVariants(ShowcaseItems.AMINOBENZO_CROWN_ETHER_SOLUTION, location);
        ModelLoader.setCustomModelResourceLocation(ShowcaseItems.AMINOBENZO_CROWN_ETHER_SOLUTION,
                0, new ModelResourceLocation(location, "inventory"));
        ResourceLocation typst = new ResourceLocation(InlineContentShowcaseMod.MOD_ID,
                "typst_chemistry_demonstrator");
        ModelBakery.registerItemVariants(ShowcaseItems.TYPST_CHEMISTRY_DEMONSTRATOR, typst);
        ModelLoader.setCustomModelResourceLocation(ShowcaseItems.TYPST_CHEMISTRY_DEMONSTRATOR,
                0, new ModelResourceLocation(typst, "inventory"));
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
