package neofontrender.addons.muixml.tile;

import com.cleanroommc.modularui.factory.PosGuiData;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.UISettings;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;

public final class ShowcaseChestTile extends ShowcaseInventoryTile {

    public ShowcaseChestTile() {
        super(ShowcaseProtocolTemplates.CHEST);
    }

    @Override
    public ModularPanel buildUI(PosGuiData data, PanelSyncManager syncManager, UISettings settings) {
        return createInventoryPanel(syncManager);
    }

    @Override
    public String getScreenName() {
        return "mui_xml_chest";
    }

    @Override
    public String getXmlResource() {
        return "screens/chest.xml";
    }

    @Override
    public String getProtocolResource() {
        return "protocols/chest.xml";
    }
}
