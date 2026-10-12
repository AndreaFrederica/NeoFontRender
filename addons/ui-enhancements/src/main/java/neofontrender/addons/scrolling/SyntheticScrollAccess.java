package neofontrender.addons.scrolling;

/** Receives a synthetic mouse-wheel notch through the same scroller used by physical input. */
public interface SyntheticScrollAccess {
    boolean nfrUi$scrollWheel(int wheel);
}
