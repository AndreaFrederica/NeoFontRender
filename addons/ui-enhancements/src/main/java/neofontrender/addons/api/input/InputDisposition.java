package neofontrender.addons.api.input;

/** How the active input-context stack routed one logical action. */
public enum InputDisposition {
    /** No UIE context owns the action; the vanilla bridge may consume it. */
    PASS,
    /** A named context owns the action and consumes the frame value. */
    CLAIM,
    /** A named context intentionally produces a neutral value for the action. */
    BLOCK
}
