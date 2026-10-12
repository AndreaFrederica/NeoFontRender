package neofontrender.addons.muixml.client;

import com.cleanroommc.modularui.api.dom.MuiDocument;
import com.cleanroommc.modularui.api.dom.MuiElement;
import com.cleanroommc.modularui.api.dom.MuiNode;
import com.cleanroommc.modularui.api.dom.MutationScope;
import com.cleanroommc.modularui.api.event.EventListenerOptions;
import com.cleanroommc.modularui.api.event.PointerEvent;
import com.cleanroommc.modularui.api.state.MuiStore;
import com.cleanroommc.modularui.api.state.MuiStoreBinding;
import com.cleanroommc.modularui.api.state.StoreSubscription;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

final class ShowcaseController implements AutoCloseable {
    private static final List<String> MODES = Arrays.asList("Balanced", "Quality", "Performance");

    private final ShowcaseScreen screen;
    private final MuiElement root;
    private final MuiStore store;
    private final List<AutoCloseable> subscriptions = new ArrayList<>();

    private ShowcaseController(ShowcaseScreen screen, MuiElement root, MuiStore store) {
        this.screen = screen;
        this.root = root;
        this.store = store;
    }

    static ShowcaseController attach(ShowcaseScreen screen, MuiElement root, MuiStore store) {
        ShowcaseController controller = new ShowcaseController(screen, root, store);
        controller.bind();
        return controller;
    }

    private void bind() {
        bindText("status", "#status-text");
        bindText("runtimeRows", "#runtime-count");
        bindText("events", "#event-count");
        bindText("scale", "#scale-value");
        bindText("sampleText", "#sample-preview");
        bindAttribute("fontEnabled", "#toggle-font", "checked");
        bindAttribute("shadowEnabled", "#toggle-shadow", "checked");
        bindText("mode", "#mode-value");
        subscriptions.add(store.subscribe(change -> {
            if (change.getChangedKeys().contains("page")) updatePage();
        }));
        updatePage();

        action("showcase:nav-overview", () -> selectPage("overview"));
        action("showcase:nav-components", () -> selectPage("components"));
        action("showcase:toggle-font", () -> toggle("fontEnabled", "Font renderer"));
        action("showcase:toggle-shadow", () -> toggle("shadowEnabled", "Text shadow"));
        action("showcase:cycle-mode", this::cycleMode);
        action("showcase:add-row", this::addRuntimeRow);
        action("showcase:remove-row", this::removeRuntimeRow);
        action("showcase:reset", this::reset);

        subscriptions.add(root.addEventListener(PointerEvent.DOWN, event -> {
            store.set("events", integer("events") + 1);
        }, EventListenerOptions.CAPTURE));
    }

    private void bindText(String key, String selector) {
        subscriptions.add(MuiStoreBinding.text(store, key, require(selector)));
    }

    private void bindAttribute(String key, String selector, String attribute) {
        subscriptions.add(MuiStoreBinding.attribute(store, key, require(selector), attribute));
    }

    private void action(String name, Runnable action) {
        subscriptions.add(root.getOwnerDocument().getActions().register(name, invocation -> {
            invocation.getEvent().preventDefault();
            action.run();
        }));
    }

    private void selectPage(String page) {
        store.patch(java.util.Map.of("page", page, "status", "DOM route switched to " + page));
    }

    private void updatePage() {
        String page = String.valueOf(store.get("page"));
        require("#page-overview").setAttribute("data-active", Boolean.toString("overview".equals(page)));
        require("#page-components").setAttribute("data-active", Boolean.toString("components".equals(page)));
        require("#nav-overview").setAttribute("data-selected", Boolean.toString("overview".equals(page)));
        require("#nav-components").setAttribute("data-selected", Boolean.toString("components".equals(page)));
    }

    private void toggle(String key, String label) {
        boolean next = !Boolean.TRUE.equals(store.get(key));
        store.patch(java.util.Map.of(key, next, "status", label + (next ? " enabled" : " disabled")));
    }

    private void cycleMode() {
        String current = String.valueOf(store.get("mode"));
        int index = MODES.indexOf(current);
        String next = MODES.get((index + 1 + MODES.size()) % MODES.size());
        store.patch(java.util.Map.of("mode", next, "status", "Rendering mode changed to " + next));
    }

    private void addRuntimeRow() {
        int number = integer("runtimeRows") + 1;
        MuiDocument document = root.getOwnerDocument();
        MuiElement list = require("#runtime-list");
        try (MutationScope mutation = document.beginMutation()) {
            MuiElement row = document.createElement("mui:container");
            row.setAttribute("class", "runtime-row");
            MuiElement name = document.createElement("mui:text");
            name.setAttribute("class", "runtime-name");
            name.setAttribute("text", "Runtime component " + number);
            MuiElement badge = document.createElement("mui:text");
            badge.setAttribute("class", "runtime-badge");
            badge.setAttribute("text", "DOM");
            row.appendChild(name);
            row.appendChild(badge);
            list.appendChild(row);
            mutation.commit();
        }
        store.patch(java.util.Map.of("runtimeRows", number,
                "status", "Created a projected component in one DOM transaction"));
    }

    private void removeRuntimeRow() {
        MuiElement list = require("#runtime-list");
        List<MuiNode> children = list.getChildNodes();
        if (children.isEmpty()) {
            store.set("status", "Runtime list is already empty");
            return;
        }
        list.removeChild(children.get(children.size() - 1));
        store.patch(java.util.Map.of("runtimeRows", Math.max(0, integer("runtimeRows") - 1),
                "status", "Removed the last runtime component"));
    }

    private void reset() {
        MuiElement list = require("#runtime-list");
        try (MutationScope mutation = root.getOwnerDocument().beginMutation()) {
            for (MuiNode child : new ArrayList<>(list.getChildNodes())) list.removeChild(child);
            mutation.commit();
        }
        store.reset();
        store.set("status", "Store and runtime DOM reset");
    }

    private int integer(String key) {
        Object value = store.get(key);
        return value instanceof Number ? ((Number) value).intValue() : 0;
    }

    private MuiElement require(String selector) {
        MuiElement element = root.querySelector(selector);
        if (element == null) throw new IllegalStateException("Missing showcase element: " + selector);
        return element;
    }

    @Override
    public void close() {
        for (int i = subscriptions.size() - 1; i >= 0; i--) {
            try { subscriptions.get(i).close(); }
            catch (Exception ignored) {}
        }
        subscriptions.clear();
    }
}
