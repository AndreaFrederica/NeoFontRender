package neofontrender.api.client.settings;

import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Process-wide registry for About and third-party license information. */
@SideOnly(Side.CLIENT)
public final class NfrInfoPageRegistry {
    private static final Map<String, NfrInfoPageContribution> CONTRIBUTIONS = new LinkedHashMap<>();

    private NfrInfoPageRegistry() {}

    public static synchronized void register(NfrInfoPageContribution contribution) {
        if (contribution == null) throw new IllegalArgumentException("contribution must not be null");
        String id = validateId(contribution.id());
        if (contribution.page() == null) throw new IllegalArgumentException("page must not be null");
        if (contribution.lines() == null) throw new IllegalArgumentException("lines must not be null");
        if (CONTRIBUTIONS.containsKey(id)) throw new IllegalStateException("Info contribution already registered: " + id);
        CONTRIBUTIONS.put(id, contribution);
    }

    public static synchronized boolean unregister(String id) {
        return CONTRIBUTIONS.remove(validateId(id)) != null;
    }

    public static synchronized List<NfrInfoPageContribution> snapshot(NfrInfoPage page) {
        if (page == null) throw new IllegalArgumentException("page must not be null");
        List<NfrInfoPageContribution> result = new ArrayList<>();
        for (NfrInfoPageContribution contribution : CONTRIBUTIONS.values()) {
            if (contribution.page() == page) result.add(contribution);
        }
        result.sort(Comparator.comparingInt(NfrInfoPageContribution::order)
                .thenComparing(NfrInfoPageContribution::id));
        return result;
    }

    private static String validateId(String id) {
        if (id == null || !id.matches("[a-z0-9_.-]+:[a-z0-9_.-]+"))
            throw new IllegalArgumentException("contribution id must be namespaced, for example modid:about");
        return id;
    }
}
