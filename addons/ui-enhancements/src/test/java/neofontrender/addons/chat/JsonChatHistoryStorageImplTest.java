package neofontrender.addons.chat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JsonChatHistoryStorageImplTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void roundTripsScopesIdsComponentsUnicodeAndSentHistory() {
        Path file = temporaryDirectory.resolve("history.json");
        JsonChatHistoryStorageImpl storage = new JsonChatHistoryStorageImpl(file);
        Map<String, ChatHistoryData> expected = new LinkedHashMap<>();
        expected.put("server:play.example:25565", new ChatHistoryData(
                Arrays.asList(
                        new ChatHistoryEntry(0, "{\"text\":\"hello\"}"),
                        new ChatHistoryEntry(42, "{\"text\":\"中文\"}")),
                Arrays.asList("/help", "你好")));
        expected.put("singleplayer:World One", new ChatHistoryData(
                Collections.singletonList(new ChatHistoryEntry(7, "{\"text\":\"world\"}")),
                Collections.singletonList("/spawn")));

        assertTrue(storage.save(expected));
        Map<String, ChatHistoryData> actual = storage.load();
        assertEquals(expected.keySet(), actual.keySet());
        for (String scope : expected.keySet()) {
            assertEquals(expected.get(scope).received(), actual.get(scope).received());
            assertEquals(expected.get(scope).sent(), actual.get(scope).sent());
        }
    }

    @Test
    void loadSkipsEntriesWithInvalidScopeKeys() throws Exception {
        Path file = temporaryDirectory.resolve("history.json");
        Files.write(file, ("{\"version\":2,\"scopes\":{"
                + "\"bogus\":{\"received\":[],\"sent\":[]},"
                + "\"server:play.example:25565\":{\"received\":[{\"id\":1,\"text\":\"{}\"}],\"sent\":[\"/help\"]}"
                + "}}").getBytes(StandardCharsets.UTF_8));

        Map<String, ChatHistoryData> loaded = new JsonChatHistoryStorageImpl(file).load();
        assertEquals(Collections.singleton("server:play.example:25565"), loaded.keySet());
        assertEquals(Collections.singletonList(new ChatHistoryEntry(1, "{}")),
                loaded.get("server:play.example:25565").received());
    }

    @Test
    void malformedDocumentIsLoggedAndDoesNotEscapeIntoTheClient() throws Exception {
        Path file = temporaryDirectory.resolve("broken.json");
        Files.write(file, "{\"scopes\":[]}".getBytes(StandardCharsets.UTF_8));

        Map<String, ChatHistoryData> loaded = new JsonChatHistoryStorageImpl(file).load();
        assertTrue(loaded.isEmpty());
    }

    @Test
    void ioFailureIsReportedWithoutCrashingTheClient() throws Exception {
        Path parentFile = temporaryDirectory.resolve("not-a-directory");
        Files.write(parentFile, new byte[] {1});
        JsonChatHistoryStorageImpl storage = new JsonChatHistoryStorageImpl(parentFile.resolve("history.json"));

        assertFalse(storage.save(Collections.<String, ChatHistoryData>emptyMap()));
    }
}
