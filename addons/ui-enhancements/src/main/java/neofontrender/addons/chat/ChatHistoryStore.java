package neofontrender.addons.chat;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Embedded H2 store backing the chat-history persistence layer. Messages are
 * written synchronously on arrival instead of being batched into periodic
 * full-file JSON rewrites. Minecraft keeps one process on the database file, so
 * the default single-server file mode is sufficient.
 */
public final class ChatHistoryStore implements AutoCloseable {

    private final Connection connection;
    private final ReentrantLock lock = new ReentrantLock();

    public ChatHistoryStore(Path dataBase) throws SQLException {
        String path = dataBase.toAbsolutePath().toString().replace('\\', '/');
        // H2 appends .mv.db; callers pass the extension-less base path.
        connection = DriverManager.getConnection("jdbc:h2:file:" + path);
        initialize();
    }

    /** Test-only entry point backed by a fresh in-memory database. */
    static ChatHistoryStore inMemory() throws SQLException {
        String name = "chat-history-" + Long.toHexString(System.nanoTime());
        return new ChatHistoryStore(DriverManager.getConnection("jdbc:h2:mem:" + name + ";DB_CLOSE_DELAY=-1"));
    }

    private ChatHistoryStore(Connection connection) throws SQLException {
        this.connection = connection;
        initialize();
    }

    public void initialize() throws SQLException {
        lock.lock();
        try (Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE IF NOT EXISTS received_messages ("
                    + "id BIGINT AUTO_INCREMENT PRIMARY KEY,"
                    + "scope VARCHAR(255) NOT NULL,"
                    + "msg_id INT NOT NULL DEFAULT 0,"
                    + "timestamp BIGINT NOT NULL,"
                    + "source VARCHAR(16) NOT NULL,"
                    + "player_name VARCHAR(64) NOT NULL DEFAULT '',"
                    + "player_id VARCHAR(36),"
                    + "private_peer VARCHAR(64) NOT NULL DEFAULT '',"
                    + "outgoing BOOLEAN NOT NULL DEFAULT FALSE,"
                    + "private_body CLOB,"
                    + "group_name VARCHAR(128) NOT NULL DEFAULT '',"
                    + "json CLOB NOT NULL)");
            statement.execute("ALTER TABLE received_messages ADD COLUMN IF NOT EXISTS "
                    + "group_name VARCHAR(128) NOT NULL DEFAULT ''");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_received_scope_id "
                    + "ON received_messages(scope, id)");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_received_scope_msgid "
                    + "ON received_messages(scope, msg_id)");
            statement.execute("CREATE TABLE IF NOT EXISTS sent_messages ("
                    + "id BIGINT AUTO_INCREMENT PRIMARY KEY,"
                    + "scope VARCHAR(255) NOT NULL,"
                    + "text VARCHAR(32767) NOT NULL)");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_sent_scope_id "
                    + "ON sent_messages(scope, id)");
        } finally {
            lock.unlock();
        }
    }

    /** Runs {@code work} atomically; any thrown exception rolls the batch back. */
    public void runInTransaction(Runnable work) {
        lock.lock();
        try {
            connection.setAutoCommit(false);
            try {
                work.run();
                connection.commit();
            } catch (RuntimeException exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException exception) {
            throw new ChatHistoryException("Transaction failed", exception);
        } finally {
            lock.unlock();
        }
    }

    public void insertReceived(String scope, int msgId, long timestamp, ChatMessageMetadata metadata, String json) {
        lock.lock();
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO received_messages (scope, msg_id, timestamp, source, player_name, player_id,"
                        + " private_peer, outgoing, private_body, group_name, json)"
                        + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
            statement.setString(1, scope);
            statement.setInt(2, msgId);
            statement.setLong(3, timestamp);
            statement.setString(4, metadata.source.name());
            statement.setString(5, metadata.playerName);
            setNullable(statement, 6, metadata.playerId == null ? null : metadata.playerId.toString());
            statement.setString(7, metadata.privatePeer);
            statement.setBoolean(8, metadata.outgoing);
            statement.setString(9, metadata.privateBody.isEmpty() ? null : metadata.privateBody);
            statement.setString(10, metadata.group);
            statement.setString(11, json);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new ChatHistoryException("Could not store received message", exception);
        } finally {
            lock.unlock();
        }
    }

    public void insertSent(String scope, String text) {
        lock.lock();
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO sent_messages (scope, text) VALUES (?, ?)")) {
            statement.setString(1, scope);
            statement.setString(2, text);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new ChatHistoryException("Could not store sent message", exception);
        } finally {
            lock.unlock();
        }
    }

    /** Removes a replaceable message (msg_id != 0) so its newest variant wins. */
    public void deleteReceivedById(String scope, int msgId) {
        if (msgId == 0) return;
        lock.lock();
        try (PreparedStatement statement = connection.prepareStatement(
                "DELETE FROM received_messages WHERE scope = ? AND msg_id = ?")) {
            statement.setString(1, scope);
            statement.setInt(2, msgId);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new ChatHistoryException("Could not delete received message", exception);
        } finally {
            lock.unlock();
        }
    }

    /** Removes a single stored message by its table row id (history-management UI). */
    public void deleteRow(long rowId) {
        lock.lock();
        try (PreparedStatement statement = connection.prepareStatement(
                "DELETE FROM received_messages WHERE id = ?")) {
            statement.setLong(1, rowId);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new ChatHistoryException("Could not delete received message row " + rowId, exception);
        } finally {
            lock.unlock();
        }
    }

    /** Removes every message of a source across all scopes (history-management UI). */
    public void deleteBySource(ChatSource source) {
        lock.lock();
        try (PreparedStatement statement = connection.prepareStatement(
                "DELETE FROM received_messages WHERE source = ?")) {
            statement.setString(1, source.name());
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new ChatHistoryException("Could not delete messages of source " + source, exception);
        } finally {
            lock.unlock();
        }
    }

    public List<ReceivedMessage> loadReceived(String scope) {
        lock.lock();
        List<ReceivedMessage> messages = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT id, msg_id, timestamp, source, player_name, player_id, private_peer, outgoing, private_body,"
                        + " group_name, json FROM received_messages WHERE scope = ? ORDER BY id ASC")) {
            statement.setString(1, scope);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    ChatSource source = ChatSource.valueOf(result.getString("source"));
                    UUID playerId = nullableUuid(result.getString("player_id"));
                    ChatMessageMetadata metadata = new ChatMessageMetadata(result.getLong("timestamp"),
                            source, result.getString("player_name"), playerId,
                            result.getString("private_peer"), result.getBoolean("outgoing"),
                            result.getString("private_body") == null ? "" : result.getString("private_body"),
                            result.getString("group_name") == null ? "" : result.getString("group_name"));
                    messages.add(new ReceivedMessage(result.getLong("id"), result.getInt("msg_id"),
                            metadata, result.getString("json")));
                }
            }
        } catch (SQLException exception) {
            throw new ChatHistoryException("Could not load received messages", exception);
        } finally {
            lock.unlock();
        }
        return messages;
    }

    public List<String> loadSent(String scope) {
        lock.lock();
        List<String> messages = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT text FROM sent_messages WHERE scope = ? ORDER BY id ASC")) {
            statement.setString(1, scope);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) messages.add(result.getString("text"));
            }
        } catch (SQLException exception) {
            throw new ChatHistoryException("Could not load sent messages", exception);
        } finally {
            lock.unlock();
        }
        return messages;
    }

    /** Keeps only the newest {@code limit} received messages of a scope. */
    public void trimReceived(String scope, int limit) {
        lock.lock();
        try (PreparedStatement statement = connection.prepareStatement(
                "DELETE FROM received_messages WHERE scope = ? AND id NOT IN ("
                        + "SELECT id FROM received_messages WHERE scope = ? ORDER BY id DESC LIMIT ?)")) {
            statement.setString(1, scope);
            statement.setString(2, scope);
            statement.setInt(3, limit);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new ChatHistoryException("Could not trim received messages", exception);
        } finally {
            lock.unlock();
        }
    }

    public void trimSent(String scope, int limit) {
        lock.lock();
        try (PreparedStatement statement = connection.prepareStatement(
                "DELETE FROM sent_messages WHERE scope = ? AND id NOT IN ("
                        + "SELECT id FROM sent_messages WHERE scope = ? ORDER BY id DESC LIMIT ?)")) {
            statement.setString(1, scope);
            statement.setString(2, scope);
            statement.setInt(3, limit);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new ChatHistoryException("Could not trim sent messages", exception);
        } finally {
            lock.unlock();
        }
    }

    public void deleteScope(String scope) {
        lock.lock();
        try (PreparedStatement received = connection.prepareStatement(
                "DELETE FROM received_messages WHERE scope = ?");
             PreparedStatement sent = connection.prepareStatement(
                     "DELETE FROM sent_messages WHERE scope = ?")) {
            received.setString(1, scope);
            received.executeUpdate();
            sent.setString(1, scope);
            sent.executeUpdate();
        } catch (SQLException exception) {
            throw new ChatHistoryException("Could not delete scope " + scope, exception);
        } finally {
            lock.unlock();
        }
    }

    public List<String> scopes() {
        lock.lock();
        List<String> scopes = new ArrayList<>();
        try (Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery(
                     "SELECT DISTINCT scope FROM received_messages UNION "
                             + "SELECT DISTINCT scope FROM sent_messages")) {
            while (result.next()) scopes.add(result.getString("scope"));
        } catch (SQLException exception) {
            throw new ChatHistoryException("Could not list scopes", exception);
        } finally {
            lock.unlock();
        }
        return scopes;
    }

    public boolean isEmpty() {
        lock.lock();
        try (Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery(
                     "SELECT 1 FROM received_messages UNION ALL SELECT 1 FROM sent_messages")) {
            return !result.next();
        } catch (SQLException exception) {
            throw new ChatHistoryException("Could not inspect store", exception);
        } finally {
            lock.unlock();
        }
    }

    private static void setNullable(PreparedStatement statement, int index, String value) throws SQLException {
        if (value == null) statement.setNull(index, java.sql.Types.VARCHAR);
        else statement.setString(index, value);
    }

    private static UUID nullableUuid(String value) {
        if (value == null || value.isEmpty()) return null;
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    @Override
    public void close() {
        lock.lock();
        try {
            connection.close();
        } catch (SQLException ignored) {
        } finally {
            lock.unlock();
        }
    }

    public static final class ReceivedMessage {
        public final long rowId;
        public final int msgId;
        public final ChatMessageMetadata metadata;
        public final String json;

        private ReceivedMessage(long rowId, int msgId, ChatMessageMetadata metadata, String json) {
            this.rowId = rowId;
            this.msgId = msgId;
            this.metadata = metadata;
            this.json = json;
        }
    }
}
