package neofontrender.addons.audio;

import com.google.gson.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.*;
import net.minecraft.client.resources.*;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import neofontrender.addons.ui.*;
import neofontrender.api.client.settings.NfrSettingsPageRegistry;
import neofontrender.audio.SceneMusicPolicy;
import org.apache.logging.log4j.*;
import paulscode.sound.SoundSystemConfig;
import java.io.*;
import java.nio.file.*;
import java.util.*;

public final class AudioModule implements UiEnhancementModule, IResourceManagerReloadListener {
    public static final Logger LOG = LogManager.getLogger("Revo UI Audio");
    public static AudioLibrary library;
    static boolean sceneMode, exclusive = true;
    static String selectedPlaylist = "Library";
    static String status = "";
    private AutoCloseable focus;
    private MusicTicker.MusicType previousScene;
    private SceneMusicPolicy policy = new SceneMusicPolicy(new Random());
    private MusicPlayer.State previousState;
    private static final Map<String, List<JsonElement>> definitions = new HashMap<>();
    private static final Map<String, AudioLibrary.Album> albums = new LinkedHashMap<>();
    private static final Map<String, Integer> ALPHA_TRACKS = trackNumbers(
            "Key", 1, "Subwoofer Lullaby", 3, "Living Mice", 5, "Haggstrom", 7,
            "Minecraft", 8, "Oxygene", 9, "Mice on Venus", 11, "Dry Hands", 12,
            "Wet Hands", 13, "Clark", 14, "Sweden", 18, "Danny", 21);
    private static final Map<String, Integer> BETA_TRACKS = trackNumbers(
            "Alpha", 2, "Dead Voxel", 3, "Blind Spots", 4, "Moog City 2", 6,
            "Concrete Halls", 7, "Biome Fest", 8, "Mutation", 9, "Haunt Muskie", 10,
            "Warmth", 11, "Floating Trees", 12, "Aria Math", 13,
            "Ballad of the Cats", 15, "Taswell", 16, "Beginning 2", 17,
            "Dreiton", 18, "The End", 19);
    public void preInit() {
        library = new AudioLibrary(Minecraft.getMinecraft().gameDir.toPath().resolve("config/revo-ui-audio"));
        library.load();
        // Local albums are derived from file tags, never user-authored data.
        // Drop stale pre-tagging results immediately so the UI cannot expose
        // legacy one-track albums named after folders (for example "music").
        library.albums.clear();
        library.indexMetadataAlbums().whenComplete((count, error) -> {
            if (error != null) LOG.warn("Cannot index local music metadata", error);
            else Minecraft.getMinecraft().addScheduledTask(AudioModule::refreshLocalAlbums);
        });
        var config = UiEnhancementsConfig.file();
        config.define("audio.sceneMode", false, "Use Minecraft scene scheduling for independent music.")
              .define("audio.exclusive", true, "Pause vanilla BGM while independent music is active.")
              .define("audio.vanillaEnabled", true, "Allow vanilla music scheduling.")
              .define("audio.vanillaOrdered", false, "Play vanilla scene entries in resource order.");
        sceneMode = config.getBoolean("audio.sceneMode", false);
        exclusive = config.getBoolean("audio.exclusive", true);
        UieAudio.vanillaMusic().enabled(config.getBoolean("audio.vanillaEnabled", true));
        UieAudio.vanillaMusic().ordered(config.getBoolean("audio.vanillaOrdered", false));
    }
    private static boolean shutdownHookInstalled;
    public void init() {
        try { SoundSystemConfig.setCodec("nfraudio", LavaStreamingCodec.class); }
        catch (Exception error) { throw new IllegalStateException("Cannot register local music codec", error); }
        NfrSettingsPageRegistry.register(new AudioSettingsPage(false));
        NfrSettingsPageRegistry.register(new AudioSettingsPage(true));
        MinecraftForge.EVENT_BUS.register(this);
        ((IReloadableResourceManager) Minecraft.getMinecraft().getResourceManager()).registerReloadListener(this);
        if (!shutdownHookInstalled) {
            shutdownHookInstalled = true;
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                UieAudio.closeAll();
                if (library != null) library.close();
            }, "UIE audio shutdown"));
        }
    }
    static void save() {
        UiEnhancementsConfig.file().set("audio.sceneMode", sceneMode).set("audio.exclusive", exclusive)
                .set("audio.vanillaEnabled", UieAudio.vanillaMusic().enabled())
                .set("audio.vanillaOrdered", UieAudio.vanillaMusic().ordered()).save();
        library.save();
    }
    @SubscribeEvent public void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getMinecraft();
        UieAudio.tick();
        MusicPlayer player = UieAudio.independent();
        if (sceneMode && !mc.isGamePaused()) {
            MusicTicker.MusicType scene = mc.getAmbientMusicType();
            player.autoAdvance(false);
            boolean active = player.state() == MusicPlayer.State.PLAYING || player.state() == MusicPlayer.State.LOADING || player.state() == MusicPlayer.State.PAUSED;
            if (player.state() == MusicPlayer.State.FINISHED && previousState != MusicPlayer.State.FINISHED)
                policy.finished(scene.getMinDelay(), scene.getMaxDelay());
            SceneMusicPolicy.Action action = policy.tick(scene.name(), scene.getMinDelay(), scene.getMaxDelay(), active, player.state() == MusicPlayer.State.PAUSED);
            if (action == SceneMusicPolicy.Action.STOP) player.stop();
            if (scene != previousScene) {
                String list = library.scenes.get(scene.name());
                List<String> entries = list == null ? Collections.emptyList() : playlistEntries(list);
                player.playlist().replace(resolveAll(entries));
                previousScene = scene;
            }
            if (action == SceneMusicPolicy.Action.PLAY && !player.playlist().tracks().isEmpty()) {
                player.playlist().mode(neofontrender.audio.Playlist.Mode.REPEAT_ALL); player.next();
            }
        } else { player.autoAdvance(true); previousScene = null; }
        previousState = player.state();
        boolean owns = exclusive && (sceneMode || player.state() == MusicPlayer.State.PLAYING || player.state() == MusicPlayer.State.LOADING || player.state() == MusicPlayer.State.PAUSED);
        try {
            if (owns && focus == null) focus = UieAudio.vanillaMusic().focus();
            else if (!owns && focus != null) { focus.close(); focus = null; }
        } catch (Exception e) { LOG.warn("Music focus release failed", e); }
    }
    /** Identity-compared against a page's entry list; maps queue index to entry index. */
    static List<String> lastEntries;
    static List<Integer> lastMapping = Collections.emptyList();
    /** Resolve all entries, skipping (and warming) uncached resources; records the queue mapping. */
    static List<Path> resolveAll(List<String> entries) {
        List<Path> result = new ArrayList<>();
        List<Integer> mapping = new ArrayList<>();
        for (int i = 0; i < entries.size(); i++) {
            Path p = resolve(entries.get(i));
            if (p != null) { mapping.add(i); result.add(p); }
        }
        lastEntries = entries;
        lastMapping = mapping;
        return result;
    }
    static List<Path> paths(String playlist) {
        return resolveAll(playlistEntries(playlist));
    }
    static Collection<String> playlistNames() {
        List<String> names = new ArrayList<>(library.playlists.keySet());
        for (String id : albums.keySet()) names.add(albumPlaylistId(id));
        return names;
    }
    static void refreshLocalAlbums() {
        for (String id : new ArrayList<>(albums.keySet())) if (id.startsWith("local/")) albums.remove(id);
        for (Map.Entry<String, AudioLibrary.Album> entry : library.albums.entrySet())
            albums.put("local/" + entry.getKey(), entry.getValue());
    }
    static List<String> playlistEntries(String playlist) {
        if (!playlist.startsWith("@album/")) return library.playlists.getOrDefault(playlist, Collections.emptyList());
        AudioLibrary.Album album = albums.get(playlist.substring(7));
        if (album == null) return Collections.emptyList();
        List<String> result = new ArrayList<>();
        for (AudioLibrary.AlbumTrack track : album.tracks) if (track.source != null && !track.source.isEmpty()) result.add(track.source);
        return result;
    }
    static String playlistDisplay(String playlist) {
        if (!playlist.startsWith("@album/")) return "DIR  " + playlist;
        AudioLibrary.Album album = albums.get(playlist.substring(7));
        return "ALBUM  " + (album == null || album.title.isEmpty() ? playlist.substring(7) : album.title);
    }
    static boolean readOnlyPlaylist(String playlist) { return playlist.startsWith("@album/"); }
    /** True for local files that are visibly unusable (currently empty/truncated files). */
    static boolean isBrokenEntry(String entry) {
        if (entry == null || entry.startsWith("res:")) return false;
        try { return Files.isRegularFile(Paths.get(entry)) && Files.size(Paths.get(entry)) == 0; }
        catch (Exception ignored) { return false; }
    }
    private static String albumPlaylistId(String id) { return "@album/" + id; }
    static String albumTrackDisplay(String source) {
        String result = null;
        for (AudioLibrary.Album album : albums.values()) for (AudioLibrary.AlbumTrack track : album.tracks) {
            if (!source.equals(track.source) || track.title == null || track.title.isEmpty()) continue;
            String artist = track.artist == null || track.artist.isEmpty() ? album.artist : track.artist;
            result = artist == null || artist.isEmpty() ? track.title : track.title + " - " + artist;
        }
        return result;
    }
    static List<MusicAlbum> albumCatalog() {
        List<MusicAlbum> result = new ArrayList<>();
        for (Map.Entry<String, AudioLibrary.Album> entry : albums.entrySet())
            result.add(new MusicAlbum(entry.getKey(), entry.getValue()));
        return Collections.unmodifiableList(result);
    }
    static void playAlbum(String id, int trackIndex) {
        AudioLibrary.Album album = albums.get(id);
        if (album == null) throw new IllegalArgumentException("Unknown album: " + id);
        List<String> entries = playlistEntries(albumPlaylistId(id));
        if (trackIndex < 0 || trackIndex >= entries.size()) throw new IndexOutOfBoundsException("Track " + trackIndex);
        sceneMode = false;
        play(entries, trackIndex);
    }
    private static Path resolve(String entry) {
        try {
            if (entry.startsWith("res:")) {
                ResourceLocation location = new ResourceLocation(entry.substring(4));
                Path cached = library.cachedResource(location);
                if (Files.isRegularFile(cached)) return cached;
                library.materializeAsync(location);
                return null;
            }
            Path path = Paths.get(entry).toAbsolutePath().normalize();
            return Files.isRegularFile(path) ? path : null;
        } catch (Exception error) { status = "Cannot resolve " + entry; return null; }
    }
    /** Entry index currently audible in the independent player, or -1. */
    static int playingEntryIndex(List<String> entries) {
        MusicPlayer player = UieAudio.independent();
        int queue = player.playlist().index();
        if (queue < 0 || entries != lastEntries || queue >= lastMapping.size()) return -1;
        if (player.state() != MusicPlayer.State.PLAYING && player.state() != MusicPlayer.State.PAUSED
                && player.state() != MusicPlayer.State.LOADING) return -1;
        return lastMapping.get(queue);
    }
    /** Play entries[index], materializing resource entries on first use. Client thread only. */
    static void play(List<String> entries, int index) { play(entries, index, true); }
    private static void play(List<String> entries, int index, boolean allowWarm) {
        MusicPlayer player = UieAudio.independent();
        List<Path> resolved = resolveAll(entries);
        int target = -1;
        for (int i = 0; i < lastMapping.size(); i++) if (lastMapping.get(i) == index) { target = i; break; }
        if (target >= 0) { player.playlist().replace(resolved); player.select(target); return; }
        String entry = entries.get(index);
        if (!entry.startsWith("res:") || !allowWarm) { status = "Cannot resolve " + entry; return; }
        status = "Preparing " + MusicNames.display(entry) + "...";
        library.materializeAsync(new ResourceLocation(entry.substring(4)))
                .whenComplete((p, err) -> Minecraft.getMinecraft().addScheduledTask(() -> {
                    if (err != null) status = "Cannot load " + entry + ": " + err;
                    else play(entries, index, false);
                }));
    }
    public void onResourceManagerReload(IResourceManager manager) {
        definitions.clear();
        albums.clear();
        for (String domain : manager.getResourceDomains()) {            try {
                for (IResource resource : manager.getAllResources(new ResourceLocation(domain, "sounds.json"))) {
                    try (IResource closeable = resource; Reader reader = new InputStreamReader(resource.getInputStream(), java.nio.charset.StandardCharsets.UTF_8)) {
                        JsonObject json = new Gson().fromJson(reader, JsonObject.class);
                        for (Map.Entry<String, JsonElement> entry : json.entrySet()) {
                            JsonObject event = entry.getValue().getAsJsonObject();
                            String id = domain + ":" + entry.getKey();
                            if (event.has("replace") && event.get("replace").getAsBoolean()) definitions.remove(id);
                            List<JsonElement> list = definitions.computeIfAbsent(id, k -> new ArrayList<>());
                            if (event.has("sounds")) for (JsonElement sound : event.getAsJsonArray("sounds")) list.add(sound);
                        }
                    }
                }
            } catch (FileNotFoundException ignored) {} catch (Exception e) { LOG.warn("Cannot index music in {}", domain, e); }
        }
        addVanillaAlbums();
        for (Map.Entry<String, AudioLibrary.Album> entry : library.albums.entrySet())
            albums.put("local/" + entry.getKey(), entry.getValue());
        loadResourceAlbums(manager);
        // A fresh library starts with the vanilla soundtrack as a built-in playlist.
        if (library != null && !library.defaultsSeeded) {
            List<String> vanilla = new ArrayList<>();
            Set<String> seen = new HashSet<>();
            for (MusicTicker.MusicType type : MusicTicker.MusicType.values()) {
                for (Sound sound : catalog(type)) {
                    ResourceLocation loc = sound.getSoundLocation();
                    String entry = "res:" + loc.getNamespace() + ":sounds/" + loc.getPath() + ".ogg";
                    if (seen.add(entry)) vanilla.add(entry);
                }
            }
            if (!vanilla.isEmpty()) {
                library.playlists.putIfAbsent("Vanilla Music", vanilla);
                library.defaultsSeeded = true;
                library.save();
            }
        }
    }
    private static void addVanillaAlbums() {
        AudioLibrary.Album alpha = album("Minecraft - Volume Alpha", "C418", "minecraft:textures/items/record_13.png", 2011);
        AudioLibrary.Album beta = album("Minecraft - Volume Beta", "C418", "minecraft:textures/items/record_wait.png", 2013);
        Set<String> seen = new HashSet<>();
        for (MusicTicker.MusicType type : MusicTicker.MusicType.values()) for (Sound sound : catalog(type)) {
            ResourceLocation location = sound.getSoundLocation();
            String source = "res:" + location.getNamespace() + ":sounds/" + location.getPath() + ".ogg";
            if (!seen.add(source)) continue;
            String title = MusicNames.vanillaTitle(location);
            AudioLibrary.Album album = ALPHA_TRACKS.containsKey(title) ? alpha : beta;
            Map<String, Integer> numbers = album == alpha ? ALPHA_TRACKS : BETA_TRACKS;
            addTrack(album, source, title == null ? MusicNames.display(source) : title, "C418", numbers.getOrDefault(title, 0));
        }
        Comparator<AudioLibrary.AlbumTrack> order = Comparator.comparingInt(track -> track.trackNumber == 0 ? Integer.MAX_VALUE : track.trackNumber);
        alpha.tracks.sort(order); beta.tracks.sort(order);
        if (!alpha.tracks.isEmpty()) albums.put("minecraft/volume_alpha", alpha);
        if (!beta.tracks.isEmpty()) albums.put("minecraft/volume_beta", beta);
    }
    private static AudioLibrary.Album album(String title, String artist, String cover, int year) {
        AudioLibrary.Album album = new AudioLibrary.Album();
        album.title = title; album.artist = artist; album.cover = cover; album.year = year; album.genre = "Soundtrack / Ambient";
        return album;
    }
    private static void addTrack(AudioLibrary.Album album, String source, String title, String artist, int trackNumber) {
        AudioLibrary.AlbumTrack track = new AudioLibrary.AlbumTrack();
        track.source = source; track.title = title == null ? "" : title; track.artist = artist == null ? "" : artist;
        track.composer = "C418"; track.trackNumber = trackNumber;
        album.tracks.add(track);
    }
    private static Map<String, Integer> trackNumbers(Object... values) {
        Map<String, Integer> result = new HashMap<>();
        for (int i = 0; i < values.length; i += 2) result.put((String) values[i], (Integer) values[i + 1]);
        return result;
    }
    private static void loadResourceAlbums(IResourceManager manager) {
        Gson gson = new Gson();
        for (String domain : manager.getResourceDomains()) try {
            for (IResource resource : manager.getAllResources(new ResourceLocation(domain, "music/albums.json"))) {
                try (IResource closeable = resource; Reader reader = new InputStreamReader(resource.getInputStream(), java.nio.charset.StandardCharsets.UTF_8)) {
                    JsonObject root = gson.fromJson(reader, JsonObject.class);
                    JsonObject entries = root.has("albums") ? root.getAsJsonObject("albums") : root;
                    for (Map.Entry<String, JsonElement> entry : entries.entrySet()) {
                        AudioLibrary.Album parsed = gson.fromJson(entry.getValue(), AudioLibrary.Album.class);
                        if (parsed == null) continue;
                        if (parsed.title == null || parsed.title.isEmpty()) parsed.title = entry.getKey();
                        if (parsed.artist == null) parsed.artist = "";
                        if (parsed.cover == null) parsed.cover = "";
                        if (parsed.tracks == null) parsed.tracks = new ArrayList<>();
                        for (AudioLibrary.AlbumTrack track : parsed.tracks) {
                            if (track.source != null && track.source.startsWith("res:") && !track.source.substring(4).contains(":"))
                                track.source = "res:" + domain + ":" + track.source.substring(4);
                        }
                        albums.put(domain + "/" + entry.getKey(), parsed);
                    }
                }
            }
        } catch (FileNotFoundException ignored) {
        } catch (Exception error) { LOG.warn("Cannot load music albums from {}", domain, error); }
    }
    static List<Sound> catalog(MusicTicker.MusicType type) {
        List<Sound> result = new ArrayList<>();
        expand(type.getMusicLocation().getSoundName().toString(), new HashSet<>(), result);
        return result;
    }
    private static void expand(String id, Set<String> seen, List<Sound> result) {
        if (!seen.add(id)) return;
        for (JsonElement entry : definitions.getOrDefault(id, Collections.emptyList())) {
            JsonObject obj = entry.isJsonObject() ? entry.getAsJsonObject() : null;
            String name = obj == null ? entry.getAsString() : obj.get("name").getAsString();
            if (!name.contains(":")) name = "minecraft:" + name;
            if (obj != null && obj.has("type") && obj.get("type").getAsString().equals("event")) expand(name, seen, result);
            else result.add(new Sound(name, obj != null && obj.has("volume") ? obj.get("volume").getAsFloat() : 1,
                    obj != null && obj.has("pitch") ? obj.get("pitch").getAsFloat() : 1,
                    obj != null && obj.has("weight") ? obj.get("weight").getAsInt() : 1, Sound.Type.FILE, true));
        }
        seen.remove(id);
    }
}
