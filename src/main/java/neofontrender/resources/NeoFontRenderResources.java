package neofontrender.resources;

import cpw.mods.fml.common.Mod;
import neofontrender.Tags;

/**
 * Resource-only Forge container for the optional bundled font pack.
 *
 * <p>Forge 1.12 only adds discovered mod containers to its resource search path. Keeping this
 * empty container lets the font assets ship as a standalone mod JAR, while the full distribution
 * can carry this container and the main renderer container in the same file.</p>
 */
@Mod(
        modid = NeoFontRenderResources.MOD_ID,
        name = "Revo Font Resources",
        version = Tags.VERSION,
        dependencies = "required-after:" + Tags.MOD_ID + "@[" + Tags.VERSION + ",)",
        acceptedMinecraftVersions = "[1.7.10]",
        acceptableRemoteVersions = "*"
)
public final class NeoFontRenderResources {
    public static final String MOD_ID = "neofontrender_resources";

    public NeoFontRenderResources() {
    }
}
