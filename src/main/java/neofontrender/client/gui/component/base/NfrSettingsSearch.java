package neofontrender.client.gui.component.base;

import com.cleanroommc.modularui.api.widget.IWidget;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Supplier;

/** Shared search state and metadata for settings controls. */
public final class NfrSettingsSearch {
    private static final Map<IWidget, Supplier<String>> METADATA =
            Collections.synchronizedMap(new WeakHashMap<IWidget, Supplier<String>>());
    private static volatile String query = "";

    private NfrSettingsSearch() {}

    public static void query(String value) { query = value == null ? "" : value; }
    public static String query() { return query; }

    public static <T extends IWidget> T register(T widget, Supplier<String> label, String... keywords) {
        if (widget == null) return widget;
        METADATA.put(widget, () -> {
            StringBuilder text = new StringBuilder(label == null ? "" : String.valueOf(label.get()));
            if (keywords != null) for (String keyword : keywords) text.append(' ').append(keyword);
            return text.toString();
        });
        return widget;
    }

    public static boolean matches(IWidget widget) {
        String needle = query.trim().toLowerCase(java.util.Locale.ROOT);
        if (needle.isEmpty()) return true;
        Supplier<String> supplier = METADATA.get(widget);
        return supplier != null && supplier.get().toLowerCase(java.util.Locale.ROOT).contains(needle);
    }
}
