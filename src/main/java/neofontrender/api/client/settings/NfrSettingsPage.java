package neofontrender.api.client.settings;

import net.minecraft.client.resources.I18n;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/** Describes one settings page contributed by another mod. */
@SideOnly(Side.CLIENT)
public interface NfrSettingsPage {
    /** Globally unique stable id, normally {@code modid:page}. */
    String id();
    /** Translation key shown in the side bar and title. */
    String titleKey();
    /** Display title. Override when an integration owns a separate localization source. */
    default String title() { return I18n.format(titleKey()); }
    /** Higher values are placed later, after NFR's built-in pages. */
    default int order() { return 1000; }
    /** Creates a screen-local draft. Called once whenever the NFR settings screen opens. */
    NfrSettingsPageSession createSession();
}
