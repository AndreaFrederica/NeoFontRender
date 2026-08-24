package neofontrender.addons.navigation.vanilla;

import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiButtonToggle;
import net.minecraft.client.gui.GuiKeyBindingList;
import net.minecraft.client.gui.GuiListButton;
import net.minecraft.client.gui.GuiListExtended;
import net.minecraft.client.gui.GuiOptionSlider;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiSlider;
import net.minecraft.client.gui.GuiSlot;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.inventory.ClickType;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.client.GuiScrollingList;
import neofontrender.addons.api.ui.navigation.UiAction;
import neofontrender.addons.api.ui.navigation.UiActionResult;
import neofontrender.addons.api.ui.navigation.CreativeTabNavigation;
import neofontrender.addons.api.ui.navigation.UiAxis;
import neofontrender.addons.api.ui.navigation.UiInteractionMode;
import neofontrender.addons.api.ui.navigation.UiNavigationHints;
import neofontrender.addons.api.ui.navigation.UiNodeId;
import neofontrender.addons.api.ui.navigation.UiRect;
import neofontrender.addons.api.ui.navigation.UiRole;
import neofontrender.addons.api.ui.navigation.UiTreeSession;
import neofontrender.addons.api.ui.navigation.UiTreeSnapshot;
import neofontrender.addons.mixin.AccessorGuiContainer;
import neofontrender.addons.mixin.AccessorGuiKeyBindingEntryNavigation;
import neofontrender.addons.mixin.AccessorGuiOptionSliderNavigation;
import neofontrender.addons.mixin.AccessorGuiScrollingListNavigation;
import neofontrender.addons.mixin.AccessorGuiScreenNavigation;
import neofontrender.addons.mixin.AccessorGuiSlotNavigation;
import neofontrender.addons.mixin.AccessorGuiTextFieldNavigation;
import neofontrender.addons.navigation.ImmutableUiNode;
import neofontrender.addons.navigation.ImmutableUiTreeSnapshot;
import neofontrender.addons.navigation.UiNavigationRuntime;
import neofontrender.addons.scrolling.SyntheticScrollAccess;
import neofontrender.addons.ui.NfrUiEnhancements;
import org.lwjgl.input.Keyboard;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class VanillaGuiTreeSession implements UiTreeSession {
    private static final ResourceLocation OWNER = new ResourceLocation(NfrUiEnhancements.MOD_ID, "vanilla");

    private final GuiScreen screen;
    private final UiNodeId rootId;
    private UiTreeSnapshot snapshot;
    private Map<UiNodeId, Handle> handles = Collections.emptyMap();
    private int signature = Integer.MIN_VALUE;
    private long captureRevision = Long.MIN_VALUE;
    private long revision;
    private boolean closed;

    VanillaGuiTreeSession(GuiScreen screen) {
        this.screen = screen;
        this.rootId = id("screen/" + screen.getClass().getName());
        refresh();
    }

    @Override public GuiScreen screen() { return screen; }
    @Override public UiTreeSnapshot snapshot() { return snapshot; }

    @Override public UiActionResult perform(UiNodeId node, UiAction action) {
        if (closed) return UiActionResult.STALE;
        if ((action == UiAction.PAGE_PREVIOUS || action == UiAction.PAGE_NEXT)
                && screen instanceof CreativeTabNavigation) {
            return ((CreativeTabNavigation) screen).nfrUi$changeCreativeTab(
                    action == UiAction.PAGE_PREVIOUS ? -1 : 1)
                    ? UiActionResult.CHANGED : UiActionResult.HANDLED;
        }
        Handle handle = handles.get(node);
        if (handle == null) return node == null ? performPointer(action) : UiActionResult.STALE;
        if (handle instanceof CreativeTabHandle) {
            return performCreativeTab((CreativeTabHandle) handle, action);
        }
        if (handle instanceof WidgetHandle) return performWidget((WidgetHandle) handle, action);
        if (handle instanceof SlotHandle) return performSlot((SlotHandle) handle, action);
        if (handle instanceof ListHandle) return performListScroll((ListHandle) handle, action);
        if (handle instanceof ListEntryHandle) return performList((ListEntryHandle) handle, action);
        if (handle instanceof ForgeListEntryHandle) {
            return performForgeList((ForgeListEntryHandle) handle, action);
        }
        return UiActionResult.IGNORED;
    }

    @Override public UiActionResult pointerDown(int x, int y, int button) {
        if (closed) return UiActionResult.STALE;
        ((AccessorGuiScreenNavigation) screen).nfrUi$invokeMouseClicked(x, y, button);
        return UiActionResult.HANDLED;
    }

    @Override public UiActionResult pointerMove(int x, int y, int button, long timeSincePress) {
        if (closed) return UiActionResult.STALE;
        ((AccessorGuiScreenNavigation) screen).nfrUi$invokeMouseClickMove(x, y, button, timeSincePress);
        return UiActionResult.CHANGED;
    }

    @Override public UiActionResult pointerUp(int x, int y, int button) {
        if (closed) return UiActionResult.STALE;
        ((AccessorGuiScreenNavigation) screen).nfrUi$invokeMouseReleased(x, y, button);
        return UiActionResult.CHANGED;
    }

    @Override public UiActionResult pointerScroll(int x, int y, int wheel) {
        if (closed || wheel == 0) return closed ? UiActionResult.STALE : UiActionResult.IGNORED;
        ListHandle target = pointerList(x, y);
        if (target == null) return UiActionResult.IGNORED;
        if (target.list instanceof SyntheticScrollAccess) {
            return ((SyntheticScrollAccess) target.list).nfrUi$scrollWheel(wheel)
                    ? UiActionResult.CHANGED : UiActionResult.HANDLED;
        }
        return performListScroll(target, wheel > 0 ? UiAction.SCROLL_UP : UiAction.SCROLL_DOWN);
    }

    private ListHandle pointerList(int x, int y) {
        ListHandle best = null;
        long bestArea = Long.MAX_VALUE;
        for (Map.Entry<UiNodeId, Handle> entry : handles.entrySet()) {
            if (!(entry.getValue() instanceof ListHandle)) continue;
            neofontrender.addons.api.ui.navigation.UiNode node = snapshot.node(entry.getKey());
            if (node == null || !node.visible() || !node.enabled()) continue;
            UiRect bounds = node.visibleBounds();
            if (x < bounds.left || x >= bounds.right || y < bounds.top || y >= bounds.bottom) continue;
            long area = (long) bounds.width() * bounds.height();
            if (area < bestArea) {
                best = (ListHandle) entry.getValue();
                bestArea = area;
            }
        }
        return best;
    }

    @Override public UiActionResult reveal(UiNodeId node) {
        Handle handle = handles.get(node);
        if (handle instanceof WidgetHandle && ((WidgetHandle) handle).list != null) {
            WidgetHandle entry = (WidgetHandle) handle;
            return revealListEntry(entry.list, entry.index);
        }
        if (!(handle instanceof ListEntryHandle)) return revealForgeList(handle);
        ListEntryHandle entry = (ListEntryHandle) handle;
        return revealListEntry(entry.list, entry.index);
    }

    private UiActionResult revealListEntry(GuiSlot list, int index) {
        AccessorGuiSlotNavigation access = (AccessorGuiSlotNavigation) list;
        int rowTop = access.nfrUi$getTop() + 4 - (int) access.nfrUi$getAmountScrolled()
                + index * access.nfrUi$getSlotHeight();
        int rowBottom = rowTop + access.nfrUi$getSlotHeight();
        int delta = rowTop < access.nfrUi$getTop() ? rowTop - access.nfrUi$getTop()
                : rowBottom > access.nfrUi$getBottom() ? rowBottom - access.nfrUi$getBottom() : 0;
        if (delta == 0) return UiActionResult.HANDLED;
        access.nfrUi$invokeScrollBy(delta);
        return UiActionResult.CHANGED;
    }

    private UiActionResult revealForgeList(Handle handle) {
        if (!(handle instanceof ForgeListEntryHandle)) {
            return handle == null ? UiActionResult.STALE : UiActionResult.HANDLED;
        }
        ForgeListEntryHandle entry = (ForgeListEntryHandle) handle;
        AccessorGuiScrollingListNavigation access =
                (AccessorGuiScrollingListNavigation) entry.list;
        int rowTop = access.nfrUi$getTop() + 4 + access.nfrUi$getHeaderHeight()
                - (int) access.nfrUi$getScrollDistance()
                + entry.index * access.nfrUi$getSlotHeight();
        int rowBottom = rowTop + access.nfrUi$getSlotHeight();
        int delta = rowTop < access.nfrUi$getTop() ? rowTop - access.nfrUi$getTop()
                : rowBottom > access.nfrUi$getBottom() ? rowBottom - access.nfrUi$getBottom() : 0;
        if (delta == 0) return UiActionResult.HANDLED;
        access.nfrUi$setScrollDistance(access.nfrUi$getScrollDistance() + delta);
        access.nfrUi$invokeApplyScrollLimits();
        return UiActionResult.CHANGED;
    }

    @Override public UiActionResult back() {
        if (closed) return UiActionResult.STALE;
        ((AccessorGuiScreenNavigation) screen).nfrUi$invokeKeyTyped('\0', Keyboard.KEY_ESCAPE);
        return UiActionResult.CHANGED;
    }

    @Override public void refresh() {
        if (closed) return;
        List<GuiButton> buttons = ((AccessorGuiScreenNavigation) screen).nfrUi$getNavigationButtons();
        List<GuiSlot> lists = lists(screen);
        List<GuiScrollingList> forgeLists = forgeLists(screen);
        Container container = screen instanceof GuiContainer
                ? ((AccessorGuiContainer) screen).nfrUi$getInventorySlots() : null;
        int nextSignature = signature(buttons, lists, forgeLists, container);
        if (screen instanceof CreativeTabNavigation) {
            CreativeTabNavigation creative = (CreativeTabNavigation) screen;
            nextSignature = 31 * nextSignature + creative.nfrUi$getSelectedCreativeTab();
            for (CreativeTabNavigation.Tab tab : creative.nfrUi$getVisibleCreativeTabs()) {
                nextSignature = 31 * nextSignature + tab.index();
                nextSignature = 31 * nextSignature + tab.bounds().hashCode();
            }
        }
        for (VanillaWidgetCapture.CapturedWidget captured : VanillaWidgetCapture.topLevel(screen)) {
            nextSignature = 31 * nextSignature + widgetStateSignature(captured.widget);
        }
        long nextCaptureRevision = VanillaWidgetCapture.revision(screen);
        if (snapshot != null && nextSignature == signature
                && nextCaptureRevision == captureRevision) return;
        signature = nextSignature;
        captureRevision = nextCaptureRevision;
        revision++;
        rebuild(buttons, lists, forgeLists, container);
    }

    @Override public void close() {
        closed = true;
        handles = Collections.emptyMap();
    }

    private void rebuild(List<GuiButton> buttons, List<GuiSlot> lists,
                         List<GuiScrollingList> forgeLists, Container container) {
        List<ImmutableUiNode> nodes = new ArrayList<>();
        List<UiNodeId> rootChildren = new ArrayList<>();
        Map<UiNodeId, Handle> nextHandles = new LinkedHashMap<>();
        Set<Gui> includedWidgets = Collections.newSetFromMap(new IdentityHashMap<Gui, Boolean>());
        UiRect screenBounds = new UiRect(0, 0, Math.max(0, screen.width), Math.max(0, screen.height));

        int buttonOrdinal = 0;
        for (GuiButton button : buttons) {
            if (button == null) continue;
            UiNodeId nodeId = id("screen/" + screen.getClass().getName() + "/button/"
                    + button.id + "/" + buttonOrdinal++);
            UiRect bounds = rect(button.x, button.y, button.width, button.height);
            addWidget(nodes, rootChildren, nextHandles, nodeId, rootId, button, bounds,
                    bounds.intersect(screenBounds), "screen-controls", buttonOrdinal,
                    new WidgetHandle(button, null, -1, bounds), null);
            includedWidgets.add(button);
        }

        for (VanillaWidgetCapture.CapturedWidget captured : VanillaWidgetCapture.topLevel(screen)) {
            if (!includedWidgets.add(captured.widget)) continue;
            UiNodeId nodeId = id("screen/" + screen.getClass().getName() + "/captured/"
                    + VanillaWidgetCapture.stableId(screen, captured.widget));
            addWidget(nodes, rootChildren, nextHandles, nodeId, rootId, captured.widget,
                    captured.bounds, captured.bounds.intersect(screenBounds), "screen-controls",
                    buttonOrdinal++, new WidgetHandle(captured.widget, null, -1, captured.bounds), null);
        }

        if (screen instanceof CreativeTabNavigation) {
            addCreativeTabs(nodes, rootChildren, nextHandles,
                    (CreativeTabNavigation) screen, screenBounds);
        }

        if (container != null) addContainer(nodes, rootChildren, nextHandles, container, screenBounds);
        int listOrdinal = 0;
        for (GuiSlot list : lists) {
            if (list instanceof GuiListExtended) {
                addExtendedList(nodes, rootChildren, nextHandles,
                        (GuiListExtended) list, listOrdinal++, screenBounds);
            } else {
                addList(nodes, rootChildren, nextHandles, list, listOrdinal++, screenBounds);
            }
        }
        int forgeListOrdinal = 0;
        for (GuiScrollingList list : forgeLists) {
            addForgeList(nodes, rootChildren, nextHandles, list, forgeListOrdinal++, screenBounds);
        }

        ImmutableUiNode.Builder root = ImmutableUiNode.builder(rootId, UiRole.ROOT)
                .children(rootChildren).bounds(screenBounds).visibleBounds(screenBounds)
                .focusable(false);
        if (screen instanceof CreativeTabNavigation) {
            root.action(UiAction.PAGE_PREVIOUS).action(UiAction.PAGE_NEXT);
        }
        nodes.add(root.build());
        UiInteractionMode mode = rootChildren.isEmpty()
                ? UiInteractionMode.CURSOR : UiInteractionMode.HYBRID;
        snapshot = new ImmutableUiTreeSnapshot(revision, screen, mode,
                Collections.singletonList(rootId), nodes, null, rootId);
        handles = nextHandles;
    }

    private void addCreativeTabs(List<ImmutableUiNode> nodes, List<UiNodeId> rootChildren,
                                 Map<UiNodeId, Handle> nextHandles,
                                 CreativeTabNavigation creative, UiRect screenBounds) {
        String basePath = "screen/" + screen.getClass().getName() + "/creative-tabs";
        UiNodeId listId = id(basePath);
        List<UiNodeId> tabs = new ArrayList<>();
        UiRect listBounds = UiRect.EMPTY;
        int order = 0;
        for (CreativeTabNavigation.Tab tab : creative.nfrUi$getVisibleCreativeTabs()) {
            UiNodeId tabId = id(basePath + "/tab/" + tab.index());
            UiRect bounds = tab.bounds();
            UiRect visibleBounds = bounds.intersect(screenBounds);
            nodes.add(creativeTabNode(tabId, listId, tab, visibleBounds, order++));
            tabs.add(tabId);
            nextHandles.put(tabId, new CreativeTabHandle(tab.index()));
            listBounds = listBounds.isEmpty() ? bounds : union(listBounds, bounds);
        }
        if (tabs.isEmpty()) return;
        nodes.add(ImmutableUiNode.builder(listId, UiRole.TAB_LIST)
                .parent(rootId).children(tabs).bounds(listBounds)
                .visibleBounds(listBounds.intersect(screenBounds))
                .navigation(UiNavigationHints.builder().group("creative-tabs").build())
                .action(UiAction.PAGE_PREVIOUS).action(UiAction.PAGE_NEXT)
                .focusable(false).build());
        rootChildren.add(listId);
        nextHandles.put(listId, new CreativeTabListHandle());
    }

    static ImmutableUiNode creativeTabNode(UiNodeId tabId, UiNodeId listId,
                                           CreativeTabNavigation.Tab tab,
                                           UiRect visibleBounds, int order) {
        return ImmutableUiNode.builder(tabId, UiRole.TAB)
                .parent(listId).label(tab.label()).bounds(tab.bounds()).visibleBounds(visibleBounds)
                .navigation(UiNavigationHints.builder().group("creative-tabs")
                        .order(order).primaryAxis(UiAxis.HORIZONTAL)
                        .wrapHorizontal(true).build())
                .action(UiAction.ACTIVATE).enabled(true).visible(true).focusable(true).build();
    }

    private static UiRect union(UiRect first, UiRect second) {
        return new UiRect(Math.min(first.left, second.left), Math.min(first.top, second.top),
                Math.max(first.right, second.right), Math.max(first.bottom, second.bottom));
    }

    private void addForgeList(List<ImmutableUiNode> nodes, List<UiNodeId> rootChildren,
                              Map<UiNodeId, Handle> nextHandles, GuiScrollingList list,
                              int listOrdinal, UiRect screenBounds) {
        AccessorGuiScrollingListNavigation access = (AccessorGuiScrollingListNavigation) list;
        String basePath = "screen/" + screen.getClass().getName() + "/forge-list/" + listOrdinal;
        UiNodeId listId = id(basePath);
        UiRect viewport = new UiRect(access.nfrUi$getLeft(), access.nfrUi$getTop(),
                Math.max(access.nfrUi$getLeft(), access.nfrUi$getRight()),
                Math.max(access.nfrUi$getTop(), access.nfrUi$getBottom())).intersect(screenBounds);
        List<UiNodeId> entries = new ArrayList<>();
        int size = Math.max(0, access.nfrUi$invokeNavigationSize());
        int width = Math.max(1, access.nfrUi$getRight() - access.nfrUi$getLeft());
        for (int index = 0; index < size; index++) {
            UiNodeId entryId = id(basePath + "/entry/" + index);
            int y = access.nfrUi$getTop() + 4 + access.nfrUi$getHeaderHeight()
                    - (int) access.nfrUi$getScrollDistance()
                    + index * access.nfrUi$getSlotHeight();
            UiRect bounds = rect(access.nfrUi$getLeft(), y, width, access.nfrUi$getSlotHeight());
            nodes.add(ImmutableUiNode.builder(entryId, UiRole.LIST_ITEM)
                    .parent(listId).label("Item " + (index + 1)).bounds(bounds)
                    .visibleBounds(bounds.intersect(viewport))
                    .navigation(UiNavigationHints.builder()
                            .group("forge-list." + listOrdinal).order(index).build())
                    .action(UiAction.ACTIVATE).enabled(true).visible(true).focusable(true).build());
            entries.add(entryId);
            nextHandles.put(entryId, new ForgeListEntryHandle(list, index));
        }
        nodes.add(ImmutableUiNode.builder(listId, UiRole.LIST)
                .parent(rootId).children(entries).bounds(viewport).visibleBounds(viewport)
                .navigation(UiNavigationHints.builder().group("forge-list." + listOrdinal).build())
                .action(UiAction.SCROLL_UP).action(UiAction.SCROLL_DOWN)
                .focusable(false).build());
        rootChildren.add(listId);
        nextHandles.put(listId, new ListHandle(list));
    }

    private void addContainer(List<ImmutableUiNode> nodes, List<UiNodeId> rootChildren,
                              Map<UiNodeId, Handle> nextHandles, Container container,
                              UiRect screenBounds) {
        GuiContainer gui = (GuiContainer) screen;
        UiNodeId inventoryId = id("screen/" + screen.getClass().getName() + "/inventory");
        List<UiNodeId> slotIds = new ArrayList<>();
        for (int slotOrdinal = 0; slotOrdinal < container.inventorySlots.size(); slotOrdinal++) {
            Slot slot = container.inventorySlots.get(slotOrdinal);
            UiNodeId slotId = containerSlotId(screen.getClass(), slotOrdinal);
            UiRect bounds = rect(gui.getGuiLeft() + slot.xPos, gui.getGuiTop() + slot.yPos, 16, 16);
            ItemStack stack = slot.getStack();
            String label = stack.isEmpty() ? "Slot " + slotOrdinal : stack.getDisplayName();
            nodes.add(ImmutableUiNode.builder(slotId, UiRole.INVENTORY_SLOT)
                    .parent(inventoryId).label(label).bounds(bounds)
                    .visibleBounds(bounds.intersect(screenBounds))
                    .navigation(UiNavigationHints.builder().group("inventory").order(slotOrdinal).build())
                    .action(UiAction.ACTIVATE).action(UiAction.SECONDARY)
                    .action(UiAction.QUICK_MOVE).action(UiAction.TAKE_HALF).action(UiAction.DROP)
                    .enabled(slot.isEnabled()).visible(true).focusable(true).build());
            slotIds.add(slotId);
            nextHandles.put(slotId, new SlotHandle(slot));
        }
        nodes.add(ImmutableUiNode.builder(inventoryId, UiRole.INVENTORY)
                .parent(rootId).children(slotIds).bounds(screenBounds).visibleBounds(screenBounds)
                .navigation(UiNavigationHints.builder().group("inventory").build())
                .focusable(false).build());
        rootChildren.add(inventoryId);
    }

    private void addList(List<ImmutableUiNode> nodes, List<UiNodeId> rootChildren,
                         Map<UiNodeId, Handle> nextHandles, GuiSlot list, int listOrdinal,
                         UiRect screenBounds) {
        AccessorGuiSlotNavigation access = (AccessorGuiSlotNavigation) list;
        UiNodeId listId = id("screen/" + screen.getClass().getName() + "/list/" + listOrdinal);
        UiRect viewport = new UiRect(access.nfrUi$getLeft(), access.nfrUi$getTop(),
                Math.max(access.nfrUi$getLeft(), access.nfrUi$getRight()),
                Math.max(access.nfrUi$getTop(), access.nfrUi$getBottom())).intersect(screenBounds);
        List<UiNodeId> entries = new ArrayList<>();
        int size = Math.max(0, access.nfrUi$invokeNavigationSize());
        int width = Math.max(1, list.getListWidth());
        int entryLeft = listEntryLeft(access.nfrUi$getLeft(), access.nfrUi$getRight(), width);
        for (int index = 0; index < size; index++) {
            UiNodeId entryId = id("screen/" + screen.getClass().getName() + "/list/"
                    + listOrdinal + "/entry/" + index);
            int y = access.nfrUi$getTop() + 4 - (int) access.nfrUi$getAmountScrolled()
                    + index * access.nfrUi$getSlotHeight();
            UiRect bounds = rect(entryLeft, y, width, access.nfrUi$getSlotHeight());
            nodes.add(ImmutableUiNode.builder(entryId, UiRole.LIST_ITEM)
                    .parent(listId).label("Item " + (index + 1)).bounds(bounds)
                    .visibleBounds(bounds.intersect(viewport))
                    .navigation(UiNavigationHints.builder().group("list." + listOrdinal).order(index).build())
                    .action(UiAction.ACTIVATE).enabled(true).visible(true).focusable(true).build());
            entries.add(entryId);
            nextHandles.put(entryId, new ListEntryHandle(list, index));
        }
        nodes.add(ImmutableUiNode.builder(listId, UiRole.LIST)
                .parent(rootId).children(entries).bounds(viewport).visibleBounds(viewport)
                .navigation(UiNavigationHints.builder().group("list." + listOrdinal).build())
                .action(UiAction.SCROLL_UP).action(UiAction.SCROLL_DOWN)
                .focusable(false).build());
        rootChildren.add(listId);
        nextHandles.put(listId, new ListHandle(list));
    }

    private void addExtendedList(List<ImmutableUiNode> nodes, List<UiNodeId> rootChildren,
                                 Map<UiNodeId, Handle> nextHandles, GuiListExtended list,
                                 int listOrdinal, UiRect screenBounds) {
        AccessorGuiSlotNavigation access = (AccessorGuiSlotNavigation) list;
        String basePath = "screen/" + screen.getClass().getName() + "/extended-list/" + listOrdinal;
        UiNodeId listId = id(basePath);
        UiRect viewport = new UiRect(access.nfrUi$getLeft(), access.nfrUi$getTop(),
                Math.max(access.nfrUi$getLeft(), access.nfrUi$getRight()),
                Math.max(access.nfrUi$getTop(), access.nfrUi$getBottom())).intersect(screenBounds);
        List<UiNodeId> rowIds = new ArrayList<>();
        int size = Math.max(0, access.nfrUi$invokeNavigationSize());
        int entryLeft = listEntryLeft(access.nfrUi$getLeft(), access.nfrUi$getRight(),
                Math.max(1, list.getListWidth()));
        List<ExtendedRow> rows = new ArrayList<>();
        Map<TemplateKey, VanillaWidgetCapture.RelativeGeometry> templates = new HashMap<>();
        for (int index = 0; index < size; index++) {
            Object entry = list.getListEntry(index);
            List<VanillaControlIntrospector.ControlRef> controls =
                    VanillaControlIntrospector.controls(entry);
            ExtendedRow row = new ExtendedRow(index, entry, controls,
                    id(basePath + "/entry/" + index));
            rows.add(row);
            for (VanillaControlIntrospector.ControlRef control : controls) {
                VanillaWidgetCapture.RelativeGeometry geometry =
                        VanillaWidgetCapture.relativeGeometry(screen, control.widget);
                if (geometry != null) templates.put(
                        new TemplateKey(entry.getClass(), control.path), geometry);
            }
        }

        int order = 0;
        for (ExtendedRow row : rows) {
            int index = row.entryIndex;
            int y = access.nfrUi$getTop() + 4 - (int) access.nfrUi$getAmountScrolled()
                    + index * access.nfrUi$getSlotHeight();
            List<UiNodeId> childIds = new ArrayList<>();
            for (VanillaControlIntrospector.ControlRef control : row.controls) {
                VanillaWidgetCapture.RelativeGeometry geometry =
                        VanillaWidgetCapture.relativeGeometry(screen, control.widget);
                if (geometry == null) geometry = templates.get(
                        new TemplateKey(row.entry.getClass(), control.path));
                if (geometry == null) continue;
                UiRect bounds = geometry.at(entryLeft, y);
                UiNodeId controlId = id(basePath + "/entry/" + index + "/control/"
                        + stablePath(control.path));
                String label = keyBindingLabel(row.entry, control.widget);
                addWidget(nodes, childIds, nextHandles, controlId, row.rowId, control.widget,
                        bounds, bounds.intersect(viewport), "extended-list." + listOrdinal,
                        order++, new WidgetHandle(control.widget, list, index, bounds), label);
            }
            UiRect rowBounds = rect(entryLeft, y, Math.max(1, list.getListWidth()),
                    access.nfrUi$getSlotHeight());
            nodes.add(ImmutableUiNode.builder(row.rowId, UiRole.LIST_ITEM)
                    .parent(listId).children(childIds).label(rowLabel(row.entry, index))
                    .bounds(rowBounds).visibleBounds(rowBounds.intersect(viewport))
                    .navigation(UiNavigationHints.builder()
                            .group("extended-list." + listOrdinal).order(order++).build())
                    .action(UiAction.ACTIVATE).enabled(true).visible(true)
                    .focusable(childIds.isEmpty() && !keyBindingCategory(row.entry)).build());
            rowIds.add(row.rowId);
            nextHandles.put(row.rowId, new ListEntryHandle(list, index));
        }
        nodes.add(ImmutableUiNode.builder(listId, UiRole.LIST)
                .parent(rootId).children(rowIds).bounds(viewport).visibleBounds(viewport)
                .navigation(UiNavigationHints.builder().group("extended-list." + listOrdinal).build())
                .action(UiAction.SCROLL_UP).action(UiAction.SCROLL_DOWN)
                .focusable(false).build());
        rootChildren.add(listId);
        nextHandles.put(listId, new ListHandle(list));
    }

    private UiActionResult performWidget(WidgetHandle handle, UiAction action) {
        if (handle.widget instanceof GuiTextField) {
            GuiTextField field = (GuiTextField) handle.widget;
            if (action == UiAction.END_EDIT) {
                field.setFocused(false);
                return UiActionResult.CHANGED;
            }
            if (action != UiAction.ACTIVATE && action != UiAction.BEGIN_EDIT) {
                return UiActionResult.IGNORED;
            }
            UiActionResult result = click(handle, centerX(handle.bounds), centerY(handle.bounds), 0);
            field.setFocused(true);
            return result;
        }
        if (!(handle.widget instanceof GuiButton)) return UiActionResult.IGNORED;
        GuiButton button = (GuiButton) handle.widget;
        if (action == UiAction.INCREMENT || action == UiAction.DECREMENT) {
            Float current = sliderPosition(button);
            if (current == null) return UiActionResult.IGNORED;
            float delta = action == UiAction.INCREMENT ? 0.05F : -0.05F;
            float target = Math.max(0.0F, Math.min(1.0F, current + delta));
            if (target == current) return UiActionResult.HANDLED;
            int x = handle.bounds.left + 4
                    + Math.round(target * Math.max(1, handle.bounds.width() - 8));
            return click(handle, x, centerY(handle.bounds), 0);
        }
        if (action != UiAction.ACTIVATE) return UiActionResult.IGNORED;
        return click(handle, centerX(handle.bounds), centerY(handle.bounds), 0);
    }

    private UiActionResult performCreativeTab(CreativeTabHandle handle, UiAction action) {
        if (action != UiAction.ACTIVATE || !(screen instanceof CreativeTabNavigation)) {
            return UiActionResult.IGNORED;
        }
        CreativeTabNavigation creative = (CreativeTabNavigation) screen;
        if (creative.nfrUi$getSelectedCreativeTab() == handle.index) return UiActionResult.HANDLED;
        return creative.nfrUi$selectCreativeTab(handle.index)
                ? UiActionResult.CHANGED : UiActionResult.REJECTED;
    }

    private UiActionResult performSlot(SlotHandle handle, UiAction action) {
        int button;
        ClickType type;
        switch (action) {
            case ACTIVATE: button = 0; type = ClickType.PICKUP; break;
            case SECONDARY:
            case TAKE_HALF: button = 1; type = ClickType.PICKUP; break;
            case QUICK_MOVE: button = 0; type = ClickType.QUICK_MOVE; break;
            case DROP: button = 0; type = ClickType.THROW; break;
            default: return UiActionResult.IGNORED;
        }
        ((AccessorGuiContainer) screen).nfrUi$invokeHandleMouseClick(
                handle.slot, handle.slot.slotNumber, button, type);
        return UiActionResult.CHANGED;
    }

    private UiActionResult performList(ListEntryHandle handle, UiAction action) {
        if (action != UiAction.ACTIVATE) return UiActionResult.IGNORED;
        AccessorGuiSlotNavigation access = (AccessorGuiSlotNavigation) handle.list;
        int y = access.nfrUi$getTop() + 4 + handle.index * access.nfrUi$getSlotHeight()
                - (int) access.nfrUi$getAmountScrolled() + access.nfrUi$getSlotHeight() / 2;
        if (handle.list instanceof GuiListExtended) {
            int x = (access.nfrUi$getLeft() + access.nfrUi$getRight()) / 2;
            GuiListExtended list = (GuiListExtended) handle.list;
            list.mouseClicked(x, y, 0);
            list.mouseReleased(x, y, 0);
            return UiActionResult.CHANGED;
        }
        access.nfrUi$invokeElementClicked(handle.index, false,
                (access.nfrUi$getLeft() + access.nfrUi$getRight()) / 2,
                y);
        return UiActionResult.CHANGED;
    }

    private UiActionResult click(WidgetHandle handle, int x, int y, int button) {
        if (handle.list != null) {
            handle.list.mouseClicked(x, y, button);
            handle.list.mouseReleased(x, y, button);
        } else {
            AccessorGuiScreenNavigation access = (AccessorGuiScreenNavigation) screen;
            access.nfrUi$invokeMouseClicked(x, y, button);
            access.nfrUi$invokeMouseReleased(x, y, button);
        }
        return UiActionResult.CHANGED;
    }

    private UiActionResult performPointer(UiAction action) {
        int button = action == UiAction.ACTIVATE ? 0 : action == UiAction.SECONDARY ? 1 : -1;
        if (button < 0) return UiActionResult.IGNORED;
        UiNavigationRuntime runtime = UiNavigationRuntime.instance();
        AccessorGuiScreenNavigation access = (AccessorGuiScreenNavigation) screen;
        int x = runtime.renderPointerX(1.0F);
        int y = runtime.renderPointerY(1.0F);
        access.nfrUi$invokeMouseClicked(x, y, button);
        access.nfrUi$invokeMouseReleased(x, y, button);
        return UiActionResult.CHANGED;
    }

    private UiActionResult performListScroll(ListHandle handle, UiAction action) {
        int direction = action == UiAction.SCROLL_UP ? -1
                : action == UiAction.SCROLL_DOWN ? 1 : 0;
        if (direction == 0) return UiActionResult.IGNORED;
        if (handle.list instanceof GuiSlot) {
            AccessorGuiSlotNavigation access = (AccessorGuiSlotNavigation) handle.list;
            access.nfrUi$invokeScrollBy(direction * Math.max(1, access.nfrUi$getSlotHeight()));
            return UiActionResult.CHANGED;
        }
        if (handle.list instanceof GuiScrollingList) {
            AccessorGuiScrollingListNavigation access =
                    (AccessorGuiScrollingListNavigation) handle.list;
            access.nfrUi$setScrollDistance(access.nfrUi$getScrollDistance()
                    + direction * Math.max(1, access.nfrUi$getSlotHeight()));
            access.nfrUi$invokeApplyScrollLimits();
            return UiActionResult.CHANGED;
        }
        return UiActionResult.STALE;
    }

    private UiActionResult performForgeList(ForgeListEntryHandle handle, UiAction action) {
        if (action != UiAction.ACTIVATE) return UiActionResult.IGNORED;
        AccessorGuiScrollingListNavigation access =
                (AccessorGuiScrollingListNavigation) handle.list;
        access.nfrUi$invokeElementClicked(handle.index, false);
        access.nfrUi$setSelectedIndex(handle.index);
        return UiActionResult.CHANGED;
    }

    private static void addWidget(List<ImmutableUiNode> nodes, List<UiNodeId> parentChildren,
                                  Map<UiNodeId, Handle> handles, UiNodeId nodeId,
                                  UiNodeId parentId, Gui widget, UiRect bounds,
                                  UiRect visibleBounds, String group, int order,
                                  WidgetHandle handle, String labelOverride) {
        UiRole role = widgetRole(widget);
        ImmutableUiNode.Builder node = ImmutableUiNode.builder(nodeId, role)
                .parent(parentId).label(labelOverride == null ? widgetLabel(widget) : labelOverride)
                .bounds(bounds).visibleBounds(visibleBounds)
                .navigation(UiNavigationHints.builder().group(group).order(order)
                        .primaryAxis(role == UiRole.SLIDER ? UiAxis.HORIZONTAL : UiAxis.NONE).build())
                .enabled(widgetEnabled(widget)).visible(widgetVisible(widget)).focusable(true);
        if (widget instanceof GuiTextField) {
            node.action(UiAction.ACTIVATE).action(UiAction.BEGIN_EDIT).action(UiAction.END_EDIT);
        } else {
            node.action(UiAction.ACTIVATE);
            if (role == UiRole.SLIDER) {
                node.action(UiAction.INCREMENT).action(UiAction.DECREMENT);
            }
        }
        nodes.add(node.build());
        parentChildren.add(nodeId);
        handles.put(nodeId, handle);
    }

    static UiRole widgetRole(Gui widget) {
        if (widget instanceof GuiTextField) return UiRole.TEXT_INPUT;
        if (widget instanceof GuiSlider || widget instanceof GuiOptionSlider) return UiRole.SLIDER;
        if (widget instanceof GuiListButton || widget instanceof GuiButtonToggle) return UiRole.TOGGLE;
        return UiRole.BUTTON;
    }

    private static boolean widgetEnabled(Gui widget) {
        if (widget instanceof GuiButton) return ((GuiButton) widget).enabled;
        return widget instanceof GuiTextField
                && ((AccessorGuiTextFieldNavigation) widget).nfrUi$isEnabled();
    }

    private static boolean widgetVisible(Gui widget) {
        if (widget instanceof GuiButton) return ((GuiButton) widget).visible;
        return widget instanceof GuiTextField && ((GuiTextField) widget).getVisible();
    }

    private static String widgetLabel(Gui widget) {
        if (widget instanceof GuiButton) {
            String label = ((GuiButton) widget).displayString;
            return label == null || label.isEmpty()
                    ? widget.getClass().getSimpleName() + " " + ((GuiButton) widget).id : label;
        }
        GuiTextField field = (GuiTextField) widget;
        return field.getText().isEmpty() ? "Text field " + field.getId() : field.getText();
    }

    private static Float sliderPosition(GuiButton button) {
        if (button instanceof GuiSlider) return ((GuiSlider) button).getSliderPosition();
        if (button instanceof GuiOptionSlider) {
            return ((AccessorGuiOptionSliderNavigation) button).nfrUi$getSliderValue();
        }
        return null;
    }

    private static int centerX(UiRect bounds) { return (int) Math.round(bounds.centerX()); }
    private static int centerY(UiRect bounds) { return (int) Math.round(bounds.centerY()); }

    private static String stablePath(String fieldPath) {
        StringBuilder result = new StringBuilder(fieldPath.length());
        for (int index = 0; index < fieldPath.length(); index++) {
            char value = fieldPath.charAt(index);
            result.append(Character.isLetterOrDigit(value) || value == '_' || value == '-'
                    ? value : '_');
        }
        return result.toString();
    }

    private static String rowLabel(Object entry, int index) {
        if (entry instanceof GuiKeyBindingList.KeyEntry) {
            KeyBinding key = ((AccessorGuiKeyBindingEntryNavigation) entry).nfrUi$getKeyBinding();
            return I18n.format(key.getKeyDescription());
        }
        return "Item " + (index + 1);
    }

    private static String keyBindingLabel(Object entry, Gui widget) {
        if (!(entry instanceof GuiKeyBindingList.KeyEntry)) return null;
        AccessorGuiKeyBindingEntryNavigation access =
                (AccessorGuiKeyBindingEntryNavigation) entry;
        KeyBinding key = access.nfrUi$getKeyBinding();
        String description = I18n.format(key.getKeyDescription());
        if (widget == access.nfrUi$getResetButton()) {
            return I18n.format("controls.reset") + ": " + description;
        }
        return description + ": " + key.getDisplayName();
    }

    private static boolean keyBindingCategory(Object entry) {
        return entry != null && entry.getClass().getName().contains("GuiKeyBindingList$CategoryEntry");
    }

    private static int signature(List<GuiButton> buttons, List<GuiSlot> lists,
                                 List<GuiScrollingList> forgeLists, Container container) {
        int result = 1;
        for (GuiButton button : buttons) {
            if (button == null) continue;
            result = 31 * result + button.id;
            result = 31 * result + button.x + 31 * button.y + button.width + button.height;
            result = 31 * result + (button.visible ? 1 : 0) + (button.enabled ? 2 : 0);
            result = 31 * result + (button.displayString == null ? 0 : button.displayString.hashCode());
        }
        if (container != null) {
            result = 31 * result + container.inventorySlots.size();
            for (Slot slot : container.inventorySlots) result = 31 * result + slot.slotNumber + slot.xPos + slot.yPos;
        }
        for (GuiSlot list : lists) {
            AccessorGuiSlotNavigation access = (AccessorGuiSlotNavigation) list;
            result = 31 * result + access.nfrUi$invokeNavigationSize();
            result = 31 * result + Float.floatToIntBits(access.nfrUi$getAmountScrolled());
            if (list instanceof GuiKeyBindingList) {
                GuiKeyBindingList keyList = (GuiKeyBindingList) list;
                for (int index = 0; index < access.nfrUi$invokeNavigationSize(); index++) {
                    Object entry = keyList.getListEntry(index);
                    if (!(entry instanceof GuiKeyBindingList.KeyEntry)) continue;
                    KeyBinding key = ((AccessorGuiKeyBindingEntryNavigation) entry).nfrUi$getKeyBinding();
                    result = 31 * result + key.getDisplayName().hashCode();
                    result = 31 * result + (key.isSetToDefaultValue() ? 1 : 0);
                }
            }
            if (list instanceof GuiListExtended) {
                GuiListExtended extended = (GuiListExtended) list;
                for (int index = 0; index < access.nfrUi$invokeNavigationSize(); index++) {
                    for (VanillaControlIntrospector.ControlRef control
                            : VanillaControlIntrospector.controls(extended.getListEntry(index))) {
                        result = 31 * result + widgetStateSignature(control.widget);
                    }
                }
            }
        }
        for (GuiScrollingList list : forgeLists) {
            AccessorGuiScrollingListNavigation access = (AccessorGuiScrollingListNavigation) list;
            result = 31 * result + access.nfrUi$invokeNavigationSize();
            result = 31 * result + Float.floatToIntBits(access.nfrUi$getScrollDistance());
        }
        return result;
    }

    private static int widgetStateSignature(Gui widget) {
        int result = System.identityHashCode(widget);
        if (widget instanceof GuiButton) {
            GuiButton button = (GuiButton) widget;
            result = 31 * result + button.x + 31 * button.y + button.width + button.height;
            result = 31 * result + (button.visible ? 1 : 0) + (button.enabled ? 2 : 0);
            return 31 * result + (button.displayString == null ? 0 : button.displayString.hashCode());
        }
        GuiTextField field = (GuiTextField) widget;
        result = 31 * result + field.x + 31 * field.y + field.width + field.height;
        result = 31 * result + (field.getVisible() ? 1 : 0)
                + (((AccessorGuiTextFieldNavigation) field).nfrUi$isEnabled() ? 2 : 0);
        return 31 * result + field.getText().hashCode();
    }

    private static List<GuiSlot> lists(GuiScreen screen) {
        List<GuiSlot> result = new ArrayList<>();
        Set<GuiSlot> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        Class<?> type = screen.getClass();
        while (type != null && GuiScreen.class.isAssignableFrom(type)) {
            for (Field field : type.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers()) || !GuiSlot.class.isAssignableFrom(field.getType())) continue;
                try {
                    field.setAccessible(true);
                    GuiSlot list = (GuiSlot) field.get(screen);
                    if (list != null && seen.add(list)) result.add(list);
                } catch (IllegalAccessException ignored) {
                    // The page still exposes its buttons and other supported controls.
                }
            }
            type = type.getSuperclass();
        }
        return result;
    }

    private static List<GuiScrollingList> forgeLists(GuiScreen screen) {
        List<GuiScrollingList> result = new ArrayList<>();
        Set<GuiScrollingList> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        Class<?> type = screen.getClass();
        while (type != null && GuiScreen.class.isAssignableFrom(type)) {
            for (Field field : type.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers())
                        || !GuiScrollingList.class.isAssignableFrom(field.getType())) continue;
                try {
                    field.setAccessible(true);
                    GuiScrollingList list = (GuiScrollingList) field.get(screen);
                    if (list != null && seen.add(list)) result.add(list);
                } catch (IllegalAccessException ignored) {
                    // The page still exposes its buttons and other supported controls.
                }
            }
            type = type.getSuperclass();
        }
        return result;
    }

    private static UiRect rect(int x, int y, int width, int height) {
        return new UiRect(x, y, Math.max(x, x + width), Math.max(y, y + height));
    }

    static int listEntryLeft(int left, int right, int listWidth) {
        return left + (right - left) / 2 - listWidth / 2 + 2;
    }

    static UiNodeId containerSlotId(Class<?> screenClass, int slotOrdinal) {
        return id("screen/" + screenClass.getName() + "/slot/" + slotOrdinal);
    }

    private static UiNodeId id(String path) { return new UiNodeId(OWNER, path); }

    private interface Handle {}
    private static final class CreativeTabHandle implements Handle {
        private final int index;
        private CreativeTabHandle(int index) { this.index = index; }
    }
    private static final class CreativeTabListHandle implements Handle {}
    private static final class WidgetHandle implements Handle {
        private final Gui widget;
        private final GuiListExtended list;
        private final int index;
        private final UiRect bounds;

        private WidgetHandle(Gui widget, GuiListExtended list, int index, UiRect bounds) {
            this.widget = widget;
            this.list = list;
            this.index = index;
            this.bounds = bounds;
        }
    }
    private static final class SlotHandle implements Handle {
        private final Slot slot;
        private SlotHandle(Slot slot) { this.slot = slot; }
    }
    private static final class ListEntryHandle implements Handle {
        private final GuiSlot list;
        private final int index;
        private ListEntryHandle(GuiSlot list, int index) { this.list = list; this.index = index; }
    }
    private static final class ListHandle implements Handle {
        private final Object list;
        private ListHandle(Object list) { this.list = list; }
    }
    private static final class ExtendedRow {
        private final int entryIndex;
        private final Object entry;
        private final List<VanillaControlIntrospector.ControlRef> controls;
        private final UiNodeId rowId;

        private ExtendedRow(int entryIndex, Object entry,
                            List<VanillaControlIntrospector.ControlRef> controls,
                            UiNodeId rowId) {
            this.entryIndex = entryIndex;
            this.entry = entry;
            this.controls = controls;
            this.rowId = rowId;
        }
    }

    private static final class TemplateKey {
        private final Class<?> entryType;
        private final String fieldPath;

        private TemplateKey(Class<?> entryType, String fieldPath) {
            this.entryType = entryType;
            this.fieldPath = fieldPath;
        }

        @Override public boolean equals(Object object) {
            if (this == object) return true;
            if (!(object instanceof TemplateKey)) return false;
            TemplateKey other = (TemplateKey) object;
            return entryType == other.entryType && fieldPath.equals(other.fieldPath);
        }

        @Override public int hashCode() {
            return 31 * entryType.hashCode() + fieldPath.hashCode();
        }
    }
    private static final class ForgeListEntryHandle implements Handle {
        private final GuiScrollingList list;
        private final int index;
        private ForgeListEntryHandle(GuiScrollingList list, int index) {
            this.list = list;
            this.index = index;
        }
    }
}
