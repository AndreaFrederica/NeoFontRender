package neofontrender.addons.server;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Server-side embedded H2 store for chat messages. Everything a player says or
 * sends (public chat, private messages, group messages) lands here on arrival,
 * keyed by scope so the future chat-group feature can query per-group history.
 */
public final class ServerChatHistoryStore implements AutoCloseable {

    public static final String SCOPE_GLOBAL = "global";
    public static final String SCOPE_PRIVATE = "private";
    public static final String GROUP_PREFIX = "group:";

    private final Connection connection;
    private final ReentrantLock lock = new ReentrantLock();

    public ServerChatHistoryStore(Path dataBase) throws SQLException {
        String path = dataBase.toAbsolutePath().toString().replace('\\', '/');
        connection = DriverManager.getConnection("jdbc:h2:file:" + path);
        initialize();
    }

    /** Test-only entry point backed by a fresh in-memory database. */
    static ServerChatHistoryStore inMemory() throws SQLException {
        String name = "server-chat-" + Long.toHexString(System.nanoTime());
        ServerChatHistoryStore store = new ServerChatHistoryStore(
                DriverManager.getConnection("jdbc:h2:mem:" + name + ";DB_CLOSE_DELAY=-1"));
        store.initialize();
        return store;
    }

    private ServerChatHistoryStore(Connection connection) {
        this.connection = connection;
    }

    public void initialize() throws SQLException {
        lock.lock();
        try (Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE IF NOT EXISTS chat_messages ("
                    + "id BIGINT AUTO_INCREMENT PRIMARY KEY,"
                    + "msg_type VARCHAR(16) NOT NULL,"
                    + "scope VARCHAR(128) NOT NULL,"
                    + "sender VARCHAR(64) NOT NULL,"
                    + "recipients VARCHAR(1024) NOT NULL DEFAULT '',"
                    + "timestamp BIGINT NOT NULL,"
                    + "message VARCHAR(32767) NOT NULL)");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_chat_scope_id "
                    + "ON chat_messages(scope, id)");
        } finally {
            lock.unlock();
        }
    }

    public void insert(String type, String scope, String sender, String recipients, long timestamp, String message) {
        lock.lock();
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO chat_messages (msg_type, scope, sender, recipients, timestamp, message)"
                        + " VALUES (?, ?, ?, ?, ?, ?)")) {
            statement.setString(1, type);
            statement.setString(2, scope);
            statement.setString(3, sender);
            statement.setString(4, recipients == null ? "" : recipients);
            statement.setLong(5, timestamp);
            statement.setString(6, message);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new ServerChatHistoryException("Could not store chat message", exception);
        } finally {
            lock.unlock();
        }
    }

    /** Messages of a scope in arrival order. */
    public List<Message> load(String scope) {
        lock.lock();
        List<Message> messages = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT id, msg_type, scope, sender, recipients, timestamp, message"
                        + " FROM chat_messages WHERE scope = ? ORDER BY id ASC")) {
            statement.setString(1, scope);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) messages.add(read(result));
            }
        } catch (SQLException exception) {
            throw new ServerChatHistoryException("Could not load chat messages", exception);
        } finally {
            lock.unlock();
        }
        return messages;
    }

    /** Messages newer than {@code afterId}, limited to {@code limit} newest. */
    public List<Message> loadSince(String scope, long afterId, int limit) {
        lock.lock();
        List<Message> messages = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT id, msg_type, scope, sender, recipients, timestamp, message"
                        + " FROM chat_messages WHERE scope = ? AND id > ?"
                        + " ORDER BY id ASC LIMIT ?")) {
            statement.setString(1, scope);
            statement.setLong(2, afterId);
            statement.setInt(3, limit);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) messages.add(read(result));
            }
        } catch (SQLException exception) {
            throw new ServerChatHistoryException("Could not load chat messages", exception);
        } finally {
            lock.unlock();
        }
        return messages;
    }

    public List<String> scopes() {
        lock.lock();
        List<String> scopes = new ArrayList<>();
        try (Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery(
                     "SELECT DISTINCT scope FROM chat_messages ORDER BY scope")) {
            while (result.next()) scopes.add(result.getString("scope"));
        } catch (SQLException exception) {
            throw new ServerChatHistoryException("Could not list scopes", exception);
        } finally {
            lock.unlock();
        }
        return scopes;
    }

    /** Keeps only the newest {@code limit} messages of a scope. */
    public void trim(String scope, int limit) {
        lock.lock();
        try (PreparedStatement statement = connection.prepareStatement(
                "DELETE FROM chat_messages WHERE scope = ? AND id NOT IN ("
                        + "SELECT id FROM chat_messages WHERE scope = ? ORDER BY id DESC LIMIT ?)")) {
            statement.setString(1, scope);
            statement.setString(2, scope);
            statement.setInt(3, limit);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new ServerChatHistoryException("Could not trim chat messages", exception);
        } finally {
            lock.unlock();
        }
    }

    public boolean isEmpty() {
        lock.lock();
        try (Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery("SELECT 1 FROM chat_messages")) {
            return !result.next();
        } catch (SQLException exception) {
            throw new ServerChatHistoryException("Could not inspect store", exception);
        } finally {
            lock.unlock();
        }
    }

    private static Message read(ResultSet result) throws SQLException {
        return new Message(result.getLong("id"), result.getString("msg_type"), result.getString("scope"),
                result.getString("sender"), result.getString("recipients"), result.getLong("timestamp"),
                result.getString("message"));
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

    public static final class Message {
        public final long id;
        public final String type;
        public final String scope;
        public final String sender;
        public final String recipients;
        public final long timestamp;
        public final String message;

        private Message(long id, String type, String scope, String sender, String recipients,
                        long timestamp, String message) {
            this.id = id;
            this.type = type;
            this.scope = scope;
            this.sender = sender;
            this.recipients = recipients;
            this.timestamp = timestamp;
            this.message = message;
        }
    }
}
