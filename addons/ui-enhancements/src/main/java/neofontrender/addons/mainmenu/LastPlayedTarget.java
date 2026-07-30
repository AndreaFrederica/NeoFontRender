package neofontrender.addons.mainmenu;

import java.util.Locale;
import java.util.Objects;

public final class LastPlayedTarget {
    public enum Kind {
        SINGLEPLAYER("singleplayer"),
        SERVER("server");

        private final String id;

        Kind(String id) {
            this.id = id;
        }

        public String id() {
            return id;
        }

        private static Kind parse(String value) {
            if (value == null) return null;
            String normalized = value.trim().toLowerCase(Locale.ROOT);
            for (Kind kind : values()) {
                if (kind.id.equals(normalized)) return kind;
            }
            return null;
        }
    }

    private final Kind kind;
    private final String identifier;
    private final String displayName;
    private final String address;

    private LastPlayedTarget(Kind kind, String identifier, String displayName, String address) {
        this.kind = kind;
        this.identifier = identifier;
        this.displayName = displayName;
        this.address = address;
    }

    public static LastPlayedTarget singleplayer(String folder, String displayName) {
        String safeFolder = clean(folder, 255);
        if (!safeWorldFolder(safeFolder)) return null;
        return new LastPlayedTarget(Kind.SINGLEPLAYER, safeFolder,
                name(displayName, safeFolder), "");
    }

    public static LastPlayedTarget server(String address, String displayName) {
        String safeAddress = clean(address, 512);
        if (safeAddress.isEmpty()) return null;
        return new LastPlayedTarget(Kind.SERVER, safeAddress,
                name(displayName, safeAddress), safeAddress);
    }

    static LastPlayedTarget persisted(String type, String identifier,
                                      String displayName, String address) {
        Kind kind = Kind.parse(type);
        if (kind == Kind.SINGLEPLAYER) return singleplayer(identifier, displayName);
        if (kind == Kind.SERVER) {
            String resolvedAddress = clean(address, 512);
            if (resolvedAddress.isEmpty()) resolvedAddress = identifier;
            return server(resolvedAddress, displayName);
        }
        return null;
    }

    public Kind kind() {
        return kind;
    }

    public String identifier() {
        return identifier;
    }

    public String displayName() {
        return displayName;
    }

    public String address() {
        return address;
    }

    String stableKey() {
        return kind.id + "\n" + identifier;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (!(object instanceof LastPlayedTarget)) return false;
        LastPlayedTarget other = (LastPlayedTarget) object;
        return kind == other.kind && identifier.equals(other.identifier)
                && displayName.equals(other.displayName) && address.equals(other.address);
    }

    @Override
    public int hashCode() {
        return Objects.hash(kind, identifier, displayName, address);
    }

    private static String name(String value, String fallback) {
        String result = clean(value, 255);
        return result.isEmpty() ? fallback : result;
    }

    private static String clean(String value, int maximumLength) {
        if (value == null) return "";
        String result = value.replace('\r', ' ').replace('\n', ' ').trim();
        return result.length() <= maximumLength ? result : result.substring(0, maximumLength);
    }

    private static boolean safeWorldFolder(String folder) {
        return !folder.isEmpty() && !".".equals(folder) && !"..".equals(folder)
                && folder.indexOf('/') < 0 && folder.indexOf('\\') < 0;
    }
}
