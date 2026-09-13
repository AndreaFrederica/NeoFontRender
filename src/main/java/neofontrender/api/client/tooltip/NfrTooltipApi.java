package neofontrender.api.client.tooltip;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.item.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Public client tooltip API. Integrations submit a description of the tooltip
 * and its visual content; the registered renderer owns measurement, placement
 * and drawing. Visual nodes deliberately contain no screen coordinates.
 */
public final class NfrTooltipApi {
    public interface Renderer {
        boolean render(TooltipDocument document, int mouseX, int mouseY, FontRenderer font);
    }

    /** Immutable, coordinate-free description of one tooltip. */
    public static final class TooltipDocument {
        public final ItemStack stack;
        public final List<String> lines;
        public final List<VisualNode> nodes;

        public TooltipDocument(ItemStack stack, List<String> lines, List<? extends VisualNode> nodes) {
            this.stack = stack;
            this.lines = immutableCopy(lines);
            this.nodes = immutableCopy(nodes);
        }

        public static Builder builder(ItemStack stack, List<String> lines) {
            return new Builder(stack, lines);
        }

        public static final class Builder {
            private final ItemStack stack;
            private final List<String> lines;
            private final List<VisualNode> nodes = new ArrayList<>();

            private Builder(ItemStack stack, List<String> lines) {
                this.stack = stack;
                this.lines = lines == null ? Collections.<String>emptyList() : lines;
            }

            public Builder add(VisualNode node) {
                if (node != null) nodes.add(node);
                return this;
            }

            public Builder addAll(Iterable<? extends VisualNode> values) {
                if (values != null) for (VisualNode node : values) add(node);
                return this;
            }

            public TooltipDocument build() {
                return new TooltipDocument(stack, lines, nodes);
            }
        }
    }

    /** A coordinate-free visual element understood by the modern renderer. */
    public interface VisualNode {
        Kind kind();
        default int width(FontRenderer font) { return 0; }
        default int height(FontRenderer font) { return 0; }
    }

    public enum Kind { ITEM, ITEM_ROW, TEXT, SPACER }

    /** An item icon and optional quantity label. */
    public static final class ItemNode implements VisualNode {
        private final ItemStack stack;
        private final long amount;
        private final boolean showAmount;

        public ItemNode(ItemStack stack, long amount, boolean showAmount) {
            this.stack = stack == null ? ItemStack.EMPTY : stack.copy();
            this.amount = amount;
            this.showAmount = showAmount;
        }

        public ItemStack stack() { return stack; }
        public long amount() { return amount; }
        public boolean showAmount() { return showAmount; }
        @Override public Kind kind() { return Kind.ITEM; }
        @Override public int width(FontRenderer font) { return 17; }
        @Override public int height(FontRenderer font) { return 17; }
    }

    /** A row of item nodes. The renderer chooses wrapping and spacing. */
    public static final class ItemRowNode implements VisualNode {
        private final List<ItemNode> items;
        private final boolean hasMore;

        public ItemRowNode(List<? extends ItemNode> items, boolean hasMore) {
            this.items = immutableCopy(items);
            this.hasMore = hasMore;
        }

        public List<ItemNode> items() { return items; }
        public boolean hasMore() { return hasMore; }
        @Override public Kind kind() { return Kind.ITEM_ROW; }
        @Override public int width(FontRenderer font) { return items.size() * 17 + (hasMore ? 10 : 0); }
        @Override public int height(FontRenderer font) { return 17; }
    }

    /** A renderer-owned text fragment, useful for labels next to visual rows. */
    public static final class TextNode implements VisualNode {
        private final String text;
        private final int color;
        private final boolean shadow;

        public TextNode(String text, int color, boolean shadow) {
            this.text = text == null ? "" : text;
            this.color = color;
            this.shadow = shadow;
        }

        public String text() { return text; }
        public int color() { return color; }
        public boolean shadow() { return shadow; }
        @Override public Kind kind() { return Kind.TEXT; }
        @Override public int width(FontRenderer font) { return font == null ? 0 : font.getStringWidth(text); }
        @Override public int height(FontRenderer font) { return 9; }
    }

    /** A renderer-owned vertical gap. Height is interpreted in layout units. */
    public static final class SpacerNode implements VisualNode {
        private final int height;

        public SpacerNode(int height) { this.height = Math.max(0, height); }
        public int height() { return height; }
        @Override public Kind kind() { return Kind.SPACER; }
    }

    private static volatile Renderer renderer;
    /** Thread-local escape hatch used by integrations that intentionally render their native tooltip. */
    private static final ThreadLocal<Boolean> NATIVE_BYPASS = new ThreadLocal<>();

    private NfrTooltipApi() {}

    public static void register(Renderer value) { renderer = value; }

    public static void unregister(Renderer value) {
        if (renderer == value) renderer = null;
    }

    public static boolean isRegistered() { return renderer != null; }

    /**
     * Marks the current render call as native-tooltip work. UIE integrations
     * should skip handling Forge tooltip events while this scope is active.
     */
    public static void beginNativeBypass() { NATIVE_BYPASS.set(Boolean.TRUE); }

    /** Ends the current native-tooltip scope. */
    public static void endNativeBypass() { NATIVE_BYPASS.remove(); }

    /** Returns whether the current thread is rendering through a native backend. */
    public static boolean isNativeBypass() { return Boolean.TRUE.equals(NATIVE_BYPASS.get()); }

    public static boolean render(ItemStack stack, List<String> lines, int mouseX, int mouseY,
                                 FontRenderer font) {
        return render(new TooltipDocument(stack, lines, Collections.<VisualNode>emptyList()),
                mouseX, mouseY, font);
    }

    public static boolean render(TooltipDocument document, int mouseX, int mouseY,
                                 FontRenderer font) {
        Renderer value = renderer;
        return value != null && document != null && value.render(document, mouseX, mouseY, font);
    }

    private static <T> List<T> immutableCopy(List<? extends T> source) {
        if (source == null || source.isEmpty()) return Collections.emptyList();
        return Collections.unmodifiableList(new ArrayList<T>(source));
    }
}
