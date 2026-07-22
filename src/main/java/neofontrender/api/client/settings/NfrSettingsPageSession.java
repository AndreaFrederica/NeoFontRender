package neofontrender.api.client.settings;

import com.cleanroommc.modularui.api.widget.IWidget;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/** Screen-local lifecycle for an extension settings page. */
@SideOnly(Side.CLIENT)
public interface NfrSettingsPageSession {
    IWidget createView(NfrSettingsPageContext context);
    /** Called for controls that request a live preview. */
    default void preview() {}
    /** Persist the draft when the shared Apply button is pressed. */
    default void apply() {}
    /** Restore runtime state when the shared Cancel button is pressed. */
    default void cancel() {}
}
