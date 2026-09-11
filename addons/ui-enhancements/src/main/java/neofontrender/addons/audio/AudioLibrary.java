package neofontrender.addons.audio;

import com.google.gson.*;
import net.minecraft.client.Minecraft;
import java.nio.file.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;

/** Local persistent playlists. Import IO runs outside the client thread. */
public final class AudioLibrary {
    /** Album metadata is deliberately separate from the playable playlist entries. */
    public static final class AlbumTrack {
        public String source = "";
        public String title = "";
        public String artist = "";
        public String composer = "";
        public int trackNumber;
    }
    public static final class Album {
        public String title = "";
        public String artist = "";
        public String cover = "";
        public String genre = "";
        public int year;
        public List<AlbumTrack> tracks = new ArrayList<>();
    }
    public final Map<String, List<String>> playlists = new LinkedHashMap<>();
    public final Map<String, Album> albums = new LinkedHashMap<>();
    public final Map<String, String> scenes = new LinkedHashMap<>();
    private final Path root;
    private final ExecutorService io = Executors.newSingleThreadExecutor(r -> { Thread t = new Thread(r, "UIE music import"); t.setDaemon(true); return t; });
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    /** Persisted after built-in playlists have been created successfully. */
    public boolean defaultsSeeded;
    public AudioLibrary(Path root) { this.root = root; }
    public void load() {
        Path file = root.resolve("library.json");
        if (Files.isRegularFile(file)) try (Reader reader = Files.newBufferedReader(file)) {
            JsonObject data = gson.fromJson(reader, JsonObject.class);
            defaultsSeeded = data.has("defaultsSeeded") && data.get("defaultsSeeded").getAsBoolean();
            if (data.has("playlists")) for (Map.Entry<String, JsonElement> entry : data.getAsJsonObject("playlists").entrySet()) {
                List<String> tracks = new ArrayList<>();
                for (JsonElement track : entry.getValue().getAsJsonArray()) tracks.add(track.getAsString());
                playlists.put(entry.getKey(), tracks);
            }
            if (data.has("scenes")) for (Map.Entry<String, JsonElement> entry : data.getAsJsonObject("scenes").entrySet()) scenes.put(entry.getKey(), entry.getValue().getAsString());
            if (data.has("albums")) for (Map.Entry<String, JsonElement> entry : data.getAsJsonObject("albums").entrySet()) {
                Album album = gson.fromJson(entry.getValue(), Album.class);
                if (album != null) {
                    if (album.title == null || album.title.isEmpty()) album.title = entry.getKey();
                    if (album.artist == null) album.artist = "";
                    if (album.cover == null) album.cover = "";
                    if (album.genre == null) album.genre = "";
                    if (album.tracks == null) album.tracks = new ArrayList<>();
                    albums.put(entry.getKey(), album);
                }
            }
        } catch (Exception error) { AudioModule.LOG.warn("Cannot read music library", error); }
        playlists.computeIfAbsent("Library", k -> new ArrayList<>());
    }
    public void save() {
        JsonObject data = new JsonObject();
        data.add("playlists", gson.toJsonTree(playlists));
        data.add("scenes", gson.toJsonTree(scenes));
        data.add("albums", gson.toJsonTree(albums));
        data.addProperty("defaultsSeeded", defaultsSeeded);
        String json = gson.toJson(data);
        io.execute(() -> {
            try {
                Files.createDirectories(root);
                Path tmp = Files.createTempFile(root, "library-", ".tmp");
                Files.writeString(tmp, json);
                try { Files.move(tmp, root.resolve("library.json"), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE); }
                catch (AtomicMoveNotSupportedException e) { Files.move(tmp, root.resolve("library.json"), StandardCopyOption.REPLACE_EXISTING); }
            } catch (IOException error) { AudioModule.LOG.error("Cannot save music library", error); }
        });
    }
    public CompletableFuture<Path> importFile(Path source) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Files.createDirectories(root.resolve("tracks"));
                String name = source.getFileName().toString();
                Path target = root.resolve("tracks").resolve(UUID.randomUUID() + "-" + name);
                Files.copy(source, target); return target;
            } catch (IOException error) { throw new CompletionException(error); }
        }, io);
    }
    /** Stable cache for resource-backed entries: same resource always maps to the same file. */
    public Path cachedResource(net.minecraft.util.ResourceLocation location) {
        return root.resolve("cache").resolve(location.getNamespace() + "_" + location.getPath().replace('/', '_'));
    }
    /** Acquire the resource on the client thread, then copy it on the library IO thread. */
    public CompletableFuture<Path> materializeAsync(net.minecraft.util.ResourceLocation location) {
        Path target = cachedResource(location);
        if (Files.isRegularFile(target)) return CompletableFuture.completedFuture(target);
        final net.minecraft.client.resources.IResource resource;
        final InputStream input;
        try {
            resource = Minecraft.getMinecraft().getResourceManager().getResource(location);
            input = resource.getInputStream();
        } catch (IOException error) {
            CompletableFuture<Path> failed = new CompletableFuture<>();
            failed.completeExceptionally(error);
            return failed;
        }
        return CompletableFuture.supplyAsync(() -> {
            try (net.minecraft.client.resources.IResource closeable = resource; InputStream stream = input) {
                if (Files.isRegularFile(target)) return target;
                Files.createDirectories(target.getParent());
                Path temporary = Files.createTempFile(target.getParent(), "resource-", ".tmp");
                try {
                    Files.copy(stream, temporary, StandardCopyOption.REPLACE_EXISTING);
                    try { Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE); }
                    catch (AtomicMoveNotSupportedException ignored) { Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING); }
                    return target;
                } finally { Files.deleteIfExists(temporary); }
            } catch (IOException error) { throw new CompletionException(error); }
        }, io);
    }
    public void close() { io.shutdown(); }
}
