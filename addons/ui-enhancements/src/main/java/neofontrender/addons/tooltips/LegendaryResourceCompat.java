package neofontrender.addons.tooltips;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.resources.IResource;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.IResourceManagerReloadListener;
import net.minecraft.util.ResourceLocation;
import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.OreDictionary;

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
    private volatile List<Animation> animations = Collections.emptyList();

    private LegendaryResourceCompat() { }

    public List<Frame> frames() { return frames; }
    public List<Flipbook> flipbooks() { return flipbooks; }
    public List<Animation> animations() { return animations; }

    /** Returns the highest-priority resource frame whose simple selector matches the item. */
    public Frame match(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;
        String id = stack.getItem().getRegistryName() == null ? "" : stack.getItem().getRegistryName().toString();
        Frame best = null;
        for (Frame frame : frames) for (String selector : frame.selectors()) {
            boolean negate = selector.startsWith("!");
            String value = negate ? selector.substring(1) : selector;
            boolean hit = id.equals(value) || id.endsWith(":" + value) ||
                    (value.startsWith("%") && oreMatch(stack, value.substring(1))) ||
                    (value.startsWith("@") && id.startsWith(value.substring(1) + ":")) ||
                    ("epic".equalsIgnoreCase(value) && "EPIC".equals(stack.getRarity().name())) ||
                    ("rare".equalsIgnoreCase(value) && "RARE".equals(stack.getRarity().name()));
            if (negate) hit = !hit;
            if (hit && (best == null || frame.priority() > best.priority())) best = frame;
        }
        return best;
    }

    private static boolean oreMatch(ItemStack stack, String name) {
        for (ItemStack candidate : OreDictionary.getOres(name)) {
            if (candidate != null && candidate.getItem() == stack.getItem() &&
                    (candidate.getItemDamage() == OreDictionary.WILDCARD_VALUE || candidate.getItemDamage() == stack.getItemDamage())) return true;
        }
        return false;
    }

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
        animations = Collections.unmodifiableList(readAnimations(manager));
    }

    private static List<Animation> readAnimations(IResourceManager manager) {
        List<Animation> result = new ArrayList<>();
        List<ResourceLocation> textures = new ArrayList<>();
        for (Flipbook flipbook : INSTANCE.flipbooks) textures.add(new ResourceLocation(flipbook.texture()));
        for (Frame frame : INSTANCE.frames) if (!textures.contains(frame.image())) textures.add(frame.image());
        for (ResourceLocation id : textures) {
            String[] parts = id.toString().split(":", 2);
            String path = parts[1];
            if (path.endsWith(".png")) path += ".mcmeta";
            else path += ".png.mcmeta";
            ResourceLocation meta = new ResourceLocation(parts[0], path);
            try {
                IResource resource = manager.getResource(meta);
                try (InputStreamReader reader = new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8)) {
                    JsonElement root = new JsonParser().parse(reader);
                    JsonObject animation = root.isJsonObject() && root.getAsJsonObject().has("animation")
                            ? root.getAsJsonObject().getAsJsonObject("animation") : null;
                    if (animation != null) result.add(animation(id, animation));
                }
            } catch (Exception ignored) { }
        }
        return result;
    }

    private static Animation animation(ResourceLocation id, JsonObject o) {
        List<Integer> frames = integers(o.get("frames"));
        int time = integer(o, "frametime", 1);
        boolean interpolate = o.has("interpolate") && o.get("interpolate").getAsBoolean();
        return new Animation(id, time, frames, interpolate);
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
    public record Animation(ResourceLocation texture, int frameTime, List<Integer> frames, boolean interpolate) { }

    public int animationFrame(Animation animation, long ticks) {
        if (animation == null) return 0;
        List<Integer> sequence = animation.frames().isEmpty() ? Collections.singletonList(0) : animation.frames();
        int duration = Math.max(1, animation.frameTime());
        return sequence.get((int) ((ticks / duration) % sequence.size()));
    }
}
