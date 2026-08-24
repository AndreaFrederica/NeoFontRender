package neofontrender.addons.navigation.vanilla;

import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiListExtended;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import neofontrender.addons.api.ui.navigation.UiRect;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/** Captures final vanilla widget geometry after other draw-time layout changes have run. */
public final class VanillaWidgetCapture {
    private static final Map<GuiScreen, FrameData> FRAMES = new WeakHashMap<>();
    private static GuiScreen drawingScreen;
    private static EntryContext entryContext;

    private VanillaWidgetCapture() {}

    public static synchronized void beginFrame(GuiScreen screen) {
        drawingScreen = screen;
        entryContext = null;
        frame(screen).begin();
    }

    public static synchronized void endFrame(GuiScreen screen) {
        if (drawingScreen != screen) return;
        entryContext = null;
        frame(screen).finish();
        drawingScreen = null;
    }

    public static synchronized void beginListEntry(GuiListExtended list, Object entry,
                                                   int index, int entryLeft, int rowTop) {
        if (drawingScreen == null) return;
        entryContext = new EntryContext(list, entry, index, entryLeft, rowTop);
    }

    public static synchronized void endListEntry(GuiListExtended list, Object entry) {
        if (entryContext != null && entryContext.list == list && entryContext.entry == entry) {
            entryContext = null;
        }
    }

    public static synchronized void widgetDrawn(Gui widget) {
        if (drawingScreen == null || !supported(widget)) return;
        UiRect bounds = bounds(widget);
        FrameData data = frame(drawingScreen);
        if (entryContext == null) {
            data.topLevel.put(widget, new CapturedWidget(widget, bounds));
            return;
        }
        RelativeGeometry geometry = new RelativeGeometry(
                bounds.left - entryContext.entryLeft,
                bounds.top - entryContext.rowTop,
                bounds.width(), bounds.height());
        data.captureRelative(widget, geometry);
    }

    static synchronized long revision(GuiScreen screen) { return frame(screen).revision; }

    static synchronized List<CapturedWidget> topLevel(GuiScreen screen) {
        return new ArrayList<>(frame(screen).publishedTopLevel);
    }

    static synchronized RelativeGeometry relativeGeometry(GuiScreen screen, Gui widget) {
        return frame(screen).relative.get(widget);
    }

    static synchronized long stableId(GuiScreen screen, Gui widget) {
        return frame(screen).stableId(widget);
    }

    private static FrameData frame(GuiScreen screen) {
        FrameData data = FRAMES.get(screen);
        if (data == null) {
            data = new FrameData();
            FRAMES.put(screen, data);
        }
        return data;
    }

    private static boolean supported(Gui widget) {
        return widget instanceof GuiButton || widget instanceof GuiTextField;
    }

    private static UiRect bounds(Gui widget) {
        if (widget instanceof GuiButton) {
            GuiButton button = (GuiButton) widget;
            return rect(button.x, button.y, button.width, button.height);
        }
        GuiTextField field = (GuiTextField) widget;
        return rect(field.x, field.y, field.width, field.height);
    }

    private static UiRect rect(int x, int y, int width, int height) {
        return new UiRect(x, y, Math.max(x, x + width), Math.max(y, y + height));
    }

    static final class CapturedWidget {
        final Gui widget;
        final UiRect bounds;

        CapturedWidget(Gui widget, UiRect bounds) {
            this.widget = widget;
            this.bounds = bounds;
        }
    }

    static final class RelativeGeometry {
        final int offsetX;
        final int offsetY;
        final int width;
        final int height;

        RelativeGeometry(int offsetX, int offsetY, int width, int height) {
            this.offsetX = offsetX;
            this.offsetY = offsetY;
            this.width = width;
            this.height = height;
        }

        UiRect at(int entryLeft, int rowTop) {
            return rect(entryLeft + offsetX, rowTop + offsetY, width, height);
        }

        @Override public boolean equals(Object object) {
            if (this == object) return true;
            if (!(object instanceof RelativeGeometry)) return false;
            RelativeGeometry other = (RelativeGeometry) object;
            return offsetX == other.offsetX && offsetY == other.offsetY
                    && width == other.width && height == other.height;
        }

        @Override public int hashCode() {
            int result = offsetX;
            result = 31 * result + offsetY;
            result = 31 * result + width;
            return 31 * result + height;
        }
    }

    private static final class EntryContext {
        private final GuiListExtended list;
        private final Object entry;
        @SuppressWarnings("unused") private final int index;
        private final int entryLeft;
        private final int rowTop;

        private EntryContext(GuiListExtended list, Object entry, int index,
                             int entryLeft, int rowTop) {
            this.list = list;
            this.entry = entry;
            this.index = index;
            this.entryLeft = entryLeft;
            this.rowTop = rowTop;
        }
    }

    private static final class FrameData {
        private final IdentityHashMap<Gui, CapturedWidget> topLevel = new IdentityHashMap<>();
        private final IdentityHashMap<Gui, RelativeGeometry> relative = new IdentityHashMap<>();
        private final IdentityHashMap<Gui, Long> widgetIds = new IdentityHashMap<>();
        private List<CapturedWidget> publishedTopLevel = Collections.emptyList();
        private long revision;
        private long nextWidgetId;

        private void begin() { topLevel.clear(); }

        private void captureRelative(Gui widget, RelativeGeometry geometry) {
            stableId(widget);
            RelativeGeometry previous = relative.put(widget, geometry);
            if (!geometry.equals(previous)) revision++;
        }

        private long stableId(Gui widget) {
            Long existing = widgetIds.get(widget);
            if (existing != null) return existing;
            long assigned = ++nextWidgetId;
            widgetIds.put(widget, assigned);
            return assigned;
        }

        private void finish() {
            List<CapturedWidget> next = new ArrayList<>(topLevel.values());
            for (CapturedWidget widget : next) stableId(widget.widget);
            next.sort((left, right) -> {
                int y = Integer.compare(left.bounds.top, right.bounds.top);
                if (y != 0) return y;
                int x = Integer.compare(left.bounds.left, right.bounds.left);
                if (x != 0) return x;
                return Integer.compare(System.identityHashCode(left.widget),
                        System.identityHashCode(right.widget));
            });
            if (!sameWidgets(publishedTopLevel, next)) revision++;
            publishedTopLevel = next;
        }

        private static boolean sameWidgets(List<CapturedWidget> left, List<CapturedWidget> right) {
            if (left.size() != right.size()) return false;
            for (int index = 0; index < left.size(); index++) {
                CapturedWidget a = left.get(index);
                CapturedWidget b = right.get(index);
                if (a.widget != b.widget || !a.bounds.equals(b.bounds)) return false;
            }
            return true;
        }
    }
}
