package neofontrender.addons.muixml.client;

import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.item.Item;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import neofontrender.addons.muixml.ShowcaseCommonProxy;
import neofontrender.addons.muixml.ShowcaseBlocks;
import org.lwjgl.input.Keyboard;

public final class ShowcaseClientProxy extends ShowcaseCommonProxy {
    private static final KeyBinding OPEN = new KeyBinding(
            "key.neofontrender_mui_xml_showcase.open", Keyboard.KEY_F8,
            "key.categories.neofontrender_mui_xml_showcase");

    @Override
    public void preInit() {
        registerModel(ShowcaseBlocks.XML_CHEST);
        registerModel(ShowcaseBlocks.XML_FURNACE);
    }

    @Override
    public void init() {
        ClientRegistry.registerKeyBinding(OPEN);
        MinecraftForge.EVENT_BUS.register(this);
        ClientCommandHandler.instance.registerCommand(new ShowcaseCommand());
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (OPEN.isPressed()) ShowcaseScreen.open();
    }

    private static void registerModel(net.minecraft.block.Block block) {
        ModelLoader.setCustomModelResourceLocation(Item.getItemFromBlock(block), 0,
                new ModelResourceLocation(block.getRegistryName(), "inventory"));
    }
}
