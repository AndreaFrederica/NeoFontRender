package neofontrender.api.client.settings;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Process-wide registry for extension controls embedded into NFR's built-in settings pages. */
@SideOnly(Side.CLIENT)
public final class NfrSettingsSectionRegistry {
    private static final Map<String, NfrSettingsSectionContribution> CONTRIBUTIONS =
            new LinkedHashMap<>();

    private NfrSettingsSectionRegistry() {}

    public static synchronized void register(NfrSettingsSectionContribution contribution) {
        if (contribution == null) throw new IllegalArgumentException("contribution must not be null");
        String id = validateId(contribution.id());
        if (contribution.section() == null) throw new IllegalArgumentException("section must not be null");
        if (CONTRIBUTIONS.containsKey(id)) {
            throw new IllegalStateException("Settings section contribution already registered: " + id);
        }
        CONTRIBUTIONS.put(id, contribution);
    }

    public static synchronized boolean unregister(String id) {
        return CONTRIBUTIONS.remove(validateId(id)) != null;
    }

    public static synchronized List<NfrSettingsSectionContribution> snapshot(
            NfrSettingsSection section) {
        List<NfrSettingsSectionContribution> result = new ArrayList<>();
        for (NfrSettingsSectionContribution contribution : CONTRIBUTIONS.values()) {
            if (contribution.section() == section) result.add(contribution);
        }
        result.sort(Comparator.comparingInt(NfrSettingsSectionContribution::order)
                .thenComparing(NfrSettingsSectionContribution::id));
        return result;
    }

    private static String validateId(String id) {
        if (id == null || !id.matches("[a-z0-9_.-]+:[a-z0-9_.-]+")) {
            throw new IllegalArgumentException(
                    "contribution id must be namespaced, for example modid:feature");
        }
        return id;
    }
}
