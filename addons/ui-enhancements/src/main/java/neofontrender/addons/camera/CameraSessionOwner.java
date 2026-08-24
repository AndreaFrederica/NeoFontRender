package neofontrender.addons.camera;

import neofontrender.addons.api.camera.CameraSession;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/** Owns a session returned by the public API, including sessions supplied by external providers. */
final class CameraSessionOwner {
    private static final Logger LOGGER = LogManager.getLogger("UIE Camera Session Owner");
    private CameraSession session;

    synchronized boolean adopt(CameraSession candidate) {
        close();
        if (candidate == null) return false;
        boolean active;
        try {
            active = candidate.isActive();
        } catch (RuntimeException error) {
            log("isActive during adopt", error);
            closeAfterFailure(candidate, error);
            throw error;
        }
        if (!active) {
            closeOrThrow(candidate);
            return false;
        }
        session = candidate;
        return true;
    }

    synchronized boolean isActive() {
        if (session == null) return false;
        boolean active;
        try {
            active = session.isActive();
        } catch (RuntimeException error) {
            CameraSession failed = session;
            session = null;
            log("isActive", error);
            closeAfterFailure(failed, error);
            throw error;
        }
        if (active) return true;
        CameraSession inactive = session;
        session = null;
        closeOrThrow(inactive);
        return false;
    }

    synchronized void close() {
        if (session == null) return;
        CameraSession closing = session;
        session = null;
        closeOrThrow(closing);
    }

    private static void closeAfterFailure(CameraSession candidate, RuntimeException original) {
        try {
            candidate.close();
        } catch (RuntimeException closeError) {
            log("close after failure", closeError);
            original.addSuppressed(closeError);
        }
    }

    private static void closeOrThrow(CameraSession candidate) {
        try {
            candidate.close();
        } catch (RuntimeException error) {
            log("close", error);
            throw error;
        }
    }

    private static void log(String operation, RuntimeException error) {
        LOGGER.error("Camera session {} callback failed", operation, error);
    }
}
