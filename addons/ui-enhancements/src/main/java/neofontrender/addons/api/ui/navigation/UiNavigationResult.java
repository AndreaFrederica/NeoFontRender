package neofontrender.addons.api.ui.navigation;

public enum UiNavigationResult {
    MOVED(true),
    ACTION_HANDLED(true),
    DEFERRED(true),
    NO_FOCUS(false),
    NO_TARGET(false),
    REJECTED(false),
    FAILED(false);

    private final boolean handled;

    UiNavigationResult(boolean handled) { this.handled = handled; }

    public boolean isHandled() { return handled; }
}
