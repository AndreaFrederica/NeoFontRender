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
    static SoundSystem system() {
        try { return (SoundSystem) SYSTEM.get(MANAGER.get(Minecraft.getMinecraft().getSoundHandler())); }
        catch (ReflectiveOperationException e) { throw new IllegalStateException(e); }
    }
    private static final java.util.Set<String> PAUSED = new java.util.HashSet<>();
    static void pause(net.minecraft.client.audio.ISound sound, boolean value) {
        try {
            Object manager = MANAGER.get(Minecraft.getMinecraft().getSoundHandler());
            String channel = ((neofontrender.addons.mixin.AccessorAudioChannels) manager).uie$channels().get(sound);
            SoundSystem system = (SoundSystem) SYSTEM.get(manager);
            if (channel == null || system == null) return;
            if (value) { if (PAUSED.add(channel)) system.pause(channel); }
            else if (PAUSED.remove(channel)) system.play(channel);
        } catch (ReflectiveOperationException e) { throw new IllegalStateException(e); }
    }
    private GameAudioBackend() {}
}
