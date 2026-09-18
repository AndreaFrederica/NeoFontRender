package neofontrender.addons.tooltips;

import net.minecraft.client.gui.FontRenderer;
import neofontrender.api.client.tooltip.NfrTooltipApi;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Converts public tooltip visual nodes into retained geometry without painting them. */
final class TooltipVisualNodeLayout {
    private TooltipVisualNodeLayout() {}

    static TooltipLayoutEngine.Node adapt(NfrTooltipApi.VisualNode node, FontRenderer font) {
        if (node == null) return new TooltipLayoutEngine.Leaf(0, 0);
        if (node instanceof NfrTooltipApi.GroupNode) {
            NfrTooltipApi.GroupNode group = (NfrTooltipApi.GroupNode) node;
            List<TooltipLayoutEngine.Node> children = new ArrayList<>();
            for (NfrTooltipApi.VisualNode child : group.children()) children.add(adapt(child, font));
            TooltipLayoutEngine.Direction direction = group.direction() == NfrTooltipApi.LayoutDirection.HORIZONTAL
                    ? TooltipLayoutEngine.Direction.ROW : TooltipLayoutEngine.Direction.COLUMN;
            TooltipLayoutEngine.Alignment alignment = group.alignment() == NfrTooltipApi.LayoutAlignment.CENTER
                    ? TooltipLayoutEngine.Alignment.CENTER
                    : group.alignment() == NfrTooltipApi.LayoutAlignment.END
                    ? TooltipLayoutEngine.Alignment.END : TooltipLayoutEngine.Alignment.START;
            return new TooltipLayoutEngine.Flow(direction, alignment, group.gap(),
                    TooltipLayoutEngine.Insets.none(), children);
        }
        if (node instanceof NfrTooltipApi.GridNode) {
            NfrTooltipApi.GridNode grid = (NfrTooltipApi.GridNode) node;
            List<TooltipLayoutEngine.Node> children = new ArrayList<>();
            for (NfrTooltipApi.VisualNode child : grid.children()) children.add(adapt(child, font));
            return new TooltipLayoutEngine.Grid(grid.columns(), grid.gap(), children);
        }
        return new TooltipLayoutEngine.Leaf(node.width(font), node.height(font));
    }

    static TooltipLayoutEngine.Measurement measure(List<? extends NfrTooltipApi.VisualNode> nodes,
                                                   FontRenderer font, int maxWidth, int maxHeight) {
        List<TooltipLayoutEngine.Node> adapted = new ArrayList<>();
        if (nodes != null) for (NfrTooltipApi.VisualNode node : nodes) adapted.add(adapt(node, font));
        TooltipLayoutEngine.Flow root = new TooltipLayoutEngine.Flow(
                TooltipLayoutEngine.Direction.COLUMN, TooltipLayoutEngine.Alignment.START, 0,
                TooltipLayoutEngine.Insets.none(), adapted);
        return root.measure(new TooltipLayoutEngine.Constraints(maxWidth, maxHeight));
    }
}
