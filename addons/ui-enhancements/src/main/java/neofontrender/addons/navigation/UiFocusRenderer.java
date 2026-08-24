package neofontrender.addons.navigation;

import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import neofontrender.addons.api.ui.navigation.UiFocusState;
import neofontrender.addons.api.ui.navigation.UiNode;
import neofontrender.addons.api.ui.navigation.UiRect;
import neofontrender.addons.api.ui.navigation.UiRole;
import neofontrender.addons.api.ui.navigation.UiTreeSnapshot;

public final class UiFocusRenderer {
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void afterDraw(GuiScreenEvent.DrawScreenEvent.Post event) {
        UiNavigationRuntime runtime = UiNavigationRuntime.instance();
        UiFocusState focus = runtime.focusState();
        if (!focus.focusVisible() || focus.focusedNodeId() == null) return;
        UiTreeSnapshot tree = runtime.treeForRender();
        if (tree.screen() != event.getGui()) return;
        UiNode node = tree.node(focus.focusedNodeId());
        if (node == null || !node.visible() || node.visibleBounds().isEmpty()) return;

        UiRect bounds = node.visibleBounds();
        int inset = node.role() == UiRole.INVENTORY_SLOT ? 0 : 2;
        int color = focus.editing() ? 0xFFE0A32A : 0xFF35C2B2;
        int left = bounds.left - inset;
        int top = bounds.top - inset;
        int right = bounds.right + inset;
        int bottom = bounds.bottom + inset;

        GlStateManager.pushMatrix();
        GlStateManager.translate(0.0F, 0.0F, 600.0F);
        Gui.drawRect(left, top, right, top + 2, color);
        Gui.drawRect(left, bottom - 2, right, bottom, color);
        Gui.drawRect(left, top + 2, left + 2, bottom - 2, color);
        Gui.drawRect(right - 2, top + 2, right, bottom - 2, color);
        GlStateManager.popMatrix();
    }
}
