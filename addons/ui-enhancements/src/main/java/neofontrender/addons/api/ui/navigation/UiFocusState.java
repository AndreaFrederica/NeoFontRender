package neofontrender.addons.api.ui.navigation;

public final class UiFocusState {
    public static final UiFocusState EMPTY = new UiFocusState(null, null, null, false, false);

    private final UiNodeId focusedNodeId;
    private final UiNodeId activeScopeId;
    private final UiInputSource inputSource;
    private final boolean focusVisible;
    private final boolean editing;

    public UiFocusState(UiNodeId focusedNodeId, UiNodeId activeScopeId, UiInputSource inputSource,
                        boolean focusVisible, boolean editing) {
        this.focusedNodeId = focusedNodeId;
        this.activeScopeId = activeScopeId;
        this.inputSource = inputSource;
        this.focusVisible = focusVisible;
        this.editing = editing;
    }

    public UiNodeId focusedNodeId() { return focusedNodeId; }
    public UiNodeId activeScopeId() { return activeScopeId; }
    public UiInputSource inputSource() { return inputSource; }
    public boolean focusVisible() { return focusVisible; }
    public boolean editing() { return editing; }
}
