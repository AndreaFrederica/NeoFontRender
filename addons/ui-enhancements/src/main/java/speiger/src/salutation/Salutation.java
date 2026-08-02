package speiger.src.salutation;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import neofontrender.addons.chat.EnhancedChatConfigAccess;

/**
 * UIE's embedded Salutation facade. The original Salutation entry point only created
 * CarbonConfig values and registered the client handler; UIE owns both lifecycle and
 * configuration now, while retaining the public fields used by the upstream command and
 * translation classes.
 */
public final class Salutation {
    public static final Logger LOGGER = LogManager.getLogger("Salutation");
    public static final BoolValue FORCE_SERVER_TRANSLATIONS = new BoolValue();
    public static final BoolValue DISABLE_OVERRIDE = new BoolValue();

    private Salutation() {}

    public static void initialize() {
        FORCE_SERVER_TRANSLATIONS.set(EnhancedChatConfigAccess.forceServerTranslations());
        DISABLE_OVERRIDE.set(EnhancedChatConfigAccess.salutationOverrideDisabled());
    }

    public static final class BoolValue {
        private volatile boolean value;

        public boolean get() {
            return value;
        }

        public void set(boolean value) {
            this.value = value;
        }
    }
}
