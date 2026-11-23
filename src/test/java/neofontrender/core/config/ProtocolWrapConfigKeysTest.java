package neofontrender.core.config;

import com.electronwill.nightconfig.core.Config;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guards the on-disk shape of the protocol-wrap settings.
 *
 * <p>NightConfig treats dot-separated keys as paths. A key cannot be a boolean leaf and a
 * parent table at the same time: writing {@code fix.protocolWrap = true} and then
 * {@code fix.protocolWrap.chat = true} throws
 * {@code IncompatibleIntermediaryLevelException}, which used to escape from the settings
 * screen and crash the client. The scope table therefore lives under its own parent.</p>
 */
class ProtocolWrapConfigKeysTest {
    @Test
    void masterBooleanAndScopeTableCoexist() {
        Config config = Config.inMemory();
        config.set("fix.protocolWrap", true);
        config.set("fix.protocolWrapScopes.chat", false);
        config.set("fix.protocolWrapScopes.tooltip", true);
        config.set("fix.protocolWrapScopes.sign", true);
        config.set("fix.protocolWrapScopes.other", false);

        assertEquals(Boolean.TRUE, config.get("fix.protocolWrap"));
        assertEquals(Boolean.FALSE, config.get("fix.protocolWrapScopes.chat"));
        assertEquals(Boolean.TRUE, config.get("fix.protocolWrapScopes.tooltip"));
        assertEquals(Boolean.TRUE, config.get("fix.protocolWrapScopes.sign"));
        assertEquals(Boolean.FALSE, config.get("fix.protocolWrapScopes.other"));
        assertTrue(config.get("fix.protocolWrapScopes") instanceof Config);
    }

    @Test
    void nestingAScopeUnderTheBooleanMasterIsRejected() {
        Config config = Config.inMemory();
        config.set("fix.protocolWrap", true);

        assertThrows(RuntimeException.class,
                () -> config.set("fix.protocolWrap.chat", true),
                "this shape crashed the settings screen; scopes must use their own table");
    }
}
