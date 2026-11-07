package neofontrender.addons.audio;

import com.google.gson.*;
import net.minecraft.client.Minecraft;
import java.nio.file.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

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
    public AudioLibrary(Path root) { this.root = root; try { Files.createDirectories(musicRoot()); } catch (IOException e) { throw new IllegalStateException("Cannot create music directory " + musicRoot(), e); } }
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
    /** User-owned music root. Subdirectories are treated as album folders. */
    public Path musicRoot() {
        return root.getParent().getParent().resolve("music");
    }
    /** Imports one file or a complete album folder while preserving its folder layout. */
    public CompletableFuture<List<Path>> importPath(Path source) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (Files.isRegularFile(source)) return Collections.singletonList(importFileSync(source, musicRoot()));
                if (!Files.isDirectory(source)) throw new IOException("Not a file or directory: " + source);
                Path albumRoot = musicRoot().resolve(source.getFileName().toString());
                List<Path> imported = new ArrayList<>();
                try (java.util.stream.Stream<Path> walk = Files.walk(source)) {
                    walk.filter(Files::isRegularFile).filter(AudioLibrary::isAudioFile).forEach(file -> {
                        try {
                            Path target = albumRoot.resolve(source.relativize(file));
                            Files.createDirectories(target.getParent());
                            Files.copy(file, target, StandardCopyOption.REPLACE_EXISTING);
                            imported.add(target);
                        } catch (IOException error) { throw new CompletionException(error); }
                    });
                }
                if (imported.isEmpty()) throw new IOException("No supported audio files in " + source);
                return imported;
            } catch (IOException error) { throw new CompletionException(error); }
        }, io);
    }

    /** Builds metadata albums from imported audio files. Tag values are authoritative. */
    public CompletableFuture<Integer> indexMetadataAlbums() {
        return CompletableFuture.supplyAsync(() -> {
            // Expose the physical tree as directory entries; these remain
            // playlists and are deliberately separate from metadata albums.
            Map<String, List<String>> directoryTracks = new LinkedHashMap<>();
            try (java.util.stream.Stream<Path> files = Files.walk(musicRoot())) {
                files.filter(Files::isRegularFile).filter(AudioLibrary::isAudioFile).forEach(file -> {
                    Path relative = musicRoot().relativize(file);
                    directoryTracks.computeIfAbsent("music", k -> new ArrayList<>()).add(file.toString());
                    Path parent = relative.getParent();
                    while (parent != null) {
                        directoryTracks.computeIfAbsent(parent.toString().replace('\\','/'), k -> new ArrayList<>()).add(file.toString());
                        parent = parent.getParent();
                    }
                });
            } catch (IOException e) { AudioModule.LOG.warn("Cannot index music files", e); }
            try (java.util.stream.Stream<Path> dirs = Files.walk(musicRoot())) {
                dirs.filter(Files::isDirectory).forEach(dir -> {
                    String name = dir.equals(musicRoot()) ? "music" : musicRoot().relativize(dir).toString().replace('\\','/');
                    playlists.put(name, new ArrayList<>(directoryTracks.getOrDefault(name, Collections.emptyList())));
                });
            } catch (IOException e) { AudioModule.LOG.warn("Cannot index music directories", e); }
            Map<String, Album> found = new LinkedHashMap<>();
            try (java.util.stream.Stream<Path> walk = Files.walk(musicRoot())) {
                walk.filter(Files::isRegularFile).filter(AudioLibrary::isAudioFile).forEach(file -> {
                    try {
                        com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo info = readInfo(file);
                        TagMetadata tags = readTags(file);
                        if (info == null && tags == null) return;
                        String albumName = tags == null ? "" : tags.album;
                        String artist = tags == null ? (info == null ? "" : info.author) : tags.artist;
                        // A directory is not an album. Files without an ALBUM tag
                        // remain available through DIR entries and are not placed
                        // into misleading one-track albums.
                        if (albumName == null || albumName.trim().isEmpty()) return;
                        final String albumTitle = albumName.trim(), albumArtist = artist == null ? "" : artist.trim();
                        String key = albumTitle + "\u0000" + albumArtist;
                        Album album = found.computeIfAbsent(key, k -> { Album a = new Album(); a.title = albumTitle; a.artist = albumArtist; return a; });
                        AlbumTrack track = new AlbumTrack(); track.source = file.toString();
                        track.title = tags != null && !tags.title.isEmpty() ? tags.title : (info == null || info.title == null || info.title.isEmpty() ? stripExtension(file.getFileName().toString()) : info.title);
                        track.artist = artist == null ? "" : artist.trim(); track.trackNumber = tags == null ? 0 : tags.track;
                        if (tags != null) { album.genre = tags.genre; album.year = tags.year; }
                        album.tracks.add(track);
                    } catch (Exception ignored) { }
                });
            } catch (IOException ignored) { }
            for (Album album : found.values()) album.tracks.sort(Comparator.comparingInt(t -> t.trackNumber == 0 ? Integer.MAX_VALUE : t.trackNumber));
            albums.clear(); albums.putAll(found); save(); return found.size();
        }, io);
    }
    private static com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo readInfo(Path file) throws InterruptedException {
        com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager manager = new com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager();
        manager.registerSourceManager(new com.sedmelluq.discord.lavaplayer.source.local.LocalAudioSourceManager());
        CountDownLatch done = new CountDownLatch(1);
        final com.sedmelluq.discord.lavaplayer.track.AudioTrack[] loaded = new com.sedmelluq.discord.lavaplayer.track.AudioTrack[1];
        manager.loadItem(file.toString(), new com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler() {
            public void trackLoaded(com.sedmelluq.discord.lavaplayer.track.AudioTrack t) { loaded[0] = t; done.countDown(); }
            public void playlistLoaded(com.sedmelluq.discord.lavaplayer.track.AudioPlaylist p) { done.countDown(); }
            public void noMatches() { done.countDown(); }
            public void loadFailed(com.sedmelluq.discord.lavaplayer.tools.FriendlyException e) { done.countDown(); }
        });
        done.await(20, TimeUnit.SECONDS);
        com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo result = loaded[0] == null ? null : loaded[0].getInfo();
        manager.shutdown();
        return result;
    }
    private static String stripExtension(String value) { int dot = value.lastIndexOf('.'); return dot > 0 ? value.substring(0, dot) : value; }
    private static final class TagMetadata {
        String album = "", artist = "", title = "", genre = ""; int track, year;
    }
    private static TagMetadata readTags(Path file) {
        try {
            org.jaudiotagger.audio.AudioFile audio = org.jaudiotagger.audio.AudioFileIO.read(file.toFile());
            org.jaudiotagger.tag.Tag tag = audio.getTag();
            if (tag == null) return null;
            TagMetadata result = new TagMetadata();
            result.album = value(tag, org.jaudiotagger.tag.FieldKey.ALBUM);
            result.artist = value(tag, org.jaudiotagger.tag.FieldKey.ARTIST);
            result.title = value(tag, org.jaudiotagger.tag.FieldKey.TITLE);
            result.genre = value(tag, org.jaudiotagger.tag.FieldKey.GENRE);
            try { result.track = Integer.parseInt(value(tag, org.jaudiotagger.tag.FieldKey.TRACK).replaceAll("/.*", "")); } catch (Exception ignored) { }
            try { result.year = Integer.parseInt(value(tag, org.jaudiotagger.tag.FieldKey.YEAR)); } catch (Exception ignored) { }
            return result;
        } catch (Exception ignored) { return null; }
    }
    private static String value(org.jaudiotagger.tag.Tag tag, org.jaudiotagger.tag.FieldKey key) {
        String value = tag.getFirst(key); return value == null ? "" : value.trim();
    }
    private static String metadataField(Object info, String name) {
        try { java.lang.reflect.Field field = info.getClass().getField(name); Object value = field.get(info); return value == null ? "" : value.toString(); }
        catch (Exception ignored) { return ""; }
    }
    private Path importFileSync(Path source, Path destinationRoot) throws IOException {
        Path directory = destinationRoot.resolve("Singles");
        Path target = directory.resolve(source.getFileName().toString());
        String name = source.getFileName().toString();
        String base = name, extension = "";
        int dot = name.lastIndexOf('.');
        if (dot > 0) { base = name.substring(0, dot); extension = name.substring(dot); }
        int suffix = 2;
        while (Files.exists(target)) target = directory.resolve(base + " (" + suffix++ + ")" + extension);
        Files.createDirectories(target.getParent());
        Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
        return target;
    }
    private static boolean isAudioFile(Path path) {
        String name = path.getFileName().toString().toLowerCase(Locale.ROOT);
        return name.endsWith(".mp3") || name.endsWith(".ogg") || name.endsWith(".flac") || name.endsWith(".wav");
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
