package neofontrender.api.client.tooltip;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.item.ItemStack;
import net.minecraft.inventory.EntityEquipmentSlot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.Map;

/**
 * Public client tooltip API. Integrations submit a description of the tooltip
 * and its visual content; the registered renderer owns measurement, placement
 * and drawing. Visual nodes deliberately contain no screen coordinates.
 */
public final class NfrTooltipApi {
    private static final String ANCHOR_PREFIX = "<nfr:anchor";

    public interface Renderer {
        boolean render(TooltipDocument document, int mouseX, int mouseY, FontRenderer font);
    }

    /**
     * Supplies coordinate-free visual content for a normal Forge tooltip.
     * Providers are composable so integrations do not replace one another.
     */
    public interface DocumentProvider {
        Optional<TooltipDocument> create(ItemStack stack, List<String> lines);
    }

    /** Final document transformation invoked after all tooltip source providers ran. */
    public interface DocumentFinalizer {
        TooltipDocument finalize(TooltipDocument document);
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

    public enum Kind { ITEM, ITEM_ROW, TEXT, SPACER, PREVIEW, GROUP, GRID }

    public enum LayoutDirection { HORIZONTAL, VERTICAL }
    public enum LayoutAlignment { START, CENTER, END }

    /** Identifies the rendering backend used by a preview node. */
    public enum PreviewKind { ITEM_STACK, ARMOR, CUSTOM }

    /** Immutable dimensions returned by a preview renderer. */
    public static final class PreviewSize {
        private final int width;
        private final int height;

        public PreviewSize(int width, int height) {
            this.width = Math.max(0, width);
            this.height = Math.max(0, height);
        }

        public int width() { return width; }
        public int height() { return height; }
    }

    /** Extra GUI pixels painted outside a preview's measured model rectangle. */
    public static final class PreviewInsets {
        public static final PreviewInsets NONE = new PreviewInsets(0, 0, 0, 0);

        private final int left;
        private final int top;
        private final int right;
        private final int bottom;

        public PreviewInsets(int left, int top, int right, int bottom) {
            this.left = Math.max(0, left);
            this.top = Math.max(0, top);
            this.right = Math.max(0, right);
            this.bottom = Math.max(0, bottom);
        }

        public int left() { return left; }
        public int top() { return top; }
        public int right() { return right; }
        public int bottom() { return bottom; }
    }

    /** Coordinate-free description of a model preview. */
    public interface PreviewRequest {
        PreviewKind previewKind();

        /** Stable renderer key. Custom requests should override this value. */
        default String rendererId() {
            PreviewKind kind = previewKind();
            return kind == null ? "" : kind.name().toLowerCase(java.util.Locale.ROOT);
        }

        default List<String> effects() { return Collections.emptyList(); }

        /** Optional request-specific appearance sound; null uses renderer configuration. */
        default PreviewSound sound() { return null; }
    }

    /** Immutable sound parameters attached to one preview request. */
    public static final class PreviewSound {
        private final boolean enabled;
        private final String eventId;
        private final float volume;
        private final float pitch;
        private final int cooldownMillis;

        public PreviewSound(boolean enabled, String eventId, float volume, float pitch,
                            int cooldownMillis) {
            this.enabled = enabled;
            this.eventId = eventId == null ? "" : eventId.trim();
            this.volume = Math.max(0.0F, Math.min(1.0F, volume));
            this.pitch = Math.max(0.01F, pitch);
            this.cooldownMillis = Math.max(0, cooldownMillis);
        }

        public boolean enabled() { return enabled; }
        public String eventId() { return eventId; }
        public float volume() { return volume; }
        public float pitch() { return pitch; }
        public int cooldownMillis() { return cooldownMillis; }
    }

    /** Client-side renderer contract for preview nodes. Implementations are optional. */
    public interface PreviewRenderer {
        PreviewKind previewKind();

        /** Stable registry key. Multiple CUSTOM renderers may coexist. */
        default String id() {
            PreviewKind kind = previewKind();
            return kind == null ? "" : kind.name().toLowerCase(java.util.Locale.ROOT);
        }

        PreviewSize measure(PreviewRequest request, FontRenderer font);

        void render(PreviewRequest request, int x, int y, PreviewSize size,
                    FontRenderer font);
    }

    /** Cosmetic overlay that a preview backend may render after its model. */
    public interface PreviewEffect {
        String id();

        /** Insets reserved by layout for pixels drawn outside the model rectangle. */
        default PreviewInsets outsets(PreviewRequest request, PreviewSize size) {
            return PreviewInsets.NONE;
        }

        void render(PreviewEffectContext context);
    }

    /** Immutable state passed to a preview effect for one frame. */
    public static final class PreviewEffectContext {
        private final PreviewRequest request;
        private final int x;
        private final int y;
        private final PreviewSize size;
        private final float appearance;
        private final long timeNanos;

        public PreviewEffectContext(PreviewRequest request, int x, int y,
                                    PreviewSize size, float appearance, long timeNanos) {
            this.request = request;
            this.x = x;
            this.y = y;
            this.size = size == null ? new PreviewSize(0, 0) : size;
            this.appearance = Math.max(0.0F, Math.min(1.0F, appearance));
            this.timeNanos = timeNanos;
        }

        public PreviewRequest request() { return request; }
        public int x() { return x; }
        public int y() { return y; }
        public PreviewSize size() { return size; }
        public float appearance() { return appearance; }
        public long timeNanos() { return timeNanos; }
    }

    /** Selects a preview request for an item before the built-in fallback rules run. */
    public interface PreviewSelector {
        Optional<PreviewRequest> select(ItemStack stack, List<String> lines);
    }

    private static final CopyOnWriteArrayList<PreviewSelector> PREVIEW_SELECTORS =
            new CopyOnWriteArrayList<>();

    public static void registerPreviewSelector(PreviewSelector selector) {
        if (selector != null && !PREVIEW_SELECTORS.contains(selector)) PREVIEW_SELECTORS.add(selector);
    }

    public static void unregisterPreviewSelector(PreviewSelector selector) {
        if (selector != null) PREVIEW_SELECTORS.remove(selector);
    }

    public static Optional<PreviewRequest> selectPreview(ItemStack stack, List<String> lines) {
        for (PreviewSelector selector : PREVIEW_SELECTORS) {
            try {
                Optional<PreviewRequest> result = selector.select(stack, lines);
                if (result != null && result.isPresent()) return result;
            } catch (RuntimeException | LinkageError ignored) {
                // Optional selectors must not break normal tooltips.
            }
        }
        return Optional.empty();
    }

    /** Registry for client preview backends. */
    public static final class PreviewRegistry {
        private static final Map<String, PreviewRenderer> RENDERERS =
                new java.util.LinkedHashMap<>();
        private static final Map<String, PreviewEffect> EFFECTS =
                new java.util.LinkedHashMap<>();

        private PreviewRegistry() {}

        public static synchronized void register(PreviewRenderer renderer) {
            if (renderer != null && registryKey(renderer.id()) != null) {
                RENDERERS.put(registryKey(renderer.id()), renderer);
            }
        }

        public static synchronized void unregister(PreviewRenderer renderer) {
            String key = renderer == null ? null : registryKey(renderer.id());
            if (key != null && RENDERERS.get(key) == renderer) {
                RENDERERS.remove(key);
            }
        }

        public static synchronized PreviewRenderer find(PreviewRequest request) {
            String key = request == null ? null : registryKey(request.rendererId());
            return key == null ? null : RENDERERS.get(key);
        }

        public static synchronized void registerEffect(PreviewEffect effect) {
            if (effect != null && effect.id() != null && !effect.id().trim().isEmpty()) {
                EFFECTS.put(effect.id().trim().toLowerCase(java.util.Locale.ROOT), effect);
            }
        }

        public static synchronized void unregisterEffect(PreviewEffect effect) {
            if (effect != null && effect.id() != null
                    && EFFECTS.get(effect.id().trim().toLowerCase(java.util.Locale.ROOT)) == effect) {
                EFFECTS.remove(effect.id().trim().toLowerCase(java.util.Locale.ROOT));
            }
        }

        public static synchronized PreviewEffect findEffect(String id) {
            if (id == null) return null;
            return EFFECTS.get(id.trim().toLowerCase(java.util.Locale.ROOT));
        }

        private static String registryKey(String id) {
            if (id == null || id.trim().isEmpty()) return null;
            return id.trim().toLowerCase(java.util.Locale.ROOT);
        }
    }

    /** Rotating ItemStack model preview request. */
    public static final class ItemPreviewRequest implements PreviewRequest {
        private final ItemStack stack;
        private final float scale;
        private final float pitch;
        private final float roll;
        private final float rotationSpeed;
        private final int width;
        private final int height;
        private final List<String> effects;
        private final PreviewSound sound;

        public ItemPreviewRequest(ItemStack stack) {
            this(stack, 2.75F, -30.0F, -45.0F, -20.0F);
        }

        public ItemPreviewRequest(ItemStack stack, float scale, float pitch,
                                   float roll, float rotationSpeed) {
            this(stack, scale, pitch, roll, rotationSpeed, 30, 64,
                    Collections.<String>emptyList());
        }

        public ItemPreviewRequest(ItemStack stack, float scale, float pitch,
                                   float roll, float rotationSpeed, int width, int height,
                                   List<String> effects) {
            this(stack, scale, pitch, roll, rotationSpeed, width, height, effects, null);
        }

        public ItemPreviewRequest(ItemStack stack, float scale, float pitch,
                                   float roll, float rotationSpeed, int width, int height,
                                   List<String> effects, PreviewSound sound) {
            this.stack = stack == null ? ItemStack.EMPTY : stack.copy();
            this.scale = scale;
            this.pitch = pitch;
            this.roll = roll;
            this.rotationSpeed = rotationSpeed;
            this.width = Math.max(0, width);
            this.height = Math.max(0, height);
            this.effects = immutableCopy(effects);
            this.sound = sound;
        }

        public ItemStack stack() { return stack; }
        public float scale() { return scale; }
        public float pitch() { return pitch; }
        public float roll() { return roll; }
        public float rotationSpeed() { return rotationSpeed; }
        public int width() { return width; }
        public int height() { return height; }
        public List<String> effects() { return effects; }
        @Override public PreviewSound sound() { return sound; }
        @Override public PreviewKind previewKind() { return PreviewKind.ITEM_STACK; }
    }

    public enum ArmorPreviewModel { ARMOR_STAND, PLAYER }
    public enum ArmorPreviewMode { SINGLE_PIECE, FULL_SET }

    /** Armor preview request; the renderer supplies the temporary model. */
    public static final class ArmorPreviewRequest implements PreviewRequest {
        private final List<ItemStack> equipment;
        private final ItemStack primaryStack;
        private final EntityEquipmentSlot slot;
        private final ArmorPreviewModel model;
        private final ArmorPreviewMode mode;
        private final float scale;
        private final float pitch;
        private final float rotationSpeed;
        private final int width;
        private final int height;
        private final List<String> effects;
        private final PreviewSound sound;

        public ArmorPreviewRequest(ItemStack stack, EntityEquipmentSlot slot) {
            this(stack, slot, ArmorPreviewModel.ARMOR_STAND,
                    ArmorPreviewMode.SINGLE_PIECE, 30.0F, 25.0F, 20.0F,
                    40, 64, Collections.<String>emptyList());
        }

        public ArmorPreviewRequest(ItemStack stack, EntityEquipmentSlot slot,
                                   float scale, float pitch, float rotationSpeed) {
            this(stack, slot, ArmorPreviewModel.ARMOR_STAND,
                    ArmorPreviewMode.SINGLE_PIECE, scale, pitch, rotationSpeed,
                    40, 64, Collections.<String>emptyList());
        }

        public ArmorPreviewRequest(List<ItemStack> equipment, ArmorPreviewModel model,
                                   ArmorPreviewMode mode, float scale, float pitch,
                                   float rotationSpeed) {
            this(equipment, model, mode, scale, pitch, rotationSpeed, 40, 64,
                    Collections.<String>emptyList());
        }

        public ArmorPreviewRequest(List<ItemStack> equipment, ArmorPreviewModel model,
                                   ArmorPreviewMode mode, float scale, float pitch,
                                   float rotationSpeed, int width, int height,
                                   List<String> effects) {
            this(firstStack(equipment), equipment, model, mode, scale, pitch,
                    rotationSpeed, width, height, effects, null);
        }

        public ArmorPreviewRequest(ItemStack primaryStack, List<ItemStack> equipment,
                                   ArmorPreviewModel model, ArmorPreviewMode mode,
                                   float scale, float pitch, float rotationSpeed,
                                   int width, int height, List<String> effects,
                                   PreviewSound sound) {
            this.equipment = new ArrayList<>();
            if (equipment != null) for (ItemStack value : equipment) {
                this.equipment.add(value == null ? ItemStack.EMPTY : value.copy());
            }
            this.primaryStack = primaryStack == null ? ItemStack.EMPTY : primaryStack.copy();
            this.slot = EntityEquipmentSlot.CHEST;
            this.model = model == null ? ArmorPreviewModel.ARMOR_STAND : model;
            this.mode = mode == null ? ArmorPreviewMode.FULL_SET : mode;
            this.scale = scale;
            this.pitch = pitch;
            this.rotationSpeed = rotationSpeed;
            this.width = Math.max(0, width);
            this.height = Math.max(0, height);
            this.effects = immutableCopy(effects);
            this.sound = sound;
        }

        public ArmorPreviewRequest(ItemStack stack, EntityEquipmentSlot slot,
                                   ArmorPreviewModel model, ArmorPreviewMode mode,
                                   float scale, float pitch, float rotationSpeed) {
            this(stack, slot, model, mode, scale, pitch, rotationSpeed, 40, 64,
                    Collections.<String>emptyList());
        }

        public ArmorPreviewRequest(ItemStack stack, EntityEquipmentSlot slot,
                                   ArmorPreviewModel model, ArmorPreviewMode mode,
                                   float scale, float pitch, float rotationSpeed,
                                   int width, int height, List<String> effects) {
            this(stack, slot, model, mode, scale, pitch, rotationSpeed,
                    width, height, effects, null);
        }

        public ArmorPreviewRequest(ItemStack stack, EntityEquipmentSlot slot,
                                   ArmorPreviewModel model, ArmorPreviewMode mode,
                                   float scale, float pitch, float rotationSpeed,
                                   int width, int height, List<String> effects,
                                   PreviewSound sound) {
            this.equipment = new ArrayList<>();
            this.equipment.add(stack == null ? ItemStack.EMPTY : stack.copy());
            this.primaryStack = stack == null ? ItemStack.EMPTY : stack.copy();
            this.slot = slot == null ? EntityEquipmentSlot.CHEST : slot;
            this.model = model == null ? ArmorPreviewModel.ARMOR_STAND : model;
            this.mode = mode == null ? ArmorPreviewMode.SINGLE_PIECE : mode;
            this.scale = scale;
            this.pitch = pitch;
            this.rotationSpeed = rotationSpeed;
            this.width = Math.max(0, width);
            this.height = Math.max(0, height);
            this.effects = immutableCopy(effects);
            this.sound = sound;
        }

        public ItemStack stack() { return primaryStack; }
        public List<ItemStack> equipment() { return Collections.unmodifiableList(equipment); }
        public EntityEquipmentSlot slot() { return slot; }
        public ArmorPreviewModel model() { return model; }
        public ArmorPreviewMode mode() { return mode; }
        public float scale() { return scale; }
        public float pitch() { return pitch; }
        public float rotationSpeed() { return rotationSpeed; }
        public int width() { return width; }
        public int height() { return height; }
        public List<String> effects() { return effects; }
        @Override public PreviewSound sound() { return sound; }
        @Override public PreviewKind previewKind() { return PreviewKind.ARMOR; }
    }

    /** A visual node that asks the registered client renderer to draw a preview. */
    public static final class PreviewNode implements VisualNode {
        private final PreviewRequest request;
        private final int width;
        private final int height;

        public PreviewNode(PreviewRequest request, int width, int height) {
            this.request = request;
            this.width = Math.max(0, width);
            this.height = Math.max(0, height);
        }

        /** Uses the registered renderer's measured dimensions at layout time. */
        public PreviewNode(PreviewRequest request) {
            this(request, 0, 0);
        }

        public PreviewRequest request() { return request; }
        @Override public Kind kind() { return Kind.PREVIEW; }
        @Override public int width(FontRenderer font) { return width; }
        @Override public int height(FontRenderer font) { return height; }
    }

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
        @Override public int height(FontRenderer font) { return height; }
    }

    /** A nested flow of visual nodes with explicit direction, gap and alignment. */
    public static final class GroupNode implements VisualNode {
        private final List<VisualNode> children;
        private final LayoutDirection direction;
        private final LayoutAlignment alignment;
        private final int gap;

        public GroupNode(List<? extends VisualNode> children, LayoutDirection direction,
                         LayoutAlignment alignment, int gap) {
            this.children = immutableCopy(children);
            this.direction = direction == null ? LayoutDirection.VERTICAL : direction;
            this.alignment = alignment == null ? LayoutAlignment.START : alignment;
            this.gap = Math.max(0, gap);
        }

        public GroupNode(List<? extends VisualNode> children, LayoutDirection direction) {
            this(children, direction, LayoutAlignment.START, 2);
        }

        public List<VisualNode> children() { return children; }
        public LayoutDirection direction() { return direction; }
        public LayoutAlignment alignment() { return alignment; }
        public int gap() { return gap; }

        @Override public Kind kind() { return Kind.GROUP; }
        @Override public int width(FontRenderer font) {
            int width = 0;
            if (direction == LayoutDirection.HORIZONTAL) {
                for (VisualNode child : children) width += child.width(font);
                if (children.size() > 1) width += gap * (children.size() - 1);
            } else {
                for (VisualNode child : children) width = Math.max(width, child.width(font));
            }
            return width;
        }
        @Override public int height(FontRenderer font) {
            int height = 0;
            if (direction == LayoutDirection.VERTICAL) {
                for (VisualNode child : children) height += child.height(font);
                if (children.size() > 1) height += gap * (children.size() - 1);
            } else {
                for (VisualNode child : children) height = Math.max(height, child.height(font));
            }
            return height;
        }
    }

    /** A responsive grid whose column count is capped by {@code columns}. */
    public static final class GridNode implements VisualNode {
        private final List<VisualNode> children;
        private final int columns;
        private final int gap;

        public GridNode(List<? extends VisualNode> children, int columns, int gap) {
            this.children = immutableCopy(children);
            this.columns = Math.max(1, columns);
            this.gap = Math.max(0, gap);
        }

        public List<VisualNode> children() { return children; }
        public int columns() { return columns; }
        public int gap() { return gap; }
        @Override public Kind kind() { return Kind.GRID; }
        @Override public int width(FontRenderer font) {
            int width = 0;
            int rows = Math.min(columns, children.size());
            for (int column = 0; column < rows; column++) {
                int columnWidth = 0;
                for (int index = column; index < children.size(); index += columns) {
                    columnWidth = Math.max(columnWidth, children.get(index).width(font));
                }
                width += columnWidth;
            }
            return width + Math.max(0, rows - 1) * gap;
        }
        @Override public int height(FontRenderer font) {
            int rows = (children.size() + columns - 1) / columns;
            int height = 0;
            for (int row = 0; row < rows; row++) {
                int rowHeight = 0;
                for (int column = 0; column < columns; column++) {
                    int index = row * columns + column;
                    if (index < children.size()) rowHeight = Math.max(rowHeight, children.get(index).height(font));
                }
                height += rowHeight;
            }
            return height + Math.max(0, rows - 1) * gap;
        }
    }

    private static volatile Renderer renderer;
    private static final CopyOnWriteArrayList<DocumentProvider> DOCUMENT_PROVIDERS =
            new CopyOnWriteArrayList<>();
    /** Thread-local escape hatch used by integrations that intentionally render their native tooltip. */
    private static final ThreadLocal<Boolean> NATIVE_BYPASS = new ThreadLocal<>();
    /** Marks a tooltip event published by a GUI that is not using GuiUtils. */
    private static final ThreadLocal<Boolean> EXTERNAL_GUI_RENDER = new ThreadLocal<>();
    private static final CopyOnWriteArrayList<DocumentFinalizer> DOCUMENT_FINALIZERS =
            new CopyOnWriteArrayList<>();
    /** Thread-local marker set while the registered renderer draws a document. */
    private static final ThreadLocal<Boolean> MODERN_RENDER = new ThreadLocal<>();

    private NfrTooltipApi() {}

    public static void register(Renderer value) { renderer = value; }

    public static void unregister(Renderer value) {
        if (renderer == value) renderer = null;
    }

    public static boolean isRegistered() { return renderer != null; }

    /**
     * Creates the zero-height logical marker used to place a visual node family.
     * This is deliberately part of the core API so producers can select the
     * modern path without depending on a UIE implementation package.
     */
    public static String visualAnchorLine(String family, String producer) {
        if (!isAnchorToken(family) || !isAnchorToken(producer)) {
            throw new IllegalArgumentException("Invalid tooltip anchor token");
        }
        return ANCHOR_PREFIX + " family=\"" + family + "\" producer=\"" + producer + "\"/>";
    }

    private static boolean isAnchorToken(String value) {
        if (value == null || value.isEmpty()) return false;
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (!((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9')
                    || c == '_' || c == '.' || c == '-')) return false;
        }
        return true;
    }

    /** Registers a source of visual nodes for the common tooltip pipeline. */
    public static void registerDocumentProvider(DocumentProvider provider) {
        if (provider != null && !DOCUMENT_PROVIDERS.contains(provider)) {
            DOCUMENT_PROVIDERS.add(provider);
        }
    }

    /** Removes a previously registered visual-node source. */
    public static void unregisterDocumentProvider(DocumentProvider provider) {
        if (provider != null) DOCUMENT_PROVIDERS.remove(provider);
    }

    public static void registerDocumentFinalizer(DocumentFinalizer finalizer) {
        if (finalizer != null && !DOCUMENT_FINALIZERS.contains(finalizer)) {
            DOCUMENT_FINALIZERS.add(finalizer);
        }
    }

    public static void unregisterDocumentFinalizer(DocumentFinalizer finalizer) {
        if (finalizer != null) DOCUMENT_FINALIZERS.remove(finalizer);
    }

    /**
     * Composes all registered visual sources into one coordinate-free document.
     * A provider failure is isolated so one optional integration cannot break the
     * normal tooltip path.
     */
    public static TooltipDocument composeDocument(ItemStack stack, List<String> lines) {
        TooltipDocument.Builder builder = TooltipDocument.builder(stack, lines);
        for (DocumentProvider provider : DOCUMENT_PROVIDERS) {
            try {
                Optional<TooltipDocument> document = provider.create(stack, lines);
                if (document.isPresent()) builder.addAll(document.get().nodes);
            } catch (RuntimeException | LinkageError ignored) {
                // Optional integrations must fail closed and leave vanilla text intact.
            }
        }
        return builder.build();
    }

    /** Applies all registered finalizers exactly once at the renderer boundary. */
    public static TooltipDocument finalizeDocument(TooltipDocument document) {
        TooltipDocument result = document;
        if (result == null) return null;
        for (DocumentFinalizer finalizer : DOCUMENT_FINALIZERS) {
            try {
                TooltipDocument next = finalizer.finalize(result);
                if (next != null) result = next;
            } catch (RuntimeException | LinkageError ignored) {
                // An optional finalizer must never break the tooltip itself.
            }
        }
        return result;
    }

    /**
     * Marks the current render call as native-tooltip work. UIE integrations
     * should skip handling Forge tooltip events while this scope is active.
     */
    public static void beginNativeBypass() { NATIVE_BYPASS.set(Boolean.TRUE); }

    /** Ends the current native-tooltip scope. */
    public static void endNativeBypass() { NATIVE_BYPASS.remove(); }

    /** Returns whether the current thread is rendering through a native backend. */
    public static boolean isNativeBypass() { return Boolean.TRUE.equals(NATIVE_BYPASS.get()); }

    public static void beginExternalGuiRender() { EXTERNAL_GUI_RENDER.set(Boolean.TRUE); }

    public static void endExternalGuiRender() { EXTERNAL_GUI_RENDER.remove(); }

    public static boolean isExternalGuiRender() {
        return Boolean.TRUE.equals(EXTERNAL_GUI_RENDER.get());
    }

    /**
     * Marks the current thread as performing a modern document render. Forge
     * tooltip events posted by the renderer while this scope is active are
     * synthetic, so native content drawers (such as AE2's PostText image pass)
     * must not draw the same content a second time.
     */
    public static void beginModernRender() { MODERN_RENDER.set(Boolean.TRUE); }

    /** Ends the current modern-render scope. */
    public static void endModernRender() { MODERN_RENDER.remove(); }

    /** Returns whether the current thread is inside a modern document render. */
    public static boolean isModernRender() { return Boolean.TRUE.equals(MODERN_RENDER.get()); }

    public static boolean render(ItemStack stack, List<String> lines, int mouseX, int mouseY,
                                 FontRenderer font) {
        return render(composeDocument(stack, lines), mouseX, mouseY, font);
    }

    public static boolean render(TooltipDocument document, int mouseX, int mouseY,
                                 FontRenderer font) {
        Renderer value = renderer;
        if (value == null || document == null) return false;
        beginModernRender();
        try {
            return value.render(document, mouseX, mouseY, font);
        } finally {
            endModernRender();
        }
    }

    private static <T> List<T> immutableCopy(List<? extends T> source) {
        if (source == null || source.isEmpty()) return Collections.emptyList();
        return Collections.unmodifiableList(new ArrayList<T>(source));
    }

    private static ItemStack firstStack(List<ItemStack> stacks) {
        if (stacks != null) for (ItemStack stack : stacks) {
            if (stack != null && !stack.isEmpty()) return stack;
        }
        return ItemStack.EMPTY;
    }
}
