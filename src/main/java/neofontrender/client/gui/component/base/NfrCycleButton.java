package neofontrender.client.gui.component.base;

import com.cleanroommc.modularui.api.navigation.INavigationActionHandler;
import com.cleanroommc.modularui.api.navigation.NavigationAction;
import com.cleanroommc.modularui.api.navigation.NavigationActionResult;
import com.cleanroommc.modularui.api.navigation.NavigationInfo;
import com.cleanroommc.modularui.api.navigation.NavigationRole;

import java.util.function.Supplier;

/**
 * Reusable public cycle button for Neo Font Render and dependent mods.
 *
 * <p>The component intentionally has no in-project call site yet: it is part of the exported
 * base component library.</p>
 */
public final class NfrCycleButton extends NfrTextButton implements INavigationActionHandler {
    private final Runnable cycle;

    public NfrCycleButton(Supplier<String> label, Runnable cycle) {
        super(label, true);
        this.cycle = cycle;
        navigationInfo(NavigationInfo.builder(NavigationRole.CYCLE)
                .label(label)
                .actions(NavigationAction.ACTIVATE, NavigationAction.INCREMENT)
                .build());
        onMousePressed(button -> {
            cycle.run();
            return true;
        });
    }

    @Override public NavigationActionResult onNavigationAction(NavigationAction action) {
        if (action != NavigationAction.ACTIVATE && action != NavigationAction.INCREMENT) {
            return NavigationActionResult.IGNORED;
        }
        cycle.run();
        return NavigationActionResult.CHANGED;
    }
}
