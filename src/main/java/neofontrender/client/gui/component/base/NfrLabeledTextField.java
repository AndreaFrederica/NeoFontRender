package neofontrender.client.gui.component.base;

import com.cleanroommc.modularui.api.layout.ILayoutWidget;
import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.widget.ParentWidget;
import com.cleanroommc.modularui.widgets.TextWidget;
import com.cleanroommc.modularui.widgets.textfield.TextFieldWidget;
import com.cleanroommc.modularui.utils.Alignment;
import net.minecraft.client.Minecraft;
import com.cleanroommc.modularui.api.navigation.NavigationAction;
import com.cleanroommc.modularui.api.navigation.NavigationInfo;
import com.cleanroommc.modularui.api.navigation.NavigationRole;

/** A settings-form field with a consistently styled label above the editor. */
public final class NfrLabeledTextField extends ParentWidget<NfrLabeledTextField>
        implements ILayoutWidget, NfrPreferredHeight {
    private static final int EDITOR_HEIGHT = 18;
    private static final int BOTTOM_PADDING = 8;
    private final TextWidget label;
    private final TextFieldWidget field;

    public NfrLabeledTextField(String text, TextFieldWidget field) {
        this.label = new TextWidget(IKey.str(text)).alignment(Alignment.CenterLeft).color(0xA9B5C5);
        this.field = field;
        navigationInfo(NavigationInfo.builder(NavigationRole.GROUP).focusable(false).build());
        field.navigationInfo(NavigationInfo.builder(NavigationRole.TEXT_INPUT)
                .label(() -> text)
                .actions(NavigationAction.ACTIVATE, NavigationAction.BEGIN_EDIT,
                        NavigationAction.END_EDIT)
                .build());
        child(label);
        child(field);
    }

    @Override
    public int preferredHeight() {
        return Minecraft.getMinecraft().fontRenderer.FONT_HEIGHT + 3 + 4 + EDITOR_HEIGHT + BOTTOM_PADDING;
    }

    @Override
    public boolean layoutWidgets() {
        int width = getArea().w();
        int labelHeight = Minecraft.getMinecraft().fontRenderer.FONT_HEIGHT + 3;
        NfrLayout.place(label, 0, 0, width, labelHeight);
        NfrLayout.place(field, 0, labelHeight + 4, width,
                Math.max(EDITOR_HEIGHT, getArea().h() - labelHeight - 4 - BOTTOM_PADDING));
        return true;
    }
}
