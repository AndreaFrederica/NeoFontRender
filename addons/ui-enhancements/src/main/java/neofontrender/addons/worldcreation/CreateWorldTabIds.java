package neofontrender.addons.worldcreation;

/** Synthetic button ids of the create-world tabs, shared between the layout and style mixins. */
public final class CreateWorldTabIds {
    public static final int GAME = 28640;
    public static final int WORLD = 28641;
    public static final int RULES = 28642;

    private CreateWorldTabIds() {}

    public static boolean isTab(int buttonId) {
        return buttonId == GAME || buttonId == WORLD || buttonId == RULES;
    }
}
