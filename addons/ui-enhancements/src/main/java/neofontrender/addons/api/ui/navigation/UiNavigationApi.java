package neofontrender.addons.api.ui.navigation;

import net.minecraft.util.ResourceLocation;
import neofontrender.addons.navigation.UiNavigationRegistry;
import neofontrender.addons.navigation.UiNavigationRuntime;

import java.util.Objects;

public final class UiNavigationApi {
    private UiNavigationApi() {}

    public static UiRegistration registerProvider(ResourceLocation id, int priority, UiTreeProvider provider) {
        return UiNavigationRegistry.global().register(id, priority, provider);
    }

    public static UiTreeSnapshot currentTree() { return UiNavigationRuntime.instance().currentTree(); }
    public static UiFocusState focusState() { return UiNavigationRuntime.instance().focusState(); }

    public static UiNavigationResult navigate(UiDirection direction, UiInputSource source) {
        return UiNavigationRuntime.instance().navigate(
                Objects.requireNonNull(direction, "direction"), Objects.requireNonNull(source, "source"));
    }

    public static UiActionResult perform(UiAction action, UiInputSource source) {
        return UiNavigationRuntime.instance().perform(
                Objects.requireNonNull(action, "action"), Objects.requireNonNull(source, "source"));
    }

    public static UiActionResult back(UiInputSource source) {
        return UiNavigationRuntime.instance().back(Objects.requireNonNull(source, "source"));
    }

    public static void movePointer(double x, double y, UiInputSource source) {
        UiNavigationRuntime.instance().movePointer(x, y, Objects.requireNonNull(source, "source"));
    }

    public static UiActionResult pointerButton(int button, boolean pressed, UiInputSource source) {
        return UiNavigationRuntime.instance().pointerButton(button, pressed,
                Objects.requireNonNull(source, "source"));
    }

    public static UiActionResult scrollPointer(int wheel, UiInputSource source) {
        return UiNavigationRuntime.instance().scrollPointer(wheel,
                Objects.requireNonNull(source, "source"));
    }

    public static UiInteractionLease acquire(UiInputSource source) {
        return UiNavigationRuntime.instance().acquire(Objects.requireNonNull(source, "source"));
    }

    public static boolean isSyntheticMouseEvent() {
        return UiNavigationRuntime.instance().isSyntheticMouseEvent();
    }

    public static boolean isPointerButtonDown(int button) {
        return UiNavigationRuntime.instance().isPointerButtonDown(button);
    }
}
