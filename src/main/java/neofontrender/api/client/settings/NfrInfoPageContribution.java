package neofontrender.api.client.settings;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

import java.util.List;

/** Content contributed by another mod to NFR's About or Licenses page. */
@SideOnly(Side.CLIENT)
public interface NfrInfoPageContribution {
    /** Globally unique stable id, normally {@code modid:section}. */
    String id();
    NfrInfoPage page();
    /** Higher values are rendered later. */
    default int order() { return 1000; }
    List<NfrInfoLine> lines();
}
