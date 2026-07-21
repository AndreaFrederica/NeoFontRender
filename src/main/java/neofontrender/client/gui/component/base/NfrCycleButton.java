package neofontrender.client.gui.component.base;

import java.util.function.Supplier;

/**
 * Reusable public cycle button for Neo Font Render and dependent mods.
 *
 * <p>The component intentionally has no in-project call site yet: it is part of the exported
 * base component library.</p>
 */
public final class NfrCycleButton extends NfrTextButton {
    public NfrCycleButton(Supplier<String> label, Runnable cycle) {
        super(label, true);
        onMousePressed(button -> {
            cycle.run();
            return true;
        });
    }
}
