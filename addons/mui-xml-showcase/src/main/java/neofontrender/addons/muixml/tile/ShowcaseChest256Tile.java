package neofontrender.addons.muixml.tile;

import com.cleanroommc.modularui.factory.PosGuiData;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.UISettings;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;

public final class ShowcaseChest256Tile extends ShowcaseInventoryTile {
    public ShowcaseChest256Tile() { super(ShowcaseProtocolTemplates.CHEST_256); }
    @Override public ModularPanel buildUI(PosGuiData data, PanelSyncManager syncManager, UISettings settings) {
        return createInventoryPanel(syncManager).size(342, 480);
    }
    @Override public String getScreenName() { return "mui_xml_chest_256"; }
    @Override public String getXmlResource() { return "screens/chest-256.xml"; }
    @Override public String getProtocolResource() { return "protocols/chest-256.xml"; }
}
