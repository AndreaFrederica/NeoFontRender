package neofontrender.addons.server;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Group definitions for the group-chat command. The file is a plain UTF-8
 * properties file (groups.&lt;name&gt;=player1,player2,...) so group names may
 * contain any characters the server console accepts.
 */
public final class ServerGroupConfig {
    private static final String PREFIX = "groups.";

    private final Path file;
    private final Map<String, List<String>> groups = new LinkedHashMap<>();

    public ServerGroupConfig(Path file) {
        this.file = file;
        load();
    }

    public void load() {
        groups.clear();
        if (!Files.isRegularFile(file)) return;
        try {
            for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#") || trimmed.startsWith("!")) continue;
                int separator = trimmed.indexOf('=');
                if (separator <= 0) continue;
                String key = trimmed.substring(0, separator).trim();
                if (!key.startsWith(PREFIX)) continue;
                String name = key.substring(PREFIX.length()).trim();
                if (name.isEmpty()) continue;
                groups.put(name, splitMembers(trimmed.substring(separator + 1)));
            }
        } catch (IOException exception) {
            throw new ServerChatHistoryException("Could not read group config " + file, exception);
        }
    }

    /** Persists the current definition set back to disk. */
    public void save() {
        List<String> lines = new ArrayList<>();
        lines.add("# NFR group chat definitions: groups.<name>=player1,player2,...");
        for (Map.Entry<String, List<String>> entry : groups.entrySet()) {
            lines.add(PREFIX + entry.getKey() + "=" + String.join(",", entry.getValue()));
        }
        try {
            Files.createDirectories(file.getParent());
            Files.write(file, lines, StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new ServerChatHistoryException("Could not write group config " + file, exception);
        }
    }

    public List<String> groupNames() {
        List<String> names = new ArrayList<>(groups.keySet());
        Collections.sort(names, String.CASE_INSENSITIVE_ORDER);
        return names;
    }

    public boolean hasGroup(String name) {
        return findGroup(name) != null;
    }

    /** Canonical group name for a case-insensitive match, or null. */
    public String findGroup(String name) {
        String trimmed = name == null ? "" : name.trim();
        for (String existing : groups.keySet()) {
            if (existing.equalsIgnoreCase(trimmed)) return existing;
        }
        return null;
    }

    public List<String> members(String name) {
        String canonical = findGroup(name);
        List<String> members = canonical == null ? null : groups.get(canonical);
        return members == null ? Collections.emptyList() : members;
    }

    /** Replaces or adds a group; case-insensitive lookup is used for commands. */
    public void setGroup(String name, List<String> members) {
        groups.put(name.trim(), new ArrayList<>(members));
    }

    private static List<String> splitMembers(String value) {
        List<String> members = new ArrayList<>();
        for (String member : value.split(",")) {
            String trimmed = member.trim();
            if (!trimmed.isEmpty()) members.add(trimmed);
        }
        return members;
    }
}
