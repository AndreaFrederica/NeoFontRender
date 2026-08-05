package neofontrender.api.client.settings;

import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/** Describes controls appended to one of NFR's built-in settings sections. */
@SideOnly(Side.CLIENT)
public interface NfrSettingsSectionContribution {
    /** Globally unique stable id, normally {@code modid:feature}. */
    String id();
    NfrSettingsSection section();
    default int order() { return 1000; }
    NfrSettingsSectionSession createSession();
}
