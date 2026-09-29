package neofontrender.addons.chat.network;

/** Connection identity and request generation prevent delayed replies from granting stale permission. */
public final class ChatPolicySession {
    public static final int PROTOCOL_VERSION = 1;
    private Object connection;
    private long generation;
    private boolean sectionSignAllowed;

    public synchronized long begin(Object connection) {
        this.connection = connection;
        sectionSignAllowed = false;
        return ++generation;
    }

    public synchronized void disconnect(Object connection) {
        if (this.connection != connection) return;
        this.connection = null;
        sectionSignAllowed = false;
        ++generation;
    }

    public synchronized boolean matches(Object connection, long request) {
        return connection != null && this.connection == connection && generation == request;
    }

    public synchronized void accept(Object connection, long request, int version, boolean allowed) {
        if (matches(connection, request)) {
            sectionSignAllowed = version == PROTOCOL_VERSION && allowed;
        }
    }

    public synchronized boolean allowsSectionSign(Object connection) {
        return connection != null && this.connection == connection && sectionSignAllowed;
    }
}
