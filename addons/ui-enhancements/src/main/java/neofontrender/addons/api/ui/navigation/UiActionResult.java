package neofontrender.addons.api.ui.navigation;

public enum UiActionResult {
    IGNORED(false, false),
    HANDLED(true, false),
    CHANGED(true, true),
    DEFERRED(true, false),
    STALE(false, false),
    REJECTED(false, false),
    FAILED(false, false);

    private final boolean handled;
    private final boolean changed;

    UiActionResult(boolean handled, boolean changed) {
        this.handled = handled;
        this.changed = changed;
    }

    public boolean isHandled() { return handled; }
    public boolean isChanged() { return changed; }
}
