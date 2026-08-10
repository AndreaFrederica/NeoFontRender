package neofontrender.addons.camera;

import neofontrender.addons.api.camera.CameraSession;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CameraSessionOwnerTest {
    @Test
    void replacingOrClosingAnF5LeaseClosesTheProviderSessionExactlyOnce() {
        CameraSessionOwner owner = new CameraSessionOwner();
        TestSession first = new TestSession(true);
        TestSession second = new TestSession(true);

        assertTrue(owner.adopt(first));
        assertTrue(owner.adopt(second));
        assertEquals(1, first.closeCount);
        assertTrue(owner.isActive());

        owner.close();
        owner.close();
        assertEquals(1, second.closeCount);
        assertFalse(owner.isActive());
    }

    @Test
    void inactiveProviderSessionIsRejectedAndClosed() {
        CameraSessionOwner owner = new CameraSessionOwner();
        TestSession inactive = new TestSession(false);

        assertFalse(owner.adopt(inactive));
        assertEquals(1, inactive.closeCount);
    }

    @Test
    void callbackFailuresPropagateAfterTheSessionIsDetached() {
        CameraSessionOwner owner = new CameraSessionOwner();
        ThrowingSession acquireFailure = new ThrowingSession(true, false);
        assertThrows(IllegalStateException.class, () -> owner.adopt(acquireFailure));
        assertEquals(1, acquireFailure.closeCount);

        ThrowingSession pollFailure = new ThrowingSession(false, false);
        assertTrue(owner.adopt(pollFailure));
        pollFailure.throwOnActive = true;
        assertThrows(IllegalStateException.class, owner::isActive);
        assertEquals(1, pollFailure.closeCount);
        assertFalse(owner.isActive());

        ThrowingSession closeFailure = new ThrowingSession(false, true);
        assertTrue(owner.adopt(closeFailure));
        assertThrows(IllegalStateException.class, owner::close);
        owner.close();
        assertEquals(1, closeFailure.closeCount);
        assertFalse(owner.isActive());
    }

    private static final class TestSession implements CameraSession {
        private boolean active;
        private int closeCount;

        private TestSession(boolean active) { this.active = active; }
        @Override public boolean isActive() { return active; }
        @Override public void close() {
            if (!active && closeCount > 0) return;
            active = false;
            closeCount++;
        }
    }

    private static final class ThrowingSession implements CameraSession {
        private boolean throwOnActive;
        private final boolean throwOnClose;
        private int closeCount;

        private ThrowingSession(boolean throwOnActive, boolean throwOnClose) {
            this.throwOnActive = throwOnActive;
            this.throwOnClose = throwOnClose;
        }

        @Override public boolean isActive() {
            if (throwOnActive) throw new IllegalStateException("provider isActive failure");
            return true;
        }

        @Override public void close() {
            closeCount++;
            if (throwOnClose) throw new IllegalStateException("provider close failure");
        }
    }
}
