package neofontrender.addons.api.ui.navigation;

import net.minecraft.client.gui.GuiScreen;

import java.util.Collection;
import java.util.List;

public interface UiTreeSnapshot {
    long revision();
    GuiScreen screen();
    UiInteractionMode interactionMode();
    List<UiNodeId> roots();
    UiNode node(UiNodeId id);
    Collection<UiNode> nodes();
    UiNodeId focusedNodeId();
    UiNodeId activeScopeId();
}
