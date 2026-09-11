package neofontrender.addons.audio;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.*;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import java.util.*;
import java.util.function.UnaryOperator;

/** Client-thread API for vanilla scheduling. Does not control independent players. */
public final class VanillaMusic {
    public interface Ticker {
        ISound uie$current();
        void uie$current(ISound sound);
        void uie$delay(int delay);
    }
    private Ticker ticker;
    private boolean enabled = true, paused;
    private Sound selectedTrack;
    private final Set<Object> focus = Collections.newSetFromMap(new IdentityHashMap<>());
    private final Map<MusicTicker.MusicType, List<Sound>> overrides = new EnumMap<>(MusicTicker.MusicType.class);
    private final Map<MusicTicker.MusicType, List<UnaryOperator<List<Sound>>>> filters = new EnumMap<>(MusicTicker.MusicType.class);
    private final Map<MusicTicker.MusicType, Integer> cursors = new EnumMap<>(MusicTicker.MusicType.class);
    private boolean ordered;
    private final Random random = new Random();
    public boolean enabled() { return enabled; }
    public void enabled(boolean value) { enabled = value; }
    public boolean paused() { return paused; }
    public void pause(boolean value) { paused = value; }
    public boolean ordered() { return ordered; }
    public void ordered(boolean value) { ordered = value; cursors.clear(); }
    public ISound current() { return ticker == null ? null : ticker.uie$current(); }
    public String state() {
        if (current() == null) return "STOPPED";
        return paused || !enabled || !focus.isEmpty() ? "PAUSED" : "PLAYING";
    }
    /** Concrete file selected by this controller; null for an unmodified vanilla-random choice. */
    public Sound selectedTrack() { return current() == null ? null : selectedTrack; }
    public AutoCloseable focus() {
        Object token = new Object(); focus.add(token);
        return () -> focus.remove(token);
    }
    public void replace(MusicTicker.MusicType scene, List<Sound> tracks) { overrides.put(scene, new ArrayList<>(tracks)); cursors.remove(scene); }
    public void restore(MusicTicker.MusicType scene) { overrides.remove(scene); cursors.remove(scene); }
    public AutoCloseable filter(MusicTicker.MusicType scene, UnaryOperator<List<Sound>> filter) {
        filters.computeIfAbsent(scene, k -> new ArrayList<>()).add(filter);
        return () -> filters.get(scene).remove(filter);
    }
    public List<Sound> tracks(MusicTicker.MusicType scene) {
        List<Sound> result = new ArrayList<>(overrides.getOrDefault(scene, AudioModule.catalog(scene)));
        for (UnaryOperator<List<Sound>> filter : filters.getOrDefault(scene, Collections.emptyList()))
            result = new ArrayList<>(Objects.requireNonNull(filter.apply(Collections.unmodifiableList(result))));
        return Collections.unmodifiableList(result);
    }
    public void next() { stop(); if (ticker != null) ticker.uie$delay(0); }
    public void stop() {
        if (current() != null) Minecraft.getMinecraft().getSoundHandler().stopSound(current());
        if (ticker != null) { ticker.uie$current(null); ticker.uie$delay(100); }
        selectedTrack = null;
    }
    public float volume() { return Minecraft.getMinecraft().gameSettings.getSoundLevel(SoundCategory.MUSIC); }
    public void volume(float value) {
        Minecraft.getMinecraft().gameSettings.setSoundLevel(SoundCategory.MUSIC, Math.max(0, Math.min(1, value)));
        Minecraft.getMinecraft().gameSettings.saveOptions();
    }
    public boolean beforeTick(Ticker value) {
        ticker = value;
        if (current() == null) selectedTrack = null;
        boolean blocked = !enabled || paused || !focus.isEmpty();
        if (current() != null) {
            AudioModule.LOG.debug("Vanilla BGM tick: sound={}, blocked={}, paused={}, enabled={}, focus={}", current().getSoundLocation(), blocked, paused, enabled, focus.size());
            GameAudioBackend.pause(current(), blocked);
        }
        return blocked;
    }
    public boolean play(Ticker value, MusicTicker.MusicType scene) {
        ticker = value;
        if (!enabled || paused || !focus.isEmpty()) return true;
        if (!ordered && !overrides.containsKey(scene) && !filters.containsKey(scene)) return false;
        List<Sound> pool = tracks(scene);
        if (pool.isEmpty()) { value.uie$delay(100); return true; }
        Sound sound;
        if (ordered) {
            int cursor = cursors.getOrDefault(scene, 0);
            sound = pool.get(Math.floorMod(cursor, pool.size())); cursors.put(scene, cursor + 1);
        } else {
            int total = pool.stream().mapToInt(s -> Math.max(1, s.getWeight())).sum();
            int choice = random.nextInt(total); sound = pool.get(0);
            for (Sound candidate : pool) { choice -= Math.max(1, candidate.getWeight()); if (choice < 0) { sound = candidate; break; } }
        }
        playSound(value, scene, sound);
        return true;
    }
    public void select(MusicTicker.MusicType scene, int index) {
        if (ticker == null) return;
        stop(); playSound(ticker, scene, tracks(scene).get(index));
    }
    private void playSound(Ticker value, MusicTicker.MusicType scene, Sound selected) {
        ResourceLocation event = scene.getMusicLocation().getSoundName();
        PositionedSoundRecord record = new PositionedSoundRecord(event, SoundCategory.MUSIC, 1, 1, false, 0,
                ISound.AttenuationType.NONE, 0, 0, 0) {
            @Override public SoundEventAccessor createAccessor(SoundHandler handler) {
                this.sound = selected;
                SoundEventAccessor accessor = new SoundEventAccessor(event, null);
                accessor.addSound(selected); return accessor;
            }
        };
        selectedTrack = selected;
        AudioModule.LOG.info("Vanilla BGM play: scene={}, sound={}, event={}, weight={}", scene, selected.getSoundLocation(), event, selected.getWeight());
        value.uie$current(record); value.uie$delay(Integer.MAX_VALUE);
        Minecraft.getMinecraft().getSoundHandler().playSound(record);
    }
}
