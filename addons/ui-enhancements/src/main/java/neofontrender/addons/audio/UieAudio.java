package neofontrender.addons.audio;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.SoundHandler;
import net.minecraft.util.ResourceLocation;
import java.util.ArrayList;
import java.util.List;

/** Client-thread entry points. Registrations/players are owned handles, never server services. */
public final class UieAudio {
    private static final VanillaMusic VANILLA = new VanillaMusic();
    private static final List<MusicPlayer> PLAYERS = new ArrayList<>();
    private static MusicPlayer music;
    public static VanillaMusic vanillaMusic() { return VANILLA; }
    public static MusicPlayer createPlayer() { MusicPlayer player = new MusicPlayer(); PLAYERS.add(player); return player; }
    public static MusicPlayer independent() { if (music == null || music.state() == MusicPlayer.State.CLOSED) music = createPlayer(); return music; }
    /** Immutable snapshot of built-in, local and enabled resource-pack albums. */
    public static List<MusicAlbum> albums() { return AudioModule.albumCatalog(); }
    public static void playAlbum(String albumId, int trackIndex) { AudioModule.playAlbum(albumId, trackIndex); }
    static void tick() {
        PLAYERS.removeIf(p -> p.state() == MusicPlayer.State.CLOSED);
        for (MusicPlayer player : new ArrayList<>(PLAYERS)) player.tick();
    }
    static void closeAll() {
        for (MusicPlayer player : new ArrayList<>(PLAYERS)) {
            try { player.close(); } catch (Exception e) { AudioModule.LOG.warn("Player close failed", e); }
        }
        PLAYERS.clear();
    }
    /**
     * Plays one registered sound event.
     *
     * <p>1.7.10 has no per-call sound category: an event's category is fixed by its
     * {@code sounds.json} entry, so callers only choose volume, pitch, looping and position.
     */
    public static AutoCloseable playSound(ResourceLocation event, float volume, float pitch,
            boolean loop, float x, float y, float z, boolean positional) {
        SoundHandler handler = Minecraft.getMinecraft().getSoundHandler();
        UiePositionedSound sound = new UiePositionedSound(event, volume, pitch, loop, x, y, z, positional);
        handler.playSound(sound);
        return () -> handler.stopSound(sound);
    }
    private UieAudio() {}
}
