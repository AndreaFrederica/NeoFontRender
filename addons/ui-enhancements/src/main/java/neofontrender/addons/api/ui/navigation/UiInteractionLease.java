package neofontrender.addons.api.ui.navigation;

public interface UiInteractionLease extends AutoCloseable {
    UiInputSource source();
    boolean isActive();
    @Override void close();
}
