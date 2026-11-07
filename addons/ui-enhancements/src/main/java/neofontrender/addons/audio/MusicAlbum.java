package neofontrender.addons.audio;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Immutable album metadata exposed to UIE consumers and the future player screen. */
public final class MusicAlbum {
    public static final class Track {
        public final String source, title, artist, composer;
        public final int trackNumber;
        Track(AudioLibrary.AlbumTrack track, String albumArtist) {
            source = value(track.source);
            title = value(track.title);
            artist = value(track.artist).isEmpty() ? value(albumArtist) : track.artist;
            composer = value(track.composer);
            trackNumber = track.trackNumber;
        }
    }
    public final String id, title, artist, cover, genre;
    public final int year;
    public final List<Track> tracks;
    MusicAlbum(String id, AudioLibrary.Album album) {
        this.id = id;
        title = value(album.title);
        artist = value(album.artist);
        cover = value(album.cover);
        genre = value(album.genre);
        year = album.year;
        List<Track> copy = new ArrayList<>();
        for (AudioLibrary.AlbumTrack track : album.tracks) copy.add(new Track(track, artist));
        tracks = Collections.unmodifiableList(copy);
    }
    private static String value(String value) { return value == null ? "" : value; }
}
