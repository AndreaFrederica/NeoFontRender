package neofontrender.addons.tips;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.IResource;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.IResourceManagerReloadListener;
import net.minecraft.util.ResourceLocation;
import neofontrender.addons.ui.NfrUiEnhancements;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Loads tips from registered resource locations and manages cycling.
 * <p>
 * Compatible with the original Tips mod format: {@code assets/<namespace>/tips/*.json}.
 * Also supports {@code assets/<namespace>/tips/<name>.json} with {@code {"type":"tipsmod:simple", ...}}.
 * <p>
 * The built-in tip file and the Tips mod's default path are both registered
 * so that existing resource packs continue to work.
 */
public enum TipManager implements IResourceManagerReloadListener {
    INSTANCE;

    private static final Gson GSON = new Gson();

    private final Set<ResourceLocation> registeredFiles = new LinkedHashSet<>();
    private final List<Tip> allTips = new ArrayList<>();
    private final List<Tip> shuffled = new ArrayList<>();
    private int cycleIndex;
    private long lastCycleNanos;
    private Tip currentTip;

    /** Register a tip JSON file to be loaded on next resource reload. */
    public void registerTipFile(ResourceLocation file) {
        registeredFiles.add(file);
    }

    public void unregisterTipFile(ResourceLocation file) {
        registeredFiles.remove(file);
    }

    public List<Tip> getAllTips() {
        return Collections.unmodifiableList(allTips);
    }

    public Tip currentTip() {
        return currentTip;
    }

    /** Returns the next tip if enough time has elapsed, or null to keep the current one. */
    public Tip update(long nowNanos, int defaultCycleTime) {
        if (shuffled.isEmpty()) return null;
        if (currentTip == null) {
            currentTip = shuffled.get(0);
            lastCycleNanos = nowNanos;
            return currentTip;
        }
        int cycleMs = currentTip.cycleTimeMillis() > 0
                ? currentTip.cycleTimeMillis() : defaultCycleTime;
        long elapsedMs = (nowNanos - lastCycleNanos) / 1_000_000L;
        if (elapsedMs >= cycleMs) {
            cycleIndex = (cycleIndex + 1) % shuffled.size();
            currentTip = shuffled.get(cycleIndex);
            lastCycleNanos = nowNanos;
            return currentTip;
        }
        return null;
    }

    public void reset() {
        currentTip = null;
        cycleIndex = 0;
        lastCycleNanos = 0;
    }

    @Override
    public void onResourceManagerReload(IResourceManager resourceManager) {
        allTips.clear();
        shuffled.clear();

        for (ResourceLocation file : registeredFiles) {
            loadTipFile(resourceManager, file);
        }

        shuffled.addAll(allTips);
        Collections.shuffle(shuffled);
        reset();

        NfrUiEnhancements.LOGGER.info("Loaded {} tips from {} registered files",
                allTips.size(), registeredFiles.size());
    }

    private void loadTipFile(IResourceManager rm, ResourceLocation file) {
        try {
            List<IResource> resources = rm.getAllResources(file);
            for (IResource resource : resources) {
                try (InputStream is = resource.getInputStream()) {
                    JsonObject root = GSON.fromJson(
                            new InputStreamReader(is, StandardCharsets.UTF_8), JsonObject.class);
                    parseTips(file, root);
                }
            }
        } catch (Exception e) {
            NfrUiEnhancements.LOGGER.debug("Could not load tip file {}: {}", file, e.getMessage());
        }
    }

    /**
     * Parses a tip file. Supports three formats:
     * <ul>
     *   <li>Single tip: {@code {"text": {"translate": "key"}}}</li>
     *   <li>Array: {@code {"tips": [ {"text": {...}}, ... ]}}</li>
     *   <li>Typed (Tips mod compat): {@code {"type": "tipsmod:simple", "text": {...}}}</li>
     * </ul>
     */
    private void parseTips(ResourceLocation file, JsonObject root) {
        // Typed single tip (Tips mod format)
        if (root.has("type") && root.has("text")) {
            Tip tip = parseSingleTip(file, root);
            if (tip != null) allTips.add(tip);
            return;
        }
        // Untyped single tip
        if (root.has("text") && !root.has("tips")) {
            Tip tip = parseSingleTip(file, root);
            if (tip != null) allTips.add(tip);
            return;
        }
        // Array of tips
        if (root.has("tips") && root.get("tips").isJsonArray()) {
            int index = 0;
            for (JsonElement elem : root.getAsJsonArray("tips")) {
                if (elem.isJsonObject()) {
                    ResourceLocation id = new ResourceLocation(
                            file.getResourceDomain(),
                            file.getResourcePath().replace(".json", "") + "/" + index);
                    Tip tip = parseSingleTip(id, elem.getAsJsonObject());
                    if (tip != null) allTips.add(tip);
                    index++;
                }
            }
        }
    }

    private Tip parseSingleTip(ResourceLocation id, JsonObject json) {
        String textKey = extractTextKey(json.get("text"));
        if (textKey.isEmpty()) return null;

        String titleKey = "";
        if (json.has("title")) {
            titleKey = extractTextKey(json.get("title"));
        }

        int cycleTime = 0;
        if (json.has("cycle_time")) {
            cycleTime = json.get("cycle_time").getAsInt();
        }

        String category = "";
        if (json.has("category")) {
            category = json.get("category").getAsString();
        }

        return new Tip(id, textKey, titleKey, cycleTime, category);
    }

    private String extractTextKey(JsonElement element) {
        if (element == null) return "";
        if (element.isJsonPrimitive()) return element.getAsString();
        if (element.isJsonObject()) {
            JsonObject obj = element.getAsJsonObject();
            if (obj.has("translate")) return obj.get("translate").getAsString();
            if (obj.has("text")) return obj.get("text").getAsString();
            if (obj.has("key")) return obj.get("key").getAsString();
        }
        return "";
    }
}
