package neofontrender.api.client.settings;

import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import neofontrender.client.gui.component.business.NfrSettingsControls;

/** Services supplied to a contributed settings page. */
@SideOnly(Side.CLIENT)
public final class NfrSettingsPageContext {
    private final NfrSettingsControls controls;
    private final Runnable refresh;

    public NfrSettingsPageContext(NfrSettingsControls controls, Runnable refresh) {
        this.controls = controls;
        this.refresh = refresh;
    }

    public NfrSettingsControls controls() { return controls; }
    /** Rebuilds this page while retaining every built-in and extension draft. */
    public void refresh() { refresh.run(); }
}
