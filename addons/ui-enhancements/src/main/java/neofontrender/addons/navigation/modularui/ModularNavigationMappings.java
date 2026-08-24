package neofontrender.addons.navigation.modularui;

import com.cleanroommc.modularui.api.navigation.NavigationAction;
import com.cleanroommc.modularui.api.navigation.NavigationActionResult;
import com.cleanroommc.modularui.api.navigation.NavigationAxis;
import com.cleanroommc.modularui.api.navigation.NavigationRole;
import neofontrender.addons.api.ui.navigation.UiAction;
import neofontrender.addons.api.ui.navigation.UiActionResult;
import neofontrender.addons.api.ui.navigation.UiAxis;
import neofontrender.addons.api.ui.navigation.UiRole;

final class ModularNavigationMappings {
    private ModularNavigationMappings() {}

    static UiRole role(NavigationRole role) {
        return role == NavigationRole.NONE ? UiRole.GROUP : UiRole.valueOf(role.name());
    }

    static UiAction action(NavigationAction action) { return UiAction.valueOf(action.name()); }
    static NavigationAction action(UiAction action) { return NavigationAction.valueOf(action.name()); }
    static UiAxis axis(NavigationAxis axis) { return UiAxis.valueOf(axis.name()); }

    static UiActionResult result(NavigationActionResult result) {
        switch (result) {
            case HANDLED: return UiActionResult.HANDLED;
            case CHANGED: return UiActionResult.CHANGED;
            case STALE: return UiActionResult.STALE;
            case REJECTED: return UiActionResult.REJECTED;
            case IGNORED:
            default: return UiActionResult.IGNORED;
        }
    }
}
