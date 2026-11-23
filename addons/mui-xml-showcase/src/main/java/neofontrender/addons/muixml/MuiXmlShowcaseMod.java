package neofontrender.addons.muixml;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

@Mod(
        modid = MuiXmlShowcaseMod.MOD_ID,
        name = MuiXmlShowcaseMod.MOD_NAME,
        version = MuiXmlShowcaseMod.VERSION,
        dependencies = "required-after:modularui@[3.2.0-nfr.1,)",
        acceptedMinecraftVersions = "[1.12,1.13)"
)
public final class MuiXmlShowcaseMod {
    public static final String MOD_ID = "neofontrender_mui_xml_showcase";
    public static final String MOD_NAME = "MUI XML Showcase";
    public static final String VERSION = "0.1.0";

    @SidedProxy(
            clientSide = "neofontrender.addons.muixml.client.ShowcaseClientProxy",
            serverSide = "neofontrender.addons.muixml.ShowcaseCommonProxy")
    public static ShowcaseCommonProxy proxy;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        ShowcaseBlocks.registerTileEntities();
        proxy.preInit();
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        proxy.init();
    }
}
