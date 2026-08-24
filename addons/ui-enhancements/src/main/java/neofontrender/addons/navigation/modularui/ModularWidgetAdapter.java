package neofontrender.addons.navigation.modularui;

import com.cleanroommc.modularui.api.navigation.NavigationInfo;
import com.cleanroommc.modularui.api.widget.IWidget;

public interface ModularWidgetAdapter {
    boolean supports(IWidget widget);
    NavigationInfo navigationInfo(IWidget widget, NavigationInfo declared);
}
