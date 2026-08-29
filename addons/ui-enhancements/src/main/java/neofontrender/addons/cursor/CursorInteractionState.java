package neofontrender.addons.cursor;

/** State of the object under the pointer, independent of its semantic shape. */
public enum CursorInteractionState {
    NORMAL,
    HOVER,
    PRESSED,
    ACTIVE,
    DISABLED,
    FOCUSED,
    SELECTED,
    DRAGGABLE,
    DRAGGING,
    DROP_ALLOWED,
    DROP_FORBIDDEN,
    LOADING,
    ERROR
}
