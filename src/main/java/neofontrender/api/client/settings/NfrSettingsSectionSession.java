package neofontrender.api.client.settings;

import com.cleanroommc.modularui.api.widget.IWidget;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

import java.util.List;

/** Screen-local lifecycle for controls contributed to a built-in settings section. */
@SideOnly(Side.CLIENT)
public interface NfrSettingsSectionSession {
    List<IWidget> createControls(NfrSettingsPageContext context);
    default void preview() {}
    default void apply() {}
    default void cancel() {}
}
