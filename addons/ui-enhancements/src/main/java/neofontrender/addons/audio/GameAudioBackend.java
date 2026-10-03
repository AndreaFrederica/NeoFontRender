package neofontrender.addons.audio;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.SoundHandler;
import net.minecraft.client.audio.SoundManager;
import paulscode.sound.SoundSystem;
import java.lang.reflect.Field;

/** Resolve private implementation fields by type once, independent of MCP/SRG field names. */
final class GameAudioBackend {
    private static final Field MANAGER = field(SoundHandler.class, SoundManager.class);
    private static final Field SYSTEM = field(SoundManager.class, SoundSystem.class);
    private static Field field(Class<?> owner, Class<?> type) {
        for (Field f : owner.getDeclaredFields()) if (type.isAssignableFrom(f.getType())) {
            f.setAccessible(true); return f;
        }
        throw new IllegalStateException("Unsupported audio backend: " + owner.getName());
    }

    private static SoundSystem rawSystem() {
        try { return (SoundSystem) SYSTEM.get(MANAGER.get(Minecraft.getMinecraft().getSoundHandler())); }
        catch (ReflectiveOperationException e) { throw new IllegalStateException(e); }
    }

    static SoundSystem system() {
        return rawSystem();
    }

    /**
     * The live sound system, or {@code null} while it is still starting up.
     *
     * <p>{@code SoundManager} publishes {@code sndSystem} from its "Sound Library Loader" thread and
     * only then lets the command thread build the real library, so there is a window where
     * {@code SoundSystemStarterThread.soundLibrary} is non-null but its {@code sources} map is not
     * populated yet. Its own {@code playing()} override only guards against a null library, so any
     * call we make in that window throws a NullPointerException from {@code getSources().get(...)}
     * and kills the game. Vanilla avoids it by checking {@code SoundManager.loaded} first; this
     * mirrors that check instead of reaching into the raw field.
     */
    static SoundSystem readySystem() {
        SoundSystem system = rawSystem();
        if (system == null) return null;
        try {
            Object library = LIBRARY.get(system);
            if (library == null) return null;
            return SOURCES.get(library) == null ? null : system;
        } catch (IllegalAccessException | RuntimeException | LinkageError probeFailed) {
            // Field layout is unexpected; keep working rather than disabling audio entirely.
            return system;
        }
    }

    /**
     * Runs a sound-system call, treating a mid-startup system as "not ready" instead of fatal.
     * {@code MusicPlayer} already retries on the next tick, so dropping one call is enough.
     */
    static <T> T guard(java.util.function.Supplier<T> call, T fallback) {
        try {
            return call.get();
        } catch (RuntimeException | LinkageError notReady) {
            return fallback;
        }
    }

    static void guard(Runnable call) {
        try {
            call.run();
        } catch (RuntimeException | LinkageError notReady) {
            // mid-startup system; dropped, the caller retries on a later tick
        }
    }

    private static final Field LIBRARY = optionalField(SoundSystem.class, "soundLibrary");
    private static final Field SOURCES = optionalField(paulscode.sound.Library.class, "sources");

    private static Field optionalField(Class<?> owner, String name) {
        for (Class<?> type = owner; type != null; type = type.getSuperclass()) {
            try {
                Field f = type.getDeclaredField(name);
                f.setAccessible(true);
                return f;
            } catch (NoSuchFieldException ignored) {
                // walk up
            } catch (RuntimeException ignored) {
                return null;
            }
        }
        return null;
    }

    private static final java.util.Set<String> PAUSED = new java.util.HashSet<>();
    static void pause(net.minecraft.client.audio.ISound sound, boolean value) {
        try {
            Object manager = MANAGER.get(Minecraft.getMinecraft().getSoundHandler());
            String channel = ((neofontrender.addons.mixin.AccessorAudioChannels) manager).uie$channels().get(sound);
            SoundSystem system = readySystem();
            if (channel == null || system == null) return;
            if (value) { if (PAUSED.add(channel)) guard(() -> system.pause(channel)); }
            else if (PAUSED.remove(channel)) guard(() -> system.play(channel));
        } catch (ReflectiveOperationException e) { throw new IllegalStateException(e); }
    }
    private GameAudioBackend() {}
}
