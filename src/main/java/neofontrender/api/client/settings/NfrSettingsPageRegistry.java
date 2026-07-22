package neofontrender.api.client.settings;

import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Process-wide registry used by integrations to add tabs to NFR's settings screen. */
@SideOnly(Side.CLIENT)
public final class NfrSettingsPageRegistry {
    private static final Map<String, NfrSettingsPage> PAGES = new LinkedHashMap<>();

    private NfrSettingsPageRegistry() {}

    public static synchronized void register(NfrSettingsPage page) {
        if (page == null) throw new IllegalArgumentException("page must not be null");
        String id = validateId(page.id());
        if (PAGES.containsKey(id)) throw new IllegalStateException("Settings page already registered: " + id);
        PAGES.put(id, page);
    }

    public static synchronized boolean unregister(String id) {
        return PAGES.remove(validateId(id)) != null;
    }

    public static synchronized List<NfrSettingsPage> snapshot() {
        List<NfrSettingsPage> pages = new ArrayList<>(PAGES.values());
        pages.sort(Comparator.comparingInt(NfrSettingsPage::order).thenComparing(NfrSettingsPage::id));
        return pages;
    }

    private static String validateId(String id) {
        if (id == null || !id.matches("[a-z0-9_.-]+:[a-z0-9_.-]+"))
            throw new IllegalArgumentException("page id must be namespaced, for example modid:page");
        return id;
    }
}
