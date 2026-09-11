package neofontrender.addons.audio;

import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.widgets.textfield.TextFieldWidget;
import neofontrender.api.client.settings.*;
import neofontrender.addons.tooltips.AddonI18n;
import neofontrender.client.gui.component.base.*;
import neofontrender.client.gui.views.NfrContentView;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.MusicTicker;
import net.minecraft.client.audio.Sound;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

/** Music pages use the same NFR controls/layout as other ModularUI settings pages. */
final class AudioSettingsPage implements NfrSettingsPage {
    private final boolean vanilla;
    AudioSettingsPage(boolean vanilla) { this.vanilla = vanilla; }
    public String id() { return "neofontrender_ui_enhancements:" + (vanilla ? "vanilla_music" : "music"); }
    public String titleKey() { return "neofontrender_ui_enhancements.audio." + (vanilla ? "vanilla" : "independent"); }
    public String title() { return AddonI18n.tr(titleKey()); }
    public int order() { return vanilla ? 1191 : 1190; }
    public NfrSettingsPageSession createSession() { return new Session(); }
    static String tr(String key) { return AddonI18n.tr("neofontrender_ui_enhancements.audio." + key); }
    private final class Session implements NfrSettingsPageSession {
        private String path = "", name = "", search = "", selected = "0";
        private String scene = "GAME";
        private NfrMusicList musicList;
        public IWidget createView(NfrSettingsPageContext context) {
            var controls = context.controls();
            NfrOptionsGrid grid = controls.grid();
            if (vanilla) {
                VanillaMusic music = UieAudio.vanillaMusic();
                grid.add(controls.toggleText(() -> tr("enabled"), () -> "", music::enabled, music::enabled));
                grid.add(controls.toggleText(() -> tr("ordered"), () -> "", music::ordered, music::ordered));
                grid.add(controls.toggleText(() -> tr("paused"), () -> "", music::paused, music::pause));
                grid.add(controls.action(() -> tr("next"), 260, 24, music::next));
                grid.add(controls.action(() -> tr("stop"), 260, 24, music::stop));
                grid.add(controls.decimalSlider(() -> tr("volume"), music::volume, music::volume, 0, 1, .01f));
                sceneControl(context, grid);
                var type = MusicTicker.MusicType.valueOf(scene);
                grid.add(new NfrMusicList(
                        () -> {
                            List<String> result = new ArrayList<>();
                            for (Sound sound : music.tracks(MusicTicker.MusicType.valueOf(scene)))
                                result.add(sound.getSoundLocation().toString());
                            return result;
                        },
                        value -> true,
                        () -> parseSelected(),
                        () -> {
                            Sound selectedTrack = music.selectedTrack();
                            if (selectedTrack == null) return -1;
                            String id = selectedTrack.getSoundLocation().toString();
                            List<Sound> tracks = music.tracks(MusicTicker.MusicType.valueOf(scene));
                            for (int i = 0; i < tracks.size(); i++)
                                if (id.equals(tracks.get(i).getSoundLocation().toString())) return i;
                            return -1;
                        },
                        index -> { selected = Integer.toString(index); music.select(type, index); }
                ));
                for (int direction : new int[]{-1, 1}) grid.add(controls.action(() -> tr(direction < 0 ? "up" : "down"), 260, 24, () -> {
                    var tracks = new ArrayList<>(music.tracks(type)); int index = Integer.parseInt(selected), target = index + direction;
                    if (index >= 0 && index < tracks.size() && target >= 0 && target < tracks.size()) {
                        tracks.add(target, tracks.remove(index)); music.replace(type, tracks); selected = Integer.toString(target); context.refresh();
                    }
                }));
                grid.add(field(tr("resource"), () -> path, v -> path = v));
                grid.add(controls.action(() -> tr("append"), 260, 24, () -> {
                    try {
                        var tracks = new ArrayList<>(music.tracks(type));
                        tracks.add(new net.minecraft.client.audio.Sound(new net.minecraft.util.ResourceLocation(path).toString(), 1, 1, 1,
                                net.minecraft.client.audio.Sound.Type.FILE, true));
                        music.replace(type, tracks); context.refresh();
                    } catch (Exception e) { AudioModule.status = e.toString(); }
                }));
                grid.add(controls.action(() -> tr("restore"), 260, 24, () -> { music.restore(type); context.refresh(); }));
            } else {
                MusicPlayer player = UieAudio.independent();
                grid.add(controls.dropdownText("music_lists", () -> tr("playlist"), () -> AudioModule.selectedPlaylist,
                        v -> { AudioModule.selectedPlaylist = v; selected = "0"; context.refresh(); },
                        AudioModule.playlistNames(), AudioModule::playlistDisplay).size(260, 24));
                grid.add(field(tr("new_list"), () -> name, v -> name = v));
                grid.add(controls.action(() -> tr("create"), 260, 24, () -> {
                    if (!name.trim().isEmpty()) { AudioModule.library.playlists.putIfAbsent(name.trim(), new ArrayList<>()); AudioModule.selectedPlaylist = name.trim(); AudioModule.library.save(); context.refresh(); }
                }));
                grid.add(field(tr("path"), () -> path, v -> path = v));
                grid.add(controls.action(() -> tr("browse"), 260, 24, () -> browseForFile(context)));
                grid.add(controls.action(() -> tr("import"), 260, 24, () -> {
                    final String list = AudioModule.selectedPlaylist;
                    try {
                        if (AudioModule.readOnlyPlaylist(list)) { AudioModule.status = tr("album_read_only"); return; }
                        AudioModule.status = tr("importing");
                        Path selectedPath = Paths.get(path);
                        AudioModule.library.importPath(selectedPath).whenComplete((files, error) -> Minecraft.getMinecraft().addScheduledTask(() -> {
                            if (error != null) AudioModule.status = error.toString();
                            else {
                                List<String> target = AudioModule.library.playlists.computeIfAbsent(list, k -> new ArrayList<>());
                                if (Files.isDirectory(selectedPath)) {
                                    AudioModule.library.indexMetadataAlbums().whenComplete((count, scanError) -> Minecraft.getMinecraft().addScheduledTask(() -> {
                                        if (scanError != null) AudioModule.status = scanError.toString();
                                        else { AudioModule.refreshLocalAlbums(); AudioModule.status = tr("imported"); context.refresh(); }
                                    }));
                                } else {
                                    for (Path file : files) target.add(file.toString());
                                }
                                AudioModule.library.save();
                                AudioModule.status = tr("imported");
                                context.refresh();
                            }
                        }));
                    } catch (Exception e) { AudioModule.status = e.toString(); }
                }));
                grid.add(field(tr("search"), () -> search, v -> { search = v; if (musicList != null) musicList.refresh(); }));
                grid.add(controls.action(() -> tr("refresh"), 260, 24, context::refresh));
                List<String> tracks = AudioModule.playlistEntries(AudioModule.selectedPlaylist);
                musicList = new NfrMusicList(
                        () -> tracks,
                        value -> value.toLowerCase(Locale.ROOT).contains(search.toLowerCase(Locale.ROOT)),
                        () -> parseSelected(),
                        () -> AudioModule.playingEntryIndex(tracks),
                        index -> {
                            selected = Integer.toString(index);
                            if (index < tracks.size()) { AudioModule.sceneMode = false; AudioModule.play(tracks, index); }
                        }
                );
                grid.add(musicList);
                grid.add(controls.action(() -> tr("play"), 260, 24, () -> {
                    if (player.state() == MusicPlayer.State.PAUSED) player.pause(false);
                    else if (player.state() == MusicPlayer.State.STOPPED || player.state() == MusicPlayer.State.FINISHED) {
                        int index = parseSelected();
                        if (index >= 0 && index < tracks.size()) { AudioModule.sceneMode = false; AudioModule.play(tracks, index); }
                    }
                }));
                grid.add(controls.toggleText(() -> tr("paused"), () -> "", () -> player.state() == MusicPlayer.State.PAUSED, player::pause));
                grid.add(controls.action(() -> tr("stop"), 260, 24, () -> { AudioModule.sceneMode = false; player.stop(); }));
                grid.add(controls.action(() -> tr("previous"), 260, 24, player::previous));
                grid.add(controls.action(() -> tr("next"), 260, 24, player::next));
                grid.add(controls.dropdownText("music_mode", () -> tr("mode"), () -> player.playlist().mode().name(),
                        v -> player.playlist().mode(neofontrender.audio.Playlist.Mode.valueOf(v)),
                        Arrays.stream(neofontrender.audio.Playlist.Mode.values()).map(Enum::name).collect(Collectors.toList()), v -> tr(v)).size(260, 24));
                grid.add(controls.decimalSlider(() -> tr("volume"), player::volume, player::volume, 0, 1, .01f));
                grid.add(controls.decimalSlider(() -> tr("position") + " " + player.positionMillis()/1000 + "/" + player.durationMillis()/1000 + "s",
                        () -> player.durationMillis() == 0 ? 0 : Math.min(1f, (float) player.positionMillis()/player.durationMillis()),
                        v -> { if (player.durationMillis() > 0) player.seek((long) (v * player.durationMillis())); }, 0, 1, .01f));
                for (int direction : new int[]{-1, 1}) grid.add(controls.action(() -> tr(direction < 0 ? "up" : "down"), 260, 24, () -> {
                    if (AudioModule.readOnlyPlaylist(AudioModule.selectedPlaylist)) { AudioModule.status = tr("album_read_only"); return; }
                    int index = Integer.parseInt(selected), target = index + direction;
                    if (index < tracks.size() && target >= 0 && target < tracks.size()) { tracks.add(target, tracks.remove(index)); selected = Integer.toString(target); AudioModule.library.save(); context.refresh(); }
                }));
                grid.add(controls.action(() -> tr("remove"), 260, 24, () -> {
                    if (AudioModule.readOnlyPlaylist(AudioModule.selectedPlaylist)) { AudioModule.status = tr("album_read_only"); return; }
                    int index = Integer.parseInt(selected); if (index < tracks.size()) { tracks.remove(index); selected = "0"; AudioModule.library.save(); context.refresh(); }
                }));
                grid.add(controls.toggleText(() -> tr("exclusive"), () -> "", () -> AudioModule.exclusive, v -> AudioModule.exclusive = v));
                grid.add(controls.toggleText(() -> tr("scene_mode"), () -> "", () -> AudioModule.sceneMode, v -> AudioModule.sceneMode = v));
                sceneControl(context, grid);
                grid.add(controls.dropdownText("scene_list", () -> tr("scene_list"), () -> AudioModule.library.scenes.getOrDefault(scene, ""),
                        v -> { AudioModule.library.scenes.put(scene, v); AudioModule.library.save(); },
                        AudioModule.playlistNames(), AudioModule::playlistDisplay).size(260, 24));
            }
            grid.add(controls.action(() -> AudioModule.status.isEmpty() ? tr("status") + ": " + UieAudio.independent().state() : AudioModule.status, 260, 24, () -> {}));
            grid.add(controls.action(() -> UieAudio.independent().error(), 260, 24, () -> {}));
            return new View(grid);
        }
        private int parseSelected() {
            try { return Integer.parseInt(selected); } catch (NumberFormatException ignored) { return -1; }
        }
        private void sceneControl(NfrSettingsPageContext context, NfrOptionsGrid grid) {
            grid.add(context.controls().dropdownText("music_scene", () -> tr("scene"), () -> scene,
                    v -> { scene = v; selected = "0"; context.refresh(); },
                    Arrays.stream(MusicTicker.MusicType.values()).map(Enum::name).collect(Collectors.toList()), v -> tr(v)).size(260, 24));
        }
        private void browseForFile(NfrSettingsPageContext context) {
            // AWT dialogs must not run on the client thread; macOS + LWJGL2 cannot mix AWT at all.
            if (System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("mac")) {
                AudioModule.status = tr("browse_unsupported");
                return;
            }
            new Thread(() -> {
                java.awt.FileDialog dialog = new java.awt.FileDialog((java.awt.Frame) null, tr("browse"));
                try {
                    dialog.setFilenameFilter((dir, file) -> {
                        String n = file.toLowerCase(Locale.ROOT);
                        return n.endsWith(".mp3") || n.endsWith(".ogg") || n.endsWith(".flac") || n.endsWith(".wav");
                    });
                    dialog.setVisible(true);
                    String dir = dialog.getDirectory(), file = dialog.getFile();
                    if (file == null) return;
                    Minecraft.getMinecraft().addScheduledTask(() -> { path = dir + file; context.refresh(); });
                } finally { dialog.dispose(); }
            }, "UIE audio file dialog").start();
        }
        public void apply() { AudioModule.save(); }
        // Playback and library operations are immediate, like other media controls.
        public void cancel() { AudioModule.save(); }
    }
    private static NfrLabeledTextField field(String label, java.util.function.Supplier<String> getter, java.util.function.Consumer<String> setter) {
        return new NfrLabeledTextField(label, new TextFieldWidget().setMaxLength(4096).value(new NfrStringValue(getter, setter))).size(260, 52);
    }
    private static final class View extends NfrContentView<View> {
        View(NfrOptionsGrid grid) { super(section(grid, grid::preferredHeight)); }
    }
}
