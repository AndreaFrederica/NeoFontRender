package neofontrender.addons.api.ui.navigation;

import java.util.List;
import java.util.Objects;

/** Semantic access to the tabs currently rendered by the creative inventory. */
public interface CreativeTabNavigation {
    List<Tab> nfrUi$getVisibleCreativeTabs();

    int nfrUi$getSelectedCreativeTab();

    boolean nfrUi$selectCreativeTab(int tabIndex);

    boolean nfrUi$changeCreativeTab(int direction);

    final class Tab {
        private final int index;
        private final String label;
        private final UiRect bounds;

        public Tab(int index, String label, UiRect bounds) {
            this.index = index;
            this.label = label == null ? "" : label;
            this.bounds = Objects.requireNonNull(bounds, "bounds");
        }

        public int index() { return index; }
        public String label() { return label; }
        public UiRect bounds() { return bounds; }
    }
}
