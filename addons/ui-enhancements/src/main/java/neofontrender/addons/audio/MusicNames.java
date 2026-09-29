package neofontrender.addons.audio;

import net.minecraft.util.ResourceLocation;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/** Display names for vanilla 1.12.2 music (C418) and fallback derivation for other entries. */
public final class MusicNames {
    private static final Map<String, String> VANILLA = new HashMap<>();
    static {
        VANILLA.put("minecraft:music/menu/menu1", "Mutation");
        VANILLA.put("minecraft:music/menu/menu2", "Moog City 2");
        VANILLA.put("minecraft:music/menu/menu3", "Beginning 2");
        VANILLA.put("minecraft:music/menu/menu4", "Floating Trees");
        VANILLA.put("minecraft:music/game/calm1", "Minecraft");
        VANILLA.put("minecraft:music/game/calm2", "Clark");
        VANILLA.put("minecraft:music/game/calm3", "Sweden");
        VANILLA.put("minecraft:music/game/hal1", "Subwoofer Lullaby");
        VANILLA.put("minecraft:music/game/hal2", "Living Mice");
        VANILLA.put("minecraft:music/game/hal3", "Haggstrom");
        VANILLA.put("minecraft:music/game/hal4", "Danny");
        VANILLA.put("minecraft:music/game/nuance1", "Key");
        VANILLA.put("minecraft:music/game/nuance2", "Oxygene");
        VANILLA.put("minecraft:music/game/piano1", "Dry Hands");
        VANILLA.put("minecraft:music/game/piano2", "Wet Hands");
        VANILLA.put("minecraft:music/game/piano3", "Mice on Venus");
        VANILLA.put("minecraft:music/game/creative/creative1", "Biome Fest");
        VANILLA.put("minecraft:music/game/creative/creative2", "Blind Spots");
        VANILLA.put("minecraft:music/game/creative/creative3", "Haunt Muskie");
        VANILLA.put("minecraft:music/game/creative/creative4", "Aria Math");
        VANILLA.put("minecraft:music/game/creative/creative5", "Dreiton");
        VANILLA.put("minecraft:music/game/creative/creative6", "Taswell");
        VANILLA.put("minecraft:music/game/nether/nether1", "Concrete Halls");
        VANILLA.put("minecraft:music/game/nether/nether2", "Dead Voxel");
        VANILLA.put("minecraft:music/game/nether/nether3", "Warmth");
        VANILLA.put("minecraft:music/game/nether/nether4", "Ballad of the Cats");
        VANILLA.put("minecraft:music/game/end/end", "The End");
        VANILLA.put("minecraft:music/game/end/boss", "Boss");
        VANILLA.put("minecraft:music/game/end/credits", "Alpha");
    }

    /** Named title for a vanilla sound location, or null when unknown. */
    public static String vanillaTitle(ResourceLocation sound) {
        return VANILLA.get(sound.toString());
    }

    /** Display name for a library entry: vanilla title, then filename without extension. */
    public static String display(String entry) {
        String albumTitle = AudioModule.albumTrackDisplay(entry);
        if (albumTitle != null) return albumTitle;
        String vanilla = VANILLA.get(entry);
        if (vanilla != null) return vanilla + " - C418";
        if (entry.startsWith("res:")) {
            ResourceLocation loc = new ResourceLocation(entry.substring(4));
            // Resource entries point at the file: <domain>:sounds/music/game/calm1.ogg
            String path = loc.getPath();
            String soundPath = path.startsWith("sounds/") ? path.substring(7) : path;
            if (soundPath.endsWith(".ogg")) soundPath = soundPath.substring(0, soundPath.length() - 4);
            String title = VANILLA.get(loc.getNamespace() + ":" + soundPath);
            if (title != null) return title + " (C418)";
            String file = soundPath.substring(soundPath.lastIndexOf('/') + 1);
            return loc.getNamespace() + ": " + file;
        }
        try {
            Path path = Paths.get(entry).getFileName();
            String name = path == null ? entry : path.toString();
            int dot = name.lastIndexOf('.');
            return dot > 0 ? name.substring(0, dot) : name;
        } catch (Exception e) {
            return entry;
        }
    }

    private MusicNames() {}
}
