package neofontrender.addons.audio;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.ISound;
import net.minecraft.client.audio.MusicTicker;
import net.minecraft.client.audio.SoundCategory;
import neofontrender.addons.build.UiBuildFeatures;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

/**
 * Client-thread control over the vanilla background-music schedule. Does not control independent players.
 *
 * <p>1.7.10's {@code MusicTicker} selects and starts its track inside {@code update()} and exposes no
 * play/stop API, so the schedule is driven through {@link Ticker}: a mixin on {@code MusicTicker} maps
 * it onto the ticker's current-sound and delay fields.
 *
 * <p>Per-track overrides (choosing one file out of an event's sound pool) are not reachable here:
 * 1.7.10 resolves playing sounds by event name through a registry that has no public write path, so
 * this controller only starts, stops, pauses and mutes the vanilla schedule, and custom scene music is
 * played by {@link MusicPlayer} instead.
 */
public final class VanillaMusic {
    /** Bridge onto {@code MusicTicker}'s private schedule state, supplied by the ticker mixin. */
    public interface Ticker {
        ISound uie$current();
        void uie$current(ISound sound);
        void uie$delay(int delay);
    }
    private Ticker ticker;
    private boolean enabled = true, paused;
    private final Set<Object> focus = Collections.newSetFromMap(new IdentityHashMap<>());
    public boolean enabled() { return enabled; }
    public void enabled(boolean value) { enabled = value; }
    public boolean paused() { return paused; }
    public void pause(boolean value) { paused = value; }
    public ISound current() { return ticker == null ? null : ticker.uie$current(); }
    public String state() {
        if (current() == null) return "STOPPED";
        return paused || !enabled || !focus.isEmpty() ? "PAUSED" : "PLAYING";
    }
    public AutoCloseable focus() {
        Object token = new Object(); focus.add(token);
        return () -> focus.remove(token);
    }
    /** Stops the current track and lets the vanilla schedule pick the next one immediately. */
    public void next() { stop(); if (ticker != null) ticker.uie$delay(0); }
    /** Stops the current track and holds the vanilla schedule back. */
    public void stop() {
        if (current() != null) Minecraft.getMinecraft().getSoundHandler().stopSound(current());
        if (ticker != null) { ticker.uie$current(null); ticker.uie$delay(100); }
    }
    public float volume() { return Minecraft.getMinecraft().gameSettings.getSoundLevel(SoundCategory.MUSIC); }
    public void volume(float value) {
        Minecraft.getMinecraft().gameSettings.setSoundLevel(SoundCategory.MUSIC, Math.max(0, Math.min(1, value)));
        Minecraft.getMinecraft().gameSettings.saveOptions();
    }
    /** Cancels the vanilla ticker while vanilla BGM is disabled, paused or focused away; true when it must not run. */
    public boolean beforeTick(Ticker value) {
        ticker = value;
        boolean blocked = owns();
        if (current() != null) {
            if (UiBuildFeatures.DIAGNOSTIC_LOGS) {
                AudioModule.LOG.debug("Vanilla BGM tick: sound={}, blocked={}, paused={}, enabled={}, focus={}",
                        current().getPositionedSoundLocation(), blocked, paused, enabled, focus.size());
            }
            GameAudioBackend.pause(current(), blocked);
        }
        return blocked;
    }
    /**
     * Called where the vanilla schedule would start a track; true cancels it.
     *
     * <p>1.7.10 has no separate "play this track" entry point, so an unblocked schedule always keeps
     * its own random pick and {@code scene} only documents which schedule asked.
     */
    public boolean play(Ticker value, MusicTicker.MusicType scene) {
        ticker = value;
        return owns();
    }
    private boolean owns() { return !enabled || paused || !focus.isEmpty(); }
}
