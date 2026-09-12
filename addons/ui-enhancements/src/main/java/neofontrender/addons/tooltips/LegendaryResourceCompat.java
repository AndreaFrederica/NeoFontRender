package neofontrender.addons.tooltips;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.resources.IResource;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.IResourceManagerReloadListener;
import net.minecraft.util.ResourceLocation;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Reads the public LegendaryTooltips resource-pack data format without depending on that mod. */
public final class LegendaryResourceCompat implements IResourceManagerReloadListener {
    public static final LegendaryResourceCompat INSTANCE = new LegendaryResourceCompat();
    private static final ResourceLocation DEFINITIONS = new ResourceLocation("legendarytooltips", "frame_definitions.json");
    private static final ResourceLocation FLIPBOOKS = new ResourceLocation("minecraft", "textures/flipbook_textures.json");
    private volatile List<Frame> frames = Collections.emptyList();
    private volatile List<Flipbook> flipbooks = Collections.emptyList();

    private LegendaryResourceCompat() { }

    public List<Frame> frames() { return frames; }
    public List<Flipbook> flipbooks() { return flipbooks; }

    @Override public synchronized void onResourceManagerReload(IResourceManager manager) {
        List<Frame> nextFrames = new ArrayList<>();
        try {
            for (IResource resource : manager.getAllResources(DEFINITIONS)) {
                try (InputStreamReader reader = new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8)) {
                    JsonElement root = new JsonParser().parse(reader);
                    if (!root.isJsonArray()) continue;
                    for (JsonElement value : root.getAsJsonArray()) {
                        if (!value.isJsonObject()) continue;
                        JsonObject o = value.getAsJsonObject();
                        String image = string(o, "image", "legendarytooltips:textures/gui/tooltip_borders.png");
                        int index = integer(o, "index", 0);
                        int priority = integer(o, "priority", 0);
                        List<String> selectors = strings(o.get("selectors"));
                        if (!selectors.isEmpty()) nextFrames.add(new Frame(new ResourceLocation(image), index, priority, selectors));
                    }
                }
            }
        } catch (Exception ignored) { }
        frames = Collections.unmodifiableList(nextFrames);
        flipbooks = Collections.unmodifiableList(readFlipbooks(manager));
    }

    private static List<Flipbook> readFlipbooks(IResourceManager manager) {
        List<Flipbook> result = new ArrayList<>();
        try {
            for (IResource resource : manager.getAllResources(FLIPBOOKS)) {
                try (InputStreamReader reader = new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8)) {
                    JsonElement root = new JsonParser().parse(reader);
                    if (!root.isJsonArray()) continue;
                    for (JsonElement value : root.getAsJsonArray()) {
                        if (!value.isJsonObject()) continue;
                        JsonObject o = value.getAsJsonObject();
                        String texture = string(o, "flipbook_texture", string(o, "atlas_tile", ""));
                        if (texture.isEmpty()) continue;
                        result.add(new Flipbook(texture, integer(o, "ticks_per_frame", 1),
                                integers(o.get("frames")), o.has("blend_frames") && o.get("blend_frames").getAsBoolean()));
                    }
                }
            }
        } catch (Exception ignored) { }
        return result;
    }

    private static String string(JsonObject o, String key, String fallback) { return o.has(key) ? o.get(key).getAsString() : fallback; }
    private static int integer(JsonObject o, String key, int fallback) { try { return o.has(key) ? o.get(key).getAsInt() : fallback; } catch (Exception e) { return fallback; } }
    private static List<String> strings(JsonElement e) { List<String> r = new ArrayList<>(); if (e != null && e.isJsonArray()) for (JsonElement x : e.getAsJsonArray()) if (x.isJsonPrimitive()) r.add(x.getAsString()); return r; }
    private static List<Integer> integers(JsonElement e) { List<Integer> r = new ArrayList<>(); if (e != null && e.isJsonArray()) for (JsonElement x : e.getAsJsonArray()) if (x.isJsonPrimitive()) try { r.add(x.getAsInt()); } catch (Exception ignored) { } return r; }

    public record Frame(ResourceLocation image, int index, int priority, List<String> selectors) { }
    public record Flipbook(String texture, int ticksPerFrame, List<Integer> frames, boolean blendFrames) { }
}
