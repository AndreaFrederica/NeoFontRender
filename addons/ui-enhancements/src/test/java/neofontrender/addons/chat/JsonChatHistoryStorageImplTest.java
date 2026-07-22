package neofontrender.addons.chat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JsonChatHistoryStorageImplTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void roundTripsIdsComponentsUnicodeAndSentHistory() {
        Path file = temporaryDirectory.resolve("history.json");
        JsonChatHistoryStorageImpl storage = new JsonChatHistoryStorageImpl(file);
        ChatHistoryData expected = new ChatHistoryData(
                Arrays.asList(
                        new ChatHistoryEntry(0, "{\"text\":\"hello\"}"),
                        new ChatHistoryEntry(42, "{\"text\":\"中文\"}")),
                Arrays.asList("/help", "你好"));

        assertTrue(storage.save(expected));
        ChatHistoryData actual = storage.load();
        assertEquals(expected.received(), actual.received());
        assertEquals(expected.sent(), actual.sent());
    }

    @Test
    void malformedDocumentIsLoggedAndDoesNotEscapeIntoTheClient() throws Exception {
        Path file = temporaryDirectory.resolve("broken.json");
        Files.write(file, "{\"received\":{}}".getBytes(StandardCharsets.UTF_8));

        ChatHistoryData loaded = new JsonChatHistoryStorageImpl(file).load();
        assertTrue(loaded.received().isEmpty());
        assertTrue(loaded.sent().isEmpty());
    }

    @Test
    void ioFailureIsReportedWithoutCrashingTheClient() throws Exception {
        Path parentFile = temporaryDirectory.resolve("not-a-directory");
        Files.write(parentFile, new byte[] {1});
        JsonChatHistoryStorageImpl storage = new JsonChatHistoryStorageImpl(parentFile.resolve("history.json"));

        assertFalse(storage.save(ChatHistoryData.empty()));
    }
}
